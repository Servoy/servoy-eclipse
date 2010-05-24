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
import java.util.Iterator;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.servoy.j2db.persistence.ClientMethodTemplatesLoader;
import com.servoy.j2db.persistence.MethodTemplate;

public class MethodTemplatesLoader
{
	public static void loadMethodTemplatesFromXML()
	{
		synchronized (ClientMethodTemplatesLoader.getLock())
		{
			// even if it was loaded before with "ClientMethodTemplatesLoader.loadClientMethodTemplatesIfNeeded" we must load the XML, as we are in developer and we need all the method template info
			ClientMethodTemplatesLoader.setLoaded();

			URL url = MethodTemplatesLoader.class.getResource("doc/methodtemplates.xml"); //$NON-NLS-1$
			try
			{
				SAXReader reader = new SAXReader();
				Document doc = reader.read(url);
				Element rootElement = doc.getRootElement();
				Iterator it = rootElement.elementIterator("event");
				int counter = 0;
				while (it.hasNext())
				{
					Element eventElem = (Element)it.next();
					String name = eventElem.attributeValue("name");
					Element methTempl = eventElem.element("methodtemplate");
					MethodTemplate mt = MethodTemplate.fromXML(methTempl);
					MethodTemplate.COMMON_TEMPLATES.put(name, mt);
					counter++;
				}
				System.out.println("Loaded " + counter + " method templates.");
			}
			catch (DocumentException e)
			{
				e.printStackTrace();
			}
		}
	}

}
