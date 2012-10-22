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
package com.servoy.eclipse.designer.editor;


import java.util.List;

import org.eclipse.gef.editparts.AbstractGraphicalEditPart;

import com.servoy.eclipse.designer.property.IPersistEditPart;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.Form;

/**
 * The Contents Graphical Edit Part for a Servoy Form.
 * 
 * @author rgansevles
 */
public abstract class BaseFormGraphicalEditPart extends AbstractGraphicalEditPart implements IPersistEditPart
{
	private final IApplication application;
	private final BaseVisualFormEditor editorPart;

	public BaseFormGraphicalEditPart(IApplication application, BaseVisualFormEditor editorPart)
	{
		this.application = application;
		this.editorPart = editorPart;
		setModel(editorPart.getForm());
	}

	/**
	 * @return the editorPart
	 */
	public BaseVisualFormEditor getEditorPart()
	{
		return editorPart;
	}

	/**
	 * @return the application
	 */
	public IApplication getApplication()
	{
		return application;
	}

	@Override
	protected abstract List<Object> getModelChildren();

	@Override
	protected void refreshVisuals()
	{
		getFigure().repaint();
		super.refreshVisuals();
	}

	/*
	 * Added because refreshing forms with many children (specially tabpanels) leads to performance degradation (refresh takes too long).
	 */
	public void refreshWithoutChildren()
	{
		refreshVisuals();
		refreshSourceConnections();
		refreshTargetConnections();
	}

	public Form getPersist()
	{
		return (Form)getModel();
	}

	public boolean isInherited()
	{
		return false;
	}
}
