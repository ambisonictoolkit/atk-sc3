/*
	Copyright the ATK Community, Joseph Anderson, and Josh Parmenter, 2011
		J Anderson	j.anderson[at]ambisonictoolkit.net
		J Parmenter	j.parmenter[at]ambisonictoolkit.net


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
// 	Class (UGen superclass): Foa
//
// 	Class (UGen): FoaPanB
//     Class: FoaMFreqOsc
//
// 	Class (UGen): FoaDirectO
// 	Class (UGen): FoaDirectX
// 	Class (UGen): FoaDirectZ
// 	Class (UGen): FoaRotate
// 	Class (UGen): FoaTilt
// 	Class (UGen): FoaTumble
// 	Class (UGen): FoaFocusX
// 	Class (UGen): FoaFocusY
// 	Class (UGen): FoaFocusZ
// 	Class (UGen): FoaPushX
// 	Class (UGen): FoaPushY
// 	Class (UGen): FoaPushZ
// 	Class (UGen): FoaPressX
// 	Class (UGen): FoaPressY
// 	Class (UGen): FoaPressZ
// 	Class (UGen): FoaZoomX
// 	Class (UGen): FoaZoomY
// 	Class (UGen): FoaZoomZ
// 	Class (UGen): FoaDominateX
// 	Class (UGen): FoaDominateY
// 	Class (UGen): FoaDominateZ
// 	Class (UGen): FoaAsymmetry
//
// 	Class: FoaRTT
// 	Class: FoaMirror
// 	Class: FoaDirect
// 	Class: FoaDominate
// 	Class: FoaZoom
// 	Class: FoaFocus
// 	Class: FoaPush
// 	Class: FoaPress
//
// 	Class (UGen): FoaProximity
// 	Class (UGen): FoaNFC
// 	Class (UGen): FoaPsychoShelf
//
// 	Class: AtkMatrixMix
// 	Class: AtkKernelConv
//
// 	Class: FoaDecode
// 	Class: FoaEncode
//
// 	Class: FoaXform
// 	Class: FoaTransform
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

Atk {
	classvar <userSupportDir, <userSoundsDir, <userKernelDir, <userMatrixDir, <userExtensionsDir;
	classvar <systemSupportDir, <systemSoundsDir, <systemKernelDir, <systemMatrixDir, <systemExtensionsDir;
	classvar <sets;
	classvar <matricesDownloadUrl = "https://github.com/ambisonictoolkit/atk-matrices/releases/latest/download/matrices.zip";
	classvar <kernelsDownloadUrl = "https://github.com/ambisonictoolkit/atk-kernels/releases/latest/download/kernels.zip";
	classvar <soundsDownloadUrl = "https://github.com/ambisonictoolkit/atk-sounds/archive/v1.0.0.zip";

	*initClass {
		userSupportDir = Platform.userAppSupportDir.dirname ++ "/ATK";
		userSoundsDir = userSupportDir ++ "/sounds";
		userKernelDir = userSupportDir ++ "/kernels";
		userMatrixDir = userSupportDir ++ "/matrices";
		userExtensionsDir = userSupportDir ++ "/extensions";

		systemSupportDir = Platform.systemAppSupportDir.dirname ++ "/ATK";
		systemSoundsDir = systemSupportDir ++ "/sounds";
		systemKernelDir = systemSupportDir ++ "/kernels";
		systemMatrixDir = systemSupportDir ++ "/matrices";
		systemExtensionsDir = systemSupportDir ++ "/extensions";

		// Supported sets—this is only for directory management and
		// should be revisited. No need for arbitrary set limit.
		sets = [
			\FOA, \HOA1, \HOA2, \HOA3, \HOA4, \HOA5,
			\HOA6, \HOA7, \HOA8, \HOA9, \HOA10, \HOA11,
			\HOA12, \HOA13, \HOA14, \HOA15
		];

		this.checkSupportDirsExist;
	}

	*userSupportDir_ { |userSupportDirIn|
		userSupportDir = userSupportDirIn;
		userSoundsDir = userSupportDir ++ "/sounds";
		userKernelDir = userSupportDir ++ "/kernels";
		userMatrixDir = userSupportDir ++ "/matrices";
		userExtensionsDir = userSupportDir ++ "/extensions";
	}

	*systemSupportDir_ { |systemSupportDurIn|
		systemSupportDir = systemSupportDurIn;
		systemSoundsDir = systemSupportDir ++ "/sounds";
		systemKernelDir = systemSupportDir ++ "/kernels";
		systemMatrixDir = systemSupportDir ++ "/matrices";
		systemExtensionsDir = systemSupportDir ++ "/extensions";
	}

	*checkSupportDirsExist {
		if((File.exists(Atk.userKernelDir) || File.exists(Atk.systemKernelDir)).not, {
			"Atk: kernels don't appear to be installed".warn;
			"Run 'Atk.downloadKernels' to attempt automatic installation".postln;
		});
		if((File.exists(Atk.userMatrixDir) || File.exists(Atk.systemMatrixDir)).not, {
			"Atk: matrices don't appear to be installed".warn;
			"Run 'Atk.downloadMatrices' to attempt automatic installation".postln;
		});
	}

	*downloadFile { |url, path, action|
		var cmd;
		if(url.notNil && path.notNil, {
			thisProcess.platform.name.switch(
				\windows, {
					cmd = format("powershell.exe -nologo -noprofile -command \"try{(New-Object System.Net.WebClient).DownloadFile('%', '%')} catch [Exception] {Write-Host $_.Exception | format-list -force; exit 1}\"", url, (path.asString));
					"Please note, there is no download progress indication on Windows; you will be notified once the download finishes.".postln;
				}, { //assuming linux/macOS
					if("which curl".unixCmdGetStdOut.replace($\n).size.asBoolean, {
						cmd = format("curl -L % --output % -# --fail 2>&1", url, (path.asString).shellQuote);
					}, {
						if("which wget".unixCmdGetStdOut.replace($\n).size.asBoolean, {
							cmd = format("wget % --output-document % 2>&1", url, (path.asString).shellQuote);
						}, {
							"Neither 'curl' nor 'wget' appear to be installed or are not in your $PATH".warn;
							"If you would like to proceed with manual installation, please see the Ambisonic Toolkit webpage at ambisonictoolkit.net or run \"http://ambisonictoolkit.net\".openOS".postln;
						});
					});
				}
			);
			cmd !? {
				// cmd.postln;
				cmd.unixCmd({|err|
					if(err == 0, {
						postf("downloaded %\n", path);
						action.(err);
					}, {
						postf("download error, exit code %\n", err);
						"If you would like to proceed with manual installation, please see the Ambisonic Toolkit webpage at ambisonictoolkit.net or run \"http://ambisonictoolkit.net\".openOS".postln;
					});
				});
				postf("downloading\n%\ninto\n%\nplease wait...\n", url, path);
			};
		}, {
			"url or path is nil, not downloading".warn;
		});
	}

	*unzipFile { |filePath, destDir, action|
		var cmd;
		if(filePath.notNil && destDir.notNil, {
			thisProcess.platform.name.switch(
				\windows, {
					cmd = format("powershell.exe -nologo -noprofile -command \"& { Add-Type -A 'System.IO.Compression.FileSystem'; [IO.Compression.ZipFile]::ExtractToDirectory('%', '%'); }\"", filePath.asString, destDir.asString);
				}, { //assuming linux/macOS
					if("which unzip".unixCmdGetStdOut.replace($\n).size.asBoolean, {
						cmd = format("unzip -q % -d % -x '__MACOSX/*'", (filePath.asString).shellQuote, (destDir.asString).shellQuote)
					}, {
						"'unzip' doesn't seem to be installed or is not in your $PATH".warn;
					});
				}
			);
			cmd !? {
				// cmd.postln;
				cmd.unixCmd({|err|
					if(err <= 1, { //exit code 1 from unzip means warning errors, but processing completed successfully
						"decompressing finished".postln;
						action.(err);
					}, {
						postf("decompressing error for file at %, exit code %\n", filePath, err);
					});
				});
				postf("\ndecompressing\n%\ninto\n%\nplease wait...\n", filePath, destDir);
			};
		}, {
			"filePath or destDir is nil, not decompressing".warn;
		});
	}

	*downloadAndUnzip { |url, tempPath, destDir, action| //action triggered after the last command
		this.downloadFile(url, tempPath, {|err|
			this.unzipFile(tempPath, destDir, {|err|
				File.delete(tempPath);
				postf("temporary file deleted: %\n", tempPath);
				action.();
				if(err != 0, {postf("error decompressing file, code %\n", err)});
			});
			if(err != 0, {postf("error downloading file, code %\n", err)});
		})
	}

	*downloadMatrices { |useSystemLocation = false, action|
		var tmpPath, url;
		tmpPath = Platform.defaultTempDir ++ this.hash.asString ++ "matrices.zip";
		url = this.matricesDownloadUrl;
		if(useSystemLocation, {
			if(File.exists(Atk.systemMatrixDir), {
				"Atk.systemMatrixDir exists, will not download matrices".warn;
				this.halt;
			}, {
				this.createSystemSupportDir;
			});
		}, {
			if(File.exists(Atk.userMatrixDir), {
				"Atk.userMatrixDir exists, will not download matrices".warn;
				this.halt;
			}, {
				this.createUserSupportDir;
			});
		});
		this.downloadAndUnzip(url, tmpPath, if(useSystemLocation, {this.systemSupportDir}, {this.userSupportDir}), {postf("\nAtk matrices should now be installed. Confirm by reviewing contents of the support directory: %\n\n", if(useSystemLocation, {"Atk.openSystemSupportDir"}, {"Atk.openUserSupportDir"})); action.()});
	}

	*downloadKernels { |useSystemLocation = false, action|
		var tmpPath, url;
		tmpPath = Platform.defaultTempDir ++ this.hash.asString ++ "kernels.zip";
		url = this.kernelsDownloadUrl;
		if(useSystemLocation, {
			if(File.exists(Atk.systemKernelDir), {
				"Atk.systemKernelDir exists, will not download kernels".warn;
				this.halt;
			}, {
				this.createSystemSupportDir;
			});
		}, {
			if(File.exists(Atk.userKernelDir), {
				"Atk.userKernelDir exists, will not download kernels".warn;
				this.halt;
			}, {
				this.createUserSupportDir;
			});
		});
		this.downloadAndUnzip(url, tmpPath, if(useSystemLocation, {this.systemSupportDir}, {this.userSupportDir}), {postf("\nAtk kernels should now be installed. Confirm by reviewing contents of the support directory: %\n\n", if(useSystemLocation, {"Atk.openSystemSupportDir"}, {"Atk.openUserSupportDir"})); action.()});
	}

	*downloadSounds { |useSystemLocation = false, action|
		var tmpPath, url, cmd;
		var oldFolders, newFolders, diffFolders, pathBeforeRenaiming, pathAfterRenaiming;
		tmpPath = Platform.defaultTempDir ++ this.hash.asString ++ "sounds.zip";
		url = this.soundsDownloadUrl;
		if(useSystemLocation, {
			if(File.exists(Atk.systemSoundsDir), {
				"Atk.systemSoundsDir exists, will not download soundfiles".warn;
				this.halt;
			}, {
				this.createSystemSupportDir;
			});
		}, {
			if(File.exists(Atk.userSoundsDir), {
				"Atk.userSoundsDir exists, will not download soundfiles".warn;
				this.halt;
			}, {
				this.createUserSupportDir;
			});
		});
		oldFolders = PathName(if(useSystemLocation, {this.systemSupportDir}, {this.userSupportDir})).folders.collect({|pn| pn.fullPath.asSymbol});
		// "old folders: ".post; oldFolders.postln;
		this.downloadAndUnzip(url, tmpPath, if(useSystemLocation, {this.systemSupportDir}, {this.userSupportDir}), {
			//let's try renaming extracted folder
			newFolders = PathName(if(useSystemLocation, {this.systemSupportDir}, {this.userSupportDir})).folders.collect({|pn| pn.fullPath.asSymbol});
			// "new folders:".postln; newFolders.postln;
			//select only difference between new and old
			diffFolders = newFolders.select({|item| oldFolders.includes(item).not;});
			//select the path name with "sounds" in it
			diffFolders = diffFolders.select({|pathSymbol| pathSymbol.asString.contains("sounds")});
			//continue only if we have a single folder
			if(diffFolders.size == 1, {
				pathBeforeRenaiming = PathName(diffFolders.first.asString);
				pathAfterRenaiming = PathName(pathBeforeRenaiming.fullPath.dirname +/+ "sounds");
				postf("renaming % to %\n", pathBeforeRenaiming.fullPath.withoutTrailingSlash, pathAfterRenaiming.fullPath.withoutTrailingSlash);
				thisProcess.platform.name.switch(
					\windows, {
						cmd = format("move \"%\" \"%\"", pathBeforeRenaiming.fullPath.withoutTrailingSlash, pathAfterRenaiming.fullPath.withoutTrailingSlash);
					}, {//assuming linux/macOS
						cmd = format("mv % %", pathBeforeRenaiming.fullPath.shellQuote, pathAfterRenaiming.fullPath.shellQuote);
					}
				);
				cmd !? {
					// cmd.postln;
					cmd.unixCmd({|err| if(err != 0, {"renaming failed...".postln})})
				};
			});
			postf("\nAtk sounds should now be installed. Confirm by reviewing contents of the support directory: %\n", if(useSystemLocation, {"Atk.openSystemSupportDir"}, {"Atk.openUserSupportDir"}));
			action.()
		});
	}

	*openUserSupportDir {
		if(File.exists(Atk.userSupportDir), {
			Atk.userSupportDir.openOS
		}, {
			"User Support Dir may not exist. Run \n\tAtk.createUserSupportDir\nto create it".warn
		})
	}

	*createUserSupportDir {
		File.mkdir(Atk.userSupportDir);
	}

	*createSystemSupportDir {
		File.mkdir(Atk.systemSupportDir);
	}

	*openSystemSupportDir {
		if(File.exists(Atk.systemSupportDir), {
			Atk.systemSupportDir.openOS
		}, {
			"System Support Dir may not exist.".warn
		})
	}

	*createExtensionsDir {
		var exists = false, dir, ops, mtxTypes, makeDirs;

		ops =	["kernels", "matrices"];
		mtxTypes =	["encoders", "decoders", "xformers"];

		makeDirs = { |baseDir|
			(Atk.sets).do({ |set|
				var path;

				ops.do({ |op|
					mtxTypes.do({ |mtxType|
						path = baseDir +/+ op +/+ set.asString +/+ mtxType;
						File.mkdir(path);
					})
				})
			})
		};

		if(File.exists(userExtensionsDir), {
			exists = true;
			dir = userExtensionsDir
		});

		if(File.exists(systemExtensionsDir), {
			exists = true;
			dir = userExtensionsDir
		});

		if(exists, { ^"ATK extensions directory already found at: %\n".format(dir).warn });

		if(File.exists(userSupportDir), {  // try user dir first
			makeDirs.(Atk.userExtensionsDir)
		}, {
			if(File.exists(systemSupportDir), {  // then system dir
				makeDirs.(systemExtensionsDir)
			}, {
				format(
					"No /ATK directory found in\n\t%\nor\n\t%\n",
					Platform.userAppSupportDir.dirname,
					Platform.systemAppSupportDir.dirname
				).throw
			})
		})
	}

	// op: \matrices, \kernels
	*getAtkOpPath { |op, isExtension = false|
		var str, subPath, kindPath, fullPath, tested;

		tested = List();

		str = switch(op.asSymbol,
			\matrices, { "/matrices" },
			\kernels,  { "/kernels" },
			// include singular
			\matrix,   { "/matrices" },
			\kernel,   { "/kernels" }
		);

		// assume user directory first
		subPath = PathName.new(
			if(isExtension, {
				Atk.userExtensionsDir ++ str
			}, {
				Atk.userSupportDir ++ str
			})
		);

		tested.add(subPath);

		if(subPath.isFolder.not, {   // is  lib installed for user?
			subPath = PathName.new(  // no? check for system wide install
				if(isExtension, {
					Atk.systemExtensionsDir ++ str
				}, {
					Atk.systemSupportDir ++ str
				})
			);
			tested.add(subPath)
		});

		if(subPath.isFolder.not, {
			if(isExtension, {
				format("\nNo extensions folder found at\n\t%\n"
					"You can create it with 'Atk.createExtensionsDir'.",
					tested.join("\nor\n\t")).warn;
				subPath = nil;
			}, {
				Error("Atk: matrices don't appear to be installed. Run 'Atk.downloadMatrices' to attempt automatic installation").throw;
			});
		});

		^subPath
	}

	//  set: \FOA, \HOA1, \HOA2, etc
	//  type: \decoder(s), \encoder(s), \xformer(s)
	//  op: \matrices, \kernels
	*getExtensionSubPath { |set, type, op|
		var subPath, typePath, fullPath;

		Atk.checkSet(set);

		subPath = Atk.getAtkOpPath(op, isExtension: true);

		typePath = PathName.new(
			set.asString.toUpper ++ "/" ++ // folder structure is uppercase
			switch(type.asSymbol,
				\decoders, { "decoders" },
				\encoders, { "encoders" },
				\xformers, { "xformers" },
				// include singular
				\decoder, { "decoders" },
				\encoder, { "encoders" },
				\xformer, { "xformers" }
			)
		);

		fullPath = subPath +/+ typePath;
		Atk.folderExists(fullPath); // throws on fail
		^fullPath
	}

	//  set: \FOA, \HOA1, \HOA2, etc
	//  type: \decoder(s), \encoder(s), \xformer(s)
	//  op: \matrices, \kernels
	*getAtkOpSubPath { |set, type, op|
		var subPath, typePath, fullPath;

		Atk.checkSet(set);

		subPath = Atk.getAtkOpPath(op, isExtension: false);

		typePath = PathName.new(
			set.asString.toUpper ++ "/" ++ // folder structure is uppercase
			switch(type.asSymbol,
				\decoders, { "decoders" },
				\encoders, { "encoders" },
				\xformers, { "xformers" },
				// include singular
				\decoder, { "decoders" },
				\encoder, { "encoders" },
				\xformer, { "xformers" }
			)
		);

		fullPath = subPath +/+ typePath;
		Atk.folderExists(fullPath); // throws on fail
		^fullPath
	}

	// shortcuts for matrices and kernels, aka 'ops'
	*getMatrixExtensionSubPath { |set, type|
		type ?? { Error("Unspecified matrix type. Please specify 'encoder', 'decoder', or 'xformer'.").errorString.postln; ^nil };
		^Atk.getExtensionSubPath(set, type, \matrices);
	}

	*getKernelExtensionSubPath { |set, type|
		type ?? { Error("Unspecified kernel type. Please specify 'encoder', 'decoder', or 'xformer'.").errorString.postln; ^nil };
		^Atk.getExtensionSubPath(set, type, \kernels);
	}

	*getAtkMatrixSubPath { |set, type|
		type ?? { Error("Unspecified matrix type. Please specify 'encoder', 'decoder', or 'xformer'.").errorString.postln; ^nil };
		^Atk.getAtkOpSubPath(set, type, \matrices);
	}

	*getAtkKernelSubPath { |set, type|
		type ?? { Error("Unspecified matrix type. Please specify 'encoder', 'decoder', or 'xformer'.").errorString.postln; ^nil };
		^Atk.getAtkOpSubPath(set, type, \kernels);
	}

	*folderExists { |folderPathName, throwOnFail = true|
		if(folderPathName.isFolder, {
			^true
		}, {
			if(throwOnFail, {
				Error(
					format("No directory found at\n\t%\n", folderPathName.fullPath)
				).errorString.postln;
				this.halt
			}, {
				^false
			})
		})
	}

	// NOTE: could be generalized for other user extensions, e.g. kernels, etc.
	// set: \FOA, \HOA1, \HOA2, etc., required if filePathOrName isn't a full path
	*resolveMtxPath { |filePathOrName, mtxType, set, searchExtensions = false|
		var usrPN, srcPath, relPath, mtxDirPath;
		var hasExtension, hasRelPath;
		var name, matches;
		var relWithoutLast;
		var foundCnt;
		var str;

		usrPN = PathName(filePathOrName); // as PathName

		if(usrPN.isFile, {
			srcPath = usrPN // valid absolute path, easy!
		}, {
			// TODO: consider implementing, if no set provided, recursively
			// search through the appropriate directory for a file matching
			// the name, posting results if multiple are found.
			set ?? { ^nil }; // can't resolve partial path without set

			hasExtension = usrPN.extension.size > 0;
			hasRelPath = usrPN.colonIndices.size > 0;

			mtxDirPath = if(searchExtensions, {
				Atk.getMatrixExtensionSubPath(set, mtxType)
			}, {
				Atk.getAtkMatrixSubPath(set, mtxType)
			});

			relPath = mtxDirPath +/+ usrPN;

			if(hasRelPath, {
				// search specific path within matrix directory
				if(hasExtension, {
					if(relPath.isFile, {
						srcPath = relPath; // valid relative path, with file extension
					}, {
						Error(format("[%:*resolveMtxPath] No file found at\n\t%", this.class.asString, relPath)).throw;
					});
				}, { // user gives a path, but no file extension

					relWithoutLast = PathName(relPath.fullPath.dirname);

					if(relWithoutLast.isFolder, { // test enclosing folder
						foundCnt = 0;
						name = usrPN.fileNameWithoutExtension;

						// NOTE: filesDo searches recursively in the parent folder,
						// so keep track of matches in case there are multiple
						relWithoutLast.filesDo({ |file|
							if(file.fileNameWithoutExtension == name, {
								srcPath = file;
								foundCnt = foundCnt + 1
							})
						});

						if(foundCnt > 1, {
							Error(format(
								"Found multiple matches in recursive search of\n\t%\n"
								"Please provide a more specific path",
								relWithoutLast.fullPath
							)).errorString.postln;
							this.halt
						})
					}, {
						Error(format(
							"Parent directory isn't a folder:\n\t%\n",
							relWithoutLast.fullPath
						)).errorString.postln;
						this.halt
					})
				})
			}, {	// single filename, no other path
				matches = [];

				// name = usrPN.fileNameWithoutExtension;
				name = usrPN.fileName;

				// recursively search whole directory
				mtxDirPath.filesDo { |file|
					var test;

					test = if(hasExtension, {
						file.fileName
					}, {
						file.fileNameWithoutExtension
					});
					(test == name).if({ matches = matches.add(file) })
				};

				case(
					{ matches.size == 1 }, {
						srcPath = matches[0]
					},
					{ matches.size == 0 }, {
						Error("No file found for %".format(name)).errorString.postln;
						this.halt
					},
					{ matches.size > 1 }, {
						str = "Multiple matches found for filename:\t%\n".format(usrPN.fileName);
						matches.do({ |file|
							str = str ++ "\t" ++ file.asRelativePath(mtxDirPath) ++ "\n"
						});
						str = str ++ format(
							"Provide either an absolute path to the matrix, or one relative to\n\t%\n",
							mtxDirPath
						);
						Error(str).errorString.postln; this.halt
					}
				)
			})
		});

		if(srcPath.notNil, {
			^srcPath
		}, {
			Error("No matrix file found!").throw
		})
	}

	// TODO: revisit how available sets are handled.
	// Not ideal to have hardcoded sets in Atk.sets.
	*checkSet { |set|
		if(Atk.sets.includes(set.asString.toUpper.asSymbol).not, {
			Error("Invalid set").errorString.postln;
			this.halt
		})
	}

	// NOTE: could be generalized for other user extensions.
	// e.g. kernels, etc. type: \decoders, \encoders, \xformers
	*postMyMatrices { |set, type|
		var postContents;

		block({ |break|
			if(set.isNil, {
				// no set provided, show all sets
				(Atk.sets).do(Atk.postMyMatrices(_, type));
				break.()
			}, {
				Atk.checkSet(set)
			});

			postf("~ %%% ~\n", set.asString.toUpper, type.notNil.if({ " " }, { "" }), type ?? "");

			postContents = { |folderPN, depth = 1|
				var offset, f_offset;

				offset = ("\t"!depth).join;
				f_offset = ("\t"!(depth - 1)).join;
				postf("%:: % ::\n", f_offset, folderPN.folderName);

				(folderPN.entries).do({ |entry|
					offset = ("\t"!depth).join;
					offset.post;
					if(entry.isFolder, {
						postContents.(entry, depth + 1)
					}, {
						postf("%%\n", offset, entry.fileName)
					})
				})
			};

			postContents.(
				if(type.isNil, {
					Atk.getAtkOpPath(\matrices, isExtension:true) +/+ set.asString.toUpper
				}, {
					if([
						\decoders, \encoders, \xformers,
						\decoder, \encoder, \xformer		// include singular
					].includes(type.asSymbol), {
						Atk.getMatrixExtensionSubPath(set, type)
					}, {
						Error(
							"'type' must be 'decoder', 'encoder', 'xformer', "
							"or nil (to see all matrix directories)"
						).errorString.postln;
						this.halt
					})
				})
			)
		})
	}

}


FoaPanB : MultiOutUGen {

	*ar { |in, azimuth = 0, elevation = 0, mul = 1, add = 0|
		^this.multiNew(\audio, in, azimuth, elevation).madd(mul, add);
	}

	init { |... theInputs|
		inputs = theInputs;
		channels = [OutputProxy(\audio, this, 0), OutputProxy(\audio, this, 1),
					OutputProxy(\audio, this, 2), OutputProxy(\audio, this, 3)];
		^channels
	}

	checkInputs { ^this.checkNInputs(1) }
}


FoaMFreqOsc : FoaUGen  {

	*ar { |freq = 440, phase = 0, azimuthA = 0, elevationA = 0, azimuthR = 0, elevationR = 0, alpha = 0, beta = 0, mul = 1, add = 0|
		var dc, dcl0a, dcl0r, aA, aR, out;

		dc = DC.ar(1);
		dcl0a = 2.sqrt.reciprocal * dc;
		dcl0r = 0 * dc;

		aA = FoaPanB.ar(dc, azimuthA, elevationA, alpha.cos).put(0, dcl0a) * FoaDirectO.ar(Array.fill(4, { dc }), beta);
		aR = FoaPanB.ar(dc, azimuthR, elevationR, alpha.sin).put(0, dcl0r) * FoaDirectO.ar(Array.fill(4, { dc }), beta);

		out = SinOsc.ar(freq, phase + pi/2, aA) + SinOsc.ar(freq, phase, aR);
		out = out.madd(mul, add);
		out = this.checkChans(out);

		^out
	}
}


Foa : MultiOutUGen {

	init { |... theInputs|
		inputs = theInputs;
		channels = [OutputProxy(\audio, this, 0), OutputProxy(\audio, this, 1),
					OutputProxy(\audio, this, 2), OutputProxy(\audio, this, 3)];
		^channels
	}

	 checkInputs { ^this.checkNInputs(4) }

	*checkChans { |in|
		if(in.size < 4, {
			^([in] ++ (4 - in.size).collect({ Silent.ar })).flat
		}, {
			^in
		})
	}

}

FoaDirectO : Foa {
	*ar { |in, angle = (pi/2), mul = 1, add = 0|
		var w, x, y, z;

		in = this.checkChans(in);
		#w, x, y, z = in;
		^this.multiNew(\audio, w, x, y, z, angle).madd(mul, add);
	}
}


FoaDirectX : Foa {
	*ar { |in, angle = (pi/2), mul = 1, add = 0|
		var w, x, y, z;

		in = this.checkChans(in);
		#w, x, y, z = in;
		^this.multiNew(\audio, w, x, y, z, angle).madd(mul, add);
	}
}

FoaDirectY : FoaDirectX { }
FoaDirectZ : FoaDirectX { }

FoaRotate : Foa {
	*ar { |in, angle = 0, mul = 1, add = 0|
		var w, x, y, z;

		in = this.checkChans(in);
		#w, x, y, z = in;
		^this.multiNew(\audio, w, x, y, z, angle).madd(mul, add);
	}
}
FoaTilt : FoaRotate { }
FoaTumble : FoaRotate { }

FoaFocusX : FoaRotate { }
FoaFocusY : FoaRotate { }
FoaFocusZ : FoaRotate { }

FoaPushX : FoaRotate { }
FoaPushY : FoaRotate { }
FoaPushZ : FoaRotate { }

FoaPressX : FoaRotate { }
FoaPressY : FoaRotate { }
FoaPressZ : FoaRotate { }

FoaZoomX : FoaRotate { }
FoaZoomY : FoaRotate { }
FoaZoomZ : FoaRotate { }

FoaBalance {
	*ar { |in, angle = 0, mul = 1, add = 0|
		var w, x, y, z;

		in = this.checkChans(in);
		#w, x, y, z = in;
		^FoaZoomY.ar(w, x, y, z, angle, mul, add);
	}
}


FoaDominateX : Foa {
	*ar { |in, gain = 0, mul = 1, add = 0|
		var w, x, y, z;

		in = this.checkChans(in);
		#w, x, y, z = in;
		^this.multiNew(\audio, w, x, y, z, gain).madd(mul, add);
	}
}

FoaDominateY : FoaDominateX { }
FoaDominateZ : FoaDominateX { }

FoaAsymmetry : FoaRotate { }


FoaRTT {
	*ar { |in, rotAngle = 0, tilAngle = 0, tumAngle = 0, mul = 1, add = 0|
		in = FoaRotate.ar(in, rotAngle);
		in = FoaTilt.ar(in, tilAngle);
		^FoaTumble.ar(in, tumAngle, mul, add);
	}
}

FoaMirror {
	*ar { |in, theta = 0, phi = 0, mul = 1, add = 0|
		in = FoaRotate.ar(in, theta.neg);
		in = FoaTumble.ar(in, phi.neg);
		in = FoaXform.ar(in, FoaXformerMatrix.newMirrorX);
		in = FoaTumble.ar(in, phi);
		^FoaRotate.ar(in, theta, mul, add);
	}
}

FoaDirect {
	*ar { |in, angle = 0, theta = 0, phi = 0, mul = 1, add = 0|

		in = FoaRotate.ar(in, theta.neg);
		in = FoaTumble.ar(in, phi.neg);
		in = FoaDirectX.ar(in, angle);
		in = FoaTumble.ar(in, phi);
		^FoaRotate.ar(in, theta, mul, add);
	}
}

FoaDominate {
	*ar { |in, gain = 0, theta = 0, phi = 0, mul = 1, add = 0|

		in = FoaRotate.ar(in, theta.neg);
		in = FoaTumble.ar(in, phi.neg);
		in = FoaDominateX.ar(in, gain);
		in = FoaTumble.ar(in, phi);
		^FoaRotate.ar(in, theta, mul, add);
	}
}

FoaZoom {
	*ar { |in, angle = 0, theta = 0, phi = 0, mul = 1, add = 0|

		in = FoaRotate.ar(in, theta.neg);
		in = FoaTumble.ar(in, phi.neg);
		in = FoaZoomX.ar(in, angle);
		in = FoaTumble.ar(in, phi);
		^FoaRotate.ar(in, theta, mul, add);
	}
}

FoaFocus {
	*ar { |in, angle = 0, theta = 0, phi = 0, mul = 1, add = 0|

		in = FoaRotate.ar(in, theta.neg);
		in = FoaTumble.ar(in, phi.neg);
		in = FoaFocusX.ar(in, angle);
		in = FoaTumble.ar(in, phi);
		^FoaRotate.ar(in, theta, mul, add);
	}
}

FoaPush {
	*ar { |in, angle = 0, theta = 0, phi = 0, mul = 1, add = 0|

		in = FoaRotate.ar(in, theta.neg);
		in = FoaTumble.ar(in, phi.neg);
		in = FoaPushX.ar(in, angle);
		in = FoaTumble.ar(in, phi);
		^FoaRotate.ar(in, theta, mul, add);
	}
}

FoaPress {
	*ar { |in, angle = 0, theta = 0, phi = 0, mul = 1, add = 0|

		in = FoaRotate.ar(in, theta.neg);
		in = FoaTumble.ar(in, phi.neg);
		in = FoaPressX.ar(in, angle);
		in = FoaTumble.ar(in, phi);
		^FoaRotate.ar(in, theta, mul, add);
	}
}


//------------------------------------------------------------------------
// Filters

FoaProximity : Foa {
	*ar { |in, distance = 1, mul = 1, add = 0|
		var w, x, y, z;
		var speedOfSound = AtkFoa.speedOfSound;

		in = this.checkChans(in);
		#w, x, y, z = in;
		^this.multiNew(\audio, w, x, y, z, distance, speedOfSound).madd(mul, add);
	}

}

FoaNFC : Foa {
	*ar { |in, distance = 1, mul = 1, add = 0|
		var w, x, y, z;
		var speedOfSound = AtkFoa.speedOfSound;

		in = this.checkChans(in);
		#w, x, y, z = in;
		^this.multiNew(\audio, w, x, y, z, distance, speedOfSound).madd(mul, add);
	}

}

FoaPsychoShelf : Foa {
	*ar { |in, freq = 400, k0 = ((3/2).sqrt), k1 = (3.sqrt/2), mul = 1, add = 0|
		var w, x, y, z;

		in = this.checkChans(in);
		#w, x, y, z = in;
		^this.multiNew(\audio, w, x, y, z, freq, k0, k1).madd(mul, add);
	}

}


//------------------------------------------------------------------------
// AtkMatrixMix & AtkKernelConv

AtkMatrixMix {
	*ar { |in, matrix, mul = 1, add = 0|
		var out;

		// wrap input as array if needed, for mono inputs
		(in.isArray.not).if({ in = [in] });

		out = Mix.fill(matrix.cols, { |i| // fill input
			UGen.replaceZeroesWithSilence(
				matrix.flop.asArray[i] * in[i]
			)
		});

		^out.madd(mul, add)
	}
}

AtkKernelConv {
	*ar { |in, kernel, mul = 1, add = 0|
		var out;

		// wrap input as array if needed, for mono inputs
		(in.isArray.not).if({ in = [in] });

		out = Mix.new(
			(kernel.shape[0]).collect({ |i|
				(kernel.shape[1]).collect({ |j|
					Convolution2.ar(
						in[i],
						kernel[i][j],
						framesize: kernel[i][j].numFrames
					)
				})
			})
		);

		^out.madd(mul, add)
	}
}


//------------------------------------------------------------------------
// Decoder built using AtkMatrixMix & AtkKernelConv

FoaUGen {
	*checkChans { |in|
		if(in.size < 4, {
			^([in] ++ (4 - in.size).collect({ Silent.ar })).flat
		}, {
			^in
		})
	}

	*argDict { |ugen, args, argDefaults|
		var index, userDict;
		var ugenKeys;
		var ugenDict;

		// find ugen args, drop ['this', sig]
		ugenKeys = ugen.class.findRespondingMethodFor(\ar).argNames.drop(2);
		ugenDict = Dictionary.new;
		ugenKeys.do({ |key, i| ugenDict.put(key, argDefaults[i]) });

		// find index dividing ordered and named args
		index = args.detectIndex({ |item| ugenKeys.matchItem(item) });

		// build user dictionary
		userDict = Dictionary.new(ugenKeys.size);
		if((index == nil).not, {
			userDict = userDict.putAll(Dictionary.newFrom(args[index..]))
		}, {
			index = args.size
		});
		userDict = userDict.putAll(
			Dictionary.newFrom(
				index.collect({ |i|
					[ugenKeys[i], args[i]]
				}).flat
			)
		);

		// merge
		^ugenDict.merge(userDict, { |ugenArg, userArg|
			(userArg.notNil).if{ userArg }
		})
	}
}

FoaDecode : FoaUGen {
	*ar { |in, decoder, mul = 1, add = 0|
		in = this.checkChans(in);

		case(
			{ decoder.isKindOf(FoaDecoderMatrix) }, {

				if(decoder.shelfFreq.isNumber, { // shelf filter?
					in = FoaPsychoShelf.ar(in,
						decoder.shelfFreq, decoder.shelfK[0], decoder.shelfK[1])
				});

				^AtkMatrixMix.ar(in, decoder.matrix, mul, add)
			},
			{ decoder.isKindOf(FoaDecoderKernel) }, {
				^AtkKernelConv.ar(in, decoder.kernel, mul, add)
			}
		)
	}
}


//------------------------------------------------------------------------
// Encoder built using AtkMatrixMix & AtkKernelConv

FoaEncode : FoaUGen {
	*ar { |in, encoder, mul = 1, add = 0|
		var out;

		case(
			{ encoder.isKindOf(FoaEncoderMatrix) }, {
				out = AtkMatrixMix.ar(in, encoder.matrix, mul, add)
			},
			{ encoder.isKindOf(FoaEncoderKernel) }, {
				out = AtkKernelConv.ar(in, encoder.kernel, mul, add)
			}
		);

//		if (out.size < 4, {			// 1st order, fill missing harms with zeros
//			out = out ++ Silent.ar(4 - out.size)
//		});
		out = this.checkChans(out);
		^out
	}
}


//------------------------------------------------------------------------
// Transformer built using AtkMatrixMix & AtkKernelConv

FoaXform : FoaUGen {
	*ar { |in, xformer, mul = 1, add = 0|
		var out;

		in = this.checkChans(in);

//		switch(xformer.class,
//
//			FoaXformerMatrix, {
//				out = AtkMatrixMix.ar(in, xformer.matrix, mul, add)
//			},
//
//			FoaXformerKernel, {
//				out = AtkKernelConv.ar(in, xformer.kernel, mul, add)
//			}
//		);
//
//		^out

		// for now...
		^AtkMatrixMix.ar(in, xformer.matrix, mul, add)
	}
}


//------------------------------------------------------------------------
// Transformer: UGen wrapper
/*
argument key - see helpfile for reasonable values
\rtt - angle

*/

FoaTransform : FoaUGen {
	*ar { |in, kind ... args|
		var argDict, argDefaults;
		var ugen;

		in = this.checkChans(in);

//		argDict = { |ugen, args, argDefaults|
//			var index, userDict;
//			var ugenKeys;
//			var ugenDict;
//			[ugen, args, argDefaults].postln;
//			// find index dividing ordered and named args
//			index = args.detectIndex({ |item| item.isKindOf(Symbol) });
//
//			// find ugen args, drop [\this, w, x, y, z]
//			ugenKeys = ugen.class.findRespondingMethodFor(\ar).argNames.drop(2);
//			ugenDict = Dictionary.new;
//			ugenKeys.do({ |key, i| ugenDict.put(key, argDefaults[i]) });
//
//			// build user dictionary
//			userDict = Dictionary.new(ugenKeys.size);
//			(index == nil).not.if({
//				userDict = userDict.putAll(Dictionary.newFrom(args[index..]));
//			}, {
//				index = args.size;
//			});
//			userDict = userDict.putAll(Dictionary.newFrom((index).collect({ |i|
//				[ugenKeys[i], args[i]] }).flat));
//
//			// merge
//			ugenDict.merge(userDict, {
//				|ugenArg, userArg| (userArg != nil).if({ userArg })
//			})
//		};
//

		switch(kind,

			\rotate, {

				ugen = FoaRotate;
				argDefaults = [0, 1, 0];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\angle), argDict.at(\mul), argDict.at(\add)
				)
			},

			\tilt, {

				ugen = FoaTilt;
				argDefaults = [0, 1, 0];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\angle), argDict.at(\mul), argDict.at(\add)
				)
			},

			\tumble, {

				ugen = FoaTumble;
				argDefaults = [0, 1, 0];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\angle), argDict.at(\mul), argDict.at(\add)
				)
			},

			\directO, {

				ugen = FoaDirectO;
				argDefaults = [0, 1, 0];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\angle), argDict.at(\mul), argDict.at(\add)
				)
			},

			\directX, {

				ugen = FoaDirectX;
				argDefaults = [0, 1, 0];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\angle), argDict.at(\mul), argDict.at(\add)
				)
			},

			\directY, {

				ugen = FoaDirectY;
				argDefaults = [0, 1, 0];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\angle), argDict.at(\mul), argDict.at(\add)
				)
			},

			\directZ, {

				ugen = FoaDirectZ;
				argDefaults = [0, 1, 0];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\angle), argDict.at(\mul), argDict.at(\add)
				)
			},

			\dominateX, {

				ugen = FoaDominateX;
				argDefaults = [0, 1, 0];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\gain), argDict.at(\mul), argDict.at(\add)
				)
			},

			\dominateY, {

				ugen = FoaDominateY;
				argDefaults = [0, 1, 0];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\gain), argDict.at(\mul), argDict.at(\add)
				)
			},

			\dominateZ, {

				ugen = FoaDominateZ;
				argDefaults = [0, 1, 0];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\gain), argDict.at(\mul), argDict.at(\add)
				)
			},

			\zoomX, {

				ugen = FoaZoomX;
				argDefaults = [0, 1, 0];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\angle), argDict.at(\mul), argDict.at(\add)
				)
			},

			\zoomY, {

				ugen = FoaZoomY;
				argDefaults = [0, 1, 0];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\angle), argDict.at(\mul), argDict.at(\add)
				)
			},

			\zoomZ, {

				ugen = FoaZoomZ;
				argDefaults = [0, 1, 0];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\angle), argDict.at(\mul), argDict.at(\add)
				)
			},

			\focusX, {

				ugen = FoaFocusX;
				argDefaults = [0, 1, 0];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\angle), argDict.at(\mul), argDict.at(\add)
				)
			},

			\focusY, {

				ugen = FoaFocusY;
				argDefaults = [0, 1, 0];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\angle), argDict.at(\mul), argDict.at(\add)
				)
			},

			\focusZ, {

				ugen = FoaFocusZ;
				argDefaults = [0, 1, 0];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\angle), argDict.at(\mul), argDict.at(\add)
				)
			},

			\pushX, {

				ugen = FoaPushX;
				argDefaults = [0, 1, 0];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\angle), argDict.at(\mul), argDict.at(\add)
				)
			},

			\pushY, {

				ugen = FoaPushY;
				argDefaults = [0, 1, 0];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\angle), argDict.at(\mul), argDict.at(\add)
				)
			},

			\pushZ, {

				ugen = FoaPushZ;
				argDefaults = [0, 1, 0];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\angle), argDict.at(\mul), argDict.at(\add)
				)
			},

			\pressX, {

				ugen = FoaPressX;
				argDefaults = [0, 1, 0];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\angle), argDict.at(\mul), argDict.at(\add)
				)
			},

			\pressY, {

				ugen = FoaPressY;
				argDefaults = [0, 1, 0];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\angle), argDict.at(\mul), argDict.at(\add)
				)
			},

			\pressZ, {

				ugen = FoaPressZ;
				argDefaults = [0, 1, 0];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\angle), argDict.at(\mul), argDict.at(\add)
				)
			},

			\asymmetry, {

				ugen = FoaAsymmetry;
				argDefaults = [0, 1, 0];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\angle), argDict.at(\mul), argDict.at(\add)
				)
			},

			\balance, {

				ugen = FoaZoomY;
				argDefaults = [0, 1, 0];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\angle), argDict.at(\mul), argDict.at(\add)
				)
			},

			\rtt, {

				ugen = FoaRTT;
				argDefaults = [0, 0, 0, 1, 0];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\rotAngle), argDict.at(\tilAngle), argDict.at(\tumAngle),
					argDict.at(\mul), argDict.at(\add)
				)
			},

			\mirror, {

				ugen = FoaMirror;
				argDefaults = [0, 0, 1, 0];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\theta), argDict.at(\phi),
					argDict.at(\mul), argDict.at(\add)
				)
			},

			\direct, {

				ugen = FoaDirect;
				argDefaults = [0, 0, 0, 1, 0];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\angle), argDict.at(\theta), argDict.at(\phi),
					argDict.at(\mul), argDict.at(\add)
				)
			},

			\dominate, {

				ugen = FoaDominate;
				argDefaults = [0, 0, 0, 1, 0];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\gain), argDict.at(\theta), argDict.at(\phi),
					argDict.at(\mul), argDict.at(\add)
				)
			},

			\zoom, {

				ugen = FoaZoom;
				argDefaults = [0, 0, 0, 1, 0];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\angle), argDict.at(\theta), argDict.at(\phi),
					argDict.at(\mul), argDict.at(\add)
				)
			},

			\focus, {

				ugen = FoaFocus;
				argDefaults = [0, 0, 0, 1, 0];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\angle), argDict.at(\theta), argDict.at(\phi),
					argDict.at(\mul), argDict.at(\add)
				)
			},

			\push, {

				ugen = FoaPush;
				argDefaults = [0, 0, 0, 1, 0];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\angle), argDict.at(\theta), argDict.at(\phi),
					argDict.at(\mul), argDict.at(\add)
				)
			},

			\press, {

				ugen = FoaPress;
				argDefaults = [0, 0, 0, 1, 0];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\angle), argDict.at(\theta), argDict.at(\phi),
					argDict.at(\mul), argDict.at(\add)
				)
			},

			\nfc, {

				ugen = FoaNFC;
				argDefaults = [1, 1, 0];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\distance),
					argDict.at(\mul), argDict.at(\add)
				)
			},

			\proximity, {

				ugen = FoaProximity;
				argDefaults = [1, 1, 0];

				argDict = this.argDict(ugen, args, argDefaults);

				^ugen.ar(
					in,
					argDict.at(\distance),
					argDict.at(\mul), argDict.at(\add)
				)
			}
		)
	}
}
