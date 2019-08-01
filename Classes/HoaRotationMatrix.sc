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

/*
---------------------------------------------------------------------
	Third Party Attribution / License
---------------------------------------------------------------------

The following is largely a port of spherical harmonic rotations from
Archontis Politis's Spherical-Harmonic-Transform Library for Matlab/Octave.

https://github.com/polarch/Spherical-Harmonic-Transform

Specifically:
euler2rotationMatrix.m
getSHrotMtx.m
complex2realSHMtx.m

Politis's code for real SH rotations is a port of Bing Jian's
implementations, found here:
http://www.mathworks.com/matlabcentral/fileexchange/15377-real-valued-spherical-harmonics

Copyright (c) 2016, Bing Jian
Copyright (c) 2015, Archontis Politis
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

* Neither the name of Spherical-Harmonic-Transform nor the names of its
  contributors may be used to endorse or promote products derived from
  this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


The technique was originally authored here:
Ivanic, J., Ruedenberg, K. (1996). Rotation Matrices for Real
Spherical Harmonics. Direct Determination by Recursion. The Journal
of Physical Chemistry, 100(15), 6342?6347.

...with corrections:

Ivanic, J., Ruedenberg, K. (1998). Rotation Matrices for Real
Spherical Harmonics. Direct Determination by Recursion Page: Additions
and Corrections. Journal of Physical Chemistry A, 102(45), 9099-9100.
*/


//---------------------------------------------------------------------
//	The Ambisonic Toolkit (ATK) is a soundfield kernel support library.
//
// 	Class: HoaRotationMatrix
//
//  Note: this implementation is used for complete static matrices,
//  while a more efficient method is used for time-varying rotations which operates
//  on the signal coefficients directly. See rotation (pseudo-)UGens in
//  this library for details on that alternative method.
//
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


HoaRotationMatrix {

	var r1, r2, r3, axes; // copyArgs
	var <matrix, <r3x3, <order;

	*new { |r1 = 0, r2 = 0, r3 = 0, axes = \xyz, order = (AtkHoa.defaultOrder)|
		^super.newCopyArgs(r1,r2,r3,axes).init(order)
	}

	init { |order|
		r3x3 = this.eulerToR3(r1, r2, r3, axes);
		matrix = this.buildSHRotMtx(r3x3, order, 'real');
	}


	/*
	Build the basic 3x3 rotation matrix from Euler angle rotations
	alpha, beta, gamma:  first, second, and third angle of rotation
	axes: definition of the order of axes to rotate, e.g. 'zyz'
	*/
	eulerToR3 { |alpha, beta, gamma, axes|
		var r1, r2, r3, cstr;

		// unpack chars of axes
		cstr = axes.asString;

		// resultant rotations for each axis, the order of which depends on 'axes'
		r1 = this.buildR1(cstr[0].asSymbol, alpha);
		r2 = this.buildR1(cstr[1].asSymbol, beta);
		r3 = this.buildR1(cstr[2].asSymbol, gamma);

		^r3*(r2*r1); // return matrix multiply
	}


	buildR1 { |axis, theta|
		var cost, sint, sint_neg;

		cost = cos(theta);
		sint = sin(theta);
		sint_neg = sint.neg;

		// Note: this R1 kernel follows classic ambisonic rotation
		// convention which differs from ported reference (Politis):
		// commented rows to the right show source's values,
		^Matrix.with(
			switch( axis,
				'x', {[
					[1, 0, 0],
					[0, cost, sint_neg], // [0, cost, sint],
					[0, sint, cost]      // [0, sint_neg, cost]
				]},
				'y', {[
					[cost, 0, sint_neg],
					[0, 1, 0],
					[sint, 0, cost]
				]},
				'z', {[
					[cost, sint_neg, 0], // [cost, sint, 0],
					[sint, cost, 0],     // [sint_neg, cost, 0],
					[0, 0, 1]
				]},
			)
		);
	}

	/*
	Recursively generate a matrix for rotating real SHs up to maxDegree.
	r3x3: the basic 3x3 rotation matrix (via eulerToR3 method)
	*/
	buildSHRotMtx { |r3x3, maxDegree|
		var r;         // output rotation matrix
		var r_1;       // first band rotation matrix
		var r_l;       // current band rotation matrix
		var r_lm1;     // previous band's rotation matrix
		var band_idx;  // offset of sub matrix into the full rotation matrix
		var setSize, denom, u, v, w, d;

		setSize = (maxDegree+1).squared;
		// intialize total rotation matrix of size ((L+1)^2) x ((L+1)^2)
		r = Matrix.newClear(setSize, setSize);
		// zeroth-band (l=0) is invariant to rotation
		r.put(0,0,1);

		if (maxDegree == 0) {^r};

		r_1 = Matrix.newClear(3, 3); // l = 1 rotation matrix

		// r3x3 = [  Rxx Rxy Rxz
		// 			 Ryx Ryy Ryz
		// 			 Rzx Rzy Rzz  ]
		// the first band (l=1) is directly related to the rotation matrix
		r_1.put(0,0, r3x3.at(1,1));
		r_1.put(0,1, r3x3.at(1,2));
		r_1.put(0,2, r3x3.at(1,0));
		r_1.put(1,0, r3x3.at(2,1));
		r_1.put(1,1, r3x3.at(2,2));
		r_1.put(1,2, r3x3.at(2,0));
		r_1.put(2,0, r3x3.at(0,1));
		r_1.put(2,1, r3x3.at(0,2));
		r_1.put(2,2, r3x3.at(0,0));

		// insert this matrix into the output matrix in 1st band
		r_1.rows.do{|rowi|
			r_1.cols.do{|coli|
				r.put(rowi+1, coli+1, r_1.at(rowi, coli))
			}
		};

		if (maxDegree == 1) {^r};

		r_lm1 = r_1;    // 1st band becomes "previous"
		band_idx = 4;   // index into output matrix for placing each new band sub-matrix

		// compute rotation matrix of each subsequent band recursively
		// for band 2 to "l"
		(2..maxDegree).do{ |l|

			setSize = 2*l+1;

			r_l = Matrix.newClear(setSize, setSize);

			(l.neg..l).do{|m|
				(l.neg..l).do{|n|

					// compute u,v,w terms of Eq.8.1 (Table I)
					d = (m==0).asInt; // the delta function d_m0
					denom = if (n.abs==l) {
						(2*l) * (2*l-1)
					}{
						l.squared - n.squared
					};

					u = sqrt((l.squared-m.squared) / denom);
					v = sqrt((1+d) * (l+abs(m)-1) * (l+abs(m)) / denom) * (1-(2*d)) * 0.5;
					w = sqrt((l-abs(m)-1) * (l-abs(m)) / denom) * (1-d) * -0.5;

					//  computes Eq.8.1
					if (u != 0) {u = u * this.prU(l,m,n,r_1,r_lm1)};
					if (v != 0) {v = v * this.prV(l,m,n,r_1,r_lm1)};
					if (w != 0) {w = w * this.prW(l,m,n,r_1,r_lm1)};

					r_l.put(m+l,n+l, u + v + w);
				}
			};

			// insert roation matrix for this band into it's proper
			// place in the output rotation matrix
			r_l.rows.do{ |rowi|
				r_l.cols.do{ |coli|
					r.put(rowi+band_idx, coli+band_idx, r_l.at(rowi, coli))
				}
			};

			// assign current band matrix to previous
			r_lm1 = r_l;

			band_idx = band_idx + setSize;
		};

		^r // return rotation matrix
	}


	/* Functions to compute terms U, V, W of Eq.8.1 (Table II) in Ivanic, J., Ruedenberg, K. (1998) */

	//  Function U
	prU { |l, m, n, r_1, r_lm1|
		^this.prP(0, l, m, n, r_1, r_lm1);
	}

	//  Function V
	prV { |l, m, n, r_1, r_lm1|
		var p0, p1, d;

		^if (m == 0) {
			p0 = this.prP(1, l, 1, n, r_1, r_lm1);
			p1 = this.prP(-1, l, -1, n, r_1, r_lm1);
			p0+p1; // return
		} {
			if (m > 0) {
				d = (m == 1).asInt;
				p0 = this.prP(1, l, m-1, n, r_1, r_lm1);
				p1 = this.prP(-1, l, m.neg+1, n, r_1, r_lm1);
				(p0*sqrt(1+d)) - (p1*(1-d)); // return
			} {
				d = (m == -1).asInt;
				p0 = this.prP(1, l, m+1, n, r_1, r_lm1);
				p1 = this.prP(-1, l, m.neg-1, n, r_1, r_lm1);
				(p0*(1-d)) + (p1*sqrt(1+d)); // return
			}
		}
	}

	//  Function W
	prW { |l, m, n, r_1, r_lm1|
		var p0, p1;

		if (m == 0) {"HoaRotationMatrix:prW should not be called with m = 0".throw};

		^if (m > 0) {
			p0 = this.prP(1, l, m+1, n, r_1, r_lm1);
			p1 = this.prP(-1, l, m.neg-1, n, r_1, r_lm1);
			p0 + p1; // return
		} {
			p0 = this.prP( 1, l, m-1, n, r_1, r_lm1);
			p1 = this.prP(-1, l, m.neg+1,n, r_1, r_lm1);
			p0 - p1; // return
		}
	}

	//  Function P
	prP { |i, l, a, b, r_1, r_lm1|
		var ri1, rim1, ri0;

		ri1 = r_1.at(i+1, 2);
		rim1 = r_1.at(i+1, 0);
		ri0 = r_1.at(i+1, 1);

		^if (b == l.neg) {
			(ri1 * r_lm1.at(a+l-1, 0)) + (rim1 * r_lm1.at(a+l-1, 2*l-2));
		} {
			if (b == l) {
				(ri1 * r_lm1.at(a+l-1, 2*l-2)) - (rim1 * r_lm1.at(a+l-1, 0));
			} {
				ri0 * r_lm1.at(a+l-1, b+l-1);
			}
		}
	}


	// Build a rotation matrix of complex SH from that of the real SH
	// through real-to-complex-transformation matrices
	buildComplexSHRotMtx {
		var setSize, degreeSize, idx, m;
		var diagT, adiagT;
		var diagTMtx, adiagTMtx, tempT, wMtx, conjW;

		setSize = (order+1).squared;

		// complex -> real SH matrix
		wMtx = Matrix.with(
			setSize.collect{
				setSize.collect{
					Complex(0,0)
				}
			}
		);

		wMtx.put(0,0, Complex(1,0)); // l = 0

		if (order > 0) {
			idx = 1;
			(1..order).do{ |l|
				degreeSize = 2*l+1;

				m = (1..l);

				// form the diagonal
				diagT = (
					l.collect({Complex(0,1)}) ++
					[Complex(2.sqrt/2, 0)] ++
					-1.pow(m).collect(Complex(_, 0))
				) / 2.sqrt;

				diagTMtx = Matrix.with(
					degreeSize.collect{
						degreeSize.collect{
							Complex(0,0)
						}
					}
				);
				diagT.do{|me, i| diagTMtx.put(i, i, me)};

				// form the antidiagonal
				adiagT = (
					Complex(0,-1)*(-1).pow(m.reverse) ++
					[Complex(2.sqrt/2, 0)] ++
					1.dup(l).collect(Complex(_, 0))
				) / 2.sqrt;

				adiagTMtx = Matrix.newClear(degreeSize, degreeSize);

				// place into diagnal, flipped L<>R
				adiagT.do{|me, i|
					var dex = degreeSize-i-1;
					adiagTMtx.put(i, dex, me)
				};

				// form the transformation matrix for the specific band n
				tempT = diagTMtx + adiagTMtx;

				tempT.rows.do{|rowi|
					tempT.cols.do{|coli|
						wMtx.put(rowi+idx, coli+idx, tempT.at(rowi, coli))
					}
				};

				idx = idx + (2*l+1);
			}
		};

		conjW = Matrix.with(
			wMtx.rows.collect({ |row|
				wMtx.rows.collect({ |col|
					wMtx.at(row, col).conjugate
				})
			})
		);

		// matrix is the real rotation matrix built by -buildSHRotMtx
		^wMtx.flop * matrix * conjW;
	}

}
