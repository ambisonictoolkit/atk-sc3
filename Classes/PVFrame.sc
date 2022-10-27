PVFrame[slot] : Array {

	*newClear {
		^super.fill(4, { Complex(0, 0) })
	}

	*newRand {
		^super.fill(4,
			{ |i|
				var weight = if (i==0) { 3.sqrt/3 } { 1.0 };
				Polar(weight.rand, 2pi.rand).asComplex
			}
		)
	}

	*newFromArray { |array|
		^super.fill(4, { |i| array[i] })
	}

	/* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	/  Component Access  */

	pressure { ^this[0] }
	p 		 { ^this[0] }
	velocity { ^this[1..3] }
	v		 { ^this[1..3] }

	/* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	/  Energetic quantities  */

	// Potential energy
	wpot {
		var p = this.pressure;
		// Equivalent to: p * p.conj; returns real
		^p.real.squared + p.imag.squared
	}

	// Kinetic energy
	wkin {
		var v = this.velocity;
		// Equivalent to: v * v.conj; returns real
		^(v.real.squared + v.imag.squared).sum
	}

	// Potential & kinetic energy mean
	wpkmean {
		// [this.wpot, this.wkin].mean // slower
		^(this.wpot + this.wkin) / 2
	}

	// Potential & kinetic energy difference
	wpkdiff {
		// TODO: .mean or .sum?
		// [this.wpot, this.wkin.neg].mean // slower
		^(this.wpot - this.wkin) / 2
	}

	// Energy density (Heyser)
	wdens {
		^this.wpkmean - this.magIr
	}

	// Synonyms 								// TODO: revisit
	wp { ^this.wpot }
	wv { ^this.wkin }	 						// wu? wk?
	ws { ^this.wpkmean } 						// wm?
	wd { ^this.wpkdiff }
	wh { ^this.wdens }


	/* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	/  Aggregate intensimetric quantities  */

	// TODO: this varies over a block in the freq-domain version (PVf)
	intensity {
		^IntensityFrame.newFromArray(
			this.pressure * this.velocity.conj;
		)
	}

	// Note - perform this with the block when possible
	admittance {
		^AdmittanceFrame.newFromArray(
			this.intensity / (this.wpot + Atk.regSq)
		)
		// var i = this.intensity;
		// var wpot = this.wpot;
		// var wpotReg = wpot + Atk.regSq;
		//
		// ^AdmittanceFrame.newFromArray(
		// 	i.collect({ |i_n|
		// 		Complex.new(  // explicit... slow otherwise!!	// TODO: test to confirm
		// 			i_n.real / wpotReg,
		// 			i_n.imag / wpotReg
		// 		)
		// 	})
		// )
	}


	/* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	/  Magnitudes
	/
	/  Note - these are largely for convenience, and will
	/  calculate the intensity/admittance on each
	/  method call. For multiple calls, call on
	/  IntensityFrame directly to avoid recalculations.
	*/

	/* Intensity */
	magI  { ^this.intensity.magnitude   }
	magIa { ^this.intensity.activeMag   }
	magIr { ^this.intensity.reactiveMag }
	/* "Complex vector magnitude"
	/  Equivalent to:
	/    = this.intensity.cmag.vmag
	/    = magnitude(Complex(magIa, magIr))
	/    = IntensityFrame:-cvmag = hypot(activeMag, reactiveMag)
	*/
	magIcv { ^sqrt(this.wpot * this.wkin) } // local calc 		// TODO Rename? magIcv? magIc? magIt (total)?
	// Magnitudes normalized by cvmag
	magINorm  { ^this.intensity.magNorm         }
	magIaNorm { ^this.intensity.activeMagNorm   }
	magIrNorm { ^this.intensity.reactiveMagNorm }

	/* Admittance */
	magA      { ^this.admittance.magnitude       }
	magAa     { ^this.admittance.activeMag       }
	magAr     { ^this.admittance.reactiveMag     }
	magAcv    { ^this.admittance.cvmag           }
	// Magnitudes normalized by cvmag
	magANorm  { ^this.admittance.magNorm         }
	magAaNorm { ^this.admittance.activeMagNorm   }
	magArNorm { ^this.admittance.reactiveMagNorm }

	/* Energy */
	magW      { ^this.magI   / (this.wpkmean + Atk.regSq) }
	magWa     { ^this.magIa  / (this.wpkmean + Atk.regSq) }
	magWr     { ^this.magIr  / (this.wpkmean + Atk.regSq) }
	magWcv    { ^this.magIcv / (this.wpkmean + Atk.regSq) }
	// Magnitudes normalized by cvmag
	magWNorm  { ^this.magW  / (this.magWcv + Atk.regSq) } // TODO: remove + Atk.regSq? it's in magWcv
	magWaNorm { ^this.magWa / (this.magWcv + Atk.regSq) }
	magWrNorm { ^this.magWr / (this.magWcv + Atk.regSq) }


	/* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	/  Indicators: analyzed quantities		*/

	// Alpha: Active-Reactive Soundfield Balance Angle
	alpha { ^this.intensity.alpha }

	// Beta: Potential-Kinetic Soundfield Balance Angle
	beta  { ^atan2(this.wpkdiff, this.magIcv) }

	// Gamma: Active-Reactive Vector Alignment Angle
	gamma { ^this.intensity.gamma }

	// Mu: Active Admittance Balance Angle
	mu    { ^this.admittance.mu }

	/* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	/  Utils  */

	// Posting: standard return
	printOn { |stream|
		if (stream.atLimit) { ^this };
		stream << this.class.name << "[ " ;
		this.printItemsOn(stream);
		stream << " ]" ;
	}
}


// Frame representing a spatial 3-vector (components can be Complex)
CartesianFrame[slot] : Array {

	*newClear {
		^super.fill(3, { Complex(0, 0) })
	}

	*newFromArray { |array|
		^super.fill(3, { |i| array[i] })
	}

	x { ^this[0] }
	y { ^this[1] }
	z { ^this[2] }
	// theta, phi, thetaPhi: inherited from extArray

	// Posting: standard return
	printOn { |stream|
		if (stream.atLimit) { ^this };
		stream << this.class.name << "[ " ;
		this.printItemsOn(stream);
		stream << " ]" ;
	}
}


AbstractIntensityFrame[slot] : CartesianFrame {

	active   { ^this.real }
	reactive { ^this.imag }

	magnitude   { ^Complex(this.activeMag, this.reactiveMag) }
	activeMag   { ^this.real.vmag } // TODO: amag, rmag? aMag, rMag? maga, magr? activeMag, reactiveMag?
	reactiveMag { ^this.imag.vmag }
	cvmag       {
		/* "Complex vector magnitude"
		/  Equivalent to:
		/    = this.cmag.vmag
		/    = Complex(this.activeMag, this.reactiveMag).magnitude
		/    = PVFrame:-magIcv = (wpot * wkin).sqrt */
		^hypot(this.activeMag, this.reactiveMag)
	}

	// TODO: both active and reactive are scaled by the "complex" vector magnitude
	// 	     Is there a use for active / active.vmag and reactive / reactive.vmag ?
	// 		 Rename to unitCVNorm?
	unitNorm         { ^this / (this.cvmag + Atk.regSq) }
	activeUnitNorm   { ^this.active / (this.cvmag + Atk.regSq) }
	reactiveUnitNorm { ^this.reactive / (this.cvmag + Atk.regSq) }

	magNorm {
		var aMag  = this.activeMag;
		var rMag  = this.reactiveMag;
		var cvMag = hypot(aMag, rMag) + Atk.regSq;

		^Complex(
			aMag / cvMag,
			rMag / cvMag
		);
	}
	activeMagNorm {
		var aMag  = this.activeMag;
		var cvMag = hypot(aMag, this.reactiveMag) + Atk.regSq;

		^aMag / cvMag;
	}
	reactiveMagNorm {
		var rMag  = this.reactiveMag;
		var cvMag = hypot(this.activeMag, rMag) + Atk.regSq;

		^rMag / cvMag;
	}

	/* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	/  Indicators: analyzed quantities  */

	// TODO: consider the significance of alpha and gamma of _Admittance_

	// Alpha: Active-Reactive Soundfield Balance Angle
	alpha {
		^atan2(this.activeMag, this.reactiveMag)
	}

	// Beta: use PVFrame:-beta, needs the pressure-velocity components

	// Gamma: Active-Reactive Vector Alignment Angle
	gamma {
		var cosFac = (this.active * this.reactive).sum;
		var sinFac = ((this.activeMag * this.reactiveMag).squared - cosFac.squared).abs.sqrt;  // -abs for numerical precision errors

		^atan2(sinFac, cosFac)
	}

	// Incidence Angle: active
	activeThetaPhi {
		// var theta = atan2(ia.y, ia.x);
		// var phi   = atan2(ia.z, (ia.x.squared + ia.y.squared).sqrt);
		// ^[ theta, phi ]
		^this.real.thetaPhi
	}

	// Incidence Angle: reactive
	reactiveThetaPhi { ^this.imag.thetaPhi }
}


IntensityFrame[slot] : AbstractIntensityFrame {

	*newFromPVFrame { |pvFrame|
		^pvFrame.intensity
	}

	intensity { ^this }

	// TODO:  admittance { ^ }

	/* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	/  Indicators: analyzed quantities */

	// Mu: Active Admittance Balance Angle
	// Note - requires calculation of admittance on demand
	mu { ^AdmittanceFrame.newFromPVFrame(this).mu }
}


AdmittanceFrame[slot] : AbstractIntensityFrame {

	*newFromPVFrame { |pvFrame|
		^pvFrame.admittance
	}

	admittance { ^this }

	// TODO:  intensity { ^ }

	/* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	/  Indicators: analyzed quantities */

	// Mu: Active Admittance Balance Angle
	mu {
		var activeMag = this.activeMag;
		// ^(2 * magAa.atan).tan.reciprocal.atan  // the double angle form
		// ^atan2((1 - magAa.squared) / 2, magAa)
		^atan2(1 - activeMag.squared, 2 * activeMag)
	}
}
