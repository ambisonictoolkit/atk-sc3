/*
Copyright the ATK Community and Joseph Anderson, 2011-2018
J Anderson  j.anderson[at]ambisonictoolkit.net
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
// 	Class: AtkTests
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
//	We hope you enjoy the ATK!
//
//	For more information visit http://ambisonictoolkit.net/ or
//	email info[at]ambisonictoolkit.net
//
//---------------------------------------------------------------------


// AtkTests is a convenience class to run all Atk Tests:
// AtkTests.run
// You can also change the reporting flag and testing
// order through the classvar before running the test.

AtkTests {
	classvar
	<>report = false,
	<>order = 3,          // for HOA tests
	<>floatWithin = 1e-8; // float error tolerance

	// Run all HOA tests
	*run { |verbose=false, hoaOrder|
		report = verbose;
		hoaOrder !? { order = hoaOrder };
		[
			// list of Test classes
			TestHoaRotationMatrix,
			TestHoaMatrixXformer,
			TestNumberForHoa,      // tests sphericalHarmonic method
			TestFoaMatrix,         // includes matrix encoders, decoders, xformers
		].do(_.run)

	}

	// Return reference directions used in tests
	// orientation:  tetrahedral orientation for \tetra group
	// numDirs:      number of directions to return in the case the
	//               group doesn't inherently determine the number (e.g. random directions)
	*getDirs { |group = \tetra, orientation = 'flu', numDirs = 25|

		^switch(group,
			\axis, { [ // axis directions
				[0, 0], [pi/2, 0], [0, pi/2],      // +X, +Y, +Z
				[pi/2, 0], [-pi/2, 0], [0, -pi/2]  // -X, -Y, -Z
			] },
			\tetra, { // tetrahedral directions
				FoaDecoderMatrix.newBtoA(orientation).dirOutputs
			},
			\cube, { // directions of cube corners
				4.collect({ |i|
					[45.degrad + (i * 90.degrad), atan(2.sqrt.reciprocal)]
				}) ++ 4.collect({ |i|
					[45.degrad + (i * 90.degrad), atan(2.sqrt.reciprocal).neg]
				})
			},
			\random, { // random directions
				numDirs.collect{ [rrand(-2pi, 2pi), rrand(-2pi, 2pi)] }
			}
		);
	}
}
