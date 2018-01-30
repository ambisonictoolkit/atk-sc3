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
// 	Class: HoaIndex
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
// Hoa Indexing Utilities

/*
May want to refactor all below classes to conveniently handle
  - single args
  - arrayed args

This could be done via a "multi dispatch" approach by calling an -init to set up.
The goal would be to have an implementation that appears to "multichannel expand"
as required.
*/

HoaIndex {
    var <>index;


    // ------------
    // Set "by Index"


    // Set ACN index
    *new { arg index = 0;
        ^super.newCopyArgs(index)
    }

    // Set [ l, m ]
    *newLm { arg lm = [ 0, 0 ];
        var l, m;
        #l, m = lm;
        ^this.new(l.squared + l + m)
    }

    // Set SID
    *newSid { arg sid = 0;
        var lm, sidLm = { arg sid;
            var l, m, m0, m1, bool;

            l = sid.sqrt.floor.asInt;

            m0 = (((l + 1).squared - (sid + 1)) / 2).floor.asInt;
            m1 = -1 * (((l + 1).squared - sid) / 2).floor.asInt;

            bool = (m0 == m1.abs);
            m = (m0 * bool.asInt) + (m1 * bool.not.asInt);

            [ l, m ]
        };

        (sid.class == Array).if({
            lm = sid.collect({ arg item;
                sidLm.value(item)
            }).flop
        }, {
            lm = sidLm.value(sid)
        });

        ^this.newLm(lm)
    }

    // Set FuMa
    *newFuma { arg fuma = 0;
        var lm, fumaLm = { arg fuma;
            var l, m, m0, m1, bool, bool2;

            l = fuma.sqrt.floor.asInt;

            m0 = -1 * ((fuma - l.squared) / 2).floor.asInt;
            m1 = ((fuma +1 - l.squared) / 2).floor.asInt;

            bool = (m1 == m0.abs);
            m = (m0 * bool.asInt) + (m1 * bool.not.asInt);

            // correct m for <=1st order
            bool2 = (l < 2);
            m = (HoaIndex.newSid(fuma).lm.at(1) * bool2.asInt) + (m * bool2.not.asInt);

            [ l, m ]
        };

        (fuma.class == Array).if({
            lm = fuma.collect({ arg item;
                fumaLm.value(item)
            }).flop
        }, {
            lm = fumaLm.value(fuma)
        });

        ^this.newLm(lm)
    }

    // ------------
    // Set by group

    // Degree
    *newL { arg l = 0;
        var index, indexFn = { arg l;
            Array.series((2*l)+1, l.squared)
        };
        index = case
        { l == 0 }   { 0 }
        { l.class == Integer } { index = indexFn.value(l) }
        { l.class == Array } {
            l.collect({ arg item;
                indexFn.value(item)
            }).flatten.sort
        };

        ^this.new(index)
    }

    // Degree - Zonal
    *newLZonal { arg l = 0;
        ^this.new(l*(l+1))
    }

    // Degree - Sectoral
    *newLSectoral { arg l = 0;
        var index;
        index = case
        { l == 0 }   { 0 }
        { l.class == Integer } { [ l.squared, (l+1).squared - 1 ] }
        { l.class == Array } {
            var index2;
            index2 = (l.squared ++ ((l+1).squared - 1)).flatten.sort;

            // drop first 0, if present
            (index2.at(0) == 0).if({
                index2 = index2.drop(1)
            });
            index2
        };

        ^this.new(index)
    }

    // Degree - Tesseral
    *newLTesseral { arg l = 0;
        var index;

        index = Array.with(
            this.newL(l).index
        ).flatten.difference(
            Array.with(this.newLZonal(l).index).flatten ++ Array.with(this.newLSectoral(l).index).flatten
        );
        (index.size <= 1).if({ index = index.first});

        ^this.new(index)
    }

    // Order
    *newOrder { arg order = 1;
        var index;
        (order == 0).if({
            index = 0
        }, {
            index = Array.series((order+1).squared)
        })
        ^this.new(index)
    }

    // Order - Zonal
    *newOrderZonal { arg order = 1;
        var index;
        (order == 0).if({
            index = 0
        }, {
            index = Array.series(order+1)
        })
        ^this.newLZonal(index)
    }

    // Order - Sectoral
    // returns an array for 0 --> [ 0 ]: FIX THIS
    *newOrderSectoral { arg order = 1;
        var index;
        (order == 0).if({
            index = 0
        }, {
            index = Array.series(order+1)
        })
        ^this.newLSectoral(index)
    }

    // Order - Tesseral
    *newOrderTesseral { arg order = 1;
        ^this.newLTesseral(Array.series(order+1))
    }


    // ------------
    // Set by group - mirroring

    // Degree - Mirror across Origin
    *newLReflect { arg l = 0;
        var index, indexFn = { arg l;
            l.odd.if({
                this.newL(l).index
            })
        };

        (l.class == Array).if({
            index = [];
            l.do({ arg item;
                index = index ++ indexFn.value(item)
            });
            index.sort
        }, {
            index = indexFn.value(l)
        });

        ^this.new(index)
    }

    // Degree - Mirror across Z-axis
    *newLFlap { arg l = 0;
        var index, indexFn = { arg l;
            Array.series(l, l.squared + 1, 2);
        };

        (l.class == Array).if({
            index = [];
            l.do({ arg item;
                index = index ++ indexFn.value(item)
            });
            index.sort
        }, {
            index = indexFn.value(l)
        });

        // return single values as required
        (index.size <=1).if({ index = index.first });

        ^this.new(index)
    }

    // Degree - Mirror across Y-axis
    *newLFlip { arg l = 0;
        var index, indexFn = { arg l;
            Array.series(l, l.squared, 1);
        };

        (l.class == Array).if({
            index = [];
            l.do({ arg item;
                index = index ++ indexFn.value(item)
            });
            index.sort
            }, {
                index = indexFn.value(l)
        });

        // return single values as required
        (index.size <=1).if({ index = index.first });

        ^this.new(index)
    }

    // Degree - Mirror across X-axis
    *newLFlop { arg l = 0;
        var index, indexFn = { arg l;
            l.even.if({
                Array.series(l/2, l.squared, 2) ++ Array.series(l/2, l*(l+1)+1, 2)
            }, {
                Array.series((l-1)/2, l.squared+1, 2) ++ Array.series((l+1)/2, l*(l+1)+1, 2)
            })
        };

        (l.class == Array).if({
            index = [];
            l.do({ arg item;
                index = index ++ indexFn.value(item)
            });
            index.sort
            }, {
                index = indexFn.value(l)
        });

        // return single values as required
        (index.size <=1).if({ index = index.first });

        ^this.new(index)
    }

    // Order - Mirror across Origin
    *newOrderReflect { arg order = 1;
        (order == 0).if({
            ^this.new(nil)
        }, {
            ^this.newLReflect(Array.series(order+1))
        })
    }

    // Order - Mirror across Z-axis
    *newOrderFlap { arg order = 1;
        ^this.newLFlap(Array.series(order+1))
    }

    // Order - Mirror across Y-axis
    *newOrderFlip { arg order = 1;
        ^this.newLFlip(Array.series(order+1))
    }

    // Order - Mirror across X-axis
    *newOrderFlop { arg order = 1;
        ^this.newLFlop(Array.series(order+1))
    }


    // ------------
    // Set by group - rotation

    // Degree - Rotate around Z-axis, aka Yaw
    *newLRotate { arg l = 1;
        var index;
        (l == 0).if({
            index = nil
        }, {
            index = Array.with(
                this.newL(l).index
            ).flatten.difference(
                Array.with(this.newLZonal(l).index).flatten
            )
        });
        ^this.new(index)
    }

    // Order - Rotate around Z-axis, aka Yaw
    *newOrderRotate { arg order = 1;
        (order == 0).if({
            ^this.new(nil)
        }, {
            var index = this.newOrder(order).index.difference(
                this.newOrderZonal(order).index
            );
            ^this.new(index)
        })
    }


    // ------------
    // Set by group - Condon-Shortley Phase (polarity)

    // m.collect({ arg item;
    //     item.odd.if({-1}, {1})
    // })

    // Degree - Condon-Shortley Phase
    *newLCSP { arg l = 0;
        var index, indexFn = { arg l;
            l.even.if({
                Array.series(l, l.squared+1, 2)
            }, {
                Array.series(l+1, l.squared, 2)
            })
        };
        index = case
        { l == 0 }   { nil }
        { l.class == Integer } { index = indexFn.value(l) }
        { l.class == Array } {
            l.collect({ arg item;
                indexFn.value(item)
            }).flatten.sort
        };

        ^this.new(index)
    }

    // Order - Condon-Shortley Phase
    *newOrderCSP { arg order = 1;
        var index;
        (order == 0).if({
            index = nil
        }, {
            index = Array.series(((order+1).squared)/2, 1, 2)
        })
        ^this.new(index)
    }


    // ------------
    // Return "by Index"
    //
    // May want to catch index = nil & throw error

    // Return [ l, m ]
    lm {
        var l, m;
        l = (floor(sqrt(this.index))).asInt;
        m = this.index - l.squared - l
        ^[ l, m ]
    }

    // Return SID
    sid {
        var l, m, res;
        #l, m = this.lm;

        res = l**2 + (2 * (l - m.abs));
        ^res - m.sign.clip(-1, 0)
    }

    // Return FuMa
    fuma {
        var l, m;
        var bool, res1, res2;
        #l, m = this.lm;

        // ordering for <=1st order
        res1 = this.sid;

        // ordering for >1st order
        res2 = l.squared + (2 * m.abs);
        res2 = res2 - m.sign.clip(0, 1);

        bool = (l<2);

        ^(res1 * bool.asInt) + (res2 * (1 - bool.asInt))
    }

}
