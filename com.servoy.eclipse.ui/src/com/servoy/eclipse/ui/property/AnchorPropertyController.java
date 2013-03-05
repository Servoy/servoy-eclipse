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
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;

import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.property.ComplexProperty.ComplexPropertyConverter;
import com.servoy.j2db.persistence.IAnchorConstants;

/**
 * Property controller for anchors properties.
 * 
 * @author rgansevles
 *
 */
public class AnchorPropertyController extends PropertyController<Integer, Object>
{
	public AnchorPropertyController(String id, String displayName)
	{
		super(id, displayName, null, AnchorLabelProvider.LABEL_INSTANCE, new DummyCellEditorFactory(AnchorLabelProvider.LABEL_INSTANCE));
	}

	@Override
	protected IPropertyConverter<Integer, Object> createConverter()
	{
		return new AnchorPropertyConverter();
	}

	public class AnchorPropertyConverter extends ComplexPropertyConverter<Integer>
	{
		@Override
		public Object convertProperty(Object id, Integer value)
		{
			return new ComplexProperty<Integer>(new Integer(0).equals(value) ? new Integer(IAnchorConstants.DEFAULT) : value)
			{
				@Override
				public IPropertySource getPropertySource()
				{
					AnchorPropertySource anchorPropertySource = new AnchorPropertySource(this);
					anchorPropertySource.setReadonly(AnchorPropertyController.this.isReadOnly());
					return anchorPropertySource;
				}
			};
		}
	}

	public static class AnchorPropertySource extends ComplexPropertySource<Integer>
	{
		public static final String TOP = "top";
		public static final String RIGHT = "right";
		public static final String BOTTOM = "bottom";
		public static final String LEFT = "left";

		public AnchorPropertySource(ComplexProperty<Integer> complexProperty)
		{
			super(complexProperty);
		}

		@Override
		public Object resetComplexPropertyValue(Object id)
		{
			return Integer.valueOf(IAnchorConstants.DEFAULT);
		}


		@Override
		public IPropertyDescriptor[] createPropertyDescriptors()
		{
			// make sure sub-properties are sorted in defined order
			return PropertyController.applySequencePropertyComparator(new IPropertyDescriptor[] { new CheckboxPropertyDescriptor(TOP, "top"), new CheckboxPropertyDescriptor(
				RIGHT, "right"), new CheckboxPropertyDescriptor(BOTTOM, "bottom"), new CheckboxPropertyDescriptor(LEFT, "left") });
		}

		@Override
		public Object getPropertyValue(Object id)
		{
			int anchors = getEditableValue().intValue();
			if (anchors == -1) return Boolean.FALSE;

			if (TOP.equals(id)) return (anchors & IAnchorConstants.NORTH) == 0 ? Boolean.FALSE : Boolean.TRUE;
			if (RIGHT.equals(id)) return (anchors & IAnchorConstants.EAST) == 0 ? Boolean.FALSE : Boolean.TRUE;
			if (BOTTOM.equals(id)) return (anchors & IAnchorConstants.SOUTH) == 0 ? Boolean.FALSE : Boolean.TRUE;
			if (LEFT.equals(id)) return (anchors & IAnchorConstants.WEST) == 0 ? Boolean.FALSE : Boolean.TRUE;
			return null;
		}

		@Override
		public Integer setComplexPropertyValue(Object id, Object v)
		{
			int anchors = getEditableValue().intValue();
			if (anchors == -1)
			{
				anchors = 0;
			}
			int flag;
			if (TOP.equals(id)) flag = IAnchorConstants.NORTH;
			else if (RIGHT.equals(id)) flag = IAnchorConstants.EAST;
			else if (BOTTOM.equals(id)) flag = IAnchorConstants.SOUTH;
			else if (LEFT.equals(id)) flag = IAnchorConstants.WEST;
			else return null;

			if (Boolean.TRUE.equals(v))
			{
				anchors |= flag;
			}
			else
			{
				anchors &= (IAnchorConstants.ALL - flag);
			}

			if (anchors == 0)
			{
				return new Integer(-1);
			}
			if (anchors == IAnchorConstants.DEFAULT)
			{
				return new Integer(0);
			}
			// for each direction at least 1 shold be turned on
			if ((anchors & (IAnchorConstants.NORTH | IAnchorConstants.SOUTH)) == 0)
			{
				anchors |= (flag == IAnchorConstants.NORTH ? IAnchorConstants.SOUTH : IAnchorConstants.NORTH);
			}
			if ((anchors & (IAnchorConstants.WEST | IAnchorConstants.EAST)) == 0)
			{
				anchors |= (flag == IAnchorConstants.WEST ? IAnchorConstants.EAST : IAnchorConstants.WEST);
			}
			return new Integer(anchors);
		}
	}


	public static class AnchorLabelProvider extends LabelProvider
	{
		public static AnchorLabelProvider LABEL_INSTANCE = new AnchorLabelProvider();

		@Override
		public String getText(Object element)
		{
			int anchors = ((Integer)element).intValue();

			if (anchors == -1)
			{
				return Messages.LabelNone;
			}
			if (anchors == 0)
			{
				return Messages.AlignTop + ',' + Messages.AlignLeft;
			}

			StringBuffer retval = new StringBuffer();
			if ((anchors & IAnchorConstants.NORTH) == IAnchorConstants.NORTH)
			{
				retval.append(Messages.AlignTop);
			}
			if ((anchors & IAnchorConstants.EAST) == IAnchorConstants.EAST)
			{
				if (retval.length() != 0) retval.append(',');
				retval.append(Messages.AlignRight);
			}
			if ((anchors & IAnchorConstants.SOUTH) == IAnchorConstants.SOUTH)
			{
				if (retval.length() != 0) retval.append(',');
				retval.append(Messages.AlignBottom);
			}
			if ((anchors & IAnchorConstants.WEST) == IAnchorConstants.WEST)
			{
				if (retval.length() != 0) retval.append(',');
				retval.append(Messages.AlignLeft);
			}
			return retval.toString();
		}
	}
}
