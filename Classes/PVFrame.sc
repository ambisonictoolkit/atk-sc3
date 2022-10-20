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
		^super.fill(array.size, { |i| array[i] })
	}

	/* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	/  Component Access			*/

	pressure { ^this[0] }
	p 		 { ^this[0] }
	velocity { ^this[1..3] }
	v		 { ^this[1..3] }

	/* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	/  Energetic quantities		*/

	// Potential energy
	wpot {
		var p = this.pressure;
		// = (p * p.conj), returns real 		// TODO: may not be needed with complex primitive
		^p.real.squared + p.imag.squared
	}

	// Kinetic energy
	wkin {
		var v = this.velocity;
		^(	// = (v * v.conj), returns real 	// TODO: shortcut may not be needed with complex primitive
			v.real.squared + v.imag.squared
		).sum
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
	wv { ^this.wkin }	 // wu? wk?
	ws { ^this.wpkmean } // wm?
	wd { ^this.wpkdiff }
	wh { ^this.wdens }


	/* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	/  Aggregate quantities: intensimetric */

	// TODO: this varies over a block in the freq-domain version (PVf)
	intensity {
		^IntensityFrame.newFromArray(
			this.pressure * this.velocity.conj;
		)
	}

	// NOTE: perform this with the block when possible
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
	/  Magnitudes	*/

	magIa { ^this.intensity.activeMag }
	magIr { ^this.intensity.reactiveMag }

	magAa { ^this.admittance.activeMag }
	magAr { ^this.admittance.reactiveMag }

	magWa {
		^this.magIa / (this.wpkmean + Atk.regSq)
	}
	magWr {
		^this.magIr / (this.wpkmean + Atk.regSq)
	}

	// Complex vector magnitudes 			// TODO Rename? magIcv? magIc? magIt (total)?
	cvmagI {
		^sqrt(this.wpot * this.wkin)
		// Equivalent to:
		// = this.intensity.cmag.vmag
		// = Complex(this.magIa, this.magIr).magnitude
		// = IntensityFrame:-cvmag = hypot(activeMag, reactiveMag)
	}
	cvmagA {
		var a = this.admittance;
		^hypot(a.activeMag, a.reactiveMag)
		// Equivalent to:
		// = a.cmag.vmag
		// = Complex(this.magAa, this.magAr).magnitude
	}
	cvmagW {
		^this.cvmagI / (this.wpkmean + Atk.regSq)
	}

	/* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	/  Indicators: analyzed quantities		*/

	// Alpha: Active-Reactive Soundfield Balance Angle
	alpha {
		var i = this.intensity;
		^atan2(i.activeMag, i.reactiveMag)
	}

	// Beta: Potential-Kinetic Soundfield Balance Angle
	beta {
		^atan2(this.wpkdiff, this.cvmagI)
	}

	// Gamma: Active-Reactive Vector Alignment Angle
	gamma {
		var i = this.intensity;
		var cosFac = (i.active * i.reactive).sum;
		var sinFac = ((i.activeMag * i.reactiveMag).squared - cosFac.squared).abs.sqrt;  // -abs for numerical precision errors

		^atan2(sinFac, cosFac)
	}

	// Mu: Active Admittance Balance Angle
	mu {
		var magAa = this.magAa;

		// ^(2 * magAa.atan).tan.reciprocal.atan  // the double angle form
		// ^atan2((1 - magAa.squared) / 2, magAa)
		^atan2(1 - magAa.squared, 2 * magAa)
	}

	/* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	/  Utils	*/

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
		^super.fill(array.size, { |i| array[i] })		// TODO: check size?
	}

	x { ^this[0] }
	y { ^this[1] }
	z { ^this[2] }

	// theta, phi, thetaPhi { // inherited from extArray

	// Posting: standard return
	printOn { |stream|
		if (stream.atLimit) { ^this };
		stream << this.class.name << "[ " ;
		this.printItemsOn(stream);
		stream << " ]" ;
	}

}


IntensityFrame[slot] : CartesianFrame {

	*newFromPVFrame { |pvFrame|
		^pvFrame.intensity
	}

	active   { ^this.real }
	reactive { ^this.imag }

	// amag, rmag? aMag, rMag? maga, magr? activeMag, reactiveMag?
	activeMag   { ^this.real.vmag }
	reactiveMag { ^this.imag.vmag }

	// Complex vector mag
	cvmag {
		^hypot(this.activeMag, this.reactiveMag)
		/* Equivalent to:
		/  = this.cmag.vmag
		/  = Complex(this.activeMag, this.reactiveMag).magnitude
		/  = PVFrame:-cvmagI = (wpot * wkin).sqrt */
	}

	unitNorm {
		^this / (this.cvmag + Atk.regSq)
	}

	/* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	/  Indicators: analyzed quantities		*/

	// Alpha: Active-Reactive Soundfield Balance Angle
	alpha {
		var magI = this.magI;
		^atan2(this.activeMag, this.reactiveMag)
	}

	// beta { // needs the pressure-velocity components

	// Gamma: Active-Reactive Vector Alignment Angle
	gamma {
		var cosFac = (this.active * this.reactive).sum;
		var sinFac = ((this.activeMag * this.reactiveMag).squared - cosFac.squared).abs.sqrt;  // -abs for numerical precision errors

		^atan2(sinFac, cosFac)
	}

	// Mu: Active Admittance Balance Angle
	// 	   NOTE: requires calculation of admittance on demand
	mu {
		^AdmittanceFrame.newFromPVFrame(this).mu;
	}

	// Incidence Angle: active
	activeThetaPhi {
		// var theta = atan2(ia.y, ia.x);
		// var phi   = atan2(ia.z, (ia.x.squared + ia.y.squared).sqrt);
		// ^[ theta, phi ]
		^this.active.thetaPhi
	}
	// Incidence Angle: reactive
	reactiveThetaPhi {
		^this.reactive.thetaPhi
	}
}


AdmittanceFrame[slot] : IntensityFrame {

	*newFromPVFrame { |pvFrame|
		^pvFrame.admittance
	}

	/* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	/  Indicators: analyzed quantities		*/

	// Mu: Active Admittance Balance Angle
	mu {
		var activeMag = this.activeMag;

		// ^(2 * magAa.atan).tan.reciprocal.atan  // the double angle form
		// ^atan2((1 - magAa.squared) / 2, magAa)
		^atan2(1 - activeMag.squared, 2 * activeMag)
	}
	// alpha { // needs the intensity components
	// beta  { // needs the pressure-velocity components
	// gamma { // needs the intensity components
}


/*
/	FrameBlock: Utilities for 2-D arrays, viewed as blocks of frames
/	(e.g. component vectors organized in "rows" of time or frequency)
/	where dim 1 is the frame index, dim 2 is the component index.
/	E.g. IntensityBlock : a block of cartesian component frames, shape: [ numFrames, 3 ]
/		 PVBlock        : pressure-velocity frame blocks, shape: [ numFrames, 4 ]
*/
FrameBlock[slot] : Array {

	numFrames     { ^this.size } // shape[0]
	numComponents { ^this.shape[1] }

	// Synonymouse vector magnitudes, calculates vector magnitude of
	// the frame components ("rows")
	vmag   { ^this.collect(_.vmag) }
	l2norm { ^this.collect(_.l2norm) }


	/* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	/  Posting		*/

	// Standard return
	printOn { |stream|
		if (stream.atLimit) { ^this };
		stream << this.class.name << "[ ";
		this.printItemsOn(stream);
		stream << " ]";
	}
	// Nicer format for posting full block on demand
	post {
		"%[ (size: %)\n".postf(this.class.name, this.size);
		this.do{ |frm, i| "\t".post; frm.postln };
		"  ]".post;
	}
	postln { this.post; "".postln; }
}


PVBlock[slot] : FrameBlock {

	*newClear { |blockSize|
		^super.fill(blockSize, { PVFrame.newClear })
	}

	*newRand { |blockSize|
		^super.fill(blockSize, { PVFrame.newRand })
	}

	// Construct from an 2D array shape: [nFrames, 4]		TODO: check dimensions, now assumes nFrm x 4
	*newFromPVArray { |pvArray|
		var blockSize = pvArray.shape[0];

		^super.fill(blockSize, { |i|
			PVFrame.newFromArray(pvArray[i])
		})
	}

	// Construct from an array of PVFrames
	*withPVFrames { |pvFrames|
		^super.fill(pvFrames.size, { |i| pvFrames[i] })
	}


	/* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	/  Component access		*/

	// NOTE: Collecting is faster than, e.g., `.performUnaryOp('p')`
	pressure { ^this.collect(_[0]) }
	velocity { ^this.collect(_[1..3]) }
	// synonyms
	p	{ ^this.collect(_[0]) }
	v	{ ^this.collect(_[1..3]) }


	/* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	/  Frame-wise quantities	*/

	// NOTE: Dispatching with with `performUnaryOp` seems to speed
	// up calculation of aggregate quantities.
	wpot 	{ ^this.performUnaryOp('wpot') }
	wkin 	{ ^this.performUnaryOp('wkin') }
	wpkmean { ^this.performUnaryOp('wpkmean') }
	wpkdiff { ^this.performUnaryOp('wpkdiff') }
	wdens 	{ ^this.performUnaryOp('wdens') }

	// synonyms												TODO: revisit
	wp { ^this.performUnaryOp('wpot') }
	wv { ^this.performUnaryOp('wkin') } 	// wu? wk?
	ws { ^this.performUnaryOp('wpkmean') }	// wm?
	wd { ^this.performUnaryOp('wpkdiff') }
	wh { ^this.performUnaryOp('wdens') }


	/* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	/  Aggregate quantities		*/

	// NOTE: Use CCBlock for efficiency if accessing these quantities
	// multiple times, e.g. for higher level quantities using intensity
	intensity  { ^IntensityBlock.newFromPVBlock(this) }
	admittance { ^AdmittanceBlock.newFromPVBlock(this) }


	/* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	/  Magnitudes		*/

	magIa { ^this.performUnaryOp('magIa') }
	magIr { ^this.performUnaryOp('magIr') }
	magAa { ^this.performUnaryOp('magAa') }
	magAr { ^this.performUnaryOp('magAr') }
	cvmagI { ^this.performUnaryOp('cvmagI') }
	cvmagA { ^this.performUnaryOp('cvmagA') }


	/* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	/  Total Quantities		*/

	totalWp {
		^this.wp.sum
	}
}


// Intensimetric Cartesian quantities [x,y,z]
CartesianBlock[slot] : FrameBlock {

	x { ^this.collect(_[0]) }
	y { ^this.collect(_[1]) }
	z { ^this.collect(_[2]) }

}

IntensityBlock[slot] : CartesianBlock {

	*newFromPVBlock { |pvBlock|
		var intensity = pvBlock.performUnaryOp('intensity');

		^super.fill(pvBlock.numFrames, { |i|
			intensity[i]
		})
	}

	intensity 	{ ^this }

	// 'perform' ops: dispatch methods to the frames
	active   	{ ^this.performUnaryOp('active') }
	reactive 	{ ^this.performUnaryOp('reactive') }

	activeMag	{ ^this.performUnaryOp('activeMag') }
	reactiveMag	{ ^this.performUnaryOp('reactiveMag') }
	cvmag		{ ^this.performUnaryOp('cvmag') }
}


AdmittanceBlock[slot] : IntensityBlock {

	*newFromPVBlock { |pvBlock|
		var admittance = pvBlock.performUnaryOp('admittance');

		^super.fill(pvBlock.numFrames, { |i|
			admittance[i]
		})
	}

	admittance { ^this }
}


// Class to store intermediate quantities for reuse and multiple access
CCBlock {
	var pvBlock, iBlock, aBlock;
	var wp, wu, ws, wd, wh;

	*newFromPV { |pvArray|
		^super.new.init(pvArray)
	}

	init { |pvArray|
		pvBlock = PVBlock.newFromPV(pvArray);
	}

	wp { ^wp ?? wp = pvBlock.wp }
	wu { ^wu ?? wu = pvBlock.wu }
	ws { ^ws ?? ws = pvBlock.ws }
	wd { ^wd ?? wd = pvBlock.wd }
	wh { ^wh ?? wh = pvBlock.wh }

	intensity {
		^iBlock ?? {
			iBlock = IntensityBlock.newFromPVBlock(pvBlock);
		}
	}

	admittance {
		// ^aBlock ?? {
		// 	aBlock = AdmittanceBlock.newFromIntensiyBlock(this.iBlock);
		// }
		var i = this.intensity;
		var wpReg = this.wp + Atk.regSq;

		^aBlock ?? { aBlock = (i / wpReg) }
		// i.collect({ |i_n|
		// 	Complex.new(  // explicit... slow otherwise!!
		// 		i_n.real / wpReg,
		// 		i_n.imag / wpReg
		// 	)
		// })

	}
}