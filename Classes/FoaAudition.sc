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
// 	Class: FoaAudition
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

FoaAudition {
	classvar <auditionSynthDefs;

	// copyArgs
	var <outbus, <xFade, xformMatrix, <inbus, addAction, target, <server, initCond, initGUI;
	var <loading, <auditionEnabled = false, <matrixFader, <group;
	var <pwSynth, <soundfileSynth, <sfSynth_3ch, <sfSynth_4ch, <inbusSynth, <diffSynth, <ui;
	var <soundfileBuf, <internalInbus, <gMul = 0.5;
	var synthReleaseTime = 0.5, vdiskinBufSize = 65536;


	*new { | outbus = 0, matrixFadeTime = 0.1, xformMatrix, inbus, addAction, target, server, initCond, initGUI = true|
		^super.newCopyArgs( outbus, matrixFadeTime, xformMatrix, inbus, addAction, target, server, initCond, initGUI ).init;
	}


	init {
		initCond = initCond ?? Condition(false);
		loading = true;

		fork({
			this.prepareAuditioning( initCond );
			initCond.wait;
			initGUI.if{ this.gui };
			loading = false;
		}, AppClock);

		// notify dependants of change in planewave azimuth direction
		// (e.g. FoaXformDisplay)
		OSCdef( \azResponder,
			{ |msg, time, addr, recvPort|
				this.changed(\pwAzim, msg[3]);
			}, '/audition_az', NetAddr("127.0.0.1", 57110)
		);

		OSCdef( \elResponder,
			{ |msg, time, addr, recvPort|
				this.changed(\pwElev, msg[3]);
			}, '/audition_el', NetAddr("127.0.0.1", 57110)
		);
	}


	playSynth { |which|
		var synth, tag, abort = false;

		fork ({
			if( auditionEnabled.not and: loading ) { "waiting for load".postln; initCond.wait };

			[ 'inbus', 'soundfile', 'pwNoise', 'diffuseNoise' ].includes(which).not.if{
				error( "didn't find that synth tag." ) };

			#synth, tag = switch( which,
				'inbus',        { [inbusSynth, \inbusSynthRunning] },
				'soundfile',    { [soundfileSynth, \sfSynthRunning] },
				'pwNoise',      { [pwSynth, \pwSynthRunning] },
				'diffuseNoise', { [diffSynth, \diffSynthRunning] }
			);

			// for sf case, check that there's a buffer
			if( which == 'soundfile' ) {
				soundfileBuf ?? {
					abort = true;
					this.changed( \status, warn("No Soundfile buffer loaded!") );
					this.changed( \sfSynthRunning, false );
					[ 'inbus', 'soundfile', 'pwNoise', 'diffuseNoise' ].do{ |name| this.stopSynth( name ) };
				};
			};

			if( abort.not ) {
				// must run synth before changing buffer for
				// BufRateScale to work correctly
				synth.run;
				server.sync;
				if( which == 'soundfile' ) {
					soundfileSynth.set(\buffer, soundfileBuf);
					server.sync;
				};
				synth.set(\gate, 1);
				this.changed(tag, true);
				// stop the other synths - ony 1 plays at a time
				[ 'inbus', 'soundfile', 'pwNoise', 'diffuseNoise' ].do({
					|name|
					if( name != which, { this.stopSynth( name ) })
				});
			};

		}, AppClock );
	}


	stopSynth { |which|
		var synth, tag;

		[ 'inbus', 'soundfile', 'pwNoise', 'diffuseNoise' ].includes(which).not.if{
				error( "stopSynth didn't find that synth tag" ) };

		#synth, tag = switch( which,
			'inbus',        { [inbusSynth, \inbusSynthRunning] },
			'soundfile',    { [soundfileSynth, \sfSynthRunning] },
			'pwNoise',        { [pwSynth, \pwSynthRunning] },
			'diffuseNoise', { [diffSynth, \diffSynthRunning] }
		);

		// if( synth.notNil and: synth.isRunning ) {
		if( synth.notNil ) {
			fork ({
				synth.release;
				synthReleaseTime.wait;

				// in case play was pressed again before releaseTime,
				// check that gate is still closed
				synth.get(\gate, { |gate|
					gate.asBoolean.not.if{ {
						synth.run(false);
						server.sync;
						this.changed( tag, false );
					}.fork(AppClock) };
				});
			}, AppClock);
		}
	}


	loadSoundfile { |soundfilePath, loadCondition|
		var sf,test, numChans, newBuf, bufLoadCond = Condition();

		// for some reason for the buffer to load correctly, it needs to be in
		// a separate routine from the routine below it, so sync'd with bufLoadCond

		fork {
			var msg;
			test = PathName( soundfilePath ).isFile;
			if( test ) {
				sf = SoundFile.new;
				if( sf.openRead( soundfilePath ) ) {
					numChans = sf.numChannels;
					sf.close;
					if( (numChans != 3) and: (numChans != 4) ) {
						test = false;
						warn( "Selected file must be 3 or 4 channels for first order playback. Yours is " ++ numChans );
					};
				} { test = false };
			};

			if(test.not) {loadCondition !? {loadCondition.test_(true).signal}};

			msg = "loading file...".post;
			defer {this.changed( \status, msg)};

			newBuf = Buffer.cueSoundFile(
				server, soundfilePath, 0, numChans,  vdiskinBufSize, {
					|buf|
					bufLoadCond.test_(true).signal;
					postf("soundfile loaded:\n\t%\n", buf.path.asString);
				}
			);
		};

		fork {
			block { |break|
				var currentlyPlaying;

				server.sync;
				currentlyPlaying = soundfileSynth.isRunning;
				if( currentlyPlaying ) {
					soundfileSynth.release;
					server.sync;
					synthReleaseTime.wait;
				};
				// make sure the buffer is loaded
				bufLoadCond.wait;

				soundfileBuf !? { soundfileBuf.free };
				server.sync;
				// re-assign soundfileBuf to the newly-loaded buffer
				soundfileBuf = newBuf;

				soundfileSynth = switch( numChans,
					3, { sfSynth_4ch.run(false); sfSynth_3ch },
					4, { sfSynth_3ch.run(false); sfSynth_4ch }
				);
				server.sync;

				// TODO: should start a new soundfileSynth so the VDiskIn buffer pointer starts at 0!
				// otherwise you'll have to silence the beginning of the synth onset for about a
				// VDiskIn buffer length every time it's run with a new buffer. boooooo
				// Only play now if it was previously playing when new buffer was loaded
				if( currentlyPlaying, {
					soundfileSynth.run;
					server.sync;
					soundfileSynth.set(\buffer, soundfileBuf);
					server.sync;
					soundfileSynth.set(\gate, 1);
				});

				// "loadSoundfile complete".postln;
				defer{ this.changed(\buffer, PathName( soundfilePath ).fileName ) };
				loadCondition !? {loadCondition.test_(true).signal};
			}
		};
	}


	outbus_ { |busnum|
		outbus = busnum;
		matrixFader !? { matrixFader.synth.set(\outbus, outbus) };
		this.changed(\outbus, busnum);
	}


	inbus_ { |busnum|
		inbus = busnum;
		inbusSynth.set(\inbus, inbus);
		this.changed(\inbus, inbus);
	}


	mul_ { |mul|
		gMul = mul;
		matrixFader.mul_(gMul);
		this.changed(\mul, gMul);
	}


	pwAzim_ { |azimRad|
		pwSynth.set(\rotating, 0);
		pwSynth.set(\azimReset, azimRad);   // reset panning phasor new startPos
		pwSynth.set(\t_azim, 1 );           // reset panning phasor to startPos
		this.changed(\pwAzim, azimRad);
	}


	pwEl_ { |elRad|
		pwSynth.set(\tumbling, 0);
		pwSynth.set(\elReset, elRad);   // reset tumbling phasor new startPos
		pwSynth.set(\t_elev, 1 );       // reset panning phasor to startPos
		this.changed(\pwElev, elRad);
	}


	diffRtt_ { |boolInt|
		diffSynth.set(\rtt, boolInt.asInt);
		this.changed(\rtt, boolInt);
	}


	diffRttFreq_ { |freq|
		diffSynth.set(\rttFreq, freq);
		this.changed(\rttFreq, freq);
	}


	prepareAuditioning { | completeCond |
		fork{
			var addAct, targ, numchans, mtxFaderLoadCond = Condition(false), dummyLoad, dummyBuf;

			server = server ?? Server.default;
			addAct = addAction ?? {\addToHead};
			targ = target ?? {1};
			xformMatrix ?? { xformMatrix = Matrix.newIdentity( 4 ) };

			server.waitForBoot({
				group = Group.new(targ, addAct);
				server.sync;

				// this will start the server if needed
				matrixFader = FoaMatrixFade(
					outbus, nil, xformMatrix, xFade,
					addAction: \addToTail, target: group, server: server,
					completeCond: mtxFaderLoadCond
				);
			});

			mtxFaderLoadCond.wait;

			FoaAudition.auditionSynthDefs.isNil.if{
				FoaAudition.loadSynths(server);
			};
			server.sync;

			dummyLoad = Condition();
			dummyBuf = Buffer.alloc(server, 65536, 4, {dummyLoad.test_(true).signal});
			dummyLoad.wait;

			inbus ?? {
				// assign internally to free if created
				internalInbus = server.audioBusAllocator.alloc( 4 );
				inbus = internalInbus;
			};

			pwSynth = Synth.newPaused( \FoaAudition_foaPanNoise,
				[    'outbus', matrixFader.inbus,
					'rotating', 1, 'rotfreq', 0.1,
					'tumbling', 0, 'tumfreq', 0.1,
					'pulsed', 1, 'pulsefreq', 3,
					'releaseTime', synthReleaseTime,
					'gate', 0  // initialized with gate closed
				],
				group, \addToHead
			);

			sfSynth_3ch = Synth.newPaused( \FoaAudition_foaSoundfile_3ch,
				[    'outbus', matrixFader.inbus,
					'buffer', dummyBuf,
					// 'buffer', ,    // set by .loadSoundfileBuffer
					'releaseTime', synthReleaseTime,
					'gate', 0
				],
				group, \addToHead
			);

			sfSynth_4ch = Synth.newPaused( \FoaAudition_foaSoundfile_4ch,
				[    'outbus', matrixFader.inbus,
					'buffer', dummyBuf,
					// 'buffer', ,    // set by .loadSoundfileBuffer
					'releaseTime', synthReleaseTime,
					'gate', 0
				],
				group, \addToHead
			);

			inbusSynth = Synth.newPaused( \FoaAudition_foaInbus,
				[   'outbus', matrixFader.inbus,
					'inbus', inbus,
					'releaseTime', synthReleaseTime,
					'gate', 0
				],
				group, \addToHead
			);

			diffSynth = Synth.newPaused( \FoaAudition_foaDiffuseNoise,
				[   'outbus', matrixFader.inbus,
					'releaseTime', synthReleaseTime,
					'gate', 0
				],
				group, \addToHead
			);

			server.sync;
			// register to respond to isRunning/isPlaying
			[pwSynth, sfSynth_3ch, sfSynth_4ch, inbusSynth, diffSynth].do(_.register);
			soundfileSynth = sfSynth_4ch; // initialize
			auditionEnabled = true;
			completeCond !? { completeCond.test_(true).signal };
		}
	}


	*loadSynths { |server|
		auditionSynthDefs = Dictionary.new.putPairs([

			\FoaAudition_foaDiffuseNoise,
			SynthDef( \FoaAudition_foaDiffuseNoise, {
				arg outbus, gate = 1, mul = 1, releaseTime = 0.5, rttFreq=0.333, rtt=0;
				var env, mtx, foa, rttfrq;

				env = EnvGen.kr(Env([0,1,0],[0.1,releaseTime], \sin, 1), gate);
				mtx = FoaEncoderMatrix.newAtoB;
				foa = FoaEncode.ar( PinkNoise.ar(-3.dbamp.dup(4)), mtx, mul ) * env;
				rttfrq = rttFreq * rtt;
				foa = FoaRTT.ar(foa,
					LFDNoise3.kr(rttfrq, 2pi),
					LFDNoise3.kr(rttfrq, 2pi),
					LFDNoise3.kr(rttfrq, 2pi) );
				Out.ar(outbus, foa * 0.5); // -6dB output
			}).load(server),

			\FoaAudition_foaSoundfile_3ch,
			SynthDef( \FoaAudition_foaSoundfile_3ch, {
				arg outbus, buffer, mul = 1, gate = 1, releaseTime = 0.5;
				var env, foa;

				env = EnvGen.kr(Env([0,1,0],[0.1,releaseTime], \sin, 1), gate);
				foa = VDiskIn.ar(3, buffer, BufRateScale.kr(buffer), 1);
				Out.ar(outbus, foa * mul * env);
			}).load(server),

			\FoaAudition_foaSoundfile_4ch,
			SynthDef( \FoaAudition_foaSoundfile_4ch, {
				arg outbus, buffer, mul = 1, gate = 1, releaseTime = 0.5;
				var env, foa;

				env = EnvGen.kr(Env([0,1,0],[0.1,releaseTime], \sin, 1), gate);
				foa = VDiskIn.ar(4, buffer, BufRateScale.kr(buffer), 1);
				Out.ar(outbus, foa * mul * env);
			}).load(server),

			\FoaAudition_foaInbus,
			SynthDef( \FoaAudition_foaInbus, {
				arg outbus, inbus, mul = 1, gate = 1, releaseTime = 0.5;
				var env, foa;

				env = EnvGen.kr(Env([0,1,0],[0.1,releaseTime], \sin, 1), gate);
				foa = In.ar(inbus, 4) * mul * env;
				Out.ar(outbus, foa);
			}).load(server),

			\FoaAudition_foaPanNoise,
			SynthDef( \FoaAudition_foaPanNoise, {
				arg outbus, rotating=1, rotfreq=0.1, tumbling=0, tumfreq=0.1,
				pulsed=1, pulsefreq=3, mul=0.5, gate=1, releaseTime=0.5,
				t_azim=0, t_elev=0, azimReset=0, elReset;
				var env, lagTimeU, lagTimeD, src, foa, azim, elev, normRate;

				env = EnvGen.kr(Env([0,1,0],[0.1,releaseTime], \sin, 1), gate);

				lagTimeU = 0.001;
				lagTimeD = pulsefreq.reciprocal * 0.6;

				src = PinkNoise.ar(
					// amplitude:
					SelectX.kr(
						Lag.kr(pulsed, 0.3),
						[
							Lag.kr(mul),    // steady state noise
							Lag2UD.kr(      // pulsed noise
								LFSaw.kr(pulsefreq, 1, -1).range(0, mul),
								lagTimeU, lagTimeD
							)
						]
					)
				) * env;
				normRate = 2pi * ControlRate.ir.reciprocal;
				azim = Phasor.kr(t_azim, rotfreq * rotating * normRate, -pi, pi, azimReset);
				elev = Phasor.kr(t_elev, tumfreq * tumbling * normRate, -pi, pi, elReset);
				elev = elev.fold(-pi/2,pi/2);

				SendReply.kr( Impulse.kr(24) * rotating, '/audition_az', azim ); // for XformDisplay
				SendReply.kr( Impulse.kr(24) * tumbling, '/audition_el', elev );

				foa = FoaPanB.ar( src, azim, elev );
				Out.ar( outbus, foa );
			}).load(server)
		]);

	}


	gui { |show = true|
		ui = FoaAuditionView( this, show );
	}


	free {
		auditionEnabled = false;
		this.changed( \closed );
		[   group, soundfileBuf,
			OSCdef( \azResponder ),
			OSCdef( \azResponder_request )
		].do{ |me| me !? {me.free} };

		internalInbus !? { server.audioBusAllocator.free( inbus ) };
		ui !? { ui.free };
	}
}


FoaAuditionView {
	// copyArgs
	var audition, show;
	// GUI layout
	var scrnB, winW, winH, <win, playerLayout, <view, <scope;
	var ctlViews, tabViews, <palette, playColor, paramBkgColor, stopColor, focusBkgColor, paramHeaderTxtColor;
	// GUI controls
	var ampSpec, azimSpec, elSpec, ampSl, ampBx, outbusBx;
	var pwPlayBut, sfPlayBut, pulsedChk, pulseBx, rotChk, tumChk, rotPerBx, tumPerBx, azimBx, azimSl, elBx, elSl;
	var inPlayBut, inbusBx, loadBut, fileTxt, playSymbol, addXformBut, xformDisplay;
	var diffPlayBut, diffRttChk, diffRttBx, globalCtlView, tabContainerView;


	*new { |anFoaAudition, show = true|
		^super.newCopyArgs( anFoaAudition, show ).init;
	}


	init {
		audition.addDependant( this );
		playSymbol = [226, 150, 182].asAscii;
		this.defineColors;
		this.initWidgets;
		this.layItOut;
	}


	setPalette {
		var iter;
		win.view.palette_(palette);
		this.findKindDo( win.view, NumberBox,
			{ |bx|
				bx.stringColor_(palette.baseText);
				bx.normalColor_(palette.baseText);
				bx.typingColor_(palette.windowText);
			}
		);
	}


	// find a widget within a view and change something about it
	findKindDo { |view, kind, performFunc|
		view.children.do{|child|
			child.isKindOf(View).if{
				this.findKindDo(child, kind, performFunc)   // call self
			};
			child.isKindOf(kind).if{ performFunc.(child) };
		}
	}


	// scheme copied from FoaXformDisplay
	defineColors {
		palette = QPalette.new
		.window_(Color.hsv(*[242,63,25]/[360,100,100]))
		.windowText_(Color.hsv(*[162,15,62]/[360,100,100]))
		.button_(Color.hsv(*[124,17,76]/[360,100,100]))     // button color
		.buttonText_(Color.hsv(*[242,63,31]/[360,100,100])) // button/dropdown text color
		.base_(Color.hsv(*[225,31,46]/[360,100,100]))       // num/check box background color
		.baseText_(Color.hsv(*[113,21,95]/[360,100,100]))   // check mark color
		.highlight_(Color.hsv(*[124,17,76]/[360,100,100]))  // numbox select outline
		.highlightText_(Color.hsv(*[124,17,76]/[360,100,100]));

		paramBkgColor = Color.hsv(*palette.base.asHSV.put(2,
			(palette.base.asHSV[2] - 0.12).wrap(0,1)));
		paramHeaderTxtColor = palette.baseText;
		focusBkgColor = Color.hsv(*palette.base.asHSV.put(2, (palette.base.asHSV[2] - 0.05).wrap(0,1)));
		playColor = palette.baseText;
		stopColor = paramBkgColor;
	}


	initWidgets {
		/* global controls */
		ampSpec = ControlSpec( -80, 12, warp: 'db', default: 0 );
		azimSpec = ControlSpec( 180, -180, warp: 'lin', default: 0 );
		elSpec = ControlSpec( -90, 90, warp: 'lin', default: 0 );

		outbusBx = NumberBox().action_({ |bx|
			audition.outbus_( bx.value );
			scope !? {scope.setIndex( bx.value )};
		})
		.step_(1).align_(\center)
		.value_( audition.outbus );

		ampSl = Slider()
		.action_({|sl|
			audition.mul_( ampSpec.map( sl.value ).dbamp )
		}).value_( ampSpec.unmap( audition.gMul.ampdb ) );

		ampBx = NumberBox()
		.action_({ |bx|
			audition.mul_( bx.value.dbamp )
		})
		.decimals_(1)
		.align_(\center)
		.value_( audition.gMul.ampdb )
		;

		/* noise panner controls */
		pwPlayBut = StaticText()
		.string_(playSymbol)
		.stringColor_(stopColor)
		.mouseDownAction_({ |but|
			audition.pwSynth.get(\gate, { |gate|
				if (gate.asBoolean)
				{    audition.stopSynth( 'pwNoise' );
					defer {but.stringColor_(stopColor)};
				}{     audition.playSynth( 'pwNoise' );
					defer {but.stringColor_(playColor)};
				};
			});
		});

		pulsedChk = CheckBox()
		.action_({ |me|
			audition.pwSynth.set( \pulsed, me.value.asInt );
		});
		audition.pwSynth.get(\pulsed, { |val|
			defer {pulsedChk.value_( val )};
		})
		;
		pulseBx = NumberBox()
		.action_({ |bx|
			audition.pwSynth.set( \pulsefreq, bx.value );
			pulsedChk.valueAction_(1);
		})
		.align_(\center)
		;
		audition.pwSynth.get( \pulsefreq, { |val|
			defer {pulseBx.value_( val )};
		});

		rotChk = CheckBox()
		.action_({ |me|
			var val = me.value.asInt;
			audition.pwSynth.set( \rotating, val );
		});

		audition.pwSynth.get( \rotating, { |val|
			defer {rotChk.value_( val.asInt )};
		});

		tumChk = CheckBox()
		.action_({ |me|
			var val = me.value.asInt;
			audition.pwSynth.set( \tumbling, val );
		});

		audition.pwSynth.get( \tumbling, { |val|
			defer {tumChk.value_( val.asInt )};
		});

		rotPerBx = NumberBox()
		.action_({ |me|
			(me.value == 0).if(
				{error("Rotation period cannot be zero, that's infinite speed.")},
				{
					audition.pwSynth.set( \rotfreq, me.value.reciprocal );
					rotChk.valueAction_( 1 );
				}
			);
		})
		.align_(\center);

		audition.pwSynth.get(\rotfreq, { |val|
			defer {rotPerBx.value_( val.reciprocal )};
		});

		tumPerBx = NumberBox()
		.action_({ |me|
			(me.value == 0).if(
				{error("Rotation period cannot be zero, that's infinite speed.")},
				{
					audition.pwSynth.set( \tumfreq, me.value.reciprocal );
					tumChk.valueAction_( 1 );
				}
			);
		})
		.align_(\center);

		audition.pwSynth.get(\tumfreq, { |val|
			defer {tumPerBx.value_( val.reciprocal )};
		});

		azimBx = NumberBox()
		.action_({ |me|
			audition.pwAzim_( me.value.degrad );
			rotChk.value_(0);
		})
		.decimals_(1)
		.align_(\center);

		azimSl = Slider().action_({ |me|
			var val = azimSpec.map( me.value ).degrad;
			audition.pwAzim_(val);
			rotChk.value_(0);
		});

		elBx = NumberBox().action_({ |me|
			audition.pwEl_( me.value.degrad );
			tumChk.value_(0);
		})
		.decimals_(1)
		.align_(\center);

		elSl = Slider()
		.action_({ |me|
			var val = elSpec.map( me.value ).degrad;
			audition.pwEl_(val);
			tumChk.value_(0);
		})
		.value_(elSpec.unmap(elSpec.default))
		;

		audition.pwSynth.get(\az, { |val|
			defer {
				var deg = val.raddeg;
				azimBx.value_( deg.round(0.1) );
				azimSl.value_( azimSpec.unmap(deg.wrap(-180,180)) );
			};
		});

		sfPlayBut = StaticText()
		.string_(playSymbol)
		.stringColor_(stopColor)
		.mouseDownAction_({ |but|
			var synth;

			synth = if (audition.sfSynth_4ch.isRunning) {
				audition.sfSynth_4ch;
			} {
				if (audition.sfSynth_3ch.isRunning)
				{audition.sfSynth_3ch}
				{nil}
			};

			if (synth.notNil) {
				// found a running synth, switch it's state
				synth.get(\gate, { |gate|
					if (gate.asBoolean) {
						audition.stopSynth( 'soundfile' );
						defer {but.stringColor_(stopColor)};
					} {
						audition.playSynth( 'soundfile' );
						defer {but.stringColor_(playColor)};
					};
				});
			} {
				// found a no running synth, start one
				audition.playSynth( 'soundfile' );
			};
		});

		diffPlayBut = StaticText()
		.string_(playSymbol)
		.stringColor_(stopColor)
		.mouseDownAction_({ |but|
			audition.diffSynth.get(\gate, { |gate|
				if (gate.asBoolean) {
					audition.stopSynth( 'diffuseNoise' );
					defer {but.stringColor_(stopColor)};
				}{
					audition.playSynth( 'diffuseNoise' );
					defer {but.stringColor_(playColor)};
				};
			});
		});

		inPlayBut = StaticText()
		.string_(playSymbol)
		.stringColor_(stopColor)
		.mouseDownAction_({ |but|
			audition.inbusSynth.get(\gate, { |gate|
				if (gate.asBoolean) {
					audition.stopSynth( 'inbus' );
					defer {but.stringColor_(stopColor)};
				}{
					audition.playSynth( 'inbus' );
					defer {but.stringColor_(playColor)};
				};
			});
		});

		inbusBx = NumberBox()
		.action_({ |me|
			audition.inbus_( me.value );
		})
		.decimals_(0).scroll_step_(1).step_(1)
		.align_(\center)
		;

		loadBut = Button()
		.states_([["Load"]])
		.action_({ |but|
			fork({
				var loadCond = Condition();

				Dialog.openPanel({ |path|
					audition.loadSoundfile( path, loadCond );
				});
				loadCond.wait;
			}, AppClock)
		})
		;
		fileTxt = StaticText();

		diffRttChk = CheckBox()
		.action_({|bx| audition.diffRtt_(bx.value.asInt) });

		audition.diffSynth.get(\rtt, { |val|
			defer {diffRttChk.value_( val.asBoolean )};
		});

		diffRttBx = NumberBox()
		.action_({|bx| audition.diffRttFreq_(bx.value) })
		.step_(0.05).scroll_step_(0.05).decimals_(2);

		audition.diffSynth.get(\rttFreq, { |val|
			defer {diffRttBx.value_( val )};
		});

		addXformBut = Button().states_([["Add Transform"]])
		.action_({
			// check that the audition isn't already talking to an FoaXformDisplay
			audition.dependants.collect(_.class).includes(FoaXformDisplay).not.if{
				xformDisplay = FoaXformDisplay(16);
				xformDisplay.setAudition(audition);
			}
		})
	}


	buildCtlViewFmLayout { |layout|
		^ View()
		.background_( focusBkgColor )
		.layout_(
			HLayout(
				// background that matches the tab
				View().layout_( layout.margins_(4).spacing_(3) )
			).margins_([2,0,2,0])
		)
	}


	makeTabView { |name, ctlView, playBut|
		var thisTabView, labelTxt;
		thisTabView = View().layout_(
			HLayout(
				labelTxt = StaticText().string_(name).align_(\left).fixedHeight_(30),
				playBut.isKindOf(StaticText).if({playBut},{nil})
			).margins_(3)
		)
		.mouseDownAction_({
			ctlView.visible.not.if{
				// "deselect" other views and show this one
				tabViews.do{|v| if(v!=thisTabView) {
					v.background_(
						Color.hsv(*palette.window.asHSV.put(1,
							(palette.window.asHSV[1] - 0.2).wrap(0,1))) );
					this.findKindDo( v, StaticText,
						{|txt| if(txt.string != playSymbol){txt.stringColor_(palette.windowText)}}
					);
				}};
				// "select" this view
				thisTabView.background_(focusBkgColor);
				labelTxt.stringColor_(palette.baseText);
				ctlViews.do{|v| if(v != ctlView) {v.visible_(false)} };
				ctlView.visible_(true);
		} });

		^thisTabView
	}


	buildParamView { |name, layout|
		^View()
		.background_(paramBkgColor)
		.layout_(
			VLayout(
				StaticText().string_(name) // parameter name/title
				.stringColor_(paramHeaderTxtColor)
				.align_(\center),
				layout,
				nil
			).margins_(3)
		)
	}


	layItOut {
		var pwTabView, sfTabView, diffTabView, inTabView,
		pwView, sfView, diffView, inView,
		ctlContainerView, ctlContainerLayout;

		ctlViews = [];
		tabViews = [];

		playerLayout = VLayout().spacing_(0).margins_(3);

		// if show is false, a view is created that can be
		// embedded in another window
		if( show ) {
			scrnB = Window.screenBounds;
			winW= 410;
			winH= 50;// make it small, let widgets grow it
			win = Window( "Audition a Soundfield",
				Rect( (scrnB.width + winW).half, scrnB.height / 3, winW, winH )
			);

			// this will in turn free me
			win.onClose_({ audition.free; xformDisplay !? {xformDisplay.free} });
			win.view.layout_( playerLayout );
			win.front;
		}{
			view = View().layout_( playerLayout );
		};

		// build planewave noise view
		pwView = this.buildCtlViewFmLayout(
			HLayout(
				this.buildParamView( "Envelope",
					VLayout(
						HLayout(
							[pulsedChk.maxWidth_(20), a: \left],
							[StaticText().string_("Pulsed").align_(\left), a: \left],
						),
						HLayout(
							[StaticText().string_("Freq").align_(\right), a: \right],
							[pulseBx.maxWidth_(25).fixedHeight_(18), a: \right],
						), nil,
					).margins_(0)
				),

				this.buildParamView( "Azimuth",
					HLayout(
						VLayout(
							HLayout(
								[rotChk.maxWidth_(20), a: \left],
								[StaticText().string_("Rotating").maxHeight_(25), a: \left],
							),
							HLayout(
								[rotPerBx.maxWidth_(25).fixedHeight_(18), a: \right],
								[StaticText().string_("Period ").align_(\left), a: \left],
							), nil
						),
						VLayout(
							// [StaticText().string_("Degree").align_(\center), a: \center],
							[azimBx.fixedWidth_(40).fixedHeight_(18), a: \center],
							azimSl.orientation_('horizontal').fixedHeight_(18),
						)
					).margins_(0)
				),

				this.buildParamView( "Elevation",
					HLayout(
						VLayout(
							HLayout(
								[tumChk.maxWidth_(20), a: \left],
								[StaticText().string_("Tumbling").maxHeight_(25), a: \left],
							),
							HLayout(
								[tumPerBx.maxWidth_(25).fixedHeight_(18), a: \left],
								[StaticText().string_("Period").align_(\left), a: \left],
								nil
							), nil
						),
						VLayout(
							// [StaticText().string_("Degree").align_(\center), a: \center],
							[elBx.fixedWidth_(40).fixedHeight_(18), a: \center],
							elSl.orientation_('horizontal').fixedHeight_(18),
						)
					).margins_(0)
				),
				nil
			).margins_(0)
		);

		// give it a layout, returns a view
		sfView = this.buildCtlViewFmLayout(
			HLayout(
				this.buildParamView("Choose a Soundfile",
					VLayout(
						[loadBut.fixedWidth_(50).maxHeight_(20), a: \center ],
						[    fileTxt
							.string_("None Selected")
							// .stringColor_(Color.black.alpha_(0.5))
							.align_(\center).minWidth_(340),
							a: \center ],
						nil,
					).margins_(2)
				)
			)
		);

		diffView = this.buildCtlViewFmLayout(
			HLayout(
				this.buildParamView("Diffuse Soundfield",
					HLayout(
						nil,
						diffRttChk,
						StaticText().string_("RTT").align_(\left),
						15,
						diffRttBx.fixedWidth_(40).fixedHeight_(18),
						StaticText().string_("LFO Freq").align_(\left),
						nil
					).margins_(2)
				)
			)
		);

		inView = this.buildCtlViewFmLayout(
			HLayout(
				this.buildParamView("Read in from a Bus",
					HLayout(
						nil,
						inbusBx.maxWidth_(25).fixedHeight_(18),
						StaticText().string_("Bus number").align_(\left),
						nil
					).margins_(2)
				)
			)
		);

		// build source tab container view
		tabContainerView = View().minWidth_(185).maxHeight_(40)
		.layout_(HLayout(
			// src tab views
			pwTabView = this.makeTabView("Planewave", pwView, pwPlayBut),
			sfTabView = this.makeTabView("Soundfile", sfView, sfPlayBut),
			diffTabView = this.makeTabView("Diffuse", diffView, diffPlayBut),
			inTabView = this.makeTabView("Inbus", inView, inPlayBut),
			nil,
			addXformBut
		).spacing_(8).margins_([0,4,2,0])
		);

		// global controls view
		globalCtlView = View()
		.background_(focusBkgColor)
		.maxHeight_(55)
		.layout_(
			HLayout(
				View().layout_(
					HLayout(
						View()
						.background_(paramBkgColor)
						.layout_(
							HLayout(
								[StaticText().string_("Gain")
									.align_('left').stringColor_(paramHeaderTxtColor),
									a: \center ],
								[ampBx.fixedWidth_(45), a: \center],
								[ampSl.orientation_('horizontal').minWidth_(285), a: \left],
							).margins_(0),
						),

						View()
						.background_(paramBkgColor)
						.layout_(
							VLayout(
								[StaticText().string_("Outbus")
									.stringColor_(paramHeaderTxtColor)
									.align_(\center) ],
								[outbusBx.maxWidth_(25), a: \center],
							).spacing_(2).margins_(2)
						).maxWidth_(50),
						nil,
					).spacing_(2).margins_([6,4,6,4])
				)
			).margins_(0)
		);

		// // scope
		// scope = FoaSignalScope(
		// audition.outbus, makeWindow: false, server: audition.server);
		// // give the scope background a color,
		// // also see .scopeView.waveColors
		// scope.scopeView.background_( Color.hsv(
		// 1/3, 0.89183673469387, 0.36862745098039, 0.65));

		// lay out view in the window...
		playerLayout.add(tabContainerView);

		// add all the control views, set them default invisible
		// order determins display order
		ctlContainerView = View();
		ctlContainerLayout = VLayout().margins_(0).spacing_(0);
		ctlContainerView.layout_(ctlContainerLayout);
		[pwView, sfView, diffView, inView].do{|v, i|
			if(i==0){v.visible = true}{v.visible = false}; // first view visible
			v.minWidth_(335);
			ctlViews = ctlViews.add(v);    // for "muting" views on selecting tab
			ctlContainerLayout.add(v);
		};
		ctlContainerView.maxHeight_(85);
		playerLayout.add(ctlContainerView);

		[pwTabView, sfTabView, diffTabView, inTabView].do{|v, i|
		tabViews = tabViews.add(v) };

		playerLayout.add(3);          // gap
		playerLayout.add(globalCtlView);
		playerLayout.add(3);          // gap
		playerLayout.add(nil);        // prevent from spacing various views on resize
		pwTabView.mouseDownAction.(); // perform the mouse down action to focus the tab
		this.setPalette;
	}


	free {
		audition.removeDependant( this );
		win !? { win.isClosed.not.if{ win.close } };
	}


	update {
		| who, what ... args |

		if( who == audition, {
			switch ( what,
				\buffer, {
					fileTxt.string_( args[0] );
				},
				\pwSynthRunning, {
					pwPlayBut.stringColor = if (args[0]) {playColor}{stopColor};
				},
				\sfSynthRunning, {
					sfPlayBut.stringColor = if (args[0]) {playColor}{stopColor};
				},
				\inbusSynthRunning, {
					inPlayBut.stringColor = if (args[0]) {playColor}{stopColor};
				},
				\diffSynthRunning, {
					diffPlayBut.stringColor = if (args[0]) {playColor}{stopColor};
				},
				\mul, {
					var db = args[0].ampdb;
					ampSl.value_(ampSpec.unmap( db ));
					ampBx.value_( db );
				},
				\rttFreq, {
					diffRttBx.value_(args[0]);
				},
				\rtt, {
					diffRttChk.value_(args[0].asBoolean);
				},
				\pwAzim, {
					var deg = args[0].raddeg;
					defer {
						azimBx.hasFocus !? { // sometimes .hasFocus can return nil (race condition?)
							azimBx.hasFocus.not.if{azimBx.value_( deg )};
						};
						azimSl.value_( azimSpec.unmap(deg) );
					};
				},
				\pwElev, {
					var deg = args[0].raddeg;
					defer {
						elBx.hasFocus !? { // sometimes .hasFocus can return nil (race condition?)
							elBx.hasFocus.not.if{elBx.value_( deg )};
						};
						elSl.value_( elSpec.unmap(deg) );
					};
				},
				\status, {
					fileTxt.string_( args[0] );
				},
			)
		})
	}
}
