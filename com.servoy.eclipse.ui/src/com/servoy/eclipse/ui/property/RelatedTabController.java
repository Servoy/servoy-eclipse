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
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.properties.IPropertySource;

import com.servoy.eclipse.core.elements.ElementFactory.RelatedForm;
import com.servoy.eclipse.core.util.DatabaseUtils;
import com.servoy.eclipse.ui.dialogs.RelationContentProvider;
import com.servoy.eclipse.ui.dialogs.RelationContentProvider.RelationsWrapper;
import com.servoy.eclipse.ui.editors.ListSelectCellEditor;
import com.servoy.eclipse.ui.labelproviders.RelatedFormsLabelProvider;
import com.servoy.eclipse.ui.util.UnresolvedValue;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;

/**
 * Property controller that combines containsFormID and relationName in 1 selector.
 *
 * @author rgansevles
 */
public class RelatedTabController extends PropertyController<String, Object> implements IPropertySetter<Object, IPropertySource>
{
	private final String title;
	private final Form form;
	private final FlattenedSolution flattenedEditingSolution;

	public RelatedTabController(String id, String displayName, String title, boolean readOnly, Form form, FlattenedSolution flattenedEditingSolution)
	{
		super(id, displayName);
		this.title = title;
		this.form = form;
		this.flattenedEditingSolution = flattenedEditingSolution;
		setReadonly(readOnly);
		setSupportsReadonly(true);
	}

	public void setProperty(IPropertySource propertySource, Object value)
	{
		if (value instanceof RelatedForm)
		{
			Object oldValue = getProperty(propertySource);

			RelatedForm relatedForm = (RelatedForm)value;
			propertySource.setPropertyValue("containsFormID", relatedForm.form.getID());
			propertySource.setPropertyValue("relationName", DatabaseUtils.getRelationsString(relatedForm.relations));
			// update text as well if it has not been changed yet
			if (oldValue instanceof RelatedForm && ((RelatedForm)oldValue).form.getName().equals(propertySource.getPropertyValue("text")))
			{
				propertySource.setPropertyValue("text", relatedForm.form.getName());
			}
		}
	}

	public Object getProperty(IPropertySource propertySource)
	{
		Integer containsFormID = (Integer)(propertySource.getPropertyValue("containsFormID"));
		Object relation = propertySource.getPropertyValue("relationName"); // RelationContentProvider.NONE when no relation is set

		Form containsForm = null;
		if (containsFormID == null || containsFormID.intValue() == 0)
		{
			return RelationContentProvider.NONE;
		}

		containsForm = flattenedEditingSolution.getForm(containsFormID.intValue());
		if (containsForm == null)
		{
			return UnresolvedValue.NO_STRING_VALUE;
		}

		return new RelatedForm(relation instanceof RelationsWrapper ? ((RelationsWrapper)relation).relations : null, containsForm);
	}

	public void resetPropertyValue(IPropertySource propertySource)
	{
		// allow reset only for inherited forms
		if (form.getExtendsID() > 0)
		{
			Object oldValue = getProperty(propertySource);
			propertySource.resetPropertyValue("containsFormID");
			propertySource.resetPropertyValue("relationName");
			//update the text as well if it hasn't been changed yet
			if (oldValue instanceof RelatedForm && ((RelatedForm)oldValue).form.getName().equals(propertySource.getPropertyValue("text")))
			{
				propertySource.resetPropertyValue("text");
			}
		}
	}

	public boolean isPropertySet(IPropertySource propertySource)
	{
		return form.getExtendsID() > 0 ? propertySource.isPropertySet("containsFormID") : false;
	}

	@Override
	public ILabelProvider getLabelProvider()
	{
		return RelatedFormsLabelProvider.INSTANCE_NO_IMAGE;
	}

	@Override
	public CellEditor createPropertyEditor(Composite parent)
	{
		ListSelectCellEditor listSelectCellEditor = new ListSelectCellEditor(parent, title, new RelatedFormsContentProvider(form),
			RelatedFormsLabelProvider.INSTANCE, RelatedFormValueEditor.INSTANCE, isReadOnly(), form, SWT.NONE, null, "relatedFormDialog");
		listSelectCellEditor.setShowFilterMenu(true);
		return listSelectCellEditor;
	}
}
