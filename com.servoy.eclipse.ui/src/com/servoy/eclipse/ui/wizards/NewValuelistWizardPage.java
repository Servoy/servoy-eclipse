/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2021 Servoy BV

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

package com.servoy.eclipse.ui.wizards;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.LegacyActionTools;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.ValidatorSearchContext;
import com.servoy.j2db.util.docvalidator.IdentDocumentValidator;

/**
 * @author Diana
 *
 */
public class NewValuelistWizardPage extends WizardPage implements Listener
{


	private final IDeveloperServoyModel servoyModel;

	private String[] currentSolutionNames;

	private String selectedSolutionName = "";
	private String selectedValueListName = "";
	private Text valuelistNameText;

	private final String activeSolutionName;

	private Text solutionNamePattern;

	private TableViewer tableViewer;

	private ItemsFilter filter;


	/**
	 * @param pageName
	 */
	protected NewValuelistWizardPage(String pageName, IDeveloperServoyModel servoyModel, String activeSolutionName)
	{
		super(pageName);
		this.servoyModel = servoyModel;
		this.activeSolutionName = activeSolutionName;
		setTitle("Select the solution and set the valuelist name.");

		retrieveCurrentSolutionNames();

	}

	private void retrieveCurrentSolutionNames()
	{
		Solution[] modules = activeSolutionName != null ? servoyModel.getServoyProject(activeSolutionName).getModules()
			: servoyModel.getActiveProject().getModules();

		List<String> list = new ArrayList<String>();

		for (Solution sol : modules)
		{
			if (list.contains(sol.getName()) == false)
			{
				list.add(sol.getName());
			}
		}

		currentSolutionNames = new String[list.size()];
		list.toArray(currentSolutionNames);

		filter = new ItemsFilter();
		filter.setSearchText("");
	}

	@Override
	public void setVisible(boolean visible)
	{
		super.setVisible(visible);

		if (visible)
		{
			setPageComplete(validatePage());
			//validatePage();
		}
	}

	/**
	 *
	 * @return a string[] with the selected combo items
	 */
	public String[] getSelectedItems()
	{
		selectedSolutionName = ((IStructuredSelection)tableViewer.getSelection()).toList().get(0).toString();
		selectedValueListName = valuelistNameText.getText();
		return new String[] { selectedSolutionName, selectedValueListName };
	}


	public void createControl(Composite parent)
	{
		initializeDialogUnits(parent);
		// top level group
		Composite topLevel = new Composite(parent, SWT.NONE);
		topLevel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));

		GridLayout gd = new GridLayout(2, false);
		gd.horizontalSpacing = 10;
		topLevel.setLayout(gd);

		setControl(topLevel);

		//Source table
		Label valuelistNameLabel = new Label(topLevel, SWT.NONE);
		valuelistNameLabel.setText("Valuelist name");

		valuelistNameText = new Text(topLevel, SWT.BORDER);
		valuelistNameText.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent event)
			{
				selectedValueListName = valuelistNameText.getText();
			}
		});

		valuelistNameText.addModifyListener(new ModifyListener()
		{
			public void modifyText(ModifyEvent e)
			{
				setPageComplete(validatePage());
			}
		});
		GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		valuelistNameText.setLayoutData(gridData);


		Label solutionNamePatternLabel = new Label(topLevel, SWT.NONE);
		solutionNamePattern = new Text(topLevel, SWT.SINGLE | SWT.BORDER | SWT.SEARCH | SWT.ICON_CANCEL);
		solutionNamePattern.setMessage("Filter solutions");
		solutionNamePattern.getAccessible().addAccessibleListener(new AccessibleAdapter()
		{
			@Override
			public void getName(AccessibleEvent e)
			{
				e.result = LegacyActionTools.removeMnemonics(solutionNamePatternLabel.getText());
			}
		});
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		solutionNamePattern.setLayoutData(gridData);

		// Source server
		Label solutionNameLabel = new Label(topLevel, SWT.NONE);
		solutionNameLabel.setText("Select solution");
		gridData = new GridData();
		gridData.verticalAlignment = SWT.TOP;
		solutionNameLabel.setLayoutData(gridData);

		tableViewer = new TableViewer(topLevel, SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL | SWT.VIRTUAL);
		tableViewer.setContentProvider(ArrayContentProvider.getInstance());
		tableViewer.setLabelProvider(new LabelProvider());
		tableViewer.setInput(currentSolutionNames);
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.verticalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.heightHint = 200;
		tableViewer.getTable().setLayoutData(gridData);

		int counter = 0;
		if (activeSolutionName.equals("") == false)
		{
			for (int i = 0; i < currentSolutionNames.length; i++)
			{
				if (currentSolutionNames[i].equals(activeSolutionName))
				{
					counter = i;
					break;
				}
			}
		}
		tableViewer.setSelection(new StructuredSelection(currentSolutionNames[counter]));

		solutionNamePattern.addModifyListener(new ModifyListener()
		{
			@Override
			public void modifyText(ModifyEvent e)
			{
				filter.setSearchText(solutionNamePattern.getText());
				tableViewer.refresh();
				Display.getDefault().asyncExec(() -> {
					Object elementAt = tableViewer.getElementAt(0);
					if (elementAt != null)
					{
						tableViewer.setSelection(new StructuredSelection(elementAt));
					}
				});
			}
		});

		tableViewer.addFilter(filter);

		tableViewer.addSelectionChangedListener(new ISelectionChangedListener()
		{
			@Override
			public void selectionChanged(SelectionChangedEvent event)
			{
				selectedSolutionName = tableViewer.getSelection().toString();
				setPageComplete(validatePage());
			}
		});

	}

	protected String valueListNameOk(String valueListName, String solutionName)
	{

		IValidateName validator = servoyModel.getNameValidator();
		ServoyProject servoyProject = servoyModel.getServoyProject(solutionName);

		ValidatorSearchContext searchContext = null;
		if (servoyProject != null)
		{
			searchContext = new ValidatorSearchContext(servoyProject.getEditingSolution(), IRepository.VALUELISTS);
		}
		try
		{
			validator.checkName(valueListName, null, searchContext, false);
		}
		catch (RepositoryException e)
		{
			return e.getMessage();
		}

		return "";
	}

	protected boolean validatePage()
	{
		String error = null;
		if (valuelistNameText.getText().trim().length() == 0)
		{
			error = "Enter a valuelist name";
		}
		else if (!IdentDocumentValidator.isJavaIdentifier(valuelistNameText.getText()))
		{
			error = "Invalid valuelist name";
		}
		else if (tableViewer.getSelection().isEmpty())
		{
			error = "Select a solution";
		}
		else
		{
			String valName = valuelistNameText.getText();
			String solName = ((IStructuredSelection)tableViewer.getSelection()).getFirstElement().toString();
			String searchResult = valueListNameOk(valName, solName);
			if (!searchResult.equals(""))
			{
				setErrorMessage(searchResult);
				return false;
			}
		}

		setErrorMessage(error);
		return error == null;
	}

	@Override
	public void handleEvent(Event event)
	{
		setPageComplete(validatePage());
	}

	private class ItemsFilter extends ViewerFilter
	{

		private String searchString;

		public void setSearchText(String s)
		{
			// ensure that the value can be used for matching
			this.searchString = "(?i)" + s + ".*";
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see org.eclipse.jface.viewers.ViewerFilter#select(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
		 */
		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element)
		{
			if (searchString != null && !"".equals(searchString))
			{
				String elementString = element.toString();
				if (elementString.matches(searchString))
				{
					return true;
				}
				return false;
			}

			return true;
		}

	}
}
