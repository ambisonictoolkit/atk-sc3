TestHoaRotationMatrix : UnitTest {
	var order, report, floatWithin;
	var initializedPws, targetPws;

	setUp {
		order = HoaTests.order;
		report = HoaTests.report;
		floatWithin = HoaTests.floatWithin;
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
		initialDirs = dirsArray.collect{ |azel| Spherical(1, *azel)};

		rotatedDirs = initialDirs.collect{ |sph|
			// perform rotation on the sphere in the order specified
			axes.asString.do{ |axis, j|
				sph = sph.perform(rotMethod[axis.asSymbol], r123[j]);
			};
			sph
		};

		// generate the planewaves from the initial directions
		initializedPws = initialDirs.collect{|sph|
			HoaEncoderMatrix.newDirection(sph.theta, sph.phi, order: argOrder ?? order).matrix.flop.getRow(0);
		};

		// Encode the rotated directions as planewaves.
		// These are the targets for comparison
		targetPws = rotatedDirs.collect{|sph|
			HoaEncoderMatrix.newDirection(sph.theta, sph.phi, order: argOrder ?? order).matrix.flop.getRow(0);
		};
	}

	test_newKeywords {
		var r1, r2, r3, t1, t2;

		#r1, r2, r3 = 3.collect{rrand(-2pi,2pi)};

		// TODO: check the following 2 tests to make sure it's a valid comparison
		t1 = HoaXformerMatrix.newRTT(r1, r2, r3, order);
		t2 = HoaRotationMatrix(r1, r2, r3, \zxy, order);
		this.assertArrayFloatEquals(
			t1.matrix.asArray.flat, t2.matrix.asArray.flat,
			"HoaXformerMatrix.newRTT should be equivalent to HoaRotationMatrix with axes: 'zxy'", floatWithin, report
		);

		t1 = HoaXformerMatrix.newYPR(r1, r2, r3, order);
		t2 = HoaRotationMatrix(r3, r2, r1, \xyz, order);
		this.assertArrayFloatEquals(
			t1.matrix.asArray.flat, t2.matrix.asArray.flat,
			"HoaXformerMatrix.newYPR should be equivalent to HoaRotationMatrix with axes: 'xyz', with rotations applied in reverse order to make the rotation conform to intrinsic convention.", floatWithin, report
		);

	}

	// compare a planewaves encoded rotated via the rotation matrix with
	// a planewave encoded directly at the destination direction
	test_matrixVsSphericalRotation {
		var testDirs;

		// a function to perform rotation tests on groups of directions
		testDirs = { |dirs, groupName="a tetrahedron", numRotTests = 5|
			numRotTests.do{
				var r1, r2, r3, axes, rMtx, rotatedPws, groupTests;

				#r1, r2, r3 = 3.collect{rrand(-2pi,2pi)};
				axes = "xyz".scramble;      // randomize the convention

				// initialize the initializedPws an targetPws vars
				this.initPlanewaves(dirs, axes, r1, r2, r3, order);

				rMtx = HoaRotationMatrix(r1, r2, r3, axes.asSymbol, order);

				// "encode" the planewave coeffs via matrix multiply
				rotatedPws = initializedPws.collect{ |pw|
					rMtx.matrix * Matrix.with([pw]).flop;
				};

				// convert to an Array for the test
				rotatedPws = rotatedPws.collect{ |mtx| mtx.asArray.flat };

				// compare to target planewaves, directly encoded in the pre-rotated directions
				groupTests = rotatedPws.collect{|rpw, i|
					rpw.round(floatWithin) == targetPws[i].round(floatWithin)
				};

				this.assert(groupTests.every({|item| item}),
					format("Planewaves encoded in the directions of %, "
						"then rotated via axes % should match those planewaves "
						"encoded directly via Spherical directions that have been rotated.",
						groupName, axes
					), report
				)

			};
		};

		// run tests on each of the direction groups
		testDirs.(HoaTests.getDirs(\axis), "axes", 5);
		testDirs.(HoaTests.getDirs(\tetra, \flu), "a tetrahedon", 5);
		testDirs.(HoaTests.getDirs(\cube), "a cube", 5);
		testDirs.(HoaTests.getDirs(\random, 10), "randomness", 5);
	}

	test_FoaRttVsHoaRtt {
		var rtt, test, ref, res;
		res = 50.collect{
			rtt = 3.collect{rrand(-2pi, 2pi)};
			// A first order planewave, encoded with FOA rotation matrix
			ref = FoaXformerMatrix.newRTT(*rtt).matrix * FoaEncoderMatrix.newDirection(0,0).matrix.addRow([0]);
			// A first order planewave, encoded with HOA rotation matrix
			test = (
				HoaRotationMatrix(rtt.at(0), rtt.at(1), rtt.at(2), 'zxy', 1).matrix *
				HoaEncoderMatrix.newDirection(0,0, order: 1).matrix;
			);
			// "decode" the HOA (acn-n3d) to FOA (fuma-maxN), for test comparison
			test = HoaDecoderMatrix.newFormat(\fuma, 1).matrix * test;
			// test
			ref.round(0.00001) == test.round(0.00001)
		};

		// did every test return true?
		this.assert(res.every({|item| item}), "Rotations using Atk-Foa should match those of Atk-Hoa", report);
	}

	// test_buildR1 {
	// 	var r123, x, res, test, axes = 'xyz';
	// 	var order = 3; // order for pre-computed results is fixed
	//
	// 	r123 = [ -2.420257662452, -0.76733344238074, -0.49621582037497 ];
	// 	x = HoaRotationMatrix(r123[0], r123[1], r123[2], axes, order);
	// 	test = [\x,\y, \z].collect({|axis, i| x.buildR1(axis, r123[i])}).asArray.flat;
	// 	res = [ 1, 0, 0, 0, -0.75092478677443, 0.66038773808103, 0, -0.66038773808103, -0.75092478677443, 0.71976439977468, 0, 0.69421841578641, 0, 1, 0, -0.69421841578641, 0, 0.71976439977468, 0.87939050642489, 0.47610118379371, 0, -0.47610118379371, 0.87939050642489, 0, 0, 0, 1 ];
	// 	this.assertArrayFloatEquals(test, res, "buildR1 should match pre-computed result", floatWithin, report);
	//
	// }
	//
	// test_eulerToR3 {
	// 	var r1, r2, r3, x, res, test, axes = 'xyz';
	// 	var order = 3; // order for pre-computed results is fixed
	//
	// 	#r1,r2,r3 = [ 4.7864320159343, -2.6168319075184, 5.9114735993464 ];
	// 	x = HoaRotationMatrix(r1, r2, r3, axes, order);
	// 	test = x.eulerToR3(r1, r2, r3, axes, order).asArray.flat;
	// 	res = [ -0.8063400783711, -0.43864312088774, 0.39674663263895, 0.31433852433757, 0.25039557075208, 0.91569282527768, -0.50100595817791, 0.86307257553789, -0.064021552813329 ];
	// 	this.assertArrayFloatEquals(test, res, "eulerToR3 should match pre-computed result", floatWithin, report);
	//
	//
	// }
	//
	// test_buildSHRotMtx {
	// 	var r1, r2, r3, x, res, test, axes = 'xyz';
	// 	var order = 3; // order for pre-computed results is fixed
	//
	// 	#r1,r2,r3 = [ -1.5766910674202, 2.3862029893554, -3.1537581398968 ];
	// 	x = HoaRotationMatrix(r1, r2, r3, axes, order);
	// 	test = x.matrix.asArray.flat;
	// 	res = [
	// 		[ 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
	// 		[ 0, 0.014234241281494, -0.99985946620127, -0.0088563097758047, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
	// 		[ 0, 0.72799180711449, 0.0042913725859059, 0.68557239799711, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
	// 		[ 0, -0.68543804617873, -0.016205923887077, 0.72795058409322, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
	// 		[ 0, 0, 0, 0, 0.016432275924068, 0.68511103993551, 0.028065538844859, -0.7277047577502, 0.0033097346586161, 0, 0, 0, 0, 0, 0, 0 ],
	// 		[ 0, 0, 0, 0, 0.0033112819709691, -0.72782841522758, -0.0074318307827884, -0.6855140576287, -0.016434052563822, 0, 0, 0, 0, 0, 0, 0 ],
	// 		[ 0, 0, 0, 0, 0.86445112362424, 0.0054110723602852, -0.49997237618199, 0.0050957741797679, -0.051929098789969, 0, 0, 0, 0, 0, 0, 0 ],
	// 		[ 0, 0, 0, 0, 0.06002465620687, -0.014739249857221, -0.0001204566122328, -0.00798642692055, 0.99805610946286, 0, 0, 0, 0, 0, 0, 0 ],
	// 		[ 0, 0, 0, 0, -0.49883896322531, 0.025340397695173, -0.86555456325162, -0.020652176924317, 0.030105458567178, 0, 0, 0, 0, 0, 0, 0 ],
	// 		[ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.0072698337734171, 0.61053845285123, -0.035282581884742, 0.78961335784159, 0.031412221159795, -0.036932016306087, 0.010253300638871 ],
	// 		[ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.016748354915915, -0.059999366079082, 0.023299959947215, 0.00026931132062937, 0.012626848957814, -0.99763531653687, -0.011872063820905 ],
	// 		[ 0, 0, 0, 0, 0, 0, 0, 0, 0, -0.0093859157492523, -0.7889989327943, -0.011367345261326, 0.61222999771305, -0.0051402092705432, 0.047286393469116, -0.013243007148767 ],
	// 		[ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.50649718886116, 0.0082951007999419, -0.44576106690088, -0.0064368613053666, -0.41978698191712, -0.00049830128869168, -0.60698111052909 ],
	// 		[ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.74334936042523, -0.01238133534945, 0.17121716119262, 0.0099231472819056, -0.18209008516921, 0.0075403008021736, 0.62020237866233 ],
	// 		[ 0, 0, 0, 0, 0, 0, 0, 0, 0, -0.39200861892496, 0.00019735776253183, -0.57504321303613, -0.0083056891868219, -0.54183802953651, -0.032476970629392, 0.47004501756044 ],
	// 		[ 0, 0, 0, 0, 0, 0, 0, 0, 0, -0.19183356179186, 0.030023534055703, 0.66286506382017, 0.038421556208996, -0.70417402234095, 0.0034554740642034, -0.15987235257344 ],
	// 	].flat;
	// 	this.assertArrayFloatEquals(test, res, "buildSHRotMtx should match pre-computed result", floatWithin, report);
	// }
}


/* loops to generate code for tests */

/*

// test for buildSHRotMtx
(
var r1, r2, r3, axes = 'xyz';
10.do{
#r1,r2,r3 = 3.collect{rrand(-2pi,2pi)};
x = HoaRotationMatrix(r1, r2, r3, axes, 3);
postf("#r1,r2,r3 = %;\n", [r1,r2,r3]);
	"x = HoaRotationMatrix(r1, r2, r3, axes, order);".postln;
	"test = x.matrix.asArray.flat;".postln;
	"res = [".postln;
	x.matrix.asArray.do{|me| me.post; ",".postln};
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
x = HoaRotationMatrix(r1, r2, r3, axes);
x.eulerToR3(r1, r2, r3, axes);
postf("#r1,r2,r3 = %;\n", [r1,r2,r3]);
	"x = HoaRotationMatrix(r1, r2, r3, axes);".postln;
	"test = x.eulerToR3(r1, r2, r3, axes).asArray.flat;".postln;
	postf("res = %;\n", x.eulerToR3(r1, r2, r3, axes).asArray.flat);

	"this.assertArrayFloatEquals(test, res, \"eulerToR3 should match pre-computed result\", floatWithin, report);\n".postln;
};
nil
)

// test for buildR1
(
var r123, x,res, axes = 'xyz';
10.do{
	r123 = 3.collect{rrand(-2pi,2pi)};
	x = HoaRotationMatrix(r1, r2, r3, axes);
	res = [\x,\y, \z].collect({|axis, i| x.buildR1(axis, r123[i])}).asArray.flat;

	postf("r123 = %;\n", r123);
	"x = HoaRotationMatrix(r1, r2, r3, axes);".postln;
	"test = [\\x,\\y, \\z].collect({|axis, i| x.buildR1(axis, r123[i])}).asArray.flat;".postln;
	postf("res = %;\n", res);

	"this.assertArrayFloatEquals(test, res, \"buildR1 should match pre-computed result\", floatWithin, report);\n".postln;
};
nil
)
*/