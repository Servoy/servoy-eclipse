package com.servoy.eclipse.core.tomat;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.tomcat.starter.ITomcatStartedListener;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.Utils;

public class TomcatStartedListener implements ITomcatStartedListener
{
	@Override
	public void started()
	{
		try
		{
			URL url = new URL("http://localhost:" + ApplicationServerRegistry.get().getWebServerPort() + "/testdevelopertomcat");
			String uuid = Utils.getURLContent(url);
			if (!Utils.stringSafeEquals(uuid, TomcatTesterServlet.UNIQUE_VALUE.toString()))
			{
				Display.getDefault().asyncExec(() -> {
					String message = "Another developer is running this thise url: http://localhost:" +
						ApplicationServerRegistry.get().getWebServerPort() +
						"/ please close that one or use different ports in application_server/server/conf/server.xml for one install";
					if (uuid == null || "".equals(uuid))
					{
						message = "Please check your startup if you have something else running at http://localhost:" +
							ApplicationServerRegistry.get().getWebServerPort() + "/";
					}
					MessageDialog.openWarning(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
						"Internal Tomcat Webserver not started up correctly",
						message);
				});
			}
		}
		catch (MalformedURLException e)
		{
		}
	}
}
