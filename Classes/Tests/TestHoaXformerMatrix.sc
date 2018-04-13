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
		var comparePwFunc;

		comparePwFunc = { |pw, theta, phi, axis|
			// reference planewave, encoded directly at [theta, phi]
			var pwRef = HoaEncoderMatrix.newDirection(theta, phi, order: order).matrix.flop.getRow(0);

			this.assertEquals(pw.size, pwRef.size, "rotated planewave coefficient array size should match that of the reference planewave", report);
			this.assertArrayFloatEquals(pw, pwRef,
				format("Planewave rotated on % should match a planewave encoded in that direction", axis.asString), floatWithin, report);
		};

		// planewave coefficients, encoded at [0,0]
		pw00 = HoaEncoderMatrix.newDirection(0,0,order:order).matrix.flop.getRow(0);
		// planewave coefficients, encoded at [pi/2,0] (+Y)
		pw90 = HoaEncoderMatrix.newDirection(pi/2,0,order:order).matrix.flop.getRow(0);

		// rotate on Z
		numTests.do{
			angle = rrand(-2pi, 2pi);
			rMtx = HoaXformerMatrix.newRotateAxis(\z, angle, order);
			// rotate the planewave with the rotation matrix
			pwRotated = rMtx.mixCoeffs(pw00);
			comparePwFunc.(pwRotated, angle, 0, \z);
		};

		// rotate on Y
		numTests.do{
			angle = rrand(-2pi, 2pi);
			rMtx = HoaXformerMatrix.newRotateAxis(\y, angle, order);
			// rotate the planewave with the rotation matrix
			pwRotated = rMtx.mixCoeffs(pw00);
			comparePwFunc.(pwRotated, 0, angle, \y);
		};

		// rotate on X
		numTests.do{
			angle = rrand(-2pi, 2pi);
			rMtx = HoaXformerMatrix.newRotateAxis(\x, angle, order);
			// rotate the planewave with the rotation matrix
			pwRotated = rMtx.mixCoeffs(pw90);  // note pw90
			comparePwFunc.(pwRotated, pi/2, angle, \x);
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