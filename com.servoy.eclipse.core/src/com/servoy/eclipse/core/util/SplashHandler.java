/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

 This program is free software; you can redistribute it and/or modify it under
 the terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along
 with this program; if not, see http://www.gnu.org/licenses or write to the Free
 Software Foundation,Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
 */
package com.servoy.eclipse.core.util;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.splash.BasicSplashHandler;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.servoy.eclipse.core.Activator;
import com.servoy.j2db.ClientVersion;
import com.servoy.j2db.server.starter.ILicenseManager;
import com.servoy.j2db.util.Utils;

public class SplashHandler extends BasicSplashHandler
{
	static IProgressMonitor progressMonitor;

	@Override
	public void init(final Shell splash)
	{
		java.awt.Toolkit.getDefaultToolkit(); // initialize toolkit here to prevent deadlock on the mac
		try
		{
			new Thread(new Runnable()
			{
				public void run()
				{
//					ServoyModel.startAppServer();
					if (!splash.isDisposed()) splash.getDisplay().wake();
				}

			}).start();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		setProgressRect(new Rectangle(0, 605, 600, 20));
		super.init(splash);
		progressMonitor = getBundleProgressMonitor();
		getContent(); // ensure creation of the progress
		showBackground(splash, false);
	}

	private void showBackground(final Shell splash, boolean doGetRegisterString)
	{
		if (splash.isDisposed()) return;

		Image background = splash.getBackgroundImage();
		if (background != null)
		{
			GC gc = new GC(background);
			// size is very different on the three os
			String osName = System.getProperty("os.name").toLowerCase();
			if (osName.contains("windows")) gc.setFont(new Font(splash.getDisplay(), "SansSerif", 8, SWT.NORMAL));
			else if (osName.contains("mac")) gc.setFont(new Font(splash.getDisplay(), "SansSerif", 9, SWT.NORMAL));
			else if (osName.contains("linux")) gc.setFont(new Font(splash.getDisplay(), "SansSerif", 8, SWT.NORMAL));

//			Color color = new Color(splash.getDisplay(), 255, 0, 0);
			StringBuffer text = getSplashText(doGetRegisterString);
			gc.drawText(text.toString(), 10, 540, true);
//			gc.setBackground(color);
			gc.getFont().dispose();
			gc.dispose();
//			color.dispose();

//			if (!doGetRegisterString)
//			{
//				splash.getDisplay().asyncExec(new Runnable()
//				{
//					public void run()
//					{
//						SplashHandler.this.showBackground(splash, true);
//					}
//				});
//			}
		}
	}

	private StringBuffer getSplashText(boolean doGetRegisterString)
	{
		StringBuffer text = new StringBuffer();
		text.append("Version ");
		text.append(ClientVersion.getVersion());
		text.append(" (");
		text.append(ClientVersion.getBuildDate());
		text.append(")\n");
		text.append("This program is protected by international\ncopyright laws as described in Help About");
		text.append("\nCopyright \u00A9 Servoy BV 1997 - " + Utils.formatTime(System.currentTimeMillis(), "yyyy"));

		if (doGetRegisterString)
		{
//			//ugly near busy wait loop
//			try
//			{
//				int count = 0;
//				while (ApplicationServerSingleton.get() == null)
//				{
//					Thread.sleep(200);
//					count++;
//					if (count > 50) break;
//				}
//			}
//			catch (InterruptedException e)
//			{
//				//ignore
//			}

			BundleContext context = Activator.getBundleContext();
			ServiceReference<ILicenseManager> ref = context.getServiceReference(ILicenseManager.class);
			ILicenseManager lm = context.getService(ref);
			String regText = lm.getDeveloperRegistrationText();
			text.append("\n");
			text.append(regText);
		}
		return text;
	}
}
