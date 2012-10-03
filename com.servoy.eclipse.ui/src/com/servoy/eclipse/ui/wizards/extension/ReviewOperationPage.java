/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.eclipse.ui.wizards.extension;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.model.util.ServoyLog;

/**
 * This page is the base UI setup for showing information on the extension that is about to be installed/uninstalled.
 * @author acostescu
 */
public abstract class ReviewOperationPage extends WizardPage
{

	protected InstallExtensionState state;

	private Label descriptionText;
	private Link productUrlLink;
	private Label productUrl;
	private Composite textInfo;

	private Label imgLbl;

	private Composite topLevel;

	/**
	 * Creates a new review operation page.
	 * @param pageName see super.
	 * @param state the state of the (un)install extension process. It contains the information needed by this page.
	 */
	public ReviewOperationPage(String pageName, String pageTitle, InstallExtensionState state)
	{
		super(pageName);
		this.state = state;

		setTitle(pageTitle);
		setDescription(""); //$NON-NLS-1$
	}

	public void createControl(Composite parent)
	{
		initializeDialogUnits(parent);

		topLevel = new Composite(parent, SWT.NONE);
		topLevel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));
		setControl(topLevel);

		Composite infoComposite = new Composite(topLevel, SWT.NONE);
		GridLayout gridLayout = new GridLayout(2, false);
		gridLayout.horizontalSpacing = 15;
		gridLayout.marginWidth = gridLayout.marginHeight = 0;
		infoComposite.setLayout(gridLayout);

		imgLbl = new Label(infoComposite, SWT.NONE);
		imgLbl.setImage(null); // will be set by a runnable that provides progress
		imgLbl.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, true));

		textInfo = new Composite(infoComposite, SWT.NONE);
		gridLayout = new GridLayout(2, false);
		gridLayout.horizontalSpacing = 10;
		gridLayout.verticalSpacing = 5;
		gridLayout.marginHeight = gridLayout.marginWidth = 0;
		textInfo.setLayout(gridLayout);
		textInfo.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true));

//		Label extensionName = new Label(textInfo, SWT.NONE);
//		extensionName.setText("Name:"); //$NON-NLS-1$
//		Text extensionNameText = new Text(textInfo, SWT.READ_ONLY | SWT.BORDER);

//		extensionName.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
//		extensionNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		Label extensionID = new Label(textInfo, SWT.NONE);
		extensionID.setText("Id"); //$NON-NLS-1$
		Text extensionIDText = new Text(textInfo, SWT.READ_ONLY | SWT.BORDER);
		extensionIDText.setText(state.extensionID);
//		extensionIDText.setBackground(state.display.getSystemColor(SWT.COLOR_LIST_BACKGROUND));

		extensionID.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		extensionIDText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		Label version = new Label(textInfo, SWT.NONE);
		version.setText("Version"); //$NON-NLS-1$
		Text versionText = new Text(textInfo, SWT.READ_ONLY | SWT.BORDER);
		versionText.setText(state.version);
//		versionText.setBackground(state.display.getSystemColor(SWT.COLOR_LIST_BACKGROUND));

		version.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		versionText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		productUrl = new Label(textInfo, SWT.NONE);
		productUrl.setText("Product URL"); //$NON-NLS-1$
		productUrlLink = new Link(textInfo, SWT.NONE);
		productUrlLink.addListener(SWT.Selection, new Listener()
		{
			public void handleEvent(Event event)
			{
				try
				{
					PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(event.text));
				}
				catch (PartInitException e)
				{
					ServoyLog.logError(e);
				}
				catch (MalformedURLException e)
				{
					ServoyLog.logError(e);
				}
			}
		});

		productUrl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		productUrl.setVisible(false);
		productUrlLink.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		productUrlLink.setVisible(false);

		ScrolledComposite descriptionComposite = new ScrolledComposite(topLevel, SWT.V_SCROLL);
		descriptionComposite.setAlwaysShowScrollBars(false);
		descriptionComposite.setExpandHorizontal(true);
		descriptionComposite.setMinWidth(10);
		descriptionText = new Label(descriptionComposite, SWT.WRAP);
		descriptionText.setText(""); //$NON-NLS-1$
		descriptionComposite.setContent(descriptionText);

		// layout the page
		gridLayout = new GridLayout(1, false);
		gridLayout.horizontalSpacing = gridLayout.verticalSpacing = 0;
		gridLayout.marginWidth = 10;
		gridLayout.marginHeight = 10;
		topLevel.setLayout(gridLayout);

		infoComposite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.verticalIndent = 15;
		descriptionComposite.setLayoutData(gd);

		addMoreContent(topLevel);
		fillData();
	}

	/**
	 * Called from create control to add any more needed content to the bottom of existing content.
	 * @param parent the parent container; it uses a 1 column grid layout.
	 */
	protected abstract void addMoreContent(Composite parent);

	/**
	 * Called to populate the information related to the extension. The extension's name should be put in the page's description,
	 * for the others there are custom setter methods.
	 */
	protected abstract void fillData();

	protected void setExtensionDescription(final String text, boolean afterInitialLayout)
	{
		if (afterInitialLayout)
		{
			descriptionText.setText(text.replace("\r\n", "\n").replace("\n", System.getProperty("line.separator"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			descriptionText.setSize(descriptionText.computeSize(descriptionText.getParent().getSize().x, SWT.DEFAULT));
		}
		else
		{
			Display.getCurrent().asyncExec(new Runnable()
			{
				public void run()
				{
					descriptionText.setText(text.replace("\r\n", "\n").replace("\n", System.getProperty("line.separator"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
					descriptionText.setSize(descriptionText.computeSize(descriptionText.getParent().getSize().x, SWT.DEFAULT));
				}
			});
		}
	}

	protected void setExtensionProductUrl(String url, boolean afterFirstLayout)
	{
		productUrl.setVisible(true);
		productUrlLink.setText(url);
		productUrlLink.setVisible(true);
		if (afterFirstLayout)
		{
			textInfo.layout(true, true);
		}
	}

	protected void setExtensionIcon(Image img, boolean afterFirstLayout)
	{
		imgLbl.setImage(img);
		if (afterFirstLayout)
		{
			topLevel.layout(true, true);
		}
	}

	@Override
	public boolean canFlipToNextPage()
	{
		return true;
	}

}
