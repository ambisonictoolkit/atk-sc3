Hoa {

	*degreeStIdx { |degree|
		^(degree+1).squared - (degree*2 + 1)
	}

	*numDegreeCoeffs { |degree|
		^(degree*2 + 1)
	}

	*numOrderCoeffs { |order|
		^(order + 1).squared
	}

	// full 3D only
	*detectOrder { |numCoeffs|
		var squareOf = numCoeffs.squareOf;
		(squareOf == nil).if({
			"Could not detect order from % coefficients".format(numCoeffs).throw
		}, {
			^(squareOf - 1)
		});
	}

	*confirmOrder { |numCoeffs, order = (AtkHoa.defaultOrder)|
		^(this.detectOrder(numCoeffs) == order)
	}

}
