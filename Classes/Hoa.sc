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
		var d = degree ?? globalOrder;

		^(d+1).squared - (d*2+1)
	}

	*numDegreeCoeffs { |degree|
		var d = degree ?? globalOrder;

		^(d*2+1)
	}
}