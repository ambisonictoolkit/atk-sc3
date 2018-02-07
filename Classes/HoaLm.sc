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

    // index - ACN
    *newIndex { arg index;
        var l, m;
        l = (floor(sqrt(index))).asInt;
        m = index - l.squared - l
        ^this.new([l, m])
    }

    // SID
    *newSidIndex { arg sid;
        var l, m, m0, m1, bool;
        l = sid.sqrt.floor.asInt;
        m0 = (((l + 1).squared - (sid + 1)) / 2).floor.asInt;
        m1 = -1 * (((l + 1).squared - sid) / 2).floor.asInt;
        bool = (m0 == m1.abs);
        m = (m0 * bool.asInt) + (m1 * bool.not.asInt);
        ^this.new([l, m])
    }

    // FuMa
    *newFumaIndex { arg fuma;
        var l;

        l = fuma.sqrt.floor.asInt;

        ^(l<=1).if({
            this.newSidIndex(fuma)
        }, {
            var m, m0, m1, bool;

            m0 = -1 * ((fuma - l.squared) / 2).floor.asInt;
            m1 = ((fuma +1 - l.squared) / 2).floor.asInt;

            bool = (m1 == m0.abs);
            m = (m0 * bool.asInt) + (m1 * bool.not.asInt);

            this.new([l, m])
        })
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

    // index - ACN
    index {
        var l, m;
        #l, m = this.lm;
        ^l.squared + l + m
    }

    // SID
    sidIndex {
        var l, m;
        #l, m = this.lm;
        ^(l**2 + (2 * (l - m.abs))) - m.sign.clip(-1, 0)
    }

    // FuMa
    fumaIndex {
        var l, m;
        #l, m = this.lm;
        ^(l <= 1).if ({
            this.sidIndex  // sid
        }, {
            (l.squared + (2 * m.abs)) - m.sign.clip(0, 1);
        })
    }

    // ------------
    // Test sub-group membership

    // zonal
    zonal {
        ^HoaDegree.new(this.l).zonalIndices.includes(this.index)
    }

    // sectoral
    sectoral {
        ^HoaDegree.new(this.l).sectoralIndices.includes(this.index)
    }

    // tesseral
    tesseral {
        ^HoaDegree.new(this.l).tesseralIndices.includes(this.index)
    }

    // rotate around z-axis, aka yaw
    rotate {
        ^this.zonal.not
    }

    // ------------
    // Return simple transform coefficients

    // Condon-Shortley Phase - flip * flop
    csp {
        ^this.m.odd.if({-1.0}, {1.0});
    }

    // reflect - mirror across origin
    reflect {
        ^this.l.odd.if({-1.0}, {1.0});
    }

    // flap - mirror across z-axis
    flap {
        var l, m;
        #l, m = this.lm;
        ^(m + l).odd.if({-1.0}, {1.0})
    }

    // flip - mirror across y-axis
    flip {
        ^(this.m < 0).if({-1.0}, {1.0})
    }

    // flop - mirror across x-axis
    flop {
        var m;
        m = this.m;
        ^((m < 0 && m.even) || (m > 0 && m.odd)).if({-1.0}, {1.0})
    }


    // ------------
    // Return normalisation coefficients

    // 3D Schmidt semi-normalisation
    sn3d {
        var l, m, dm, mabs;
        #l, m = this.lm;
        dm = (m==0).asInt;
        mabs = m.abs;
        ^sqrt(
            (2 - dm) * (
                floatFactorial(l - mabs) / floatFactorial(l + mabs)
            )
        )
    }

    // 3D full normalisation
    n3d {
        ^sqrt((2*this.l) + 1) * this.sn3d
    }

    // 2D full normalisation
    n2d {
        var l = this.l;
        ^sqrt(
            2.pow(2*l) * floatFactorial(l).pow(2) /
            floatFactorial((2*l) + 1)
        ) * this.n3d
    }

    // 2D semi-normalisation
    sn2d {
        var lne0;
        lne0 = (this.l>0).asInt;
        ^2.pow(-0.5 * lne0) * this.n2d
    }

    // // Add: maxN, bigMaxN


    // ------------
    // Return encoding coefficients

    // N3D normalized coefficient
    sph { arg theta = 0.0, phi = 0.0;
        var l, m, mabs;
        var sphHarm;
        var res;

        #l, m = this.lm;
        mabs = m.abs;

        // remap phi
        phi = pi/2 - phi;

        // evaluate spherical harmonic
        sphHarm = SphHarm.new(l);
        case
        { m < 0 } { res = 2.sqrt * sphHarm.imag(mabs, phi, theta) }  // imag
        { m == 0 } { res = sphHarm.real(mabs, phi, theta) }  // real
        { m > 0 } { res = 2.sqrt * sphHarm.real(mabs, phi, theta) };  // real

        // remove Condon-Shortley phase
        res = this.csp * res;

        // normalize (l, m) = (0, 0) to 1
        res = 4pi.sqrt * res;

        // return
        ^res
    }

}