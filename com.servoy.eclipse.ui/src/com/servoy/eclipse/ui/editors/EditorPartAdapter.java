package com.servoy.eclipse.ui.editors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;

import com.servoy.eclipse.model.repository.SolutionDeserializer;
import com.servoy.j2db.persistence.IPersist;

public class EditorPartAdapter implements IAdapterFactory
{

	public Object getAdapter(Object adaptableObject, Class adapterType)
	{
		if (adaptableObject instanceof IEditorPart && adapterType.equals(IPersist.class))
		{
			IEditorPart ep = (IEditorPart)adaptableObject;
			IEditorInput edInput = ep.getEditorInput();
			if (edInput != null)
			{
				IFile file = (IFile)edInput.getAdapter(IFile.class);
				if (file != null)
				{
					return SolutionDeserializer.findPersistFromFile(file);
				}
			}
		}
		return null;
	}

	public Class[] getAdapterList()
	{
		return new Class[] { IPersist.class };
	}

}
