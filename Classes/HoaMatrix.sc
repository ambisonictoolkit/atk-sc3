/*
	Copyright the ATK Community and Joseph Anderson, 2011-2017
        J Anderson	j.anderson[at]ambisonictoolkit.net
        M McCrea    mtm5[at]uw.edu

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
// 	Class: HoaEncoderMatrix
// 	Class: HoaDecoderMatrix
// 	Class: HoaXformerMatrix
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

HoaMatrix : AtkMatrix {

	initDirChannels { |directions|
		dirChannels = (directions == nil).if({
			Hoa.numOrderCoeffs(this.order).collect({ inf })
		}, {
			directions.rank.switch(
				0, { Array.with(directions, 0.0).reshape(1, 2) },
				1, { directions.collect({ |dir| Array.with(dir, 0.0)}) },
				2, { directions },
			).collect({ |thetaPhi|  // wrap to [ +/-pi, +/-pi/2 ]
				Spherical.new(1, thetaPhi.at(0), thetaPhi.at(1)).asCartesian.asSpherical.angles
			})
		})
	}

	initDirTDesign { |numChans, order|
		var minT = 2 * order;
		var designs, design, designIndex;
		var allDesigns, validDesignIndices;

		// init library - may need to catch an error here...
		TDesignLib.initLib;

		// retreive possible designs
		designs = TDesignLib.getDesign(numChans);

		// catch design of numChans not available
		dirChannels = (designs.size != 0).if({

			// sort by t
			designs.sortBy(\t);

			// select index for largest available t >= minT
			// ... allows low to high order compatibility
			designIndex = designs.selectIndices({ |des|
				des[\t] >= minT
			}).last;

			// found a design for given order?
			(designIndex != nil).if({
				// retreive design
				design = TDesign.new(
					designs[designIndex][\nPnts],
					designs[designIndex][\t],
					designs[designIndex][\dim]
				);

				// return directions, e.g., dirChannels
				design.directions.collect({|sph| sph.angles });
			}, {
				// no design found!
				// report back possible choices

				// find valid designs
				allDesigns = TDesignLib.lib.asArray;

				validDesignIndices = allDesigns.selectIndices({ |des|
					des[\t] >= minT
				});

				"Available t-designs, numChans: ".post;
				allDesigns[validDesignIndices].collect({ |des|
					des[\nPnts]
				}).sort.postcs;

				format(
					"A t-design of numChans % is not available for order %!",
					numChans,
					order
				).throw
			})
		}, {
			format(
				"A t-design of numChans % is not available!",
				numChans
			).throw
		})
	}

	initFormat { |inputFormat = \atk, outputFormat = \atk|
		var hoaOrder;
		var size;
		var coeffs;
		var colIndices, rowIndices;
		var formatKeyword;

		formatKeyword = { |format|
			switch (format,
				\atk, { [ \acn, \n3d ] },     // atk, mpegH
				\ambix, { [ \acn, \sn3d ] },  // ambix
				\fuma, { [ \fuma, \fuma ] },  // fuma
				{ format }  // default
			)
		};

		// test for single keyword format
		inputFormat = formatKeyword.value(inputFormat);
		outputFormat = formatKeyword.value(outputFormat);

		hoaOrder = HoaOrder.new(this.order);  // instance order
		size = hoaOrder.numCoeffs;

		(inputFormat == outputFormat).if({  // equal formats?
			matrix = Matrix.newIdentity(size).asFloat
		}, {  // unequal formats

			// 1) normalization - returned coefficients are ordered \acn
			coeffs = (inputFormat.at(1) == outputFormat.at(1)).if({
				Array.fill(size, { 1.0 })
			}, {
				hoaOrder.normalisation(outputFormat.at(1)) / hoaOrder.normalisation(inputFormat.at(1))
			});

			// 2) generate matrix
			colIndices = hoaOrder.indices(inputFormat.at(0));
			rowIndices = hoaOrder.indices(outputFormat.at(0));
			matrix = Matrix.newClear(size, size).asFloat;
			size.do({ |index|  // index increment ordered \acn
				matrix.put(
					rowIndices.at(index),
					colIndices.at(index),
					coeffs.at(index)
				)
			})
		})
	}

	// TODO: these utilities could move to AtkMatrix, or elsewhere?

	getSub { |rowStart=0, colStart=0, rowLength, colLength|
		^this.matrix.getSub(rowStart, colStart, rowLength, colLength)
	}

	postSub { |rowStart=0, colStart=0, rowLength, colLength, round=0.001|
		this.matrix.postSub(rowStart, colStart, rowLength, colLength, round)
	}

	// get the block matrix for a single degree within the full matrix
	getDegreeBlock { |degree, round = 0.001|
		var st = Hoa.degreeStIdx(degree);
		var nCoeffs = Hoa.numDegreeCoeffs(degree);

		^this.matrix.getSub(st, st, nCoeffs, nCoeffs, round);
	}

	// post the block matrix for a single degree within the full matrix
	postDegreeBlock { |degree, round = 0.001|
		var st = Hoa.degreeStIdx(degree);
		var nCoeffs = Hoa.numDegreeCoeffs(degree);

		this.matrix.postSub(st, st, nCoeffs, nCoeffs, round);
	}

	// force values to zero that are within threshold distance (positive or negative)
	zeroWithin { |within = (-180.dbamp)|
		this.matrix.zeroWithin(within);
	}


	// mix coefficients with a transform matrix
	mixCoeffs { |coeffs|

		if (coeffs.size != this.matrix.cols) {
			format("AtkMatrix:mixCoeffs - coeffs.size [%] != matrix.cols [%]", coeffs.size, this.matrix.cols).throw
		};

		// NOTE: .asList added to force Collection:flop.
		// Array:flop uses a primitive that has a GC bug:
		// https://github.com/supercollider/supercollider/issues/3454
		^this.matrix.cols.collect({|i|
			this.matrix.getCol(i) * coeffs[i]
		}).asList.flop.collect{|me| me.sum};
	}

	// ---------
	// return info
    printOn { |stream|
        stream << this.class.name << "(" <<* [kind, this.dim, this.numChannels] <<")";
    }

	// replaced by instance variable in AtkMatrix
	// order { ^this.set.asString.drop(3).asInteger }

	numInputs { ^matrix.cols }

	numOutputs { ^matrix.rows }

	numChannels {
		^Hoa.numOrderCoeffs(this.order)
	}

	dim {
		var is2D;

		^if (this.kind == \format, {
			3
		}, {
			if (dirChannels[0] == 'unspecified') { // catch unspecified dirChannels
				"[HoaMatrix:-dim] dirChannels is 'unspecified' (was your HoaMatrix loaded "
				"from a Matrix directly?). Set dirChannels before requesting -dim.".warn;
				// TODO: consider how returning a symbol will affect other calls on dim
				'unspecified' // return
			} {
				is2D = this.dirChannels.collect(_.last).every(_ == 0.0);
				if (is2D, { 2 }, { 3 });
			}
		})
	}

}

//-----------------------------------------------------------------------
// matrix encoders

HoaEncoderMatrix : HoaMatrix {

	// Format Encoder
	*newFormat { |format =\atk, order|
		^super.new('format', order).initDirChannels.initFormat(format, \atk);
	}

	// Projection Encoding beam - 'basic' pattern
    *newDirection { |theta = 0, phi = 0, order|
		var directions = [[ theta, phi ]];
		^super.new('dir', order).initDirChannels(directions).initBasic;
    }

	// Projection Encoding beams - 'basic' pattern
	*newDirections { |directions = ([[ 0, 0 ]]), order|
        ^super.new('dirs', order).initDirChannels(directions).initBasic;
    }

	// Projection Encoding beams (convenience to match FOA: may wish to deprecate) - 'basic' pattern
    *newPanto { |numChans = 4, orientation = \flat, order|
		var directions = Array.regularPolygon(numChans, orientation, pi);
		^super.new('panto', order).initDirChannels(directions).initBasic;
    }

    // Projection Encoding beam - multi pattern
    *newBeam { |theta = 0, phi = 0, k = \basic, order|
		var directions = [[ theta, phi ]];
        ^super.new('beam', order).initDirChannels(directions).initBeam(k, nil);
    }

	// Projection Encoding beams - multi pattern
	*newBeams { |directions = ([[ 0, 0 ]]), k = \basic, match = \beam, order|
        ^super.new('beams', order).initDirChannels(directions).initBeam(k, match);
    }

	// Modal Encoding beams - multi pattern
	*newModes { |directions = ([[ 0, 0 ]]), k = \basic, match = \beam, order|
        ^super.new('modes', order).initDirChannels(directions).initModes(k, match);
    }

	// t-design wrapper for *newBeams
    *newAtoB { |numChans = 4, k = \basic, order|
        ^super.new('AtoB', order).initDirTDesign(numChans, order).initBeam(k, \beam);
    }

    // *newFromFile { arg filePathOrName;
    //     ^super.new.initFromFile(filePathOrName, 'encoder', true).initEncoderVarsForFiles
    // }


    // ------------
    // Basic

    initBasic {  // basic beam encoder, k = \basic
		var directions, hoaOrder;

		directions = this.dirChannels;
        hoaOrder = HoaOrder.new(this.order);  // instance order

        // build encoder matrix, and set for instance
		// norm = 1.0, beamWeights = [ 1, 1, ..., 1 ]
        matrix = Matrix.with(
			directions.collect({ |thetaPhi|
                hoaOrder.sph(thetaPhi.at(0), thetaPhi.at(1))
            }).flop
        )
    }

    // ------------
	// Multi-pattern (projection)

    initBeam {  |k, match| // beam encoder
        var directions, hoaOrder, beamWeights;
		var degreeSeries, norm;

		directions = this.dirChannels;
        hoaOrder = HoaOrder.new(this.order);  // instance order
        beamWeights = hoaOrder.beamWeights(k);

		degreeSeries = Array.series(this.order+1, 1, 2);
		norm = (degreeSeries * beamWeights).sum / degreeSeries.sum;
		// rescale for matching a/b normalization?
		(match == \beam).if({
			norm = degreeSeries.sum / directions.size * norm
		});

        // build encoder matrix, and set for instance
        matrix = norm * Matrix.with(
            directions.collect({ |thetaPhi|
				(1/beamWeights)[hoaOrder.l] * hoaOrder.sph(thetaPhi.at(0), thetaPhi.at(1));
            }).flop
        )
    }

    // ------------
	// Multi-pattern (modal)

    initModes {  |k, match| // modal encoder
		var directions, order;
		var decodingMatrix;

		directions = this.dirChannels;
		order = this.order;  // instance order

		// build decoder matrix
		decodingMatrix = HoaDecoderMatrix.newBeams(
			directions,
			k,
			match,
			order
		).matrix;

		// match modes
		matrix = decodingMatrix.pseudoInverse;
	}

    // initEncoderVarsForFiles {
    //     dirInputs = if (fileParse.notNil) {
    //         if (fileParse.dirInputs.notNil) {
    //             fileParse.dirInputs.asFloat
    //         } { // so input directions are unspecified in the provided matrix
    //             matrix.cols.collect({'unspecified'})
    //         };
    //     } { // txt file provided, no fileParse
    //         matrix.cols.collect({'unspecified'});
    //     };
    // }

	numChannels {
		^this.numInputs
	}

	dirOutputs {
		^this.numOutputs.collect({ inf })
	}

	dirInputs {
		^this.dirChannels
	}

    type { ^\encoder }

}


//-----------------------------------------------------------------------
// matrix transforms


HoaXformerMatrix : HoaMatrix {

    // ------------
    // Rotation

	*newRotate { |r1 = 0, r2 = 0, r3 = 0, axes = \xyz, order|
		^super.new('rotate', order).initDirChannels.initRotation(r1, r2, r3, axes, order)
	}

	*newRotateAxis { |axis = \z, angle = 0, order|
		var r1=0,r2=0,r3=0;
		switch( axis,
			\x, {r1=angle},
			\y, {r2=angle},
			\z, {r3=angle},
			\rotate, {r3=angle},
			\tilt, {r1=angle},
			\tumble, {r2=angle},
			\yaw, {r3=angle},
			\pitch, {r2=angle},
			\roll, {r1=angle},
		);
		^super.new('rotateAxis', order).initDirChannels.initRotation(r1, r2, r3, \xyz)
	}

	*newRTT { |rotate = 0, tilt = 0, tumble = 0, order|
		^super.new('rotation', order).initDirChannels.initRotation(rotate, tilt, tumble, \zxy)
	}

	*newYPR { |yaw = 0, pitch = 0, roll = 0, order|
		^super.new('rotation', order).initDirChannels.initRotation(roll, pitch, yaw, \xyz)
	}

	/// ------------
    // Mirroring

	*newMirror { |mirror = \reflect, order|
		^super.new('mirror', order).initDirChannels.initMirror(mirror);
	}

	// Swap one axis for another.
	// TODO: This a subset of mirroring. Wrap into *newMirror method?
	// - if yes, would need a way to fork to initSwapAxes in *newMirror
	// - if yes, kind = 'mirror', otherwise need new kind e.g. 'axisSwap'
	*newSwapAxes { |axes = \yz, order|
		^super.new('mirror', order).initDirChannels.initSwapAxes(axes);
	}

    // ------------
    // Beaming & nulling

    *newBeam { |theta = 0, phi = 0, k = \basic, order|
		var directions = [[ theta, phi ]];
        ^super.new('beam', order).initDirChannels(directions).initBeam(k);
    }

    *newNull { |theta = 0, phi = 0, k = \basic, order|
		var directions = [[theta, phi]];
        ^super.new('null', order).initDirChannels(directions).initNull(k);
    }

	initRotation { |r1, r2, r3, convention|
		matrix = HoaRotationMatrix(r1, r2, r3, convention, this.order).matrix;
	}

    initMirror { |mirror|
        var hoaOrder;
        var size;
        var coeffs;

        hoaOrder = HoaOrder.new(this.order);  // instance order
        size = (this.order + 1).squared;

        // 1) generate mirror coefficients - ordered \acn
        coeffs = hoaOrder.reflection(mirror);

        // 2) generate matrix
        matrix = Matrix.newDiagonal(coeffs);
    }

	// NOTE: this contains near-zero values.
	// You can optimize these out by calling Matrix:-zeroWithin
	initSwapAxes { |axes|
		case
		{ axes == \yz or: { axes == \zy } } // swap Z<>Y axes, "J-matrix"
		{
			var rx, my;

			rx = this.class.newRotateAxis(\x, 0.5pi, this.order).matrix;
			my = this.class.newMirror(\y, this.order).matrix;

			// matrix = my * rx;

			// TODO: bake in this omptimization? larger question of migrating from Matrix to MatrixArray
			rx = MatrixArray.with(rx.asArray);
			my = MatrixArray.with(my.asArray);
			matrix = Matrix.with(my * rx);
		}
		{ axes == \xz or: { axes == \zx } } // swap Z<>X axes, "K-matrix"
		{
			var ry, mx;

			ry = this.class.newRotateAxis(\y, 0.5pi, this.order).matrix;
			mx = this.class.newMirror(\x, this.order).matrix;

			// matrix = mx * ry;

			// TODO: bake in this omptimization? larger question of migrating from Matrix to MatrixArray
			mx = MatrixArray.with(mx.asArray);
			ry = MatrixArray.with(ry.asArray);
			matrix = Matrix.with(mx * ry);
		}
		{ axes == \xy or: { axes == \yx } } // swap X<>Y axes
		{
			var rz, mx;

			rz = this.class.newRotateAxis(\z, 0.5pi, this.order).matrix;
			mx = this.class.newMirror(\x, this.order).matrix;

			matrix = mx * rz;
		}
		{
			"Cannot swap axes '%'".format(axes).throw
		};
	}

	initBeam { |k|
		var theta, phi, order;
		var decodingMatrix, encodingMatrix;

		#theta, phi = this.dirChannels.at(0);
		order = this.order;  // instance order

		// build decoder matrix
		decodingMatrix = HoaDecoderMatrix.newBeam(
			theta,
			phi,
			k,
			order
		).matrix;

		// build encoder matrix
		encodingMatrix = HoaEncoderMatrix.newDirection(
			theta,
			phi,
			order
		).matrix;

		// decode, re-encode
		matrix = encodingMatrix.mulMatrix(decodingMatrix)
	}

	initNull { |k|
		var theta, phi, order;
		var decodingMatrix, encodingMatrix;
		var xformingMatrix;

		#theta, phi = this.dirChannels.at(0);
		order = this.order;  // instance order

		// build xforming matrix
		xformingMatrix = HoaXformerMatrix.newBeam(
			theta,
			phi,
			k,
			order
		).matrix;

		// null
		matrix = Matrix.newIdentity((order+1).squared) - xformingMatrix
	}

	// *newFromFile { arg filePathOrName;
	// 	^super.new.initFromFile(filePathOrName, 'xformer', true);
	// }

    dim { ^3 }  // all transforms are 3D

    type { ^\xformer }

	dirOutputs {
		^this.dirChannels
	}

	dirInputs {
		^this.dirChannels
	}

}


//-----------------------------------------------------------------------
// matrix decoders


HoaDecoderMatrix : HoaMatrix {

	// Format Encoder
	*newFormat { |format = \atk, order|
		^super.new('format', order).initDirChannels.initFormat(\atk, format);
	}

	// Projection Decoding beam - 'basic' pattern
    *newDirection { |theta = 0, phi = 0, order|
		var directions = [[ theta, phi ]];
        ^super.new('dir', order).initDirChannels(directions).initBasic;
    }

	// Projection Decoding beams - 'basic' pattern
	*newDirections { |directions = ([[ 0, 0 ]]), order|
        ^super.new('dirs', order).initDirChannels(directions).initBasic;
    }

	// Projection Decoding beam - multi pattern
	*newBeam { |theta = 0, phi = 0, k = \basic, order|
		var directions = [[ theta, phi ]];
		^super.new('beam', order).initDirChannels(directions).initBeam(k, nil);
	}

	// Projection Decoding beams - multi pattern
	*newBeams { |directions = ([[ 0, 0 ]]), k = \basic, match = \beam, order|
        ^super.new('beams', order).initDirChannels(directions).initBeam(k, match);
    }

	// Projection: Simple Ambisonic Decoding, aka SAD
    *newProjection { |directions, k = \basic, match = \amp, order|
		^super.new('projection', order).initDirChannels(directions).initSAD(k, match);
    }

	// Projection: Simple Ambisonic Decoding, aka SAD (convenience to match FOA: may wish to deprecate)
	*newPanto { |numChans = 4, orientation = \flat, k = \basic, match = \amp, order|
		var directions = Array.regularPolygon(numChans, orientation, pi);
		^super.new('panto', order).initDirChannels(directions).initSAD(k, match);
	}

	// Mode Match: Mode Matched Decoding, aka Pseudoinverse
    *newModeMatch { |directions, k = \basic, match = \amp, order|
		^super.new('modeMatch', order).initDirChannels(directions).initMMD(k, match)
    }

	// Diametric: Mode Matched Decoding, aka Diametric Pseudoinverse
	*newDiametric { |directions, k = \basic, match = \amp, order|
		var directionPairs = directions ++ directions.rank.switch(
			1, {  // 2D
				directions.collect({ |item|
					Polar.new(1, item).neg.angle
				})
			},
			2, {  // 3D
				directions.collect({ |item|
					Spherical.new(1, item.at(0), item.at(1)).neg.angles
				})
			}
		);
		^super.new('diametric', order).initDirChannels(directionPairs).initMMD(k, match)
	}

	// t-design wrapper for *newBeams
    *newBtoA { |numChans = 4, k = \basic, order|
        ^super.new('BtoA', order).initDirTDesign(numChans, order).initBeam(k, \beam);
    }

    // *newFromFile { arg filePathOrName;
    //     ^super.new.initFromFile(filePathOrName, 'decoder', true).initDecoderVarsForFiles;
    // }

    // ------------
    // Basic

	initBasic {  // basic beam decoder, k = \basic
        var directions, hoaOrder;
		var degreeSeries, norm;

		directions = this.dirChannels;
        hoaOrder = HoaOrder.new(this.order);  // instance order

		degreeSeries = Array.series(this.order+1, 1, 2);
		norm = 1 / degreeSeries.sum;

        // build decoder matrix, and set for instance
		// beamWeights = [ 1, 1, ..., 1 ]
        matrix =  norm * Matrix.with(
            directions.collect({ |thetaPhi|
               hoaOrder.sph(thetaPhi.at(0), thetaPhi.at(1))
            })
        )
    }

    // ------------
	// Multi-pattern (projection)

	initBeam {  |k, match| // beam decoder
		var directions, hoaOrder, beamWeights;
		var degreeSeries, norm;

		directions = this.dirChannels;
		hoaOrder = HoaOrder.new(this.order);  // instance order
		beamWeights = hoaOrder.beamWeights(k);

		degreeSeries = Array.series(this.order+1, 1, 2);
		norm = 1 / (degreeSeries * beamWeights).sum;
		// rescale for matching a/b normalization?
		(match == \amp).if({
			norm = degreeSeries.sum / directions.size * norm
		});

		// build decoder matrix, and set for instance
		matrix = norm * Matrix.with(
			directions.collect({ |thetaPhi|
				beamWeights[hoaOrder.l] * hoaOrder.sph(thetaPhi.at(0), thetaPhi.at(1));
			})
		)
	}

    // ------------
	// Projection: Simple Ambisonic Decoding, aka SAD

	initSAD {  |k, match| // sampling beam decoder, with matching gain
		var directions, numOutputs;
		var inputOrder, outputOrder, hoaOrder;
		var encodingMatrix, decodingMatrix;
		var weights;
		var dim;

		// init
		directions = this.dirChannels;
		inputOrder = this.order;
		numOutputs = directions.size;
		dim = this.dim;

		// 1) determine decoder output order - estimate
		outputOrder = dim.switch(
			2, {  // 2D
				(numOutputs >= (2 * inputOrder + 1)).if({
					inputOrder
				}, {
					((numOutputs - 1) / 2).asInteger
				})
			},
			3, {  // 3D
				(numOutputs >= (inputOrder + 1).squared).if({
					inputOrder
				}, {
					numOutputs.sqrt.asInteger - 1
				})
			}
		);
		hoaOrder = HoaOrder.new(outputOrder);

		// 2) calculate weights: matching weight, beam weights
		weights = hoaOrder.matchWeight(k, dim, match, numOutputs) * hoaOrder.beamWeights(k, dim);
		weights = weights[hoaOrder.l];  // expand from degree...
		weights = Matrix.newDiagonal(weights);  // ... and assign to diagonal matrix

		// --------------------------------
		// 3) generate prototype planewave (basic) encoding matrix
		encodingMatrix = Matrix.with(
			directions.collect({ |item|
				hoaOrder.sph(item.at(0), item.at(1));  // encoding coefficients
			}).flop
		);

		// 3a) if 2D, convert (decode) output to N2D scaling & zero non-sectoral (3D) harmonics
		(dim == 2).if({
			encodingMatrix = this.class.newFormat([\acn, \n2d], outputOrder).matrix.mulMatrix(
				encodingMatrix
			);
			hoaOrder.indices.difference(hoaOrder.indices(subset: \sectoral)).do({ |row|
				encodingMatrix.putRow(row, Array.fill(numOutputs, { 0.0 }))
			})
		});

		// 4) transpose and scale: projection (basic beam, \amp norm)
		decodingMatrix = numOutputs.reciprocal * encodingMatrix.flop;

		// 4a) if 2D, convert (encode) input to N2D scaling
		// NOTE: could be included in step 3a, above
		(dim == 2).if({
			decodingMatrix = decodingMatrix.mulMatrix(
				this.class.newFormat([\acn, \n2d], outputOrder).matrix
			)
		});

		// 5) apply weights: matching weight, beam weights
		decodingMatrix = decodingMatrix.mulMatrix(weights);

		// 6) expand to match input order (if necessary)
		(inputOrder > outputOrder).if({
			decodingMatrix = (decodingMatrix.flop ++ Matrix.newClear(
				(inputOrder + 1).squared - (outputOrder + 1).squared, numOutputs)
			).flop
		});

		// assign
		matrix = decodingMatrix
	}

    // ------------
	// Mode Match: Mode Matched Decoding, aka Pseudoinverse

	initMMD {  |k, match|  // mode matching decoder, with matching gain
		var directions, numOutputs;
		var inputOrder, outputOrder, hoaOrder;
		var encodingMatrix, decodingMatrix, zerosMatrix;
		var weights;
		var dim;

		// init
		directions = this.dirChannels;
		inputOrder = this.order;
		numOutputs = directions.size;
		dim = this.dim;

		// 1) determine decoder output order - estimate
		outputOrder = dim.switch(
			2, {  // 2D
				(numOutputs >= (2 * inputOrder + 1)).if({
					inputOrder
				}, {
					((numOutputs - 1) / 2).asInteger
				})
			},
			3, {  // 3D
				(numOutputs >= (inputOrder + 1).squared).if({
					inputOrder
				}, {
					numOutputs.sqrt.asInteger - 1
				})
			}
		);
		hoaOrder = HoaOrder.new(outputOrder);

		// 2) calculate weights: matching weight, beam weights
		weights = hoaOrder.matchWeight(k, dim, match, numOutputs) * hoaOrder.beamWeights(k, dim);
		weights = weights[hoaOrder.l];  // expand from degree...
		weights = Matrix.newDiagonal(weights);  // ... and assign to diagonal matrix

		// --------------------------------
		// 3) generate prototype planewave (basic) encoding matrix
		encodingMatrix = Matrix.with(
			directions.collect({ |item|
				hoaOrder.sph(item.at(0), item.at(1));  // encoding coefficients
			}).flop
		);

		// 3a) if 2D, discard non-sectoral (3D) harmonics
		(dim == 2).if({
			encodingMatrix = Matrix.with(
				encodingMatrix.asArray[hoaOrder.indices(subset: \sectoral)]
			)
		});

		// 4) pseudo inverse
		decodingMatrix = encodingMatrix.pseudoInverse;

		// 4a) if 2D (re-)insert non-sectoral (3D) harmonics
		(dim == 2).if({
			zerosMatrix = Matrix.newClear(numOutputs, (outputOrder + 1).squared);
			hoaOrder.indices(subset: \sectoral).do({ |index, i|
				zerosMatrix.putCol(
					index,
					decodingMatrix.getCol(i)
				)
			});
			decodingMatrix = zerosMatrix;  // now filled
		});

		// 5) apply weights: matching weight, beam weights
		decodingMatrix = decodingMatrix.mulMatrix(weights);

		// 6) expand to match input order (if necessary)
		(inputOrder > outputOrder).if({
			decodingMatrix = (decodingMatrix.flop ++ Matrix.newClear(
				(inputOrder + 1).squared - (outputOrder + 1).squared, numOutputs)
			).flop
		});

		// assign
		matrix = decodingMatrix
	}

    // initDecoderVarsForFiles {
    //     if (fileParse.notNil) {
    //         dirOutputs = if (fileParse.dirOutputs.notNil) {
    //             fileParse.dirOutputs.asFloat
    //         } { // output directions are unspecified in the provided matrix
    //             matrix.rows.collect({ 'unspecified' })
    //         };
    //         shelfK = fileParse.shelfK !? {fileParse.shelfK.asFloat};
    //         shelfFreq = fileParse.shelfFreq !? {fileParse.shelfFreq.asFloat};
    //     } { // txt file provided, no fileParse
    //         dirOutputs = matrix.rows.collect({ 'unspecified' });
    //     };
    // }

	numChannels {
		^this.numOutputs
	}

	dirInputs {
		^this.numInputs.collect({ inf })
	}

	dirOutputs {
		^this.dirChannels
	}

	type { ^\decoder }

}
