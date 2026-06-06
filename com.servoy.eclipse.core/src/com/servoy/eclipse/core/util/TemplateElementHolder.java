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

package com.servoy.eclipse.core.util;

import com.servoy.j2db.persistence.Template;

/**
 * Holder for a template with an optional selected element name.
 * 
 * @author rgansevles
 *
 */
public class TemplateElementHolder
{
	public final Template template;
	public final int element;

	/**
	 * @param template
	 */
	public TemplateElementHolder(Template template)
	{
		this(template, -1);
	}

	public TemplateElementHolder(Template template, int element)
	{
		this.template = template;
		this.element = element;
	}

	@Override
	public String toString()
	{
		return template.getName() + (element >= 0 ? "" : ":" + element);
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + element;
		result = prime * result + ((template == null) ? 0 : template.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		TemplateElementHolder other = (TemplateElementHolder)obj;
		if (element != other.element) return false;
		if (template == null)
		{
			if (other.template != null) return false;
		}
		else if (!template.equals(other.template)) return false;
		return true;
	}

}
