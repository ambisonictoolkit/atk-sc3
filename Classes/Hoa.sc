Hoa {
	classvar <globalOrder = 3;

	*setGlobalOrder { |order|
		/*
		TODO: run initializations necessary when changing order
		after resources have already been allocated at a different order
		*/
		globalOrder = order;
	}

	*degreeStIdx { |degree|
		^(degree+1).squared - (degree*2 + 1)
	}

	*numDegreeCoeffs { |degree|
		^(degree*2 + 1)
	}

	*numOrderCoeffs { |order|
		^(order + 1).squared
	}

	// Detect the order from the number of channels/coefficients
	// Currently expects full 3D signals
	// TODO: support 2D? mixed order?
	*detectOrder { |numChans|
		var o = (numChans.sqrt - 1);
		if (o % 1 != 0) {
			"Could not detect order from % channels".format(numChans).throw
		};
		^o.asInt;
	}

}