// HoaTest is a convenience class to run all Hoa Tests:
// HoaTest.run
//
// You can also change the reporting flag and testing
// order through the classvar before running the test.


HoaTests {
	classvar <>report=false, <>order=3, <>floatWithin=1e-8;

	// Run all HOA tests
	*run {
		[ // list of Test classes
			TestHoaEncoderMatrix,
			TestHoaDecoderMatrix,
			TestHoaRotationMatrix,
			TestNumberForHoa,
		].do(_.run)

	}

	// NOTE: making all Test classes a subclass of
	// HoaTest : UnitTest resulted in silent failure.
	// It appears all Test classes need to be direct
	// decendants of UnitTest.

	// test_subclasses {
	//    HoaTest.subclasses.do(_.run)
	// }

}