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
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.server.shared.ApplicationServerSingleton;

/**
 * 
 * @author jcompagner
 * @since 6.1
 */
public class ServersSelectionPage extends WizardPage implements ICheckStateListener
{
	private final SortedSet<String> selectedServers;
	private CheckboxTableViewer checkboxTableViewer;
	private final HashMap<String, IWizardPage> serverConfigurationPages;
	private final String[] requiredServers;

	/**
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
		this.serverConfigurationPages = serverConfigurationPages;
		this.requiredServers = requiredServers;
		setTitle(title);
		setDescription(description);
	}

	public void createControl(Composite parent)
	{
		Composite container = new Composite(parent, SWT.NULL);
		setControl(container);
		container.setLayout(new FillLayout(SWT.HORIZONTAL));

		checkboxTableViewer = CheckboxTableViewer.newCheckList(container, SWT.BORDER | SWT.FULL_SELECTION);

		checkboxTableViewer.setContentProvider(new ServersContentProvider());
		checkboxTableViewer.setInput(ApplicationServerSingleton.get().getServerManager());
		if (requiredServers != null && requiredServers.length > 0) checkboxTableViewer.addCheckStateListener(this);
		if (selectedServers.size() == 0)
		{
			checkboxTableViewer.setAllChecked(true);
		}
		else
		{
			checkboxTableViewer.setCheckedElements(appendRequiredLabel(selectedServers.toArray()));
		}

	}

	private String[] appendRequiredLabel(Object[] serverNames)
	{
		ArrayList<String> appendRequiredServerNames = new ArrayList<String>();
		List<String> requiredServersAsList = requiredServers != null && requiredServers.length > 0 ? Arrays.asList(requiredServers) : null;
		String sServerName;
		for (Object serverName : serverNames)
		{
			sServerName = serverName.toString();
			if (requiredServersAsList != null && requiredServersAsList.indexOf(sServerName) != -1) appendRequiredServerNames.add(0, sServerName +
				DirectorySelectionPage.REQUIRED_LABEL);
			else appendRequiredServerNames.add(sServerName);
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
		for (String requiredServer : requiredServers)
			if (!checkable.getChecked(requiredServer + DirectorySelectionPage.REQUIRED_LABEL)) checkable.setChecked(requiredServer +
				DirectorySelectionPage.REQUIRED_LABEL, true);

	}
}