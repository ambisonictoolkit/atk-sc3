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
// 	Extension: Matrix
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
	*newDiagonal { |diagonal|
		var matrix;

		matrix = Matrix.newClear(diagonal.size, diagonal.size);  // empty
		diagonal.do({ |item, i| matrix.put(i, i, item) });  // fill
		diagonal.any({ |item| item.isFloat }).if({  // test and recast to float
			matrix = matrix.asFloat
		});

		^matrix
	}

	// TEMP (or submit to MathLib as PR) override Matrix:*with
	// work around to avoid .flop.flop, which if called on Array, will expose
	// it to a bug that could fail on large arrays.
	// also, this is more efficient
	*with { |array| // return matrix from 2D array (array of rows)
		var shapes, shapeTest, numTest, rows;

		shapes = array.asArray.collect(_.shape).flatten;

		shapeTest = shapes.every(_ == shapes[0]);
		numTest = array.flatten.every(_.isNumber);

		(shapeTest and: numTest).if({
			rows = array.size;
			^super.fill(rows, { |col| array.at(col) });
			}, {
				error("wrong type of argument in Meta_Matrix-with");this.halt
		});
	}

	rowsDo { |func|
		this.rows.do{ |row, ri| func.(this.getRow(row), ri) }
	}

	colsDo { |func|
		this.cols.do{ |col, ci| func.(this.getCol(col), ci) }
	}

	// return a sub matrix
	getSub { |rowStart=0, colStart=0, rowLength, colHeight|
		var width, height, mtx, maxw, maxh;

		maxw = this.cols - rowStart;
		maxh = this.rows - colStart;

		width = rowLength ?? { maxw };
		height = colHeight ?? { maxh };

		((width > maxw) or: (height > maxh)).if{
			format("dimensions of requested sub-matrix exceed bounds: "
				"you asked for %x%, remaining space after starting index is %x%",
				rowLength, colHeight, maxw, maxh
			).throw
		};

		mtx = Matrix.newClear(height, width);

		(colStart..colStart + height - 1).do{ |row, i|
			mtx.putRow(i,
				this.getRow(row).drop(rowStart).keep(width)
			);
		};

		^mtx
	}

	// post a sub matrix, formatted for viewing
	postSub { |rowStart=0, colStart=0, rowLength, colHeight, round=0.001|
		var pmtx, maxstrlen=0, temp;

		pmtx = this.getSub(rowStart, colStart, rowLength, colHeight).round(round);
		pmtx.doMatrix({ |item| maxstrlen = max(maxstrlen, item.asString.size) });

		pmtx.rowsDo({ |rowArray, i|
			rowArray.collect({ |item| item.asString.padLeft(maxstrlen) }).postln;
			"".postln; // space it out vertically
		})
	}

	// this is a destructive operation:
	// force values to zero that are within threshold distance (positive or negative)
	zeroWithin { |within = (-180.dbamp)|
		this.rowsDo({ |rArray, ri|
			rArray.do({ |item, ci|
				this.put(
					ri,
					ci,
					(item.abs <= within).if({
						item.isInteger.if({  // there could be more cases...
								0
							}, {
								0.0
							})
					}, {
						item
					})
				)
			})
		})
	}

}
