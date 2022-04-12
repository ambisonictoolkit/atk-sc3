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
// 	Class: PUA
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


PUA[slot] : Array  {

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
	TODO:

	rename: *newFoaT?
	*/
	*newFoa { |array|
		var hoaOrder = order.asHoaOrder;
		var put = PUT.newFoa(array);
		^super.fill(hoaOrder.size, { |i|
			put[i].analytic
		})
	}

	/*
	TODO:

	rename: *newAmbixT?
	*/
	*newAmbix { |array|
		var hoaOrder = order.asHoaOrder;
		var put = PUT.newAmbix(array);
		^super.fill(hoaOrder.size, { |i|
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
	// misc instance methods
	printOn { |stream|
		if (stream.atLimit) { ^this };
		stream << this.class.name << "[ " ;
		this.printItemsOn(stream);
		stream << " ]" ;
	}

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
	// Instantaneous measures


	//------------------------------------------------------------------------
	// ENERGY - magnitudes

	// potential energy
	instantWp {
		var p = this.pressure;
		^(p * p.conjugate).real.as(Array)
	}

	// kinetic energy
	instantWu {
		var u = this.velocity;
		^u.collect({ |item|
			(item * item.conjugate).real.as(Array)
		}).sum
	}

	// potential & kinetic energy mean
	instantWs {
		^[ this.instantWp, this.instantWu ].mean
	}

	// potential & kinetic energy difference
	instantWd {
		^[ this.instantWp, this.instantWu.neg ].mean
	}

	// // FOA Heyser energy density
	// instantWh {
	// 	var ws = this.instantWs;
	// 	var magI = this.instantMagI;
	// 	^(ws - magI.imag)
	// }


	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	// Total (sum) measures

	//------------------------------------------------------------------------
	// ENERGY - sums

	// potential energy
	totalWp {
		^this.instantWp.sum
	}

	// kinetic energy
	totalWu {
		^this.instantWu.sum
	}

	// potential & kinetic energy mean
	totalWs {
		^this.instantWs.sum
	}

	// potential & kinetic energy difference
	totalWd {
		^this.instantWd.sum
	}

	// // Heyser energy density
	// totalWh {
	// 	^this.instantWh.sum
	// }

}
