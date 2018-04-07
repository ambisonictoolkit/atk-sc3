TestHoaXformerMatrix : UnitTest {
	var order, report, floatWithin;
	var axisDirs, tetraDirs, cubeDirs, randomDirs;
	var initializedPlanewaves, targetPlanewaves;

	setUp {
		order = HoaTests.order;
		report = HoaTests.report;
		floatWithin = HoaTests.floatWithin;

		// various reference directions to rotate:
		// axis directions
		axisDirs = [
			[0,0], [pi/2,0], [0,pi/2],      // +X, +Y, +Z
			[pi/2,0], [-pi/2,0], [0,-pi/2]  // -X, -Y, -Z
		];
		// tetrahedral directions
		tetraDirs = FoaDecoderMatrix.newBtoA('flu').dirOutputs;
		// directions of cube corners
		cubeDirs = 4.collect({|i|
			[45.degrad + (i*90.degrad), atan(2.sqrt.reciprocal)]
		}) ++ 4.collect({|i|
			[45.degrad + (i*90.degrad), atan(2.sqrt.reciprocal).neg]
		});
		// random directions
		randomDirs = 25.collect{[rrand(-2pi, 2pi), rrand(-2pi, 2pi)]};
	}

	initPlanewaves { |dirsArray, yaw, pitch, roll|
		var initialDirs, rotatedDirs;

		// Use Spherical class to initialize the directions.
		initialDirs = dirsArray.collect{ |azel| Spherical(1, *azel)};

		// Use Spherical class to rotate the directions.
		// The following rotations are extrinsic, so Y-P-R (which is an intrinsic convention of Tait-Bryan rotations)
		// needs to be applied in reverse order: roll (tilt), pitch (tumble), yaw (rotate)
		rotatedDirs = initialDirs.collect{ |sph|
			sph.tilt(roll).tumble(pitch).rotate(yaw)
		};

		initializedPlanewaves = initialDirs.collect{|sph|
			HoaEncoderMatrix.newDirection(sph.theta, sph.phi, order: order).matrix.flop.getRow(0);
		};

		// Encode the resulting rotated directions as planewaves.
		// These are the targets for comparison
		targetPlanewaves = rotatedDirs.collect{|sph|
			HoaEncoderMatrix.newDirection(sph.theta, sph.phi, order: order).matrix.flop.getRow(0);
		};
	}

	test_newRotate {
		var testDirs, yaw, pitch, roll;
		var rotatedPlanewaves, rMtx;
		var numRotTests = 5; // how many random rotation tests per configuration

		testDirs = { |dirs, groupName="tetrahedron"|
			numRotTests.do{
				#yaw, pitch, roll = 3.collect{rrand(-2pi,2pi)};

				// axis directions test
				this.initPlanewaves(dirs, yaw, pitch, roll);

				// create the rotation matrix
				rMtx = HoaXformerMatrix.newRotate(roll, pitch, yaw, axes: 'xyz', order: order); // works

				// rotate teh planewaves with the rotation matrix
				rotatedPlanewaves = initializedPlanewaves.collect{ |pw|
					rMtx.mixCoeffs(pw);
				};

				// compare to target planewaves, directly encoded in the pre-rotated directions
				rotatedPlanewaves.do{|rpw, i|
					this.assertArrayFloatEquals(rpw, targetPlanewaves[i],
						format("planewaves encoded in the directions of %, then rotated via y-p-r should match those planewaves encoded directly via directions that have been rotated", groupName),
						floatWithin, report
					)
				};
			}
		};
		testDirs.(axisDirs, "axes");
		testDirs.(tetraDirs, "a tetrahedon");
		testDirs.(cubeDirs, "a cube");
		testDirs.(randomDirs, "randomness");
	}

	test_newRotateAxis {
		var pw00, pw90, pwTest, rMtx, pwRotated, angle;

		// planewave coefficients, encoded at [0,0]
		pw00 = HoaEncoderMatrix.newDirection(0,0,order:order).matrix.flop.getRow(0);

		// rotate on Z
		10.do{
			angle = rrand(-2pi, 2pi);
			rMtx = HoaXformerMatrix.newRotateAxis(\z, angle);
			// rotate the planewave with the rotation matrix
			pwRotated = rMtx.mixCoeffs(pw00);

			pwTest = HoaEncoderMatrix.newDirection(angle,0,order:order).matrix.flop.getRow(0);
			this.assertArrayFloatEquals(pwRotated, pwTest,
				"Planewave rotated on Z should match a planewave encoded in that direction", floatWithin, report);
		};

		// rotate on Y
		10.do{
			angle = rrand(-2pi, 2pi);
			rMtx = HoaXformerMatrix.newRotateAxis(\y, angle);
			// rotate the planewave with the rotation matrix
			pwRotated = rMtx.mixCoeffs(pw00);

			pwTest = HoaEncoderMatrix.newDirection(0,angle,order:order).matrix.flop.getRow(0);
			this.assertArrayFloatEquals(pwRotated, pwTest,
				"Planewave rotated on Y should match a planewave encoded in that direction", floatWithin, report);
		};

		// planewave encode at 90 degrees left, [pi/2,0] (+Y)
		pw90 = HoaEncoderMatrix.newDirection(pi/2,0,order:order).matrix.flop.getRow(0);
		// rotate on X
		10.do{
			angle = rrand(-2pi, 2pi);
			rMtx = HoaXformerMatrix.newRotateAxis(\x, angle);
			// rotate the planewave with the rotation matrix
			pwRotated = rMtx.mixCoeffs(pw90);

			pwTest = HoaEncoderMatrix.newDirection(pi/2,angle,order:order).matrix.flop.getRow(0);
			this.assertArrayFloatEquals(pwRotated, pwTest,
				"Planewave rotated on X should match a planewave encoded in that direction", floatWithin, report);
		};
	}
}