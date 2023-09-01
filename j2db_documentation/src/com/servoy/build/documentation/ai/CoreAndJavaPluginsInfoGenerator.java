/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2023 Servoy BV

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

package com.servoy.build.documentation.ai;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.servoy.build.documentation.apigen.ConfluenceGenerator;
import com.servoy.build.documentation.apigen.FunctionTemplateModel;
import com.servoy.build.documentation.apigen.MarkdownGenerator;
import com.servoy.j2db.documentation.ClientSupport;
import com.servoy.j2db.documentation.IFunctionDocumentation;
import com.servoy.j2db.documentation.IObjectDocumentation;

import freemarker.core.ParseException;
import freemarker.template.Configuration;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateNotFoundException;

/**
 * @author acostescu
 */
public class CoreAndJavaPluginsInfoGenerator
{

	private final Map<String, Object> root;
	private final Path path;
	private final String parentPath;

	private static Map<String, String> defaultTypePath = new HashMap<>();
	static
	{
		// special types
		defaultTypePath.put("JSServer", "/plugins/maintenance/");
		defaultTypePath.put("JSTableObject", "/plugins/maintenance/");
		defaultTypePath.put("JSColumnObject", "/plugins/maintenance/");

	}

	private static final HashMap<String, String> qualifiedToName = new HashMap<>();
	private static final HashMap<String, String> publicToRootPath = new HashMap<>();
	private static final HashMap<String, String> returnTypesToParentName = new HashMap<>();

	private static Template methodTemplate; // FIXME
	private static Template nodeTemplate; // FIXME

	public CoreAndJavaPluginsInfoGenerator(String publicName, String parentPath)
	{
		this.parentPath = parentPath;
		root = new HashMap<>();
		root.put("classname", publicName);
		root.put("classname_nospacde", publicName.replace(" ", "%20").toLowerCase());
		root.put("instance", this);

		if ("/design-api".equals(parentPath))
		{
			String returnType = returnTypesToParentName.get(publicName);
			if (returnType != null)
			{
				path = Paths.get(parentPath, returnType);
			}
			else
			{
				path = Paths.get(parentPath);
			}
		}
		else
		{
			path = parentPath != null ? Paths.get(parentPath) : Paths.get(generatePath(publicName));
		}
	}

	private void table(String name, List<IFunctionDocumentation> functions, Class< ? > cls, boolean ngOnly)
	{
		if (functions != null && functions.size() > 0)
		{
			List<FunctionTemplateModel> models = new ArrayList<>();
			for (IFunctionDocumentation fd : functions)
			{
				if (fd.isDeprecated()) continue;
				if (ngOnly && !fd.getClientSupport().hasSupport(ClientSupport.ng)) continue;
				FunctionTemplateModel ftm = new FunctionTemplateModel(fd, MarkdownGenerator::getPublicName, cls, ngOnly);
				models.add(ftm);
//			if ("void".equals(ftm.getReturnType()) || ftm.getReturnType() == null)
//			{
//				start = start.t("void").up(2);
//			}
//			else
//			{
//				start = start.e(LNK).e(PG).a(CT, ftm.getReturnType()).up(4);
//			}
//
//			String functionName = getFullFunctionName(fd, cls);
//			start = start.e(MCR).a(NM, "td").e(RTB).e(LNK).a(ANC, functionName).e(PTLB).cdata(functionName).up(4).e(MCR).a(NM, "td").e(RTB).t(
//				ftm.getSummary()).up(4);
			}
			root.put(name, models);
		}
	}

	private void generateClientSupport(IObjectDocumentation value)
	{
		ClientSupport clientSupport = value.getClientSupport();
		List<String> support = ConfluenceGenerator.getSupportedClientsList(clientSupport);
		root.put("supportedClients", support);
	}

	private void classList(String section, Class< ? >[] alltypes)
	{
		if (alltypes != null && alltypes.length > 0)
		{
			List<String> publicNames = new ArrayList<>(alltypes.length);
			for (Class< ? > alltype : alltypes)
			{
				String publicName = MarkdownGenerator.getPublicName(alltype);
				if (publicName != null && !"Object".equals(publicName))
				{
					publicNames.add(publicName);
				}
			}
			root.put(section, publicNames);
		}
	}

	private String generate()
	{
		// TODO Auto-generated method stub
		StringWriter out = new StringWriter();
//		try
//		{
//			temp.process(root, out); FIXME
//		}
//		catch (TemplateException | IOException e)
//		{
//			e.printStackTrace();
//		}
		return out.toString();
	}

	public String getReturnTypePath(String publicName)
	{
		Path p1 = Paths.get(generatePath(publicName));
		Path relativize = path.relativize(p1);
		String relativePath = relativize.toString().replace('\\', '/').replace(" ", "%20");
		return ((relativePath.isBlank() ? "." : relativePath) + "/" + publicName + ".md").toLowerCase();
	}

	public Path getPath()
	{
		return path;
	}

	private String generatePath(String publicName)
	{
		String p = defaultTypePath.get(publicName);
		if (p == null)
		{
			String parent = returnTypesToParentName.get(publicName);
			if (parent == null) p = "/";
			else
			{
				String rootPath = publicToRootPath.get(parent);
				if (rootPath != null)
				{
					return rootPath;
				}
				else
				{
					String parent2 = returnTypesToParentName.get(parent);
					while (parent2 != null)
					{
						parent = parent2 + "/" + parent;
						parent2 = returnTypesToParentName.get(parent2);
					}
				}
				p = "/" + parent;
			}
		}
		return p;
	}

	public static void initTemplates(Configuration cfg) throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException
	{
		// FIXME implement
//		methodTemplate = cfg.getTemplate("method_pinecone_template.md");
//		nodeTemplate = cfg.getTemplate("node_pinecone_template.md"); // like the main description of a plugin or "application" etc.
	}

}
