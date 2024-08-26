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
import com.servoy.eclipse.ui.util.IDeprecationProvider;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportDeprecated;

/**
 * LabelProvider for forms.
 *
 * @author rgansevles
 */

public class FormLabelProvider extends LabelProvider implements IPersistLabelProvider, IDeprecationProvider
{
	protected final FlattenedSolution flattenedSolution;
	private final boolean defaultIsNone;
	private final boolean defaultAsText;

	public FormLabelProvider(FlattenedSolution flattenedSolution, boolean defaultIsNone)
	{
		this(flattenedSolution, defaultIsNone, false);
	}

	public FormLabelProvider(FlattenedSolution flattenedSolution, boolean defaultIsNone, boolean defaultAsText)
	{
		this.flattenedSolution = flattenedSolution;
		this.defaultIsNone = defaultIsNone;
		this.defaultAsText = defaultAsText;
	}

	@Override
	public String getText(Object value)
	{
		if (value == null)
		{
			return Messages.LabelNone;
		}
		if (value instanceof String)
		{
			// working set
			return value.toString();
		}

		if (!(value instanceof Integer))
		{
			return Messages.LabelUnresolved;
		}

		int formId = ((Integer)value).intValue();
		if (formId == Form.NAVIGATOR_DEFAULT)
		{
			return defaultIsNone ? Messages.LabelNone : defaultAsText ? Messages.LabelDefaultAsText : Messages.LabelDefault;
		}
		if (formId == Form.NAVIGATOR_IGNORE)
		{
			return Messages.LabelIgnore;
		}
		if (formId == Form.NAVIGATOR_NONE)
		{
			return Messages.LabelNone;
		}
		IPersist persist = getPersist(value);
		if (!(persist instanceof Form))
		{
			return Messages.LabelUnresolved;
		}

		return ((Form)persist).getName();
	}

	public IPersist getPersist(Object value)
	{
		if (value == null)
		{
			return null;
		}
		if (value instanceof String)
		{
			return null;
		}
		return flattenedSolution.getForm(((Integer)value).intValue());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.eclipse.ui.util.IDeprecationProvider#isDeprecated(java.lang.Object)
	 */
	@Override
	public Boolean isDeprecated(Object element)
	{
		IPersist persist = getPersist(element);
		if (persist instanceof ISupportDeprecated)
		{
			return Boolean.valueOf(((ISupportDeprecated)persist).getDeprecated() != null);
		}

		return null;
	}

}
