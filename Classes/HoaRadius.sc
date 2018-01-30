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
// 	Class: HoaRadius
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
// Radius Utilities

HoaRadius {
    var <>radius;

    *new { arg radius = 0.12406851245573;
        ^super.newCopyArgs(radius)
    }

    // Set radius from delay (in seconds).
    *newDelay { arg delay = 0.00036171577975431;
        ^this.new(delay*Atk.speedOfSound)
    }

    // Set radius from order and wavenumber
    *newOrderNum { arg order = 1, num = 8.0600627847202;
        ^this.new(order/num)
    }

    // Set radius from order and frequency
    *newOrderFreq { arg order = 1, freq = 440.0;
        ^this.new((order*Atk.speedOfSound) / (2*pi*freq))
    }

    // Set radius from order and normalised frequency
    *newOrderWn { arg order, wn, sr;
        ^this.new((order*Atk.speedOfSound) / (pi*wn*sr))
    }


    // Return reference delay.
    delay {
        ^this.radius / Atk.speedOfSound
    }

    // ----------
    // Order

    // Return effective order.
    numOrder { arg num = 8.0600627847202;
        ^(this.radius*num)
    }

    // Return effective order.
    freqOrder { arg freq = 440.0;
        ^(2*pi*this.radius*freq) / Atk.speedOfSound
    }

    // Return effective order.
    wnOrder { arg wn, sr;
        ^(pi*this.radius*wn*sr) / Atk.speedOfSound
    }

    // ----------
    // Wavenumber / frequency

    // Return effective wavenumber.
    orderNum { arg order = 1;
        ^(order/this.radius)
    }

    // Return effective frequency.
    orderFreq { arg order = 1;
        ^(order*Atk.speedOfSound) / (2*pi*this.radius)
    }

    // Return effective normalized frequency.
    orderWn { arg order, sr;
        ^(order*Atk.speedOfSound) / (pi*this.radius*sr)
    }

}
