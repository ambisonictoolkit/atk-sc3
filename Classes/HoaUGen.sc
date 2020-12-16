/*
Copyright the ATK Community and Joseph Anderson, 2011-2018
J Anderson  j.anderson[at]ambisonictoolkit.net
M McCrea    mtm5[at]uw.edu

This file is part of SuperCollider3 version of the Ambisonic Toolkit (ATK).

The SuperCollider3 version of the Ambisonic Toolkit (ATK) is free software:
you can redistribute it and/or modify it under the terms of the GNU General
Public License as published by the Free Software Foundation, either version 3
of the License, or (at your option) any later version.

The SuperCollider3 version of the Ambisonic Toolkit (ATK) is distributed in
the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See
the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with the
SuperCollider3 version of the Ambisonic Toolkit (ATK). If not, see
<http://www.gnu.org/licenses/>.
*/


//---------------------------------------------------------------------
//	The Ambisonic Toolkit (ATK) is a soundfield kernel support library.
//
// 	Classes: HoaUGen, HoaRotate, HoaTilt, HoaTumble, HoaYaw,
//           HoaPitch, HoaRoll, HoaPan, HoaRTT, HoaYPR
//
//	The Ambisonic Toolkit (ATK) is intended to bring together a number of tools and
//	methods for working with Ambisonic surround sound. The intention is for the toolset
//	to be both ergonomic and comprehensive, providing both classic and novel algorithms
//	to creatively manipulate and synthesise complex Ambisonic soundfields.
//
//	The tools are framed for the user to think in terms of the soundfield kernel. By
//	this, it is meant the ATK addresses the holistic problem of creatively controlling a
//	complete soundfield, allowing and encouraging the composer to think beyond the placement
//	of sounds in a sound-space and instead attend to the impression and image of a soundfield.
//	This approach takes advantage of the model the Ambisonic technology presents, and is
//	viewed to be the idiomatic mode for working with the Ambisonic technique.
//
//	We hope you enjoy the ATK!
//
//	For more information visit http://ambisonictoolkit.net/ or
//	email info[at]ambisonictoolkit.net
//
//---------------------------------------------------------------------


/*

~-- Attribution --~

The method for calculating SH rotation around Z by operating directly
on signal coefficients can be found in Appendix C of:
Jaroslav Krivánek, Jaakko Konttinen, Sumanta Pattanaik, Kadi Bouatouch.
Fast Approximation to Spherical Harmonic Rotation. [Research Report] PI 1728, 2005, pp.14.

The algorithm was modified slightly to conform to ATK's orientation
convention as well as a slight refactor.

*/

HoaUGen {
	classvar xzMatrix, yzMatrix;
	classvar jMatrix, kMatrix, jkMatrix, kjMatrix, jkOrder; // stored as MatrixArrays

	/*
	// A place for utilities used by Hoa UGens, e.g.
	// checking the number of input channels,
	// confirming proper hoa order between ins/outs, etc.
	*/

	// TODO: revisit whether these should be class methods:
	//       There's a larger question of how HoaUGen inheretence
	//       should work or if it's unnecessary

	//  Confirm that the input signal size matches
	//  the number of harmonics for the order.
	//  Returns the order if signal size is valid.
	*confirmOrder { |in, order = (AtkHoa.defaultOrder)|
		var io, to;

		to = order;
		io = AtkHoa.detectOrder(in.size);
		if(io != to, {
			Error(
				"[HoaUGen] In order (%) does not match target order (%).".format(io, to)
			).errorString.postln;
			this.halt
		});

		^to
	}

	//  Confirm that the input signal size matches
	//  the number of expected inputs.
	//  Returns the input signal size if match is valid.
	*confirmNumInputs { |in, numInputs = (AtkHoa.defaultOrder.asHoaOrder.size)|
		var inNumChannels;

		inNumChannels = in.numChannels;
		if(inNumChannels != numInputs, {
			Error(
				"[HoaUGen] In number of channels (%) does not match expected numInputs (%).".format(inNumChannels, numInputs)
			).errorString.postln;
			this.halt
		});

		^inNumChannels
	}

	// returns a MatrixArray
	*getJKMatrix { |which, order|
		var nCoeffs, mtx;

		if((jkOrder.isNil or: { order > jkOrder }), {
			// j, k matrices haven't been calculated
			// or requesting higher order than has been
			// calculated... (re)calculate
			HoaUGen.prCalcJKMatrices(order)
		});

		mtx = switch(which,
			\k,  { kMatrix }, // swap Z<>X axes
			\j,  { jMatrix }, // swap Z<>Y axes
			\jk, { jkMatrix }, // J * K
			\kj, { kjMatrix } // K * J
		);

		^if(jkOrder > order, {
			nCoeffs = order.asHoaOrder.size;
			MatrixArray.with(
				mtx.getSub(0, 0, nCoeffs, nCoeffs)
			)
		}, {
			mtx
		})
	}

	*prCalcJKMatrices { |order|
		var xz, yz;

		xz = HoaMatrixXformer.newSwapAxes(\xz, order);
		yz = HoaMatrixXformer.newSwapAxes(\yz, order);

		// Save as MatrixArrays for efficiency.
		// Use thresh2 optimization for synth graphs:
		// elements which are zero are optimized out.
		kMatrix = MatrixArray.with(xz.asArray).thresh2(AtkHoa.thresh);
		jMatrix = MatrixArray.with(yz.asArray).thresh2(AtkHoa.thresh);

		jkMatrix = MatrixArray.with(jMatrix * kMatrix);
		kjMatrix = MatrixArray.with(kMatrix * jMatrix);

		jkOrder = order;
	}

	// Faster than AtkMatrixMix: doens't replace zeros with silence.
	// 'mtxArr' is a MatrixArray.
	*mixMatrix { |in, mtxArr|
		var flopped = mtxArr.flopped;

		^Mix.fill(mtxArr.cols, { |i|
			flopped[i] * in[i]
		})
	}

}


//-----------------------------------------------------------------------
// matrix rendering

// Generic matrix renderer
// More efficient than AtkMatrixMix, as doesn't replace zeros until the end.
// Also, confirms in and matrix match.
HoaRenderMatrix : HoaUGen {
	*ar { |in, hoaMatrix|

		// catch
		if(hoaMatrix.class.superclass != HoaMatrix, {
			Error(
				"[HoaUGen] hoaMatrix is not class HoaMatrix."
			).errorString.postln;
			this.halt
		});

		// wrap input as array if needed, for mono inputs
		if(in.isArray.not, { in = [in] });

		// check inputs
		this.confirmNumInputs(in, hoaMatrix.numInputs);

		^UGen.replaceZeroesWithSilence(
			this.mixMatrix(in, MatrixArray.with(hoaMatrix.matrix.asArray))
		)
	}
}

// Synonyms.
HoaEncodeMatrix : HoaRenderMatrix { }
HoaDecodeMatrix : HoaRenderMatrix { }
HoaXformMatrix : HoaRenderMatrix { }

/*
NOTE: we could do more complex error checking, confirming
matrix type (\encoder, &c.) matches class.

Doing so would require more complex design of HoaRenderMatrix,
or separate implementation details for the named type matched classes.

*/


//-----------------------------------------------------------------------
// encoders

// Basic & holophonic encoding.
HoaEncodeDirection : HoaUGen {

	*ar { |in, theta = 0, phi = 0, radius = (AtkHoa.refRadius), order = (AtkHoa.defaultOrder)|
		var toPhi, n, hoaOrder, coeffs;
		var zenith, tumbleRotate;

		// check input is a single channel (mono)
		HoaUGen.confirmNumInputs(in, 1);

		// angle to bring the zenith to phi
		toPhi = phi - 0.5pi;

		n = order;  // order

		hoaOrder = n.asHoaOrder;  // instance order

		// 1) generate basic (real) coefficients at zenith and optimize near-zeros out
		coeffs = hoaOrder.sph(0, 0.5pi);
		coeffs = coeffs.thresh2(AtkHoa.thresh);

		// 2) encode as basic (real) or NFE (complex) wave at zenith
		zenith = if(((radius == nil) || (radius == AtkHoa.refRadius)), {
			// basic: spherical wave @ radius = AtkHoa.refRadius
			in
		}, {
			// NFE
			if(radius == inf, {
				// planewave
				(n + 1).collect({ |l|
					DegreeDist.ar(in, AtkHoa.refRadius, l)
				})[hoaOrder.l]
			}, {
				// spherical wave
				(n + 1).collect({ |l|
					DegreeCtrl.ar(in, radius, AtkHoa.refRadius, l)
				})[hoaOrder.l]
			})
		});
		zenith = coeffs * zenith;  // apply coeffs

		// 3) tumble and rotate to re-align soundfield
		tumbleRotate = HoaRotate.ar(
			HoaTumble.ar(
				zenith,
				toPhi,
				n
			),
			theta,
			n
		);

		// 4) replace zeros
		^UGen.replaceZeroesWithSilence(tumbleRotate)
	}
}


//-----------------------------------------------------------------------
// transformers

// Rotation about Z axis.
HoaRotate : HoaUGen {

	*ar { |in, angle = 0, order = (AtkHoa.defaultOrder)|
		var n;
		var i = 0;
		var out, cos, sin;
		var im, imneg;
		var s, c, c2;
		var ang, ang2;

		n = HoaUGen.confirmOrder(in, order);

		out = Array.newClear(n.asHoaOrder.size);
		out[0] = in[0]; // l == 0

		if(n > 0, {
			s = Array.newClear(n);  // [sin(1 * ang), sin(2 * ang), ... sin(n * ang)]
			c = Array.newClear(n);  // [cos(1 * ang), cos(2 * ang), ... cos(n * ang)]

			// precompute first 2 sin/cos for recurrence
			ang = angle;
			s[0] = sin(ang);
			c[0] = cos(ang);
			if(n > 1, {
				ang2 = ang * 2;
				s[1] = sin(ang2);
				c[1] = cos(ang2)
			});

			// Note: modified indexing from source to replace subtraction
			// with addition, and 2 multiplications
			if(n > 2, {
				c2 = 2 * c[0];
				(1..(n - 2)).do({ |idx|
					s[idx + 1] = (c2 * s[idx]) - s[idx - 1];
					c[idx + 1] = (c2 * c[idx]) - c[idx - 1]
				})
			});

			(1..n).do({ |l|

				i = 2 * l + i;   // output index of the middle of the band
				out[i] = in[i];  // center coeff is 1, so pass val through

				(1..l).do({ |m|
					cos = c[m - 1];
					sin = s[m - 1];

					im = i + m;    // positive m index
					imneg = i - m; // negative m index

					out[imneg] = (cos * in[imneg]) + (sin * in[im]);
					out[im] = (cos * in[im]) - (sin * in[imneg])
				})
			})
		});

		^out
	}
}

// Rotation about X axis.
HoaTilt : HoaUGen {
	*ar { |in, angle = 0, order = (AtkHoa.defaultOrder)|
		var n, mK, hoa;

		n = HoaUGen.confirmOrder(in, order);

		// "K" matrix: swap Z<>X axes
		mK = HoaUGen.getJKMatrix(\k, n);

		// tilt/roll : K -> Z(tilt) -> K
		// Note: the rotation is negated here to conform with
		// ambisonic rotation conventions: a positive tilt brings
		// +Y toward +Z. Once X and Z are swapped (via "K" matrix),
		// bringing +Y toward +Z requires a rotation in the clockwise
		// (negative) direction.
		hoa = HoaUGen.mixMatrix(in, mK);
		hoa = HoaRotate.ar(hoa, angle.neg, n);
		^HoaUGen.mixMatrix(hoa, mK);
	}
}

// Rotation about Y axis.
HoaTumble : HoaUGen {
	*ar { |in, angle = 0, order = (AtkHoa.defaultOrder)|
		var n, mJ, hoa;

		n = HoaUGen.confirmOrder(in, order);

		// "J" matrix: swap Z<>Y axes
		mJ = HoaUGen.getJKMatrix(\j, n);

		// tumple/pitch : J -> Z(tumble) -> J
		hoa = HoaUGen.mixMatrix(in, mJ);
		hoa = HoaRotate.ar(hoa, angle, n);
		^HoaUGen.mixMatrix(hoa, mJ);
	}
}


// Synonyms.
HoaYaw : HoaRotate { }
HoaPitch : HoaTumble { }
HoaRoll : HoaTilt { }


// Compound rotations applied in sequential order:
// Rotate > Tilt > Tumble.
// Extrinsic, "laboratory-fixed" axes.
HoaRTT : HoaUGen {
	*ar { |in, rotate = 0, tilt = 0, tumble = 0, order = (AtkHoa.defaultOrder)|
		var n, mJ, mK, mJK;
		var hoa;

		n = HoaUGen.confirmOrder(in, order);

		mK = this.getJKMatrix(\k, n);   // "K" matrix: swap Z<>X axes
		mJ = this.getJKMatrix(\j, n);   // "J" matrix: swap Z<>Y axes
		mJK = this.getJKMatrix(\jk, n); // combine (J * K)

		// rotate : Z(rotate)
		hoa  = HoaRotate.ar(in, rotate, n);

		// tilt : K -> Z(tilt) ->
		hoa = HoaUGen.mixMatrix(hoa, mK);
		hoa = HoaRotate.ar(hoa, tilt.neg, n); // tilt.neg: see note in HoaTilt

		// combine (J * K)
		hoa = HoaUGen.mixMatrix(hoa, mJK);

		// tumble : -> Z(pitch) -> J
		hoa = HoaRotate.ar(hoa, tumble, n);
		^HoaUGen.mixMatrix(hoa, mJ);
	}
}

// Compound rotation: Yaw-Pitch-Roll.
// Mixed intrinsic/extrinsic.
// This rotation differs from HoaRTT, which is extrinsic.
HoaYPR : HoaUGen {

	*ar { |in, yaw = 0, pitch = 0, roll = 0, order = (AtkHoa.defaultOrder)|
		var n, mK, mJ, mJK, hoa;

		n = HoaUGen.confirmOrder(in, order);

		// Note reversed order of rotations to achieve intrinsic YPR

		mK = this.getJKMatrix(\k, n);    // "K" matrix: swap Z<>X axes
		mJ = this.getJKMatrix(\j, n);    // "J" matrix: swap Z<>Y axes
		mJK = this.getJKMatrix(\jk, n);  // combine (J * K)

		// roll (tilt) : K -> Z(tilt) ->
		hoa = HoaUGen.mixMatrix(in, mK);
		hoa = HoaRotate.ar(hoa, roll.neg, n); // roll.neg: see note in HoaTilt

		// combine (J * K)
		hoa = HoaUGen.mixMatrix(hoa, mJK);

		// pitch (tumble) : -> Z(tumble) -> J
		hoa = HoaRotate.ar(hoa, pitch, n);
		hoa = HoaUGen.mixMatrix(hoa, mJ);

		// yaw (rotate)
		^HoaRotate.ar(hoa, yaw, n);
	}
}

// Soundfield mirroring.
HoaReflect : HoaUGen {

	*ar { |in, reflect = \reflect, order = (AtkHoa.defaultOrder)|
		var n, mirrorCoeffs;

		n = HoaUGen.confirmOrder(in, order);

		mirrorCoeffs = n.asHoaOrder.reflection(reflect);

		^(mirrorCoeffs * in);
	}
}

// Near-field Effect - Proximity: AtkHoa.refRadius
// NOTE: unstable, requires suitably pre-conditioned input to avoid overflow
HoaNFProx : HoaUGen {

	*ar { |in, order = (AtkHoa.defaultOrder)|
		var n = HoaUGen.confirmOrder(in, order);

		// NFE: collect filtered harmonics, clumped by degree
		^(n + 1).collect({ |l|
			DegreeProx.ar(
				in[l.asHoaDegree.indices],
				AtkHoa.refRadius,
				l
			)
		}).flat // flatten back to full order coefficients
	}
}

// Near-field Effect - Distance: AtkHoa.refRadius
HoaNFDist : HoaUGen {

	*ar { |in, order = (AtkHoa.defaultOrder)|
		var n = HoaUGen.confirmOrder(in, order);

		// NFE: collect filtered harmonics, clumped by degree
		^(n + 1).collect({ |l|
			DegreeDist.ar(
				in[l.asHoaDegree.indices],
				AtkHoa.refRadius,
				l
			)
		}).flat // flatten back to full order coefficients
	}
}

// Near-field Effect - Control
// Use cases:
//     1) Decoder compensation & NFE "looking":
//            encRadius = AtkHoa.refRadius
//            decRadius = target decoder radius
//     2) NFE encoding, encode NFE from basic:
//            encRadius = target encoding radius
//            decRadius = AtkHoa.refRadius
//     3) NFE re-imaging, move source to target:
//            encRadius = target (re-imaged) encoding radius
//            decRadius = source encoding radius
HoaNFCtrl : HoaUGen {

	*ar { |in, encRadius = (AtkHoa.refRadius), decRadius = (AtkHoa.refRadius), order = (AtkHoa.defaultOrder)|
		var n, nfcByDegree;

		n = HoaUGen.confirmOrder(in, order);

		// NFE: collect filtered harmonics, clumped by degree
		^(n + 1).collect({ |l|
			DegreeCtrl.ar(
				in[l.asHoaDegree.indices],
				encRadius,
				decRadius,
				l
			)
		}).flat // flatten back to full order coefficients
	}
}

// Basic & NFE beaming.
// Gain matched to beam.
HoaBeam : HoaUGen {

	*ar { |in, theta = 0, phi = 0, radius = (AtkHoa.refRadius), beamShape = \basic, order = (AtkHoa.defaultOrder)|
		var n, basicCoeffs, beamCoeffs, toPhi;
		var hoaOrder, degreeSeries, beamWeights;
		var rotateTumble, weighted, mono, zenith, tumbleRotate;

		// angle to bring the zenith to phi
		toPhi = phi - 0.5pi;

		n = HoaUGen.confirmOrder(in, order);

		hoaOrder = n.asHoaOrder;  // instance order
		degreeSeries = Array.series(n + 1, 1, 2);

		// 1) generate and normalize beam weights
		beamWeights = hoaOrder.beamWeights(beamShape);
		beamWeights = beamWeights / (degreeSeries * beamWeights).sum;

		// 2) generate basic (real) coefficients at zenith and optimize near-zeros out
		basicCoeffs = hoaOrder.sph(0, 0.5pi);
		basicCoeffs = basicCoeffs.thresh2(AtkHoa.thresh);

		// 3) form beam coefficients
		beamCoeffs = beamWeights[hoaOrder.l] * basicCoeffs;

		// 3) rotate and tumble to align soundfield with beam (at zenith)
		rotateTumble = HoaTumble.ar(
			HoaRotate.ar(
				in,
				-1 * theta,
				n
			),
			-1 * toPhi,
			n
		);

		// 4) apply beam weights
		weighted = beamCoeffs * rotateTumble;

		// 5) apply NFE (optimized by degree) and form beam
		mono = if(((radius == nil) || (radius == AtkHoa.refRadius)), {
			// basic: beamform @ radius = AtkHoa.refRadius
			weighted.sum
		}, {
			// NFE
			if(radius == inf, {
				// planewave - unstable!
				(n + 1).collect({ |l|
					DegreeProx.ar(
						weighted[l.asHoaDegree.indices].sum,
						AtkHoa.refRadius,
						l
					)
				}).sum
			}, {
				// spherical wave
				(n + 1).collect({ |l|
					DegreeCtrl.ar(
						weighted[l.asHoaDegree.indices].sum,
						AtkHoa.refRadius,
						radius,
						l
					)
				}).sum
			})
		});

		// 6) encode as basic (real) or NFE (complex) wave at zenith
		zenith = if(((radius == nil) || (radius == AtkHoa.refRadius)), {
			// basic: spherical wave @ radius = AtkHoa.refRadius
			mono
		}, {
			// NFE
			if(radius == inf, {
				// planewave
				(n + 1).collect({ |l|
					DegreeDist.ar(mono, AtkHoa.refRadius, l)
				})[hoaOrder.l]
			}, {
				// spherical wave
				(n + 1).collect({ |l|
					DegreeCtrl.ar(mono, radius, AtkHoa.refRadius, l)
				})[hoaOrder.l]
			})
		});
		zenith = basicCoeffs * zenith;  // apply coeffs

		// 7) tumble and rotate to re-align soundfield
		tumbleRotate = HoaRotate.ar(
			HoaTumble.ar(
				zenith,
				toPhi,
				n
			),
			theta,
			n
		);

		// 8) replace zeros
		^UGen.replaceZeroesWithSilence(tumbleRotate)
	}
}

// Basic & NFE nulling.
// Gain matched to beam.
HoaNull : HoaUGen {

	*ar { |in, theta = 0, phi = 0, radius = (AtkHoa.refRadius), beamShape = \basic, order = (AtkHoa.defaultOrder)|
		var null;

		// form null
		null = in - HoaBeam.ar(in, theta, phi, radius, beamShape, order);

		^null
	}
}


//-----------------------------------------------------------------------
// decoders

// Basic & NFE decoding / beaming.
// Gain matched to beam.
HoaDecodeDirection : HoaUGen {

	*ar { |in, theta = 0, phi = 0, radius = (AtkHoa.refRadius), beamShape = \basic, order = (AtkHoa.defaultOrder)|
		var n, coeffs, toPhi;
		var hoaOrder, degreeSeries, beamWeights;
		var rotateTumble, weighted;

		// angle to bring the zenith to phi
		toPhi = phi - 0.5pi;

		n = HoaUGen.confirmOrder(in, order);

		hoaOrder = n.asHoaOrder;  // instance order
		degreeSeries = Array.series(n + 1, 1, 2);

		// 1) generate and normalize beam weights
		beamWeights = hoaOrder.beamWeights(beamShape);
		beamWeights = beamWeights / (degreeSeries * beamWeights).sum;

		// 2) generate basic (real) coefficients at zenith and optimize near-zeros out
		coeffs = hoaOrder.sph(0, 0.5pi);
		coeffs = coeffs.thresh2(AtkHoa.thresh);

		// 3) form beam coefficients
		coeffs = beamWeights[hoaOrder.l] * coeffs;

		// 3) rotate and tumble to align soundfield with beam (at zenith)
		rotateTumble = HoaTumble.ar(
			HoaRotate.ar(
				in,
				-1 * theta,
				n
			),
			-1 * toPhi,
			n
		);

		// 4) apply beam weights
		weighted = coeffs * rotateTumble;

		// 5) apply NFE (optimized by degree) and form beam
		^if(((radius == nil) || (radius == AtkHoa.refRadius)), {
			// basic: beamform @ radius = AtkHoa.refRadius
			weighted.sum
		}, {
			// NFE
			if(radius == inf, {
				// planewave - unstable!
				(n + 1).collect({ |l|
					DegreeProx.ar(
						weighted[l.asHoaDegree.indices].sum,
						AtkHoa.refRadius,
						l
					)
				}).sum
			}, {
				// spherical wave
				(n + 1).collect({ |l|
					DegreeCtrl.ar(
						weighted[l.asHoaDegree.indices].sum,
						AtkHoa.refRadius,
						radius,
						l
					)
				}).sum
			})
		})
	}
}


//-----------------------------------------------------------------------
// Near-field Effect by Degree utilities

DegreeProx {
	*ar { |in, radius = (AtkHoa.refRadius), degree = 0|
		var out, coeffDict;

		// degree 0
		out = in;

		// degree >= 1
		if(degree > 0, {
			coeffDict = NFECoeffs.new(degree).prox(radius, SampleRate.ir);

			// FOS
			if(coeffDict.keys.includes(\fos), {
				coeffDict[\fos].do({ |coeffs|
					out = FOS.ar(out, coeffs[0], coeffs[1], coeffs[2])
				})
			});

			// SOS
			coeffDict[\sos].do({ |coeffs|
				out = SOS.ar(out, coeffs[0], coeffs[1], coeffs[2], coeffs[3], coeffs[4])
			});

			// g
			out = coeffDict[\g] * out;
		});

		^out
	}
}

DegreeDist {
	*ar { |in, radius = (AtkHoa.refRadius), degree = 0|
		var out, coeffDict;

		// degree 0
		out = in;

		// degree >= 1
		if(degree > 0, {
			coeffDict = NFECoeffs.new(degree).dist(radius, SampleRate.ir);

			// FOS
			if(coeffDict.keys.includes(\fos), {
				coeffDict[\fos].do({ |coeffs|
					out = FOS.ar(out, coeffs[0], coeffs[1], coeffs[2])
				})
			});

			// SOS
			coeffDict[\sos].do({ |coeffs|
				out = SOS.ar(out, coeffs[0], coeffs[1], coeffs[2], coeffs[3], coeffs[4])
			});

			// g
			out = coeffDict[\g] * out;
		});

		^out
	}
}

DegreeCtrl {
	*ar { |in, encRadius = (AtkHoa.refRadius), decRadius = (AtkHoa.refRadius), degree = 0|
		var out, coeffDict;

		// degree 0
		out = in;

		// degree >= 1
		if(degree > 0, {
			coeffDict = NFECoeffs.new(degree).ctrl(encRadius, decRadius, SampleRate.ir);

			// FOS
			if(coeffDict.keys.includes(\fos), {
				coeffDict[\fos].do({ |coeffs|
					out = FOS.ar(out, coeffs[0], coeffs[1], coeffs[2])
				})
			});

			// SOS
			coeffDict[\sos].do({ |coeffs|
				out = SOS.ar(out, coeffs[0], coeffs[1], coeffs[2], coeffs[3], coeffs[4])
			});

			// g
			out = coeffDict[\g] * out;
		});

		^out
	}
}
