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
import com.servoy.j2db.util.Utils;

/**
 * LabelProvider for forms.
 *
 * @author rgansevles
 */

public class FormLabelProvider extends LabelProvider implements IPersistLabelProvider, IDeprecationProvider
{
	public static final String FORM_DEFAULT_STRING = "-default-";
	protected final FlattenedSolution flattenedSolution;
	private final boolean defaultIsNone;
	private final boolean defaultAsText;
	private final boolean nullIsDefault;

	public FormLabelProvider(FlattenedSolution flattenedSolution, boolean defaultIsNone)
	{
		this(flattenedSolution, defaultIsNone, false);
	}

	public FormLabelProvider(FlattenedSolution flattenedSolution, boolean defaultIsNone, boolean defaultAsText)
	{
		this(flattenedSolution, defaultIsNone, defaultAsText, false);
	}

	public FormLabelProvider(FlattenedSolution flattenedSolution, boolean defaultIsNone, boolean defaultAsText, boolean nullIsDefault)
	{
		this.flattenedSolution = flattenedSolution;
		this.defaultIsNone = defaultIsNone;
		this.defaultAsText = defaultAsText;
		this.nullIsDefault = nullIsDefault;
	}

	@Override
	public String getText(Object value)
	{
		if (FORM_DEFAULT_STRING.equals(value) || (nullIsDefault && value == null))
		{
			return defaultIsNone ? Messages.LabelNone : defaultAsText ? Messages.LabelDefaultAsText : Messages.LabelDefault;
		}
		if (value == null)
		{
			return Messages.LabelNone;
		}
		if (Form.NAVIGATOR_IGNORE.equals(value))
		{
			return Messages.LabelIgnore;
		}
		if (Form.NAVIGATOR_NONE.equals(value))
		{
			return Messages.LabelNone;
		}
		if (value instanceof String && Utils.getAsUUID(value, false) == null)
		{
			// working set
			return value.toString();
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
		return flattenedSolution.getForm(value.toString());
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
