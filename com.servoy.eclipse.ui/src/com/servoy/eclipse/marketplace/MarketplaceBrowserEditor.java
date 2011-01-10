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
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;

import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.util.ServoyLog;

/**
 * Editor used to show the Servoy Marketplace. 
 *  
 * @author gboros
 */
public class MarketplaceBrowserEditor extends EditorPart
{
	public static final String MARKETPLACE_BROWSER_EDITOR_ID = "com.servoy.eclipse.marketplace.MarketplaceBrowserEditor"; //$NON-NLS-1$
	public static final String MARKETPLACE_URL = "http://www.servoy.com/marketplace"; //$NON-NLS-1$
	//public static final String MARKETPLACE_URL = "http://localhost:8080/servoy-webclient/solutions/solution/downloadLink"; //$NON-NLS-1$
	public static final MarketplaceBrowserEditorInput INPUT = new MarketplaceBrowserEditorInput();

	private Browser browser;

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
		browser.setUrl(MARKETPLACE_URL, null, new String[] { "Cache-Control: no-cache" }); //$NON-NLS-1$
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
									PlatformUI.getWorkbench().getProgressService().run(true, false, new IRunnableWithProgress()
									{

										public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
										{
											try
											{
												installItem.install(monitor);
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
}
