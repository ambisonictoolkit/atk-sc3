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
// 	Class: TestFoaMatrix
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
			"FoaEncoderMatrix:-dirOutputs should return and array of "
			"inf's whose size matches the number of b-format channels",
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
		].clump(2).do({ |mrPair|
			var method, result;

			#method, result = mrPair;
			this.assertEquals(
				m.perform(method),
				result,
				"FoaEncoderMatrix(*newDirections):-% should be %".format(method, result),
				report
			)
		})
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
			[45.0, 135.0, -135.0, -45.0].degrad, // quad decoder
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
			"FoaDecoderMatrix:-dirInputs should return and array of inf's "
			"whose size matches the number of b-format channels",
			report
		);

		// shelfK - for dual band decoder
		this.assertArrayFloatEquals(
			m.shelfK,
			[1.2247448713916, 0.86602540378444],
			"FoaDecoderMatrix:-shelfK should should be [1.2247448713916, 0.86602540378444] (default )",
			floatWithin, report
		);

		[
			\kind, 'quad',
			\set,  'FOA',
			\type, 'decoder',
			\op,   'matrix',
			\dim,  2,
			\shelfFreq, 400.0  // default shelfK
		].clump(2).do({ |mrPair|
			var method, result;

			#method, result = mrPair;
			this.assertEquals(
				m.perform(method),
				result,
				"FoaDecoderMatrix(*newQuad, k:'dual'):-% should be %".format(method, result),
				report
			)
		})
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

		// dirOutputs, dirInputs, dirChannels
		[\dirOutputs, \dirInputs, \dirChannels].do({ |attribute|
			this.assertEquals(  // appears assertArrayFloatEquals doens't work on inf's?
				m.perform(attribute),
				inf ! 4,
				"FoaXformerMatrix:-% should return and array of inf's whose size "
				"matches the number of b-format channels".format(attribute),
				report
			)
		});

		[
			\kind, 'focus',
			\set,  'FOA',
			\type, 'xformer',
			\op,   'matrix',
			\dim,  3
		].clump(2).do({ |mrPair|
			var method, result;

			#method, result = mrPair;
			this.assertEquals(
				m.perform(method),
				result,
				"FoaXformerMatrix(*newFocus):-% should be %".format(method, result),
				report
			)
		})
	}

	// create an FoaEncoderMatrix from a Matrix directly
	test_fromMatrixMatrixAttributes {
		var m;
		var directions = AtkTests.getDirs('tetra');

		// encoder
		m = FoaEncoderMatrix.newFromMatrix(
			FoaEncoderMatrix.newDirections(directions).matrix,
			directions
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
			"FoaEncoderMatrix(*newFromMatrix):-numInputs should "
			"match the number of encoded directions",
			report
		);

		// dirChannels, dirInputs
		[\dirChannels, \dirInputs].do({ |attribute|
			this.assertEquals(
				m.perform(attribute),
				directions,
				"FoaEncoderMatrix(*newFromMatrix):-% should be an array "
				"of 'unspecified' of size directions".format(attribute),
				report
			)
		});

		// dirOutputs
		this.assertEquals( // appears assertArrayFloatEquals doens't work on inf's?
			m.dirOutputs,
			inf ! 4, // 4 harmonics for FOA
			"FoaEncoderMatrix(*newFromMatrix):-dirOutputs should return and array "
			"of inf's whose size matches the number of b-format channels",
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
		].clump(2).do({ |mrPair|
			var method, result;

			#method, result = mrPair;
			this.assertEquals(
				m.perform(method),
				result,
				"FoaEncoderMatrix(*newFromMatrix):-% should be %".format(method, result),
				report
			)
		})
	}

	test_matrixFileRdWr {
		var not, properties, atkMatrix, enc, note, path;
		var directions = AtkTests.getDirs('tetra');
		var att, orig, fromFile;

		// start with "raw" A-to-B encoder matrix:
		note = "TestFoaMatrix:-test_matrixFileRdWr test";

		// A Dictionary of more metadata to add.
		properties =  (
			author: "Me, the author",
			ordering: 'FuMa',
			normalisation: 'MaxN',
			dirInputs: directions
		);

		// atkMatrix = m.asAtkMatrix('FOA', 'encoder'); // set, type
		atkMatrix = FoaEncoderMatrix.newDirections(directions);

		path = PathName.tmp++"testA2B_Matrix.yml";
		// be sure to use .yml extension for metadata
		atkMatrix.writeToFile(path, note, properties, overwrite: true);
		1.wait;

		// read encoder back in from a file
		enc = FoaEncoderMatrix.newFromFile(path);

		// compare properties of the encoder loaded from file to
		// properties of the original atkMatrix
		[\kind, \set, \type, \op, \dim].do({ |p|
			orig = atkMatrix.tryPerform(p);
			fromFile = enc.tryPerform(p);
			this.assertEquals(
				fromFile, orig,
				"FoaEncoderMatrix(*newFromFile):-% should match the source "
				"FoaEncoderMatrix that was written to file. '%' was written, "
				"'%' was read and loaded".format(p, orig, fromFile),
				report
			)
		});

		// .matrix
		this.assertArrayFloatEquals(
			enc.matrix.asArray.flat, atkMatrix.matrix.asArray.flat,
			"FoaEncoderMatrix(*newFromFile):-matrix should match the "
			"matrix of the source FoaEncoderMatrix.",
			floatWithin, report
		);

		// user-defined attributes
		properties.keys.do({ |a|
			orig = properties[a];
			fromFile = enc.fileParse[a]
		});

		// These properties may differ, as they're "unspecified" when created, but the
		// attributeDictionary may overwrite them (as in dirIniputs, in this case)

		// .dirInputs
		this.assertArrayFloatEquals(
			enc.dirInputs.flat, properties.dirInputs.flat,
			"FoaEncoderMatrix(*newFromFile):-dirInputs should match the dirInputs property "
			"assigned in the attributeDictionary of the source FoaEncoderMatrix.",
			floatWithin, report
		);

		// .dirChannels
		this.assertArrayFloatEquals(enc.dirChannels.flat, properties.dirInputs.flat,
			"FoaEncoderMatrix(*newFromFile):-dirChannels should match the dirInputs property "
			"assigned in the attributeDictionary of the source FoaEncoderMatrix.",
			floatWithin, report
		);

		// .dirOutputs
		this.assertEquals(  // appears assertArrayFloatEquals doens't work on inf's?
			enc.dirOutputs,
			inf ! 4,        // 4 harmonics for FOA
			"FoaEncoderMatrix(*newFromFile):-dirOutputs should return and array of inf's whose "
			"size matches the number of b-format channels",
			report
		);
	}
}
