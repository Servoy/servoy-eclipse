/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

package com.servoy.eclipse.ui.property;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebObjectFunctionDefinition;
import org.sablo.specification.WebObjectSpecification;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.CSSPositionLayoutContainer;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IBasicWebComponent;
import com.servoy.j2db.persistence.IContentSpecConstants;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.LayoutContainer;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.util.Utils;

/**
 * Properties for ngclient components.
 *
 * @author rgansevles
 *
 */
public class WebComponentPropertySource extends PDPropertySource
{
	private static final Map<String, PropertyDescriptor> BEAN_PROPERTIES = new HashMap<String, PropertyDescriptor>();
	private static final Map<String, PropertyDescriptor> CONTAINER_PROPERTIES = new HashMap<String, PropertyDescriptor>();

	static
	{
		BeanInfo info;
		try
		{
			info = java.beans.Introspector.getBeanInfo(BaseComponent.class);
			for (PropertyDescriptor desc : info.getPropertyDescriptors())
			{
				if (StaticContentSpecLoader.PROPERTY_LOCATION.getPropertyName().equals(desc.getName()) ||
					StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName().equals(desc.getName()) ||
					StaticContentSpecLoader.PROPERTY_COMMENT.getPropertyName().equals(desc.getName()) ||
					StaticContentSpecLoader.PROPERTY_ANCHORS.getPropertyName().equals(desc.getName()) ||
					StaticContentSpecLoader.PROPERTY_CSS_POSITION.getPropertyName().equals(desc.getName()) ||
					StaticContentSpecLoader.PROPERTY_GROUPID.getPropertyName().equals(desc.getName()) ||
					StaticContentSpecLoader.PROPERTY_FORMINDEX.getPropertyName().equals(desc.getName()))
				{
					BEAN_PROPERTIES.put(desc.getName(), desc);
				}
			}

			info = java.beans.Introspector.getBeanInfo(CSSPositionLayoutContainer.class);
			for (PropertyDescriptor desc : info.getPropertyDescriptors())
			{
				if (StaticContentSpecLoader.PROPERTY_TAGTYPE.getPropertyName().equals(desc.getName()))
				{
					BEAN_PROPERTIES.put(desc.getName(), desc);
				}
				if (StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName().equals(desc.getName()) ||
					StaticContentSpecLoader.PROPERTY_CSS_POSITION.getPropertyName().equals(desc.getName()) ||
					StaticContentSpecLoader.PROPERTY_FORMINDEX.getPropertyName().equals(desc.getName()))
				{
					CONTAINER_PROPERTIES.put(desc.getName(), desc);
				}
			}
		}
		catch (IntrospectionException e)
		{
			ServoyLog.logError("Could not load sttaic bean properties", e);
		}
	}

	public WebComponentPropertySource(PersistContext persistContext, boolean readonly, PropertyDescription propertyDescription)
	{
		super(persistContext, readonly, propertyDescription);
		if (!(persistContext.getPersist() instanceof IBasicWebComponent) && !(persistContext.getPersist() instanceof LayoutContainer))
		{
			throw new IllegalArgumentException("Expected persist to be IWebComponent but it is not: " + persistContext.getPersist());
		}
		if (!(propertyDescription instanceof WebObjectSpecification))
		{
			throw new IllegalArgumentException("Expected pd to be WebComponentSpecification but it is not: " + propertyDescription);
		}
	}

	@Override
	protected WebObjectSpecification getPropertyDescription()
	{
		return (WebObjectSpecification)super.getPropertyDescription();
	}

	@Override
	protected IPropertyHandler[] createPropertyHandlers(Object valueObject)
	{
		IPropertyHandler[] tmp1 = super.createPropertyHandlers(valueObject);
		IPropertyHandler[] tmp2 = createComponentSpecificPropertyHandlersFromSpec(getPropertyDescription(), persistContext.getPersist());
		ArrayList<IPropertyHandler> handlers = new ArrayList<>(Arrays.asList(tmp1));
		handlers.addAll(Arrays.asList(tmp2));
		IPropertyHandler groupIDHandler = new BeanPropertyHandler(BEAN_PROPERTIES.get(StaticContentSpecLoader.PROPERTY_GROUPID.getPropertyName()));
		if (groupIDHandler != null) handlers.add(groupIDHandler);

		if (!handlers.stream().anyMatch(handler -> handler.getName().equals(StaticContentSpecLoader.PROPERTY_FORMINDEX.getPropertyName())))
		{
			IPropertyHandler zIndexHandler = new BeanPropertyHandler(BEAN_PROPERTIES.get(StaticContentSpecLoader.PROPERTY_FORMINDEX.getPropertyName()));
			if (zIndexHandler != null) handlers.add(zIndexHandler);
		}

		IPropertyHandler commentHandler = new BeanPropertyHandler(BEAN_PROPERTIES.get(StaticContentSpecLoader.PROPERTY_COMMENT.getPropertyName()));
		if (commentHandler != null) handlers.add(commentHandler);
		return handlers.toArray(new IPropertyHandler[handlers.size()]);
	}


	public static IPropertyHandler[] createComponentSpecificPropertyHandlersFromSpec(PropertyDescription propertyDescription, IPersist persist)
	{
		List<IPropertyHandler> props = new ArrayList<IPropertyHandler>();

		for (PropertyDescription desc : propertyDescription.getProperties().values())
		{
			Object scope = desc.getTag(WebFormComponent.TAG_SCOPE);
			if ("private".equals(scope) || "runtime".equals(scope))
			{
				// only show design properties
				continue;
			}

			PropertyDescriptor beanPropertyDescriptor = BEAN_PROPERTIES.get(desc.getName());
			if (persist instanceof LayoutContainer && CONTAINER_PROPERTIES.containsKey(desc.getName()))
			{
				beanPropertyDescriptor = CONTAINER_PROPERTIES.get(desc.getName());
			}
			if (beanPropertyDescriptor != null)
			{
				// handled by bean, not in web component spec
				props.add(new BeanPropertyHandler(beanPropertyDescriptor));
			}
		}
		if (persist instanceof CSSPositionLayoutContainer)
		{
			props.add(new BeanPropertyHandler(CONTAINER_PROPERTIES.get("cssPosition")));
		}
		if (propertyDescription instanceof WebObjectSpecification)
		{
			for (WebObjectFunctionDefinition desc : ((WebObjectSpecification)propertyDescription).getHandlers().values())
			{
				props.add(new WebComponentPropertyHandler(desc.getAsPropertyDescription()));
			}
		}

		return props.toArray(new IPropertyHandler[props.size()]);
	}

	/*
	 * Properties from spec should be dispayed under "Component" category except for handlers and BEAN_PROPERTIES. Properties found with reflection are handled
	 * by the super class (they go under "Properties").
	 */
	@Override
	protected PropertyCategory createPropertyCategory(PropertyDescriptorWrapper propertyDescriptor)
	{
		if (BEAN_PROPERTIES.containsKey(propertyDescriptor.propertyDescriptor.getName())) return super.createPropertyCategory(propertyDescriptor);
		if (getPropertyDescription().getHandlers().containsKey(propertyDescriptor.propertyDescriptor.getName())) return PropertyCategory.Events;
		if (getPropertyDescription().getProperties().containsKey(propertyDescriptor.propertyDescriptor.getName()) &&
			!IContentSpecConstants.PROPERTY_ATTRIBUTES.equals(propertyDescriptor.propertyDescriptor.getName())) return PropertyCategory.Component;
		return super.createPropertyCategory(propertyDescriptor);
	}

	@Override
	public String toString()
	{
		String packageName = getPropertyDescription().getPackageName();
		String parentFormString = "";
		if (WebComponentSpecProvider.isLoaded())
		{
			String packageDisplayName = WebComponentSpecProvider.getSpecProviderState().getPackageDisplayName(packageName);
			if (!Utils.stringIsEmpty(packageDisplayName)) packageName = packageDisplayName;
		}
		Form contextForm = (Form)getContext().getAncestor(IRepository.FORMS);
		Form parentForm = (Form)getPersist().getAncestor(IRepository.FORMS);
		if (contextForm != null && parentForm != null && !parentForm.getUUID().equals(contextForm.getUUID()))
		{
			parentFormString = " [" + parentForm.getName() + ']';
		}
		return getPropertyDescription().getDisplayName() + parentFormString + " (" + packageName + ")";
	}
}
