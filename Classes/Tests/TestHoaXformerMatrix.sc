TestHoaXformerMatrix : UnitTest {
	var order, report, floatWithin;
	var axisDirs, tetraDirs, cubeDirs, randomDirs;
	var initializedPlanewaves, targetPlanewaves;

	setUp {
		order = HoaTests.order;
		report = HoaTests.report;
		floatWithin = HoaTests.floatWithin;
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


	test_newRotateAxis {
		var pw00, pw90, pwTest, rMtx, pwRotated, angle;
		var numTests = 5;

		// planewave coefficients, encoded at [0,0]
		pw00 = HoaEncoderMatrix.newDirection(0,0,order:order).matrix.flop.getRow(0);

		// rotate on Z
		numTests.do{
			angle = rrand(-2pi, 2pi);
			rMtx = HoaXformerMatrix.newRotateAxis(\z, angle);
			// rotate the planewave with the rotation matrix
			pwRotated = rMtx.mixCoeffs(pw00);

			pwTest = HoaEncoderMatrix.newDirection(angle,0,order:order).matrix.flop.getRow(0);
			this.assertArrayFloatEquals(pwRotated, pwTest,
				"Planewave rotated on Z should match a planewave encoded in that direction", floatWithin, report);
		};

		// rotate on Y
		numTests.do{
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
		numTests.do{
			angle = rrand(-2pi, 2pi);
			rMtx = HoaXformerMatrix.newRotateAxis(\x, angle);
			// rotate the planewave with the rotation matrix
			pwRotated = rMtx.mixCoeffs(pw90);

			pwTest = HoaEncoderMatrix.newDirection(pi/2,angle,order:order).matrix.flop.getRow(0);
			this.assertArrayFloatEquals(pwRotated, pwTest,
				"Planewave rotated on X should match a planewave encoded in that direction", floatWithin, report);
		};
	}

	// test the equivalence between multiple individual axis rotations
	// via *newRotateAxis and they're *newRotate counterpart
	test_axisRotationOrder {
		var angles, axes, r123, r1, r2, r3, compoundRot;
		5.do{
			angles = 3.collect{rrand(0,2pi)}; // choose random rotation amounts
			axes = "xyz".scramble;      // randomize the axis convention
			// *newRotateAxis
			#r1, r2, r3 = 3.collect{ |i|
				HoaXformerMatrix.newRotateAxis(axes[i].asSymbol, angles[i], order).matrix;
			};
			compoundRot = r3 * (r2 * r1);

			// *newRotate
			r123 = HoaXformerMatrix.newRotate(angles[0], angles[1], angles[2], axes.asSymbol, order).matrix;

			this.assertArrayFloatEquals(r123.asArray.flat, compoundRot.asArray.flat,
				"3 individual axis rotations (*newRotateAxis) should equal the compound rotation (*newRotate)",
				floatWithin, report
			);
		}
	}
}