Hoa {
    classvar <speedOfSound, <refRadius;
	classvar <within;
	classvar <defaultOrder;
	classvar <ordering = \acn, <normalisation = \n3d;

	*initClass {
		speedOfSound = 343.0;  // (m/s)
		refRadius = 1.5;  // reference encoding / decoding radius, i.e., basic radius
		within = -180.dbamp;  // zero optimisation threshold

		defaultOrder = 3;
	}

	*format {
		^Array.with(this.ordering, this.normalisation)
	}

	*setSpeedOfSound { |argSpeedOfSound|
		/*
		TODO: run initializations necessary when changing speedOfSound
		after resources have already been allocated at a different speedOfSound
		*/
		speedOfSound = argSpeedOfSound.asFloat;
	}

	*setRefRadius { |radius|
		/*
		TODO: run initializations necessary when changing refRadius
		after resources have already been allocated at a different refRadius
		*/
		refRadius = radius.asFloat;
	}

	*setWithin { |argWithin|
		within = argWithin.abs;
	}

	*setWithinDb { |withinDb|
		within = withinDb.dbamp;
	}

	*withinDb {
		^within.ampdb;
	}

	*setDefaultOrder { |order|
		/*
		TODO: run initializations necessary when changing order
		after resources have already been allocated at a different order
		*/
		defaultOrder = order.asInteger;
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

	// full 3D only
	*detectOrder { |numCoeffs|
		var squareOf = numCoeffs.squareOf;
		(squareOf == nil).if({
			"Could not detect order from % coefficients".format(numCoeffs).throw
		}, {
			^(squareOf - 1)
		});
	}

	*confirmOrder { |numCoeffs, order|
		var n = order ?? { Hoa.defaultOrder };
		^(this.detectOrder(numCoeffs) == n)
	}

}