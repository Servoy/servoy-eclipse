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

package com.servoy.eclipse.designer.editor.mobile;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.RootEditPart;
import org.eclipse.gef.ui.palette.PaletteCustomizer;

import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditorDesignPage;
import com.servoy.eclipse.designer.editor.IPaletteFactory;
import com.servoy.eclipse.designer.editor.mobile.editparts.MobileFormGraphicalEditPart;
import com.servoy.eclipse.designer.editor.mobile.editparts.MobileFormGraphicalRootEditPart;
import com.servoy.eclipse.designer.editor.palette.PaletteItemTransferDropTargetListener;

/**
 * Design page for mobile form editor.
 * 
 * @author rgansevles
 *
 */
public class MobileVisualFormEditorDesignPage extends BaseVisualFormEditorDesignPage
{
	/**
	 * @param editorPart
	 */
	public MobileVisualFormEditorDesignPage(BaseVisualFormEditor editorPart)
	{
		super(editorPart);
	}

	/**
	 * @see AbstractGraphicalEditor#createGraphicalViewerContents()
	 */
	@Override
	protected EditPart createGraphicalViewerContents()
	{
		return new MobileFormGraphicalEditPart(Activator.getDefault().getDesignClient(), getEditorPart());
	}

	@Override
	protected RootEditPart createRootEditPart()
	{
		return new MobileFormGraphicalRootEditPart(getEditorPart());
	}

	@Override
	protected IPaletteFactory createPaletteFactory()
	{
		return new MobileVisualFormEditorPaletteFactory();
	}

	@Override
	protected PaletteCustomizer createPaletteCustomizer()
	{
		// TODO
		return null;
	}

	@Override
	protected void fillToolbar()
	{
		// TODO
	}

	@Override
	protected void initializeGraphicalViewer()
	{
		super.initializeGraphicalViewer();

		GraphicalViewer viewer = getGraphicalViewer();

		if (getEditorPart().getForm() != null)
		{
			viewer.addDropTargetListener(new PaletteItemTransferDropTargetListener(getGraphicalViewer(), getEditorPart()));
		}

//		// configure the context menu provider
//		String id = "#FormDesignerContext";
//		VisualFormEditorContextMenuProvider cmProvider = new VisualFormEditorContextMenuProvider(id, viewer, getActionRegistry());
//		viewer.setContextMenu(cmProvider);
//		getSite().registerContextMenu(id, cmProvider, viewer);

		//  refreshToolBars();
	}


}
