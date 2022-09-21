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
		var getPVcFromPVt;
		var pvt, pvf, pva;

		getPVcFromPVt = { |pvt|
			// test signals
			pvf = PVf.newFromPVt(pvt); // freq-domain
			pva = PVa.newFromPVt(pvt); // time-domain (analytic)
			^[pvf, pva]
		};

		// time-domain signal (real): sinusoidal planewave
		pvt = PVT.newCosineTravelling(
			sigSize, freq, phase, azEl[0], azEl[1], sampleRate:48e3
		);


		// p = PUT.newSineDiffuseAngular(128, 3300, pi/6.3, -0.123, 0.123, design: TDesign.newHoa(order: 1), sampleRate:48e3)
		// p = PUT.newCosineDiffuseModal(128, 3300, pi/6.3, 48e3);

	}

}