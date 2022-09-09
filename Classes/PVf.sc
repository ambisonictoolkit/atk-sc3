/*
This file is part of the DRC quark for SuperCollider 3 and is free software:
you can redistribute it and/or modify it under the terms of the GNU General
Public License as published by the Free Software Foundation, either version 3
of the License, or (at your option) any later version.

This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
See the GNU General Public License for more details.
<http://www.gnu.org/licenses/>.
*/


//---------------------------------------------------------------------
//
// 	Class: PVf
//
//---------------------------------------------------------------------

/*
PUF: pressure-velocity frequency domain
*/

/*
TODO:

- refactor PUC as superclass

- refactor synthesis --> Signal class?
*/

/*
- consider creating Hoa version of: FoaEval.reg
-- could use AtkHoa.thresh
*/


PVf[slot] : PVc  {

	blockNorm { ^2 / this.numFrames }

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	// Synthesis

	// Monofrequent
	/*
	travelling
	diametric
	pressure
	*/

	// Travelling
	/*
	NOTE: pressure is scaled --> magnitude
	NOTE: only supports powerOfTwo
	NOTE: supply (size / 2) + 1 args: real
	NOTE: catch arg size mismatch errors

	TODO: integrate more closely w/ FreqSpectrum --> magnitude, phase args
	TODO: default args if nil
	*/
	*newTravelling { |size, magnitude, phase, theta, phi, radius, sampleRate = nil, speedOfSound = (AtkHoa.speedOfSound)|
		var hoaOrder = order.asHoaOrder;
		var refRadius = inf;
		var freqs, complex;
		var puf;
		var rfftsize;
		var sourceNorm = \n3d;
		var targetNorm = \sn3d;
		var targetOrdering = \sid;
		var normFac = hoaOrder.normalisation(targetNorm) / hoaOrder.normalisation(sourceNorm);
		var indicesFromACN = hoaOrder.indices(targetOrdering).order;

		if(size.isPowerOfTwo, {  // rfft
			rfftsize = (size / 2 + 1).asInteger;

			puf = Array.fill(hoaOrder.size, {  // empty
				Complex.new(Signal.newClear(rfftsize), Signal.newClear(rfftsize))
			});

			freqs = rfftsize.rfftFreqs(sampleRate);
			freqs = freqs.collect({ |freq|  // blt frequency warp
				sampleRate / pi * tan(pi * freq / sampleRate)
			});

			rfftsize.do({ |i|  // complex coefficients
				complex = magnitude[i] * hoaOrder.travelling(freqs[i], phase[i], theta[i], phi[i], radius[i], refRadius, speedOfSound);
				hoaOrder.size.do({ |j|
					puf[j].real.put(i, complex.real[j]);
					puf[j].imag.put(i, complex.imag[j])
				})
			});

			// convert from rfft to fft
			puf = puf.collect({ |item| item.real.rfftToFft(item.imag) })
		}, {
			Error.new("Size = % is not supported. Use a power of two.".format(size)).throw
		});

		// normalize & reorder
		puf = (normFac * puf)[indicesFromACN];

		^super.fill(hoaOrder.size, { |i|
			puf[i]
		})
	}

	// Standing
	/*
	NOTE: only supports powerOfTwo
	NOTE: supply (size / 2) + 1 args: real
	NOTE: catch arg size mismatch errors

	TODO: integrate more closely w/ FreqSpectrum --> magnitude, phase args
	TODO: default args if nil
	*/

	*newPressure { |size, magnitude, phase|
		var lm = [ 0, 0 ];
		var hoaLm = HoaLm.new(lm);
		var hoaOrder = order.asHoaOrder;
		var angularWeight = hoaLm.sph;  // express explicitly
		var puf;
		var rfftsize;
		var sourceNorm = \n3d;
		var targetNorm = \sn3d;
		var normFac = hoaLm.normalisation(targetNorm) / hoaLm.normalisation(sourceNorm);

		if(size.isPowerOfTwo, {  // rfft
			rfftsize = (size / 2 + 1).asInteger;

			puf = Array.fill(hoaOrder.size - 1, {  // empty
				Complex.new(Signal.newClear(rfftsize), Signal.newClear(rfftsize))
			});

			// set pressure (concatenate)
			puf = [
				Complex.new(
					(magnitude * phase.cos).as(Signal),
					(magnitude * phase.sin).as(Signal)
				)
			] ++ puf;

			// convert from rfft to fft
			puf = puf.collect({ |item| item.real.rfftToFft(item.imag) })
		}, {
			Error.new("Size = % is not supported. Use a power of two.".format(size)).throw
		});

		// normalize - just pressure
		puf.put(0, normFac * puf[0]);

		^super.fill(hoaOrder.size, { |i|
			puf[i]
		})
	}

	// Standing
	/*
	NOTE: only supports powerOfTwo
	NOTE: supply (size / 2) + 1 args: real
	NOTE: catch arg size mismatch errors

	TODO: integrate more closely w/ FreqSpectrum --> magnitude, phase args
	TODO: default args if nil
	*/
	*newDiametric { |size, magnitude, phase, theta, phi, beta, sampleRate = nil, speedOfSound = (AtkHoa.speedOfSound)|
		var hoaOrder = order.asHoaOrder;
		var refRadius = inf;
		var freqs, complex;
		var puf;
		var rfftsize;
		var sourceNorm = \n3d;
		var targetNorm = \sn3d;
		var targetOrdering = \sid;
		var normFac = hoaOrder.normalisation(targetNorm) / hoaOrder.normalisation(sourceNorm);
		var indicesFromACN = hoaOrder.indices(targetOrdering).order;

		if(size.isPowerOfTwo, {  // rfft
			rfftsize = (size / 2 + 1).asInteger;

			puf = Array.fill(hoaOrder.size, {  // empty
				Complex.new(Signal.newClear(rfftsize), Signal.newClear(rfftsize))
			});

			freqs = rfftsize.rfftFreqs(sampleRate);
			freqs = freqs.collect({ |freq|  // blt frequency warp
				sampleRate / pi * tan(pi * freq / sampleRate)
			});

			rfftsize.do({ |i|  // complex coefficients
				complex = magnitude[i] * hoaOrder.diametric(freqs[i], phase[i], theta[i], phi[i], beta[i], refRadius, speedOfSound);
				hoaOrder.size.do({ |j|
					puf[j].real.put(i, complex.real[j]);
					puf[j].imag.put(i, complex.imag[j])
				})
			});

			// convert from rfft to fft
			puf = puf.collect({ |item| item.real.rfftToFft(item.imag) })
		}, {
			Error.new("Size = % is not supported. Use a power of two.".format(size)).throw
		});

		// normalize & reorder
		puf = (normFac * puf)[indicesFromACN];

		^super.fill(hoaOrder.size, { |i|
			puf[i]
		})
	}

	/*
	NOTE: only supports powerOfTwo
	NOTE: supply (size / 2) + 1 args: real
	NOTE: catch arg size mismatch errors

	TODO: integrate more closely w/ FreqSpectrum --> magnitude, phase args
	TODO: default args if nil
	*/
	/*
	NOTE: normalized Wp, Wu, Ws
	*/
	*newDiffuseModal { |size, magnitude, phase, sampleRate = nil, speedOfSound = (AtkHoa.speedOfSound)|
		var hoaOrder = order.asHoaOrder;
		var refRadius = inf;
		var freqs, complex;
		var puf;
		var rfftsize;
		var sourceNorm = \n3d;
		var targetNorm = \sn3d;
		var targetOrdering = \sid;
		var normFac = hoaOrder.normalisation(targetNorm) / hoaOrder.normalisation(sourceNorm);
		var indicesFromACN = hoaOrder.indices(targetOrdering).order;

		if(size.isPowerOfTwo, {  // rfft
			rfftsize = (size / 2 + 1).asInteger;

			puf = Array.fill(hoaOrder.size, {  // empty
				Complex.new(Signal.newClear(rfftsize), Signal.newClear(rfftsize))
			});

			freqs = rfftsize.rfftFreqs(sampleRate);
			freqs = freqs.collect({ |freq|  // blt frequency warp
				sampleRate / pi * tan(pi * freq / sampleRate)
			});

			rfftsize.do({ |i|  // complex coefficients
				complex = magnitude[i] * hoaOrder.diffuseModal(freqs[i], phase[i], refRadius, speedOfSound);
				hoaOrder.size.do({ |j|
					puf[j].real.put(i, complex.real[j]);
					puf[j].imag.put(i, complex.imag[j])
				})
			});

			// convert from rfft to fft
			puf = puf.collect({ |item| item.real.rfftToFft(item.imag) })
		}, {
			Error.new("Size = % is not supported. Use a power of two.".format(size)).throw
		});

		// normalize & reorder
		puf = (normFac * puf)[indicesFromACN];

		^super.fill(hoaOrder.size, { |i|
			puf[i]
		})
	}

	/*
	NOTE: only supports powerOfTwo
	NOTE: supply (size / 2) + 1 args: real
	NOTE: catch arg size mismatch errors

	TODO: integrate more closely w/ FreqSpectrum --> magnitude, phase args
	TODO: default args if nil
	*/
	/*
	NOTE: normalized Ws
	*/
	*newDiffuseAngular { |size, magnitude, phase, design, sampleRate = nil, speedOfSound = (AtkHoa.speedOfSound)|
		var hoaOrder = order.asHoaOrder;
		var refRadius = inf;
		var freqs, complex;
		var puf;
		var rfftsize;
		var sourceNorm = \n3d;
		var targetNorm = \sn3d;
		var targetOrdering = \sid;
		var normFac = hoaOrder.normalisation(targetNorm) / hoaOrder.normalisation(sourceNorm);
		var indicesFromACN = hoaOrder.indices(targetOrdering).order;

		if(size.isPowerOfTwo, {  // rfft
			rfftsize = (size / 2 + 1).asInteger;

			puf = Array.fill(hoaOrder.size, {  // empty
				Complex.new(Signal.newClear(rfftsize), Signal.newClear(rfftsize))
			});

			freqs = rfftsize.rfftFreqs(sampleRate);
			freqs = freqs.collect({ |freq|  // blt frequency warp
				sampleRate / pi * tan(pi * freq / sampleRate)
			});

			rfftsize.do({ |i|  // complex coefficients
				complex = magnitude[i] * hoaOrder.diffuseModal(freqs[i], phase[i], design, refRadius, speedOfSound);
				hoaOrder.size.do({ |j|
					puf[j].real.put(i, complex.real[j]);
					puf[j].imag.put(i, complex.imag[j])
				})
			});

			// convert from rfft to fft
			puf = puf.collect({ |item| item.real.rfftToFft(item.imag) })
		}, {
			Error.new("Size = % is not supported. Use a power of two.".format(size)).throw
		});

		// normalize & reorder
		puf = (normFac * puf)[indicesFromACN];

		^super.fill(hoaOrder.size, { |i|
			puf[i]
		})
	}


	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	// Encoding

	/*
	TODO: check for power of two
	TODO: update PUT -> PVt
	*/
	*newPUT { |put|
		var cosTable = Signal.fftCosTable(put.numFrames);
		var imag = Signal.zeroFill(put.numFrames);

		(put.class == PUT).if({
			^super.fill(4, { |i|
				put[i].fft(imag, cosTable)
			})
		}, {
			Error.new("Input class % != PUT!".format(put.class)).throw
		})
	}

	/*
	TODO: check for power of two
	TODO: update PUT -> PVt
	*/
	*newPVa { |pua|
		(pua.class == PVa).if({
			^this.newPUT(PUT.newPUA(pua))
		}, {
			Error.new("Input class % != PUA!".format(pua.class)).throw
		})
	}

	/*
	TODO: check for power of two
	TODO: PUT -> PVt
	*/
	*newFoa { |array|
		^this.newPUT(PUT.newFoa(array))
	}

	/*
	TODO: check for power of two
	TODO: update PUT -> PVt
	*/
	*newAmbix { |array|
		^this.newPUT(PUT.newAmbix(array))
	}


	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	// Stationary measures
	//
	/*
	TODO: use another name?
	TODO: refactor to only return coefficients for +freqs
			OR recognize I.imag is neg for -freqs
	*/

	//------------------------------------------------------------------------
	// INTENSITY - magnitudes



	//------------------------------------------------------------------------
	// INTENSITY - complex vectors

	/*
	TODO: parallel to ATK analyze names?
	TODO: defer / promote to superclass PUC??

	TODO: are negative freqs correct??
	TODO: do these align w/ HoaOrder coefficients??
	*/

	// Intensity
	// stationaryI {
	// 	var p = this.pressure;
	// 	var u = this.velocity;
	// 	^u.collect({ |item|
	// 		var i = (p * item.conjugate);
	// 		Complex.new(
	// 			i.real.as(Array),
	// 			i.imag.as(Array)
	// 		)
	// 	})
	// }
	/*
	NOTE: Negative frequencies are mirrored across origin. Doing so facilitates
	calulation of averages, negative freq radii and indicators
	*/
	/*
	TODO: add a boolean for mirror or not?
	*/
	intensity {
		var halfSize = (this.numFrames / 2).asInteger;
		var p = this.pressure;
		var u = this.velocity;
		var i, ic, icm;
		ic = u.collect({ |item|
			i = (p * item.conjugate);
			Complex.new(
				i.real.as(Array),
				i.imag.as(Array)
			)
		});
		// mirror reactive negative freqs across the origin
		icm = ic.collect({ |item|
			Complex.new(
				item.real,
				item.imag.keep(halfSize) ++ item.imag.drop(halfSize).neg
			)
		});
		^icm
	}

	//------------------------------------------------------------------------
	// SOUNDFIELD RADIUS

	// Nearfield (Spherical) Radius
	radius { |negRadius = false, sampleRate = nil, speedOfSound = (AtkHoa.speedOfSound)|
		var rfftSize = (this.numFrames / 2).asInteger + 1;
		var halfPi = 0.5pi;
		var freqs = rfftSize.rfftFreqs(sampleRate);
		var gamma = this.gamma.keep(rfftSize);  // real signal - just keep + freqs
		var a = this.admittance.keep(rfftSize);
		var magAsquared = a.real.squared.sum;  // squared magnitude of active admittance
		var magAsquaredReciprocal = (magAsquared + FoaEval.reg.squared).reciprocal;
		var magAR = (a.real * a.imag).sum * magAsquaredReciprocal;  // (scaled) magnitude of parallel reactive admittance
		var radius;

		// assume power of two
		/*
		TODO: enforce power of two
		TODO: consider single frequency implementation
		*/

		// +freqs only
		radius = freqs.collect({ |freq, i|
			((gamma[i] < halfPi) || negRadius).if({
				(magAsquared[i] != 0.0).if({
					(magAR[i] * WaveNumber.newFreq(freq, speedOfSound).waveNumber).reciprocal
				}, {
					inf
				})
			}, {  // negative radius --> planewave
				inf
			})
		});

		// mirror
		^(radius ++ (radius.deepCopy.drop(1).drop(-1).reverse))
	}

	//------------------------------------------------------------------------
	// MAGNITUDE - sums

	// inherited:
	//  totalMagMagI
	//  totalMagMagN
	//  totalMagI
	//  totalMagMagA
	//  totalMagA
	//  totalMagMagW
	//  totalMagW
	//  totalMagN

	//------------------------------------------------------------------------
	// INTENSITY - sums

	// inherited:
	//  totalI
	//  totalN
	//  totalA
	//  totalW

	//------------------------------------------------------------------------
	// MAGNITUDE - average

	// inherited:
	//  averageMagMagI
	//  averageMagI
	//  averageMagMagA
	//  averageMagA
	//  averageMagMagW
	//  averageMagW
	//  averageMagMagN
	//  averageMagN

	//------------------------------------------------------------------------
	// INTENSITY - average

	// inherited:
	//  averageI
	//  averageW
	//  averageN

	//------------------------------------------------------------------------
	// SOUNDFIELD INDICATORS

	// inherited:
	//  averageAlpha
	//  averageBeta
	//  averageGamma
	//  averageMu


	//------------------------------------------------------------------------
	// SOUNDFIELD INCIDENCE - complex vector: Complex([ thetaA, phiA ], [ thetaR, phiR ])

	// inherited:
	// averageThetaPhi

	//------------------------------------------------------------------------
	// SOUNDFIELD RADIUS

	// Nearfield (Spherical) Radius
	averageRadius { |weights, negRadius = false, sampleRate = nil, speedOfSound = (AtkHoa.speedOfSound)|
		var radius = this.radius(negRadius, sampleRate, speedOfSound);
		^weights.isNil.if({
			var normFac = 2;
			normFac * radius.reciprocal.sum.reciprocal
		}, {
			radius.reciprocal.wmean(weights).reciprocal
		})
	}

}
