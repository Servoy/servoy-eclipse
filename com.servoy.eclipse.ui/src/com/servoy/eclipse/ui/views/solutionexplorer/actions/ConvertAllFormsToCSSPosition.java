/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2018 Servoy BV

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
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.widgets.Shell;

import com.servoy.base.persistence.constants.IFormConstants;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.j2db.persistence.CSSPosition;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistVisitor;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;

/**
 * @author lvostinar
 *
 */
public class ConvertAllFormsToCSSPosition extends Action implements ISelectionChangedListener
{
	private final SolutionExplorerView viewer;

	public ConvertAllFormsToCSSPosition(SolutionExplorerView sev)
	{
		viewer = sev;
		setText("Convert all absolute forms to CSS Position");
		setToolTipText("Convert all absolute position forms to css position form. CSS Position can only be used in NGClient");
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		boolean state = (sel.size() == 1);
		if (state)
		{
			SimpleUserNode node = ((SimpleUserNode)sel.getFirstElement());
			state = (node.getType() == UserNodeType.SOLUTION && node.getRealObject() != null);
		}
		setEnabled(state);
	}

	@Override
	public void run()
	{
		SimpleUserNode node = viewer.getSelectedTreeNode();
		if (node.getRealObject() instanceof ServoyProject)
		{
			final ServoyProject project = (ServoyProject)node.getRealObject();
			if (UIUtils.askConfirmation(new Shell(), "CSS Position Conversion", "Are you sure you want to convert all forms from solution '" +
				project.getProject().getName() + "' to CSS Position? Note this action is irreversible and undoable."))
			{
				EditorUtil.saveDirtyEditors(viewer.getSite().getShell(), true);
				final Solution solution = project.getEditingSolution();
				solution.acceptVisitor(new IPersistVisitor()
				{
					public Object visit(IPersist object)
					{
						if (object instanceof Form && !((Form)object).isResponsiveLayout() &&
							(((Form)object).getView() == IFormConstants.VIEW_TYPE_RECORD || ((Form)object).getView() == IFormConstants.VIEW_TYPE_RECORD_LOCKED))
						{
							Form form = (Form)object;
							form.setUseCssPosition(true);
							CSSPosition.convertToCSSPosition(form);
						}
						return IPersistVisitor.CONTINUE_TRAVERSAL;
					}
				});
				try
				{
					project.saveEditingSolutionNodes(new IPersist[] { solution }, true);
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError(e);
				}
			}
		}
	}
}
