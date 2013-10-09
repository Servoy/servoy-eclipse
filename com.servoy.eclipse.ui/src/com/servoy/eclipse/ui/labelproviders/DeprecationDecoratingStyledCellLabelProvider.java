/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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


import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.swt.graphics.TextStyle;

import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.ui.util.IDeprecationProvider;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportDeprecated;
import com.servoy.j2db.persistence.ISupportDeprecatedAnnotation;
import com.servoy.j2db.util.IDelegate;

/**
 * Label provider with strikeout support for deprecated elements.
 * 
 * @author rgansevles
 *
 */
public class DeprecationDecoratingStyledCellLabelProvider extends DelegatingDecoratingStyledCellLabelProvider
{
	private static final Styler STRIKEOUT_STYLER = new Styler()
	{
		@Override
		public void applyStyles(TextStyle textStyle)
		{
			textStyle.strikeout = true;
		}
	};

	/**
	 * @param labelProvider
	 */
	public DeprecationDecoratingStyledCellLabelProvider(IBaseLabelProvider labelProvider)
	{
		super(labelProvider);
		setOwnerDrawEnabled(false);
	}

	@Override
	public StyledString getStyledText(Object element)
	{
		String text = getText(element);
		if (text == null) return new StyledString("");

		StyledString styledText = new StyledString(text);

		boolean useDeprecated = false;
		boolean checkMore = true;
		// check if element supports deprecated;
		if (element instanceof ISupportDeprecated)
		{
			useDeprecated = ((ISupportDeprecated)element).getDeprecated() != null;
			checkMore = false;
		}
		else if (element instanceof ISupportDeprecatedAnnotation)
		{
			useDeprecated = ((ISupportDeprecatedAnnotation)element).isDeprecated();
			checkMore = false;
		}

		// then ask (inner) label providers
		Object innerLabelprovider = getStyledStringProvider();
		while (checkMore && !useDeprecated && innerLabelprovider != null)
		{
			if (innerLabelprovider instanceof IDeprecationProvider)
			{
				Boolean isDeprecated = ((IDeprecationProvider)innerLabelprovider).isDeprecated(element);
				if (isDeprecated != null)
				{
					checkMore = false;
					useDeprecated = isDeprecated.booleanValue();
				}
			}
			if (innerLabelprovider instanceof IDelegate< ? >)
			{
				innerLabelprovider = ((IDelegate< ? >)innerLabelprovider).getDelegate();
			}
			else
			{
				innerLabelprovider = null;
			}
		}

		if (checkMore && !useDeprecated)
		{
			// then go via adapter fw
			ISupportDeprecated supportDeprecated = ModelUtils.getAdapter(element, ISupportDeprecated.class);
			if (supportDeprecated == null)
			{
				// try via IPersist
				supportDeprecated = ModelUtils.getAdapter(ModelUtils.getAdapter(element, IPersist.class), ISupportDeprecated.class);
			}

			useDeprecated = supportDeprecated != null && supportDeprecated.getDeprecated() != null;
		}

		if (useDeprecated)
		{
			styledText.setStyle(0, styledText.length(), STRIKEOUT_STYLER);
		}
		return styledText;
	}
}
