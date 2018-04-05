// Test for Hoa Matrices:
// HoaMatrix
// HoaEncoderMatrix
// HoaDecoderMatrix
// HoaEncoderMatrix

// Helper class to run tests across realted Test classes.
TestHoaMatrix {
	// shared assertions among all subclasses of HoaMatrix
	// called from tests within those subclasses
	*assertHoaMatrixProperties { |test, m|
		var report = HoaTests.report;
		var order = HoaTests.order;
		var floatWithin = HoaTests.floatWithin;

		test.assertEquals(m.set, ('HOA'++order).asSymbol, "HoaMatrix -set should be 'HOA'++order", report);
		test.assertEquals(m.op, 'matrix', "HoaMatrix -op should be 'matrix'", report);
		test.assert(m.matrix.isKindOf(Matrix), "HoaMatrix -matrix should be of kind Matrix", report);
		test.assertEquals(m.matrix.rows, (order+1).squared, "HoaMatrix should create a Matrix with (order+1).squared rows", report);
		test.assertEquals(m.numInputs, m.matrix.cols, "HoaMatrix -numInputs == -matrix.cols", report);
		test.assertEquals(m.numOutputs, m.matrix.rows, "HoaMatrix -numOutputs == -matrix.rows", report);
		test.assertEquals(m.numChannels, m.matrix.cols, "HoaMatrix -numChannels == -matrix.cols", report);
	}

}

TestHoaEncoderMatrix : UnitTest {
	var order, report, floatWithin;

	setUp {
		order = HoaTests.order;
		report = HoaTests.report;
		floatWithin = HoaTests.floatWithin;
	}


	// shared assertions among all HoaEncoderMatrix instances
	assertHoaEncoderMatrixProperties { |m|
		this.assertEquals(m.dirOutputs, inf.dup((order+1).squared), "HoaEncoderMatrix -dirOutputs == inf.dup((order+1).squared", report);

		// TODO: check size/shape/dim of e.dirInputs, e.dim
		// should dirInputs be set to [ theta ] only if phi == 0? why not [[0,0]],
		// all encoding is 3D anyway?
		// this.assertEquals(e.dirInputs, [dirs], "HoaEncoderMatrix:newDirection should have dirInputs matching the input's theta, phi", report);
		// this.assert(e.dirInputs.shape == [1,2], "HoaEncoderMatrix:newDirection dirInputs shape should be [1,2]: [[theta, phi]]", report);
		// this.assertEquals(e.dim, 3, "HoaEncoderMatrix:newDirection dim should be 3", report);
	}

	test_newDirection {
		var e, dirs=[0,0];

		// planewave encoding
		e = HoaEncoderMatrix.newDirection(dirs[0], dirs[1], order:order);

		this.assertEquals(e.kind, 'dir', "HoaEncoderMatrix:newDirection should be kind 'dir'", report);

		// TestHoaMatrix.assertHoaMatrixProperties(this, e);
		this.assertHoaEncoderMatrixProperties(e);

	}

	test_newDirections {
		var e, dirs, numDirs=5;

		// planewave encoding
		dirs = numDirs.collect{[rrand(-2pi,2pi), rrand(-2pi,2pi)]};
		e = HoaEncoderMatrix.newDirections(dirs, order:order);

		this.assertEquals(e.kind, 'dirs', "HoaEncoderMatrix:newDirections should be kind 'dirs'", report);

		TestHoaMatrix.assertHoaMatrixProperties(this, e);
		this.assertHoaEncoderMatrixProperties(e);
	}
}

TestHoaDecoderMatrix : UnitTest {
	var order, report, floatWithin;

	setUp {
		order = HoaTests.order;
		report = HoaTests.report;
		floatWithin = HoaTests.floatWithin;
	}

	// shared assertions among all HoaEncoderMatrix instances
	assertHoaDecoderMatrixProperties { |m|
		this.assertEquals(m.dirInputs, inf.dup((order+1).squared), "HoaDecoderMatrix -dirInputs == inf.dup((order+1).squared", report);

		// TODO: check size/shape/dim of e.dirInputs, e.dim
		// should dirInputs be set to [ theta ] only if phi == 0? why not [[0,0]],
		// all encoding is 3D anyway?
		// this.assertEquals(e.dirInputs, [dirs], "HoaEncoderMatrix:newDirection should have dirInputs matching the input's theta, phi", report);
		// this.assert(e.dirInputs.shape == [1,2], "HoaEncoderMatrix:newDirection dirInputs shape should be [1,2]: [[theta, phi]]", report);
		// this.assertEquals(e.dim, 3, "HoaEncoderMatrix:newDirection dim should be 3", report);
	}

	test_newFormat {
		var d, format;

		format = [\acn, \n3d];
		d = HoaDecoderMatrix.newFormat(format, order: order);

		this.assertEquals(d.kind, 'formatt', "HoaDecoderMatrix:format should be kind 'format'", report);
		this.assertEquals(d.dirOutputs, inf.dup((order+1).squared), "HoaDecoderMatrix:format -dirOutputs == inf.dup((order+1).squared", report);
		this.assertEquals(d.matrix.rows, d.matrix.cols, "HoaDecoderMatrix:format should be a square matrix", report);

		TestHoaMatrix.assertHoaMatrixProperties(this, d);
		this.assertHoaDecoderMatrixProperties(d);
	}
}