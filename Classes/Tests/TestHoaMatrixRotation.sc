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
// 	Class: TestHoaMatrixRotation
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

TestHoaMatrixRotation : UnitTest {
	var order, report, floatWithin;
	var initializedPws, targetPws;

	setUp {
		order = AtkTests.order;
		report = AtkTests.report;
		floatWithin = AtkTests.floatWithin;
	}

	initPlanewaves { |dirsArray, axes, r1, r2, r3, argOrder|
		var initialDirs, rotatedDirs;
		var rotMethod, r123;

		rotMethod = Dictionary().putPairs([
			\x, \tilt,
			\y, \tumble,
			\z, \rotate
		]);

		r123 = [r1, r2, r3];

		// Use Spherical class to initialize the directions.
		initialDirs = dirsArray.collect{ |azel| Spherical(1, *azel) };

		rotatedDirs = initialDirs.collect{ |sph|
			// perform rotation on the sphere in the order specified
			(axes.asString).do({ |axis, j|
				sph = sph.perform(rotMethod[axis.asSymbol], r123[j])
			});
			sph
		};

		// generate the planewaves from the initial directions
		initializedPws = initialDirs.collect{ |sph|
			HoaMatrixEncoder.newDirection(sph.theta, sph.phi, order: argOrder ?? order).matrix.flop.getRow(0);
		};

		// Encode the rotated directions as planewaves.
		// These are the targets for comparison
		targetPws = rotatedDirs.collect{ |sph|
			HoaMatrixEncoder.newDirection(sph.theta, sph.phi, order: argOrder ?? order).matrix.flop.getRow(0);
		};
	}

	test_newKeywords {
		var r1, r2, r3, t1, t2;

		#r1, r2, r3 = 3.collect{ rrand(-2pi, 2pi) };

		// TODO: check the following 2 tests to make sure it's a valid comparison
		t1 = HoaMatrixXformer.newRTT(r1, r2, r3, order);
		t2 = HoaMatrixRotation(r1, r2, r3, \zxy, order);
		this.assertArrayFloatEquals(
			t1.matrix.asArray.flat, t2.matrix.asArray.flat,
			"HoaMatrixXformer.newRTT should be equivalent to HoaMatrixRotation with axes: 'zxy'", floatWithin, report
		);

		t1 = HoaMatrixXformer.newYPR(r1, r2, r3, order);
		t2 = HoaMatrixRotation(r3, r2, r1, \xyz, order);
		this.assertArrayFloatEquals(
			t1.matrix.asArray.flat, t2.matrix.asArray.flat,
			"HoaMatrixXformer.newYPR should be equivalent to HoaMatrixRotation with axes: 'xyz', with rotations applied in reverse order to make the rotation conform to intrinsic convention.", floatWithin, report
		);

	}

	// compare a planewaves encoded rotated via the rotation matrix with
	// a planewave encoded directly at the destination direction
	test_matrixVsSphericalRotation {
		var testDirs;

		// a function to perform rotation tests on groups of directions
		testDirs = { |dirs, groupName = "a tetrahedron", numRotTests = 5|
			numRotTests.do({
				var r1, r2, r3, axes, rMtx, rotatedPws, groupTests;

				#r1, r2, r3 = 3.collect{ rrand(-2pi, 2pi) };
				axes = "xyz".scramble;      // randomize the convention

				// initialize the initializedPws an targetPws vars
				this.initPlanewaves(dirs, axes, r1, r2, r3, order);

				rMtx = HoaMatrixRotation(r1, r2, r3, axes.asSymbol, order);

				// "encode" the planewave coeffs via matrix multiply
				rotatedPws = initializedPws.collect{ |pw|
					rMtx.matrix * Matrix.with([pw]).flop;
				};

				// convert to an Array for the test
				rotatedPws = rotatedPws.collect{ |mtx| mtx.asArray.flat };

				// compare to target planewaves, directly encoded in the pre-rotated directions
				groupTests = rotatedPws.collect{ |rpw, i|
					rpw.round(floatWithin) == targetPws[i].round(floatWithin)
				};

				this.assert(groupTests.every({ |item| item }),
					format("Planewaves encoded in the directions of %, "
						"then rotated via axes % should match those planewaves "
						"encoded directly via Spherical directions that have been rotated.",
						groupName, axes
					), report
				)

			})
		};

		// run tests on each of the direction groups
		testDirs.(AtkTests.getDirs(\axis), "axes", 5);
		testDirs.(AtkTests.getDirs(\tetra, \flu), "a tetrahedon", 5);
		testDirs.(AtkTests.getDirs(\cube), "a cube", 5);
		testDirs.(AtkTests.getDirs(\random, 10), "randomness", 5);
	}

	test_FoaRttVsHoaRtt {
		var rtt, test, ref, res;
		res = 50.collect{
			rtt = 3.collect{ rrand(-2pi, 2pi) };
			// A first order planewave, encoded with FOA rotation matrix
			ref = FoaXformerMatrix.newRTT(*rtt).matrix * FoaEncoderMatrix.newDirection(0, 0).matrix.addRow([0]);
			// A first order planewave, encoded with HOA rotation matrix
			test = (
				HoaMatrixRotation(rtt[0], rtt[1], rtt[2], 'zxy', 1).matrix *
				HoaMatrixEncoder.newDirection(0, 0, order: 1).matrix;
			);
			// "decode" the HOA (acn-n3d) to FOA (fuma-maxN), for test comparison
			test = HoaMatrixDecoder.newFormat(\fuma, 1).matrix * test;
			// test
			ref.round(0.00001) == test.round(0.00001)
		};

		// did every test return true?
		this.assert(res.every({ |item| item }), "Rotations using Atk-Foa should match those of Atk-Hoa", report);
	}

}


/* loops to generate code for tests */

/*

// test for buildSHRotMtx
(
var r1, r2, r3, axes = 'xyz';
10.do{
#r1,r2,r3 = 3.collect{rrand(-2pi,2pi)};
x = HoaMatrixRotation(r1, r2, r3, axes, 3);
postf("#r1,r2,r3 = %;\n", [r1,r2,r3]);
	"x = HoaMatrixRotation(r1, r2, r3, axes, order);".postln;
	"test = x.matrix.asArray.flat;".postln;
	"res = [".postln;
	x.matrix.asArray.do{ |me| me.post; ", ".postln };
	"].flat;".postln;
	"this.assertArrayFloatEquals(test, res, \"buildSHRotMtx should match pre-computed result\", floatWithin, report);\n".postln;
};
nil
)

// test for eulerToR3
(
var r1, r2, r3, axes = 'xyz';
10.do{
#r1,r2,r3 = 3.collect{rrand(-2pi,2pi)};
x = HoaMatrixRotation(r1, r2, r3, axes);
x.eulerToR3(r1, r2, r3, axes);
postf("#r1,r2,r3 = %;\n", [r1,r2,r3]);
	"x = HoaMatrixRotation(r1, r2, r3, axes);".postln;
	"test = x.eulerToR3(r1, r2, r3, axes).asArray.flat;".postln;
	postf("res = %;\n", x.eulerToR3(r1, r2, r3, axes).asArray.flat);

	"this.assertArrayFloatEquals(test, res, \"eulerToR3 should match pre-computed result\", floatWithin, report);\n".postln;
};
nil
)

// test for buildR1
(
var r123, x, res, axes = 'xyz';
10.do{
	r123 = 3.collect{rrand(-2pi,2pi)};
	x = HoaMatrixRotation(r1, r2, r3, axes);
	res = [\x,\y, \z].collect({|axis, i| x.buildR1(axis, r123[i])}).asArray.flat;

	postf("r123 = %;\n", r123);
	"x = HoaMatrixRotation(r1, r2, r3, axes);".postln;
	"test = [\\x,\\y, \\z].collect({|axis, i| x.buildR1(axis, r123[i])}).asArray.flat;".postln;
	postf("res = %;\n", res);

	"this.assertArrayFloatEquals(test, res, \"buildR1 should match pre-computed result\", floatWithin, report);\n".postln;
};
nil
)
*/
