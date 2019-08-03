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
// 	Class: FoaXformDisplay
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

FoaXformDisplay {
	// copyArgs
	var numPoints;
	var <>debug = false, planewaveMatrices, transformedPlanewaves, aeds, <prePostStats;
	var <chain, <displayChain, <xfViewChains, displayXFormView;

	// GUI layout - vars used by FoaXformView need getters
	var scrnB, sfWin, winW, winH, uv, pv, tv, ctlv, matrixButtonLayout, <codeWin;
	var xfWin, <chainViews, lastUpdatedMatrix, dirDisplay, <evalTxtView;
	var <xfMargins, <addRmvMargins, <chainViewWidth, <chainViewHeight, xfHeight, chTitleHeight, xfIDwidth;
	var <idTxtColor, <idTabColor, <xfColor, <ctlColor, <colorStep, chainColor, baseColor;
	var pointRad, selectedDex;

	// soundfield player - for auditioning the soundfield transforms
	var <audition, >iOpenedAudition=true, <pwPlaying=false, <pwAzim=0;


	*new { | numDisplayPoints = 12 |
		^super.newCopyArgs(numDisplayPoints).prInit;
	}


	prInit {
		var directions;

		xfMargins =         [5,3,5,3];
		xfHeight =          80;
		chainViewWidth =    500;
		chainViewHeight =   200;
		chTitleHeight =     40;
		addRmvMargins =     [4,2,4,2];
		xfIDwidth =         20;

		this.prDefineColors;

		directions = numPoints.collect{ |i| (2pi / numPoints) * i };

		// planewave "point" matrices collected
		planewaveMatrices = numPoints.collect{ |i|
			FoaEncoderMatrix.newDirection(
				directions[i]
			).matrix.addRow([0]); // need to add z manually, for planewaves with no elev
		};

		// store tranform info for each "probe" point
		prePostStats = numPoints.collect{ |i|
			IdentityDictionary(know: true).putPairs([
				\inAz, directions[i],
				\inEl, 0,
				\az, nil, \el, nil, \dir, nil, \ampdb, nil,
				\scrnPnt, nil // point in userview
			]);

		};

		// a list containing a master view of each chain
		chainViews = List();
		// a list for FoaXFormViews
		xfViewChains = List();

		// init display chain before init gui
		displayChain = FoaMatrixChain();
		// intialize a matrix transform chain
		chain = FoaMatrixChain();

		displayChain.addDependant(this);

		this.initDisplayGUI;
	}


	prDefineColors {
		baseColor = Color.hsv( // Color.newHex("BA690B").asHSV;
			0.08952380952381, 0.94086021505376, 0.72941176470588, 1);

		idTxtColor = Color.hsv(
			*baseColor.asHSV
			.put(2, (baseColor.asHSV[2] * 1.8).clip(0,1))
			.put(1, baseColor.asHSV[1] * 0.2));

		idTabColor = Color.hsv(
			*baseColor.asHSV
			.put(2, (baseColor.asHSV[2] * 1.2).clip(0,1))
			.put(1, baseColor.asHSV[1] * 0.85));

		chainColor = Color.hsv(
			*baseColor.asHSV
			.put(2, (baseColor.asHSV[2] * 0.7).clip(0,1)));

		xfColor = Color.hsv(
			*baseColor.asHSV.put(2,baseColor.asHSV[2] * 0.3));

		ctlColor = baseColor;
		colorStep = 0.03;
	}


	free {
		displayChain.removeDependant(this);
		chain.removeDependant(this);

		audition !? {
			iOpenedAudition.if({
				audition.ui.notNil.if({
					audition.ui.win.isClosed.not.if{
						// this frees audition
						audition.ui.win.close
					}
				}, {
					audition.removeDependant(this);
					audition.free;
				})
			}, {
				audition.removeDependant(this);
			});
		};

		[sfWin, xfWin, codeWin].do{ |win|
			win !? { win.isClosed.not.if{ win.close } }
		};
	}


	initDisplayGUI {
		scrnB = Window.screenBounds;
		winW= 600;
		winH= 450;
		dirDisplay = 'size';

		// create the transform view, index [0][1] in displayChain
		displayXFormView = FoaXformView(this, 'display', 0, 1);

		// initialize aeds for display before drawing
		this.prUpdateMatrix('display');
		// init var
		lastUpdatedMatrix = 'display';

		sfWin = Window("Soundfield Transform",
			Rect(scrnB.center.x - (winW / 2), scrnB.height - winH - 45, winW, winH),
			resizable: true
		).onClose_(
			{ this.free; }
		).front;

		// view containing the soundfield representation
		uv = UserView(sfWin)
		.resize_(5)
		.maxWidth_(2000).minWidth_(400)
		.minHeight_(400)
		.background_(Color.black);

		// view containing the soundfield display controls and stats
		ctlv = View(sfWin)
		.resize_(5).maxHeight_(105).background_(chainColor)
		.layout_(VLayout().margins_(5));

		sfWin.view.palette_(
			QPalette.dark
			.setColor(Color.gray, 'base')
			.setColor(Color.gray, 'alternateBase')
		)
		.background_(xfColor)
		.layout_(VLayout(uv, ctlv).margins_(0));

		this.prDefineDrawSoundfield;

		displayXFormView.ctlLayout.insert(
			StaticText().string_("Display Transform").align_('center').maxHeight_(15),
			0
		);

		// init the first xform
		ctlv.layout.add(
			// post / enter matrix buttons on left
			HLayout(
				View()
				.maxWidth_(125).maxHeight_(80)
				.background_(
					Color.hsv(*baseColor.asHSV.put(0, (baseColor.asHSV[0] + 0.045).wrap(0,1)));
				)
				.layout_(
					VLayout(
						Button().states_([["Post Matrix"]]).action_({ |but|
							this.showMatrixWindow(true) }),
						Button().states_([["Enter Matrix"]]).action_({ |but|
							this.showMatrixWindow() }),
					)
				),

				// display transform controls
				displayXFormView.view.background_(ctlColor),

				// chain, audition buttons on right
				View()
				.maxWidth_(125).maxHeight_(80)
				.background_(
					Color.hsv(*baseColor.asHSV.put(0,
						(baseColor.asHSV[0] + 0.045).wrap(0,1)));
				)
				.layout_(
					VLayout(
						Button().states_([["Chain >>"]]).action_({

							xfWin.isNil.if({
								this.initChainGui
							}, {
								xfWin.isClosed.if{
									this.initChainGui
								}
							});
						}),
						Button().states_([["Audition >>"]]).action_({
							var test = false;

							test = audition.isNil.if({
								true
							}, {
								audition.ui.win.isClosed
							});

							test.if({
								fork ({
									var auditionCond = Condition(false);
									audition = FoaAudition(
										0, 0.1, this.curXformMatrix,
										initCond: auditionCond, initGUI: true);
									auditionCond.wait;
									audition.addDependant(this);
								}, AppClock)
							}, {
								audition.ui.win.front
							});

						}),
					)
				),
			)
		);

		sfWin.refresh;
	}


	// set the audition instance externally
	setAudition { |anAudition|
		audition !? { "This FoaXformDisplay has already created an instance of audition!".throw };
		audition = anAudition;
		audition.addDependant(this);
		iOpenedAudition = false;
	}


	initChainGui {

		chain.addDependant(this);

		// window for xform chains
		xfWin = Window("Transform Chain",
			Rect(
				sfWin.bounds.left,
				scrnB.height - sfWin.bounds.height - chainViewHeight - (2 * 20) - 25, // (title bar: 20, OS menu bar: 25)
				chainViewWidth, chainViewHeight),
			resizable: true
		).front;

		xfWin.onClose_({
			chain.removeDependant(this);
			[chainViews, xfViewChains, chain].do(_.clear);
		});

		// set color pallette
		xfWin.view.palette_(QPalette.dark.setColor(
			Color.gray, 'base').setColor(Color.gray, 'alternateBase')
		).background_(xfColor);

		// an HLayout in which to place chain views
		xfWin.view.layout_(HLayout().margins_(5));

		this.addChainView(0);

		// create the xforms for the first 2 transforms in the chain:
		// a soundfield (identity matrix), and thru soundfield
		// this has to happen manually because the matrix chain has already
		// been initialized so this GUI won't catch it's this.changed notifications
		this.createNewXForm(0, 0);
		this.createNewXForm(0, 1);
		chainViews[0].layout.add(nil); // pad end with nil so xforms anchored to top
	}


	prDefineDrawSoundfield {
		var gainThresh, alphaSpec, gainSpec, colorSpec;
		var getColor, elWarp;
		var arcH; // shared by uv and pv
		// for planewave arrow
		var rH, rW, w_2, w_2neg, w_4, h_2, h_4, h_34, whRatio=0.75;

		// below this thresh, the point isn't displayed
		gainThresh = -90;

		alphaSpec = ControlSpec(0.1, 1, warp:5);
		gainSpec = ControlSpec(-90, 6, -2);
		colorSpec = ControlSpec(768,0);

		// warp lower hemisphere toward the origin as points move "down"
		// elWarp = Env([-pi/2,0,pi/2],[pi/2,pi/2], [-3,0]);
		elWarp = Env([0.25,1, 4],[pi/2, pi/2], 2); // 0>1 scalar

		// algorithmic rainbow color scheme
		getColor = { |gain|
			var i;
			i = colorSpec.map(gainSpec.unmap(gain));
			case
			{ i < 256 } { Color.new255(255, i, 0) }
			{ i < 384 } { Color.new255(255 - (i - 256), 255, 0) }
			{ i < 512 } { Color.new255(0, 255, (i - 384) * 2) }
			{ i < 768 } { Color.new255(0, 255 - (i - 512), 255) }
			{ i >= 768 } { Color.new255(0, 0, 255) }; // catch all
		};

		/* Drawing the soundfield transform */

		uv.drawFunc_({ |view|
			var minDim, r, d, cen, circleViewRatio;
			var maxMinStr, azLineClr;
			var azPnt, drawPnt, omniRad, omniDiam, fullOmni, gainColor, gainPnt;
			var az, el, dir, gain, aeds_sortOrder, thisElWarp;
			var inspWidth, inspHeight, inspInset, inspRect;
			var gainWidth, gainHeight, gainRect, minRect, maxRect;
			var amps, rect, col;

			minDim = min(uv.bounds.width, uv.bounds.height);

			pointRad = minDim / 2 * 0.02;
			d = pointRad * 2;
			circleViewRatio = 0.8;
			arcH = minDim * circleViewRatio / 2;
			fullOmni = 2 * arcH;

			cen = view.bounds.center; // center drawing origin

			Pen.translate(cen.x - view.bounds.left, cen.y);
			Pen.addAnnularWedge(0@0, 5, arcH, 0, 2pi);
			Pen.fillColor_(Color.gray(0.9)).fill;

			// background circles
			Pen.strokeColor_(Color.gray.alpha_(0.2));
			[0.0, 0.5236, 1.0472].do{ |el|
				var val;
				// val = cos(i + 1 / 3);
				val = cos(el);
				Pen.strokeOval(Rect(
					(arcH * val).neg, (arcH * val).neg,
					arcH * 2 * val, arcH * 2 * val
				));
			};

			// line from center to point
			azLineClr = Color.gray.alpha_(0.2);

			// get sort order by directivity (arr[2]) to draw most transparent first
			aeds_sortOrder = aeds.collect({ |arr| arr[2] }).order;

			aeds_sortOrder.do{ |sortDex, i|

				#az, el, dir, gain = aeds[sortDex];

				// warp elevation for visual scaling
				thisElWarp = elWarp.at(el + pi/2);

				(gain > gainThresh).if{
					omniDiam = 1 - dir * fullOmni;
					omniDiam = omniDiam.clip(d, fullOmni);
					omniDiam = omniDiam * thisElWarp; // warp for elevation perspective
					omniRad =  omniDiam / 2;

					gainColor = getColor.(gain);

					// cartesian point in view coordinates
					azPnt = Point(cos(az), sin(az)) // = Polar(dir, az).asPoint
					.rotate(pi/2)   // convert ambi to screen coords
					* Point(1,-1)   // flip Y for drawing
					* arcH;         // scale normalized points to arcH

					drawPnt = azPnt * cos(el) * thisElWarp;
					drawPnt = drawPnt * dir;    // scale radius by elevation

					// update screen point in dictionary for mouseDown check
					prePostStats[sortDex].scrnPnt = drawPnt;

					// line from center to point
					Pen.strokeColor_(azLineClr);
					Pen.line(drawPnt, 0@0).stroke;

					// directivity circle
					switch(dirDisplay,
						'size',{
							Pen.fillColor_(gainColor.alpha_(alphaSpec.map(dir)));
							Pen.fillOval(Rect(
								drawPnt.x - omniRad, drawPnt.y - omniRad, omniDiam, omniDiam));

							// center point for mouse click guidance to cue stats
							Pen.fillColor_(
								(sortDex == selectedDex).if({ Color.yellow }, { azLineClr })
							);
							Pen.fillOval(Rect(
								drawPnt.x - (pointRad * 0.5), drawPnt.y - (pointRad * 0.5),
								pointRad, pointRad));

							// outline the selected directivity circle
							(sortDex == selectedDex).if{
								Pen.strokeColor_(Color.yellow);
								Pen.strokeOval(Rect(
									drawPnt.x - omniRad, drawPnt.y - omniRad,
									omniDiam, omniDiam));
							};

							// scale in/out toward/away from origin
							gainPnt = azPnt * 1.15;
						},
						'radius', {
							Pen.fillColor_(gainColor);
							Pen.fillOval(Rect(drawPnt.x - pointRad, drawPnt.y - pointRad, d, d));

							// outline the selected directivity circle
							(sortDex == selectedDex).if{
								Pen.strokeColor_(Color.yellow);
								Pen.strokeOval(Rect(drawPnt.x - pointRad, drawPnt.y - pointRad, d, d));
							};

							// scale in/out toward/away from origin
							gainPnt = drawPnt * dir.linlin(0,1,1.75,1.15);
						}
					);

					// gain labels
					Pen.fillColor_(gainColor.alpha_(1));
					QPen.stringCenteredIn(
						gain.round(0.1).asString,
						Rect(gainPnt.x - (pointRad * 10), gainPnt.y - (pointRad * 10), d * 10, d * 10)
					);
				}
			};

			// input 0 deg azimuth point circle ---
			#az, el, dir, gain = aeds[0];
			// warp elevation for visual scaling
			thisElWarp = elWarp.at(el + pi/2);
			omniDiam = 1 - dir * fullOmni;
			omniDiam = omniDiam.clip(d, fullOmni);
			omniRad= omniDiam / 2;

			azPnt = Point(cos(az), sin(az)) // = Polar(dir, az).asPoint
			.rotate(pi/2)    // convert ambi to screen coords
			* Point(1,-1)    // flip Y for drawing
			* arcH;          // scale normalized points to arcH
			drawPnt = azPnt * cos(el) * thisElWarp;
			drawPnt = drawPnt * dir;

			col = Color.fromHexString("#CC0000");
			rect = Rect(0,0,d * 2,d * 2).center_(drawPnt);
			Pen.strokeColor = col;
			Pen.width = 1;
			Pen.strokeOval(rect);
			Pen.stringCenteredIn("F", rect, color: col);


			Pen.translate(view.bounds.center.x.neg, view.bounds.center.y.neg);

			/* gain inspection text window */

			inspInset = 10;
			gainWidth = 60;
			gainHeight = 60;

			gainRect = Rect(
				view.bounds.width - gainWidth - inspInset,
				view.bounds.height - gainHeight - inspInset,
				gainWidth, gainHeight);
			minRect = Rect(
				gainRect.bounds.left, gainRect.bounds.top,
				gainWidth / 2, gainHeight);
			maxRect = Rect(
				gainRect.bounds.center.x, gainRect.bounds.top,
				gainWidth / 2, gainHeight);

			Pen.fillColor_(Color.gray.alpha_(0.4));
			Pen.fillRect(gainRect);
			Pen.strokeColor_(ctlColor);
			Pen.width = 2;
			Pen.strokeRect(gainRect);

			amps = aeds.collect({ |me| me[3] });

			Pen.stringCenteredIn("GAIN\n\n", gainRect,
				Font("Helvetica", 12), ctlColor);
			Pen.stringCenteredIn("\n\nMin\n", minRect,
				Font("Helvetica", 12), ctlColor);
			Pen.stringCenteredIn("\n\nMax\n", maxRect,
				Font("Helvetica", 12), ctlColor);
			Pen.stringCenteredIn(
				format("\n\n\n %", amps.minItem.round(0.1)),
				minRect, Font("Helvetica", 12), Color.yellow);
			Pen.stringCenteredIn(
				format("\n\n\n% ", amps.maxItem.round(0.1)),
				maxRect, Font("Helvetica", 12), Color.yellow);

			selectedDex !? {
				var selStats = prePostStats[selectedDex];
				// proto point inspection text window;
				inspWidth = 85;
				inspHeight = 130;
				inspRect = Rect(
					inspInset, view.bounds.height - inspHeight - inspInset,
					inspWidth, inspHeight);

				Pen.fillColor_(Color.gray.alpha_(0.4));
				Pen.fillRect(inspRect);
				Pen.strokeColor_(ctlColor);
				Pen.width = 2;
				Pen.strokeRect(inspRect);

				Pen.stringCenteredIn(
					"Azimuth\n\nElevation\n\nDirectivity\n\nGain\n",
					inspRect,
					Font("Helvetica", 12),
					ctlColor
				);

				Pen.stringCenteredIn(
					format(
						"\n\n%˚ / %˚\n\n%˚ / %˚\n\n%\n\n% dB\n",
						selStats.inAz.raddeg.wrap(0,360).round(0.1),    // input azimuth degree
						selStats.az.raddeg.wrap(0,360.0).round(0.1),    // output azimuth degree
						selStats.inEl.raddeg.fold(-90, 90.0).round(0.1),// input elevation degree
						selStats.el.raddeg.fold(-90, 90.0).round(0.1),  // output elevation degree
						selStats.dir.round(0.01),                       // directivity
						selStats.ampdb.round(0.1),                      // gain
					),
					inspRect,
					Font("Helvetica", 12),
					Color.yellow
				);
			};
			pv.refresh; // refresh planewave view
		});

		// define mouse click - select the nearest point under threshold, display its stats
		uv.mouseDownAction_({ |uvw, x, y|
			var newCursorPnt, distances;

			newCursorPnt = (x@y - uvw.bounds.center);

			#x, y = [newCursorPnt.x, newCursorPnt.y];   // translate cursor coords to origin

			distances = prePostStats.collect({ |statdict|
				statdict.scrnPnt.dist(x@y);
			});

			// select the closest point to mouse click
			// no match sets to nil
			selectedDex = distances.detectIndex(_ < (pointRad +1));

			// update the display
			uv.refresh;
		});


		// Planewave view: draw the planewave arrow
		pv = UserView(uv, Rect(0, 0, uv.bounds.width, uv.bounds.height))
		.resize_(5)
		.drawFunc_({ |view|
			// draw test signal planewave
			pwPlaying.if{
				rH = arcH * 0.2; // rect height as a ratio of arcH
				rW = rH * whRatio;
				w_2 = rW * 0.5;
				w_4 = rW * 0.25;
				w_2neg = rW * -0.5;
				h_2 = rH * 0.5;
				h_4 = rH * 0.25;
				h_34= rH * 0.75;

				Pen.translate(view.bounds.center.x, view.bounds.center.y);
				Pen.rotate(pwAzim.neg - 0.5pi);
				Pen.translate(1.1 * arcH, 0); // tip of the arrow
				// drawing as if from 3 o'clock point pointing left
				// arrow head
				Pen.lineTo(h_4@w_4);
				Pen.lineTo(h_4@w_4.neg);
				Pen.lineTo(0@0);
				// arrow shaft
				Pen.moveTo(h_4@0);
				Pen.lineTo(rH@0);
				// planewaves
				Pen.moveTo(rH@w_2);
				Pen.lineTo(rH@w_2neg);
				Pen.moveTo(h_34@w_2);
				Pen.lineTo(h_34@w_2neg);
				Pen.moveTo(h_2@w_2);
				Pen.lineTo(h_2@w_2neg);

				Pen.strokeColor_(Color(*(0.8!3)));
				Pen.stroke;
			};
		});

		// Transform View - overlays the User View
		tv = View(uv, Rect(0, 0, uv.bounds.width, uv.bounds.height)).layout_(
			VLayout(
				VLayout(
					VLayout(
						StaticText().string_("Directivity Display"),
						PopUpMenu().items_(['Size + Radius', 'Radius Only'])
						.action_({ |mn|
							dirDisplay = switch(mn.value, 0,{ 'size' },1,{ 'radius' });
							this.prUpdateMatrix(lastUpdatedMatrix); // which matrix?
						}).maxWidth_(130),
						nil
					)
					.setAlignment(0, \topRight)
					.setAlignment(1, \topRight),
				)
			)
			.setAlignment(0, \topRight)
		).resize_(5);
	}


	createNewXForm { |whichChain, index|
		var xForm, maxChainSize=0;

		xForm = FoaXformView(this, 'chain', whichChain, index);

		// add the xformView object to the chain list
		xfViewChains[whichChain].insert(index, xForm);

		// place xformView's view in the window
		chainViews[whichChain].layout.insert(
			xForm.view, index + 1, // +1 to account for chain label row at index [0]
			align: \top);

		// add the chain ID to the label
		xForm.labelTxt.string = FoaMatrixChain.abcs[whichChain] ++ (index);

		this.prUpdateChainIdLabels();
		this.alignRows;
	}


	prRemoveXForm { |whichChain, rmvDex|
		{
			var xf;

			xf = xfViewChains[whichChain][rmvDex];
			xf.isNil.if{ "view not found!".error };

			xf.view.layout.destroy;
			xf.view.remove;
			xfViewChains[whichChain].removeAt(rmvDex);

			// need to wait for some reason for win bounds to update?
			0.01.wait;

			this.alignRows;
			this.prUpdateChainIdLabels();

		}.fork(AppClock)
	}


	addChainView { |chainIndex|
		var chView, rbut, chLabel, abut;
		var titleView, titleLayout, chLayout;

		// create a new list to hold this chain's transform views
		xfViewChains.insert(chainIndex, List());

		rbut = Button().states_([["X"]]).action_({
			// chain.removeChain(chainIndex);
			chain.removeChain(this.prGetChainViewID(chView));
		}).maxWidth_(25);

		abut = Button().states_([["+"]]).action_({
			// chain.addChain(chainIndex + 1);
			chain.addChain(this.prGetChainViewID(chView) + 1);
		}).maxWidth_(25);

		chLabel = StaticText().string_(
			"Chain " ++ FoaMatrixChain.abcs[chainIndex]
		).align_(\center);

		// chain title layout
		titleLayout = (chainIndex == 0).if({
			// no remove button on first chain
			HLayout(25, nil, [chLabel, a: \center], nil, [abut, a: \right])
		}, {
			HLayout([rbut, a: \left], [chLabel, a: \center], [abut, a: \right])
		});

		// chain title view
		titleView = View()
		.layout_(titleLayout.margins_(xfMargins))
		.minWidth_(chainViewWidth).maxHeight_(chTitleHeight)
		.background_(Color.hsv(
			*ctlColor.asHSV.put(0,
				(ctlColor.asHSV[0] + (chainIndex * colorStep)).wrap(0,1)))
		);

		chLayout = VLayout([titleView, a: \top]).margins_(4);

		// create the view that will hold all transforms in this chain
		chView = View()
		.layout_(chLayout)
		.minWidth_(chainViewWidth + 8)
		.background_(Color.hsv(
			*chainColor.asHSV.put(0,
				(chainColor.asHSV[0] + (chainIndex * colorStep)).wrap(0,1)))
		);

		// add this chain view column to the window
		xfWin.view.layout.insert(chView, chainIndex);
		// add this chain view to the list of chain views
		chainViews.insert(chainIndex, chView);

		(chainIndex > 0).if{ this.prUpdateChainTitles };
		this.prUpdateChainIdLabels;
	}


	removeChainView { |rmvDex|
		fork({
			var newSize = 0, width;

			// collect view widths before removing
			width = chainViews[0].bounds.width;

			chainViews[rmvDex].remove;
			chainViews.removeAt(rmvDex);

			xfViewChains.removeAt(rmvDex);

			// need to wait for some reason for win bounds to update?
			0.02.wait;

			// resize the window
			xfWin.bounds_(xfWin.bounds.width_(width * chainViews.size));

			this.prUpdateChainTitles;
			this.prUpdateChainIdLabels;
			this.prUpdateInputMenus;
		}, AppClock)
	}


	prUpdateChainTitles {
		chainViews.do{ |chv, i|
			var titleView;
			titleView = chv.children[0];
			// find the StaticText
			titleView.children.do{ |child|
				child.isKindOf(StaticText).if{
					child.string_(format("Chain %", FoaMatrixChain.abcs[i]));
				}
			}
		}
	}


	// handles the switching between the matrix in the display,
	// and the matrix in the transform chain, depending on the last touched
	curXformMatrix {
		^case
		{ lastUpdatedMatrix === 'chain' } { chain.curXformMatrix }
		{ lastUpdatedMatrix === 'display' } { displayChain.curXformMatrix }
		{ (lastUpdatedMatrix.isKindOf(Matrix) or:
			lastUpdatedMatrix.isKindOf(FoaXformerMatrix)) }
		{ lastUpdatedMatrix };
	}


	// whichMatrix can be a Matrix, FoaXformerMatrix, 'display', or 'chain'
	prUpdateMatrix { |whichMatrix|
		var xfMatrix;

		xfMatrix = case
		{ whichMatrix === 'chain' } { chain.curXformMatrix }
		{ whichMatrix === 'display' } { displayChain.curXformMatrix }
		{ (whichMatrix.isKindOf(Matrix) or:
			whichMatrix.isKindOf(FoaXformerMatrix)) }
		{ whichMatrix }
		;

		xfMatrix ?? { error("invalid updateMatrix selection") };

		lastUpdatedMatrix = whichMatrix;

		// send the new matrix to the audition matrix fading synth
		audition !? {
			audition.auditionEnabled.if{
				audition.matrixFader.matrix_(xfMatrix);
			}
		};

		transformedPlanewaves = planewaveMatrices.collect{ |pointMtx|
			xfMatrix * pointMtx
		};

		// calculate and set aeds var for gui update
		aeds = transformedPlanewaves.collect{ |ptMtx, i|
			var aeda, statDict, az, el, dir, ampdb;
			var dir_orig, aeda2;

			// aeda = FoaMatrixChain.aedFromMatrix(ptMtx);
			aeda = FoaMatrixChain.aerEFromMatrix(ptMtx);
			// unpack
			#az, el, dir, ampdb = aeda;

			statDict = prePostStats[i];
			statDict.az = az;
			statDict.el = el;
			statDict.dir = dir;
			statDict.ampdb = ampdb;

			aeda; // return
		};

		sfWin !? { sfWin.refresh };
	}


	prUpdateChainIdLabels {
		xfViewChains.do{ |vchain, i|
			vchain.do{ |xf, j|
				xf.labelTxt.string_(FoaMatrixChain.abcs[i]++j)
			}
		}
	}


	prGetInputList { |stopChainDex, stopLinkDex|
		var items = [];

		block { |break|
			chain.chains.do({ |ch, i|
				ch.size.do{ |j|
					(i == stopChainDex and: { j == stopLinkDex }).if({
						break.()
					}, {
						items = items.add((FoaMatrixChain.abcs[i]++j).asSymbol)
					})
				}
			})
		};

		^items
	}


	// called when xforms are added/removed from the chain,
	// input index menus need to be updated
	prUpdateInputMenus {

		xfViewChains.do{ |vchain, i|
			vchain.do{ |xf, j|
				var items, inMenu, newSelection;

				inMenu = xf.inputMenu;

				// check if the xform has an input parameter
				inMenu.notNil.if{
					var items, thisLink, inputLink;

					// regenerate possible menu items based on new chain
					items = this.prGetInputList(i, j);
					// set new input key options to the menu
					inMenu.items = items;

					// look up my link by id,
					thisLink = chain.chains[i][j];

					// find my control state param (the input link),
					inputLink = thisLink.controlStates.select({ |cstate|
						cstate.isKindOf(FoaMatrixChainLink)
					}).at(0); // should only return 1 state param: an FoaMatrixChainLink

					// find the index of that link in the chain
					// and get the key label for that index
					chain.chains.do{ |xfchain, i|
						xfchain.do{ |link, j|
							(inputLink === link).if{
								newSelection = (FoaMatrixChain.abcs[i] ++ j).asSymbol;
							};
						}
					};

					newSelection ?? { error("input link not found in the chain!") };

					inMenu.value_(items.indexOf(newSelection));
				};
			}
		}
	}


	alignRows {
		var maxChainSize, maxChainHeight;

		// pad other shorter chain columns with a nil layout so rows align
		maxChainSize = xfViewChains.collect({ |chain| chain.size }).maxItem;

		xfViewChains.do{ |vchain, i|
			var numxforms;
			numxforms = vchain.size;
			(numxforms < maxChainSize).if{
				(maxChainSize - numxforms).do{
					chainViews[i].layout.add(nil)
				}
			};
		};

		maxChainHeight = (maxChainSize * xfHeight) + chTitleHeight;
		xfWin.bounds_(
			xfWin.bounds.top_(
				xfWin.bounds.top + (xfWin.bounds.height - maxChainHeight))
			.height_(maxChainHeight));
	}


	// xfView is an instance of FoaXformView
	prGetXfViewID { |xfView|
		var whichChain, rmvDex;
		xfViewChains.do{ |vchain, i|
			vchain.do{ |view, j|
				(view === xfView).if{
					rmvDex = j; whichChain = i;
					^[whichChain, rmvDex]
				};
			}
		};
		^error("xfView not found!")
	}


	// xfView is an instance of FoaXformView
	prGetChainViewID { |chainView|
		chainViews.do{ |chv, i|
			(chv === chainView).if{ ^i };
		};
		^error("chainView not found!")
	}


	showMatrixWindow { |postCurrentMatrix = false|
		var mwinH = 200;
		var evalMtxString, postMtxString;

		postMtxString =
		"/* The current transform matrix: */\n\n" ++ this.curXformMatrix.asString;

		evalMtxString =
		"/* Enter valid SC code here that returns a Matrix or FoaXformerMatrix to display */\n";

		codeWin.isNil.if({
			codeWin = Window("Evaluate Matrix",
				Rect(sfWin.bounds.left,
					scrnB.height - sfWin.bounds.height - mwinH
					- (2 * 20) - 25, // (title bar: 20, OS menu bar: 25)
					sfWin.bounds.width, mwinH))
			.onClose_({ codeWin = nil })
			.front;

			View(codeWin, Rect(0,0, codeWin.bounds.width, codeWin.bounds.height))
			.layout_(
				VLayout(
					evalTxtView = TextView().enterInterpretsSelection_(true)
					.string_(
						postCurrentMatrix.if({ postMtxString }, { evalMtxString })
					),

					HLayout(
						Button().states_([["Evaluate"]])
						.action_({
							var mtx;
							mtx = evalTxtView.string.interpret;
							(
								mtx.isKindOf(Matrix) or: { mtx.isKindOf(FoaXformerMatrix) }
							).if({
								this.prUpdateMatrix(mtx)
							}, {
								warn("code did not return a Matrix or FoaXformerMatrix")
							});
						}),
						Button().states_([["Reset"]])
						.action_({ evalTxtView.string = evalMtxString }),
					)
				)
			)
		}, {
			evalTxtView.string = postCurrentMatrix.if({
				postMtxString
			}, {
				evalMtxString
			});
		});
	}


	update {
		| who, what ... args |

		(who == chain).if{
			switch(what,
				\chainAdded, {
					var index;
					index = args[0];
					// postf("responding to \chainAdded: %\n", index);
					this.addChainView(index);
				},
				\chainRemoved, {
					var index;
					index = args[0];
					// postf("responding to \chainRemoved: %\n", index);
					this.removeChainView(index);
					this.prUpdateMatrix('chain');
				},
				\transformAdded, {
					var xformName, whichChain, index;
					#xformName, whichChain, index = args[0..2];
					this.createNewXForm(whichChain, index);
					this.prUpdateInputMenus;
					this.prUpdateMatrix('chain');
				},
				\transformRemoved, {
					{
						var whichChain, index;
						#whichChain, index = args[0..1];
						this.prRemoveXForm(whichChain, index);
						0.02.wait; // for some reason needs time to remove
						this.prUpdateInputMenus;
						this.prUpdateMatrix('chain');
					}.fork(clock: AppClock)
				},
				\transformReplaced, {
					var whichChain, index, newXformName;
					#newXformName, whichChain, index = args[0..2];
					xfViewChains[whichChain][index].rebuildControls;
					this.prUpdateMatrix('chain');
				},
				\transformMuted, {
					var whichChain, index, bool;
					#whichChain, index, bool = args[0..2];
					this.prUpdateMatrix('chain');
					xfViewChains[whichChain][index].muteState(bool); // update UI with muted state

					// if another xf is soloed, re-perform the solo
					// in case this un-mute changes its color
					// downstream from a soloed xf
					block { |break|
						xfViewChains[..whichChain].do{ |vchain,i|
							(i < whichChain).if({					// check all xf's in the chain
								vchain.do{ |xfv,j|
									chain.chains[i][j].soloed.if{
										xfViewChains[whichChain][index].updateStateColors(true);
										break.();
									}
								}
							}, {
								// same chain as the un-muted xf, check only the xf's up to this index
								vchain[..index].do{ |xfv,j|
									chain.chains[i][j].soloed.if{
										(j != index).if{			// only re-"mute" color if this isn't the soloed xf
											xfViewChains[whichChain][index].updateStateColors(true);
										};
										break.();
									}
								}
							})
						}
					}
				},
				\transformSoloed, {
					var whichChain, index, bool, unmuting, chainDex;
					#whichChain, index, bool = args[0..2];
					this.prUpdateMatrix('chain');
					unmuting = bool.not;
					xfViewChains[whichChain][index].soloState(bool); // update UI with soloed state

					// mute the colors of the UI for every link after this one
					xfViewChains[whichChain..].do{ |vchain,i|
						chainDex = whichChain + i;
						vchain.do{ |xfv, j|
							(i == 0).if({
								(j > index and: {
									bool or: {
										unmuting and: {
											// don't unmute colors if xform state is .muted
											chain.chains[chainDex][j].muted.not
										}
									}
								}).if{
									xfv.updateStateColors(bool)
								}
							}, {
								(bool or: {
									unmuting and: {
										// don't unmute colors if xform state is .muted
										chain.chains[chainDex][j].muted.not
									}
								}).if{
									xfv.updateStateColors(bool)
								}
							})
						}
					};
				},
				\paramUpdated, {
					this.prUpdateMatrix('chain');
				}
			);
		};

		(who == displayChain).if{
			switch(what,
				\transformReplaced, {
					var whichChain, index, newXformName;
					#newXformName, whichChain, index = args[0..2];
					displayXFormView.rebuildControls;
					this.prUpdateMatrix('display');
				},
				\paramUpdated, {
					this.prUpdateMatrix('display');
				}
			);
		};

		(who == audition).if{
			switch(what,
				\pwAzim, {
					var state = args[0]; // can be a number of bool
					(state == false).if({
						pwPlaying = false
					}, {
						pwAzim = state
					});
					defer { pv.refresh };
				},
				\pwSynthRunning, {
					pwPlaying = args[0];
					defer { uv.refresh };
				},
				\closed, {
					pwPlaying = false;
					audition.removeDependant(this);
					defer { uv.refresh };
				}
			);
		};
	}
}
