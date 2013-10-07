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
package com.servoy.eclipse.ui.dialogs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyResourcesProject;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.NameComparator;

/**
 * Content provider class for forms.
 * 
 * @author rgansevles
 * 
 */

public class FormContentProvider implements ITreeContentProvider
{
	private final FlattenedSolution flattenedSolution;
	private final Form form;
	private final Map<String, List<Integer>> workingSetForms = new HashMap<String, List<Integer>>();

	public FormContentProvider(FlattenedSolution flattenedSolution, Form form)
	{
		this.flattenedSolution = flattenedSolution;
		this.form = form;
	}

	@Override
	public Object[] getElements(Object inputElement)
	{
		if (inputElement instanceof FormListOptions)
		{
			ServoyResourcesProject activeProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveResourcesProject();
			List<String> workingSets = null;
			String[] solutionNames = flattenedSolution.getSolutionNames();
			if (activeProject != null)
			{
				workingSets = activeProject.getServoyWorkingSets(solutionNames);
			}
			FormListOptions options = (FormListOptions)inputElement;

			List<Object> formIdsAndWorkingSets = new ArrayList<Object>();
			if (options.includeNone) formIdsAndWorkingSets.add(new Integer(Form.NAVIGATOR_NONE));
			if (options.includeDefault) formIdsAndWorkingSets.add(new Integer(Form.NAVIGATOR_DEFAULT));
			if (options.includeIgnore) formIdsAndWorkingSets.add(new Integer(Form.NAVIGATOR_IGNORE));
			if (workingSets != null)
			{
				formIdsAndWorkingSets.addAll(workingSets);
			}

			switch (options.type)
			{
				case FORMS :
					Iterator<Form> forms = flattenedSolution.getForms(true);
					while (forms.hasNext())
					{
						Form obj = forms.next();
						if ((options.showInMenu == null || options.showInMenu.booleanValue() == obj.getShowInMenu()) && form != obj)
						{
							addFormInList(activeProject, obj, solutionNames, formIdsAndWorkingSets);
						}
					}
					break;

				case HIERARCHY :
					forms = flattenedSolution.getForms(false);
					Map<Form, Integer> possibleParentForms = new TreeMap<Form, Integer>(NameComparator.INSTANCE);
					while (forms.hasNext())
					{
						Form possibleParentForm = forms.next();
						if (form.getDataSource() == null || possibleParentForm.getDataSource() == null ||
							form.getDataSource().equals(possibleParentForm.getDataSource()))
						{
							// do not add the form if it is already a sub-form, to prevent cycles
							if (!flattenedSolution.getFormHierarchy(possibleParentForm).contains(form))
							{
								addFormInList(activeProject, possibleParentForm, solutionNames, formIdsAndWorkingSets);
							}
						}
					}

					formIdsAndWorkingSets.addAll(possibleParentForms.values());
					break;
			}
			return formIdsAndWorkingSets.toArray();
		}
		return null;
	}

	private void addFormInList(ServoyResourcesProject activeProject, Form form, String[] solutionNames, List<Object> formIdsAndWorkingSets)
	{
		String workingSetName = activeProject.getContainingWorkingSet(form.getName(), solutionNames);
		if (workingSetName == null)
		{
			formIdsAndWorkingSets.add(new Integer(form.getID()));
		}
		else
		{
			List<Integer> listForm = workingSetForms.get(workingSetName);
			if (listForm == null)
			{
				listForm = new ArrayList<Integer>();
				workingSetForms.put(workingSetName, listForm);
			}
			if (!listForm.contains(Integer.valueOf(form.getID())))
			{
				listForm.add(Integer.valueOf(form.getID()));
			}
		}
	}

	public static class FormListOptions
	{
		public static enum FormListType
		{
			FORMS, HIERARCHY;
		}

		public final Boolean showInMenu;
		public final boolean includeNone;
		public final boolean includeDefault;
		public final boolean includeIgnore;
		public final FormListType type;

		public FormListOptions(FormListType type, Boolean showInMenu, boolean includeNone, boolean includeDefault, boolean includeIgnore)
		{
			this.type = type;
			this.showInMenu = showInMenu;
			this.includeNone = includeNone;
			this.includeDefault = includeDefault;
			this.includeIgnore = includeIgnore;
		}
	}

	@Override
	public void dispose()
	{

	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
	{

	}

	@Override
	public Object[] getChildren(Object parentElement)
	{
		if (parentElement instanceof String)
		{
			return workingSetForms.get(parentElement).toArray();
		}
		return null;
	}

	@Override
	public Object getParent(Object element)
	{
		if (element instanceof Integer)
		{
			// a form
			ServoyResourcesProject activeProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveResourcesProject();
			Form form = flattenedSolution.getForm(((Integer)element).intValue());
			if (form != null)
			{
				activeProject.getContainingWorkingSet(form.getName(), flattenedSolution.getSolutionNames());
			}
		}
		return null;
	}

	@Override
	public boolean hasChildren(Object element)
	{
		if (element instanceof String)
		{
			// a working set
			ServoyResourcesProject activeProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveResourcesProject();
			return activeProject.hasPersistsInServoyWorkingSets((String)element, flattenedSolution.getSolutionNames());
		}
		return false;
	}

}
