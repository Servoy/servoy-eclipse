package com.servoy.eclipse.designer.editor.commands;

import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.designer.util.DesignerUtil;
import com.servoy.j2db.persistence.IPersist;

public class DeleteCommand extends ContentOutlineCommand
{

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException
	{
		List<IPersist> selection = getSelection();
		if (selection.size() > 0)
		{
			BaseVisualFormEditor editorPart = getEditorPart();
			if (editorPart != null) editorPart.getCommandStack().execute(new FormElementDeleteCommand(selection.toArray(new IPersist[selection.size()])));
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
		return getSelection().size() > 0 && !DesignerUtil.containsInheritedElement(getSelectionList());
	}

}
