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
- consider creating Hoa version of: FoaEval.reg
-- could use AtkHoa.thresh
- Stationary measures: refactor to only return coefficients for +freqs
OR recognize I.imag is neg for -freqs
*/


PVf[slot] : PVc  {

	blockNorm { ^2 / this.numFrames }


	/*
	~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	Synthesis (monofrequent):
	Traveling, Standing, Diffuse
	~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	*/

	/*  Travelling  */

	/*
	NOTE:
	- pressure is scaled --> magnitude
	- only supports powerOfTwo
	- supply (size / 2) + 1 args: real
	- catch arg size mismatch errors

	TODO:
	- integrate more closely w/ FreqSpectrum --> magnitude, phase args
	- default args if nil
	*/
	*newTravelling { |size, magnitude, phase, theta, phi, radius, sampleRate = nil, speedOfSound = (AtkHoa.speedOfSound)|
		var hoaOrder = order.asHoaOrder;
		var refRadius = inf;
		var freqs, complex;
		var pvf;
		var rfftsize;
		var sourceNorm = \n3d;
		var targetNorm = \sn3d;
		var targetOrdering = \sid;
		var normFac = hoaOrder.normalisation(targetNorm) / hoaOrder.normalisation(sourceNorm);
		var indicesFromACN = hoaOrder.indices(targetOrdering).order;

		if(size.isPowerOfTwo, {  // rfft
			rfftsize = (size / 2 + 1).asInteger;

			pvf = Array.fill(hoaOrder.size, {  // empty
				Complex.new(Signal.newClear(rfftsize), Signal.newClear(rfftsize))
			});

			freqs = rfftsize.rfftFreqs(sampleRate);
			freqs = freqs.collect({ |freq|  // blt frequency warp
				sampleRate / pi * tan(pi * freq / sampleRate)
			});

			rfftsize.do({ |i|  // complex coefficients
				complex = magnitude[i] * hoaOrder.travelling(freqs[i], phase[i], theta[i], phi[i], radius[i], refRadius, speedOfSound);
				hoaOrder.size.do({ |j|
					pvf[j].real.put(i, complex.real[j]);
					pvf[j].imag.put(i, complex.imag[j])
				})
			});

			// convert from rfft to fft
			pvf = pvf.collect({ |item| item.real.rfftToFft(item.imag) })
		}, {
			Error.new("Size = % is not supported. Use a power of two.".format(size)).throw
		});

		// normalize & reorder
		pvf = (normFac * pvf)[indicesFromACN];

		^super.fill(hoaOrder.size, { |i|
			pvf[i]
		})
	}


	/*  Standing: pressure, diametric  */

	/*
	NOTE:
	- only supports powerOfTwo
	- supply (size / 2) + 1 args: real
	- catch arg size mismatch errors

	TODO:
	- integrate more closely w/ FreqSpectrum --> magnitude, phase args
	- default args if nil
	*/
	*newPressure { |size, magnitude, phase|
		var lm = [ 0, 0 ];
		var hoaLm = HoaLm.new(lm);
		var hoaOrder = order.asHoaOrder;
		var angularWeight = hoaLm.sph;  // express explicitly
		var pvf;
		var rfftsize;
		var sourceNorm = \n3d;
		var targetNorm = \sn3d;
		var normFac = hoaLm.normalisation(targetNorm) / hoaLm.normalisation(sourceNorm);

		if(size.isPowerOfTwo, {  // rfft
			rfftsize = (size / 2 + 1).asInteger;

			pvf = Array.fill(hoaOrder.size - 1, {  // empty
				Complex.new(Signal.newClear(rfftsize), Signal.newClear(rfftsize))
			});

			// set pressure (concatenate)
			pvf = [
				Complex.new(
					(magnitude * phase.cos).as(Signal),
					(magnitude * phase.sin).as(Signal)
				)
			] ++ pvf;

			// convert from rfft to fft
			pvf = pvf.collect({ |item| item.real.rfftToFft(item.imag) })
		}, {
			Error.new("Size = % is not supported. Use a power of two.".format(size)).throw
		});

		// normalize - just pressure
		pvf.put(0, normFac * pvf[0]);

		^super.fill(hoaOrder.size, { |i|
			pvf[i]
		})
	}

	/*
	NOTE:
	- only supports powerOfTwo
	- supply (size / 2) + 1 args: real
	- catch arg size mismatch errors

	TODO:
	- integrate more closely w/ FreqSpectrum --> magnitude, phase args
	- default args if nil
	*/
	*newDiametric { |size, magnitude, phase, theta, phi, beta, sampleRate = nil, speedOfSound = (AtkHoa.speedOfSound)|
		var hoaOrder = order.asHoaOrder;
		var refRadius = inf;
		var freqs, complex;
		var pvf;
		var rfftsize;
		var sourceNorm = \n3d;
		var targetNorm = \sn3d;
		var targetOrdering = \sid;
		var normFac = hoaOrder.normalisation(targetNorm) / hoaOrder.normalisation(sourceNorm);
		var indicesFromACN = hoaOrder.indices(targetOrdering).order;

		if(size.isPowerOfTwo, {  // rfft
			rfftsize = (size / 2 + 1).asInteger;

			pvf = Array.fill(hoaOrder.size, {  // empty
				Complex.new(Signal.newClear(rfftsize), Signal.newClear(rfftsize))
			});

			freqs = rfftsize.rfftFreqs(sampleRate);
			freqs = freqs.collect({ |freq|  // blt frequency warp
				sampleRate / pi * tan(pi * freq / sampleRate)
			});

			rfftsize.do({ |i|  // complex coefficients
				complex = magnitude[i] * hoaOrder.diametric(freqs[i], phase[i], theta[i], phi[i], beta[i], refRadius, speedOfSound);
				hoaOrder.size.do({ |j|
					pvf[j].real.put(i, complex.real[j]);
					pvf[j].imag.put(i, complex.imag[j])
				})
			});

			// convert from rfft to fft
			pvf = pvf.collect({ |item| item.real.rfftToFft(item.imag) })
		}, {
			Error.new("Size = % is not supported. Use a power of two.".format(size)).throw
		});

		// normalize & reorder
		pvf = (normFac * pvf)[indicesFromACN];

		^super.fill(hoaOrder.size, { |i|
			pvf[i]
		})
	}

	/*  Diffuse: modal, angular (plane-wave)  */

	/*
	NOTE:
	- only supports powerOfTwo
	- supply (size / 2) + 1 args: real
	- catch arg size mismatch errors

	TODO:
	- integrate more closely w/ FreqSpectrum --> magnitude, phase args
	- default args if nil

	NOTE:
	- normalized Wp, Wu, Ws
	*/
	*newDiffuseModal { |size, magnitude, phase, sampleRate = nil, speedOfSound = (AtkHoa.speedOfSound)|
		var hoaOrder = order.asHoaOrder;
		var refRadius = inf;
		var freqs, complex;
		var pvf;
		var rfftsize;
		var sourceNorm = \n3d;
		var targetNorm = \sn3d;
		var targetOrdering = \sid;
		var normFac = hoaOrder.normalisation(targetNorm) / hoaOrder.normalisation(sourceNorm);
		var indicesFromACN = hoaOrder.indices(targetOrdering).order;

		if(size.isPowerOfTwo, {  // rfft
			rfftsize = (size / 2 + 1).asInteger;

			pvf = Array.fill(hoaOrder.size, {  // empty
				Complex.new(Signal.newClear(rfftsize), Signal.newClear(rfftsize))
			});

			freqs = rfftsize.rfftFreqs(sampleRate);
			freqs = freqs.collect({ |freq|  // blt frequency warp
				sampleRate / pi * tan(pi * freq / sampleRate)
			});

			rfftsize.do({ |i|  // complex coefficients
				complex = magnitude[i] * hoaOrder.diffuseModal(freqs[i], phase[i], refRadius, speedOfSound);
				hoaOrder.size.do({ |j|
					pvf[j].real.put(i, complex.real[j]);
					pvf[j].imag.put(i, complex.imag[j])
				})
			});

			// convert from rfft to fft
			pvf = pvf.collect({ |item| item.real.rfftToFft(item.imag) })
		}, {
			Error.new("Size = % is not supported. Use a power of two.".format(size)).throw
		});

		// normalize & reorder
		pvf = (normFac * pvf)[indicesFromACN];

		^super.fill(hoaOrder.size, { |i|
			pvf[i]
		})
	}

	/*
	NOTE:
	- only supports powerOfTwo
	- supply (size / 2) + 1 args: real
	- catch arg size mismatch errors

	TODO:
	- integrate more closely w/ FreqSpectrum --> magnitude, phase args
	- default args if nil
	*/
	/*
	NOTE: normalized Ws
	*/
	*newDiffuseAngular { |size, magnitude, phase, design, sampleRate = nil, speedOfSound = (AtkHoa.speedOfSound)|
		var hoaOrder = order.asHoaOrder;
		var refRadius = inf;
		var freqs, complex;
		var pvf;
		var rfftsize;
		var sourceNorm = \n3d;
		var targetNorm = \sn3d;
		var targetOrdering = \sid;
		var normFac = hoaOrder.normalisation(targetNorm) / hoaOrder.normalisation(sourceNorm);
		var indicesFromACN = hoaOrder.indices(targetOrdering).order;

		if(size.isPowerOfTwo, {  // rfft
			rfftsize = (size / 2 + 1).asInteger;

			pvf = Array.fill(hoaOrder.size, {  // empty
				Complex.new(Signal.newClear(rfftsize), Signal.newClear(rfftsize))
			});

			freqs = rfftsize.rfftFreqs(sampleRate);
			freqs = freqs.collect({ |freq|  // blt frequency warp
				sampleRate / pi * tan(pi * freq / sampleRate)
			});

			rfftsize.do({ |i|  // complex coefficients
				complex = magnitude[i] * hoaOrder.diffuseModal(freqs[i], phase[i], design, refRadius, speedOfSound);
				hoaOrder.size.do({ |j|
					pvf[j].real.put(i, complex.real[j]);
					pvf[j].imag.put(i, complex.imag[j])
				})
			});

			// convert from rfft to fft
			pvf = pvf.collect({ |item| item.real.rfftToFft(item.imag) })
		}, {
			Error.new("Size = % is not supported. Use a power of two.".format(size)).throw
		});

		// normalize & reorder
		pvf = (normFac * pvf)[indicesFromACN];

		^super.fill(hoaOrder.size, { |i|
			pvf[i]
		})
	}


	/*
	~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	Encoding: from other PVx or ambisonic formats:
	PVt, PVa, Foa, Ambix
	~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	*/

	*newFromPUT { |put|  				/* TEMP for testing diffuse fields with former reference implementation */
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
	*/
	*newFromPVt { |pvt|
		var cosTable = Signal.fftCosTable(pvt.numFrames);
		var imag = Signal.zeroFill(pvt.numFrames);

		(pvt.class == PVt).if({
			^super.fill(4, { |i|
				pvt[i].fft(imag, cosTable)
			})
		}, {
			Error.new("Input class % != PVt!".format(pvt.class)).throw
		})
	}

	/*
	TODO: check for power of two
	*/
	*newFromPVa { |pua|
		(pua.class == PVa).if({
			^this.newFromPVt(PVt.newPUA(pua))
		}, {
			Error.new("Input class % != PUA!".format(pua.class)).throw
		})
	}

	/*
	TODO: check for power of two
	*/
	*newFromFoa { |array|
		^this.newFromPVt(PVt.newFoa(array))
	}

	/*
	TODO:
	- check for power of two
	- update PVt -> PVt
	*/
	*newFromAmbix { |array|
		^this.newFromPVt(PVt.newAmbix(array))
	}


	/*
	~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	Analysis:
	Intensity, Radius
	~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	*/

	/*  Intensity  */

	/*
	TODO:
	- parallel to ATK analyze names?
	- defer / promote to superclass PUC??
	- are negative freqs correct??
	- do these align w/ HoaOrder coefficients??
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


	/*  Radius  */

	/*
	TODO:
	- enforce power of two
	- consider single frequency implementation
	*/
	// Nearfield (Spherical) Radius
	radius { |negRadius = false, sampleRate = nil, speedOfSound = (AtkHoa.speedOfSound)|
		var rfftSize = (this.numFrames / 2).asInteger + 1;
		var halfPi = 0.5pi;
		var gamma = this.gamma.keep(rfftSize);  // real signal - just keep + freqs
		var a = this.admittance.keep(rfftSize);
		var magAsquared = a.real.squared.sum;  // squared magnitude of active admittance
		var magAsquaredReciprocal = (magAsquared + Atk.regSq).reciprocal;
		var magAR = (a.real * a.imag).sum * magAsquaredReciprocal;  // (scaled) magnitude of parallel reactive admittance
		var freqs, radius;

		sampleRate ?? { "[PVc:radius] No sampleRate specified.".error; this.halt };

		freqs = rfftSize.rfftFreqs(sampleRate);

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
