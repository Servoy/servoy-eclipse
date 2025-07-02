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
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils.InputAndListDialog;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.nature.ServoyResourcesProject;
import com.servoy.eclipse.model.repository.DataModelManager;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ValidatorSearchContext;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.docvalidator.IdentDocumentValidator;

/**
 * Action for duplicating the selected persist(s).
 *
 * @author acostescu
 */
public class DuplicatePersistAction extends AbstractPersistSelectionAction
{

	/**
	 * Creates a new "duplicate form" action.
	 */
	public DuplicatePersistAction(Shell shell)
	{
		super(shell);
		setImageDescriptor(Activator.loadImageDescriptorFromBundle("duplicate_form.png"));
		setText(Messages.DuplicateFormAction_duplicateForm);
		setToolTipText(Messages.DuplicateFormAction_duplicateForm);
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event)
	{
		super.selectionChanged(event);
		setText("Duplicate " + persistString);
		setToolTipText("Duplicates the " + persistString + " to a different solution/module");
	}

	private Location askForNewLocation(final IPersist persist, final IValidateName nameValidator)
	{
		// populate combo with available solutions
		final ServoyProject[] activeModules = ServoyModelManager.getServoyModelManager().getServoyModel().getModulesOfActiveProject();
		if (activeModules.length == 0)
		{
			ServoyLog.logError("No active modules on duplicate/move persist?!", null);
		}
		String[] solutionNames = new String[activeModules.length];
		String initialSolutionName = persist.getRootObject().getName();

		for (int i = activeModules.length - 1; i >= 0; i--)
		{
			solutionNames[i] = activeModules[i].getProject().getName();

		}
		Arrays.sort(solutionNames);

		//ExtendedInputDialog<String> dialog = createDialog(persist, nameValidator, solutionNames, initialSolutionName);
		String newName = null;
		String oldName = getName(persist);
		if (persist instanceof Media)
		{
			if (oldName.contains("/"))
			{
				// find the file name and add a prefix to it
				String[] arr = oldName.split("/");
				arr[arr.length - 1] = "copy_" + arr[arr.length - 1];
				newName = String.join("/", arr);
			}
			else
			{
				newName = "copy_" + oldName;
			}
		}
		else newName = oldName + "_copy";
		final String[] workingSetName = new String[] { null };
		// prepare dialog
		InputAndListDialog dialog = new InputAndListDialog(shell, "Duplicate " + persistString + " " + getName(persist),
			"Name of the duplicated " + persistString + ": ", newName, new IInputValidator()
			{
				public String isValid(String newText)
				{
					String message = null;
					String checkText = newText;
					if (checkText.startsWith(".") || checkText.endsWith(".") || checkText.startsWith("/") || checkText.contains("/.") ||
						checkText.contains("./") ||
						checkText.contains("//"))
					{
						return "Invalid name";
					}
					if (persist instanceof Media)
					{
						checkText = checkText.replace(".", "");
						checkText = checkText.replace("/", "");
					}
					message = IdentDocumentValidator.isJavaIdentifier(checkText) ? null : (newText.length() == 0 ? "" : "Invalid name");
					if (message == null)
					{
						try
						{
							nameValidator.checkName(newText, -1, new ValidatorSearchContext(getPersistType()), false);
						}
						catch (RepositoryException e)
						{
							message = e.getMessage();
							if (message == null) message = "Invalid name";
						}
					}
					return message;
				}

			}, solutionNames, initialSolutionName, "Please select the destination solution:")
		{
			private final Object SELECTION_NONE = new Object();

			@Override
			protected void validateInput()
			{
				super.validateInput();
				if (getExtendedValue() == null)
				{
					setErrorMessage("Select a module");
				}
			}

			@Override
			protected void addExtraComponents(Composite parent)
			{
				if (persist instanceof Form)
				{
					Label workingSetLabel = new Label(parent, SWT.NONE);
					workingSetLabel.setText("Working Set");

					final ComboViewer workingSetNameCombo = new ComboViewer(parent, SWT.BORDER | SWT.READ_ONLY);
					workingSetNameCombo.setContentProvider(new ArrayContentProvider());
					workingSetNameCombo.setLabelProvider(new LabelProvider()
					{
						@Override
						public String getText(Object value)
						{
							if (value == SELECTION_NONE) return Messages.LabelNone;
							return super.getText(value);
						}
					});
					workingSetNameCombo.getControl().setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
					fillWorkingSetCombo(workingSetNameCombo);
					listViewer.addSelectionChangedListener(new ISelectionChangedListener()
					{
						public void selectionChanged(SelectionChangedEvent event)
						{
							fillWorkingSetCombo(workingSetNameCombo);
						}
					});
					workingSetNameCombo.addSelectionChangedListener(new ISelectionChangedListener()
					{
						public void selectionChanged(SelectionChangedEvent event)
						{
							Object firstElement = ((IStructuredSelection)workingSetNameCombo.getSelection()).getFirstElement();
							if (firstElement instanceof String)
							{
								workingSetName[0] = (String)firstElement;
							}
							else
							{
								workingSetName[0] = null;
							}
						}
					});
				}
			}

			private void fillWorkingSetCombo(ComboViewer workingSetNameCombo)
			{
				List<Object> workingSets = new ArrayList<Object>();
				workingSets.add(SELECTION_NONE);
				String listValue = null;
				ServoyResourcesProject activeResourcesPropject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveResourcesProject();
				IStructuredSelection selection = (IStructuredSelection)listViewer.getSelection();
				if (!selection.isEmpty())
				{
					listValue = (String)selection.getFirstElement();
				}
				if (listValue != null && activeResourcesPropject != null)
				{
					List<String> existingWorkingSets = activeResourcesPropject
						.getServoyWorkingSets(
							new String[] { listValue });
					if (existingWorkingSets != null)
					{
						workingSets.addAll(existingWorkingSets);
					}
				}
				workingSetNameCombo.setInput(workingSets.toArray());

				String workingSetOfFormName = (activeResourcesPropject != null) ? activeResourcesPropject
					.getWorkingSetOfPersist(((Form)persist).getName(), solutionNames) : null;
				if (workingSetOfFormName != null)
				{
					workingSetNameCombo.setSelection(new StructuredSelection(workingSetOfFormName));
					workingSetName[0] = workingSetOfFormName;
				}
				else
				{
					workingSetNameCombo.setSelection(new StructuredSelection(SELECTION_NONE));
				}
			}
		};
		dialog.open();
		if (dialog.getExtendedValue() == null)
		{
			return null;
		}
		String projectName = dialog.getExtendedValue();
		ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(projectName);
		return (dialog.getReturnCode() == Window.CANCEL) ? null : new Location(dialog.getValue(), servoyProject, workingSetName[0]);
	}

	/**
	 * @see com.servoy.eclipse.ui.views.solutionexplorer.actions.AbstractPersistSelectionAction#doWork(com.servoy.j2db.persistence.Form, java.lang.Object[],
	 *      com.servoy.j2db.persistence.IValidateName)
	 */
	@Override
	protected void doWork(IPersist[] persists, IValidateName nameValidator)
	{
		for (final IPersist persist : persists)
		{
			Location location = askForNewLocation(persist, nameValidator);
			if (location != null)
			{
				try
				{
					IPersist duplicate = PersistCloner.intelligentClonePersist(persist, location.getPersistName(), location.getServoyProject(), nameValidator,
						true);
					IWorkspaceRoot root = ServoyModel.getWorkspace().getRoot();
					Pair<String, String> duplicatePath = SolutionSerializer.getFilePath(duplicate, false);
					if (persist instanceof Form frm)
					{
						Pair<String, String> persitPath = SolutionSerializer.getFilePath(frm, false);
						IFile lessFile = root.getFile(new Path(persitPath.getLeft() + frm.getName() + SolutionSerializer.FORM_LESS_FILE_EXTENSION));
						if (lessFile.exists())
						{
							IFile duplicateLessFile = root
								.getFile(new Path(duplicatePath.getLeft() + ((Form)duplicate).getName() + SolutionSerializer.FORM_LESS_FILE_EXTENSION));
							duplicateLessFile.create(lessFile.getContents(), true, null);
						}

						IFile secFile = root.getFile(new Path(persitPath.getLeft() + frm.getName() + DataModelManager.SECURITY_FILE_EXTENSION_WITH_DOT));
						if (secFile.exists())
						{
							IFile duplicateSecFile = root
								.getFile(new Path(duplicatePath.getLeft() + ((Form)duplicate).getName() + DataModelManager.SECURITY_FILE_EXTENSION_WITH_DOT));
							duplicateSecFile.create(secFile.getContents(), true, null);
						}
					}

					String parentWorkingSet = location.getWorkingSetName();
					if (parentWorkingSet != null)
					{
						IWorkingSet ws = PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSet(parentWorkingSet);
						if (ws != null)
						{
							List<IAdaptable> files = new ArrayList<IAdaptable>(Arrays.asList(ws.getElements()));
							IFile file = root.getFile(new Path(duplicatePath.getLeft() + duplicatePath.getRight()));
							files.add(file);
							ws.setElements(files.toArray(new IAdaptable[0]));
						}
					}
					EditorUtil.openPersistEditor(duplicate);
				}
				catch (RepositoryException | CoreException e)
				{
					ServoyLog.logError(e);
					MessageDialog.openError(shell, "Cannot duplicate form",
						persistString + " " + getName(persist) + "cannot be duplicated. Reason:\n" + e.getMessage());
				}
			}
		}
	}

	@Override
	protected boolean isEnabledForNode(UserNodeType type)
	{
		return type == UserNodeType.RELATION || type == UserNodeType.VALUELIST_ITEM || type == UserNodeType.MEDIA_IMAGE || type == UserNodeType.FORM;
	}
}