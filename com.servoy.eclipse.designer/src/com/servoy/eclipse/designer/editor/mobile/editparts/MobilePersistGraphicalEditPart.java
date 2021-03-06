/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.eclipse.designer.editor.mobile.editparts;

import org.eclipse.gef.DragTracker;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.tools.DragEditPartsTracker;

import com.servoy.eclipse.designer.editor.IFigureFactory;
import com.servoy.eclipse.designer.editor.PersistGraphicalEditPart;
import com.servoy.eclipse.designer.editor.PersistImageFigure;
import com.servoy.eclipse.designer.editor.commands.SelectModelsCommandWrapper;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;

/**
 * Graphical edit part for persists in mobile form editor.
 * 
 * @author rgansevles
 *
 */
public class MobilePersistGraphicalEditPart extends PersistGraphicalEditPart
{
	/**
	 * @param application
	 * @param model
	 * @param form
	 * @param inherited
	 * @param figureFactory
	 */
	public MobilePersistGraphicalEditPart(IApplication application, IPersist model, Form form, boolean inherited,
		IFigureFactory< ? extends PersistImageFigure> figureFactory)
	{
		super(application, model, form, inherited, figureFactory);
	}

	@Override
	public DragTracker getDragTracker(Request request)
	{
		return new DragEditPartsTracker(this)
		{
			@Override
			protected void performDirectEdit()
			{
				return; // Disabled direct edit via drag tracker, it activates direct edit on single click on selected element;
				// direct edit is handled in FormSelectionTool on double-click
			}

			@Override
			protected Command getCommand()
			{
				Command command = super.getCommand();
				return command == null ? null : new SelectModelsCommandWrapper(getViewer(), MobilePersistGraphicalEditPart.this, command);
			}
		};
	}
}
