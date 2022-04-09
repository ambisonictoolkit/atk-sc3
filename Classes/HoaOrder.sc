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
	var <order;

	*new { |order = (AtkHoa.defaultOrder)|
		^super.newCopyArgs(order)
	}

	// ------------
	// Return l, m

	l {
		^(this.order + 1).collect({ |degree|
			HoaDegree.new(degree).l
		}).flatten
	}

	m {
		^(this.order + 1).collect({ |degree|
			HoaDegree.new(degree).m
		}).flatten
	}

	lm {
		^(this.order + 1).collect({ |degree|
			HoaDegree.new(degree).lm
		}).flatten
	}

	// ------------
	// Return indices

	indices { |ordering = \acn, subset = \all|
		if(subset == \all, {
			// all
			^(this.lm).collect({ |lm|
				HoaLm.new(lm).index(ordering)
			})
		}, {
			// subset
			^(this.lm).collect({ |lm|
				var hoaLm = HoaLm.new(lm);

				if(hoaLm.isInSubset(subset), {
					hoaLm.index(ordering)
				})
			}).removeEvery([nil])
		})
	}

	// ------------
	// Return reflection coefficients

	reflection { |mirror = \reflect|
		^(this.lm).collect({ |lm|
			HoaLm.new(lm).reflection(mirror)
		})
	}

	// ------------
	// Return normalisation coefficients

	normalisation { |scheme = \n3d|
		^(this.lm).collect({ |lm|
			HoaLm.new(lm).normalisation(scheme)
		})
	}

	// ------------
	// Return angular encoding coefficients

	// N3D normalized coefficients
	sph { |theta = 0, phi = 0|
		^(this.lm).collect({ |lm|
			HoaLm.new(lm).sph(theta, phi)
		})
	}

	// ------------
	// Return NFE coefficients

	// Proximity complex degree weights
	proxWeights { |freq, radius = (AtkHoa.refRadius), speedOfSound = (AtkHoa.speedOfSound)|
		^WaveNumber.newFreq(freq, speedOfSound).proxWeights(radius, this.order)
	}

	// Distance complex degree weights
	distWeights { |freq, radius = (AtkHoa.refRadius), speedOfSound = (AtkHoa.speedOfSound)|
		^WaveNumber.newFreq(freq, speedOfSound).distWeights(radius, this.order)
	}

	// Control complex degree weights
	ctrlWeights { |freq, encRadius = (AtkHoa.refRadius), decRadius = (AtkHoa.refRadius), speedOfSound = (AtkHoa.speedOfSound)|
		^WaveNumber.newFreq(freq, speedOfSound).ctrlWeights(encRadius, decRadius, this.order)
	}

	// Focalisation (real) degree weights
	foclWeights { |freq, radius = (AtkHoa.refRadius), window = \reg, speedOfSound = (AtkHoa.speedOfSound)|
		var wavNum = WaveNumber.newFreq(freq, speedOfSound);
		var effOrder;  //  hp, cos
		var beta;  // hp

		^switch(window,
			\hp, {
				effOrder = wavNum.orderAtRadius(radius);

				(this.order + 1).collect({ |degree|
					if(degree == 0, {
						1.0
					}, {
						beta = (effOrder / degree).pow(4 * degree);
						beta / (1.0 + beta)
					})
				})
			},
			\cos, {
				effOrder = wavNum.orderAtRadius(radius).abs;

				(this.order + 1).collect({ |degree|
					if(degree == 0, {
						1.0
					}, {
						if(degree > effOrder, {
							0.0
						}, {
							((pi * degree / effOrder).cos + 1.0) / 2.0
						})
					})
				})
			},
			\sin, {
				this.foclWeights(freq, radius, \cos, speedOfSound).sqrt
			},
			{  // \reg
				2.0 / (1.0 + wavNum.proxWeights(radius, this.order).abs.squared)
			}
		)
	}

	// ------------
	// Return prototype complex wave N3D normalized encoding coefficients
	/*
	TODO:

	As _Array of Complex_ or _Complex of Arrays_ ?

	The latter is more convenient for analysis.
	*/

	// Monofrequent travelling
	/*
	NOTE: normalized to Wp
	*/
	travelling { |freq, phase = 0, theta = 0, phi = 0, radius = (AtkHoa.refRadius), refRadius = (AtkHoa.refRadius), speedOfSound = (AtkHoa.speedOfSound)|
		var angularWeights = this.sph(theta, phi);
		var radialWeights;
		var complex;

		radialWeights = (refRadius == inf).if({
			(freq == 0.0).if({  // force DC to planewave to avoid overflow (-inf)
				Complex.new(1.0, 0.0).dup(this.order + 1)
			}, {
				this.proxWeights(freq, radius, speedOfSound)
			})
		}, {
			this.ctrlWeights(freq, radius, refRadius, speedOfSound)
		})[this.l];
		radialWeights = Complex.new(radialWeights.real, radialWeights.imag);  // reshape

		// multiply... seems to be quicker in sclang
		complex = Complex.new(
			angularWeights * radialWeights.real,
			angularWeights * radialWeights.imag
		) * Complex.new(phase.cos, phase.sin);

		^complex
	}

	// Monofrequent standing: diametric planewaves
	/*
	NOTE: normalized to PU energy mean, Ws
	NOTE: CCE algo scaled by 0.5 to normalize peak Wp, Wu
	*/
	diametric { |freq, phase = 0, theta = 0, phi = 0, beta = 0, refRadius = (AtkHoa.refRadius), speedOfSound = (AtkHoa.speedOfSound)|
		var reflecWeights = this.reflection;
		var halfPhaseDiff = 0.5 * (0.5pi - beta);
		var betaPhaseWeight = Complex.new(halfPhaseDiff.cos, halfPhaseDiff.neg.sin);
		var betaPhaseWeightConj = betaPhaseWeight.conjugate;
		var radius = inf;
		var travelling = this.travelling(freq, phase, theta, phi, radius, refRadius, speedOfSound);
		var normFac = 0.5.sqrt;  // normalize Ws
		// var normFac = 0.5;  // normalize max(Wp) || max(Wu)

		// normFac * travelling * (betaPhaseWeight + (reflecWeights * betaPhaseWeight.conjugate))
		^(
			travelling * (
				Complex.new(  // seems to be quicker in sclang
					normFac * (betaPhaseWeight.real + (reflecWeights * betaPhaseWeightConj.real)),
					normFac * (betaPhaseWeight.imag + (reflecWeights * betaPhaseWeightConj.imag))
				)
			)
		)
	}

	// Monofrequent diffuse(-ish)
	/*
	NOTE: normalized Wp, Ws, Ws
	*/
	modalDiffuse { |freq, phase = 0, refRadius = (AtkHoa.refRadius), speedOfSound = (AtkHoa.speedOfSound)|
		var radialWeights;
		var modalPhase;
		var complex;

		radialWeights = (refRadius == inf).if({  // planewaves
			Complex.new(1.0, 0.0).dup(this.order + 1)  // could use -proxWeights, but this is quicker
		}, {
			this.distWeights(freq, refRadius, speedOfSound)  // equivalent to: -ctrlWeights(freq, inf, refRadius)
		})[this.l];
		radialWeights = Complex.new(radialWeights.real, radialWeights.imag);  // reshape

		modalPhase = this.size.collect({ pi.rand2 });
		modalPhase = modalPhase - modalPhase[0] + phase;

		// multiply...
		complex = radialWeights * Complex.new(modalPhase.cos, modalPhase.sin);

		^complex
	}

	/*
	NOTE: normalized Ws
	*/
	angularDiffuse { |freq, phase = 0, design = nil, refRadius = (AtkHoa.refRadius), speedOfSound = (AtkHoa.speedOfSound)|
		var zeros = Array.zeroFill(this.size);
		var radialWeights, angularWeights;
		var angularPhase;
		var complex;
		// Ws analysis vars
		var wsSqrtReciprocal;
		var degree0 = HoaDegree.new(0).indices;
		var degree1 = HoaDegree.new(1).indices;

		^switch(design.class,
			TDesign, {    // TDesign only, for now

				radialWeights = (refRadius == inf).if({  // planewaves
					Complex.new(1.0, 0.0).dup(this.order + 1)  // could use -proxWeights, but this is quicker
				}, {
					this.distWeights(freq, refRadius, speedOfSound)  // equivalent to: -ctrlWeights(freq, inf, refRadius)
				})[this.l];
				radialWeights = Complex.new(radialWeights.real, radialWeights.imag);  // reshape

				angularWeights = HoaMatrixEncoder.newSphericalDesign(design, \basic, this.order).matrix;
				angularPhase = design.size.collect({ pi.rand2 });

				// encode angular
				complex = Complex.new(zeros, zeros);
				design.size.do({ |i|
					var angularCoeffs = angularWeights.getCol(i);
					complex = complex + Complex.new(
							angularCoeffs * angularPhase.cos[i],
							angularCoeffs * angularPhase.sin[i],
						)
				});
				// normalize phase
				complex = complex.rotate(
					Complex.new(complex.real[0], complex.imag[0]).phase.neg + phase
				);
				/*
				TODO: Ws analysis via PUC
				*/
				// normalize Ws
				wsSqrtReciprocal = [
					Complex.new(
						complex.real[degree0],
						complex.imag[degree0]
					).squared.magnitude.mean,
					Complex.new(
						complex.real[degree1],
						complex.imag[degree1]
					).squared.magnitude.mean
				].mean.sqrt.reciprocal;
				complex = Complex.new(
					wsSqrtReciprocal * complex.real,
					wsSqrtReciprocal * complex.imag
				);

				// encode radial, weight as planewaves
				complex = radialWeights * complex;

				complex
			},
			{  // ... or, catch un-supported
				format(
					"[HoaOrder *angularDiffuse] Design % is not supported!",
					design.class
				).throw
			}
		)
	}

	// ------------
	// Return decoder measures or coefficients

	// effective decoding radius
	radiusAtFreq { |freq, speedOfSound = (AtkHoa.speedOfSound)|
		^(this.order * speedOfSound) / (2 * pi * freq)
	}

	// effective decoding frequency
	freqAtRadius { |radius = (AtkHoa.refRadius), speedOfSound = (AtkHoa.speedOfSound)|
		^(this.order * speedOfSound) / (2 * pi * radius)
	}

	/*
	NOTE:

	May want to review the naming of the var m
	in the below code. This isn't the same m as [l, m],
	and may cause confusion!
	*/

	// maximum average rV for an Ambisonic decoder
	rV { |beamShape = \basic, dim = 3|
		var m = this.order;

		^switch(beamShape,
			\basic, { 1 },
			\energy, { this.rE(beamShape, dim) },
			\controlled, {
				if(dim == 2, {
					m / (m + 1)  // 2D
				}, {
					m / (m + 2)  // 3D
				})
			}
		)
	}

	// maximum average rE for an Ambisonic decoder
	rE { |beamShape = \basic, dim = 3|
		var m = this.order;

		^if(beamShape == \energy, {
			if(dim == 2, {
				chebyshevTZeros(m + 1).maxItem  // 2D
			}, {
				legendrePZeros(m + 1).maxItem  // 3D
			})
		}, {  // 'basic' & 'controlled'
			if(dim == 2, {
				(2 * m) / (2 * m + 1)  // 2D
			}, {
				m / (m + 1)  // 3D
			})
		})
	}

	// 1/2 angle maximum average energy spread for an Ambisonic decoder
	spreadE { |beamShape = \basic, dim = 3|
		var rE = this.rE(beamShape, dim);

		^IdentityDictionary.with(*[
			\cos->rE.acos,  // Zotter & Frank: ~-3dB
			\hvc->((2 * rE) - 1).acos  // Carpentier, Politis: ~-6dB
		]).know_(true)
	}

	// 'l’énergie réduite E' for an Ambisonic decoder
	meanE { |beamShape = \basic, dim = 3|
		var m = this.order;
		var beamWeights = this.beamWeights(beamShape, dim);

		^if(dim == 2, {
			beamWeights.removeAt(0).squared + (2 * beamWeights.squared.sum) // 2D
		}, {
			(Array.series(m + 1, 1, 2) * beamWeights.squared).sum // 3D
		}).asFloat
	}

	// 'matching gain' (scale) for a given Ambisonic decoder
	matchWeight { |beamShape = \basic, dim = 3, match = \amp, numChans = nil|
		var m = this.order;
		var n;

		^switch(match,
			\amp, { 1.0 },
			\rms, {
				if(dim == 2, {
					n = 2 * m + 1  // 2D
				}, {
					n = (m + 1).squared  // 3D
				});
				(n / this.meanE(beamShape, dim)).sqrt
			},
			\energy, {
				n = numChans;
				(n / this.meanE(beamShape, dim)).sqrt
			}
		).asFloat
	}

	// beamWeights, aka, "decoder order gains" or Gamma vector of per-degree (beam forming) scalars
	beamWeights { |beamShape = \basic, dim = 3|
		var m = this.order;
		var max_rE;  // energy

		^switch(beamShape,
			\basic, { 1.dup(m + 1) },
			\energy, {
				max_rE = this.rE(beamShape, dim);
				if(dim == 2, { // 2D
					(m + 1).collect({ |degree|
						chebyshevT(degree, max_rE)
					})
				}, { // 3D
					(m + 1).collect({ |degree|
						legendreP(degree, max_rE)
					})
				})
			},
			\controlled, {
				if(dim == 2, { // 2D
					(m + 1).collect({ |degree|
						1 / ((m + degree).asFloat.factorial * (m - degree).asFloat.factorial)
					}) * m.asFloat.factorial.squared
				}, { // 3D
					(m + 1).collect({ |degree|
						1 / ((m + degree + 1).asFloat.factorial * (m - degree).asFloat.factorial)
					}) * m.asFloat.factorial * (m + 1).asFloat.factorial
				})
			}
		)
	}

	size {
		^(this.order + 1).squared
	}
}
