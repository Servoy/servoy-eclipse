/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

package com.servoy.eclipse.designer.editor.rfb;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;

import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.util.Utils;

/**
 * @author lvostinar
 *
 */
public class RfbSelectionListener implements ISelectionListener
{
	private EditorWebsocketSession editorWebsocketSession;
	private List<String> lastSelection = new ArrayList<String>();

	public RfbSelectionListener()
	{
	}

	@SuppressWarnings("unchecked")
	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection)
	{
		if (selection instanceof IStructuredSelection)
		{
			final List<String> uuids = getPersistUUIDS((IStructuredSelection)selection);
			if (uuids.size() > 0 && (uuids.size() != lastSelection.size() || !uuids.containsAll(lastSelection)))
			{
				lastSelection = uuids;
				editorWebsocketSession.getEventDispatcher().addEvent(new Runnable()
				{
					@Override
					public void run()
					{
						editorWebsocketSession.getClientService(EditorWebsocketSession.EDITOR_SERVICE).executeAsyncServiceCall("updateSelection",
							new Object[] { uuids.toArray() });
					}
				});
			}
		}
	}

	/**
	 * @param editorWebsocketSession the editorWebsocketSession to set
	 */
	public void setEditorWebsocketSession(EditorWebsocketSession editorWebsocketSession)
	{
		this.editorWebsocketSession = editorWebsocketSession;
	}

	/**
	 * @param lastSelection the lastSelection to set
	 */
	public void setLastSelection(IStructuredSelection selection)
	{
		this.lastSelection = getPersistUUIDS(selection);
	}

	/**
	 * @param selection
	 * @return
	 */
	private List<String> getPersistUUIDS(IStructuredSelection selection)
	{
		final List<String> uuids = new ArrayList<String>();
		for (Object sel : Utils.iterate(selection.iterator()))
		{
			IPersist persist = (IPersist)Platform.getAdapterManager().getAdapter(sel, IPersist.class);
			if (persist != null)
			{
				if (persist instanceof GhostBean)
				{
					GhostBean ghostBean = (GhostBean)persist;
					uuids.add(ghostBean.getUUIDString());
				}
				else uuids.add(persist.getUUID().toString());
			}
		}
		return uuids;
	}

}
