package com.servoy.eclipse.debug.script;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.dltk.javascript.typeinfo.ITypeInferenceHandler;
import org.eclipse.dltk.javascript.typeinfo.ITypeInferenceHandlerFactory;
import org.eclipse.dltk.javascript.typeinfo.ITypeInferencerVisitor;
import org.eclipse.dltk.javascript.typeinfo.ITypeInfoContext;

import com.servoy.eclipse.model.repository.SolutionSerializer;

public class CalculationsTypeInferenceHandlerFactory implements ITypeInferenceHandlerFactory
{
	public ITypeInferenceHandler create(ITypeInfoContext context, ITypeInferencerVisitor visitor)
	{
		IResource resource = context.getModelElement().getResource();
		if (resource != null)
		{
			IPath path = resource.getProjectRelativePath();
			if (path != null && path.segmentCount() == 3 && path.segment(0).equals(SolutionSerializer.DATASOURCES_DIR_NAME) &&
				path.segment(2).endsWith(SolutionSerializer.CALCULATIONS_POSTFIX_WITH_EXT))
			{
				return new CalculationsTypeInferenceHandler(visitor);
			}
		}
		return null;
	}

}
