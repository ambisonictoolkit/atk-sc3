PVFrame[slot] : Array {

	*newClear {
		^super.fill(4, { Complex(0, 0) })
	}

	*newFromArray { |array|
		^super.fill(array.size, { |i| array[i] })
	}

	/* Component access */

	pressure { ^this[0] }
	p 		 { ^this[0] }
	velocity { ^this[1..3] }
	v		 { ^this[1..3] }

	/* Energetic quantities */

	// potential energy
	wpot { // alt: wp, ep?
		var p = this.pressure;
		// = (p * p.conj), returns real 		// TODO: may not be needed with complex primitive
		^p.real.squared + p.imag.squared
	}

	// Kinetic energy
	wkin { // alt: wk?
		var v = this.velocity;
		^(
			// = (v * v.conj), returns real 	// TODO: shortcut may not be needed with complex primitive
			v.real.squared + v.imag.squared
		).sum
	}

	// Potential & kinetic energy mean
	wpkmean { // alt: wm?
		// [this.wpot, this.wkin].mean // slower
		^(this.wpot + this.wkin) / 2
	}

	// Potential & kinetic energy difference
	wpkdiff { // alt: wd?
		// TODO: .mean or .sum?
		// [this.wpot, this.wkin.neg].mean // slower
		^(this.wpot - this.wkin) / 2
	}


	/* Aggregate quantities */

	// Energy density (Heyser)
	wdens { // alt: ?
		^this.wpkmean - this.magIr
	}

	// synonyms 								// TODO: revisit
	wp { ^this.wpot }
	wv { ^this.wkin }		// wu? wk?
	ws { ^this.wpkmean }	// wm?
	wd { ^this.wpkdiff }
	wh { ^this.wdens }

	/* Aggregate quantities: intensimetric */

	// TODO: this varies over a block in the freq-domain version (PVf)
	intensity {
		^IntensityFrame.newFromArray(
			this.pressure * this.velocity.conj;
		)
	}

	// NOTE: perform this with the block when possible
	admittance {
		var i = this.intensity;
		var wpot = this.wpot;
		var wpotReg = wpot + FoaEval.reg.squared;

		^AdmittanceFrame.newFromArray(
			i.collect({ |i_n|
				Complex.new(  // explicit... slow otherwise!!		// TODO: test to confirm
					i_n.real / wpotReg,
					i_n.imag / wpotReg
				)
			})
		)
	}


	/* Magnitudes */

	magIa { ^this.intensity.magA }
	magIr { ^this.intensity.magR }
	magAa { ^this.admittance.magA }
	magAr { ^this.admittance.magR }

	// Equivalent to the Intensity/AdmittanceFrame's magAR = hypot(magA, magR)
	magIar { ^sqrt(this.wpot * this.wkin) } // = Complex(this.magIa, this.magIr).magnitude
	magAar {
		var a = this.admittance;
		^hypot(a.magA, a.magR)		// = Complex(this.magAa, this.magAr).magnitude
	}

	/* Utils */

	// posting: standard return
	printOn { |stream|
		if (stream.atLimit) { ^this };
		stream << this.class.name << "[ " ;
		this.printItemsOn(stream);
		stream << " ]" ;
	}
}

// Frame representing a spatial 3-vector (components can be complex)
CartesianFrame[slot] : Array {

	*newClear {
		^super.fill(3, { Complex(0, 0) })
	}

	*newFromArray { |array|
		// TODO: check size
		^super.fill(array.size, { |i| array[i] })
	}

	x { ^this[0] }
	y { ^this[1] }
	z { ^this[2] }

	// posting: standard return
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
	magA  { ^this.active.vmag }
	magR  { ^this.reactive.vmag }
	// This is equivalent to PVFrame's (wpot * wkin).sqrt
	magAR { ^hypot(this.magA, this.magR) } // = Complex(this.magA, this.magR).magnitude
}

AdmittanceFrame[slot] : IntensityFrame {

	*newFromPVFrame { |pvFrame|
		^pvFrame.admittance
	}
}


/*
Includes utilities for 2-D arrays, viewed as frames of vectors
where dim 1 is the numFrames, dim 2 is numComponents.
e.g. Cartesian components (intensity)  : [ numFrames, 3 ], or
	 SH components (pressure-velocity) : [ numFrames, 4 ]
*/
FrameBlock[slot] : Array {

	numFrames     { ^this.size } 			// shape[0]
	numComponents { ^this.shape[1] }

	vmag   { ^this.collect(_.vmag) }		// vector magnitude
	l2norm { ^this.collect(_.l2norm) }		// vector magnitude
	cmag   { ^this.performUnaryOp('cmag') }	// complex magnitude

	// posting: standard return
	printOn { |stream|
		if (stream.atLimit) { ^this };
		stream << this.class.name << "[ ";
		this.printItemsOn(stream);
		stream << " ]";
	}

	// posting: nicer format for posting full block
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

	// 													TODO: check dimensions, now assumes nFrm x 4
	// Construct from an 2D array shape: [nFrames, 4]
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


	/* Component access */

	// NOTE: Collecting is faster than, e.g., `.performUnaryOp('p')`
	pressure { ^this.collect(_[0]) }
	velocity { ^this.collect(_[1..3]) }

	// synonyms
	p 		 { ^this.collect(_[0]) }
	v		 { ^this.collect(_[1..3]) }


	/* Frame-wise quantities */

	// NOTE: Dispatching with with `performUnaryOp` seems to speed
	//       up calculation of aggregate quantities.
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

	// NOTE: Use CCBlock for efficiency if accessing these quantities
	//       multiple times, e.g. for higher level quantities using intensity
	intensity  { ^IntensityBlock.newFromPVBlock(this) }
	admittance { ^AdmittanceBlock.newFromPVBlock(this) }


	/* Aggregate quantities */

	/* Magnitudes */

	magIa { ^this.performUnaryOp('magIa') }
	magIr { ^this.performUnaryOp('magIr') }
	magAa { ^this.performUnaryOp('magAa') }
	magAr { ^this.performUnaryOp('magAr') }
	magIar { ^this.performUnaryOp('magIar') }
	magAar { ^this.performUnaryOp('magAar') }

	/* Total Quantities */

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

	// dispatch messages to the frames
	active   	{ ^this.performUnaryOp('active') }
	reactive 	{ ^this.performUnaryOp('reactive') }
	magA		{ ^this.performUnaryOp('magA') } // aMag? activeMag
	magR		{ ^this.performUnaryOp('magR') } // rMag? reactiveMag
	magAR		{ ^this.performUnaryOp('magAR') } // arMag? magI?
}


AdmittanceBlock[slot] : IntensityBlock {

	*newFromPVBlock { |pvBlock|
		var admittance = pvBlock.performUnaryOp('admittance');

		^super.fill(pvBlock.numFrames, { |i|
			admittance[i]
		})
	}

	// TODO: revisit — faster?
	// *newFromPVBlock { |pvBlock|
	// 	var intensity = pvBlock.performUnaryOp('intensity');
	// 	var wpReg = pvBlock.wp + FoaEval.reg.squared;
	//
	// 	^super.fill(pvBlock.numFrames, { |i|
	// 		intensity[i] / wpReg[i]
	// 	})
	// }

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
		var wpReg = this.wp + FoaEval.reg.squared;

		^aBlock ?? { aBlock = (i / wpReg) }
		// i.collect({ |i_n|
		// 	Complex.new(  // explicit... slow otherwise!!
		// 		i_n.real / wpReg,
		// 		i_n.imag / wpReg
		// 	)
		// })

	}
}