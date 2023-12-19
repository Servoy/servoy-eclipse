package com.servoy.eclipse.aibridge.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.servoy.eclipse.aibridge.AiBridgeView;
import com.servoy.eclipse.aibridge.dto.Completion;
import com.servoy.eclipse.aibridge.editors.DualEditorInput;
import com.servoy.eclipse.ui.tweaks.IconPreferences;

public class ShowResponseAction extends Action implements ISelectionListener
{
	private final Browser htmlViewer;

	private Completion completion;
	private final Composite parent;

	public ShowResponseAction(Composite parent)
	{
		super("Split horizontal view", SWT.TOGGLE);
		boolean isDarkTheme = IconPreferences.getInstance().getUseDarkThemeIcons();
		String iconPath = isDarkTheme ? "darkicons/hsplit.png" : "icons/hsplit.png";
		ImageDescriptor splitViewImageDescriptor = AbstractUIPlugin.imageDescriptorFromPlugin("com.servoy.eclipse.aibridge", iconPath);
		setImageDescriptor(splitViewImageDescriptor);
		htmlViewer = new Browser(parent, SWT.NONE);
		htmlViewer.setVisible(false);
		this.parent = parent;
		setEnabled(true);
		htmlViewer.setVisible(true);
		setChecked(true);
	}

	@Override
	public void run()
	{
		if (htmlViewer == null) return;
		refresh();

	}

	public void refresh()
	{
		htmlViewer.setVisible(isChecked());
		if (isChecked())
		{
			String viewerContent = completion != null && completion.getResponse() != null
				? completion.getFullCompletion().getResponse().getResponseMessage()
				: DualEditorInput.getNoContentHtml();
			htmlViewer.setText(viewerContent);
		}
		parent.layout();
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

//			if (completion == null)
//			{
//				setChecked(false);
//			}
			refresh();
//			setEnabled(completion != null ? true : false);
		}
	}
}
