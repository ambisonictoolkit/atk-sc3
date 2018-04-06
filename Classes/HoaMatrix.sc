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
	zeroWithin { |threshold = (-300.dbamp)|
		this.matrix.zeroWithin(threshold);
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
}

//-----------------------------------------------------------------------
// martrix encoders

HoaEncoderMatrix : HoaMatrix {
    var <dirInputs;

    // *newAtoB { arg orientation = 'flu', weight = 'dec';
    //     ^super.new('AtoB').loadFromLib(orientation, weight)
    // }
    //
    // *newOmni {
    //     ^super.new('omni').loadFromLib;
    // }

    *newFormat { arg format = [\acn, \n3d], order;
        ^super.new('format', order).initFormat(format);
    }

    *newDirection { arg theta = 0, phi = 0, order;
        ^super.new('dir', order).initDirection(theta, phi);
    }

    *newDirections { arg directions = [ 0, 0 ], order;
        ^super.new('dirs', order).initDirections(directions);
    }

    *newPanto { arg numChans = 4, orientation = 'flat', order;
        ^super.new('panto', order).initPanto(numChans, orientation);
    }

    /*
    For beaming we may need to have two types of encoders equivalent to:
       - Sampling decoding (SAD)
       - Mode-matching decoding (MMD)
    */

    // sampling beam
    *newBeam { arg theta = 0, phi = 0, k = \basic, order;
        ^super.new('beam', order).initBeam(theta, phi, k);
    }

    // *newBeams { arg directions = [ 0, 0 ], k = \basic, order;
    //     ^super.new('dirs', order).initBeams(directions);
    // }

    // *newFromFile { arg filePathOrName;
    //     ^super.new.initFromFile(filePathOrName, 'encoder', true).initEncoderVarsForFiles
    // }

    initBasic {  // simple, k = \basic
        var directions, hoaOrder;

        directions = (dirInputs.rank == 2).if({  // peri
            dirInputs
        }, {  // panto
            Array.with(dirInputs, Array.fill(dirInputs.size, { 0.0 })).flop
        });

        hoaOrder = HoaOrder.new(this.order);  // instance order

        // build encoder matrix, and set for instance
        matrix = Matrix.with(
            directions.collect({ arg thetaPhi;
                hoaOrder.sph(thetaPhi.at(0), thetaPhi.at(1))
            }).flop
        )
    }

    initSAE {  arg k; // sampling beam encoder
        var directions, hoaOrder, beamWeights;

        directions = (dirInputs.rank == 2).if({  // peri
            dirInputs
        }, {  // panto
            Array.with(dirInputs, Array.fill(dirInputs.size, { 0.0 })).flop
        });

        hoaOrder = HoaOrder.new(this.order);  // instance order
        beamWeights = hoaOrder.beamWeights(k);

        // build encoder matrix, and set for instance
        matrix = Matrix.with(
            directions.collect({ arg thetaPhi;
                var coeffs;
                coeffs = hoaOrder.sph(thetaPhi.at(0), thetaPhi.at(1));
                coeffs = (coeffs.clumpByDegree * beamWeights.reciprocal).flatten;
                coeffs * (Array.series(this.order+1, 1, 2) * beamWeights).sum / (this.order+1).squared;
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

        dirInputs = size.collect({ inf });  // set dirInputs

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

    // initInv2D { arg pattern;
    //
    //     var g0 = 2.sqrt.reciprocal;
    //
    //     // build 'decoder' matrix, and set for instance
    //     matrix = Matrix.newClear(dirInputs.size, 3); 	// start w/ empty matrix
    //
    //     if ( pattern.isArray,
    //         {
    //             dirInputs.do({ arg theta, i;			// mic positions, indivd patterns
    //                 matrix.putRow(i, [
    //                     (1.0 - pattern.at(i)),
    //                     pattern.at(i) * theta.cos,
    //                     pattern.at(i) * theta.sin
    //                 ])
    //             })
    //         }, {
    //             dirInputs.do({ arg theta, i;			// mic positions
    //                 matrix.putRow(i, [
    //                     (1.0 - pattern),
    //                     pattern * theta.cos,
    //                     pattern * theta.sin
    //                 ])
    //             })
    //         }
    //     );
    //
    //     // invert to encoder matrix
    //     matrix = matrix.pseudoInverse;
    //
    //     // normalise matrix
    //     matrix = matrix * matrix.getRow(0).sum.reciprocal;
    //
    //     // scale W
    //     matrix = matrix.putRow(0, matrix.getRow(0) * g0);
    // }

    // initInv3D { arg pattern;
    //
    //     var g0 = 2.sqrt.reciprocal;
    //
    //     // build 'decoder' matrix, and set for instance
    //     matrix = Matrix.newClear(dirInputs.size, 4); 	// start w/ empty matrix
    //
    //     if ( pattern.isArray,
    //         {
    //             dirInputs.do({ arg thetaPhi, i;		// mic positions, indivd patterns
    //                 matrix.putRow(i, [
    //                     (1.0 - pattern.at(i)),
    //                     pattern.at(i) * thetaPhi.at(1).cos * thetaPhi.at(0).cos,
    //                     pattern.at(i) * thetaPhi.at(1).cos * thetaPhi.at(0).sin,
    //                     pattern.at(i) * thetaPhi.at(1).sin
    //                 ])
    //             })
    //         }, {
    //             dirInputs.do({ arg thetaPhi, i;		// mic positions
    //                 matrix.putRow(i, [
    //                     (1.0 - pattern),
    //                     pattern * thetaPhi.at(1).cos * thetaPhi.at(0).cos,
    //                     pattern * thetaPhi.at(1).cos * thetaPhi.at(0).sin,
    //                     pattern * thetaPhi.at(1).sin
    //                 ])
    //             })
    //         }
    //     );
    //
    //     // invert to encoder matrix
    //     matrix = matrix.pseudoInverse;
    //
    //     // normalise matrix
    //     matrix = matrix * matrix.getRow(0).sum.reciprocal;
    //
    //     // scale W
    //     matrix = matrix.putRow(0, matrix.getRow(0) * g0);
    // }

    initDirection { arg theta, phi;

        // set input channel directions for instance
        dirInputs = (phi == 0).if({
            [ theta ];  // panto
        }, {
            [ [ theta, phi ] ];  // peri
        });
        this.initBasic
    }

    initDirections { arg directions;

        // set input channel directions for instance
        dirInputs = directions;
        this.initBasic
    }

    initBeam { arg theta, phi, k;

        // set input channel directions for instance
        dirInputs = (phi == 0).if({
            [ theta ];  // panto
        }, {
            [ [ theta, phi ] ];  // peri
        });
        this.initSAE(k)
    }

    initPanto { arg numChans, orientation;

        var theta;

        // return theta from output channel (speaker) number
        theta = numChans.collect({ arg channel;
            switch (orientation,
                'flat',	{ ((1.0 + (2.0 * channel))/numChans) * pi },
                'point',	{ ((2.0 * channel)/numChans) * pi }
            )
        });
        theta = (theta + pi).mod(2pi) - pi;

        // set input channel directions for instance
        dirInputs = theta;

        this.initBasic
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


    dirOutputs { ^this.numOutputs.collect({ inf }) }

    dirChannels { ^this.dirInputs }

    numInputs { ^matrix.cols }

    numOutputs { ^matrix.rows }

    numChannels { ^this.numInputs }

    dim {
        (this.kind == \format).if({
            ^3
        }, {
            ^this.dirInputs.rank + 1
        })
    }

    type { ^'encoder' }

    order { ^this.set.asString.drop(3).asInteger }

    printOn { arg stream;
        stream << this.class.name << "(" <<* [kind, this.dim, this.numInputs] <<")";
    }
}


//-----------------------------------------------------------------------
// martrix transforms


HoaXformerMatrix : HoaMatrix {

	/*  Rotation  */

	*newRotate { |r1 = 0, r2 = 0, r3 = 0, axes = \xyz, order|
		^super.new('rotate', order).initRotation(r1, r2, r3, axes, order)
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
		^super.new('rotateAxis', order).initRotation(r1, r2, r3, \xyz, order)
	}

	initRotation { |r1, r2, r3, convention, order|
		matrix = HoaRotationMatrix(r1, r2, r3, convention, order).matrix;
	}

	/*  Mirroring  */

	*newMirror { arg mirror = \reflect, order;
		^super.new('mirror', order).initMirror(mirror);
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

	// *newDirectO { arg angle = 0;
	// 	^super.new('directO').initDirectO(angle);
	// }
	//
	// *newDirectX { arg angle = 0;
	// 	^super.new('directX').initDirectX(angle);
	// }
	//
	// *newDirectY { arg angle = 0;
	// 	^super.new('directY').initDirectY(angle);
	// }
	//
	// *newDirectZ { arg angle = 0;
	// 	^super.new('directZ').initDirectZ(angle);
	// }
	//
	// *newDominateX { arg gain = 0;
	// 	^super.new('dominateX').initDominateX(gain);
	// }
	//
	// *newDominateY { arg gain = 0;
	// 	^super.new('dominateY').initDominateY(gain);
	// }
	//
	// *newDominateZ { arg gain = 0;
	// 	^super.new('dominateZ').initDominateZ(gain);
	// }
	//
	// *newZoomX { arg angle = 0;
	// 	^super.new('zoomX').initZoomX(angle);
	// }
	//
	// *newZoomY { arg angle = 0;
	// 	^super.new('zoomY').initZoomY(angle);
	// }
	//
	// *newZoomZ { arg angle = 0;
	// 	^super.new('zoomZ').initZoomZ(angle);
	// }
	//
	// *newFocusX { arg angle = 0;
	// 	^super.new('focusX').initFocusX(angle);
	// }
	//
	// *newFocusY { arg angle = 0;
	// 	^super.new('focusY').initFocusY(angle);
	// }
	//
	// *newFocusZ { arg angle = 0;
	// 	^super.new('focusZ').initFocusZ(angle);
	// }
	//
	// *newPushX { arg angle = 0;
	// 	^super.new('pushX').initPushX(angle);
	// }
	//
	// *newPushY { arg angle = 0;
	// 	^super.new('pushY').initPushY(angle);
	// }
	//
	// *newPushZ { arg angle = 0;
	// 	^super.new('pushZ').initPushZ(angle);
	// }
	//
	// *newPressX { arg angle = 0;
	// 	^super.new('pressX').initPressX(angle);
	// }
	//
	// *newPressY { arg angle = 0;
	// 	^super.new('pressY').initPressY(angle);
	// }
	//
	// *newPressZ { arg angle = 0;
	// 	^super.new('pressZ').initPressZ(angle);
	// }
	//
	// *newAsymmetry { arg angle = 0;
	// 	^super.new('asymmetry').initAsymmetry(angle);
	// }
	//
	// *newBalance { arg angle = 0;
	// 	^super.new('zoomY').initZoomY(angle);
	// }
	//
	// *newDirect { arg angle = 0, theta = 0, phi = 0;
	// 	^super.new('direct').initDirect(angle, theta, phi);
	// }
	//
	// *newDominate { arg gain = 0, theta = 0, phi = 0;
	// 	^super.new('dominate').initDominate(gain, theta, phi);
	// }
	//
	// *newZoom { arg angle = 0, theta = 0, phi = 0;
	// 	^super.new('zoom').initZoom(angle, theta, phi);
	// }
	//
	// *newFocus { arg angle = 0, theta = 0, phi = 0;
	// 	^super.new('focus').initFocus(angle, theta, phi);
	// }
	//
	// *newPush { arg angle = 0, theta = 0, phi = 0;
	// 	^super.new('push').initPush(angle, theta, phi);
	// }
	//
	// *newPress { arg angle = 0, theta = 0, phi = 0;
	// 	^super.new('press').initPress(angle, theta, phi);
	// }
	//
	// *newFromFile { arg filePathOrName;
	// 	^super.new.initFromFile(filePathOrName, 'xformer', true);
	// }
	//
	// initDirectO { arg angle;
	// 	var g0, g1;
	//
	// 	// build transform matrix, and set for instance
	// 	g0 = (1 + angle.sin).sqrt;
	// 	g1 = (1 - angle.sin).sqrt;
	//
	// 	matrix = Matrix.with([
	// 		[ g0,	0,	0,	0 	],
	// 		[ 0, 	g1,	0,	0	],
	// 		[ 0, 	0,	g1, 	0 	],
	// 		[ 0, 	0,	0, 	g1 	]
	// 	])
	// }
	//
	// initDirectX { arg angle;
	// 	var g0, g1;
	//
	// 	// build transform matrix, and set for instance
	// 	g0 = (1 + angle.sin).sqrt;
	// 	g1 = (1 - angle.sin).sqrt;
	//
	// 	matrix = Matrix.with([
	// 		[ g0,	0,	0,	0 	],
	// 		[ 0, 	g1,	0,	0	],
	// 		[ 0, 	0,	g0, 	0 	],
	// 		[ 0, 	0,	0, 	g0 	]
	// 	])
	// }
	//
	// initDirectY { arg angle;
	// 	var g0, g1;
	//
	// 	// build transform matrix, and set for instance
	// 	g0 = (1 + angle.sin).sqrt;
	// 	g1 = (1 - angle.sin).sqrt;
	//
	// 	matrix = Matrix.with([
	// 		[ g0,	0,	0,	0 	],
	// 		[ 0, 	g0,	0,	0	],
	// 		[ 0, 	0,	g1, 	0 	],
	// 		[ 0, 	0,	0, 	g0 	]
	// 	])
	// }
	//
	// initDirectZ { arg angle;
	// 	var g0, g1;
	//
	// 	// build transform matrix, and set for instance
	// 	g0 = (1 + angle.sin).sqrt;
	// 	g1 = (1 - angle.sin).sqrt;
	//
	// 	matrix = Matrix.with([
	// 		[ g0,	0,	0,	0 	],
	// 		[ 0, 	g0,	0,	0	],
	// 		[ 0, 	0,	g0, 	0 	],
	// 		[ 0, 	0,	0, 	g1 	]
	// 	])
	// }
	//
	// initDominateX { arg gain;
	// 	var g0, g1, k;
	//
	// 	// build transform matrix, and set for instance
	// 	k = gain.dbamp;
	//
	// 	g0 = (k + k.reciprocal) / 2;
	// 	g1 = (k - k.reciprocal) / 2.sqrt;
	//
	// 	matrix = Matrix.with([
	// 		[ g0,	g1/2,	0,	0 	],
	// 		[ g1, 	g0,		0,	0	],
	// 		[ 0, 	0,		1, 	0 	],
	// 		[ 0, 	0,		0, 	1 	]
	// 	])
	// }
	//
	// initDominateY { arg gain;
	// 	var g0, g1, k;
	//
	// 	// build transform matrix, and set for instance
	// 	k = gain.dbamp;
	//
	// 	g0 = (k + k.reciprocal) / 2;
	// 	g1 = (k - k.reciprocal) / 2.sqrt;
	//
	// 	matrix = Matrix.with([
	// 		[ g0,	0,	g1/2,	0 	],
	// 		[ 0, 	1,	0, 		0 	],
	// 		[ g1, 	0,	g0,		0	],
	// 		[ 0, 	0,	0, 		1 	]
	// 	])
	// }
	//
	// initDominateZ { arg gain;
	// 	var g0, g1, k;
	//
	// 	// build transform matrix, and set for instance
	// 	k = gain.dbamp;
	//
	// 	g0 = (k + k.reciprocal) / 2;
	// 	g1 = (k - k.reciprocal) / 2.sqrt;
	//
	// 	matrix = Matrix.with([
	// 		[ g0,	0,	0,	g1/2	],
	// 		[ 0, 	1,	0, 	0 	],
	// 		[ 0, 	0,	1, 	0 	],
	// 		[ g1, 	0,	0,	g0	]
	// 	])
	// }
	//
	// initZoomX { arg angle;
	// 	var g0, g1;
	//
	// 	// build transform matrix, and set for instance
	// 	g0 = angle.sin;
	// 	g1 = angle.cos;
	//
	// 	matrix = Matrix.with([
	// 		[ 1,			g0/2.sqrt,	0,	0 	],
	// 		[ 2.sqrt*g0, 	1,			0,	0	],
	// 		[ 0, 		0,			g1, 	0 	],
	// 		[ 0, 		0,			0, 	g1 	]
	// 	])
	// }
	//
	// initZoomY { arg angle;
	// 	var g0, g1;
	//
	// 	// build transform matrix, and set for instance
	// 	g0 = angle.sin;
	// 	g1 = angle.cos;
	//
	// 	matrix = Matrix.with([
	// 		[ 1,			0,	g0/2.sqrt,	0 	],
	// 		[ 0, 		g1,	0, 			0 	],
	// 		[ 2.sqrt*g0, 	0,	1,			0	],
	// 		[ 0, 		0,	0, 			g1 	]
	// 	])
	// }
	//
	// initZoomZ { arg angle;
	// 	var g0, g1;
	//
	// 	// build transform matrix, and set for instance
	// 	g0 = angle.sin;
	// 	g1 = angle.cos;
	//
	// 	matrix = Matrix.with([
	// 		[ 1,			0,	0,	g0/2.sqrt	],
	// 		[ 0, 		g1,	0, 	0 		],
	// 		[ 0, 		0, 	g1,	0 		],
	// 		[ 2.sqrt*g0, 	0,	0,	1		]
	// 	])
	// }
	//
	// initFocusX { arg angle;
	// 	var g0, g1, g2;
	//
	// 	// build transform matrix, and set for instance
	// 	g0 = (1 + angle.abs.sin).reciprocal;
	// 	g1 = 2.sqrt * angle.sin * g0;
	// 	g2 = angle.cos * g0;
	//
	// 	matrix = Matrix.with([
	// 		[ g0,	g1/2,	0,	0	],
	// 		[ g1,	g0,		0,	0	],
	// 		[ 0,		0,		g2, 	0 	],
	// 		[ 0,		0,		0, 	g2	]
	// 	])
	// }
	//
	// initFocusY { arg angle;
	// 	var g0, g1, g2;
	//
	// 	// build transform matrix, and set for instance
	// 	g0 = (1 + angle.abs.sin).reciprocal;
	// 	g1 = 2.sqrt * angle.sin * g0;
	// 	g2 = angle.cos * g0;
	//
	// 	matrix = Matrix.with([
	// 		[ g0,	0,	g1/2,	0	],
	// 		[ 0,		g2,	0, 		0 	],
	// 		[ g1,	0,	g0,		0	],
	// 		[ 0,		0,	0, 		g2	]
	// 	])
	// }
	//
	// initFocusZ { arg angle;
	// 	var g0, g1, g2;
	//
	// 	// build transform matrix, and set for instance
	// 	g0 = (1 + angle.abs.sin).reciprocal;
	// 	g1 = 2.sqrt * angle.sin * g0;
	// 	g2 = angle.cos * g0;
	//
	// 	matrix = Matrix.with([
	// 		[ g0,	0,	0,	g1/2	],
	// 		[ 0,		g2,	0, 	0 	],
	// 		[ 0,		0, 	g2,	0	],
	// 		[ g1,	0,	0,	g0	]
	// 	])
	// }
	//
	// initPushX { arg angle;
	// 	var g0, g1;
	//
	// 	// build transform matrix, and set for instance
	// 	g0 = 2.sqrt * angle.sin * angle.abs.sin;
	// 	g1 = angle.cos.squared;
	//
	// 	matrix = Matrix.with([
	// 		[ 1,		0,	0,	0	],
	// 		[ g0,	g1,	0,	0	],
	// 		[ 0,		0,	g1, 	0 	],
	// 		[ 0,		0,	0, 	g1	]
	// 	])
	// }
	//
	// initPushY { arg angle;
	// 	var g0, g1;
	//
	// 	// build transform matrix, and set for instance
	// 	g0 = 2.sqrt * angle.sin * angle.abs.sin;
	// 	g1 = angle.cos.squared;
	//
	// 	matrix = Matrix.with([
	// 		[ 1,		0,	0,	0	],
	// 		[ 0,		g1,	0, 	0 	],
	// 		[ g0,	0,	g1,	0	],
	// 		[ 0,		0,	0, 	g1	]
	// 	])
	// }
	//
	// initPushZ { arg angle;
	// 	var g0, g1;
	//
	// 	// build transform matrix, and set for instance
	// 	g0 = 2.sqrt * angle.sin * angle.abs.sin;
	// 	g1 = angle.cos.squared;
	//
	// 	matrix = Matrix.with([
	// 		[ 1,		0,	0,	0	],
	// 		[ 0,		g1,	0, 	0 	],
	// 		[ 0,		0, 	g1,	0	],
	// 		[ g0,	0,	0,	g1	]
	// 	])
	// }
	//
	// initPressX { arg angle;
	// 	var g0, g1, g2;
	//
	// 	// build transform matrix, and set for instance
	// 	g0 = 2.sqrt * angle.sin * angle.abs.sin;
	// 	g1 = angle.cos.squared;
	// 	g2 = angle.cos;
	//
	// 	matrix = Matrix.with([
	// 		[ 1,		0,	0,	0	],
	// 		[ g0,	g1,	0,	0	],
	// 		[ 0,		0,	g2, 	0 	],
	// 		[ 0,		0,	0, 	g2	]
	// 	])
	// }
	//
	// initPressY { arg angle;
	// 	var g0, g1, g2;
	//
	// 	// build transform matrix, and set for instance
	// 	g0 = 2.sqrt * angle.sin * angle.abs.sin;
	// 	g1 = angle.cos.squared;
	// 	g2 = angle.cos;
	//
	// 	matrix = Matrix.with([
	// 		[ 1,		0,	0,	0	],
	// 		[ 0,		g2,	0, 	0 	],
	// 		[ g0,	0,	g1,	0	],
	// 		[ 0,		0,	0, 	g2	]
	// 	])
	// }
	//
	// initPressZ { arg angle;
	// 	var g0, g1, g2;
	//
	// 	// build transform matrix, and set for instance
	// 	g0 = 2.sqrt * angle.sin * angle.abs.sin;
	// 	g1 = angle.cos.squared;
	// 	g2 = angle.cos;
	//
	// 	matrix = Matrix.with([
	// 		[ 1,		0,	0,	0	],
	// 		[ 0,		g2,	0, 	0 	],
	// 		[ 0,		0, 	g2,	0	],
	// 		[ g0,	0,	0,	g1	]
	// 	])
	// }
	//
	// initAsymmetry { arg angle;
	// 	var g0, g1, g2, g3, g4;
	//
	// 	// build transform matrix, and set for instance
	// 	g0 = angle.sin.neg;
	// 	g1 = angle.sin.squared;
	// 	g2 = angle.cos.squared;
	// 	g3 = angle.cos * angle.sin;
	// 	g4 = angle.cos;
	//
	// 	matrix = Matrix.with([
	// 		[ 1,			    0, 2.sqrt.reciprocal*g0, 0 ],
	// 		[ 2.sqrt*g1,	   g2, g0,				 0 ],
	// 		[ 2.sqrt.neg*g3, g3, g4, 				 0 ],
	// 		[ 0,			   0,  0, 				g4 ]
	// 	])
	// }
	//
	// initDirect { arg angle, theta, phi;
	//
	// 	matrix = (
	// 		FoaXformerMatrix.newRotate(theta).matrix *
	// 		FoaXformerMatrix.newTumble(phi).matrix *
	// 		FoaXformerMatrix.newDirectX(angle).matrix *
	// 		FoaXformerMatrix.newTumble(phi.neg).matrix *
	// 		FoaXformerMatrix.newRotate(theta.neg).matrix
	// 	)
	// }
	//
	// initDominate { arg gain, theta, phi;
	//
	// 	matrix = (
	// 		FoaXformerMatrix.newRotate(theta).matrix *
	// 		FoaXformerMatrix.newTumble(phi).matrix *
	// 		FoaXformerMatrix.newDominateX(gain).matrix *
	// 		FoaXformerMatrix.newTumble(phi.neg).matrix *
	// 		FoaXformerMatrix.newRotate(theta.neg).matrix
	// 	)
	// }
	//
	// initZoom { arg angle, theta, phi;
	//
	// 	matrix = (
	// 		FoaXformerMatrix.newRotate(theta).matrix *
	// 		FoaXformerMatrix.newTumble(phi).matrix *
	// 		FoaXformerMatrix.newZoomX(angle).matrix *
	// 		FoaXformerMatrix.newTumble(phi.neg).matrix *
	// 		FoaXformerMatrix.newRotate(theta.neg).matrix
	// 	)
	// }
	//
	// initFocus { arg angle, theta, phi;
	//
	// 	matrix = (
	// 		FoaXformerMatrix.newRotate(theta).matrix *
	// 		FoaXformerMatrix.newTumble(phi).matrix *
	// 		FoaXformerMatrix.newFocusX(angle).matrix *
	// 		FoaXformerMatrix.newTumble(phi.neg).matrix *
	// 		FoaXformerMatrix.newRotate(theta.neg).matrix
	// 	)
	// }
	//
	// initPush { arg angle, theta, phi;
	//
	// 	matrix = (
	// 		FoaXformerMatrix.newRotate(theta).matrix *
	// 		FoaXformerMatrix.newTumble(phi).matrix *
	// 		FoaXformerMatrix.newPushX(angle).matrix *
	// 		FoaXformerMatrix.newTumble(phi.neg).matrix *
	// 		FoaXformerMatrix.newRotate(theta.neg).matrix
	// 	)
	// }
	//
	// initPress { arg angle, theta, phi;
	//
	// 	matrix = (
	// 		FoaXformerMatrix.newRotate(theta).matrix *
	// 		FoaXformerMatrix.newTumble(phi).matrix *
	// 		FoaXformerMatrix.newPressX(angle).matrix *
	// 		FoaXformerMatrix.newTumble(phi.neg).matrix *
	// 		FoaXformerMatrix.newRotate(theta.neg).matrix
	// 	)
	// }

    dirInputs { ^this.numInputs.collect({ inf }) }

    dirOutputs { ^this.numOutputs.collect({ inf }) }

    dirChannels { ^this.dirOutputs }

    dim { ^3 }  // all transforms are 3D

    numInputs { ^matrix.cols }

    numOutputs { ^matrix.rows }

    numChannels { ^(this.order + 1).squared }  // all transforms are 3D

    type { ^'xformer' }

    order { ^this.set.asString.drop(3).asInteger }

    printOn { arg stream;
        stream << this.class.name << "(" <<* [kind, this.dim, this.numChannels] <<")";
    }
}


//-----------------------------------------------------------------------
// martrix decoders


HoaDecoderMatrix : HoaMatrix {
	var <dirOutputs;
    // var <>shelfFreq, <shelfK;

    *newFormat { arg format = [\acn, \n3d], order;
        ^super.new('format', order).initFormat(format);
    }

    // *newDiametric { arg directions = [ pi/4, 3*pi/4 ], k = 'single';
    //     ^super.new('diametric').initDiametric(directions, k);
    // }
    //
    // *newPanto { arg numChans = 4, orientation = 'flat', k = 'single';
    //     ^super.new('panto').initPanto(numChans, orientation, k);
    // }
    //
    // *newPeri { arg numChanPairs = 4, elevation = 0.61547970867039,
    //     orientation = 'flat', k = 'single';
    //     ^super.new('peri').initPeri(numChanPairs, elevation,
    //     orientation, k);
    // }
    //
    // *newQuad { arg angle = pi/4, k = 'single';
    //     ^super.new('quad').initQuad(angle, k);
    // }
    //
    // *newStereo { arg angle = pi/2, pattern = 0.5;
    //     ^super.new('stereo').initStereo(angle, pattern);
    // }
    //
    // *newMono { arg theta = 0, phi = 0, pattern = 0;
    //     ^super.new('mono').initMono(theta, phi, pattern);
    // }
    //
    // *new5_0 { arg irregKind = 'focused';
    //     ^super.new('5_0').loadFromLib(irregKind);
    // }
    //
    // *newBtoA { arg orientation = 'flu', weight = 'dec';
    //     ^super.new('BtoA').loadFromLib(orientation, weight);
    // }
    //
    // *newHoa1 { arg ordering = 'acn', normalisation = 'n3d';
    //     ^super.new('hoa1').loadFromLib(ordering, normalisation);
    // }
    //
    // *newAmbix1 {
    //     var ordering = 'acn', normalisation = 'sn3d';
    //     ^super.new('hoa1').loadFromLib(ordering, normalisation);
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

        dirOutputs = size.collect({ inf });  // set dirOutputs

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

    // initK2D { arg k;
    //
    //     if ( k.isNumber, {
    //         ^k
    //         }, {
    //             switch ( k,
    //                 'velocity', 	{ ^1 },
    //                 'energy', 	{ ^2.reciprocal.sqrt },
    //                 'controlled', { ^2.reciprocal },
    //                 'single', 	{ ^2.reciprocal.sqrt },
    //                 'dual', 		{
    //                     shelfFreq = 400.0;
    //                     shelfK = [(3/2).sqrt, 3.sqrt/2];
    //                     ^1;
    //                 }
    //             )
    //         }
    //     )
    // }
    //
    // initK3D { arg k;
    //
    //     if ( k.isNumber, {
    //         ^k
    //         }, {
    //             switch ( k,
    //                 'velocity', 	{ ^1 },
    //                 'energy', 	{ ^3.reciprocal.sqrt },
    //                 'controlled', { ^3.reciprocal },
    //                 'single', 	{ ^3.reciprocal.sqrt },
    //                 'dual', 		{
    //                     shelfFreq = 400.0;
    //                     shelfK = [2.sqrt, (2/3).sqrt];
    //                     ^1;
    //                 }
    //             )
    //         }
    //     )
    // }
    //
    // initDiametric { arg directions, k;
    //
    //     var positions, positions2;
    //     var speakerMatrix, n;
    //
    //     switch (directions.rank,			// 2D or 3D?
    //         1, {									// 2D
    //
    //             // find positions
    //             positions = Matrix.with(
    //                 directions.collect({ arg item;
    //                     Polar.new(1, item).asPoint.asArray
    //                 })
    //             );
    //
    //             // list all of the output channels (speakers)
    //             // i.e., expand to actual pairs
    //             positions2 = positions ++ (positions.neg);
    //
    //
    //             // set output channel (speaker) directions for instance
    //             dirOutputs = positions2.asArray.collect({ arg item;
    //                 item.asPoint.asPolar.angle
    //             });
    //
    //             // initialise k
    //             k = this.initK2D(k);
    //         },
    //         2, {									// 3D
    //
    //             // find positions
    //             positions = Matrix.with(
    //                 directions.collect({ arg item;
    //                     Spherical.new(1, item.at(0), item.at(1)).asCartesian.asArray
    //                 })
    //             );
    //
    //             // list all of the output channels (speakers)
    //             // i.e., expand to actual pairs
    //             positions2 = positions ++ (positions.neg);
    //
    //
    //             // set output channel (speaker) directions for instance
    //             dirOutputs = positions2.asArray.collect({ arg item;
    //                 item.asCartesian.asSpherical.angles
    //             });
    //
    //             // initialise k
    //             k = this.initK3D(k);
    //         }
    //     );
    //
    //
    //     // get velocity gains
    //     // NOTE: this comment from Heller seems to be slightly
    //     //       misleading, in that the gains returned will be
    //     //       scaled by k, which may not request a velocity
    //     //       gain. I.e., k = 1 isn't necessarily true, as it
    //     //       is assigned as an argument to this function.
    //     speakerMatrix = FoaSpeakerMatrix.newPositions(positions2, k).matrix;
    //
    //     // n = number of output channels (speakers)
    //     n = speakerMatrix.cols;
    //
    //     // build decoder matrix
    //     // resulting rows (after flop) are W, X, Y, Z gains
    //     matrix = speakerMatrix.insertRow(0, Array.fill(n, {1}));
    //
    //     // return resulting matrix
    //     // ALSO: the below code calls for the complex conjugate
    //     //       of decoder_matrix. As we are expecting real vaules,
    //     //       we may regard this call as redundant.
    //     // res = sqrt(2)/n * decoder_matrix.conj().transpose()
    //     matrix = 2.sqrt/n * matrix.flop;
    // }
    //
    // initPanto { arg numChans, orientation, k;
    //
    //     var g0, g1, theta;
    //
    //     g0 = 1.0;								// decoder gains
    //     g1 = 2.sqrt;							// 0, 1st order
    //
    //
    //     // return theta from output channel (speaker) number
    //     theta = numChans.collect({ arg channel;
    //         switch (orientation,
    //             'flat',	{ ((1.0 + (2.0 * channel))/numChans) * pi },
    //             'point',	{ ((2.0 * channel)/numChans) * pi }
    //         )
    //     });
    //     theta = (theta + pi).mod(2pi) - pi;
    //
    //     // set output channel (speaker) directions for instance
    //     dirOutputs = theta;
    //
    //     // initialise k
    //     k = this.initK2D(k);
    //
    //
    //     // build decoder matrix
    //     matrix = Matrix.newClear(numChans, 3); // start w/ empty matrix
    //
    //     numChans.do({ arg i;
    //         matrix.putRow(i, [
    //             g0,
    //             k * g1 * theta.at(i).cos,
    //             k * g1 * theta.at(i).sin
    //         ])
    //     });
    //     matrix = 2.sqrt/numChans * matrix
    // }
    //
    // initPeri { arg numChanPairs, elevation, orientation, k;
    //
    //     var theta, directions, upDirs, downDirs, upMatrix, downMatrix;
    //
    //     // generate output channel (speaker) pair positions
    //     // start with polar positions. . .
    //     theta = [];
    //     numChanPairs.do({arg i;
    //         theta = theta ++ [2 * pi * i / numChanPairs]}
    //     );
    //     if ( orientation == 'flat',
    //     { theta = theta + (pi / numChanPairs) });       // 'flat' case
    //
    //     // collect directions [ [theta, phi], ... ]
    //     // upper ring only
    //     directions = [
    //         theta,
    //         Array.newClear(numChanPairs).fill(elevation)
    //     ].flop;
    //
    //
    //     // prepare output channel (speaker) directions for instance
    //     upDirs = (directions + pi).mod(2pi) - pi;
    //
    //     downDirs = upDirs.collect({ arg angles;
    //         Spherical.new(1, angles.at(0), angles.at(1)).neg.angles
    //     });
    //
    //     // initialise k
    //     k = this.initK3D(k);
    //
    //
    //     // build decoder matrix
    //     matrix = FoaDecoderMatrix.newDiametric(directions, k).matrix;
    //
    //     // reorder the lower polygon
    //     upMatrix = matrix[..(numChanPairs-1)];
    //     downMatrix = matrix[(numChanPairs)..];
    //
    //     if ( (orientation == 'flat') && (numChanPairs.mod(2) == 1),
    //         {									 // odd, 'flat'
    //
    //             downDirs = downDirs.rotate((numChanPairs/2 + 1).asInteger);
    //             downMatrix = downMatrix.rotate((numChanPairs/2 + 1).asInteger)
    //
    //         }, {     								// 'flat' case, default
    //
    //             downDirs = downDirs.rotate((numChanPairs/2).asInteger);
    //             downMatrix = downMatrix.rotate((numChanPairs/2).asInteger)
    //         }
    //     );
    //
    //     dirOutputs = upDirs ++ downDirs;		// set output channel (speaker) directions
    //     matrix = upMatrix ++ downMatrix;			// set matrix
    //
    // }
    //
    // initQuad { arg angle, k;
    //
    //     var g0, g1, g2;
    //
    //     // set output channel (speaker) directions for instance
    //     dirOutputs = [ angle, pi - angle, (pi - angle).neg, angle.neg ];
    //
    //
    //     // initialise k
    //     k = this.initK2D(k);
    //
    //     // calculate g1, g2 (scaled by k)
    //     g0	= 1;
    //     g1	= k / (2.sqrt * angle.cos);
    //     g2	= k / (2.sqrt * angle.sin);
    //
    //     // build decoder matrix
    //     matrix = 2.sqrt/4 * Matrix.with([
    //         [ g0, g1, 	g2 		],
    //         [ g0, g1.neg, g2 		],
    //         [ g0, g1.neg, g2.neg	],
    //         [ g0, g1, 	g2.neg	]
    //     ])
    // }
    //
    // initStereo { arg angle, pattern;
    //
    //     var g0, g1, g2;
    //
    //     // set output channel (speaker) directions for instance
    //     dirOutputs = [ pi/6, pi.neg/6 ];
    //
    //     // calculate g0, g1, g2 (scaled by pattern)
    //     g0	= (1.0 - pattern) * 2.sqrt;
    //     g1	= pattern * angle.cos;
    //     g2	= pattern * angle.sin;
    //
    //     // build decoder matrix, and set for instance
    //     matrix = Matrix.with([
    //         [ g0, g1, g2		],
    //         [ g0, g1, g2.neg	]
    //     ])
    // }
    //
    // initMono { arg theta, phi, pattern;
    //
    //     // set output channel (speaker) directions for instance
    //     dirOutputs = [ 0 ];
    //
    //     // build decoder matrix, and set for instance
    //     matrix = Matrix.with([
    //         [
    //             (1.0 - pattern) * 2.sqrt,
    //             pattern * theta.cos * phi.cos,
    //             pattern * theta.sin * phi.cos,
    //             pattern * phi.sin
    //         ]
    //     ])
    // }
    //
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

	dirInputs { ^this.numInputs.collect({ inf }) }

	dirChannels { ^this.dirOutputs }

	numInputs { ^matrix.cols }

	numOutputs { ^matrix.rows }

	numChannels { ^this.numOutputs }

    dim {
        (this.kind == \format).if({
            ^3
        }, {
            ^this.dirInputs.rank + 1
        })
    }

	type { ^'decoder' }

    order { ^this.set.asString.drop(3).asInteger }

	printOn { arg stream;
		stream << this.class.name << "(" <<* [this.kind, this.dim, this.numChannels] <<")";
	}
}
