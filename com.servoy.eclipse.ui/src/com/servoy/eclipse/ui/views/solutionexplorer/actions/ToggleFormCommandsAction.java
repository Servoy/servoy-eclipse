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
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.ui.views.properties.IPropertyDescriptor;

import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.property.ComplexProperty;
import com.servoy.eclipse.ui.property.IRAGTEST;
import com.servoy.eclipse.ui.property.MethodWithArguments;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.eclipse.ui.property.PropertyCategory;
import com.servoy.eclipse.ui.property.RetargetToEditorPersistProperties;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.j2db.persistence.Form;

public class ToggleFormCommandsAction extends Action implements ISelectionChangedListener
{
	private final SolutionExplorerView viewer;

	public ToggleFormCommandsAction(SolutionExplorerView viewer)
	{
		this.viewer = viewer;
		setText("Toggle Form Commands");
		setToolTipText("Toggle Form Commands");
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		SimpleUserNode node = viewer.getSelectedTreeNode();
		if (node == null || !(node.getRealObject() instanceof Form)) return;
		setEnabled(node.getRealObject() instanceof Form && !((Form)node.getRealObject()).isFormComponent());
	}

	@Override
	public boolean isEnabled()
	{
		SimpleUserNode node = viewer.getSelectedTreeNode();
		if (node == null || !(node.getRealObject() instanceof Form) || ((Form)node.getRealObject()).isFormComponent()) return false;
		return node.getRealObject() instanceof Form;
	}

	@Override
	public void run()
	{
		SimpleUserNode node = viewer.getSelectedTreeNode();
		if (node == null || !(node.getRealObject() instanceof Form)) return;
		Form nodeForm = (Form)node.getRealObject();
		ServoyProject activeProject = ((ServoyProject)node.getAncestorOfType(ServoyProject.class).getRealObject());
		if (activeProject != null)
		{
			try
			{
				Form form = (Form)activeProject.getEditingPersist(nodeForm.getUUID());
				if (form != null)
				{
					IRAGTEST persistProperties = PersistPropertySource.createPersistPropertySource(form, form, false);
					RetargetToEditorPersistProperties propertiesSource = new RetargetToEditorPersistProperties(persistProperties);
					for (IPropertyDescriptor descriptor : propertiesSource.getPropertyDescriptors())
					{
						if (descriptor.getCategory().equals(PropertyCategory.Commands.toString()))
						{
							Object propertyValue = propertiesSource.getPropertyValue(descriptor.getId());
							if (propertyValue instanceof ComplexProperty< ? >)
							{
								ComplexProperty< ? > complexProperty = (ComplexProperty< ? >)propertyValue;
								Object complexPropertyValue = complexProperty.getValue();
								if (complexPropertyValue instanceof MethodWithArguments)
								{
									MethodWithArguments methodPropertyValue = (MethodWithArguments)complexPropertyValue;

									if (methodPropertyValue.methodId == 0)
									{
										propertiesSource.setPropertyValue(descriptor.getId(), new MethodWithArguments(-1, null));
									}
									else if (methodPropertyValue.methodId == -1)
									{
										propertiesSource.setPropertyValue(descriptor.getId(), new MethodWithArguments(0, null));
									}
								}
							}
						}
					}
					viewer.refreshSelection();
				}
			}
			catch (Exception ex)
			{
				ServoyLog.logError(ex);
			}
		}
	}
}
