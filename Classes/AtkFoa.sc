AtkFoa {
	classvar <refRadius;
	classvar <defaultOrder;
	classvar format;

	*initClass {
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
}
