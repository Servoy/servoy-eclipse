Download all the libraries (all jars except installer.jar) from https://build.servoy.com:/latest/[branch_name]/ (or specific version dir)  who's name postfix matched the value 
of the releaseNumber variable in the ClientVersion class of the checked-out source code (or in case of master branch - just master) and place them in this directory

then make sure that you use as eclipse plugin target configuration either open_source.target or open_source_egit.target which do include this folder.