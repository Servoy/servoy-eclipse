package com.servoy.eclipse.core.tomcat;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.tomcat.starter.ITomcatStartedListener;
import org.eclipse.swt.widgets.Display;

import com.servoy.eclipse.core.util.ServoyMessageDialog;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.Utils;

public class TomcatStartedListener implements ITomcatStartedListener
{
	@Override
	public void started()
	{
		URL url = null;
		try
		{
			url = new URI("http://localhost:" + ApplicationServerRegistry.get().getWebServerPort() + "/testdevelopertomcat").toURL();
		}
		catch (URISyntaxException | MalformedURLException e)
		{
			return; // should never happen with a numeric port
		}

		String uuid = Utils.getURLContent(url, null);

		if (!Utils.stringSafeEquals(uuid, TomcatTesterServlet.UNIQUE_VALUE.toString()))
		{
			Display.getDefault().asyncExec(() -> {
				String message;
				if (uuid == null)
				{
					message = "Please check your startup to see if you have something else running at\nhttp://localhost:" +
						ApplicationServerRegistry.get().getWebServerPort() + "/";
				}
				else
				{
					message = "Another developer is probably running at this url: http://localhost:" +
						ApplicationServerRegistry.get().getWebServerPort() +
						"/\nPlease close that one or use different ports in the application_server/server/conf/server.xml\nfile for one of the two installs.";
				}

				ServoyMessageDialog.openWarning(UIUtils.getActiveShell(),
					"Internal Tomcat Webserver did not start up correctly",
					message);
			});
		}
	}
}
