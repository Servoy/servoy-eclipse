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
package com.servoy.eclipse.designer.editor;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.graphics.Image;

import com.servoy.eclipse.ui.Activator;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.server.ngclient.FormElementHelper;

/**
 * Label provider for setting security checkboxes in form editor security page.
 *
 * @author lvostinar
 */
public class ElementSettingsLabelProvider
{
	public static final Image TRUE_IMAGE = Activator.getDefault().loadImageFromBundle("check_on.png");
	public static final Image FALSE_IMAGE = Activator.getDefault().loadImageFromBundle("check_off.png");
	public static final Image DISABLED_IMAGE = Activator.getDefault().loadImageFromBundle("check_disabled_off.png");

	private final ElementSettingsModel model;

	public ElementSettingsLabelProvider(ElementSettingsModel model)
	{
		this.model = model;
	}


	/**
	 * @return one of {@link #TRUE_IMAGE}, {@link #FALSE_IMAGE} or {@link #DISABLED_IMAGE}
	 */
	public static Image getViewableCheckImg(Object element, ElementSettingsModel model)
	{
		// check parent form component component security rights - if applicable / present
		IPersist parentFCC = (IPersist)element;
		while ((parentFCC = ((AbstractBase)parentFCC)
			.getRuntimeProperty(FormElementHelper.FC_DIRECT_PARENT_FORM_COMPONENT_CONTAINER)) != null)
		{
			if (!model.hasRight(parentFCC, IRepository.VIEWABLE))
			{
				return DISABLED_IMAGE;
			}
		}

		if (model.hasRight((IPersist)element, IRepository.VIEWABLE))
		{
			return TRUE_IMAGE;
		}
		else
		{
			return FALSE_IMAGE;
		}
	}

	/**
	 * @return one of {@link #TRUE_IMAGE}, {@link #FALSE_IMAGE} or {@link #DISABLED_IMAGE}
	 */
	public static Image getAccessibleCheckImg(Object element, ElementSettingsModel model)
	{
		IPersist parentFCC = (IPersist)element;
		while ((parentFCC = ((AbstractBase)parentFCC)
			.getRuntimeProperty(FormElementHelper.FC_DIRECT_PARENT_FORM_COMPONENT_CONTAINER)) != null)
		{
			if (!model.hasRight(parentFCC, IRepository.ACCESSIBLE))
			{
				return DISABLED_IMAGE;
			}
		}

		if (model.hasRight((IPersist)element, IRepository.ACCESSIBLE))
		{
			return TRUE_IMAGE;
		}
		else
		{
			if (!model.hasRight((IPersist)element, IRepository.VIEWABLE))
			{
				return DISABLED_IMAGE;
			}
			return FALSE_IMAGE;
		}
	}

	public CellLabelProvider getCellLabelProvider(int columnIndex)
	{
		if (columnIndex == VisualFormEditorSecurityPage.CI_VIEWABLE)
		{
			return new ColumnLabelProvider()
			{
				@Override
				public String getToolTipText(Object element)
				{
					Image img = getViewableCheckImg(element, model);
					return (img == DISABLED_IMAGE)
						? "a Form Component Component that contains this component is\nnot viewable, so this will not be viewable either" : null;
				}

				@Override
				public String getText(Object element)
				{
					return null;
				}

				@Override
				public Image getImage(Object element)
				{
					return getViewableCheckImg(element, model);
				}
			};
		}
		else if (columnIndex == VisualFormEditorSecurityPage.CI_ACCESSABLE)
		{
			return new ColumnLabelProvider()
			{
				@Override
				public String getToolTipText(Object element)
				{
					Image img = getAccessibleCheckImg(element, model);
					// @formatter:off
					return (img == DISABLED_IMAGE) ?
						(
							!model.hasRight((IPersist)element, IRepository.VIEWABLE) ?
								"this component is not 'viewable' so there\nis no use to configure 'accessible'" :
								"a Form Component Component that contains this component is\nnot 'accessible', so this will not be 'accesible' either") :
						null;
					// @formatter:on
				}

				@Override
				public String getText(Object element)
				{
					return null;
				}

				@Override
				public Image getImage(Object element)
				{
					return getAccessibleCheckImg(element, model);
				}
			};
		}
		else if (columnIndex == VisualFormEditorSecurityPage.CI_NAME)
		{
			return new ColumnLabelProvider()
			{

				@Override
				public String getText(Object element)
				{
					String name = ((ISupportName)element).getName();
					IPersist directParentFormComponentComponentIfApplicable = ((AbstractBase)element)
						.getRuntimeProperty(FormElementHelper.FC_DIRECT_PARENT_FORM_COMPONENT_CONTAINER);
					if (directParentFormComponentComponentIfApplicable != null)
					{
						name = ((AbstractBase)element)
							.getRuntimeProperty(FormElementHelper.FC_CHILD_ELEMENT_NAME_INSIDE_DIRECT_PARENT_FORM_COMPONENT);
						StringBuilder sb = new StringBuilder(50);
						sb.append("[] ");
						boolean notLast = false;
						while (directParentFormComponentComponentIfApplicable != null)
						{
							if (notLast) sb.insert(1, " -> ");
							if (notLast == false) notLast = true;

							String fccName = ((AbstractBase)directParentFormComponentComponentIfApplicable)
								.getRuntimeProperty(FormElementHelper.FC_CHILD_ELEMENT_NAME_INSIDE_DIRECT_PARENT_FORM_COMPONENT);
							if (fccName == null) fccName = ((ISupportName)directParentFormComponentComponentIfApplicable).getName();
							sb.insert(1, fccName);
							directParentFormComponentComponentIfApplicable = ((AbstractBase)directParentFormComponentComponentIfApplicable)
								.getRuntimeProperty(FormElementHelper.FC_DIRECT_PARENT_FORM_COMPONENT_CONTAINER);
						}
						sb.append(name);
						name = sb.toString();
					}
					else if (element == model.getForm()) name = "[this form] " + name;

					return name;
				}

			};
		}

		else return null;
	}
}
