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
// 	Extension: TDesign
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

+ TDesign {

	*newHoa { |numChans, optimize = \energy, order = (AtkHoa.defaultOrder)|
		var designs, hoaDesign;
		var numPoints, minT;

		// matched design
		designs = TDesignLib.getHoaDesigns(optimize, order);

		// nil??
		numPoints = numChans.isNil.if({
			designs.first[\numPoints]
		}, {
			numChans
		});
		hoaDesign = designs.select({ |item|
			item[\numPoints] == numPoints
		}).last;

		// catch no designs
		hoaDesign ?? {
			// set minT for the reported error
			minT = switch(optimize,
				\energy, { 2 * order },      // energy
				\spreadE, { 2 * order + 1 }  // energy spread
			);
			format(
				"[TDesign:-init] No t-designs found in TDesignLib.lib matching "
				"nPnts %, t >= %, dim %. For optimize %, minimum numChans is %.",
				numChans, minT, 3, optimize, designs.first[\numPoints]
			).throw
		};

		^super.new.init(hoaDesign[\numPoints], hoaDesign[\t], 3);
	}

}
