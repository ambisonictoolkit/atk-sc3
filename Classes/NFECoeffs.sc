/*
	Copyright the ATK Community, Joseph Anderson, and Michael McCrea, 2018
		J Anderson	j.anderson[at]ambisonictoolkit.net


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
// 	Class: NFECoeffs
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
//
//	We hope you enjoy the ATK!
//
//	For more information visit http://ambisonictoolkit.net/ or
//	email info[at]ambisonictoolkit.net
//
//---------------------------------------------------------------------


//------------------------------------------------------------------------
// NFE - Near-field Effect, coefficients

NFECoeffs {
	var <degree, <reX, <absX, <numSOS, <numFOS;

	*new { |degree|
		^super.newCopyArgs(degree).init
	}

	init {
		var m = this.degree;
		var method = \eigenvalue;
		var bpr;

		// Bessel Polynomial Zeros
		bpr = Polynomial.newReverseBessel(m).findRoots(method);
		bpr = bpr.sort.reverse;  // sorted so real is at end

		// extract Re(X) & |X|, e.g., Bessel Polynomial Factors

		// average of real
		reX = ((m/2).asInteger).collect({ |q|
			(bpr.at(2 * q).real + bpr.at(2 * q + 1).real) / 2
		});

		// average of magnitude
		absX = ((m/2).asInteger).collect({ |q|
			(bpr.at(2 * q).abs + bpr.at(2 * q + 1).abs) / 2
		});

		// odd degree?
		m.odd.if{
			reX = reX ++ Array.with(bpr.last.real);
		};

		// set number of SOS & FOS
		numSOS = absX.size;
		numFOS = reX.size - numSOS;
	}

	prox { |radius, sampleRate, speedOfSound = (AtkHoa.speedOfSound)|
		var r0 = radius;
		var mOdd;
		var alpha;
		var coeffs, g;
		var coeffsSOS, coeffsFOS;

		mOdd = this.degree.odd;

		alpha = 2 * sampleRate * r0/speedOfSound;

		coeffs = numSOS.collect({ |q|
			var c1, c2;

			c1 = this.reX.at(q)/alpha;
			c2 = (this.absX.at(q)/alpha).squared;

			[
				// numerator
				1 - (2 * c1) + c2,
				-2 * (1 - c2),
				1 + (2 * c1) + c2,
				// denominator
				1, -2, 1
			]
		});

		// odd degree? - add coeffs for FOS
		mOdd.if{
			var c1;
			c1 = this.reX.last/alpha;

			coeffs = coeffs ++ [
				[
					// numerator
					1 - c1,
					-1 * (1 + c1),
					0,
					// denominator
					1, -1, 0
				]
			]
		};

		// factor down to simple SOS & FOS coeffs + gain
		g = 1.0;

		(this.numSOS + this.numFOS).do({ |q|
			var g0;

			g0 = coeffs.at(q).at(0);
			coeffs.put(q,
				(coeffs.at(q).copyRange(0, 2) / g0) ++ coeffs.at(q).copyRange(3, 5)
			);
			g = g0 * g
		});

		// parse coeffs into form usable by SOS & FOS UGens
		// NOTE: this could happen in the coefficient generation, above
		coeffsSOS = this.numSOS.collect({ |q|
			coeffs.at(q).copyRange(0, 2) ++ (-1 * coeffs.at(q).copyRange(4, 5))
		});

		// odd degree? - add coeffs for FOS
		mOdd.if{
			coeffsFOS = [
				coeffs.last.copyRange(0, 1) ++ (-1 * [coeffs.last.at(4)])
			]
		};

		^Dictionary.with(*[\sos->coeffsSOS, \fos->coeffsFOS, \g->g])
	}

	dist { |radius, sampleRate, speedOfSound = (AtkHoa.speedOfSound)|
		var r1 = radius;
		var mOdd;
		var alpha;
		var coeffs, g;
		var coeffsSOS, coeffsFOS;

		mOdd = this.degree.odd;

		alpha = 2 * sampleRate * r1/speedOfSound;

		coeffs = numSOS.collect({ |q|
			var c1, c2;

			c1 = this.reX.at(q)/alpha;
			c2 = (this.absX.at(q)/alpha).squared;

			[
				// numerator
				1, -2, 1,
				// denominator
				1 - (2 * c1) + c2,
				-2 * (1 - c2),
				1 + (2 * c1) + c2
			]
		});

		// odd degree? - add coeffs for FOS
		mOdd.if{
			var c1;
			c1 = this.reX.last/alpha;

			coeffs = coeffs ++ [
				[
					// numerator
					1, -1, 0,
					// denominator
					1 - c1,
					-1 * (1 + c1),
					0
				]
			]
		};

		// factor down to simple SOS & FOS coeffs + gain
		g = 1.0;

		(this.numSOS + this.numFOS).do({ |q|
			var g0;

			g0 = coeffs.at(q).at(3).reciprocal;
			coeffs.put(q,
				coeffs.at(q).copyRange(0, 2) ++ (coeffs.at(q).copyRange(3, 5) * g0)
			);
			g = g0 * g
		});

		// parse coeffs into form usable by SOS & FOS UGens
		// NOTE: this could happen in the coefficient generation, above
		coeffsSOS = this.numSOS.collect({ |q|
			coeffs.at(q).copyRange(0, 2) ++ (-1 * coeffs.at(q).copyRange(4, 5))
		});

		// odd degree? - add coeffs for FOS
		mOdd.if{
			coeffsFOS = [
				coeffs.last.copyRange(0, 1) ++ (-1 * [coeffs.last.at(4)])
			]
		};

		^Dictionary.with(*[\sos->coeffsSOS, \fos->coeffsFOS, \g->g])
	}

	ctrl { |encRadius, decRadius, sampleRate, speedOfSound = (AtkHoa.speedOfSound)|
		var r0 = encRadius;
		var r1 = decRadius;
		var mOdd;
		var alpha0, alpha1;
		var coeffs, g;
		var coeffsSOS, coeffsFOS;

		mOdd = this.degree.odd;

		alpha0 = 2 * sampleRate * r0/speedOfSound;  // proximity
		alpha1 = 2 * sampleRate * r1/speedOfSound;  // distance

		coeffs = numSOS.collect({ |q|
			var c10, c20, c11, c21;

			// proximity
			c10 = this.reX.at(q)/alpha0;
			c20 = (this.absX.at(q)/alpha0).squared;

			// distance
			c11 = this.reX.at(q)/alpha1;
			c21 = (this.absX.at(q)/alpha1).squared;

			[
				// numerator
				1 - (2 * c10) + c20,
				-2 * (1 - c20),
				1 + (2 * c10) + c20,
				// denominator
				1 - (2 * c11) + c21,
				-2 * (1 - c21),
				1 + (2 * c11) + c21
			]
		});

		// odd degree? - add coeffs for FOS
		mOdd.if{
			var c10, c11;
			c10 = this.reX.last/alpha0;  // proximity
			c11 = this.reX.last/alpha1;  // distance

			coeffs = coeffs ++ [
				[
					// numerator
					1 - c10,
					-1 * (1 + c10),
					0,
					// denominator
					1 - c11,
					-1 * (1 + c11),
					0
				]
			]
		};

		// factor down to simple SOS & FOS coeffs + gain
		g = 1.0;

		(this.numSOS + this.numFOS).do({ |q|
			var g0;

			// distance
			g0 = coeffs.at(q).at(3).reciprocal;
			coeffs.put(q,
				coeffs.at(q).copyRange(0, 2) ++ (coeffs.at(q).copyRange(3, 5) * g0)
			);
			g = g0 * g;

			// proximity
			g0 = coeffs.at(q).at(0);
			coeffs.put(q,
				(coeffs.at(q).copyRange(0, 2) / g0) ++ coeffs.at(q).copyRange(3, 5)
			);
			g = g0 * g
		});

		// parse coeffs into form usable by SOS & FOS UGens
		// NOTE: this could happen in the coefficient generation, above
		coeffsSOS = this.numSOS.collect({ |q|
			coeffs.at(q).copyRange(0, 2) ++ (-1 * coeffs.at(q).copyRange(4, 5))
		});

		// odd degree? - add coeffs for FOS
		mOdd.if{
			coeffsFOS = [
				coeffs.last.copyRange(0, 1) ++ (-1 * [coeffs.last.at(4)])
			]
		};

		^Dictionary.with(*[\sos->coeffsSOS, \fos->coeffsFOS, \g->g])
	}
}
