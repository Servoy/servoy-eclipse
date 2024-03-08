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
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellEditorValidator;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.property.CheckboxPropertyDescriptor;
import com.servoy.eclipse.ui.property.ComplexProperty;
import com.servoy.eclipse.ui.property.ComplexProperty.ComplexPropertyConverter;
import com.servoy.eclipse.ui.property.DimensionPropertySource;
import com.servoy.eclipse.ui.property.ICellEditorFactory;
import com.servoy.eclipse.ui.property.IModelSavePropertySource;
import com.servoy.eclipse.ui.property.IRAGTEST;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.eclipse.ui.property.PointPropertySource;
import com.servoy.eclipse.ui.property.PropertyCategory;
import com.servoy.eclipse.ui.property.PropertyController;
import com.servoy.eclipse.ui.util.DocumentValidatorVerifyListener;
import com.servoy.eclipse.ui.util.VerifyingTextCellEditor;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.CSSPositionUtils;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.PositionComparator;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.StaticContentSpecLoader.TypedProperty;
import com.servoy.j2db.persistence.ValidatorSearchContext;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * Property source for form element groups.
 *
 * @author rgansevles
 *
 */
public class FormElementGroupPropertySource implements IPropertySource, IModelSavePropertySource
{
	private FormElementGroup group;
	private final IPersist context;

	public FormElementGroupPropertySource(FormElementGroup group, IPersist context)
	{
		this.group = group;
		this.context = context;
	}

	public Object getEditableValue()
	{
		return null;
	}

	public Object getSaveModel()
	{
		return group;
	}

	/**
	 * get elements, order by y-position
	 */
	public IFormElement[] getSortedElements()
	{
		IFormElement[] asArray = Utils.asArray(group.getElements(), IFormElement.class);
		Arrays.sort(asArray, PositionComparator.XY_PERSIST_COMPARATOR);
		return asArray;
	}

	public IPropertyDescriptor[] getPropertyDescriptors()
	{
		// get elements, order by y-position
		IFormElement[] sortedElements = getSortedElements();
		List<PropertyDescriptor> lst = new ArrayList<PropertyDescriptor>(sortedElements.length + 10);
		for (int i = 0; i < sortedElements.length; i++)
		{
			IFormElement element = sortedElements[i];
			Object name = element.getName();
			if (name == null)
			{
				if (element instanceof AbstractBase)
				{
					name = ComponentFactory.getLookupName((AbstractBase)element);
				}
				else
				{
					name = Messages.LabelAnonymous;
				}
			}

			PropertyDescriptor desc = new PropertyDescriptor(Integer.valueOf(i), name.toString());
			desc.setCategory(PropertyCategory.Elements.name());
			lst.add(desc);
		}

		// Note: list other properties in alphabetic order here, they are not sorted, see PropertyController.applySequencePropertyComparator()

		// enabled property
		lst.add(new CheckboxPropertyDescriptor(StaticContentSpecLoader.PROPERTY_ENABLED.getPropertyName(), "enabled"));

		// location property
		lst.add(new PropertyController<java.awt.Point, Object>(StaticContentSpecLoader.PROPERTY_LOCATION.getPropertyName(), "location",
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
							return new PointPropertySource(this);
						}
					};
				}
			}, PointPropertySource.getLabelProvider(), new ICellEditorFactory()
			{
				public CellEditor createPropertyEditor(Composite parent)
				{
					return PointPropertySource.createPropertyEditor(parent);
				}
			}));

		// name property
		lst.add(new PropertyDescriptor(StaticContentSpecLoader.PROPERTY_NAME.getPropertyName(), "name")
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
		});

		// size property
		lst.add(new PropertyController<java.awt.Dimension, Object>(StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName(), "size",
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
							return new DimensionPropertySource(this, null);
						}
					};
				}

			}, DimensionPropertySource.getLabelProvider(), new ICellEditorFactory()
			{
				public CellEditor createPropertyEditor(Composite parent)
				{
					return DimensionPropertySource.createPropertyEditor(parent);
				}
			}));

		// visible property
		lst.add(new CheckboxPropertyDescriptor(StaticContentSpecLoader.PROPERTY_VISIBLE.getPropertyName(), "visible"));

		// all prop descs that do not have a category yet belong to category PropertyCategory.Properties
		for (PropertyDescriptor desc : lst)
		{
			if (desc.getCategory() == null)
			{
				desc.setCategory(PropertyCategory.Properties.name());
			}
		}

		return PropertyController.applySequencePropertyComparator(lst.toArray(new IPropertyDescriptor[lst.size()]));
	}

	public Object getPropertyValue(Object id)
	{
		if (StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName().equals(id)) return group.getSize();
		if (StaticContentSpecLoader.PROPERTY_LOCATION.getPropertyName().equals(id)) return group.getLocation();
		if (StaticContentSpecLoader.PROPERTY_VISIBLE.getPropertyName().equals(id)) return Boolean.valueOf(group.getVisible());
		if (StaticContentSpecLoader.PROPERTY_ENABLED.getPropertyName().equals(id)) return Boolean.valueOf(group.getEnabled());
		if (StaticContentSpecLoader.PROPERTY_NAME.getPropertyName().equals(id)) return PersistPropertySource.NULL_STRING_CONVERTER.convertProperty(id,
			group.getName());
		if (id instanceof Integer && ((Integer)id).intValue() >= 0)
		{
			IFormElement[] sortedElements = getSortedElements();
			if (((Integer)id).intValue() < sortedElements.length)
			{
				return PersistContext.create(sortedElements[((Integer)id).intValue()]);
			}
		}
		return null;
	}

	public boolean isPropertySet(Object id)
	{
		return true;
	}

	public void resetPropertyValue(Object id)
	{
		Object reset;
		if (StaticContentSpecLoader.PROPERTY_VISIBLE.getPropertyName().equals(id) || StaticContentSpecLoader.PROPERTY_ENABLED.getPropertyName().equals(id))
		{
			reset = Boolean.TRUE;
		}
		else if (StaticContentSpecLoader.PROPERTY_NAME.getPropertyName().equals(id))
		{
			reset = null;
		}
		else return; // no default for location and size

		setPropertyValue(id, reset);
	}

	public void setPropertyValue(Object id, Object value)
	{
		if (StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName().equals(id))
		{
			setSize((Dimension)value);
		}
		else if (StaticContentSpecLoader.PROPERTY_LOCATION.getPropertyName().equals(id))
		{
			setLocation((Point)value);
		}
		else if (StaticContentSpecLoader.PROPERTY_VISIBLE.getPropertyName().equals(id))
		{
			setPropertyToElements(StaticContentSpecLoader.PROPERTY_VISIBLE, (Boolean)value);
		}
		else if (StaticContentSpecLoader.PROPERTY_ENABLED.getPropertyName().equals(id))
		{
			setPropertyToElements(StaticContentSpecLoader.PROPERTY_ENABLED, (Boolean)value);
		}
		else if (StaticContentSpecLoader.PROPERTY_NAME.getPropertyName().equals(id))
		{
			try
			{
				updateName(ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator(),
					PersistPropertySource.NULL_STRING_CONVERTER.convertValue(id, (String)value));
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError(e);
				return;
			}
		}
	}

	public void setLocation(Point p)
	{
		Point oldLocation = group.getLocation();
		int dx = p.x - oldLocation.x;
		int dy = p.y - oldLocation.y;
		if (dx == 0 && dy == 0) return;

		Iterator<IFormElement> elements = group.getElements();
		while (elements.hasNext())
		{
			IFormElement element = elements.next();
			Point oldElementLocation = CSSPositionUtils.getLocation(element);
			Point location = new Point(oldElementLocation.x + dx, oldElementLocation.y + dy);
			setElementProperty(element, StaticContentSpecLoader.PROPERTY_LOCATION.getPropertyName(), location);
		}
	}

	public void setSize(Dimension d)
	{
		Rectangle oldBounds = group.getBounds();
		if (d.width == oldBounds.width && d.height == oldBounds.height || oldBounds.width == 0 || oldBounds.height == 0)
		{
			return;
		}

		float factorW = d.width / (float)oldBounds.width;
		float factorH = d.height / (float)oldBounds.height;

		Iterator<IFormElement> elements = group.getElements();
		while (elements.hasNext())
		{
			IFormElement element = elements.next();
			Dimension oldElementSize = CSSPositionUtils.getSize(element);
			Point oldElementLocation = CSSPositionUtils.getLocation(element);

			Dimension size = new Dimension((int)(oldElementSize.width * factorW), (int)(oldElementSize.height * factorH));

			int newX;
			if (oldElementLocation.x + oldElementSize.width == oldBounds.x + oldBounds.width)
			{
				// element was attached to the right side, keep it there
				newX = oldBounds.x + d.width - size.width;
			}
			else
			{
				// move relative to size factor
				newX = oldBounds.x + (int)((oldElementLocation.x - oldBounds.x) * factorW);
			}
			int newY;
			if (oldElementLocation.y + oldElementSize.height == oldBounds.y + oldBounds.height)
			{
				// element was attached to the bottom side, keep it there
				newY = oldBounds.y + d.height - size.height;
			}
			else
			{
				// move relative to size factor
				newY = oldBounds.y + (int)((oldElementLocation.y - oldBounds.y) * factorH);
			}
			Point location = new Point(newX, newY);

			setElementProperty(element, StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName(), size);
			setElementProperty(element, StaticContentSpecLoader.PROPERTY_LOCATION.getPropertyName(), location);
		}
	}

	protected <T> void setPropertyToElements(TypedProperty<T> property, T arg)
	{
		Iterator<IFormElement> elements = group.getElements();
		while (elements.hasNext())
		{
			IFormElement element = elements.next();
			setElementProperty(element, property.getPropertyName(), arg);
		}
	}

	protected void setElementProperty(IPersist element, String propertyName, Object propertyValue)
	{
		IRAGTEST elementPropertySource = PersistPropertySource.createPersistPropertySource(element, context == null ? (IPersist)element : context,
			false);
		elementPropertySource.setPropertyValue(propertyName, propertyValue);
		IPersist newPersist = (IPersist)elementPropertySource.getSaveModel();
		if (newPersist != element)
		{
			// element model was changed (added as override element), replace our model as well
			group = new FormElementGroup(group.getGroupID(), ModelUtils.getEditingFlattenedSolution(newPersist), (Form)newPersist.getParent());
		}
	}

	protected void updateName(IValidateName validator, String name) throws RepositoryException
	{
		String newGroupId;
		if (name == null)
		{
			newGroupId = UUID.randomUUID().toString();
		}
		else
		{
			if (!name.equals(group.getName()))
			{
				validator.checkName(name, -1, new ValidatorSearchContext(context, IRepository.ELEMENTS), false);
			}
			newGroupId = name;
		}

		setPropertyToElements(StaticContentSpecLoader.PROPERTY_GROUPID, newGroupId);

		// must set grouID after looping over elements (uses current groupID)
		group.setGroupID(newGroupId);
	}

	@Override
	public String toString()
	{
		String name = group.getName();
		if (name == null) return Messages.LabelAnonymous;
		return name;
	}

}
