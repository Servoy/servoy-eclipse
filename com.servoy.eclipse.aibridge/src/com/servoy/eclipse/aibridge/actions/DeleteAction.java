package com.servoy.eclipse.aibridge.actions;

import java.util.stream.Stream;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;

import com.servoy.eclipse.aibridge.AiBridgeManager;
import com.servoy.eclipse.aibridge.AiBridgeView;
import com.servoy.eclipse.aibridge.dto.Completion;

public class DeleteAction extends Action implements ISelectionListener
{
	private Completion completion;

	public DeleteAction()
	{
		super("Delete");
		setEnabled(true);
	}

	@Override
	public void run()
	{
		IStructuredSelection selection = AiBridgeView.getSelection();
		if (selection.isEmpty())
		{
			return;
		}

		Stream.of(selection.toArray())
			.filter(obj -> obj instanceof Completion)
			.map(obj -> (Completion)obj)
			.forEach(completion -> AiBridgeManager.getRequestMap().remove(completion.getId()));

		AiBridgeManager.saveData(AiBridgeView.getSolutionName());

		AiBridgeView.refresh();
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