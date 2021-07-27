AtkFoa {
	classvar <speedOfSound, <refRadius;
	classvar <defaultOrder;
	classvar format;

	*initClass {
		speedOfSound = 343.0;  // (m/s) - NOTE: 333.0 m/s for versions <= v5.0.3
		refRadius = inf;  // reference encoding / decoding radius, i.e., basic radius

		defaultOrder = 1;

		format = [\fuma, \fuma];  // fuma, Gerzon / Furse-Malham (MaxN)
	}

	*format {
		^format
	}

	*ordering {
		^format[0]
	}

	*normalisation {
		^format[1]
	}

	*setSpeedOfSound { |mps|
		var mpsAsFloat;

		mpsAsFloat = mps.asFloat;
		/*
		TODO: run initializations necessary when changing speedOfSound
		after resources have already been allocated at a different speedOfSound
		*/
		speedOfSound = mpsAsFloat;
		^mpsAsFloat
	}
}
