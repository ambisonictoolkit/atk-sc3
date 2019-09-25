/*
	Copyright the ATK Community and Joseph Anderson, 2011-2016
		J Anderson	j.anderson[at]ambisonictoolkit.net


	This file is part of SuperCollider3 version of the Ambisonic Toolkit (ATK).

	The SuperCollider3 version of the Ambisonic Toolkit (ATK) is free software:
	you can redistribute it and/or modify it under the terms of the GNU General
	Public License as published by the Free Software Foundation, either version 3
	of the License, or (at your option) any later version.

	The SuperCollider3 version of the Ambisonic Toolkit (ATK) is distributed in
	the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
	implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See
	the GNU General Public License for more details.

	You should have received a copy of the GNU General Public License along with the
	SuperCollider3 version of the Ambisonic Toolkit (ATK). If not, see
	<http://www.gnu.org/licenses/>.
*/


//---------------------------------------------------------------------
//	The Ambisonic Toolkit (ATK) is a soundfield kernel support library.
//
// 	Class: AtkMatrix
//
//	The Ambisonic Toolkit (ATK) is intended to bring together a number of tools and
//	methods for working with Ambisonic surround sound. The intention is for the toolset
//	to be both ergonomic and comprehensive, providing both classic and novel algorithms
//	to creatively manipulate and synthesise complex Ambisonic soundfields.
//
//	The tools are framed for the user to think in terms of the soundfield kernel. By
//	this, it is meant the ATK addresses the holistic problem of creatively controlling a
//	complete soundfield, allowing and encouraging the composer to think beyond the placement
//	of sounds in a sound-space and instead attend to the impression and image of a soundfield.
//	This approach takes advantage of the model the Ambisonic technology presents, and is
//	viewed to be the idiomatic mode for working with the Ambisonic technique.
//
//
//	We hope you enjoy the ATK!
//
//	For more information visit http://ambisonictoolkit.net/ or
//	email info[at]ambisonictoolkit.net
//
//-----------------------------------------------------------------------


/*
superclass of:

FoaMatrix
HoaMatrix

*/

AtkMatrix {
	// copyArgs
	var <kind;

	var <matrix;
	var <order;			// order for this matrix
	var <filePath;		// matrices from files only
	var <fileParse;		// data parsed from YAML file

	// call by subclass, only
	*new { |kind, order|
		^super.newCopyArgs(kind).init(order)
	}

	/*
	Add a warning against calling directly.
	*/
	init { |argOrder|
		(this.set == \FOA).if({
			order = 1;
		}, {
			// resolve HOA
			order = argOrder ?? { AtkHoa.defaultOrder };
		})
	}

	// For subclasses of AtkMatrix
	writeToFile { |fileNameOrPath, note, attributeDictionary, overwrite = false|
		this.prWriteToFile(fileNameOrPath, note, attributeDictionary, overwrite);
	}

	prWriteToFile { |fileNameOrPath, note, attributeDictionary, overwrite = false|
		var pn, ext;
		var mtxPath, relPath;

		pn = PathName(fileNameOrPath);

		PathName(pn.parentPath).isFolder.not.if{ // check for an enclosing folder
			// ... no enclosing folder found so assumed
			// to be relative to extensions/matrices/'type' directory

			Atk.checkSet(this.set);

			// This is only needed for relative file paths in user-matrices directory
			['encoder', 'decoder', 'xformer'].includes(this.type).not.if{
				Error(
					"'type' argument must be 'encoder', 'decoder', or 'xformer'"
				).errorString.postln;
				this.halt;
			};

			case(
				{ pn.colonIndices.size == 0 }, {
					// only filename provided, write to dir matching 'type'
					pn = Atk.getMatrixExtensionSubPath(this.set, this.type) +/+ pn
				},
				{ pn.colonIndices.size > 0 }, {
					// relative path given, look for it
					mtxPath = Atk.getMatrixExtensionSubPath(this.set, this.type);
					relPath = (mtxPath +/+ PathName(pn.parentPath));
					relPath.isFolder.if({
						// valid relative path confirmed
						pn = mtxPath +/+ pn;
					}, {
						Error(
							format(
								"Specified relative folder path was not found in %\n",
								relPath.fullPath
							)
						).errorString.postln;
						this.halt;
					})
				}
			)
		}; // otherwise, provided path is absolute

		ext = pn.extension;
		(ext == "").if{ pn = pn +/+ PathName(".yml") };

		overwrite.not.if{
			pn.isFile.if{
				Error(
					format(
						"File already exists:\n\t%\nChoose another name or location, "
						"or set overwrite: true", pn.fullPath
					)
				).errorString.postln;
				this.halt;
			}
		};

		case(
			{ ext == "txt" }, {
				pn.fileName.contains(".mosl").if({
					this.prWriteMatrixToMOSL(pn)
				}, {
					this.prWriteMatrixToTXT(pn)
				})
			},
			{ ext == "yml" }, { this.prWriteMatrixToYML(pn, note, attributeDictionary) },
			{	// catch all
				Error(
					"Invalid file extension: provide '.txt' for writing matrix only, "
					"or '.yml' or no extension to write matrix with metadata (as YAML)"
				).errorString.postln;
				this.halt
			}
		)
	}


	prWriteMatrixToTXT { |pn| // a PathName
		var wr;
		wr = FileWriter(pn.fullPath);
		// write the matrix into it by row, and close
		matrix.rows.do{ |i| wr.writeLine(matrix.getRow(i)) };
		wr.close;
	}

	prWriteMatrixToMOSL { |pn| // a PathName
		var wr;
		wr = FileWriter(pn.fullPath);

		// write num rows and cols to first 2 lines
		wr.writeLine(["// Dimensions: rows, columns"]);
		wr.writeLine(matrix.rows.asArray);
		wr.writeLine(matrix.cols.asArray);
		// write the matrix into it by row, and close
		matrix.rows.do{ |i|
			var row;
			wr.writeLine([""]); // blank line
			wr.writeLine([format("// Row %", i)]);

			row = matrix.getRow(i);
			row.do{ |j| wr.writeLine(j.asArray) };
		};
		wr.close;
	}

	// separate YML writer for FOA & HOA
	// prWriteMatrixToYML

	fileName { ^try { PathName(filePath).fileName } }

	asArray { ^matrix.asArray }

	// ---------
	// return info

	/*
	methods deferred to subclasses:

	-set                 : FoaMatrix, HoaMatrix
	-type               : FoaMatrix, HoaMatrix
	-numChannels : FoaMatrix, HoaMatrix
	-dim                : FoaMatrix, HoaMatrix
	-dirInputs        : FoaMatrix
	-dirOutputs     : FoaMatrix
	-directions      : FoaMatrix, HoaMatrix

	*/

	op { ^\matrix }

	numInputs { ^matrix.cols }

	numOutputs { ^matrix.rows }

	printOn { |stream|
		stream << this.class.name << "(" <<* [kind, this.dim, this.numChannels] <<")";
	}

	// post readable matrix information
	info {
		var attributes;

		// gather attributes in order of posting
		attributes = List.with(\set, \kind, \dim);
		this.isKindOf(FoaDecoderMatrix).if{
			attributes.add(\shelfK).add(\shelfFreq)
		};

		// other non-standard metadata provided in yml file
		fileParse !? {
			fileParse.keys.do{ |key|
				attributes.includes(key.asSymbol).not.if{
					attributes.add(key.asSymbol);
				}
			}
		};

		// bump 'type' to the top of the post...
		attributes.includes(\type).if{ attributes.remove(\type) };
		attributes.addFirst(\type);

		// ... then bump 'set' to the top of the post
		attributes.includes(\set).if{ attributes.remove(\set) };
		attributes.addFirst(\set);

		// bump to the bottom of the post
		[\dirInputs, \dirOutputs, \matrix].do{ |att|
			attributes.includes(att).if{ attributes.remove(att) };
			attributes.add(att);
		};

		filePath !? { attributes.add(\fileName).add(\filePath) };

		// remove any duplicated attributes
		attributes = OrderedIdentitySet.newFrom(attributes.reverse).asList.reverse;

		postf("\n*** % Info ***\n", this.class);

		attributes.do{ |attribute|
			var value, str;

			value = this.tryPerform(attribute);

			(value.isNil and: fileParse.notNil).if{
				value = fileParse[attribute] // this can still return nil
			};

			value.isKindOf(Array).if({
				value = value.asArray; // cast the Matrix to array for posting
				(value.rank > 1).if({
					postf("-> %\n  [\n", attribute);
					value.do{ |elem|
						postf("\t%\n",
							try {
								elem.round(0.0001).collect({ |num|
									str = num.asString.padRight(
										num.isPositive.if({ 6 }, { 7 }),
										"0"
									);
									str.padLeft(7, " ")
								})
							} { elem }
						)
					};
					"  ]".postln;
				}, {
					postf("-> %\n\t%\n", attribute, value);
				})
			}, {
				postf("-> %\n\t%\n", attribute, value);
			});
		};
	}

}
