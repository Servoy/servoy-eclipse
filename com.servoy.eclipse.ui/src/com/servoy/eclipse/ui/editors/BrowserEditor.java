package com.servoy.eclipse.ui.editors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import com.servoy.eclipse.ui.browser.BrowserFactory;
import com.servoy.eclipse.ui.browser.IBrowser;

public class BrowserEditor extends EditorPart
{
	public static final String EDITOR_ID = "com.servoy.eclipse.ui.editors.BrowserEditor";

	private IBrowser browser;

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException
	{
		if (!(input instanceof BrowserEditorInput))
		{
			throw new PartInitException("Invalid input: must be BrowserEditorInput");
		}
		setSite(site);
		setInput(input);
		setPartName(input.getName());
	}

	@Override
	public void createPartControl(Composite parent)
	{
		browser = BrowserFactory.createBrowser(parent);
		BrowserEditorInput input = (BrowserEditorInput)getEditorInput();
		browser.setUrl(input.getUrl());
	}

	public void setUrl(String url)
	{
		if (browser != null && !browser.isDisposed())
		{
			browser.setUrl(url);
		}
	}

	public IBrowser getBrowser()
	{
		return browser;
	}

	@Override
	public void setFocus()
	{
		if (browser != null) browser.setFocus();
	}

	@Override
	public void doSave(IProgressMonitor monitor)
	{
	}

	@Override
	public void doSaveAs()
	{
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
	public void dispose()
	{
		if (browser != null && !browser.isDisposed())
		{
			browser.dispose();
		}
		super.dispose();
	}
}
