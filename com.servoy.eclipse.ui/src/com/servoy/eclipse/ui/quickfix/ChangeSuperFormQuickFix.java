/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2016 Servoy BV

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

package com.servoy.eclipse.ui.quickfix;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.ui.dialogs.FormContentProvider;
import com.servoy.eclipse.ui.dialogs.LeafnodesSelectionFilter;
import com.servoy.eclipse.ui.dialogs.TreePatternFilter;
import com.servoy.eclipse.ui.dialogs.TreeSelectDialog;
import com.servoy.eclipse.ui.labelproviders.FormLabelProvider;
import com.servoy.eclipse.ui.labelproviders.SolutionContextDelegateLabelProvider;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.util.Debug;

/**
 * Allows the user to select a different parent form if the form and its parent do not have the same layout type.
 * @author emera
 */
public class ChangeSuperFormQuickFix implements IMarkerResolution
{

	private final ServoyProject project;
	private final Form form;


	public ChangeSuperFormQuickFix(Form form, ServoyProject servoyProject)
	{
		this.project = servoyProject;
		this.form = form;
	}

	@Override
	public String getLabel()
	{
		return "Select another parent form for '" + form.getName() + "'.";
	}

	@Override
	public void run(IMarker marker)
	{
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		FlattenedSolution flattenedSolution = project.getEditingFlattenedSolution();
		FormContentProvider contentProvider = new FormContentProvider(flattenedSolution, form);
		TreeSelectDialog dlg = new TreeSelectDialog(shell, true, false, TreePatternFilter.FILTER_LEAFS, contentProvider,
			new SolutionContextDelegateLabelProvider(new FormLabelProvider(flattenedSolution, true), flattenedSolution.getSolution()), null,
			new LeafnodesSelectionFilter(contentProvider), SWT.SINGLE, "Select parent form",
			new FormContentProvider.FormListOptions(FormContentProvider.FormListOptions.FormListType.HIERARCHY, null, true, false, false, false, null),
			StructuredSelection.EMPTY, true, null, null, false);

		if (dlg.open() == Window.OK)
		{
			if (!dlg.getSelection().isEmpty())
			{
				Object formUUID = ((StructuredSelection)dlg.getSelection()).getFirstElement();
				if (formUUID instanceof String)
				{
					Form superForm = project.getEditingSolution().getForm(formUUID.toString());
					form.setExtendsID(superForm != null ? superForm.getUUID().toString() : null);
					try
					{
						project.saveEditingSolutionNodes(new IPersist[] { form }, false);
					}
					catch (RepositoryException e)
					{
						Debug.error("Could not set parent form for " + form.getName(), e);
					}
				}
			}
		}

	}
}
