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
// 	Class: HoaLm
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
// Hoa [l, m] Utilities

HoaLm {
    var <>lm;

    *new { arg lm;
        ^super.newCopyArgs(lm)
    }

    *newIndex { arg index, ordering = \acn;
        var l, m;

        switch (ordering,
            \acn, {
                l = (floor(sqrt(index))).asInt;
                m = index - l.squared - l
                ^this.new([l, m])
            },
            \sid, {
                var m0, m1, bool;
                l = index.sqrt.floor.asInt;
                m0 = (((l + 1).squared - (index + 1)) / 2).floor.asInt;
                m1 = -1 * (((l + 1).squared - index) / 2).floor.asInt;
                bool = (m0 == m1.abs);
                m = (m0 * bool.asInt) + (m1 * bool.not.asInt);
                ^this.new([l, m])
            },
            \fuma, {
                l = index.sqrt.floor.asInt;

                ^(l<=1).if({
                    this.newIndex(index, \sid)
                }, {
                    var m, m0, m1, bool;

                    m0 = -1 * ((index - l.squared) / 2).floor.asInt;
                    m1 = ((index +1 - l.squared) / 2).floor.asInt;

                    bool = (m1 == m0.abs);
                    m = (m0 * bool.asInt) + (m1 * bool.not.asInt);

                    this.new([l, m])
                })
            },
        )
    }


    // ------------
    // Return l, m

    l {
        ^this.lm.at(0);
    }

    m {
        ^this.lm.at(1);
    }

    // ------------
    // Return indices

    index { arg ordering = \acn;
        var l, m;
        #l, m = this.lm;

        switch (ordering,
            \acn, {
                ^l.squared + l + m
            },
            \sid, {
                ^(l**2 + (2 * (l - m.abs))) - m.sign.clip(-1, 0)
            },
            \fuma, {
                ^(l <= 1).if ({
                    this.index(\sid)
                }, {
                    (l.squared + (2 * m.abs)) - m.sign.clip(0, 1);
                })
            }
        )
    }

    // ------------
    // Test sub-group membership

    isInSubset { arg subset = \zonal;
        var l, m;
        #l, m = this.lm;

        switch (subset,
            \zonal, {
                ^(m == 0)
            },
            \sectoral, {
                ^(m.abs == l)
            },
            \tesseral, {
                ^((m != 0) && (m.abs != l))
            },
            \rotate, {  // rotate around z-axis, aka yaw
                ^(m != 0)
            },
        )
    }

    // ------------
    // Return reflection coefficients

    reflection { arg mirror = \reflect;
        var l, m;
        #l, m = this.lm;

        switch (mirror,
            \reflect, {  // reflect - mirror across origin - flip * flop * flap
                ^l.odd.if({-1.0}, {1.0});
            },
            \flip, {  // flip - mirror across y-axis
                ^(m < 0).if({-1.0}, {1.0})
            },
            \flop, {  // flop - mirror across x-axis
                ^((m < 0 && m.even) || (m > 0 && m.odd)).if({-1.0}, {1.0})
            },
            \flap, {  // flap - mirror across z-axis
                ^(m + l).odd.if({-1.0}, {1.0})
            },
            \CondonShortleyPhase, {  // Condon-Shortley Phase - flip * flop
                ^m.odd.if({-1.0}, {1.0});
            },
            \origin, {
                ^this.reflection(\reflect)
            },
            \x, {
                ^this.reflection(\flop)
            },
            \y, {
                ^this.reflection(\flip)
            },
            \z, {
                ^this.reflection(\flap)
            },
        )
    }

    // ------------
    // Return normalisation coefficients

    normalisation { arg scheme = \n3d;
        var l, m;
        #l, m = this.lm;

        switch (scheme,
            \n3d, {
                ^sqrt((2*l) + 1) * this.normalisation(\sn3d)
            },
            \sn3d, {
                var dm, mabs;
                dm = (m==0).asInt;
                mabs = m.abs;
                ^sqrt(
                    (2 - dm) * (
                        (l - mabs).asFloat.factorial / (l + mabs).asFloat.factorial
                    )
                )
            },
            \n2d, {
                ^sqrt(
                    2.pow(2*l) * l.asFloat.factorial.pow(2) / ((2*l) + 1).asFloat.factorial
                ) * this.normalisation(\n3d)
            },
            \sn2d, {
                var lne0;
                lne0 = (l>0).asInt;
                ^2.pow(-0.5 * lne0) * this.normalisation(\n2d)
            },
            \maxN, {
                var twoDivSqrt3, sqrt45_32, threeDivSqrt5, sqrt8_5;
                var norms;
                twoDivSqrt3 = 2/3.sqrt;
                sqrt45_32 = (45/32).sqrt;
                threeDivSqrt5 = 3/5.sqrt;
                sqrt8_5 = (8/5).sqrt;

                // scaling to convert from SN3D to maxN
                // indexed by ACN
                norms = [
                    1.0,  // W
                    1.0, 1.0, 1.0,  // Y, Z, X
                    twoDivSqrt3, twoDivSqrt3, 1.0, twoDivSqrt3, twoDivSqrt3,  // V, T, R, S, U
                    sqrt8_5, threeDivSqrt5, sqrt45_32, 1, sqrt45_32, threeDivSqrt5, sqrt8_5  // Q, O, M, K, L, N, P
                ];

                ^norms[this.index(\acn)] * this.normalisation(\sn3d)
            },
            \MaxN, {
                ^(this.index(\acn) == 0).if({
                    2.sqrt.reciprocal  // W
                }, {
                    this.normalisation(\maxN)
                })
            },
            \fuma, {
                ^this.normalisation(\MaxN)
            },
        )
    }


    // ------------
    // Return encoding coefficients

    // N3D normalized coefficient
    sph { arg theta = 0.0, phi = 0.0;
        var l, m, mabs;
        var res;

        #l, m = this.lm;
        mabs = m.abs;

        // remap phi
        phi = pi/2 - phi;

        // evaluate spherical harmonic
        case
        { m < 0 } { res = 2.sqrt * sphericalHarmonicI(l, mabs, phi, theta) }  // imag
        { m == 0 } { res = sphericalHarmonicR(l, mabs, phi, theta) }          // real
        { m > 0 } { res = 2.sqrt * sphericalHarmonicR(l, mabs, phi, theta) }; // real

        // remove Condon-Shortley phase
        res = this.reflection(\CondonShortleyPhase) * res;

        // normalize (l, m) = (0, 0) to 1
        res = 4pi.sqrt * res;

        // return
        ^res
    }

}