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
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;

import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.property.MediaoptionsPropertyController.Mediaoption.MediaoptionsType;
import com.servoy.eclipse.ui.util.ModifiedComboBoxCellEditor;

public class MediaoptionsPropertyController extends PropertyController<Integer, Object>
{
	final static ComboboxPropertyController<MediaoptionsType> comboboxController = new ComboboxPropertyController<MediaoptionsType>("MEDIA_OPTIONS",
		"media options", new ComboboxPropertyModel<MediaoptionsType>(
			new MediaoptionsType[] { MediaoptionsType.Crop, MediaoptionsType.Reduce, MediaoptionsType.Enlarge, MediaoptionsType.ReduceEnlarge },
			new String[] { MediaoptionsType.Crop.name(), MediaoptionsType.Reduce.name(), MediaoptionsType.Enlarge.name(), MediaoptionsType.Reduce.name() + '/' +
				MediaoptionsType.Enlarge.name() }), Messages.LabelUnresolved);

	private static ILabelProvider labelProvider = null;

	private final IPropertySource propertySource;


	public MediaoptionsPropertyController(String id, String displayName, IPropertySource propertySource)
	{
		super(id, displayName);
		this.propertySource = propertySource;
	}

	@Override
	protected IPropertyConverter<Integer, Object> createConverter()
	{
		IPropertyConverter<Mediaoption, Object> complexConverter = new ComplexProperty.ComplexPropertyConverter<Mediaoption>()
		{
			@Override
			public Object convertProperty(Object id, Mediaoption value)
			{
				return new ComplexProperty<Mediaoption>(value)
				{
					@Override
					public IPropertySource getPropertySource()
					{
						MediaoptionsPropertySource mediaoptionsPropertySource = new MediaoptionsPropertySource(this);
						mediaoptionsPropertySource.setReadonly(MediaoptionsPropertyController.this.isReadOnly());
						return mediaoptionsPropertySource;
					}
				};
			}

			@Override
			public Mediaoption convertValue(Object id, Object value)
			{
				if (value instanceof Integer)
				{
					// media options type selected from dropdown
					MediaoptionsType optionsType = comboboxController.getConverter().convertValue(null, (Integer)value);
					Mediaoption oldValue = ((ComplexProperty<Mediaoption>)propertySource.getPropertyValue(id)).getValue();
					return new Mediaoption(optionsType, oldValue.keepAspectRatio);
				}
				return ((ComplexProperty<Mediaoption>)value).getValue();
			}
		};
		return new ChainedPropertyConverter<Integer, Mediaoption, Object>(Mediaoption.MEDIAOPTION_CONVERTER, complexConverter);
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
					Mediaoption options;
					if (value instanceof Mediaoption)
					{
						options = (Mediaoption)value;
					}
					else
					{
						options = ((ComplexProperty<Mediaoption>)value).getValue();
					}

					Integer index = comboboxController.getConverter().convertProperty(null, options.type);
					String label = comboboxController.getLabelProvider().getText(index);
					if (options.keepAspectRatio)
					{
						return label + ", keep aspect ratio";
					}
					return label;
				}
			};
		}
		return labelProvider;
	}


	@Override
	public CellEditor createPropertyEditor(Composite parent)
	{
		return new ModifiedComboBoxCellEditor(parent, comboboxController.getModel().getDisplayValues(), SWT.READ_ONLY)
		{
			@Override
			protected void doSetValue(Object value)
			{
				// set the value of the media options type in the combobox cell editor
				Mediaoption options;
				if (value instanceof Mediaoption)
				{
					options = (Mediaoption)value;
				}
				else
				{
					options = ((ComplexProperty<Mediaoption>)value).getValue();
				}
				super.doSetValue(comboboxController.getConverter().convertProperty(null, options.type));
			}
		};
	}

	public static class Mediaoption
	{
		public static enum MediaoptionsType
		{
			Crop, Reduce, Enlarge, ReduceEnlarge
		}

		public final MediaoptionsType type;
		public final boolean keepAspectRatio;

		public Mediaoption(MediaoptionsType type, boolean keepAspectRatio)
		{
			this.type = type;
			this.keepAspectRatio = keepAspectRatio;
		}

		public static final IPropertyConverter<Integer, Mediaoption> MEDIAOPTION_CONVERTER = new IPropertyConverter<Integer, Mediaoption>()
		{
			public Mediaoption convertProperty(Object id, Integer options)
			{
				MediaoptionsType type = MediaoptionsType.ReduceEnlarge;
				if (options != null)
				{
					int opts = options.intValue();
					if ((opts & 1) == 1) type = MediaoptionsType.Crop;
					else if ((opts & 6) == 6) type = MediaoptionsType.ReduceEnlarge;
					else if ((opts & 2) == 2) type = MediaoptionsType.Reduce;
					else if ((opts & 4) == 4) type = MediaoptionsType.Enlarge;
				}

				boolean keepAspectRatio = options == null || options.intValue() == 0 || (options.intValue() & 8) == 8;
				return new Mediaoption(type, keepAspectRatio);
			}

			public Integer convertValue(Object id, Mediaoption mediaoption)
			{
				int options = -1;
				switch (mediaoption.type)
				{
					case Crop :
						options = 1;
						break;
					case Enlarge :
						options = 4;
						break;
					case Reduce :
						options = 2;
						break;
					case ReduceEnlarge :
						options = 6;
						break;
				}
				if (mediaoption.type != MediaoptionsType.Crop && mediaoption.keepAspectRatio)
				{
					return new Integer(options | 8);
				}
				return new Integer(options);
			}
		};

	}


	/*
	 * Media option source property implementation
	 * 
	 * @author rob
	 * 
	 */

	public static class MediaoptionsPropertySource extends ComplexPropertySource<Mediaoption>
	{
		private static final String KEEP_ASPECT_RATIO = "keep_aspect_ratio";

		public MediaoptionsPropertySource(ComplexProperty<Mediaoption> complexProperty)
		{
			super(complexProperty);
		}

		@Override
		public IPropertyDescriptor[] createPropertyDescriptors()
		{
			if (getEditableValue().type == MediaoptionsType.Crop)
			{
				// keep aspect ratio n/a
				return new IPropertyDescriptor[0];
			}
			return new IPropertyDescriptor[] { new CheckboxPropertyDescriptor(KEEP_ASPECT_RATIO, "keep aspect ratio") };
		}

		@Override
		public Object getPropertyValue(Object id)
		{
			if (KEEP_ASPECT_RATIO.equals(id))
			{
				return Boolean.valueOf((getEditableValue().keepAspectRatio));
			}
			return null;
		}

		@Override
		protected Mediaoption setComplexPropertyValue(Object id, Object v)
		{
			if (KEEP_ASPECT_RATIO.equals(id))
			{
				return new Mediaoption(getEditableValue().type, Boolean.TRUE.equals(v));
			}
			return null;
		}
	}


}
