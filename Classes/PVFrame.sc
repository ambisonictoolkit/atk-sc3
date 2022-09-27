PVBlock[slot] : Array {

	*newClear { |blockSize|
		^super.fill(blockSize, {
			4.collect{ Complex(0, 0) }
		})
	}

	pressure { ^this.collect(_[0]) }
	velocity { ^this.collect(_[1..3]) }

	totalWp {
		var p = this.pressure;
		// var p_conj = p.collect(_.conjugate);
		// var wp = (p * p_conj).real
		var wp = p.real.squared + p.imag.squared; // == (p * p_conj).real

		^wp.sum
	}

	// posting: standard return
	printOn { |stream|
		if (stream.atLimit) { ^this };

		stream << this.class.name << "[ " ;
		this.printItemsOn(stream);
		stream << " ]" ;
	}
}

PVFrame[slot] : Array {
	var frame;

	*with { |pvArray|
		^super.newCopyArgs(pvArray);
	}
}

ComplexArray[] {
	var <arr;

	// *new { |me, i|
	// 	"new".postln;
	// 	[me, i].postln;
	// ^super.new(me.real, me.imag) }

	add { |item, i|
		"add".postln;
		[item, i].postln;

        arr = arr.add(item)
    }
}

MyClass[] {
    var <allOfThem; // the collection being aggregated

	// A Slotted Class will call *new and this.add(obj)
	// for every object in the collection.
    add { |item|
        allOfThem = allOfThem.add(item)
    }
}

// PVFrame[slot] : Complex {
// ComplexArray[slot] : Complex {
//
// 	// *newClear { |size|
// 	// 	^Array.newClear(size)
// 	// }
// 	/*	*newClear { |size|
// 	var cArray = Array.fill(4, { Complex.new(0, 0) });
// 	^cArray
// 	}
//
// 	*rand {
// 	var cArray = 4.collect({
// 	Complex.new(1.0.bilinrand, 1.0.bilinrand)
// 	}) * [sqrt(3)/3, 1, 1, 1];
// 	^cArray
// 	}
// 	*/
// 	*with { |... args|
// 		if (args.collect(_.isKindOf(Complex)).every({|bool| bool}).not) {
// 			"A ComplexArray must be filled with Complex objects.".error
// 		};
//
// 		^args
// 	}
// }