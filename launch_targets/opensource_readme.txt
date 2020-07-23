For using a target for the open source development of Servoy
You need to download the the server binaries from:  https://build.servoy.com/latest (see: https://wiki.servoy.com/display/DOCS/Setting+Up)
and then make a copy fo the com.servoy.eclipse.target.target file and add teh "libs_extra" folder where you have placed the server jars in as an extra plugins dir in the target.
And make sure that target is what is set in eclipse as the main compile/runtime target.
 