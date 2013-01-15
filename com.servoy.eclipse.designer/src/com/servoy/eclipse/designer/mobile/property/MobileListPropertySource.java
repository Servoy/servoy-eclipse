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

import com.servoy.base.persistence.IMobileProperties;
import com.servoy.eclipse.designer.editor.mobile.editparts.MobileListModel;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.eclipse.ui.property.RetargetingPropertySource;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.StaticContentSpecLoader;

/**
 * Properties source for InsetList in mobile form editor.
 * 
 * @author rgansevles
 *
 */
@SuppressWarnings("nls")
public class MobileListPropertySource extends RetargetingPropertySource
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

	private MobileListPropertySource(MobileListModel model, Form context)
	{
		super(model, context);
	}

	@Override
	protected MobileListModel getModel()
	{
		return (MobileListModel)super.getModel();
	}

	@Override
	protected void fillPropertyDescriptors()
	{
		// Delegate to members
		String prefix;
		PersistPropertySource elementPropertySource;

		// tab settings
		if (getModel().component != null)
		{
			// inset list
			elementPropertySources.put(prefix = null,
				elementPropertySource = PersistPropertySource.createPersistPropertySource(getModel().component, getContext(), false));
			addMethodPropertyDescriptor(elementPropertySource, prefix, StaticContentSpecLoader.PROPERTY_RELATIONNAME.getPropertyName());
			addMethodPropertyDescriptor(elementPropertySource, prefix, StaticContentSpecLoader.PROPERTY_NAME.getPropertyName());

			// location is based on component
			addMethodPropertyDescriptor(elementPropertySource, prefix, StaticContentSpecLoader.PROPERTY_LOCATION.getPropertyName());
		}

		// list item header
		if (getModel().header != null)
		{
			elementPropertySources.put(prefix = "listitemHeader",
				elementPropertySource = PersistPropertySource.createPersistPropertySource(getModel().header, getContext(), false));
			addMethodPropertyDescriptor(elementPropertySource, prefix, StaticContentSpecLoader.PROPERTY_DATAPROVIDERID.getPropertyName(), "headerDataProvider");
			addMethodPropertyDescriptor(elementPropertySource, prefix, StaticContentSpecLoader.PROPERTY_TEXT.getPropertyName(), "headerText");
			addMethodPropertyDescriptor(elementPropertySource, prefix, StaticContentSpecLoader.PROPERTY_STYLECLASS.getPropertyName(), "headerStyleClass");
		}

		// list item button
		if (getModel().button != null)
		{
			elementPropertySources.put(prefix = "listitemButton",
				elementPropertySource = PersistPropertySource.createPersistPropertySource(getModel().button, getContext(), false));
			addMethodPropertyDescriptor(elementPropertySource, prefix, StaticContentSpecLoader.PROPERTY_ONACTIONMETHODID.getPropertyName());
			addMethodPropertyDescriptor(elementPropertySource, prefix, StaticContentSpecLoader.PROPERTY_DATAPROVIDERID.getPropertyName(), "textDataProvider");
			addMethodPropertyDescriptor(elementPropertySource, prefix, StaticContentSpecLoader.PROPERTY_TEXT.getPropertyName());
			addMethodPropertyDescriptor(elementPropertySource, prefix, IMobileProperties.DATA_ICON.propertyName);
			addMethodPropertyDescriptor(elementPropertySource, prefix, StaticContentSpecLoader.PROPERTY_STYLECLASS.getPropertyName(), "listStyleClass");
		}

		// subtext
		if (getModel().subtext != null)
		{
			elementPropertySources.put(prefix = "listitemSubtext",
				elementPropertySource = PersistPropertySource.createPersistPropertySource(getModel().subtext, getContext(), false));
			addMethodPropertyDescriptor(elementPropertySource, prefix, StaticContentSpecLoader.PROPERTY_DATAPROVIDERID.getPropertyName(), "subtextDataProvider");
			addMethodPropertyDescriptor(elementPropertySource, prefix, StaticContentSpecLoader.PROPERTY_TEXT.getPropertyName(), "subtext");
		}

		// countBubble
		if (getModel().countBubble != null)
		{
			elementPropertySources.put(prefix = "listitemCount",
				elementPropertySource = PersistPropertySource.createPersistPropertySource(getModel().countBubble, getContext(), false));
			addMethodPropertyDescriptor(elementPropertySource, prefix, StaticContentSpecLoader.PROPERTY_DATAPROVIDERID.getPropertyName(), "countDataProvider");
		}

		// image
		if (getModel().image != null)
		{
			elementPropertySources.put(prefix = "listitemImage",
				elementPropertySource = PersistPropertySource.createPersistPropertySource(getModel().image, getContext(), false));
			addMethodPropertyDescriptor(elementPropertySource, prefix, StaticContentSpecLoader.PROPERTY_DATAPROVIDERID.getPropertyName(),
				"dataIconDataProvider");
		}
	}

	@Override
	public String toString()
	{
		return "List";
	}
}
