/*
	Copyright the ATK Community and Joseph Anderson, 2011-2017
		J Anderson	j.anderson[at]ambisonictoolkit.net
        M McCrea    mtm5[at]uw.edu

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
// 	Extension: Signal
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

+ Signal {

	*hoaDist { |size, radius = (AtkHoa.refRadius), order = (AtkHoa.defaultOrder), sampleRate = nil, speedOfSound = (AtkHoa.speedOfSound)|
		var complexes = Spectrum.hoaDist(size, radius, order, sampleRate, speedOfSound).collect({ |spectrum|
			spectrum.asComplex
		});

		^(size.isPowerOfTwo).if({  // rfft
			var rfftsize = (size/2 + 1).asInteger;
			var cosTable = Signal.rfftCosTable(rfftsize);

			// synthesize kernels
			complexes.collect({ |complex|
				var rcomplex = complex.real.fftToRfft(complex.imag);
				rcomplex.real.irfft(rcomplex.imag, cosTable)
			})
		}, {  // dft
			// synthesize kernels
			complexes.collect({ |complex|
				complex.real.idft(complex.imag).real
			})
		})
	}

	*hoaCtrl { |size, encRadius = (AtkHoa.refRadius), decRadius = (AtkHoa.refRadius), order = (AtkHoa.defaultOrder), sampleRate = nil, speedOfSound = (AtkHoa.speedOfSound)|
		var complexes = Spectrum.hoaCtrl(size, encRadius, decRadius, order, sampleRate, speedOfSound).collect({ |spectrum|
			spectrum.asComplex
		});

		^(size.isPowerOfTwo).if({  // rfft
			var rfftsize = (size/2 + 1).asInteger;
			var cosTable = Signal.rfftCosTable(rfftsize);

			// synthesize kernels
			complexes.collect({ |complex|
				var rcomplex = complex.real.fftToRfft(complex.imag);
				rcomplex.real.irfft(rcomplex.imag, cosTable)
			})
		}, {  // dft
			// synthesize kernels
			complexes.collect({ |complex|
				complex.real.idft(complex.imag).real
			})
		})
	}

	*hoaFocl { |size, radius = (AtkHoa.refRadius), order = (AtkHoa.defaultOrder), window = \reg, sampleRate = nil, speedOfSound = (AtkHoa.speedOfSound)|
		var complexes = Spectrum.hoaFocl(size, radius, order, window, sampleRate, speedOfSound).collect({ |spectrum|
			spectrum.linearPhase.asComplex  // linear phase
		});

		^(size.isPowerOfTwo).if({  // rfft
			var rfftsize = (size/2 + 1).asInteger;
			var cosTable = Signal.rfftCosTable(rfftsize);

			// synthesize kernels
			complexes.collect({ |complex|
				var rcomplex = complex.real.fftToRfft(complex.imag);
				rcomplex.real.irfft(rcomplex.imag, cosTable)
			})
		}, {  // dft
			// synthesize kernels
			complexes.collect({ |complex|
				complex.real.idft(complex.imag).real
			})
		})
	}

	*hoaMultiFocl { |size, radius = nil, beamDict = nil, dim = 3, match = \amp, numChans = nil, order = (AtkHoa.defaultOrder), window = \reg, sampleRate = nil, speedOfSound = (AtkHoa.speedOfSound)|
		var complexes = Spectrum.hoaMultiFocl(size, radius, beamDict, dim, match, numChans, order, window, sampleRate, speedOfSound).collect({ |spectrum|
			spectrum.linearPhase.asComplex  // linear phase
		});

		^(size.isPowerOfTwo).if({  // rfft
			var rfftsize = (size/2 + 1).asInteger;
			var cosTable = Signal.rfftCosTable(rfftsize);

			// synthesize kernels
			complexes.collect({ |complex|
				var rcomplex = complex.real.fftToRfft(complex.imag);
				rcomplex.real.irfft(rcomplex.imag, cosTable)
			})
		}, {  // dft
			// synthesize kernels
			complexes.collect({ |complex|
				complex.real.idft(complex.imag).real
			})
		})
	}

}
