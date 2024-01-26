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
package com.servoy.eclipse.ui.property;

import org.eclipse.jface.viewers.LabelProvider;

import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.labelproviders.IPersistLabelProvider;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.util.PersistHelper;

/**
 * Label provider for Part type.
 * <p>
 * Value may be Part or Integer (part type).
 *
 * @author rgansevles, acostescu
 *
 */
public class PartTypeLabelProvider extends LabelProvider implements IPersistLabelProvider
{

	public static final PartTypeLabelProvider INSTANCE = new PartTypeLabelProvider(null);
	private final Form form;

	public PartTypeLabelProvider(Form formContext)
	{
		this.form = formContext;
	}

	@Override
	public String getText(Object value)
	{
		if (value instanceof Part)
		{
			Part part = (Part)value;
			String text = Part.getDisplayName(part.getPartType());
			IPersist formAncestor = part.getAncestor(IRepository.FORMS);
			if (formAncestor != form)
			{
				text = Messages.labelInherited(text);
			}
			else if (PersistHelper.isOverrideElement(part))
			{
				text = Messages.labelOverride(text);
			}
			return text;
		}
		if (value instanceof Integer)
		{
			return Part.getDisplayName(((Integer)value).intValue());
		}
		return Messages.LabelUnresolved;
	}

	public IPersist getPersist(Object value)
	{
		if (value instanceof Part) return (IPersist)value;
		else return null;
	}

}
