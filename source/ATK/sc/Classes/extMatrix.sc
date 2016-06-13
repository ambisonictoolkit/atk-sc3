+ Matrix {

	// see AtkMatrix:writeToFile
	writeToFile { arg fileNameOrPath, type, note, attributeDictionary, overwrite=false;
		var atkMatrix;
		atkMatrix = AtkMatrix.newFromMatrix(this);
		atkMatrix.writeToFile(fileNameOrPath, type, note, attributeDictionary, overwrite);
	}

}
