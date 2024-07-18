/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2018 Servoy BV

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

package com.servoy.eclipse.ui.editors.less;

/**
 * The model for the Servoy Theme Properties editor.
 * @author emera
 */
public class LessPropertyEntry
{
	private final String name;
	private String value;
	private String lastTxtValue;
	private final LessPropertyType type;
	private String defaultValue;
	private String storedDefault;
	private String description;
	private String displayName;

	static enum LessPropertyType
	{
		COLOR, BORDER, FONT, TEXT, NUMBER
	}

	public LessPropertyEntry(String name, String value, LessPropertyType type, String storedDefault)
	{
		this.name = name;
		this.value = value;
		this.defaultValue = value;
		this.lastTxtValue = value;
		this.type = type;
		this.storedDefault = storedDefault;
	}

	public String getName()
	{
		return name;
	}

	public String getValue()
	{
		return value;
	}

	public void setValue(String value)
	{
		this.value = value;
	}

	public String getLabel()
	{
		if (displayName != null) return displayName;
		String label = name.length() > 1 ? name.substring(0, 1).toUpperCase() + name.substring(1) : name;
		return label.replaceAll("-", " ");
	}

	public LessPropertyType getType()
	{
		return type;
	}

	public String getLastTxtValue()
	{
		return lastTxtValue;
	}

	public void resetLastTxtValue()
	{
		lastTxtValue = value;
	}

	@Override
	public String toString()
	{
		return "@" + getName() + ": " + getValue();
	}

	/**
	 * @param defaultValue
	 */
	public void setDefaultValue(String defaultValue)
	{
		this.defaultValue = defaultValue;
	}

	/**
	 * @return the defaultValue
	 */
	public String getDefaultValue()
	{
		return defaultValue;
	}

	/**
	 * @return the storedDefault
	 */
	public String getStoredDefault()
	{
		return storedDefault;
	}

	/**
	 * @param storedDefault the storedDefault to set
	 */
	public void setStoredDefault(String storedDefault)
	{
		this.storedDefault = storedDefault;
	}

	public void setDescription(String desc)
	{
		this.description = desc;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDisplayName(String displayName)
	{
		this.displayName = displayName;
	}
}
