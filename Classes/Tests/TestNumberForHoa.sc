TestNumberForHoa : UnitTest {
	var report, floatWithin;

	setUp {
		report = AtkTests.report;
		floatWithin = AtkTests.floatWithin;
	}

	test_sphericalHarmonic {
		var t;
		t = sphericalHarmonic(3, 2, 30.degrad, 15.degrad);

		this.assert(t.isKindOf(Complex),
			"sphericalHarmonic should return a Complex number",
			report
		);

		this.assertArrayFloatEquals(
			t.real, 0.19162227683124,
			"sphericalHarmonic should report correct real component",
			floatWithin, report
		);

		this.assertArrayFloatEquals(
			t.imag, 0.11063317311125,
			"sphericalHarmonic should report correct imaginary component",
			floatWithin, report
		);
	}

}