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

package com.servoy.eclipse.marketplace;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;

import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.ClientVersion;
import com.servoy.j2db.util.Utils;

/**
 * Editor used to show the Servoy Marketplace. 
 *  
 * @author gboros
 */
public class MarketplaceBrowserEditor extends EditorPart
{
	public static final String MARKETPLACE_BROWSER_EDITOR_ID = "com.servoy.eclipse.marketplace.MarketplaceBrowserEditor"; //$NON-NLS-1$
	public static final String MARKETPLACE_URL = "https://crm.servoy.com/servoy-webclient/ss/s/marketplace"; //$NON-NLS-1$
	//public static final String MARKETPLACE_URL = "http://localhost:8080/servoy-webclient/ss/s/downloadLink"; //$NON-NLS-1$
	public static final String MARKETPLACE_XML_VERSION = "1.0"; //$NON-NLS-1$
	private static final String PARAM_SERVOY_VERSION = "servoyVersion"; //$NON-NLS-1$
	private static final String PARAM_MARKETPLACE_XML_VERSION = "marketplaceXMLVersion"; //$NON-NLS-1$ 
	private static final String PARAM_PLATFORM = "platform"; //$NON-NLS-1$
	public static final MarketplaceBrowserEditorInput INPUT = new MarketplaceBrowserEditorInput();

	private Browser browser;
	private String url;

	/*
	 * @see org.eclipse.ui.part.EditorPart#doSave(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void doSave(IProgressMonitor monitor)
	{
		// ignore
	}

	/*
	 * @see org.eclipse.ui.part.EditorPart#doSaveAs()
	 */
	@Override
	public void doSaveAs()
	{
		// ignore
	}

	/*
	 * @see org.eclipse.ui.part.EditorPart#init(org.eclipse.ui.IEditorSite, org.eclipse.ui.IEditorInput)
	 */
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException
	{
		setSite(site);
		setInput(input);
	}

	/*
	 * @see org.eclipse.ui.part.EditorPart#isDirty()
	 */
	@Override
	public boolean isDirty()
	{
		return false;
	}

	/*
	 * @see org.eclipse.ui.part.EditorPart#isSaveAsAllowed()
	 */
	@Override
	public boolean isSaveAsAllowed()
	{
		return false;
	}

	/*
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(Composite parent)
	{
		browser = new Browser(parent, SWT.NONE);
		url = new StringBuffer(MARKETPLACE_URL).append("&").append(PARAM_SERVOY_VERSION).append("=").append(ClientVersion.getBundleVersion()).append("&").append(PARAM_MARKETPLACE_XML_VERSION).append("=").append(MARKETPLACE_XML_VERSION).append("&").append(PARAM_PLATFORM).append("=").append(Utils.getPlatformAsString()).toString(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		browser.setUrl(url, null, new String[] { "Cache-Control: no-cache" }); //$NON-NLS-1$

		browser.addLocationListener(new LocationAdapter()
		{
			@Override
			public void changing(LocationEvent event)
			{
				// if install link
				if (event.location.endsWith("marketplace_test.xml"))
				{
					try
					{
						// get the link and start install
						InstallPackage installPackage = new InstallPackage(event.location);

						for (final InstallItem installItem : installPackage.getAllInstallItems())
						{
							if (MessageDialog.openConfirm(UIUtils.getActiveShell(), "Servoy Marketplace",
								"You have choosen to install the follwing product from the Servoy Marketplace\n\n" + installItem.getName() +
									"\n\nProceed with the install ?"))
							{
								try
								{
									MarketplaceProgressMonitorDialog progressMonitorDialog = new MarketplaceProgressMonitorDialog(UIUtils.getActiveShell(),
										installItem.getName());
									progressMonitorDialog.run(true, false, new IRunnableWithProgress()
									{

										public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
										{
											try
											{
												installItem.install(monitor);
												if (installItem.isRestartRequired())
												{
													Display.getDefault().syncExec(new Runnable()
													{
														public void run()
														{
															if (MessageDialog.openQuestion(UIUtils.getActiveShell(), "Servoy Marketplace",
																"Servoy Developer must be restarted to complete the installation.\n\nDo you want to restart now ?"))
															{
																PlatformUI.getWorkbench().restart();
															}
														}
													});
												}
											}
											catch (final Exception ex)
											{
												Display.getDefault().syncExec(new Runnable()
												{
													public void run()
													{
														MessageDialog.openError(UIUtils.getActiveShell(), "Servoy Marketplace", "Error installing " +
															installItem.getName() + ".\n\n" + ex.getMessage());
													}
												});
											}
										}
									});
								}
								catch (Exception ex1)
								{
									ServoyLog.logError(ex1);
								}
							}
						}
					}
					catch (Exception ex)
					{
						ServoyLog.logError(ex);
						MessageDialog.openError(UIUtils.getActiveShell(), "Servoy Marketplace", "Cannot get install informations.\n\n" + ex.getMessage());
					}
					event.doit = false;
				}
			}
		});
	}

	/*
	 * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
	 */
	@Override
	public void setFocus()
	{
		if (browser != null) browser.setFocus();
	}

	class MarketplaceProgressMonitorDialog extends ProgressMonitorDialog
	{
		private final String installName;

		public MarketplaceProgressMonitorDialog(Shell parent, String installName)
		{
			super(parent);
			this.installName = installName;
		}

		@Override
		protected void configureShell(final Shell shell)
		{
			super.configureShell(shell);
			shell.setText("Servoy Marketplace - Installing " + installName + " ...");
		}
	}

	public void deepLink(String deeplinkParam)
	{
		browser.setUrl(url.concat(deeplinkParam).toString());
	}

	public void executeDeepLink(String deeplinkParam)
	{
		browser.execute("executeMPDeepLink(\"" + deeplinkParam + "\");"); //$NON-NLS-1$ //$NON-NLS-2$
	}
}
