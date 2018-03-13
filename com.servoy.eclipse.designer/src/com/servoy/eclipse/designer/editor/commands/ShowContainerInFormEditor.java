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

package com.servoy.eclipse.designer.editor.commands;

import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.designer.editor.rfb.RfbVisualFormEditorDesignPage;
import com.servoy.j2db.persistence.AbstractContainer;
import com.servoy.j2db.persistence.IPersist;

/**
 * @author lvostinar
 *
 */
public class ShowContainerInFormEditor extends ContentOutlineCommand
{
	public ShowContainerInFormEditor()
	{

	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException
	{
		List<IPersist> selection = getSelection();
		BaseVisualFormEditor editor = getEditorPart();
		if (editor != null && selection.size() == 1 && selection.get(0) instanceof AbstractContainer)
		{
			((RfbVisualFormEditorDesignPage)editor.getGraphicaleditor()).showContainer((AbstractContainer)selection.get(0));
		}
		else if (editor != null)
		{
			((RfbVisualFormEditorDesignPage)editor.getGraphicaleditor()).showContainer(null);
		}
		return null;
	}


}
