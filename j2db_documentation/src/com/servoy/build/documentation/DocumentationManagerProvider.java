package com.servoy.build.documentation;

import java.io.InputStream;
import java.net.URL;

import com.servoy.eclipse.core.doc.IDocumentationManagerProvider;
import com.servoy.j2db.documentation.IDocumentationManager;

/**
 * Documentation manager extension point implementation.
 * @author rgansevles
 *
 */
public class DocumentationManagerProvider implements IDocumentationManagerProvider
{

	public IDocumentationManager fromXML(String path, ClassLoader loader)
	{
		return DocumentationManager.fromXML(path, loader);
	}

	public IDocumentationManager fromXML(URL url, ClassLoader loader)
	{
		return DocumentationManager.fromXML(url, loader);
	}

	public IDocumentationManager fromXML(InputStream is, ClassLoader loader)
	{
		return DocumentationManager.fromXML(is, loader);
	}

}
