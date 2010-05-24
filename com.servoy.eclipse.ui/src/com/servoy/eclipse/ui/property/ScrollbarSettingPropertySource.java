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

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.ui.views.properties.ComboBoxPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertyDescriptor;

import com.servoy.eclipse.ui.Messages;
import com.servoy.j2db.persistence.ISupportScrollbars;

public class ScrollbarSettingPropertySource extends ComplexPropertySource<Integer>
{
	public static final String HORIZONTAL = "horizontal";
	public static final String VERTICAL = "vertical";
	private static final String[] SCROLL_OPTIONS = new String[] { "when needed", "always", "never" };


	private static ILabelProvider scrollbarSettingsLabelProvider;


	public ScrollbarSettingPropertySource(ComplexProperty<Integer> scrollbars)
	{
		super(scrollbars);
	}

	@Override
	public IPropertyDescriptor[] createPropertyDescriptors()
	{
		return new IPropertyDescriptor[] { new ComboBoxPropertyDescriptor(HORIZONTAL, HORIZONTAL, SCROLL_OPTIONS), new ComboBoxPropertyDescriptor(VERTICAL,
			VERTICAL, SCROLL_OPTIONS) };
	}

	@Override
	public Object getPropertyValue(Object id)
	{
		Integer scrollBits = getEditableValue();
		if (scrollBits == null)
		{
			return new Integer(ISupportScrollbars.SCROLLBARS_WHEN_NEEDED);
		}
		if (VERTICAL.equals(id))
		{
			return new Integer(getIndex(scrollBits.intValue() & 7));
		}
		if (HORIZONTAL.equals(id))
		{
			return new Integer(getIndex(scrollBits.intValue() >> 3));
		}
		return null;
	}

	private int getIndex(int bit)
	{
		switch (bit)
		{
			case ISupportScrollbars.VERTICAL_SCROLLBAR_AS_NEEDED :
				return 0;
			case ISupportScrollbars.VERTICAL_SCROLLBAR_ALWAYS :
				return 1;
			case ISupportScrollbars.VERTICAL_SCROLLBAR_NEVER :
				return 2;
			default :
				return 0;
		}
	}

	private int getBit(int index)
	{
		switch (index)
		{
			case 0 :
				return ISupportScrollbars.VERTICAL_SCROLLBAR_AS_NEEDED;
			case 1 :
				return ISupportScrollbars.VERTICAL_SCROLLBAR_ALWAYS;
			case 2 :
				return ISupportScrollbars.VERTICAL_SCROLLBAR_NEVER;
			default :
				return ISupportScrollbars.VERTICAL_SCROLLBAR_AS_NEEDED;
		}
	}

	@Override
	public boolean isPropertySet(Object id)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void resetPropertyValue(Object id)
	{
		// TODO Auto-generated method stub

	}

	@Override
	protected Integer setComplexPropertyValue(Object id, Object v)
	{
		int index = (v == null) ? 0 : ((Integer)v).intValue();
		int scrollBits = getEditableValue() == null ? 0 : getEditableValue().intValue();
		// bits 1, 2, 4 are VERTICAL flags
		// bits 8, 16, 32 are HORIZONTAL flags
		if (VERTICAL.equals(id))
		{
			scrollBits = (scrollBits & 070) | getBit(index);
		}
		if (HORIZONTAL.equals(id))
		{
			scrollBits = (scrollBits & 07) | (getBit(index) << 3);
		}
		if ((scrollBits & (~(ISupportScrollbars.VERTICAL_SCROLLBAR_AS_NEEDED + ISupportScrollbars.HORIZONTAL_SCROLLBAR_AS_NEEDED))) == 0)
		{
			scrollBits = 0; // the default
		}
		return new Integer(scrollBits);
	}

	public static ILabelProvider getLabelProvider()
	{
		if (scrollbarSettingsLabelProvider == null)
		{
			scrollbarSettingsLabelProvider = new LabelProvider()
			{
				@Override
				public String getText(Object element)
				{
					int scrollBits = element == null ? 0 : ((Integer)element).intValue();
					if (scrollBits == 0)
					{
						return Messages.LabelDefault;
					}

					StringBuffer retval = new StringBuffer();
					int scroll = scrollBits;
					if ((scroll & ISupportScrollbars.VERTICAL_SCROLLBAR_AS_NEEDED) == ISupportScrollbars.VERTICAL_SCROLLBAR_AS_NEEDED)
					{
						retval.append("V:wn");
					}
					else
					{
						if ((scroll & ISupportScrollbars.VERTICAL_SCROLLBAR_ALWAYS) == ISupportScrollbars.VERTICAL_SCROLLBAR_ALWAYS)
						{
							retval.append("V:a");
						}
						else if ((scroll & ISupportScrollbars.VERTICAL_SCROLLBAR_NEVER) == ISupportScrollbars.VERTICAL_SCROLLBAR_NEVER)
						{
							retval.append("V:n");
						}
					}
					retval.append(" ");
					if ((scroll & ISupportScrollbars.HORIZONTAL_SCROLLBAR_AS_NEEDED) == ISupportScrollbars.HORIZONTAL_SCROLLBAR_AS_NEEDED)
					{
						retval.append("H:wn");
					}
					else
					{
						if ((scroll & ISupportScrollbars.HORIZONTAL_SCROLLBAR_ALWAYS) == ISupportScrollbars.HORIZONTAL_SCROLLBAR_ALWAYS)
						{
							retval.append("H:a");
						}
						else if ((scroll & ISupportScrollbars.HORIZONTAL_SCROLLBAR_NEVER) == ISupportScrollbars.HORIZONTAL_SCROLLBAR_NEVER)
						{
							retval.append("H:n");
						}
					}
					return retval.toString();

				}
			};
		}
		return scrollbarSettingsLabelProvider;
	}

}
