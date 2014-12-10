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

import java.awt.Dimension;
import java.util.List;
import java.util.UUID;

import org.eclipse.gef.ui.actions.SelectAllAction;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.websocket.WebsocketSessionManager;

import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.core.elements.IFieldPositioner;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditorDesignPage;
import com.servoy.eclipse.designer.editor.rfb.actions.CopyAction;
import com.servoy.eclipse.designer.editor.rfb.actions.CutAction;
import com.servoy.eclipse.designer.editor.rfb.actions.DeleteAction;
import com.servoy.eclipse.designer.editor.rfb.actions.FixedSelectAllAction;
import com.servoy.eclipse.designer.editor.rfb.actions.PasteAction;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.util.DefaultFieldPositioner;
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

	private final IFieldPositioner fieldPositioner = new DefaultFieldPositioner(new Point(40, 50))
	{

		private boolean locationNeverSet = true;

		@Override
		public void setDefaultLocation(Point defaultLocation)
		{
			super.setDefaultLocation(defaultLocation);
			locationNeverSet = false;
		}

		@Override
		public Point getNextLocation(Point location)
		{
			if (location == null)
			{
				if (locationNeverSet)
				{
					defaultLocation.x += 20;
					defaultLocation.y += 20;
				}
				return defaultLocation;
			}
			return location;
		};
	};

	// for updating selection in editor when selection changes in IDE
	private final RfbSelectionListener selectionListener = new RfbSelectionListener();

	// for reloading palette when components change
	private final RfbWebResourceListener resourceChangedListener = new RfbWebResourceListener();

	private Browser browser;

	private EditorWebsocketSession editorWebsocketSession;

	private String layout = null;
	private String editorId = null;

	public RfbVisualFormEditorDesignPage(BaseVisualFormEditor editorPart)
	{
		super(editorPart);
	}

	@Override
	public void createPartControl(Composite parent)
	{
		// always reload the current spec so that always the latest stuff is shown.
		WebComponentSpecProvider.reload();
		// Serve requests for rfb editor
		editorId = UUID.randomUUID().toString();
		WebsocketSessionManager.addSession(editorWebsocketSession = new EditorWebsocketSession(editorId));
		editorWebsocketSession.registerServerService("formeditor", new EditorServiceHandler(editorPart, selectionProvider, selectionListener, fieldPositioner));
		selectionListener.setEditorWebsocketSession(editorWebsocketSession);
		resourceChangedListener.setEditorWebsocketSession(editorWebsocketSession);
		try
		{
			browser = new Browser(parent, SWT.NONE);
		}
		catch (SWTError e)
		{
			ServoyLog.logError(e);
			return;
		}

		refreshBrowserUrl();
		try
		{
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
			ServoyLog.logError("couldn't load the editor: ", e);
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

	public void refreshBrowserUrl()
	{
		Form form = editorPart.getForm();
		Form flattenedForm = ModelUtils.getEditingFlattenedSolution(form).getFlattenedForm(form);
		String newLayout = computeLayout(flattenedForm);
		if (!Utils.equalObjects(layout, newLayout))
		{
			layout = newLayout;
			Dimension formSize = flattenedForm.getSize();
			String url = "http://localhost:8080/rfb/angular/index.html?s=" + form.getSolution().getName() + "&l=" + layout + "&f=" + form.getName() + "&w=" +
				formSize.getWidth() + "&h=" + formSize.getHeight() + "&editorid=" + editorId;
			try
			{
				ServoyLog.logInfo("Browser url for editor: " + url);
				browser.setUrl(url + "&replacewebsocket=true");
			}
			catch (Exception ex)
			{
				ServoyLog.logError("couldn't load the editor: " + url, ex);
			}
		}
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException
	{
		super.init(site, input);
		site.setSelectionProvider(selectionProvider);
		getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(selectionListener);
	}

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
		selectionProvider.setSelection(new StructuredSelection(persist));
		return true;
	}

	@Override
	public void setFocus()
	{
	}

	@Override
	protected DeleteAction createDeleteAction()
	{
		return new DeleteAction(editorPart);
	}

	@Override
	protected IAction createCopyAction()
	{
		return new CopyAction(editorPart);
	}

	@Override
	protected IAction createCutAction()
	{
		return new CutAction(editorPart);
	}

	@Override
	protected IAction createPasteAction()
	{
		return new PasteAction(Activator.getDefault().getDesignClient(), selectionProvider, editorPart, fieldPositioner);
	}

	@Override
	protected SelectAllAction createSelectAllAction()
	{
		return new FixedSelectAllAction(editorPart, selectionProvider);
	}

}