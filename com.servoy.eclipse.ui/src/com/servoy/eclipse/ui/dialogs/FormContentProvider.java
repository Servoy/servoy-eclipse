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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Display;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.SpecProviderState;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebObjectSpecification;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyResourcesProject;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.PersistEncapsulation;
import com.servoy.j2db.server.ngclient.property.types.FormComponentPropertyType;

/**
 * Content provider class for forms.
 *
 * @author rgansevles
 *
 */

public class FormContentProvider implements ITreeContentProvider
{
	private final FlattenedSolution flattenedSolution;
	private final Form childForm;
	private final Map<String, List<String>> workingSetForms = new HashMap<String, List<String>>();
	private Object[] formIdsAndWorkingSets = new Object[] { "-1" };
	private FormListOptions options;

	public FormContentProvider(FlattenedSolution flattenedSolution, Form form)
	{
		this.flattenedSolution = flattenedSolution;
		this.childForm = form;
	}

	@Override
	public Object[] getElements(Object inputElement)
	{
		return formIdsAndWorkingSets;
	}

	private void addFormInList(ServoyResourcesProject activeProject, Form f, String[] solutionNames, ArrayList<Object> list)
	{
		String workingSetName = activeProject != null ? activeProject.getContainingWorkingSet(f.getName(), solutionNames) : null;
		if (workingSetName == null)
		{
			list.add(f.getUUID().toString());
		}
		else
		{
			List<String> listForm = workingSetForms.get(workingSetName);
			if (listForm == null)
			{
				listForm = new ArrayList<String>();
				workingSetForms.put(workingSetName, listForm);
			}
			if (!listForm.contains(f.getUUID().toString()))
			{
				listForm.add(f.getUUID().toString());
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
		public final boolean showTemplates;
		public final String datasource;
		public final FormListType type;

		public FormListOptions(FormListType type, Boolean showInMenu, boolean includeNone, boolean includeDefault, boolean includeIgnore, boolean showTemplates,
			String datasource)
		{
			this.type = type;
			this.showInMenu = showInMenu;
			this.includeNone = includeNone;
			this.includeDefault = includeDefault;
			this.includeIgnore = includeIgnore;
			this.showTemplates = showTemplates;
			this.datasource = datasource;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (obj instanceof FormListOptions)
			{
				FormListOptions opt = (FormListOptions)obj;
				return (type == null && opt.type == null || type != null && type.equals((opt).type)) &&
					(showInMenu == null && opt.showInMenu == null || showInMenu != null && showInMenu.equals(opt.showInMenu)) &&
					includeNone == opt.includeNone && includeIgnore == opt.includeIgnore && showTemplates == opt.showTemplates &&
					(datasource == null && opt.datasource == null || datasource != null && datasource.equals(opt.datasource));
			}
			return false;
		}
	}

	@Override
	public void dispose()
	{

	}

	@Override
	public void inputChanged(final Viewer viewer, Object oldInput, final Object inputElement)
	{
		if (inputElement instanceof FormListOptions)
		{
			Job job = new Job("Searching possible parent forms")
			{
				@Override
				public boolean belongsTo(Object family)
				{
					return family == FilteredTreeViewer.CONTENT_LOADING_JOB_FAMILY;
				}

				@Override
				protected IStatus run(IProgressMonitor monitor)
				{
					ServoyResourcesProject activeProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveResourcesProject();
					List<String> workingSets = null;
					String[] solutionNames = flattenedSolution.getSolutionNames();
					if (activeProject != null)
					{
						workingSets = activeProject.getServoyWorkingSets(solutionNames);
					}
					options = (FormListOptions)inputElement;

					ArrayList<Object> list = new ArrayList<Object>();
					if (options.includeNone) list.add(Form.NAVIGATOR_NONE);
					if (options.includeDefault) list.add(Form.NAVIGATOR_DEFAULT);
					if (options.includeIgnore) list.add(Form.NAVIGATOR_IGNORE);
					if (workingSets != null)
					{
						list.addAll(workingSets);
					}
					switch (options.type)
					{
						case FORMS :
							Iterator<Form> forms = flattenedSolution.getForms(options.datasource, true);
							while (forms.hasNext())
							{
								Form obj = forms.next();
								if ((options.showInMenu == null || options.showInMenu.booleanValue() == obj.getShowInMenu()) &&
									(options.showTemplates == obj.isFormComponent().booleanValue()) && childForm != obj &&
									!PersistEncapsulation.isModuleScope(obj, flattenedSolution.getSolution()))
								{
									// skip if form component list and isAbsoluteCSSPositionMix
									if ((options.showTemplates == obj.isFormComponent().booleanValue()) && childForm != null &&
										!childForm.isResponsiveLayout() && !obj.isResponsiveLayout() &&
										(childForm.getUseCssPosition() != obj.getUseCssPosition()))
									{
										continue;
									}
									if (options.showTemplates && childForm != null && childForm.isFormComponent().booleanValue())
									{
										// check for recursion.
										Set<String> usedFormComponents = new HashSet<>();
										getAllFormComponents(obj, usedFormComponents);
										if (usedFormComponents.contains(childForm.getName()))
											continue;
									}

									addFormInList(activeProject, obj, solutionNames, list);
								}
							}
							break;

						case HIERARCHY :
							forms = flattenedSolution.getForms(childForm.getDataSource(), true);
							while (forms.hasNext())
							{
								Form possibleParentForm = forms.next();
								if (((childForm.getUseCssPosition() == possibleParentForm.getUseCssPosition() &&
									childForm.isResponsiveLayout() == possibleParentForm.isResponsiveLayout()) ||
									(!possibleParentForm.getParts().hasNext()) && !possibleParentForm.isResponsiveLayout()) &&
									!PersistEncapsulation.isModuleScope(possibleParentForm, flattenedSolution.getSolution()))
								{
									// do not add the form if it is already a sub-form, to prevent cycles
									if (!flattenedSolution.getFormHierarchy(possibleParentForm).contains(childForm))
									{
										addFormInList(activeProject, possibleParentForm, solutionNames, list);
									}
								}
							}
							break;
					}
					formIdsAndWorkingSets = list.toArray();
					Display.getDefault().asyncExec(new Runnable()
					{
						@Override
						public void run()
						{
							viewer.refresh();
						}
					});
					return Status.OK_STATUS;
				}

				/**
				 * @param usedFormComponents
				 */
				private void getAllFormComponents(Form form, Set<String> usedFormComponents)
				{
					SpecProviderState specs = WebComponentSpecProvider.getSpecProviderState();
					Form ff = flattenedSolution.getFlattenedForm(form);
					ff.getWebComponents().forEachRemaining(comp -> {
						String typeName = comp.getTypeName();
						WebObjectSpecification spec = specs.getWebObjectSpecification(typeName);
						if (spec != null)
						{
							Collection<PropertyDescription> properties = spec.getProperties(FormComponentPropertyType.INSTANCE);
							if (properties != null && properties.size() > 0)
							{
								properties.forEach(property -> {
									Object frmValue = comp.getProperty(property.getName());
									Form frm = FormComponentPropertyType.INSTANCE.getForm(frmValue, flattenedSolution);
									if (frm != null)
									{
										if (usedFormComponents.add(frm.getName()))
										{
											getAllFormComponents(frm, usedFormComponents);
										}
									}
								});
							}
						}
					});
				}
			};
			job.schedule();
		}
	}

	@Override
	public Object[] getChildren(Object parentElement)
	{
		if (parentElement instanceof String && workingSetForms.containsKey(parentElement))
		{
			return workingSetForms.get(parentElement).toArray();
		}
		return null;
	}

	@Override
	public Object getParent(Object element)
	{
		if (element instanceof String elementUUID)
		{
			// a form
			ServoyResourcesProject activeProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveResourcesProject();
			Form form = flattenedSolution.getForm(elementUUID);
			if (activeProject != null && form != null)
			{
				return activeProject.getContainingWorkingSet(form.getName(), flattenedSolution.getSolutionNames());
			}
		}
		return null;
	}

	@Override
	public boolean hasChildren(Object element)
	{
		if (element instanceof String && workingSetForms.containsKey(element))
		{
			// a working set, just return true because of LeafnodesSelectionFilter, this can have children
			return true;
		}
		return false;
	}

}
