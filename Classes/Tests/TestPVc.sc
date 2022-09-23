/*
Copyright the ATK Community and Joseph Anderson, 2011-2022
J Anderson  j.anderson[at]ambisonictoolkit.net
M McCrea

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
// 	Class: TestPVC
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

TestPVc : UnitTest {
	var report, floatWithin;

	setUp {
		report = AtkTests.report;
		floatWithin = AtkTests.floatWithin;
	}

	// A number
	test_PVf_PVa_equivalence {
		// test signal:
		var fs = 48e3;
		var sigSize = 128;
		var freq = rrand(fs/sigSize, fs/4.0);
		var phase = rrand(pi/8, pi/2.1);
		var azEl = 2.collect{ pi.bilinrand };
		var getPVcFromPVt, runMethods, methods;
		var pvt, pvf, pva;
		// values consider equal if within this percent of mean
		var equalityFac = 0.01; // i.e. 0.01 is equal within 1%
		var zeroBelow = 1e-04;

		getPVcFromPVt = { |pvt|
			[
				PVf.newFromPVt(pvt), // freq-domain
				PVa.newFromPVt(pvt)  // time-domain (analytic)
			]
		};

		runMethods = { |methods, pvfarg, pvaarg, caseName|
			methods.do{ |method|
				var val1, val2, val1flat, val2flat;
				var valmean, tolerance;
				var test1, test2;

				val1 = pvfarg.perform(method);
				val2 = pvaarg.perform(method);

				case
				{ val1.isKindOf(Array)} { // quantities are an array
					val1flat = val1.flat;
					val2flat = val2.flat;
					// flatten re, im components
					if (val1flat.first.isKindOf(Complex)) {
						val1flat = (val1flat.collect(_.real).flat ++ val1flat.collect(_.imag).flat);
						val2flat = (val2flat.collect(_.real).flat ++ val2flat.collect(_.imag).flat);
					};
				} { val1.isKindOf(Complex)} {
					val1flat = [val1.real, val1.imag].flat;
					val2flat = [val2.real, val2.imag].flat;
				};

				if (val1flat.notNil) {
					// quantities are arrays
					valmean = val1flat.collect{ |val, i| val + val2flat[i] / 2 };
					tolerance = (valmean * equalityFac).abs.max(zeroBelow);
					test1 = val1flat.every({ |val, i| (val - valmean[i]).abs < tolerance[i] });
					test2 = val2flat.every({ |val, i| (val - valmean[i]).abs < tolerance[i] });
				} {
					// quantities are single values
					valmean = [val1, val2].mean;
					tolerance = (valmean * equalityFac).abs.max(zeroBelow);
					test1 = (val1 - valmean).abs < tolerance;
					test2 = (val2 - valmean).abs < tolerance;
				};

				this.assert(
					test1 and: test2,
					"\n  [PVf/PVa:%] differ beyond the %\\% tolerance:\n\t%\n\tvs.\n\t%\n(% case)\n".format(
						method, equalityFac * 100, val1, val2, caseName
					), report
				);
			}
		};

		// Methods identical between PVf and PVa.
		// Some methods are ommitted for the plane-wave test case:
		// (\averageGamma) and the reactive incidence angle (\averageThetaPhi) will fail
		// when the reactive component is (near) zero.
		methods = [
			\totalWp, \totalWu, \totalWs, \totalWd, \totalWh,
			\totalMagMagI, \totalMagI,
			\totalMagMagA, \totalMagA,
			\totalMagMagW, \totalMagW,
			\totalMagMagN, \totalMagN,
			\totalI, \totalA, \totalW, \totalN,
			\averageWp, \averageWu, \averageWs, \averageWd, \averageWh,
			\averageMagMagI, \averageMagI,
			\averageMagMagA, \averageMagA,
			\averageMagMagW, \averageMagW,
			\averageMagMagN, \averageMagN,
			\averageI, \averageA, \averageW, \averageN,
			\averageAlpha, \averageBeta, \averageMu,
			// \averageGamma, \averageThetaPhi // don't test when no reactive field
			// \totalRadius, \averageRadius, // radius methods aren't shared
		];


		/* Test sinusoidal plane-wave signal */

		// generate time-domain signal
		pvt = PVt.newCosineTravelling(
			sigSize, freq, phase, azEl[0], azEl[1], sampleRate:48e3
		);
		// convert to types under test
		#pvf, pva = getPVcFromPVt.(pvt);
		equalityFac = 0.01; // 1%
		// run tests for planewave signal
		runMethods.(methods, pvf, pva, "newCosineTravelling");


		/* Test sinusoidal (monotonic) diffuse field signal */
		// TODO: every so often these will fail, even with quite high tolerance

		// generate time-domain signal
		pvt = PVt.newSineDiffuseAngular(
			sigSize, freq, phase, design: TDesign.newHoa(order: 1), sampleRate:48e3
		);
		// convert to types under test
		#pvf, pva = getPVcFromPVt.(pvt);

		// higher tolerance for diffuse field, not ideal...
		equalityFac = 0.15; // 15%

		// run tests for monotonic  diffuse field signal
		// Tests added which should return correct values for the non-zero reactive
		// component of a diffuse field.
		runMethods.(
			methods ++ [\averageGamma, \averageThetaPhi],
			pvf, pva, "newSineDiffuseAngular")

	}

}