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
package com.servoy.eclipse.core;

import java.net.URL;
import java.util.Date;

import com.servoy.eclipse.core.doc.IDocumentationManagerProvider;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.documentation.IDocumentationManager;
import com.servoy.j2db.documentation.IObjectDocumentation;

public class XMLDesignDocsLoader
{

	/**
	 * The name of the XML file which holds servoy legacy design documentation.
	 */
	private static final String DESIGN_XML_FILE = "doc/servoydoc_design.xml";

	private static IDocumentationManager docManager;
	private static boolean loaded = false;

	public static void loadDocumentationFromXML()
	{
		if (!loaded)
		{
			URL url = XMLDesignDocsLoader.class.getResource(DESIGN_XML_FILE);
			try
			{
				IDocumentationManagerProvider documentationManagerProvider = Activator.getDefault().getDocumentationManagerProvider();
				if (documentationManagerProvider != null && url != null)
				{
					Date start = new Date();
					docManager = documentationManagerProvider.fromXML(url, null);
					System.out.println("Designtime documentation initialized successfully in " + (new Date().getTime() - start.getTime()) + " ms.");
				}
			}
			catch (Throwable t)
			{
				ServoyLog.logError("Error reading design documentation from " + url, t);
			}
			loaded = true;
		}
	}

	public static IObjectDocumentation getObjectDocumentation(Class< ? > clz)
	{
		if (docManager != null) return docManager.getObjectByQualifiedName(clz.getCanonicalName());
		return null;
	}

}