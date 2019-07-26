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
// 	Extension: Spectrum
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

+ Spectrum {

	*hoaProx { |size, radius = (AtkHoa.refRadius), order = (AtkHoa.defaultOrder), sampleRate = nil, speedOfSound = (AtkHoa.speedOfSound)|
		var hoaOrder = order.asHoaOrder;
		var freqs, complexCoeffs, magnitudes, phases;

		(size.isPowerOfTwo).if({  // rfft
			var rfftsize = (size/2 + 1).asInteger;
			freqs = rfftsize.rfftFreqs(sampleRate);

			complexCoeffs = freqs.collectAs({ |freq|  // complex coefficients
				hoaOrder.proxWeights(freq, radius, speedOfSound)
			},
				List
			).flop.asArray;  // collect as List, due to Array -flop bug

			magnitudes = complexCoeffs.collect({ |degree|
				degree.collect({ |complex|
					complex.magnitude
				})
			});
			phases = complexCoeffs.collect({ |degree|
				degree.collect({ |complex|
					complex.phase
				})
			});

			magnitudes = magnitudes.collect({ arg item;  // mirror +/-frequencies
				item.mirror1
			});
			phases = phases.collect({ arg item;  // mirror +/-frequencies
				item ++ (item.deepCopy.reverse.drop(1).drop(-1).neg)
			})
		}, {  // dft
			freqs = size.dftFreqs(sampleRate);

			complexCoeffs = freqs.collectAs({ |freq|  // complex coefficients
				hoaOrder.proxWeights(freq, radius, speedOfSound)
			},
				List
			).flop.asArray;  // collect as List, due to Array -flop bug

			magnitudes = complexCoeffs.collect({ |degree|
				degree.collect({ |complex|
					complex.magnitude
				})
			});
			phases = complexCoeffs.collect({ |degree|
				degree.collect({ |complex|
					complex.phase
				})
			});
		});
		^(order + 1).collect({ |degree|
			Spectrum.new(magnitudes.at(degree), phases.at(degree))
		})
	}

	*hoaDist { |size, radius = (AtkHoa.refRadius), order = (AtkHoa.defaultOrder), sampleRate = nil, speedOfSound = (AtkHoa.speedOfSound)|
		var hoaOrder = order.asHoaOrder;
		var freqs, complexCoeffs, magnitudes, phases;

		(size.isPowerOfTwo).if({  // rfft
			var rfftsize = (size/2 + 1).asInteger;
			freqs = rfftsize.rfftFreqs(sampleRate);

			complexCoeffs = freqs.collectAs({ |freq|  // complex coefficients
				hoaOrder.distWeights(freq, radius, speedOfSound)
			},
				List
			).flop.asArray;  // collect as List, due to Array -flop bug

			magnitudes = complexCoeffs.collect({ |degree|
				degree.collect({ |complex|
					complex.magnitude
				})
			});
			phases = complexCoeffs.collect({ |degree|
				degree.collect({ |complex|
					complex.phase
				})
			});

			magnitudes = magnitudes.collect({ arg item;  // mirror +/-frequencies
				item.mirror1
			});
			phases = phases.collect({ arg item;  // mirror +/-frequencies
				item ++ (item.deepCopy.reverse.drop(1).drop(-1).neg)
			})
		}, {  // dft
			freqs = size.dftFreqs(sampleRate);

			complexCoeffs = freqs.collectAs({ |freq|  // complex coefficients
				hoaOrder.distWeights(freq, radius, speedOfSound)
			},
				List
			).flop.asArray;  // collect as List, due to Array -flop bug

			magnitudes = complexCoeffs.collect({ |degree|
				degree.collect({ |complex|
					complex.magnitude
				})
			});
			phases = complexCoeffs.collect({ |degree|
				degree.collect({ |complex|
					complex.phase
				})
			});
		});
		^(order + 1).collect({ |degree|
			Spectrum.new(magnitudes.at(degree), phases.at(degree))
		})
	}

	*hoaCtrl { |size, encRadius = (AtkHoa.refRadius), decRadius = (AtkHoa.refRadius), order = (AtkHoa.defaultOrder), sampleRate = nil, speedOfSound = (AtkHoa.speedOfSound)|
		var hoaOrder = order.asHoaOrder;
		var freqs, complexCoeffs, magnitudes, phases;

		(size.isPowerOfTwo).if({  // rfft
			var rfftsize = (size/2 + 1).asInteger;
			freqs = rfftsize.rfftFreqs(sampleRate);

			complexCoeffs = freqs.collectAs({ |freq|  // complex coefficients
				hoaOrder.ctrlWeights(freq, encRadius, decRadius, speedOfSound)
			},
				List
			).flop.asArray;  // collect as List, due to Array -flop bug

			magnitudes = complexCoeffs.collect({ |degree|
				degree.collect({ |complex|
					complex.magnitude
				})
			});
			phases = complexCoeffs.collect({ |degree|
				degree.collect({ |complex|
					complex.phase
				})
			});

			magnitudes = magnitudes.collect({ arg item;  // mirror +/-frequencies
				item.mirror1
			});
			phases = phases.collect({ arg item;  // mirror +/-frequencies
				item ++ (item.deepCopy.reverse.drop(1).drop(-1).neg)
			})
		}, {  // dft
			freqs = size.dftFreqs(sampleRate);

			complexCoeffs = freqs.collectAs({ |freq|  // complex coefficients
				hoaOrder.ctrlWeights(freq, encRadius, decRadius, speedOfSound)
			},
				List
			).flop.asArray;  // collect as List, due to Array -flop bug

			magnitudes = complexCoeffs.collect({ |degree|
				degree.collect({ |complex|
					complex.magnitude
				})
			});
			phases = complexCoeffs.collect({ |degree|
				degree.collect({ |complex|
					complex.phase
				})
			});
		});
		^(order + 1).collect({ |degree|
			Spectrum.new(magnitudes.at(degree), phases.at(degree))
		})
	}

	*hoaFocl { |size, radius = (AtkHoa.refRadius / 2), order = (AtkHoa.defaultOrder), window = \reg, sampleRate = nil, speedOfSound = (AtkHoa.speedOfSound)|
		var hoaOrder = order.asHoaOrder;
		var freqs, magnitudes;

		(size.isPowerOfTwo).if({  // rfft
			var rfftsize = (size/2 + 1).asInteger;
			freqs = rfftsize.rfftFreqs(sampleRate);

			// magnitude - collected by degree
			magnitudes = freqs.collectAs({ arg freq;  // +frequencies
				hoaOrder.foclWeights(freq, radius, window, speedOfSound)
			},
				List
			).flop.asArray;  // collect as List, due to Array -flop bug
			magnitudes = magnitudes.collect({ arg item;  // mirror +/-frequencies
				item.mirror1
			})
		}, {  // dft
			freqs = size.dftFreqs(sampleRate);

			// magnitude - collected by degree
			magnitudes = freqs.collectAs({ arg freq;  // real coefficients (magnitudes)
				hoaOrder.foclWeights(freq, radius, window, speedOfSound);
			},
				List
			).flop.asArray;  // collect as List, due to Array -flop bug
		});
		^magnitudes.collect({ |magnitude|
			Spectrum.new(magnitude)
		})
	}

	// *hoaMultiFocl { |size, radius = (AtkHoa.refRadius), beamDict = nil, dim = 3, match = \amp, numChans = nil, order = (AtkHoa.defaultOrder), window = \reg, sampleRate = nil, speedOfSound = (AtkHoa.speedOfSound)|
	// }

}
