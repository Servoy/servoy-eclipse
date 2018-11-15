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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
import com.servoy.j2db.server.ngclient.startup.resourceprovider.ResourceProvider;
import com.servoy.j2db.util.Utils;

/**
 * Editor input for Servoy Theme Properties less file.
 * @author emera
 */
public class PropertiesLessEditorInput extends FileEditorInput
{
	private LessPropertyEntry[] properties;
	private final Set<LessPropertyEntry> modified = new HashSet<>();
	private final static Map<LessPropertyType, TreeSet<String>> typesToProperties = new HashMap<>();

	private PropertiesLessEditorInput(IFile file, LessPropertyEntry[] properties)
	{
		super(file);
		this.properties = properties;
	}


	public static PropertiesLessEditorInput createFromFileEditorInput(FileEditorInput input)
	{
		LessPropertyEntry[] properties = null;
		if (input != null)
		{
			String fileName = input.getName();
			if (fileName.equals(ResourceProvider.PROPERTIES_LESS))
			{
				String content = getFileContent(input);

				if (content != null)
				{
					initTypes();
					properties = loadProperties(content, true);
				}
			}
		}
		return properties != null ? new PropertiesLessEditorInput(input.getFile(), properties) : null;
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


	private static LessPropertyEntry[] loadProperties(String text, boolean init)
	{
		List<LessPropertyEntry> properties = new ArrayList<>();
		String content = text.replaceAll("/\\*([^*]|[\\r\\n]|(\\*+([^*/]|[\\r\\n])))*\\*+/", "").trim();
		Pattern pattern = Pattern.compile("@([\\w-_]+):\\s*(.*);");
		Matcher m = pattern.matcher(content);
		while (m.find())
		{
			String name = m.group(1);
			String value = m.group(2);
			LessPropertyType type = inferType(name, value);
			if (init)
			{
				typesToProperties.get(type).add("@" + name);
			}
			properties.add(new LessPropertyEntry(name, value, type));
		}

		return properties.toArray(new LessPropertyEntry[properties.size()]);
	}

	private static LessPropertyType inferType(String name, String value)
	{
		//TODO infer types or load from some file
		LessPropertyType type = LessPropertyType.TEXT;
		String nameLowerCase = name.toLowerCase();
		if (nameLowerCase.contains("color") || nameLowerCase.contains("fg") || nameLowerCase.contains("bg") || value.contains("#"))
		{
			type = LessPropertyType.COLOR;
		}
		else if (nameLowerCase.contains("radius") || nameLowerCase.contains("width") || nameLowerCase.contains("size"))
		{
			type = LessPropertyType.NUMBER;
		}
		else if (nameLowerCase.contains("font"))
		{
			type = LessPropertyType.FONT;
		}
		else if (nameLowerCase.contains("border"))
		{
			type = LessPropertyType.BORDER;
		}
		return type;
	}


	private static String getFileContent(FileEditorInput input)
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
		return ResourceProvider.PROPERTIES_LESS;
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
		String content = getFileContent(this);
		for (LessPropertyEntry prop : modified)
		{
			//TODO investigate if there is an optimal way to replace the old value with the new value
			content = content.replaceFirst("@" + prop.getName() + ": " + prop.initialValue, "@" + prop.getName() + ": " + prop.getValue());
		}
		return content;
	}

	public LessPropertyEntry[] getProperties()
	{
		return properties;
	}


	public void reloadProperties(String text)
	{
		properties = loadProperties(text, false);
	}


	public String[] getContentProposals(LessPropertyType type, String exclude)
	{
		Set<String> result = new TreeSet<String>(typesToProperties.get(type));
		result.remove("@" + exclude);
		return result.toArray(new String[result.size()]);
	}
}
