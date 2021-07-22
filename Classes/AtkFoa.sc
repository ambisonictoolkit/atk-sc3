AtkFoa {
	classvar <speedOfSound, <refRadius;
	classvar <defaultOrder;
	classvar format;

	*initClass {
		speedOfSound = 333.0;  // (m/s)
		/*
		NOTE: this value is implied by the current setting of the cutoff
		frequency (53 Hz) found in the sc3-plugins code for FoaProximity & FoaNFC

		freq = speedOfSound / 2pi  // wavenumber = 1
		speedOfSound = 53 * 2pi  // 333.0

		this value is the speed of sound @ 3C
		https://www.weather.gov/epz/wxcalc_speedofsound

		TODO: consider updating sc3-plugins to align with
		AtkHoa.speedOfSound = 343.0
		*/
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
		NOTE: this is a convenience value, only effecting FoaNF (radial analysis);
		does NOT currently update FoaProximity & FoaNFC
		*/
		speedOfSound = mpsAsFloat;
		^mpsAsFloat
	}
}
