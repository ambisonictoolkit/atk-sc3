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


//-----------------------------------------------------------------------
// martrix encoders

HoaEncoderMatrix : AtkMatrix {
    var <dirInputs;

    // *newAtoB { arg orientation = 'flu', weight = 'dec';
    //     ^super.new('AtoB').loadFromLib(orientation, weight)
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
    // *newZoomH2n {
    //     var ordering = 'acn', normalisation = 'sn3d';
    //     ^super.new('hoa1').loadFromLib(ordering, normalisation);
    // }
    //
    // *newOmni {
    //     ^super.new('omni').loadFromLib;
    // }

    *newDirection { arg theta = 0, phi = 0, order = 1;
        ^super.new('dir', ("HOA" ++ order).asSymbol).initDirection(theta, phi);
    }

    *newDirections { arg directions = [ 0, 0 ], order = 1;
        ^super.new('dirs', ("HOA" ++ order).asSymbol).initDirections(directions);
    }

    /*
    For beaming we may need to have two types of encoders equivalent to:
       - Sampling decoding (SAD)
       - Mode-matching decoding (MMD)
    */

    // sampling beam
    *newBeam { arg theta = 0, phi = 0, k = \basic, order = 1;
        ^super.new('beam', ("HOA" ++ order).asSymbol).initBeam(theta, phi, k);
    }

    // *newBeams { arg directions = [ 0, 0 ], k = \basic, order = 1;
    //     ^super.new('dirs', ("HOA" ++ order).asSymbol).initBeams(directions);
    // }

    // *newPanto { arg numChans = 4, orientation = 'flat';
    //     ^super.new('panto').initPanto(numChans, orientation);
    // }
    //
    // *newPeri { arg numChanPairs = 4, elevation = 0.61547970867039,
    //     orientation = 'flat';
    //     ^super.new('peri').initPeri(numChanPairs, elevation,
    //     orientation);
    // }
    //
    // *newZoomH2 { arg angles = [pi/3, 3/4*pi], pattern = 0.5857, k = 1;
    //     ^super.new('zoomH2').initZoomH2(angles, pattern, k);
    // }
    //
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

    // initPanto { arg numChans, orientation;
    //
    //     var theta;
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
    //     // set input channel directions for instance
    //     dirInputs = theta;
    //
    //     this.init2D
    // }

    // initPeri { arg numChanPairs, elevation, orientation;
    //
    //     var theta, directions, upDirs, downDirs, upMatrix, downMatrix;
    //
    //     // generate input channel pair positions
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
    //     // reorder the lower polygon
    //     if ( (orientation == 'flat') && (numChanPairs.mod(2) == 1),
    //         {									 // odd, 'flat'
    //             downDirs = downDirs.rotate((numChanPairs/2 + 1).asInteger);
    //         }, {     								// 'flat' case, default
    //             downDirs = downDirs.rotate((numChanPairs/2).asInteger);
    //         }
    //     );
    //
    //     // set input channel directions for instance
    //     dirInputs = upDirs ++ downDirs;
    //
    //     this.init3D
    // }

    // initZoomH2 { arg angles, pattern, k;
    //
    //     // set input channel directions for instance
    //     dirInputs = [ angles.at(0), angles.at(0).neg, angles.at(1), angles.at(1).neg ];
    //
    //     this.initInv2D(pattern);
    //
    //     matrix = matrix.putRow(2, matrix.getRow(2) * k); // scale Y
    // }

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

    dim { ^this.dirInputs.rank + 1}

    type { ^'encoder' }

    order { ^this.set.asString.drop(3).asInteger }

    printOn { arg stream;
        stream << this.class.name << "(" <<* [kind, this.dim, this.numInputs] <<")";
    }
}


//-----------------------------------------------------------------------
// martrix transforms


// HoaXformerMatrix : AtkMatrix {
//
//     // ~~
//     // Note: the 'kind' of the mirror transforms will be
//     // superceded by the kind specified in the .yml file
//     // e.g. 'mirrorX'
//     *newMirrorX {
//         ^super.new('mirrorAxis').loadFromLib('x');
//     }
//
//     *newMirrorY {
//         ^super.new('mirrorAxis').loadFromLib('y');
//     }
//
//     *newMirrorZ {
//         ^super.new('mirrorAxis').loadFromLib('z');
//     }
//
//     *newMirrorO {
//         ^super.new('mirrorAxis').loadFromLib('o');
//     }
//     //~~~
//
//     *newRotate { arg angle = 0;
//         ^super.new('rotate').initRotate(angle);
//     }
//
//     *newTilt { arg angle = 0;
//         ^super.new('tilt').initTilt(angle);
//     }
//
//     *newTumble { arg angle = 0;
//         ^super.new('tumble').initTumble(angle);
//     }
//
//     *newDirectO { arg angle = 0;
//         ^super.new('directO').initDirectO(angle);
//     }
//
//     *newDirectX { arg angle = 0;
//         ^super.new('directX').initDirectX(angle);
//     }
//
//     *newDirectY { arg angle = 0;
//         ^super.new('directY').initDirectY(angle);
//     }
//
//     *newDirectZ { arg angle = 0;
//         ^super.new('directZ').initDirectZ(angle);
//     }
//
//     *newDominateX { arg gain = 0;
//         ^super.new('dominateX').initDominateX(gain);
//     }
//
//     *newDominateY { arg gain = 0;
//         ^super.new('dominateY').initDominateY(gain);
//     }
//
//     *newDominateZ { arg gain = 0;
//         ^super.new('dominateZ').initDominateZ(gain);
//     }
//
//     *newZoomX { arg angle = 0;
//         ^super.new('zoomX').initZoomX(angle);
//     }
//
//     *newZoomY { arg angle = 0;
//         ^super.new('zoomY').initZoomY(angle);
//     }
//
//     *newZoomZ { arg angle = 0;
//         ^super.new('zoomZ').initZoomZ(angle);
//     }
//
//     *newFocusX { arg angle = 0;
//         ^super.new('focusX').initFocusX(angle);
//     }
//
//     *newFocusY { arg angle = 0;
//         ^super.new('focusY').initFocusY(angle);
//     }
//
//     *newFocusZ { arg angle = 0;
//         ^super.new('focusZ').initFocusZ(angle);
//     }
//
//     *newPushX { arg angle = 0;
//         ^super.new('pushX').initPushX(angle);
//     }
//
//     *newPushY { arg angle = 0;
//         ^super.new('pushY').initPushY(angle);
//     }
//
//     *newPushZ { arg angle = 0;
//         ^super.new('pushZ').initPushZ(angle);
//     }
//
//     *newPressX { arg angle = 0;
//         ^super.new('pressX').initPressX(angle);
//     }
//
//     *newPressY { arg angle = 0;
//         ^super.new('pressY').initPressY(angle);
//     }
//
//     *newPressZ { arg angle = 0;
//         ^super.new('pressZ').initPressZ(angle);
//     }
//
//     *newAsymmetry { arg angle = 0;
//         ^super.new('asymmetry').initAsymmetry(angle);
//     }
//
//     *newBalance { arg angle = 0;
//         ^super.new('zoomY').initZoomY(angle);
//     }
//
//     *newRTT { arg rotAngle = 0, tilAngle = 0, tumAngle = 0;
//         ^super.new('rtt').initRTT(rotAngle, tilAngle, tumAngle);
//     }
//
//     *newMirror { arg theta = 0, phi = 0;
//         ^super.new('mirror').initMirror(theta, phi);
//     }
//
//     *newDirect { arg angle = 0, theta = 0, phi = 0;
//         ^super.new('direct').initDirect(angle, theta, phi);
//     }
//
//     *newDominate { arg gain = 0, theta = 0, phi = 0;
//         ^super.new('dominate').initDominate(gain, theta, phi);
//     }
//
//     *newZoom { arg angle = 0, theta = 0, phi = 0;
//         ^super.new('zoom').initZoom(angle, theta, phi);
//     }
//
//     *newFocus { arg angle = 0, theta = 0, phi = 0;
//         ^super.new('focus').initFocus(angle, theta, phi);
//     }
//
//     *newPush { arg angle = 0, theta = 0, phi = 0;
//         ^super.new('push').initPush(angle, theta, phi);
//     }
//
//     *newPress { arg angle = 0, theta = 0, phi = 0;
//         ^super.new('press').initPress(angle, theta, phi);
//     }
//
//     *newFromFile { arg filePathOrName;
//         ^super.new.initFromFile(filePathOrName, 'xformer', true);
//     }
//
//     initRotate { arg angle;
//         var cosAngle, sinAngle;
//
//         // build transform matrix, and set for instance
//         // calculate cos, sin
//         cosAngle	= angle.cos;
//         sinAngle	= angle.sin;
//
//         matrix = Matrix.with([
//             [ 1, 0, 			0,			0 ],
//             [ 0, cosAngle,	sinAngle.neg,	0 ],
//             [ 0, sinAngle, 	cosAngle,		0 ],
//             [ 0, 0, 			0,			1 ]
//         ])
//     }
//
//     initTilt { arg angle;
//         var cosAngle, sinAngle;
//
//         // build transform matrix, and set for instance
//         // calculate cos, sin
//         cosAngle	= angle.cos;
//         sinAngle	= angle.sin;
//
//         matrix = Matrix.with([
//             [ 1, 0, 0,		0 			],
//             [ 0, 1, 0,		0 			],
//             [ 0,	0, cosAngle,	sinAngle.neg 	],
//             [ 0,	0, sinAngle, 	cosAngle 		]
//         ])
//     }
//
//     initTumble { arg angle;
//         var cosAngle, sinAngle;
//
//         // build transform matrix, and set for instance
//         // calculate cos, sin
//         cosAngle	= angle.cos;
//         sinAngle	= angle.sin;
//
//         matrix = Matrix.with([
//             [ 1, 0, 			0,	0 			],
//             [ 0, cosAngle,	0,	sinAngle.neg	],
//             [ 0, 0,			1, 	0 			],
//             [ 0, sinAngle,	0, 	cosAngle 		]
//         ])
//     }
//
//     initDirectO { arg angle;
//         var g0, g1;
//
//         // build transform matrix, and set for instance
//         g0 = (1 + angle.sin).sqrt;
//         g1 = (1 - angle.sin).sqrt;
//
//         matrix = Matrix.with([
//             [ g0,	0,	0,	0 	],
//             [ 0, 	g1,	0,	0	],
//             [ 0, 	0,	g1, 	0 	],
//             [ 0, 	0,	0, 	g1 	]
//         ])
//     }
//
//     initDirectX { arg angle;
//         var g0, g1;
//
//         // build transform matrix, and set for instance
//         g0 = (1 + angle.sin).sqrt;
//         g1 = (1 - angle.sin).sqrt;
//
//         matrix = Matrix.with([
//             [ g0,	0,	0,	0 	],
//             [ 0, 	g1,	0,	0	],
//             [ 0, 	0,	g0, 	0 	],
//             [ 0, 	0,	0, 	g0 	]
//         ])
//     }
//
//     initDirectY { arg angle;
//         var g0, g1;
//
//         // build transform matrix, and set for instance
//         g0 = (1 + angle.sin).sqrt;
//         g1 = (1 - angle.sin).sqrt;
//
//         matrix = Matrix.with([
//             [ g0,	0,	0,	0 	],
//             [ 0, 	g0,	0,	0	],
//             [ 0, 	0,	g1, 	0 	],
//             [ 0, 	0,	0, 	g0 	]
//         ])
//     }
//
//     initDirectZ { arg angle;
//         var g0, g1;
//
//         // build transform matrix, and set for instance
//         g0 = (1 + angle.sin).sqrt;
//         g1 = (1 - angle.sin).sqrt;
//
//         matrix = Matrix.with([
//             [ g0,	0,	0,	0 	],
//             [ 0, 	g0,	0,	0	],
//             [ 0, 	0,	g0, 	0 	],
//             [ 0, 	0,	0, 	g1 	]
//         ])
//     }
//
//     initDominateX { arg gain;
//         var g0, g1, k;
//
//         // build transform matrix, and set for instance
//         k = gain.dbamp;
//
//         g0 = (k + k.reciprocal) / 2;
//         g1 = (k - k.reciprocal) / 2.sqrt;
//
//         matrix = Matrix.with([
//             [ g0,	g1/2,	0,	0 	],
//             [ g1, 	g0,		0,	0	],
//             [ 0, 	0,		1, 	0 	],
//             [ 0, 	0,		0, 	1 	]
//         ])
//     }
//
//     initDominateY { arg gain;
//         var g0, g1, k;
//
//         // build transform matrix, and set for instance
//         k = gain.dbamp;
//
//         g0 = (k + k.reciprocal) / 2;
//         g1 = (k - k.reciprocal) / 2.sqrt;
//
//         matrix = Matrix.with([
//             [ g0,	0,	g1/2,	0 	],
//             [ 0, 	1,	0, 		0 	],
//             [ g1, 	0,	g0,		0	],
//             [ 0, 	0,	0, 		1 	]
//         ])
//     }
//
//     initDominateZ { arg gain;
//         var g0, g1, k;
//
//         // build transform matrix, and set for instance
//         k = gain.dbamp;
//
//         g0 = (k + k.reciprocal) / 2;
//         g1 = (k - k.reciprocal) / 2.sqrt;
//
//         matrix = Matrix.with([
//             [ g0,	0,	0,	g1/2	],
//             [ 0, 	1,	0, 	0 	],
//             [ 0, 	0,	1, 	0 	],
//             [ g1, 	0,	0,	g0	]
//         ])
//     }
//
//     initZoomX { arg angle;
//         var g0, g1;
//
//         // build transform matrix, and set for instance
//         g0 = angle.sin;
//         g1 = angle.cos;
//
//         matrix = Matrix.with([
//             [ 1,			g0/2.sqrt,	0,	0 	],
//             [ 2.sqrt*g0, 	1,			0,	0	],
//             [ 0, 		0,			g1, 	0 	],
//             [ 0, 		0,			0, 	g1 	]
//         ])
//     }
//
//     initZoomY { arg angle;
//         var g0, g1;
//
//         // build transform matrix, and set for instance
//         g0 = angle.sin;
//         g1 = angle.cos;
//
//         matrix = Matrix.with([
//             [ 1,			0,	g0/2.sqrt,	0 	],
//             [ 0, 		g1,	0, 			0 	],
//             [ 2.sqrt*g0, 	0,	1,			0	],
//             [ 0, 		0,	0, 			g1 	]
//         ])
//     }
//
//     initZoomZ { arg angle;
//         var g0, g1;
//
//         // build transform matrix, and set for instance
//         g0 = angle.sin;
//         g1 = angle.cos;
//
//         matrix = Matrix.with([
//             [ 1,			0,	0,	g0/2.sqrt	],
//             [ 0, 		g1,	0, 	0 		],
//             [ 0, 		0, 	g1,	0 		],
//             [ 2.sqrt*g0, 	0,	0,	1		]
//         ])
//     }
//
//     initFocusX { arg angle;
//         var g0, g1, g2;
//
//         // build transform matrix, and set for instance
//         g0 = (1 + angle.abs.sin).reciprocal;
//         g1 = 2.sqrt * angle.sin * g0;
//         g2 = angle.cos * g0;
//
//         matrix = Matrix.with([
//             [ g0,	g1/2,	0,	0	],
//             [ g1,	g0,		0,	0	],
//             [ 0,		0,		g2, 	0 	],
//             [ 0,		0,		0, 	g2	]
//         ])
//     }
//
//     initFocusY { arg angle;
//         var g0, g1, g2;
//
//         // build transform matrix, and set for instance
//         g0 = (1 + angle.abs.sin).reciprocal;
//         g1 = 2.sqrt * angle.sin * g0;
//         g2 = angle.cos * g0;
//
//         matrix = Matrix.with([
//             [ g0,	0,	g1/2,	0	],
//             [ 0,		g2,	0, 		0 	],
//             [ g1,	0,	g0,		0	],
//             [ 0,		0,	0, 		g2	]
//         ])
//     }
//
//     initFocusZ { arg angle;
//         var g0, g1, g2;
//
//         // build transform matrix, and set for instance
//         g0 = (1 + angle.abs.sin).reciprocal;
//         g1 = 2.sqrt * angle.sin * g0;
//         g2 = angle.cos * g0;
//
//         matrix = Matrix.with([
//             [ g0,	0,	0,	g1/2	],
//             [ 0,		g2,	0, 	0 	],
//             [ 0,		0, 	g2,	0	],
//             [ g1,	0,	0,	g0	]
//         ])
//     }
//
//     initPushX { arg angle;
//         var g0, g1;
//
//         // build transform matrix, and set for instance
//         g0 = 2.sqrt * angle.sin * angle.abs.sin;
//         g1 = angle.cos.squared;
//
//         matrix = Matrix.with([
//             [ 1,		0,	0,	0	],
//             [ g0,	g1,	0,	0	],
//             [ 0,		0,	g1, 	0 	],
//             [ 0,		0,	0, 	g1	]
//         ])
//     }
//
//     initPushY { arg angle;
//         var g0, g1;
//
//         // build transform matrix, and set for instance
//         g0 = 2.sqrt * angle.sin * angle.abs.sin;
//         g1 = angle.cos.squared;
//
//         matrix = Matrix.with([
//             [ 1,		0,	0,	0	],
//             [ 0,		g1,	0, 	0 	],
//             [ g0,	0,	g1,	0	],
//             [ 0,		0,	0, 	g1	]
//         ])
//     }
//
//     initPushZ { arg angle;
//         var g0, g1;
//
//         // build transform matrix, and set for instance
//         g0 = 2.sqrt * angle.sin * angle.abs.sin;
//         g1 = angle.cos.squared;
//
//         matrix = Matrix.with([
//             [ 1,		0,	0,	0	],
//             [ 0,		g1,	0, 	0 	],
//             [ 0,		0, 	g1,	0	],
//             [ g0,	0,	0,	g1	]
//         ])
//     }
//
//     initPressX { arg angle;
//         var g0, g1, g2;
//
//         // build transform matrix, and set for instance
//         g0 = 2.sqrt * angle.sin * angle.abs.sin;
//         g1 = angle.cos.squared;
//         g2 = angle.cos;
//
//         matrix = Matrix.with([
//             [ 1,		0,	0,	0	],
//             [ g0,	g1,	0,	0	],
//             [ 0,		0,	g2, 	0 	],
//             [ 0,		0,	0, 	g2	]
//         ])
//     }
//
//     initPressY { arg angle;
//         var g0, g1, g2;
//
//         // build transform matrix, and set for instance
//         g0 = 2.sqrt * angle.sin * angle.abs.sin;
//         g1 = angle.cos.squared;
//         g2 = angle.cos;
//
//         matrix = Matrix.with([
//             [ 1,		0,	0,	0	],
//             [ 0,		g2,	0, 	0 	],
//             [ g0,	0,	g1,	0	],
//             [ 0,		0,	0, 	g2	]
//         ])
//     }
//
//     initPressZ { arg angle;
//         var g0, g1, g2;
//
//         // build transform matrix, and set for instance
//         g0 = 2.sqrt * angle.sin * angle.abs.sin;
//         g1 = angle.cos.squared;
//         g2 = angle.cos;
//
//         matrix = Matrix.with([
//             [ 1,		0,	0,	0	],
//             [ 0,		g2,	0, 	0 	],
//             [ 0,		0, 	g2,	0	],
//             [ g0,	0,	0,	g1	]
//         ])
//     }
//
//     initAsymmetry { arg angle;
//         var g0, g1, g2, g3, g4;
//
//         // build transform matrix, and set for instance
//         g0 = angle.sin.neg;
//         g1 = angle.sin.squared;
//         g2 = angle.cos.squared;
//         g3 = angle.cos * angle.sin;
//         g4 = angle.cos;
//
//         matrix = Matrix.with([
//             [ 1,			    0, 2.sqrt.reciprocal*g0, 0 ],
//             [ 2.sqrt*g1,	   g2, g0,				 0 ],
//             [ 2.sqrt.neg*g3, g3, g4, 				 0 ],
//             [ 0,			   0,  0, 				g4 ]
//         ])
//     }
//
//     initRTT { arg rotAngle, tilAngle, tumAngle;
//
//         matrix = (
//             FoaXformerMatrix.newTumble(tumAngle).matrix *
//             FoaXformerMatrix.newTilt(tilAngle).matrix *
//             FoaXformerMatrix.newRotate(rotAngle).matrix
//         )
//     }
//
//     initMirror { arg theta, phi;
//
//         matrix = (
//             FoaXformerMatrix.newRotate(theta).matrix *
//             FoaXformerMatrix.newTumble(phi).matrix *
//             FoaXformerMatrix.newMirrorX.matrix *
//             FoaXformerMatrix.newTumble(phi.neg).matrix *
//             FoaXformerMatrix.newRotate(theta.neg).matrix
//         )
//     }
//
//     initDirect { arg angle, theta, phi;
//
//         matrix = (
//             FoaXformerMatrix.newRotate(theta).matrix *
//             FoaXformerMatrix.newTumble(phi).matrix *
//             FoaXformerMatrix.newDirectX(angle).matrix *
//             FoaXformerMatrix.newTumble(phi.neg).matrix *
//             FoaXformerMatrix.newRotate(theta.neg).matrix
//         )
//     }
//
//     initDominate { arg gain, theta, phi;
//
//         matrix = (
//             FoaXformerMatrix.newRotate(theta).matrix *
//             FoaXformerMatrix.newTumble(phi).matrix *
//             FoaXformerMatrix.newDominateX(gain).matrix *
//             FoaXformerMatrix.newTumble(phi.neg).matrix *
//             FoaXformerMatrix.newRotate(theta.neg).matrix
//         )
//     }
//
//     initZoom { arg angle, theta, phi;
//
//         matrix = (
//             FoaXformerMatrix.newRotate(theta).matrix *
//             FoaXformerMatrix.newTumble(phi).matrix *
//             FoaXformerMatrix.newZoomX(angle).matrix *
//             FoaXformerMatrix.newTumble(phi.neg).matrix *
//             FoaXformerMatrix.newRotate(theta.neg).matrix
//         )
//     }
//
//     initFocus { arg angle, theta, phi;
//
//         matrix = (
//             FoaXformerMatrix.newRotate(theta).matrix *
//             FoaXformerMatrix.newTumble(phi).matrix *
//             FoaXformerMatrix.newFocusX(angle).matrix *
//             FoaXformerMatrix.newTumble(phi.neg).matrix *
//             FoaXformerMatrix.newRotate(theta.neg).matrix
//         )
//     }
//
//     initPush { arg angle, theta, phi;
//
//         matrix = (
//             FoaXformerMatrix.newRotate(theta).matrix *
//             FoaXformerMatrix.newTumble(phi).matrix *
//             FoaXformerMatrix.newPushX(angle).matrix *
//             FoaXformerMatrix.newTumble(phi.neg).matrix *
//             FoaXformerMatrix.newRotate(theta.neg).matrix
//         )
//     }
//
//     initPress { arg angle, theta, phi;
//
//         matrix = (
//             FoaXformerMatrix.newRotate(theta).matrix *
//             FoaXformerMatrix.newTumble(phi).matrix *
//             FoaXformerMatrix.newPressX(angle).matrix *
//             FoaXformerMatrix.newTumble(phi.neg).matrix *
//             FoaXformerMatrix.newRotate(theta.neg).matrix
//         )
//     }
//
//     dirInputs { ^this.numInputs.collect({ inf }) }
//
//     dirOutputs { ^this.numOutputs.collect({ inf }) }
//
//     dirChannels { ^this.dirOutputs }
//
//     dim { ^3 }				// all transforms are 3D
//
//     numInputs { ^matrix.cols }
//
//     numOutputs { ^matrix.rows }
//
//     numChannels { ^4 }			// all transforms are 3D
//
//     type { ^'xformer' }
//
//     printOn { arg stream;
//         stream << this.class.name << "(" <<* [kind, this.dim, this.numChannels] <<")";
//     }
// }