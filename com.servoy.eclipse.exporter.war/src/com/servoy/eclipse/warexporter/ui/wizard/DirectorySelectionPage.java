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

import java.io.File;
import java.io.FilenameFilter;
import java.util.List;

import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

/**
 * 
 * @author jcompagner
 * @since 6.1
 */
public class DirectorySelectionPage extends WizardPage
{
	private final File directory;
	private final IWizardPage nextPage;
	private final List<String> files;
	private CheckboxTableViewer checkboxTableViewer;

	public DirectorySelectionPage(String pagename, String title, String description, File directory, List<String> files)
	{
		this(pagename, title, description, directory, files, null);
	}

	/**
	 * @param string
	 * @param string2
	 * @param string3
	 * @param pluginDir
	 * @param beanSelectionPage
	 */
	public DirectorySelectionPage(String pagename, String title, String description, File directory, List<String> files, IWizardPage nextPage)
	{
		super(pagename);
		this.directory = directory;
		this.files = files;
		this.nextPage = nextPage;
		setTitle(title);
		setDescription(description);
	}

	public void createControl(Composite parent)
	{
		Composite container = new Composite(parent, SWT.NULL);
		setControl(container);
		container.setLayout(new FillLayout(SWT.HORIZONTAL));

		checkboxTableViewer = CheckboxTableViewer.newCheckList(container, SWT.BORDER | SWT.FULL_SELECTION);

		checkboxTableViewer.setContentProvider(new DirectoryContentProvider());
		checkboxTableViewer.setInput(directory);
		if (files.size() == 0)
		{
			checkboxTableViewer.setAllChecked(true);
		}
		else
		{
			checkboxTableViewer.setCheckedElements(files.toArray());
		}

	}


	public void storeInput()
	{
		files.clear();
		Object[] checkedElements = checkboxTableViewer.getCheckedElements();
		for (Object object : checkedElements)
		{
			files.add(object.toString());
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
		if (nextPage != null) return nextPage;
		return super.getNextPage();
	}

	private class DirectoryContentProvider implements IStructuredContentProvider
	{
		public void dispose()
		{
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
		{
		}

		public Object[] getElements(Object inputElement)
		{
			if (inputElement instanceof File)
			{
				return ((File)inputElement).list(new FilenameFilter()
				{

					public boolean accept(File dir, String name)
					{
						return name.toLowerCase().endsWith(".jar") || name.toLowerCase().endsWith(".zip"); //$NON-NLS-1$
					}
				});
			}
			return null;
		}

	}
}