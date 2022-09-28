+ Complex {

	// Complex magnitude, synonyms of -magnitude, -abs, -rho
	cmag    { ^hypot(real, imag) }
	modulus { ^hypot(real, imag) }

	// Conjugate (synonym)
	conj { ^Complex.new(real, imag.neg) }

}
