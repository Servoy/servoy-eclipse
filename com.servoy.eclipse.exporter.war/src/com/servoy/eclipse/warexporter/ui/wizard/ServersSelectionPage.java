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

import java.util.HashMap;
import java.util.SortedSet;

import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.server.shared.ApplicationServerSingleton;

/**
 * 
 * @author jcompagner
 * @since 6.1
 */
public class ServersSelectionPage extends WizardPage
{
	private final SortedSet<String> selectedServers;
	private CheckboxTableViewer checkboxTableViewer;
	private final HashMap<String, IWizardPage> serverConfigurationPages;

	/**
	 * @param string
	 * @param string2
	 * @param string3
	 * @param pluginDir
	 * @param beanSelectionPage
	 */
	public ServersSelectionPage(String pagename, String title, String description, SortedSet<String> selectedServers,
		HashMap<String, IWizardPage> serverConfigurationPages)
	{
		super(pagename);
		this.selectedServers = selectedServers;
		this.serverConfigurationPages = serverConfigurationPages;
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
		if (selectedServers.size() == 0)
		{
			checkboxTableViewer.setAllChecked(true);
		}
		else
		{
			checkboxTableViewer.setCheckedElements(selectedServers.toArray());
		}

	}


	public void storeInput()
	{
		selectedServers.clear();
		Object[] checkedElements = checkboxTableViewer.getCheckedElements();
		for (Object object : checkedElements)
		{
			selectedServers.add(object.toString());
		}
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
				return serverManager.getServerNames(true, true, true, false);
			}
			return null;
		}
	}
}