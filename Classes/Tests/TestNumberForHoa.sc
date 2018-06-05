TestNumberForHoa : UnitTest {

	test_sphericalHarmonic {
		var t;
		t = sphericalHarmonic(3, 2, 30.degrad, 15.degrad);
		// this.assertEquals(t, Complex( 0.19162227683124, 0.11063317311125 ), "sphericalHarmonic should return a Complex number");
		this.assert(t.isKindOf(Complex), "sphericalHarmonic should return a Complex number");
		this.assertArrayFloatEquals(t.real, 0.19162227683124, "sphericalHarmonic should report correct real component");
		this.assertArrayFloatEquals(t.imag, 0.11063317311125, "sphericalHarmonic should report correct imaginary component");
	}

}