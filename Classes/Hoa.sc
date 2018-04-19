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
}