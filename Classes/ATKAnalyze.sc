/*
	Copyright the ATK Community, Joseph Anderson, and Josh Parmenter, 2011
		J Anderson	j.anderson[at]ambisonictoolkit.net
		J Parmenter	j.parmenter[at]ambisonictoolkit.net


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
//     Class (superclass): FoaEval
//
// 	Class: FoaWp
// 	Class: FoaWu
// 	Class: FoaWs
// 	Class: FoaWd
// 	Class: FoaWh
//
// 	Class: FoaMagI
// 	Class: FoaMagIa
// 	Class: FoaMagIr
// 	Class: FoaMagA
// 	Class: FoaMagAa
// 	Class: FoaMagAr
// 	Class: FoaMagW
// 	Class: FoaMagWa
// 	Class: FoaMagWr
// 	Class: FoaMagNa
// 	Class: FoaMagNr
//
// 	Class: FoaSFPL
// 	Class: FoaSFVL
// 	Class: FoaSFWL
// 	Class: FoaSFWhL
// 	Class: FoaSFIL
//
// 	Class: FoaAlpha
// 	Class: FoaBeta
// 	Class: FoaGamma
//
// 	Class: FoaThetaPhiA
// 	Class: FoaThetaPhiR
//
// 	Class: FoaIa
// 	Class: FoaIr
// 	Class: FoaAa
// 	Class: FoaAr
// 	Class: FoaWa
// 	Class: FoaWr
// 	Class: FoaNa
// 	Class: FoaNr
//
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
//
//	We hope you enjoy the ATK!
//
//	For more information visit http://ambisonictoolkit.net/ or
//	email info[at]ambisonictoolkit.net
//
//---------------------------------------------------------------------

FoaEval : FoaUGen {
	classvar <>reg, <>gamAlpA, <>gamAlpR;

	*initClass {
		reg = -192.dbamp;  // regularization factor
		gamAlpA = 0.01.degrad;  // Active Alpha threshold, for Gamma calc
		gamAlpR = (90.0 - 5.5).degrad;  // Reactive Alpha threshold, for Gamma calc
	}
}


//------------------------------------------------------------------------
// FOA ENERGY - magnitudes

// FOA potential energy
FoaWp : FoaEval {
	*ar { |in, size = 2048, method = \instant|
		var p;

		in = this.checkChans(in);
		p = 2.sqrt * in[0];  // w * 2.sqrt = pressure

		case(
			{ method == \instant }, {
				^HilbertW.ar(p, size).squared.sum
			},
			{ method == \average }, {
				var normFac;

				normFac = 2 * size.reciprocal;

				^(normFac * RunningSum.ar(p.squared, size))
			}
		)
	}
}


// FOA kinetic energy
FoaWu : FoaEval {
	*ar { |in, size = 2048, method = \instant|
		var u;

		in = this.checkChans(in);
		u = in[1..3];  // [x, y, z] = velocity (vector)

		case(
			{ method == \instant }, {

				^HilbertW.ar(u, size).sum({ |item|
					item.squared.sum
				})
			},
			{ method == \average }, {
				var normFac;

				normFac = 2 * size.reciprocal;

				^(normFac * RunningSum.ar(u.squared, size).sum)
			}
		)
	}
}


// FOA potential & kinetic energy mean
FoaWs : FoaEval {
	*ar { |in, size = 2048, method = \instant|
		var p, u;

		in = this.checkChans(in);
		p = 2.sqrt * in[0];  // w * 2.sqrt = pressure
		u = in[1..3];  // [x, y, z] = velocity (vector)

		case(
			{ method == \instant }, {
				var wp, wu;

				wp = HilbertW.ar(p, size).squared.sum;
				wu = HilbertW.ar(u, size).sum({ |item|
					item.squared.sum
				});

				^(0.5 * (wp + wu))
			},
			{ method == \average }, {
				var wp, wu;
				var normFac;

				normFac = 2 * size.reciprocal;

				wp = normFac * RunningSum.ar(p.squared, size);
				wu = normFac * RunningSum.ar(u.squared, size).sum;

				^(0.5 * (wp + wu))
			}
		)
	}
}


// FOA potential & kinetic energy difference
FoaWd : FoaEval {
	*ar { |in, size = 2048, method = \instant|
		var p, u;

		in = this.checkChans(in);
		p = 2.sqrt * in[0];  // w * 2.sqrt = pressure
		u = in[1..3];  // [x, y, z] = velocity (vector)

		case(
			{ method == \instant }, {
				var wp, wu;

				wp = HilbertW.ar(p, size).squared.sum;
				wu = HilbertW.ar(u, size).sum({ |item|
					item.squared.sum
				});

				^(0.5 * (wp - wu))
			},
			{ method == \average }, {
				var wp, wu;
				var normFac;

				normFac = 2 * size.reciprocal;

				wp = normFac * RunningSum.ar(p.squared, size);
				wu = normFac * RunningSum.ar(u.squared, size).sum;

				^(0.5 * (wp - wu))
			}
		)
	}
}


// FOA Heyser energy density
FoaWh : FoaEval {
	*ar { |in, size = 2048, method = \instant|
		var p, u;

		in = this.checkChans(in);
		p = 2.sqrt * in[0];  // w * 2.sqrt = pressure
		u = in[1..3];  // [x, y, z] = velocity (vector)

		case(
			{ method == \instant }, {
				var pReIm, pImRe, uReIm, wp, wu, ws, ir, magIr;

				pReIm = HilbertW.ar(p, size);
				pImRe = [1, -1] * pReIm.reverse;
				uReIm = HilbertW.ar(u, size);

				wp = pReIm.squared.sum;
				wu = uReIm.sum({ |item|
					item.squared.sum
				});
				ws = (0.5 * (wp + wu));

				ir = uReIm.collect({ |item|
					(pImRe * item).sum
				});
				magIr = ir.squared.sum.sqrt;

				^(ws - magIr)
			},
			{ method == \average }, {
				var wp, wu, ws, ia, magI_squared, magIa_squared, magIr;
				var normFac;

				normFac = 2 * size.reciprocal;

				wp = normFac * RunningSum.ar(p.squared, size);
				wu = normFac * RunningSum.ar(u.squared, size).sum;
				ia = normFac * RunningSum.ar(p * u, size);

				ws = (0.5 * (wp + wu));

				magI_squared = wp * wu;
				magIa_squared = ia.squared.sum;
				magIr = (magI_squared - magIa_squared).sqrt;

				^(ws - magIr)
			}
		)
	}
}


//------------------------------------------------------------------------
// FOA INTENSITY - magnitudes

// FOA Magnitude Intensity
FoaMagI : FoaEval {
	*ar { |in, size = 2048, method = \instant|
		var p, u;

		in = this.checkChans(in);
		p = 2.sqrt * in[0];  // w * 2.sqrt = pressure
		u = in[1..3];  // [x, y, z] = velocity (vector)

		case(
			{ method == \instant }, {
				var wp, wu;

				wp = HilbertW.ar(p, size).squared.sum;
				wu = HilbertW.ar(u, size).sum({ |item|
					item.squared.sum
				});

				^(wp * wu).sqrt
			},
			{ method == \average }, {
				var wp, wu;
				var normFac;

				normFac = 2 * size.reciprocal;

				wp = normFac * RunningSum.ar(p.squared, size);
				wu = normFac * RunningSum.ar(u.squared, size).sum;

				^(wp * wu).sqrt
			}
		)
	}
}


// FOA Magnitude Active Intensity
FoaMagIa : FoaEval {
	*ar { |in, size = 2048, method = \instant|
		var p, u;

		in = this.checkChans(in);
		p = 2.sqrt * in[0];  // w * 2.sqrt = pressure
		u = in[1..3];  // [x, y, z] = velocity (vector)

		case(
			{ method == \instant }, {
				var pReIm, ia;

				pReIm = HilbertW.ar(p, size);

				ia = HilbertW.ar(u, size).collect({ |item|
					(pReIm * item).sum
				});

				^ia.squared.sum.sqrt
			},
			{ method == \average }, {
				var ia;
				var normFac;

				normFac = 2 * size.reciprocal;

				ia = normFac * RunningSum.ar(p * u, size);

				^ia.squared.sum.sqrt
			}
		)
	}
}


// FOA Magnitude Reactive Intensity
FoaMagIr : FoaEval {
	*ar { |in, size = 2048, method = \instant|
		var p, u;

		in = this.checkChans(in);
		p = 2.sqrt * in[0];  // w * 2.sqrt = pressure
		u = in[1..3];  // [x, y, z] = velocity (vector)

		case(
			{ method == \instant }, {
				var pImRe, ir;

				pImRe = [1, -1] * HilbertW.ar(p, size).reverse;

				ir = HilbertW.ar(u, size).collect({ |item|
					(pImRe * item).sum
				});

				^ir.squared.sum.sqrt
			},
			{ method == \average }, {
				var wp, wu, ia, magI_squared, magIa_squared;
				var normFac;

				normFac = 2 * size.reciprocal;

				wp = normFac * RunningSum.ar(p.squared, size);
				wu = normFac * RunningSum.ar(u.squared, size).sum;
				ia = normFac * RunningSum.ar(p * u, size);

				magI_squared = wp * wu;
				magIa_squared = ia.squared.sum;

				^(magI_squared - magIa_squared).sqrt
			}
		)
	}
}



// FOA Magnitude Admittance
FoaMagA : FoaEval {
	*ar { |in, size = 2048, method = \instant|
		var p, u;

		in = this.checkChans(in);
		p = 2.sqrt * in[0];  // w * 2.sqrt = pressure
		u = in[1..3];  // [x, y, z] = velocity (vector)

		case(
			{ method == \instant }, {
				var wp, wu, magI;

				wp = HilbertW.ar(p, size).squared.sum;
				wu = HilbertW.ar(u, size).sum({ |item|
					item.squared.sum
				});

				magI = (wp * wu).sqrt;

				^(magI / (wp + DC.ar(FoaEval.reg)))
			},
			{ method == \average }, {
				var wp, wu, magI;
				var normFac;

				normFac = 2 * size.reciprocal;

				wp = normFac * RunningSum.ar(p.squared, size);
				wu = normFac * RunningSum.ar(u.squared, size).sum;

				magI = (wp * wu).sqrt;

				^(magI / (wp + DC.ar(FoaEval.reg)))
			}
		)
	}
}


// FOA Magnitude Active Admittance
FoaMagAa : FoaEval {
	*ar { |in, size = 2048, method = \instant|
		var p, u;

		in = this.checkChans(in);
		p = 2.sqrt * in[0];  // w * 2.sqrt = pressure
		u = in[1..3];  // [x, y, z] = velocity (vector)

		case(
			{ method == \instant }, {
				var pReIm, wp, ia, magIa;

				pReIm = HilbertW.ar(p, size);
				wp = pReIm.squared.sum;

				ia = HilbertW.ar(u, size).collect({ |item|
					(pReIm * item).sum
				});
				magIa = ia.squared.sum.sqrt;

				^(magIa / (wp + DC.ar(FoaEval.reg)))
			},
			{ method == \average }, {
				var wp, ia, magIa;
				var normFac;

				normFac = 2 * size.reciprocal;

				wp = normFac * RunningSum.ar(p.squared, size);
				ia = normFac * RunningSum.ar(p * u, size);
				magIa = ia.squared.sum.sqrt;

				^(magIa / (wp + DC.ar(FoaEval.reg)))
			}
		)
	}
}


// FOA Magnitude Reactive Admittance
FoaMagAr : FoaEval {
	*ar { |in, size = 2048, method = \instant|
		var p, u;

		in = this.checkChans(in);
		p = 2.sqrt * in[0];  // w * 2.sqrt = pressure
		u = in[1..3];  // [x, y, z] = velocity (vector)

		case(
			{ method == \instant }, {
				var pReIm, pImRe, wp, ir, magIr;

				pReIm = HilbertW.ar(p, size);
				pImRe = [1, -1] * pReIm.reverse;

				wp = pReIm.squared.sum;

				ir = HilbertW.ar(u, size).collect({ |item|
					(pImRe * item).sum
				});

				magIr = ir.squared.sum.sqrt;

				^(magIr / (wp + DC.ar(FoaEval.reg)))
			},
			{ method == \average }, {
				var wp, wu, ia, magI_squared, magIa_squared, magIr;
				var normFac;

				normFac = 2 * size.reciprocal;

				wp = normFac * RunningSum.ar(p.squared, size);
				wu = normFac * RunningSum.ar(u.squared, size).sum;
				ia = normFac * RunningSum.ar(p * u, size);

				magI_squared = wp * wu;
				magIa_squared = ia.squared.sum;
				magIr = (magI_squared - magIa_squared).sqrt;

				^(magIr / (wp + DC.ar(FoaEval.reg)))
			}
		)
	}
}


// FOA Magnitude Energy
FoaMagW : FoaEval {
	*ar { |in, size = 2048, method = \instant|
		var p, u;

		in = this.checkChans(in);
		p = 2.sqrt * in[0];  // w * 2.sqrt = pressure
		u = in[1..3];  // [x, y, z] = velocity (vector)

		case(
			{ method == \instant }, {
				var wp, wu, ws, magI;

				wp = HilbertW.ar(p, size).squared.sum;
				wu = HilbertW.ar(u, size).sum({ |item|
					item.squared.sum
				});
				ws = (0.5 * (wp + wu));

				magI = (wp * wu).sqrt;

				^(magI / (ws + DC.ar(FoaEval.reg)))
			},
			{ method == \average }, {
				var wp, wu, ws, magI;
				var normFac;

				normFac = 2 * size.reciprocal;

				wp = normFac * RunningSum.ar(p.squared, size);
				wu = normFac * RunningSum.ar(u.squared, size).sum;
				ws = (0.5 * (wp + wu));

				magI = (wp * wu).sqrt;

				^(magI / (ws + DC.ar(FoaEval.reg)))
			}
		)
	}
}


// FOA Magnitude Active Energy
FoaMagWa : FoaEval {
	*ar { |in, size = 2048, method = \instant|
		var p, u;

		in = this.checkChans(in);
		p = 2.sqrt * in[0];  // w * 2.sqrt = pressure
		u = in[1..3];  // [x, y, z] = velocity (vector)

		case(
			{ method == \instant }, {
				var pReIm, uReIm, wp, wu, ws, ia, magIa;

				pReIm = HilbertW.ar(p, size);
				uReIm = HilbertW.ar(u, size);

				wp = pReIm.squared.sum;
				wu = uReIm.sum({ |item|
					item.squared.sum
				});
				ws = (0.5 * (wp + wu));

				ia = uReIm.collect({ |item|
					(pReIm * item).sum
				});
				magIa = ia.squared.sum.sqrt;

				^(magIa / (ws + DC.ar(FoaEval.reg)))
			},
			{ method == \average }, {
				var wp, wu, ws, ia, magIa;
				var normFac;

				normFac = 2 * size.reciprocal;

				wp = normFac * RunningSum.ar(p.squared, size);
				wu = normFac * RunningSum.ar(u.squared, size).sum;
				ws = (0.5 * (wp + wu));

				ia = normFac * RunningSum.ar(p * u, size);
				magIa = ia.squared.sum.sqrt;

				^(magIa / (ws + DC.ar(FoaEval.reg)))
			}
		)
	}
}


// FOA Magnitude Reactive Energy
FoaMagWr : FoaEval {
	*ar { |in, size = 2048, method = \instant|
		var p, u;

		in = this.checkChans(in);
		p = 2.sqrt * in[0];  // w * 2.sqrt = pressure
		u = in[1..3];  // [x, y, z] = velocity (vector)

		case(
			{ method == \instant }, {
				var pReIm, pImRe, uReIm, wp, wu, ws, ir, magIr;

				pReIm = HilbertW.ar(p, size);
				pImRe = [1, -1] * pReIm.reverse;
				uReIm = HilbertW.ar(u, size);

				wp = pReIm.squared.sum;
				wu = uReIm.sum({ |item|
					item.squared.sum
				});
				ws = (0.5 * (wp + wu));

				ir = uReIm.collect({ |item|
					(pImRe * item).sum
				});
				magIr = ir.squared.sum.sqrt;

				^(magIr / (ws + DC.ar(FoaEval.reg)))
			},
			{ method == \average }, {
				var wp, wu, ws, ia, magI_squared, magIa_squared, magIr;
				var normFac;

				normFac = 2 * size.reciprocal;

				wp = normFac * RunningSum.ar(p.squared, size);
				wu = normFac * RunningSum.ar(u.squared, size).sum;
				ws = (0.5 * (wp + wu));
				ia = normFac * RunningSum.ar(p * u, size);

				magI_squared = wp * wu;
				magIa_squared = ia.squared.sum;
				magIr = (magI_squared - magIa_squared).sqrt;

				^(magIr / (ws + DC.ar(FoaEval.reg)))
			}
		)
	}
}


// FOA Magnitude Unit Normalized Active Intensity
FoaMagNa : FoaEval {
	*ar { |in, size = 2048, method = \instant|
		var p, u;

		in = this.checkChans(in);
		p = 2.sqrt * in[0];  // w * 2.sqrt = pressure
		u = in[1..3];  // [x, y, z] = velocity (vector)

		case(
			{ method == \instant }, {
				var pReIm, uReIm, wp, wu, magI, ia, magIa;

				pReIm = HilbertW.ar(p, size);
				uReIm = HilbertW.ar(u, size);

				wp = pReIm.squared.sum;
				wu = uReIm.sum({ |item|
					item.squared.sum
				});
				magI = (wp * wu).sqrt;

				ia = uReIm.collect({ |item|
					(pReIm * item).sum
				});
				magIa = ia.squared.sum.sqrt;

				^(magIa / (magI + DC.ar(FoaEval.reg)))
			},
			{ method == \average }, {
				var wp, wu, magI, ia, magIa;
				var normFac;

				normFac = 2 * size.reciprocal;

				wp = normFac * RunningSum.ar(p.squared, size);
				wu = normFac * RunningSum.ar(u.squared, size).sum;
				magI = (wp * wu).sqrt;

				ia = normFac * RunningSum.ar(p * u, size);
				magIa = ia.squared.sum.sqrt;

				^(magIa / (magI + DC.ar(FoaEval.reg)))
			}
		)
	}
}


// FOA Magnitude Unit Normalized Reactive Intensity
FoaMagNr : FoaEval {
	*ar { |in, size = 2048, method = \instant|
		var p, u;

		in = this.checkChans(in);
		p = 2.sqrt * in[0];  // w * 2.sqrt = pressure
		u = in[1..3];  // [x, y, z] = velocity (vector)

		case(
			{ method == \instant }, {
				var pReIm, pImRe, uReIm, wp, wu, magI, ir, magIr;

				pReIm = HilbertW.ar(p, size);
				pImRe = [1, -1] * pReIm.reverse;
				uReIm = HilbertW.ar(u, size);

				wp = pReIm.squared.sum;
				wu = uReIm.sum({ |item|
					item.squared.sum
				});
				magI = (wp * wu).sqrt;

				ir = uReIm.collect({ |item|
					(pImRe * item).sum
				});
				magIr = ir.squared.sum.sqrt;

				^(magIr / (magI + DC.ar(FoaEval.reg)))
			},
			{ method == \average }, {
				var wp, wu, magI, ia, magI_squared, magIa_squared, magIr;
				var normFac;

				normFac = 2 * size.reciprocal;

				wp = normFac * RunningSum.ar(p.squared, size);
				wu = normFac * RunningSum.ar(u.squared, size).sum;
				magI = (wp * wu).sqrt;
				ia = normFac * RunningSum.ar(p * u, size);

				magI_squared = wp * wu;
				magIa_squared = ia.squared.sum;
				magIr = (magI_squared - magIa_squared).sqrt;

				^(magIr / (magI + DC.ar(FoaEval.reg)))
			}
		)
	}
}


//------------------------------------------------------------------------
// FOA SOUNDFIELD LEVELS - magnitudes in dB

// FOA SFPL
FoaSFPL : FoaEval {
	*ar { |in, size = 2048, method = \instant|
		var wp;

		wp = FoaWp.ar(in, size = 2048, method);

		^(10 * wp.log10)
	}
}


// FOA SFVL
FoaSFVL : FoaEval {
	*ar { |in, size = 2048, method = \instant|
		var wu;

		wu = FoaWu.ar(in, size = 2048, method);

		^(10 * wu.log10)
	}
}


// FOA SFWL
FoaSFWL : FoaEval {
	*ar { |in, size = 2048, method = \instant|
		var ws;

		ws = FoaWs.ar(in, size = 2048, method);

		^(10 * ws.log10)
	}
}


// FOA SFWhL
FoaSFWhL : FoaEval {
	*ar { |in, size = 2048, method = \instant|
		var wh;

		wh = FoaWh.ar(in, size = 2048, method);

		^(10 * wh.log10)
	}
}


// FOA SFIL
FoaSFIL : FoaEval {
	*ar { |in, size = 2048, method = \instant|
		var i;

		i = FoaMagI.ar(in, size = 2048, method);

		^(10 * i.log10)
	}
}




//------------------------------------------------------------------------
// FOA SOUNDFIELD INDICATORS

// FOA Active-Reactive Soundfield Balance Angle: Alpha
FoaAlpha : FoaEval {
	*ar { |in, size = 2048, method = \instant|
		var p, u;

		in = this.checkChans(in);
		p = 2.sqrt * in[0];  // w * 2.sqrt = pressure
		u = in[1..3];  // [x, y, z] = velocity (vector)

		case(
			{ method == \instant }, {
				var pReIm, pImRe, uReIm, ia, magIa, ir, magIr;

				pReIm = HilbertW.ar(p, size);
				pImRe = [1, -1] * pReIm.reverse;
				uReIm = HilbertW.ar(u, size);

				ia = uReIm.collect({ |item|
					(pReIm * item).sum
				});
				magIa = ia.squared.sum.sqrt;

				ir = uReIm.collect({ |item|
					(pImRe * item).sum
				});
				magIr = ir.squared.sum.sqrt

				^atan2(magIr, magIa + DC.ar(FoaEval.reg))
			},
			{ method == \average }, {
				var wp, wu, ia, magI_squared, magIa_squared, magIa, magIr;
				var normFac;

				normFac = 2 * size.reciprocal;

				wp = normFac * RunningSum.ar(p.squared, size);
				wu = normFac * RunningSum.ar(u.squared, size).sum;
				ia = normFac * RunningSum.ar(p * u, size);

				magI_squared = wp * wu;
				magIa_squared = ia.squared.sum;
				magIa = magIa_squared.sqrt;

				magIr = (magI_squared - magIa_squared).sqrt;

				^atan2(magIr, magIa + DC.ar(FoaEval.reg))
			}
		)
	}
}


// FOA Potential-Kinetic Soundfield Balance Angle: Beta
FoaBeta : FoaEval {
	*ar { |in, size = 2048, method = \instant|
		var p, u;

		in = this.checkChans(in);
		p = 2.sqrt * in[0];  // w * 2.sqrt = pressure
		u = in[1..3];  // [x, y, z] = velocity (vector)

		case(
			{ method == \instant }, {
				var wp, wu, wd, magI;

				wp = HilbertW.ar(p, size).squared.sum;
				wu = HilbertW.ar(u, size).sum({ |item|
					item.squared.sum
				});
				wd = (0.5 * (wp - wu));
				magI = (wp * wu).sqrt;

				^atan2(wd, magI + DC.ar(FoaEval.reg))
			},
			{ method == \average }, {
				var wp, wu, wd, magI;
				var normFac;

				normFac = 2 * size.reciprocal;

				wp = normFac * RunningSum.ar(p.squared, size);
				wu = normFac * RunningSum.ar(u.squared, size).sum;

				wd = (0.5 * (wp - wu));
				magI = (wp * wu).sqrt;

				^atan2(wd, magI + DC.ar(FoaEval.reg))
			}
		)
	}
}


// FOA Active-Reactive Vector Alignment Angle: Gamma
FoaGamma : FoaEval {
	*ar { |in, size = 2048, method = \instant|
		var p, u;

		in = this.checkChans(in);
		p = 2.sqrt * in[0];  // w * 2.sqrt = pressure
		u = in[1..3];  // [x, y, z] = velocity (vector)

		case(
			{ method == \instant }, {
				var pReIm, pImRe, uReIm, ia, magIa, ir, magIr, cosFac, sinFac;
				var reg, alpha, gateA, gateR;  // gate using -thresh

				pReIm = HilbertW.ar(p, size);
				pImRe = [1, -1] * pReIm.reverse;
				uReIm = HilbertW.ar(u, size);

				ia = uReIm.collect({ |item|
					(pReIm * item).sum
				});
				magIa = ia.squared.sum.sqrt;

				ir = uReIm.collect({ |item|
					(pImRe * item).sum
				});
				magIr = ir.squared.sum.sqrt;

				// GATE coefficients with respect to Alpha
				reg = DC.ar(FoaEval.reg);

				alpha = atan2(magIr, magIa + reg);  // active-reactive balance
				gateA = 1 - (alpha.thresh(FoaEval.gamAlpR) / (alpha + reg));
				gateR = alpha.thresh(FoaEval.gamAlpA) / (alpha + reg);

				ia = gateA * ia;
				magIa = gateA * magIa;
				ir = gateR * ir;
				magIr = gateR * magIr;

				cosFac = (ia * ir).sum;
				sinFac = ((magIa * magIr).squared - cosFac.squared).sqrt;

				^atan2(sinFac, cosFac + reg)
			},
			{ method == \average }, {
				// Consider re-writing this!!
				Error(format("FoaGamma.ar argument method = %, INVALID.", method)).throw;
			}
		)
	}
}


//--
// "Third party" indicators go here....



//------------------------------------------------------------------------
// FOA SOUNDFIELD INCIDENCE - vectors

// FOA Active Intensity Azimuth, Elevation
FoaThetaPhiA : FoaEval {
	*ar { |in, size = 2048, method = \instant|
		var p, u;

		in = this.checkChans(in);
		p = 2.sqrt * in[0];  // w * 2.sqrt = pressure
		u = in[1..3];  // [x, y, z] = velocity (vector)

		case(
			{ method == \instant }, {
				var pReIm, pImRe, uReIm, ia, magIa, ir, magIr, theta, phi;
				var reg, alpha, gateA;  // gate using -thresh

				pReIm = HilbertW.ar(p, size);
				pImRe = [1, -1] * pReIm.reverse;
				uReIm = HilbertW.ar(u, size);

				ia = uReIm.collect({ |item|
					(pReIm * item).sum
				});
				magIa = ia.squared.sum.sqrt;

				ir = uReIm.collect({ |item|
					(pImRe * item).sum
				});
				magIr = ir.squared.sum.sqrt;

				// GATE coefficients with respect to Alpha
				reg = DC.ar(FoaEval.reg);

				alpha = atan2(magIr, magIa + reg);  // active-reactive balance
				gateA = 1 - (alpha.thresh(FoaEval.gamAlpR) / (alpha + reg));

				ia = gateA * ia;

				theta = atan2(ia[1], ia[0] + reg);
				phi = atan2(ia[2], (ia[0].squared + ia[1].squared + reg).sqrt);

				^Array.with(theta, phi)
			},
			{ method == \average }, {
				var wp, wu, ia, magI_squared, magIa_squared, magIa, magIr, theta, phi;
				var reg, alpha, gateA;  // gate using -thresh
				var normFac;

				normFac = 2 * size.reciprocal;

				wp = normFac * RunningSum.ar(p.squared, size);
				wu = normFac * RunningSum.ar(u.squared, size).sum;
				ia = normFac * RunningSum.ar(p * u, size);

				magI_squared = wp * wu;
				magIa_squared = ia.squared.sum;
				magIa = magIa_squared.sqrt;

				magIr = (magI_squared - magIa_squared).sqrt;

				// GATE coefficients with respect to Alpha
				reg = DC.ar(FoaEval.reg);

				alpha = atan2(magIr, magIa + reg);  // active-reactive balance
				gateA = 1 - (alpha.thresh(FoaEval.gamAlpR) / (alpha + reg));

				ia = gateA * ia;

				theta = atan2(ia[1], ia[0] + reg);
				phi = atan2(ia[2], (ia[0].squared + ia[1].squared + reg).sqrt);

				^Array.with(theta, phi)
			}
		)
	}
}


// FOA Reactive Intensity Azimuth, Elevation
FoaThetaPhiR : FoaEval {
	*ar { |in, size = 2048, method = \instant|
		var p, u;

		in = this.checkChans(in);
		p = 2.sqrt * in[0];  // w * 2.sqrt = pressure
		u = in[1..3];  // [x, y, z] = velocity (vector)

		case(
			{ method == \instant }, {
				var pReIm, pImRe, uReIm, ia, magIa, ir, magIr, theta, phi;
				var reg, alpha, gateR;  // gate using -thresh

				pReIm = HilbertW.ar(p, size);
				pImRe = [1, -1] * pReIm.reverse;
				uReIm = HilbertW.ar(u, size);

				ia = uReIm.collect({ |item|
					(pReIm * item).sum
				});
				magIa = ia.squared.sum.sqrt;

				ir = uReIm.collect({ |item|
					(pImRe * item).sum
				});
				magIr = ir.squared.sum.sqrt;

				// GATE coefficients with respect to Alpha
				reg = DC.ar(FoaEval.reg);

				alpha = atan2(magIr, magIa + reg);  // active-reactive balance
				gateR = alpha.thresh(FoaEval.gamAlpA) / (alpha + reg);

				ir = gateR * ir;

				theta = atan2(ir[1], ir[0] + reg);
				phi = atan2(ir[2], (ir[0].squared + ir[1].squared + reg).sqrt);

				^Array.with(theta, phi)
			},
			{ method == \average }, {
				// Consider re-writing this!!
				Error(format("FoaThetaPhiR.ar argument method = %, INVALID.", method)).throw;
			}
		)
	}
}


//------------------------------------------------------------------------
// FOA INTENSITY - vectors

// FOA Active Intensity
FoaIa : FoaEval {
	*ar { |in, size = 2048, method = \instant|
		var p, u;

		in = this.checkChans(in);
		p = 2.sqrt * in[0];  // w * 2.sqrt = pressure
		u = in[1..3];  // [x, y, z] = velocity (vector)

		case(
			{ method == \instant }, {
				var pReIm, ia;

				pReIm = HilbertW.ar(p, size);

				ia = HilbertW.ar(u, size).collect({ |item|
					(pReIm * item).sum
				});

				^ia
			},
			{ method == \average }, {
				var ia;
				var normFac;

				normFac = 2 * size.reciprocal;

				ia = normFac * RunningSum.ar(p * u, size);

				^ia
			}
		)
	}
}


// FOA Reactive Intensity
FoaIr : FoaEval {
	*ar { |in, size = 2048, method = \instant|
		var p, u;

		in = this.checkChans(in);
		p = 2.sqrt * in[0];  // w * 2.sqrt = pressure
		u = in[1..3];  // [x, y, z] = velocity (vector)

		case(
			{ method == \instant }, {
				var pImRe, ir;

				pImRe = [1, -1] * HilbertW.ar(p, size).reverse;

				ir = HilbertW.ar(u, size).collect({ |item|
					(pImRe * item).sum
				});

				^ir
			},
			{ method == \average }, {
				// Consider re-writing this!!
				Error(format("FoaIr.ar argument method = %, INVALID.", method)).throw;
			}
		)
	}
}


// FOA Active Admittance
FoaAa : FoaEval {
	*ar { |in, size = 2048, method = \instant|
		var p, u;

		in = this.checkChans(in);
		p = 2.sqrt * in[0];  // w * 2.sqrt = pressure
		u = in[1..3];  // [x, y, z] = velocity (vector)

		case(
			{ method == \instant }, {
				var pReIm, wp, ia, aa;

				pReIm = HilbertW.ar(p, size);
				wp = pReIm.squared.sum;

				ia = HilbertW.ar(u, size).collect({ |item|
					(pReIm * item).sum
				});
				aa = ia / (wp + DC.ar(FoaEval.reg));

				^aa
			},
			{ method == \average }, {
				var wp, ia, aa;
				var normFac;

				normFac = 2 * size.reciprocal;

				wp = normFac * RunningSum.ar(p.squared, size);
				ia = normFac * RunningSum.ar(p * u, size);
				aa = ia / (wp + DC.ar(FoaEval.reg));

				^aa
			}
		)
	}
}


// FOA Reactive Admittance
FoaAr : FoaEval {
	*ar { |in, size = 2048, method = \instant|
		var p, u;

		in = this.checkChans(in);
		p = 2.sqrt * in[0];  // w * 2.sqrt = pressure
		u = in[1..3];  // [x, y, z] = velocity (vector)

		case(
			{ method == \instant }, {
				var pReIm, pImRe, wp, ir, ar;

				pReIm = HilbertW.ar(p, size);
				pImRe = [1, -1] * pReIm.reverse;

				wp = pReIm.squared.sum;

				ir = HilbertW.ar(u, size).collect({ |item|
					(pImRe * item).sum
				});

				ar = ir / (wp + DC.ar(FoaEval.reg));

				^ar
			},
			{ method == \average }, {
				// Consider re-writing this!!
				Error(format("FoaAr.ar argument method = %, INVALID.", method)).throw;
			}
		)
	}
}


// FOA Active Energy
FoaWa : FoaEval {
	*ar { |in, size = 2048, method = \instant|
		var p, u;

		in = this.checkChans(in);
		p = 2.sqrt * in[0];  // w * 2.sqrt = pressure
		u = in[1..3];  // [x, y, z] = velocity (vector)

		case(
			{ method == \instant }, {
				var pReIm, uReIm, wp, wu, ws, ia, wa;

				pReIm = HilbertW.ar(p, size);
				uReIm = HilbertW.ar(u, size);

				wp = pReIm.squared.sum;
				wu = uReIm.sum({ |item|
					item.squared.sum
				});
				ws = (0.5 * (wp + wu));

				ia = uReIm.collect({ |item|
					(pReIm * item).sum
				});
				wa = ia / (ws + DC.ar(FoaEval.reg));

				^wa
			},
			{ method == \average }, {
				var wp, wu, ws, ia, wa;
				var normFac;

				normFac = 2 * size.reciprocal;

				wp = normFac * RunningSum.ar(p.squared, size);
				wu = normFac * RunningSum.ar(u.squared, size).sum;
				ws = (0.5 * (wp + wu));

				ia = normFac * RunningSum.ar(p * u, size);
				wa = ia / (ws + DC.ar(FoaEval.reg));

				^wa
			}
		)
	}
}


// FOA Reactive Energy
FoaWr : FoaEval {
	*ar { |in, size = 2048, method = \instant|
		var p, u;

		in = this.checkChans(in);
		p = 2.sqrt * in[0];  // w * 2.sqrt = pressure
		u = in[1..3];  // [x, y, z] = velocity (vector)

		case(
			{ method == \instant }, {
				var pReIm, pImRe, uReIm, wp, wu, ws, ir, wr;

				pReIm = HilbertW.ar(p, size);
				pImRe = [1, -1] * pReIm.reverse;
				uReIm = HilbertW.ar(u, size);

				wp = pReIm.squared.sum;
				wu = uReIm.sum({ |item|
					item.squared.sum
				});
				ws = (0.5 * (wp + wu));

				ir = uReIm.collect({ |item|
					(pImRe * item).sum
				});
				wr = ir / (ws + DC.ar(FoaEval.reg));

				^wr
			},
			{ method == \average }, {
				// Consider re-writing this!!
				Error(format("FoaWr.ar argument method = %, INVALID.", method)).throw;
			}
		)
	}
}


// FOA Unit Normalized Active Intensity
FoaNa : FoaEval {
	*ar { |in, size = 2048, method = \instant|
		var p, u;

		in = this.checkChans(in);
		p = 2.sqrt * in[0];  // w * 2.sqrt = pressure
		u = in[1..3];  // [x, y, z] = velocity (vector)

		case(
			{ method == \instant }, {
				var pReIm, uReIm, wp, wu, magI, ia, na;

				pReIm = HilbertW.ar(p, size);
				uReIm = HilbertW.ar(u, size);

				wp = pReIm.squared.sum;
				wu = uReIm.sum({ |item|
					item.squared.sum
				});
				magI = (wp * wu).sqrt;

				ia = uReIm.collect({ |item|
					(pReIm * item).sum
				});
				na = ia / (magI + DC.ar(FoaEval.reg));

				^na
			},
			{ method == \average }, {
				var wp, wu, magI, ia, na;
				var normFac;

				normFac = 2 * size.reciprocal;

				wp = normFac * RunningSum.ar(p.squared, size);
				wu = normFac * RunningSum.ar(u.squared, size).sum;
				magI = (wp * wu).sqrt;

				ia = normFac * RunningSum.ar(p * u, size);
				na = ia / (magI + DC.ar(FoaEval.reg));

				^na
			}
		)
	}
}


// FOA Unit Normalized Reactive Intensity
FoaNr : FoaEval {
	*ar { |in, size = 2048, method = \instant|
		var p, u;

		in = this.checkChans(in);
		p = 2.sqrt * in[0];  // w * 2.sqrt = pressure
		u = in[1..3];  // [x, y, z] = velocity (vector)

		case(
			{ method == \instant }, {
				var pReIm, pImRe, uReIm, wp, wu, magI, ir, nr;

				pReIm = HilbertW.ar(p, size);
				pImRe = [1, -1] * pReIm.reverse;
				uReIm = HilbertW.ar(u, size);

				wp = pReIm.squared.sum;
				wu = uReIm.sum({ |item|
					item.squared.sum
				});
				magI = (wp * wu).sqrt;

				ir = uReIm.collect({ |item|
					(pImRe * item).sum
				});
				nr = ir / (magI + DC.ar(FoaEval.reg));

				^nr
			},
			{ method == \average }, {
				// Consider re-writing this!!
				Error(format("FoaNr.ar argument method = %, INVALID.", method)).throw;
			}
		)
	}
}


// TODO
//
// Reactive vectors into parallel and orthogonal
// Include (additional) soundfield indicators


//------------------------------------------------------------------------
// Analyzer: peudo-UGen wrapper
/*
argument key - see helpfile for reasonable values

*/

FoaAnalyze : FoaEval {
	*ar { |in, kind ... args|
		var argDict, argDefaults;
		var ugen;

		in = this.checkChans(in);

		switch(kind,

			\Wp, {

				ugen = FoaWp;
				argDefaults = [2048, \instant];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\size), argDict.at(\method)
				)
			},

			\Wu, {

				ugen = FoaWu;
				argDefaults = [2048, \instant];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\size), argDict.at(\method)
				)
			},

			\Ws, {

				ugen = FoaWs;
				argDefaults = [2048, \instant];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\size), argDict.at(\method)
				)
			},

			\Wd, {

				ugen = FoaWd;
				argDefaults = [2048, \instant];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\size), argDict.at(\method)
				)
			},

			\Wh, {

				ugen = FoaWh;
				argDefaults = [2048, \instant];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\size), argDict.at(\method)
				)
			},

			// \magI, {
			'||I||', {

				ugen = FoaMagI;
				argDefaults = [2048, \instant];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\size), argDict.at(\method)
				)
			},

			// \magIa, {
			'||Ia||', {

				ugen = FoaMagIa;
				argDefaults = [2048, \instant];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\size), argDict.at(\method)
				)
			},

			// \magIr, {
			'||Ir||', {

				ugen = FoaMagIr;
				argDefaults = [2048, \instant];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\size), argDict.at(\method)
				)
			},

			// \magA, {
			'||A||', {

				ugen = FoaMagA;
				argDefaults = [2048, \instant];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\size), argDict.at(\method)
				)
			},

			// \magAa, {
			'||Aa||', {

				ugen = FoaMagAa;
				argDefaults = [2048, \instant];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\size), argDict.at(\method)
				)
			},

			// \magAr, {
			'||Ar||', {

				ugen = FoaMagAr;
				argDefaults = [2048, \instant];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\size), argDict.at(\method)
				)
			},

			// \magW, {
			'||W||', {

				ugen = FoaMagW;
				argDefaults = [2048, \instant];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\size), argDict.at(\method)
				)
			},

			// \magWa, {
			'||Wa||', {

				ugen = FoaMagWa;
				argDefaults = [2048, \instant];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\size), argDict.at(\method)
				)
			},

			// \magWr, {
			'||Wr||', {

				ugen = FoaMagWr;
				argDefaults = [2048, \instant];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\size), argDict.at(\method)
				)
			},

			// \magNa, {
			'||Na||', {

				ugen = FoaMagNa;
				argDefaults = [2048, \instant];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\size), argDict.at(\method)
				)
			},

			// \magNr, {
			'||Nr||', {

				ugen = FoaMagNr;
				argDefaults = [2048, \instant];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\size), argDict.at(\method)
				)
			},

			\SFPL, {

				ugen = FoaSFPL;
				argDefaults = [2048, \instant];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\size), argDict.at(\method)
				)
			},

			\SFVL, {

				ugen = FoaSFVL;
				argDefaults = [2048, \instant];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\size), argDict.at(\method)
				)
			},

			\SFWL, {

				ugen = FoaSFWL;
				argDefaults = [2048, \instant];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\size), argDict.at(\method)
				)
			},

			\SFWhL, {

				ugen = FoaSFWhL;
				argDefaults = [2048, \instant];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\size), argDict.at(\method)
				)
			},

			\SFIL, {

				ugen = FoaSFIL;
				argDefaults = [2048, \instant];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\size), argDict.at(\method)
				)
			},

			\alpha, {

				ugen = FoaAlpha;
				argDefaults = [2048, \instant];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\size), argDict.at(\method)
				)
			},

			\beta, {

				ugen = FoaBeta;
				argDefaults = [2048, \instant];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\size), argDict.at(\method)
				)
			},

			\gamma, {

				ugen = FoaGamma;
				argDefaults = [2048, \instant];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\size), argDict.at(\method)
				)
			},

			\thetaPhia, {

				ugen = FoaThetaPhiA;
				argDefaults = [2048, \instant];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\size), argDict.at(\method)
				)
			},

			\thetaPhir, {

				ugen = FoaThetaPhiR;
				argDefaults = [2048, \instant];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\size), argDict.at(\method)
				)
			},

			\Ia, {

				ugen = FoaIa;
				argDefaults = [2048, \instant];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\size), argDict.at(\method)
				)
			},

			\Ir, {

				ugen = FoaIr;
				argDefaults = [2048, \instant];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\size), argDict.at(\method)
				)
			},

			\Aa, {

				ugen = FoaAa;
				argDefaults = [2048, \instant];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\size), argDict.at(\method)
				)
			},

			\Ar, {

				ugen = FoaAr;
				argDefaults = [2048, \instant];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\size), argDict.at(\method)
				)
			},

			\Wa, {

				ugen = FoaWa;
				argDefaults = [2048, \instant];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\size), argDict.at(\method)
				)
			},

			\Wr, {

				ugen = FoaWr;
				argDefaults = [2048, \instant];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\size), argDict.at(\method)
				)
			},

			\Na, {

				ugen = FoaNa;
				argDefaults = [2048, \instant];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\size), argDict.at(\method)
				)
			},

			\Nr, {

				ugen = FoaNr;
				argDefaults = [2048, \instant];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\size), argDict.at(\method)
				)
			},
		)
	}
}
