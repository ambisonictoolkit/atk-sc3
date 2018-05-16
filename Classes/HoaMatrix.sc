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
    var <dirChannels;

	initDirChannels { arg directions;
		dirChannels = (directions == nil).if({
			(this.order + 1).squared.collect({ inf })
		}, {
			directions.rank.switch(
				0, { Array.with(directions, 0.0).reshape(1, 2) },
				1, { directions.collect({ arg dir; Array.with(dir, 0.0)}) },
				2, { directions },
			).collect({ arg thetaPhi;  // wrap to [ +/-pi, +/-pi/2 ]
				Spherical.new(1, thetaPhi.at(0), thetaPhi.at(1)).asCartesian.asSpherical.angles
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
	zeroWithin { |within = (-300.dbamp)|
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

	order { ^this.set.asString.drop(3).asInteger }

	numInputs { ^matrix.cols }

	numOutputs { ^matrix.rows }

	numChannels {
		^(this.order + 1).squared
	}

	dirInputs {
		^this.dirChannels
	}

	dirOutputs {
		^this.dirChannels
	}

	dim {
		^(this.kind == \format).if({
			3
		}, {
			this.dirChannels.flatten.unlace.last.every({ arg item; item == 0.0 }).if({  // test - 2D
				2
			}, {
				3
			})
		})
	}

}

//-----------------------------------------------------------------------
// matrix encoders

HoaEncoderMatrix : HoaMatrix {

    // *newAtoB { arg orientation = 'flu', weight = 'dec';
    //     ^super.new('AtoB').loadFromLib(orientation, weight)
    // }

    *newFormat { arg format = [\acn, \n3d], order;
        ^super.new('format', order).initDirChannels.initFormat(format);
    }

    /*
	NOTE:

    For beaming we may need to have two types of encoders equivalent to:
       - Sampling decoding (SAD)
       - Mode-matching decoding (MMD)
    */

	// Sampling Encoding beam - 'basic' pattern
    *newDirection { arg theta = 0, phi = 0, order;
		var directions = [[ theta, phi ]];
		^super.new('dir', order).initDirChannels(directions).initBasic;
    }

	// Sampling Encoding beams - 'basic' pattern
	*newDirections { arg directions = [[ 0, 0 ]], order;
        ^super.new('dirs', order).initDirChannels(directions).initBasic;
    }

	// panto is a convenience - may wish to deprecate
    *newPanto { arg numChans = 4, orientation = \flat, order;
		var directions = Array.regularPolygon(numChans, orientation, pi);
		^super.new('panto', order).initDirChannels(directions).initBasic;
    }

    // Sampling Encoding (SAE) beam - multi pattern
    *newBeam { arg theta = 0, phi = 0, k = \basic, match = \beam, order;
		var directions = [[ theta, phi ]];
        ^super.new('beam', order).initDirChannels(directions).initBeam(k, match);
    }

	// Sampling Encoding (SAE) beams - multi pattern
    *newBeams { arg directions = [ 0, 0 ], k = \basic, match = \beam, order;
        ^super.new('beams', order).initDirChannels(directions).initBeam(k, match);
    }

    // *newFromFile { arg filePathOrName;
    //     ^super.new.initFromFile(filePathOrName, 'encoder', true).initEncoderVarsForFiles
    // }

    initBasic {  // basic beam encoder, k = \basic
		var directions, hoaOrder;

		directions = this.dirChannels;
        hoaOrder = HoaOrder.new(this.order);  // instance order

        // build encoder matrix, and set for instance
		// norm = 1.0, beamWeights = [ 1, 1, ..., 1 ]
        matrix = Matrix.with(
			directions.collect({ arg thetaPhi;
                hoaOrder.sph(thetaPhi.at(0), thetaPhi.at(1))
            }).flop
        )
    }

    initBeam {  arg k, match; // beam encoder
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
            directions.collect({ arg thetaPhi;
				(1/beamWeights)[hoaOrder.l] * hoaOrder.sph(thetaPhi.at(0), thetaPhi.at(1));
            }).flop
        )
    }

    /*
    NOTE:

    We may like to make the format matrixing more general
    by implementing via a superclass.
    */
    initFormat { arg format;
        var inputFormat;
        var outputFormat = [ \acn, \n3d ];
        var hoaOrder;
        var size;
        var coeffs;
        var colIndices, rowIndices;

        // test for single keyword format
        inputFormat = switch (format,
                    \ambix, { [ \acn, \sn3d ] },  // ambix
                    \fuma, { [ \fuma, \fuma ] },  // fuma
                    { format }  // default
        );

        hoaOrder = HoaOrder.new(this.order);  // instance order
        size = (this.order + 1).squared;

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
            size.do({ arg index;  // index increment ordered \acn
                matrix.put(
                    rowIndices.at(index),
                    colIndices.at(index),
                    coeffs.at(index)
                )
            })
        })
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

    type { ^\encoder }

}


//-----------------------------------------------------------------------
// martrix transforms


HoaXformerMatrix : HoaMatrix {

	/*  Rotation  */

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
		^super.new('rotateAxis', order).initDirChannels.initRotation(r1, r2, r3, \xyz, order)
	}

	*newRTT { |rotate = 0, tilt = 0, tumble = 0, order|
		^super.new('rotation').initDirChannels.initRotation(rotate, tilt, tumble, \zxy, order)
	}

	*newYPR { |yaw = 0, pitch = 0, roll = 0, order|
		^super.new('rotation').initDirChannels.initRotation(roll, pitch, yaw, \xyz, order)
	}

	initRotation { |r1, r2, r3, convention, order|
		matrix = HoaRotationMatrix(r1, r2, r3, convention, order).matrix;
	}

	/*  Mirroring  */

	*newMirror { arg mirror = \reflect, order;
		^super.new('mirror', order).initDirChannels.initMirror(mirror);
	}

    initMirror { arg mirror;
        var hoaOrder;
        var size;
        var coeffs;

        hoaOrder = HoaOrder.new(this.order);  // instance order
        size = (this.order + 1).squared;

        // 1) generate coefficients - ordered \acn
        coeffs = hoaOrder.reflection(mirror);

        // 2) generate matrix
        matrix = Matrix.newDiagonal(coeffs);
    }

	/* Beaming */

	// Sampling (SADE) beams - multi pattern
    *newBeam { arg theta = 0, phi = 0, decK = \basic, encK = \basic, order;
		var directions = [[ theta, phi ]];
        ^super.new('beam', order).initDirChannels(directions).initSADE(decK, encK);
    }

	// *newBeams { arg theta = 0, phi = 0, decK = \basic, encK = \basic, order;
	// 	var directions = [[ theta, phi ]];
	// 	^super.new('beam', order).initDirChannels(directions).initSADE(decK, encK);
	// }

    *newNull { arg theta = 0, phi = 0, decK = \basic, encK = \basic, order;
		var directions = [[ theta, phi ]];
        ^super.new('null', order).initDirChannels(directions).initSADER(decK, encK);
    }

	// *newNulls { arg theta = 0, phi = 0, decK = \basic, encK = \basic, order;
	// 	var directions = [[ theta, phi ]];
	// 	^super.new('null', order).initDirChannels(directions).initSADER(decK, encK);
	// }

	/*
	NOTE: matrix generation may be simplified
	*/
    initSADE {  arg decK, encK; // sampling beam decoder / re-encoder
        var directions, hoaOrder, decodingBeamWeights, encodingBeamWeights;
		var decodingMatrix, encodingMatrix;

		directions = this.dirChannels;
        hoaOrder = HoaOrder.new(this.order);  // instance order
        decodingBeamWeights = hoaOrder.beamWeights(decK);
        encodingBeamWeights = hoaOrder.beamWeights(encK);

		// build decoder matrix
		decodingMatrix = Matrix.with(
			directions.collect({ arg thetaPhi;
				var coeffs;
				coeffs = hoaOrder.sph(thetaPhi.at(0), thetaPhi.at(1));
				coeffs = (coeffs.clumpByDegree * decodingBeamWeights).flatten;
				coeffs * (Array.series(this.order+1, 1, 2) * decodingBeamWeights).sum.reciprocal;
			})
		);

		// build encoder matrix
        encodingMatrix = Matrix.with(
            directions.collect({ arg thetaPhi;
                var coeffs;
                coeffs = hoaOrder.sph(thetaPhi.at(0), thetaPhi.at(1));
                coeffs = (coeffs.clumpByDegree * encodingBeamWeights.reciprocal).flatten;
                coeffs * (Array.series(this.order+1, 1, 2) * encodingBeamWeights).sum / (this.order+1).squared;
            }).flop
        );

		// decode, re-encode
		matrix = encodingMatrix.mulMatrix(decodingMatrix)
    }

	/*
	NOTE: matrix generation may be simplified
	NOTE: This could likely be refactored to use -initSADE
	*/
	initSADER {  arg decK, encK; // sampling null decoder / re-encoder
		var directions, hoaOrder, decodingBeamWeights, encodingBeamWeights;
		var decodingMatrix, encodingMatrix;

		directions = this.dirChannels;
		hoaOrder = HoaOrder.new(this.order);  // instance order
		decodingBeamWeights = hoaOrder.beamWeights(decK);
		encodingBeamWeights = hoaOrder.beamWeights(encK);

		// build decoder matrix
		decodingMatrix = Matrix.with(
			directions.collect({ arg thetaPhi;
				var coeffs;
				coeffs = hoaOrder.sph(thetaPhi.at(0), thetaPhi.at(1));
				coeffs = (coeffs.clumpByDegree * decodingBeamWeights).flatten;
				coeffs * (Array.series(this.order+1, 1, 2) * decodingBeamWeights).sum.reciprocal;
			})
		);

		// build encoder matrix
		encodingMatrix = Matrix.with(
			directions.collect({ arg thetaPhi;
				var coeffs;
				coeffs = hoaOrder.sph(thetaPhi.at(0), thetaPhi.at(1));
				coeffs = (coeffs.clumpByDegree * encodingBeamWeights.reciprocal).flatten;
				coeffs * (Array.series(this.order+1, 1, 2) * encodingBeamWeights).sum / (this.order+1).squared;
			}).flop
		);

		// null, decode, re-encode
		matrix = Matrix.newIdentity((this.order+1).squared) - encodingMatrix.mulMatrix(decodingMatrix)
	}

	// *newFromFile { arg filePathOrName;
	// 	^super.new.initFromFile(filePathOrName, 'xformer', true);
	// }

    dim { ^3 }  // all transforms are 3D

    type { ^\xformer }

}


//-----------------------------------------------------------------------
// matrix decoders


HoaDecoderMatrix : HoaMatrix {

    *newFormat { arg format = [\acn, \n3d], order;
        ^super.new('format', order).initDirChannels.initFormat(format);
    }

    /*
    Two types:
       - Sampling decoding (SAD)
       - Mode-matching decoding (MMD)
    */

	// Sampling Decoding beam - 'basic' pattern
    *newDirection { arg theta = 0, phi = 0, order;
		var directions = [[ theta, phi ]];
        ^super.new('dir', order).initDirChannels(directions).initBasic;
    }

	// Sampling Decoding beams - 'basic' pattern
	*newDirections { arg directions = [[ 0, 0 ]], order;
        ^super.new('dirs', order).initDirChannels(directions).initBasic;
    }

	// Sampling Decoding beam - multi pattern
	*newBeam { arg theta = 0, phi = 0, k = \basic, match = \beam, order;
		var directions = [[ theta, phi ]];
		^super.new('beam', order).initDirChannels(directions).initBeam(k, match);
	}

	// Sampling Decoding beams - multi pattern
    *newBeams { arg directions = [ 0, 0 ], k = \basic, match = \beam, order;
        ^super.new('beams', order).initDirChannels(directions).initBeam(k, match);
    }

	// NOTE: these arguments diverge from FOA newPeri & newPanto
    *newProjection { arg directions, k = \basic, match = \amp, order;
		^super.new('projection', order).initDirChannels(directions).initSAD(k, match);
    }

	// NOTE: these arguments diverge from FOA newPeri & newPanto
    *newModeMatch { arg directions, k = \basic, match = \amp, order;
		^super.new('modeMatch', order).initDirChannels(directions).initMMD(k, match)
    }

	*newDiametric { arg directions, k = \basic, match = \amp, order;
		var directionPairs = directions ++ directions.rank.switch(
			1, {  // 2D
				directions.collect({ arg item;
					Polar.new(1, item).neg.angle
				})
			},
			2, {  // 3D
				directions.collect({ arg item;
					Spherical.new(1, item.at(0), item.at(1)).neg.angles
				})
			}
		);
		^super.new('diametric', order).initDirChannels(directionPairs).initMMD(k, match)
	}

    // *newBtoA { arg orientation = 'flu', weight = 'dec';
    //     ^super.new('BtoA').loadFromLib(orientation, weight);
    // }
    //
    // *newFromFile { arg filePathOrName;
    //     ^super.new.initFromFile(filePathOrName, 'decoder', true).initDecoderVarsForFiles;
    // }

    /*
    NOTE:

    We may like to make the format matrixing more general
    by implementing via a superclass.
    */
    initFormat { arg format;
        var inputFormat = [ \acn, \n3d ];
        var outputFormat;
        var hoaOrder;
        var size;
        var coeffs;
        var colIndices, rowIndices;

        // test for single keyword format
        outputFormat = switch (format,
                    \ambix, { [ \acn, \sn3d ] },  // ambix
                    \fuma, { [ \fuma, \fuma ] },  // fuma
                    { format }  // default
        );

        hoaOrder = HoaOrder.new(this.order);  // instance order
        size = (this.order + 1).squared;

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
            size.do({ arg index;  // index increment ordered \acn
                matrix.put(
                    rowIndices.at(index),
                    colIndices.at(index),
                    coeffs.at(index)
                )
            })
        })
    }

	//-----------
	// Sampling Decoders, aka SAD

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
            directions.collect({ arg thetaPhi;
               hoaOrder.sph(thetaPhi.at(0), thetaPhi.at(1))
            })
        )
    }

	initBeam {  arg k, match; // beam decoder
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
			directions.collect({ arg thetaPhi;
				beamWeights[hoaOrder.l] * hoaOrder.sph(thetaPhi.at(0), thetaPhi.at(1));
			})
		)
	}

	initSAD {  arg k, match; // sampling beam decoder, with matching gain
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
			directions.collect({arg item;
				hoaOrder.sph(item.at(0), item.at(1));  // encoding coefficients
			}).flop
		);

		// 3a) if 2D, convert (decode) output to N2D scaling & zero non-sectoral (3D) harmonics
		(dim == 2).if({
			encodingMatrix = this.class.newFormat([\acn, \n2d], outputOrder).matrix.mulMatrix(
				encodingMatrix
			);
			hoaOrder.indices.difference(hoaOrder.indices(subset: \sectoral)).do({ arg row;
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

	//-----------
	// Mode Matching Decoders, aka Pseudo-inverse
	initMMD {  arg k, match;  // mode matching decoder, with matching gain
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
			directions.collect({arg item;
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
			hoaOrder.indices(subset: \sectoral).do({ arg index, i;
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

	type { ^\decoder }

}
