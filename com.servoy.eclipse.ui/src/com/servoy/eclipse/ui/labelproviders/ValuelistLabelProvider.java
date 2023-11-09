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

import java.util.Map;

import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.sablo.specification.PropertyDescription;

import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.resource.FontResource;
import com.servoy.eclipse.ui.util.IDeprecationProvider;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.IChildWebObject;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportDeprecated;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.persistence.WebObjectImpl;
import com.servoy.j2db.server.ngclient.property.types.ValueListPropertyType;

/**
 * Label provider for value lists.
 *
 * @author rgansevles
 */

public class ValuelistLabelProvider extends LabelProvider implements IFontProvider, IPersistLabelProvider, IDeprecationProvider
{
	public static final int VALUELIST_NONE = 0;
	public static final int VALUELIST_SPECIAL = Integer.MAX_VALUE;
	private final FlattenedSolution flattenedSolution;
	private final IPersist persist;

	public ValuelistLabelProvider(FlattenedSolution flattenedSolution, IPersist persist)
	{
		this.flattenedSolution = flattenedSolution;
		this.persist = persist;
	}

	@Override
	public String getText(Object value)
	{
		if (value == null) return Messages.LabelNone;

		int vlmId = ((Integer)value).intValue();

		if (vlmId == VALUELIST_NONE || vlmId == VALUELIST_SPECIAL)
		{
			PropertyDescription specPD = null;
			if (persist instanceof IChildWebObject) specPD = ((IChildWebObject)persist).getPropertyDescription();
			if (persist instanceof WebComponent && ((WebComponent)persist).getImplementation() instanceof WebObjectImpl)
				specPD = ((WebObjectImpl)(((WebComponent)persist).getImplementation())).getPropertyDescription();
			if (specPD != null)
			{
				Map<String, PropertyDescription> allProperties = specPD.getProperties();
				for (var entry : allProperties.entrySet())
				{
					PropertyDescription propDescription = specPD.getProperty(entry.getKey());
					if (propDescription != null)
					{
						if (propDescription.getType() instanceof ValueListPropertyType)
						{
							if (propDescription.hasDefault() && !WebObjectImpl.isPersistMappedProperty(propDescription))
							{
								return propDescription.getDefaultValue().toString();
							}
						}
					}
				}
			}
			return Messages.LabelNone;
		}

		ValueList vl = (ValueList)getPersist(value);
		if (vl != null)
		{
			return vl.getName();
		}

		return Messages.LabelUnresolved;
	}

	public Font getFont(Object value)
	{
		if (value == null) return FontResource.getDefaultFont(SWT.ITALIC, 0);

		int vlmId = ((Integer)value).intValue();

		if (vlmId == VALUELIST_NONE)
		{
			return FontResource.getDefaultFont(SWT.ITALIC, 0);
		}
		return FontResource.getDefaultFont(SWT.NONE, 0);
	}

	public IPersist getPersist(Object value)
	{
		if (value instanceof Integer)
		{
			return flattenedSolution.getValueList(((Integer)value).intValue());
		}
		return null;
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
