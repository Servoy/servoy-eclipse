package com.servoy.eclipse.designer.editor.commands;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.model.util.WebFormComponentChildType;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IPersist;

public class DeleteCommand extends ContentOutlineCommand
{

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException
	{
		List<Object> selection = getMixedSelection();
		if (!selection.isEmpty())
		{
			BaseVisualFormEditor editorPart = getEditorPart();
			if (editorPart != null)
			{
				List<IPersist> persists = new ArrayList<>();
				List<FormElementGroup> groups = new ArrayList<>();

				for (Object next : selection)
				{
					if (next instanceof PersistContext pc)
					{
						if (pc.getPersist() != null && !(pc.getPersist() instanceof WebFormComponentChildType))
						{
							persists.add(pc.getPersist());
						}
					}
					else if (next instanceof IPersist p)
					{
						if (!(p instanceof WebFormComponentChildType))
						{
							persists.add(p);
						}
					}
					else if (next instanceof FormElementGroup g)
					{
						groups.add(g);
					}
				}

				if (!persists.isEmpty())
				{
					editorPart.getCommandStack().execute(new FormElementDeleteCommand(
						persists.toArray(new IPersist[0])));
				}
				if (!groups.isEmpty())
				{
					editorPart.getCommandStack().execute(new FormElementGroupDeleteCommand(
						groups, editorPart));
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
		List<Object> selection = getMixedSelection();
		return selection.stream().anyMatch(sel -> {
			if (sel instanceof FormElementGroup) return true;
			if (sel instanceof PersistContext pc) return pc.getPersist() != null;
			if (sel instanceof IPersist p) return true;
			return false;
		});
	}

}
