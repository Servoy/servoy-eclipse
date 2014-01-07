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
package com.servoy.eclipse.ui.views.solutionexplorer.actions;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils.ExtendedInputDialog;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.EclipseRepository;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.AggregateVariable;
import com.servoy.j2db.persistence.ContentSpec;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistVisitor;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.ISupportUpdateableName;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptCalculation;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.Utils;

/**
 * @author jcompagner
 * 
 */
public abstract class AbstractMovePersistAction extends Action implements ISelectionChangedListener
{

	private IStructuredSelection selection;
	protected final Shell shell;
	protected UserNodeType moveType;
	protected String persistString = null;
	protected IPersist selectedPersist;

	/**
	 * 
	 */
	public AbstractMovePersistAction(Shell shell)
	{
		super();
		this.shell = shell;
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		moveType = null;
		selection = null;
		ServoyProject project = null;
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		boolean state = (sel.size() > 0);
		if (state)
		{
			Iterator<SimpleUserNode> it = sel.iterator();
			while (it.hasNext())
			{

				SimpleUserNode node = it.next();
				UserNodeType type = node.getType();
				SimpleUserNode projectNode = node.getAncestorOfType(ServoyProject.class);
				if (projectNode != null)
				{
					ServoyProject sp = (ServoyProject)projectNode.getRealObject();
					if (project == null) project = sp;
					else
					{
						if (!project.equals(sp))
						{
							state = false;
						}
					}
					if (moveType == null)
					{
						moveType = type;
					}
					if (type != moveType ||
						(type != UserNodeType.RELATION && type != UserNodeType.VALUELIST_ITEM && type != UserNodeType.MEDIA_IMAGE && type != UserNodeType.FORM))
					{
						state = false;
						break;
					}
				}
				else
				{
					state = false;
				}
			}
			selection = sel;
		}
		if (moveType == UserNodeType.RELATION) persistString = "relation"; //$NON-NLS-1$
		if (moveType == UserNodeType.VALUELIST_ITEM) persistString = "valuelist";//$NON-NLS-1$
		if (moveType == UserNodeType.MEDIA_IMAGE) persistString = "media";//$NON-NLS-1$
		if (moveType == UserNodeType.FORM) persistString = "form";//$NON-NLS-1$

		setEnabled(state);
	}

	protected int getPersistType()
	{
		if (moveType == UserNodeType.FORM) return IRepository.FORMS;
		if (moveType == UserNodeType.RELATION) return IRepository.RELATIONS;
		if (moveType == UserNodeType.VALUELISTS) return IRepository.VALUELISTS;
		if (moveType == UserNodeType.MEDIA_IMAGE) return IRepository.MEDIA;
		if (selectedPersist instanceof ScriptCalculation) return IRepository.SCRIPTCALCULATIONS;
		if (selectedPersist instanceof AggregateVariable) return IRepository.AGGREGATEVARIABLES;
		return -1;
	}

	public void setPersist(IPersist persist)
	{
		this.selectedPersist = persist;
		if (persist instanceof ScriptCalculation) persistString = "calculation";//$NON-NLS-1$
		if (persist instanceof AggregateVariable) persistString = "aggregation";//$NON-NLS-1$
	}

	protected boolean isMoving = false;

	@Override
	public void run()
	{
		IValidateName nameValidator = ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator();
		if (selection != null)
		{
			isMoving = true;
			List<IPersist> persistList = new ArrayList<IPersist>();
			Iterator<SimpleUserNode> it = selection.iterator();
			while (it.hasNext())
			{
				SimpleUserNode node = it.next();
				SimpleUserNode projectNode = node.getAncestorOfType(ServoyProject.class);
				if (projectNode != null)
				{
					IPersist persist = (IPersist)node.getRealObject();
					if (persist instanceof ISupportName)
					{
						persistList.add(persist);
					}
				}
			}
			
			doWork(persistList.toArray(new IPersist[persistList.size()]), nameValidator);
			isMoving = false;
		}
		if (selectedPersist instanceof ISupportName)
		{
			doWork(new IPersist[] { selectedPersist }, nameValidator);
		}
	}

	/**
	 * @param editingForms
	 * @param nameValidator
	 */
	protected abstract void doWork(IPersist[] editingForms, IValidateName nameValidator);

	protected Location askForNewFormLocation(final IPersist persist, final IValidateName nameValidator)
	{
		// populate combo with available solutions
		final ServoyProject[] activeModules = ServoyModelManager.getServoyModelManager().getServoyModel().getModulesOfActiveProject();
		if (activeModules.length == 0)
		{
			ServoyLog.logError("No active modules on duplicate/move persist?!", null); //$NON-NLS-1$
		}
		String[] solutionNames = new String[activeModules.length];
		String initialSolutionName = persist.getRootObject().getName();

		for (int i = activeModules.length - 1; i >= 0; i--)
		{
			solutionNames[i] = activeModules[i].getProject().getName();

		}
		Arrays.sort(solutionNames);

		ExtendedInputDialog<String> dialog = createDialog(persist, nameValidator, solutionNames, initialSolutionName);
		dialog.open();
		if (dialog.getExtendedValue() == null)
		{
			return null;
		}
		String projectName = dialog.getExtendedValue();
		ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(projectName);
		return (dialog.getReturnCode() == Window.CANCEL) ? null : new Location(dialog.getValue(), servoyProject);
	}

	/**
	 * Creates a copy of the given form at the specified location. Also updates references within the new form to form methods and form variables.
	 * 
	 * @param formToDuplicate the form to be duplicated/copied.
	 * @param location the location where the new form should be put. Object[2] - { newFormName, destinationSolutionServoyProject }
	 * @param nameValidator the name validator to be used.
	 * @throws RepositoryException when the new form cannot be persisted.
	 */
	public IPersist intelligentClonePersist(IPersist persist, String newPersistName, ServoyProject servoyProject, IValidateName nameValidator, boolean save)
		throws RepositoryException
	{
		IPersist duplicate = duplicatePersist(persist, newPersistName, servoyProject, nameValidator);
		if (duplicate != null && persist instanceof Form)
		{
			// link the events that point to form methods to the cloned methods instead of the original form methods
			relinkFormMethodsToEvents((Form)persist, (Form)duplicate);
		}
		if (save)
		{
			ServoyProject sp = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(duplicate.getRootObject().getName());
			if (sp != null)
			{
				if (duplicate.getParent() instanceof Solution)
				{
					sp.saveEditingSolutionNodes(new IPersist[] { duplicate }, true);
				}
				else
				{
					sp.saveEditingSolutionNodes(new IPersist[] { duplicate.getParent() }, true);
				}
			}
			else
			{
				ServoyLog.logError("Cannot find solution project for duplicated form", null); //$NON-NLS-1$
			}
		}
		return duplicate;
	}

	protected void relinkFormMethodsToEvents(final Form original, final Form duplicate)
	{
		duplicate.acceptVisitor(new IPersistVisitor()
		{

			public Object visit(IPersist o)
			{
				try
				{
					ServoyModelManager.getServoyModelManager().getServoyModel();
					EclipseRepository er = (EclipseRepository)ServoyModel.getDeveloperRepository();
					Iterator<ContentSpec.Element> iterator = er.getContentSpec().getPropertiesForObjectType(o.getTypeID());

					while (iterator.hasNext())
					{
						final ContentSpec.Element element = iterator.next();
						// Don't set meta data properties.
						if (element.isMetaData() || element.isDeprecated()) continue;
						// Get default property value as an object.
						final int typeId = element.getTypeID();

						if (typeId == IRepository.ELEMENTS)
						{
							Object property_value = ((AbstractBase)o).getProperty(element.getName());
							final int element_id = Utils.getAsInteger(property_value);
							if (element_id > 0)
							{
								boolean idFound = false;
								Iterator<ScriptMethod> originalScriptMethods = original.getScriptMethods(false);
								while (originalScriptMethods.hasNext())
								{
									ScriptMethod originalMethod = originalScriptMethods.next();
									if (originalMethod.getID() == element_id)
									{
										// reference to a method in the original form - change this to the duplicated form method
										ScriptMethod duplicateMethod = duplicate.getScriptMethod(originalMethod.getName());
										if (duplicateMethod != null)
										{
											((AbstractBase)o).setProperty(element.getName(), new Integer(duplicateMethod.getID()));
										}
										idFound = true;
									}
								}

								if (!idFound)
								{
									Iterator<ScriptVariable> originalScriptVariables = original.getScriptVariables(false);
									while (originalScriptVariables.hasNext())
									{
										ScriptVariable originalVariable = originalScriptVariables.next();
										if (originalVariable.getID() == element_id)
										{
											// reference to a variable in the original form - change this to the duplicated form variable
											ScriptVariable duplicateVariable = duplicate.getScriptVariable(originalVariable.getName());
											if (duplicateVariable != null)
											{
												((AbstractBase)o).setProperty(element.getName(), new Integer(duplicateVariable.getID()));
											}
										}
									}
								}
							}
						}
					}
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError(e);
				}
				catch (IllegalArgumentException e)
				{
					ServoyLog.logError(e);
				}
				return IPersistVisitor.CONTINUE_TRAVERSAL;
			}

		});
	}

	protected IPersist duplicatePersist(IPersist persist, String newPersistName, ServoyProject servoyProject, IValidateName nameValidator)
	{
		try
		{
			if (newPersistName != null && servoyProject != null)
			{
				Solution destinationEditingSolution = servoyProject.getEditingSolution();

				if (destinationEditingSolution != null)
				{
					if (persist instanceof Media)
					{
						newPersistName = Utils.stringReplace(newPersistName, " ", "_");//$NON-NLS-1$//$NON-NLS-2$
					}
					AbstractBase clone = null;
					if (persist instanceof ScriptCalculation)
					{
						clone = destinationEditingSolution.createNewScriptCalculation(nameValidator, ((ScriptCalculation)persist).getTable().getDataSource(),
							newPersistName, null);
						clone.copyPropertiesMap(((ScriptCalculation)persist).getPropertiesMap(), true);
					}
					else if (persist instanceof AggregateVariable)
					{
						clone = destinationEditingSolution.createNewAggregateVariable(nameValidator, ((AggregateVariable)persist).getTable().getDataSource(),
							newPersistName, ((AggregateVariable)persist).getType(), ((AggregateVariable)persist).getDataProviderIDToAggregate());
						clone.copyPropertiesMap(((AggregateVariable)persist).getPropertiesMap(), true);
					}
					else
					{
						clone = (AbstractBase)((AbstractBase)persist).cloneObj(destinationEditingSolution, true, nameValidator, true, false //
							, false /* elements of original form should remain override, not a flattened element */);
					}
					if (clone instanceof ISupportUpdateableName)
					{
						((ISupportUpdateableName)clone).updateName(nameValidator, newPersistName);
					}
					else if (clone instanceof Media)
					{
						((Media)clone).setName(newPersistName);
						((Media)clone).setPermMediaData(((Media)persist).getMediaData());
					}
					clone.setRuntimeProperty(AbstractBase.NameChangeProperty, "");//$NON-NLS-1$
					return clone;
				}
				else
				{
					ServoyLog.logError("Cannot get solution for destination solution project", null);//$NON-NLS-1$
				}
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
			MessageDialog.openError(shell, "Cannot duplicate form",//$NON-NLS-1$
				"Persist " + ((ISupportName)persist).getName() + "cannot be duplicated. Reason:\n" + e.getMessage());//$NON-NLS-1$//$NON-NLS-2$
		}
		return null;
	}

	/**
	 * @param form
	 * @param nameValidator
	 * @param solutionNames
	 * @param initialSolutionIndex
	 * @return
	 */
	protected abstract ExtendedInputDialog<String> createDialog(final IPersist persist, final IValidateName nameValidator, String[] solutionNames,
		String initialSolutionName);

	protected class Location
	{
		private final ServoyProject project;
		private final String persistName;

		/**
		 * @param value
		 * @param servoyProject
		 */
		public Location(String persistName, ServoyProject servoyProject)
		{
			this.persistName = persistName;
			this.project = servoyProject;
		}

		/**
		 * @return
		 */
		public String getPersistName()
		{
			return persistName;
		}

		/**
		 * @return
		 */
		public ServoyProject getServoyProject()
		{
			return project;
		}
	}

}