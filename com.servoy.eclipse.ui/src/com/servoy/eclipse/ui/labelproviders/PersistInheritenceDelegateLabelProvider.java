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
package com.servoy.eclipse.ui.labelproviders;


import java.util.Map;

import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.json.JSONObject;

import com.servoy.eclipse.model.util.IParentOverridable;
import com.servoy.eclipse.ui.Messages;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.IBasicWebObject;
import com.servoy.j2db.persistence.IChildWebObject;
import com.servoy.j2db.persistence.IContentSpecConstants;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.LayoutContainer;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.ServoyJSONObject;

/**
 * Delegate label provider that adds the form inheritance context to the label.
 *
 * @author rgansevles
 *
 */
public class PersistInheritenceDelegateLabelProvider extends DelegateLabelProvider implements IFontProvider
{
	//public static final Image INHERITED_IMAGE = Activator.loadImageDescriptorFromBundle("inherited.gif").createImage();

	private final IPersist persist;
	private final Object propertyId;

	public PersistInheritenceDelegateLabelProvider(IPersist persist, ILabelProvider labelProvider, Object propertyId)
	{
		super(labelProvider);
		this.persist = persist;
		this.propertyId = propertyId;
	}

	/**
	 * Mark the entry with an inherited image hen there is no image yet
	 */
	@Override
	public Image getImage(Object value)
	{
		Image image = super.getImage(value);
//		if (image == null && form != null && form.getExtendsFormID() > 0)
//		{
//			Object inheritedValue = new PersistProperties(form, null, true).getInheritedPropertyValue(value, propertyId);
//			if (!Utils.equalObjects(value, inheritedValue))
//			{
//				return INHERITED_IMAGE;
//			}
//		}
		return image;
	}

	/**
	 * show the actually used value per form inheritance.
	 */
	@Override
	public String getText(Object value)
	{
		String superText = super.getText(value);

		IPersist persistToCheckForOverride = persist;

		// get persistToCheckForOverride in case of nested custom objects or arrays of custom objects
		// also see if any in the parent changing is a default custom object (in which case the value is not overridden)
		// and see - if we don't have a default value - which one is the deep-most parent that has a non-default value
		while (persistToCheckForOverride instanceof IChildWebObject)
		{
			persistToCheckForOverride = persistToCheckForOverride.getParent();
		}

		if (persistToCheckForOverride instanceof IParentOverridable)
		{
			persistToCheckForOverride = ((IParentOverridable)persistToCheckForOverride).getParentToOverride();
		}

		boolean isOverridden = false;
		if (PersistHelper.isOverrideElement(persistToCheckForOverride))
		{
			// ok so the ancestor that can be overridden is overridden; see if the value we are interested in is inherited (set - for most cases or non-default - in case of persist mapped) or not
			if (value instanceof IChildWebObject || value instanceof IChildWebObject[])
			{
				// see if this is a non-default custom object or array of custom object value in parent persist;
				isOverridden = !isDefaultOrInheritedChildObjectValue(persist, String.valueOf(propertyId));
			}
			else
			{
				if (persist instanceof LayoutContainer && "class".equals(propertyId))
				{
					Map<String, String> attributes = (Map<String, String>)((LayoutContainer)persist).getCustomPropertyNonFlattened(
						new String[] { IContentSpecConstants.PROPERTY_ATTRIBUTES });
					if (attributes != null && attributes.containsKey(propertyId))
					{
						isOverridden = true;
					}
				}
				// the value is not persist-mapped; if value is set in current persist then it is overridden
				if (((AbstractBase)persist).hasProperty((String)propertyId))
				{
					isOverridden = true;
				}
			}

		}

		if (isOverridden)
		{
			return Messages.labelOverride(superText);
		}

		return superText;
	}

	private boolean isDefaultOrInheritedChildObjectValue(IPersist parent, String keyOfValue)
	{
		boolean isDefaultOrInheritedValue = true;

		// see if this is a non-default custom object
		if (parent instanceof IBasicWebObject)
		{
			// getJson() would actually return the flattened JSON - so we can't know from it if it's inherited or not - it's already merged
			// so we use getOwnProperty which only takes the JSON from current persist
			JSONObject parentJSON = (JSONObject)((IBasicWebObject)parent).getOwnProperty(StaticContentSpecLoader.PROPERTY_JSON.getPropertyName());

			if (!ServoyJSONObject.isJavascriptNullOrUndefined(parentJSON) && parentJSON.has(keyOfValue))
			{
				// non-default value (WebObjectImpl automatically generates IChildWebObjects for default values from .spec, but the json of the parent is then not linked
				// to the json of the child); we take advantage of that to determine if it's a default spec value for this IChildWebObject or not

				// make sure parent persists are non-default values as well
				if (parent instanceof IChildWebObject)
					isDefaultOrInheritedValue = isDefaultOrInheritedChildObjectValue(parent.getParent(), ((IChildWebObject)parent).getJsonKey());
				else isDefaultOrInheritedValue = false;
			} // else it is default value
		} // else should never happen

		return isDefaultOrInheritedValue;
	}

	/**
	 * @see IFontProvider
	 *
	 */
	@Override
	public Font getFont(Object value)
	{
		if (getLabelProvider() instanceof IFontProvider)
		{
			return ((IFontProvider)getLabelProvider()).getFont(value);
		}
		return null;
	}

}
