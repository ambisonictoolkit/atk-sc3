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
		var hoaOrder = order.asHoaOrder;
		var freqs, coeffs;

		// function to return Complex of two Signals, from Array of complex values
		var reshapeComplexArray = { arg cArr;
			var arr = cArr.collect({ arg item; [ item.real, item.imag ] }).flop;
			Complex.new(
				arr.at(0).as(Signal),  // real
				arr.at(1).as(Signal)  // imag
			)
		};

		^(size.isPowerOfTwo).if({  // rfft
			var rfftsize = (size/2 + 1).asInteger;
			var cosTable = Signal.rfftCosTable(rfftsize);

			freqs = rfftsize.rfftFreqs(sampleRate);
			coeffs = freqs.collectAs({ arg freq;  // complex coefficients
				hoaOrder.distWeights(freq, radius, speedOfSound);
			},
				List
			).flop.asArray;  // collect as List, due to Array -flop bug

			// synthesize kernels
			coeffs.collect({ arg item;
				var complex = reshapeComplexArray.value(item);
				complex.real.irfft(complex.imag, cosTable)
			})
		}, {  // dft
			freqs = size.dftFreqs(sampleRate);
			coeffs = freqs.collectAs({ arg freq;  // complex coefficients
				hoaOrder.distWeights(freq, radius, speedOfSound)
			},
				List
			).flop.asArray;  // collect as List, due to Array -flop bug

			// synthesize kernels
			coeffs.collect({ arg item;
				var complex = reshapeComplexArray.value(item);
				complex.real.idft(complex.imag).real
			})
		})
	}

	*hoaCtrl { |size, encRadius = (AtkHoa.refRadius), decRadius = (AtkHoa.refRadius), order = (AtkHoa.defaultOrder), sampleRate = nil, speedOfSound = (AtkHoa.speedOfSound)|
		var hoaOrder = order.asHoaOrder;
		var freqs, coeffs;

		// function to return Complex of two Signals, from Array of complex values
		var reshapeComplexArray = { arg cArr;
			var arr = cArr.collect({ arg item; [ item.real, item.imag ] }).flop;
			Complex.new(
				arr.at(0).as(Signal),  // real
				arr.at(1).as(Signal)  // imag
			)
		};

		^(size.isPowerOfTwo).if({  // rfft
			var rfftsize = (size/2 + 1).asInteger;
			var cosTable = Signal.rfftCosTable(rfftsize);

			freqs = rfftsize.rfftFreqs(sampleRate);
			coeffs = freqs.collect({ arg freq;  // complex coefficients
				hoaOrder.ctrlWeights(freq, encRadius, decRadius, speedOfSound)
			},
				List
			).flop.asArray;  // collect as List, due to Array -flop bug

			// synthesize kernels
			coeffs.collect({ arg item;
				var complex = reshapeComplexArray.value(item);
				complex.real.irfft(complex.imag, cosTable)
			})
		}, {  // dft
			freqs = size.dftFreqs(sampleRate);
			coeffs = freqs.collectAs({ arg freq;  // complex coefficients
				hoaOrder.ctrlWeights(freq, encRadius, decRadius, speedOfSound)
			},
				List
			).flop.asArray;  // collect as List, due to Array -flop bug

			// synthesize kernels
			coeffs.collect({ arg item;
				var complex = reshapeComplexArray.value(item);
				complex.real.idft(complex.imag).real
			})
		})
	}

	*hoaFocl { |size, radius = (AtkHoa.refRadius), order = (AtkHoa.defaultOrder), window = \reg, sampleRate = nil, speedOfSound = (AtkHoa.speedOfSound)|
		var hoaOrder = order.asHoaOrder;
		var freqs, magnitudes, pha, complexes;

		^(size.isPowerOfTwo).if({  // rfft
			var rfftsize = (size/2 + 1).asInteger;
			var cosTable = Signal.rfftCosTable(rfftsize);

			freqs = rfftsize.rfftFreqs(sampleRate);

			// magnitude - collected by degree
			magnitudes = freqs.collectAs({ arg freq;  // +frequencies
				hoaOrder.foclWeights(freq, radius, window, speedOfSound)
			},
				List
			).flop.asArray;  // collect as List, due to Array -flop bug
			magnitudes = magnitudes.collect({ arg item;  // mirror +/-frequencies
				item.mirror1
			});

			// linear phase
			complexes = magnitudes.collect({ arg magnitude;
				var complex = Spectrum.new(magnitude).linearPhase.asComplex;
				complex.real.fftToRfft(complex.imag);  // convert to rcomplex
			});

			// synthesize kernels
			complexes.collect({ arg item;
				item.real.irfft(item.imag, cosTable)
			})
		}, {  // dft
			freqs = size.dftFreqs(sampleRate);

			// magnitude - collected by degree
			magnitudes = freqs.collectAs({ arg freq;  // real coefficients (magnitudes)
				hoaOrder.foclWeights(freq, radius, window, speedOfSound);
			},
				List
			).flop.asArray;  // collect as List, due to Array -flop bug

			// linear phase
			complexes = magnitudes.collect({ arg magnitude;
				Spectrum.new(magnitude).linearPhase.asComplex
			});

			// synthesize kernels
			complexes.collect({ arg item;
				item.real.idft(item.imag).real
			})
		})
	}

}
