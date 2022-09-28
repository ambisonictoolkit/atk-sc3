+ SequenceableCollection {

	/* Vector support *
	/  Note behavior with multidimensional arrays!
	/  E.g., summation happens across sub-arrays.
	*/

	// Vector manitude, L2-norm, Euclidean distatnce
	vmag    { ^this.squared.sum.sqrt }
	l2norm  { ^this.squared.sum.sqrt }
	// L1-norm, "taxicab" norm
	l1norm  { ^this.abs.sum }
	// Max norm, inf norm
	maxnorm { ^this.abs.maxItem }
	infnorm { ^this.abs.maxItem }

	/* Complex support */

	// Complex magnitude, hypot(real, imag)
	// Synonyms of -magnitude, -abs, -rho
	cmag    { ^this.performUnaryOp('cmag') }
	modulus { ^this.performUnaryOp('modulus') }

	conj 	{ ^this.performUnaryOp('conj') }
}
