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
	var <radius;

	*new { |radius = (AtkHoa.refRadius)|
		^super.newCopyArgs(radius)
	}

	// Set radius from delay (in seconds).
	*newDelay { |delay, speedOfSound = (AtkHoa.speedOfSound)|
		^this.new(delay * speedOfSound)
	}

	// Set radius from wavenumber and order
	*newWaveNumber { |waveNumber, order = (AtkHoa.defaultOrder)|
		^this.new(order / waveNumber)
	}

	// Set radius from frequency and order
	*newFreq { |freq, order = (AtkHoa.defaultOrder), speedOfSound = (AtkHoa.speedOfSound)|
		^this.new((order * speedOfSound) / (2 * pi * freq))
	}

	// Set radius from normalised frequency and order
	*newWn { |wn, sampleRate, order = (AtkHoa.defaultOrder), speedOfSound = (AtkHoa.speedOfSound)|
		^this.new((order * speedOfSound) / (pi * wn * sampleRate))
	}


	// Return reference delay.
	delay { |speedOfSound = (AtkHoa.speedOfSound)|
		^this.radius / speedOfSound
	}

	// ----------
	// Order

	// Return effective order.
	orderAtWaveNumber { |waveNumber|
		^(this.radius * waveNumber)
	}

	// Return effective order.
	orderAtFreq { |freq, speedOfSound = (AtkHoa.speedOfSound)|
		^(2 * pi * this.radius * freq) / speedOfSound
	}

	// Return effective order.
	orderAtWn { |wn, sampleRate, speedOfSound = (AtkHoa.speedOfSound)|
		^(pi * this.radius * wn * sampleRate) / speedOfSound
	}

	// ----------
	// Wavenumber / frequency

	// Return effective wavenumber.
	waveNumber { |order = (AtkHoa.defaultOrder)|
		^(order / this.radius)
	}

	// Return effective frequency.
	freq { |order = (AtkHoa.defaultOrder), speedOfSound = (AtkHoa.speedOfSound)|
		^(order * speedOfSound) / (2 * pi * this.radius)
	}

	// Return effective normalized frequency.
	wn { |sampleRate, order = (AtkHoa.defaultOrder), speedOfSound = (AtkHoa.speedOfSound)|
		^(order * speedOfSound) / (pi * this.radius * sampleRate)
	}

}
