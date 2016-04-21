package com.servoy.eclipse.designer.webpackages;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.sablo.websocket.WebsocketSessionManager;

import com.servoy.eclipse.designer.editor.rfb.EditorWebsocketSession;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;

public class WebPackageManager extends EditorPart
{
	public static final String EDITOR_ID = "com.servoy.eclipse.ui.webpackagemanager";
	public static final String STARTPAGE_URL = "http://developer.servoy.com/developer/8xx_intro/startpage.html";
	private Browser browser;
	private EditorWebsocketSession pmWebsocketSession;

	@Override
	public void doSave(IProgressMonitor monitor)
	{
		//ignore
	}

	@Override
	public void doSaveAs()
	{
		//ignore
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException
	{
		setSite(site);
		setInput(input);
	}

	@Override
	public boolean isDirty()
	{
		return false;
	}

	@Override
	public boolean isSaveAsAllowed()
	{
		return false;
	}

	@Override
	public void createPartControl(Composite parent)
	{


		WebsocketSessionManager.addSession(pmWebsocketSession = new EditorWebsocketSession(this.getClass().getName()));
		pmWebsocketSession.registerServerService("packagemanager", new WebPackageManagerServiceHandler());
		browser = new Browser(parent, SWT.NONE);

		String url = "http://localhost:" + ApplicationServerRegistry.get().getWebServerPort() + "/webpackage/angular2/index.html";
		try
		{
			browser.setUrl(url, null, new String[] { "Cache-Control: no-cache" });
		}
		catch (Exception ex)
		{
			ServoyLog.logError("couldn't load the package manager: " + url, ex);
		}

	}

	@Override
	public void setFocus()
	{
		if (browser != null) browser.setFocus();
	}

}
