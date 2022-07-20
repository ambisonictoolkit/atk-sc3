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
// 	Class: TestHoaMatrixXformer
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

TestWaveNumber : UnitTest {
	var order, report, floatWithin;
	var dataDir, numFailuresToExit;

	setUp {
		order = AtkTests.order;
		report = AtkTests.report;
		floatWithin = AtkTests.floatWithin;

		dataDir = PathName(AtkTests.filenameSymbol.asString).parentLevelPath(1) +/+ "TestData";
		// When checking many values in reference file, just exit after numerous failures
		numFailuresToExit = 5;
	}

	// A method for calculating proxWeights for both writing
	// the reference values and testing the method
	calcProxWeights {
		var orders = [2,5,10];
		var freqs = [0.0] ++ (15 * 4.pow((0..5)));
		var dists = [0.001] ++ geom(6, 0.05, 3);
		var sos = AtkHoa.speedOfSound;
		var res = []; // collect results in flat structure for reading/writing

		orders.do{ |m|
			var order = HoaOrder(m);

			freqs.do{ |f|
				dists.do{ |d|
					var w = order.distWeights(f, d, sos);
					// extract re and im, re-interleave
					res = res.add( [w.real, w.imag].lace(size(w)*2) )
				}
			}
		};
		^res
	}

	// To write reference to file:
	// TestWaveNumber().setUp.writeProxWeights
	writeProxWeights {
		var wr = CSVFileWriter(dataDir +/+ "proxWeights.txt");

		this.calcProxWeights.do{ |prox, i|
			wr.writeLine(prox);
		};
		wr.close;
	}

	test_proxWeights {
		var ref = CSVFileReader.readInterpret(dataDir +/+ "proxWeights.txt", true, true);
		var pass, failCnt = 0;

		this.calcProxWeights.do{ |prox, i|
			pass = this.assertArrayFloatEquals(
				prox, ref[i],
				format("proxWeights should match reference values."),
			floatWithin, report);

			pass.not.if{ failCnt = failCnt + 1};

			(failCnt > numFailuresToExit).if{
				"Exiting test_proxWeights after % failures".format(numFailuresToExit).warn;
				^nil
			}
		};
	}

}
