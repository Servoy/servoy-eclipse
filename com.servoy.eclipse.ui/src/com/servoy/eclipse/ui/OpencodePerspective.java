package com.servoy.eclipse.ui;

import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.editors.BrowserEditor;
import com.servoy.eclipse.ui.editors.BrowserEditorInput;

public class OpencodePerspective implements IPerspectiveFactory
{
	public static final String PERSPECTIVE_ID = "com.servoy.eclipse.ui.OpencodePerspective";
	private static final String DEFAULT_URL = "http://127.0.0.1:4096/";
	private static final String URL_PROPERTY = "opencode.url";

	@Override
	public void createInitialLayout(IPageLayout layout)
	{
		layout.setEditorAreaVisible(true);
		layout.setFixed(true);

		PlatformUI.getWorkbench().getDisplay().asyncExec(() -> {
			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			if (page != null)
			{
				try
				{
					String url = System.getProperty(URL_PROPERTY, DEFAULT_URL);
					page.openEditor(new BrowserEditorInput(url, "Opencode"), BrowserEditor.EDITOR_ID);
				}
				catch (PartInitException e)
				{
					ServoyLog.logError(e);
				}
			}
		});
	}
}
