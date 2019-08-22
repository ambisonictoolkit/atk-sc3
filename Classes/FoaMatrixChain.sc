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
// 	Class: FoaMatrixChain
// 	Class: FoaMatrixChainLink
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

FoaMatrixChain {
    //copyArgs
    var <>verbose;
    var <xFormDict, <xFormDict_sorted;
    var <chains, <curXformMatrix;


    *new { arg verbose = false; ^super.newCopyArgs(verbose).init }


    // abcs used for transform id keys in the chain(s)
    *abcs { ^['A','B','C','D','E','F','G','H','I','J'] }


    init {
        this.loadXForms;
        // list containing each branch in the chain
        chains = List();
        this.addChain( 0 );
    }


    addChain { |index|
        // create a chain
        chains.insert( index, List() );
        this.changed(\chainAdded, index);

        if(index == 0)
        { this.addTransform( 'a soundfield', index, 0 ) }   // first "transform" in first chain
        { this.addTransform(                                // head of a new chain
            'input soundfield', index, 0,
            // pass the last transform in previous chain
            chains[ index-1 ][ chains[index-1].size - 1 ]
            )
        };

        // followed by a 'thru' transform that user will change to desired transform
        this.addTransform( 'soundfield thru', index, 1 );
    }


    removeChain { |index|
        var removedLinks;
        removedLinks = chains[index].collect{ |lnk| lnk };
        chains.removeAt( index );
        removedLinks.do{ |rmvdLink| this.checkIfInputRemoved( rmvdLink ) };
        this.chainXForms;
        this.changed(\chainRemoved, index);
    }


    // clear the chain
    reset {
        chains.clear;
        this.addChain( 0 );
    }


    clear { this.reset }


    // initializes and returns the dictionary with transform attributes
    // and optionally params for setting initial control vals
    createXformLink { | xformName ... params |
        var link, controls;

        // let 'mute' by, check that xformName is in the xFormDict
        if ( xformName != 'mute' )
        {
            xFormDict[xformName] ?? { error( "transform name not found!" ) };

            // initialize the transform
            link = FoaMatrixChainLink( xformName );

            // initialize controls
            controls = xFormDict[xformName].controls.clump(2);

            controls.do{|pair, i|
                var paramName, ctl;
                #paramName, ctl = pair;

                params[i].notNil.if(
                    {
                        link.controlStates.put(i, params[i])
                    },{
                        if( ctl.isKindOf(ControlSpec) ) {
                            link.controlStates.put(i, ctl.default)
                        } {
                            if( ctl == 'A0' )
                            // default to the first link in the chain (a soundfield)
                            { link.controlStates.put(i, chains[0][0]) }
                            { link.controlStates.put(i, ctl) }
                        };
                });
            };
        }{  // mute
            link = FoaMatrixChainLink( xformName );
        };

        ^link
    }

    setParam { | whichChain, index, ctlDex, value |
        this.checkLinkExists(whichChain, index) ?? {^this};
        chains[whichChain][index].controlStates[ctlDex] = if( value.isKindOf( Symbol ) )
        { this.getLinkByKey( value ) } // for controls with input index parameter
        { value };

        this.chainXForms;
        this.changed( \paramUpdated );
    }


    addTransform { | xformName, whichChain, index ... params |
        var link;
        link = this.createXformLink( xformName, *params );
        chains[whichChain].insert( index, link );
        this.chainXForms;
        this.changed(\transformAdded, xformName, whichChain, index);
    }


    // for changing the transform at a point in the chain
    replaceTransform { | newXformName, whichChain, index ... params |
        var newLink, formerLink;
        this.checkLinkExists(whichChain, index) ?? {^this};
        newLink = this.createXformLink(newXformName, *params);
        formerLink = chains[whichChain][index];
        chains[whichChain].put( index, newLink );

        this.checkIfInputRemoved(formerLink, newLink);
        this.chainXForms;
        this.changed(\transformReplaced, newXformName, whichChain, index);
    }


    removeTransform { | whichChain, index |
        var rmvdLink;
        this.checkLinkExists(whichChain, index) ?? {^this};
        rmvdLink = chains[whichChain][index];
        chains[whichChain].removeAt(index);
        // update any xform inputs that were removed before .chainXForms
        this.checkIfInputRemoved( rmvdLink );
        this.chainXForms;
        this.changed( \transformRemoved, whichChain, index );
    }


    muteXform { | bool, whichChain, index |
        this.checkLinkExists(whichChain, index) ?? {^this};
        chains[whichChain][index].muted = bool;
        this.chainXForms;
        this.changed( \transformMuted, whichChain, index, bool );
    }


    soloXform { | bool, whichChain, index |
        var changed = List();
        this.checkLinkExists(whichChain, index) ?? {^this};
        // keep a list of the solo states that are changing

        if (bool) { // if activating solo
            chains.do{ |chain, i|
                chain.do{ |lnk, j|
                    if (lnk.soloed) {
                        lnk.soloed = false;             // ... un-solo anything else that's soloed
                        changed.add([i,j,false]); // keep track of the changes
                    }
                }
            }
        };

        chains[whichChain][index].soloed = bool; // perform this solo
        changed.add([whichChain, index, bool]);  // add this solo to changed list last

        this.chainXForms;
        changed.do{|whichDxBool|
            this.changed( \transformSoloed, *whichDxBool );
        }
    }


    checkLinkExists { |whichChain, index|
        var warning;
        warning = format("No transform found at chain % index %", whichChain, index);
        try {chains[whichChain][index]} {warning.warn; ^nil};
        // return the link. this can be nil if
        // only the index is out of range but whichChain isn't
        // so it should still get caught by the receiver
        ^chains[whichChain][index] ?? {warning.warn; nil};
    }


    checkIfInputRemoved { | rmvdLink, replacementLink |
        chains.do{ |chain, i|
            chain.do{ |lnk, j|
                var cStates = lnk.controlStates;
                cStates.indices.do{|dex|
                    if (cStates[dex] === rmvdLink) {
                        // replace input with new link or A0 if none
                        cStates.put(dex, replacementLink ?? chains[0][0] );
                    };
                }
            }
        }
    }


    // Note: the output soundfield will always be the result of the
    // last transform in the last transform chain
    chainXForms {
        var mtx;
        block {|break|
            chains.do{ |chain, i|
                chain.do{ |xf, j|

                    if( (i == 0) and: (j == 0),
                        {    // init input soundfield for first chain
                            mtx = Matrix.newIdentity(4);
                            xf.mtx_( mtx );
                            curXformMatrix = mtx; // init var
                        },
                        {   // Note: names set to 'mute' ('-') or .muted are skipped
                            // 'mute' is a psuedo transform ('-'), like a pass-through,
                            // while .muted is a state
                            ( (xf.name != 'mute') and: xf.muted.not).if({
                                var ctlStates, ctlVals;

                                ctlStates = xf.controlStates;

                                ctlVals = ctlStates.indices.collect({ |dex|
                                    var val = ctlStates[dex];
                                    if( val.isKindOf( FoaMatrixChainLink ) )
                                    { val.mtx }
                                    { val };
                                });

                                // pass in preceding soundfield followed by the control
                                // values for the transform operation, in order
                                mtx = xFormDict[xf.name]['getMatrix'].( mtx, *ctlVals );
                                xf.mtx_( mtx );     // store resulting matrix
                            },{
                                xf.mtx_( mtx );     // xf is muted or "thru", forward the preceding the matrix
                            });
                            xf.soloed.if{break.()}  // stop chaining here if solo'd
                        }
                    );
                }
            };
        };

        verbose.if{ this.postChain };

        curXformMatrix = mtx;
    }


    postChain {
        chains.do{ |chain, i|
            ("\nCHAIN "++i).postln;
            chain.do{ |xf, j|
                postf( "\t%, % %\n", xf.name, xf.controlStates,
                    if(xf.muted,{"[MUTED]"},{""})++if(xf.soloed,{"[SOLOED]"},{""})
                )
            };
            "".postln;
        };
    }


    getLinkByKey { |key|
        var str, index, xfDex;

        str = key.asString;
        index = this.class.abcs.indexOf( str.keep(1).asSymbol );
        xfDex = str.drop(1).asInteger;

        ^chains[index][xfDex];
    }


    loadXForms {
        var xformSpecs;
        /* [
            'xformName',
            [    // this order defines the order of arguments passed to the transform matrix
                ctl1Name, ctl1Spec,
                ctl2Name, ctl2Spec,
                ...
            ],
            { |mtx, ctl1, ctl2, ..| FoaXformerMatrix.newTransform(ctl1, ctl2).matrix * mtx }
        ] */

        xformSpecs = [
            'push', [
                'Degree',     ControlSpec(-pi/2, pi/2, default: 0, units: "π"),
                'Azimuth',    ControlSpec(pi, -pi, default: 0, units: "π"),
                'Elevation',  ControlSpec(-pi/2, pi/2, default: 0, units: "π"),
            ],
            { |mtx, deg=0, az=0, el=0|
                FoaXformerMatrix.newPush(deg, az, el).matrix * mtx },

            'press', [
                'Degree',     ControlSpec(-pi/2, pi/2, default: 0, units: "π"),
                'Azimuth',    ControlSpec(pi, -pi, default: 0, units: "π"),
                'Elevation',  ControlSpec(-pi/2, pi/2, default: 0, units: "π"),
            ],
            { |mtx, deg=0, az=0, el=0|
                FoaXformerMatrix.newPress(deg, az, el).matrix * mtx },

            'focus', [
                'Degree',     ControlSpec(-pi/2, pi/2, default: 0, units: "π"),
                'Azimuth',    ControlSpec(pi, -pi, default: 0, units: "π"),
                'Elevation',  ControlSpec(-pi/2, pi/2, default: 0, units: "π"),
            ],
            { |mtx, deg=0, az=0, el=0|
                FoaXformerMatrix.newFocus(deg, az, el).matrix * mtx },

            'zoom',    [
                'Degree',     ControlSpec(-pi/2, pi/2, default: 0, units: "π"),
                'Azimuth',    ControlSpec(pi, -pi, default: 0, units: "π"),
                'Elevation',  ControlSpec(-pi/2, pi/2, default: 0, units: "π"),
            ],
            { |mtx, deg=0, az=0, el=0|
                FoaXformerMatrix.newZoom(deg, az, el).matrix * mtx },

            'direct', [
                'Degree',     ControlSpec(-pi/2, pi/2, default: 0, units: "π"),
                'Azimuth',    ControlSpec(pi, -pi, default: 0, units: "π"),
                'Elevation',  ControlSpec(-pi/2, pi/2, default: 0, units: "π"),
            ],
            { |mtx, deg=0, az=0, el=0|
                FoaXformerMatrix.newDirect(deg, az, el).matrix * mtx },

            'directO', [
                'Degree',     ControlSpec(-pi/2, pi/2, default: 0, units: "π"),
            ],
            { |mtx, deg=0, az=0, el=0|
                FoaXformerMatrix.newDirectO(deg).matrix * mtx },

            'directX', [
                'Degree',     ControlSpec(-pi/2, pi/2, default: 0, units: "π"),
            ],
            { |mtx, deg=0|
                FoaXformerMatrix.newDirectX(deg).matrix * mtx },

            'directY', [
                'Degree',     ControlSpec(-pi/2, pi/2, default: 0, units: "π"),
            ],
            { |mtx, deg=0|
                FoaXformerMatrix.newDirectY(deg).matrix * mtx },

            'directZ', [
                'Degree',     ControlSpec(-pi/2, pi/2, default: 0, units: "π"),
            ],
            { |mtx, deg=0|
                FoaXformerMatrix.newDirectZ(deg).matrix * mtx },

            'rotate', [
                'Azimuth',    ControlSpec(2pi, -2pi, default: 0, units: "π")
            ],
            { |mtx, az=0|
                FoaXformerMatrix.newRotate(az).matrix * mtx },

            'tilt',    [
                'Angle',      ControlSpec(2pi, -2pi, default: 0, units: "π")
            ],
            { |mtx, ang=0|
                FoaXformerMatrix.newTilt(ang).matrix * mtx },

            'tumble', [
                'Angle',      ControlSpec(2pi, -2pi, default: 0, units: "π")
            ],
            { |mtx, ang=0|
                FoaXformerMatrix.newTumble(ang).matrix * mtx },

            'rtt',    [
                'rotate',     ControlSpec(2pi, -2pi, default: 0, units: "π"),
                'tilt',       ControlSpec(2pi, -2pi, default: 0, units: "π"),
                'tumble',     ControlSpec(2pi, -2pi, default: 0, units: "π"),
            ],
            { |mtx, rotate=0, tilt=0, tumble=0|
                FoaXformerMatrix.newRTT(rotate, tilt, tumble).matrix * mtx },

            'asymmetry', [
                'Degree',     ControlSpec(-pi/2, pi/2, default: 0, units: "π")
            ],
            { |mtx, deg=0|
                FoaXformerMatrix.newAsymmetry(deg).matrix * mtx },

            'balance', [
                'Degree',     ControlSpec(pi/2, -pi/2, default: 0, units: "π")
            ],
            { |mtx, deg=0|
                FoaXformerMatrix.newBalance(deg).matrix * mtx },

            'mirror', [
                'Azimuth',    ControlSpec(pi, -pi, default: 0, units: "π"),
                'Elevation',  ControlSpec(-pi/2, pi/2, default: 0, units: "π"),
            ],
            { |mtx, theta=0, phi=0|
                FoaXformerMatrix.newMirror(theta, phi).matrix * mtx },

            'mirrorO', [],
            { |mtx| FoaXformerMatrix.newMirrorO.matrix * mtx },

            'mirrorX', [],
            { |mtx| FoaXformerMatrix.newMirrorX.matrix * mtx },

            'mirrorY', [],
            { |mtx| FoaXformerMatrix.newMirrorY.matrix * mtx },

            'mirrorZ', [],
            { |mtx| FoaXformerMatrix.newMirrorZ.matrix * mtx },

            'dominate', [
                'Gain',       ControlSpec(-24, 24, warp: \db, default: 0, units: "dB"),
                'Azimuth',    ControlSpec(pi, -pi, default: 0, units: "π"),
                'Elevation',  ControlSpec(-pi/2, pi/2, default: 0, units: "π"),
            ],
            { |mtx, gain=0, az=0, el=0|
                FoaXformerMatrix.newDominate(gain, az, el).matrix * mtx },

            'gain', [
                'Gain',       ControlSpec(-48, 24, warp: \db, default: 0, units: "dB")
            ],
            { |mtx, gainDB=0| gainDB.dbamp * mtx },

            //  ----------------------------------------------------------
            /* ADDITION AND SUBTRACTION TRANSFORMS  */
            // Note:  'this index' param defaults to first xform in chain,
            // as it's constant and won't ever be removed or changed

            'subtract', [  // subtract another soundfield from me
                'this index', 'A0',
                'amount',   ControlSpec(-inf, 0, warp: \db, default: 0, units: "dB")
            ],
            { |receiverMtx, subMtx, amountDB|
                receiverMtx + (subMtx * -1 * amountDB.dbamp) },

            'subtract from', [  // subtract me from another soundfield
                'from this index', 'A0',
                'amount',   ControlSpec(-inf, 0, warp: \db, default: 0, units: "dB")
            ],
            { | subtrMtx, receiverMtx, amountDB|
                receiverMtx + (subtrMtx * -1 * amountDB.dbamp) },

            'add', [  // add another soundfield to me
                'this index', 'A0',
                'amount',   ControlSpec(-inf, 0, warp: \db, default: 0, units: "dB")
            ],
            { |receiverMtx, addMtx, amountDB|
                receiverMtx + (addMtx * amountDB.dbamp) },

            'add to', [  // add me to another soundfield
                'this index', 'A0',
                'amount',   ControlSpec(-inf, 0, warp: \db, default: 0, units: "dB")
            ],
            { |addMtx, receiverMtx, amountDB|
                receiverMtx + (addMtx * amountDB.dbamp) },

            'xfade-lin', [
                'fade with', 'A0',
                'xfade',    ControlSpec(0, 1, default: 0.5, units: "")
            ],
            { |thisMtx, thatMtx, fade=0|
                (thisMtx * (1-fade)) + (thatMtx * fade);
            },

            'xfade-cos', [
                'fade with', 'A0',
                'xfade',    ControlSpec(0, 1, default: 0.5, units: "")
            ],
            { |thisMtx, thatMtx, fade=0|
                var thisAmp, thatAmp;
                thisAmp = cos((1-fade)*0.5pi);
                thatAmp = cos(fade*0.5pi);
                (thisMtx * thisAmp) + (thatMtx * thatAmp);
            },

            // input a soundfield - only used at the head of each chain
            'input soundfield', [
                'this index', 'A0',
            ],
            { |mtx, inMtx| inMtx },

            // used when a transform is first added - returns input
            'soundfield thru', [], { |mtx| mtx },

            // the first transform in the chain - returns an identity matrix
            'a soundfield', [], { |mtx| Matrix.newIdentity( 4 ) },

        ];

        // clump into triplets: [[name, ctlSpecs, function], ...]
        xformSpecs = xformSpecs.clump(3);

        // pack the above transform specification triplets in to a dictionary
        xFormDict = IdentityDictionary(know: true);

        xformSpecs.do{ |clump|
            var name, ctls, func;
            #name, ctls, func = clump; // unpack the clump
            xFormDict.put( name,
                 IdentityDictionary(know: true)
                .put('controls', ctls)
                .put('getMatrix', func)
            );
        };

        // alphabetically sorted for menu displays
        xFormDict_sorted = xFormDict.asSortedArray;
    }


    // Note: both these should probably be re-written as a general Atk utility,
    // as part of a coefficient analysis suite modeled on the real-time
    // signal analysis found in ATKAnalyze.sc
    //
    // The returned values are equivalent to those listed below.
    //
    // see also Tapani Pihlajamäki - Multi-resolution Short-time Fourier
    // Transform Implementation of Directional Audio Coding (ch. 6)
    // http://lib.tkk.fi/Dipl/2009/urn100011.pdf

    // FoaThetaPhiA - [Azimuth, Elevation] of Active Intensity Vector
    // --- [ATK doesn't include the listed measure for directivity]
    // FoaSFWL - FOA potential & kinetic energy mean, in dB
    *aedFromMatrix { |matrix|
        var b, pv_mean, b_sqrd_mean, p_sqrd, v_sqrd, a, e, d, d_norm, amp;

        b = matrix.getCol(0);
        b[0] = b[0] * sqrt(2);            // scale W
        b_sqrd_mean = b**2;
        p_sqrd = b_sqrd_mean[0];        // W component, pressure
        v_sqrd = b_sqrd_mean[1..].sum;    // summed X^2,Y^2,Z^2; velocity

        // directivity measure (planewave = 1)
        // calculated measure : 1 - beta.abs / 0.5pi
        d = (pi/2) - (2 * atan2(sqrt(v_sqrd), sqrt(p_sqrd)));
        d_norm = 1 - (d.abs / 0.5pi);

        // W*sqrt(2) * [w,x,y,z]
        pv_mean = b[0] * b;
        // atan2(y,x)
        a = atan2(pv_mean[2], pv_mean[1]);
        // atan2(z,sqrt(x^2 + y^2))
        e = atan2(pv_mean[3], sqrt((pv_mean[1]**2) + (pv_mean[2]**2)));

        amp = sqrt((b**2).sum / 2);

        // return amp in db
        ^[a, e, d_norm, amp.abs.ampdb];
    }

    // FoaThetaPhiA - [Azimuth, Elevation] of Active Intensity Vector
    // FoaMagWa - Magnitude Active Energy Vector: ||Wa||, aka rE
    // FoaSFWL - FOA potential & kinetic energy mean, in dB
    *aerEFromMatrix { |matrix|
        var b, pressure, velocity,
        activeIntensityVec, potentialEnergy, kineticEnergy, meanEnergy,
        eVec, eVec_sqrd, az, el, rE, amp;

        b = matrix.getCol(0);       // b-format "signal"
        pressure = b[0] * sqrt(2);  // w * 2.sqrt
        velocity = b[1..3];         // [x,y,z]

        activeIntensityVec = pressure * velocity;
        potentialEnergy = pressure.squared;
        kineticEnergy = b.squared[1..3].sum; // magnitude^2, so sqrt cancels
        meanEnergy = 0.5 * (potentialEnergy + kineticEnergy);

        // ambisonic energy vector to Spherical coords to extract
        // azimuth, elevation and magnitude of the vector
        eVec = activeIntensityVec / meanEnergy;
        eVec = Cartesian(*eVec);
        eVec = eVec.asSpherical;
        az = eVec.theta;
        el = eVec.phi;
        rE = eVec.rho;

        // equivalently:
        // rE = activeIntensityVec.squared.sum.sqrt / meanEnergy;
        // or for rV:
        // rV = activeIntensityVec.squared.sum.sqrt / potentialEnergy;

        amp = sqrt(meanEnergy);

        ^[az, el, rE, amp.abs.ampdb];
    }
}


// a link holds the state of a transform's parameters
// as well as it's current matrix state, as a result of it's place
// in the chain

FoaMatrixChainLink {
    var <>name; // copyArgs
    var <>controlStates, <>mtx, <>muted, <>soloed;

    *new { | xformName | ^super.newCopyArgs(xformName).init }

    init {
        controlStates = Order();
        mtx = nil;
        muted = false;
        soloed = false;
    }
}
