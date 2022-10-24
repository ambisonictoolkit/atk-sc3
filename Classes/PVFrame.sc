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
		// = (p * p.conj), returns real
		^p.real.squared + p.imag.squared		// TODO: may not be needed with complex primitive
	}

	// Kinetic energy
	wkin {
		var v = this.velocity;
		^(	// = (v * v.conj), returns real
			v.real.squared + v.imag.squared		// TODO: may not be needed with complex primitive
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
	/  Aggregate intensimetric quantities */

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

	// Intensity
	magIa { ^this.intensity.activeMag }
	magIr { ^this.intensity.reactiveMag }
	magIaNorm { ^this.magIa / (this.magIcv + Atk.regSq) } // TODO: should this be normalized by magIa instead?
	magIrNorm { ^this.magIr / (this.magIcv + Atk.regSq) } // TODO: should this be normalized by magIr instead?

	// Admittance
	magAa { ^this.admittance.activeMag }
	magAr { ^this.admittance.reactiveMag }
	magAaNorm { ^this.magAa / (this.magAcv + Atk.regSq) } // TODO: should this be normalized by magAa instead?
	magArNorm { ^this.magAr / (this.magAcv + Atk.regSq) } // TODO: should this be normalized by magAr instead?


	// Energy
	magWa { ^this.magIa / (this.wpkmean + Atk.regSq) }
	magWr { ^this.magIr / (this.wpkmean + Atk.regSq) }
	magWaNorm { ^this.magWa / (this.magWcv + Atk.regSq) } // TODO: should this be normalized by magWa instead?
	magWrNorm { ^this.magWr / (this.magWcv + Atk.regSq) } // TODO: should this be normalized by magWr instead?

	// "Complex vector magnitudes" 			// TODO Rename? magIcv? magIc? magIt (total)?
	magIcv {
		^sqrt(this.wpot * this.wkin)
		// Equivalent to:
		// = this.intensity.cmag.vmag
		// = Complex(this.magIa, this.magIr).magnitude
		// = IntensityFrame:-cvmag = hypot(activeMag, reactiveMag)
	}
	magAcv {
		var a = this.admittance;
		^hypot(a.activeMag, a.reactiveMag)
		// Equivalent to:
		// = a.cmag.vmag
		// = Complex(this.magAa, this.magAr).magnitude
	}
	magWcv {
		^this.magIcv / (this.wpkmean + Atk.regSq)
	}

	magIcvNorm {
		^Array.fill(this.numFrames, { 1.0 })
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
		^atan2(this.wpkdiff, this.magIcv)
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

	// "Complex vector mag"
	cvmag {
		^hypot(this.activeMag, this.reactiveMag)
		/* Equivalent to:
		/  = this.cmag.vmag
		/  = Complex(this.activeMag, this.reactiveMag).magnitude
		/  = PVFrame:-magIcv = (wpot * wkin).sqrt */
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

	// beta { // use PVFrame: needs the pressure-velocity components

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
		^this.real.thetaPhi
	}
	// Incidence Angle: reactive
	reactiveThetaPhi {
		^this.imag.thetaPhi
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
	// alpha { // use IntensityFrame: needs the intensity components
	// beta  { // use PVFrame: needs the pressure-velocity components
	// gamma { // use IntensityFrame: needs the intensity components
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

	// Note: Dispatching with with `performUnaryOp` seems to speed
	//       up calculation of aggregate quantities.
	wpot 	{ ^this.performUnaryOp('wpot') }
	wp      { ^this.performUnaryOp('wpot') }

	wkin 	{ ^this.performUnaryOp('wkin') }
	wv      { ^this.performUnaryOp('wkin') } 	// wu? wk?

	wpkmean { ^this.performUnaryOp('wpkmean') }
	ws      { ^this.performUnaryOp('wpkmean') }	// wm?

	wpkdiff { ^this.performUnaryOp('wpkdiff') }
	wd      { ^this.performUnaryOp('wpkdiff') }

	wdens 	{ ^this.performUnaryOp('wdens') }
	wh      { ^this.performUnaryOp('wdens') }

	/* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	/  Aggregate quantities		*/

	// NOTE: Use CCBlock for efficiency if accessing these quantities
	// multiple times, e.g. for higher level quantities using intensity
	intensity  { ^IntensityBlock.newFromPVBlock(this) }
	admittance { ^AdmittanceBlock.newFromPVBlock(this) }

	/* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	/  Magnitudes		*/

	magIa  { ^this.performUnaryOp('magIa') }
	magIr  { ^this.performUnaryOp('magIr') }
	magAa  { ^this.performUnaryOp('magAa') }
	magAr  { ^this.performUnaryOp('magAr') }
	magWa  { ^this.performUnaryOp('magWa') }
	magWr  { ^this.performUnaryOp('magWr') }
	magIcv { ^this.performUnaryOp('magIcv') }
	magAcv { ^this.performUnaryOp('magAcv') }
	magWcv { ^this.performUnaryOp('magWcv') }

	/* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	/  Total (sum) measures	*/

	blockNorm { this.subclassResponsibility }

	totalWpot    { ^this.blockNorm * this.wpot.sum }
	totalWkin    { ^this.blockNorm * this.wkin.sum }
	totalWpkmean { ^this.blockNorm * this.wpkmean.sum }
	totalWpkdiff { ^this.blockNorm * this.wpkdiff.sum }
	totalWdens   { ^this.blockNorm * this.wdens.sum }

	totalMagIa  { ^this.blockNorm * this.magIa.sum }
	totalMagIr  { ^this.blockNorm * this.magIr.sum }
	totalmagIcv { ^this.blockNorm * this.magIcv.sum }

	totalMagAa  { ^this.totalMagIa  / (this.wpot.mean + Atk.regSq) }
	totalMagAr  { ^this.totalMagIr  / (this.wpot.mean + Atk.regSq) }
	totalMagAcv { ^this.totalmagIcv / (this.wpot.mean + Atk.regSq) }
	// or?: totalMagAcv { ^this.magIcv.sum / (this.wpot.mean + Atk.regSq) }

	totalMagWa  { ^this.totalMagIa  / (this.wpkmean.mean + Atk.regSq) }
	totalMagWr  { ^this.totalMagIr  / (this.wpkmean.mean + Atk.regSq) }
	totalMagWcv { ^this.totalmagIcv / (this.wpkmean.mean + Atk.regSq) }
	// or?: totalMagAcv { ^this.magWcv.sum / (this.wpot.mean + Atk.regSq) }

	// TODO: should these be normalized by the avg magnitude over the frame?
	//       and: subclass for different normalization for time vs. freq domain frame?
	totalMagIaNorm { ^this.totalMagIa / (this.magIcv.mean + Atk.regSq) }
	totalMagIrNorm { ^this.totalMagIr / (this.magIcv.mean + Atk.regSq) }
	totalMagAaNorm { ^this.totalMagAa / (this.magAcv.mean + Atk.regSq) }
	totalMagArNorm { ^this.totalMagAr / (this.magAcv.mean + Atk.regSq) }
	totalMagWaNorm { ^this.totalMagWa / (this.magWcv.mean + Atk.regSq) }
	totalMagWrNorm { ^this.totalMagWr / (this.magWcv.mean + Atk.regSq) }

	totalIa { ^this.blockNorm * this.intensity.active.sum }
	totalIr { ^this.blockNorm * this.intensity.reactive.sum }
	totalAa { ^this.numFrames * this.totalIa / (this.totalWpot + Atk.regSq) }
	totalAr { ^this.numFrames * this.totalIr / (this.totalWpot + Atk.regSq) }
	totalWa { ^this.numFrames * this.totalIa / (this.totalWpkmean + Atk.regSq) }
	totalWr { ^this.numFrames * this.totalIr / (this.totalWpkmean + Atk.regSq) }

	totalIaNorm { ^this.numFrames * this.avgIaNorm }
	totalIrNorm { ^this.numFrames * this.avgIrNorm }
	totalAaNorm { ^this.numFrames * this.avgAaNorm }
	totalArNorm { ^this.numFrames * this.avgArNorm }
	totalWaNorm { ^this.numFrames * this.avgWaNorm }
	totalWrNorm { ^this.numFrames * this.avgWrNorm }

	/* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	/  Mean (avg) measures	*/

	prBlockAvg { |quantity, weights|
		^if(weights.isNil, {
			// == this.totalWxxx / numFrames
			(this.blockNorm * quantity.sum) / this.numFrames
		}, {
			quantity.wmean(weights)
		})
	}

	avgWpot    { |weights = nil| ^this.prBlockAvg(this.wpot, weights) }
	avgWkin    { |weights = nil| ^this.prBlockAvg(this.wkin, weights) }
	avgWpkmean { |weights = nil| ^this.prBlockAvg(this.wpkmean, weights) }
	avgWpkdiff { |weights = nil| ^this.prBlockAvg(this.wpkdiff, weights) }
	avgWdens   { |weights = nil| ^this.prBlockAvg(this.wdens, weights) }

	avgMagIa  { |weights = nil| ^this.prBlockAvg(this.magIa, weights) }
	avgMagIr  { |weights = nil| ^this.prBlockAvg(this.magIr, weights) }
	avgMagIcv { |weights = nil| ^this.prBlockAvg(this.magIcv, weights) }

	// TODO: below averages should be confirmed and refactored

	avgMagAa  { |weights = nil|
		^if(weights.isNil,
			{ this.magIa.sum / (this.wp.sum + Atk.regSq)},
			{ this.magAa.wmean(weights) }
		)
	}
	avgMagAr  { |weights = nil|
		^if(weights.isNil,
			{ this.magIr.sum / (this.wp.sum + Atk.regSq)},
			{ this.magAr.wmean(weights) }
		)
	}
	avgMagAcv { |weights = nil| ^this.prBlockAvg(this.magAcv, weights) }

	avgMagWa  { |weights = nil|
		^if(weights.isNil,
			{ this.totalMagWa / this.numFrames },
			{ this.magWa.wmean(weights) }
		)
	}
	avgMagWr  { |weights = nil|
		^if(weights.isNil,
			{ this.totalMagWr / this.numFrames },
			{ this.magWr.wmean(weights) }
		)
	}
	avgMagWcv { |weights = nil| ^this.prBlockAvg(this.magWcv, weights) }

	avgMagIaNorm  { |weights = nil|
		^if(weights.isNil,
			{ this.totalMagIa / this.numFrames },
			{ this.magIaNorm.wmean(weights) }	// TODO: check accumulating regularization
		)
	}
	avgMagIrNorm  { |weights = nil|
		^if(weights.isNil,
			{ this.totalMagIr / this.numFrames },
			{ this.magIrNorm.wmean(weights) }	// TODO: check accumulating regularization
		)
	}
	avgMagIcvNorm { |weights = nil|
		^if(weights.isNil,
			{ 1.0 }, 							// TODO: don't assume?
			{ this.magIcvNorm.wmean(weights) }
		)
	}
}


PVFBlock[slot] : PVBlock {

	blockNorm { ^2 / this.numFrames }
}


PVABlock[slot] : PVBlock {

	blockNorm { ^1 }
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

	// dispatch methods to the frames
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