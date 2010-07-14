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
package com.servoy.eclipse.ui.labelproviders;

import org.eclipse.jface.viewers.LabelProvider;

import com.servoy.eclipse.ui.Messages;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportName;

/**
 * Simple label provider for ISupportName objects.
 * 
 * @author rgansevles
 * 
 */
public class SupportNameLabelProvider extends LabelProvider implements IPersistLabelProvider
{
	public static final SupportNameLabelProvider INSTANCE_DEFAULT_NONE = new SupportNameLabelProvider(Messages.LabelNone);
	public static final SupportNameLabelProvider INSTANCE_DEFAULT_UNRESOLVED = new SupportNameLabelProvider(Messages.LabelUnresolved);
	public static final SupportNameLabelProvider INSTANCE_DEFAULT_EMPTY = new SupportNameLabelProvider("");
	public static final SupportNameLabelProvider INSTANCE_DEFAULT_ANONYMOUS = new SupportNameLabelProvider(Messages.LabelAnonymous);

	protected final String defaultText;

	public SupportNameLabelProvider(String defaultText)
	{
		this.defaultText = defaultText;
	}

	@Override
	public String getText(Object value)
	{
		if (value instanceof ISupportName)
		{
			String name = ((ISupportName)value).getName();
			if (name != null && name.length() > 0)
			{
				return name;
			}
		}
		return defaultText;
	}

	public IPersist getPersist(Object value)
	{
		if (value instanceof IPersist)
		{
			return (IPersist)value;
		}
		return null;
	}
}
