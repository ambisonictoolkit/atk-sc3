/*
	Copyright the ATK Community and Joseph Anderson, 2011-2026
		J Anderson	j.anderson[at]ambisonictoolkit.net
		M McCrea

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
// 	Class: CartesianMatrix
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
//---------------------------------------------------------------------

CartesianMatrix : AtkMatrix {


	*newFromMatrix { |matrix|
		if(matrix.isKindOf(Matrix).not, {
			"[CarteseianMatrix:*newFromMatrix] The passed matrix must be of class Matrix.".throw
		});
		^super.new(\fromMatrix).initFromMatrix(matrix)
	}

	// call by subclass, only
	*newFromFile { |filePathOrName, searchExtensions = true|
		^super.new(\fromFile).initFromFile(filePathOrName, searchExtensions)
	}

	// TODO
/*	initDirections { |argDirections|
		directions = if(argDirections == nil, {
			(this.order.asHoaOrder.size).collect({ inf })
		}, {
			switch(argDirections.rank,
				0, { Array.with(argDirections, 0.0).reshape(1, 2) },
				1, { argDirections.collect({ |dir| Array.with(dir, 0.0) }) },
				2, { argDirections }
			).collect({ |thetaPhi|  // wrap to [ + /-pi, +/-pi/2]
				Spherical.new(1, thetaPhi[0], thetaPhi[1]).asCartesian.asSpherical.angles
			})
		})
	}
*/

	set { ^\Cartesian }

	type { ^\directions }

	// TODO:
	// By definition Cartesian points are 3D, but could
	// could inspect directions and report whether they points are
	// confined to the horizontal plan, in which case answer 2
	dim { ^3 }

	size { ^matrix.rows }
	numChannels { ^this.size }

	/* Return radians from cartesian directions */
	directions {
		var dirs = Array.newClear(this.size);
		var sph;

		matrix.rowsDo({ |row, i|
			sph = Cartesian(*row).asSpherical;
			dirs[i] = [sph.theta, sph.phi]; // azim, elev
		});

		^dirs
	}
	dirChannels { ^this.directions }
	dirOutputs { ^this.directions }


	coords { ^matrix.asArray }
	points { ^this.coords }

	initFromMatrix { |aMatrix|

		// set instance matrix
		matrix = aMatrix.thresh2(AtkHoa.thresh);

		// Check: directions are 3-vectors
		if(matrix.cols != 3, {
			Error(
				(
					"[%:-initFromMatrix] An 'cartesian' matrix "
					"should have 3 columns. rows = %, cols = %"
				).format(
					this.class.asString, matrix.rows, matrix.cols
				)
			).errorString.postln;
			this.halt
		});
	}

	/* TODO */
	initFromFile {} /*{ |filePathOrName, searchExtensions|
		var pn, dict;
		var instVars, instMeths;

		// (redundant) instance variables & methods to remove from fileParse
		instVars = List.with(\kind, \matrix, \order, \filePath, \directions);
		instMeths = List.with(\op, \set, \type, \dim, \fileName,
			\numChannels, \numInputs, \numOutputs, \dirInputs, \dirOutputs);

		// first try with path name only
		pn = Atk.resolveMtxPath(filePathOrName);

		pn ?? {
			// partial path reqires set to resolve
			pn = Atk.resolveMtxPath(filePathOrName, this.type, this.set, searchExtensions);
		};

		// instance var
		filePath = pn.fullPath;

		case(
			{ pn.extension == "yml" }, {
				dict = filePath.parseYAMLFile;
				fileParse = IdentityDictionary(know: true);

				// replace String keys with Symbol keys, make "knowable"
				dict.keysValuesDo{ |k, v|
					fileParse.put(k.asSymbol,
						if(v == "nil", { nil }, { v }) // so .info parsing doesn't see nil as array
					)
				};

				// check against \set
				if(fileParse[\set].isNil, {
					"Matrix 'set' is undefined in the .yml file: cannot confirm the "
					"set matches the loaded object".warn
				}, {
					if((fileParse[\set].asSymbol != this.set.asSymbol), {
						Error(
							(
								"[%:-initFromFile] Matrix 'set' defined in the .yml file (%) doesn't match "
								"the object set trying to load (%)"
							).format(
								this.class.asString, fileParse[\set], this.set
							).errorString.postln;
							this.halt
						)
					})
				});

				// check against \type
				if((fileParse[\type].isNil), {
					"Matrix 'type' is undefined in the .yml file: cannot confirm the "
					"type matches the loaded object (encoder/decoder/xformer)".warn
				}, {
					if((fileParse[\type].asSymbol != this.type.asSymbol), {
						Error(
							format(
								"[%:-initFromFile] Matrix 'type' defined in the .yml file (%) doesn't match "
								"the type of matrix you're trying to load (%)",
								this.class.asString, fileParse[\type], this.type
							).errorString.postln;
							this.halt
						)
					})
				});

				// set unset instance vars
				kind = if(fileParse.kind.notNil, {  // reset kind
					fileParse.kind.asSymbol
				}, {
					pn.fileNameWithoutExtension.asSymbol
				});
				matrix = Matrix.with(fileParse.matrix.asFloat).thresh2(AtkHoa.thresh);
				directions = fileParse.directions.asFloat;

				// Remove parsed Instance variables & methods from fileParse.
				// Keep just the user defined attributes and values.
				//
				// May wish to revisit.
				instVars.do({ |att|
					if(fileParse.includesKey(att), {
						fileParse.removeAt(att)
					})
				});
				instMeths.do({ |att|
					if(fileParse.includesKey(att), {
						fileParse.removeAt(att)
					})
				})
			},
			{	// catch all, including .txt
				Error(
					"[%:-initFromFile] Unsupported file extension.".format(this.class.asString)
				).errorString.postln;
				this.halt
			}
		)
	}*/


/*
	TODO: the below method was copied from HoaMatrix.
	That should be revisited given this new "native" support for t-designs
*/
	initDirTDesign {}/*{ |design, order|
		var minT = 2 * order;

		// check for valid t
		if(design.t >= minT, {
			directions = design.directions
		}, {
			format(
				"[HoaMatrix -initDirTDesign] A t-design of t >= % is required for order %.\nSupplied design t: ",
				minT, order, design.t
			).throw
		})
	}*/

	// TODO
	visualize {}

	// separate YML writer for Cartesian
	prWriteMatrixToYML { |pn, note, attributeDictionary|
		var wr, wrAtt, wrAttArr, defaults;

		wr = FileWriter(pn.fullPath);

		// function to write a one-line attribute
		wrAtt = { |att, val|
			wr.write("% : ".format(att));
			wr.write(
				(
					val ?? { this.tryPerform(att) }
				).asCompileString; // allow for large strings
			);
			wr.write("\n\n");
		};

		// function to write a multi-line attribute (2D array)
		wrAttArr = { |att, arr|
			var vals = arr ?? { this.tryPerform(att) };

			if(vals.isNil, {
				wr.writeLine(["% : nil".format(att)]);
			}, {
				wr.writeLine(["% : [".format(att)]);
				(vals.asArray).do({ |elem, i|
					wr.write(elem.asCompileString); // allow for large row strings
					wr.write(
						(i == (vals.size - 1)).if({ "\n]\n" }, { ", \n" })
					)
				})
			});
			wr.write("\n");
		};

		// specify default attributes to write - use to catch
		// conflicting values if found in user supplied attributeDictionary
		defaults = [\set, \type, \kind, \dim, \directions];

		// write attributes
		wrAtt.(\fileName, pn.fileName);
		note !? { wrAtt.(\note, note) };

		// remove defaults from supplied attributeDictionary
		// and write out
		if(attributeDictionary.notNil, {
			defaults.do({ |att|
				attributeDictionary.removeAt(att)
			});
			attributeDictionary.keysValuesDo({ |k, v|
				if(v.isKindOf(Array), { wrAttArr.(k, v) }, { wrAtt.(k, v) })
			})
		});

		// the rest of the attributes
		wrAtt.(\set);
		wrAtt.(\type);
		wrAtt.(\kind);
		wrAtt.(\dim);
		//wrAttArr.(\directions); // don't write directions, it's redundant with matrix of directional coords
		wrAttArr.(\matrix);

		wr.close;
	}
}