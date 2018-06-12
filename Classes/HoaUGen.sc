HoaUGen {
	classvar xzMatrix, yzMatrix;
	classvar jMatrix, kMatrix, jkMatrix, kjMatrix, jkOrder; // stored as MatrixArrays

	/*
	// A place for utilities used by Hoa UGens, e.g.
	// checking the number of input channels,
	// confirming proper hoa order between ins/outs, etc.
	*/

	*confirmOrder { |in, targetOrder| // would you like fries with that?
		var io, to;
		to = targetOrder ?? { Hoa.globalOrder };
		io = Hoa.detectOrder(in.size);
		if (io != to) {
			"[HoaUGen] In order (%) does not match target order (%).".format(io, to).throw
		};

		^to
	}

	// xzMatrix { |order|
	//
	// 	xzMatrix ?? {
	// 		// calculate, set and return the matrix for the given order
	// 		^xzMatrix = HoaXformerMatrix.newSwapAxes(\xz, order);
	// 	};
	//
	// 	case
	// 	{ xzMatrix.order == order } { // matrix already calculated for requested order
	// 		^xzMatrix
	// 	}
	// 	{ xzMatrix.order > order }{   // matrix already calculated for a higher order
	// 		var ncoeffs = Hoa.numOrderCoeffs(order);
	//
	// 		// return a matrix subset up to the requested order
	// 		^HoaMatrix.newFromMatrix(
	// 			xzMatrix.getSub(0, 0, ncoeffs, ncoeffs),
	// 			xzMatrix.set, xzMatrix.type
	// 		)
	// 	}
	// 	{ xzMatrix.order < order }{   // matrix already calculated for lower order, recalculate
	// 		// force recalc
	// 		xzMatrix = nil;
	// 		^HoaUGen.xzMatrix(order)
	// 	}
	// }


	// getSwapAxisMatrix { |axes, order|
	// 	var mtx;
	//
	// 	case
	// 	{ axes == \xz or: { axes == \zx }} {
	// 		if (xzMatrix.isNil) {
	// 			// calculate, set and return the matrix for the given order
	// 			^xzMatrix = HoaXformerMatrix.newSwapAxes(\xz, order);
	// 		} {
	// 			mtx = xzMatrix
	// 		}
	// 	}
	// 	{ axes == \yz or: { axes == \zy }} {
	// 		if (yzMatrix.isNil) {
	// 			// calculate, set and return the matrix for the given order
	// 			^yzMatrix = HoaXformerMatrix.newSwapAxes(\yz, order);
	// 		} {
	// 			mtx = yzMatrix
	// 		}
	// 	}
	// 	{
	// 		"[FoaUGen:getSwapAxisMatrix] Invalid combination of axes (%)".format(axes.asString).throw
	// 	};
	//
	// 	case
	// 	{ mtx.order == order } { // matrix already calculated for requested order
	// 		^mtx
	// 	}
	// 	{ mtx.order > order }{   // matrix already calculated for a higher order
	// 		var ncoeffs = Hoa.numOrderCoeffs(order);
	//
	// 		// return a matrix subset up to the requested order
	// 		^HoaMatrix.newFromMatrix(
	// 			mtx.getSub(0, 0, ncoeffs, ncoeffs),
	// 			mtx.set, mtx.type
	// 		)
	// 	}
	// 	{ mtx.order < order }{   // matrix already calculated for lower order, recalculate
	// 		// force recalc
	// 		switch(mtx.detail,
	// 			\xz, { xzMatrix = nil },
	// 			\yz, { yzMatrix = nil }
	// 		);
	//
	// 		^HoaUGen.getSwapAxisMatrix(axes, order)
	// 	}
	// }


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

	// TODO: revisit whether these should be class methods:
	//       There's a larger question of how HoaUGen inheretence
	//       should work or if it's unnecessary

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

	// faster than AtkMatrixMix, doens't replace zeros with silence
	// mtxarr is a MatrixArray
	*mixMatrix { |in, mtxarr|
		var flopped = mtxarr.flopped;

		^Mix.fill(mtxarr.cols, { |i|
			flopped[i] * in[i]
		})
	}

}


// Rotations applied in order RTT, extrinsic
HoaRTT : HoaUGen {

	*ar { |in, rotate, tilt, tumble, order|
		var o, mJ, mK, mJK;
		var rot, til, tum;

		o = HoaUGen.confirmOrder(in, order);

		mK = this.getJKMatrix('k', o); // "K" matrix;
		mJ = this.getJKMatrix('j', o); // "J" matrix;

		// rotate : Z(rotate)
		rot  = HoaRotate.ar(in, rotate, o);

		// tilt : K -> Z(tilt.neg) -> K
		til = this.mixMatrix(rot, mK);
		til = HoaRotate.ar(til, tilt.neg, o);   // << TODO: tilt.neg, can be removed?
		til = this.mixMatrix(til, mK);

		// combine (J*K)
		mJK = this.getJKMatrix('jk', o);

		// tumble : (J*K) -> Z(pitch) -> J
		tum = this.mixMatrix(til, mJK);
		tum = HoaRotate.ar(tum, tumble, o);
		tum = this.mixMatrix(tum, mJ);

		^tum
	}
}

// Rotation about Z axis
HoaRotate : HoaUGen {

	*ar { |in, radians, order|
		var n;
		var i = 0;
		var out, cos, sin;
		var dex_m, dex_mneg;
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

			// modified indexing to replace subtraction with addition, and 2 multiplications
			c2 = 2 * c[0];
			(1..n-2).do{|idx|
				s[idx+1] = (c2 * s[idx]) - s[idx-1];
				c[idx+1] = (c2 * c[idx]) - c[idx-1];
			};

			(1..n).do{ |l|

				i = 2 * l + i;    // out index to the middle of the band
				out[i] = in[i];   // center coeff is 1, so pass val through

				(1..l).do{ |m|
					cos = c[m-1];
					sin = s[m-1];

					dex_m = i + m;
					dex_mneg = i - m;

					out[dex_mneg] = (cos * in[dex_mneg]) + (sin * in[dex_m]);
					out[dex_m] = (cos * in[dex_m]) - (sin * in[dex_mneg]);

				}
			}
		};

		^out
	}
}

HoaTilt : HoaUGen {
	*ar { |in, radians, order|
		var n, mK, hoa;

		n = HoaUGen.confirmOrder(in, order);

		// "K" matrix: swap Z<>X axes
		mK = HoaUGen.getJKMatrix('k', n);

		// tilt/roll : K -> Z(tilt.neg) -> K
		hoa = HoaUGen.mixMatrix(in, mK);
		hoa = HoaRotate.ar(hoa, radians, n);
		hoa = HoaUGen.mixMatrix(hoa, mK);

		^hoa
	}
}


HoaTumble : HoaUGen {
	*ar { |in, radians, order|
		var o, mJ, hoa;

		o = HoaUGen.confirmOrder(in, order);

		// "J" matrix: swap Z<>Y axes
		mJ = HoaUGen.getJKMatrix('j', o);

		// tumple/pitch : J -> Z(tumble) -> J
		hoa = HoaUGen.mixMatrix(in, mJ);
		hoa = HoaRotate.ar(hoa, radians, o);
		hoa = HoaUGen.mixMatrix(hoa, mJ);

		^hoa
	}
}

// // perform yaw-pitch-roll rotation set (intrinsic)
// // order of operations differs from HoaRTT (extrinsic)
// HoaYPR : HoaUGen {
//
// 	*ar { |in, yaw, pitch, roll, order|
// 		var r, p, y;
// 		var o = HoaUGen.confirmOrder(in, order);
//
// 		/* Rotation */
// 		// note reverse order of rotations to achieve intrinsic YPR
//
// 		// roll : K -> Z(roll.neg) ->
// 		// rot = mtxMix.(hoa, r.kMtx);
// 		// rot = z_direct.(rot, roll.neg);        // << NOTE: roll.neg, can be removed?
// 		r = this.mixMatrix(in, this.getSwapAxisMatrix(\xz, o));
// 		r = HoaRotate.ar(r, roll.neg, o);   // << TODO: roll.neg, can be removed?
//
// 		// -> K*J ->
// 		// combine K matrix and J matrix in one to save a matrix mix
// 		// rot = mtxMix.(rot, r.jMtx_homegrown * r.kMtx); // correct matrix multiply order
// 		p = this.mixMatrix(r,
// 			this.getSwapAxisMatrix(\yz, o) *
// 			this.getSwapAxisMatrix(\xz, o)
// 		);
//
// 		// pitch : -> Z(Pitch) -> J ->
// 		// rot = z_direct.(rot, pitch);
// 		// rot = mtxMix.(rot, r.jMtx_homegrown);
// 		p = HoaRotate.ar(p, p, o);
// 		p = this.mixMatrix(p, this.getSwapAxisMatrix(\yz, o));
//
// 		// yaw : -> Z(yaw) ->
// 		// rotate = z_direct.(tumble, yaw);
// 		y = HoaRotate.ar(p, yaw, o);
//
// 		^y
// 	}
// }


// synonyms
HoaYaw : HoaRotate {}
HoaPitch : HoaTumble {}
HoaRoll : HoaTilt {}
