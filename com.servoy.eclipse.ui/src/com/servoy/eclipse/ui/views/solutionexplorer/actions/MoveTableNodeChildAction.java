/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.IDeveloperRepository;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;

/**
 * Move for calculations and aggregations.
 * @author emera
 */
public class MoveTableNodeChildAction extends AbstractMovePersistAction
{

	/**
	 * @param shell
	 */
	public MoveTableNodeChildAction(Shell shell)
	{
		super(shell);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.eclipse.ui.views.solutionexplorer.actions.AbstractPersistAction#doWork(com.servoy.j2db.persistence.IPersist[],
	 * com.servoy.j2db.persistence.IValidateName)
	 */
	@Override
	protected void doWork(IPersist[] persistList, IValidateName nameValidator)
	{
		location = askForNewLocation(persistList[0], nameValidator);
		if (location == null)
		{
			return;
		}

		IRootObject rootObject = persistList[0].getRootObject();
		if (rootObject instanceof Solution)
		{
			ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(rootObject.getName());
			IPersist editingNode = servoyProject.getEditingPersist(persistList[0].getUUID());
			try
			{
				((IDeveloperRepository)editingNode.getRootObject().getRepository()).deleteObject(editingNode);
				servoyProject.saveEditingSolutionNodes(new IPersist[] { editingNode.getParent() }, true, false);
				PersistCloner.intelligentClonePersist(editingNode, ((ISupportName)editingNode).getName(), location.getServoyProject(), nameValidator, true);
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError(e);
				MessageDialog.openError(shell, "Cannot move",
					persistString + " " + ((ISupportName)persistList[0]).getName() + "cannot be moved. Reason:\n" + e.getMessage());
			}
		}

	}

}
