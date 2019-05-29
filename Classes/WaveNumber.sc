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
    var <waveNumber;

    *new { |waveNumber|
        ^super.newCopyArgs(waveNumber)
    }

    // Set wavenumber from freq (in hz).
	*newFreq { |freq, speedOfSound = (AtkHoa.speedOfSound)|
        ^this.new(2*pi*freq / speedOfSound);
    }

    // Set wavenumber from normalised frequency.
	*newWn { |wn, sampleRate, speedOfSound = (AtkHoa.speedOfSound)|
        ^this.new(pi*wn*sampleRate / speedOfSound)
    }

    // Set wavenumber from delay and effective order.
	*newDelay { |delay, order, speedOfSound = (AtkHoa.speedOfSound)|
        ^this.new(order / (delay*speedOfSound))
    }

    // Set wavenumber from radius and effective order.
	*newRadius { |radius, order|
        ^this.new(order / radius)
    }

    // Return freq (in hz) from wavenumber.
    freq { |speedOfSound = (AtkHoa.speedOfSound)|
        ^this.waveNumber*speedOfSound / (2*pi)
    }

    // Return normalised frequency from wavenumber.
    wn { |sampleRate, speedOfSound = (AtkHoa.speedOfSound)|
        ^this.num*speedOfSound / (pi*sampleRate)
    }

    // ------------
    // Radius Utilities

    // Return effective radius.
    radius { |order|
        ^order / this.waveNumber
    }

    // Return effective delay.
    delay { |order, speedOfSound = (AtkHoa.speedOfSound)|
        ^order / (speedOfSound*this.waveNumber)
    }

    // Return effective order.
    orderAtRadius { |radius|
        ^radius*this.waveNumber
    }

    // Return effective order.
    orderAtDelay { |delay, speedOfSound = (AtkHoa.speedOfSound)|
        ^delay*this.waveNumber*speedOfSound
    }

    // ------------
    // NFE Utilities

    // Return complex degree weights
    proxWeights { arg radius, order;
        var m = order;
		var r0 = radius;
        var nearZero = 1e-08;

        (this.waveNumber.abs <= nearZero).if({
            ^Array.with(Complex.new(1, 0)) ++ m.collect({ |k|
                Complex.new(-inf.pow(((k+1)/2).floor), -inf.pow(((k+2)/2).floor))
            })
        }, {
            ^(m+1).collect({ |j|
                (j+1).collect({ |k|
                    var fact;
                    fact = (j+k).asFloat.factorial/((j-k).asFloat.factorial*k.asFloat.factorial);
                    fact * Complex.new(0, -1/(2*this.waveNumber*r0)).pow(k)
                }).sum
            })
        })
    }

    // Return complex degree weights
    distWeights { arg radius, order;
        var m = order;
		var r1 = radius;
        var nearZero = 1e-08;

        (this.waveNumber.abs <= nearZero).if({
            ^Array.with(Complex.new(1, 0)) ++ m.collect({Complex.new(0, 0)})
        }, {
            ^(m+1).collect({ |j|
                (j+1).collect({ |k|
                    var fact;
                    fact = (j+k).asFloat.factorial/((j-k).asFloat.factorial*k.asFloat.factorial);
                    fact * Complex.new(0, -1/(2*this.waveNumber*r1)).pow(k)
                }).sum.reciprocal
            })
        })
    }

    // Return complex degree weights
    ctrlWeights { arg encRadius, decRadius, order;
        var m = order;
		var r0 = encRadius;
		var r1 = decRadius;
        var nearZero = 1e-08;

        (this.waveNumber.abs <= nearZero).if({
            ^(m+1).collect({ |k|
                Complex.new((r1/r0).pow(k), 0)
            })
        }, {
            ^(m+1).collect({ |j|
                ((j+1).collect({ |k|
                    var fact;
                    fact = (j+k).asFloat.factorial/((j-k).asFloat.factorial*k.asFloat.factorial);
                    fact * Complex.new(0, -1/(2*this.waveNumber*r0)).pow(k)
                }).sum) / ((j+1).collect({ |k|
                    var fact;
                    fact = (j+k).asFloat.factorial/((j-k).asFloat.factorial*k.asFloat.factorial);
                    fact * Complex.new(0, -1/(2*this.waveNumber*r1)).pow(k)
                }).sum)
            })
        })
    }

}
