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

import org.eclipse.swt.graphics.Image;

import com.servoy.eclipse.core.util.DatabaseUtils;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.dialogs.RelationContentProvider.RelationsWrapper;
import com.servoy.eclipse.ui.util.UnresolvedValue;
import com.servoy.j2db.persistence.PersistEncapsulation;
import com.servoy.j2db.persistence.Relation;

/**
 * Label provider for relations.
 * 
 * @author rgansevles
 * 
 */
public class RelationLabelProvider extends SupportNameLabelProvider implements IPersistLabelProvider
{
	public static final RelationLabelProvider INSTANCE_LAST_NAME_ONLY_NO_IMAGE = new RelationLabelProvider(Messages.LabelNone, false, true);
	public static final RelationLabelProvider INSTANCE_ALL_NO_IMAGE = new RelationLabelProvider(Messages.LabelNone, false, false);
	public static final RelationLabelProvider INSTANCE_LAST_NAME_ONLY = new RelationLabelProvider(Messages.LabelNone, true, true);
	public static final RelationLabelProvider INSTANCE_ALL = new RelationLabelProvider(Messages.LabelNone, true, false);
	
	private static final Image RELATION_IMAGE = Activator.getDefault().loadImageFromBundle("relation.gif");
	private static final Image GLOBAL_RELATION_IMAGE = Activator.getDefault().loadImageFromBundle("global_relation.gif");
	private static final Image RELATION_PROTECTED_IMAGE = Activator.getDefault().loadImageFromBundle("relation_protected.gif");
	private static final Image GLOBAL_RELATION_PROTECTED_IMAGE = Activator.getDefault().loadImageFromBundle("global_relation_protected.gif");

	private final boolean lastNameOnly;
	private final boolean showImage;

	public RelationLabelProvider(String defaultText, boolean showImage, boolean lastNameOnly)
	{
		super(defaultText);
		this.showImage = showImage;
		this.lastNameOnly = lastNameOnly;
	}

	@Override
	public String getText(Object value)
	{
		if (value instanceof Relation[] && ((Relation[])value).length > 0)
		{
			if (lastNameOnly)
			{
				return ((Relation[])value)[((Relation[])value).length - 1].getName();
			}
			return DatabaseUtils.getRelationsString((Relation[])value);
		}
		if (value instanceof RelationsWrapper)
		{
			return getText(((RelationsWrapper)value).relations);
		}
		if (value instanceof UnresolvedValue)
		{
			return ((UnresolvedValue)value).getUnresolvedMessage();
		}
		return super.getText(value);
	}

	@Override
	public Image getImage(Object element)
	{
		if (showImage)
		{
			if (element instanceof Relation)
			{
				if (PersistEncapsulation.isModulePrivate((Relation)element, null))
				{
					return ((Relation)element).isGlobal() ? GLOBAL_RELATION_PROTECTED_IMAGE : RELATION_PROTECTED_IMAGE;
				}
				return ((Relation)element).isGlobal() ? GLOBAL_RELATION_IMAGE : RELATION_IMAGE;
			}
			if (element instanceof Relation[] && ((Relation[])element).length > 0)
			{
				Relation[] relations = (Relation[])element;
				return getImage(relations[lastNameOnly ? relations.length - 1 : 0]);
			}
			if (element instanceof RelationsWrapper)
			{
				return getImage(((RelationsWrapper)element).relations);
			}
		}
		return super.getImage(element);
	}
}
