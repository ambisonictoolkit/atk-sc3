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

	/* Quantities */

	// potential energy
	wp {
		var p = this.pressure;
		// TODO: may not be needed with complex primitive
		^p.real.squared + p.imag.squared // = (p * p.conj), returns real
	}

	// kinetic energy
	wu {
		var v = this.velocity;
		// TODO: may not be needed with complex primitive
		^(
			v.real.squared + v.imag.squared // = (v * v.conj), returns real
		).sum
	}

	// potential & kinetic energy mean
	ws {
		^[this.wp, this.wu].mean
	}

	// potential & kinetic energy difference
	wd {
		^[this.wp, this.wu.neg].mean
	}

	// // Heyser energy density
	// wh {
	// 	var ws = this.ws;
	// 	var magI = this.magI;
	//
	// 	^ws - magI.imag
	// }


	/* Utils */

	// posting: standard return
	printOn { |stream|
		if (stream.atLimit) { ^this };
		stream << this.class.name << "[ " ;
		this.printItemsOn(stream);
		stream << " ]" ;

	}
}


PVBlock[slot] : Array {

	*newClear { |blockSize|
		^super.fill(blockSize, {
			PVFrame.newClear
			// 4.collect{ Complex(0, 0) } // TODO: update with PVFrame
		})
	}

	*withFrames { |pvFrames|
		^super.fill(pvFrames.size, { |i| pvFrames[i] })
	}


	/* Component access */

	// NOTE: Collecting is faster than, e.g., `.performUnaryOp('p')`

	pressure { ^this.collect(_[0]) }
	p 		 { ^this.collect(_[0]) }
	velocity { ^this.collect(_[1..3]) }
	v		 { ^this.collect(_[1..3]) }


	/* Frame-wise quantities */

	// NOTE: Dispatching with with `performUnaryOp` seems to speed
	//       up calculation of aggregate quantities.

	wp { ^this.performUnaryOp('wp') }
	wu { ^this.performUnaryOp('wu') }
	ws { ^this.performUnaryOp('ws') }
	wd { ^this.performUnaryOp('wd') }

	/* Aggregate quantities */

	totalWp {
		^this.wp.sum
	}


	/* Utils */

	numFrames { ^this.size }

	// posting: standard return
	printOn { |stream|
		if (stream.atLimit) { ^this };
		stream << this.class.name << "[ " ;
		this.printItemsOn(stream);
		stream << " ]" ;
	}

	// TODO:
	// // posting: nicer format for on-demand posting
	// post {
	// 	"%[\n".postf(this.class.name);
	// 	this.do{ |chan, i|
	// 		"\t%( ".postf(chan.class.name);
	// 		chan.real.post; ",".postln;
	// 		"\t\t\t ".post;
	// 		chan.imag.post;
	// 		if (i < 3, { " ),\n" }, { " ) ]" }).post;
	// 	};
	// }
	// postln { this.post; "".postln; }
}

