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
	*confirmOrder { |in, order|
		var io, to;
		to = order ?? { Hoa.globalOrder };
		io = Hoa.detectOrder(in.size);
		if (io != to) {
			"[HoaUGen] In order (%) does not match target order (%).".format(io, to).throw
		};

		^to
	}

	*getJKMatrix { |which, order|
		var nCoeffs, mtx;

		if (jkOrder.isNil or: { order > jkOrder }) {
			// j,k matrices haven't been calculated
			// or requesting higher order than has been
			// calculated... (re)calculate
			HoaUGen.prCalcJKMatrices(order)
		};

		mtx = switch (which,
			'k',  { kMatrix }, // swap Z<>X axes
			'j',  { jMatrix }, // swap Z<>Y axes
			'jk', { jkMatrix}, // J * K
			'kj', { kjMatrix } // K * J
		);

		^if (jkOrder > order) {
			nCoeffs = Hoa.numOrderCoeffs(order);
			mtx.getSub(0, 0, nCoeffs, nCoeffs);
		} {
			mtx
		}
	}

	*prCalcJKMatrices { |order|
		var xz, yz;
		var zeroWithin = -180.dbamp;

		xz = HoaXformerMatrix.newSwapAxes(\xz, order);
		yz = HoaXformerMatrix.newSwapAxes(\yz, order);

		// save a MatrixArrays for efficiency
		// zeroWithin - optimization for synth graphs:
		// Zero out matrix elements which are close to zero so they're optimized out.
		kMatrix = MatrixArray.with(xz.asArray).zeroWithin(zeroWithin);
		jMatrix = MatrixArray.with(yz.asArray).zeroWithin(zeroWithin);

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


// Rotation about Z axis.
HoaRotate : HoaUGen {

	*ar { |in, radians, order|
		var n;
		var i = 0;
		var out, cos, sin;
		var im, imneg;
		var s, c, c2;
		var ang, ang2;

		n = HoaUGen.confirmOrder(in, order);

		out = Array.newClear(Hoa.numOrderCoeffs(n));
		out[0] = in[0]; // l == 0

		if (n > 0) {
			s = Array.newClear(n);  // [sin(1*ang), sin(2*ang), ... sin(n*ang)]
			c = Array.newClear(n);  // [cos(1*ang), cos(2*ang), ... cos(n*ang)]

			ang = radians;
			ang2 = ang * 2;

			// precompute first 2 sin/cos for recurrence
			s[0] = sin(ang);
			c[0] = cos(ang);
			s[1] = sin(ang2);
			c[1] = cos(ang2);

			// Note: modified indexing from source to replace subtraction
			// with addition, and 2 multiplications
			c2 = 2 * c[0];
			(1..n-2).do{|idx|
				s[idx+1] = (c2 * s[idx]) - s[idx-1];
				c[idx+1] = (c2 * c[idx]) - c[idx-1];
			};

			(1..n).do{ |l|

				i = 2 * l + i;   // output index of the middle of the band
				out[i] = in[i];  // center coeff is 1, so pass val through

				(1..l).do{ |m|
					cos = c[m-1];
					sin = s[m-1];

					im = i + m;    // positive m index
					imneg = i - m; // negative m index

					out[imneg] = (cos * in[imneg]) + (sin * in[im]);
					out[im] = (cos * in[im]) - (sin * in[imneg]);
				}
			}
		};

		^out
	}
}

// Rotation about X axis.
HoaTilt : HoaUGen {
	*ar { |in, radians, order|
		var n, mK, hoa;

		n = HoaUGen.confirmOrder(in, order);

		// "K" matrix: swap Z<>X axes
		mK = HoaUGen.getJKMatrix('k', n);

		// tilt/roll : K -> Z(tilt) -> K
		// Note: the rotation is negated here to conform with
		// ambisonic rotation conventions: a positive tilt brings
		// +Y toward +Z. Once X and Z are swapped (via "K" matrix),
		// bringing +Y toward +Z requires a rotation in the clockwise
		// (negative) direction.
		hoa = HoaUGen.mixMatrix(in, mK);
		hoa = HoaRotate.ar(hoa, radians.neg, n);
		^HoaUGen.mixMatrix(hoa, mK);
	}
}

// Rotation about Y axis.
HoaTumble : HoaUGen {
	*ar { |in, radians, order|
		var n, mJ, hoa;

		n = HoaUGen.confirmOrder(in, order);

		// "J" matrix: swap Z<>Y axes
		mJ = HoaUGen.getJKMatrix('j', n);

		// tumple/pitch : J -> Z(tumble) -> J
		hoa = HoaUGen.mixMatrix(in, mJ);
		hoa = HoaRotate.ar(hoa, radians, n);
		^HoaUGen.mixMatrix(hoa, mJ);
	}
}


// Synonyms.
HoaYaw : HoaRotate {}
HoaPitch : HoaTumble {}
HoaRoll : HoaTilt {}


// Compound rotations applied in sequential order:
// Rotate > Tilt > Tumble.
// Extrinsic, "laboratory-fixed" axes.
HoaRTT : HoaUGen {
	*ar { |in, rotate, tilt, tumble, order|
		var n, mJ, mK, mJK;
		var hoa;

		n = HoaUGen.confirmOrder(in, order);

		mK = this.getJKMatrix('k', n);   // "K" matrix: swap Z<>X axes
		mJ = this.getJKMatrix('j', n);   // "J" matrix: swap Z<>Y axes
		mJK = this.getJKMatrix('jk', n); // combine (J * K)

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

	*ar { |in, yaw, pitch, roll, order|
		var n, mK, mJ, mJK, hoa;

		n = HoaUGen.confirmOrder(in, order);

		// Note reversed order of rotations to achieve intrinsic YPR

		mK = this.getJKMatrix('k', n);    // "K" matrix: swap Z<>X axes
		mJ = this.getJKMatrix('j', n);    // "J" matrix: swap Z<>Y axes
		mJK = this.getJKMatrix('jk', n);  // combine (J * K)

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


// Planewave panning.
HoaPan : HoaUGen {

	*ar { |in, theta, phi, order|
		var n, pw, pwCoeffs, toPhi, tumble;

		n = order ?? { Hoa.globalOrder };

		// planewave coefficients at zenith
		pwCoeffs = HoaOrder(n).sph(0, 0.5pi);
		// round to optimize near-zeros out
		pwCoeffs = pwCoeffs.round(-180.dbamp);
		// input signal encoded as a planewave at zenith
		pw = in * pwCoeffs;

		// angle to bring the zenith to phi
		toPhi = phi - 0.5pi;

		tumble = HoaTumble.ar(pw, toPhi, n);
		^HoaRotate.ar(tumble, theta, n);
	}
}
