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

import com.servoy.eclipse.core.doc.IDocumentationManager;
import com.servoy.eclipse.core.doc.IDocumentationManagerProvider;
import com.servoy.eclipse.core.doc.IObjectDocumentation;
import com.servoy.eclipse.core.doc.XMLScriptObjectAdapter;
import com.servoy.j2db.documentation.DocumentationUtil;
import com.servoy.j2db.scripting.IScriptObject;
import com.servoy.j2db.scripting.ScriptObjectRegistry;

public class XMLScriptObjectAdapterLoader
{
	private static IDocumentationManager docManager;

	static
	{
		URL url = XMLScriptObjectAdapterLoader.class.getResource("doc/servoydoc.xml"); //$NON-NLS-1$
		IDocumentationManagerProvider documentationManagerProvider = Activator.getDefault().getDocumentationManagerProvider();
		if (documentationManagerProvider != null)
		{
			docManager = documentationManagerProvider.fromXML(url);
		}
	}

	public static void loadDocumentationFromXML()
	{
		Date start = new Date();

		if (docManager != null)
		{
			int succeeded = 0;
			int failed = 0;
			for (IObjectDocumentation objDoc : docManager.getObjects().values())
			{
				try
				{
					Class< ? > clazz = DocumentationUtil.loadClass(objDoc.getQualifiedName());
					XMLScriptObjectAdapter adapter = new XMLScriptObjectAdapter(objDoc);
					// see if there are already return types that the class itself specifies (to be exactly the same as a real client)
					IScriptObject scriptObjectForClass = ScriptObjectRegistry.getScriptObjectForClass(clazz);
					if (scriptObjectForClass != null)
					{
						Class< ? >[] allReturnedTypes = scriptObjectForClass.getAllReturnedTypes();
						if (allReturnedTypes != null && allReturnedTypes.length > 0)
						{
							// if there are return types already set those.
							adapter.setReturnTypes(allReturnedTypes);
						}
					}
					ScriptObjectRegistry.registerScriptObjectForClass(clazz, adapter);
					succeeded++;
				}
				catch (ClassNotFoundException e)
				{
					System.out.println("Class " + objDoc.getQualifiedName() + " not found for script object registration."); //$NON-NLS-1$//$NON-NLS-2$
					failed++;
				}
			}

			System.out.println("Documentation loaded successfully. " + succeeded + " classes registered successfully. " + failed + //$NON-NLS-1$//$NON-NLS-2$
				" classes failed registration."); //$NON-NLS-1$
		}

		Date stop = new Date();
		System.out.println("Documentation loaded and registered in " + (stop.getTime() - start.getTime()) + " ms."); //$NON-NLS-1$//$NON-NLS-2$
	}

	public static IObjectDocumentation getObjectDocumentation(Class< ? > clz)
	{
		if (docManager != null) return docManager.getObjectByQualifiedName(clz.getCanonicalName());
		return null;
	}

}
