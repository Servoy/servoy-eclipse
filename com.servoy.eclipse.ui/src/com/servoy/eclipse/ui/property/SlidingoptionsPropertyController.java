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

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;

import com.servoy.j2db.persistence.ISupportPrintSliding;

/**
 * Property controller for printSliding property.
 * 
 * @author rgansevles
 */

public class SlidingoptionsPropertyController extends PropertyController<Integer, Object>
{
	private static ILabelProvider labelProvider = null;

	public SlidingoptionsPropertyController(String id, String displayName)
	{
		super(id, displayName);
	}

	@Override
	protected IPropertyConverter<Integer, Object> createConverter()
	{
		IPropertyConverter<Slidingoption, Object> complexConverter = new ComplexProperty.ComplexPropertyConverter<Slidingoption>()
		{
			@Override
			public Object convertProperty(Object id, Slidingoption value)
			{
				return new ComplexProperty<Slidingoption>(value)
				{
					@Override
					public IPropertySource getPropertySource()
					{
						SlidingoptionsPropertySource slidingoptionsPropertySource = new SlidingoptionsPropertySource(this);
						slidingoptionsPropertySource.setReadonly(SlidingoptionsPropertyController.this.isReadOnly());
						return slidingoptionsPropertySource;
					}
				};
			}
		};
		return new ChainedPropertyConverter<Integer, Slidingoption, Object>(Slidingoption.SLIDINGOPTION_CONVERTER, complexConverter);
	}

	@Override
	public ILabelProvider getLabelProvider()
	{
		if (labelProvider == null)
		{
			labelProvider = new LabelProvider()
			{
				@Override
				public String getText(Object value)
				{
					Slidingoption val = (Slidingoption)value;
					StringBuffer sb = new StringBuffer();
					if (val.move_horizontal || val.move_vertical)
					{
						sb.append("Mv: ");
						if (val.move_horizontal) sb.append("Ho");
						if (val.move_vertical) sb.append("Ve");
					}
					if (val.width_grow || val.width_shrink)
					{
						if (sb.length() > 0) sb.append(';');
						sb.append("Wi: ");
						if (val.width_grow) sb.append("Gr");
						if (val.width_shrink) sb.append("Sh");
					}
					if (val.height_grow || val.height_shrink)
					{
						if (sb.length() > 0) sb.append(';');
						sb.append("He: ");
						if (val.height_grow) sb.append("Gr");
						if (val.height_shrink) sb.append("Sh");
					}

					return sb.toString();
				}
			};
		}
		return labelProvider;
	}

	@Override
	public CellEditor createPropertyEditor(Composite parent)
	{
		return null;
	}

	public static class Slidingoption
	{
		public final boolean move_horizontal;
		public final boolean move_vertical;
		public final boolean width_grow;
		public final boolean width_shrink;
		public final boolean height_grow;
		public final boolean height_shrink;

		public Slidingoption(boolean move_horizontal, boolean move_vertical, boolean width_grow, boolean width_shrink, boolean height_grow,
			boolean height_shrink)
		{
			this.move_horizontal = move_horizontal;
			this.move_vertical = move_vertical;
			this.width_grow = width_grow;
			this.width_shrink = width_shrink;
			this.height_grow = height_grow;
			this.height_shrink = height_shrink;
		}

		public static final IPropertyConverter<Integer, Slidingoption> SLIDINGOPTION_CONVERTER = new IPropertyConverter<Integer, Slidingoption>()
		{
			public Slidingoption convertProperty(Object id, Integer options)
			{
				int slide = options.intValue();
				return new Slidingoption(
					(((slide & ISupportPrintSliding.ALLOW_MOVE_MIN_X) == ISupportPrintSliding.ALLOW_MOVE_MIN_X) || ((slide & ISupportPrintSliding.ALLOW_MOVE_PLUS_X) == ISupportPrintSliding.ALLOW_MOVE_PLUS_X)),
					(((slide & ISupportPrintSliding.ALLOW_MOVE_PLUS_Y) == ISupportPrintSliding.ALLOW_MOVE_PLUS_Y) || ((slide & ISupportPrintSliding.ALLOW_MOVE_MIN_Y) == ISupportPrintSliding.ALLOW_MOVE_MIN_Y)),
					((slide & ISupportPrintSliding.GROW_WIDTH) == ISupportPrintSliding.GROW_WIDTH),
					((slide & ISupportPrintSliding.SHRINK_WIDTH) == ISupportPrintSliding.SHRINK_WIDTH),
					((slide & ISupportPrintSliding.GROW_HEIGHT) == ISupportPrintSliding.GROW_HEIGHT),
					((slide & ISupportPrintSliding.SHRINK_HEIGHT) == ISupportPrintSliding.SHRINK_HEIGHT));
			}

			public Integer convertValue(Object id, Slidingoption slidingoption)
			{
				int retval = ISupportPrintSliding.NO_SLIDING;
				retval += ((slidingoption.move_horizontal) ? ISupportPrintSliding.ALLOW_MOVE_MIN_X : 0);
				retval += ((slidingoption.move_horizontal) ? ISupportPrintSliding.ALLOW_MOVE_PLUS_X : 0);
				retval += ((slidingoption.move_vertical) ? ISupportPrintSliding.ALLOW_MOVE_MIN_Y : 0);
				retval += ((slidingoption.move_vertical) ? ISupportPrintSliding.ALLOW_MOVE_PLUS_Y : 0);
				retval += ((slidingoption.width_grow) ? ISupportPrintSliding.GROW_WIDTH : 0);
				retval += ((slidingoption.width_shrink) ? ISupportPrintSliding.SHRINK_WIDTH : 0);
				retval += ((slidingoption.height_grow) ? ISupportPrintSliding.GROW_HEIGHT : 0);
				retval += ((slidingoption.height_shrink) ? ISupportPrintSliding.SHRINK_HEIGHT : 0);
				return new Integer(retval);
			}
		};
	}


	/*
	 * Sliding option source property implementation
	 * 
	 * @author rgansevles
	 */

	public static class SlidingoptionsPropertySource extends ComplexPropertySource<Slidingoption>
	{
		public final String MOVE_HORIZONTAL = "move_horizontal";
		public final String MOVE_VERTICAL = "move_vertical";
		public final String WIDTH_GROW = "width_grow";
		public final String WIDTH_SHRINK = "width_shrink";
		public final String HEIGHT_GROW = "height_grow";
		public final String HEIGHT_SHRINK = "height_shrink";

		public SlidingoptionsPropertySource(ComplexProperty<Slidingoption> complexProperty)
		{
			super(complexProperty);
		}

		@Override
		public IPropertyDescriptor[] createPropertyDescriptors()
		{
			// make sure sub-properties are sorted in defined order
			return PropertyController.applySequencePropertyComparator(new IPropertyDescriptor[] { new CheckboxPropertyDescriptor(MOVE_HORIZONTAL,
				"Move: allow horizontal"), new CheckboxPropertyDescriptor(MOVE_VERTICAL, "Move: allow vertical"), new CheckboxPropertyDescriptor(WIDTH_GROW,
				"Width: allow grow"), new CheckboxPropertyDescriptor(WIDTH_SHRINK, "Width: allow shrink"), new CheckboxPropertyDescriptor(HEIGHT_GROW,
				"Height: allow grow"), new CheckboxPropertyDescriptor(HEIGHT_SHRINK, "Height: allow shrink") });
		}

		@Override
		public Object getPropertyValue(Object id)
		{
			if (MOVE_HORIZONTAL.equals(id)) return Boolean.valueOf((getEditableValue().move_horizontal));
			if (MOVE_VERTICAL.equals(id)) return Boolean.valueOf((getEditableValue().move_vertical));
			if (WIDTH_GROW.equals(id)) return Boolean.valueOf((getEditableValue().width_grow));
			if (WIDTH_SHRINK.equals(id)) return Boolean.valueOf((getEditableValue().width_shrink));
			if (HEIGHT_GROW.equals(id)) return Boolean.valueOf((getEditableValue().height_grow));
			if (HEIGHT_SHRINK.equals(id)) return Boolean.valueOf((getEditableValue().height_shrink));
			return null;
		}

		@Override
		public Object resetComplexPropertyValue(Object id)
		{
			return Boolean.FALSE;
		}

		@Override
		protected Slidingoption setComplexPropertyValue(Object id, Object v)
		{
			Slidingoption val = getEditableValue();
			if (MOVE_HORIZONTAL.equals(id)) return new Slidingoption(Boolean.TRUE.equals(v), val.move_vertical, val.width_grow, val.width_shrink,
				val.height_grow, val.height_shrink);
			if (MOVE_VERTICAL.equals(id)) return new Slidingoption(val.move_horizontal, Boolean.TRUE.equals(v), val.width_grow, val.width_shrink,
				val.height_grow, val.height_shrink);
			if (WIDTH_GROW.equals(id)) return new Slidingoption(val.move_horizontal, val.move_vertical, Boolean.TRUE.equals(v), val.width_shrink,
				val.height_grow, val.height_shrink);
			if (WIDTH_SHRINK.equals(id)) return new Slidingoption(val.move_horizontal, val.move_vertical, val.width_grow, Boolean.TRUE.equals(v),
				val.height_grow, val.height_shrink);
			if (HEIGHT_GROW.equals(id)) return new Slidingoption(val.move_horizontal, val.move_vertical, val.width_grow, val.width_shrink,
				Boolean.TRUE.equals(v), val.height_shrink);
			if (HEIGHT_SHRINK.equals(id)) return new Slidingoption(val.move_horizontal, val.move_vertical, val.width_grow, val.width_shrink, val.height_grow,
				Boolean.TRUE.equals(v));

			return null;
		}
	}
}
