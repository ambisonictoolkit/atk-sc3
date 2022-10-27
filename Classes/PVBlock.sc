/*
/	FrameBlock: Utilities for 2-D arrays, viewed as blocks of frames
/	(e.g. component vectors organized into "rows" of time or frequency)
/	where dim 1 is the frame index, dim 2 is the component index.
/	E.g. IntensityBlock : a block of cartesian component frames, shape: [ numFrames, 3 ]
/		 PVBlock        : a block of pressure-velocity component frames, shape: [ numFrames, 4 ]
/
/	There could be an optimization by inheriting Array2D instead
/ 	(provides an interface for a flat array as a "2D array"), furthermore
/	stored with columns as frames, for faster iteration through frames.
/	Not sure if Array2D is a feature-complete implementation and fully
/	compatible with all methods of its superclass, Collection.
*/
FrameBlock[slot] : Array {

	numFrames     { ^this.size     } // shape[0]
	numComponents { ^this.shape[1] }

	// Synonymous vector magnitudes.
	// Calculates vector magnitude of the frame components ("rows").
	vmag   { ^this.collect(_.vmag)   }
	l2norm { ^this.collect(_.l2norm) }

	// Weighted mean of each component across frames/"columns" of the block
	prwmeanCol { |dataBlock, weights|
		^dataBlock.flop.collect(_.wmean(weights))
	}

	/* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	/  Posting  */

	// Standard return
	printOn { |stream|
		if (stream.atLimit) { ^this };
		stream << this.class.name << "[ ";
		this.printItemsOn(stream);
		stream << " ]";
	}
	// Nicer format for posting full block on demand
	post {
		"%[ (size: %)\n".postf(this.class.name, this.size);
		this.do{ |frm, i| "\t".post; frm.postln };
		"  ]".post;
	}
	postln { this.post; "".postln; }
}


PVBlock[slot] : FrameBlock {

	*newClear { |blockSize|
		^super.fill(blockSize, { PVFrame.newClear })
	}

	*newRand { |blockSize|
		^super.fill(blockSize, { PVFrame.newRand })
	}

	// Construct from a 2D array, shape: [nFrames, 4]		TODO: check dimensions, now assumes nFrm x 4
	*newFromPVArray { |pvArray|
		var blockSize = pvArray.shape[0];

		^super.fill(blockSize, { |i|
			PVFrame.newFromArray(pvArray[i])
		})
	}

	// Construct from an array of PVFrames
	*withPVFrames { |pvFrames|
		^super.fill(pvFrames.size, { |i| pvFrames[i] })
	}

	/* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	/  Component access	 */

	// Note - collecting is faster than, e.g., `.performUnaryOp('p')`
	pressure { ^this.collect(_[0]) }
	p	     { ^this.collect(_[0]) }
	velocity { ^this.collect(_[1..3]) }
	v	     { ^this.collect(_[1..3]) }

	/* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	/  Framewise quantities
	/
	/  Note - Dispatching with with `performUnaryOp` seems to speed
	/  up calculation of aggregate quantities. */

	wpot 	{ ^this.performUnaryOp('wpot') }
	wp      { ^this.performUnaryOp('wpot') }

	wkin 	{ ^this.performUnaryOp('wkin') }
	wv      { ^this.performUnaryOp('wkin') } 	// wu? wk?

	wpkmean { ^this.performUnaryOp('wpkmean') }
	ws      { ^this.performUnaryOp('wpkmean') }	// wm?

	wpkdiff { ^this.performUnaryOp('wpkdiff') }
	wd      { ^this.performUnaryOp('wpkdiff') }

	wdens 	{ ^this.performUnaryOp('wdens') }
	wh      { ^this.performUnaryOp('wdens') }

	/* Magnitudes
	/
	/  Note - these are largely for convenience, and will
	/  calculate the intensity/admittance for each frame
	/  on each method call. For multiple calls, call on
	/  IntensityBlock directly to avoid recalculations. */

	// Intensity
	magI      { ^this.performUnaryOp('magI')      }
	magIa     { ^this.performUnaryOp('magIa')     }
	magIr     { ^this.performUnaryOp('magIr')     }
	magIcv    { ^this.performUnaryOp('magIcv')    } // local calc
	magINorm  { ^this.performUnaryOp('magINorm')  }
	magIaNorm { ^this.performUnaryOp('magIaNorm') }
	magIrNorm { ^this.performUnaryOp('magIrNorm') }

	// Admittance
	magA      { ^this.performUnaryOp('magA')      }
	magAa     { ^this.performUnaryOp('magAa')     }
	magAr     { ^this.performUnaryOp('magAr')     }
	magAcv    { ^this.performUnaryOp('magAcv')    }
	magANorm  { ^this.performUnaryOp('magANorm')  }
	magAaNorm { ^this.performUnaryOp('magAaNorm') }
	magArNorm { ^this.performUnaryOp('magArNorm') }

	// Energy
	magW      { ^this.performUnaryOp('magW')      }
	magWa     { ^this.performUnaryOp('magWa')     }
	magWr     { ^this.performUnaryOp('magWr')     }
	magWcv    { ^this.performUnaryOp('magWcv')    }
	magWNorm  { ^this.performUnaryOp('magWNorm')  }
	magWaNorm { ^this.performUnaryOp('magWaNorm') }
	magWrNorm { ^this.performUnaryOp('magWrNorm') }

	/* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	/  Aggregate quantities
	/
	/  Note - Use CCBlock for efficiency if accessing these
	/  quantities multiple times, e.g. for higher level
	/  quantities using intensity */

	intensity  { ^IntensityBlock.newFromPVBlock(this)  }
	admittance { ^AdmittanceBlock.newFromPVBlock(this) }

	/* Total (sum) measures	*/

	blockNorm { this.subclassResponsibility }

	totalWpot    { ^this.blockNorm * this.wpot.sum    }
	totalWkin    { ^this.blockNorm * this.wkin.sum    }
	totalWpkmean { ^this.blockNorm * this.wpkmean.sum }
	totalWpkdiff { ^this.blockNorm * this.wpkdiff.sum }
	totalWdens   { ^this.blockNorm * this.wdens.sum   }

	totalI  { ^this.blockNorm * this.intensity.sum          }
	totalIa { ^this.blockNorm * this.intensity.active.sum   }
	totalIr { ^this.blockNorm * this.intensity.reactive.sum }
	totalA  { ^this.numFrames * this.totalI  / (this.totalWpot + Atk.regSq) }
	totalAa { ^this.numFrames * this.totalIa / (this.totalWpot + Atk.regSq) }
	totalAr { ^this.numFrames * this.totalIr / (this.totalWpot + Atk.regSq) }
	totalW  { ^this.numFrames * this.totalI  / (this.totalWpkmean + Atk.regSq) }
	totalWa { ^this.numFrames * this.totalIa / (this.totalWpkmean + Atk.regSq) }
	totalWr { ^this.numFrames * this.totalIr / (this.totalWpkmean + Atk.regSq) }

	totalINorm  { ^this.numFrames * this.avgINorm  }	// TODO: revisit "total" quantity, conceptually
	// e.g. why is it not this.performUnaryOp('unitNorm').sum ?
	totalIaNorm { ^this.numFrames * this.avgIaNorm }
	totalIrNorm { ^this.numFrames * this.avgIrNorm }
	totalANorm  { ^this.numFrames * this.avgANorm  }
	totalAaNorm { ^this.numFrames * this.avgAaNorm }
	totalArNorm { ^this.numFrames * this.avgArNorm }
	totalWNorm  { ^this.numFrames * this.avgWNorm  }
	totalWaNorm { ^this.numFrames * this.avgWaNorm }
	totalWrNorm { ^this.numFrames * this.avgWrNorm }

	totalMagI   { ^this.blockNorm * this.magI.sum   }
	totalMagIa  { ^this.blockNorm * this.magIa.sum  }
	totalMagIr  { ^this.blockNorm * this.magIr.sum  }
	totalMagIcv { ^this.blockNorm * this.magIcv.sum }

	totalMagA   { ^this.totalMagI   / (this.wpot.mean + Atk.regSq) }
	totalMagAa  { ^this.totalMagIa  / (this.wpot.mean + Atk.regSq) }
	totalMagAr  { ^this.totalMagIr  / (this.wpot.mean + Atk.regSq) }
	totalMagAcv { ^this.totalMagIcv / (this.wpot.mean + Atk.regSq) }
	// or?: totalMagAcv { ^this.magIcv.sum / (this.wpot.mean + Atk.regSq) }

	totalMagW   { ^this.totalMagI   / (this.wpkmean.mean + Atk.regSq) }
	totalMagWa  { ^this.totalMagIa  / (this.wpkmean.mean + Atk.regSq) }
	totalMagWr  { ^this.totalMagIr  / (this.wpkmean.mean + Atk.regSq) }
	totalMagWcv { ^this.totalMagIcv / (this.wpkmean.mean + Atk.regSq) }
	// or?: totalMagAcv { ^this.magWcv.sum / (this.wpot.mean + Atk.regSq) }

	// TODO: should these be normalized by the avg magnitude over the frame?
	//       and: subclass for different normalization for time vs. freq domain frame?
	totalMagINorm  { ^this.totalMagI  / (this.magIcv.mean + Atk.regSq) }
	totalMagIaNorm { ^this.totalMagIa / (this.magIcv.mean + Atk.regSq) }
	totalMagIrNorm { ^this.totalMagIr / (this.magIcv.mean + Atk.regSq) }
	totalMagANorm  { ^this.totalMagA  / (this.magAcv.mean + Atk.regSq) }
	totalMagAaNorm { ^this.totalMagAa / (this.magAcv.mean + Atk.regSq) }
	totalMagArNorm { ^this.totalMagAr / (this.magAcv.mean + Atk.regSq) }
	totalMagWNorm  { ^this.totalMagW  / (this.magWcv.mean + Atk.regSq) }
	totalMagWaNorm { ^this.totalMagWa / (this.magWcv.mean + Atk.regSq) }
	totalMagWrNorm { ^this.totalMagWr / (this.magWcv.mean + Atk.regSq) }

	/* Average (mean) measures */

	avgWpot    { |weights = nil| ^this.prBlockAvg(this.wpot, weights)    }
	avgWkin    { |weights = nil| ^this.prBlockAvg(this.wkin, weights)    }
	avgWpkmean { |weights = nil| ^this.prBlockAvg(this.wpkmean, weights) }
	avgWpkdiff { |weights = nil| ^this.prBlockAvg(this.wpkdiff, weights) }
	avgWdens   { |weights = nil| ^this.prBlockAvg(this.wdens, weights)   }

	avgI { |weights = nil|
		^weights.isNil.if(
			{ this.totalI / this.numFrames },
			{ this.prwmeanCol(this.intensity, weights) }
		)
	}
	avgIa { |weights = nil|
		^weights.isNil.if(
			{ this.totalIa / this.numFrames },
			{ this.prwmeanCol(this.intensity.active, weights) }
		)
	}
	avgIr { |weights = nil|
		^weights.isNil.if(
			{ this.totalIr / this.numFrames },
			{ this.prwmeanCol(this.intensity.reactive, weights) }
		)
	}
	avgA { |weights = nil|
		^weights.isNil.if(
			{ this.totalA / this.numFrames },
			{ this.prwmeanCol(this.admittance, weights) }
		)
	}
	avgAa { |weights = nil|
		^weights.isNil.if(
			{ this.totalAa / this.numFrames },
			{ this.prwmeanCol(this.admittance.active, weights) }
		)
	}
	avgAr { |weights = nil|
		^weights.isNil.if(
			{ this.totalAr / this.numFrames },
			{ this.prwmeanCol(this.admittance.reactive, weights) }
		)
	}
	avgW { |weights = nil|
		^weights.isNil.if(
			{ this.totalW / this.numFrames },
			{ this.prwmeanCol(this.energy, weights) }
		)
	}
	avgWa { |weights = nil|
		^weights.isNil.if(
			{ this.totaWa / this.numFrames },
			{ this.prwmeanCol(this.energy.active, weights) }
		)
	}
	avgWr { |weights = nil|
		^weights.isNil.if(
			{ this.totalWr / this.numFrames },
			{ this.prwmeanCol(this.energy.reactive, weights) }
		)
	}

	avgINorm { |weights = nil|
		^weights.isNil.if(
			{ this.intensity.unitNorm.sum / this.numFrames },
			{ this.prwmeanCol(this.intensity.unitNorm, weights) }
		)
	}
	avgIaNorm { |weights = nil|
		^weights.isNil.if(
			{ this.intensity.activeUnitNorm.sum / this.numFrames },
			{ this.prwmeanCol(this.intensity.activeUnitNorm, weights) }
		)
	}
	avgIrNorm { |weights = nil|
		^weights.isNil.if(
			{ this.intensity.reactiveUnitNorm.sum / this.numFrames },
			{ this.prwmeanCol(this.intensity.reactiveUnitNorm, weights) }
		)
	}

	avgMagI   { |weights = nil| ^this.prBlockAvg(this.magI, weights)   }
	avgMagIa  { |weights = nil| ^this.prBlockAvg(this.magIa, weights)  }
	avgMagIr  { |weights = nil| ^this.prBlockAvg(this.magIr, weights)  }
	avgMagIcv { |weights = nil| ^this.prBlockAvg(this.magIcv, weights) }

	avgMagINorm   { |weights = nil|
		^if(weights.isNil,
			{ this.totalMagINorm / this.numFrames },
			{ this.magINorm.wmean(weights) }	// TODO: check accumulating regularization
		)
	}
	avgMagIaNorm  { |weights = nil|
		^if(weights.isNil,
			{ this.totalMagIaNorm / this.numFrames },
			{ this.magIaNorm.wmean(weights) }	// TODO: check accumulating regularization
		)
	}
	avgMagIrNorm  { |weights = nil|
		^if(weights.isNil,
			{ this.totalMagIrNorm / this.numFrames },
			{ this.magIrNorm.wmean(weights) }	// TODO: check accumulating regularization
		)
	}
	avgMagIcvNorm { |weights = nil|
		^if(weights.isNil,
			{ 1.0 },
			{ this.magIcvNorm.wmean(weights) }
		)
	}

	// TODO: below averages should be confirmed and refactored

	avgMagA   { |weights = nil|
		^if(weights.isNil,
			{ this.magI.sum / (this.wpot.sum + Atk.regSq)},
			{ this.magA.wmean(weights) }
		)
	}
	avgMagAa  { |weights = nil|
		^if(weights.isNil,
			{ this.magIa.sum / (this.wpot.sum + Atk.regSq)},
			{ this.magAa.wmean(weights) }
		)
	}
	avgMagAr  { |weights = nil|
		^if(weights.isNil,
			{ this.magIr.sum / (this.wpot.sum + Atk.regSq)},
			{ this.magAr.wmean(weights) }
		)
	}
	avgMagAcv { |weights = nil| ^this.prBlockAvg(this.magAcv, weights) }


	avgMagANorm   { |weights = nil|
		^if(weights.isNil,
			{ this.totalMagANorm / this.numFrames },
			{ this.magANorm.wmean(weights)        }		// TODO: check accumulating regularization
		)
	}
	avgMagAaNorm  { |weights = nil|
		^if(weights.isNil,
			{ this.totalMagAaNorm / this.numFrames },
			{ this.magAaNorm.wmean(weights)        }	// TODO: check accumulating regularization
		)
	}
	avgMagArNorm  { |weights = nil|
		^if(weights.isNil,
			{ this.totalMagArNorm / this.numFrames },
			{ this.magArNorm.wmean(weights)        }	// TODO: check accumulating regularization
		)
	}
	avgMagAcvNorm { |weights = nil|
		^if(weights.isNil,
			{ 1.0 },
			{ this.magAcvNorm.wmean(weights) }
		)
	}


	avgMagW   { |weights = nil|
		^if(weights.isNil,
			{ this.totalMagW / this.numFrames },
			{ this.magW.wmean(weights)        }
		)
	}
	avgMagWa  { |weights = nil|
		^if(weights.isNil,
			{ this.totalMagWa / this.numFrames },
			{ this.magWa.wmean(weights)        }
		)
	}
	avgMagWr  { |weights = nil|
		^if(weights.isNil,
			{ this.totalMagWr / this.numFrames },
			{ this.magWr.wmean(weights)        }
		)
	}
	avgMagWcv { |weights = nil| ^this.prBlockAvg(this.magWcv, weights) }

	avgMagWNorm   { |weights = nil|
		^if(weights.isNil,
			{ this.totalMagWNorm / this.numFrames },
			{ this.magWNorm.wmean(weights) }	// TODO: check accumulating regularization
		)
	}
	avgMagWaNorm  { |weights = nil|
		^if(weights.isNil,
			{ this.totalMagWaNorm / this.numFrames },
			{ this.magWaNorm.wmean(weights) }	// TODO: check accumulating regularization
		)
	}
	avgMagWrNorm  { |weights = nil|
		^if(weights.isNil,
			{ this.totalMagWrNorm / this.numFrames },
			{ this.magWrNorm.wmean(weights) }	// TODO: check accumulating regularization
		)
	}
	avgMagWcvNorm { |weights = nil|
		^if(weights.isNil,
			{ 1.0 },
			{ this.magWcvNorm.wmean(weights) }
		)
	}

	/* Helpers */

	// Average over the block, 1D quantities
	prBlockAvg { |quantity, weights|
		^if(weights.isNil, {
			// == this.totalWxxx / numFrames
			(this.blockNorm * quantity.sum) / this.numFrames
		}, {
			quantity.wmean(weights)
		})
	}

	/* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	/  Indicators
	/
	/  Note - all but -beta will calculate intensity or
	/  admittance, so for efficiency call on those
	/  Blocks directly */

	alpha { ^this.intensity.alpha        }
	beta  { ^this.performUnaryOp('beta') }
	gamma { ^this.intensity.gamma        }
	mu    { ^this.admittance.mu          }
}


PVFBlock[slot] : PVBlock {

	blockNorm { ^2 / this.numFrames }
}


PVABlock[slot] : PVBlock {

	blockNorm { ^1 }
}


// Intensimetric Cartesian quantities [x,y,z]
CartesianBlock[slot] : FrameBlock {

	x { ^this.collect(_[0]) }
	y { ^this.collect(_[1]) }
	z { ^this.collect(_[2]) }
}

AbstractIntensityBlock[slot] : CartesianBlock {

	// Note - performUnaryOp to dispatch methods to the frames

	/* Components */
	active           { ^this.performUnaryOp('active')   }
	reactive 	     { ^this.performUnaryOp('reactive') }

	unitNorm	     { ^this.performUnaryOp('unitNorm')         }
	activeUnitNorm   { ^this.performUnaryOp('activeUnitNorm')   }
	reactiveUnitNorm { ^this.performUnaryOp('reactiveUnitNorm') }

	/* Magnitudes */
	activeMag	     { ^this.performUnaryOp('activeMag')   }
	reactiveMag	     { ^this.performUnaryOp('reactiveMag') }
	cvmag		     { ^this.performUnaryOp('cvmag')       }

	magNorm	         { ^this.performUnaryOp('unitNorm')         }
	activeMagNorm    { ^this.performUnaryOp('activeUnitNorm')   }
	reactiveMagNorm  { ^this.performUnaryOp('reactiveUnitNorm') }

	/* Indicators */
	// beta: use PBBlock:-beta, needs the pressure-velocity components
	alpha { ^this.performUnaryOp('alpha') }
	gamma { ^this.performUnaryOp('gamma') }
	mu    { ^this.performUnaryOp('mu')    } // calling from IntensityBlock will recalculate Admittance

	/* Incidence */
	activeThetaPhi   { ^this.performUnaryOp('activeThetaPhi')   }
	reactiveThetaPhi { ^this.performUnaryOp('reactiveThetaPhi') }
}

IntensityBlock[slot] : AbstractIntensityBlock {

	*newFromPVBlock { |pvBlock|
		var intensity = pvBlock.performUnaryOp('intensity');

		^super.fill(pvBlock.numFrames, { |i|
			intensity[i]
		})
	}

	intensity 	{ ^this }

	// TODO:  admittance { ^ }

	/* Magnitudes
	/
	/  Note - these are largely for convenience, and will
	/  calculate the intensity/admittance for each frame
	/  on each method call. For multiple calls, call on
	/  IntensityBlock directly to avoid recalculations. */

	// Intensity
	magI      { ^this.performUnaryOp('magI')      }
	magIa     { ^this.performUnaryOp('magIa')     }
	magIr     { ^this.performUnaryOp('magIr')     }
	magIcv    { ^this.performUnaryOp('magIcv')    } // local calc
	magINorm  { ^this.performUnaryOp('magINorm')  }
	magIaNorm { ^this.performUnaryOp('magIaNorm') }
	magIrNorm { ^this.performUnaryOp('magIrNorm') }
}


AdmittanceBlock[slot] : AbstractIntensityBlock {

	*newFromPVBlock { |pvBlock|
		var admittance = pvBlock.performUnaryOp('admittance');

		^super.fill(pvBlock.numFrames, { |i|
			admittance[i]
		})
	}

	admittance { ^this }

	// TODO:  intensity { ^ }
}


// Class to store intermediate quantities for reuse and multiple access
CCBlock {
	var pvBlock, iBlock, aBlock;
	var wp, wu, ws, wd, wh;

	*newFromPV { |pvArray|
		^super.new.init(pvArray)
	}

	init { |pvArray|
		pvBlock = PVBlock.newFromPV(pvArray);
	}

	wp { ^wp ?? wp = pvBlock.wp }
	wu { ^wu ?? wu = pvBlock.wu }
	ws { ^ws ?? ws = pvBlock.ws }
	wd { ^wd ?? wd = pvBlock.wd }
	wh { ^wh ?? wh = pvBlock.wh }

	intensity {
		^iBlock ?? {
			iBlock = IntensityBlock.newFromPVBlock(pvBlock);
		}
	}

	admittance {
		// ^aBlock ?? {
		// 	aBlock = AdmittanceBlock.newFromIntensiyBlock(this.iBlock);
		// }
		var i = this.intensity;
		var wpReg = this.wp + Atk.regSq;

		^aBlock ?? { aBlock = (i / wpReg) }
		// i.collect({ |i_n|
		// 	Complex.new(  // explicit... slow otherwise!!
		// 		i_n.real / wpReg,
		// 		i_n.imag / wpReg
		// 	)
		// })

	}
}
