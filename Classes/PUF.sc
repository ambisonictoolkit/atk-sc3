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
// 	Class: PUF
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


PUF[slot] : Array  {

	/*
	use -init to set vars?

	*/
	classvar order = 1;


	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	// Synthesis

	*newClear { |size|
		^super.fill(order.asHoaOrder.size, {
			Complex.new(Signal.newClear(size), Signal.newClear(size))
		})
	}

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
	TODO:

	*newPUA
	*newFOA
	*newAmbix
	*/

	/*
	TODO: check for power of two
	*/
	*newPUT { |put|
		var hoaOrder = order.asHoaOrder;
		var cosTable = Signal.fftCosTable(put.numFrames);
		var imag = Signal.zeroFill(put.numFrames);
		(put.class == PUT).if({
			^super.fill(hoaOrder.size, { |i|
				put[i].fft(imag, cosTable)
			})
		}, {
			Error.new("Input class % != PUT!".format(put.class)).throw
		})
	}

	/*
	TODO: check for power of two
	*/
	*newPUA { |pua|
		(pua.class == PUA).if({
			^this.newPUT(PUT.newPUA(pua))
		}, {
			Error.new("Input class % != PUA!".format(pua.class)).throw
		})
	}

	/*
	TODO: check for power of two
	*/
	*newFoa { |array|
		^this.newPUT(PUT.newFoa(array))
	}

	/*
	TODO: check for power of two
	*/
	*newAmbix { |array|
		^this.newPUT(PUT.newAmbix(array))
	}


	//------------------------------------------------------------------------
	// misc instance methods
	printOn { |stream|
		if (stream.atLimit) { ^this };
		stream << this.class.name << "[ " ;
		this.printItemsOn(stream);
		stream << " ]" ;
	}

	/*
	TODO: rename to -size??
	*/
	numFrames {
		^this.pressure.real.size
	}


	//------------------------------------------------------------------------
	// COMPONENTS - pressure, velocity [ X, Y, Z ]

	// degree
	degree { |degree|
		var res = this[degree.asHoaDegree.indices];
		^(res.size == 1).if({ res = res[0] }, { res });
	}

	// pressure
	pressure {
		var l = 0;  // degree 0
		^this.degree(l)
	}

	// velocity
	velocity {
		var l = 1;  // degree 1
		^this.degree(l)
	}


	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	// Stationary measures
	/*
	TODO: use another name?
	*/

	//------------------------------------------------------------------------
	// ENERGY - magnitudes

	// potential energy
	stationaryWp {
		var p = this.pressure;
		^(p * p.conjugate).real.as(Array)
	}

	// kinetic energy
	stationaryWu {
		var u = this.velocity;
		^u.collect({ |item|
			(item * item.conjugate).real.as(Array)
		}).sum
	}

	// potential & kinetic energy mean
	stationaryWs {
		^[ this.stationaryWp, this.stationaryWu ].mean
	}

	// potential & kinetic energy difference
	stationaryWd {
		^[ this.stationaryWp, this.stationaryWu.neg ].mean
	}

	// // Heyser energy density
	// stationaryWh {
	// 	var ws = this.instantWs;
	// 	var magI = this.instantMagI;
	// 	^(ws - magI.imag)
	// }

}
