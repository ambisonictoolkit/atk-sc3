+ Matrix {

	// see also: AtkMatrix:writeToFile
	// type: \encoder, \decoder, \xformer
	// set: foa, hoa1, hoa2, etc
	writeToFile { arg fileNameOrPath, type, set, note, attributeDictionary, overwrite=false;
		var atkMatrix;
		atkMatrix = AtkMatrix.newFromMatrix(this);
		atkMatrix.writeToFile(fileNameOrPath, type, set, note, attributeDictionary, overwrite);
	}

}
