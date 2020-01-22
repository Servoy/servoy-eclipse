Download all the libraries (all jars except installer.jar) from https://build.servoy.com:/latest/[branch_name]/ (or specific version dir)  who's name postfix matched the value 
of the releaseNumber variable in the ClientVersion class of the checked-out source code (or in case of master branch - just master) and place them in this directory

then make sure that you use as eclipse plug-in target configuration the correct one from this project - which includes the libs_extra folder in it.