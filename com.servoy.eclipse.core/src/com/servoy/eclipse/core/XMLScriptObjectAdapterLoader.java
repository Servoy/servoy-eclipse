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
import com.servoy.j2db.documentation.DocumentationUtil;
import com.servoy.j2db.documentation.IDocumentationManager;
import com.servoy.j2db.documentation.IObjectDocumentation;
import com.servoy.j2db.documentation.XMLScriptObjectAdapter;
import com.servoy.j2db.scripting.IScriptObject;
import com.servoy.j2db.scripting.ScriptObjectRegistry;

public class XMLScriptObjectAdapterLoader
{
	private static IDocumentationManager docManager;
	private static boolean coreLoaded = false;

	public static void loadCoreDocumentationFromXML()
	{
		if (!coreLoaded)
		{
			URL url = XMLScriptObjectAdapterLoader.class.getResource("doc/servoydoc.xml"); //$NON-NLS-1$
			IDocumentationManagerProvider documentationManagerProvider = Activator.getDefault().getDocumentationManagerProvider();
			if (documentationManagerProvider != null)
			{
				docManager = documentationManagerProvider.fromXML(url, null);
			}
			loadDocumentationFromXML(null, docManager);
			coreLoaded = true;
		}
	}

	public static void loadDocumentationFromXML(ClassLoader loader, IDocumentationManager docmgr)
	{
		Date start = new Date();

		if (docmgr != null)
		{
			int succeeded = 0;
			int failed = 0;
			for (IObjectDocumentation objDoc : docmgr.getObjects().values())
			{
				try
				{
					Class< ? > clazz = DocumentationUtil.loadClass(loader, objDoc.getQualifiedName());
					// see if there are already return types that the class itself specifies (to be exactly the same as a real client)
					IScriptObject scriptObjectForClass = ScriptObjectRegistry.getScriptObjectForClass(clazz);
					XMLScriptObjectAdapter adapter = new XMLScriptObjectAdapter(objDoc, scriptObjectForClass);
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

			System.out.print("Documentation loaded successfully. " + succeeded + " classes registered successfully."); //$NON-NLS-1$ //$NON-NLS-2$
			if (failed > 0) System.out.print(" " + failed + " classes failed registration."); //$NON-NLS-1$ //$NON-NLS-2$
			System.out.println();
		}

		Date stop = new Date();
		System.out.println("Documentation loaded and registered in " + (stop.getTime() - start.getTime()) + " ms."); //$NON-NLS-1$//$NON-NLS-2$
	}

	public static IObjectDocumentation getObjectDocumentation(Class< ? > clz)
	{
		if (docManager != null) return docManager.getObjectByQualifiedName(clz.getCanonicalName());
		return null;
	}

	@SuppressWarnings("nls")
	public static String getPluginDocXMLForClass(Class< ? > clz)
	{
		String clzCanonical = clz.getCanonicalName().replace(".", "/");
		int idx = clzCanonical.lastIndexOf("/");
		String docFileName = "servoy-doc.xml";
		if (idx >= 0)
		{
			return clzCanonical.substring(0, idx + 1) + docFileName;
		}
		else
		{
			return docFileName;
		}
	}
}
