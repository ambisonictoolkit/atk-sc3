+ Complex {

	// Complex magnitude, synonyms of -magnitude, -abs, -rho
	cmag    { ^hypot(real, imag) }
	modulus { ^hypot(real, imag) }

	// Conjugate (synonym of -conjugate)
	conj { ^Complex.new(real, imag.neg) }

	// Intensimetric extensions
	active { ^real }
	reactive { ^imag }

}
