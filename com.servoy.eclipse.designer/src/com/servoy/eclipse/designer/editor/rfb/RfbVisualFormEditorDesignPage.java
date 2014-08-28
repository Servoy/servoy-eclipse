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
import java.util.UUID;

import org.eclipse.core.runtime.Platform;
import org.eclipse.gef.ui.actions.DeleteAction;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.sablo.websocket.WebsocketSessionManager;

import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditorDesignPage;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.util.SelectionProviderAdapter;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.util.Utils;

/**
 * Design page for browser based rfb editor.
 *
 * @author rgansevles
 *
 */
public class RfbVisualFormEditorDesignPage extends BaseVisualFormEditorDesignPage
{
	// for setting selection when clicked in editor
	private final ISelectionProvider selectionProvider = new SelectionProviderAdapter()
	{
		@Override
		public ISelection getSelection()
		{
			// select form if nothing else is selected
			return selection == null ? new StructuredSelection(editorPart.getForm()) : selection;
		}
	};

	// for updating selection in editor when selection changes in IDE
	private final ISelectionListener selectionListener = new ISelectionListener()
	{
		@SuppressWarnings("unchecked")
		@Override
		public void selectionChanged(IWorkbenchPart part, ISelection selection)
		{
			final List<String> uuids = new ArrayList<String>();
			if (selection instanceof IStructuredSelection)
			{
				for (Object sel : Utils.iterate(((IStructuredSelection)selection).iterator()))
				{
					IPersist persist = (IPersist)Platform.getAdapterManager().getAdapter(sel, IPersist.class);
					if (persist != null)
					{
						uuids.add(persist.getUUID().toString());
					}
				}
				editorWebsocketSession.getEventDispatcher().addEvent(new Runnable()
				{
					@Override
					public void run()
					{
						editorWebsocketSession.getService(EditorWebsocketSession.EDITOR_SERVICE).executeAsyncServiceCall("updateSelection",
							new Object[] { uuids.toArray() });
					}
				});
			}
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
		WebsocketSessionManager.addSession(editorWebsocketSession = new EditorWebsocketSession(editorId));
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

//		String url = "http://localhost:8080/rfb/index.html?absolute_layout=true&editorid=" + editorId;
		Form form = editorPart.getForm();
		String layout = computeLayout(form);
		String url = "http://localhost:8080/rfb/angular/index.html?s=" + form.getSolution().getName() + "&l=" + layout + "&f=" + form.getName() + "&editorid=" +
			editorId;
		try
		{
			ServoyLog.logInfo("Browser url for editor: " + url);
			browser.setUrl(url);
			// install fake WebSocket in case browser does not support it
			SwtWebsocket.installFakeWebSocket(browser);
			// install console
			new BrowserFunction(browser, "consoleLog")
			{
				@Override
				public Object function(Object[] arguments)
				{
					if (arguments.length > 1)
					{
						if ("log".equals(arguments[0]))
						{
							ServoyLog.logInfo((String)arguments[1]);
						}
						else if ("error".equals(arguments[0]))
						{
							ServoyLog.logError((String)arguments[1], null);
						}
					}
					return null;
				}
			};
		}
		catch (Exception e)
		{
			ServoyLog.logError("couldn't load the editor: " + url, e);
		}
	}

	/**
	 * @param form
	 * @return
	 */
	private String computeLayout(Form form)
	{
		if (form.getLayoutContainers().hasNext()) return "flow";
		else return "absolute";
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException
	{
		super.init(site, input);
		site.setSelectionProvider(selectionProvider);
		getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(selectionListener);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.gef.ui.parts.GraphicalEditor#dispose()
	 */
	@Override
	public void dispose()
	{
		super.dispose();
		getSite().getWorkbenchWindow().getSelectionService().removeSelectionListener(selectionListener);
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