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

/*
- consider creating Hoa version of: FoaEval.reg
-- could use AtkHoa.thresh
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

	// Heyser energy density
	instantWh {
		var ws = this.instantWs;
		var magI = this.instantMagI;
		^(ws - magI.imag)
	}

	//------------------------------------------------------------------------
	// INTENSITY - magnitudes

	// Magnitude of Magnitude of Complex Intensity
	instantMagMagI {
		var wp = this.instantWp;
		var wu = this.instantWu;
		^(wp * wu).sqrt
	}

	// Magnitude of Complex Intensity
	instantMagI {
		var i = this.instantI;
		^Complex.new(
			i.real.squared.sum.sqrt,
			i.imag.squared.sum.sqrt
		)
	}

	// Magnitude of Magnitude of Complex Admittance
	instantMagMagA {
		var magMagI = this.instantMagMagI;
		var wp = this.instantWp;
		var wpReciprocal = (wp + FoaEval.reg.squared).reciprocal;
		^(magMagI * wpReciprocal)
	}

	// Magnitude of Complex Admittance
	instantMagA {
		var magI = this.instantMagI;
		var wp = this.instantWp;
		var wpReciprocal = (wp + FoaEval.reg.squared).reciprocal;
		^Complex.new(  // explicit... slow otherwise!!
			magI.real * wpReciprocal,
			magI.imag * wpReciprocal
		)
	}

	// Magnitude of Magnitude of Complex Energy
	instantMagMagW {
		var magMagI = this.instantMagMagI;
		var ws = this.instantWs;
		var wsReciprocal = (ws + FoaEval.reg.squared).reciprocal;
		^(magMagI * wsReciprocal)
	}

	// Magnitude of Complex Energy
	instantMagW {
		var magI = this.instantMagI;
		var ws = this.instantWs;
		var wsReciprocal = (ws + FoaEval.reg.squared).reciprocal;
		^Complex.new(  // explicit... slow otherwise!!
			magI.real * wsReciprocal,
			magI.imag * wsReciprocal
		)
	}

	// Magnitude of Magnitude Unit Normalized Complex Intensity - Convenience
	instantMagMagN {
		^Array.fill(this.numFrames, { 1.0 })
	}

	// Magnitude of Unit Normalized Complex Intensity
	instantMagN {
		var magI = this.instantMagI;
		var magMagI = this.instantMagMagI;
		var magMagIReciprocal = (magMagI + FoaEval.reg.squared.squared).reciprocal;
		^Complex.new(  // explicit... slow otherwise!!
			magI.real * magMagIReciprocal,
			magI.imag * magMagIReciprocal
		)
	}

	//------------------------------------------------------------------------
	// INTENSITY - complex vectors

	/*
	TODO: parallel to ATK analyze names?
	TODO: defer / promote to superclass PUC??
	*/

	// Intensity
	instantI {
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

	// Admittance
	instantA {
		var i = this.instantI;
		var wp = this.instantWp;
		var wpReciprocal = (wp + FoaEval.reg.squared).reciprocal;
		^i.collect({ |item|
			Complex.new(  // explicit... slow otherwise!!
				item.real * wpReciprocal,
				item.imag * wpReciprocal
			)
		})
	}

	// Energy
	instantW {
		var i = this.instantI;
		var ws = this.instantWs;
		var wsReciprocal = (ws + FoaEval.reg.squared).reciprocal;
		^i.collect({ |item|
			Complex.new(  // explicit... slow otherwise!!
				item.real * wsReciprocal,
				item.imag * wsReciprocal
			)
		})
	}

	// Unit Normalized Intensity
	instantN {
		var i = this.instantI;
		var magMagI = this.instantMagMagI;
		var magMagIReciprocal = (magMagI + FoaEval.reg.squared).reciprocal;
		^i.collect({ |item|
			Complex.new(  // explicit... slow otherwise!!
				item.real * magMagIReciprocal,
				item.imag * magMagIReciprocal
			)
		})
	}


	//------------------------------------------------------------------------
	// SOUNDFIELD INDICATORS

	// FOA Active-Reactive Soundfield Balance Angle: Alpha
	instantAlpha {
		var magI = this.instantMagI;
		^atan2(magI.imag, magI.real)
	}

	// FOA Potential-Kinetic Soundfield Balance Angle: Beta
	instantBeta {
		var wd = this.instantWd;
		var magMagI = this.instantMagMagI;
		^atan2(wd, magMagI)
	}

	// FOA Active-Reactive Vector Alignment Angle: Gamma
	instantGamma {
		var i = this.instantI;
		var magI = Complex.new(i.real.squared.sum.sqrt, i.imag.squared.sum.sqrt);
		var cosFac, sinFac;
		cosFac = (i.real * i.imag).sum;
		sinFac = ((magI.real * magI.imag).squared - cosFac.squared).abs.sqrt;  // -abs for numerical precision errors
		^atan2(sinFac, cosFac)
	}

	// FOA Active Admittance Balance Angle: Mu
	instantMu {
		var magAa = this.instantMagA.real;
		// ^(2 * magAa.atan).tan.reciprocal.atan  // the double angle form
		// ^atan2((1 - magAa.squared) / 2, magAa)
		^atan2(1 - magAa.squared, 2 * magAa)
	}


	//------------------------------------------------------------------------
	// SOUNDFIELD INCIDENCE - complex vector: Complex([ thetaA, phiA ], [ thetaR, phiR ])

	// Complex Incidence Angle
	instantThetaPhi {
		var i = this.instantI;
		var thetaA = atan2(i.real[1], i.real[0]);
		var phiA = atan2(i.real[2], (i.real[0].squared + i.real[1].squared).sqrt);
		var thetaR = atan2(i.imag[1], i.imag[0]);
		var phiR = atan2(i.imag[2], (i.imag[0].squared + i.imag[1].squared).sqrt);
		^Complex.new(
			[ thetaA, phiA ],
			[ thetaR, phiR ]
		)
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
	instantRadius { |sampleRate|
		var aA = this.instantA.real;
		var thisIntegP = this.deepCopy;
		var integPaA;
		thisIntegP.put(
			0,
			Complex.new(  // (one pole, one zero) integrate pressure
				0.5 * (this[0].real + (Signal.zeroFill(1) ++ this[0].real.drop(-1))).integrate.discardDC,
				0.5 * (this[0].imag + (Signal.zeroFill(1) ++ this[0].imag.drop(-1))).integrate.discardDC
			)
		);
		integPaA = thisIntegP.instantA.real;
		^((AtkFoa.speedOfSound / sampleRate) * (aA.squared.sum / ((aA * integPaA).sum + FoaEval.reg.squared)))
	}


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

	// Heyser energy density
	totalWh {
		^this.instantWh.sum
	}

	//------------------------------------------------------------------------
	// MAGNITUDE - sums

	// Magnitude of Magnitude of Complex Intensity
	totalMagMagI {
		^this.instantMagMagI.sum
	}

	// Magnitude of Complex Intensity
	totalMagI {
		var magI = this.instantMagI;
		^Complex.new(
			magI.real.sum,
			magI.imag.sum
		)
	}

	// Magnitude of Magnitude of Complex Admittance
	totalMagMagA {
		^this.instantMagMagA.sum
	}

	// Magnitude of Complex Admittance
	totalMagA {
		var magA = this.instantMagA;
		^Complex.new(
			magA.real.sum,
			magA.imag.sum
		)
	}

	// Magnitude of Magnitude of Complex Energy
	totalMagMagW {
		^this.instantMagMagW.sum
	}

	// Magnitude of Complex Energy
	totalMagW {
		var magW = this.instantMagW;
		^Complex.new(
			magW.real.sum,
			magW.imag.sum
		)
	}

	// Magnitude of Magnitude Unit Normalized Complex Intensity - Convenience
	totalMagMagN {
		^this.numFrames.asFloat
	}

	// Magnitude of Unit Normalized Complex Intensity
	totalMagN {
		var magN = this.instantMagN;
		^Complex.new(
			magN.real.sum,
			magN.imag.sum
		)
	}

	//------------------------------------------------------------------------
	// INTENSITY - sums

	// Intensity
	totalI {
		var i = this.instantI;
		^Complex.new(
			i.real.flop.sum,
			i.imag.flop.sum
		)
	}

	// Admittance
	totalA {
		var a = this.instantA;
		^Complex.new(
			a.real.flop.sum,
			a.imag.flop.sum
		)
	}

	// Energy
	totalW {
		var w = this.instantW;
		^Complex.new(
			w.real.flop.sum,
			w.imag.flop.sum
		)
	}

	// Unit Normalized Intensity
	totalN {
		var n = this.instantN;
		^Complex.new(
			n.real.flop.sum,
			n.imag.flop.sum
		)
	}


	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	// Average measures

	//------------------------------------------------------------------------
	// ENERGY - average

	// potential energy
	averageWp { |weights = nil|
		^(weights == nil).if({
			var normFac = this.numFrames.reciprocal;
			normFac * this.totalWp
		}, {
			this.instantWp.wmean(weights)
		})
	}

	// kinetic energy
	averageWu { |weights = nil|
		^(weights == nil).if({
			var normFac = this.numFrames.reciprocal;
			normFac * this.totalWu
		}, {
			this.instantWu.wmean(weights)
		})
	}

	// potential & kinetic energy mean
	averageWs { |weights = nil|
		^(weights == nil).if({
			var normFac = this.numFrames.reciprocal;
			normFac * this.totalWs
		}, {
			this.instantWs.wmean(weights)
		})
	}

	// potential & kinetic energy difference
	averageWd { |weights = nil|
		^(weights == nil).if({
			var normFac = this.numFrames.reciprocal;
			normFac * this.totalWd
		}, {
			this.instantWd.wmean(weights)
		})
	}

	// Heyser energy density
	averageWh { |weights = nil|
		^(weights == nil).if({
			var normFac = this.numFrames.reciprocal;
			normFac * this.totalWh
		}, {
			this.instantWh.wmean(weights)
		})
	}


	//------------------------------------------------------------------------
	// MAGNITUDE - average

	// Magnitude of Magnitude of Complex Intensity
	averageMagMagI { |weights = nil|
		^(weights == nil).if({
			var normFac = this.numFrames.reciprocal;
			normFac * this.totalMagMagI
		}, {
			this.instantMagMagI.wmean(weights)
		})
	}

	// Magnitude of Complex Intensity
	averageMagI { |weights = nil|
		^(weights == nil).if({
			var normFac = this.numFrames.reciprocal;
			normFac * this.totalMagI
		}, {
			var magI = this.instantMagI;
			var weightsReciprocal = weights.sum.reciprocal;
			Complex.new(  // explicit...
				weightsReciprocal * (magI.real * weights).sum,
				weightsReciprocal * (magI.imag * weights).sum
			)
		})
	}

	// Magnitude of Magnitude of Complex Admittance
	averageMagMagA { |weights = nil|
		^(weights == nil).if({
			var normFac = this.numFrames.reciprocal;
			normFac * this.totalMagMagA
		}, {
			this.instantMagMagA.wmean(weights)
		})
	}

	// Magnitude of Complex Admittance
	averageMagA { |weights = nil|
		^(weights == nil).if({
			var normFac = this.numFrames.reciprocal;
			normFac * this.totalMagA
		}, {
			var magA = this.instantMagA;
			var weightsReciprocal = weights.sum.reciprocal;
			Complex.new(  // explicit...
				weightsReciprocal * (magA.real * weights).sum,
				weightsReciprocal * (magA.imag * weights).sum
			)
		})
	}

	// Magnitude of Magnitude of Complex Energy
	averageMagMagW { |weights = nil|
		^(weights == nil).if({
			var normFac = this.numFrames.reciprocal;
			normFac * this.totalMagMagW
		}, {
			this.instantMagMagW.wmean(weights)
		})
	}

	// Magnitude of Complex Energy
	averageMagW { |weights = nil|
		^(weights == nil).if({
			var normFac = this.numFrames.reciprocal;
			normFac * this.totalMagW
		}, {
			var magW = this.instantMagW;
			var weightsReciprocal = weights.sum.reciprocal;
			Complex.new(  // explicit...
				weightsReciprocal * (magW.real * weights).sum,
				weightsReciprocal * (magW.imag * weights).sum
			)
		})
	}

	// Magnitude of Magnitude Unit Normalized Complex Intensity - Convenience
	averageMagMagN { |weights = nil|
		^(weights == nil).if({
			1.0
		}, {
			this.instantMagMagN.wmean(weights)
		})
	}

	// Magnitude of Unit Normalized Complex Intensity
	averageMagN { |weights = nil|
		^(weights == nil).if({
			var normFac = this.numFrames.reciprocal;
			normFac * this.totalMagN
		}, {
			var magN = this.instantMagN;
			var weightsReciprocal = weights.sum.reciprocal;
			Complex.new(  // explicit...
				weightsReciprocal * (magN.real * weights).sum,
				weightsReciprocal * (magN.imag * weights).sum
			)
		})
	}

	//------------------------------------------------------------------------
	// INTENSITY - average

	// Intensity
	averageI { |weights = nil|
		^(weights == nil).if({
			var normFac = this.numFrames.reciprocal;
			normFac * this.totalI
		}, {
			var i = this.instantI;
			var weightsReciprocal = weights.sum.reciprocal;
			Complex.new(  // explicit...
				weightsReciprocal * i.real.collect({ |item| (item * weights).sum }),
				weightsReciprocal * i.imag.collect({ |item| (item * weights).sum }),
			)
		})
	}

	// Admittance
	averageA { |weights = nil|
		^(weights == nil).if({
			var normFac = this.numFrames.reciprocal;
			normFac * this.totalA
		}, {
			var a = this.instantA;
			var weightsReciprocal = weights.sum.reciprocal;
			Complex.new(  // explicit...
				weightsReciprocal * a.real.collect({ |item| (item * weights).sum }),
				weightsReciprocal * a.imag.collect({ |item| (item * weights).sum }),
			)
		})
	}

	// Energy
	averageW { |weights = nil|
		^(weights == nil).if({
			var normFac = this.numFrames.reciprocal;
			normFac * this.totalW
		}, {
			var w = this.instantW;
			var weightsReciprocal = weights.sum.reciprocal;
			Complex.new(  // explicit...
				weightsReciprocal * w.real.collect({ |item| (item * weights).sum }),
				weightsReciprocal * w.imag.collect({ |item| (item * weights).sum }),
			)
		})
	}

	// Unit Normalized Intensity
	averageN { |weights = nil|
		^(weights == nil).if({
			var normFac = this.numFrames.reciprocal;
			normFac * this.totalN
		}, {
			var n = this.instantN;
			var weightsReciprocal = weights.sum.reciprocal;
			Complex.new(  // explicit...
				weightsReciprocal * n.real.collect({ |item| (item * weights).sum }),
				weightsReciprocal * n.imag.collect({ |item| (item * weights).sum }),
			)
		})
	}


	//------------------------------------------------------------------------
	// SOUNDFIELD INDICATORS

	/*
	TODO: can these be optimized / simplified?
	*/

	// FOA Active-Reactive Soundfield Balance Angle: Alpha
	averageAlpha { |weights|
		var magI = this.averageMagI(weights);
		^atan2(magI.imag, magI.real)
	}

	// FOA Potential-Kinetic Soundfield Balance Angle: Beta
	averageBeta { |weights|
		var wd = this.averageWd(weights);
		var magMagI = this.averageMagMagI(weights);
		^atan2(wd, magMagI)
	}

	// FOA Active-Reactive Vector Alignment Angle: Gamma
	averageGamma { |weights|
		var gamma = this.instantGamma;
		var sinFac = gamma.sin;
		var cosFac = gamma.cos;
		(weights == nil).if({
			sinFac = sinFac.sum;
			cosFac = cosFac.sum;
		}, {
			sinFac = (sinFac * weights).sum;
			cosFac = (cosFac * weights).sum;
		});
		^atan2(sinFac, cosFac)
	}

	// FOA Active Admittance Balance Angle: Mu
	averageMu { |weights|
		var magAa = this.averageMagA(weights).real;
		// ^(2 * magAa.atan).tan.reciprocal.atan  // the double angle form
		// ^atan2((1 - magAa.squared) / 2, magAa)
		^atan2(1 - magAa.squared, 2 * magAa)
	}

}
