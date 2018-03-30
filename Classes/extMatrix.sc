/*
	Copyright the ATK Community and Joseph Anderson, 2011-2017
		J Anderson	j.anderson[at]ambisonictoolkit.net
        M McCrea    mtm5[at]uw.edu

	This file is part of SuperCollider3 version of the Ambisonic Toolkit (ATK).

	The SuperCollider3 version of the Ambisonic Toolkit (ATK) is free software:
	you can redistribute it and/or modify it under the terms of the GNU General
	Public License as published by the Free Software Foundation, either version 3
	of the License, or (at your option) any later version.

	The SuperCollider3 version of the Ambisonic Toolkit (ATK) is distributed in
	the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
	implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See
	the GNU General Public License for more details.

	You should have received a copy of the GNU General Public License along with the
	SuperCollider3 version of the Ambisonic Toolkit (ATK). If not, see
	<http://www.gnu.org/licenses/>.
*/


//---------------------------------------------------------------------
//	The Ambisonic Toolkit (ATK) is a soundfield kernel support library.
//
// 	Extension: Array
//
//	The Ambisonic Toolkit (ATK) is intended to bring together a number of tools and
//	methods for working with Ambisonic surround sound. The intention is for the toolset
//	to be both ergonomic and comprehensive, providing both classic and novel algorithms
//	to creatively manipulate and synthesise complex Ambisonic soundfields.
//
//	The tools are framed for the user to think in terms of the soundfield kernel. By
//	this, it is meant the ATK addresses the holistic problem of creatively controlling a
//	complete soundfield, allowing and encouraging the composer to think beyond the placement
//	of sounds in a sound-space and instead attend to the impression and image of a soundfield.
//	This approach takes advantage of the model the Ambisonic technology presents, and is
//	viewed to be the idiomatic mode for working with the Ambisonic technique.
//
//
//	We hope you enjoy the ATK!
//
//	For more information visit http://ambisonictoolkit.net/ or
//	email info[at]ambisonictoolkit.net
//
//---------------------------------------------------------------------

+ Matrix {

    // diagonal matrix - consider adding to Matrix Quark Extension
    *newDiagonal { arg diagonal;
        var matrix;

        matrix = Matrix.newClear(diagonal.size, diagonal.size);  // empty
        diagonal.do({ arg item, i; matrix.put(i, i, item)});  // fill
        diagonal.any({ arg item; item.isFloat }).if({  // test and recast to float
            matrix = matrix.asFloat
        });

        ^matrix
    }

	// TEMP (or submit to MathLib as PR) override Matrix:*with
	// work around to avoid .flop.flop, which if called on Array, will expose
	// it to a bug that could fail on large arrays.
	// also, this is more efficient
	*with { arg array; // return matrix from 2D array (array of rows)
		var shapes, shapeTest, numTest, rows;

		shapes = array.asArray.collect(_.shape).flatten;

		shapeTest = shapes.every(_ == shapes[0]);
		numTest = array.flatten.every(_.isNumber);

		if (shapeTest and: numTest, {
			rows = array.size;
			^super.fill(rows, {arg col; array.at(col) });
			},{
				error("wrong type of argument in Meta_Matrix-with");this.halt
		});
	}

	rowsDo { |func|
		this.rows.do{|row, ri| func.(this.getRow(row), ri)}
	}

	colsDo { |func|
		this.cols.do{|col, ci| func.(this.getCol(col), ci)}
	}

	// return a sub matrix
	getSub { |rowStart=0, colStart=0, rowLength, colHeight|
		var width, height, mtx, maxw, maxh;

		maxw = this.cols - rowStart;
		maxh = this.rows - colStart;

		width = rowLength ?? {maxw};
		height = colHeight ?? {maxh};

		if ((width > maxw) or: (height > maxh)) {
			format(
				"dimensions of requested sub-matrix exceed bounds: "
				"asked for %x%, remaining space after starting index is %x%",
				rowLength, colHeight, maxw, maxh
			).throw
		};

		mtx = Matrix.newClear(height, width);

		(colStart..colStart+height-1).do{ |row, i|
			mtx.putRow(i,
				this.getRow(row).drop(rowStart).keep(width)
			);
		};

		^mtx
	}

	// post a sub matrix, formatted for viewing
	postSub { |rowStart=0, colStart=0, rowLength, colLength, round=0.001|
		var pmtx, maxstrlen=0, temp;

		pmtx = this.getSub(rowStart, colStart, rowLength, colLength).round(round);
		pmtx.doMatrix({|item| maxstrlen = max(maxstrlen, item.asString.size)});

		pmtx.rowsDo(
			{ |rowArray, i|
				rowArray.collect({ |item| item.asString.padLeft(maxstrlen) }).postln;
				"".postln; // space it out vertically
				// min((maxstrlen/2).asInt-1, 3).do{"".postln}; // space it out vertically
			}
		)

	}

	// this is a destructive operation:
	// the matrix will be zero'd within the absolute valude of the threshold
	zeroWithin { |threshold = (-300.dbamp)|
		this.rowsDo({ |rArray, ri|
			rArray.do{ |item, ci|
				this.put(ri, ci, if(item.abs <= threshold, {0},{item}))}
		});
	}

}

