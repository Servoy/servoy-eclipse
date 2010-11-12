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

package com.servoy.eclipse.ui.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.browser.BrowserViewer;
import org.eclipse.ui.internal.browser.BusyIndicator;
import org.eclipse.ui.internal.browser.WebBrowserView;

import com.servoy.eclipse.ui.Activator;

/**
 * Shows a page from the Servoy web site. The intention is to provide the Servoy developer with latest info related to Servoy.
 * 
 * @author gerzse
 */
public class ServoyWebBrowserView extends WebBrowserView
{
	public static final String ID = "com.servoy.eclipse.ui.browser.view"; //$NON-NLS-1$
	private static final String SERVOY_URL = "http://www.servoy.com/i"; //$NON-NLS-1$

	@Override
	public void init(IViewSite site) throws PartInitException
	{
		super.init(site);
		getSite().getWorkbenchWindow().getPartService().addPartListener(new IPartListener()
		{
			public void partActivated(IWorkbenchPart part)
			{
			}

			public void partBroughtToTop(IWorkbenchPart part)
			{
			}

			public void partClosed(IWorkbenchPart part)
			{
			}

			public void partDeactivated(IWorkbenchPart part)
			{
			}

			public void partOpened(IWorkbenchPart part)
			{
				if (ServoyWebBrowserView.this == part)
				{
					setURL(getViewURL());
				}
			}
		});
	}

	// Not a nice solution, but we need to get rid of the navigation bar
	// and this is a handy way to clear the style. Another way would be 
	// through secondary-id, but then the view needs to allow multiple instances.
	@Override
	public void createPartControl(Composite parent)
	{
		viewer = new ServoyBrowserViewer(parent, getViewID());
		viewer.setContainer(this);
		initDragAndDrop();
	}

	public static class ServoyBrowserViewer extends BrowserViewer
	{
		public ServoyBrowserViewer(Composite parent, final String parentBrowserViewId)
		{
			super(parent, BrowserViewer.BUTTON_BAR);
			// Locate the toolbar and the busy indicator. The toolbar gets extended 
			// and the indicator is dropped.
			if (this.getChildren().length > 0)
			{
				Control firstChild = this.getChildren()[0]; // this one holds the toolbar and the indicator
				if (firstChild instanceof Composite)
				{
					Composite holder = (Composite)firstChild;
					if (holder.getChildren().length == 2)
					{
						Control toolbarRaw = holder.getChildren()[0];
						Control indicatorRaw = holder.getChildren()[1];
						if (toolbarRaw instanceof ToolBar)
						{
							ToolBar toolbar = (ToolBar)toolbarRaw;

							final ToolItem maximizeRestore = new ToolItem(toolbar, SWT.NONE);
							final Image maximize = Activator.loadImageDescriptorFromBundle("maximize.gif").createImage(); //$NON-NLS-1$
							final Image restore = Activator.loadImageDescriptorFromBundle("restore.gif").createImage(); //$NON-NLS-1$
							maximizeRestore.setImage(maximize);
							maximizeRestore.setToolTipText("Maximize");
							maximizeRestore.addSelectionListener(new SelectionAdapter()
							{
								@Override
								public void widgetSelected(SelectionEvent event)
								{
									IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
									if (page != null)
									{
										for (IViewReference view : page.getViewReferences())
										{
											if (view.getId().equals(parentBrowserViewId))
											{
												int currentState = page.getPartState(view);
												int newState;
												String newTooltip;
												Image newImage;
												if (currentState == IWorkbenchPage.STATE_MAXIMIZED)
												{
													newState = IWorkbenchPage.STATE_RESTORED;
													newTooltip = "Maximize"; //$NON-NLS-1$
													newImage = maximize;
												}
												else
												{
													newState = IWorkbenchPage.STATE_MAXIMIZED;
													newTooltip = "Restore"; //$NON-NLS-1$
													newImage = restore;
												}
												page.setPartState(view, newState);
												maximizeRestore.setImage(newImage);
												maximizeRestore.setToolTipText(newTooltip);
											}
										}
									}
								}
							});

						}
						if (indicatorRaw instanceof BusyIndicator)
						{
							indicatorRaw.setVisible(false);
						}
					}
				}
				this.layout();
			}
		}
	}

	protected String getViewID()
	{
		return ID;
	}

	protected String getViewURL()
	{
		return SERVOY_URL;
	}
}
