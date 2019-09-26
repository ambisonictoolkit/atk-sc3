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
// 	Class: TestHoaMatrixXformer
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

TestHoaMatrixXformer : UnitTest {
	var order, report, floatWithin;
	var axisDirs, tetraDirs, cubeDirs, randomDirs;
	var initializedPlanewaves, targetPlanewaves;

	setUp {
		order = AtkTests.order;
		report = AtkTests.report;
		floatWithin = AtkTests.floatWithin;
	}

	initPlanewaves { |dirsArray, yaw, pitch, roll|
		var initialDirs, rotatedDirs;

		// Use Spherical class to initialize the directions.
		initialDirs = dirsArray.collect({ |azel| Spherical(1, *azel) });

		// Use Spherical class to rotate the directions.
		// The following rotations are extrinsic, so Y-P-R (which is an intrinsic convention of Tait-Bryan rotations)
		// needs to be applied in reverse order: roll (tilt), pitch (tumble), yaw (rotate)
		rotatedDirs = initialDirs.collect({ |sph|
			sph.tilt(roll).tumble(pitch).rotate(yaw)
		});

		initializedPlanewaves = initialDirs.collect({ |sph|
			HoaMatrixEncoder.newDirection(sph.theta, sph.phi, nil, order).matrix.flop.getRow(0)
		});

		// Encode the resulting rotated directions as planewaves.
		// These are the targets for comparison
		targetPlanewaves = rotatedDirs.collect({ |sph|
			HoaMatrixEncoder.newDirection(sph.theta, sph.phi, nil, order).matrix.flop.getRow(0)
		});
	}


	test_newRotateAxis {
		var pw00, pw90, comparePwFunc;
		var numTests = 5;

		comparePwFunc = { |initPw, rMtx, theta, phi, axis|
			var pwRot, pwRef;

			// generate reference planewave coefficeints (as matrix), encoded directly at [theta, phi]
			pwRef = HoaMatrixEncoder.newDirection(theta, phi, nil, order).matrix;

			this.assert(initPw.matrix.rows == rMtx.matrix.cols, "rotated planewave coefficient rows should match rotation matrix cols", report);

			// rotate the input planewave (as matrix) with the rotation matrix
			pwRot = rMtx.matrix * initPw.matrix;

			this.assertEquals(pwRot.rows, pwRef.rows,
				"Rotated planewave coefficient array size should match that of the reference planewave", report);

			this.assertArrayFloatEquals(pwRot.asArray.flat, pwRef.asArray.flat,
				format("Planewave rotated on % should match a planewave encoded in that direction", axis.asString),
				floatWithin, report);
		};

		// planewave coefficients, encoded at [0, 0]
		pw00 = HoaMatrixEncoder.newDirection(0, 0, nil, order);
		// planewave coefficients, encoded at [pi/2, 0] (+Y)
		pw90 = HoaMatrixEncoder.newDirection(pi/2, 0, nil, order);

		numTests.do({
			var angle = rrand(-2pi, 2pi);
			var rMtx = HoaMatrixXformer.newRotateAxis(\z, angle, order);

			comparePwFunc.(pw00, rMtx, angle, 0, \z);

			rMtx = HoaMatrixXformer.newRotateAxis(\y, angle, order);
			comparePwFunc.(pw00, rMtx, 0, angle, \y);

			rMtx = HoaMatrixXformer.newRotateAxis(\x, angle, order);
			comparePwFunc.(pw90, rMtx, pi/2, angle, \x)
		})
	}

	// test the equivalence between multiple individual axis rotations
	// via *newRotateAxis and they're *newRotate counterpart
	test_axisRotationOrder {
		var angles, axes, r123, r1, r2, r3, compoundRot;

		5.do({
			angles = 3.collect({ rrand(0, 2pi) }); // choose random rotation amounts
			axes = "xyz".scramble;      // randomize the axis convention
			// *newRotateAxis
			#r1, r2, r3 = 3.collect({ |i|
				HoaMatrixXformer.newRotateAxis(axes[i].asSymbol, angles[i], order).matrix
			});
			compoundRot = r3 * (r2 * r1);

			// *newRotate
			r123 = HoaMatrixXformer.newRotate(angles[0], angles[1], angles[2], axes.asSymbol, order).matrix;

			this.assertArrayFloatEquals(r123.asArray.flat, compoundRot.asArray.flat,
				"3 individual axis rotations (*newRotateAxis) should equal the compound rotation (*newRotate)",
				floatWithin, report
			)
		})
	}
}
