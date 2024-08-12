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

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.servoy.eclipse.core.doc.IDocumentationManagerProvider;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.documentation.DocumentationUtil;
import com.servoy.j2db.documentation.IDocumentationManager;
import com.servoy.j2db.documentation.IObjectDocumentation;
import com.servoy.j2db.documentation.XMLScriptObjectAdapter;
import com.servoy.j2db.plugins.IClientPlugin;
import com.servoy.j2db.plugins.PluginManager;
import com.servoy.j2db.scripting.IScriptObject;
import com.servoy.j2db.scripting.ScriptObjectRegistry;
import com.servoy.j2db.util.JarManager.Extension;

public class XMLScriptObjectAdapterLoader
{
	/**
	 * The name of the XML file which holds documentation and other Servoy extension related info.
	 *
	 * @see com.servoy.eclipse.docgenerator.parser.DocumentationXMLBuilder.EXTENSION_XML_FILE
	 */
	private static final String EXTENSION_XML_FILE = "servoy-extension.xml";

	private static IDocumentationManager docManager;
	private static boolean coreLoaded = false;

	public static void loadCoreDocumentationFromXML()
	{
		if (!coreLoaded)
		{
			URL url = XMLScriptObjectAdapterLoader.class.getResource("doc/servoydoc.xml");
			try
			{
				IDocumentationManagerProvider documentationManagerProvider = Activator.getDefault().getDocumentationManagerProvider();
				if (documentationManagerProvider != null && url != null)
				{
					docManager = documentationManagerProvider.fromXML(url, null);
				}
				loadDocumentationFromXML(XMLScriptObjectAdapterLoader.class.getClassLoader(), docManager);
			}
			catch (Throwable t)
			{
				ServoyLog.logError("Error reading documentation from " + url, t);
			}

			// the following - commented out code is only to be used when for adjusting native-references.xml of org.eclipse.dltk.javascript.core/resources
			// when one of the classes in com.servoy.j2db.documentation.scripting.docs changed
			// see NGClientStarter commented code that prints those out to console and those can be used as descriptions in native-references.xml (used in code completion for native javascript types)
//			url = XMLScriptObjectAdapterLoader.class.getResource("doc/servoydoc_jslib.xml");
//			try
//			{
//				IDocumentationManagerProvider documentationManagerProvider = Activator.getDefault().getDocumentationManagerProvider();
//				if (documentationManagerProvider != null && url != null)
//				{
//					docManager = documentationManagerProvider.fromXML(url, null);
//				}
//				loadDocumentationFromXML(XMLScriptObjectAdapterLoader.class.getClassLoader(), docManager);
//			}
//			catch (Throwable t)
//			{
//				ServoyLog.logError("Error reading documentation from " + url, t);
//			}

			coreLoaded = true;
		}
	}

	private static void loadDocumentationFromXML(ClassLoader loader, IDocumentationManager docmgr)
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
					registerReturnTypesFromDoc(loader, objDoc);
					succeeded++;
				}
				catch (Throwable e)
				{
					ServoyLog.logError("Class " + objDoc.getQualifiedName() + " not found for script object registration.", e);
					failed++;
				}
			}

			ServoyLog.logInfo("Documentation loaded successfully. " + succeeded + " classes registered successfully.");
			if (failed > 0) ServoyLog.logError(" " + failed + " classes failed registration.", null);
		}

		Date stop = new Date();
		ServoyLog.logInfo("Documentation loaded and registered in " + (stop.getTime() - start.getTime()) + " ms.");
	}

	public static void registerReturnTypesFromDoc(ClassLoader loader, IObjectDocumentation objDoc) throws ClassNotFoundException
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
	}

	public static IObjectDocumentation getObjectDocumentation(Class< ? > clz)
	{
		if (docManager != null) return docManager.getObjectByQualifiedName(clz.getCanonicalName());
		return null;
	}

	public static String getPluginDocXMLForClass(String clz)
	{
		String clzCanonical = clz.replace(".", "/");
		int idx = clzCanonical.lastIndexOf("/");
		if (idx >= 0)
		{
			return clzCanonical.substring(0, idx + 1) + EXTENSION_XML_FILE;
		}
		else
		{
			return EXTENSION_XML_FILE;
		}
	}

	/**
	 * Tries to load documentation XMLs for available client plugins.
	 */
	public static void loadDocumentationForPlugins(PluginManager pluginManager, IDocumentationManagerProvider documentationManagerProvider)
	{
		for (Extension<IClientPlugin> ext : pluginManager.loadClientPluginDefs())
		{
			URL url = ext.jar.jarUrl;
			try
			{
				File urlFile = new File(new URI(url.toExternalForm()));
				ZipFile zf = new ZipFile(urlFile);
				String docXMLPath = XMLScriptObjectAdapterLoader.getPluginDocXMLForClass(ext.instanceClass.getCanonicalName());
				ZipEntry docEntry = zf.getEntry(docXMLPath);
				if (docEntry != null)
				{
					InputStream is = zf.getInputStream(docEntry);
					IDocumentationManager mgr = documentationManagerProvider.fromXML(is, pluginManager.getClassLoader());
					XMLScriptObjectAdapterLoader.loadDocumentationFromXML(pluginManager.getClassLoader(), mgr);
				}
			}
			catch (Exception e)
			{
				ServoyLog.logError("Exception while loading extension XML file from JAR file '" + url.toExternalForm() + "'.", e);
			}
		}
	}

}
