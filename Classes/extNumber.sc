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
// 	Class: LegendreP
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


+ Number {

	/*
	NOTE: these are boost functions which will eventually be supported in the
	SC core library in Number (or elsewhere)
	*/

	// TODO: replace Integer:factorial?
	factorial { _Factorial; ^this.primitiveFailed }
	// TODO: overflow_error handling?
	// factorial {
	//     if (this > 170) {
	//         format("Resolution is too low for this factorial: % (max 170)", this).throw;
	//     } {
	//         ^prFactorial(this)
	//     }
	// }
	//


	/*  Polynomials  */
	// Legendre (and Associated) Polynomials
	//
	// The definition of the associated Legendre polynomial used here includes a leading Condon-Shortley phase term
	// of (-1)m. This matches the definition given by Abramowitz and Stegun (8.6.6) and that used by Mathworld and
	// Mathematica's LegendreP function. However, uses in the literature do not always include this phase term, and
	// strangely the specification for the associated Legendre function in the C++ TR1 (assoc_legendre) also omits it,
	// in spite of stating that it uses Abramowitz and Stegun as the final arbiter on these matters.
	//
	// See:
	// Weisstein, Eric W. "Legendre Polynomial." From MathWorld--A Wolfram Web Resource.
	//
	// Abramowitz, M. and Stegun, I. A. (Eds.). "Legendre Functions" and "Orthogonal Polynomials." Ch. 22 in Chs. 8
	// and 22 in Handbook of Mathematical Functions with Formulas, Graphs, and Mathematical Tables, 9th printing.
	// New York: Dover, pp. 331-339 and 771-802, 1972.
	//
	// "http://www.boost.org/doc/libs/1_65_1/libs/math/doc/html/math_toolkit/sf_poly/legendre.html"]
	//
	// TODO: where to clip values at [-1,1] ??
	legendre { |x| _LegendreP; ^this.primitiveFailed }
	legendrePrime { |x| _LegendrePPrime; ^this.primitiveFailed }
	legendreZeros { _LegendrePZeros; ^this.primitiveFailed }
	legendreAssoc { |m, x| _LegendrePAssoc; ^this.primitiveFailed }
	// TODO: proper way to catch error?
	legendre2 { |x|
		if (this < 0) {
			format("n = %, but Legendre Polynomial of the Second Kind requires n >= 0", this).throw
		};
		^prLegendre2(this, x)
	}
	// TODO: name?
	prLegendre2 { |x| _LegendreQ; ^this.primitiveFailed }

	// Chebyshev polynomials - first, second kind & derivative
	chebyshev { |x| _ChebyshevT; ^this.primitiveFailed }
	chebyshev2 { |x| _ChebyshevU; ^this.primitiveFailed }
	chebyshevPrime { |x| _ChebyshevTPrime; ^this.primitiveFailed }

	// calculate the roots (zeros) of Chebyshev polynomial
	// "https://en.wikipedia.org/wiki/Chebyshev_polynomials#Roots_and_extrema"
	// "http://mathworld.wolfram.com/ChebyshevPolynomialoftheFirstKind.html"
	chebyZeros {
		var n = this.asInt;
		^(1..n).collect({ arg k;
			cos(pi* ((2*k) - 1) / (2*n))
		});
	}

	chebyMaxZero {
		var n = this.asInt;
		^cos(pi * (2*n).reciprocal)
	}

	// Spherical Harmonics

    sphHarm { |m, theta, phi| _SphHarmComplex; ^this.primitiveFailed }
    sphHarmRe { |m, theta, phi| _SphHarmReal; ^this.primitiveFailed }
    sphHarmImag { |m, theta, phi| _SphHarmImag; ^this.primitiveFailed }


	/*  Basic Functions  */

	sinpi { _SinPi; ^this.primitiveFailed }
	cospi { _CosPi; ^this.primitiveFailed }

	/*  Sinus Cardinal ("sinc") and Hyperbolic Sinus Cardinal Functions  */

	// sin(x) / x
	sinc { _SinCpi; ^this.primitiveFailed }
	// sinh(x) / x
	sinhc { _SinHCpi; ^this.primitiveFailed }
}
