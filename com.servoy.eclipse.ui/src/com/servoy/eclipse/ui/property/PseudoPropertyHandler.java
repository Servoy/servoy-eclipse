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

package com.servoy.eclipse.ui.property;

import java.util.Map;

import org.eclipse.ui.views.properties.IPropertySource;
import org.sablo.specification.PropertyDescription;

import com.servoy.j2db.documentation.ClientSupport;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.RepositoryHelper;
import com.servoy.j2db.persistence.Solution;

/**
 * Handler for some known pseudo proeties/
 * 
 * @author rgansevles
 *
 */
public class PseudoPropertyHandler implements IPropertyHandler
{
	// null type: use property controller internally
	public static final PropertyDescription LOGIN_SOLUTION_DESCRIPTION = new PropertyDescription("loginSolutionName", null,
		new LoginSolutionPropertyController("loginSolutionName", RepositoryHelper.getDisplayName("loginSolutionName", Solution.class)));

	// null type: use property controller internally
	public static final PropertyDescription DESIGN_PROPERTIES_DESCRIPTION = new PropertyDescription("designTimeProperties", null,
		new PropertySetterDelegatePropertyController<Map<String, Object>, PersistPropertySource>(new MapEntriesPropertyController("designTimeProperties",
			RepositoryHelper.getDisplayName("designTimeProperties", Form.class)), "designTimeProperties")
		{
			@Override
			public Map<String, Object> getProperty(PersistPropertySource propSource)
			{
				IPersist persist = propSource.getPersist();
				if (persist instanceof AbstractBase)
				{
					return ((AbstractBase)persist).getMergedCustomDesignTimeProperties(); // returns non-null map with copied/merged values, may be written to
				}
				return null;
			}

			public void setProperty(PersistPropertySource propSource, Map<String, Object> value)
			{
				IPersist persist = propSource.getPersist();
				if (persist instanceof AbstractBase)
				{
					((AbstractBase)persist).setUnmergedCustomDesignTimeProperties(value);
				}
			}
		});


	private final String name;

	public PseudoPropertyHandler(String name)
	{
		this.name = name;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public boolean isProperty()
	{
		return true;
	}

	@Override
	public String getDisplayName()
	{
		return getName();
	}

	@Override
	public PropertyDescription getPropertyDescription(Object obj, IPropertySource propertySource, PersistContext persistContext)
	{
		if (name.equals("loginSolutionName"))
		{
			return LOGIN_SOLUTION_DESCRIPTION;
		}

		if (name.equals("designTimeProperties"))
		{
			return DESIGN_PROPERTIES_DESCRIPTION;
		}

		return null;
	}

	@Override
	public Object getValue(Object obj, PersistContext persistContext)
	{
		// is handled in property controllers
		return null;
	}

	@Override
	public void setValue(Object obj, Object value, PersistContext persistContext)
	{
		// is handled in property controllers
	}

	@Override
	public boolean hasSupportForClientType(Object obj, ClientSupport csp)
	{
		return true;
	}

	public boolean shouldShow(Object obj)
	{
		return true;
	}
}
