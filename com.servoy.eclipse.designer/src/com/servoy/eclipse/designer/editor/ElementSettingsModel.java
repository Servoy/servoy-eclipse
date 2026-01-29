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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.PersistFinder;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.util.ElementUtil;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.NameComparator;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.server.ngclient.FormElementHelper;
import com.servoy.j2db.server.ngclient.template.FormTemplateGenerator;
import com.servoy.j2db.server.ngclient.utils.FormComponentUtils;
import com.servoy.j2db.server.ngclient.utils.FormComponentUtils.FCCCHandlerArgs;
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
	private final Map<String, Map<String, Integer>> securityInfo;
	private final Form form;

	public ElementSettingsModel(Form form)
	{
		this.form = form;
		securityInfo = new HashMap<String, Map<String, Integer>>();
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
		String uuidToString = element.getUUID().toString();
		String identifierToString = PersistFinder.INSTANCE.fromPersist(element).toJSONString();

		int access = form.getImplicitSecurityNoRights() ? IRepository.IMPLICIT_FORM_NO_ACCESS : IRepository.IMPLICIT_FORM_ACCESS;// default value
		Map<String, Integer> currentGroupSecurityInfo = securityInfo.get(currentGroup);

		Integer explicitAccessForThisElement = null;

		if (currentGroupSecurityInfo != null)
		{
			explicitAccessForThisElement = currentGroupSecurityInfo.get(identifierToString);
			if (explicitAccessForThisElement == null) explicitAccessForThisElement = currentGroupSecurityInfo.get(uuidToString);
		}

		if (explicitAccessForThisElement != null)
		{
			// we already had it read/used in this editor's map
			access = explicitAccessForThisElement.intValue();
		}
		else
		{
			// read it from the user manager
			List<SecurityInfo> infos = ServoyModelManager.getServoyModelManager().getServoyModel().getUserManager().getSecurityInfos(currentGroup, form);
			if (infos != null)
			{
				// first check using PersistIdentifier - so it works OK for children of form components
				// for other elements it will be identical to the UUID anyway so it will match
				for (SecurityInfo info : infos)
				{
					if (info.element_uid.equals(identifierToString))
					{
						explicitAccessForThisElement = Integer.valueOf(info.access);
						break;
					}

				}
				// check using UUID as it worked before - as it might have been stored like that with old code even for children of from components
				if (explicitAccessForThisElement == null) for (SecurityInfo info : infos)
				{
					if (info.element_uid.equals(uuidToString))
					{
						explicitAccessForThisElement = Integer.valueOf(info.access);
						break;
					}

				}
				if (explicitAccessForThisElement != null) access = explicitAccessForThisElement.intValue();
			}
		}
		return access;
	}

	public void setAccessRight(boolean hasRight, IPersist element, int mask)
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
		Map<String, Integer> currentGroupSecurityInfo = securityInfo.get(currentGroup);
		if (currentGroupSecurityInfo == null)
		{
			currentGroupSecurityInfo = new HashMap<String, Integer>();
			securityInfo.put(currentGroup, currentGroupSecurityInfo);
		}

		String persistIdentifierToString = PersistFinder.INSTANCE.fromPersist(element).toJSONString();
		currentGroupSecurityInfo.remove(uuid.toString());
		currentGroupSecurityInfo.remove(persistIdentifierToString);

		currentGroupSecurityInfo.put(persistIdentifierToString, Integer.valueOf(access));
	}

	public void saveSecurityElements()
	{
		try
		{
			List<IPersist> formElements = getFormElements();
			String solutionName = form.getSolution().getName();
			for (String group : securityInfo.keySet())
			{
				Map<String, Integer> currentGroupSecurityInfo = securityInfo.get(group);
				if (currentGroupSecurityInfo != null)
				{
					for (String uid : currentGroupSecurityInfo.keySet())
					{
						if (formElements.stream().anyMatch(formElement -> (PersistFinder.INSTANCE.fromPersist(formElement).toJSONString().equals(uid) ||
							formElement.getUUID().toString().equals(uid))))
						{
							ServoyModelManager.getServoyModelManager().getServoyModel().getUserManager().setFormSecurityAccess(
								ApplicationServerRegistry.get().getClientId(), group, currentGroupSecurityInfo.get(uid), form.getUUID(), uid, solutionName);
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

		// track names that are already in formElements
		HashSet<String> formElementNames = new java.util.HashSet<String>();

		for (IFormElement elem : elements)
		{
			String name = elem.getName();
			if (name != null && name.length() != 0)
			{
				formElements.add(elem);
				formElementNames.add(name);
				if (FormTemplateGenerator.isWebcomponentBean(elem))
				{
					FormComponentUtils.addFormComponentComponentChildren(elem,
						ModelUtils.getEditingFlattenedSolution(form), true, (FCCCHandlerArgs<Void> args) -> {
							String ownName = ((AbstractBase)args.childFe())
								.getRuntimeProperty(FormElementHelper.FC_CHILD_ELEMENT_NAME_INSIDE_DIRECT_PARENT_FORM_COMPONENT);
							if (ownName != null && ownName.length() != 0)
							{
								if (formElementNames.add(ownName)) formElements.add(args.childFe());
							}
							return null;
						}, null, true);
				}
			}
		}
		// try to find if the form has a parent form and if the parent form has components
		if (form.extendsForm != null)
		{
			elements = form.extendsForm.getFlattenedObjects(NameComparator.INSTANCE);
			for (IFormElement elem : elements)
			{
				String name = elem.getName();
				if (name != null && name.length() != 0)
				{
					// check by name instead of contains(elem)
					if (formElementNames.add(name)) formElements.add(elem);
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
