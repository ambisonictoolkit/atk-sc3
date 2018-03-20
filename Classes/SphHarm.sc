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
// 	Class: SphHarm
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
// Simple class to import Boost Spherical Harmonic functionality

/*
For θ outside [0, π] and φ outside [0, 2π] this implementation follows the convention used by Mathematica:
the function is periodic with period π in θ and 2π in φ. Please note that this is not the behaviour one would
get from a casual application of the function's definition. Cautious users should keep θ and φ to the
range [0, π] and [0, 2π] respectively.

Some other sources include an additional Condon-Shortley phase term of (-1)m in the definition of this function:
note however that our definition of the associated Legendre polynomial already includes this term.

"http://www.boost.org/doc/libs/1_65_1/libs/math/doc/html/math_toolkit/sf_poly/sph_harm.html"
*/

SphHarm {
    var <>degree;

    *new { arg degree;
        ^super.newCopyArgs(degree)
    }

    // spherical_harmomic function _from boost_
    complex { arg order, theta, phi;
        ^sphHarmComplex(degree, order, theta, phi)
    }

    // spherical_harmomic function returns the real part _from boost_
    real { arg order, theta, phi;
        ^sphHarmReal(degree, order, theta, phi)
    }

    // spherical_harmomic function returns the imaginary part _from boost_
    imag { arg order, theta, phi;
        ^sphHarmImag(degree, order, theta, phi)
    }
}
