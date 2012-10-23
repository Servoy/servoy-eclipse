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

import java.util.ArrayList;
import java.util.List;

import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditorDesignPage;
import com.servoy.eclipse.designer.editor.mobile.editparts.MobileFormGraphicalEditPart;
import com.servoy.eclipse.designer.editor.mobile.editparts.MobileListModel;
import com.servoy.j2db.persistence.IPersist;

/**
 * Mobile form editor.
 * 
 * @author rgansevles
 *
 */
public class MobileVisualFormEditor extends BaseVisualFormEditor
{
	public static final RequestType REQ_PLACE_BUTTON = new RequestType(RequestType.TYPE_BUTTON);
	public static final RequestType REQ_PLACE_HEADER_TITLE = new RequestType(RequestType.TYPE_LABEL);
	public static final RequestType REQ_PLACE_HEADER = new RequestType(RequestType.TYPE_PART);
	public static final RequestType REQ_PLACE_FOOTER = new RequestType(RequestType.TYPE_PART);
	public static final RequestType REQ_PLACE_FIELD = new RequestType(RequestType.TYPE_FIELD);
	public static final RequestType REQ_PLACE_INSET_LIST = new RequestType(RequestType.TYPE_TAB);
	public static final RequestType REQ_PLACE_FORM_LIST = new RequestType(RequestType.TYPE_TAB);
	public static final RequestType REQ_PLACE_TOGGLE = new RequestType(RequestType.TYPE_FIELD);

	@Override
	protected BaseVisualFormEditorDesignPage createGraphicaleditor()
	{
		return new MobileVisualFormEditorDesignPage(this);
	}

	@Override
	protected IPersist[] getNodesToSave()
	{
		List<IPersist> nodesToSave = new ArrayList<IPersist>();
		nodesToSave.add(getForm());

		// look for inset-tabs, save those forms as well
		MobileFormGraphicalEditPart contents = (MobileFormGraphicalEditPart)getGraphicaleditor().getGraphicalViewer().getContents();
		for (Object model : contents.getModelChildren())
		{
			if (model instanceof MobileListModel)
			{
				nodesToSave.add(((MobileListModel)model).containedForm);
			}
		}

		return nodesToSave.toArray(new IPersist[nodesToSave.size()]);
	}
}
