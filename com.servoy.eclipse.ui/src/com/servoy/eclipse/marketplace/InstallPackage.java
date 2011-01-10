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

package com.servoy.eclipse.marketplace;

import java.net.URL;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Class representing a collection of installable items from the Servoy Marketplace 
 * @author gabi
 *
 */
public class InstallPackage
{
	private final ArrayList<InstallItem> installItems = new ArrayList<InstallItem>();

	public InstallPackage(String urlInstallXML) throws Exception
	{
		URL installXML = new URL(urlInstallXML);
		Document installDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(installXML.openStream());

		NodeList installEntryNodes = installDocument.getElementsByTagName("entry"); //$NON-NLS-1$
		Node installEntryNode;
		for (int i = 0; i < installEntryNodes.getLength(); i++)
		{
			installEntryNode = installEntryNodes.item(i);
			String type = installEntryNode.getAttributes().getNamedItem("type").getNodeValue(); //$NON-NLS-1$					
			InstallItem installItem = null;
			if ("solution".equals(type)) //$NON-NLS-1$
			{
				installItem = new SolutionInstall(installEntryNode);
			}
			else if ("bean".equals(type)) //$NON-NLS-1$
			{
				installItem = new BeanInstall(installEntryNode);
			}
			else if ("plugin".equals(type)) //$NON-NLS-1$
			{
				installItem = new PluginInstall(installEntryNode);
			}

			if (installItem != null) installItems.add(installItem);
		}
	}

	public ArrayList<InstallItem> getAllInstallItems()
	{
		return installItems;
	}
}
