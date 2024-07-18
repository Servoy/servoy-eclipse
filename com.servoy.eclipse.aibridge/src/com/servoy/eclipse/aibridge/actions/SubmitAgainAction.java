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

public class SubmitAgainAction extends Action implements ISelectionListener
{

	private Completion completion;

	public SubmitAgainAction()
	{
		super("Submit again");
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

		AiBridgeManager aiBridgeManager = AiBridgeManager.getInstance();
		String solutionName = AiBridgeView.getSolutionName();

		// Use Stream.of(selection.toArray()) to create a stream directly from the array
		Stream.of(selection.toArray())
			.filter(obj -> obj instanceof Completion)
			.map(obj -> (Completion)obj)
			.forEach(obj -> {
				aiBridgeManager.sendCompletion(obj);
			});

		AiBridgeView.refresh();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.ui.ISelectionListener#selectionChanged(org.eclipse.ui.IWorkbenchPart, org.eclipse.jface.viewers.ISelection)
	 */
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