ATK for SuperCollider : Read Me
========================

This is the SuperCollider version of the Ambisonic Toolkit (ATK).
It can be used with [SuperCollider](http://supercollider.github.io/) on OSX,
Linux and Windows, and is distributed as a
[Quark package](https://github.com/ambisonictoolkit/atk-sc3) with [sc3-plugins](https://github.com/supercollider/sc3-plugins) components, and
[other dependencies](http://www.ambisonictoolkit.net/download/supercollider/).

The Ambisonic Toolkit (ATK) is intended to bring together a number of
tools and methods for working with Ambisonic surround sound. The intention
is for the toolset to be both ergonomic and comprehensive, providing both
classic and novel algorithms to creatively manipulate and synthesise
complex Ambisonic soundfields.

Tools are offered in two sets:
* The first of these is modeled after the hardware and software
tools offered in the world of classic, aka Gerzonic, First Order
Ambisonics (FOA).
* The second is framed as a modern Near-Field Controlled Higher Order
Ambisonic (NFC-HOA) solution.

The tools are framed for the user to think in terms of the soundfield
kernel. By this, it is meant the ATK addresses the holistic problem of
creatively controlling a complete soundfield, allowing and encouraging
the composer to think beyond the placement of sounds in a sound-space
and instead attend to the impression and image of a soundfield. This
approach takes advantage of the model the Ambisonic technology presents,
and is viewed to be the idiomatic mode for working with the Ambisonic
technique.

We hope you enjoy the ATK!

For more information please visit the [Ambisonic Toolkit
website](http:www.ambisonictoolkit.net/) or send us an
[e-mail](mailto:info[at]ambisonictoolkit.net). See also
[ABCs of the ATK](http://doc.sccode.org/Tutorials/ABCs-of-the-ATK.html)
for an overview on working with the ATK for SuperCollider.



&nbsp;

&nbsp;

Installing
==========

&nbsp;

Requirements
------------

ATK for [SuperCollider](http://supercollider.github.io) requires version 3.10
or later. Download the latest version
[here](http://supercollider.github.io/download), or fork the source code at
[GitHub](http://supercollider.github.io/).

&nbsp;

atk-sc3 Quark
-----------

The ATK for [SuperCollider](http://supercollider.github.io)'s classes,
extension methods and documentation are distributed via the
[atk-sc3 quark](https://github.com/ambisonictoolkit/atk-sc3). Start by reviewing
the Quark installation instructions
[found here](https://github.com/supercollider-quarks/quarks#installing). See
also [Using Quarks](http://doc.sccode.org/Guides/UsingQuarks.html).

With [git](https://git-scm.com/) installed, you can easily install the
[atk-sc3 Quark](https://github.com/ambisonictoolkit/atk-sc3) directly by
running the following line of code in SuperCollider:

```supercollider
Quarks.install("https://github.com/ambisonictoolkit/atk-sc3.git");
```

If you've previously installed the ATK, you'll want to update all the dependencies
to their current versions. The easiest way to do so is via the Quarks GUI:

```supercollider
QuarksGui.new;
```


sc3-plugins
-----------

The ATK's compiled UGen component releases are available from the
[sc3-plugins](https://github.com/supercollider/sc3-plugins/releases)
releases page.

Place the downloaded `SC3plugins` folder in your `Extensions` folder. On Mac
OS X, this resolves to:

    ~/Library/Application Support/SuperCollider/Extensions

You may need to create the `Extensions` folder if it does not already exist.  

On other platforms, you can find where this is by running the following line of
code in SuperCollider:

```supercollider
(  
// post the directory in which to move the SC3Plugins folder  
Platform.userExtensionDir.postln;  
)  
(  
// alternatively, SC can open it for you  
// (assuming it already exists! - you may need to create /Extensions)  
Platform.userExtensionDir.openOS;  
)
```

If you've previously installed the ATK, you'll want to be sure to install the
version of [sc3-plugins](https://github.com/supercollider/sc3-plugins/releases)
that is compatible with your installed version of [SuperCollider](http://supercollider.github.io/download).


Kernels, Matrices & Soundfiles
--------------------

Additionally, the SuperCollider version of the ATK has further dependencies:

* [ATK Kernels](http://www.ambisonictoolkit.net/download/kernels/)
* [ATK Matrices](http://www.ambisonictoolkit.net/download/matrices/)
* [ATK Soundfiles](http://www.ambisonictoolkit.net/download/recordings/)

Install Kernels, Matrices, and Soundfiles by running the following code:

```supercollider
Atk.downloadKernels;
Atk.downloadMatrices;
Atk.downloadSounds;
```

If successful, these three dependencies are installed here:

```supercollider
(
// post the kernel, matrix and sounds directories
Atk.userKernelDir.postln;
Atk.userMatrixDir.postln;
Atk.userSoundsDir.postln;
)
```

&nbsp;

Source code
-----------

You can build the ATK for SuperCollider UGen components from the [sc3-plugins](https://github.com/supercollider/sc3-plugins) source-code.


&nbsp;

Need Some Sound Files to Play Around With?
------------------------------------------

You can find a collection of sound files here. (Download as part of installation):

* [http://www.ambisonictoolkit.net/download/recordings/](http://www.ambisonictoolkit.net/download/recordings/)

&nbsp;

Additional sound files can be grabbed from these fine sources:

* [Ambisonic Sound Library](https://library.soundfield.com/).
* [Ambisonia](http://ambisonia.com/).
* [Freesound](http://www.freesound.org/browse/tags/B-format/).

&nbsp;

And most of the catalogue of Nimbus Records are UHJ recordings:

* [http://www.wyastone.co.uk/](http://www.wyastone.co.uk/).

&nbsp;

&nbsp;

Feedback and Bug Reports
========================

Known issues are logged at
[GitHub](https://github.com/ambisonictoolkit/atk-sc3/issues).

If you experience problems or have questions pertaining to the ATK for
SuperCollider, please create an issue in the
[atk-sc3 issue tracker](https://github.com/ambisonictoolkit/atk-sc3/issues).

Experienced users and the developers are active participants on
the [sc-users mailing list](http://www.beast.bham.ac.uk/research/sc_mailing_lists.shtml).
Questions posted here are usually answered fairly quickly.

An archive of this list can be searched from
[this page](http://www.listarc.bham.ac.uk/lists/sc-users/search/).

If need be, the developers can be contacted directly via
[this address](info@ambisonictoolkit.net).

&nbsp;


List of Changes
---------------

Version 5.0.0

*  New features (highlights):
    * Integrated support for classic First Order Ambisonics (FOA) and modern Higher Order Ambisonics (HOA).
    * Implements the Near-Field Controlled, aka Near-Field Compensated, form of higher order Ambisonics (NFC-HOA).
    * Ambisonic order is merely limited by system speed, channel capacity and numerical precision rather than by design.
    * Control and synthesis of the near-field effect (NFE) of finite distance sources in both FOA and HOA, including radial beamforming.
    * Ambisonic coefficients and matrices are available for inspection and manipulation in the interperter.
    * Angular domain soundfield decomposition and recomposition.
    * Analysis of transformer and decoder matrices.
    * Automatic installation of Kernels, Matrices and Soundfiles

* Documentation Updates:
    * Substantial refactor to document and explore new HOA framework!

* Issue fixes:
    * Fix nans on directional analysis


Version 4.0.3

*  Issue fixes:
    *  Make sampleRate value integer strings to support SC 3.10. Fixes
    [supercollider/sc3-plugins #244](https://github.com/supercollider/sc3-plugins/issues/224).


Version 4.0.2

*  Issue fixes:
    *  Remove CTK dependency from kernel loading routines.


Version 4.0.1

*  Documentation Updates:
   *  README: add Michael McCrea as named contributor in Author & Copyright
   notices.

*  Issue fixes:
    *  Correct Quark file dependencies


Version 4.0.0

*  New features:
    *  Soundfield Analysis: Real-time soundfield features and vectors
    *  Soundfield (matrix) transform display
    *  B-format Audition / Player

*  Refactoring:
    *  Quark-ify: classes, extension methods & documentation moved to
    [atk-sc3 Quark](https://github.com/ambisonictoolkit/atk-sc3). UGens remain
    in [sc3-plugins](https://github.com/supercollider/sc3-plugins).
    *  Matrix library: fixed matrices moved to [atk-matrices](https://github.com/ambisonictoolkit/atk-matrices). Download
    fixed matrices: https://github.com/ambisonictoolkit/atk-matrices/releases

*  Documentation Updates:
   *  README updated to reflect recent changes and installation instructions.
   *  Fix broken Decoder k links
   *  Fix -newZoomH2n encoder links
   *  AtkMatrix - remove duplicated methods & hide -type

*  Issue fixes:
    *  Class library: fix inline warnings


Version 3.8.0

*  New features:
    *  Matrix reading & writing: encoder, xformer, & decoder.
    *  Improved NRT encoder / decoder support.
    *  Supported SRs: 44100, 48000, 88200, 96000, 176400, 192000. Download new
    kernels: https://github.com/ambisonictoolkit/atk-kernels/releases

*  Documentation Updates:
    *  README updated to reflect recent changes and installation instructions.
    *  Document decoder k and microphone pattern equivalences, both in terms of
    keywords and numerical values.
    *  Update Intro-to-the-ATK & imaging figures. Transform plots now
    illustrate rE rather than rV, which is more closely tied to perception.
    *  Update Help with Server -numOutputBusChannels advice.
    *  Update various broken links in Help.
    *  Include details on delay introduced by use of kernel encoders & decoders.
    *  More verbose Help for SynthDefs and NRT.
    *  Update Help Files for Multiple SRs.
    *  Help now uses -degrad & -raddeg for unit conversion.
    *  README: add contributors. List under release in alphabetical order by
    first name.

*  Issue fixes:
    *  Score NRT kernel bug fixes
    *  Support for SR = 176400
    *  AtkMatrix.initPeri, (re-)fix for shelfK 3D.


Version 3.7.2

*  New features:
    *  Ambisonic exchange: 1st-order Ambisonic exchange encoders & decoders.
    These support channel orders: ACN and SID; normalization: N3D and SN3D.
    Added to support evolving VR standards.

* Documentation Updates:
    *  ATK has a new web page! Update links in Intro-to-the-ATK.
    *  README updated to reflect recent changes and point to current web page
    and installation sources.
    *  Wiggins credit: Corrected to 5.0 coefficients (only)

Version 3.7.0-beta

*  Refactoring:
    *  ATK speed-up: Optimizations for higher speed UGen matrix calculation.
    *  Generalize FoaEncode and FoaDecode behavior: now more able to support
    subclass extensions

Version 3.7.0-alpha0

*  New features:
    *  Spreader & Diffusion kernel encoders: Frequency spreading & phase
    diffusion. Requires download of new kernel distribution.
    *  Cross-platform support for user and system support directories. Required
    by kernel encoders and decoders.

*  Issue fixes:
    *  Fix clicks on .kr transforms: ATK transform UGens operating at .kr
    clicked when angle arguments wrap.
    *  Fix for FoaPsychoShelf signal passing
    *  Fix for shelfK 3D, AtkMatrix initPeri: was assigning shelfK for the
    psycho acoustic shelf filter to be the values appropriate for 2D in all
    cases. Now corrected for 3D.
    *  Fixes for incorrect kernel paths.

*  Refactoring:
    *  ATKMatrix: save path before freeing kernel
    *  Binaural decoders: Update to CIPIC, Listen and Spherical interfaces to
    support newly diffuse field equalised HRTF decoder kernels. You'll download
    the new kernels.
    *  Assure channel arrays are flat for signal passing.

* Documentation Updates:
    *  CIPIC decoder: now diffuse field equalised.
    *  3rd party HRTFs: kernel licensing notice update
    *  Pampin "On Space" credit updates
    *  SCDoc errors & broken links, formatting & tidying

Version 3.5

* First Public Release as part of the sc3-plugins project!

__A note on the ATK's version numbers__: Versioning for the
[atk-sc3 quark](https://github.com/ambisonictoolkit/atk-sc3) adheres to the
familiar system known as [Semantic Versioning](http://semver.org/). In contrast,
as part of the
[sc3-plugins](https://github.com/supercollider/sc3-plugins) project, the
versioning for UGen components is synced to the release numbers assigned to
[sc3-plugins releases](https://github.com/supercollider/sc3-plugins/releases).

As SuperCollider's plugin system continues to develop, we expect to adopt
[Semantic Versioning](http://semver.org/) for all components when it is possible
to do so.


&nbsp;

&nbsp;

Credits
=======

&nbsp;

Copyright the ATK Community, Joseph Anderson, Joshua Parmenter, and
Michael McCrea 2011, 2016-20.

* J Anderson : [[e-mail]](mailto:j.anderson[at]ambisonictoolkit.net)
* M McCrea : [[e-mail]](mailto:mtm5[at]uw.edu)
* J Parmenter : [[e-mail]](mailto:j.parmenter[at]ambisonictoolkit.net)

&nbsp;

The development of the ATK for SuperCollider is
supported by [DXARTS, Center for Digital Arts and Experimental Media](https://dxarts.washington.edu/).

The filter kernels distributed with the Ambisonic Toolkit are licensed
under a Creative Commons Attribution-Share Alike 3.0 Unported [(CC BY-SA 3.0)](http://creativecommons.org/licenses/by-sa/3.0/) License and
are copyright the Ambisonic Toolkit Community and Joseph Anderson,
2011.

Contributors
------------

Version 5.0.0
*  Joseph Anderson (@joslloand)
*  Michael McCrea (@mtmccrea)
*  Marcin Pączkowski (@dyfer)
*  Eirik Arthur Blekesaune (@blacksound)

Version 4.0.3
*  Michael McCrea (@mtmccrea)
*  Eirik Arthur Blekesaune (@blacksound)

Version 4.0.2
*  Michael McCrea (@mtmccrea)
*  David Granström (@davidgranstrom)

Version 4.0.1
*  Joseph Anderson (@joslloand)

Version 4.0.0
*  Joseph Anderson (@joslloand)
*  Julian Rohrhuber (@telephon)
*  Michael McCrea (@mtmccrea)

Version 3.8.0
*  Daniel Peterson (@dmartinp)
*  Joseph Anderson (@joslloand)
*  Michael McCrea (@mtmccrea)

Version 3.7.2
*  Joseph Anderson (@joslloand)
*  Luis Lloret (@llloret)

Version 3.7.0-beta
*  Nathan Ho (@snappizz)
*  Tim Blechmann (@timblechmann)

Version 3.7.0-alpha0
*  James Harkins (@jamshark70)
*  Joseph Anderson (@joslloand)
*  Joshua Parmenter (@joshpar)

Version 3.5
*  Jonatan Liljedahl (@lijon)
*  Joseph Anderson (@joslloand)
*  Joshua Parmenter (@joshpar)

&nbsp;

&nbsp;

Third Party Notices
===================

&nbsp;

Higher Order Matrix Rotation
----------------------------

The algorithm found in HoaMatrixRotation is largely a port of spherical harmonic
rotations from Archontis Politis's Spherical-Harmonic-Transform Library for
Matlab/Octave.

https://github.com/polarch/Spherical-Harmonic-Transform

Specifically: euler2rotationMatrix.m; getSHrotMtx.m; complex2realSHMtx.m

Politis's code for real SH rotations is a port of Bing Jian's
implementations, found here:
http://www.mathworks.com/matlabcentral/fileexchange/15377-real-valued-spherical-harmonics

Copyright (c) 2016, Bing Jian
Copyright (c) 2015, Archontis Politis
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

* Neither the name of Spherical-Harmonic-Transform nor the names of its
  contributors may be used to endorse or promote products derived from
  this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


The technique was originally authored here:
Ivanic, J., Ruedenberg, K. (1996). Rotation Matrices for Real
Spherical Harmonics. Direct Determination by Recursion. The Journal
of Physical Chemistry, 100(15), 6342?6347.

...with corrections:

Ivanic, J., Ruedenberg, K. (1998). Rotation Matrices for Real
Spherical Harmonics. Direct Determination by Recursion Page: Additions
and Corrections. Journal of Physical Chemistry A, 102(45), 9099-9100.


Diametric Decoder Theorem (DDT) decoding
----------------------------------------

Support for Gerzon's Diametric Decoder Theorem (DDT) decoding algorithm
is derived from Aaron Heller's Octave code available at:
http://www.ai.sri.com/ajh/ambisonics/

Benjamin, et al., "Localization in Horizontal-Only Ambisonic Systems"
Preprint from AES-121, 10/2006, San Francisco

Implementation in the SuperCollider version of the ATK is by [Joseph
Anderson](mailto:j.anderson[at]ambisonictoolkit.net).

&nbsp;

Irregular array decoding
------------------------

Irregular array decoding coefficients (5.0) are kindly provided by
Bruce Wiggins: http://www.brucewiggins.co.uk/

B. Wiggins, "An Investigation into the Real-time Manipulation and
Control of Three-dimensional Sound Fields," PhD Thesis, University of
Derby, Derby, 2004.

&nbsp;

CIPIC HRTF Database (University of California)
----------------------------------------------

V. R. Algazi, R. O. Duda, D. M. Thompson, and C. Avendano, "The CIPIC
HRTF Database," in Proceedings of the 2001 IEEE ASSP Workshop on
Applications of Signal Processing to Audio and Acoustics, New Paltz, NY,
2001.

"The CIPIC HRTF Database - CIPIC International Laboratory." [Online].
Available: <http://interface.cipic.ucdavis.edu/sound/hrtf.html>.
[Accessed: 07-Jul-2011].

**CIPIC Notices:**

Copyright (c) 2001 The Regents of the University of California. All
Rights Reserved

Disclaimer

THE REGENTS OF THE UNIVERSITY OF CALIFORNIA MAKE NO REPRESENTATION OR
WARRANTIES WITH RESPECT TO THE CONTENTS HEREOF AND SPECIFICALLY DISCLAIM
ANY IMPLIED WARRANTIES OR MERCHANTABILITY OR FITNESS FOR ANY PARTICULAR
PURPOSE.

Further, the Regents of the University of California reserve the right
to revise this software and/or documentation and to make changes from
time to time in the content hereof without obligation of the Regents of
the University of California to notify any person of such revision or
change.

Use of Materials

The Regents of the University of California hereby grant users
permission to reproduce and/or use materials available therein for any
purpose- educational, research or commercial. However, each reproduction
of any part of the materials must include the copyright notice, if it is
present. In addition, as a courtesy, if these materials are used in
published research, this use should be acknowledged in the publication.
If these materials are used in the development of commercial products,
the Regents of the University of California request that written
acknowledgment of such use be sent to:

CIPIC- Center for Image Processing and Integrated Computing University
of California 1 Shields Avenue Davis, CA 95616-8553

&nbsp;

Listen HRTF Database (IRCAM)
----------------------------

"LISTEN HRTF DATABASE." [Online]. Available:
<http://recherche.ircam.fr/equipes/salles/listen/>. [Accessed:
07-Jul-2011].

**IRCAM Notices:**

Copyright (c) 2002 IRCAM (Institut de Recherche et Coordination
Acoustique/Musique). All Rights Reserved

Use of Materials

The Listen database is public and available for any use. We would
however appreciate an acknowledgment of the database somewhere in the
description of your work (e.g. paper) or in your development.

Contacts:

Olivier Warusfel, Room Acoustics Team, IRCAM 1, place Igor Stravinsky
75004 PARIS, France
