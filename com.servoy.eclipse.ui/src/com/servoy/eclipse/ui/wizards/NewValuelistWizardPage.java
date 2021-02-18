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
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.ui.Activator;
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
		setTitle("Select the solution and set the value-list name.");

		NewRelationWizard newRelWizard = new NewRelationWizard();
		newRelWizard.setDialogSettings(Activator.getDefault().getDialogSettings());

		retrieveCurrentSolutionNames();

	}

	private void retrieveCurrentSolutionNames()
	{
		ServoyProject servoyProject = servoyModel.getActiveProject();
		Solution solution = servoyProject.getSolution();
		ServoyProject projects[] = servoyModel.getModulesOfActiveProject();

		List<String> list = new ArrayList<String>();

		for (ServoyProject project : projects)
		{
			if (list.contains(project.getSolution().getName()) == false)
			{
				list.add(project.getSolution().getName());
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

		setControl(topLevel);

		Label solutionNamePatternLabel = new Label(topLevel, SWT.NONE);
		solutionNamePatternLabel.setText("Solution name");
		solutionNamePattern = new Text(topLevel, SWT.SINGLE | SWT.BORDER | SWT.SEARCH | SWT.ICON_CANCEL);
		solutionNamePattern.getAccessible().addAccessibleListener(new AccessibleAdapter()
		{
			@Override
			public void getName(AccessibleEvent e)
			{
				e.result = LegacyActionTools.removeMnemonics(solutionNamePatternLabel.getText());
			}
		});
		// Source server
		Label solutionNameLabel = new Label(topLevel, SWT.NONE);
		solutionNameLabel.setText("Select solution");

		tableViewer = new TableViewer(topLevel, SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL | SWT.VIRTUAL);
		tableViewer.setContentProvider(ArrayContentProvider.getInstance());
		tableViewer.setLabelProvider(new LabelProvider());
		tableViewer.setInput(currentSolutionNames);

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

		solutionNamePattern.addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyPressed(KeyEvent e)
			{
				filter.setSearchText(solutionNamePattern.getText());
				tableViewer.refresh();
			}
		});

		tableViewer.addFilter(filter);

		tableViewer.addSelectionChangedListener(new ISelectionChangedListener()
		{

			@Override
			public void selectionChanged(SelectionChangedEvent event)
			{
				selectedSolutionName = tableViewer.getSelection().toString();
			}
		});


		//Source table
		Label valuelistNameLabel = new Label(topLevel, SWT.NONE);
		valuelistNameLabel.setText("Value-list name");

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

		//Define the layout and place the components
		FormLayout formLayout = new FormLayout();
		formLayout.spacing = 10;
		formLayout.marginWidth = formLayout.marginHeight = 15;
		topLevel.setLayout(formLayout);

		//label
		FormData formData = new FormData();
		formData.left = new FormAttachment(0, 0);
		formData.top = new FormAttachment(solutionNamePattern, 0, SWT.CENTER);
		solutionNamePatternLabel.setLayoutData(formData);

		//text
		formData = new FormData();
		formData.left = new FormAttachment(solutionNamePatternLabel, 0);
		formData.top = new FormAttachment(0, 0);
		formData.right = new FormAttachment(100, 0);
		solutionNamePattern.setLayoutData(formData);

		//label
		formData = new FormData();
		formData.left = new FormAttachment(0, 0);
		formData.top = new FormAttachment(tableViewer.getTable(), 0, SWT.CENTER);
		solutionNameLabel.setLayoutData(formData);

		//table
		formData = new FormData();
		formData.left = new FormAttachment(solutionNamePattern, 0, SWT.LEFT);
		formData.top = new FormAttachment(solutionNamePattern, 0, SWT.BOTTOM);
		formData.right = new FormAttachment(100, 0);
		tableViewer.getTable().setLayoutData(formData);

		//label
		formData = new FormData();
		formData.left = new FormAttachment(0, 0);
		formData.top = new FormAttachment(valuelistNameText, 0, SWT.CENTER);
		valuelistNameLabel.setLayoutData(formData);

		//text
		formData = new FormData();
		formData.left = new FormAttachment(tableViewer.getTable(), 0, SWT.LEFT);
		formData.top = new FormAttachment(tableViewer.getTable(), 0, SWT.BOTTOM);
		formData.right = new FormAttachment(100, 0);
		valuelistNameText.setLayoutData(formData);

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
			validator.checkName(valueListName, 0, null, false);
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
			error = "Invalid relation name";
		}
		else
		{
			String valName = valuelistNameText.getText();
			String solName = ((IStructuredSelection)tableViewer.getSelection()).toList().get(0).toString();
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
			this.searchString = ".*" + s + ".*";
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
			}

			return false;
		}

	}


}
