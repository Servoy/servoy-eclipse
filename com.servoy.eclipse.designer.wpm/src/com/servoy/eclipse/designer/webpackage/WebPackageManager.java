package com.servoy.eclipse.designer.webpackage;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.chromium.Browser;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import com.servoy.eclipse.core.resource.WebPackageManagerEditorInput;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.preferences.DesignerPreferences;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;

public class WebPackageManager extends EditorPart
{
	private Browser browser;
	private org.eclipse.swt.browser.Browser internalBrowser;

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

		if (new DesignerPreferences().useChromiumBrowser())
		{
			browser = new Browser(parent, SWT.NONE);
		}
		else
		{
			internalBrowser = new org.eclipse.swt.browser.Browser(parent, SWT.NONE);
		}

		String url = "http://localhost:" + ApplicationServerRegistry.get().getWebServerPort() + "/wpm/index.html";
		if (getEditorInput() instanceof WebPackageManagerEditorInput)
		{
			String solutionName = ((WebPackageManagerEditorInput)getEditorInput()).getSolutionName();
			if (solutionName != null)
			{
				url += "?solution=" + solutionName;
			}
		}
		try
		{
			if (browser != null)
			{
				browser.setUrl(url, null, new String[] { "Cache-Control: no-cache" });
			}
			else
			{
				internalBrowser.setUrl(url, null, new String[] { "Cache-Control: no-cache" });
			}
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
		if (internalBrowser != null) internalBrowser.setFocus();
	}

}
