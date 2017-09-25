FoaMatrixFade {
    classvar <mtxFadeDef;
    // copyArgs
    var <outbus, <inbus, initMatrix, <>xFade, <amp, addAction, target, <server, completeCond;
    var <synth, <matrix, server, internalInbus=false, internalOutbus=false;


    *new { |outbus, inbus, initMatrix, xFade = 0.5, amp = 1,
        addAction, target, server, completeCond|

        ^super.newCopyArgs(
            outbus, inbus, initMatrix, xFade, amp,
            addAction, target, server, completeCond).init;
    }


    init {
        fork {
            var addAct, targ;
            var cond = Condition(false);

            server = server ?? Server.default;

            if ( FoaMatrixFade.mtxFadeDef.isNil, {
                FoaMatrixFade.loadSynthDefs(server, cond);
                cond.wait;
            });

            inbus ?? {
                internalInbus = true;
                // "No input bus specified for FoaMatrixFade, so creating an input bus for you. Get it with .inbus".postln;
                inbus = server.audioBusAllocator.alloc( 4 );
            };
            outbus ?? {
                internalOutbus = true;
                "Creating an output bus. Get it with .outbus".postln;
                outbus = server.audioBusAllocator.alloc( 4 );
            };
            addAct = addAction ?? {\addToTail};
            targ = target ?? {1};

            synth = Synth.new( mtxFadeDef.name, [
                \outbus, outbus,
                \inbus, inbus,
                \fade, xFade,
                \amp, amp
            ], targ, addAct );

            server.sync;

            initMatrix !? {this.matrix_(initMatrix)};

            completeCond !? {completeCond.test_(true).signal;};
        }
    }


    matrix_ { |newMatrix|
        var flatMatrix;

        flatMatrix = case
        { newMatrix.isKindOf( Matrix ) } { newMatrix.asArray.flat }
        { newMatrix.isKindOf( FoaXformerMatrix ) } { newMatrix.matrix.asArray.flat };

        synth.set(\fade, xFade, \matrixArray, flatMatrix);

        // update instance var for introspection
        matrix = newMatrix;
    }


    amp_{ |amplitude|
        synth.set(\amp, amplitude);
        amp = amplitude;
    }


    free {
        synth.free;
        internalInbus.if{ server.audioBusAllocator.free( inbus ) };
        internalOutbus.if{ server.audioBusAllocator.free( outbus ) };
    }


    *loadSynthDefs { |server, cond|

        server.waitForBoot({
            mtxFadeDef = SynthDef(\foaMatrixFade, { arg outbus, inbus, fade = 1.5, amp = 1;
                var foaSrc, array, out;

                foaSrc = In.ar(inbus, 4) * Lag.kr(amp);

                array = Control.names([\matrixArray]).kr(
                    Matrix.newIdentity(4).asArray.flat // initialize with no transform
                );
                array = Lag.kr(array, fade); // lag the matrix swap

                array = array.clump(4).flop;

                out = Mix.fill( 4, { arg i; // fill input
                    array.at(i) * foaSrc.at(i)
                });

                Out.ar(outbus, out);
            }).load(server);

            // wait for synthdef to load
            server.sync;
            cond.test_(true).signal;
        });

    }
}
