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
package com.servoy.eclipse.designer.editor.commands;


/**
 * Request with options for placing fields.
 * 
 * @author rgansevles
 * 
 */
public class DataFieldRequest extends DataRequest
{
	public final boolean placeAsLabels;
	public final boolean placeWithLabels;
	public final boolean placeHorizontal;
	public final boolean fillText;
	public final boolean fillName;

	public DataFieldRequest(Object type, Object data, boolean placeAsLabels, boolean placeWithLabels, boolean placeHorizontal, boolean fillText,
		boolean fillName)
	{
		super(type, data);
		this.placeAsLabels = placeAsLabels;
		this.placeWithLabels = placeWithLabels;
		this.placeHorizontal = placeHorizontal;
		this.fillText = fillText;
		this.fillName = fillName;
	}
}
