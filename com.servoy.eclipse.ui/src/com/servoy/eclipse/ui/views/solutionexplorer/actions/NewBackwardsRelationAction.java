/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2025 Servoy BV

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

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RelationItem;

/**
 * @author lvostinar
 *
 */
public class NewBackwardsRelationAction extends Action implements ISelectionChangedListener
{

	private Relation selectedRelation;

	/**
	 * Creates a new open action for relations.
	 */
	public NewBackwardsRelationAction()
	{
		setImageDescriptor(Activator.loadImageDescriptorFromBundle("duplicate_form.png"));
		setText("Create backwards relation");
		setToolTipText("Create backwards relation");
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		selectedRelation = null;
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		boolean state = (sel.size() == 1);
		if (state)
		{
			UserNodeType type = ((SimpleUserNode)sel.getFirstElement()).getType();
			state = (type == UserNodeType.RELATION) || (type == UserNodeType.CALC_RELATION);
			if (state)
			{
				selectedRelation = (Relation)((SimpleUserNode)sel.getFirstElement()).getRealObject();
			}
		}
		setEnabled(state);
	}

	@Override
	public void run()
	{
		if (selectedRelation != null)
		{
			String solutionName = selectedRelation.getRootObject().getName();
			IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
			IValidateName validator = servoyModel.getNameValidator();

			ServoyProject servoyProject = servoyModel.getServoyProject(solutionName);
			if (servoyProject == null)
			{
				return;
			}

			try
			{
				Relation relation = (Relation)PersistCloner.intelligentClonePersist(selectedRelation,
					selectedRelation.getForeignTableName() + "_to_" + selectedRelation.getPrimaryTableName(), servoyProject, validator, false);
				relation.setPrimaryDataSource(selectedRelation.getForeignDataSource());
				relation.setForeignDataSource(selectedRelation.getPrimaryDataSource());
				List<IPersist> newItems = relation.getAllObjectsAsList();
				List<IPersist> oldItems = selectedRelation.getAllObjectsAsList();
				if (newItems.size() != oldItems.size())
				{
					ServoyLog.logError("Error creating backwards relation: size mismatch between new and old items", null);
					return;
				}
				for (int i = 0; i < oldItems.size(); i++)
				{
					IPersist oldItem = oldItems.get(i);
					IPersist newItem = newItems.get(i);
					if (oldItem instanceof RelationItem oldRelationItem && newItem instanceof RelationItem newRelationItem)
					{
						newRelationItem.setPrimaryDataProviderID(oldRelationItem.getForeignColumnName());
						newRelationItem.setForeignColumnName(oldRelationItem.getPrimaryDataProviderID());
					}
				}
				EditorUtil.openRelationEditor(relation);
			}
			catch (Exception e)
			{
				ServoyLog.logError("Error creating backwards relation", e);
			}
		}
	}
}