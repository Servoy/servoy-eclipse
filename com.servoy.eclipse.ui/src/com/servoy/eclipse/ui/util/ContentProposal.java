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
package com.servoy.eclipse.ui.util;

import org.eclipse.jface.fieldassist.IContentProposal;

/**
 * IContentProposal implementation with input object.
 * 
 * @author rgansevles
 *
 */
public class ContentProposal implements IContentProposal
{
	private final String content;
	private final int cursorPosition;
	private final String description;
	private final String label;
	private final Object input;

	public ContentProposal(String content, int cursorPosition, String description, String label, Object input)
	{
		this.content = content;
		this.cursorPosition = cursorPosition;
		this.description = description;
		this.label = label;
		this.input = input;
	}

	public String getContent()
	{
		return content;
	}

	public int getCursorPosition()
	{
		return cursorPosition;
	}

	public String getDescription()
	{
		return description;
	}

	public String getLabel()
	{
		return label;
	}

	public Object getInput()
	{
		return input;
	}
}
