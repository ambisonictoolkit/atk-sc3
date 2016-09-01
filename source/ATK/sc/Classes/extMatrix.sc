+ Matrix {

	// see also: AtkMatrix:writeToFile
	// set: FOA, HOA1, HOA2, etc
	// type: \encoder, \decoder, \xformer
	writeToFile { arg fileNameOrPath, set, type, note, attributeDictionary, overwrite=false;
		var atkMatrix;
		atkMatrix = AtkMatrix.newFromMatrix(this);
		atkMatrix.writeToFile(fileNameOrPath, set, type, note, attributeDictionary, overwrite);
	}

}
