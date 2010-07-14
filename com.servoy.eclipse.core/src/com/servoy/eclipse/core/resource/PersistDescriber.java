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
package com.servoy.eclipse.core.resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.content.IContentDescriber;
import org.eclipse.core.runtime.content.IContentDescription;

/**
 * A describer that peeks in the json-file for a typeid value
 * 
 * @author rgansevles
 * 
 */
public abstract class PersistDescriber implements IContentDescriber
{
	public int describe(InputStream contents, IContentDescription description)
	{
		BufferedReader reader = null;
		try
		{
			reader = new BufferedReader(new InputStreamReader(contents, "UTF8"));
			String line;
			for (int count = 0; count < 100 && (line = reader.readLine()) != null; count++)
			{
				if (line.startsWith("typeid:"))
				{
					String typeStr = line.substring("typeid:".length());
					if (typeStr.endsWith(","))
					{
						typeStr = typeStr.substring(0, typeStr.length() - 1);
					}
					if (isValidType(Integer.parseInt(typeStr)))
					{
						return VALID;
					}
					// other type, no need to look any further
					return INVALID;
				}
			}
		}
		catch (Exception e)
		{
			return INDETERMINATE;
		}
		finally
		{
			if (reader != null)
			{
				try
				{
					reader.close();
				}
				catch (IOException e)
				{
				}
			}
		}
		return INVALID;
	}

	protected abstract boolean isValidType(int typeId);

	public QualifiedName[] getSupportedOptions()
	{
		return new QualifiedName[0];
	}

}
