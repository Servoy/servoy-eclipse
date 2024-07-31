/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2017 Servoy BV

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

package com.servoy.eclipse.ui.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.stream.Collectors;

import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IPropertyType;

import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.WebCustomType;
import com.servoy.j2db.server.ngclient.property.types.FormPropertyType;
import com.servoy.j2db.server.ngclient.property.types.NGCustomJSONObjectType;

/**
 * Utility class that can be used throughout developer.
 *
 * @author acostescu
 */
public class DeveloperUtils
{

	/**
	 * Use {@link #USE_AS_CAPTION_IN_DEVELOPER} instead.
	 */
	@Deprecated
	public static final String TAG_SHOW_IN_OUTLINE_VIEW = "showInOutlineView";

	/**
	 * <p>Tag in .spec file for a sub-property of a custom object; it tells developer to use the value
	 * of that property as a text/caption for when that custom object value is represented by one item in the UI.</p>
	 *
	 * <p>One can mark multiple sub-properties with this tag, and using {@link #CAPTION_PRIORITY} one can specify the order in which their values will be checked.</p>
	 */
	public static final String TAG_USE_AS_CAPTION_IN_DEVELOPER = "useAsCaptionInDeveloper";
	/**
	 * <p>Tag in .spec file for a sub-property of a custom object that is also marked with {@link #USE_AS_CAPTION_IN_DEVELOPER}.</p>
	 * <p>When multiple sub-properties are marked as developer caption, they will be sorted by CAPTION_PRIORITY value (1 is highest prio/first, higher values have lower prio) and the first non-null caption property value will be used as caption.</p>
	 */
	public static final String TAG_CAPTION_PRIORITY = "captionPriority";

	/**
	 *  <p>Tag in .spec file for a sub-property of a tags object, specifying what field type should the object inherit  </p>
	 */
	public final static String TAG_PROPERTY_INPUT_FIELD_TYPE = "valuesFieldType";

	/**
	 * Looks in the custom type sub-prop values that are tagged using {@link #TAG_USE_AS_CAPTION_IN_DEVELOPER} or {@link #TAG_SHOW_IN_OUTLINE_VIEW} and gets the captio to use.
	 * It can return null if it did not find a suitable caption...
	 */
	public static String getCustomObjectTypeCaptionFromTaggedSubproperties(WebCustomType webCustomType)
	{
		String caption = null;

		IPropertyType< ? > iPropertyType = webCustomType.getPropertyDescription().getType();
		if (iPropertyType instanceof NGCustomJSONObjectType)
		{
			FlattenedSolution fs = ServoyModelFinder.getServoyModel().getActiveProject().getEditingFlattenedSolution();
			NGCustomJSONObjectType< ? , ? , ? > ngCustomJSONObjectType = (NGCustomJSONObjectType< ? , ? , ? >)iPropertyType;

			ArrayList<PropertyDescription> captionSubProperties = new ArrayList<>();
			captionSubProperties.addAll(ngCustomJSONObjectType.getCustomJSONTypeDefinition().getTaggedProperties(DeveloperUtils.TAG_SHOW_IN_OUTLINE_VIEW));
			captionSubProperties.addAll(
				ngCustomJSONObjectType.getCustomJSONTypeDefinition().getTaggedProperties(DeveloperUtils.TAG_USE_AS_CAPTION_IN_DEVELOPER));

			// we got an unsorted collection, add it to a list and sort it based on captionPriority
			Collections.sort(captionSubProperties, new Comparator<PropertyDescription>()
			{

				@Override
				public int compare(PropertyDescription pd1, PropertyDescription pd2)
				{
					int prio1 = getCaptionPriority(pd1);
					int prio2 = getCaptionPriority(pd2);

					if (prio1 == prio2)
					{
						return pd1.getName().compareTo(pd2.getName());
					}
					else if (prio1 == 0) return 1;
					else if (prio2 == 0) return -1;
					else return prio1 - prio2;
				}

				private int getCaptionPriority(PropertyDescription pd)
				{
					int prio = 0;
					Object prioTag = pd.getTag(DeveloperUtils.TAG_CAPTION_PRIORITY);
					if (prioTag instanceof Number)
					{
						prio = ((Number)prioTag).intValue();
						if (prio < 0) prio = 0;
					}
					return prio;
				}

			});

			for (PropertyDescription captionPD : captionSubProperties)
			{
				Object useAsCaption = captionPD.getTag(DeveloperUtils.TAG_USE_AS_CAPTION_IN_DEVELOPER);
				if (useAsCaption == null) useAsCaption = captionPD.getTag(DeveloperUtils.TAG_SHOW_IN_OUTLINE_VIEW);

				if (useAsCaption instanceof Boolean && ((Boolean)useAsCaption).booleanValue())
				{
					Object propertyValue = webCustomType.getProperty(captionPD.getName());
					if (captionPD.getType() instanceof FormPropertyType && propertyValue != null)
					{
						propertyValue = fs.getForm(propertyValue.toString()).getName();
					}
					if (propertyValue != null)
					{
						caption = String.valueOf(propertyValue).trim();
					}
					else if (!webCustomType.hasProperty(captionPD.getName()) && captionPD.hasDefault())
					{
						caption = String.valueOf(captionPD.getDefaultValue()).trim();
					}
				}

				if (caption != null && caption.length() != 0) break;
				else caption = null;
			}
		}

		return caption;
	}

	/**
	 * Return the input field type that property should inherit.
	 */
	public enum TagAllowerdPropertyInputFieldType
	{
		typeahead, combobox, multipleTypeahead;

		public static boolean stringValue(String s)
		{
			if (s == null) return false;
			else return Arrays.stream(TagAllowerdPropertyInputFieldType.values()).map(i -> i.toString()).collect(Collectors.toList()).contains(s);
		}
	}
}
