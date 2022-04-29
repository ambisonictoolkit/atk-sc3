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

	*newPUA { |pua|
		var hoaOrder = order.asHoaOrder;
		(pua.class == PUA).if({
			^super.fill(hoaOrder.size, { |i|
				pua[i].real
			})
		}, {
			Error.new("Input class % != PUA!".format(pua.class)).throw
		})
	}

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
	// Instantaneous measures


	//------------------------------------------------------------------------
	// ENERGY - magnitudes

	// potential energy
	prInstantWp {
		var p = this.pressure;
		var normFac = 2;
		^(normFac * p.squared.as(Array))
	}

	// kinetic energy
	prInstantWu {
		var u = this.velocity;
		var normFac = 2;
		^(normFac * u.squared.sum.as(Array))
	}

	// potential & kinetic energy mean
	prInstantWs {
		^[ this.prInstantWp, this.prInstantWu ].mean.as(Array)
	}

	// potential & kinetic energy difference
	prInstantWd {
		^[ this.prInstantWp, this.prInstantWu.neg ].mean.as(Array)
	}

	//------------------------------------------------------------------------
	// INTENSITY - magnitudes


	//------------------------------------------------------------------------
	// INTENSITY - complex vectors

	/*
	TODO: parallel to ATK analyze names?
	TODO: defer / promote to superclass??
	*/

	// Intensity
	prInstantIa {
		var p = this.pressure;
		var u = this.velocity;
		var normFac = 2;
		^u.collect({ |item|
			normFac * (p * item).as(Array)
		})
	}


	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	// Total (sum) measures

	//------------------------------------------------------------------------
	// ENERGY - sums

	// potential energy
	totalWp {
		var wp = this.prInstantWp;
		^wp.sum
	}

	// kinetic energy
	totalWu {
		var wu = this.prInstantWu;
		^wu.sum
	}

	// potential & kinetic energy mean
	totalWs {
		var ws = this.prInstantWs;
		^ws.sum
	}

	// potential & kinetic energy difference
	totalWd {
		var wd = this.prInstantWd;
		^wd.sum
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
		var wp = this.totalWp;
		var wu = this.totalWu;
		^(wp * wu).sqrt
	}

	// Magnitude of Complex Intensity
	totalMagI {
		var wp = this.totalWp;
		var wu = this.totalWu;
		var magI_squared = wp * wu;
		var magIa_squared = this.totalIa.squared.sum;
		^Complex.new(
			magIa_squared.sqrt,
			(magI_squared - magIa_squared).sqrt
		)
	}

	// Magnitude of Magnitude of Complex Admittance
	totalMagMagA {
		var magMagA = this.averageMagMagA;
		var normFac = this.numFrames;
		^(normFac * magMagA)
	}

	// Magnitude of Complex Admittance
	totalMagA {
		var magA = this.averageMagA;
		var normFac = this.numFrames;
		^(normFac * magA)
	}

	// Magnitude of Magnitude of Complex Energy
	totalMagMagW {
		var magMagW = this.averageMagMagW;
		var normFac = this.numFrames;
		^(normFac * magMagW)
	}

	// Magnitude of Complex Energy
	totalMagW {
		var magW = this.averageMagW;
		var normFac = this.numFrames;
		^(normFac * magW)
	}

	// Magnitude of Magnitude Unit Normalized Complex Intensity - Convenience
	totalMagMagN {
		^this.numFrames.asFloat
	}

	// Magnitude of Unit Normalized Complex Intensity
	totalMagN {
		var magN = this.averageMagN;
		var normFac = this.numFrames;
		^(normFac * magN)
	}

	//------------------------------------------------------------------------
	// INTENSITY (ACTIVE) - sums

	// Intensity
	totalIa {
		var ia = this.prInstantIa;
		^ia.collect({ |item|
			item.sum
		})
	}

	// Admittance
	totalAa {
		var aa = this.averageAa;
		var normFac = this.numFrames;
		^(normFac * aa)
	}

	// Energy
	totalWa {
		var wa = this.averageWa;
		var normFac = this.numFrames;
		^(normFac * wa)
	}

	// Unit Normalized Intensity
	totalNa {
		var na = this.averageNa;
		var normFac = this.numFrames;
		^(normFac * na)
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
	totalRadius { |negRadius = false, sampleRate = nil, speedOfSound = (AtkHoa.speedOfSound)|
		var radius = this.averageRadius(negRadius, sampleRate, speedOfSound);
		var normFac = this.numFrames;
		^(normFac * radius)
	}


	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	// Average (time) measures

	//------------------------------------------------------------------------
	// ENERGY - average

	// potential energy
	averageWp {
		var wp = this.totalWp;
		var normFac = this.numFrames.reciprocal;
		^(normFac * wp)
	}

	// kinetic energy
	averageWu {
		var wu = this.totalWu;
		var normFac = this.numFrames.reciprocal;
		^(normFac * wu)
	}

	// potential & kinetic energy mean
	averageWs {
		var ws = this.totalWs;
		var normFac = this.numFrames.reciprocal;
		^(normFac * ws)
	}

	// potential & kinetic energy difference
	averageWd {
		var wd = this.totalWd;
		var normFac = this.numFrames.reciprocal;
		^(normFac * wd)
	}

	// Heyser energy density
	averageWh {
		var wh = this.totalWh;
		var normFac = this.numFrames.reciprocal;
		^(normFac * wh)
	}

	//------------------------------------------------------------------------
	// MAGNITUDE - average

	// Magnitude of Magnitude of Complex Intensity
	averageMagMagI {
		var magMagI = this.totalMagMagI;
		var normFac = this.numFrames.reciprocal;
		^(normFac * magMagI)
	}

	// Magnitude of Complex Intensity
	averageMagI {
		var magI = this.totalMagI;
		var normFac = this.numFrames.reciprocal;
		^(normFac * magI)
	}

	// Magnitude of Magnitude of Complex Admittance
	averageMagMagA {
		var magMagI = this.totalMagMagI;
		var wp = this.totalWp;
		var wpReciprocal = (wp + FoaEval.reg.squared).reciprocal;
		^(magMagI * wpReciprocal)
	}

	// Magnitude of Complex Admittance
	averageMagA {
		var magI = this.totalMagI;
		var wp = this.totalWp;
		var wpReciprocal = (wp + FoaEval.reg.squared).reciprocal;
		^Complex.new(  // explicit... slow otherwise!!
			magI.real * wpReciprocal,
			magI.imag * wpReciprocal
		)
	}

	// Magnitude of Magnitude of Complex Energy
	averageMagMagW {
		var magMagI = this.totalMagMagI;
		var ws = this.totalWs;
		var wsReciprocal = (ws + FoaEval.reg.squared).reciprocal;
		^(magMagI * wsReciprocal)
	}

	// Magnitude of Complex Energy
	averageMagW {
		var magI = this.totalMagI;
		var ws = this.totalWs;
		var wsReciprocal = (ws + FoaEval.reg.squared).reciprocal;
		^Complex.new(  // explicit... slow otherwise!!
			magI.real * wsReciprocal,
			magI.imag * wsReciprocal
		)
	}

	// Magnitude of Magnitude Unit Normalized Complex Intensity - Convenience
	averageMagMagN {
		^1.0
	}

	// Magnitude of Unit Normalized Complex Intensity
	averageMagN {
		var magI = this.totalMagI;
		var magMagI = this.totalMagMagI;
		var magMagIReciprocal = (magMagI + FoaEval.reg.squared).reciprocal;
		^Complex.new(  // explicit... slow otherwise!!
			magI.real * magMagIReciprocal,
			magI.imag * magMagIReciprocal
		)
	}


	//------------------------------------------------------------------------
	// INTENSITY (ACTIVE) - average

	// Intensity
	averageIa {
		var ia = this.totalIa;
		var normFac = this.numFrames.reciprocal;
		^(normFac * ia)
	}

	// Admittance
	averageAa {
		var ia = this.totalIa;
		var wp = this.totalWp;
		var wpReciprocal = (wp + FoaEval.reg.squared).reciprocal;
		^(ia * wpReciprocal)
	}

	// Energy
	averageWa {
		var ia = this.totalIa;
		var ws = this.totalWs;
		var wsReciprocal = (ws + FoaEval.reg.squared).reciprocal;
		^(ia * wsReciprocal)
	}

	// Unit Normalized Intensity
	averageNa {
		var ia = this.totalIa;
		var magMagI = this.totalMagMagI;
		var magMagIReciprocal = (magMagI + FoaEval.reg.squared).reciprocal;
		^(ia * magMagIReciprocal)
	}


	//------------------------------------------------------------------------
	// SOUNDFIELD INDICATORS

	// FOA Active-Reactive Soundfield Balance Angle: Alpha
	averageAlpha {
		var magI = this.averageMagI;
		^atan2(magI.imag, magI.real)
	}

	// FOA Potential-Kinetic Soundfield Balance Angle: Beta
	averageBeta {
		var wd = this.averageWd;
		var magMagI = this.averageMagMagI;
		^atan2(wd, magMagI)
	}

	// FOA Active-Reactive Vector Alignment Angle: Gamma
	/*
	Reactive vector not available!
	*/

	// FOA Active Admittance Balance Angle: Mu
	averageMu {
		var magAa = this.averageMagA.real;
		// ^(2 * magAa.atan).tan.reciprocal.atan  // the double angle form
		// ^atan2((1 - magAa.squared) / 2, magAa)
		^atan2(1 - magAa.squared, 2 * magAa)
	}


	//------------------------------------------------------------------------
	// SOUNDFIELD INCIDENCE - real vector: [ thetaA, phiA ]

	// Active Incidence Angle
	averageThetaPhiA {
		var ia = this.totalIa;
		var thetaA = atan2(ia[1], ia[0]);
		var phiA = atan2(ia[2], (ia[0].squared + ia[1].squared).sqrt);
		^[ thetaA, phiA ]
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
	averageRadius { |negRadius = false, sampleRate = nil, speedOfSound = (AtkHoa.speedOfSound)|
		var aA = this.averageAa;
		var thisIntegP = this.deepCopy;
		var integPaA;
		var radius;
		thisIntegP.put(
			0,
			// (one pole, one zero) integrate pressure
			0.5 * (this[0] + (Signal.zeroFill(1) ++ this[0].drop(-1))).integrate.discardDC,
		);
		integPaA = thisIntegP.averageAa;
		radius = ((speedOfSound / sampleRate) * (aA.squared.sum / ((aA * integPaA).sum + FoaEval.reg.squared)))

		^negRadius.if({
			radius
		}, {  // negative radius --> planewave
			radius.isNegative.if({ inf }, { radius })
		})
	}

}
