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
			freqs = freqs.collect({ |freq|  // blt frequency warp
				sampleRate / pi * tan(pi * freq / sampleRate)
			});

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

			magnitudes = magnitudes.collect({ |item|  // mirror +/-frequencies
				item.mirror1
			});
			phases = phases.collect({ |item|  // mirror +/-frequencies
				item ++ (item.deepCopy.reverse.drop(1).drop(-1).neg)
			})
		}, {  // dft
			freqs = size.dftFreqs(sampleRate);
			freqs = freqs.collect({ |freq|  // blt frequency warp
				sampleRate / pi * tan(pi * freq / sampleRate)
			});

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
			freqs = freqs.collect({ |freq|  // blt frequency warp
				sampleRate / pi * tan(pi * freq / sampleRate)
			});

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

			magnitudes = magnitudes.collect({ |item|  // mirror +/-frequencies
				item.mirror1
			});
			phases = phases.collect({ |item|  // mirror +/-frequencies
				item ++ (item.deepCopy.reverse.drop(1).drop(-1).neg)
			})
		}, {  // dft
			freqs = size.dftFreqs(sampleRate);
			freqs = freqs.collect({ |freq|  // blt frequency warp
				sampleRate / pi * tan(pi * freq / sampleRate)
			});

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
			freqs = freqs.collect({ |freq|  // blt frequency warp
				sampleRate / pi * tan(pi * freq / sampleRate)
			});

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

			magnitudes = magnitudes.collect({ |item|  // mirror +/-frequencies
				item.mirror1
			});
			phases = phases.collect({ |item|  // mirror +/-frequencies
				item ++ (item.deepCopy.reverse.drop(1).drop(-1).neg)
			})
		}, {  // dft
			freqs = size.dftFreqs(sampleRate);
			freqs = freqs.collect({ |freq|  // blt frequency warp
				sampleRate / pi * tan(pi * freq / sampleRate)
			});

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
			magnitudes = freqs.collectAs({ |freq|  // +frequencies
				hoaOrder.foclWeights(freq, radius, window, speedOfSound)
			},
				List
			).flop.asArray;  // collect as List, due to Array -flop bug
			magnitudes = magnitudes.collect({ |item|  // mirror +/-frequencies
				item.mirror1
			})
		}, {  // dft
			freqs = size.dftFreqs(sampleRate);

			// magnitude - collected by degree
			magnitudes = freqs.collectAs({ |freq|  // real coefficients (magnitudes)
				hoaOrder.foclWeights(freq, radius, window, speedOfSound);
			},
				List
			).flop.asArray;  // collect as List, due to Array -flop bug
		});
		^magnitudes.collect({ |magnitude|
			Spectrum.new(magnitude)
		})
	}

	*hoaMultiBandFocl { |size, radius = nil, beamDict = nil, dim = 3, match = \amp, numChans = nil, order = (AtkHoa.defaultOrder), window = \reg, sampleRate = nil, speedOfSound = (AtkHoa.speedOfSound)|
		// functions to determine meanE & matchWeight from beam weights
		var meanE = { |beamWeights, dim = 3|
			// var m = beamWeights.size - 1;  // order
			var m = order;  // order

			(dim == 2).if({
				beamWeights.removeAt(0).squared + (2 * beamWeights.squared.sum) // 2D
			}, {
				(Array.series(m + 1, 1, 2) * beamWeights.squared).sum // 3D
			}).asFloat
		};
		var matchWeight = { |beamWeights, dim = 3, match = 'amp', numChans = nil|
			// var m = beamWeights.size - 1;  // order
			var m = order;  // order
			var n;

			switch(match,
				'amp', { 1.0 },
				'rms', {
					(dim == 2).if({
						n = 2 * m + 1  // 2D
					}, {
						n = (m + 1).squared  // 3D
					});
					(n/meanE.value(beamWeights, dim)).sqrt
					},
				'energy', {
					n = numChans;
					(n/meanE.value(beamWeights, dim)).sqrt
				}
			).asFloat
		};
		var matchWeights = { |magnitudes, dim, match, numChans|
			magnitudes.flop.collect({ |beamWeights|
				matchWeight.value(beamWeights, dim, match, numChans)
			}).flop
		};
		var hoaOrder = order.asHoaOrder;
		var foclMags, beamMags, magnitudes;

		// focalisation magnitudes?
		(radius != nil).if({
			// focal magnitude
			foclMags = Spectrum.hoaFocl(size, radius, order, window, sampleRate, speedOfSound).collect({ |spectrum|
				spectrum.magnitude
			})
		}, {
			// or... just unity
			foclMags = Array.fill((order + 1) * size, { 1.0 }).reshape((order + 1), size);
		});

		// beam magnitudes
		switch(beamDict.at(\beamShapes).size,
			1, {
				beamMags = hoaOrder.beamWeights(beamDict.at(\beamShapes).first, dim)
			},
			2, {
				(beamDict.at(\edgeFreqs).size != 2).if({
					Error("Must supply two edge frequencies for a two band shelf. Supplied: % ".format(beamDict.at(\edgeFreqs).size)).throw
				}, {
					var freqs = beamDict.at(\edgeFreqs).sort;
					var beamWeights = beamDict.at(\beamShapes).collect({ |beamShape|
						hoaOrder.beamWeights(beamShape, dim)
					});
					beamMags = (order + 1).collect({ |degree|
						Spectrum.logShelf(size, freqs.at(0), freqs.at(1), beamWeights.at(0).at(degree).ampdb, beamWeights.at(1).at(degree).ampdb, sampleRate).magnitude
					})
				})
			},
			3, {
				(beamDict.at(\edgeFreqs).size != 4).if({
					Error("Must supply four edge frequencies for a two band shelf. Supplied: % ".format(beamDict.at(\edgeFreqs).size)).throw
				}, {
					var freqs = beamDict.at(\edgeFreqs).sort;
					var beamWeights = beamDict.at(\beamShapes).collect({ |beamShape|
						hoaOrder.beamWeights(beamShape, dim)
					});
					beamMags = (order + 1).collect({ |degree|
						Spectrum.logShelf(size, freqs.at(0), freqs.at(1), beamWeights.at(0).at(degree).ampdb, beamWeights.at(1).at(degree).ampdb, sampleRate).magnitude *
						Spectrum.logShelf(size, freqs.at(2), freqs.at(3), 0.0, (beamWeights.at(2).at(degree) / beamWeights.at(1).at(degree)).ampdb, sampleRate).magnitude
					})
				})
			}
		);

		// normalization - two cases
		match.asString.beginsWith("f").if({  // include focalisation
			magnitudes = foclMags * beamMags;
			magnitudes = magnitudes * matchWeights.value(magnitudes, dim, match.asString.drop(1).asSymbol, numChans)
		}, {  // exclude focalisation
			magnitudes = foclMags * beamMags * matchWeights.value(beamMags, dim, match, numChans)
		});
		^magnitudes.collect({ |magnitude|
			Spectrum.new(magnitude)
		})
	}

}
