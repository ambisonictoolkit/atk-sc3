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
// 	Class: HoaOrder
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
// Hoa Order Utilities

HoaOrder {
    var <>order;

    *new { arg order;
        ^super.newCopyArgs(order)
    }

    // ------------
    // Return l, m

    l {
        ^(this.order + 1).collect({ arg degree;
            HoaDegree.new(degree).l
        }).flatten
    }

    m {
        ^(this.order + 1).collect({ arg degree;
            HoaDegree.new(degree).m
        }).flatten
    }

    lm {
        ^(this.order + 1).collect({ arg degree;
            HoaDegree.new(degree).lm
        }).flatten
    }

    // ------------
    // Return indices

    indices { arg ordering = \acn, subset = \all;
        (subset == \all).if({
            // all
            ^this.lm.collect({ arg lm;
                HoaLm.new(lm).index(ordering)
            })
        }, {
            // subset
            ^this.lm.collect({ arg lm;
                var hoaLm = HoaLm.new(lm);
                hoaLm.isSubsetOf(subset).if({
                    hoaLm.index(ordering)
                })
            }).removeEvery([nil])
        })
    }

    // ------------
    // Return reflection coefficients

    reflection { arg mirror = \reflect;
        ^this.lm.collect({ arg lm;
            HoaLm.new(lm).reflection(mirror)
        })
    }

    // ------------
    // Return normalisation coefficients

    normalisation { arg scheme = \n3d;
        ^this.lm.collect({ arg lm;
            HoaLm.new(lm).normalisation(scheme)
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

    // ------------
    // Return decoder measures or coefficients

    // effective decoding radius
    radiusAtFreq { arg freq;
        ^(this.order*Atk.speedOfSound) / (2*pi*freq)
    }

    // effective decoding frequency
    freqAtRadius { arg radius;
        ^(this.order*Atk.speedOfSound) / (2*pi*radius)
    }

    // maximum average rV for an Ambisonic decoder
    rV { arg k = 'basic', dim = 3;
        var m = this.order;

        ^switch( k,
            'basic', { 1 },
            'energy', { this.rE(k, dim) },
            'controlled', {
                (dim == 2).if({
                    m / (m + 1)  // 2D
                }, {
                    m / (m + 2)  // 3D
                })
            }
        )
    }

    // maximum average rE for an Ambisonic decoder
    rE { arg k = 'basic', dim = 3;
        var m = this.order;

        ^(k == 'energy').if({
            (dim == 2).if ({
                PolyDegree.new(m+1).chebyMaxZero  // 2D
            }, {
                PolyDegree.new(m+1).legendreMaxZero  // 3D
            })
        }, {  // 'basic' & 'controlled'
            (dim == 2).if({
                (2*m) / (2*m + 1)  // 2D
            }, {
                m / (m + 1)  // 3D
            })
        })
    }

    // 'l’énergie réduite E' for an Ambisonic decoder
    meanE { arg k = 'basic', dim = 3;
        var m = this.order;
        var kDegreeWeights;

        kDegreeWeights = this.kDegreeWeights(k, dim);

        ^(dim == 2).if({
            kDegreeWeights.removeAt(0).squared + (2*kDegreeWeights.squared.sum) // 2D
        }, {
            (Array.series(m + 1, 1, 2) * kDegreeWeights.squared).sum // 3D
        })
    }

    // 'matching gain' (scale) for a given Ambisonic decoder
    matchWeight { arg k = 'basic', dim = 3, match = 'amp', numSpkrs = nil;
        var m = this.order;
        var n;

        ^switch( match,
            'amp', { 1 },
            'rms', {
                (dim == 2).if({
                    n = 2*m + 1  // 2D
                }, {
                    n = (m + 1).squared  // 3D
                });
                (n/this.meanE(k, dim)).sqrt
            },
            'energy', {
                n = numSpkrs;
                (n/this.meanE(k, dim)).sqrt
            }
        )
    }

    // beamWeights, aka, "decoder order gains" or Gamma vector of per-degree (beam forming) scalars
    beamWeights { arg k = 'basic', dim = 3;
        var m = this.order;
        var max_rE;

        ^switch( k,
            'basic', { 1.dup(m + 1) },
            'energy', {
                max_rE = this.rE(k, dim);
                (dim == 2).if({ // 2D
                    (m+1).collect({ arg order;
                        PolyDegree.new(order).chebyEval(max_rE)
                    })
                }, { // 3D
                    (m+1).collect({ arg order;
                        PolyDegree.new(order).legendreEval(max_rE)
                    })
                })
            },
            'controlled', {
                (dim == 2).if ({ // 2D
                    (m+1).collect({ arg order;
                        1 / (floatFactorial(m + order) * floatFactorial(m - order))
                    }) * floatFactorial(order).squared;
                }, { // 3D
                    (m+1).collect({ arg order;
                        1 / (floatFactorial(m + order + 1) * floatFactorial(m - order))
                    }) * floatFactorial(m) * floatFactorial(m + 1);
                })
            }
        )
    }


}