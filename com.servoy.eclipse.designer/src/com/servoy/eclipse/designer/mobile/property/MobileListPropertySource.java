/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.eclipse.designer.mobile.property;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;

import com.servoy.eclipse.designer.editor.mobile.editparts.MobileListModel;
import com.servoy.eclipse.ui.property.DelegatePropertyController;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.StaticContentSpecLoader.TypedProperty;
import com.servoy.j2db.util.Pair;

/**
 * Properties source for InsetList im mobile form editor.
 * 
 * @author rgansevles
 *
 */
public class MobileListPropertySource implements IPropertySource
{
//	private static final WeakHashMap<Pair<MobileInsetListModel, Form>, MobileInsetListPropertySource> cache = new WeakHashMap<Pair<MobileInsetListModel, Form>, MobileInsetListPropertySource>();

	public static MobileListPropertySource getMobileListPropertySource(MobileListModel model, Form context)
	{
		return new MobileListPropertySource(model, context);
		// TODO: add caching?
//		Pair<MobileInsetListModel, Form> key = new Pair<MobileInsetListModel, Form>(model, context);
//		MobileInsetListPropertySource mobileInsetListPropertySource = cache.get(key);
//		if (mobileInsetListPropertySource == null)
//		{
//			cache.put(key, mobileInsetListPropertySource = new MobileInsetListPropertySource(model, context));
//		}
//		return mobileInsetListPropertySource;
	}


	private final MobileListModel model;
	private LinkedHashMap<Object, IPropertyDescriptor> propertyDescriptors;
	private Map<String, IPropertySource> elementPropertySources;
	private final Form context;

	private MobileListPropertySource(MobileListModel model, Form context)
	{
		this.model = model;
		this.context = context;
	}

	public Object getEditableValue()
	{
		return model;
	}

	public IPropertyDescriptor[] getPropertyDescriptors()
	{
		init();
		return propertyDescriptors.values().toArray(new IPropertyDescriptor[propertyDescriptors.size()]);
	}

	public void init()
	{
		if (propertyDescriptors == null)
		{
			propertyDescriptors = new LinkedHashMap<Object, IPropertyDescriptor>();// defines order

			elementPropertySources = new HashMap<String, IPropertySource>();

			// Delegate to members
			String prefix;
			PersistPropertySource elementPropertySource;

			// tab settings
			if (model.tab != null)
			{
				elementPropertySources.put(prefix = "listitemTab",
					elementPropertySource = PersistPropertySource.createPersistPropertySource(model.tab, context, false));
				addMethodPropertyDescriptor(elementPropertySource, prefix, StaticContentSpecLoader.PROPERTY_RELATIONNAME);
			}

			if (model.tabPanel != null)
			{
				// inset list
				elementPropertySources.put(prefix = "containedForm",
					elementPropertySource = PersistPropertySource.createPersistPropertySource(model.containedForm, model.containedForm, false));
				addMethodPropertyDescriptor(elementPropertySource, prefix, StaticContentSpecLoader.PROPERTY_DATASOURCE);
			}

			// list item header
			if (model.header != null)
			{
				elementPropertySources.put(prefix = "listitemHeader",
					elementPropertySource = PersistPropertySource.createPersistPropertySource(model.header, model.containedForm, false));
				addMethodPropertyDescriptor(elementPropertySource, prefix, StaticContentSpecLoader.PROPERTY_DATAPROVIDERID, "headerDataProvider");
				addMethodPropertyDescriptor(elementPropertySource, prefix, StaticContentSpecLoader.PROPERTY_TEXT, "headerText");
			}

			// list item button
			if (model.button != null)
			{
				elementPropertySources.put(prefix = "listitemButton",
					elementPropertySource = PersistPropertySource.createPersistPropertySource(model.button, model.containedForm, false));
				addMethodPropertyDescriptor(elementPropertySource, prefix, StaticContentSpecLoader.PROPERTY_ONACTIONMETHODID);
				addMethodPropertyDescriptor(elementPropertySource, prefix, StaticContentSpecLoader.PROPERTY_DATAPROVIDERID, "textDataProvider");
				addMethodPropertyDescriptor(elementPropertySource, prefix, StaticContentSpecLoader.PROPERTY_TEXT);
			}

			// aside
			if (model.aside != null)
			{
				elementPropertySources.put(prefix = "listitemAside",
					elementPropertySource = PersistPropertySource.createPersistPropertySource(model.aside, model.containedForm, false));
				addMethodPropertyDescriptor(elementPropertySource, prefix, StaticContentSpecLoader.PROPERTY_DATAPROVIDERID, "asideDataProvider");
				addMethodPropertyDescriptor(elementPropertySource, prefix, StaticContentSpecLoader.PROPERTY_TEXT, "asideText");
			}

			// countBubble
			if (model.countBubble != null)
			{
				elementPropertySources.put(prefix = "listitemCount",
					elementPropertySource = PersistPropertySource.createPersistPropertySource(model.countBubble, model.containedForm, false));
				addMethodPropertyDescriptor(elementPropertySource, prefix, StaticContentSpecLoader.PROPERTY_DATAPROVIDERID, "countDataProvider");
			}

			// image
			if (model.image != null)
			{
				elementPropertySources.put(prefix = "listitemImage",
					elementPropertySource = PersistPropertySource.createPersistPropertySource(model.image, model.containedForm, false));
				addMethodPropertyDescriptor(elementPropertySource, prefix, StaticContentSpecLoader.PROPERTY_DATAPROVIDERID, "imageDataProvider");
			}
		}
	}

	private void addMethodPropertyDescriptor(PersistPropertySource elementPropertySource, String prefix, TypedProperty< ? > property)
	{
		addMethodPropertyDescriptor(elementPropertySource, prefix, property, null);
	}

	private void addMethodPropertyDescriptor(PersistPropertySource elementPropertySource, String prefix, TypedProperty< ? > property, String displayName)
	{
		String id = prefix + '.' + property.getPropertyName();
		IPropertyDescriptor propertyDescriptor = elementPropertySource.getPropertyDescriptor(property.getPropertyName());
		if (propertyDescriptor != null)
		{
			propertyDescriptors.put(id,
				new DelegatePropertyController<Object, Object>(propertyDescriptor, id, displayName == null ? propertyDescriptor.getDisplayName() : displayName));
		}
	}

	private Pair<IPropertySource, String> getElementPropertySource(Object id)
	{
		init();

		String[] split = id.toString().split("\\.");
		if (split.length == 2)
		{
			IPropertySource elementPropertySource = elementPropertySources.get(split[0] /* prefix */);
			if (elementPropertySource != null)
			{
				return new Pair<IPropertySource, String>(elementPropertySource, split[1] /* id */);
			}
		}

		return null;
	}

	public Object getPropertyValue(Object id)
	{
		Pair<IPropertySource, String> pair = getElementPropertySource(id);
		if (pair != null)
		{
			return pair.getLeft().getPropertyValue(pair.getRight());
		}

		return null;
	}

	public boolean isPropertySet(Object id)
	{
		Pair<IPropertySource, String> pair = getElementPropertySource(id);
		if (pair != null)
		{
			return pair.getLeft().isPropertySet(pair.getRight());
		}

		return false;
	}

	public void resetPropertyValue(Object id)
	{
		Pair<IPropertySource, String> pair = getElementPropertySource(id);
		if (pair != null)
		{
			pair.getLeft().resetPropertyValue(pair.getRight());
		}
	}

	public void setPropertyValue(Object id, Object value)
	{
		Pair<IPropertySource, String> pair = getElementPropertySource(id);
		if (pair != null)
		{
			pair.getLeft().setPropertyValue(pair.getRight(), value);
		}
	}

}
