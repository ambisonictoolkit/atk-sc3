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

    init2D {
        var hoaOrder;

        hoaOrder = HoaOrder.new(this.order);  // instance order

        // build encoder matrix, and set for instance
        matrix = Matrix.with(
            dirInputs.collect({ arg theta;
                hoaOrder.sph(theta, 0)
            }).flop
        )
    }

	init3D {
        var hoaOrder;

        hoaOrder = HoaOrder.new(this.order);  // instance order

        // build encoder matrix, and set for instance
        matrix = Matrix.with(
            dirInputs.collect({ arg thetaPhi;
                hoaOrder.sph(thetaPhi.at(0), thetaPhi.at(1))
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
        (phi == 0).if (
            {
                dirInputs = [ theta ];
                this.init2D
            }, {
                dirInputs = [ [ theta, phi ] ];
                this.init3D
            }
        )
	}

    initDirections { arg directions;

        // set input channel directions for instance
        dirInputs = directions;

        switch (directions.rank,	  // 2D or 3D?
            1, { this.init2D },
            2, { this.init3D }
        )
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
