/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.eclipse.mobileexporter.export;

import java.util.HashMap;
import java.util.Map;

import com.servoy.j2db.util.ServoyJSONObject;

/**
 * @author lvostinar
 *
 */
public class PhoneGapApplication
{
	private String title;
	private String version;
	private String description;
	private boolean publicApplication = false;

	/**
	 * @param title
	 * @param version
	 * @param description
	 * @param publicApplication
	 */
	public PhoneGapApplication(String title, String version, String description, boolean publicApplication)
	{
		this.title = title;
		this.version = version;
		this.description = description;
		this.publicApplication = publicApplication;
	}

	/**
	 * @param title the title to set
	 */
	public void setTitle(String title)
	{
		this.title = title;
	}

	/**
	 * @param version the version to set
	 */
	public void setVersion(String version)
	{
		this.version = version;
	}


	/**
	 * @param description the description to set
	 */
	public void setDescription(String description)
	{
		this.description = description;
	}


	/**
	 * @param publicApplcation the publicApplication to set
	 */
	public void setPublicApplcation(boolean publicApplcation)
	{
		this.publicApplication = publicApplcation;
	}

	public String getJSON()
	{
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("title", title);
		properties.put("create_method", "file");
		properties.put("private", Boolean.toString(!publicApplication));
		properties.put("version", version);
		properties.put("description", description);
		ServoyJSONObject json = new ServoyJSONObject(properties, false, false);
		json.setNoBrackets(false);
		return json.toString();
	}

	/**
	 * @return the version
	 */
	public String getVersion()
	{
		return version;
	}

	/**
	 * @return the description
	 */
	public String getDescription()
	{
		return description;
	}

	/**
	 * @return the publicApplication
	 */
	public boolean isPublicApplication()
	{
		return publicApplication;
	}

}
