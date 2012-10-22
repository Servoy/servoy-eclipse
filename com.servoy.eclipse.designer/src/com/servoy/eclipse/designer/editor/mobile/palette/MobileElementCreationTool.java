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

package com.servoy.eclipse.designer.editor.mobile.palette;

import org.eclipse.gef.Request;
import org.eclipse.gef.requests.CreateRequest;
import org.eclipse.gef.requests.CreationFactory;

import com.servoy.eclipse.designer.editor.CreateElementRequest;
import com.servoy.eclipse.designer.editor.palette.BaseElementCreationTool;
import com.servoy.eclipse.designer.editor.palette.RequestTypeCreationFactory;

/**
 * Tool for creating elements from the palette in the mobile form editor.
 * 
 * @author rgansevles
 *
 */
public class MobileElementCreationTool extends BaseElementCreationTool
{
	/**
	 * Creates a {@link CreateRequest} and sets this tool's factory on the
	 * request.
	 * 
	 * @see org.eclipse.gef.tools.TargetingTool#createTargetRequest()
	 */
	@Override
	protected Request createTargetRequest()
	{
		CreationFactory fact = getFactory();
		CreateElementRequest request = new CreateElementRequest(fact);
		if (fact instanceof RequestTypeCreationFactory)
		{
			request.setSize(((RequestTypeCreationFactory)fact).getNewObjectSize());
		}
		return request;
	}
}
