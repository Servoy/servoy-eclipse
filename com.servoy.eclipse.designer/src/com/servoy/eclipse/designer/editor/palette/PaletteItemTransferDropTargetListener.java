/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

 This program is free software; you can redistribute it and/or modify it under
 the terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along
 with this program; if not, see http://www.gnu.org/licenses or write to the Free
 Software Foundation,Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
 */

package com.servoy.eclipse.designer.editor.palette;

import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.Request;
import org.eclipse.gef.dnd.TemplateTransfer;
import org.eclipse.gef.requests.CreationFactory;

import com.servoy.eclipse.core.elements.ElementFactory;
import com.servoy.eclipse.designer.dnd.ElementTransferDropTarget;
import com.servoy.eclipse.designer.editor.CreateElementRequest;
import com.servoy.j2db.persistence.Template;

/**
 * Drop target for elements from the palette.
 * 
 * @author rgansevles
 *
 */
public class PaletteItemTransferDropTargetListener extends ElementTransferDropTarget
{

	/**
	 * @param viewer
	 */
	public PaletteItemTransferDropTargetListener(EditPartViewer viewer)
	{
		super(viewer, TemplateTransfer.getInstance());
	}

	@Override
	protected Request createTargetRequest()
	{
		CreationFactory factory = getFactory(TemplateTransfer.getInstance().getTemplate());
		CreateElementRequest request = new CreateElementRequest(factory);

		Dimension d2size = null;
		if (factory instanceof RequestTypeCreationFactory)
		{
			Object data = ((RequestTypeCreationFactory)factory).getData();
			if (data instanceof Template)
			{
				java.awt.Dimension size = ElementFactory.getTemplateBoundsize((Template)data);
				if (size != null)
				{
					d2size = new Dimension(size.width, size.height);
				}
			}
		}

		if (d2size == null)
		{
			d2size = new Dimension(80, 20);
		}
		request.setSize(d2size);
		return request;
	}

	/**
	 * Returns the appropriate Factory object to be used for the specified
	 * template. This Factory is used on the CreateRequest that is sent to the
	 * target EditPart.
	 * 
	 * @param template
	 *            the template Object
	 * @return a Factory
	 */
	protected CreationFactory getFactory(Object template)
	{
		if (template instanceof CreationFactory)
		{
			return ((CreationFactory)template);
		}
		return null;
	}
}
