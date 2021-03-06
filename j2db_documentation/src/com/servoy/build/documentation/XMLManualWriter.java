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

package com.servoy.build.documentation;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;

import javax.swing.AbstractAction;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.servoy.j2db.persistence.MethodArgument;
import com.servoy.j2db.persistence.MethodTemplate;
import com.servoy.j2db.util.Debug;

public class XMLManualWriter extends AbstractAction
{
	public void actionPerformed(ActionEvent e)
	{
		try
		{
			for (String name : MethodTemplate.COMMON_TEMPLATES.keySet())
			{
				MethodTemplate mt = MethodTemplate.COMMON_TEMPLATES.get(name);
				System.out.println(name + " --> " + mt.getSignature().getName());
			}
			System.out.println();
			System.out.println();


			PrintWriter out = new PrintWriter(new FileOutputStream("templates.java.txt"));
			for (String name : MethodTemplate.COMMON_TEMPLATES.keySet())
			{
				out.println();
				out.println("name: " + name);
				MethodTemplate mt = MethodTemplate.COMMON_TEMPLATES.get(name);
				out.println("\t * @templatedescription " + mt.getDescription());
				out.println("\t * @templatename " + mt.getSignature().getName());
				if (mt.getSignature().getType() != null) out.println("\t * @templatetype " + mt.getSignature().getType().getName());
				if (mt.getArguments() != null)
				{
					for (MethodArgument mta : mt.getArguments())
						out.println("\t * @templateparam " + mta.getType() + " " + mta.getName() + " " + mta.getDescription());
				}
				if (mt.hasAddTodoBlock()) out.println("\t * @templateaddtodo");
				if (mt.getDefaultMethodCode() != null)
				{
					out.println("\t * @templatecode");
					String[] lines = mt.getDefaultMethodCode().split("\n");
					for (String line : lines)
						out.println("\t * " + line);
				}
				out.println();
				out.println();
				out.println(mt.getMethodDeclaration(mt.getSignature().getName(), null, null));
				out.println("---------");
			}
			out.close();

			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document document = builder.newDocument();
			document.appendChild(document.createComment("This file is automatically generated. Don't bother editing it, because your changes will probably be lost at the next build."));
			Element rootElement = document.createElement("root");
			document.appendChild(rootElement);

			for (String name : MethodTemplate.COMMON_TEMPLATES.keySet())
			{
				MethodTemplate mt = MethodTemplate.COMMON_TEMPLATES.get(name);
				Element event = document.createElement("event");
				rootElement.appendChild(event);
				event.setAttribute("name", name);
				event.appendChild(mt.toXML(document));
			}

			OutputFormat outformat = OutputFormat.createPrettyPrint();
			outformat.setEncoding("UTF-8");

			File fout = new File("templates.xml");
			XMLWriter writer = new XMLWriter(new PrintWriter(fout), outformat);
			System.out.println("templates written in ->" + fout.getAbsolutePath() + "<-");

			writer.write(document);
			writer.flush();
		}
		catch (Exception ex)
		{
			Debug.error(ex);
		}
	}
}