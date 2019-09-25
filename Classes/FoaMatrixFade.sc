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
// 	Class: FoaMatrixFade
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

FoaMatrixFade {
	classvar <mtxFadeDef;
	// copyArgs
	var <outbus, <inbus, initMatrix, <>xFade, <mul, addAction, target, <server, completeCond;
	var <synth, <matrix, server, internalInbus = false, internalOutbus = false;


	*new { |outbus, inbus, initMatrix, xFade = 0.5, mul = 1,
		addAction, target, server, completeCond|

		^super.newCopyArgs(
			outbus, inbus, initMatrix, xFade, mul,
			addAction, target, server, completeCond).init;
	}


	init {
		fork({
			var addAct, targ;
			var cond = Condition(false);

			server = server ?? Server.default;

			FoaMatrixFade.mtxFadeDef.isNil.if{
				FoaMatrixFade.loadSynthDefs(server, cond);
				cond.wait;
			};

			inbus ?? {
				internalInbus = true;
				// "No input bus specified for FoaMatrixFade, so creating an input bus for you. Get it with .inbus".postln;
				inbus = server.audioBusAllocator.alloc(4);
			};
			outbus ?? {
				internalOutbus = true;
				"Creating an output bus. Get it with .outbus".postln;
				outbus = server.audioBusAllocator.alloc(4);
			};
			addAct = addAction ?? { \addToTail };
			targ = target ?? { 1 };

			synth = Synth.new(mtxFadeDef.name, [
				\outbus, outbus,
				\inbus, inbus,
				\fade, xFade,
				\mul, mul
			], targ, addAct);

			server.sync;

			initMatrix !? { this.matrix_(initMatrix) };

			completeCond !? { completeCond.test_(true).signal; };
		})
	}


	matrix_ { |newMatrix|
		var flatMatrix;

		flatMatrix = case(
			{ newMatrix.isKindOf(Matrix) }, { newMatrix.asArray.flat },
			{ newMatrix.isKindOf(FoaXformerMatrix) }, { newMatrix.matrix.asArray.flat }
		);

		synth.set(\fade, xFade, \matrixArray, flatMatrix);

		// update instance var for introspection
		matrix = newMatrix;
	}


	mul_{ |mul|
		synth.set(\mul, mul);
		mul = mul;
	}


	free {
		synth.free;
		internalInbus.if{ server.audioBusAllocator.free(inbus) };
		internalOutbus.if{ server.audioBusAllocator.free(outbus) };
	}


	*loadSynthDefs { |server, cond|

		server.waitForBoot({
			mtxFadeDef = SynthDef(\foaMatrixFade, { |outbus, inbus, fade = 1.5, mul = 1|
				var foaSrc, array, out;

				foaSrc = In.ar(inbus, 4) * Lag.kr(mul);

				array = Control.names([\matrixArray]).kr(
					Matrix.newIdentity(4).asArray.flat // initialize with no transform
				);
				array = Lag.kr(array, fade); // lag the matrix swap

				array = array.clump(4).flop;

				out = Mix.fill(4, { |i| // fill input
					array[i] * foaSrc[i]
				});

				Out.ar(outbus, out);
			}).load(server);

			// wait for synthdef to load
			server.sync;
			cond.test_(true).signal;
		});

	}
}
