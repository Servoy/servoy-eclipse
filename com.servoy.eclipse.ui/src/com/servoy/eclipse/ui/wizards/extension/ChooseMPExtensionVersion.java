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

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.extension.DependencyMetadata;
import com.servoy.extension.ExtensionDependencyDeclaration;
import com.servoy.extension.MarketPlaceExtensionProvider;
import com.servoy.extension.Message;
import com.servoy.extension.ServoyDependencyDeclaration;
import com.servoy.extension.VersionStringUtils;

/**
 * A page that is presented when installing from the marketplace, and there are multiple versions to choose from. 
 * @author acostescu
 */
public class ChooseMPExtensionVersion extends WizardPage
{

	protected InstallExtensionState state;
	protected String[] versions;
	protected MarketPlaceExtensionProvider marketplaceProvider;
	protected InstallExtensionWizardOptions dialogOptions;
	protected boolean canGoToNext;

	/**
	 * Creates a new page for choosing which version of the given extension id should be imported.
	 * @param pageName see super.
	 * @param marketplaceProvider the MP provider.
	 * @param state the state of the install extension process. Will be filled with info retrieved from this page in order for the wizard to go forward.
	 * @param versions the available version of the extension listed in state. This array will get sorted.
	 * @param dialogOptions initial check-box states are held in this object; they will be updated.
	 */
	public ChooseMPExtensionVersion(String pageName, InstallExtensionState state, InstallExtensionWizardOptions dialogOptions,
		MarketPlaceExtensionProvider marketplaceProvider, String[] versions)
	{
		super(pageName);
		this.state = state;
		this.dialogOptions = dialogOptions;
		sortVersions(versions);
		this.versions = versions;
		this.marketplaceProvider = marketplaceProvider;

		setTitle("Install extension from Marketplace"); //$NON-NLS-1$
		setDescription("You can choose one of the available versions (or use suggested version)."); //$NON-NLS-1$
		canGoToNext = false;
	}

	public void createControl(Composite parent)
	{
		initializeDialogUnits(parent);

		Composite topLevel = new Composite(parent, SWT.NONE);
		topLevel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));
		setControl(topLevel);

		final Label extensionID = new Label(topLevel, SWT.NONE);
		extensionID.setText("Extension ID: '" + state.extensionID + "'"); //$NON-NLS-1$ //$NON-NLS-2$

		final Label extensionName = new Label(topLevel, SWT.NONE);
		extensionName.setVisible(false);

		final Label versionLabel = new Label(topLevel, SWT.NONE);
		versionLabel.setText("Version"); //$NON-NLS-1$
		versionLabel.setVisible(false);

		final Combo versionCombo = new Combo(topLevel, SWT.READ_ONLY);
		versionCombo.setItems(versions);
		versionCombo.addModifyListener(new ModifyListener()
		{
			public void modifyText(ModifyEvent e)
			{
				state.version = versionCombo.getText();
				DependencyMetadata[] dmds = marketplaceProvider.getDependencyMetadata(new ExtensionDependencyDeclaration(state.extensionID, state.version,
					state.version));
				if (dmds != null && dmds.length == 1)
				{
					extensionName.setText(dmds[0].extensionName);
				}
				marketplaceProvider.clearMessages();
			}
		});
		versionCombo.setVisible(false);

		// layout the page
		FormLayout formLayout = new FormLayout();
		formLayout.spacing = 5;
		formLayout.marginWidth = 10;
		formLayout.marginHeight = 10;
		topLevel.setLayout(formLayout);

		FormData formData = new FormData();
		formData.left = new FormAttachment(0, 0);
		formData.top = new FormAttachment(0, 0);
		formData.right = new FormAttachment(100, 0);
		extensionID.setLayoutData(formData);

		formData = new FormData();
		formData.left = new FormAttachment(0, 0);
		formData.top = new FormAttachment(extensionID, 0);
		formData.right = new FormAttachment(100, 0);
		extensionName.setLayoutData(formData);

		formData = new FormData();
		formData.left = new FormAttachment(0, 0);
		formData.top = new FormAttachment(extensionName, 20);
		formData.right = new FormAttachment(versionCombo, 0);
		versionLabel.setLayoutData(formData);

		formData = new FormData();
		formData.left = new FormAttachment(versionLabel, 0);
		formData.top = new FormAttachment(versionLabel, 0, SWT.CENTER);
//		formData.right = new FormAttachment(100, 0);
		versionCombo.setLayoutData(formData);

		final IRunnableWithProgress toRun = new IRunnableWithProgress()
		{
			public void run(IProgressMonitor monitor)
			{
				monitor.beginTask("Getting version info", IProgressMonitor.UNKNOWN); //$NON-NLS-1$
				final int initialSelectedVersion = getInitialSelectedVersion(versions);
				monitor.done();
				// switch back to display thread to update UI
				state.display.asyncExec(new Runnable()
				{
					public void run()
					{
						versionCombo.select(initialSelectedVersion);

						// ok, we are good to go now
						extensionID.setText("Extension ID: '" + state.extensionID + "'"); //$NON-NLS-1$ //$NON-NLS-2$
						extensionName.setVisible(true);
						versionLabel.setVisible(true);
						versionCombo.setVisible(true);

						canGoToNext = true;
					}
				});
			}
		};

		// run later because getWizard().getContainer().run does not guarantee it will not block, even if it's called with fork = true
		state.display.asyncExec(new Runnable()
		{
			public void run()
			{
				try
				{
					getWizard().getContainer().run(true, false, toRun);
					getContainer().updateButtons();
				}
				catch (InvocationTargetException e)
				{
					ServoyLog.logError(e);
					setPageComplete(state.version != null);
				}
				catch (InterruptedException e)
				{
					ServoyLog.logError(e);
					setPageComplete(state.version != null);
				}
			}
		});
	}

	/**
	 * Sorts a version array.
	 */
	protected void sortVersions(String[] versions_)
	{
		Arrays.sort(versions_, new Comparator<String>()
		{

			public int compare(String ver1, String ver2)
			{
				return VersionStringUtils.compareVersions(ver1, ver2);
			}

		});
	}

	/**
	 * Searches for the latest version of the extension that is compatible with the current Servoy version.
	 * @param versions a sorted array of versions.
	 * @return it's index in the version array.
	 */
	private int getInitialSelectedVersion(String[] versions_)
	{
		int chosen = versions_.length - 1;
		for (int i = versions_.length - 1; i >= 0; i--)
		{
			DependencyMetadata[] dmds = marketplaceProvider.getDependencyMetadata(new ExtensionDependencyDeclaration(state.extensionID, versions_[i],
				versions_[i]));
			if (dmds != null && dmds.length == 1)
			{
				ServoyDependencyDeclaration servoyDependency = dmds[0].getServoyDependency();
				if (servoyDependency == null ||
					VersionStringUtils.belongsToInterval(VersionStringUtils.getCurrentServoyVersion(), servoyDependency.minVersion, servoyDependency.maxVersion))
				{
					chosen = i;
					break;
				}
			}
			else
			{
				// else something went wrong... connection failure? anyway just continue cause we do have a version selected anyway
				Message[] w = marketplaceProvider.getMessages();
				ServoyLog.logWarning("Problems when trying to pre-select best version: " + (w.length > 0 ? w[w.length - 1].message : "<unknown>"), null); //$NON-NLS-1$ //$NON-NLS-2$
			}
			marketplaceProvider.clearMessages();
		}

		return chosen;
	}

	@Override
	public boolean canFlipToNextPage()
	{
		return canGoToNext;
	}

	@Override
	public IWizardPage getNextPage()
	{
		// second page is always the dependency resolving page; when we reach it, first pages should have already set the extensionID/version pair in 'state'
		DependencyResolvingPage next = new DependencyResolvingPage("DepResolver", state, dialogOptions, false); //$NON-NLS-1$
		next.setWizard(getWizard());
		return next;
	}

}