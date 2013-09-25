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

import java.awt.Color;
import java.awt.Insets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.border.TitledBorder;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.properties.ComboBoxPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;
import org.eclipse.ui.views.properties.TextPropertyDescriptor;

import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.editors.FontCellEditor;
import com.servoy.eclipse.ui.editors.TagsAndI18NTextCellEditor;
import com.servoy.eclipse.ui.labelproviders.FontLabelProvider;
import com.servoy.eclipse.ui.labelproviders.TextCutoffLabelProvider;
import com.servoy.eclipse.ui.property.ComplexProperty.ComplexPropertyConverter;
import com.servoy.eclipse.ui.property.ConvertorObjectCellEditor.IObjectTextConverter;
import com.servoy.eclipse.ui.util.ModifiedComboBoxCellEditor;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.debug.DebugUtils;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.IStyleSheet;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.gui.RoundedBorder;
import com.servoy.j2db.util.gui.SpecialMatteBorder;

/**
 * Property controller for selecting border in Properties view.
 * 
 * @author rgansevles
 *
 */

public class BorderPropertyController extends PropertyController<Border, Object>
{
	public static enum BorderType
	{
		Default, Empty, Etched, Bevel, Line, Title, Matte, SpecialMatte, RoundedWebBorder
	}

	private static HashMap<BorderType, Border> defaultBorderValues = new HashMap<BorderType, Border>();
	private final PersistContext persistContext;

	final static ComboboxPropertyController<BorderType> comboboxController = new ComboboxPropertyController<BorderType>("BORDER_TYPE", "borderTypes",
		new ComboboxPropertyModel<BorderType>(BorderType.values()), Messages.LabelUnresolved);

	private static ILabelProvider labelProvider = null;

	/*
	 * Members
	 */
	private final IPropertySource propertySource;

	public BorderPropertyController(String id, String displayName, IPropertySource propertySource, PersistContext persistContext)
	{
		super(id, displayName);
		this.propertySource = propertySource;
		this.persistContext = persistContext;
		getDefaultBorderValuesMap(false);
	}

	public static HashMap<BorderType, Border> getDefaultBorderValuesMap()
	{
		return getDefaultBorderValuesMap(true);
	}

	private static HashMap<BorderType, Border> getDefaultBorderValuesMap(boolean wait)
	{
		Runnable runnable = null;
		synchronized (defaultBorderValues)
		{
			if (defaultBorderValues.size() == 0)
			{
				runnable = new Runnable()
				{
					@Override
					public void run()
					{
						synchronized (defaultBorderValues)
						{
							if (defaultBorderValues.size() == 0)
							{
								defaultBorderValues.put(BorderType.Default, null);
								defaultBorderValues.put(BorderType.Empty, new EmptyBorder(0, 0, 0, 0));
								defaultBorderValues.put(BorderType.Etched, new EtchedBorder(EtchedBorder.RAISED));
								defaultBorderValues.put(BorderType.Bevel, new BevelBorder(BevelBorder.RAISED));
								defaultBorderValues.put(BorderType.Line, new LineBorder(Color.BLACK));
								defaultBorderValues.put(BorderType.Title, new TitledBorder("Title")); //$NON-NLS-1$
								defaultBorderValues.put(BorderType.Matte, new MatteBorder(0, 0, 0, 0, Color.BLACK));
								defaultBorderValues.put(BorderType.SpecialMatte, new SpecialMatteBorder(0, 0, 0, 0, Color.BLACK, Color.BLACK, Color.BLACK,
									Color.BLACK));
								defaultBorderValues.put(BorderType.RoundedWebBorder, new RoundedBorder(0, 0, 0, 0, Color.BLACK, Color.BLACK, Color.BLACK,
									Color.BLACK));
							}
						}
					}
				};
			}
		}
		if (runnable != null)
		{
			if (wait)
			{
				try
				{
					DebugUtils.invokeAndWaitWhileDispatchingOnSWT(runnable);
				}
				catch (Exception e)
				{
					Debug.error(e);
				}
			}
			else
			{
				SwingUtilities.invokeLater(runnable);
			}
		}
		return defaultBorderValues;
	}

	/**
	 * Use this if you want custom default values for your border types.
	 * ex:
	 * <p>
	 *  getDefaultBorderValues(BorderType.SpecialMatte,0, 0, 0, 0, Color.BLACK, Color.BLACK, Color.BLACK,Color.BLACK)
	 *  </p>
	 *  
	 *  <br/><br/>
	 *  Add more Border types in the implementation of this method as needed.
	 * @param borderType
	 * @param args
	 * @return
	 */
	public static Border getDefaultBorderValues(BorderType borderType, Object... args)
	{
		Runnable runnable = null;
		final Object[] f_args = args;
		final Border[] ret = new Border[1];
		switch (borderType)
		{
			case SpecialMatte :
			{
				if (args.length != 8)
				{
					return getDefaultBorderValuesMap().get(BorderType.SpecialMatte);
				}
				runnable = new Runnable()
				{
					@Override
					public void run()
					{
						ret[0] = new SpecialMatteBorder((Integer)f_args[0], (Integer)f_args[1], (Integer)f_args[2], (Integer)f_args[3], (Color)f_args[4],
							(Color)f_args[5], (Color)f_args[6], (Color)f_args[7]);
					}

				};
			}
		}
		if (runnable != null)
		{
			try
			{
				DebugUtils.invokeAndWaitWhileDispatchingOnSWT(runnable);
			}
			catch (Exception e)
			{
				Debug.error(e);
			}

			return ret[0];
		}
		return null;
	}

	private static BorderType getBorderTypeConstant(Border border)
	{
		if (border != null)
		{
			Class cls = border.getClass();
			if (cls == javax.swing.border.EmptyBorder.class) return BorderType.Empty;
			if (cls == javax.swing.border.EtchedBorder.class) return BorderType.Etched;
			if (cls == javax.swing.border.BevelBorder.class) return BorderType.Bevel;
			if (cls == javax.swing.border.LineBorder.class) return BorderType.Line;
			if (cls == javax.swing.border.TitledBorder.class) return BorderType.Title;
			if (cls == javax.swing.border.MatteBorder.class) return BorderType.Matte;
			if (cls == com.servoy.j2db.util.gui.SpecialMatteBorder.class) return BorderType.SpecialMatte;
			if (cls == com.servoy.j2db.util.gui.RoundedBorder.class) return BorderType.RoundedWebBorder;
		}
		return BorderType.Default;
	}

	/**
	 * Create a default Border for the border type
	 * 
	 * @param borderType
	 * @return
	 */
	private static javax.swing.border.Border createBorder(BorderType borderType)
	{
		switch (borderType)
		{
			case Default :
				return null;
			case Empty :
				return new EmptyBorder(0, 0, 0, 0);
			case Etched :
				return new EtchedBorder(EtchedBorder.RAISED);
			case Bevel :
				return new BevelBorder(BevelBorder.RAISED);
			case Line :
				return new LineBorder(Color.BLACK);
			case Title :
				return new TitledBorder("Title");
			case Matte :
				return new MatteBorder(0, 0, 0, 0, Color.BLACK);
			case SpecialMatte :
				return new SpecialMatteBorder(0, 0, 0, 0, Color.BLACK, Color.BLACK, Color.BLACK, Color.BLACK);
			case RoundedWebBorder :
				return new RoundedBorder(0, 0, 0, 0, Color.BLACK, Color.BLACK, Color.BLACK, Color.BLACK);
		}
		return null;
	}

	private <T extends Border> IPropertySource getPropertySource(ComplexProperty<T> complexProperty)
	{
		ComplexPropertySource propertySource = null;
		switch (getBorderTypeConstant(complexProperty.getValue()))
		{
			case Default :
				propertySource = new ComplexPropertySource<T>(complexProperty); // dummy without property descriptors
				break;
			case Empty :
				propertySource = new EmptyBorderPropertySource((ComplexProperty<EmptyBorder>)complexProperty);
				break;
			case Etched :
				propertySource = new EtchedBorderPropertySource((ComplexProperty<EtchedBorder>)complexProperty);
				break;
			case Bevel :
				propertySource = new BevelBorderPropertySource((ComplexProperty<BevelBorder>)complexProperty);
				break;
			case Line :
				propertySource = new LineBorderPropertySource((ComplexProperty<LineBorder>)complexProperty);
				break;
			case Title :
				propertySource = new TitledBorderPropertySource(persistContext, (ComplexProperty<TitledBorder>)complexProperty);
				break;
			case Matte :
				propertySource = new MatteBorderPropertySource((ComplexProperty<MatteBorder>)complexProperty);
				break;
			case SpecialMatte :
				propertySource = new SpecialMatteBorderPropertySource((ComplexProperty<SpecialMatteBorder>)complexProperty);
				break;
			case RoundedWebBorder :
				propertySource = new RoundedBorderPropertySource((ComplexProperty<SpecialMatteBorder>)complexProperty);
				break;
		}
		if (propertySource != null)
		{
			propertySource.setReadonly(isReadOnly());
		}
		return propertySource;
	}

	@Override
	protected IPropertyConverter<Border, Object> createConverter()
	{
		return new ComplexProperty.ComplexPropertyConverter<Border>()
		{
			@Override
			public Object convertProperty(Object id, Border value)
			{
				return new ComplexProperty<Border>(value)
				{
					@Override
					public IPropertySource getPropertySource()
					{
						return BorderPropertyController.this.getPropertySource(this);
					}
				};
			}

			@Override
			public Border convertValue(Object id, Object value)
			{
				if (value instanceof Integer)
				{
					// border type selected from dropdown
					BorderType borderType = comboboxController.getConverter().convertValue(null, (Integer)value);
					ComplexProperty<Border> oldValue = (ComplexProperty<Border>)propertySource.getPropertyValue(id);
					if (getBorderTypeConstant(oldValue.getValue()) == borderType)
					{
						// border type not changed, return old value
						return oldValue.getValue();
					}
					// border type changed, create new value
					return createBorder(borderType);
				}
				if (value == null || value instanceof Border)
				{
					return (Border)value;
				}
				return ((ComplexProperty<Border>)value).getValue();
			}
		};
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
					Border border = null;
					if (value instanceof Border)
					{
						border = (Border)value;
					}
					else if (value instanceof ComplexProperty)
					{
						border = ((ComplexProperty<Border>)value).getValue();
					}
					BorderType borderTypeConstant = getBorderTypeConstant(border);
					return borderTypeConstant == BorderType.Default ? Messages.LabelDefault : borderTypeConstant.name();
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
				// set the value of the border type in the combobox cell editor
				Border border = null;
				if (value instanceof Border)
				{
					border = (Border)value;
				}
				else if (value instanceof ComplexProperty)
				{
					border = ((ComplexProperty<Border>)value).getValue();
				}
				super.doSetValue(comboboxController.getConverter().convertProperty(null, getBorderTypeConstant(border)));
			}
		};
	}

	/*
	 * Border source property implementations
	 * 
	 * @author rgansevles
	 */

	public static class EmptyBorderPropertySource extends ComplexPropertySource<EmptyBorder>
	{
		private static final String INSETS = "insets.";

		// delegate to InsetsPropertySource
		InsetsPropertySource insetPropertySource;

		public EmptyBorderPropertySource(ComplexProperty<EmptyBorder> complexProperty)
		{
			super(complexProperty);
			EmptyBorder border = complexProperty.getValue();
			insetPropertySource = new InsetsPropertySource(new ComplexProperty<Insets>(border.getBorderInsets()));
		}

		@Override
		public IPropertyDescriptor[] createPropertyDescriptors()
		{
			IPropertyDescriptor[] insetsPropertyDescriptors = insetPropertySource.getPropertyDescriptors();
			IPropertyDescriptor[] propertyDescriptors = new IPropertyDescriptor[insetsPropertyDescriptors.length];
			for (int i = 0; i < insetsPropertyDescriptors.length; i++)
			{
				propertyDescriptors[i] = new DelegatePropertyController(insetsPropertyDescriptors[i], INSETS + insetsPropertyDescriptors[i].getId());
			}
			// make sure sub-properties are sorted in defined order
			return PropertyController.applySequencePropertyComparator(propertyDescriptors);
		}

		@Override
		public Object getPropertyValue(Object id)
		{
			if (id instanceof String && ((String)id).startsWith(INSETS))
			{
				return insetPropertySource.getPropertyValue(((String)id).substring(INSETS.length()));
			}
			return null;
		}

		@Override
		protected EmptyBorder setComplexPropertyValue(Object id, Object v)
		{
			if (id instanceof String && ((String)id).startsWith(INSETS))
			{
				Insets insets = insetPropertySource.setComplexPropertyValue(((String)id).substring(INSETS.length()), v);
				return new EmptyBorder(insets);
			}
			return null;
		}

		@Override
		public Object resetComplexPropertyValue(Object id)
		{
			EmptyBorder defVal = (EmptyBorder)getDefaultBorderValuesMap().get(BorderType.Empty);
			if (id instanceof String && ((String)id).contains("top")) //$NON-NLS-1$
			{
				return defVal.getBorderInsets().top;
			}
			if (id instanceof String && ((String)id).contains("bottom")) //$NON-NLS-1$
			{
				return defVal.getBorderInsets().bottom;
			}
			if (id instanceof String && ((String)id).contains("left")) //$NON-NLS-1$
			{
				return defVal.getBorderInsets().left;
			}
			if (id instanceof String && ((String)id).contains("right")) //$NON-NLS-1$
			{
				return defVal.getBorderInsets().right;
			}
			return null;
		}
	}

	public static class MatteBorderPropertySource extends ComplexPropertySource<MatteBorder>
	{
		private static final String INSETS = "insets.";
		private static final String COLOR = "color";

		// delegate to InsetsPropertySource and ColorPropertyController
		InsetsPropertySource insetPropertySource;


		public MatteBorderPropertySource(ComplexProperty<MatteBorder> complexProperty)
		{
			super(complexProperty);
			MatteBorder border = complexProperty.getValue();
			insetPropertySource = new InsetsPropertySource(new ComplexProperty<Insets>(border.getBorderInsets()));
		}

		@Override
		public IPropertyDescriptor[] createPropertyDescriptors()
		{
			IPropertyDescriptor[] insetsPropertyDescriptors = insetPropertySource.getPropertyDescriptors();
			IPropertyDescriptor[] propertyDescriptors = new IPropertyDescriptor[insetsPropertyDescriptors.length + 1];
			int n = 0;
			for (int i = 0; i < insetsPropertyDescriptors.length; i++, n++)
			{
				propertyDescriptors[n] = new DelegatePropertyController(insetsPropertyDescriptors[i], INSETS + insetsPropertyDescriptors[i].getId());
			}
			propertyDescriptors[n++] = new ColorPropertyController(COLOR, "color");
			// make sure sub-properties are sorted in defined order
			return PropertyController.applySequencePropertyComparator(propertyDescriptors);
		}

		@Override
		public Object getPropertyValue(Object id)
		{
			if (COLOR.equals(id))
			{
				MatteBorder border = getEditableValue();
				return ColorPropertyController.PROPERTY_COLOR_CONVERTER.convertProperty(id, border.getMatteColor());
			}
			if (id instanceof String && ((String)id).startsWith(INSETS))
			{
				return insetPropertySource.getPropertyValue(((String)id).substring(INSETS.length()));
			}
			return null;
		}

		@Override
		protected MatteBorder setComplexPropertyValue(Object id, Object v)
		{
			MatteBorder border = getEditableValue();
			if (border == null)
			{
				border = (MatteBorder)createBorder(BorderType.Matte);
			}

			if (COLOR.equals(id))
			{
				return new MatteBorder(border.getBorderInsets(), ColorPropertyController.PROPERTY_COLOR_CONVERTER.convertValue(id, (String)v));
			}
			if (id instanceof String && ((String)id).startsWith(INSETS))
			{
				Insets insets = insetPropertySource.setComplexPropertyValue(((String)id).substring(INSETS.length()), v);
				return new MatteBorder(insets, border.getMatteColor());
			}
			return null;
		}

		@Override
		public Object resetComplexPropertyValue(Object id)
		{
			MatteBorder defVal = (MatteBorder)getDefaultBorderValuesMap().get(BorderType.Matte);
			if (COLOR.equals(id))
			{
				return ColorPropertyController.PROPERTY_COLOR_CONVERTER.convertProperty(id, defVal.getMatteColor());
			}
			if (id instanceof String && ((String)id).contains("top")) //$NON-NLS-1$
			{
				return defVal.getBorderInsets().top;
			}
			if (id instanceof String && ((String)id).contains("bottom")) //$NON-NLS-1$
			{
				return defVal.getBorderInsets().bottom;
			}
			if (id instanceof String && ((String)id).contains("left")) //$NON-NLS-1$
			{
				return defVal.getBorderInsets().left;
			}
			if (id instanceof String && ((String)id).contains("right")) //$NON-NLS-1$
			{
				return defVal.getBorderInsets().right;
			}
			return null;
		}
	}

	public static class BorderStylesPropertySource extends ComplexPropertySource<String[]>
	{
		private static final String TOP_STYLE = "top_style";
		private static final String LEFT_STYLE = "left_style";
		private static final String BOTTOM_STYLE = "bottom_style";
		private static final String RIGHT_STYLE = "right_style";
		protected static final List<String> borderStyles = Arrays.asList(IStyleSheet.BORDER_STYLES);

		public BorderStylesPropertySource(ComplexProperty<String[]> complexProperty)
		{
			super(complexProperty);
		}

		private static final IPropertyDescriptor[] PROPERTY_DESCRIPTORS = PropertyController.applySequencePropertyComparator(new IPropertyDescriptor[] {//
		new ComboBoxPropertyDescriptor(TOP_STYLE, "top style", IStyleSheet.BORDER_STYLES), //
		new ComboBoxPropertyDescriptor(LEFT_STYLE, "left style", IStyleSheet.BORDER_STYLES),//
		new ComboBoxPropertyDescriptor(BOTTOM_STYLE, "bottom style", IStyleSheet.BORDER_STYLES),//
		new ComboBoxPropertyDescriptor(RIGHT_STYLE, "right style", IStyleSheet.BORDER_STYLES),//
		});

		@Override
		public IPropertyDescriptor[] createPropertyDescriptors()
		{
			return PROPERTY_DESCRIPTORS;
		}

		@Override
		public Object getPropertyValue(Object id)
		{
			String[] style = getEditableValue();
			if (TOP_STYLE.equals(id))
			{
				return Integer.valueOf(borderStyles.indexOf(style[0]));
			}
			if (LEFT_STYLE.equals(id))
			{
				return Integer.valueOf(borderStyles.indexOf(style[1]));
			}
			if (BOTTOM_STYLE.equals(id))
			{
				return Integer.valueOf(borderStyles.indexOf(style[2]));
			}
			if (RIGHT_STYLE.equals(id))
			{
				return Integer.valueOf(borderStyles.indexOf(style[3]));
			}

			return null;
		}

		@Override
		protected String[] setComplexPropertyValue(Object id, Object v)
		{
			String[] styles = getEditableValue();
			if (styles == null)
			{
				styles = new String[4];
			}
			if (TOP_STYLE.equals(id))
			{
				styles[0] = borderStyles.get(Utils.getAsInteger(v));
			}
			if (LEFT_STYLE.equals(id))
			{
				styles[1] = borderStyles.get(Utils.getAsInteger(v));
			}
			if (BOTTOM_STYLE.equals(id))
			{
				styles[2] = borderStyles.get(Utils.getAsInteger(v));
			}
			if (RIGHT_STYLE.equals(id))
			{
				styles[3] = borderStyles.get(Utils.getAsInteger(v));
			}
			return styles;
		}

		@Override
		public Object resetComplexPropertyValue(Object id)
		{
			return "solid"; //$NON-NLS-1$
		}
	}

	public static class RoundedRadiusPropertySource extends ComplexPropertySource<float[]>
	{
		private static final String TOP_LEFT_HORIZONTAL = "top_left_width";
		private static final String TOP_RIGHT_HORIZONTAL = "top_right_width";
		private static final String BOTTOM_RIGHT_HORIZONTAL = "bottom_right_width";
		private static final String BOTTOM_LEFT_HORIZONTAL = "bottom_left_width";
		private static final String TOP_LEFT_VERTICAL = "top_left_height";
		private static final String TOP_RIGHT_VERTICAL = "top_right_height";
		private static final String BOTTOM_RIGHT_VERTICAL = "bottom_right_height";
		private static final String BOTTOM_LEFT_VERTICAL = "bottom_left_height";

		public RoundedRadiusPropertySource(ComplexProperty<float[]> complexProperty)
		{
			super(complexProperty);
		}

		private static final IPropertyDescriptor[] PROPERTY_DESCRIPTORS = PropertyController.applySequencePropertyComparator(new IPropertyDescriptor[] {//
		new NumberTypePropertyDescriptor(NumberCellEditor.FLOAT, TOP_LEFT_HORIZONTAL, "top left horizontal"), //
		new NumberTypePropertyDescriptor(NumberCellEditor.FLOAT, TOP_RIGHT_HORIZONTAL, "top right horizontal"),//
		new NumberTypePropertyDescriptor(NumberCellEditor.FLOAT, BOTTOM_RIGHT_HORIZONTAL, "bottom right horizontal"),//
		new NumberTypePropertyDescriptor(NumberCellEditor.FLOAT, BOTTOM_LEFT_HORIZONTAL, "bottom left horizontal"),//
		new NumberTypePropertyDescriptor(NumberCellEditor.FLOAT, TOP_LEFT_VERTICAL, "top left vertical"), //
		new NumberTypePropertyDescriptor(NumberCellEditor.FLOAT, TOP_RIGHT_VERTICAL, "top right vertical"),//
		new NumberTypePropertyDescriptor(NumberCellEditor.FLOAT, BOTTOM_RIGHT_VERTICAL, "bottom right vertical"),//
		new NumberTypePropertyDescriptor(NumberCellEditor.FLOAT, BOTTOM_LEFT_VERTICAL, "bottom left vertical") //
		});

		@Override
		public IPropertyDescriptor[] createPropertyDescriptors()
		{
			return PROPERTY_DESCRIPTORS;
		}

		@Override
		public Object getPropertyValue(Object id)
		{
			float[] radius = getEditableValue();
			if (TOP_LEFT_HORIZONTAL.equals(id))
			{
				return radius[0];
			}
			if (TOP_RIGHT_HORIZONTAL.equals(id))
			{
				return radius[1];
			}
			if (BOTTOM_RIGHT_HORIZONTAL.equals(id))
			{
				return radius[2];
			}
			if (BOTTOM_LEFT_HORIZONTAL.equals(id))
			{
				return radius[3];
			}
			if (TOP_LEFT_VERTICAL.equals(id))
			{
				return radius[4];
			}
			if (TOP_RIGHT_VERTICAL.equals(id))
			{
				return radius[5];
			}
			if (BOTTOM_RIGHT_VERTICAL.equals(id))
			{
				return radius[6];
			}
			if (BOTTOM_LEFT_VERTICAL.equals(id))
			{
				return radius[7];
			}

			return null;
		}

		@Override
		protected float[] setComplexPropertyValue(Object id, Object v)
		{
			float[] radius = getEditableValue();
			if (radius == null)
			{
				radius = new float[] { 0, 0, 0, 0, 0, 0, 0, 0 };
			}
			if (TOP_LEFT_HORIZONTAL.equals(id))
			{
				radius[0] = ((Float)v).floatValue();
			}
			if (TOP_RIGHT_HORIZONTAL.equals(id))
			{
				radius[1] = ((Float)v).floatValue();
			}
			if (BOTTOM_RIGHT_HORIZONTAL.equals(id))
			{
				radius[2] = ((Float)v).floatValue();
			}
			if (BOTTOM_LEFT_HORIZONTAL.equals(id))
			{
				radius[3] = ((Float)v).floatValue();
			}
			if (TOP_LEFT_VERTICAL.equals(id))
			{
				radius[4] = ((Float)v).floatValue();
			}
			if (TOP_RIGHT_VERTICAL.equals(id))
			{
				radius[5] = ((Float)v).floatValue();
			}
			if (BOTTOM_RIGHT_VERTICAL.equals(id))
			{
				radius[6] = ((Float)v).floatValue();
			}
			if (BOTTOM_LEFT_VERTICAL.equals(id))
			{
				radius[7] = ((Float)v).floatValue();
			}
			return radius;
		}

		@Override
		public Object resetComplexPropertyValue(Object id)
		{
			return 0;
		}
	}

	public static class RoundedBorderPropertySource extends SpecialMatteBorderPropertySource
	{
		private static final IObjectTextConverter converter = new IObjectTextConverter()
		{

			public Object convertToObject(String value)
			{
				if (value == null)
				{
					return null;
				}
				StringTokenizer tok = new StringTokenizer(value, ",");
				float[] values = new float[] { 0, 0, 0, 0, 0, 0, 0, 0 };
				try
				{
					if (tok.countTokens() == 1)
					{
						Arrays.fill(values, Integer.parseInt(tok.nextToken().trim()));
					}
					else if (tok.countTokens() == 4)
					{
						for (int index = 0; index < 4; index++)
						{
							values[index] = values[index + 4] = Integer.parseInt(tok.nextToken().trim());
						}
					}
					else
					{
						for (int index = 0; index < 8; index++)
						{
							values[index] = Integer.parseInt(tok.nextToken().trim());
						}
					}
				}
				catch (NumberFormatException e)
				{
					ServoyLog.logError(e);
					return null;
				}
				return values;
			}

			public String convertToString(Object value)
			{
				if (value == null)
				{
					return "";
				}
				StringBuilder buf = new StringBuilder();
				float[] values = (float[])value;
				float[] templateValue = new float[8];
				Arrays.fill(templateValue, values[0]);
				if (Arrays.equals(values, templateValue)) return String.valueOf(values[0]);
				for (int i = 0; i < templateValue.length; i++)
				{
					templateValue[i] = values[i % 4];
				}
				if (Arrays.equals(values, templateValue))
				{
					return new StringBuilder().append(values[0]).append(",").append(values[1]).append(",").append(values[2]).append(",").append(values[3]).toString();
				}
				for (int i = 0; i < values.length; i++)
				{
					buf.append(values[i]);
					if (i != values.length - 1)
					{
						buf.append(",");
					}
				}
				return buf.toString();
			}

			public String isCorrectString(String value)
			{
				if (value != null && value.trim().length() > 0 && convertToObject(value) == null)
				{
					return "Expecting 1, 4 or 8 numbers.";
				}
				return null;
			}

			public String isCorrectObject(Object value)
			{
				if (value == null || (value instanceof float[]))
				{
					return null;
				}
				return "Object is not an array of numbers.";
			}
		};
		private static final ComplexPropertyConverter<float[]> complexConverter = new ComplexPropertyConverter<float[]>()
		{
			@Override
			public Object convertProperty(Object property, float[] value)
			{
				return new ComplexProperty<float[]>(value)
				{
					@Override
					public IPropertySource getPropertySource()
					{
						return new RoundedRadiusPropertySource(this);
					}
				};
			}
		};
		private static PropertyController<float[], Object> radiusController = new PropertyController<float[], Object>(ROUNDING_RADIUS, "rounding radius",
			complexConverter, new LabelProvider()
			{

				@Override
				public String getText(Object element)
				{
					return converter.convertToString(element);
				}
			}, new ICellEditorFactory()
			{
				public CellEditor createPropertyEditor(Composite parent)
				{
					return new ConvertorObjectCellEditor(parent, converter);
				}
			});
		private static final IObjectTextConverter converterStyles = new IObjectTextConverter()
		{

			public Object convertToObject(String value)
			{
				if (value == null)
				{
					return null;
				}
				StringTokenizer tok = new StringTokenizer(value, ",");
				String[] values = new String[4];
				if (tok.countTokens() != 4)
				{
					Arrays.fill(values, tok.nextToken());
				}
				else
				{
					for (int index = 0; index < 4; index++)
					{
						values[index] = tok.nextToken();
					}
				}
				for (int i = 0; i < 4; i++)
				{
					if (!BorderStylesPropertySource.borderStyles.contains(values[i]))
					{
						return null;
					}
				}
				return values;
			}

			public String convertToString(Object value)
			{
				if (value == null)
				{
					return "";
				}
				String[] values = (String[])value;
				return new StringBuilder().append(values[0]).append(",").append(values[1]).append(",").append(values[2]).append(",").append(values[3]).toString();
			}

			public String isCorrectString(String value)
			{
				if (value != null && value.trim().length() > 0 && convertToObject(value) == null)
				{
					return "Expecting 1 or 4 border styles.";
				}
				return null;
			}

			public String isCorrectObject(Object value)
			{
				if (value == null || (value instanceof String[]))
				{
					return null;
				}
				return "Object is not an array of styles.";
			}
		};
		private static final ComplexPropertyConverter<String[]> complexStyleConverter = new ComplexPropertyConverter<String[]>()
		{
			@Override
			public Object convertProperty(Object property, String[] value)
			{
				return new ComplexProperty<String[]>(value)
				{
					@Override
					public IPropertySource getPropertySource()
					{
						return new BorderStylesPropertySource(this);
					}
				};
			}
		};
		private static PropertyController<String[], Object> stylesController = new PropertyController<String[], Object>(BORDER_STYLE, "border style",
			complexStyleConverter, new LabelProvider()
			{

				@Override
				public String getText(Object element)
				{
					return converterStyles.convertToString(element);
				}
			}, new ICellEditorFactory()
			{
				public CellEditor createPropertyEditor(Composite parent)
				{
					return new ConvertorObjectCellEditor(parent, converterStyles);
				}
			});

		public RoundedBorderPropertySource(ComplexProperty<SpecialMatteBorder> complexProperty)
		{
			super(complexProperty);
		}

		private IPropertyDescriptor[] descriptors = null;

		@Override
		public IPropertyDescriptor[] createPropertyDescriptors()
		{
			if (descriptors == null)
			{
				IPropertyDescriptor[] oldDescriptors = super.createPropertyDescriptors();
				IPropertyDescriptor[] fixedDescriptors = new IPropertyDescriptor[oldDescriptors.length];
				System.arraycopy(oldDescriptors, 0, fixedDescriptors, 0, oldDescriptors.length);
				fixedDescriptors[fixedDescriptors.length - 2] = radiusController;
				fixedDescriptors[fixedDescriptors.length - 1] = stylesController;
				descriptors = PropertyController.applySequencePropertyComparator(fixedDescriptors);
			}
			return descriptors;
		}

		@Override
		public Object getPropertyValue(Object id)
		{
			SpecialMatteBorder border = getEditableValue();
			if (ROUNDING_RADIUS.equals(id))
			{
				return complexConverter.convertProperty(id, ((RoundedBorder)border).getRadius());
			}
			if (BORDER_STYLE.equals(id))
			{
				return complexStyleConverter.convertProperty(id, ((RoundedBorder)border).getBorderStyles());
			}
			return super.getPropertyValue(id);
		}

		@Override
		protected RoundedBorder setComplexPropertyValue(Object id, Object v)
		{
			RoundedBorder border = (RoundedBorder)getEditableValue();
			if (border == null)
			{
				border = (RoundedBorder)createBorder(BorderType.RoundedWebBorder);
			}

			SpecialMatteBorder smb = super.setComplexPropertyValue(id, v);
			RoundedBorder roundedBorder = new RoundedBorder(smb.getTop(), smb.getLeft(), smb.getBottom(), smb.getRight(), smb.getTopColor(),
				smb.getLeftColor(), smb.getBottomColor(), smb.getRightColor());
			if (ROUNDING_RADIUS.equals(id))
			{
				if (v instanceof float[])
				{
					roundedBorder.setRoundingRadius((float[])v);
				}
				else
				{
					roundedBorder.setRoundingRadius(((ComplexProperty<float[]>)v).getValue());
				}
				roundedBorder.setBorderStyles(border.getBorderStyles());
			}
			else if (BORDER_STYLE.equals(id))
			{
				if (v instanceof String[])
				{
					roundedBorder.setBorderStyles((String[])v);
				}
				else
				{
					roundedBorder.setBorderStyles(((ComplexProperty<String[]>)v).getValue());
				}
				roundedBorder.setRoundingRadius(border.getRadius());
			}
			else
			{
				roundedBorder.setBorderStyles(border.getBorderStyles());
				roundedBorder.setRoundingRadius(border.getRadius());
			}
			return roundedBorder;
		}

		@Override
		public Object resetComplexPropertyValue(Object id)
		{
			RoundedBorder defVal = (RoundedBorder)getDefaultBorderValuesMap().get(BorderType.RoundedWebBorder);
			if (ROUNDING_RADIUS.equals(id))
			{
				return defVal.getRadius();
			}
			else if (BORDER_STYLE.equals(id))
			{
				return defVal.getBorderStyles();
			}
			return super.resetComplexPropertyValue(id);
		}
	}
	public static class SpecialMatteBorderPropertySource extends ComplexPropertySource<SpecialMatteBorder>
	{
		private static final ComplexPropertyConverter<Insets> sizeConverter = new ComplexPropertyConverter<java.awt.Insets>()
		{
			@Override
			public Object convertProperty(Object property, java.awt.Insets value)
			{
				return new ComplexProperty<Insets>(value)
				{
					@Override
					public IPropertySource getPropertySource()
					{
						return new InsetsPropertySource(this);
					}
				};
			}
		};

		private static final String WIDTH = "width";

		private static PropertyController<java.awt.Insets, Object> sizeController = new PropertyController<java.awt.Insets, Object>(WIDTH, "width",
			sizeConverter, InsetsPropertySource.getLabelProvider(), new ICellEditorFactory()
			{
				public CellEditor createPropertyEditor(Composite parent)
				{
					return InsetsPropertySource.createPropertyEditor(parent);
				}
			});

		private static final String TOP_COLOR = "top_color";
		private static final String LEFT_COLOR = "left_color";
		private static final String BOTTOM_COLOR = "bottom_color";
		private static final String RIGHT_COLOR = "right_color";

		protected static final String ROUNDING_RADIUS = "rounding_radius";
		protected static final String BORDER_STYLE = "dash_pattern";

		// make sure sub-properties are sorted in defined order
		private static final IPropertyDescriptor[] PROPERTY_DESCRIPTORS = PropertyController.applySequencePropertyComparator(new IPropertyDescriptor[] {//
		sizeController, //
		new ColorPropertyController(TOP_COLOR, "top color"), //
		new ColorPropertyController(LEFT_COLOR, "left color"),//
		new ColorPropertyController(BOTTOM_COLOR, "bottom color"),//
		new ColorPropertyController(RIGHT_COLOR, "right color"),//
		new NumberTypePropertyDescriptor(NumberCellEditor.FLOAT, ROUNDING_RADIUS, "rounding radius"),//
		new TextPropertyDescriptor(BORDER_STYLE, "dash pattern") //
		});

		public SpecialMatteBorderPropertySource(ComplexProperty<SpecialMatteBorder> complexProperty)
		{
			super(complexProperty);
		}

		@Override
		public IPropertyDescriptor[] createPropertyDescriptors()
		{
			return PROPERTY_DESCRIPTORS;
		}

		@Override
		public Object getPropertyValue(Object id)
		{
			SpecialMatteBorder border = getEditableValue();
			if (WIDTH.equals(id))
			{
				return sizeConverter.convertProperty(id,
					new Insets((int)border.getTop(), (int)border.getLeft(), (int)border.getBottom(), (int)border.getRight()));
			}
			if (TOP_COLOR.equals(id))
			{
				return ColorPropertyController.PROPERTY_COLOR_CONVERTER.convertProperty(id, border.getTopColor());
			}
			if (LEFT_COLOR.equals(id))
			{
				return ColorPropertyController.PROPERTY_COLOR_CONVERTER.convertProperty(id, border.getLeftColor());
			}
			if (BOTTOM_COLOR.equals(id))
			{
				return ColorPropertyController.PROPERTY_COLOR_CONVERTER.convertProperty(id, border.getBottomColor());
			}
			if (RIGHT_COLOR.equals(id))
			{
				return ColorPropertyController.PROPERTY_COLOR_CONVERTER.convertProperty(id, border.getRightColor());
			}
			if (ROUNDING_RADIUS.equals(id))
			{
				return new Float(border.getRoundingRadius());
			}
			if (BORDER_STYLE.equals(id))
			{
				return SpecialMatteBorder.createDashString(border.getDashPattern());
			}

			return null;
		}

		@Override
		protected SpecialMatteBorder setComplexPropertyValue(Object id, Object v)
		{
			SpecialMatteBorder border = getEditableValue();
			if (border == null)
			{
				border = (SpecialMatteBorder)createBorder(BorderType.SpecialMatte);
			}

			SpecialMatteBorder smb = null;
			if (WIDTH.equals(id))
			{
				Insets insets = null;
				if (v instanceof Insets)
				{
					insets = (Insets)v;
				}
				else
				{
					insets = ((ComplexProperty<Insets>)v).getValue();
				}
				smb = new SpecialMatteBorder(insets.top, insets.left, insets.bottom, insets.right, border.getTopColor(), border.getLeftColor(),
					border.getBottomColor(), border.getRightColor());
			}
			else if (TOP_COLOR.equals(id))
			{
				smb = new SpecialMatteBorder(border.getTop(), border.getLeft(), border.getBottom(), border.getRight(),
					ColorPropertyController.PROPERTY_COLOR_CONVERTER.convertValue(id, (String)v), border.getLeftColor(), border.getBottomColor(),
					border.getRightColor());
			}
			else if (LEFT_COLOR.equals(id))
			{
				smb = new SpecialMatteBorder(border.getTop(), border.getLeft(), border.getBottom(), border.getRight(), border.getTopColor(),
					ColorPropertyController.PROPERTY_COLOR_CONVERTER.convertValue(id, (String)v), border.getBottomColor(), border.getRightColor());
			}
			else if (BOTTOM_COLOR.equals(id))
			{
				smb = new SpecialMatteBorder(border.getTop(), border.getLeft(), border.getBottom(), border.getRight(), border.getTopColor(),
					border.getLeftColor(), ColorPropertyController.PROPERTY_COLOR_CONVERTER.convertValue(id, (String)v), border.getRightColor());
			}
			else if (RIGHT_COLOR.equals(id))
			{
				smb = new SpecialMatteBorder(border.getTop(), border.getLeft(), border.getBottom(), border.getRight(), border.getTopColor(),
					border.getLeftColor(), border.getBottomColor(), ColorPropertyController.PROPERTY_COLOR_CONVERTER.convertValue(id, (String)v));
			}

			if (smb != null)
			{ // these are not set in the constructor
				smb.setRoundingRadius(border.getRoundingRadius());
				smb.setDashPattern(border.getDashPattern());
			}

			else if (ROUNDING_RADIUS.equals(id))
			{
				smb = new SpecialMatteBorder(border.getTop(), border.getLeft(), border.getBottom(), border.getRight(), border.getTopColor(),
					border.getLeftColor(), border.getBottomColor(), border.getRightColor());
				smb.setDashPattern(border.getDashPattern());
				if (v instanceof Number) smb.setRoundingRadius(((Number)v).floatValue());
			}
			else if (BORDER_STYLE.equals(id))
			{
				smb = new SpecialMatteBorder(border.getTop(), border.getLeft(), border.getBottom(), border.getRight(), border.getTopColor(),
					border.getLeftColor(), border.getBottomColor(), border.getRightColor());
				smb.setRoundingRadius(border.getRoundingRadius());
				if (v instanceof String) smb.setDashPattern(SpecialMatteBorder.createDash((String)v));
			}
			return smb;
		}

		@Override
		public Object resetComplexPropertyValue(Object id)
		{
			SpecialMatteBorder defVal = (SpecialMatteBorder)getDefaultBorderValuesMap().get(BorderType.SpecialMatte);
			if (WIDTH.equals(id))
			{
				return new Insets((int)defVal.getTop(), (int)defVal.getLeft(), (int)defVal.getBottom(), (int)defVal.getRight());
			}
			else if (TOP_COLOR.equals(id))
			{
				return ColorPropertyController.PROPERTY_COLOR_CONVERTER.convertProperty(id, defVal.getTopColor());
			}
			else if (LEFT_COLOR.equals(id))
			{
				return ColorPropertyController.PROPERTY_COLOR_CONVERTER.convertProperty(id, defVal.getLeftColor());
			}
			else if (BOTTOM_COLOR.equals(id))
			{
				return ColorPropertyController.PROPERTY_COLOR_CONVERTER.convertProperty(id, defVal.getBottomColor());
			}
			else if (RIGHT_COLOR.equals(id))
			{
				return ColorPropertyController.PROPERTY_COLOR_CONVERTER.convertProperty(id, defVal.getRightColor());
			}
			else if (ROUNDING_RADIUS.equals(id))
			{
				return defVal.getRoundingRadius();
			}
			else if (BORDER_STYLE.equals(id))
			{
				return defVal.getDashPattern();
			}
			return null;
		}
	}

	public static class EtchedBorderPropertySource extends ComplexPropertySource<EtchedBorder>
	{
		private static final String HIGHLIGHT = "highlight";
		private static final String SHADOW = "shadow";
		private static final String RAISED = "raised";

		// make sure sub-properties are sorted in defined order
		private static final IPropertyDescriptor[] PROPERTY_DESCRIPTORS = PropertyController.applySequencePropertyComparator(new IPropertyDescriptor[] {//
		new ColorPropertyController(HIGHLIGHT, "highlight color"), //
		new ColorPropertyController(SHADOW, "shadow color"), //
		new CheckboxPropertyDescriptor(RAISED, "raised") //
		});

		// delegate to InsetsPropertySource and ColorPropertyController


		public EtchedBorderPropertySource(ComplexProperty<EtchedBorder> complexProperty)
		{
			super(complexProperty);
		}

		@Override
		public IPropertyDescriptor[] createPropertyDescriptors()
		{
			return PROPERTY_DESCRIPTORS;
		}

		@Override
		public Object getPropertyValue(Object id)
		{
			EtchedBorder border = getEditableValue();
			if (HIGHLIGHT.equals(id))
			{
				return ColorPropertyController.PROPERTY_COLOR_CONVERTER.convertProperty(id, border.getHighlightColor());
			}
			if (SHADOW.equals(id))
			{
				return ColorPropertyController.PROPERTY_COLOR_CONVERTER.convertProperty(id, border.getShadowColor());
			}
			if (RAISED.equals(id))
			{
				return border.getEtchType() == EtchedBorder.RAISED ? Boolean.TRUE : Boolean.FALSE;
			}
			return null;
		}

		@Override
		protected EtchedBorder setComplexPropertyValue(Object id, Object v)
		{
			EtchedBorder border = getEditableValue();
			if (border == null)
			{
				border = (EtchedBorder)createBorder(BorderType.Etched);
			}

			if (HIGHLIGHT.equals(id))
			{
				return new EtchedBorder(border.getEtchType(), ColorPropertyController.PROPERTY_COLOR_CONVERTER.convertValue(id, (String)v),
					border.getShadowColor());
			}
			if (SHADOW.equals(id))
			{
				return new EtchedBorder(border.getEtchType(), border.getHighlightColor(), ColorPropertyController.PROPERTY_COLOR_CONVERTER.convertValue(id,
					(String)v));
			}
			if (RAISED.equals(id))
			{
				return new EtchedBorder(Boolean.FALSE.equals(v) ? EtchedBorder.LOWERED : EtchedBorder.RAISED, border.getHighlightColor(),
					border.getShadowColor());
			}
			return null;
		}
	}

	public static class BevelBorderPropertySource extends ComplexPropertySource<BevelBorder>
	{
		private static final String HIGHLIGHT_INNER = "highlight_inner";
		private static final String SHADOW_OUTER = "shadow_outer";
		private static final String RAISED = "raised";

		// make sure sub-properties are sorted in defined order
		private static final IPropertyDescriptor[] PROPERTY_DESCRIPTORS = PropertyController.applySequencePropertyComparator(new IPropertyDescriptor[] {//
		new ColorPropertyController(HIGHLIGHT_INNER, "highlight color"),//
		new ColorPropertyController(SHADOW_OUTER, "shadow color"),//
		new CheckboxPropertyDescriptor(RAISED, "raised") //
		});

		public BevelBorderPropertySource(ComplexProperty<BevelBorder> complexProperty)
		{
			super(complexProperty);
		}

		@Override
		public IPropertyDescriptor[] createPropertyDescriptors()
		{
			return PROPERTY_DESCRIPTORS;
		}

		@Override
		public Object getPropertyValue(Object id)
		{
			BevelBorder border = getEditableValue();
			if (HIGHLIGHT_INNER.equals(id))
			{
				return ColorPropertyController.PROPERTY_COLOR_CONVERTER.convertProperty(id, border.getHighlightInnerColor());
			}
			if (SHADOW_OUTER.equals(id))
			{
				return ColorPropertyController.PROPERTY_COLOR_CONVERTER.convertProperty(id, border.getShadowOuterColor());
			}
			if (RAISED.equals(id))
			{
				return border.getBevelType() == BevelBorder.RAISED ? Boolean.TRUE : Boolean.FALSE;
			}
			return null;
		}

		@Override
		protected BevelBorder setComplexPropertyValue(Object id, Object v)
		{
			int type = BevelBorder.LOWERED;
			Color highlight = null;
			Color shadow = null;

			BevelBorder border = getEditableValue();
			if (border != null)
			{
				type = border.getBevelType();
				highlight = border.getHighlightInnerColor();
				shadow = border.getShadowOuterColor();
			}

			if (HIGHLIGHT_INNER.equals(id))
			{
				highlight = ColorPropertyController.PROPERTY_COLOR_CONVERTER.convertValue(id, (String)v);
			}
			if (SHADOW_OUTER.equals(id))
			{
				shadow = ColorPropertyController.PROPERTY_COLOR_CONVERTER.convertValue(id, (String)v);
			}
			if (RAISED.equals(id))
			{
				type = Boolean.FALSE.equals(v) ? BevelBorder.LOWERED : BevelBorder.RAISED;
			}
			if (highlight != null && shadow != null)
			{
				return (BevelBorder)BorderFactory.createBevelBorder(type, highlight, shadow);
			}
			if (highlight != null)
			{
				return (BevelBorder)BorderFactory.createBevelBorder(type, highlight.brighter(), highlight, null, null);
			}
			if (shadow != null)
			{
				return (BevelBorder)BorderFactory.createBevelBorder(type, null, null, shadow, shadow.brighter());
			}
			return (BevelBorder)BorderFactory.createBevelBorder(type);
		}
	}

	public static class LineBorderPropertySource extends ComplexPropertySource<LineBorder>
	{
		private static final String COLOR = "color";
		private static final String THICKNESS = "thickness";

		// make sure sub-properties are sorted in defined order
		private static final IPropertyDescriptor[] PROPERTY_DESCRIPTORS = PropertyController.applySequencePropertyComparator(new IPropertyDescriptor[] { //
		new ColorPropertyController(COLOR, "color"), //
		new NumberTypePropertyDescriptor(NumberCellEditor.INTEGER, THICKNESS, "thickness") //
		});

		public LineBorderPropertySource(ComplexProperty<LineBorder> complexProperty)
		{
			super(complexProperty);
		}

		@Override
		public IPropertyDescriptor[] createPropertyDescriptors()
		{
			return PROPERTY_DESCRIPTORS;
		}

		@Override
		public Object getPropertyValue(Object id)
		{
			LineBorder border = getEditableValue();
			if (COLOR.equals(id))
			{
				return ColorPropertyController.PROPERTY_COLOR_CONVERTER.convertProperty(id, border.getLineColor());
			}
			if (THICKNESS.equals(id))
			{
				return new Integer(border.getThickness());
			}
			return null;
		}

		@Override
		protected LineBorder setComplexPropertyValue(Object id, Object v)
		{
			LineBorder border = getEditableValue();
			if (border == null)
			{
				border = (LineBorder)createBorder(BorderType.Line);
			}

			if (COLOR.equals(id))
			{
				return new LineBorder(ColorPropertyController.PROPERTY_COLOR_CONVERTER.convertValue(id, (String)v), border.getThickness());
			}
			if (THICKNESS.equals(id))
			{
				return new LineBorder(border.getLineColor(), ((Integer)v).intValue());
			}
			return null;
		}

		@Override
		public Object resetComplexPropertyValue(Object id)
		{
			LineBorder defVal = (LineBorder)getDefaultBorderValuesMap().get(BorderType.Line);
			if (COLOR.equals(id))
			{
				//even though this worked (default color work with null values); was added for the sake of completeness
				return ColorPropertyController.PROPERTY_COLOR_CONVERTER.convertProperty(id, defVal.getLineColor());
			}
			if (THICKNESS.equals(id))
			{
				return defVal.getThickness();
			}
			return null;
		}
	}

	public static class TitledBorderPropertySource extends ComplexPropertySource<TitledBorder>
	{
		private static final String TITLE = "title";
		private static final String JUSTIFICATION = "justification";
		private static final String POSITION = "position";
		private static final String FONT = "font";
		private static final String COLOR = "color";

		private static final IPropertyController<Integer, Integer> JUSTIFICATION_CONTROLLER = new ComboboxPropertyController<Integer>(
			JUSTIFICATION,
			"justification",
			new ComboboxPropertyModel<Integer>(
				new Integer[] { new Integer(TitledBorder.DEFAULT_JUSTIFICATION), new Integer(TitledBorder.LEFT), new Integer(TitledBorder.CENTER), new Integer(
					TitledBorder.RIGHT), new Integer(TitledBorder.LEADING), new Integer(TitledBorder.TRAILING) },
				new String[] { Messages.LabelDefault, Messages.AlignLeft, Messages.AlignCenter, Messages.AlignRight, Messages.JustifyLeading, Messages.JustifyTrailing }),
			Messages.LabelUnresolved);

		private static final IPropertyController<Integer, Integer> POSITION_CONTROLLER = new ComboboxPropertyController<Integer>(
			POSITION,
			"position",
			new ComboboxPropertyModel<Integer>(
				new Integer[] { new Integer(TitledBorder.DEFAULT_POSITION), new Integer(TitledBorder.ABOVE_TOP), new Integer(TitledBorder.TOP), new Integer(
					TitledBorder.BELOW_TOP), new Integer(TitledBorder.ABOVE_BOTTOM), new Integer(TitledBorder.BOTTOM), new Integer(TitledBorder.BELOW_BOTTOM) },
				new String[] { Messages.LabelDefault, Messages.PostionABOVETOP, Messages.PostionTOP, Messages.PostionBELOWTOP, Messages.PostionABOVEBOTTOM, Messages.PostionBOTTOM, Messages.PostionBELOWBOTTOM }),
			Messages.LabelUnresolved);

		private final PersistContext persistContext;
		private IPropertyDescriptor[] propertyDescriptors = null;

		public TitledBorderPropertySource(PersistContext persistContext, ComplexProperty<TitledBorder> complexProperty)
		{
			super(complexProperty);
			this.persistContext = persistContext;
		}

		@Override
		public IPropertyDescriptor[] createPropertyDescriptors()
		{
			if (propertyDescriptors == null)
			{
				try
				{
					final FlattenedSolution flattenedEditingSolution = ModelUtils.getEditingFlattenedSolution(persistContext.getPersist(),
						persistContext.getContext());
					final Form form = ModelUtils.isInheritedFormElement(persistContext.getPersist(), persistContext.getContext())
						? (Form)persistContext.getContext().getAncestor(IRepository.FORMS) : (Form)persistContext.getPersist().getAncestor(IRepository.FORMS);
					final Table table = form == null ? null : form.getTable();
					propertyDescriptors = new IPropertyDescriptor[] { new PropertyDescriptor(TITLE, "title text")
					{
						@Override
						public CellEditor createPropertyEditor(Composite parent)
						{
							return new TagsAndI18NTextCellEditor(parent, persistContext, flattenedEditingSolution, TextCutoffLabelProvider.DEFAULT, table,
								"Edit text property", Activator.getDefault().getDesignClient(), false);
						}

						@Override
						public ILabelProvider getLabelProvider()
						{
							return TextCutoffLabelProvider.DEFAULT;
						}
					},

					JUSTIFICATION_CONTROLLER, POSITION_CONTROLLER, new PropertyController<java.awt.Font, String>(FONT, "font", PropertyFontConverter.INSTANCE,
						FontLabelProvider.INSTANCE, new ICellEditorFactory()
						{
							public CellEditor createPropertyEditor(Composite parent)
							{
								return new FontCellEditor(parent, null);
							}
						}), new ColorPropertyController(COLOR, "color") };

					// make sure sub-properties are sorted in defined order
					propertyDescriptors = PropertyController.applySequencePropertyComparator(propertyDescriptors);
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError(e);
				}
			}
			return propertyDescriptors;
		}

		@Override
		public Object getPropertyValue(Object id)
		{
			TitledBorder border = getEditableValue();
			if (TITLE.equals(id))
			{
				return border.getTitle();
			}
			if (JUSTIFICATION.equals(id))
			{
				return JUSTIFICATION_CONTROLLER.getConverter().convertProperty(id, new Integer(border.getTitleJustification()));
			}
			if (POSITION.equals(id))
			{
				return POSITION_CONTROLLER.getConverter().convertProperty(id, new Integer(border.getTitlePosition()));
			}
			if (FONT.equals(id))
			{
				return PropertyFontConverter.INSTANCE.convertProperty(id, border.getTitleFont());
			}
			if (COLOR.equals(id))
			{
				return ColorPropertyController.PROPERTY_COLOR_CONVERTER.convertProperty(id, border.getTitleColor());
			}
			return null;
		}

		@Override
		protected TitledBorder setComplexPropertyValue(Object id, Object v)
		{
			TitledBorder border = getEditableValue();
			if (border == null)
			{
				border = (TitledBorder)createBorder(BorderType.Title);
			}

			if (TITLE.equals(id))
			{
				return new TitledBorder(null, (String)v, border.getTitleJustification(), border.getTitlePosition(), border.getTitleFont(),
					border.getTitleColor());
			}
			if (JUSTIFICATION.equals(id))
			{
				return new TitledBorder(null, border.getTitle(), JUSTIFICATION_CONTROLLER.getConverter().convertValue(id, (Integer)v).intValue(),
					border.getTitlePosition(), border.getTitleFont(), border.getTitleColor());
			}
			if (POSITION.equals(id))
			{
				return new TitledBorder(null, border.getTitle(), border.getTitleJustification(),
					POSITION_CONTROLLER.getConverter().convertValue(id, (Integer)v).intValue(), border.getTitleFont(), border.getTitleColor());
			}
			if (FONT.equals(id))
			{
				return new TitledBorder(null, border.getTitle(), border.getTitleJustification(), border.getTitlePosition(),
					PropertyFontConverter.INSTANCE.convertValue(id, (String)v), border.getTitleColor());
			}
			if (COLOR.equals(id))
			{
				return new TitledBorder(null, border.getTitle(), border.getTitleJustification(), border.getTitlePosition(), border.getTitleFont(),
					ColorPropertyController.PROPERTY_COLOR_CONVERTER.convertValue(id, (String)v));
			}
			return null;
		}

		@Override
		public Object resetComplexPropertyValue(@SuppressWarnings("unused")
		Object id)
		{
			TitledBorder defVal = (TitledBorder)getDefaultBorderValuesMap().get(BorderType.Title);
			if (TITLE.equals(id))
			{
				return defVal.getTitle();
			}
			if (JUSTIFICATION.equals(id))
			{
				return defVal.getTitleJustification();
			}
			if (POSITION.equals(id))
			{
				return defVal.getTitlePosition();
			}
			if (FONT.equals(id))
			{
				return PropertyFontConverter.INSTANCE.convertProperty(id, defVal.getTitleFont());
			}
			if (COLOR.equals(id))
			{
				return ColorPropertyController.PROPERTY_COLOR_CONVERTER.convertProperty(id, defVal.getTitleColor());
			}
			return null;
		}

	}
}
