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

import java.util.List;
import java.util.UUID;

import org.eclipse.gef.ui.actions.DeleteAction;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.sablo.websocket.WebsocketSessionManager;

import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditorDesignPage;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.util.SelectionProviderAdapter;
import com.servoy.j2db.persistence.IPersist;

/**
 * Design page for browser based rfb editor.
 * 
 * @author rgansevles
 *
 */
public class RfbVisualFormEditorDesignPage extends BaseVisualFormEditorDesignPage
{
	private final ISelectionProvider selectionProvider = new SelectionProviderAdapter()
	{
		@Override
		public ISelection getSelection()
		{
			// select form if nothing else is selected
			return selection == null ? new StructuredSelection(editorPart.getForm()) : selection;
		}
	};

	private Browser browser;

	private EditorWebsocketSession editorWebsocketSession;

	public RfbVisualFormEditorDesignPage(BaseVisualFormEditor editorPart)
	{
		super(editorPart);
	}

	@Override
	public void createPartControl(Composite parent)
	{
		// Serve requests for rfb editor
		String editorId = UUID.randomUUID().toString();
		WebsocketSessionManager.addSession(EditorWebsocketSession.EDITOR_ENDPOINT, editorWebsocketSession = new EditorWebsocketSession(editorId));
		editorWebsocketSession.registerServerService("formeditor", new EditorServiceHandler(editorPart, selectionProvider));

		try
		{
			browser = new Browser(parent, SWT.NONE);
		}
		catch (SWTError e)
		{
			ServoyLog.logError(e);
			return;
		}

		String url = "http://localhost:8080/rfb/index.html?absolute_layout=true&editorid=" + editorId;
		try
		{
			// System.err.println("Browser url: " + url);
			browser.setUrl(url);
		}
		catch (Exception e)
		{
			ServoyLog.logError("couldn't load the editor: " + url, e);
		}

		// install fake WebSocket in case browser does not support it
		SwtWebsocket.installFakeWebSocket(browser);

		selectionProvider.addSelectionChangedListener(new ISelectionChangedListener()
		{
			@Override
			public void selectionChanged(SelectionChangedEvent event)
			{
				// TODO: set selection in browser
			}
		});
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException
	{
		super.init(site, input);
		site.setSelectionProvider(selectionProvider);
	}

	@Override
	public void refreshAllParts()
	{
		//sendMessage("refreshForm");
	}

	@Override
	public void refreshPersists(List<IPersist> persists)
	{
	}

	@Override
	public boolean showPersist(IPersist persist)
	{
		return false; // selectNode(persist);
	}

	@Override
	public void setFocus()
	{
	}

	@Override
	protected DeleteAction createDeleteAction()
	{
		return null;
	}

	@Override
	protected IAction createCopyAction()
	{
		return null;
	}

	@Override
	protected IAction createCutAction()
	{
		return null;
	}

	@Override
	protected IAction createPasteAction()
	{
		return null;
	}
}