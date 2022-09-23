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
PVc dimensions:
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

		^(p.real.squared + p.imag.squared).as(Array) // == p * p.conjugate
	}

	// kinetic energy
	wu {
		var v = this.velocity;

		^v.collect({ |v_i|
			(v_i.real.squared + v_i.imag.squared).as(Array) // == v_i * v_i.conjugate
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

		^ws - magI.imag
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

		^magMagI / (wp + FoaEval.reg.squared)
	}

	// Magnitude of Complex Admittance
	magA {
		var magI = this.magI;
		var wp = this.wp;
		var wpReg = wp + FoaEval.reg.squared;

		^Complex.new(  // explicit... slow otherwise!!
			magI.real / wpReg,
			magI.imag / wpReg
		)
	}

	// Magnitude of Magnitude of Complex Energy
	magMagW {
		var magMagI = this.magMagI;
		var ws = this.ws;

		^magMagI / (ws + FoaEval.reg.squared)
	}

	// Magnitude of Complex Energy
	magW {
		var magI = this.magI;
		var ws = this.ws;
		var wsDenom = ws + FoaEval.reg.squared;

		^Complex.new(  // explicit... slow otherwise!!
			magI.real / wsDenom,
			magI.imag / wsDenom
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
		var magMagIDenom = magMagI + FoaEval.reg.squared.squared;

		^Complex.new(  // explicit... slow otherwise!!
			magI.real / magMagIDenom,
			magI.imag / magMagIDenom
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
		var wpDenom = wp + FoaEval.reg.squared;

		^i.collect({ |i_n|
			Complex.new(  // explicit... slow otherwise!!
				i_n.real / wpDenom,
				i_n.imag / wpDenom
			)
		})
	}

	energy {
		var i = this.intensity;
		var ws = this.ws;
		var wsDenom = ws + FoaEval.reg.squared;

		^i.collect({ |i_n|
			Complex.new(  // explicit... slow otherwise!!
				i_n.real / wsDenom,
				i_n.imag / wsDenom
			)
		})
	}

	// Unit Normalized Intensity
	intensityN {
		var i = this.intensity;
		var magMagI = this.magMagI;
		var magMagIDenom = magMagI + FoaEval.reg.squared;

		^i.collect({ |i_n|
			Complex.new(  // explicit... slow otherwise!!
				i_n.real / magMagIDenom,
				i_n.imag / magMagIDenom
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
		)
		* this.blockNorm
	}

	/*
	TODO: Confirm normalization of magnitudes below.
	*/

	// Magnitude of Magnitude of Complex Admittance
	totalMagMagA {
		// was: ^this.magMagA.sum
		var magMagI_tot = this.magMagI.sum;
		var wp_avg = this.wp.sum / this.numFrames;

		^magMagI_tot / (wp_avg + FoaEval.reg.squared)
	}

	// Magnitude of Complex Admittance
	totalMagA {
		// was:
		// var magA = this.magA;
		// ^Complex.new(
		// 	magA.real.sum,
		// 	magA.imag.sum
		// )
		var magI = this.magI;
		var magI_total = Complex(magI.real.sum, magI.imag.sum);
		var wp_avg = this.wp.sum / this.numFrames;

		^magI_total / (wp_avg + FoaEval.reg.squared)
	}

	// Magnitude of Magnitude of Complex Energy
	totalMagMagW {
		//was: ^this.magMagW.sum
		^this.magMagI.sum / ((this.ws.sum / this.numFrames) + FoaEval.reg.squared)
	}

	// Magnitude of Complex Energy
	totalMagW {
		// was:
		// var magA = this.magW;
		// ^Complex.new(
		// 	magA.real.sum,
		// 	magA.imag.sum
		// )
		var magI = this.magI;
		var magI_tot = Complex(magI.real.sum, magI.imag.sum);

		^magI_tot / ((this.ws.sum / this.numFrames) + FoaEval.reg.squared);
	}

	// Magnitude of Magnitude Unit Normalized Complex Intensity - Convenience
	// TODO: should calculate, not assume this normalized mag?
	totalMagMagN {
		^this.numFrames.asFloat
	}

	// Magnitude of Unit Normalized Complex Intensity
	totalMagN {
		// was:
		// var magN = this.magN;
		// ^Complex.new(
		// 	magN.real.sum,
		// 	magN.imag.sum
		// )
		var magI = this.magI;
		var magI_tot = Complex(magI.real.sum, magI.imag.sum);
		var magMagI_avg = (this.magMagI.sum / this.numFrames);

		^magI_tot / (magMagI_avg + FoaEval.reg.squared)

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

		^normFac * n
	}

	// Total Admittance
	totalA {
		var i = this.totalI;
		var wp = this.totalWp;
		var normFac = this.numFrames;

		^normFac * i / (wp + FoaEval.reg.squared)
	}

	// Total Energy
	totalW {
		var i = this.totalI;
		var ws = this.totalWs;
		var normFac = this.numFrames;

		^normFac * i / (ws + FoaEval.reg.squared)
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

		^normFac * radius
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
			this.totalWp / this.numFrames
		}, {
			this.wp.wmean(weights)
		})
	}

	// kinetic energy
	averageWu { |weights = nil|

		^weights.isNil.if({
			this.totalWu / this.numFrames
		}, {
			this.wu.wmean(weights)
		})
	}

	// potential & kinetic energy mean
	averageWs { |weights = nil|

		^weights.isNil.if({
			this.totalWs / this.numFrames
		}, {
			this.ws.wmean(weights)
		})
	}

	// potential & kinetic energy difference
	averageWd { |weights = nil|

		^weights.isNil.if({
			this.totalWd / this.numFrames
		}, {
			this.wd.wmean(weights)
		})
	}

	// Heyser energy density
	averageWh { |weights = nil|

		^weights.isNil.if({
			this.totalWh / this.numFrames
		}, {
			this.wh.wmean(weights)
		})
	}


	//------------------------------------------------------------------------
	// MAGNITUDE - average

	// Magnitude of Magnitude of Complex Intensity
	averageMagMagI { |weights = nil|

		^weights.isNil.if({
			this.totalMagMagI / this.numFrames
		}, {
			this.magMagI.wmean(weights)
		})
	}

	// Magnitude of Complex Intensity
	averageMagI { |weights = nil|
		var magI;

		^weights.isNil.if({
			this.totalMagI / this.numFrames
		}, {
			magI = this.magI;
			Complex.new(
				magI.real.wmean(weights),
				magI.imag.wmean(weights)
			)
		})
	}

	// Magnitude of Magnitude of Complex Admittance
	averageMagMagA { |weights = nil|
		var magMagI_tot, wp_tot;

		^weights.isNil.if({
			// was: this.totalMagMagA / this.numFrames
			magMagI_tot = this.magMagI.sum;
			wp_tot 		= this.wp.sum;

			magMagI_tot / (wp_tot + FoaEval.reg.squared)
		}, {
			this.magMagA.wmean(weights)
		})
	}

	// Magnitude of Complex Admittance
	averageMagA { |weights = nil|
		var magI, magI_tot, wp_tot, magA;

		^weights.isNil.if({
			// was: this.totalMagA / this.numFrames
			magI     = this.magI;
			magI_tot = Complex(magI.real.sum, magI.imag.sum);
			wp_tot   = this.wp.sum;

			^magI_tot / (wp_tot + FoaEval.reg.squared)
		}, {
			magA = this.magA;

			Complex.new(
				magA.real.wmean(weights),
				magA.imag.wmean(weights)
			)
		})
	}

	// Magnitude of Magnitude of Complex Energy
	averageMagMagW { |weights = nil|

		^weights.isNil.if({
			this.totalMagMagW / this.numFrames
		}, {
			this.magMagW.wmean(weights)
		})
	}

	// Magnitude of Complex Energy
	averageMagW { |weights = nil|

		^weights.isNil.if({
			this.totalMagW / this.numFrames
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
			1.0 				// TODO: don't assume?
		}, {
			this.magMagN.wmean(weights)
		})
	}

	// Magnitude of Unit Normalized Complex Intensity
	averageMagN { |weights = nil|
		var magN;

		^weights.isNil.if({
			this.totalMagN / this.numFrames
		}, {
			magN = this.magN;
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
		var i, weightsDenom;

		^weights.isNil.if({
			this.totalI / this.numFrames
		}, {
			i = this.intensity;
			weightsDenom = weights.sum;

			Complex.new(  // explicit...
				i.real.collect({ |ir_n| (ir_n * weights).sum }) / weightsDenom,
				i.imag.collect({ |ii_n| (ii_n * weights).sum }) / weightsDenom,
			)
		})
	}

	// Admittance
	averageA { |weights = nil|
		var a, weightsDenom;

		^weights.isNil.if({
			this.totalA / this.numFrames
		}, {
			a = this.admittance;
			weightsDenom = weights.sum;

			Complex.new(  // explicit...
				a.real.collect({ |ir_n| (ir_n * weights).sum }) / weightsDenom,
				a.imag.collect({ |ii_n| (ii_n * weights).sum }) / weightsDenom,
			)
		})
	}

	// Energy
	averageW { |weights = nil|
		var w, weightsDenom;

		^weights.isNil.if({
			this.totalW / this.numFrames
		}, {
			w = this.energy;
			weightsDenom = weights.sum;
			Complex.new(  // explicit...
				w.real.collect({ |er_n| (er_n * weights).sum }) / weightsDenom,
				w.imag.collect({ |ei_n| (ei_n * weights).sum }) / weightsDenom,
			)
		})
	}

	// Unit Normalized Intensity
	averageN { |weights = nil|
		var i, magMagI, n, weightsDenom;

		^weights.isNil.if({
			i = this.totalI;
			// was: magMagI = this.totalMagMagI;
			magMagI = Complex.new(
				i.real.squared.sum.sqrt,
				i.imag.squared.sum.sqrt,
			).magnitude;

			i / (magMagI + FoaEval.reg.squared)
		}, {
			n = this.intensityN;
			weightsDenom = weights.sum;

			Complex.new(  // explicit...
				n.real.collect({ |ir_n| (ir_n * weights).sum }) / weightsDenom,
				n.imag.collect({ |ii_n| (ii_n * weights).sum }) / weightsDenom,
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
