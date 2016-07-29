package com.servoy.eclipse.designer.editor.commands;

import java.util.ArrayList;

import org.eclipse.jface.viewers.ISelectionProvider;

import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.designer.editor.rfb.actions.handlers.ChangeParentCommand;
import com.servoy.j2db.persistence.IPersist;

public class MoveDownCommand extends MoveCommand
{
	public MoveDownCommand()
	{
		super(null, null);
	}

	public MoveDownCommand(BaseVisualFormEditor editorPart, ISelectionProvider selectionProvider)
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
					if (selectedPersistIdx + 1 < sortedChildren.size())
					{
						editorPart.getCommandStack().execute(
							new ChangeParentCommand(singleSelection, null, sortedChildren.get(selectedPersistIdx + 1), editorPart.getForm(), true));
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
				return selectedPersistIdx < sortedChildren.size() - 1;
			}
		}
		return false;
	}

}
