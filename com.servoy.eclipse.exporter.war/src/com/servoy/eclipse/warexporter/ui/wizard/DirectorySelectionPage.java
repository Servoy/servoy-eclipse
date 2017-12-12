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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ICheckable;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import com.servoy.eclipse.ui.wizards.ICheckBoxView;
import com.servoy.eclipse.ui.wizards.SelectAllButtonsBar;
import com.servoy.j2db.util.Utils;

/**
 *
 * @author jcompagner
 * @since 6.1
 */
public class DirectorySelectionPage extends WizardPage implements ICheckStateListener, ICheckBoxView, IRestoreDefaultPage
{
	public static final String REQUIRED_LABEL = " (required)";

	private final File directory;
	private final List<String> files;
	private final String[] requiredFiles;
	private CheckboxTableViewer checkboxTableViewer;
	private SelectAllButtonsBar selectAllButtons;
	private final boolean selectAll;
	private final boolean includeSubdirectories;

	public DirectorySelectionPage(String pagename, String title, String description, File directory, List<String> files, String[] requiredFiles,
		boolean selectAll)
	{
		this(pagename, title, description, directory, files, requiredFiles, selectAll, false);
	}

	/**
	 * @param string
	 * @param string2
	 * @param string3
	 * @param pluginDir
	 * @param beanSelectionPage
	 */
	public DirectorySelectionPage(String pagename, String title, String description, File directory, List<String> files, String[] requiredFiles,
		boolean selectAll, boolean includeSubdirectories)
	{
		super(pagename);
		this.directory = directory;
		this.files = files;
		this.requiredFiles = requiredFiles;
		this.selectAll = selectAll;
		this.includeSubdirectories = includeSubdirectories;
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
		checkboxTableViewer.setContentProvider(new DirectoryContentProvider());
		checkboxTableViewer.setInput(directory);
		gridLayout.numColumns = 2;
		GridData gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.verticalAlignment = GridData.FILL;
		gridData.grabExcessVerticalSpace = true;
		gridData.grabExcessHorizontalSpace = true;
		gridData.horizontalSpan = 2;
		checkboxTableViewer.getTable().setLayoutData(gridData);
		checkboxTableViewer.addCheckStateListener(this);
		selectAllButtons = new SelectAllButtonsBar(this, container);
		if (selectAll)
		{
			checkboxTableViewer.setAllChecked(true);
		}
		else
		{
			checkboxTableViewer.setCheckedElements(appendRequiredLabelAndFolderBrackets(files.toArray()));
		}
		if (checkboxTableViewer.getTable().getItemCount() == 0)
		{
			selectAllButtons.disableButtons();
		}
		else
		{
			selectAllButtons.enableAll();
		}
	}


	public void storeInput()
	{
		files.clear();
		String[] checkedElements = removeRequiredLabelAndFolderBrackets(checkboxTableViewer.getCheckedElements());
		for (String el : checkedElements)
			files.add(el);
	}

	@Override
	public void dispose()
	{
		super.dispose();
		if (checkboxTableViewer != null) checkboxTableViewer.removeCheckStateListener(this);
	}

	private String[] appendRequiredLabelAndFolderBrackets(Object[] fileNames)
	{
		ArrayList<String> appendRequiredFileNames = new ArrayList<String>();
		List<String> requiredFilesAsList = requiredFiles != null && requiredFiles.length > 0 ? Arrays.asList(requiredFiles) : null;
		String sFileName;
		for (Object fileName : fileNames)
		{
			sFileName = fileName.toString();
			if (isAcceptedFileType(sFileName))
			{
				if (requiredFilesAsList != null && requiredFilesAsList.indexOf(sFileName) != -1) appendRequiredFileNames.add(0, sFileName + REQUIRED_LABEL);
				else appendRequiredFileNames.add(sFileName);
			}
			else
			{
				appendRequiredFileNames.add('[' + sFileName + ']');
			}
		}
		return appendRequiredFileNames.toArray(new String[appendRequiredFileNames.size()]);

	}

	private String[] removeRequiredLabelAndFolderBrackets(Object[] fileNames)
	{
		ArrayList<String> removeRequiedFileNames = new ArrayList<String>();
		List<String> requiredFilesAsList = requiredFiles != null && requiredFiles.length > 0 ? Arrays.asList(requiredFiles) : null;
		String sFileName, requiredFileName;
		for (Object fileName : fileNames)
		{
			sFileName = fileName.toString();
			if (requiredFilesAsList != null && sFileName.endsWith(REQUIRED_LABEL))
			{
				requiredFileName = sFileName.substring(0, sFileName.length() - REQUIRED_LABEL.length());
				if (requiredFilesAsList.indexOf(requiredFileName) != -1) sFileName = requiredFileName;
			}
			else if (sFileName.startsWith("[") && sFileName.endsWith("]"))
			{
				sFileName = sFileName.substring(1, sFileName.length() - 1);
			}
			removeRequiedFileNames.add(sFileName);
		}
		return removeRequiedFileNames.toArray(new String[removeRequiedFileNames.size()]);
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
				final String[] fileNames = ((File)inputElement).list(new FilenameFilter()
				{
					public boolean accept(File dir, String name)
					{
						return isAcceptedFileType(name);
					}
				});

				if (includeSubdirectories)
				{
					String[] directoryNames = ((File)inputElement).list(new FilenameFilter()
					{
						public boolean accept(File dir, String name)
						{
							for (String fileName : fileNames)
							{
								String fileNameNoExtension = fileName.substring(0, fileName.indexOf('.'));
								if (fileNameNoExtension.equalsIgnoreCase(name)) return false;
							}
							return new File(dir, name).isDirectory();
						}
					});
					return appendRequiredLabelAndFolderBrackets(Utils.arrayJoin(fileNames, directoryNames));
				}
				else
				{
					return appendRequiredLabelAndFolderBrackets(fileNames);
				}
			}
			return null;
		}
	}

	private static boolean isAcceptedFileType(String fileName)
	{
		return fileName.toLowerCase().endsWith(".jar") || fileName.toLowerCase().endsWith(".zip");
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jface.viewers.ICheckStateListener#checkStateChanged(org.eclipse.jface.viewers.CheckStateChangedEvent)
	 */
	@Override
	public void checkStateChanged(CheckStateChangedEvent event)
	{
		ICheckable checkable = event.getCheckable();
		if (requiredFiles != null)
		{
			for (String requiredFile : requiredFiles)
				if (!checkable.getChecked(requiredFile + REQUIRED_LABEL)) checkable.setChecked(requiredFile + REQUIRED_LABEL, true);
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
		if (requiredFiles != null)
		{
			for (String requiredFile : requiredFiles)
				if (requiredFile != null && requiredFiles.length > 0)
					checkboxTableViewer.setChecked(requiredFile + DirectorySelectionPage.REQUIRED_LABEL, true);
		}
	}

	@Override
	public void restoreDefaults()
	{
		checkboxTableViewer.setAllChecked(true);
		selectAllButtons.enableAll();
		selectAllButtons.disableSelectAll();
	}
}