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

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import com.servoy.eclipse.core.elements.ElementFactory.RelatedForm;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.dialogs.RelationContentProvider;
import com.servoy.eclipse.ui.util.UnresolvedValue;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.Table;

/**
 * label provider for forms with relations.
 * 
 * @author rob
 * 
 */
public class RelatedFormsLabelProvider extends LabelProvider
{
	public static final RelatedFormsLabelProvider INSTANCE = new RelatedFormsLabelProvider(true);
	public static final RelatedFormsLabelProvider INSTANCE_NO_IMAGE = new RelatedFormsLabelProvider(false);

	private final boolean showImage;

	public RelatedFormsLabelProvider(boolean showImage)
	{
		this.showImage = showImage;
	}

	@Override
	public String getText(Object element)
	{
		if (RelationContentProvider.NONE.equals(element))
		{
			return Messages.LabelNone;
		}
		if (element instanceof UnresolvedValue)
		{
			return ((UnresolvedValue)element).getUnresolvedMessage();
		}
		if (element instanceof RelatedForm)
		{
			if (((RelatedForm)element).form == null)
			{
				return RelationLabelProvider.INSTANCE_LAST_NAME_ONLY.getText(((RelatedForm)element).relations);
			}
			return getText(((RelatedForm)element).form);
		}

		if (element instanceof Table)
		{
			return ((Table)element).getName();
		}

		return super.getText(element);
	}

	@Override
	public Image getImage(Object element)
	{
		if (showImage)
		{
			if (element instanceof RelatedForm)
			{
				if (((RelatedForm)element).form == null)
				{
					return RelationLabelProvider.INSTANCE_LAST_NAME_ONLY.getImage(((RelatedForm)element).relations);
				}
				return getImage(((RelatedForm)element).form);
			}

			Image image = null;
			if (element instanceof Table)
			{
				image = Activator.getDefault().loadImageFromBundle("portal.gif"); //$NON-NLS-1$
			}
			else if (element instanceof Form)
			{
				image = Activator.getDefault().loadImageFromBundle("designer.gif"); //$NON-NLS-1$
			}
			if (image != null) return image;
		}
		return super.getImage(element);
	}
}