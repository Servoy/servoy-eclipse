package com.servoy.eclipse.designer.editor.commands;

import java.util.ArrayList;

import org.eclipse.jface.viewers.ISelectionProvider;

import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.designer.editor.rfb.actions.handlers.ChangeParentCommand;
import com.servoy.j2db.persistence.IPersist;

public class MoveUpCommand extends MoveCommand
{

	public MoveUpCommand()
	{
		super(null, null);
	}

	public MoveUpCommand(BaseVisualFormEditor editorPart, ISelectionProvider selectionProvider)
	{
		super(editorPart, selectionProvider);
	}

	@Override
	public Object execute()
	{
		IPersist singleSelection = getSingleSelection();
		if (singleSelection != null)
		{
			ArrayList<IPersist> sortedChildren = getSortedChildren(singleSelection.getParent());
			if (sortedChildren != null)
			{
				BaseVisualFormEditor editorPart = getEditorPart();
				if (editorPart != null)
				{
					int selectedPersistIdx = sortedChildren.indexOf(singleSelection);
					if (selectedPersistIdx > 0)
					{
						editorPart.getCommandStack().execute(
							new ChangeParentCommand(singleSelection, singleSelection.getParent(), sortedChildren.get(selectedPersistIdx - 1), false));
					}
				}
			}
		}
		return null;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.core.commands.AbstractHandler#isEnabled()
	 */
	@Override
	public boolean isEnabled()
	{
		IPersist singleSelection = getSingleSelection();
		if (singleSelection != null)
		{
			ArrayList<IPersist> sortedChildren = getSortedChildren(singleSelection.getParent());
			if (sortedChildren != null)
			{
				int selectedPersistIdx = sortedChildren.indexOf(singleSelection);
				return selectedPersistIdx > 0;
			}
		}
		return false;
	}

}
