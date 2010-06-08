package com.servoy.eclipse.debug.script;

import org.eclipse.dltk.javascript.typeinfo.model.Element;
import org.eclipse.dltk.javascript.ui.typeinfo.IElementLabelProvider;
import org.eclipse.jface.resource.ImageDescriptor;

public class ElementLabelProvider implements IElementLabelProvider
{
	public ImageDescriptor getImageDescriptor(Element element)
	{
		return (ImageDescriptor)element.getAttribute(TypeCreator.IMAGE_DESCRIPTOR);
	}

}
