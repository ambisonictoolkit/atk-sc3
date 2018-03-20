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
// 	Class: WaveNumber
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
// Wavenumber Utilities

WaveNumber {
    var <>waveNumber;

    *new { arg waveNumber = 8.0600627847202;
        ^super.newCopyArgs(waveNumber)
    }

    // Set wavenumber from freq (in hz).
    *newFreq { arg freq = 440.0;
        ^this.new(2*pi*freq / Atk.speedOfSound);
    }

    // Set wavenumber from normalised frequency.
    *newWn { arg wn, sr;
        ^this.new(pi*wn*sr / Atk.speedOfSound)
    }

    // Set wavenumber from delay and effective order.
    *newDelay { arg delay = 0.00036171577975431, order = 1;
        ^this.new(order/(delay*this.c))
    }

    // Set wavenumber from radius and effective order.
    *newRadius { arg radius = 0.12406851245573, order = 1;
        ^this.new(order / radius)
    }

    // Return freq (in hz) from wavenumber.
    freq {
        ^this.waveNumber*Atk.speedOfSound / (2*pi)
    }

    // Return normalised frequency from wavenumber.
    wn { arg sr;
        ^this.num*Atk.speedOfSound / (pi*sr)
    }

    // ------------
    // Radius Utilities

    // Return effective radius.
    radius { arg order = 1;
        ^order / this.waveNumber
    }

    // Return effective delay.
    delay { arg order = 1;
        ^order / (Atk.speedOfSound*this.waveNumber)
    }

    // Return effective order.
    orderAtRadius { arg radius = 0.12406851245573;
        ^radius*this.waveNumber
    }

    // Return effective order.
    orderAtDelay { arg delay = 0.00036171577975431;
        ^delay*this.waveNumber*Atk.speedOfSound
    }

    // ------------
    // NFE Utilities

    // Return complex degree weights
    proxDegreeWeights { arg order, radius = Atk.encRadius;
        var m = order;
        var nearZero = 1e-08;

        (this.waveNumber.abs <= nearZero).if({
            ^Array.with(Complex.new(1, 0)) ++ m.collect({ arg k;
                Complex.new(-inf.pow(((k+1)/2).floor), -inf.pow(((k+2)/2).floor))
            })
        }, {
            ^(m+1).collect({ arg j;
                (j+1).collect({ arg k;
                    var fact;
                    fact = (j+k).asFloat.factorial/((j-k).asFloat.factorial*k.asFloat.factorial);
                    fact * Complex.new(0, -1/(2*this.waveNumber*radius)).pow(k)
                }).sum
            })
        })
    }

    // Return complex degree weights
    distDegreeWeights { arg order, radius = Atk.decRadius;
        var m = order;
        var nearZero = 1e-08;

        (this.waveNumber.abs <= nearZero).if({
            ^Array.with(Complex.new(1, 0)) ++ m.collect({Complex.new(0, 0)})
        }, {
            ^(m+1).collect({ arg j;
                (j+1).collect({ arg k;
                    var fact;
                    fact = (j+k).asFloat.factorial/((j-k).asFloat.factorial*k.asFloat.factorial);
                    fact * Complex.new(0, -1/(2*this.waveNumber*radius)).pow(k)
                }).sum.reciprocal
            })
        })
    }

    // Return complex degree weights
    ctrlDegreeWeights { arg order, encRadius = Atk.encRadius, decRadius = Atk.decRadius;
        var m = order;
        var nearZero = 1e-08;

        (this.waveNumber.abs <= nearZero).if({
            ^(m+1).collect({ arg k;
                Complex.new((decRadius/encRadius).pow(k), 0)
            })
        }, {
            ^(m+1).collect({ arg j;
                ((j+1).collect({ arg k;
                    var fact;
                    fact = (j+k).asFloat.factorial/((j-k).asFloat.factorial*k.asFloat.factorial);
                    fact * Complex.new(0, -1/(2*this.waveNumber*encRadius)).pow(k)
                }).sum) / ((j+1).collect({ arg k;
                    var fact;
                    fact = (j+k).asFloat.factorial/((j-k).asFloat.factorial*k.asFloat.factorial);
                    fact * Complex.new(0, -1/(2*this.waveNumber*decRadius)).pow(k)
                }).sum)
            })
        })
    }

}
