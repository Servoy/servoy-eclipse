package com.servoy.eclipse.designer.editor.commands;

import org.eclipse.jface.viewers.ISelectionProvider;

import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;

public class MoveDownCommand extends MoveCommand
{
	public MoveDownCommand()
	{
		super(null, null, 1);
	}

	public MoveDownCommand(BaseVisualFormEditor editorPart, ISelectionProvider selectionProvider)
	{
		super(editorPart, selectionProvider, 1);
	}
}
