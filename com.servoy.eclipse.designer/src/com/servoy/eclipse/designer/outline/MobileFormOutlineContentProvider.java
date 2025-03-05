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

import com.servoy.base.persistence.IMobileProperties;
import com.servoy.base.persistence.PersistUtils;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.property.MobileListModel;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IScriptElement;
import com.servoy.j2db.persistence.ISupportFormElement;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.PositionComparator;
import com.servoy.j2db.persistence.RepositoryException;
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
		if (parentElement instanceof FormElementGroup)
		{
			return null;
		}
		if (parentElement == ELEMENTS || parentElement instanceof PersistContext && ((PersistContext)parentElement).getPersist() instanceof Part)
		{
			try
			{
				Form flattenedForm = (Form)getFlattenedWhenForm(form);
				List<Object> nodes = new ArrayList<Object>();
				Set<FormElementGroup> groups = new HashSet<FormElementGroup>();
				Part parentPart = null;
				if (parentElement instanceof PersistContext && ((PersistContext)parentElement).getPersist() instanceof Part)
				{
					parentPart = (Part)((PersistContext)parentElement).getPersist();
				}
				if (flattenedForm != null)
				{
					for (IPersist persist : Utils.iterate(flattenedForm.getAllObjects(PositionComparator.XY_PERSIST_COMPARATOR)))
					{
						if (persist instanceof IScriptElement || isMobilePersist(persist) || persist instanceof Part)
						{
							continue;
						}
						if (parentElement == ELEMENTS && persist instanceof Portal && ((Portal)persist).isMobileInsetList())
						{
							nodes.add(MobileListModel.create(form, (Portal)persist));
							continue;
						}
						if (parentElement == ELEMENTS && persist instanceof ISupportFormElement && ((ISupportFormElement)persist).getGroupID() != null)
						{
							FormElementGroup group = new FormElementGroup(((ISupportFormElement)persist).getGroupID(),
								ModelUtils.getEditingFlattenedSolution(form),
								form);
							if (groups.add(group))
							{
								nodes.add(group);
							}
							continue;
						}
						if (parentPart != null)
						{
							if (PersistUtils.isHeaderPart(parentPart.getPartType()) && !isHeaderMobilePersist(persist))
							{
								continue;
							}
							if (PersistUtils.isFooterPart(parentPart.getPartType()) && !isFooterMobilePersist(persist))
							{
								continue;
							}
						}
						else
						{
							if (isHeaderMobilePersist(persist) || isFooterMobilePersist(persist)) continue;
						}

						nodes.add(PersistContext.create(persist, form));
					}
				}
				return nodes.toArray();
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
	public Object[] getChildren(Object parentElement)
	{
		return getChildrenNonGrouped(parentElement);
	}

	@Override
	public Object getParent(Object element)
	{
		return getParentNonGrouped(element);
	}

	@Override
	public Object getParentNonGrouped(Object element)
	{
		if (element instanceof MobileListModel || element instanceof FormElementGroup) return ELEMENTS;

		if (element instanceof PersistContext)
		{
			IPersist persist = ((PersistContext)element).getPersist();
			if (persist != null)
			{
				if (persist instanceof Part)
				{
					return null;
				}
				if (isHeaderMobilePersist(persist) || isFooterMobilePersist(persist))
				{
					return PersistContext.create(persist.getParent(), ((PersistContext)element).getContext());
				}
			}
			return ELEMENTS;
		}
		return null;
	}

	static ISupportFormElement getGroupMainComponent(FormElementGroup formElementGroup)
	{
		ISupportFormElement component = null;
		for (ISupportFormElement fe : Utils.iterate(formElementGroup.getElements()))
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

	static boolean isHeaderMobilePersist(IPersist persist)
	{
		if (persist instanceof AbstractBase)
		{
			return ((AbstractBase)persist).getCustomMobileProperty(IMobileProperties.HEADER_ITEM.propertyName) != null;
		}
		return false;
	}

	static boolean isFooterMobilePersist(IPersist persist)
	{
		if (persist instanceof AbstractBase)
		{
			return ((AbstractBase)persist).getCustomMobileProperty(IMobileProperties.FOOTER_ITEM.propertyName) != null;
		}
		return false;
	}

	@Override
	public boolean hasChildren(Object element)
	{
		if (element instanceof PersistContext && ((PersistContext)element).getPersist() instanceof Part)
		{
			Part parentPart = (Part)((PersistContext)element).getPersist();
			try
			{
				Form flattenedForm = (Form)getFlattenedWhenForm(form);
				if (flattenedForm != null)
				{
					for (IPersist persist : flattenedForm.getAllObjectsAsList())
					{
						if (PersistUtils.isHeaderPart(parentPart.getPartType()) && isHeaderMobilePersist(persist))
						{
							return true;
						}
						if (PersistUtils.isFooterPart(parentPart.getPartType()) && isFooterMobilePersist(persist))
						{
							return true;
						}
					}
				}
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError("Could not create flattened form for form " + form, e);
				return false;
			}
		}
		return !(element instanceof FormElementGroup) && super.hasChildren(element);
	}

	@Override
	public Object[] getElements(Object inputElement)
	{
		List<Object> elements = new ArrayList<Object>();
		elements.add(ELEMENTS);
		for (Part part : Utils.iterate(form.getParts()))
		{
			if (PersistUtils.isHeaderPart(part.getPartType()))
			{
				elements.add(elements.indexOf(ELEMENTS), PersistContext.create(part, form));
			}
			else if (PersistUtils.isFooterPart(part.getPartType()))
			{
				elements.add(PersistContext.create(part, form));
			}
		}
		return elements.toArray();
	}
}
