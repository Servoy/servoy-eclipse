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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.part.FileEditorInput;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.editors.less.LessPropertyEntry.LessPropertyType;
import com.servoy.j2db.persistence.NameComparator;
import com.servoy.j2db.server.ngclient.startup.resourceprovider.resources.ThemeResourceLoader;
import com.servoy.j2db.util.Utils;

/**
 * Editor input for Servoy Theme Properties less file.
 * @author emera
 */
public class PropertiesLessEditorInput extends FileEditorInput
{
	public static PropertiesLessEditorInput createFromFileEditorInput(FileEditorInput input)
	{
		String content = null;
		if (input != null)
		{
			String fileName = input.getName();
			if (fileName.equals(ThemeResourceLoader.CUSTOM_PROPERTIES_LESS))
			{
				content = getFileContent(input);
				if (content != null)
				{
					initTypes();
				}
			}
		}
		return content != null ? new PropertiesLessEditorInput(input.getFile(), content) : null;
	}

	private static void initTypes()
	{
		for (LessPropertyType type : LessPropertyType.values())
		{
			typesToProperties.put(type, new TreeSet<>(NameComparator.INSTANCE));
		}
		//TODO other types ?
		typesToProperties.get(LessPropertyType.COLOR).add("darken(@property, 10%)");
		typesToProperties.get(LessPropertyType.COLOR).add("lighten(@property, 10%)");
	}

	private LinkedHashMap<String, LessPropertyEntry> properties;
	private final Set<LessPropertyEntry> modified = new HashSet<>();
	private final static Map<LessPropertyType, TreeSet<String>> typesToProperties = new HashMap<>();
	private final static Pattern lessVariablePattern = Pattern.compile("@([\\w-_]+)\\s*:\\s*(.*);\\s?(?:// default?:(.*))?");
	private String version;

	private PropertiesLessEditorInput(IFile file, String content)
	{
		super(file);
		this.properties = loadProperties(content, null);
	}

	private LinkedHashMap<String, LessPropertyEntry> loadProperties(String text, Map<String, LessPropertyEntry> previousVaues)
	{
		// First read in the default properties file for the given version (that is in the text)
		String defaultThemeProperties;
		int versionIndex = text.indexOf(ThemeResourceLoader.THEME_LESS + "?version=");
		if (versionIndex == -1)
		{
			defaultThemeProperties = ThemeResourceLoader.getLatestThemeProperties();
			version = "latest";
		}
		else
		{
			int endIndex = text.indexOf(';', versionIndex);
			version = text.substring(versionIndex + (ThemeResourceLoader.THEME_LESS + "?version=").length(), endIndex - 1);
			defaultThemeProperties = ThemeResourceLoader.getThemeProperties(version);
		}

		LinkedHashMap<String, LessPropertyEntry> props = new LinkedHashMap<>();
		parseContent(defaultThemeProperties, previousVaues, props);
		parseContent(text, previousVaues, props);

		return props;
	}


	/**
	 * @param text
	 * @param init
	 * @param props
	 */
	private void parseContent(String text, Map<String, LessPropertyEntry> previousValues, LinkedHashMap<String, LessPropertyEntry> props)
	{
		String content = text.replaceAll("/\\*([^*]|[\\r\\n]|(\\*+([^*/]|[\\r\\n])))*\\*+/", "").trim();
		Matcher m = lessVariablePattern.matcher(content);
		while (m.find())
		{
			String name = m.group(1);
			String value = m.group(2);
			String storedDefaultValue = m.group(3);
			LessPropertyType type = inferType(name, value);
			if (previousValues == null)
			{
				typesToProperties.get(type).add("@" + name);
			}
			if (previousValues != null && previousValues.containsKey(name))
			{
				LessPropertyEntry lessProp = previousValues.get(name);
				lessProp.setValue(value);
				lessProp.resetLastTxtValue();
				props.put(name, lessProp);
			}
			else
			{
				LessPropertyEntry lessProp = new LessPropertyEntry(name, value, type, storedDefaultValue);
				LessPropertyEntry overwrittenValue = props.put(name, lessProp);
				if (overwrittenValue != null) lessProp.setDefaultValue(overwrittenValue.getValue());
			}
		}
	}

	private static LessPropertyType inferType(String name, String value)
	{
		//TODO infer types or load from some file
		LessPropertyType type = null;
		String nameLowerCase = name.toLowerCase();
		if (nameLowerCase.contains("color") || nameLowerCase.contains("fg") || nameLowerCase.contains("bg") || value.contains("#"))
		{
			type = LessPropertyType.COLOR;
		}
		if (nameLowerCase.contains("radius") || nameLowerCase.contains("width") || nameLowerCase.contains("size"))
		{
			type = LessPropertyType.NUMBER;
		}
		if (nameLowerCase.contains("font"))
		{
			if (type == null)
			{
				type = LessPropertyType.FONT;
			}
			else
			{
				//stuff like "font-size" have the actual type number, but they should be in allowed as content proposals for font properties
				typesToProperties.get(LessPropertyType.FONT).add("@" + name);
			}
		}
		if (nameLowerCase.contains("border"))
		{
			if (type == null)
			{
				type = LessPropertyType.BORDER;
			}
			else
			{
				//stuff like "border-width" have the actual type number, but they should be in allowed as content proposals for border properties
				typesToProperties.get(LessPropertyType.BORDER).add("@" + name);
			}
		}
		if (type == null)
		{
			type = LessPropertyType.TEXT;
		}
		return type;
	}


	static String getFileContent(FileEditorInput input)
	{
		String content = null;
		try (InputStream is = input.getFile().getContents())
		{
			content = Utils.getTXTFileContent(is, Charset.forName("UTF8"));
		}
		catch (IOException | CoreException e)
		{
			ServoyLog.logError("Cannot open properties less file in the editor.", e);
		}
		return content;
	}

	/**
	 * @return the version
	 */
	public String getVersion()
	{
		return version;
	}

	@Override
	public <T> T getAdapter(Class<T> adapter)
	{
		return Platform.getAdapterManager().getAdapter(this, adapter);
	}

	@Override
	public boolean exists()
	{
		return true;
	}

	@Override
	public ImageDescriptor getImageDescriptor()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getName()
	{
		return ThemeResourceLoader.CUSTOM_PROPERTIES_LESS;
	}

	@Override
	public IPersistableElement getPersistable()
	{
		return getAdapter(IPersistableElement.class);
	}

	@Override
	public String getToolTipText()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public void propertyModified(LessPropertyEntry property)
	{
		modified.add(property);
	}

	public void clearChanges()
	{
		modified.clear();
	}


	public String getText()
	{
		StringBuilder content = new StringBuilder(getFileContent(this));
		int lastImport = content.lastIndexOf("@import");
		if (lastImport > 0)
		{
			lastImport = content.indexOf("\n", lastImport) + 1;
			content.setLength(lastImport);
		}

		for (LessPropertyEntry prop : properties.values())
		{
			if (prop.getDefaultValue() != null && !prop.getDefaultValue().equals(prop.getValue()))
			{
				content.append("\n\n");
				content.append(prop.toString());
				content.append("; // default:");
				content.append(prop.getDefaultValue());
			}
			prop.resetLastTxtValue();
		}
		return content.toString();
	}

	public LessPropertyEntry[] getProperties()
	{
		return properties.values().toArray(new LessPropertyEntry[properties.size()]);
	}


	public void reloadProperties(String text, boolean recreate)
	{
		properties = loadProperties(text, recreate ? null : properties);
	}


	public String[] getContentProposals(LessPropertyType type, String exclude)
	{
		Set<String> result = new TreeSet<String>(typesToProperties.get(type));
		result.remove("@" + exclude);
		return result.toArray(new String[result.size()]);
	}
}
