+ Matrix {

	// see AtkMatrix:writeToFile
	// type: /encoder, \decoder, \xformer
	// family: foa, hoa1, hoa2, etc
	writeToFile { arg fileNameOrPath, type, family, note, attributeDictionary, overwrite=false;
		var atkMatrix;
		atkMatrix = AtkMatrix.newFromMatrix(this);
		atkMatrix.writeToFile(fileNameOrPath, type, family, note, attributeDictionary, overwrite);
	}

}
