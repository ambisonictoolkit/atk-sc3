TestHoaEncoderMatrix : HoaTest {

	test_newDirection {
		var e;
		// planewave encoding, coeffs normalized to 1
		e = HoaEncoderMatrix.newDirection(0,0,order:3);
	}
}