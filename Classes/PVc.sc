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
// 	Class: PVc
//
//---------------------------------------------------------------------

/*
PVC: pressure-velocity complex (frequency, analytic-time) domain

A class to support Pressure-Velocity Signal operations
in the Complex domain.

Users use subclasses PVF, PVA.
*/

/*
PVC dimensions:
n = numFrames;
[
	Complex(Signal[n], Signal[n]), // p
	Complex(Signal[n], Signal[n]), // v_x
	Complex(Signal[n], Signal[n]), // v_y
	Complex(Signal[n], Signal[n])  // v_z
]
*/

PVc[slot] : Array {

	classvar order = 1; // TODO: remove (currently synthesis class methods rely on it)

	*newClear { |size|
		^super.fill(4, {
			Complex.new(Signal.newClear(size), Signal.newClear(size))
		})
	}

	//------------------------------------------------------------------------
	// misc instance methods

	numFrames {
		^this.pressure.real.size
	}

	size {
		^this.numFrames
	}

	blockNorm { ^this.subclassResponsibility }

	// posting: standard return
	printOn { |stream|
		if (stream.atLimit) { ^this };
		stream << this.class.name << "[ " ;
		this.printItemsOn(stream);
		stream << " ]" ;
	}

	// posting: nicer format for on-demand posting
	post {
		"%[\n".postf(this.class.name);
		this.do{ |chan, i|
			"\t%( ".postf(chan.class.name);
			chan.real.post; ",".postln;
			"\t\t\t ".post;
			chan.imag.post;
			if (i < 3, { " ),\n" }, { " ) ]" }).post;
		};
	}
	postln { this.post; "".postln; }

	//------------------------------------------------------------------------
	// COMPONENTS - pressure, velocity [ X, Y, Z ]

	/*
	TODO: strip the Signal format in the case numFrames = 1?
	*/
	pressure { ^this[0] }

	velocity { ^(1..3).collect( this.at(_) ) }

	/*
	TODO: remove if it's agree PV will only ever be degree = 0 or 1
	*/
	degree { |degree|
		var res = this[degree.asHoaDegree.indices];
		^(res.size == 1).if({ res = res[0] }, { res });
	}

	//------------------------------------------------------------------------
	// Quantities
	// Note: Interpretation of these methods depend on the subclass:
	//       PVa: instantaneous
	//       PVf: stationary

	//--------------------
	// ENERGY - magnitudes

	// potential energy
	wp {
		var p = this.pressure;
		^(p * p.conjugate).real.as(Array)
	}

	// kinetic energy
	wu {
		var u = this.velocity;

		^u.collect({ |item|
			(item * item.conjugate).real.as(Array)
		}).sum
	}

	// potential & kinetic energy mean
	ws {
		^[ this.wp, this.wu ].mean
	}

	// potential & kinetic energy difference
	wd {
		^[ this.wp, this.wu.neg ].mean
	}

	// Heyser energy density
	wh {
		var ws = this.ws;
		var magI = this.magI;

		^(ws - magI.imag)
	}


	//-----------------------
	// INTENSITY - magnitudes

	// Magnitude of Magnitude of Complex Intensity
	magMagI {
		var wp = this.wp;
		var wu = this.wu;

		^(wp * wu).sqrt
	}

	// Magnitude of Complex Intensity
	magI {
		var i = this.intensity;

		^Complex.new(
			i.real.squared.sum.sqrt,
			i.imag.squared.sum.sqrt
		)
	}

	// Magnitude of Magnitude of Complex Admittance
	magMagA {
		var magMagI = this.magMagI;
		var wp = this.wp;
		var wpReciprocal = (wp + FoaEval.reg.squared).reciprocal;

		^(magMagI * wpReciprocal)
	}

	// Magnitude of Complex Admittance
	magA {
		var magI = this.magI;
		var wp = this.wp;
		var wpReciprocal = (wp + FoaEval.reg.squared).reciprocal;

		^Complex.new(  // explicit... slow otherwise!!
			magI.real * wpReciprocal,
			magI.imag * wpReciprocal
		)
	}

	// Magnitude of Magnitude of Complex Energy
	magMagW {
		var magMagI = this.magMagI;
		var ws = this.ws;
		var wsReciprocal = (ws + FoaEval.reg.squared).reciprocal;

		^(magMagI * wsReciprocal)
	}

	// Magnitude of Complex Energy
	magW {
		var magI = this.magI;
		var ws = this.ws;
		var wsReciprocal = (ws + FoaEval.reg.squared).reciprocal;

		^Complex.new(  // explicit... slow otherwise!!
			magI.real * wsReciprocal,
			magI.imag * wsReciprocal
		)
	}

	// Magnitude of Magnitude Unit Normalized Complex Intensity - Convenience
	magMagN {
		^Array.fill(this.numFrames, { 1.0 })
	}

	// Magnitude of Unit Normalized Complex Intensity
	magN {
		var magI = this.magI;
		var magMagI = this.magMagI;
		var magMagIReciprocal = (magMagI + FoaEval.reg.squared.squared).reciprocal;

		^Complex.new(  // explicit... slow otherwise!!
			magI.real * magMagIReciprocal,
			magI.imag * magMagIReciprocal
		)
	}

	//------------------------------------------------------------------------
	// INTENSITY - complex vectors

	intensity {
		^this.subclassResponsibility
	}

	admittance {
		var i = this.intensity;
		var wp = this.wp;
		var wpReciprocal = (wp + FoaEval.reg.squared).reciprocal;

		^i.collect({ |item|
			Complex.new(  // explicit... slow otherwise!!
				item.real * wpReciprocal,
				item.imag * wpReciprocal
			)
		})
	}

	energy {
		var i = this.intensity;
		var ws = this.ws;
		var wsReciprocal = (ws + FoaEval.reg.squared).reciprocal;

		^i.collect({ |item|
			Complex.new(  // explicit... slow otherwise!!
				item.real * wsReciprocal,
				item.imag * wsReciprocal
			)
		})
	}

	// Unit Normalized Intensity
	intensityN {
		var i = this.intensity;
		var magMagI = this.magMagI;
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

	/*
	TODO: PVf: check negative freqs --> gamma??
	*/

	// Alpha: Active-Reactive Soundfield Balance Angle
	alpha {
		var magI = this.magI;
		^atan2(magI.imag, magI.real)
	}

	// Beta: Potential-Kinetic Soundfield Balance Angle
	beta {
		var wd = this.wd;
		var magMagI = this.magMagI;

		^atan2(wd, magMagI)
	}

	// Gamma: Active-Reactive Vector Alignment Angle
	gamma {
		var i = this.intensity;
		var magI = Complex.new(i.real.squared.sum.sqrt, i.imag.squared.sum.sqrt);
		var cosFac, sinFac;

		cosFac = (i.real * i.imag).sum;
		sinFac = ((magI.real * magI.imag).squared - cosFac.squared).abs.sqrt;  // -abs for numerical precision errors
		^atan2(sinFac, cosFac)
	}

	// Mu: Active Admittance Balance Angle
	mu {
		var magAa = this.magA.real;

		// ^(2 * magAa.atan).tan.reciprocal.atan  // the double angle form
		// ^atan2((1 - magAa.squared) / 2, magAa)
		^atan2(1 - magAa.squared, 2 * magAa)
	}


	//------------------------------------------------------------------------
	// SOUNDFIELD INCIDENCE - complex vector: Complex([ thetaA, phiA ], [ thetaR, phiR ])

	// Complex Incidence Angle
	thetaPhi {
		var i = this.intensity;
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
	//------------------------------------------------------------------------
	// Total (sum) measures

	//------------------------------------------------------------------------
	// ENERGY - sums across p-v components

	/*
	TODO: consider rename method family: wpTotal, etc.
	*/

	// potential energy
	totalWp { ^this.wp.sum * this.blockNorm }

	// kinetic energy
	totalWu { ^this.wu.sum * this.blockNorm }

	// potential & kinetic energy mean
	totalWs { ^this.ws.sum * this.blockNorm }

	// potential & kinetic energy difference
	totalWd { ^this.wd.sum * this.blockNorm }

	// Heyser energy density
	totalWh { ^this.wh.sum * this.blockNorm }


	//------------------------------------------------------------------------
	// MAGNITUDE - sums

	/*
	NOTE: PVf: only matches w/ stationary signals!
	TODO: why? - is this because we should do +freqs only and then double?
	*/

	// Magnitude of Magnitude of Complex Intensity
	totalMagMagI {
		^this.magMagI.sum * this.blockNorm
	}

	// Magnitude of Complex Intensity
	totalMagI {
		var magI = this.magI;

		^Complex.new(
			magI.real.sum,
			magI.imag.sum
		) * this.blockNorm
	}

	/*
	TODO: Confirm normalization of magnitudes below.
	*/

	// Magnitude of Magnitude of Complex Admittance
	totalMagMagA {
		^this.magMagA.sum
	}

	// Magnitude of Complex Admittance
	totalMagA {
		var magA = this.magA;
		^Complex.new(
			magA.real.sum,
			magA.imag.sum
		)
	}

	// Magnitude of Magnitude of Complex Energy
	totalMagMagW {
		^this.magMagW.sum
	}

	// Magnitude of Complex Energy
	totalMagW {
		var magA = this.magW;
		^Complex.new(
			magA.real.sum,
			magA.imag.sum
		)
	}

	// Magnitude of Magnitude Unit Normalized Complex Intensity - Convenience
	totalMagMagN {
		^this.numFrames.asFloat
	}

	// Magnitude of Unit Normalized Complex Intensity
	totalMagN {
		var magN = this.magN;
		^Complex.new(
			magN.real.sum,
			magN.imag.sum
		)
	}

	//------------------------------------------------------------------------
	// INTENSITY - sums

	// Total Intensity
	totalI {
		var i = this.intensity;

		^this.blockNorm * Complex.new(
			i.real.flop.sum,
			i.imag.flop.sum
		)
	}

	/*
	TODO: document implementation of N, A, W in terms of unweighted averages
	*/
	// Total Intensity (Unit Normalized)
	totalN {
		var n = this.averageN;
		var normFac = this.numFrames;

		^(normFac * n)
	}

	// Total Admittance
	totalA {
		var i = this.totalI;
		var wp = this.totalWp;
		var wpReciprocal = (wp + FoaEval.reg.squared).reciprocal;
		var normFac = this.numFrames;

		^(normFac * i * wpReciprocal)
	}

	// Total Energy
	totalW {
		var i = this.totalI;
		var ws = this.totalWs;
		var wsReciprocal = (ws + FoaEval.reg.squared).reciprocal;
		var normFac = this.numFrames;

		^(normFac * i * wsReciprocal)
	}



	//------------------------------------------------------------------------
	// SOUNDFIELD RADIUS

	/*
	TODO: PVc: valid? valid for (pure) monotonic
			   or.. use time domain?? - if so, refactor -averageRadius
	TODO: PVa: frequency domain implementation?
	NOTE: PVa:
	 - biased towards LF performance
	 - biased towards steady state monotones
	*/
	// Nearfield (Spherical) Radius
	totalRadius { |negRadius = false, sampleRate = nil, speedOfSound = (AtkHoa.speedOfSound)|
		var radius = this.averageRadius(nil, negRadius, sampleRate, speedOfSound);
		var normFac = this.numFrames;
		^(normFac * radius)
	}


	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	// Average measures
	/*
	TODO: confirm weighted averages of PVf & PVa agree
	*/

	//------------------------------------------------------------------------
	// ENERGY - average

	// potential energy
	averageWp { |weights = nil|
		^weights.isNil.if({
			var normFac = this.numFrames.reciprocal;
			normFac * this.totalWp
		}, {
			this.wp.wmean(weights)
		})
	}

	// kinetic energy
	averageWu { |weights = nil|
		^weights.isNil.if({
			var normFac = this.numFrames.reciprocal;
			normFac * this.totalWu
		}, {
			this.wu.wmean(weights)
		})
	}

	// potential & kinetic energy mean
	averageWs { |weights = nil|
		^weights.isNil.if({
			var normFac = this.numFrames.reciprocal;
			normFac * this.totalWs
		}, {
			this.ws.wmean(weights)
		})
	}

	// potential & kinetic energy difference
	averageWd { |weights = nil|
		^weights.isNil.if({
			var normFac = this.numFrames.reciprocal;
			normFac * this.totalWd
		}, {
			this.wd.wmean(weights)
		})
	}

	// Heyser energy density
	averageWh { |weights = nil|
		^weights.isNil.if({
			var normFac = this.numFrames.reciprocal;
			normFac * this.totalWh
		}, {
			this.wh.wmean(weights)
		})
	}


	//------------------------------------------------------------------------
	// MAGNITUDE - average

	// Magnitude of Magnitude of Complex Intensity
	averageMagMagI { |weights = nil|
		^weights.isNil.if({
			var normFac = this.numFrames.reciprocal;
			normFac * this.totalMagMagI
		}, {
			this.magMagI.wmean(weights)
		})
	}

	// Magnitude of Complex Intensity
	averageMagI { |weights = nil|
		^weights.isNil.if({
			var normFac = this.numFrames.reciprocal;
			normFac * this.totalMagI
		}, {
			var magI = this.magI;
			Complex.new(
				magI.real.wmean(weights),
				magI.imag.wmean(weights)
			)
		})
	}

	// Magnitude of Magnitude of Complex Admittance
	averageMagMagA { |weights = nil|
		^weights.isNil.if({
			var normFac = this.numFrames.reciprocal;
			normFac * this.totalMagMagA
		}, {
			this.magMagA.wmean(weights)
		})
	}

	// Magnitude of Complex Admittance
	averageMagA { |weights = nil|
		^weights.isNil.if({
			var normFac = this.numFrames.reciprocal;
			normFac * this.totalMagA
		}, {
			var magA = this.magA;
			Complex.new(
				magA.real.wmean(weights),
				magA.imag.wmean(weights)
			)
		})
	}

	// Magnitude of Magnitude of Complex Energy
	averageMagMagW { |weights = nil|
		^weights.isNil.if({
			var normFac = this.numFrames.reciprocal;
			normFac * this.totalMagMagW
		}, {
			this.magMagW.wmean(weights)
		})
	}

	// Magnitude of Complex Energy
	averageMagW { |weights = nil|
		^weights.isNil.if({
			var normFac = this.numFrames.reciprocal;
			normFac * this.totalMagW
		}, {
			var magW = this.magW;
			Complex.new(
				magW.real.wmean(weights),
				magW.imag.wmean(weights)
			)
		})
	}

	// Magnitude of Magnitude Unit Normalized Complex Intensity - Convenience
	averageMagMagN { |weights = nil|
		^weights.isNil.if({
			1.0
		}, {
			this.magMagN.wmean(weights)
		})
	}

	// Magnitude of Unit Normalized Complex Intensity
	averageMagN { |weights = nil|
		^weights.isNil.if({
			var normFac = this.numFrames.reciprocal;
			normFac * this.totalMagN
		}, {
			var magN = this.magN;
			Complex.new(
				magN.real.wmean(weights),
				magN.imag.wmean(weights)
			)
		})
	}


	//------------------------------------------------------------------------
	// INTENSITY - average

	// Intensity
	averageI { |weights = nil|
		^weights.isNil.if({
			var normFac = this.numFrames.reciprocal;
			normFac * this.totalI
		}, {
			var i = this.intensity;
			var weightsReciprocal = weights.sum.reciprocal;
			Complex.new(  // explicit...
				weightsReciprocal * i.real.collect({ |item| (item * weights).sum }),
				weightsReciprocal * i.imag.collect({ |item| (item * weights).sum }),
			)
		})
	}

	// Admittance
	averageA { |weights = nil|
		^weights.isNil.if({
			var normFac = this.numFrames.reciprocal;
			normFac * this.totalA
		}, {
			var a = this.admittance;
			var weightsReciprocal = weights.sum.reciprocal;
			Complex.new(  // explicit...
				weightsReciprocal * a.real.collect({ |item| (item * weights).sum }),
				weightsReciprocal * a.imag.collect({ |item| (item * weights).sum }),
			)
		})
	}

	// Energy
	averageW { |weights = nil|
		^weights.isNil.if({
			var normFac = this.numFrames.reciprocal;
			normFac * this.totalW
		}, {
			var w = this.energy;
			var weightsReciprocal = weights.sum.reciprocal;
			Complex.new(  // explicit...
				weightsReciprocal * w.real.collect({ |item| (item * weights).sum }),
				weightsReciprocal * w.imag.collect({ |item| (item * weights).sum }),
			)
		})
	}

	// Unit Normalized Intensity
	averageN { |weights = nil|
		^weights.isNil.if({
			var i = this.totalI;
			// var magMagI = this.totalMagMagI;
			var magMagI = Complex.new(
				i.real.squared.sum.sqrt,
				i.imag.squared.sum.sqrt,
			).magnitude;
			var magMagIReciprocal = (magMagI + FoaEval.reg.squared).reciprocal;

			i * magMagIReciprocal
		}, {
			var n = this.intensityN;
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

	// Active-Reactive Soundfield Balance Angle: Alpha
	averageAlpha { |weights|
		var magI = this.averageMagI(weights);
		^atan2(magI.imag, magI.real)
	}

	// Potential-Kinetic Soundfield Balance Angle: Beta
	averageBeta { |weights|
		var wd = this.averageWd(weights);
		var magMagI = this.averageMagMagI(weights);

		^atan2(wd, magMagI)
	}

	// Active-Reactive Vector Alignment Angle: Gamma
	averageGamma { |weights|
		// var gamma = this.gamma;
		// var sinFac = gamma.sin;
		// var cosFac = gamma.cos;
		var i = this.intensity;
		var magI = this.magI;
		var cosFac = (i.real * i.imag).sum;
		var sinFac = ((magI.real * magI.imag).squared - cosFac.squared).abs.sqrt;  // -abs for numerical precision errors

		weights.isNil.if({
			sinFac = sinFac.sum;
			cosFac = cosFac.sum;
		}, {
			sinFac = (sinFac * weights).sum;
			cosFac = (cosFac * weights).sum;
		});

		^atan2(sinFac, cosFac)
	}

	// Active Admittance Balance Angle: Mu
	averageMu { |weights|
		var magAa = this.averageMagA(weights).real;

		// ^(2 * magAa.atan).tan.reciprocal.atan  // the double angle form
		// ^atan2((1 - magAa.squared) / 2, magAa)
		^atan2(1 - magAa.squared, 2 * magAa)
	}

	//------------------------------------------------------------------------
	// SOUNDFIELD INCIDENCE - complex vector: Complex([ thetaA, phiA ], [ thetaR, phiR ])

	// Complex Incidence Angle
	averageThetaPhi { |weights = nil|
		var i = this.averageI(weights);
		var thetaA = atan2(i.real[1], i.real[0]);
		var phiA   = atan2(i.real[2], (i.real[0].squared + i.real[1].squared).sqrt);
		var thetaR = atan2(i.imag[1], i.imag[0]);
		var phiR   = atan2(i.imag[2], (i.imag[0].squared + i.imag[1].squared).sqrt);

		^Complex.new(
			[ thetaA, phiA ],
			[ thetaR, phiR ]
		)
	}

	//------------------------------------------------------------------------
	// SOUNDFIELD RADIUS

	// Nearfield (Spherical) Radius

	/*
	TODO: refactor into this superclass?
	*/
	averageRadius { ^this.subclassResponsibility }
}
