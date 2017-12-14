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

import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import com.servoy.eclipse.designer.util.DeveloperUtils;
import com.servoy.eclipse.designer.util.WebFormComponentChildType;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.labelproviders.IPersistLabelProvider;
import com.servoy.eclipse.ui.labelproviders.SupportNameLabelProvider;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.util.ElementUtil;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.LayoutContainer;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.persistence.WebCustomType;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.Utils;

/**
 * Label provider for Servoy form in outline view.
 *
 * @author rgansevles
 */

public class FormOutlineLabelprovider extends ColumnLabelProvider implements IPersistLabelProvider, IColorProvider
{

	public static final FormOutlineLabelprovider FORM_OUTLINE_LABEL_PROVIDER_INSTANCE = new FormOutlineLabelprovider();

	@Override
	public Image getImage(Object element)
	{
		if (element == FormOutlineContentProvider.PARTS)
		{
			return Activator.getDefault().loadImageFromBundle("parts.png");
		}
		if (element == FormOutlineContentProvider.ELEMENTS)
		{
			return Activator.getDefault().loadImageFromBundle("element.png");
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
			return Activator.getDefault().loadImageFromBundle("group.png");
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
				Map<String, String> attributes = layout.getMergedAttributes();
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
				if (layout.getName() != null)
				{
					tag.append("[");
					tag.append(layout.getName());
					tag.append("]");
				}
				return tag.toString();
			}
			if (((PersistContext)element).getPersist() instanceof WebCustomType)
			{
				WebCustomType webCustomType = ((WebCustomType)(((PersistContext)element).getPersist()));
				String captionFromSubProp = DeveloperUtils.getCustomObjectTypeCaptionFromTaggedSubproperties(webCustomType);
				return webCustomType.getJsonKey() + (webCustomType.getIndex() >= 0 ? "[" + webCustomType.getIndex() + "]" : "") +
					(captionFromSubProp != null ? " (" + captionFromSubProp + ")" : "");

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

	@Override
	public Color getBackground(Object element)
	{
		return null;
	}

	@Override
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
				imageName = "designer.png";
			}
			else
			{
				imageName = "element.png";
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

	@Override
	public String getToolTipText(Object element)
	{
		IPersist persist = null;
		if (element instanceof PersistContext)
		{
			persist = ((PersistContext)element).getPersist();
		}
		else if (element instanceof IPersist)
		{
			persist = (IPersist)element;
		}
		if (persist instanceof ISupportName)
		{
			return ((ISupportName)persist).getName();
		}
		return null;
	}
}
