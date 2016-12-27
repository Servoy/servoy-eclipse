package com.servoy.eclipse.designer.editor.commands;

import org.eclipse.jface.viewers.ISelectionProvider;

import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;

public class MoveUpCommand extends MoveCommand
{

	public MoveUpCommand()
	{
		super(null, null, -1);
	}

	public MoveUpCommand(BaseVisualFormEditor editorPart, ISelectionProvider selectionProvider)
	{
		super(editorPart, selectionProvider, -1);
	}
}
