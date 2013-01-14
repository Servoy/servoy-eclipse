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
import com.servoy.j2db.scripting.solutionhelper.IMobileProperties;
import com.servoy.j2db.util.Pair;

/**
 * Properties source for InsetList in mobile form editor.
 * 
 * @author rgansevles
 *
 */
@SuppressWarnings("nls")
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
			if (model.component != null)
			{
				// inset list
				elementPropertySources.put(prefix = null,
					elementPropertySource = PersistPropertySource.createPersistPropertySource(model.component, context, false));
				addMethodPropertyDescriptor(elementPropertySource, prefix, StaticContentSpecLoader.PROPERTY_RELATIONNAME.getPropertyName());
				addMethodPropertyDescriptor(elementPropertySource, prefix, StaticContentSpecLoader.PROPERTY_NAME.getPropertyName());

				// location is based on component
				addMethodPropertyDescriptor(elementPropertySource, prefix, StaticContentSpecLoader.PROPERTY_LOCATION.getPropertyName());
			}

			// list item header
			if (model.header != null)
			{
				elementPropertySources.put(prefix = "listitemHeader",
					elementPropertySource = PersistPropertySource.createPersistPropertySource(model.header, model.form, false));
				addMethodPropertyDescriptor(elementPropertySource, prefix, StaticContentSpecLoader.PROPERTY_DATAPROVIDERID.getPropertyName(),
					"headerDataProvider");
				addMethodPropertyDescriptor(elementPropertySource, prefix, StaticContentSpecLoader.PROPERTY_TEXT.getPropertyName(), "headerText");
				addMethodPropertyDescriptor(elementPropertySource, prefix, StaticContentSpecLoader.PROPERTY_STYLECLASS.getPropertyName(), "headerStyleClass");
			}

			// list item button
			if (model.button != null)
			{
				elementPropertySources.put(prefix = "listitemButton",
					elementPropertySource = PersistPropertySource.createPersistPropertySource(model.button, model.form, false));
				addMethodPropertyDescriptor(elementPropertySource, prefix, StaticContentSpecLoader.PROPERTY_ONACTIONMETHODID.getPropertyName());
				addMethodPropertyDescriptor(elementPropertySource, prefix, StaticContentSpecLoader.PROPERTY_DATAPROVIDERID.getPropertyName(),
					"textDataProvider");
				addMethodPropertyDescriptor(elementPropertySource, prefix, StaticContentSpecLoader.PROPERTY_TEXT.getPropertyName());
				addMethodPropertyDescriptor(elementPropertySource, prefix, IMobileProperties.DATA_ICON.propertyName);
				addMethodPropertyDescriptor(elementPropertySource, prefix, StaticContentSpecLoader.PROPERTY_STYLECLASS.getPropertyName());
			}

			// subtext
			if (model.subtext != null)
			{
				elementPropertySources.put(prefix = "listitemSubtext",
					elementPropertySource = PersistPropertySource.createPersistPropertySource(model.subtext, model.form, false));
				addMethodPropertyDescriptor(elementPropertySource, prefix, StaticContentSpecLoader.PROPERTY_DATAPROVIDERID.getPropertyName(),
					"subtextDataProvider");
				addMethodPropertyDescriptor(elementPropertySource, prefix, StaticContentSpecLoader.PROPERTY_TEXT.getPropertyName(), "subtext");
			}

			// countBubble
			if (model.countBubble != null)
			{
				elementPropertySources.put(prefix = "listitemCount",
					elementPropertySource = PersistPropertySource.createPersistPropertySource(model.countBubble, model.form, false));
				addMethodPropertyDescriptor(elementPropertySource, prefix, StaticContentSpecLoader.PROPERTY_DATAPROVIDERID.getPropertyName(),
					"countDataProvider");
			}

			// image
			if (model.image != null)
			{
				elementPropertySources.put(prefix = "listitemImage",
					elementPropertySource = PersistPropertySource.createPersistPropertySource(model.image, model.form, false));
				addMethodPropertyDescriptor(elementPropertySource, prefix, StaticContentSpecLoader.PROPERTY_DATAPROVIDERID.getPropertyName(),
					"dataIconDataProvider");
			}
		}
	}

	private void addMethodPropertyDescriptor(PersistPropertySource elementPropertySource, String prefix, String propertyName)
	{
		addMethodPropertyDescriptor(elementPropertySource, prefix, propertyName, null);
	}

	private void addMethodPropertyDescriptor(PersistPropertySource elementPropertySource, String prefix, String propertyName, String displayName)
	{
		String id = prefix == null ? propertyName : prefix + '.' + propertyName;
		IPropertyDescriptor propertyDescriptor = elementPropertySource.getPropertyDescriptor(propertyName);
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
		if (split.length <= 2) // when prefix is null, property is top-level, like location
		{
			IPropertySource elementPropertySource = elementPropertySources.get(split.length == 1 ? null : split[0] /* prefix */);
			if (elementPropertySource != null)
			{
				return new Pair<IPropertySource, String>(elementPropertySource, split[split.length - 1] /* id */);
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

	@Override
	public String toString()
	{
		return "List";
	}
}
