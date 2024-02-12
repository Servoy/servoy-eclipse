package com.servoy.eclipse.designer.webpackage;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import com.servoy.eclipse.core.resource.WebPackageManagerEditorInput;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.browser.BrowserFactory;
import com.servoy.eclipse.ui.browser.IBrowser;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;

public class WebPackageManager extends EditorPart
{
	private IBrowser browser;

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
		boolean darkTheme = UIUtils.isDarkThemeSelected(false);
		String url = "http://localhost:" + ApplicationServerRegistry.get().getWebServerPort() + "/wpm/index.html";
		if (getEditorInput() instanceof WebPackageManagerEditorInput)
		{
			String solutionName = ((WebPackageManagerEditorInput)getEditorInput()).getSolutionName();
			if (solutionName != null)
			{
				url += "?solution=" + solutionName;
				url += "&darkTheme=" + darkTheme;
			}
			else
			{
				url += "?darkTheme=" + darkTheme;
			}
		}
		else
		{
			url += "?darkTheme=" + darkTheme;
		}
		browser = BrowserFactory.createBrowser(parent);
		String finalUrl = url;
		Display.getDefault().asyncExec(() -> {
			try
			{
				browser.setUrl(finalUrl, null, new String[] { "Cache-Control: no-cache" });
			}
			catch (Exception ex)
			{
				ServoyLog.logError("couldn't load the package manager: " + finalUrl, ex);
			}
		});
	}

	@Override
	public void setFocus()
	{
		if (browser != null) browser.setFocus();
	}

}
