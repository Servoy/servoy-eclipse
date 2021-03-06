/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2019 Servoy BV

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
import org.eclipse.gef.commands.Command;
import org.eclipse.jface.viewers.ISelectionProvider;

import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.designer.editor.rfb.actions.PasteAction;
import com.servoy.j2db.persistence.IPersist;

/**
 * @author costinchiulan
 *
 */
public class PasteCommand extends ContentOutlineCommand
{
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException

	{

		final List<IPersist> selection = getSelection();
		if (selection.size() > 0)
		{

			final BaseVisualFormEditor editorPart = getEditorPart();
			final ISelectionProvider sp = editorPart.getEditorSite().getSelectionProvider();

			final PasteAction pa = new PasteAction(Activator.getDefault().getDesignClient(), sp, editorPart, null);
			final Command pasteC = pa.createPasteCommand(selection);
			if (editorPart != null) editorPart.getCommandStack().execute(pasteC);
		}

		return null;
	}
}
