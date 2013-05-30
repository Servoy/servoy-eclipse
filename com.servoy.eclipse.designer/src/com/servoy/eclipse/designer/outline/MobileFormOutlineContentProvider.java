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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.graphics.Image;

import com.servoy.base.persistence.IMobileProperties;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.property.MobileListModel;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.util.ElementUtil;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IScriptElement;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.SortedList;
import com.servoy.j2db.util.Utils;

/**
 * Content provider for Servoy mobile form in outline view.
 * 
 * @author gboros
 */

public class MobileFormOutlineContentProvider extends FormOutlineContentProvider
{
	public MobileFormOutlineContentProvider(Form form)
	{
		super(form);
	}

	@Override
	public Object[] getChildrenNonGrouped(Object parentElement)
	{
		if (parentElement == ELEMENTS || parentElement == PARTS)
		{
			try
			{
				Form flattenedForm = (Form)getFlattenedWhenForm(form);
				List<Object> nodes = new ArrayList<Object>();
				Set<FormElementGroup> groups = new HashSet<FormElementGroup>();
				if (flattenedForm != null)
				{
					for (IPersist persist : flattenedForm.getAllObjectsAsList())
					{
						if (persist instanceof IScriptElement ||
							(persist instanceof Part && ((Part)persist).getPartType() != Part.HEADER && ((Part)persist).getPartType() != Part.TITLE_HEADER &&
								((Part)persist).getPartType() != Part.FOOTER && ((Part)persist).getPartType() != Part.TITLE_FOOTER) || isMobilePersist(persist))
						{
							continue;
						}
						if (parentElement == ELEMENTS && persist instanceof Portal && ((Portal)persist).isMobileInsetList())
						{
							nodes.add(MobileListModel.create(form, (Portal)persist));
							continue;
						}
						if (parentElement == ELEMENTS && persist instanceof IFormElement && ((IFormElement)persist).getGroupID() != null)
						{
							FormElementGroup group = new FormElementGroup(((IFormElement)persist).getGroupID(), ModelUtils.getEditingFlattenedSolution(form),
								form);
							if (groups.add(group))
							{
								nodes.add(group);
							}
							continue;
						}
						if ((parentElement == PARTS) != (persist instanceof Part))
						{
							continue;
						}

						nodes.add(PersistContext.create(persist, form));
					}
				}
				return (parentElement == ELEMENTS) ? new SortedList(PersistContextNameComparator.INSTANCE, nodes).toArray() : nodes.toArray();
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError("Could not create flattened form for form " + form, e);
			}
			return null;
		}

		return super.getChildrenNonGrouped(parentElement);
	}

	@Override
	public Object[] getChildrenGrouped(Object parentElement)
	{
		if (parentElement == ELEMENTS || parentElement == PARTS)
		{
			HashSet<Object> availableCategories = null;
			try
			{
				Form flattenedForm = (Form)getFlattenedWhenForm(form);
				if (flattenedForm != null)
				{
					availableCategories = new HashSet<Object>();
					for (IPersist persist : flattenedForm.getAllObjectsAsList())
					{
						if (persist instanceof IScriptElement || isMobilePersist(persist))
						{
							continue;
						}
						if (parentElement == ELEMENTS && persist instanceof IFormElement)
						{
							availableCategories.add(ElementUtil.getPersistNameAndImage(persist));
							continue;
						}
						if ((parentElement == PARTS) &&
							(persist instanceof Part) &&
							(((Part)persist).getPartType() == Part.HEADER || ((Part)persist).getPartType() == Part.TITLE_HEADER ||
								((Part)persist).getPartType() == Part.FOOTER || ((Part)persist).getPartType() == Part.TITLE_FOOTER))
						{
							availableCategories.add(PersistContext.create(persist, form));
							continue;
						}
					}
				}
				return availableCategories.toArray();
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError("Could not create flattened form for form " + form, e); //$NON-NLS-1$
			}
			return null;
		}
		else if (parentElement instanceof Pair)
		{
			try
			{
				Form flattenedForm = (Form)getFlattenedWhenForm(form);
				List<Object> nodes = new ArrayList<Object>();
				if (flattenedForm != null)
				{
					for (IPersist persist : flattenedForm.getAllObjectsAsList())
					{
						if (persist instanceof IScriptElement || persist instanceof Part || isMobilePersist(persist))
						{
							continue;
						}
						if (persist instanceof IFormElement)
						{
							Pair<String, Image> nameAndImage = ElementUtil.getPersistNameAndImage(persist);
							if (nameAndImage.equals(parentElement))
							{
								if (((IFormElement)persist).getGroupID() != null) nodes.add(new FormElementGroup(((IFormElement)persist).getGroupID(),
									ModelUtils.getEditingFlattenedSolution(form), form));
								else if (persist instanceof Portal && ((Portal)persist).isMobileInsetList()) nodes.add(MobileListModel.create(form,
									(Portal)persist));
								else nodes.add(PersistContext.create(persist, form));
							}
						}
					}
				}
				return new SortedList(PersistContextNameComparator.INSTANCE, nodes).toArray();
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError("Could not create flattened form for form " + form, e); //$NON-NLS-1$
			}
		}

		return super.getChildrenGrouped(parentElement);
	}

	@Override
	public Object getParentNonGrouped(Object element)
	{
		if (element instanceof MobileListModel || element instanceof FormElementGroup) return ELEMENTS;
		else return super.getParentNonGrouped(element);
	}

	@Override
	public Object getGroupedParent(Object element)
	{
		if (element instanceof MobileListModel) return ElementUtil.getPersistNameAndImage(((MobileListModel)element).component);
		else if (element instanceof FormElementGroup)
		{
			return ElementUtil.getPersistNameAndImage(getGroupMainComponent((FormElementGroup)element));
		}
		else return super.getGroupedParent(element);
	}

	static IFormElement getGroupMainComponent(FormElementGroup formElementGroup)
	{
		IFormElement component = null;
		for (IFormElement fe : Utils.iterate(formElementGroup.getElements()))
		{
			if (fe instanceof AbstractBase && ((AbstractBase)fe).getCustomMobileProperty(IMobileProperties.COMPONENT_TITLE.propertyName) != null) continue;
			else component = fe;
		}
		return component;
	}

	static boolean isMobilePersist(IPersist persist)
	{
		if (persist instanceof AbstractBase)
		{
			AbstractBase ab = (AbstractBase)persist;
			return ab.getCustomMobileProperty(IMobileProperties.LIST_ITEM_HEADER.propertyName) != null ||
				ab.getCustomMobileProperty(IMobileProperties.LIST_ITEM_BUTTON.propertyName) != null ||
				ab.getCustomMobileProperty(IMobileProperties.LIST_ITEM_SUBTEXT.propertyName) != null ||
				ab.getCustomMobileProperty(IMobileProperties.LIST_ITEM_COUNT.propertyName) != null ||
				ab.getCustomMobileProperty(IMobileProperties.LIST_ITEM_IMAGE.propertyName) != null ||
				ab.getCustomMobileProperty(IMobileProperties.COMPONENT_TITLE.propertyName) != null;
		}
		return false;
	}

	@Override
	public boolean hasChildren(Object element)
	{
		return !(element instanceof FormElementGroup) && super.hasChildren(element);
	}
}
