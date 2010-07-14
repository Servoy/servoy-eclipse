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
package com.servoy.eclipse.designer.property;

import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellEditorValidator;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.repository.SolutionSerializer;
import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.property.ComplexProperty;
import com.servoy.eclipse.ui.property.DimensionPropertySource;
import com.servoy.eclipse.ui.property.ICellEditorFactory;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.eclipse.ui.property.PointPropertySource;
import com.servoy.eclipse.ui.property.PropertyCategory;
import com.servoy.eclipse.ui.property.PropertyController;
import com.servoy.eclipse.ui.property.ComplexProperty.ComplexPropertyConverter;
import com.servoy.eclipse.ui.util.DocumentValidatorVerifyListener;
import com.servoy.eclipse.ui.util.VerifyingTextCellEditor;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ValidatorSearchContext;

/**
 * Property source for form element groups.
 * 
 * @author rgansevles
 * 
 */
public class FormElementGroupPropertySource implements IPropertySource
{
	private final FormElementGroup group;

	public FormElementGroupPropertySource(FormElementGroup group)
	{
		this.group = group;
	}

	public Object getEditableValue()
	{
		return null;
	}

	public IPropertyDescriptor[] getPropertyDescriptors()
	{
		List<IPropertyDescriptor> lst = new ArrayList<IPropertyDescriptor>();
		Iterator<IFormElement> elements = group.getElements();
		for (int i = 0; elements.hasNext(); i++)
		{
			IFormElement element = elements.next();
			Object name = element.getName();
			if (name == null)
			{
				name = Messages.LabelAnonymous;
			}

			PropertyDescriptor desc = new PropertyDescriptor(new Integer(i), name.toString());
			desc.setCategory(PropertyCategory.Elements.name());
			lst.add(desc);
		}

		// size property
		PropertyController<java.awt.Dimension, Object> sizePc = new PropertyController<java.awt.Dimension, Object>("size", "size",
			new ComplexPropertyConverter<java.awt.Dimension>()
			{
				@Override
				public Object convertProperty(Object id, java.awt.Dimension value)
				{
					return new ComplexProperty<java.awt.Dimension>(value)
					{
						@Override
						public IPropertySource getPropertySource()
						{
							DimensionPropertySource dimensionPropertySource = new DimensionPropertySource(this, null);
							return dimensionPropertySource;
						}
					};
				}

			}, DimensionPropertySource.getLabelProvider(), new ICellEditorFactory()
			{
				public CellEditor createPropertyEditor(Composite parent)
				{
					return DimensionPropertySource.createPropertyEditor(parent);
				}
			});
		sizePc.setCategory(PropertyCategory.Properties.name());
		lst.add(sizePc);

		// location property
		PropertyController<java.awt.Point, Object> locationPc = new PropertyController<java.awt.Point, Object>("location", "location",
			new ComplexPropertyConverter<java.awt.Point>()
			{
				@Override
				public Object convertProperty(Object id, java.awt.Point value)
				{
					return new ComplexProperty<java.awt.Point>(value)
					{
						@Override
						public IPropertySource getPropertySource()
						{
							PointPropertySource pointPropertySource = new PointPropertySource(this);
							return pointPropertySource;
						}
					};
				}
			}, PointPropertySource.getLabelProvider(), new ICellEditorFactory()
			{
				public CellEditor createPropertyEditor(Composite parent)
				{
					return PointPropertySource.createPropertyEditor(parent);
				}
			});
		locationPc.setCategory(PropertyCategory.Properties.name());
		lst.add(locationPc);

		// name property
		PropertyDescriptor namePc = new PropertyDescriptor(SolutionSerializer.PROP_NAME, "name")
		{
			@Override
			public CellEditor createPropertyEditor(Composite parent)
			{
				VerifyingTextCellEditor cellEditor = new VerifyingTextCellEditor(parent);
				cellEditor.addVerifyListener(DocumentValidatorVerifyListener.IDENT_SERVOY_VERIFIER);
				cellEditor.setValidator(new ICellEditorValidator()
				{
					public String isValid(Object value)
					{
						if (value instanceof String && ((String)value).length() > 0 && !value.equals(group.getName()))
						{
							try
							{
								ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator().checkName((String)value, -1,
									new ValidatorSearchContext(group.getParent(), IRepository.ELEMENTS), false);
							}
							catch (RepositoryException e)
							{
								return e.getMessage();
							}
						}
						return null;
					}
				});

				return cellEditor;
			}
		};
		namePc.setCategory(PropertyCategory.Properties.name());
		lst.add(namePc);

		return lst.toArray(new IPropertyDescriptor[lst.size()]);
	}

	public Object getPropertyValue(Object id)
	{
		if ("size".equals(id)) return group.getSize();
		if ("location".equals(id)) return group.getLocation();
		if (SolutionSerializer.PROP_NAME.equals(id)) return PersistPropertySource.NULL_STRING_CONVERTER.convertProperty(id, group.getName());
		if (id instanceof Integer)
		{
			return new PersistContext((IPersist)group.getElement(((Integer)id).intValue()), null);
		}
		return null;
	}

	public boolean isPropertySet(Object id)
	{
		return true;
	}

	public void resetPropertyValue(Object id)
	{
	}

	public void setPropertyValue(Object id, Object value)
	{
		if ("size".equals(id)) group.setSize((Dimension)value);
		if ("location".equals(id)) group.setLocation((Point)value);
		if (SolutionSerializer.PROP_NAME.equals(id))
		{
			try
			{
				group.updateName(ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator(),
					PersistPropertySource.NULL_STRING_CONVERTER.convertValue(id, (String)value));
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError(e);
				return;
			}
		}

		// fire persist changes
		ServoyModelManager.getServoyModelManager().getServoyModel().firePersistChanged(false, group, true);
	}

	@Override
	public String toString()
	{
		String name = group.getName();
		if (name == null) return Messages.LabelAnonymous;
		return name;
	}

}
