package com.servoy.eclipse.debug.handlers;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;

import org.eclipse.jface.util.Util;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.browser.AbstractWebBrowser;
import org.eclipse.ui.internal.browser.IBrowserDescriptor;
import org.eclipse.ui.internal.browser.Trace;
import org.eclipse.ui.internal.browser.WebBrowserUtil;

/**
 * An instance of a running Web browser. rundll32.exe
 * url.dll,FileProtocolHandler www.ibm.com
 */
public class ExternalBrowserInstance extends AbstractWebBrowser
{
	protected IBrowserDescriptor browser;

	protected Process process;

	public ExternalBrowserInstance(String id, IBrowserDescriptor browser)
	{
		super(id);
		this.browser = browser;
	}

	public void openURL(URL url) throws PartInitException
	{
		final String urlText = url == null ? null : url.toExternalForm();

		ArrayList<String> cmdOptions = new ArrayList<String>();
		String location = browser.getLocation();
		cmdOptions.add(location);
		String parameters = browser.getParameters();
		Trace.trace(Trace.FINEST, "Launching external Web browser: " + location + " - " + parameters + " - " + urlText); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		// For MacOS X .app, we use open(1) to launch the app for the given URL
		// The order of the arguments is specific:
		//
		// open -a APP URL --args PARAMETERS
		//
		// As #createParameterArray() will append the URL to the end if %URL%
		// isn't found, we only include urlText if the parameters makes
		// reference to %URL%. This could mean that %URL% is specified
		// twice on the command line (e.g., "open -a XXX URL --args XXX URL
		// %%%") but presumably the user means to do that.
		boolean isMacBundle = Util.isMac() && isMacAppBundle(location);
		boolean includeUrlInParams = !isMacBundle || (parameters != null && parameters.contains(IBrowserDescriptor.URL_PARAMETER));
		String[] params = WebBrowserUtil.createParameterArray(parameters, includeUrlInParams ? urlText : null);

		try
		{
			if (isMacBundle)
			{
				cmdOptions.add(0, "-a"); //$NON-NLS-1$
				cmdOptions.add(0, "open"); //$NON-NLS-1$
				if (urlText != null)
				{
					cmdOptions.add(urlText);
				}
				// --args supported in 10.6 and later
				if (params.length > 0)
				{
					cmdOptions.add("--args");//$NON-NLS-1$
				}
			}

			for (String param : params)
			{
				cmdOptions.add(param);
			}
			String[] cmd = cmdOptions.toArray(new String[cmdOptions.size()]);
			Trace.trace(Trace.FINEST, "Launching " + join(" ", cmd)); //$NON-NLS-1$//$NON-NLS-2$

			process = Runtime.getRuntime().exec(cmd);
		}
		catch (Exception e)
		{
			Trace.trace(Trace.SEVERE, "Could not launch external browser", e); //$NON-NLS-1$
			WebBrowserUtil.openError(NLS.bind("Could not launch external web browser for {0}. Check the Web Browser preferences.", urlText));
		}
	}

	/**
	 * @return true if the location appears to be a Mac Application bundle
	 *         (.app)
	 */
	private boolean isMacAppBundle(String location)
	{
		// A very quick heuristic based on Apple's Bundle Programming Guide
		// https://developer.apple.com/library/mac/documentation/CoreFoundation/Conceptual/CFBundles/BundleTypes/BundleTypes.html#//apple_ref/doc/uid/10000123i-CH101-SW19
		File bundleLoc = new File(location);
		File macosDir = new File(new File(bundleLoc, "Contents"), "MacOS"); //$NON-NLS-1$ //$NON-NLS-2$
		File plist = new File(new File(bundleLoc, "Contents"), "Info.plist"); //$NON-NLS-1$ //$NON-NLS-2$
		return bundleLoc.isDirectory() && macosDir.isDirectory() && plist.isFile();
	}

	private String join(String delim, String... data)
	{
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < data.length; i++)
		{
			sb.append(data[i]);
			if (i >= data.length - 1)
			{
				break;
			}
			sb.append(delim);
		}
		return sb.toString();
	}


	@Override
	public boolean close()
	{
		try
		{
			process.destroy();
			return true;
		}
		catch (Exception e)
		{
			return false;
		}
	}
}