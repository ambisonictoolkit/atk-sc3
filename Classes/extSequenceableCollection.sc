+ SequenceableCollection {

	/* Vector support *
	/  Note behavior with multidimensional arrays!
	/  E.g., summation happens across dimension 2 ("columns").
	*/

	// Vector manitude, L2-norm, Euclidean distance
	vmag    { ^this.squared.sum.sqrt }
	l2norm  { ^this.squared.sum.sqrt }
	// L1-norm, "taxicab" norm
	l1norm  { ^this.abs.sum }
	// Max norm, inf norm
	maxnorm { ^this.abs.maxItem }
	infnorm { ^this.abs.maxItem }

	/* Complex support */

	// Complex magnitude: synonyms of -magnitude, -abs, -rho, hypot(real, imag)
	cmag    { ^this.performUnaryOp('cmag') }
	modulus { ^this.performUnaryOp('modulus') }

	// Complex conjugate: synonym of -conjugate
	conj 	{ ^this.performUnaryOp('conj') }
}
