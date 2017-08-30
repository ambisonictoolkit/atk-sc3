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
//     Class: FoaEval
// 	Class: Foa[AnalysisName]
// 	Class: ...
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
		gamAlpR = (90.0 - 5.5).degrad;  // Reactive Alpha threshold, '' ''
	}

}


//------------------------------------------------------------------------
// FOA ENERGY - magnitudes

// FOA potential energy
FoaWp : FoaEval {
	*ar { arg in, size, method = 'instant';
		var p;
		in = this.checkChans(in);
		p = 2.sqrt * in[0];  // w * 2.sqrt = pressure

		case
		{ method == 'instant' } {
			^Mix.new(HilbertW.ar(p, size).squared)
		}
		{ method == 'average' } {
			var normFac;
			normFac = 2*size.reciprocal;

			^(normFac * RunningSum.ar(p.squared, size))
		}
	}
}


// FOA kinetic energy
FoaWu : FoaEval {
	*ar { arg in, size, method = 'instant';
		var u;
		in = this.checkChans(in);
		u = in[1..3];  // [x, y, z] = velocity (vector)

		case
		{ method == 'instant' } {

			^Mix.new(
				u.collect({ arg item;
					Mix.new(HilbertW.ar(item, size).squared)
				})
			)
		}
		{ method == 'average' } {
			var normFac;
			normFac = 2*size.reciprocal;

			^(normFac * Mix.new(RunningSum.ar(u.squared, size)))
		}
	}
}


// FOA potential & kinetic energy mean
FoaWs : FoaEval {
	*ar { arg in, size, method = 'instant';
		var p, u;
		in = this.checkChans(in);
		p = 2.sqrt * in[0];  // w * 2.sqrt = pressure
		u = in[1..3];  // [x, y, z] = velocity (vector)

		case
		{ method == 'instant' } {
			var wp, wu;
			wp = Mix.new(HilbertW.ar(p, size).squared);
			wu = Mix.new(
				u.collect({ arg item;
					Mix.new(HilbertW.ar(item, size).squared)
				})
			);

			^(0.5 * (wp + wu))
		}
		{ method == 'average' } {
			var wp, wu;
			var normFac;
			normFac = 2*size.reciprocal;

			wp = normFac * RunningSum.ar(p.squared, size);
			wu = normFac * Mix.new(RunningSum.ar(u.squared, size));

			^(0.5 * (wp + wu))
		}
	}
}


// FOA potential & kinetic energy difference
FoaWd : FoaEval {
	*ar { arg in, size, method = 'instant';
		var p, u;
		in = this.checkChans(in);
		p = 2.sqrt * in[0];  // w * 2.sqrt = pressure
		u = in[1..3];  // [x, y, z] = velocity (vector)

		case
		{ method == 'instant' } {
			var wp, wu;
			wp = Mix.new(HilbertW.ar(p, size).squared);
			wu = Mix.new(
				u.collect({ arg item;
					Mix.new(HilbertW.ar(item, size).squared)
				})
			);

			^(0.5 * (wp - wu))
		}
		{ method == 'average' } {
			var wp, wu;
			var normFac;
			normFac = 2*size.reciprocal;

			wp = normFac * RunningSum.ar(p.squared, size);
			wu = normFac * Mix.new(RunningSum.ar(u.squared, size));

			^(0.5 * (wp - wu))
		}
	}
}


// FOA Heyser energy density
FoaWh : FoaEval {
	*ar { arg in, size, method = 'instant';
		var p, u;
		in = this.checkChans(in);
		p = 2.sqrt * in[0];  // w * 2.sqrt = pressure
		u = in[1..3];  // [x, y, z] = velocity (vector)

		case
		{ method == 'instant' } {
			var pReIm, pImRe, uReIm, wp, wu, ws, ir, magIr;

			pReIm = HilbertW.ar(p, size);
			pImRe = [1, -1] * pReIm.reverse;
			uReIm = u.collect({ arg item;
				HilbertW.ar(item, size)
			});

			wp = Mix.new(pReIm.squared);
			wu = Mix.new(
				Mix.new(uReIm.squared)
			);
			ws = (0.5 * (wp + wu));

			ir = uReIm.collect({ arg item;
				Mix.new(pImRe * item)
			});
			magIr = Mix.ar(ir.squared).sqrt

			^(ws - magIr)
		}
		{ method == 'average' } {
			var wp, wu, ws, ia, magI_squared, magIa_squared, magIr;
			var normFac;
			normFac = 2*size.reciprocal;

			wp = normFac * RunningSum.ar(p.squared, size);
			wu = normFac * Mix.new(RunningSum.ar(u.squared, size));
			ia = normFac * RunningSum.ar(p * u, size);

			ws = (0.5 * (wp + wu));

			magI_squared = wp * wu;
			magIa_squared = Mix.ar(ia.squared);
			magIr = (magI_squared - magIa_squared).sqrt;

			^(ws - magIr)
		}
	}
}


//------------------------------------------------------------------------
// FOA INTENSITY - magnitudes

// FOA Magnitude Intensity
FoaMagI : FoaEval {
	*ar { arg in, size, method = 'instant';
		var p, u;
		in = this.checkChans(in);
		p = 2.sqrt * in[0];  // w * 2.sqrt = pressure
		u = in[1..3];  // [x, y, z] = velocity (vector)

		case
		{ method == 'instant' } {
			var wp, wu;
			wp = Mix.new(HilbertW.ar(p, size).squared);
			wu = Mix.new(
				u.collect({ arg item;
					Mix.new(HilbertW.ar(item, size).squared)
				})
			);

			^(wp * wu).sqrt
		}
		{ method == 'average' } {
			var wp, wu;
			var normFac;
			normFac = 2*size.reciprocal;

			wp = normFac * RunningSum.ar(p.squared, size);
			wu = normFac * Mix.new(RunningSum.ar(u.squared, size));

			^(wp * wu).sqrt
		}
	}
}


// FOA Magnitude Active Intensity
FoaMagIa : FoaEval {
	*ar { arg in, size, method = 'instant';
		var p, u;
		in = this.checkChans(in);
		p = 2.sqrt * in[0];  // w * 2.sqrt = pressure
		u = in[1..3];  // [x, y, z] = velocity (vector)

		case
		{ method == 'instant' } {
			var pReIm, ia;

			pReIm = HilbertW.ar(p, size);


			ia = u.collect({ arg item;
				Mix.new(pReIm * HilbertW.ar(item, size))
			})

			^Mix.ar(ia.squared).sqrt
		}
		{ method == 'average' } {
			var ia;
			var normFac;
			normFac = 2*size.reciprocal;

			ia = normFac * RunningSum.ar(p * u, size);

			^Mix.ar(ia.squared).sqrt
		}
	}
}


// FOA Magnitude Reactive Intensity
FoaMagIr : FoaEval {
	*ar { arg in, size, method = 'instant';
		var p, u;
		in = this.checkChans(in);
		p = 2.sqrt * in[0];  // w * 2.sqrt = pressure
		u = in[1..3];  // [x, y, z] = velocity (vector)

		case
		{ method == 'instant' } {
			var pImRe, ir;

			pImRe = [1, -1] * HilbertW.ar(p, size).reverse;


			ir = u.collect({ arg item;
				Mix.new(pImRe * HilbertW.ar(item, size))
			})

			^Mix.ar(ir.squared).sqrt
		}
		{ method == 'average' } {
			var wp, wu, ia, magI_squared, magIa_squared;
			var normFac;
			normFac = 2*size.reciprocal;

			wp = normFac * RunningSum.ar(p.squared, size);
			wu = normFac * Mix.new(RunningSum.ar(u.squared, size));
			ia = normFac * RunningSum.ar(p * u, size);

			magI_squared = wp * wu;
			magIa_squared = Mix.ar(ia.squared);

			^(magI_squared - magIa_squared).sqrt
		}
	}
}



// FOA Magnitude Admittance
FoaMagA : FoaEval {
	*ar { arg in, size, method = 'instant';
		var p, u;
		in = this.checkChans(in);
		p = 2.sqrt * in[0];  // w * 2.sqrt = pressure
		u = in[1..3];  // [x, y, z] = velocity (vector)

		case
		{ method == 'instant' } {
			var wp, wu, magI;
			wp = Mix.new(HilbertW.ar(p, size).squared);
			wu = Mix.new(
				u.collect({ arg item;
					Mix.new(HilbertW.ar(item, size).squared)
				})
			);

			magI = (wp * wu).sqrt;

			^(magI / (wp + DC.ar(FoaEval.reg)))
		}
		{ method == 'average' } {
			var wp, wu, magI;
			var normFac;
			normFac = 2*size.reciprocal;

			wp = normFac * RunningSum.ar(p.squared, size);
			wu = normFac * Mix.new(RunningSum.ar(u.squared, size));

			magI = (wp * wu).sqrt;

			^(magI / (wp + DC.ar(FoaEval.reg)))
		}
	}
}


// FOA Magnitude Active Admittance
FoaMagAa : FoaEval {
	*ar { arg in, size, method = 'instant';
		var p, u;
		in = this.checkChans(in);
		p = 2.sqrt * in[0];  // w * 2.sqrt = pressure
		u = in[1..3];  // [x, y, z] = velocity (vector)

		case
		{ method == 'instant' } {
			var pReIm, wp, ia, magIa;

			pReIm = HilbertW.ar(p, size);
			wp = Mix.new(pReIm.squared);


			ia = u.collect({ arg item;
				Mix.new(pReIm * HilbertW.ar(item, size))
			});
			magIa = Mix.ar(ia.squared).sqrt;

			^(magIa / (wp + DC.ar(FoaEval.reg)))
		}
		{ method == 'average' } {
			var wp, ia, magIa;
			var normFac;
			normFac = 2*size.reciprocal;

			wp = normFac * RunningSum.ar(p.squared, size);
			ia = normFac * RunningSum.ar(p * u, size);
			magIa = Mix.ar(ia.squared).sqrt;

			^(magIa / (wp + DC.ar(FoaEval.reg)))
		}
	}
}


// FOA Magnitude Reactive Admittance
FoaMagAr : FoaEval {
	*ar { arg in, size, method = 'instant';
		var p, u;
		in = this.checkChans(in);
		p = 2.sqrt * in[0];  // w * 2.sqrt = pressure
		u = in[1..3];  // [x, y, z] = velocity (vector)

		case
		{ method == 'instant' } {
			var pReIm, pImRe, wp, ir, magIr;

			pReIm = HilbertW.ar(p, size);
			pImRe = [1, -1] * pReIm.reverse;

			wp = Mix.new(pReIm.squared);


			ir = u.collect({ arg item;
				Mix.new(pImRe * HilbertW.ar(item, size))
			});

			magIr = Mix.ar(ir.squared).sqrt;

			^(magIr / (wp + DC.ar(FoaEval.reg)))
		}
		{ method == 'average' } {
			var wp, wu, ia, magI_squared, magIa_squared, magIr;
			var normFac;
			normFac = 2*size.reciprocal;

			wp = normFac * RunningSum.ar(p.squared, size);
			wu = normFac * Mix.new(RunningSum.ar(u.squared, size));
			ia = normFac * RunningSum.ar(p * u, size);

			magI_squared = wp * wu;
			magIa_squared = Mix.ar(ia.squared);
			magIr = (magI_squared - magIa_squared).sqrt;

			^(magIr / (wp + DC.ar(FoaEval.reg)))
		}
	}
}


// FOA Magnitude Energy
FoaMagW : FoaEval {
	*ar { arg in, size, method = 'instant';
		var p, u;
		in = this.checkChans(in);
		p = 2.sqrt * in[0];  // w * 2.sqrt = pressure
		u = in[1..3];  // [x, y, z] = velocity (vector)

		case
		{ method == 'instant' } {
			var wp, wu, ws, magI;
			wp = Mix.new(HilbertW.ar(p, size).squared);
			wu = Mix.new(
				u.collect({ arg item;
					Mix.new(HilbertW.ar(item, size).squared)
				})
			);
			ws = (0.5 * (wp + wu));

			magI = (wp * wu).sqrt;

			^(magI / (ws + DC.ar(FoaEval.reg)))
		}
		{ method == 'average' } {
			var wp, wu, ws, magI;
			var normFac;
			normFac = 2*size.reciprocal;

			wp = normFac * RunningSum.ar(p.squared, size);
			wu = normFac * Mix.new(RunningSum.ar(u.squared, size));
			ws = (0.5 * (wp + wu));

			magI = (wp * wu).sqrt;

			^(magI / (ws + DC.ar(FoaEval.reg)))
		}
	}
}


// FOA Magnitude Active Energy
FoaMagWa : FoaEval {
	*ar { arg in, size, method = 'instant';
		var p, u;
		in = this.checkChans(in);
		p = 2.sqrt * in[0];  // w * 2.sqrt = pressure
		u = in[1..3];  // [x, y, z] = velocity (vector)

		case
		{ method == 'instant' } {
			var pReIm, wp, wu, ws, ia, magIa;

			pReIm = HilbertW.ar(p, size);
			wp = Mix.new(pReIm.squared);
			wu = Mix.new(
				u.collect({ arg item;
					Mix.new(HilbertW.ar(item, size).squared)
				})
			);
			ws = (0.5 * (wp + wu));


			ia = u.collect({ arg item;
				Mix.new(pReIm * HilbertW.ar(item, size))
			});
			magIa = Mix.ar(ia.squared).sqrt;

			^(magIa / (ws + DC.ar(FoaEval.reg)))
		}
		{ method == 'average' } {
			var wp, wu, ws, ia, magIa;
			var normFac;
			normFac = 2*size.reciprocal;

			wp = normFac * RunningSum.ar(p.squared, size);
			wu = normFac * Mix.new(RunningSum.ar(u.squared, size));
			ws = (0.5 * (wp + wu));

			ia = normFac * RunningSum.ar(p * u, size);
			magIa = Mix.ar(ia.squared).sqrt;

			^(magIa / (ws + DC.ar(FoaEval.reg)))
		}
	}
}


// FOA Magnitude Reactive Energy
FoaMagWr : FoaEval {
	*ar { arg in, size, method = 'instant';
		var p, u;
		in = this.checkChans(in);
		p = 2.sqrt * in[0];  // w * 2.sqrt = pressure
		u = in[1..3];  // [x, y, z] = velocity (vector)

		case
		{ method == 'instant' } {
			var pReIm, pImRe, wp, wu, ws, ir, magIr;

			pReIm = HilbertW.ar(p, size);
			pImRe = [1, -1] * pReIm.reverse;

			wp = Mix.new(pReIm.squared);
			wu = Mix.new(
				u.collect({ arg item;
					Mix.new(HilbertW.ar(item, size).squared)
				})
			);
			ws = (0.5 * (wp + wu));


			ir = u.collect({ arg item;
				Mix.new(pImRe * HilbertW.ar(item, size))
			});

			magIr = Mix.ar(ir.squared).sqrt;

			^(magIr / (ws + DC.ar(FoaEval.reg)))
		}
		{ method == 'average' } {
			var wp, wu, ws, ia, magI_squared, magIa_squared, magIr;
			var normFac;
			normFac = 2*size.reciprocal;

			wp = normFac * RunningSum.ar(p.squared, size);
			wu = normFac * Mix.new(RunningSum.ar(u.squared, size));
			ws = (0.5 * (wp + wu));
			ia = normFac * RunningSum.ar(p * u, size);

			magI_squared = wp * wu;
			magIa_squared = Mix.ar(ia.squared);
			magIr = (magI_squared - magIa_squared).sqrt;

			^(magIr / (ws + DC.ar(FoaEval.reg)))
		}
	}
}


// FOA Magnitude Unit Normalized Active Intensity
FoaMagNa : FoaEval {
	*ar { arg in, size, method = 'instant';
		var p, u;
		in = this.checkChans(in);
		p = 2.sqrt * in[0];  // w * 2.sqrt = pressure
		u = in[1..3];  // [x, y, z] = velocity (vector)

		case
		{ method == 'instant' } {
			var pReIm, wp, wu, magI, ia, magIa;

			pReIm = HilbertW.ar(p, size);
			wp = Mix.new(pReIm.squared);
			wu = Mix.new(
				u.collect({ arg item;
					Mix.new(HilbertW.ar(item, size).squared)
				})
			);
			magI = (wp * wu).sqrt;


			ia = u.collect({ arg item;
				Mix.new(pReIm * HilbertW.ar(item, size))
			});
			magIa = Mix.ar(ia.squared).sqrt;

			^(magIa / (magI + DC.ar(FoaEval.reg)))
		}
		{ method == 'average' } {
			var wp, wu, magI, ia, magIa;
			var normFac;
			normFac = 2*size.reciprocal;

			wp = normFac * RunningSum.ar(p.squared, size);
			wu = normFac * Mix.new(RunningSum.ar(u.squared, size));
			magI = (wp * wu).sqrt;

			ia = normFac * RunningSum.ar(p * u, size);
			magIa = Mix.ar(ia.squared).sqrt;

			^(magIa / (magI + DC.ar(FoaEval.reg)))
		}
	}
}


// FOA Magnitude Unit Normalized Reactive Intensity
FoaMagNr : FoaEval {
	*ar { arg in, size, method = 'instant';
		var p, u;
		in = this.checkChans(in);
		p = 2.sqrt * in[0];  // w * 2.sqrt = pressure
		u = in[1..3];  // [x, y, z] = velocity (vector)

		case
		{ method == 'instant' } {
			var pReIm, pImRe, wp, wu, magI, ir, magIr;

			pReIm = HilbertW.ar(p, size);
			pImRe = [1, -1] * pReIm.reverse;

			wp = Mix.new(pReIm.squared);
			wu = Mix.new(
				u.collect({ arg item;
					Mix.new(HilbertW.ar(item, size).squared)
				})
			);
			magI = (wp * wu).sqrt;


			ir = u.collect({ arg item;
				Mix.new(pImRe * HilbertW.ar(item, size))
			});

			magIr = Mix.ar(ir.squared).sqrt;

			^(magIr / (magI + DC.ar(FoaEval.reg)))
		}
		{ method == 'average' } {
			var wp, wu, magI, ia, magI_squared, magIa_squared, magIr;
			var normFac;
			normFac = 2*size.reciprocal;

			wp = normFac * RunningSum.ar(p.squared, size);
			wu = normFac * Mix.new(RunningSum.ar(u.squared, size));
			magI = (wp * wu).sqrt;
			ia = normFac * RunningSum.ar(p * u, size);

			magI_squared = wp * wu;
			magIa_squared = Mix.ar(ia.squared);
			magIr = (magI_squared - magIa_squared).sqrt;

			^(magIr / (magI + DC.ar(FoaEval.reg)))
		}
	}
}


//------------------------------------------------------------------------
// FOA SOUNDFIELD LEVELS - magnitudes in dB

// FOA SFPL
FoaSFPL : FoaEval {
	*ar { arg in, size, method = 'instant';
		var wp;

		wp = FoaWp.ar(in, size, method);

		^(10 * wp.log10)
	}
}


// FOA SFVL
FoaSFVL : FoaEval {
	*ar { arg in, size, method = 'instant';
		var wu;

		wu = FoaWu.ar(in, size, method);

		^(10 * wu.log10)
	}
}


// FOA SFWL
FoaSFWL : FoaEval {
	*ar { arg in, size, method = 'instant';
		var ws;

		ws = FoaWs.ar(in, size, method);

		^(10 * ws.log10)
	}
}


// FOA SFWhL
FoaSFWhL : FoaEval {
	*ar { arg in, size, method = 'instant';
		var wh;

		wh = FoaWh.ar(in, size, method);

		^(10 * wh.log10)
	}
}


// FOA SFIL
FoaSFIL : FoaEval {
	*ar { arg in, size, method = 'instant';
		var i;

		i = FoaMagI.ar(in, size, method);

		^(10 * i.log10)
	}
}




//------------------------------------------------------------------------
// FOA SOUNDFIELD INDICATORS

// FOA Active-Reactive Soundfield Balance Angle: Alpha
FoaAlpha : FoaEval {
	*ar { arg in, size, method = 'instant';
		var p, u;
		in = this.checkChans(in);
		p = 2.sqrt * in[0];  // w * 2.sqrt = pressure
		u = in[1..3];  // [x, y, z] = velocity (vector)

		case
		{ method == 'instant' } {
			var pReIm, pImRe, uReIm, ia, magIa, ir, magIr;

			pReIm = HilbertW.ar(p, size);
			pImRe = [1, -1] * pReIm.reverse;
			uReIm = u.collect({ arg item;
				HilbertW.ar(item, size)
			});

			ia = uReIm.collect({ arg item;
				Mix.new(pReIm * item)
			});
			magIa = Mix.ar(ia.squared).sqrt;

			ir = uReIm.collect({ arg item;
				Mix.new(pImRe * item)
			});
			magIr = Mix.ar(ir.squared).sqrt

			^atan2(magIr, magIa + DC.ar(FoaEval.reg))
		}
		{ method == 'average' } {
			var wp, wu, ia, magI_squared, magIa_squared, magIa, magIr;
			var normFac;
			normFac = 2*size.reciprocal;

			wp = normFac * RunningSum.ar(p.squared, size);
			wu = normFac * Mix.new(RunningSum.ar(u.squared, size));
			ia = normFac * RunningSum.ar(p * u, size);

			magI_squared = wp * wu;
			magIa_squared = Mix.ar(ia.squared);
			magIa = magIa_squared.sqrt;

			magIr = (magI_squared - magIa_squared).sqrt;

			^atan2(magIr, magIa + DC.ar(FoaEval.reg))
		}
	}
}


// FOA Potential-Kinetic Soundfield Balance Angle: Beta
FoaBeta : FoaEval {
	*ar { arg in, size, method = 'instant';
		var p, u;
		in = this.checkChans(in);
		p = 2.sqrt * in[0];  // w * 2.sqrt = pressure
		u = in[1..3];  // [x, y, z] = velocity (vector)

		case
		{ method == 'instant' } {
			var wp, wu, wd, magI;

			wp = Mix.new(HilbertW.ar(p, size).squared);
			wu = Mix.new(
				u.collect({ arg item;
					Mix.new(HilbertW.ar(item, size).squared)
				})
			);
			wd = (0.5 * (wp - wu));
			magI = (wp * wu).sqrt;

			^atan2(wd, magI + DC.ar(FoaEval.reg))
		}
		{ method == 'average' } {
			var wp, wu, wd, magI;
			var normFac;
			normFac = 2*size.reciprocal;

			wp = normFac * RunningSum.ar(p.squared, size);
			wu = normFac * Mix.new(RunningSum.ar(u.squared, size));

			wd = (0.5 * (wp - wu));
			magI = (wp * wu).sqrt;

			^atan2(wd, magI + DC.ar(FoaEval.reg))
		}
	}
}


// FOA Active-Reactive Vector Alignment Angle: Gamma
FoaGamma : FoaEval {
	*ar { arg in, size, method = 'instant';
		var p, u;
		in = this.checkChans(in);
		p = 2.sqrt * in[0];  // w * 2.sqrt = pressure
		u = in[1..3];  // [x, y, z] = velocity (vector)

		case
		{ method == 'instant' } {
			var pReIm, pImRe, uReIm, ia, magIa, ir, magIr, cosFac, sinFac;
			var reg, alpha, gateA, gateR;  // gate using -thresh

			pReIm = HilbertW.ar(p, size);
			pImRe = [1, -1] * pReIm.reverse;
			uReIm = u.collect({ arg item;
				HilbertW.ar(item, size)
			});

			ia = uReIm.collect({ arg item;
				Mix.new(pReIm * item)
			});
			magIa = Mix.ar(ia.squared).sqrt;

			ir = uReIm.collect({ arg item;
				Mix.new(pImRe * item)
			});
			magIr = Mix.ar(ir.squared).sqrt;

			// GATE coefficients with respect to Alpha
			reg = DC.ar(FoaEval.reg);

			alpha = atan2(magIr, magIa + reg);  // active-reactive balance
			gateA = 1 - (alpha.thresh(FoaEval.gamAlpR) / (alpha + reg));
			gateR = alpha.thresh(FoaEval.gamAlpA) / (alpha + reg);

			ia = gateA * ia;
			magIa = gateA * magIa;
			ir = gateR * ir;
			magIr = gateR * magIr;

			cosFac = Mix.new(ia * ir);
			sinFac = ((magIa * magIr).squared - cosFac.squared).sqrt;

			^atan2(sinFac, cosFac + reg)
		}
		{ method == 'average' } {
			// Consider re-writing this!!
			Error(format("FoaGamma.ar argument method = %, INVALID.", method)).throw;
		}
	}
}


//--
// "Third party" indicators go here....



//------------------------------------------------------------------------
// FOA SOUNDFIELD INCIDENCE - vectors

// FOA Active Intensity Azimuth, Elevation
FoaAzEla : FoaEval {
	*ar { arg in, size, method = 'instant';
		var p, u;
		in = this.checkChans(in);
		p = 2.sqrt * in[0];  // w * 2.sqrt = pressure
		u = in[1..3];  // [x, y, z] = velocity (vector)

		case
		{ method == 'instant' } {
			var pReIm, pImRe, uReIm, ia, magIa, ir, magIr, az, el;
			var reg, alpha, gateA;  // gate using -thresh

			pReIm = HilbertW.ar(p, size);
			pImRe = [1, -1] * pReIm.reverse;
			uReIm = u.collect({ arg item;
				HilbertW.ar(item, size)
			});

			ia = uReIm.collect({ arg item;
				Mix.new(pReIm * item)
			});
			magIa = Mix.ar(ia.squared).sqrt;

			ir = uReIm.collect({ arg item;
				Mix.new(pImRe * item)
			});
			magIr = Mix.ar(ir.squared).sqrt;

			// GATE coefficients with respect to Alpha
			reg = DC.ar(FoaEval.reg);

			alpha = atan2(magIr, magIa + reg);  // active-reactive balance
			gateA = 1 - (alpha.thresh(FoaEval.gamAlpR) / (alpha + reg));

			ia = gateA * ia;

			az = atan2(ia.at(1), ia.at(0) + reg);
			el = atan2(ia.at(2), (ia.at(0).squared + ia.at(1).squared + reg).sqrt);

			^Array.with(az, el)
		}
		{ method == 'average' } {
			var wp, wu, ia, magI_squared, magIa_squared, magIa, magIr, az, el;
			var reg, alpha, gateA;  // gate using -thresh
			var normFac;
			normFac = 2*size.reciprocal;

			wp = normFac * RunningSum.ar(p.squared, size);
			wu = normFac * Mix.new(RunningSum.ar(u.squared, size));
			ia = normFac * RunningSum.ar(p * u, size);

			magI_squared = wp * wu;
			magIa_squared = Mix.ar(ia.squared);
			magIa = magIa_squared.sqrt;

			magIr = (magI_squared - magIa_squared).sqrt;

			// GATE coefficients with respect to Alpha
			reg = DC.ar(FoaEval.reg);

			alpha = atan2(magIr, magIa + reg);  // active-reactive balance
			gateA = 1 - (alpha.thresh(FoaEval.gamAlpR) / (alpha + reg));

			ia = gateA * ia;

			az = atan2(ia.at(1), ia.at(0) + reg);
			el = atan2(ia.at(2), (ia.at(0).squared + ia.at(1).squared + reg).sqrt);

			^Array.with(az, el)
		}
	}
}


// FOA Reactive Intensity Azimuth, Elevation
FoaAzElr : FoaEval {
	*ar { arg in, size, method = 'instant';
		var p, u;
		in = this.checkChans(in);
		p = 2.sqrt * in[0];  // w * 2.sqrt = pressure
		u = in[1..3];  // [x, y, z] = velocity (vector)

		case
		{ method == 'instant' } {
			var pReIm, pImRe, uReIm, ia, magIa, ir, magIr, az, el;
			var reg, alpha, gateR;  // gate using -thresh

			pReIm = HilbertW.ar(p, size);
			pImRe = [1, -1] * pReIm.reverse;
			uReIm = u.collect({ arg item;
				HilbertW.ar(item, size)
			});

			ia = uReIm.collect({ arg item;
				Mix.new(pReIm * item)
			});
			magIa = Mix.ar(ia.squared).sqrt;

			ir = uReIm.collect({ arg item;
				Mix.new(pImRe * item)
			});
			magIr = Mix.ar(ir.squared).sqrt;

			// GATE coefficients with respect to Alpha
			reg = DC.ar(FoaEval.reg);

			alpha = atan2(magIr, magIa + reg);  // active-reactive balance
			gateR = alpha.thresh(FoaEval.gamAlpA) / (alpha + reg);

			ir = gateR * ir;

			az = atan2(ir.at(1), ir.at(0) + reg);
			el = atan2(ir.at(2), (ir.at(0).squared + ir.at(1).squared + reg).sqrt);

			^Array.with(az, el)
		}
		{ method == 'average' } {
			// Consider re-writing this!!
			Error(format("FoaAzElr.ar argument method = %, INVALID.", method)).throw;
		}
	}
}


//------------------------------------------------------------------------
// FOA INTENSITY - vectors

// FOA Active Intensity
FoaIa : FoaEval {
	*ar { arg in, size, method = 'instant';
		var p, u;
		in = this.checkChans(in);
		p = 2.sqrt * in[0];  // w * 2.sqrt = pressure
		u = in[1..3];  // [x, y, z] = velocity (vector)

		case
		{ method == 'instant' } {
			var pReIm, ia;

			pReIm = HilbertW.ar(p, size);

			ia = u.collect({ arg item;
				Mix.new(pReIm * HilbertW.ar(item, size))
			})

			^ia
		}
		{ method == 'average' } {
			var ia;
			var normFac;
			normFac = 2*size.reciprocal;

			ia = normFac * RunningSum.ar(p * u, size);

			^ia
		}
	}
}


// FOA Reactive Intensity
FoaIr : FoaEval {
	*ar { arg in, size, method = 'instant';
		var p, u;
		in = this.checkChans(in);
		p = 2.sqrt * in[0];  // w * 2.sqrt = pressure
		u = in[1..3];  // [x, y, z] = velocity (vector)

		case
		{ method == 'instant' } {
			var pImRe, ir;

			pImRe = [1, -1] * HilbertW.ar(p, size).reverse;

			ir = u.collect({ arg item;
				Mix.new(pImRe * HilbertW.ar(item, size))
			})

			^ir
		}
		{ method == 'average' } {
			// Consider re-writing this!!
			Error(format("FoaIr.ar argument method = %, INVALID.", method)).throw;
		}
	}
}


// FOA Active Admittance
FoaAa : FoaEval {
	*ar { arg in, size, method = 'instant';
		var p, u;
		in = this.checkChans(in);
		p = 2.sqrt * in[0];  // w * 2.sqrt = pressure
		u = in[1..3];  // [x, y, z] = velocity (vector)

		case
		{ method == 'instant' } {
			var pReIm, wp, ia, aa;

			pReIm = HilbertW.ar(p, size);
			wp = Mix.new(pReIm.squared);

			ia = u.collect({ arg item;
				Mix.new(pReIm * HilbertW.ar(item, size))
			});
			aa = ia / (wp + DC.ar(FoaEval.reg));

			^aa
		}
		{ method == 'average' } {
			var wp, ia, aa;
			var normFac;
			normFac = 2*size.reciprocal;

			wp = normFac * RunningSum.ar(p.squared, size);
			ia = normFac * RunningSum.ar(p * u, size);
			aa = ia / (wp + DC.ar(FoaEval.reg));

			^aa
		}
	}
}


// FOA Magnitude Reactive Admittance
FoaAr : FoaEval {
	*ar { arg in, size, method = 'instant';
		var p, u;
		in = this.checkChans(in);
		p = 2.sqrt * in[0];  // w * 2.sqrt = pressure
		u = in[1..3];  // [x, y, z] = velocity (vector)

		case
		{ method == 'instant' } {
			var pReIm, pImRe, wp, ir, ar;

			pReIm = HilbertW.ar(p, size);
			pImRe = [1, -1] * pReIm.reverse;

			wp = Mix.new(pReIm.squared);

			ir = u.collect({ arg item;
				Mix.new(pImRe * HilbertW.ar(item, size))
			});

			ar = ir / (wp + DC.ar(FoaEval.reg));

			^ar
		}
		{ method == 'average' } {
			// Consider re-writing this!!
			Error(format("FoaAr.ar argument method = %, INVALID.", method)).throw;
		}
	}
}


// FOA Active Energy
FoaWa : FoaEval {
	*ar { arg in, size, method = 'instant';
		var p, u;
		in = this.checkChans(in);
		p = 2.sqrt * in[0];  // w * 2.sqrt = pressure
		u = in[1..3];  // [x, y, z] = velocity (vector)

		case
		{ method == 'instant' } {
			var pReIm, wp, wu, ws, ia, wa;

			pReIm = HilbertW.ar(p, size);
			wp = Mix.new(pReIm.squared);
			wu = Mix.new(
				u.collect({ arg item;
					Mix.new(HilbertW.ar(item, size).squared)
				})
			);
			ws = (0.5 * (wp + wu));


			ia = u.collect({ arg item;
				Mix.new(pReIm * HilbertW.ar(item, size))
			});
			wa = ia / (ws + DC.ar(FoaEval.reg));

			^wa
		}
		{ method == 'average' } {
			var wp, wu, ws, ia, wa;
			var normFac;
			normFac = 2*size.reciprocal;

			wp = normFac * RunningSum.ar(p.squared, size);
			wu = normFac * Mix.new(RunningSum.ar(u.squared, size));
			ws = (0.5 * (wp + wu));

			ia = normFac * RunningSum.ar(p * u, size);
			wa = ia / (ws + DC.ar(FoaEval.reg));

			^wa
		}
	}
}


// FOA Reactive Energy
FoaWr : FoaEval {
	*ar { arg in, size, method = 'instant';
		var p, u;
		in = this.checkChans(in);
		p = 2.sqrt * in[0];  // w * 2.sqrt = pressure
		u = in[1..3];  // [x, y, z] = velocity (vector)

		case
		{ method == 'instant' } {
			var pReIm, pImRe, wp, wu, ws, ir, wr;

			pReIm = HilbertW.ar(p, size);
			pImRe = [1, -1] * pReIm.reverse;

			wp = Mix.new(pReIm.squared);
			wu = Mix.new(
				u.collect({ arg item;
					Mix.new(HilbertW.ar(item, size).squared)
				})
			);
			ws = (0.5 * (wp + wu));


			ir = u.collect({ arg item;
				Mix.new(pImRe * HilbertW.ar(item, size))
			});
			wr = ir / (ws + DC.ar(FoaEval.reg));

			^wr
		}
		{ method == 'average' } {
			// Consider re-writing this!!
			Error(format("FoaWr.ar argument method = %, INVALID.", method)).throw;
		}
	}
}


// FOA Magnitude Unit Normalized Active Intensity
FoaNa : FoaEval {
	*ar { arg in, size, method = 'instant';
		var p, u;
		in = this.checkChans(in);
		p = 2.sqrt * in[0];  // w * 2.sqrt = pressure
		u = in[1..3];  // [x, y, z] = velocity (vector)

		case
		{ method == 'instant' } {
			var pReIm, wp, wu, magI, ia, na;

			pReIm = HilbertW.ar(p, size);
			wp = Mix.new(pReIm.squared);
			wu = Mix.new(
				u.collect({ arg item;
					Mix.new(HilbertW.ar(item, size).squared)
				})
			);
			magI = (wp * wu).sqrt;


			ia = u.collect({ arg item;
				Mix.new(pReIm * HilbertW.ar(item, size))
			});
			na = ia / (magI + DC.ar(FoaEval.reg));

			^na
		}
		{ method == 'average' } {
			var wp, wu, magI, ia, na;
			var normFac;
			normFac = 2*size.reciprocal;

			wp = normFac * RunningSum.ar(p.squared, size);
			wu = normFac * Mix.new(RunningSum.ar(u.squared, size));
			magI = (wp * wu).sqrt;

			ia = normFac * RunningSum.ar(p * u, size);
			na = ia / (magI + DC.ar(FoaEval.reg));

			^na
		}
	}
}


// FOA Magnitude Unit Normalized Reactive Intensity
FoaNr : FoaEval {
	*ar { arg in, size, method = 'instant';
		var p, u;
		in = this.checkChans(in);
		p = 2.sqrt * in[0];  // w * 2.sqrt = pressure
		u = in[1..3];  // [x, y, z] = velocity (vector)

		case
		{ method == 'instant' } {
			var pReIm, pImRe, wp, wu, magI, ir, nr;

			pReIm = HilbertW.ar(p, size);
			pImRe = [1, -1] * pReIm.reverse;

			wp = Mix.new(pReIm.squared);
			wu = Mix.new(
				u.collect({ arg item;
					Mix.new(HilbertW.ar(item, size).squared)
				})
			);
			magI = (wp * wu).sqrt;


			ir = u.collect({ arg item;
				Mix.new(pImRe * HilbertW.ar(item, size))
			});
			nr = ir / (magI + DC.ar(FoaEval.reg));

			^nr
		}
		{ method == 'average' } {
			// Consider re-writing this!!
			Error(format("FoaNr.ar argument method = %, INVALID.", method)).throw;
		}
	}
}


// TODO
//
// Reactive vectors into parallel and orthogonal
// Include (additional) soundfield indicators