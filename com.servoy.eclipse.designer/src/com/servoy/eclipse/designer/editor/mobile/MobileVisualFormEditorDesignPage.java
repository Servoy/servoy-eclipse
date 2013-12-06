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
import org.eclipse.gef.ui.actions.DeleteAction;
import org.eclipse.gef.ui.palette.PaletteCustomizer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IWorkbenchPart;

import com.servoy.base.persistence.IMobileProperties;
import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditorGEFDesignPage;
import com.servoy.eclipse.designer.editor.IPaletteFactory;
import com.servoy.eclipse.designer.editor.mobile.editparts.MobileFormGraphicalEditPart;
import com.servoy.eclipse.designer.editor.mobile.editparts.MobileFormGraphicalRootEditPart;
import com.servoy.eclipse.designer.editor.mobile.palette.MobilePaletteItemTransferDropTargetListener;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.ui.property.MobileListModel;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.Portal;

/**
 * Design page for mobile form editor.
 * 
 * @author rgansevles
 *
 */
public class MobileVisualFormEditorDesignPage extends BaseVisualFormEditorGEFDesignPage
{
	/**
	 * @param editorPart
	 */
	public MobileVisualFormEditorDesignPage(BaseVisualFormEditor editorPart)
	{
		super(editorPart);
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
		return null;
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
	protected void initializeGraphicalViewer()
	{
		super.initializeGraphicalViewer();

		GraphicalViewer viewer = getGraphicalViewer();

		if (getEditorPart().getForm() != null)
		{
			viewer.addDropTargetListener(new MobilePaletteItemTransferDropTargetListener(viewer, getEditorPart()));
		}

		// configure the context menu provider
		MobileVisualFormEditorContextMenuProvider cmProvider = new MobileVisualFormEditorContextMenuProvider("#MobileFormDesignerContext", getActionRegistry());
		viewer.setContextMenu(cmProvider);
		getSite().registerContextMenu(cmProvider.getId(), cmProvider, viewer);

		//  refreshToolBars();
	}

	@Override
	public boolean showPersist(IPersist persist)
	{
		Object element = null;
		if (persist instanceof IFormElement)
		{
			IFormElement formElement = (IFormElement)persist;
			if (formElement.getGroupID() != null)
			{
				Form form = (Form)formElement.getAncestor(IRepository.FORMS);
				element = new FormElementGroup(((IFormElement)persist).getGroupID(), ModelUtils.getEditingFlattenedSolution(form), form);
			}
		}
		if (element == null && persist instanceof AbstractBase)
		{
			AbstractBase ab = (AbstractBase)persist;
			if (ab.getCustomMobileProperty(IMobileProperties.LIST_ITEM_HEADER.propertyName) != null ||
				ab.getCustomMobileProperty(IMobileProperties.LIST_ITEM_BUTTON.propertyName) != null ||
				ab.getCustomMobileProperty(IMobileProperties.LIST_ITEM_SUBTEXT.propertyName) != null ||
				ab.getCustomMobileProperty(IMobileProperties.LIST_ITEM_COUNT.propertyName) != null ||
				ab.getCustomMobileProperty(IMobileProperties.LIST_ITEM_IMAGE.propertyName) != null ||
				ab.getCustomMobileProperty(IMobileProperties.LIST_COMPONENT.propertyName) != null)
			{
				Portal portal = (Portal)ab.getAncestor(IRepository.PORTALS);
				if (portal != null && portal.isMobileInsetList()) element = MobileListModel.create((Form)portal.getAncestor(IRepository.FORMS), portal);
			}
		}
		if (element == null) element = persist;

		Object editPart = getGraphicalViewer().getRootEditPart().getViewer().getEditPartRegistry().get(element);
		if (editPart instanceof EditPart)
		{
			// select the marked element
			getGraphicalViewer().setSelection(new StructuredSelection(editPart));
			getGraphicalViewer().reveal((EditPart)editPart);
			return true;
		}
		return false;
	}

	@Override
	protected DeleteAction createDeleteAction()
	{
		return new DeleteAction((IWorkbenchPart)editorPart);
	}
}