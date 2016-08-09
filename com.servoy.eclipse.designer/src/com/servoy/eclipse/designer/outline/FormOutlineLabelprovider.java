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
package com.servoy.eclipse.designer.outline;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.specification.property.IPropertyType;

import com.servoy.eclipse.designer.util.WebFormComponentChildType;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.labelproviders.IPersistLabelProvider;
import com.servoy.eclipse.ui.labelproviders.SupportNameLabelProvider;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.util.ElementUtil;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.LayoutContainer;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.persistence.WebCustomType;
import com.servoy.j2db.server.ngclient.property.types.NGCustomJSONObjectType;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.Utils;

/**
 * Label provider for Servoy form in outline view.
 *
 * @author rgansevles
 */

public class FormOutlineLabelprovider extends LabelProvider implements IPersistLabelProvider, IColorProvider
{
	public static final FormOutlineLabelprovider FORM_OUTLINE_LABEL_PROVIDER_INSTANCE = new FormOutlineLabelprovider();

	@Override
	public Image getImage(Object element)
	{
		if (element == FormOutlineContentProvider.PARTS)
		{
			return Activator.getDefault().loadImageFromOldLocation("parts.gif");
		}
		if (element == FormOutlineContentProvider.ELEMENTS)
		{
			return Activator.getDefault().loadImageFromBundle("element.gif");
		}
		if (element instanceof Pair)
		{
			return (Image)((Pair)element).getRight();
		}
		if (element instanceof PersistContext)
		{
			return getImageForPersist(((PersistContext)element).getPersist());
		}
		if (element instanceof FormElementGroup)
		{
			return Activator.getDefault().loadImageFromBundle("group.gif");
		}
		return super.getImage(element);
	}

	@Override
	public String getText(Object element)
	{
		if (element == FormOutlineContentProvider.PARTS)
		{
			return "parts";
		}
		if (element == FormOutlineContentProvider.ELEMENTS)
		{
			return "elements";
		}
		if (element instanceof Pair)
		{
			return (String)((Pair)element).getLeft();
		}
		if (element instanceof PersistContext)
		{
			if (((PersistContext)element).getPersist() instanceof Part)
			{
				return ((Part)((PersistContext)element).getPersist()).getEditorName();
			}
			if (((PersistContext)element).getPersist() instanceof LayoutContainer)
			{
				LayoutContainer layout = (LayoutContainer)((PersistContext)element).getPersist();
				StringBuilder tag = new StringBuilder("<");
				tag.append(layout.getTagType());
				Map<String, String> attributes = layout.getAttributes();
				for (Entry<String, String> entry : attributes.entrySet())
				{
					tag.append(" ");
					tag.append(entry.getKey());
					if (entry.getValue() != null && entry.getValue().length() > 0)
					{
						tag.append("=\"");
						tag.append(entry.getValue());
						tag.append("\"");
					}
				}
				tag.append(">");
				return tag.toString();
			}
			if (((PersistContext)element).getPersist() instanceof WebCustomType)
			{
				WebCustomType webCustomType = ((WebCustomType)(((PersistContext)element).getPersist()));

				String typeName = webCustomType.getTypeName();
				String postFix = "";
				String webComponentType = null;
				WebComponent wc = (WebComponent)webCustomType.getAncestor(IRepository.WEBCOMPONENTS);
				if (wc != null)
				{
					webComponentType = wc.getTypeName();
				}
				WebObjectSpecification spec = WebComponentSpecProvider.getInstance().getWebComponentSpecification(webComponentType);
				if (spec != null)
				{
					IPropertyType< ? > iPropertyType = spec.getDeclaredCustomObjectTypes().get(typeName);
					if (iPropertyType instanceof NGCustomJSONObjectType)
					{
						NGCustomJSONObjectType ngCustomJSONObjectType = (NGCustomJSONObjectType)iPropertyType;
						Collection<PropertyDescription> taggedProperties = ngCustomJSONObjectType.getCustomJSONTypeDefinition().getTaggedProperties(
							"showInOutlineView");

						//we got an unsorted collection, add it to a list and sort it so that we show consistent labels
						ArrayList<PropertyDescription> asList = new ArrayList<PropertyDescription>(taggedProperties);

						Collections.sort(asList, new Comparator<PropertyDescription>()
						{

							@Override
							public int compare(PropertyDescription o1, PropertyDescription o2)
							{
								return o1.getName().compareTo(o2.getName());
							}

						});
						for (PropertyDescription propertyDescription : asList)
						{
							Object showInOutlineView = propertyDescription.getTag("showInOutlineView");
							if (Boolean.valueOf(showInOutlineView.toString()).booleanValue())
							{
								if (webCustomType.getJson().has(propertyDescription.getName()))
								{
									Object property = webCustomType.getJson().get(propertyDescription.getName());
									postFix += "_" + property;
								}
								else if (propertyDescription.hasDefault())
								{
									postFix += "_" + propertyDescription.getDefaultValue();
								}
							}
						}
					}
				}
				return typeName + postFix;
			}
			if (((PersistContext)element).getPersist() instanceof WebFormComponentChildType)
			{
				return ((WebFormComponentChildType)((PersistContext)element).getPersist()).getKey();
			}

			return SupportNameLabelProvider.INSTANCE_DEFAULT_ANONYMOUS.getText(((PersistContext)element).getPersist());
		}
		if (element instanceof FormElementGroup)
		{
			String name = ((FormElementGroup)element).getName();
			return name == null ? Messages.LabelAnonymous : name;
		}
		return "???";
	}

	public Color getBackground(Object element)
	{
		return null;
	}

	public Color getForeground(Object element)
	{
		if (element instanceof PersistContext && !(((PersistContext)element).getPersist() instanceof WebFormComponentChildType) &&
			Utils.isInheritedFormElement(((PersistContext)element).getPersist(), ((PersistContext)element).getContext()))
		{
			// inherited elements
			return Display.getCurrent().getSystemColor(SWT.COLOR_RED);
		}
		return null;
	}

	public IPersist getPersist(Object value)
	{
		if (value instanceof PersistContext)
		{
			return ((PersistContext)value).getPersist();
		}
		return null;
	}

	protected Image getImageForPersist(IPersist persist)
	{
		IPersist p = persist instanceof WebFormComponentChildType ? ((WebFormComponentChildType)persist).getElement() : persist;
		Pair<String, Image> elementNameAndImage = ElementUtil.getPersistNameAndImage(p);
		String imageName = elementNameAndImage.getLeft();
		if (imageName == null)
		{
			// TODO use the image from the webcomponent spec
			if (p instanceof WebComponent && "servoycore-formcomponent".equals(((WebComponent)p).getTypeName()))
			{
				imageName = "designer.gif";
			}
			else
			{
				imageName = "element.gif";
			}
		}
		else return elementNameAndImage.getRight();
		Image img = Activator.getDefault().loadImageFromOldLocation(imageName);
		if (img == null)
		{
			img = Activator.getDefault().loadImageFromBundle(imageName);
		}
		return img;
	}
}
