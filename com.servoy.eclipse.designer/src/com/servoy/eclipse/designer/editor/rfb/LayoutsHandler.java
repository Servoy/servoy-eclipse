/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2016 Servoy BV

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

package com.servoy.eclipse.designer.editor.rfb;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONWriter;
import org.sablo.specification.PackageSpecification;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebLayoutSpecification;
import org.sablo.websocket.IServerService;

/**
 * Sends to the client editor all possible children of layouts.
 * @author emera
 */
public class LayoutsHandler implements IServerService
{
	@Override
	public Object executeMethod(String methodName, JSONObject args) throws Exception
	{
		if ("getAllowedChildren".equals(methodName))
		{
			JSONWriter writer = new JSONStringer();
			writer.object();
			Map<String, PackageSpecification<WebLayoutSpecification>> map = WebComponentSpecProvider.getInstance().getLayoutSpecifications();
			for (PackageSpecification<WebLayoutSpecification> pack : map.values())
			{
				for (WebLayoutSpecification spec : pack.getSpecifications().values())
				{
					List<String> excludedChildren = spec.getExcludedChildren();
					Set<String> allowedChildren = excludedChildren.size() > 0 ? new HashSet<String>() : new HashSet<String>(spec.getAllowedChildren());
					if (excludedChildren.size() > 0)
					{
						for (PackageSpecification<WebLayoutSpecification> pack2 : map.values())
						{
							String packageName = pack2.getPackageName();
							Collection<String> objs = WebComponentSpecProvider.getInstance().getLayoutsInPackage(packageName);
							for (String layoutName : objs)
							{
								if (!excludedChildren.contains(layoutName) && !excludedChildren.contains(packageName + "." + layoutName))
								{
									allowedChildren.add(packageName + "." + layoutName);
								}
							}
						}
						if (!excludedChildren.contains("component")) allowedChildren.add("component");
					}

					if (allowedChildren.size() > 0)
					{
						writer.key(spec.getPackageName() + "." + spec.getName());
						writer.array();
						for (String child : allowedChildren)
						{
							writer.value(child);
						}
						writer.endArray();
					}
				}
			}
			writer.endObject();
			return writer.toString();
		}
		return null;
	}
}
