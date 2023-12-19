package com.servoy.eclipse.aibridge.actions;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
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

		List<UUID> selectedIds = Stream.of(selection.toArray())
			.filter(obj -> obj instanceof Completion)
			.map(obj -> ((Completion)obj).getId())
			.collect(Collectors.toList());

		selectedIds.forEach(id -> AiBridgeManager.getInstance().getRequestMap().remove(id));
		AiBridgeManager.getInstance().deleteFiles(AiBridgeView.getSolutionName(), selectedIds);

		UUID newSelectedUUID = findPostDeletionSelectionId(AiBridgeView.getAllItemUUIDs(), selectedIds);
		AiBridgeView.setSelectionId(newSelectedUUID);
		AiBridgeView.refresh(); // Delete columns in view
	}

	private UUID findPostDeletionSelectionId(List<UUID> allUuids, List<UUID> deletedUuids)
	{
		UUID priorDeletedId = null;
		boolean foundDeleted = false;
		UUID afterDeleteId = null;

		for (UUID uuid : allUuids)
		{
			if (deletedUuids.contains(uuid))
			{
				foundDeleted = true;
			}
			else
			{
				if (foundDeleted)
				{
					afterDeleteId = uuid;
					break; //found an id following the first selected id for deletion
				}
				else
				{
					priorDeletedId = uuid;
				}
			}
		}

		return afterDeleteId != null ? afterDeleteId : priorDeletedId;
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