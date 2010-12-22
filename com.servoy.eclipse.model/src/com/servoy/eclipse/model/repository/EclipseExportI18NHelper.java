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
package com.servoy.eclipse.model.repository;

import java.io.OutputStream;
import java.io.PrintStream;

import com.servoy.eclipse.model.util.IFileAccess;
import com.servoy.j2db.persistence.IXMLExportI18NHelper;

public class EclipseExportI18NHelper implements IXMLExportI18NHelper
{
	private final IFileAccess workspace;

	public EclipseExportI18NHelper(IFileAccess workspace)
	{
		this.workspace = workspace;
	}

	public String[] getI18NFileNames(String i18nServer, String i18nTable)
	{
		return EclipseMessages.getMessageFileNames(i18nServer, i18nTable);
	}

	public void writeI18NFileContent(String messageFileName, OutputStream os)
	{
		if (os != null)
		{
			String content = EclipseMessages.getMessageFileContent(messageFileName, workspace);
			if (content != null)
			{
				PrintStream ps = new PrintStream(os);
				ps.print(content);
				ps.flush();
			}
		}
	}
}
