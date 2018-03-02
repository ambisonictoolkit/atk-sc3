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


//------------------------------------------------------------------------
// Simple class to import Boost Legendre (and Associated) Polynomials functionality

/*
The definition of the associated Legendre polynomial used here includes a leading Condon-Shortley phase term
of (-1)m. This matches the definition given by Abramowitz and Stegun (8.6.6) and that used by Mathworld and
Mathematica's LegendreP function. However, uses in the literature do not always include this phase term, and
strangely the specification for the associated Legendre function in the C++ TR1 (assoc_legendre) also omits it,
in spite of stating that it uses Abramowitz and Stegun as the final arbiter on these matters.

See:

Weisstein, Eric W. "Legendre Polynomial." From MathWorld--A Wolfram Web Resource.

Abramowitz, M. and Stegun, I. A. (Eds.). "Legendre Functions" and "Orthogonal Polynomials." Ch. 22 in Chs. 8
and 22 in Handbook of Mathematical Functions with Formulas, Graphs, and Mathematical Tables, 9th printing.
New York: Dover, pp. 331-339 and 771-802, 1972.

"http://www.boost.org/doc/libs/1_65_1/libs/math/doc/html/math_toolkit/sf_poly/legendre.html"
*/

LegendreP {
    var <>degree;

    *new { arg degree;
        ^super.newCopyArgs(degree)
    }

    // legendre_p function returns the Associated Legendre polynomial of the first kind _from boost_
    assocEval { arg order, x;
        _LegendreP
        ^this.primitiveFailed;
    }

    assocEval2 { arg x;
        _LegendreP2
        ^this.primitiveFailed;
    }

    // for testing
     besselJ { arg x;
        _BesselJ
        ^this.primitiveFailed;
    }


    // returns the Legendre polynomial (order = 0) of the first kind _from boost_
    eval { arg x;
        ^this.assocEval(0, x);
    }

    // // legendre_p_zeros function returns the zeros of the Legendre polynomials, _from boost_
    zeros {
        ^legendreZeroes(degree)
        // _LegendrePZeros
        // ^this.primitiveFailed;
    }

    maxZero {
        ^this.zeros.maxItem;
    }
}

// Booster {
//     var var1;
//
//     *newWithVar { arg inVar;
//         ^super.newCopyArgs(inVar)
//     }
//
//
//     calcLP { arg arg1;
//         _BoostPrimLP
//         ^this.primitiveFailed;
//     }
//
//     calcBJ { arg arg1;
//         _BoostPrimBJ
//         ^this.primitiveFailed;
//     }
// }

+Number {

    /*	Legendre (and Associated) Polynomials  */
    // TODO: where to clip values at [-1,1] ??
    legendre { arg x;
        _LegendreP
        ^this.primitiveFailed;
    }

    legendrePrime { arg x;
        _LegendrePPrime
        ^this.primitiveFailed;
    }

    legendreZeros {
        _LegendrePZeros
        ^this.primitiveFailed;
    }

    legendreAssoc { arg m, x;
        _LegendrePAssoc
        ^this.primitiveFailed;
    }

    // TODO: proper way to catch error?
    legendre2 { arg x;
        if (this < 0) {
            format("n = %, but Legendre Polynomial of the Second Kind requires n >= 0", this).throw
        };
        ^prLegendre2(this, x)
    }
    prLegendre2 { arg x;
        _LegendreQ
        ^this.primitiveFailed;
    }


    /*	Bessel Functions  */
    //  First and Second Kinds
    bessel { arg x;
        _BesselJ
        ^this.primitiveFailed;
    }

    bessel2 { arg x;
        _BesselNeumann
        ^this.primitiveFailed;
    }

    //  Modified, First and Second Kinds
    besselMod { arg x;
        _BesselI
        ^this.primitiveFailed;
    }

    besselMod2 { arg arg1;
        _BesselK
        ^this.primitiveFailed;
    }

    // Spherical, First and Second Kinds
    besselSph { arg x;
        _BesselSph
        ^this.primitiveFailed;
    }

    besselSph2 { arg x;
        _BesselNeumannSph
        ^this.primitiveFailed;
    }

    // Derivatives
    besselPrime { arg x;
        _BesselJPrime
        ^this.primitiveFailed;
    }

    besselPrime2 { arg x;
        _BesselNeumannPrime
        ^this.primitiveFailed;
    }

    besselModPrime { arg x;
        _BesselIPrime
        ^this.primitiveFailed;
    }

    besselModPrime2 { arg arg1;
        _BesselKPrime
        ^this.primitiveFailed;
    }

    besselSphPrime { arg x;
        _BesselSphPrime
        ^this.primitiveFailed;
    }

    besselSphPrime2 { arg x;
        _BesselNeumannSphPrime
        ^this.primitiveFailed;
    }

    // Zero finder for bessel polynomials of first and second kind
    // Retrieve one zero at a time, by index
    besselZero { arg index;
        _BesselZero
        ^this.primitiveFailed;
    }

    besselZero2 { arg index;
        _BesselNeumannZero
        ^this.primitiveFailed;
    }


    /*  Laguerre polynomial  */
    laguerre { arg x;
        _Laguerre
        ^this.primitiveFailed;
    }

    // TODO: proper way to catch error?
    laguerreAssoc { arg m, x;
        if (this < 0) {
            format("n = %, but Associated Laguerre Polynomial requires n >= 0", this).throw
        };
        ^prLaguerreAssoc(this, m, x);
    }
    prLaguerreAssoc { arg m, x;
        _LaguerreAssoc
        ^this.primitiveFailed;
    }

    // Hermite Polynomial
    // TODO: proper way to catch error?
    hermite { arg x;
        if (this < 0) {
            format("n = %, but Hermite Polynomial requires n >= 0", this).throw
        };
        ^prHermite(this, x);
    }
    prHermite { arg x;
        _Hermite
        ^this.primitiveFailed;
    }

    // Spherical Hankel Functions
    hankelSph { arg x;
        _SphHankel1
        ^this.primitiveFailed;
    }

    hankelSph2 { arg x;
        _SphHankel2
        ^this.primitiveFailed;
    }

    // Beta functions
    beta { arg z;
        _Beta
        ^this.primitiveFailed;
    }


    // boosterBJ { arg arg1;
    //     _BoostPrimBJ
    //     ^this.primitiveFailed;
    // }
    //
    // boosterLag { arg arg1;
    //     _BoostPrimLaguerre
    //     ^this.primitiveFailed;
    // }
    //
    //
    // boosterFactorial {
    //     _BoostFactorial
    //     ^this.primitiveFailed;
    // }
    //
    //
    //
    // legendreZeroes {
    //     _LegendrePZeros
    //     ^this.primitiveFailed;
    // }
    //
    // legendreAssocEval2 { arg x;
    //     _LegendreP2
    //     ^this.primitiveFailed;
    // }

}


