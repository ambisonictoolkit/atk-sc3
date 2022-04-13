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
// 	Class: PUT
//
//---------------------------------------------------------------------

/*
PUT: pressure-velocity time domain
*/

/*
TODO:

- include as HOA (and FOA)??
-- e.g., Signal: *hoaCosineTravel
--- if yes, refactor relevant PUT class methods
--- e.g., new classes: FoaT, HoaT?
*/


PUT[slot] : Array  {

	/*
	use -init to set vars?

	*/
	classvar order = 1;


	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	// Synthesis

	*newClear { |size|
		^super.fill(order.asHoaOrder.size, {
			Signal.newClear(size)
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
		^this.newSineTravelling(size, freq, phase + 0.5pi, theta, phi, radius, sampleRate, speedOfSound)
	}

	*newSineTravelling { |size, freq, phase = 0, theta = 0, phi = 0, radius = inf, sampleRate = nil, speedOfSound = (AtkHoa.speedOfSound)|
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
			Signal.newClear(size).sineFill2([ [
				k,
				normMag[i],
				normPha[i]
			] ])
		})
	}

	*newCosinePressure { |size, freq, phase = 0, sampleRate = nil|
		^this.newSinePressure(size, freq, phase + 0.5pi, sampleRate)
	}

	*newSinePressure { |size, freq, phase = 0, sampleRate = nil|
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
			Signal.newClear(size).sineFill2([ [  // pressure
				k,
				normMag,
				normPha
			] ]) ] ++ Array.fill(hoaOrder.size - 1, {
				Signal.newClear(size)  // velocity
			})
		)
	}

	*newCosineDiametric { |size, freq, phase = 0, theta = 0, phi = 0, beta = 0, sampleRate = nil, speedOfSound = (AtkHoa.speedOfSound)|
		^this.newSineDiametric(size, freq, phase + 0.5pi, theta, phi, beta, sampleRate, speedOfSound)
	}

	*newSineDiametric { |size, freq, phase = 0, theta = 0, phi = 0, beta = 0, sampleRate = nil, speedOfSound = (AtkHoa.speedOfSound)|
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
			Signal.newClear(size).sineFill2([ [
				k,
				normMag[i],
				normPha[i]
			] ])
		})
	}

	*newCosineDiffuseModal { |size, freq, phase = 0, sampleRate = nil, speedOfSound = (AtkHoa.speedOfSound)|
		^this.newSineDiffuseModal(size, freq, phase + 0.5pi, sampleRate, speedOfSound)
	}

	*newSineDiffuseModal { |size, freq, phase = 0, sampleRate = nil, speedOfSound = (AtkHoa.speedOfSound)|
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
			Signal.newClear(size).sineFill2([ [
				k,
				normMag[i],
				normPha[i]
			] ])
		})
	}

	*newCosineDiffuseAngular { |size, freq, phase = 0, design = nil, sampleRate = nil, speedOfSound = (AtkHoa.speedOfSound)|
		^this.newSineDiffuseAngular(size, freq, phase + 0.5pi, design, sampleRate, speedOfSound)
	}

	*newSineDiffuseAngular { |size, freq, phase = 0, design = nil, sampleRate = nil, speedOfSound = (AtkHoa.speedOfSound)|
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
			Signal.newClear(size).sineFill2([ [
				k,
				normMag[i],
				normPha[i]
			] ])
		})
	}
	/*
	TODO:

	broadband impulse response
	- implement in fequency domain

	NOTE: inf scaling @ DC will need to be clipped - probably like Signal.periodicPLNoise
	*/


	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	// Encoding

	/*
	TODO:

	rename: *newFoaT?
	*/
	*newFoa { |array|
		var hoaOrder = order.asHoaOrder;
		var classTest, sizeTest;
		var trimmedArray = array[hoaOrder.indices];
		var sourceNorm = \fuma;
		var targetNorm = \sn3d;
		var sourceOrdering = \fuma;
		var targetOrdering = \sid;
		var normFac = hoaOrder.normalisation(targetNorm) / hoaOrder.normalisation(sourceNorm);
		var indicesToACN = hoaOrder.indices(sourceOrdering);
		var indicesFromACN = hoaOrder.indices(targetOrdering).order;
		var normArray;

		classTest = trimmedArray.every({ |item| item.class == Signal });
		sizeTest = trimmedArray.every({ |item| item.size == trimmedArray.first.size });

		classTest.if({
			sizeTest.if({
				// normalize
				normArray = (normFac * trimmedArray[indicesToACN])[indicesFromACN];
				^super.fill(hoaOrder.size, { |i|
					normArray[i]
				})
			}, {
				Error.new("Sizes of arrayed Signals are not matched!").throw;
			})
		}, {
			Error.new("Array elements are not Signals").throw;
		})
	}

	/*
	TODO:

	rename: *newAmbixT?
	*/
	*newAmbix { |array|
		var hoaOrder = order.asHoaOrder;
		var classTest, sizeTest;
		var trimmedArray = array[hoaOrder.indices];
		var targetOrdering = \sid;
		var indicesFromACN = hoaOrder.indices(targetOrdering).order;
		var normArray;

		classTest = trimmedArray.every({ |item| item.class == Signal });
		sizeTest = trimmedArray.every({ |item| item.size == trimmedArray.first.size });

		classTest.if({
			sizeTest.if({
				// normalize
				normArray = trimmedArray[indicesFromACN];  // ambix is already normalized to \sn3d
				^super.fill(hoaOrder.size, { |i|
					normArray[i]
				})
			}, {
				Error.new("Sizes of arrayed Signals are not matched!").throw;
			})
		}, {
			Error.new("Array elements are not Signals").throw;
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
		^this.pressure.size
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
	// Total (sum) measures

	//------------------------------------------------------------------------
	// ENERGY - sums

	// potential energy
	totalWp {
		var p = this.pressure;
		var normFac = 2;
		^(normFac * p.squared.sum)
	}

	// kinetic energy
	totalWu {
		var u = this.velocity;
		var normFac = 2;
		^(normFac * u.squared.sum.sum)
	}

	// potential & kinetic energy mean
	totalWs {
		^[ this.totalWp, this.totalWu ].mean
	}

	// potential & kinetic energy difference
	totalWd {
		^[ this.totalWp, this.totalWu.neg ].mean
	}

	// Heyser energy density
	totalWh {
		var ws = this.totalWs;
		var magI = this.totalMagI;
		^(ws - magI.imag)
	}

	//------------------------------------------------------------------------
	// MAGNITUDE - sums

	// Magnitude of Magnitude of Complex Intensity
	totalMagMagI {
		var p = this.pressure;
		var u = this.velocity;
		var wp = p.squared.sum;
		var wu = u.squared.sum.sum;
		var normFac = 2;
		^(normFac * (wp * wu).sqrt)
	}

	// Magnitude of Complex Intensity
	totalMagI {
		var normFac = 2;
		var p = this.pressure;
		var u = this.velocity;
		var wp = p.squared.sum;
		var wu = u.squared.sum.sum;
		var magI_squared = normFac.squared * wp * wu;
		var ia = u.collect({ |item|
			normFac * (p * item).sum
		});
		var magIa_squared = ia.squared.sum;

		^Complex.new(
			magIa_squared.sqrt,
			(magI_squared - magIa_squared).sqrt
		)
	}

	// Magnitude of Magnitude of Complex Admittance
	/*
	TODO: refactor to avoid use of normFac?
	*/
	totalMagMagA {
		var magMagI = this.totalMagMagI;
		var wp = this.totalWp;
		var wpReciprocal = (wp + FoaEval.reg.squared).reciprocal;
		var normFac = this.numFrames;
		^(normFac * magMagI * wpReciprocal)
	}

	// Magnitude of Complex Admittance
	totalMagA {
		var magI = this.totalMagI;
		var wp = this.totalWp;
		var wpReciprocal = (wp + FoaEval.reg.squared).reciprocal;
		var normFac = this.numFrames;
		^Complex.new(  // explicit... slow otherwise!!
			normFac * magI.real * wpReciprocal,
			normFac * magI.imag * wpReciprocal
		)
	}

	// Magnitude of Magnitude of Complex Energy
	/*
	TODO: refactor to avoid use of normFac?
	*/
	totalMagMagW {
		var magMagI = this.totalMagMagI;
		var ws = this.totalWs;
		var wsReciprocal = (ws + FoaEval.reg.squared).reciprocal;
		var normFac = this.numFrames;
		^(normFac * magMagI * wsReciprocal)
	}

	// Magnitude of Complex Energy
	totalMagW {
		var magI = this.totalMagI;
		var ws = this.totalWs;
		var wsReciprocal = (ws + FoaEval.reg.squared).reciprocal;
		var normFac = this.numFrames;
		^Complex.new(  // explicit... slow otherwise!!
			normFac * magI.real * wsReciprocal,
			normFac * magI.imag * wsReciprocal
		)
	}

	// Magnitude of Magnitude Unit Normalized Complex Intensity - Convenience
	totalMagMagN {
		^this.numFrames.asFloat
	}

	// Magnitude of Unit Normalized Complex Intensity
	/*
	TODO: refactor to avoid use of normFac?
	*/
	totalMagN {
		var magI = this.totalMagI;
		var magMagI = this.totalMagMagI;
		var magMagIReciprocal = (magMagI + FoaEval.reg.squared.squared).reciprocal;
		var normFac = this.numFrames;
		^Complex.new(  // explicit... slow otherwise!!
			normFac * magI.real * magMagIReciprocal,
			normFac * magI.imag * magMagIReciprocal
		)
	}

	//------------------------------------------------------------------------
	// INTENSITY (ACTIVE) - sums

	// Intensity
	totalIa {
		var p = this.pressure;
		var u = this.velocity;
		var normFac = 2;
		^u.collect({ |item|
			normFac * (p * item).sum
		})
	}

	// Admittance
	totalAa {
		var p = this.pressure;
		var u = this.velocity;
		var wp = p.squared;
		var wpReciprocal = (wp + FoaEval.reg.squared).reciprocal;
		^u.collect({ |item|
			((p * item) * wpReciprocal).sum
		})
	}

	// Energy
	totalWa {
		var p = this.pressure;
		var u = this.velocity;
		var wp = p.squared;
		var wu = u.squared.sum;
		var ws = [ wp, wu ].mean;
		var wsReciprocal = (ws + FoaEval.reg.squared).reciprocal;
		^u.collect({ |item|
			((p * item) * wsReciprocal).sum
		})
	}

	// Unit Normalized Intensity
	totalNa {
		var p = this.pressure;
		var u = this.velocity;
		var wp = p.squared;
		var wu = u.squared.sum;
		var magMagI = (wp * wu).sqrt;
		var magMagIReciprocal = (magMagI + FoaEval.reg.squared).reciprocal;
		^u.collect({ |item|
			((p * item) * magMagIReciprocal).sum
		})
	}

}
