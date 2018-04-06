TestNumberForHoa : UnitTest {
	var order, report, floatWithin;

	setUp {
		order = HoaTests.order;
		report = HoaTests.report;
		floatWithin = HoaTests.floatWithin;
	}

	test_sphericalHarmonic {
		var t;
		t = sphericalHarmonic(3, 2, 30.degrad, 15.degrad);
		this.assert(t.isKindOf(Complex), "sphericalHarmonic should return a Complex number", report: report);
		this.assertFloatEquals(t.real, 0.19162227683124, "sphericalHarmonic should report correct real component", within: floatWithin, report: report);
		this.assertFloatEquals(t.imag, 0.11063317311125, "sphericalHarmonic should report correct imaginary component", within: floatWithin, report: report);
	}

}