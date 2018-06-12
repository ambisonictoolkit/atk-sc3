TestFoaMatrix : UnitTest {
	var report, floatWithin;

	setUp {
		report = AtkTests.report;
		floatWithin = AtkTests.floatWithin;
	}

	// basic check that matrix attributes are populated correctly
	test_encoderMatrixAttributes {
		var m;
		var directions = AtkTests.getDirs('tetra');

		// encoder
		m = FoaEncoderMatrix.newDirections(directions, 0.5);

		// matrix
		this.assert(
			m.matrix.isKindOf(Matrix),
			"FoaEncoderMatrix:-matrix should return a Matrix",
			report
		);

		// numInputs
		this.assertEquals(
			m.numInputs,
			directions.size,
			"FoaEncoderMatrix:-numInputs should match the number of encoded directions",
			report
		);

		// dirChannels
		this.assertArrayFloatEquals(
			m.dirChannels.flat, // flat: array must be 1D to test
			directions.flat,
			"FoaEncoderMatrix:-dirChannels should match encoded directions",
			floatWithin, report
		);

		// dirInputs
		this.assertArrayFloatEquals(
			m.dirInputs.flat, // flat: array must be 1D to test
			directions.flat,
			"FoaEncoderMatrix:-dirInputs should match encoded directions",
			floatWithin, report
		);

		// dirOutputs
		this.assertEquals( // appears assertArrayFloatEquals doens't work on inf's?
			m.dirOutputs,
			inf ! 4, // 4 harmonics for FOA
			"FoaEncoderMatrix:-dirOutputs should return and array of inf's whose size matches the number of b-format channels",
			report
		);

		// dirInputs == dirChannels
		this.assertArrayFloatEquals(
			m.dirInputs,
			m.dirChannels,
			"FoaEncoderMatrix:-dirInputs should equal -dirChannels",
			floatWithin, report
		);

		[
			\kind, 'dirs',
			\set,  'FOA',
			\type, 'encoder',
			\op,   'matrix',
			\dim,  3
		].clump(2).do{ |mrPair|
			var method, result;
			#method, result = mrPair;
			this.assertEquals(
				m.perform(method),
				result,
				"FoaEncoderMatrix(*newDirections):-% should be %".format(method, result),
				report
			)
		};
	}

	test_decoderMatrixAttributes {
		var m;

		// decoder
		m = FoaDecoderMatrix.newQuad(45.degrad, 'dual');

		// matrix
		this.assert(
			m.matrix.isKindOf(Matrix),
			"FoaDecoderMatrix:-matrix should return a Matrix",
			report
		);

		// numOutputs
		this.assertEquals(
			m.numOutputs,
			4, // quad decoder
			"FoaDecoderMatrix -numOutputs should match the number of decoded directions (*newQuad == 4)",
			report
		);

		// dirChannels
		this.assertArrayFloatEquals(
			m.dirChannels,
			[ 45.0, 135.0, -135.0, -45.0 ].degrad, // quad decoder
			"FoaDecoderMatrix:-dirChannels should match decoded directions",
			floatWithin, report
		);

		// dirOutputs == dirChannels
		this.assertArrayFloatEquals(
			m.dirOutputs,
			m.dirChannels,
			"FoaDecoderMatrix:-dirOutputs should equal -dirChannels",
			floatWithin, report
		);

		// dirInputs
		this.assertEquals( // appears assertArrayFloatEquals doens't work on inf's?
			m.dirInputs,
			inf ! 3, // 3 harmonics for 2D FOA (no Z)
			"FoaDecoderMatrix:-dirInputs should return and array of inf's whose size matches the number of b-format channels",
			report
		);

		// shelfK - for dual band decoder
		this.assertArrayFloatEquals(
			m.shelfK,
			[ 1.2247448713916, 0.86602540378444 ],
			"FoaDecoderMatrix:-shelfK should should be [ 1.2247448713916, 0.86602540378444 ] (default )",
			floatWithin, report
		);

		[
			\kind, 'quad',
			\set,  'FOA',
			\type, 'decoder',
			\op,   'matrix',
			\dim,  2,
			\shelfFreq, 400.0  // default shelfK
		].clump(2).do{ |mrPair|
			var method, result;
			#method, result = mrPair;
			this.assertEquals(
				m.perform(method),
				result,
				"FoaDecoderMatrix(*newQuad, k:'dual'):-% should be %".format(method, result),
				report
			)
		};
	}

	test_xformerMatrixAttributes {
		var m;

		// xformer
		m = FoaXformerMatrix.newFocus(22.5.degrad, 45.degrad, -30.degrad);

		// matrix
		this.assert(
			m.matrix.isKindOf(Matrix),
			"FoaXformerMatrix:-matrix should return a Matrix",
			report
		);

		// numOutputs
		this.assertEquals(
			m.numOutputs,
			4, // quad decoder
			"FoaXformerMatrix:-numOutputs should match the number of b-format channels",
			report
		);

		// dirOutputs
		this.assertEquals( // appears assertArrayFloatEquals doens't work on inf's?
			m.dirOutputs,
			inf ! 4,
			"FoaXformerMatrix:-dirOutputs should return and array of inf's whose size matches the number of b-format channels",
			report
		);

		// dirInputs
		this.assertEquals(
			m.dirInputs,
			inf ! 4,
			"FoaXformerMatrix:-dirInputs should return and array of inf's whose size matches the number of b-format channels",
			report
		);

		// dirChannels
		this.assertEquals(
			m.dirChannels,
			inf ! 4,
			"FoaXformerMatrix:-dirChannels should return and array of inf's whose size matches the number of b-format channels",
			report
		);

		[
			\kind, 'focus',
			\set,  'FOA',
			\type, 'xformer',
			\op,   'matrix',
			\dim,  3
		].clump(2).do{ |mrPair|
			var method, result;
			#method, result = mrPair;
			this.assertEquals(
				m.perform(method),
				result,
				"FoaXformerMatrix(*newFocus):-% should be %".format(method, result),
				report
			)
		};
	}

	// create an FoaEncoderMatrix from a Matrix directly
	test_fromMatrixMatrixAttributes {
		var m;
		var directions = AtkTests.getDirs('tetra');

		// encoder
		m = FoaEncoderMatrix.newFromMatrix(
			FoaEncoderMatrix.newDirections(directions).matrix
			// set, type are unspecified to test internal inference
		);

		// matrix
		this.assert(
			m.matrix.isKindOf(Matrix),
			"FoaEncoderMatrix:-matrix should return a Matrix",
			report
		);

		// numInputs
		this.assertEquals(
			m.numInputs,
			directions.size,
			"FoaEncoderMatrix(*newFromMatrix):-numInputs should match the number of encoded directions",
			report
		);

		// dirChannels
		this.assertEquals(
			m.dirChannels,
			'unspecified' ! directions.size,
			"FoaEncoderMatrix(*newFromMatrix):-dirChannels should be an array of 'unspecified' of size directions",
			report
		);

		// dirInputs
		this.assertEquals(
			m.dirInputs,
			'unspecified' ! directions.size,
			"FoaEncoderMatrix(*newFromMatrix):-dirInputs should be an array of 'unspecified' of size directions",
			report
		);

		// dirOutputs
		this.assertEquals( // appears assertArrayFloatEquals doens't work on inf's?
			m.dirOutputs,
			inf ! 4, // 4 harmonics for FOA
			"FoaEncoderMatrix(*newFromMatrix):-dirOutputs should return and array of inf's whose size matches the number of b-format channels",
			report
		);

		// dirInputs == dirChannels
		this.assertEquals(
			m.dirInputs,
			m.dirChannels,
			"FoaEncoderMatrix(*newFromMatrix):-dirInputs should equal -dirChannels",
			report
		);

		[
			\kind, 'fromMatrix',
			\set,  'FOA',
			\type, 'encoder',
			\op,   'matrix',
			\dim,  3
		].clump(2).do{ |mrPair|
			var method, result;
			#method, result = mrPair;
			this.assertEquals(
				m.perform(method),
				result,
				"FoaEncoderMatrix(*newFromMatrix):-% should be %".format(method, result),
				report
			)
		};
	}

	// // TODO: complete once .writeToFile method is updated from master
	// test_matrixFileRdWr {
	// 	var m, not, properties, atkMatrix, enc, note, path;
	// 	var directions = AtkTests.getDirs('tetra');
	//
	// 	// start with "raw" A-to-B encoder matrix:
	// 	m = Matrix.with(directions);
	// 	note = "TestFoaMatrix:-test_matrixFileRdWr test";
	//
	// 	// A Dictionary of more metadata to add.
	// 	properties =  (
	// 		    author: "Me, the author",
	// 		    ordering: 'FuMa',
	// 		    normalisation: 'MaxN',
	// 		    dirInputs: directions
	// 	);
	//
	// 	atkMatrix = m.asAtkMatrix('FOA', 'encoder'); // set, type
	//
	// 	path = PathName.tmp++"testA2B_Matrix.yml";
	// 	// be sure to use .yml extension for metadata
	// 	atkMatrix.writeToFile(path, note, properties, overwrite: true);
	// 	1.wait;
	//
	// 	// read encoder back in
	// 	enc = FoaEncoderMatrix.newFromFile(path);
	//
	// 	// compare encoder properties to the original atkMatrix
	// 	[
	// 		\dirChannels,
	// 		\dirInputs,
	// 		\dirOutputs,
	// 	].do{ |p|
	// 		var test, target;
	// 		target = atkMatrix.tryPerform(p);
	// 		test = enc.tryPerform(p);
	// 		"%: %".format(p, [target, test]).postln;
	// 	}
	// }
}