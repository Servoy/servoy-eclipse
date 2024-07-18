package com.servoy.eclipse.aibridge.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.servoy.eclipse.aibridge.AiBridgeView;
import com.servoy.eclipse.aibridge.dto.Completion;
import com.servoy.eclipse.aibridge.editors.DualEditorInput;
import com.servoy.eclipse.model.util.ServoyLog;

public class OpenDualEditorAction extends Action implements ISelectionListener
{

	private Completion completion;

	public OpenDualEditorAction()
	{
		super("Open in Dual View", SWT.PUSH);
		setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin("com.servoy.eclipse.aibridge", "icons/vsplit.png"));
	}

	@Override
	public void run()
	{
		Display display = Display.getCurrent();
		if (display != null)
		{
			display.asyncExec(() -> display.timerExec(300, () -> setChecked(false)));
		}
		if (completion == null) return;
		try
		{
			IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			if (window == null) return;

			IWorkbenchPage page = window.getActivePage();

			for (IEditorReference editorRef : page.getEditorReferences())
			{
				if (editorRef.getEditorInput() instanceof DualEditorInput dualEditorInput)
				{
					if (dualEditorInput.getId().equals(completion.getId()))
					{
						page.activate(editorRef.getPart(false));
						return;
					}
				}
			}
			DualEditorInput editorInput = new DualEditorInput(completion);
			page.openEditor(editorInput, "com.servoy.eclipse.aibridge.dualeditor");
		}
		catch (PartInitException e)
		{
			ServoyLog.logError(e);
		}
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection)
	{
		if (part instanceof AiBridgeView)
		{
			this.completion = null;
			if (!selection.isEmpty() && selection instanceof IStructuredSelection structuredSelection)
			{
				this.completion = (Completion)structuredSelection.getFirstElement();

			}
			setEnabled(completion != null ? true : false);
		}
	}
}