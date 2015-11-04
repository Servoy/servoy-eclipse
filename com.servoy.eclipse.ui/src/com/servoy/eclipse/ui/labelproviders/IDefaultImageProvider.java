/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2015 Servoy BV

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

import org.eclipse.swt.graphics.Image;

import com.servoy.eclipse.ui.dialogs.CombinedTreeContentProvider;

/**
 * Classes that can provide a default image representing their use can implement this interface.
 * (for example an ILabelProvider that is used as a child in a {@link CombinedTreeContentProvider} with grouping enabled - can give this to be used as the grouping node icon)
 *
 * @author acostescu
 */
public interface IDefaultImageProvider
{

	public Image getDefaultImage();

}
