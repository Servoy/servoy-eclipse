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


import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.widgets.Shell;

import com.servoy.base.query.IJoinConstants;
import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.dialogs.ModuleListSelectionDialog;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;

/**
 * Action to create a new form/global method depending on the selection of a solution view.
 *
 * @author jcompagner
 */
public class NewRelationAction extends Action implements ISelectionChangedListener
{

	private final SolutionExplorerView viewer;

	/**
	 * Creates a new "create new method" action for the given solution view.
	 *
	 * @param sev the solution view to use.
	 */
	public NewRelationAction(SolutionExplorerView sev)
	{
		viewer = sev;

		setImageDescriptor(Activator.loadImageDescriptorFromBundle("new_relation_wizard.png"));
		setText("Create relation");
		setToolTipText("Create relation");
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		boolean state = (sel.size() == 1);
		if (state)
		{
			UserNodeType type = ((SimpleUserNode)sel.getFirstElement()).getType();
			state = (type == UserNodeType.ALL_RELATIONS || type == UserNodeType.RELATIONS || type == UserNodeType.GLOBALRELATIONS);
		}
		setEnabled(state);
	}

	@Override
	public void run()
	{

		final Shell shell = viewer.getSite().getShell();

		SimpleUserNode node = viewer.getSelectedTreeNode();
		String solutionName = null;

		ModuleListSelectionDialog moduleSelDialog = new ModuleListSelectionDialog(shell, "Please select a module for the relation");

		final int resultCode = moduleSelDialog.open();
		if (resultCode == IDialogConstants.OK_ID)
		{
			solutionName = moduleSelDialog.getFirstResult().toString();
		}

		if (solutionName == null)
		{
			if (node.getRealObject() instanceof IPersist)
			{
				solutionName = ((Solution)((IPersist)node.getRealObject()).getRootObject()).getName();
			}
			else
			{
				SimpleUserNode solutionNode = node.getAncestorOfType(ServoyProject.class);
				if (solutionNode != null)
				{
					solutionName = ((ServoyProject)solutionNode.getRealObject()).getSolution().getName();
				}
			}
		}
		if (solutionName != null)
		{
			createRelation(solutionName, "untitled", node);

		}
	}

	public static void createRelation(String solutionName, String relationName, SimpleUserNode node)
	{
		try
		{
			IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
			IValidateName validator = servoyModel.getNameValidator();

			ServoyProject servoyProject = servoyModel.getServoyProject(solutionName);
			if (servoyProject == null)
			{
				return;
			}
			Solution editingSolution = servoyProject.getEditingSolution();
			Relation relation = editingSolution.getRelation(relationName);
			if (relation == null)
			{
				relation = editingSolution.createNewRelation(validator, relationName, IJoinConstants.LEFT_OUTER_JOIN);
				// this should be probably changed in content spec
				relation.setAllowCreationRelatedRecords(true);
			}

			if (node != null)
			{
				SimpleUserNode formNode = node.getAncestorOfType(Form.class);
				if (formNode != null && formNode.getForm() != null)
				{
					relation.setPrimaryDataSource(formNode.getForm().getDataSource());
					relation.setForeignDataSource(formNode.getForm().getDataSource());
				}
			}
			EditorUtil.openRelationEditor(relation);
		}
		catch (RepositoryException e)
		{
			ServoyLog.logError(e);
		}

	}
}
