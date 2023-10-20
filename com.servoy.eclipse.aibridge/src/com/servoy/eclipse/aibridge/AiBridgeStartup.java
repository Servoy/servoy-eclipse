package com.servoy.eclipse.aibridge;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.aibridge.editors.DualEditor;

public class AiBridgeStartup implements IStartup
{

	public static String EMPTY_TAB_ID = "org.eclipse.ui.internal.emptyEditorTab";

	@Override
	public void earlyStartup()
	{
		Display.getDefault().asyncExec(() -> closeDualEditors());
		PlatformUI.getWorkbench().addWorkbenchListener(new AiBridgeShutdownListener());
	}

	private void closeDualEditors()
	{
		IWorkbench workbench = PlatformUI.getWorkbench();
		IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
		if (window != null)
		{
			IWorkbenchPage page = window.getActivePage();
			if (page != null)
			{
				IEditorReference[] editorReferences = page.getEditorReferences();
				for (IEditorReference reference : editorReferences)
				{
					if (DualEditor.ID.equals(reference.getId()) || EMPTY_TAB_ID.equals(reference.getId()))
					{
						page.closeEditor(reference.getEditor(false), false);
					}
				}
			}
		}
	}
}