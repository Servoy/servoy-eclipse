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

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.designer.editor.rfb.RfbVisualFormEditorDesignPage;
import com.servoy.eclipse.designer.util.DesignerUtil;
import com.servoy.eclipse.model.util.WebFormComponentChildType;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.labelproviders.IPersistLabelProvider;
import com.servoy.eclipse.ui.labelproviders.SupportNameLabelProvider;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.util.DeveloperUtils;
import com.servoy.eclipse.ui.util.ElementUtil;
import com.servoy.j2db.persistence.AbstractContainer;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IChildWebObject;
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
				return DesignerUtil.getLayoutContainerAsString((LayoutContainer)((PersistContext)element).getPersist());
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
				return ((WebFormComponentChildType)((PersistContext)element).getPersist()).getFcPropAndCompPathAsString();
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
		if (element instanceof PersistContext)
		{
			BaseVisualFormEditor editor = DesignerUtil.getActiveEditor();
			if (editor != null && editor.getGraphicaleditor() instanceof RfbVisualFormEditorDesignPage)
			{
				AbstractContainer container = ((RfbVisualFormEditorDesignPage)editor.getGraphicaleditor()).getShowedContainer();
				if (container != null)
				{
					IPersist currentPersist = ((PersistContext)element).getPersist();
					if (container == currentPersist)
					{
						if (UIUtils.isDarkThemeSelected(false))
						{
							return Display.getCurrent().getSystemColor(SWT.COLOR_CYAN);
						}
						return Display.getCurrent().getSystemColor(SWT.COLOR_BLUE);
					}
					if (container.getParent() == currentPersist.getParent())
					{
						if (UIUtils.isDarkThemeSelected(false))
						{
							return Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY);
						}
						return Display.getCurrent().getSystemColor(SWT.COLOR_GRAY);
					}
					boolean isShowedContainer = false;
					if (currentPersist instanceof IChildWebObject)
					{
						currentPersist = currentPersist.getParent();
					}
					while (currentPersist.getParent() instanceof LayoutContainer)
					{
						if (currentPersist.getParent() == container)
						{
							isShowedContainer = true;
							break;
						}
						currentPersist = currentPersist.getParent();
					}
					if (!isShowedContainer)
					{
						if (UIUtils.isDarkThemeSelected(false))
						{
							return Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY);
						}
						return Display.getCurrent().getSystemColor(SWT.COLOR_GRAY);
					}
				}
			}
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
