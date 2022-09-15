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
// 	Class: PVa
//
//---------------------------------------------------------------------

/*
PUA: pressure-velocity analytic time domain
*/

/*
TODO:

- refactor PUC as superclass

- include as HOA (and FOA)??
-- e.g., Signal: *hoaCosineTravel
--- if yes, refactor relevant PUT class methods
--- e.g., new classes: FoaT, HoaT?

- reshape from Array of Complex to Complex of Array?
*/

/*
- consider creating Hoa version of: FoaEval.reg
-- could use AtkHoa.thresh
*/


PVa[slot] : PVc  {

	blockNorm { ^1 }

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	// Synthesis

	// Monofrequent
	/*
	travelling
	diametric
	pressure
	*/

	// Monofrequent Travelling
	/*
	NOTE: pressure is normalized
	*/
	*newCosineTravelling { |size, freq, phase = 0, theta = 0, phi = 0, radius = inf, sampleRate = nil, speedOfSound = (AtkHoa.speedOfSound)|
		var hoaOrder = order.asHoaOrder;
		var refRadius = inf;
		var complex = hoaOrder.travelling(freq, phase, theta, phi, radius, refRadius, speedOfSound);
		var mag = complex.magnitude;
		var pha = complex.phase;
		var k = size * freq / sampleRate;
		var sourceNorm = \n3d;
		var targetNorm = \sn3d;
		var targetOrdering = \sid;
		var normFac = hoaOrder.normalisation(targetNorm) / hoaOrder.normalisation(sourceNorm);
		var indicesFromACN = hoaOrder.indices(targetOrdering).order;
		var normMag = (normFac * mag)[indicesFromACN];
		var normPha = pha[indicesFromACN];

		^super.fill(hoaOrder.size, { |i|
			Complex.new(
				Signal.newClear(size).cosineFill2([ [
					k,
					normMag[i],
					normPha[i]
				] ]),
				Signal.newClear(size).sineFill2([ [
					k,
					normMag[i],
					normPha[i]
				] ])
			)
		})
	}

	*newSineTravelling { |size, freq, phase = 0, theta = 0, phi = 0, radius = inf, sampleRate = nil, speedOfSound = (AtkHoa.speedOfSound)|
		^this.newCosineTravelling(size, freq, phase - 0.5pi, theta, phi, radius, sampleRate, speedOfSound)
	}

	*newCosinePressure { |size, freq, phase = 0, sampleRate = nil|
		var lm = [ 0, 0 ];
		var hoaLm = HoaLm.new(lm);
		var hoaOrder = order.asHoaOrder;
		var complex = hoaOrder.pressure(phase);
		var mag = complex.magnitude;
		var pha = complex.phase;
		var k = size * freq / sampleRate;
		var sourceNorm = \n3d;
		var targetNorm = \sn3d;
		var normFac = hoaLm.normalisation(targetNorm) / hoaLm.normalisation(sourceNorm);
		var normMag = normFac * mag.first;  // just keep / normalize pressure
		var normPha = pha.first;  // just keep pressure

		^super.newFrom([
			Complex.new(  // pressure
				Signal.newClear(size).cosineFill2([ [
					k,
					normMag,
					normPha
				] ]),
				Signal.newClear(size).sineFill2([ [
					k,
					normMag,
					normPha
				] ])
			)
			] ++ Array.fill(hoaOrder.size - 1, {  // velocity
				Complex.new(Signal.newClear(size), Signal.newClear(size))
			})
		)
	}

	*newSinePressure { |size, freq, phase = 0, sampleRate = nil|
		^this.newCosinePressure(size, freq, phase - 0.5pi, sampleRate)
	}

	*newCosineDiametric { |size, freq, phase = 0, theta = 0, phi = 0, beta = 0, sampleRate = nil, speedOfSound = (AtkHoa.speedOfSound)|
		var hoaOrder = order.asHoaOrder;
		var refRadius = inf;
		var complex = hoaOrder.diametric(freq, phase, theta, phi, beta, refRadius, speedOfSound);
		var mag = complex.magnitude;
		var pha = complex.phase;
		var k = size * freq / sampleRate;
		var sourceNorm = \n3d;
		var targetNorm = \sn3d;
		var targetOrdering = \sid;
		var normFac = hoaOrder.normalisation(targetNorm) / hoaOrder.normalisation(sourceNorm);
		var indicesFromACN = hoaOrder.indices(targetOrdering).order;
		var normMag = (normFac * mag)[indicesFromACN];
		var normPha = pha[indicesFromACN];

		^super.fill(hoaOrder.size, { |i|
			Complex.new(
				Signal.newClear(size).cosineFill2([ [
					k,
					normMag[i],
					normPha[i]
				] ]),
				Signal.newClear(size).sineFill2([ [
					k,
					normMag[i],
					normPha[i]
				] ])
			)
		})
	}

	*newSineDiametric { |size, freq, phase = 0, theta = 0, phi = 0, beta = 0, sampleRate = nil, speedOfSound = (AtkHoa.speedOfSound)|
		^this.newCosineDiametric(size, freq, phase - 0.5pi, theta, phi, beta, sampleRate, speedOfSound)
	}

	*newCosineDiffuseModal { |size, freq, phase = 0, sampleRate = nil, speedOfSound = (AtkHoa.speedOfSound)|
		var hoaOrder = order.asHoaOrder;
		var refRadius = inf;
		var complex = hoaOrder.diffuseModal(freq, phase, refRadius, speedOfSound);
		var mag = complex.magnitude;
		var pha = complex.phase;
		var k = size * freq / sampleRate;
		var sourceNorm = \n3d;
		var targetNorm = \sn3d;
		var targetOrdering = \sid;
		var normFac = hoaOrder.normalisation(targetNorm) / hoaOrder.normalisation(sourceNorm);
		var indicesFromACN = hoaOrder.indices(targetOrdering).order;
		var normMag = (normFac * mag)[indicesFromACN];
		var normPha = pha[indicesFromACN];

		^super.fill(hoaOrder.size, { |i|
			Complex.new(
				Signal.newClear(size).cosineFill2([ [
					k,
					normMag[i],
					normPha[i]
				] ]),
				Signal.newClear(size).sineFill2([ [
					k,
					normMag[i],
					normPha[i]
				] ])
			)
		})
	}

	*newSineDiffuseModal { |size, freq, phase = 0, sampleRate = nil, speedOfSound = (AtkHoa.speedOfSound)|
		^this.newCosineDiffuseModal(size, freq, phase - 0.5pi, sampleRate, speedOfSound)
	}

	*newCosineDiffuseAngular { |size, freq, phase = 0, design = nil, sampleRate = nil, speedOfSound = (AtkHoa.speedOfSound)|
		var hoaOrder = order.asHoaOrder;
		var refRadius = inf;
		var complex = hoaOrder.diffuseAngular(freq, phase, design, refRadius, speedOfSound);
		var mag = complex.magnitude;
		var pha = complex.phase;
		var k = size * freq / sampleRate;
		var sourceNorm = \n3d;
		var targetNorm = \sn3d;
		var targetOrdering = \sid;
		var normFac = hoaOrder.normalisation(targetNorm) / hoaOrder.normalisation(sourceNorm);
		var indicesFromACN = hoaOrder.indices(targetOrdering).order;
		var normMag = (normFac * mag)[indicesFromACN];
		var normPha = pha[indicesFromACN];

		^super.fill(hoaOrder.size, { |i|
			Complex.new(
				Signal.newClear(size).cosineFill2([ [
					k,
					normMag[i],
					normPha[i]
				] ]),
				Signal.newClear(size).sineFill2([ [
					k,
					normMag[i],
					normPha[i]
				] ])
			)
		})
	}

	*newSineDiffuseAngular { |size, freq, phase = 0, design = nil, sampleRate = nil, speedOfSound = (AtkHoa.speedOfSound)|
		^this.newCosineDiffuseAngular(size, freq, phase - 0.5pi, design, sampleRate, speedOfSound)
	}
// 	/*
// 	TODO:
//
// 	broadband impulse response
// 	- implement in fequency domain
//
// 	NOTE: inf scaling @ DC will need to be clipped - probably like Signal.periodicPLNoise
// 	*/


	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	// Encoding

	/*
	TODO: PUT -> PVt
	*/
	*newPUT { |put|
		(put.class == PUT).if({
			^super.fill(4, { |i|
				put[i].analytic
			})
		}, {
			Error.new("Input class % != PUT!".format(put.class)).throw
		})
	}

	/*
	TODO: PUT -> PVt
	rename: *newFoaT?
	*/
	*newFoa { |array|
		var put = PUT.newFoa(array);
		^super.fill(4, { |i|
			put[i].analytic
		})
	}

	/*
	TODO: PUT -> PVt
	rename: *newAmbixT?
	*/
	*newAmbix { |array|
		var put = PUT.newAmbix(array);
		^super.fill(4, { |i|
			put[i].analytic
		})
	}

	/*
	TODO:

	*newHoa

	include radial filter
	- circular convolution?
	- frequency domain mul?  <--
	*/

	//------------------------------------------------------------------------
	// INTENSITY - complex vectors

	/*
	TODO: parallel to ATK analyze names?
	TODO: defer / promote to superclass PUC??
	*/

	intensity {
		var p = this.pressure;
		var u = this.velocity;
		^u.collect({ |item|
			var i = (p * item.conjugate);
			Complex.new(
				i.real.as(Array),
				i.imag.as(Array)
			)
		})
	}


	//------------------------------------------------------------------------
	// SOUNDFIELD RADIUS

	// Nearfield (Spherical) Radius
	/*
	TODO: frequency domain implementation?

	NOTE:
	 - biased towards LF performance
	 - biased towards steady state monotones
	*/
	radius { |negRadius = false, sampleRate = nil, speedOfSound = (AtkHoa.speedOfSound)|
		var aA = this.admittance.real;
		var thisIntegP = this.deepCopy;
		var integPaA;
		var radius;

		sampleRate ?? { "[PVa:radius] No sampleRate specified.".error; this.halt };

		thisIntegP.put(
			0,
			Complex.new(  // (one pole, one zero) integrate pressure
				0.5 * (this[0].real + (Signal.zeroFill(1) ++ this[0].real.drop(-1))).integrate.discardDC,
				0.5 * (this[0].imag + (Signal.zeroFill(1) ++ this[0].imag.drop(-1))).integrate.discardDC
			)
		);
		integPaA = thisIntegP.admittance.real;
		radius = ((speedOfSound / sampleRate) * (aA.squared.sum / ((aA * integPaA).sum + FoaEval.reg.squared)));

		^negRadius.if({
			radius
		}, {  // negative radius --> planewave
			radius.collect({ |item|
				item.isNegative.if({ inf }, { item })
			})
		})
	}


	//------------------------------------------------------------------------
	// MAGNITUDE - sums

	// inherited:
	// totalMagMagI
	// totalMagMagN
	// totalMagI
	// totalMagMagA
	// totalMagA
	// totalMagMagW
	// totalMagW
	// totalMagN

	//------------------------------------------------------------------------
	// INTENSITY - sums

	// inherited:
	// totalI
	// totalN
	// totalA
	// totalW

	//------------------------------------------------------------------------
	// MAGNITUDE - average

	// inherited:
	// averageMagMagI
	// averageMagI
	// averageMagMagA
	// averageMagA
	// averageMagMagW
	// averageMagW
	// averageMagMagN
	// averageMagN

	//------------------------------------------------------------------------
	// INTENSITY - average

	// inherited:
	// averageI
	// averageA
	// averageW
	// averageN


	//------------------------------------------------------------------------
	// SOUNDFIELD INDICATORS

	// inherited:
	// averageAlpha
	// averageBeta
	// averageGamma
	// averageMu


	//------------------------------------------------------------------------
	// SOUNDFIELD INCIDENCE - complex vector: Complex([ thetaA, phiA ], [ thetaR, phiR ])

	// inherited:
	// averageThetaPhi


	//------------------------------------------------------------------------
	// SOUNDFIELD RADIUS

	// Nearfield (Spherical) Radius
	/*
	TODO: frequency domain implementation?
	TODO: manage infs?

	NOTE:
	 - biased towards LF performance
	 - biased towards steady state monotones
	*/
	averageRadius { |weights, negRadius = false, sampleRate = nil, speedOfSound = (AtkHoa.speedOfSound)|
		var radius = this.instantRadius(negRadius, sampleRate, speedOfSound);
		^weights.isNil.if({
			radius.mean
		}, {
			radius.wmean(weights)
		})
	}

}
