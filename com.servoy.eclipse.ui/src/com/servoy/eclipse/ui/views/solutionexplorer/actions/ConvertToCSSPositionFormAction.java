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

import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.widgets.Shell;

import com.servoy.base.persistence.constants.IFormConstants;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.CSSPosition;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;

/**
 * @author lvostinar
 *
 */
public class ConvertToCSSPositionFormAction extends Action implements ISelectionChangedListener
{
	private final SolutionExplorerView viewer;

	public ConvertToCSSPositionFormAction(SolutionExplorerView viewer)
	{
		this.viewer = viewer;
		setText("Convert to CSS Position");
		setToolTipText("Convert to CSS Position");
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		SimpleUserNode node = viewer.getSelectedTreeNode();
		if (node == null || !(node.getRealObject() instanceof Form)) return;
		setEnabled(!((Form)node.getRealObject()).isResponsiveLayout() && !((Form)node.getRealObject()).getUseCssPosition());
	}

	@Override
	public boolean isEnabled()
	{
		SimpleUserNode node = viewer.getSelectedTreeNode();
		if (node == null || !(node.getRealObject() instanceof Form) || ((Form)node.getRealObject()).isResponsiveLayout() ||
			((Form)node.getRealObject()).getUseCssPosition()) return false;
		if (((Form)node.getRealObject()).getView() != IFormConstants.VIEW_TYPE_RECORD &&
			((Form)node.getRealObject()).getView() != IFormConstants.VIEW_TYPE_RECORD_LOCKED) return false;
		return true;
	}

	@Override
	public void run()
	{
		SimpleUserNode node = viewer.getSelectedTreeNode();
		if (node == null || !(node.getRealObject() instanceof Form)) return;
		Form nodeForm = (Form)node.getRealObject();
		ServoyProject parentProject = ((ServoyProject)node.getAncestorOfType(ServoyProject.class).getRealObject());
		if (parentProject != null)
		{
			if (UIUtils.askConfirmation(new Shell(), "CSS Position Conversion", "Are you sure you want to convert form '" + nodeForm.getName() +
				"' to CSS Position? Note this action is irreversible and undoable, also all forms in inheritance hierarchy will also be converted."))
			{

				Form form = (Form)parentProject.getEditingPersist(nodeForm.getUUID());
				if (ServoyModelFinder.getServoyModel().getActiveProject() != null)
				{
					FlattenedSolution fs = ServoyModelFinder.getServoyModel().getActiveProject().getEditingFlattenedSolution();
					List<Form> toConvertForms = fs.getFormHierarchy(form);

					Iterator<Form> it = fs.getForms(false);
					while (it.hasNext())
					{
						Form currentForm = it.next();
						if (!toConvertForms.contains(currentForm))
						{
							Form parentForm = currentForm.getExtendsForm();
							while (parentForm != null)
							{
								if (toConvertForms.contains(parentForm))
								{
									toConvertForms.add(currentForm);
									break;
								}
								parentForm = parentForm.getExtendsForm();
							}
						}
					}
					for (Form toConvert : toConvertForms)
					{
						try
						{
							if (toConvert.getParts().hasNext() || toConvert == form)
							{
								// skip abstract forms
								ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(
									toConvert.getSolution().getName());
								CSSPosition.convertToCSSPosition(toConvert);
								servoyProject.saveEditingSolutionNodes(new IPersist[] { toConvert }, true);
							}
						}
						catch (Exception ex)
						{
							ServoyLog.logError(ex);
						}
					}

				}
			}
		}
	}
}
