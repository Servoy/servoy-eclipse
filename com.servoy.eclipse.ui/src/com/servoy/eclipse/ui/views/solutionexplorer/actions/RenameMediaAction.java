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
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.Window;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.util.MediaNode;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.ValidatorSearchContext;

/**
 * Action to rename media.
 *
 * @author lvostinar
 *
 */
public class RenameMediaAction extends Action implements ISelectionChangedListener
{
	private final SolutionExplorerView viewer;
	private Solution solution;
	private String selectedMediaName = "";
	private String selectedMediaPath = "";

	/**
	 * Creates a new "rename media" action for the given solution view.
	 *
	 * @param viewer the solution view to use.
	 */
	public RenameMediaAction(SolutionExplorerView viewer)
	{
		this.viewer = viewer;

		setImageDescriptor(Activator.loadImageDescriptorFromBundle("rename_media.png"));
		setText("Rename media item");
		setToolTipText(getText());
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		solution = null;
		if (sel.size() == 1 && (((SimpleUserNode)sel.getFirstElement()).getType() == UserNodeType.MEDIA_IMAGE))
		{
			SimpleUserNode node = ((SimpleUserNode)sel.getFirstElement());
			SimpleUserNode solutionNode = node.getAncestorOfType(Solution.class);
			if (solutionNode != null)
			{
				//get media folder for further usage
				if (node.parent.getRealObject() instanceof Solution)
				{
					selectedMediaPath = "";
				}
				else if ((node.parent.getRealObject() instanceof MediaNode))
				{
					selectedMediaPath = ((MediaNode)node.parent.getRealObject()).getPath();
				}
				selectedMediaName = node.getName();

				// make sure you have the in-memory version of the solution
				solution = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(
					((Solution)solutionNode.getRealObject()).getName()).getEditingSolution();
			}
		}
		setEnabled(solution != null);

	}

	@Override
	public void run()
	{
		if (solution == null) return;
		InputDialog nameDialog = new InputDialog(viewer.getViewSite().getShell(), "Rename media item", "Supply a new media name", selectedMediaName,
			new IInputValidator()
			{
				public String isValid(String newText)
				{
					if (newText.length() == 0)
					{
						return "";
					}
					if (newText.indexOf('\\') >= 0 || newText.indexOf('/') >= 0 || newText.indexOf(' ') >= 0 || newText.endsWith(".") ||
						newText.startsWith("."))
					{
						return "Invalid new media name";
					}

					try
					{
						ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator().checkName(selectedMediaPath + newText, null,
							new ValidatorSearchContext(IRepository.MEDIA), false);
					}
					catch (RepositoryException e)
					{
						return e.getMessage();
					}
					// ok
					return null;
				}
			});
		int res = nameDialog.open();
		if (res == Window.OK)
		{
			String newName = nameDialog.getValue();
			ServoyProject project = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(solution.getName());
			Media selectedMediaItem = project.getEditingSolution().getMedia(
				selectedMediaPath != null ? selectedMediaPath + selectedMediaName : selectedMediaName);
			selectedMediaItem.setName(selectedMediaPath != null ? selectedMediaPath + newName : newName);
			try
			{
				project.saveEditingSolutionNodes(new IPersist[] { selectedMediaItem }, true);
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError(e);
			}

		}

	}

}
