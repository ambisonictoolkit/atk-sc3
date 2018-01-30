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
// 	Class: PolyDegree
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
// Polynomial Utilities for HOA

PolyDegree {
    var <>degree;

    *new { arg degree;
        ^super.newCopyArgs(degree)
    }

    // evaluate Chebyshev polynomial
    //
    // "https://en.wikipedia.org/wiki/Chebyshev_polynomials#Explicit_expressions"
    // "http://mathworld.wolfram.com/ChebyshevPolynomialoftheFirstKind.html"
    chebyEval { arg x;
        var n = this.degree;
        ^cos(n * acos(x))
    }

    // calculate the roots (zeros) of Chebyshev polynomial
    //
    // "https://en.wikipedia.org/wiki/Chebyshev_polynomials#Roots_and_extrema"
    // "http://mathworld.wolfram.com/ChebyshevPolynomialoftheFirstKind.html"
    chebyZeros {
        var n = this.degree;
        ^(1..n).collect({ arg k;
            cos(pi* ((2*k) - 1) / (2*n))
        });
    }

    chebyMaxZero {
        var n = this.degree;
        ^cos(pi * (2*n).reciprocal)
    }

    // evaluate Associated Legendre polynomial
    //
    // use LegrendP _from boost_
    assocLegendreEval { arg order, x;
        ^LegendreP.new(this.degree).assocEval(order, x)
    }

    // evaluate Legendre polynomial
    //
    // use LegrendP _from boost_
    legendreEval { arg x;
        ^LegendreP.new(this.degree).eval(x)
    }

    // Legendre polynomial root (zeros) approximation
    //
    // NOTE: Use the Boost implementation legendre_p_zeros. See LegendreP above.
    //
    // // use LegrendP _from boost_
    // legendreZeros {
    //     ^LegendreP.new(this.degree).zeros
    // }
    //
    // // use LegrendP _from boost_
    // legendreMaxZero {
    //     ^LegendreP.new(this.degree).maxZero
    // }
    //
    // FOR NOW, USE:
    //
    // "https://math.stackexchange.com/questions/12160/roots-of-legendre-polynomial"
    //
    // A less accurate approximation can be found in
    // Press, et al, use that approximation in their book "Numerical Recipes"
    // source code for Gaussian-Legendre quadrature (chapter 4, 4.5), page 152
    // "https://www2.units.it/ipl/students_area/imm2/files/Numerical_Recipes.pdf"
    // found via "http://mathforum.org/kb/message.jspa?messageID=6474874"
    //
    // See also:
    // TABLE OF THE ZEROS OF THE LEGENDRE POLYNOMIALS
    // OF ORDER 1-16 AND THE WEIGHT COEFFICIENTS
    // FOR GAUSS' MECHANICAL QUADRATURE FORMULA
    // ARNOLD N. LOWAN, NORMAN DAVIDS AND ARTHUR LEVENSON
    // "http://kfe.fjfi.cvut.cz/~klimo/nm/c8/legpoly.pdf"
    //
    // Note: accuracy improves (relative to numpy) with ascending degree
    legendreZeros {
        var n = this.degree;

        ^(1..degree).collect({ arg k; // k = root number
            (1 - (8*n.pow(2)).reciprocal + (8*n.pow(3)).reciprocal) * (cos(pi * ((4*k - 1)/(4*n + 2))))
            // cos(pi * (4*k +3) / (4*n+2))
        })
    }

    legendreMaxZero { arg degree = 3;
        var n = this.degree;

        ^(1 - (8*n.pow(2)).reciprocal + (8*n.pow(3)).reciprocal) * (cos(pi * (3/(4*n + 2))))
        // cos(pi * (4+3) / (4*n+2))
        // (n-1) * ((n+2)/(n*(n.squared+2))).sqrt
    }

    // evaluate Reverse Bessel polynomial
    //
    rBesselEval { arg x;
        var n = this.degree;
        var coeffs;

        coeffs = (n+1).collect({ arg k;
            ((2*n) - k).floatFactorial / (pow(2, n-k)*k.floatFactorial*(n-k).floatFactorial)
        });

        ^Polynomial.newFrom(coeffs).eval(x)
    }

    // calculate the roots (zeros) of Reverse Bessel polynomial
    //
    rBesselZeros {
        var n = this.degree;
        var coeffs, method = 'eigenvalue';

        coeffs = (n+1).collect({ arg k;
            ((2*n) - k).floatFactorial / (pow(2, n-k)*k.floatFactorial*(n-k).floatFactorial)
        });

        ^Polynomial.newFrom(coeffs).findRoots(method)
    }

    rBesselMaxZero {
        ^this.rBesselZeros.maxItem
        // ^this.rBesselZeros.sort({ arg a, b; a > b }).maxItem
    }
}