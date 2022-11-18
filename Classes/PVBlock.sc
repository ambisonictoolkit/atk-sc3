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
	vmag   { ^this.collect(_.vmag) }
	l2norm { ^this.collect(_.vmag) }

	// Weighted mean of each component across frames/"columns" of the block
	prwmean { |dataBlock, weights|
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


	/* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	/  Aggregate quantities
	/
	/  Note - Use CCBlock for efficiency if accessing these
	/  quantities multiple times, e.g. for higher level
	/  quantities using intensity */

	intensity  { ^IntensityBlock.newFromPVBlock(this)  }
	admittance { ^AdmittanceBlock.newFromPVBlock(this) }

	/*
	/	Magnitudes
	*/

	//	Energy

	magW      { ^this.performUnaryOp('magW')      }
	magWa     { ^this.performUnaryOp('magWa')     }
	magWr     { ^this.performUnaryOp('magWr')     }
	magWcv    { ^this.performUnaryOp('magWcv')    }
	magWNorm  { ^this.performUnaryOp('magWNorm')  }
	magWaNorm { ^this.performUnaryOp('magWaNorm') }
	magWrNorm { ^this.performUnaryOp('magWrNorm') }

	/* Intensity/Admittance magnitudes are largely here for
	/  convenience and will calculate the intensity/admittance
	/  for each block on each method call. For multiple calls,
	/  call on IntensityBlock directly to avoid recalculations. */

	//	Intensity

	magI      { ^this.intensity.magnitude       }
	magIa     { ^this.intensity.activeMag       }
	magIr     { ^this.intensity.reactiveMag     }
	magIcv    { ^this.performUnaryOp('magIcv')  } // local calc—no intensity needed
	magINorm  { ^this.intensity.magNorm         }
	magIaNorm { ^this.intensity.activeMagNorm   }
	magIrNorm { ^this.intensity.reactiveMagNorm }

	//	Admittance

	magA      { ^this.admittance.magnitude       }
	magAa     { ^this.admittance.activeMag       }
	magAr     { ^this.admittance.reactiveMag     }
	magAcv    { ^this.admittance.cvmag           }
	magANorm  { ^this.admittance.magNorm         }
	magAaNorm { ^this.admittance.activeMagNorm   }
	magArNorm { ^this.admittance.reactiveMagNorm }

	/*
	/	Total (sum) measures
	*/

	blockNorm { this.subclassResponsibility }

	//	Energy

	totalWpot    { ^this.blockNorm * this.wpot.sum    }
	totalWkin    { ^this.blockNorm * this.wkin.sum    }
	totalWpkmean { ^this.blockNorm * this.wpkmean.sum }
	totalWpkdiff { ^this.blockNorm * this.wpkdiff.sum }
	totalWdens   { ^this.blockNorm * this.wdens.sum   }

	/* TODO:
	/	In general, the total & average quantities need to be revisited
	/   wrt their assumptions about whether the total refers to the assumed
	/   total of that *single* bin extrapolated across the frequency frame (all bins),
	/	OR - total of energy of the (time) frame, assuming a monofrequent analytic signal */

	/* Intensity/Admittance magnitudes are largely here for
	/  convenience and will calculate the intensity/admittance
	/  for each block on each method call. For multiple calls,
	/  call on IntensityBlock directly to avoid recalculations. */
	//	Intensity
	totalI  { ^this.intensity.total         }
	totalIa { ^this.intensity.totalActive   }
	totalIr { ^this.intensity.totalReactive }
	//	Admittance
	// Note: admitttance is intensity / (wpot + reg),
	// so need to break out the calculation here to avoid accumulating
	// the regularization 													TODO: confirm
	totalA  { ^this.numFrames * this.totalI  / (this.totalWpot + Atk.regSq) }
	totalAa { ^this.numFrames * this.totalIa / (this.totalWpot + Atk.regSq) }
	totalAr { ^this.numFrames * this.totalIr / (this.totalWpot + Atk.regSq) }

	totalW  { ^this.numFrames * this.totalI  / (this.totalWpkmean + Atk.regSq) }
	totalWa { ^this.numFrames * this.totalIa / (this.totalWpkmean + Atk.regSq) }
	totalWr { ^this.numFrames * this.totalIr / (this.totalWpkmean + Atk.regSq) }

	totalINorm  { ^this.intensity.avgUnitNorm }
	totalIaNorm { ^this.intensity.avgActiveUnitNorm }
	totalIrNorm { ^this.intensity.avgReactiveUnitNorm }

	totalANorm  { ^this.admittance.avgUnitNorm }
	totalAaNorm { ^this.admittance.avgActiveUnitNorm }
	totalArNorm { ^this.admittance.avgReactiveUnitNorm }

	// Not yet defined...
	// totalWNorm  { ^this.numFrames * this.avgWNorm  }
	// totalWaNorm { ^this.numFrames * this.avgWaNorm }
	// totalWrNorm { ^this.numFrames * this.avgWrNorm }

	totalMagI   { ^this.intensity.totalMag }
	totalMagIa  { ^this.intensity.totalActiveMag }
	totalMagIr  { ^this.intensity.totalReactiveMag }
	totalMagIcv { ^this.intensity.totalcvmag }

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

	/*
	/	Average (mean) measures
	*/

	//	Energetic means

	avgWpot    { |weights = nil| ^this.prBlockAvg(this.wpot, weights)    }
	avgWkin    { |weights = nil| ^this.prBlockAvg(this.wkin, weights)    }
	avgWpkmean { |weights = nil| ^this.prBlockAvg(this.wpkmean, weights) }
	avgWpkdiff { |weights = nil| ^this.prBlockAvg(this.wpkdiff, weights) }
	avgWdens   { |weights = nil| ^this.prBlockAvg(this.wdens, weights)   }

	//	Intensimetric means

	// avgI { |weights = nil|
	// 	^weights.isNil.if(
	// 		{ this.totalI / this.numFrames },
	// 		{ this.prwmean(this.intensity, weights) }
	// 	)
	// }
	// avgIa { |weights = nil|
	// 	^weights.isNil.if(
	// 		{ this.totalIa / this.numFrames },
	// 		{ this.prwmean(this.intensity.active, weights) }
	// 	)
	// }
	// avgIr { |weights = nil|
	// 	^weights.isNil.if(
	// 		{ this.totalIr / this.numFrames },
	// 		{ this.prwmean(this.intensity.reactive, weights) }
	// 	)
	// }
	// avgA { |weights = nil|
	// 	^weights.isNil.if(
	// 		{ this.totalA / this.numFrames },
	// 		{ this.prwmean(this.admittance, weights) }
	// 	)
	// }
	// avgAa { |weights = nil|
	// 	^weights.isNil.if(
	// 		{ this.totalAa / this.numFrames },
	// 		{ this.prwmean(this.admittance.active, weights) }
	// 	)
	// }
	// avgAr { |weights = nil|
	// 	^weights.isNil.if(
	// 		{ this.totalAr / this.numFrames },
	// 		{ this.prwmean(this.admittance.reactive, weights) }
	// 	)
	// }
	// avgW { |weights = nil|
	// 	^weights.isNil.if(
	// 		{ this.totalW / this.numFrames },
	// 		{ this.prwmean(this.energy, weights) }
	// 	)
	// }
	// avgWa { |weights = nil|
	// 	^weights.isNil.if(
	// 		{ this.totaWa / this.numFrames },
	// 		{ this.prwmean(this.energy.active, weights) }
	// 	)
	// }
	// avgWr { |weights = nil|
	// 	^weights.isNil.if(
	// 		{ this.totalWr / this.numFrames },
	// 		{ this.prwmean(this.energy.reactive, weights) }
	// 	)
	// }

	// avgINorm { |weights = nil|
	// 	^weights.isNil.if(
	// 		{ this.intensity.unitNorm.sum / this.numFrames },
	// 		{ this.prwmean(this.intensity.unitNorm, weights) }
	// 	)
	// }
	// avgIaNorm { |weights = nil|
	// 	^weights.isNil.if(
	// 		{ this.intensity.activeUnitNorm.sum / this.numFrames },
	// 		{ this.prwmean(this.intensity.activeUnitNorm, weights) }
	// 	)
	// }
	// avgIrNorm { |weights = nil|
	// 	^weights.isNil.if(
	// 		{ this.intensity.reactiveUnitNorm.sum / this.numFrames },
	// 		{ this.prwmean(this.intensity.reactiveUnitNorm, weights) }
	// 	)
	// }

	//	Magnitude means

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

	/*	Soundfield Indicator means */

	// Alpha
	avgAlpha { |weights = nil|
		^this.intensity.avgAlpha(weights)
	}

	/* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	/  Helpers */

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

	activeMag   { ^this.performUnaryOp('activeMag')   }
	reactiveMag { ^this.performUnaryOp('reactiveMag') }
	cvmag		{ ^this.performUnaryOp('cvmag')       }

	magNorm	        { ^this.performUnaryOp('unitNorm')         }
	activeMagNorm   { ^this.performUnaryOp('activeUnitNorm')   }
	reactiveMagNorm { ^this.performUnaryOp('reactiveUnitNorm') }

	/* Totals (sums) */

	// Base quantity totals

	totalUnitNorm     { ^this.numFrames * this.avgUnitNorm         }	// TODO: revisit "total" quantity, conceptually
	totalActiveNorm   { ^this.numFrames * this.avgActiveUnitNorm   }
	totalReactiveNorm { ^this.numFrames * this.avgReactiveUnitNorm }

	total         { ^this.subclassResponsibility }
	totalActive   { ^this.subclassResponsibility }
	totalReactive { ^this.subclassResponsibility }

	totalMag         { ^this.subclassResponsibility }
	totalActiveMag   { ^this.subclassResponsibility }
	totalReactiveMag { ^this.subclassResponsibility }
	totalcvmag       { ^this.subclassResponsibility }

	/* Averages (means) */
	avgerage { |weights = nil|
		^weights.isNil.if(
			{ this.total / this.numFrames },
			{ this.prwmean(this, weights) }
		)
	}
	avgActive { |weights = nil|
		^weights.isNil.if(
			{ this.totalActive / this.numFrames },
			{ this.prwmean(this.active, weights) }
		)
	}
	avgReactive { |weights = nil|
		^weights.isNil.if(
			{ this.totalReactive / this.numFrames },
			{ this.prwmean(this.reactive, weights) }
		)
	}

	/* Indicators */

	alpha { ^this.performUnaryOp('alpha') }
	// beta: use PBBlock:-beta, needs the pressure-velocity components
	gamma { ^this.performUnaryOp('gamma') }
	mu    { ^this.performUnaryOp('mu')    } // calling from IntensityBlock will recalculate Admittance

	activeThetaPhi   { ^this.performUnaryOp('activeThetaPhi')   }
	reactiveThetaPhi { ^this.performUnaryOp('reactiveThetaPhi') }

	/*
	/	Averages (means)
	*/

	//	Normalized vector averages

	avgUnitNorm { |weights = nil|
		^weights.isNil.if(
			{ this.unitNorm.sum / this.numFrames },
			{ this.prwmean(this.unitNorm, weights) }
		)
	}
	avgActiveUnitNorm { |weights = nil|
		^weights.isNil.if(
			{ this.activeUnitNorm.sum / this.numFrames },
			{ this.prwmean(this.activeUnitNorm, weights) }
		)
	}
	avgReactiveUnitNorm { |weights = nil|
		^weights.isNil.if(
			{ this.reactiveUnitNorm.sum / this.numFrames },
			{ this.prwmean(this.reactiveUnitNorm, weights) }
		)
	}
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

	total         { ^this.blockNorm * this.sum          }
	totalActive   { ^this.blockNorm * this.active.sum   }
	totalReactive { ^this.blockNorm * this.reactive.sum }

	totalMag         { ^this.blockNorm * this.magnitude.sum   }
	totalActiveMag   { ^this.blockNorm * this.activeMag.sum  }
	totalReactiveMag { ^this.blockNorm * this.reactiveMag.sum  }
	totalcvmag       { ^this.blockNorm * this.cvmag.sum }

	/*
	/	Averages (means)
	*/

	//	Indicator averages

	avgAlpha { |weights = nil|
		var avgMagI = this.avgMagI(weights);
		^atan2(this.avgMagIa, magIa)
	}
}


AdmittanceBlock[slot] : AbstractIntensityBlock {

	*newFromPVBlock { |pvBlock|
		var admittance = pvBlock.performUnaryOp('admittance');

		^super.fill(pvBlock.numFrames, { |i|
			admittance[i]
		})
	}

	admittance { ^this }

	// intensity { ^ }				// TODO

	// TODO: see PVBLock totalA, totalAa, totalAr
	// total         { ^ }
	// totalActive   { ^ }
	// totalReactive { ^ }

	// totalMag         { ^ }
	// totalActiveMag   { ^ }
	// totalReactiveMag { ^ }
	// totalcvmag       { ^ }

	/*
	/	Averages (means)
	*/

	//	Indicator averages			// TODO
	avgMu { |weights = nil|
	}
}


EnergyBlock[slot] : AbstractIntensityBlock {

	*newFromPVBlock { |pvBlock|
		var energy = pvBlock.performUnaryOp('energy');

		^super.fill(pvBlock.numFrames, { |i|
			energy[i]
		})
	}

	energy { ^this }

	// intensity { ^ }				// TODO

	// TODO: see PVBLock totalA, totalAa, totalAr
	// total         { ^ }
	// totalActive   { ^ }
	// totalReactive { ^ }

	// totalMag         { ^ }
	// totalActiveMag   { ^ }
	// totalReactiveMag { ^ }
	// totalcvmag       { ^ }

	/*
	/	Averages (means)
	*/

	//	Indicator averages			// TODO
	avgMu { |weights = nil|
	}
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
		var wp_reg = this.wp + Atk.regSq;

		^aBlock ?? {
			aBlock = this.intensity / wp_reg
		}
		// i.collect({ |i_n|
		// 	Complex.new(  // explicit... slow otherwise!!
		// 		i_n.real / wpReg,
		// 		i_n.imag / wpReg
		// 	)
		// })

	}
}
