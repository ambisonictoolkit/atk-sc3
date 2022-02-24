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
// 	Class: FoaSpeakerMatrix
// 	Class: FoaMatrix
// 	Class: FoaDecoderMatrix
// 	Class: FoaEncoderMatrix
// 	Class: FoaXformerMatrix
// 	Class: FoaDecoderKernel
// 	Class: FoaEncoderKernel
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


//-----------------------------------------------------------------------
// Third Party Notices
//-----------------------------------------------------------------------
//
//-----------------------------------------------------------------------
// Support for Gerzon's Diametric Decoder Theorem (DDT) decoding
// algorithm is derived from Aaron Heller's Octave code available at:
// http://www.ai.sri.com/ajh/ambisonics/
//
// Benjamin, et al., "Localization in Horizontal-Only Ambisonic Systems"
// Preprint from AES-121, 10/2006, San Francisco
//
// Implementation in the SuperCollider3 version of the ATK is by
// Joseph Anderson <j.anderson[at]ambisonictoolkit.net>
//-----------------------------------------------------------------------
//
//
//-----------------------------------------------------------------------
// Irregular array decoding coefficients (5.0) are kindly provided
// by Bruce Wiggins: http://www.brucewiggins.co.uk/
//
// B. Wiggins, "An Investigation into the Real-time Manipulation and
// Control of Three-dimensional Sound Fields, " PhD Thesis, University of
// Derby, Derby, 2004.
//-----------------------------------------------------------------------


//-----------------------------------------------------------------------
// matrix decoders

/*
Given HOA, FoaSpeakerMatrix is redundant.

TODO: Replace with updated HOA implementation.
*/

//   Heller's DDT (helper function)
FoaSpeakerMatrix {
	var <positions, <k, m, n;

	*new { |directions, k|
		var positions;

		switch(directions.rank,					// 2D or 3D?
			1, { positions = Matrix.with(			// 2D
					directions.collect({ |item|
						Polar.new(1, item).asPoint.asArray
					})
				)
			},
			2, { positions = Matrix.with(			// 3D
					directions.collect({ |item|
						Spherical.new(1, item[0], item[1]).asCartesian.asArray
					})
				)
			}
		);

		^super.newCopyArgs(positions, k).initDiametric;
	}

	*newPositions { |positions, k|
		^super.newCopyArgs(positions, k).initDiametric;
	}

	initDiametric {

		// n = number of output channel (speaker) pairs
		// m = number of dimensions,
		//        2 = horizontal, 3 = periphonic
		m = this.positions.cols;
		n = this.positions.rows;
	}

	dim { ^m }

	numChannels { ^n * 2 }

	matrix {
		var s, directions, pos, dir;

		// scatter matrix accumulator
		s = Matrix.newClear(m, m);

		// output channel (speaker) directions matrix
		// NOTE: this isn't the user supplied directions arg
		directions = Matrix.newClear(m, n);

		n.do({ |i|

			// allow entry of positions as
			// transpose for convenience
			// e.g., output channel (speaker) positions are now in columns
			// rather than rows, then
			// get the i'th output channel (speaker) position
			// e.g., select the i'th column
			pos = positions.flop.getCol(i);

			// normalize to get direction cosines
			dir = pos /  pos.squared.sum.sqrt;

			// form scatter matrix and accumulate
			s = s + Matrix.with(dir * dir.flop);

			// form matrix of output channel (speaker) directions
			directions.putCol(i, dir)

		});

		// return resulting matrix
		^sqrt(1 / 2) * n * k * (s.inverse * directions);
	}

	printOn { |stream|
		stream << this.class.name << "(" <<* [this.dim, this.numChannels] <<")";
	}
}


FoaMatrix : AtkMatrix {
	var <>dirChannels;  // setter added for matrix-to-file & file-to-matrix support

	// most typically called by subclass
	*new { |kind|
		^super.new(kind).init
	}

	*newFromMatrix { |matrix, directions|
		^super.new(\fromMatrix).initFromMatrix(matrix, directions)
	}

	initFromMatrix { |aMatrix, argDirections|
		var numCoeffs;
		var numCoeffsDim, dirChannelsDim;

		// set instance matrix
		matrix = aMatrix;

		// set instance dirChannels
		dirChannels = argDirections;

		// 1) Check: matrix numChannels == directions.size
		if(this.numChannels != this.dirChannels.size, {
			Error(
				format(
					"[%:-initFromMatrix] Matrix number of channels "
					"should matches directions. Number of channels = %, number of directions = %",
					this.class.asString, this.numChannels, this.dirChannels.size
				)
			).errorString.postln;
			this.halt
		});

		// 2) Check: matrix numCoeffs == 3 || 4, is it a valid 2D or 3D FOA matrix?
		numCoeffs = switch(this.type,
			\encoder, { matrix.rows },
			\decoder, { matrix.cols },
			\xformer, { matrix.rows }
		);
		if((numCoeffs == 3 or: { numCoeffs == 4 }).not, {
			Error(
				format(
					"[%:-initFromMatrix] Matrix coefficient size invalid for FOA."
					"Should be 3 for 2D or 4 for 3D. (detected: %)",
					this.class.asString, numCoeffs
				)
			).errorString.postln;
			this.halt
		});

		// 3) Check: matrix dim against dirChannels dim
		numCoeffsDim = numCoeffs - 1;
		dirChannelsDim = if(this.type == \xformer, {
			this.dim  // exception required for \xformer, as dirChannels = nil
		}, {
			this.dirChannels.rank + 1
		});
		if(numCoeffsDim != dirChannelsDim, {
			Error(
				format(
					"[%:-initFromMatrix] Detected matrix and specified directions "
					"dimensions don't matched. (matrix: %, directions: %)",
					this.class.asString, numCoeffsDim, dirChannelsDim
				)
			).errorString.postln;
			this.halt
		});

		// 4) Check: xformer is square matrix
		if((this.type == \xformer and: { matrix.isSquare.not }), {
			Error(
				format(
					"[%:-initFromMatrix] An 'xformer' matrix "
					"should be square. rows = %, cols = %",
					this.class.asString, matrix.rows, matrix.cols
				)
			).errorString.postln;
			this.halt
		})
	}

	initFromFile { |filePathOrName, mtxType, searchExtensions = false|
		var pn, dict;

		// first try with path name only
		pn = Atk.resolveMtxPath(filePathOrName);

		pn ?? {
			// partial path reqires set to resolve
			pn = Atk.resolveMtxPath(filePathOrName, mtxType, this.set, searchExtensions);
		};

		// instance var
		filePath = pn.fullPath;

		case(
			{ pn.extension == "txt" }, {
				matrix = if(pn.fileName.contains(".mosl"), {
					// .mosl.txt file: expected to be matrix only,
					// single values on each line, by rows
					Matrix.with(this.prParseMOSL(pn));
				}, {
					// .txt file: expected to be matrix only, cols
					// separated by spaces, rows by newlines
					Matrix.with(FileReader.read(filePath).asFloat);
				});

				kind = pn.fileName.asSymbol // kind defaults to filename
			},
			{ pn.extension == "yml" }, {
				dict = filePath.parseYAMLFile;
				fileParse = IdentityDictionary(know: true);

				// replace String keys with Symbol keys, make "knowable"
				dict.keysValuesDo{ |k, v|
					fileParse.put(k.asSymbol,
						(v == "nil").if({ nil }, { v }) // so .info parsing doesn't see nil as array
					)
				};

				if(fileParse[\type].isNil, {
					"Matrix 'type' is undefined in the .yml file: cannot confirm the "
					"type matches the loaded object (encoder/decoder/xformer)".warn
				}, {
					if((fileParse[\type].asSymbol != mtxType.asSymbol), {
						Error(
							format(
								"[%:-initFromFile] Matrix 'type' defined in the .yml file (%) doesn't match "
								"the type of matrix you're trying to load (%)",
								this.class.asString, fileParse[\type], mtxType
							).errorString.postln;
							this.halt
						)
					})
				});

				matrix = Matrix.with(fileParse.matrix.asFloat);

				kind = if(fileParse.kind.notNil, {
					fileParse.kind.asSymbol
				}, {
					pn.fileNameWithoutExtension.asSymbol
				})
			},
			{ // catch all
				Error(
					"[%:-initFromFile] Unsupported file extension.".format(this.class.asString)
				).errorString.postln;
				this.halt
			}
		)
	}

	// separate YML writer for FOA
	prWriteMatrixToYML { |pn, note, attributeDictionary|
		var wr, wrAtt, wrAttArr, defaults;
		var dirIns, dirOuts;

		wr = FileWriter(pn.fullPath);

		// write a one-line attribute
		wrAtt = { |att, val|
			wr.write("% : ".format(att));
			wr.write(
				(
					val ?? { this.tryPerform(att) }
				).asCompileString; // allow for large strings
			);
			wr.write("\n\n")
		};

		// write a multi-line attribute (2D array)
		wrAttArr = { |att, arr|
			var vals = arr ?? { this.tryPerform(att) };

			if(vals.isNil, {
				wr.writeLine(["% : nil".format(att)])
			}, {
				wr.writeLine(["% : [".format(att)]);
				(vals.asArray).do({ |elem, i|
					wr.write(elem.asCompileString); // allow for large row strings
					wr.write(
						(i == (vals.size - 1)).if({ "\n]\n" }, { ",\n" })
					)
				})
			});
			wr.write("\n")
		};

		note !? { wrAtt.(\note, note) };

		wrAtt.(\type);

		// write default attributes
		defaults = if(this.type == \decoder, {
			[\kind, \shelfK, \shelfFreq]
		}, {
			[\kind]
		});

		if(attributeDictionary.notNil, {
			// make sure attribute dict doesn't explicitly set the attribute first
			defaults.do({ |att|
				attributeDictionary[att] ?? { wrAtt.(att) }
			})
		}, {
			defaults.do({ |att| wrAtt.(att) })
		});

		attributeDictionary !? {
			attributeDictionary.keysValuesDo{ |k, v|
				// catch overridden dirIn/Outputs
				switch(k,
					\dirInputs, { dirIns = v },
					\dirOutputs, { dirOuts = v },
					{
						if(v.isKindOf(Array), {
							wrAttArr.(k, v)
						}, {
							wrAtt.(k, v)
						})
					}
				)
			}
		};

		wrAttArr.(\dirInputs, dirIns);
		wrAttArr.(\dirOutputs, dirOuts);
		wrAttArr.(\matrix);

		wr.close;
	}

	prParseMOSL { |pn|
		var file, numRows, numCols, mtx, row;

		file = FileReader.read(pn.fullPath);
		numRows = nil;
		numCols = nil;
		mtx = [];
		row = [];
		file.do({ |line|
			var val = line[0];

			switch(val,
				"//",	{ }, // ignore comments: maintain backwards compatability
				"#",	{ }, // ignore comments
				";",	{ }, // ignore comments
				"",		{ }, // ignore blank line
				{	// found valid line
					case(
						{ numRows.isNil }, { numRows = val.asInteger },
						{ numCols.isNil }, { numCols = val.asInteger },
						{
							row = row.add(val.asFloat);
							if(row.size == numCols, {
								mtx = mtx.add(row);
								row = [];
							})
						}
					)
				}
			)
		});
		// test matrix dimensions
		if((mtx.size == numRows).not, {
			Error(
				format(
					"Mismatch in matrix dimensions: rows specified [%], rows parsed from file [%]",
					numRows, mtx.size
				)
			).throw
		});
		mtx.do({ |row, i|
			if(row.size != numCols, {
				Error(
					format(
						"Mismatch in matrix dimensions: rows % has % columns, but file species %",
						i, row.size, numCols
					)
				).throw
			})
		});

		^mtx
	}

	// FOA only
	loadFromLib { |...args|
		var pathStr;

		pathStr = this.kind.asString ++ "/";

		if(args.size == 0, {
			// no args... filename is assumed to be this.kind
			pathStr = this.kind.asString
		}, {
			args.do({ |argParam, i|
				pathStr = if(i > 0, {
					format("%-%", pathStr, argParam.asString)
				}, {
					format("%%", pathStr, argParam.asString)
				})
			})
		});

		this.initFromFile(pathStr ++ ".yml", this.type, false);

		switch(this.type,
			\encoder, { this.initEncoderVarsForFiles }, // properly set dirInputs
			\decoder, { this.initDecoderVarsForFiles }, // properly set dirOutputs
			\xformer, { }
		)
	}

	set { ^\FOA }

	// infer from subclass
	type { ^this.class.asString.drop(3).drop(-6).toLower.asSymbol }

	numChannels {
		^switch(this.type,
			\encoder, { this.numInputs },
			\decoder, { this.numOutputs },
			\xformer, { 4 }
		)
	}

	dim {
		^switch(this.type,
			\encoder, { this.numOutputs - 1 },
			\decoder, { this.numInputs - 1 },
			\xformer, { 3 }  // all transforms are 3D
		)
	}

	dirInputs {
		^switch(this.type,
			\encoder, { this.dirChannels },
			\decoder, { (this.numInputs).collect({ inf }) },
			// \xformer, { this.numInputs.collect({ inf }) },
			\xformer, { this.dirChannels }  // requires set to inf
		)
	}

	dirOutputs {
		^switch(this.type,
			\encoder, { (this.numOutputs).collect({ inf }) },
			\decoder, { this.dirChannels },
			// \xformer, { this.numInputs.collect({ inf }) },
			\xformer, { this.dirChannels }  // requires set to inf
		)
	}

	directions { ^this.dirChannels }

}


FoaDecoderMatrix : FoaMatrix {
	var <>shelfFreq, <shelfK;

	*newDiametric { |directions = ([pi / 4, 3 * pi / 4]), k = \single|
		^super.new(\diametric).initDiametric(directions, k);
	}

	*newPanto { |numChans = 4, orientation = \flat, k = \single|
		^super.new(\panto).initPanto(numChans, orientation, k);
	}

	*newPeri { |numChanPairs = 4, elevation = 0.61547970867039, orientation = \flat, k = \single|
		^super.new(\peri).initPeri(numChanPairs, elevation, orientation, k);
	}

	*newQuad { |angle = (pi / 4), k = \single|
		^super.new(\quad).initQuad(angle, k);
	}

	*newStereo { |angle = (pi/2), pattern = 0.5|
		^super.new(\stereo).initStereo(angle, pattern);
	}

	*newMono { |theta = 0, phi = 0, pattern = 0|
		^super.new(\mono).initMono(theta, phi, pattern);
	}

	*new5_0 { |irregKind = \focused|
		^super.new('5_0').loadFromLib(irregKind);
	}

	*newBtoA { |orientation = \flu, weight = \dec|
		^super.new(\BtoA).loadFromLib(orientation, weight);
	}

	*newHoa1 { |ordering = \acn, normalisation = \n3d|
		^super.new(\hoa1).loadFromLib(ordering, normalisation);
	}

	*newAmbix1 {
		var ordering = \acn, normalisation = \sn3d;

		^super.new(\hoa1).loadFromLib(ordering, normalisation);
	}

	*newFromFile { |filePathOrName|
		^super.new.initFromFile(filePathOrName, \decoder, true).initDecoderVarsForFiles;
	}

	initK2D { |k|

		if(k.isNumber, {
			^k
		}, {
			^switch(k,
				\velocity, { 1 },
				\energy, { 2.reciprocal.sqrt },
				\controlled, { 2.reciprocal },
				\single, { 2.reciprocal.sqrt },
				\dual, {
					shelfFreq = 400.0;
					shelfK = [(3/2).sqrt, 3.sqrt/2];
					1 // return
				}
			)
		})
	}

	initK3D { |k|

		if(k.isNumber, {
			^k
		}, {
			^switch(k,
				\velocity, { 1 },
				\energy, { 3.reciprocal.sqrt },
				\controlled, { 3.reciprocal },
				\single, { 3.reciprocal.sqrt },
				\dual, {
					shelfFreq = 400.0;
					shelfK = [2.sqrt, (2 / 3).sqrt];
					1 // return
				}
			)
		})
	}

	initDiametric { |directions, k|
		var positions, positions2;
		var speakerMatrix, n;

		switch(directions.rank,  // 2D or 3D?
			1, {  // 2D

				// find positions
				positions = Matrix.with(
					directions.collect({ |item|
						Polar.new(1, item).asPoint.asArray
					})
				);

				// list all of the output channels (speakers)
				// i.e., expand to actual pairs
				positions2 = positions ++ (positions.neg);

				// set output channel (speaker) directions for instance
				dirChannels = positions2.asArray.collect({ |item|
					item.asPoint.asPolar.angle
				});

				// initialise k
				k = this.initK2D(k)
			},
			2, {  // 3D

				// find positions
				positions = Matrix.with(
					directions.collect({ |item|
						Spherical.new(1, item[0], item[1]).asCartesian.asArray
					})
				);

				// list all of the output channels (speakers)
				// i.e., expand to actual pairs
				positions2 = positions ++ (positions.neg);

				// set output channel (speaker) directions for instance
				dirChannels = (positions2.asArray).collect({ |item|
					item.asCartesian.asSpherical.angles
				});

				// initialise k
				k = this.initK3D(k)
			}
		);


		// get velocity gains
		// NOTE: this comment from Heller seems to be slightly
		//       misleading, in that the gains returned will be
		//       scaled by k, which may not request a velocity
		//       gain. I.e., k = 1 isn't necessarily true, as it
		//       is assigned as an argument to this function.
		speakerMatrix = FoaSpeakerMatrix.newPositions(positions2, k).matrix;

		// n = number of output channels (speakers)
		n = speakerMatrix.cols;

		// build decoder matrix
		// resulting rows (after flop) are W, X, Y, Z gains
		matrix = speakerMatrix.insertRow(0, Array.fill(n, { 1 }));

		// return resulting matrix
		// ALSO: the below code calls for the complex conjugate
		//       of decoder_matrix. As we are expecting real vaules,
		//       we may regard this call as redundant.
		// res = sqrt(2) / n * decoder_matrix.conj().transpose()
		matrix = 2.sqrt / n * matrix.flop;
	}

	initPanto { |numChans, orientation, k|
		var g0, g1, theta;

		g0 = 1.0;     // decoder gains
		g1 = 2.sqrt;  // 0, 1st order


		// return theta from output channel (speaker) number
		theta = numChans.collect({ |channel|
			switch(orientation,
				\flat, { ((1.0 + (2.0 * channel)) / numChans) * pi },
				\point, { ((2.0 * channel) / numChans) * pi }
			)
		});
		theta = (theta + pi).mod(2pi) - pi;

		// set output channel (speaker) directions for instance
		dirChannels = theta;

		// initialise k
		k = this.initK2D(k);


		// build decoder matrix
		matrix = Matrix.newClear(numChans, 3); // start w/ empty matrix

		numChans.do({ |i|
			matrix.putRow(i, [
				g0,
				k * g1 * theta[i].cos,
				k * g1 * theta[i].sin
			])
		});
		matrix = 2.sqrt / numChans * matrix
	}

	initPeri { |numChanPairs, elevation, orientation, k|
		var theta, directions, upDirs, downDirs, upMatrix, downMatrix;

		// generate output channel (speaker) pair positions
		// start with polar positions. . .
		theta = [];
		numChanPairs.do({ |i|
			theta = theta ++ [2 * pi * i / numChanPairs]
		});

		if(orientation == \flat, {
			theta = theta + (pi / numChanPairs)  // 'flat' case
		});

		// collect directions [[theta, phi], ...]
		// upper ring only
		directions = [
			theta,
			Array.newClear(numChanPairs).fill(elevation)
		].flop;


		// prepare output channel (speaker) directions for instance
		upDirs = (directions + pi).mod(2pi) - pi;

		downDirs = upDirs.collect({ |angles|
			Spherical.new(1, angles[0], angles[1]).neg.angles
		});

		// initialise k
		k = this.initK3D(k);


		// build decoder matrix
		matrix = FoaDecoderMatrix.newDiametric(directions, k).matrix;

		// reorder the lower polygon
		upMatrix = matrix[..(numChanPairs - 1)];
		downMatrix = matrix[(numChanPairs)..];

		if(((orientation == \flat) and: { numChanPairs.mod(2) == 1 }), {
			// odd, 'flat'
			downDirs = downDirs.rotate((numChanPairs / 2 + 1).asInteger);
			downMatrix = downMatrix.rotate((numChanPairs / 2 + 1).asInteger)
		}, {
			// 'flat' case, default
			downDirs = downDirs.rotate((numChanPairs / 2).asInteger);
			downMatrix = downMatrix.rotate((numChanPairs / 2).asInteger)
		});

		dirChannels = upDirs ++ downDirs;  // set output channel (speaker) directions
		matrix = upMatrix ++ downMatrix;  // set matrix

	}

	initQuad { |angle, k|
		var g0, g1, g2;

		// set output channel (speaker) directions for instance
		dirChannels = [angle, pi - angle, (pi - angle).neg, angle.neg];


		// initialise k
		k = this.initK2D(k);

		// calculate g1, g2 (scaled by k)
		g0	= 1;
		g1	= k / (2.sqrt * angle.cos);
		g2	= k / (2.sqrt * angle.sin);

		// build decoder matrix
		matrix = 2.sqrt / 4 * Matrix.with([
				[g0, g1, 	g2 		],
				[g0, g1.neg, g2 		],
				[g0, g1.neg, g2.neg	],
				[g0, g1, 	g2.neg	]
		])
	}

	initStereo { |angle, pattern|
		var g0, g1, g2;

		// set output channel (speaker) directions for instance
		dirChannels = [pi/6, (pi/6).neg];

		// calculate g0, g1, g2 (scaled by pattern)
		g0	= (1.0 - pattern) * 2.sqrt;
		g1	= pattern * angle.cos;
		g2	= pattern * angle.sin;

		// build decoder matrix, and set for instance
		matrix = Matrix.with([
				[g0, g1, g2		],
				[g0, g1, g2.neg	]
		])
	}

	initMono { |theta, phi, pattern|

		// set output channel (speaker) directions for instance
		dirChannels = [0];

		// build decoder matrix, and set for instance
		matrix = Matrix.with([
				[
					(1.0 - pattern) * 2.sqrt,
					pattern * theta.cos * phi.cos,
					pattern * theta.sin * phi.cos,
					pattern * phi.sin
				]
		])
	}

	initDecoderVarsForFiles {
		if(fileParse.notNil, {
			// TODO: check use of dirOutputs vs. dirChannels here
			dirChannels = if(fileParse.dirOutputs.notNil, {
				fileParse.dirOutputs.asFloat
			}, { // output directions are unspecified in the provided matrix
				(this.matrix.rows).collect({ \unspecified })
			});
			shelfK = fileParse.shelfK !? { fileParse.shelfK.asFloat };
			shelfFreq = fileParse.shelfFreq !? { fileParse.shelfFreq.asFloat };
		}, { // txt file provided, no fileParse
			dirChannels = (this.matrix.rows).collect({ \unspecified });
		});
	}

}


//-----------------------------------------------------------------------
// martrix encoders

FoaEncoderMatrix : FoaMatrix {

	*newAtoB { |orientation = \flu, weight = \dec|
		^super.new(\AtoB).loadFromLib(orientation, weight)
	}

	*newHoa1 { |ordering = \acn, normalisation = \n3d|
		^super.new(\hoa1).loadFromLib(ordering, normalisation);
	}

	*newAmbix1 {
		var ordering = \acn, normalisation = \sn3d;
		^super.new(\hoa1).loadFromLib(ordering, normalisation);
	}

	*newZoomH2n {
		var ordering = \acn, normalisation = \sn3d;
		^super.new(\hoa1).loadFromLib(ordering, normalisation);
	}

	*newOmni {
		^super.new(\omni).loadFromLib;
	}

	*newDirection { |theta = 0, phi = 0|
		^super.new(\dir).initDirection(theta, phi);
	}

	*newStereo { |angle = 0|
		^super.new(\stereo).initStereo(angle);
	}

	*newQuad {
		^super.new(\quad).loadFromLib;
	}

	*new5_0 {
		^super.new('5_0').loadFromLib;
	}

	*new7_0 {
		^super.new('7_0').loadFromLib;
	}

	*newDirections { |directions, pattern = nil|
		^super.new(\dirs).initDirections(directions, pattern);
	}

	*newPanto { |numChans = 4, orientation = \flat|
		^super.new(\panto).initPanto(numChans, orientation);
	}

	*newPeri { |numChanPairs = 4, elevation = 0.61547970867039, orientation = \flat|
		^super.new(\peri).initPeri(numChanPairs, elevation, orientation);
	}

	*newZoomH2 { |angles = ([pi / 3, 3 / 4 * pi]), pattern = 0.5857, k = 1|
		^super.new(\zoomH2).initZoomH2(angles, pattern, k);
	}

	*newFromFile { |filePathOrName|
		^super.new.initFromFile(filePathOrName, \encoder, true).initEncoderVarsForFiles
	}

	init2D {
		var g0 = 2.sqrt.reciprocal;

		// build encoder matrix, and set for instance
		matrix = Matrix.newClear(3, dirChannels.size); // start w/ empty matrix

		dirChannels.do({ |theta, i|
			matrix.putCol(i, [
				g0,
				theta.cos,
				theta.sin
			])
		})
	}

	init3D {
		var g0 = 2.sqrt.reciprocal;

		// build encoder matrix, and set for instance
		matrix = Matrix.newClear(4, dirChannels.size); // start w/ empty matrix

		dirChannels.do({ |thetaPhi, i|
			matrix.putCol(i, [
				g0,
				thetaPhi[1].cos * thetaPhi[0].cos,
				thetaPhi[1].cos * thetaPhi[0].sin,
				thetaPhi[1].sin
			])
		})
	}

	initInv2D { |pattern|
		var g0 = 2.sqrt.reciprocal;

		// build 'decoder' matrix, and set for instance
		matrix = Matrix.newClear(dirChannels.size, 3); // start w/ empty matrix

		if(pattern.isArray, {
			dirChannels.do({ |theta, i|  // mic positions, indivd patterns
				matrix.putRow(i, [
					(1.0 - pattern[i]),
					pattern[i] * theta.cos,
					pattern[i] * theta.sin
				])
			})
		}, {
			dirChannels.do({ |theta, i|  // mic positions
				matrix.putRow(i, [
					(1.0 - pattern),
					pattern * theta.cos,
					pattern * theta.sin
				])
			})
		});

		// invert to encoder matrix
		matrix = matrix.pseudoInverse;

		// normalise matrix
		matrix = matrix * matrix.getRow(0).sum.reciprocal;

		// scale W
		matrix = matrix.putRow(0, matrix.getRow(0) * g0);
	}

	initInv3D { |pattern|
		var g0 = 2.sqrt.reciprocal;

		// build 'decoder' matrix, and set for instance
		matrix = Matrix.newClear(dirChannels.size, 4);  // start w/ empty matrix

		if(pattern.isArray, {
			dirChannels.do({ |thetaPhi, i|  // mic positions, indivd patterns
				matrix.putRow(i, [
					(1.0 - pattern[i]),
					pattern[i] * thetaPhi[1].cos * thetaPhi[0].cos,
					pattern[i] * thetaPhi[1].cos * thetaPhi[0].sin,
					pattern[i] * thetaPhi[1].sin
				])
			})
		}, {
			dirChannels.do({ |thetaPhi, i|  // mic positions
				matrix.putRow(i, [
					(1.0 - pattern),
					pattern * thetaPhi[1].cos * thetaPhi[0].cos,
					pattern * thetaPhi[1].cos * thetaPhi[0].sin,
					pattern * thetaPhi[1].sin
				])
			})
		});

		// invert to encoder matrix
		matrix = matrix.pseudoInverse;

		// normalise matrix
		matrix = matrix * matrix.getRow(0).sum.reciprocal;

		// scale W
		matrix = matrix.putRow(0, matrix.getRow(0) * g0);
	}

	initDirection { |theta, phi|

		// set input channel directions for instance
		if(phi == 0, {
			dirChannels = [theta];
			this.init2D
		}, {
			dirChannels = [[theta, phi]];
			this.init3D
		})
	}

	initStereo { |angle|

		// set input channel directions for instance
		dirChannels = [pi/2 - angle, (pi/2 - angle).neg];

		this.init2D
	}

	initDirections { |directions, pattern|

		// set input channel directions for instance
		dirChannels = directions;

		switch(directions.rank,					// 2D or 3D?
			1, {								// 2D
				if(pattern.isNil, {
					this.init2D					// plane wave
				}, {
					this.initInv2D(pattern)		// mic inversion
				})
			},
			2, {								// 3D
				if(pattern.isNil, {
					this.init3D					// plane wave
				}, {
					this.initInv3D(pattern)		// mic inversion
				})
			}
		)
	}

	initPanto { |numChans, orientation|
		var theta;

		// return theta from output channel (speaker) number
		theta = numChans.collect({ |channel|
			switch(orientation,
				\flat, { ((1.0 + (2.0 * channel)) / numChans) * pi },
				\point, { ((2.0 * channel) / numChans) * pi }
			)
		});
		theta = (theta + pi).mod(2pi) - pi;

		// set input channel directions for instance
		dirChannels = theta;

		this.init2D
	}

	initPeri { |numChanPairs, elevation, orientation|
		var theta, directions, upDirs, downDirs, upMatrix, downMatrix;

		// generate input channel pair positions
		// start with polar positions. . .
		theta = [];
		numChanPairs.do({ |i|
			theta = theta ++ [2 * pi * i / numChanPairs]
		});
		if(orientation == \flat, {
			theta = theta + (pi / numChanPairs)
		});       // 'flat' case

		// collect directions [[theta, phi], ...]
		// upper ring only
		directions = [
			theta,
			Array.newClear(numChanPairs).fill(elevation)
		].flop;


		// prepare output channel (speaker) directions for instance
		upDirs = (directions + pi).mod(2pi) - pi;

		downDirs = upDirs.collect({ |angles|
			Spherical.new(1, angles[0], angles[1]).neg.angles
		});

		// reorder the lower polygon
		if((orientation == \flat and: { numChanPairs.mod(2) == 1 }), {
			// odd, 'flat'
			downDirs = downDirs.rotate((numChanPairs / 2 + 1).asInteger)
		}, {
			// 'flat' case, default
			downDirs = downDirs.rotate((numChanPairs / 2).asInteger)
		});

		// set input channel directions for instance
		dirChannels = upDirs ++ downDirs;

		this.init3D
	}

	initZoomH2 { |angles, pattern, k|

		// set input channel directions for instance
		dirChannels = [angles[0], angles[0].neg, angles[1], angles[1].neg];

		this.initInv2D(pattern);

		matrix = matrix.putRow(2, matrix.getRow(2) * k); // scale Y
	}

	initEncoderVarsForFiles {
		dirChannels = if(fileParse.notNil, {
			// TODO: check use of dirInputs vs. dirChannels here
			if(fileParse.dirInputs.notNil, {
				fileParse.dirInputs.asFloat
			}, { // so input directions are unspecified in the provided matrix
				(this.matrix.cols).collect({ \unspecified })
			})
		}, { // txt file provided, no fileParse
			(this.matrix.cols).collect({ \unspecified })
		})
	}

}


//-----------------------------------------------------------------------
// martrix transforms


FoaXformerMatrix : FoaMatrix {

	// ------------
	// From matrix

	// overload FoaMatrix *newFromMatrix
	*newFromMatrix { |matrix|
		^super.new(\fromMatrix).initFromMatrix(matrix, nil)
	}

	// ~~
	// Note: the 'kind' of the mirror transforms will be
	// superceded by the kind specified in the .yml file
	// e.g. 'mirrorX'
	*newMirrorX {
		^super.new(\mirrorAxis).loadFromLib(\x);
	}

	*newMirrorY {
		^super.new(\mirrorAxis).loadFromLib(\y);
	}

	*newMirrorZ {
		^super.new(\mirrorAxis).loadFromLib(\z);
	}

	*newMirrorO {
		^super.new(\mirrorAxis).loadFromLib(\o);
	}
	//~~~

	*newRotate { |angle = 0|
		^super.new(\rotate).initRotate(angle);
	}

	*newTilt { |angle = 0|
		^super.new(\tilt).initTilt(angle);
	}

	*newTumble { |angle = 0|
		^super.new(\tumble).initTumble(angle);
	}

	*newDirectO { |angle = 0|
		^super.new(\directO).initDirectO(angle);
	}

	*newDirectX { |angle = 0|
		^super.new(\directX).initDirectX(angle);
	}

	*newDirectY { |angle = 0|
		^super.new(\directY).initDirectY(angle);
	}

	*newDirectZ { |angle = 0|
		^super.new(\directZ).initDirectZ(angle);
	}

	*newDominateX { |gain = 0|
		^super.new(\dominateX).initDominateX(gain);
	}

	*newDominateY { |gain = 0|
		^super.new(\dominateY).initDominateY(gain);
	}

	*newDominateZ { |gain = 0|
		^super.new(\dominateZ).initDominateZ(gain);
	}

	*newZoomX { |angle = 0|
		^super.new(\zoomX).initZoomX(angle);
	}

	*newZoomY { |angle = 0|
		^super.new(\zoomY).initZoomY(angle);
	}

	*newZoomZ { |angle = 0|
		^super.new(\zoomZ).initZoomZ(angle);
	}

	*newFocusX { |angle = 0|
		^super.new(\focusX).initFocusX(angle);
	}

	*newFocusY { |angle = 0|
		^super.new(\focusY).initFocusY(angle);
	}

	*newFocusZ { |angle = 0|
		^super.new(\focusZ).initFocusZ(angle);
	}

	*newPushX { |angle = 0|
		^super.new(\pushX).initPushX(angle);
	}

	*newPushY { |angle = 0|
		^super.new(\pushY).initPushY(angle);
	}

	*newPushZ { |angle = 0|
		^super.new(\pushZ).initPushZ(angle);
	}

	*newPressX { |angle = 0|
		^super.new(\pressX).initPressX(angle);
	}

	*newPressY { |angle = 0|
		^super.new(\pressY).initPressY(angle);
	}

	*newPressZ { |angle = 0|
		^super.new(\pressZ).initPressZ(angle);
	}

	*newAsymmetry { |angle = 0|
		^super.new(\asymmetry).initAsymmetry(angle);
	}

	*newBalance { |angle = 0|
		^super.new(\zoomY).initZoomY(angle);
	}

	*newRTT { |rotAngle = 0, tilAngle = 0, tumAngle = 0|
		^super.new(\rtt).initRTT(rotAngle, tilAngle, tumAngle);
	}

	*newMirror { |theta = 0, phi = 0|
		^super.new(\mirror).initMirror(theta, phi);
	}

	*newDirect { |angle = 0, theta = 0, phi = 0|
		^super.new(\direct).initDirect(angle, theta, phi);
	}

	*newDominate { |gain = 0, theta = 0, phi = 0|
		^super.new(\dominate).initDominate(gain, theta, phi);
	}

	*newZoom { |angle = 0, theta = 0, phi = 0|
		^super.new(\zoom).initZoom(angle, theta, phi);
	}

	*newFocus { |angle = 0, theta = 0, phi = 0|
		^super.new(\focus).initFocus(angle, theta, phi);
	}

	*newPush { |angle = 0, theta = 0, phi = 0|
		^super.new(\push).initPush(angle, theta, phi);
	}

	*newPress { |angle = 0, theta = 0, phi = 0|
		^super.new(\press).initPress(angle, theta, phi);
	}

	*newFromFile { |filePathOrName|
		^super.new.initFromFile(filePathOrName, \xformer, true);
	}

	initRotate { |angle|
		var cosAngle, sinAngle;

		// build transform matrix, and set for instance
		// calculate cos, sin
		cosAngle	= angle.cos;
		sinAngle	= angle.sin;

		matrix = Matrix.with([
			[ 1, 0, 			0,			0 ],
			[ 0, cosAngle,	sinAngle.neg,	0 ],
			[ 0, sinAngle, 	cosAngle,		0 ],
			[ 0, 0, 			0,			1 ]
		])
	}

	initTilt { |angle|
		var cosAngle, sinAngle;

		// build transform matrix, and set for instance
		// calculate cos, sin
		cosAngle	= angle.cos;
		sinAngle	= angle.sin;

		matrix = Matrix.with([
			[ 1, 0, 0,		0 			],
			[ 0, 1, 0,		0 			],
			[ 0,	0, cosAngle,	sinAngle.neg 	],
			[ 0,	0, sinAngle, 	cosAngle 		]
		])
	}

	initTumble { |angle|
		var cosAngle, sinAngle;

		// build transform matrix, and set for instance
		// calculate cos, sin
		cosAngle	= angle.cos;
		sinAngle	= angle.sin;

		matrix = Matrix.with([
			[ 1, 0, 			0,	0 			],
			[ 0, cosAngle,	0,	sinAngle.neg	],
			[ 0, 0,			1, 	0 			],
			[ 0, sinAngle,	0, 	cosAngle 		]
		])
	}

	initDirectO { |angle|
		var g0, g1;

		// build transform matrix, and set for instance
		g0 = (1 + angle.sin).sqrt;
		g1 = (1 - angle.sin).sqrt;

		matrix = Matrix.with([
			[ g0,	0,	0,	0 	],
			[ 0, 	g1,	0,	0	],
			[ 0, 	0,	g1, 	0 	],
			[ 0, 	0,	0, 	g1 	]
		])
	}

	initDirectX { |angle|
		var g0, g1;

		// build transform matrix, and set for instance
		g0 = (1 + angle.sin).sqrt;
		g1 = (1 - angle.sin).sqrt;

		matrix = Matrix.with([
			[ g0,	0,	0,	0 	],
			[ 0, 	g1,	0,	0	],
			[ 0, 	0,	g0, 	0 	],
			[ 0, 	0,	0, 	g0 	]
		])
	}

	initDirectY { |angle|
		var g0, g1;

		// build transform matrix, and set for instance
		g0 = (1 + angle.sin).sqrt;
		g1 = (1 - angle.sin).sqrt;

		matrix = Matrix.with([
			[ g0,	0,	0,	0 	],
			[ 0, 	g0,	0,	0	],
			[ 0, 	0,	g1, 	0 	],
			[ 0, 	0,	0, 	g0 	]
		])
	}

	initDirectZ { |angle|
		var g0, g1;

		// build transform matrix, and set for instance
		g0 = (1 + angle.sin).sqrt;
		g1 = (1 - angle.sin).sqrt;

		matrix = Matrix.with([
			[ g0,	0,	0,	0 	],
			[ 0, 	g0,	0,	0	],
			[ 0, 	0,	g0, 	0 	],
			[ 0, 	0,	0, 	g1 	]
		])
	}

	initDominateX { |gain|
		var g0, g1, k;

		// build transform matrix, and set for instance
		k = gain.dbamp;

		g0 = (k + k.reciprocal) / 2;
		g1 = (k - k.reciprocal) / 2.sqrt;

		matrix = Matrix.with([
			[ g0,	g1 / 2,	0,	0 	],
			[ g1, 	g0,		0,	0	],
			[ 0, 	0,		1, 	0 	],
			[ 0, 	0,		0, 	1 	]
		])
	}

	initDominateY { |gain|
		var g0, g1, k;

		// build transform matrix, and set for instance
		k = gain.dbamp;

		g0 = (k + k.reciprocal) / 2;
		g1 = (k - k.reciprocal) / 2.sqrt;

		matrix = Matrix.with([
			[ g0,	0,	g1 / 2,	0 	],
			[ 0, 	1,	0, 		0 	],
			[ g1, 	0,	g0,		0	],
			[ 0, 	0,	0, 		1 	]
		])
	}

	initDominateZ { |gain|
		var g0, g1, k;

		// build transform matrix, and set for instance
		k = gain.dbamp;

		g0 = (k + k.reciprocal) / 2;
		g1 = (k - k.reciprocal) / 2.sqrt;

		matrix = Matrix.with([
			[ g0,	0,	0,	g1 / 2	],
			[ 0, 	1,	0, 	0 	],
			[ 0, 	0,	1, 	0 	],
			[ g1, 	0,	0,	g0	]
		])
	}

	initZoomX { |angle|
		var g0, g1;

		// build transform matrix, and set for instance
		g0 = angle.sin;
		g1 = angle.cos;

		matrix = Matrix.with([
			[ 1,			g0 / 2.sqrt,	0,	0 	],
			[ 2.sqrt * g0, 	1,			0,	0	],
			[ 0, 		0,			g1, 	0 	],
			[ 0, 		0,			0, 	g1 	]
		])
	}

	initZoomY { |angle|
		var g0, g1;

		// build transform matrix, and set for instance
		g0 = angle.sin;
		g1 = angle.cos;

		matrix = Matrix.with([
			[ 1,			0,	g0 / 2.sqrt,	0 	],
			[ 0, 		g1,	0, 			0 	],
			[ 2.sqrt * g0, 	0,	1,			0	],
			[ 0, 		0,	0, 			g1 	]
		])
	}

	initZoomZ { |angle|
		var g0, g1;

		// build transform matrix, and set for instance
		g0 = angle.sin;
		g1 = angle.cos;

		matrix = Matrix.with([
			[ 1,			0,	0,	g0 / 2.sqrt	],
			[ 0, 		g1,	0, 	0 		],
			[ 0, 		0, 	g1,	0 		],
			[ 2.sqrt * g0, 	0,	0,	1		]
		])
	}

	initFocusX { |angle|
		var g0, g1, g2;

		// build transform matrix, and set for instance
		g0 = (1 + angle.abs.sin).reciprocal;
		g1 = 2.sqrt * angle.sin * g0;
		g2 = angle.cos * g0;

		matrix = Matrix.with([
			[ g0,	g1 / 2,	0,	0	],
			[ g1,	g0,		0,	0	],
			[ 0,		0,		g2, 	0 	],
			[ 0,		0,		0, 	g2	]
		])
	}

	initFocusY { |angle|
		var g0, g1, g2;

		// build transform matrix, and set for instance
		g0 = (1 + angle.abs.sin).reciprocal;
		g1 = 2.sqrt * angle.sin * g0;
		g2 = angle.cos * g0;

		matrix = Matrix.with([
			[ g0,	0,	g1 / 2,	0	],
			[ 0,		g2,	0, 		0 	],
			[ g1,	0,	g0,		0	],
			[ 0,		0,	0, 		g2	]
		])
	}

	initFocusZ { |angle|
		var g0, g1, g2;

		// build transform matrix, and set for instance
		g0 = (1 + angle.abs.sin).reciprocal;
		g1 = 2.sqrt * angle.sin * g0;
		g2 = angle.cos * g0;

		matrix = Matrix.with([
			[ g0,	0,	0,	g1 / 2	],
			[ 0,		g2,	0, 	0 	],
			[ 0,		0, 	g2,	0	],
			[ g1,	0,	0,	g0	]
		])
	}

	initPushX { |angle|
		var g0, g1;

		// build transform matrix, and set for instance
		g0 = 2.sqrt * angle.sin * angle.abs.sin;
		g1 = angle.cos.squared;

		matrix = Matrix.with([
			[ 1,		0,	0,	0	],
			[ g0,	g1,	0,	0	],
			[ 0,		0,	g1, 	0 	],
			[ 0,		0,	0, 	g1	]
		])
	}

	initPushY { |angle|
		var g0, g1;

		// build transform matrix, and set for instance
		g0 = 2.sqrt * angle.sin * angle.abs.sin;
		g1 = angle.cos.squared;

		matrix = Matrix.with([
			[ 1,		0,	0,	0	],
			[ 0,		g1,	0, 	0 	],
			[ g0,	0,	g1,	0	],
			[ 0,		0,	0, 	g1	]
		])
	}

	initPushZ { |angle|
		var g0, g1;

		// build transform matrix, and set for instance
		g0 = 2.sqrt * angle.sin * angle.abs.sin;
		g1 = angle.cos.squared;

		matrix = Matrix.with([
			[ 1,		0,	0,	0	],
			[ 0,		g1,	0, 	0 	],
			[ 0,		0, 	g1,	0	],
			[ g0,	0,	0,	g1	]
		])
	}

	initPressX { |angle|
		var g0, g1, g2;

		// build transform matrix, and set for instance
		g0 = 2.sqrt * angle.sin * angle.abs.sin;
		g1 = angle.cos.squared;
		g2 = angle.cos;

		matrix = Matrix.with([
			[ 1,		0,	0,	0	],
			[ g0,	g1,	0,	0	],
			[ 0,		0,	g2, 	0 	],
			[ 0,		0,	0, 	g2	]
		])
	}

	initPressY { |angle|
		var g0, g1, g2;

		// build transform matrix, and set for instance
		g0 = 2.sqrt * angle.sin * angle.abs.sin;
		g1 = angle.cos.squared;
		g2 = angle.cos;

		matrix = Matrix.with([
			[ 1,		0,	0,	0	],
			[ 0,		g2,	0, 	0 	],
			[ g0,	0,	g1,	0	],
			[ 0,		0,	0, 	g2	]
		])
	}

	initPressZ { |angle|
		var g0, g1, g2;

		// build transform matrix, and set for instance
		g0 = 2.sqrt * angle.sin * angle.abs.sin;
		g1 = angle.cos.squared;
		g2 = angle.cos;

		matrix = Matrix.with([
			[ 1,		0,	0,	0	],
			[ 0,		g2,	0, 	0 	],
			[ 0,		0, 	g2,	0	],
			[ g0,	0,	0,	g1	]
		])
	}

	initAsymmetry { |angle|
		var g0, g1, g2, g3, g4;

		// build transform matrix, and set for instance
		g0 = angle.sin.neg;
		g1 = angle.sin.squared;
		g2 = angle.cos.squared;
		g3 = angle.cos * angle.sin;
		g4 = angle.cos;

		matrix = Matrix.with([
			[ 1,			    0, 2.sqrt.reciprocal * g0, 0 ],
			[ 2.sqrt * g1,	   g2, g0,				 0 ],
			[ 2.sqrt.neg * g3, g3, g4, 				 0 ],
			[ 0,			   0,  0, 				g4 ]
		])
	}

	initRTT { |rotAngle, tilAngle, tumAngle|

		matrix = (
			FoaXformerMatrix.newTumble(tumAngle).matrix *
			FoaXformerMatrix.newTilt(tilAngle).matrix *
			FoaXformerMatrix.newRotate(rotAngle).matrix
		)
	}

	initMirror { |theta, phi|

		matrix = (
			FoaXformerMatrix.newRotate(theta).matrix *
			FoaXformerMatrix.newTumble(phi).matrix *
			FoaXformerMatrix.newMirrorX.matrix *
			FoaXformerMatrix.newTumble(phi.neg).matrix *
			FoaXformerMatrix.newRotate(theta.neg).matrix
		)
	}

	initDirect { |angle, theta, phi|

		matrix = (
			FoaXformerMatrix.newRotate(theta).matrix *
			FoaXformerMatrix.newTumble(phi).matrix *
			FoaXformerMatrix.newDirectX(angle).matrix *
			FoaXformerMatrix.newTumble(phi.neg).matrix *
			FoaXformerMatrix.newRotate(theta.neg).matrix
		)
	}

	initDominate { |gain, theta, phi|

		matrix = (
			FoaXformerMatrix.newRotate(theta).matrix *
			FoaXformerMatrix.newTumble(phi).matrix *
			FoaXformerMatrix.newDominateX(gain).matrix *
			FoaXformerMatrix.newTumble(phi.neg).matrix *
			FoaXformerMatrix.newRotate(theta.neg).matrix
		)
	}

	initZoom { |angle, theta, phi|

		matrix = (
			FoaXformerMatrix.newRotate(theta).matrix *
			FoaXformerMatrix.newTumble(phi).matrix *
			FoaXformerMatrix.newZoomX(angle).matrix *
			FoaXformerMatrix.newTumble(phi.neg).matrix *
			FoaXformerMatrix.newRotate(theta.neg).matrix
		)
	}

	initFocus { |angle, theta, phi|

		matrix = (
			FoaXformerMatrix.newRotate(theta).matrix *
			FoaXformerMatrix.newTumble(phi).matrix *
			FoaXformerMatrix.newFocusX(angle).matrix *
			FoaXformerMatrix.newTumble(phi.neg).matrix *
			FoaXformerMatrix.newRotate(theta.neg).matrix
		)
	}

	initPush { |angle, theta, phi|

		matrix = (
			FoaXformerMatrix.newRotate(theta).matrix *
			FoaXformerMatrix.newTumble(phi).matrix *
			FoaXformerMatrix.newPushX(angle).matrix *
			FoaXformerMatrix.newTumble(phi.neg).matrix *
			FoaXformerMatrix.newRotate(theta.neg).matrix
		)
	}

	initPress { |angle, theta, phi|

		matrix = (
			FoaXformerMatrix.newRotate(theta).matrix *
			FoaXformerMatrix.newTumble(phi).matrix *
			FoaXformerMatrix.newPressX(angle).matrix *
			FoaXformerMatrix.newTumble(phi.neg).matrix *
			FoaXformerMatrix.newRotate(theta.neg).matrix
		)
	}

	// overload instance var dirChannels
	dirChannels { ^(this.numOutputs).collect({ inf }) }

}



//------------------------------------------------------------------------
// kernel decoders

FoaDecoderKernel {
	var <kind, <subjectID;
	var <kernel, kernelBundle, kernelInfo;
	var <dirChannels;
	var <op = \kernel;
	var <set = \FOA;


	// *newSpherical { |subjectID = 0004, kernelSize = 512, server = Server.default|
	// 	^super.newCopyArgs(\spherical, subjectID).initKernel(kernelSize, server);
	// }
	//
	// *newListen { |subjectID = 1002, server = Server.default|
	// 	^super.newCopyArgs(\listen, subjectID).initKernel(512, server);
	// }
	//
	// *newCIPIC { |subjectID = 0021, server = Server.default|
	// 	^super.newCopyArgs(\cipic, subjectID).initKernel(256, server);
	// }

	*newSpherical { |subjectID = 0004, server = (Server.default), sampleRate, score|
		^super.newCopyArgs(\spherical, subjectID).initKernel(nil, server, sampleRate, score);
	}

	*newListen { |subjectID = 1002, server = (Server.default), sampleRate, score|
		^super.newCopyArgs(\listen, subjectID).initKernel(nil, server, sampleRate, score);
	}

	*newCIPIC { |subjectID = 0021, server = (Server.default), sampleRate, score|
		^super.newCopyArgs(\cipic, subjectID).initKernel(nil, server, sampleRate, score);
	}

	*newUHJ { |kernelSize = 512, server = (Server.default), sampleRate, score|
		^super.newCopyArgs(\uhj, 0).initKernel(kernelSize, server, sampleRate, score);
	}

	initPath {
		var kernelLibPath;
		var decodersPath;

		kernelLibPath = PathName.new(
			Atk.userKernelDir
		);

		if(kernelLibPath.isFolder.not, {	// is kernel lib installed for all users?
			PathName.new(					// no? set for single user
				Atk.systemKernelDir
			)
		});

		decodersPath = PathName.new("/FOA/decoders");

		^kernelLibPath +/+ decodersPath +/+ PathName.new(this.kind.asString)
	}

	initKernel { |kernelSize, server, sampleRate, score|
		var databasePath, subjectPath;
		var chans;
		var errorMsg;
		var sampleRateStr;

		if(sampleRate.notNil, {
			sampleRateStr = sampleRate.asInteger.asString
		});

		if((server.serverRunning.not and: { sampleRateStr.isNil and: { score.isNil } }), {
			Error(
				"Please boot server: %, or provide a CtkScore or Score.".format(
					server.name.asString
				)
			).throw
		});

		kernelBundle = [0.0];
		kernelInfo = [];

		// constants
		chans = 2;			// stereo kernel

		// init dirChannels (output channel (speaker) directions) and kernel sr
		if(this.kind == \uhj, {
			dirChannels = [pi/6, (pi/6).neg];
			sampleRateStr = "None"
		}, {
			dirChannels = [5/9 * pi, 5/9 * pi.neg];
			if(sampleRateStr.isNil, {
				sampleRateStr = server.sampleRate.asInteger.asString
			})
		});

		// init kernelSize if need be (usually for HRIRs)
		if(kernelSize == nil, {
			kernelSize = switch(sampleRateStr.asSymbol,
				\None, 512,
				\44100, 512,
				\48000, 512,
				\88200, 1024,
				\96000, 1024,
				\176400, 2048,
				\192000, 2048
			)
		});


		// init kernel root, generate subjectPath and kernelFiles
		databasePath = this.initPath;

		subjectPath = databasePath +/+ PathName.new(
			sampleRateStr ++ "/" ++
			kernelSize ++ "/" ++
			subjectID.asString.padLeft(4, "0")
		);


		// attempt to load kernel
		if(subjectPath.isFolder.not, {	// does kernel path exist?

			case(
				// --> missing kernel database
				{ databasePath.isFolder.not }, {
					errorMsg = "Atk: kernels don't appear to be installed\nRun 'Atk.downloadKernels' to attempt automatic installation"
				},

				// --> unsupported SR
				{ PathName.new(subjectPath.parentLevelPath(2)).isFolder.not }, {
					"Supported samplerates:".warn;
					(PathName.new(subjectPath.parentLevelPath(3)).folders).do({
						|folder|
						("\t" + folder.folderName).postln
					});

					errorMsg = "Samplerate = % is not available for".format(sampleRateStr)
					+
								"% kernel decoder.".format(this.kind)
				},

				// --> unsupported kernelSize
				{ PathName.new(subjectPath.parentLevelPath(1)).isFolder.not }, {
					"Supported kernel sizes:".warn;
					(PathName.new(subjectPath.parentLevelPath(2)).folders).do({
						|folder|
						("\t" + folder.folderName).postln
					});

					errorMsg = "Kernel size = % is not available for".format(kernelSize)
					+
							"% kernel decoder.".format(this.kind)
				},

				// --> unsupported subject
				{ subjectPath.isFolder.not }, {
					"Supported subjects:".warn;
					(PathName.new(subjectPath.parentLevelPath(1)).folders).do({
						|folder|
						("\t" + folder.folderName).postln
					});

					errorMsg = "Subject % is not available for".format(subjectID)
					+
							"% kernel decoder.".format(this.kind)
				}
			);

			// throw error!
			"\n".post;
			Error(errorMsg).throw
		}, {
			if(score.isNil, {
				if(server.serverRunning.not, {		// is server running?
					// throw server error!
					Error(
						"Please boot server: %. Encoder kernel failed to load.".format(
							server.name.asString
						)
					).throw
				}, {
					// Else... everything is fine! Load kernel.
					kernel = (subjectPath.files).collect({ |kernelPath|
						chans.collect({ |chan|
							Buffer.readChannel(server, kernelPath.fullPath, channels: [chan],
								action: { |buf|
									(
										kernelBundle = kernelBundle.add(
							["/b_allocReadChannel", buf.bufnum, kernelPath.fullPath, 0, kernelSize, chan]
						);
										kernelInfo = kernelInfo.add([kernelPath.fullPath, buf.bufnum, [chan]]);
										"Kernel %, channel % loaded.".format(
											kernelPath.fileName, chan
										)
									).postln
								}
							)
						})
					})
				})
			});

			if((\CtkScore.asClass.notNil and: { score.isKindOf(\CtkScore.asClass) }), {
				kernel = (subjectPath.files).collect({ |kernelPath|
					chans.collect({ |chan|
						var buf = CtkBuffer(kernelPath.fullPath, channels: [chan]);

						kernelInfo = kernelInfo.add([kernelPath.fullPath, buf.bufnum, [chan]]);
						score.add(buf);
						buf
					})
				})
			});

			if(score.isKindOf(Score), {
				kernel = (subjectPath.files).collect({ |kernelPath|
					chans.collect({ |chan|
						var buf;

						buf = Buffer(server, kernelSize);
						kernelBundle = kernelBundle.add(
							["/b_allocReadChannel", buf.bufnum, kernelPath.fullPath, 0, kernelSize, chan]
						);
						kernelInfo = kernelInfo.add([kernelPath.fullPath, buf.bufnum, [chan]]);
						buf
					})
				});
				score.add(kernelBundle)
			});

			if((kernel.isNil and: { score.notNil }), {
				Error(
					"Score is not a Score or a CtkScore. Score is a %.".format(
						score.class.asString
					)
				).throw
			})
		})
	}

	free {
		var path;

		(kernel.shape[0]).do({ |i|
			(kernel.shape[1]).do({ |j|
				path = kernel[i][j].path;
				kernel[i][j].free;
				if(path.notNil, {
					"Decoder kernel %, channel % freed.\n".postf(
						PathName.new(path).fileName, j
					)
				});
			})
		})
	}

	buffers { ^kernel.flat }

	kernelInfo { ^kernelInfo }

	kernelBundle { ^kernelBundle }

	dim { ^kernel.shape[0] - 1 }

	numChannels { ^kernel.shape[1] }

	kernelSize { ^kernel[0][0].numFrames }

	numOutputs { ^kernel.shape[1] }

	dirOutputs { ^dirChannels }

	numInputs { ^kernel.shape[0] }

	dirInputs { ^(this.numInputs).collect({ inf }) }

	directions { ^dirChannels }

	type { ^\decoder }

	printOn { |stream|
		stream << this.class.name << "(" <<*
			[this.kind, this.dim, this.numChannels, subjectID, this.kernelSize] <<")";
	}
}


//------------------------------------------------------------------------
// kernel encoders

FoaEncoderKernel {
	var <kind, <subjectID;
	var <kernel, kernelBundle, kernelInfo;
	var <dirChannels;
	var <op = \kernel;
	var <set = \FOA;

	*newUHJ { |kernelSize = nil, server = (Server.default), sampleRate, score|
		^super.newCopyArgs(\uhj, 0).initKernel(kernelSize, server, sampleRate, score);
	}

	*newSuper { |kernelSize = nil, server = (Server.default), sampleRate, score|
		^super.newCopyArgs(\super, 0).initKernel(kernelSize, server, sampleRate, score);
	}

	*newSpread { |subjectID = 0006, kernelSize = 2048, server = (Server.default), sampleRate, score|
		^super.newCopyArgs(\spread, subjectID).initKernel(kernelSize, server, sampleRate, score);
	}

	*newDiffuse { |subjectID = 0003, kernelSize = 2048, server = (Server.default), sampleRate, score|
		^super.newCopyArgs(\diffuse, subjectID).initKernel(kernelSize, server, sampleRate, score);
	}

	// Encoding via Isophonics Room Impulse Response Data Set, not yet implemented.
	// (http://isophonics.net/content/room-impulse-response-data-set)
	//
	// NOTE: Convolution2 doesn't support large, arbitrary sized kernels.

//	*newGreatHall { |subjectID = "x06y02", server = Server.default|
//		^super.newCopyArgs(\greathall, subjectID).initKernel("None", server);
//	}
//
//	*newOctagon { |subjectID = "x06y02", server = Server.default|
//		^super.newCopyArgs(\octagon, subjectID).initKernel("None", server);
//	}
//
//	*newClassroom { |subjectID = "x30y10", server = Server.default|
//		^super.newCopyArgs(\classroom, subjectID).initKernel("None", server);
//	}

	initPath {
		var kernelLibPath;
		var encodersPath;

		kernelLibPath = PathName.new(Atk.userKernelDir);

		if(kernelLibPath.isFolder.not, {		// is kernel lib installed for all users?
			PathName.new(Atk.systemKernelDir)	// no? set for single user
		});

		encodersPath = PathName.new("/FOA/encoders");

		^kernelLibPath +/+ encodersPath +/+ PathName.new(this.kind.asString)
	}

	initKernel { |kernelSize, server, sampleRate, score|
		var databasePath, subjectPath;
		var chans;
		var errorMsg;
		var sampleRateStr;

		if(sampleRate.notNil, {
			sampleRateStr = sampleRate.asInteger.asString
		});

		if((server.serverRunning.not and: { sampleRateStr.isNil and: { score.isNil } }), {
			Error(
				"Please boot server: %, or provide a CtkScore or Score.".format(
					server.name.asString
				)
			).throw
		});

		kernelBundle = [0.0];
		kernelInfo = [];

		// init dirChannels (output channel (speaker) directions) and kernel sr
		switch(this.kind,
			\super, {
				dirChannels = [pi/4, (pi/4).neg];	 // approx, doesn't include phasiness
				sampleRateStr = "None";
				chans = 3					// [w, x, y]
			},
			\uhj, {
				dirChannels = [inf, inf];
				if(sampleRateStr.isNil, {
					sampleRateStr = server.sampleRate.asInteger.asString
				});
				chans = 3					// [w, x, y]
			},
			\spread, {
				dirChannels = [inf];
				if(sampleRateStr.isNil, {
					sampleRateStr = server.sampleRate.asInteger.asString
				});
				chans = 4					// [w, x, y, z]
			},
			\diffuse, {
				dirChannels = [inf];
				sampleRateStr = "None";
				chans = 4					// [w, x, y, z]
			}
		);

		// init kernelSize if need be
		if(kernelSize.isNil, {
			kernelSize = switch(sampleRateStr.asSymbol,
				\None, 512,
				\44100, 512,
				\48000, 512,
				\88200, 1024,
				\96000, 1024,
				\176400, 2048,
				\192000, 2048
			)
		});


		// init kernel root, generate subjectPath and kernelFiles
		databasePath = this.initPath;

		subjectPath = databasePath +/+ PathName.new(
			sampleRateStr ++ "/" ++
			kernelSize ++ "/" ++
			subjectID.asString.padLeft(4, "0")
		);

		// attempt to load kernel

		if(subjectPath.isFolder.not, {	// does kernel path exist?

			case(
				// --> missing kernel database
				{ databasePath.isFolder.not }, {
					errorMsg = "ATK kernel database missing!" +
					"Please install % database.".format(this.kind)
				},

				// --> unsupported SR
				{ PathName.new(subjectPath.parentLevelPath(2)).isFolder.not }, {
					"Supported samplerates:".warn;
					(PathName.new(subjectPath.parentLevelPath(3)).folders).do({
						|folder|
						("\t" + folder.folderName).postln
					});

					errorMsg = "Samplerate = % is not available for".format(sampleRateStr)
					+
					"% kernel encoder.".format(this.kind)
				},

				// --> unsupported kernelSize
				{ PathName.new(subjectPath.parentLevelPath(1)).isFolder.not }, {
					"Supported kernel sizes:".warn;
					(PathName.new(subjectPath.parentLevelPath(2)).folders).do({
						|folder|
						("\t" + folder.folderName).postln
					});

					errorMsg = "Kernel size = % is not available for".format(kernelSize)
					+
					"% kernel encoder.".format(this.kind)
				},

				// --> unsupported subject
				{ subjectPath.isFolder.not }, {
					"Supported subjects:".warn;
					(PathName.new(subjectPath.parentLevelPath(1)).folders).do({
						|folder|
						("\t" + folder.folderName).postln
					});

					errorMsg = "Subject % is not available for".format(subjectID)
					+
					"% kernel encoder.".format(this.kind)
				}
			);

			// throw error!
			"\n".post;
			Error(errorMsg).throw
		}, {
			if(score.isNil, {
				if(server.serverRunning.not, {		// is server running?
					// throw server error!
					Error(
						"Please boot server: %. Encoder kernel failed to load.".format(
							server.name.asString
						)
					).throw
				}, {
					// Else... everything is fine! Load kernel.
					kernel = (subjectPath.files).collect({ |kernelPath|
						chans.collect({ |chan|
							Buffer.readChannel(server, kernelPath.fullPath, channels: [chan],
								action: { |buf|
									(
										kernelBundle = kernelBundle.add(
							["/b_allocReadChannel", buf.bufnum, kernelPath.fullPath, 0, kernelSize, chan]
						);
										kernelInfo = kernelInfo.add([kernelPath.fullPath, buf.bufnum, [chan]]);
										"Kernel %, channel % loaded.".format(
											kernelPath.fileName, chan
										)
									).postln
								}
							)
						})
					})
				})
			});

			if((\CtkScore.asClass.notNil and: { score.isKindOf(\CtkScore.asClass) }), {
				kernel = (subjectPath.files).collect({ |kernelPath|
					chans.collect({ |chan|
						var buf = CtkBuffer(kernelPath.fullPath, channels: [chan]);

						kernelInfo = kernelInfo.add([kernelPath.fullPath, buf.bufnum, [chan]]);
						score.add(buf);
						buf
					})
				})
			});

			if(score.isKindOf(Score), {
				kernel = (subjectPath.files).collect({ |kernelPath|
					chans.collect({ |chan|
						var buf;

						buf = Buffer(server, kernelSize);
						kernelBundle = kernelBundle.add(
							["/b_allocReadChannel", buf.bufnum, kernelPath.fullPath, 0, kernelSize, chan]
						);
						kernelInfo = kernelInfo.add([kernelPath.fullPath, buf.bufnum, [chan]]);
						buf
					})
				});
				score.add(kernelBundle)
			});

			if((kernel.isNil and: { score.notNil }), {
				Error(
					"Score is not a Score or a CtkScore. Score is a %.".format(
						score.class.asString
					)
				).throw
			})
		})
	}

	free {
		var path;

		(kernel.shape[0]).do({ |i|
			(kernel.shape[1]).do({ |j|
				path = kernel[i][j].path;
				kernel[i][j].free;
				if(path.notNil, {
					"Encoder kernel %, channel % freed.\n".postf(
						PathName.new(path).fileName, j
					)
				});
			})
		})
	}

	buffers { ^kernel.flat }

	kernelInfo { ^kernelInfo }

	kernelBundle { ^kernelBundle }

	dim { ^kernel.shape[1] - 1 }

	numChannels { ^kernel.shape[0] }

	kernelSize { ^kernel[0][0].numFrames }

	numOutputs { ^kernel.shape[1] }

	dirInputs { ^dirChannels }

	numInputs { ^kernel.shape[0] }

	dirOutputs { ^(this.numOutputs).collect({ inf }) }

	directions { ^dirChannels }

	type { ^\encoder }

	printOn { |stream|
		stream << this.class.name << "(" <<*
			[this.kind, this.dim, this.numChannels, subjectID, this.kernelSize] <<")";
	}
}
