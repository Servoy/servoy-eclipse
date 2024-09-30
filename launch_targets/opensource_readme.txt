This doc is explaining how to debug through an installer of servoy (that is the latest release)
If this is an lts then instead of the "release branch" the "lts" or "lts_latest" branch should be checked out.


take an installer from: https://download.eclipse.org/eclipse/downloads/
the zip of Eclipse SDK

unzip this and start it up and point to a new folder for the workspace location question.

configure eclipse so that it has a Java 21 (run against) and a Java 17 (compile against) installed:
Window>preferences->java>installed jres> add both, point to a root dir of an install of both, java 17 can be default selected

checkout 4 git repos

https://github.com/Servoy/rhino.git (servoy branch)

https://github.com/Servoy/servoy-client.git (release branch)

https://github.com/Servoy/servoy-eclipse.git (release branch)

https://github.com/Servoy/sablo.git (release branch) (not directly needed, more difficult to setup needs Maven, so ignored for now)




This checkout can be done by any git tool, if you want to use eclipse you need to install "git"
Help -> Install -> select the url with the year like: https://download.eclipse.org/releases/2024-09
and then search for "git" and install "git integration for eclipse"

After you have checked out the 4 git then you can import the right projects from those dirs
File->Import->General->Existing projects into Workspace

first import the root of rhino (should result in a org.eclipse.dltk.javascript.rhino project)

then import direct sub dirs of the "servoy-client" dir (servoy_base, servoy_debug, servoy_doc,servoy_headless_client,servoy_ngclient,servoy_shared,servoy_smart_client) (don't include servoy_ngclient.tests or close that project)

then import all the direct sub dirs of "servoy-eclipse"

got to the "launch_targets" project:

open the "open_source.target" file and change the url (https://download.servoy.com/developer/latest/4001) if needed to a different buildnumber that you run against.
If the url is changed then the versions also change, just select all categories that are listed there.

The press Set as Target platform (or Reload Target Platform)

The error "An API baseline has not been set for the current workspace" can be quick fixed (context menu) and then set the severity to ignore in the base line preference page.


Open the Debug Launch - > Run Configurations.  Under Eclipse Application there is a opensource launch.  In 3 places you need to point to an existing Servoy Installation:

1> Main Tab-> Workspace Data -> Location: [workspacelocation]

2> Arguments Tab-> VM Arguments: -Dservoy.application_server.dir="[install]\application_server"

3> Arguments Tab-> Working directory -> Other: [install]/developer


then you should be able to launch this target.


