// from wslib - swiped to avoid the dependency in ATK

+ String {

    isFile { ^PathName( this ).isFile; }
    isFolder { ^PathName( this ).isFolder; } // can also be bundle

    isSoundFile { var sf;
        if( this.isFile )
        { sf = SoundFile.new;
            if( sf.openRead( this.standardizePath ) )
            { sf.close;
                ^true }
            { ^false };
        }
        { ^false  }
    }

    extension { var ext; // returns only if valid
        ext = this.splitext.last;
        if( ext.isValidExtension )
        { ^ext.removeFwdSlash } // removes fwdSlash from bundle paths
        { ^nil }
    }
}
