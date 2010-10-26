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
import com.servoy.j2db.persistence.FormEncapsulation;

/**
 * Property controller for encapsulation properties.
 * 
 * @author jcompagner
 * @since 6.0
 */
public class EncapsulationPropertyController extends PropertyController<Integer, Object>
{
	public EncapsulationPropertyController(String id, String displayName)
	{
		super(id, displayName, null, EncapsulationLabelProvider.LABEL_INSTANCE, new DummyCellEditorFactory(EncapsulationLabelProvider.LABEL_INSTANCE));
	}

	@Override
	protected IPropertyConverter<Integer, Object> createConverter()
	{
		return new EncapsulationPropertyConverter();
	}

	public class EncapsulationPropertyConverter extends ComplexPropertyConverter<Integer>
	{
		@Override
		public Object convertProperty(Object id, Integer value)
		{
			return new ComplexProperty<Integer>(new Integer(0).equals(value) ? new Integer(FormEncapsulation.DEFAULT) : value)
			{
				@Override
				public IPropertySource getPropertySource()
				{
					EncapsulationPropertySource encapsulationPropertySource = new EncapsulationPropertySource(this);
					encapsulationPropertySource.setReadonly(EncapsulationPropertyController.this.isReadOnly());
					return encapsulationPropertySource;
				}
			};
		}
	}

	@SuppressWarnings("nls")
	public static class EncapsulationPropertySource extends ComplexPropertySource<Integer>
	{
		private static final int ALL = FormEncapsulation.PRIVATE + FormEncapsulation.MODULE_PRIVATE + FormEncapsulation.HIDE_CONTROLLER +
			FormEncapsulation.HIDE_DATAPROVIDERS + FormEncapsulation.HIDE_ELEMENTS + FormEncapsulation.HIDE_FOUNDSET;

		private static final String PRIVATE = "private";
		private static final String MODULE_PRIVATE = "module private";
		private static final String HIDE_DATAPROVIDERS = "hide dataproviders";
		private static final String HIDE_FOUNDSET = "hide foundset";
		private static final String HIDE_CONTROLLER = "hide controller";
		private static final String HIDE_ELEMENTS = "hide elements";

		public EncapsulationPropertySource(ComplexProperty<Integer> complexProperty)
		{
			super(complexProperty);
		}

		@Override
		public Object resetComplexPropertyValue(Object id)
		{
			return Integer.valueOf(FormEncapsulation.DEFAULT);
		}

		@Override
		public IPropertyDescriptor[] createPropertyDescriptors()
		{
			// make sure sub-properties are sorted in defined order
			return PropertyController.applySequencePropertyComparator(new IPropertyDescriptor[] { new CheckboxPropertyDescriptor(PRIVATE, PRIVATE), new CheckboxPropertyDescriptor(
				MODULE_PRIVATE, MODULE_PRIVATE), new CheckboxPropertyDescriptor(HIDE_DATAPROVIDERS, HIDE_DATAPROVIDERS), new CheckboxPropertyDescriptor(
				HIDE_FOUNDSET, HIDE_FOUNDSET), new CheckboxPropertyDescriptor(HIDE_CONTROLLER, HIDE_CONTROLLER), new CheckboxPropertyDescriptor(HIDE_ELEMENTS,
				HIDE_ELEMENTS) });
		}

		@Override
		public Object getPropertyValue(Object id)
		{
			int encapsulation = getEditableValue().intValue();
			if (encapsulation == -1) return Boolean.FALSE;

			if (PRIVATE.equals(id)) return (encapsulation & FormEncapsulation.PRIVATE) == 0 ? Boolean.FALSE : Boolean.TRUE;
			if (MODULE_PRIVATE.equals(id)) return (encapsulation & FormEncapsulation.MODULE_PRIVATE) == 0 ? Boolean.FALSE : Boolean.TRUE;
			if (HIDE_DATAPROVIDERS.equals(id)) return (encapsulation & FormEncapsulation.HIDE_DATAPROVIDERS) == 0 ? Boolean.FALSE : Boolean.TRUE;
			if (HIDE_FOUNDSET.equals(id)) return (encapsulation & FormEncapsulation.HIDE_FOUNDSET) == 0 ? Boolean.FALSE : Boolean.TRUE;
			if (HIDE_CONTROLLER.equals(id)) return (encapsulation & FormEncapsulation.HIDE_CONTROLLER) == 0 ? Boolean.FALSE : Boolean.TRUE;
			if (HIDE_ELEMENTS.equals(id)) return (encapsulation & FormEncapsulation.HIDE_ELEMENTS) == 0 ? Boolean.FALSE : Boolean.TRUE;
			return null;
		}

		@Override
		public Integer setComplexPropertyValue(Object id, Object v)
		{
			int encapsulation = getEditableValue().intValue();
			if (encapsulation == -1)
			{
				encapsulation = 0;
			}
			int flag;
			if (PRIVATE.equals(id)) flag = FormEncapsulation.PRIVATE;
			else if (MODULE_PRIVATE.equals(id)) flag = FormEncapsulation.MODULE_PRIVATE;
			else if (HIDE_DATAPROVIDERS.equals(id)) flag = FormEncapsulation.HIDE_DATAPROVIDERS;
			else if (HIDE_FOUNDSET.equals(id)) flag = FormEncapsulation.HIDE_FOUNDSET;
			else if (HIDE_CONTROLLER.equals(id)) flag = FormEncapsulation.HIDE_CONTROLLER;
			else if (HIDE_ELEMENTS.equals(id)) flag = FormEncapsulation.HIDE_ELEMENTS;
			else return null;

			if (Boolean.TRUE.equals(v))
			{
				encapsulation |= flag;
			}
			else
			{
				encapsulation &= (ALL - flag);
			}

			return new Integer(encapsulation);
		}
	}


	public static class EncapsulationLabelProvider extends LabelProvider
	{
		public static EncapsulationLabelProvider LABEL_INSTANCE = new EncapsulationLabelProvider();

		@Override
		public String getText(Object element)
		{
			int encapsulation = ((Integer)element).intValue();

			if (encapsulation == 0)
			{
				return Messages.Public;
			}

			StringBuffer retval = new StringBuffer();
			if ((encapsulation & FormEncapsulation.PRIVATE) == FormEncapsulation.PRIVATE)
			{
				retval.append(Messages.Private);
			}
			if ((encapsulation & FormEncapsulation.MODULE_PRIVATE) == FormEncapsulation.MODULE_PRIVATE)
			{
				if (retval.length() != 0) retval.append(',');
				retval.append(Messages.ModulePrivate);
			}
			if ((encapsulation & FormEncapsulation.HIDE_CONTROLLER) == FormEncapsulation.HIDE_CONTROLLER)
			{
				if (retval.length() != 0) retval.append(',');
				retval.append(Messages.HideController);
			}
			if ((encapsulation & FormEncapsulation.HIDE_DATAPROVIDERS) == FormEncapsulation.HIDE_DATAPROVIDERS)
			{
				if (retval.length() != 0) retval.append(',');
				retval.append(Messages.HideDataproviders);
			}
			if ((encapsulation & FormEncapsulation.HIDE_FOUNDSET) == FormEncapsulation.HIDE_FOUNDSET)
			{
				if (retval.length() != 0) retval.append(',');
				retval.append(Messages.HideFoundset);
			}
			if ((encapsulation & FormEncapsulation.HIDE_ELEMENTS) == FormEncapsulation.HIDE_ELEMENTS)
			{
				if (retval.length() != 0) retval.append(',');
				retval.append(Messages.HideElements);
			}
			return retval.toString();
		}
	}
}
