// AtkTests is a convenience class to run all Atk Tests:
// AtkTests.run
//
// You can also change the reporting flag and testing
// order through the classvar before running the test.


AtkTests {
	classvar
	<>report = false,
	<>order = 3,          // for HOA tests
	<>floatWithin = 1e-8; // float error tolerance

	// Run all HOA tests
	*run { |verbose=false, hoaOrder|
		report = verbose;
		hoaOrder !? {order = hoaOrder};
		[
			// list of Test classes
			TestHoaRotationMatrix,
			TestHoaXformerMatrix,
			TestNumberForHoa,      // tests sphericalHarmonic method
			TestFoaMatrix,         // includes matrix encoders, decoders, xformers
		].do(_.run)

	}

	// Return reference directions used in tests
	// orientation:  tetrahedral orientation for \tetra group
	// numDirs:      number of directions to return in the case the
	//               group doesn't inherently determine the number (e.g. random directions)
	*getDirs { |group = \tetra, orientation = 'flu', numDirs = 25|

		^switch(group,
			\axis, {[ // axis directions
				[0,0], [pi/2,0], [0,pi/2],      // +X, +Y, +Z
				[pi/2,0], [-pi/2,0], [0,-pi/2]  // -X, -Y, -Z
			]},
			\tetra, { // tetrahedral directions
				FoaDecoderMatrix.newBtoA(orientation).dirOutputs
			},
			\cube, { // directions of cube corners
				4.collect({|i|
					[45.degrad + (i*90.degrad), atan(2.sqrt.reciprocal)]
				}) ++ 4.collect({|i|
					[45.degrad + (i*90.degrad), atan(2.sqrt.reciprocal).neg]
				})
			},
			\random, { // random directions
				numDirs.collect{[rrand(-2pi, 2pi), rrand(-2pi, 2pi)]}
			}
		);
	}

	// NOTE: making all Test classes a subclass of
	// AtkTest:UnitTest resulted in silent failure.
	// It appears all Test classes need to be direct
	// decendants of UnitTest.

	// test_subclasses {
	//    AtkTest.subclasses.do(_.run)
	// }

}