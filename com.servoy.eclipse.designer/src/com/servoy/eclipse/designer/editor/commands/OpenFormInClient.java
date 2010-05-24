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
package com.servoy.eclipse.designer.editor.commands;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;

import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;

public class OpenFormInClient extends AbstractHandler implements IHandler
{
	public Object execute(ExecutionEvent arg0) throws ExecutionException
	{
		IEditorPart activeEditor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		IPersist persist = (IPersist)activeEditor.getAdapter(IPersist.class);
		if (persist instanceof Form)
		{
			com.servoy.eclipse.core.Activator.getDefault().showInDebugClients((Form)persist);
		}
		return null;
	}
}
