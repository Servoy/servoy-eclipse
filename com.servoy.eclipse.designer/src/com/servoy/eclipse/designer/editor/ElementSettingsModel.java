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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebObjectSpecification;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.util.ElementUtil;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.NameComparator;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.FormElementHelper;
import com.servoy.j2db.server.ngclient.property.types.FormComponentPropertyType;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.SecurityInfo;
import com.servoy.j2db.util.UUID;

/**
 * Model for setting security checkboxes in form editor security page.
 *
 * @author lvostinar
 */

public class ElementSettingsModel
{
	private String currentGroup;
	private final Map<String, Map<UUID, Integer>> securityInfo;
	private final Form form;

	public ElementSettingsModel(Form form)
	{
		this.form = form;
		securityInfo = new HashMap<String, Map<UUID, Integer>>();
	}

	public void setCurrentGroup(String group)
	{
		this.currentGroup = group;
	}

	public boolean hasRight(IPersist element, int mask)
	{
		int access = getAccess(element);
		return (access & mask) != 0;
	}

	private int getAccess(IPersist element)
	{
		UUID uuid = element.getUUID();
		int access = form.getImplicitSecurityNoRights() ? IRepository.IMPLICIT_FORM_NO_ACCESS : IRepository.IMPLICIT_FORM_ACCESS;// default value
		Map<UUID, Integer> currentGroupSecurityInfo = securityInfo.get(currentGroup);
		if (currentGroupSecurityInfo != null && currentGroupSecurityInfo.containsKey(uuid))
		{
			access = currentGroupSecurityInfo.get(uuid).intValue();
		}
		else
		{
			List<SecurityInfo> infos = ServoyModelManager.getServoyModelManager().getServoyModel().getUserManager().getSecurityInfos(currentGroup, form);
			if (infos != null)
			{
				for (SecurityInfo info : infos)
				{
					if (info.element_uid.equals(uuid.toString()))
					{
						access = info.access;
						break;
					}

				}
			}
		}
		return access;
	}

	public void setRight(boolean hasRight, IPersist element, int mask)
	{
		int access = getAccess(element);
		if (hasRight) access = access | mask;
		else access = access & (~mask);
		UUID uuid = element.getUUID();
		try
		{
			element = ElementUtil.getOverridePersist(PersistContext.create(element, form));
			if (!uuid.equals(element.getUUID()))
			{
				ServoyModelManager.getServoyModelManager().getServoyModel().firePersistChanged(false, element.getAncestor(IRepository.FORMS), false);
			}
		}
		catch (RepositoryException ex)
		{
			ServoyLog.logError(ex);
		}
		Map<UUID, Integer> currentGroupSecurityInfo = securityInfo.get(currentGroup);
		if (currentGroupSecurityInfo == null)
		{
			currentGroupSecurityInfo = new HashMap<UUID, Integer>();
			securityInfo.put(currentGroup, currentGroupSecurityInfo);
		}
		currentGroupSecurityInfo.remove(uuid);
		currentGroupSecurityInfo.put(element.getUUID(), new Integer(access));
	}

	public void saveSecurityElements()
	{
		try
		{
			List<IPersist> formElements = getFormElements();
			String solutionName = form.getSolution().getName();
			for (String group : securityInfo.keySet())
			{
				Map<UUID, Integer> currentGroupSecurityInfo = securityInfo.get(group);
				if (currentGroupSecurityInfo != null)
				{
					for (UUID uuid : currentGroupSecurityInfo.keySet())
					{
						if (formElements.stream().anyMatch(formElement -> formElement.getUUID().equals(uuid)))
						{
							ServoyModelManager.getServoyModelManager().getServoyModel().getUserManager().setFormSecurityAccess(
								ApplicationServerRegistry.get().getClientId(), group, currentGroupSecurityInfo.get(uuid), uuid, solutionName);
						}

					}
				}
			}
			securityInfo.clear();
		}
		catch (Exception ex)
		{
			ServoyLog.logError(ex);
		}
	}

	public List<IPersist> getFormElements()
	{
		ArrayList<IPersist> formElements = new ArrayList<IPersist>();
		List<IFormElement> elements = form.getFlattenedObjects(NameComparator.INSTANCE);
		for (IFormElement elem : elements)
		{
			if (elem.getName() != null && elem.getName().length() != 0)
			{
				formElements.add(elem);
				FormElement formComponentEl = FormElementHelper.INSTANCE.getFormElement(elem, ModelUtils.getEditingFlattenedSolution(form),
					null, true);
				WebObjectSpecification spec = formComponentEl.getWebComponentSpec();
				if (spec != null)
				{
					Collection<PropertyDescription> properties = spec.getProperties(FormComponentPropertyType.INSTANCE);
					if (properties.size() > 0)
					{
						for (PropertyDescription pd : properties)
						{
							Object propertyValue = formComponentEl.getPropertyValue(pd.getName());
							Form frm = FormComponentPropertyType.INSTANCE.getForm(propertyValue, ModelUtils.getEditingFlattenedSolution(form));
							if (frm != null)
							{
								List<IFormElement> formComponentElements = frm.getFlattenedObjects(NameComparator.INSTANCE);
								for (IFormElement formCompElement : formComponentElements)
								{
									if (formCompElement.getName() != null && formCompElement.getName().length() != 0)
									{
										formElements.add(formCompElement);
									}
								}
							}
						}
					}
				}
			}
		}
		formElements.add(0, form);
		return formElements;
	}

	public Form getForm()
	{
		return form;
	}
}
