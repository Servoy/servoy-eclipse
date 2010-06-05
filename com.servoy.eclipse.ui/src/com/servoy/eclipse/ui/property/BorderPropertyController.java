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

import javax.swing.BorderFactory;
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
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;
import org.eclipse.ui.views.properties.TextPropertyDescriptor;

import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.editors.FontCellEditor;
import com.servoy.eclipse.ui.editors.TagsAndI18NTextCellEditor;
import com.servoy.eclipse.ui.labelproviders.FontLabelProvider;
import com.servoy.eclipse.ui.labelproviders.TextCutoffLabelProvider;
import com.servoy.eclipse.ui.util.ModifiedComboBoxCellEditor;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.util.gui.SpecialMatteBorder;

public class BorderPropertyController extends PropertyController<Border, Object>
{
	public static enum BorderType
	{
		Default, Empty, Etched, Bevel, Line, Title, Matte, SpecialMatte
	}

	private final IPersist persist;

	final static ComboboxPropertyController<BorderType> comboboxController = new ComboboxPropertyController<BorderType>("BORDER_TYPE", "borderTypes",
		new ComboboxPropertyModel<BorderType>(BorderType.values()), Messages.LabelUnresolved);

	private static ILabelProvider labelProvider = null;

	/*
	 * Members
	 */
	private final IPropertySource propertySource;

	public BorderPropertyController(String id, String displayName, IPropertySource propertySource, IPersist persist)
	{
		super(id, displayName);
		this.propertySource = propertySource;
		this.persist = persist;
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
				propertySource = new TitledBorderPropertySource(persist, (ComplexProperty<TitledBorder>)complexProperty);
				break;
			case Matte :
				propertySource = new MatteBorderPropertySource((ComplexProperty<MatteBorder>)complexProperty);
				break;
			case SpecialMatte :
				propertySource = new SpecialMatteBorderPropertySource((ComplexProperty<SpecialMatteBorder>)complexProperty);
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
					return getBorderTypeConstant(border).name();
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
	 * @author rob
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
				return new MatteBorder(border.getBorderInsets(), ColorPropertyController.PROPERTY_COLOR_CONVERTER.convertValue(id, (RGB)v));
			}
			if (id instanceof String && ((String)id).startsWith(INSETS))
			{
				Insets insets = insetPropertySource.setComplexPropertyValue(((String)id).substring(INSETS.length()), v);
				return new MatteBorder(insets, border.getMatteColor());
			}
			return null;
		}
	}

	public static class SpecialMatteBorderPropertySource extends ComplexPropertySource<SpecialMatteBorder>
	{
		private static final String TOP_SIZE = "top_size";
		private static final String LEFT_SIZE = "left_size";
		private static final String BOTTOM_SIZE = "bottom_size";
		private static final String RIGHT_SIZE = "right_size";

		private static final String TOP_COLOR = "top_color";
		private static final String LEFT_COLOR = "left_color";
		private static final String BOTTOM_COLOR = "bottom_color";
		private static final String RIGHT_COLOR = "right_color";

		private static final String ROUNDING_RADIUS = "rounding_radius";
		private static final String DASH_PATTERN = "dash_pattern";

		// make sure sub-properties are sorted in defined order
		private static final IPropertyDescriptor[] PROPERTY_DESCRIPTORS = PropertyController.applySequencePropertyComparator(new IPropertyDescriptor[] {//
		new NumberTypePropertyDescriptor(NumberCellEditor.FLOAT, TOP_SIZE, "top size"), //
		new NumberTypePropertyDescriptor(NumberCellEditor.FLOAT, LEFT_SIZE, "left size"),//
		new NumberTypePropertyDescriptor(NumberCellEditor.FLOAT, BOTTOM_SIZE, "bottom size"),//
		new NumberTypePropertyDescriptor(NumberCellEditor.FLOAT, RIGHT_SIZE, "right size"),//
		new ColorPropertyController(TOP_COLOR, "top color"), //
		new ColorPropertyController(LEFT_COLOR, "left color"),//
		new ColorPropertyController(BOTTOM_COLOR, "bottom color"),//
		new ColorPropertyController(RIGHT_COLOR, "right color"),//
		new NumberTypePropertyDescriptor(NumberCellEditor.FLOAT, ROUNDING_RADIUS, "rounding radius"),//
		new TextPropertyDescriptor(DASH_PATTERN, "dash pattern") //
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
			if (TOP_SIZE.equals(id))
			{
				return new Float(border.getTop());
			}
			if (LEFT_SIZE.equals(id))
			{
				return new Float(border.getLeft());
			}
			if (BOTTOM_SIZE.equals(id))
			{
				return new Float(border.getBottom());
			}
			if (RIGHT_SIZE.equals(id))
			{
				return new Float(border.getRight());
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
			if (DASH_PATTERN.equals(id))
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
			if (TOP_SIZE.equals(id))
			{
				smb = new SpecialMatteBorder(((Number)v).floatValue(), border.getLeft(), border.getBottom(), border.getRight(), border.getTopColor(),
					border.getLeftColor(), border.getBottomColor(), border.getRightColor());
			}
			else if (LEFT_SIZE.equals(id))
			{
				smb = new SpecialMatteBorder(border.getTop(), ((Number)v).floatValue(), border.getBottom(), border.getRight(), border.getTopColor(),
					border.getLeftColor(), border.getBottomColor(), border.getRightColor());
			}
			else if (BOTTOM_SIZE.equals(id))
			{
				smb = new SpecialMatteBorder(border.getTop(), border.getLeft(), ((Number)v).floatValue(), border.getRight(), border.getTopColor(),
					border.getLeftColor(), border.getBottomColor(), border.getRightColor());
			}
			else if (RIGHT_SIZE.equals(id))
			{
				smb = new SpecialMatteBorder(border.getTop(), border.getLeft(), border.getBottom(), ((Number)v).floatValue(), border.getTopColor(),
					border.getLeftColor(), border.getBottomColor(), border.getRightColor());
			}
			else if (TOP_COLOR.equals(id))
			{
				smb = new SpecialMatteBorder(border.getTop(), border.getLeft(), border.getBottom(), border.getRight(),
					ColorPropertyController.PROPERTY_COLOR_CONVERTER.convertValue(id, (RGB)v), border.getLeftColor(), border.getBottomColor(),
					border.getRightColor());
			}
			else if (LEFT_COLOR.equals(id))
			{
				smb = new SpecialMatteBorder(border.getTop(), border.getLeft(), border.getBottom(), border.getRight(), border.getTopColor(),
					ColorPropertyController.PROPERTY_COLOR_CONVERTER.convertValue(id, (RGB)v), border.getBottomColor(), border.getRightColor());
			}
			else if (BOTTOM_COLOR.equals(id))
			{
				smb = new SpecialMatteBorder(border.getTop(), border.getLeft(), border.getBottom(), border.getRight(), border.getTopColor(),
					border.getLeftColor(), ColorPropertyController.PROPERTY_COLOR_CONVERTER.convertValue(id, (RGB)v), border.getRightColor());
			}
			else if (RIGHT_COLOR.equals(id))
			{
				smb = new SpecialMatteBorder(border.getTop(), border.getLeft(), border.getBottom(), border.getRight(), border.getTopColor(),
					border.getLeftColor(), border.getBottomColor(), ColorPropertyController.PROPERTY_COLOR_CONVERTER.convertValue(id, (RGB)v));
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
				smb.setRoundingRadius(((Number)v).floatValue());
			}
			else if (DASH_PATTERN.equals(id))
			{
				smb = new SpecialMatteBorder(border.getTop(), border.getLeft(), border.getBottom(), border.getRight(), border.getTopColor(),
					border.getLeftColor(), border.getBottomColor(), border.getRightColor());
				smb.setRoundingRadius(border.getRoundingRadius());
				smb.setDashPattern(SpecialMatteBorder.createDash((String)v));
			}
			return smb;
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
				return new EtchedBorder(border.getEtchType(), ColorPropertyController.PROPERTY_COLOR_CONVERTER.convertValue(id, (RGB)v),
					border.getShadowColor());
			}
			if (SHADOW.equals(id))
			{
				return new EtchedBorder(border.getEtchType(), border.getHighlightColor(), ColorPropertyController.PROPERTY_COLOR_CONVERTER.convertValue(id,
					(RGB)v));
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
				highlight = ColorPropertyController.PROPERTY_COLOR_CONVERTER.convertValue(id, (RGB)v);
			}
			if (SHADOW_OUTER.equals(id))
			{
				shadow = ColorPropertyController.PROPERTY_COLOR_CONVERTER.convertValue(id, (RGB)v);
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
				return new LineBorder(ColorPropertyController.PROPERTY_COLOR_CONVERTER.convertValue(id, (RGB)v), border.getThickness());
			}
			if (THICKNESS.equals(id))
			{
				return new LineBorder(border.getLineColor(), ((Integer)v).intValue());
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

		private final IPersist persist;
		private IPropertyDescriptor[] propertyDescriptors = null;

		public TitledBorderPropertySource(IPersist persist, ComplexProperty<TitledBorder> complexProperty)
		{
			super(complexProperty);
			this.persist = persist;
		}

		@Override
		public IPropertyDescriptor[] createPropertyDescriptors()
		{
			if (propertyDescriptors == null)
			{
				try
				{
					final FlattenedSolution flattenedEditingSolution = ServoyModelManager.getServoyModelManager().getServoyModel().getEditingFlattenedSolution(
						persist);
					final Form form = (Form)persist.getAncestor(IRepository.FORMS);
					final Table table = form == null ? null : form.getTable();
					propertyDescriptors = new IPropertyDescriptor[] { new PropertyDescriptor(TITLE, "title text")
					{
						@Override
						public CellEditor createPropertyEditor(Composite parent)
						{
							return new TagsAndI18NTextCellEditor(parent, persist, flattenedEditingSolution, TextCutoffLabelProvider.DEFAULT, table,
								"Edit text property", Activator.getDefault().getDesignClient());
						}

						@Override
						public ILabelProvider getLabelProvider()
						{
							return TextCutoffLabelProvider.DEFAULT;
						}
					},

					JUSTIFICATION_CONTROLLER, POSITION_CONTROLLER, new PropertyController<java.awt.Font, FontData[]>(FONT, "font",
						PropertyFontConverter.INSTANCE, FontLabelProvider.INSTANCE, new ICellEditorFactory()
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
					PropertyFontConverter.INSTANCE.convertValue(id, (FontData[])v), border.getTitleColor());
			}
			if (COLOR.equals(id))
			{
				return new TitledBorder(null, border.getTitle(), border.getTitleJustification(), border.getTitlePosition(), border.getTitleFont(),
					ColorPropertyController.PROPERTY_COLOR_CONVERTER.convertValue(id, (RGB)v));
			}

			return null;
		}
	}
}
