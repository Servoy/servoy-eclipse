/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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

package com.servoy.eclipse.warexporter.ui.wizard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.SortedSet;

import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ICheckable;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import com.servoy.eclipse.ui.wizards.ICheckBoxView;
import com.servoy.eclipse.ui.wizards.SelectAllButtonsBar;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;

/**
 *
 * @author jcompagner
 * @since 6.1
 */
public class ServersSelectionPage extends WizardPage implements ICheckStateListener, ICheckBoxView
{
	private final SortedSet<String> selectedServers;
	private CheckboxTableViewer checkboxTableViewer;
	private final String[] requiredServers;
	private SelectAllButtonsBar selectAllButtons;
	private final HashMap<String, IWizardPage> serverConfigurationPages;

	/**
	 * @param serverConfigurationPages
	 * @param string
	 * @param string2
	 * @param string3
	 * @param pluginDir
	 * @param beanSelectionPage
	 */
	public ServersSelectionPage(String pagename, String title, String description, SortedSet<String> selectedServers, String[] requiredServers,
		HashMap<String, IWizardPage> serverConfigurationPages)
	{
		super(pagename);
		this.selectedServers = selectedServers;
		this.requiredServers = requiredServers;
		this.serverConfigurationPages = serverConfigurationPages;
		setTitle(title);
		setDescription(description);
	}

	public void createControl(Composite parent)
	{
		GridLayout gridLayout = new GridLayout();
		Composite container = new Composite(parent, SWT.NONE);
		setControl(container);
		container.setLayout(gridLayout);

		checkboxTableViewer = CheckboxTableViewer.newCheckList(container, SWT.BORDER | SWT.FULL_SELECTION);
		checkboxTableViewer.setContentProvider(new ServersContentProvider());
		checkboxTableViewer.setInput(ApplicationServerRegistry.get().getServerManager());
		gridLayout.numColumns = 2;
		GridData gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.verticalAlignment = GridData.FILL;
		gridData.grabExcessVerticalSpace = true;
		gridData.grabExcessHorizontalSpace = true;
		gridData.horizontalSpan = 2;
		checkboxTableViewer.getTable().setLayoutData(gridData);
		checkboxTableViewer.addCheckStateListener(this);
		selectAllButtons = new SelectAllButtonsBar(this, container, true);
		if (selectedServers.size() == 0)
		{
			checkboxTableViewer.setAllChecked(true);
		}
		else
		{
			checkboxTableViewer.setCheckedElements(appendRequiredLabel(selectedServers.toArray()));
		}
		if (checkboxTableViewer.getTable().getItemCount() == 0)
		{
			selectAllButtons.disableButtons();
		}
		else
		{
			selectAllButtons.enableAll();
		}
		selectAllButtons.addRestoreSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				checkboxTableViewer.setAllChecked(true);
				selectAllButtons.enableAll();
				selectAllButtons.disableSelectAll();
			}
		});
	}

	private String[] appendRequiredLabel(Object[] serverNames)
	{
		ArrayList<String> appendRequiredServerNames = new ArrayList<String>();
		List<String> requiredServersAsList = requiredServers != null && requiredServers.length > 0 ? Arrays.asList(requiredServers) : null;
		if (requiredServersAsList != null)
		{
			for (String requiredServerName : requiredServersAsList)
			{
				appendRequiredServerNames.add(requiredServerName + DirectorySelectionPage.REQUIRED_LABEL);
			}
		}
		String sServerName;
		for (Object serverName : serverNames)
		{
			sServerName = serverName.toString();
			if (requiredServersAsList == null || requiredServersAsList.indexOf(sServerName) == -1) appendRequiredServerNames.add(sServerName);
		}
		return appendRequiredServerNames.toArray(new String[appendRequiredServerNames.size()]);

	}

	private String[] removeRequiredLabel(Object[] serverNames)
	{
		ArrayList<String> removeRequiedServerNames = new ArrayList<String>();
		List<String> requiredServersAsList = requiredServers != null && requiredServers.length > 0 ? Arrays.asList(requiredServers) : null;
		String sServerName, requiredServerName;
		for (Object serverName : serverNames)
		{
			sServerName = serverName.toString();
			if (requiredServersAsList != null && sServerName.endsWith(DirectorySelectionPage.REQUIRED_LABEL))
			{
				requiredServerName = sServerName.substring(0, sServerName.length() - DirectorySelectionPage.REQUIRED_LABEL.length());
				if (requiredServersAsList.indexOf(requiredServerName) != -1) sServerName = requiredServerName;
			}
			removeRequiedServerNames.add(sServerName);
		}
		return removeRequiedServerNames.toArray(new String[removeRequiedServerNames.size()]);
	}

	public void storeInput()
	{
		selectedServers.clear();
		String[] checkedElements = removeRequiredLabel(checkboxTableViewer.getCheckedElements());
		for (Object object : checkedElements)
		{
			selectedServers.add(object.toString());
		}
		if (!selectedServers.contains(IServer.REPOSITORY_SERVER)) selectedServers.add(IServer.REPOSITORY_SERVER);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jface.wizard.WizardPage#getNextPage()
	 */
	@Override
	public IWizardPage getNextPage()
	{
		storeInput();
		if (selectedServers.size() == 0) return null;
		return serverConfigurationPages.get(selectedServers.first());
	}

	private class ServersContentProvider implements IStructuredContentProvider
	{
		public void dispose()
		{
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
		{
		}

		public Object[] getElements(Object inputElement)
		{
			if (inputElement instanceof IServerManagerInternal)
			{
				IServerManagerInternal serverManager = (IServerManagerInternal)inputElement;
				return appendRequiredLabel(serverManager.getServerNames(true, true, true, false));
			}
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.core.commands.IStateListener#handleStateChange(org.eclipse.core.commands.State, java.lang.Object)
	 */
	@Override
	public void checkStateChanged(CheckStateChangedEvent event)
	{
		ICheckable checkable = event.getCheckable();
		if (requiredServers != null)
		{
			for (String requiredServer : requiredServers)
				if (!checkable.getChecked(requiredServer + DirectorySelectionPage.REQUIRED_LABEL))
				{
					checkable.setChecked(requiredServer + DirectorySelectionPage.REQUIRED_LABEL, true);
				}
		}
		if (checkboxTableViewer.getCheckedElements().length < checkboxTableViewer.getTable().getItemCount())
		{
			selectAllButtons.enableAll();
		}
		else
		{
			selectAllButtons.disableSelectAll();
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.eclipse.ui.wizards.ICheckBoxView#selectAll()
	 */
	@Override
	public void selectAll()
	{
		checkboxTableViewer.setAllChecked(true);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.eclipse.ui.wizards.ICheckBoxView#deselectAll()
	 */
	@Override
	public void deselectAll()
	{
		checkboxTableViewer.setAllChecked(false);
		if (requiredServers != null)
		{
			for (String requiredServer : requiredServers)
				if (requiredServer != null && requiredServers.length > 0)
					checkboxTableViewer.setChecked(requiredServer + DirectorySelectionPage.REQUIRED_LABEL, true);
		}
	}
}