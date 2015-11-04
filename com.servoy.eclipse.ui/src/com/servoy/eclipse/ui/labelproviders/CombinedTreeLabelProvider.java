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

import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;

import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.dialogs.CombinedTreeContentProvider;
import com.servoy.eclipse.ui.dialogs.CombinedTreeContentProvider.GroupingNode;
import com.servoy.j2db.persistence.IPersist;

/**
 * Combine 2 or more label providers. The first label provider to provide something useful will be used.
 * If the content provider's ({@link CombinedTreeContentProvider}) input specifies that it should grop into categories (so create one parent node for each content/label provider)
 * then those parent nodes will either have no image at all or have the default image of the corresponding label provider if that one implements {@link IDefaultImageProvider}.
 *
 * @see CombinedTreeContentProvider
 * @see IDefaultImageProvider
 *
 * @author rgansevles
 * @author acostescu
 */
public class CombinedTreeLabelProvider implements IFontProvider, IPersistLabelProvider, IDefaultImageProvider
{
	private final ILabelProvider[] labelProviders;
	private final int id;
	private final Image defaultImage;

	public CombinedTreeLabelProvider(int id, ILabelProvider[] labelProviders)
	{
		this(id, labelProviders, null);
	}

	/**
	 * @param id as {@link CombinedTreeContentProvider}s can be nested, they need to know which node belongs to which level (especially {@link CombinedTreeLabelProvider}s need that). So this is a random (unique in the tree) id that should be the same for corresponding content providers and label providers.
	 */
	public CombinedTreeLabelProvider(int id, ILabelProvider[] labelProviders, Image defaultImage)
	{
		this.labelProviders = labelProviders;
		this.id = id;
		this.defaultImage = defaultImage;
	}

	public void addListener(ILabelProviderListener listener)
	{
		for (ILabelProvider lp : labelProviders)
			lp.addListener(listener);
	}

	public void removeListener(ILabelProviderListener listener)
	{
		for (ILabelProvider lp : labelProviders)
			lp.removeListener(listener);
	}

	public void dispose()
	{
		for (ILabelProvider lp : labelProviders)
			lp.dispose();
	}

	public Image getImage(Object element)
	{
		if (element == CombinedTreeContentProvider.NONE) return null;
		if (element instanceof GroupingNode && ((GroupingNode)element).id == id)
		{
			ILabelProvider correspondingLP = labelProviders[((GroupingNode)element).idx];
			if (correspondingLP instanceof IDefaultImageProvider) return ((IDefaultImageProvider)correspondingLP).getDefaultImage();
			else return null;
		}

		for (ILabelProvider lp : labelProviders)
		{
			Image img = lp.getImage(element);
			if (img != null) return img;
		}
		return null;
	}

	public String getText(Object element)
	{
		if (element == CombinedTreeContentProvider.NONE) return Messages.LabelNone;
		if (element instanceof GroupingNode && ((GroupingNode)element).id == id)
		{
			return ((GroupingNode)element).groupingNodeText;
		}

		for (ILabelProvider lp : labelProviders)
		{
			String text = lp.getText(element);
			if (text != null && !"".equals(text))
			{
				return text;
			}
		}
		if (element == null) return Messages.LabelNone;

		return Messages.LabelUnresolved; // should never happen!
	}

	public boolean isLabelProperty(Object element, String property)
	{
		for (ILabelProvider lp : labelProviders)
			if (lp.isLabelProperty(element, property)) return true;

		return false;
	}

	public Font getFont(Object element)
	{
		for (ILabelProvider lp : labelProviders)
		{
			Font font = null;
			if (lp instanceof IFontProvider)
			{
				font = ((IFontProvider)lp).getFont(element);
			}
			if (font != null) return font;
		}
		return null;
	}

	@Override
	public IPersist getPersist(Object value)
	{
		if ((value instanceof GroupingNode && ((GroupingNode)value).id == id) || value == CombinedTreeContentProvider.NONE) return null;
		for (ILabelProvider labelProvider : labelProviders)
		{
			if (labelProvider instanceof IPersistLabelProvider)
			{
				IPersist p = ((IPersistLabelProvider)labelProvider).getPersist(value);
				if (p != null) return p;
			}
		}

		return null;
	}

	@Override
	public Image getDefaultImage()
	{
		return defaultImage;
	}

}
