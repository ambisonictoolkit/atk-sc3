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
// 	Class: HoaDegree
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
// Hoa Degree Utilities

HoaDegree {
    var <>degree;

    *new { arg degree;
        ^super.newCopyArgs(degree)
    }

    // ------------
    // Return l, m

    l {
        ^Array.fill(2*(this.degree+1)-1, {this.degree})
    }

    m {
        ^Array.series(2*(this.degree+1)-1, -1 * this.degree)
    }

    lm {
        ^Array.with(
            this.l,  // fill l
            this.m  // fill m
        ).flop
    }

    // ------------
    // Return indices

    // channel ordering
    indices {
        ^Array.series((2*this.degree)+1, this.degree.squared)
    }

    sidIndices {
        ^this.lm.collect({ arg lm;
            var l, m;
            #l, m = lm;
            (l**2 + (2 * (l - m.abs))) - m.sign.clip(-1, 0)
        })
    }

    fumaIndices {
        ^this.lm.collect({ arg lm;
            var l, m;
            #l, m = lm;

            (l <= 1).if ({
                (l**2 + (2 * (l - m.abs))) - m.sign.clip(-1, 0)  // sid
            }, {
                (l.squared + (2 * m.abs)) - m.sign.clip(0, 1);
            })
        })
    }

    // sub-groups

    // zonal
    zonalIndices {
        ^Array.with(this.degree * (this.degree + 1))
    }

    // sectoral
    sectoralIndices {
        ^(this.degree == 0).if({
            Array.with(0)
        }, {
            Array.with(this.degree.squared, (this.degree+1).squared - 1)
        })
    }

    // tesseral
    tesseralIndices {
        ^this.indices.difference(
            union(
                this.zonalIndices,
                this.sectoralIndices
            )
        )
    }

    // Condon-Shortley Phase - indices
    cspIndices {
        ^this.degree.odd.if({
            Array.series(this.degree+1, this.degree.squared, 2)
        }, {
            Array.series(this.degree, this.degree.squared+1, 2)
        })
    }

    // reflect - mirror across origin
    reflectIndices {
        ^this.degree.odd.if({
            ^this.indices
        }, {
            Array.newClear
        })
    }

    // flap - mirror across z-axis
    flapIndices {
        ^Array.series(this.degree, this.degree.squared + 1, 2)
    }

    // flip - mirror across y-axis
    flipIndices {
        ^Array.series(this.degree, this.degree.squared)
    }

    // flop - mirror across x-axis
    flopIndices {
        ^this.degree.even.if({
            Array.series(this.degree/2, this.degree.squared, 2) ++ Array.series(this.degree/2, this.degree*(this.degree+1)+1, 2)
        }, {
            Array.series((this.degree-1)/2, this.degree.squared+1, 2) ++ Array.series((this.degree+1)/2, this.degree*(this.degree+1)+1, 2)
        })
    }

    // rotate around z-axis, aka yaw
    rotateIndices {
        ^this.indices.difference(
            this.zonalIndices
        )
    }


    // ------------
    // Return simple transform coefficients

    // Condon-Shortley Phase - flip * flop
    csp {
        ^(Array.fill((2*this.degree)+1, { 1.0 })[this.cspIndices - this.degree.squared] = -1.0)
    }

    // reflect - mirror across origin
    reflect {
        ^(Array.fill((2*this.degree)+1, { 1.0 })[this.reflectIndices - this.degree.squared] = -1.0)
    }

    // flap - mirror across z-axis
    flap {
        ^(Array.fill((2*this.degree)+1, { 1.0 })[this.flapIndices - this.degree.squared] = -1.0)
    }

    // flip - mirror across y-axis
    flip {
        ^(Array.fill((2*this.degree)+1, { 1.0 })[this.flipIndices - this.degree.squared] = -1.0)
    }

    // flop - mirror across x-axis
    flop {
        ^(Array.fill((2*this.degree)+1, { 1.0 })[this.flopIndices - this.degree.squared] = -1.0)
    }


    // ------------
    // Return normalisation coefficients

    // 3D Schmidt semi-normalisation
    sn3d {
        ^this.lm.collect({ arg lm;
            HoaLm.new(lm).sn3d
        })
    }

    // 3D full normalisation
    n3d {
        ^this.lm.collect({ arg lm;
            HoaLm.new(lm).n3d
        })
    }

    // 2D full normalisation
    n2d {
        ^this.lm.collect({ arg lm;
            HoaLm.new(lm).n2d
        })
    }

    // 2D semi-normalisation
    sn2d {
        ^this.lm.collect({ arg lm;
            HoaLm.new(lm).sn2d
        })
    }

    // maxN normalization
    maxN {
        ^this.lm.collect({ arg lm;
            HoaLm.new(lm).maxN
        })
    }

    // MaxN normalization, aka FuMa
    fuma {
        ^this.lm.collect({ arg lm;
            HoaLm.new(lm).fuma
        })
    }

    // ------------
    // Return encoding coefficients

    // N3D normalized coefficients
    sph { arg theta = 0.0, phi = 0.0;
        ^this.lm.collect({ arg lm;
            HoaLm.new(lm).sph(theta, phi)
        })
    }

}