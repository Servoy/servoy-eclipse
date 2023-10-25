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

package com.servoy.build.documentation.apigen;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.SortedSet;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.IdScriptableObject;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;

import com.jamesmurty.utils.XMLBuilder2;
import com.servoy.base.scripting.api.IJSController;
import com.servoy.base.scripting.api.IJSDataSet;
import com.servoy.base.scripting.api.IJSFoundSet;
import com.servoy.base.scripting.api.IJSRecord;
import com.servoy.base.solutionmodel.IBaseSMComponent;
import com.servoy.base.solutionmodel.IBaseSMField;
import com.servoy.base.solutionmodel.IBaseSMForm;
import com.servoy.base.solutionmodel.IBaseSMMethod;
import com.servoy.build.documentation.DocumentationManager;
import com.servoy.j2db.dataprocessing.IDataSet;
import com.servoy.j2db.dataprocessing.IFoundSet;
import com.servoy.j2db.dataprocessing.IRecord;
import com.servoy.j2db.documentation.ClientSupport;
import com.servoy.j2db.documentation.IFunctionDocumentation;
import com.servoy.j2db.documentation.IObjectDocumentation;
import com.servoy.j2db.documentation.IParameterDocumentation;
import com.servoy.j2db.persistence.CSSPosition;
import com.servoy.j2db.querybuilder.IQueryBuilderColumn;
import com.servoy.j2db.querybuilder.IQueryBuilderCondition;
import com.servoy.j2db.querybuilder.IQueryBuilderLogicalCondition;
import com.servoy.j2db.scripting.FormScope;
import com.servoy.j2db.scripting.IReturnedTypesProvider;
import com.servoy.j2db.scripting.JSMap;
import com.servoy.j2db.scripting.ScriptObjectRegistry;
import com.servoy.j2db.scripting.solutionmodel.ICSSPosition;
import com.servoy.j2db.solutionmodel.ISMPart;
import com.servoy.j2db.util.ServoyJSONObject;

/**
 * @author jcompagner
 *
 */
public class ConfluenceGenerator
{
	private static final String PTLB = "ac:plain-text-link-body";
	private static final String PTB = "ac:plain-text-body";
	private static final String ANC = "ac:anchor";
	private static final String LNK = "ac:link";
	private static final String RTB = "ac:rich-text-body";
	private static final String PRM = "ac:parameter";
	private static final String MCR = "ac:structured-macro";
	private static final String NM = "ac:name";
	private static final String PG = "ri:page";
	private static final String CT = "ri:content-title";

	private static final String SPACE = "DOCS";

	private final XMLBuilder2 root;
	private final HashMap<String, String> qualifiedToName;
	private final HashMap<String, String> returnTypesToParentName;


	private static CloseableHttpClient client;
	private static HttpClientContext context;
	private static Object parentId;

	private static boolean realUpdate = false;

	private static void createClient(String parentName) throws ClientProtocolException, IOException
	{
		HttpHost targetHost = new HttpHost("wiki.servoy.com", 443, "https");
		CredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials("x", "y"));

		AuthCache authCache = new BasicAuthCache();
		authCache.put(targetHost, new BasicScheme());
		context = HttpClientContext.create();
		context.setCredentialsProvider(credsProvider);
		context.setAuthCache(authCache);
		client = HttpClientBuilder.create().build();

		StringWriter writer = new StringWriter();
		HttpGet get = new HttpGet("https://wiki.servoy.com/rest/api/content?spaceKey=" + SPACE + "&expand=version&type=page&title=" + parentName);
		try
		{
			HttpResponse execute = client.execute(get, context);
			IOUtils.copy(execute.getEntity().getContent(), writer);
			EntityUtils.consume(execute.getEntity());
		}
		finally
		{
			get.releaseConnection();
		}
		JSONObject json = new JSONObject(writer.toString());
		JSONArray array = json.optJSONArray("results");
		if (array == null || array.length() != 1)
		{
			throw new IOException("cant find the parent " + parentName);
		}
		JSONObject object = array.optJSONObject(0);
		parentId = object.opt("id");
	}

	public ConfluenceGenerator(String parentName, HashMap<String, String> qualifiedToName, HashMap<String, String> returnTypesToParentName)
	{
		this.qualifiedToName = qualifiedToName;
		this.returnTypesToParentName = returnTypesToParentName;
		XMLBuilder2 start = XMLBuilder2.create("root");
		start.a("xmlns:ac", "http://test.com");
		start.a("xmlns:ri", "http://test.com");
		start = start.e(MCR).a(NM, "cache").a("ac:schema-verson", "1").e(PRM).a(NM, "index").text("true").up();
		start = start.e(PRM).a(NM, "refresh").text("100d").up();
		start = start.e(PRM).a(NM, "showRefresh").text("true").up();
		start = start.e(PRM).a(NM, "id").text("doc").up();
		start = start.e(PRM).a(NM, "title").text("Refresh page").up();
		start = start.e(PRM).a(NM, "showDate").text("true").up();
		start = start.e(PRM).a(NM, "retry").text("Enable").up();
		start = start.e(PRM).a(NM, "atlassian-macro-output-type").text("INLINE").up();
		start = start.e(RTB);
		root = start;
	}

	private void updateContent(String name, String content) throws ClientProtocolException, IOException
	{
		StringWriter writer = new StringWriter();
		HttpGet get = new HttpGet("https://wiki.servoy.com/rest/api/content?spaceKey=" + SPACE + "&expand=version&type=page&title=" + name.replace(' ', '+'));
		try
		{
			HttpResponse execute = client.execute(get, context);
			IOUtils.copy(execute.getEntity().getContent(), writer);
			EntityUtils.consume(execute.getEntity());
		}
		finally
		{
			get.releaseConnection();
		}
		JSONObject json = new JSONObject(writer.toString());
		JSONArray array = json.optJSONArray("results");
		if (array == null || array.length() != 1)
		{
			Object id = null;
			String parentName = returnTypesToParentName.get(name);
			if (parentName != null)
			{
				writer = new StringWriter();
				get = new HttpGet(
					"https://wiki.servoy.com/rest/api/content?spaceKey=" + SPACE + "&expand=version&type=page&title=" + parentName.replace(' ', '+'));
				try
				{
					HttpResponse execute = client.execute(get, context);
					IOUtils.copy(execute.getEntity().getContent(), writer);
					EntityUtils.consume(execute.getEntity());
				}
				finally
				{
					get.releaseConnection();
				}
				json = new JSONObject(writer.toString());
				array = json.optJSONArray("results");
				if (array == null || array.length() != 1)
				{
					System.out.println(" parent " + parentName + " also not found in the wiki to store " + name);
					updateContent(parentName, "<p></p>");
					updateContent(name, content);
					return;
				}
				else
				{
					JSONObject object = array.optJSONObject(0);
					id = object.opt("id");
				}
			}
			else
			{
				System.err.println(" did not found a parent name for " + name + " adding it to the " + parentId);
				id = parentId;
			}
			System.out.println("Creating new page " + name + " with parent: " + parentName + "(" + id + ")" + " on space " + SPACE +
				" must be published and restrictions must be cleared");
			if (realUpdate)
			{
				HttpPost post = new HttpPost("https://wiki.servoy.com/rest/api/content/");

				JSONObject entityContent = new JSONObject();
				entityContent.put("type", "page");
				entityContent.put("title", name);
				entityContent.put("space", new JSONObject().put("key", SPACE));
				entityContent.put("body", new JSONObject().put("storage", new JSONObject().put("value", content).put("representation", "storage")));
				entityContent.put("ancestors", new JSONArray().put(new JSONObject().put("id", id)));
				post.setHeader("Content-Type", "application/json");
				String postContent = entityContent.toString();
				StringEntity entity = new StringEntity(postContent);
				post.setEntity(entity);
				try
				{
					HttpResponse execute = client.execute(post, context);
					EntityUtils.consume(execute.getEntity());
					System.out.println(execute.getStatusLine());
				}
				finally
				{
					post.releaseConnection();
				}
			}
		}
		else
		{
			JSONObject object = array.optJSONObject(0);
			Object id = object.opt("id");
			int version = object.optJSONObject("version").getInt("number");

			System.out.println("updating " + name + " with id : " + id + " and version " + version);
			if (realUpdate)
			{
				HttpPut put = new HttpPut("https://wiki.servoy.com/rest/api/content/" + id);

				JSONObject entityContent = new JSONObject();
				entityContent.put("id", id);
				entityContent.put("type", "page");
				entityContent.put("title", name);
				entityContent.put("space", new JSONObject().put("key", SPACE));
				entityContent.put("body", new JSONObject().put("storage", new JSONObject().put("value", content).put("representation", "storage")));
				entityContent.put("version", new JSONObject().put("number", version + 1).put("minorEdit", true));
				put.setHeader("Content-Type", "application/json");
				String putContent = entityContent.toString();
				StringEntity entity = new StringEntity(putContent);

				put.setEntity(entity);
				try
				{
					HttpResponse execute = client.execute(put, context);
					EntityUtils.consume(execute.getEntity());
					System.out.println(execute.getStatusLine());
				}
				finally
				{
					put.releaseConnection();
				}
			}
		}
	}

	public static void main(String[] args) throws IOException, ClassNotFoundException
	{
		if (args.length < 2)
		{
			System.err.println(
				"usage: ConfluenceGenerator uri_to_doc_xml common+root+wiki (will use " + System.getProperty("user.dir") + " generated and " +
					System.getProperty("user.dir") + " generated_old to generate in and compare to)");
			return;
		}
		String parentName = args[1];
		DocumentationManager manager = DocumentationManager.fromXML(new URL(args[0]), ConfluenceGenerator.class.getClassLoader());
		HashMap<String, String> qualifiedToName = new HashMap<>();
		HashMap<String, String> returnTypesToParentName = new HashMap<>();
		fillStaticParents(returnTypesToParentName);
		SortedMap<String, IObjectDocumentation> objects = manager.getObjects();
		for (IObjectDocumentation doc : objects.values())
		{
			qualifiedToName.put(doc.getQualifiedName(), doc.getPublicName());
		}
		for (IObjectDocumentation doc : objects.values())
		{
			Class< ? > cls = Class.forName(doc.getQualifiedName());
			IReturnedTypesProvider returnTypes = ScriptObjectRegistry.getScriptObjectForClass(cls);
			if (returnTypes != null && returnTypes.getAllReturnedTypes() != null)
			{
				for (Class< ? > retCls : returnTypes.getAllReturnedTypes())
				{
					String qname = qualifiedToName.get(retCls.getCanonicalName());
					if (qname != null)
					{
						returnTypesToParentName.put(qname, doc.getPublicName());
					}
					else
					{
						System.err.println(" qname not found for " + retCls);
					}
				}
			}
		}
		File userDir = new File(System.getProperty("user.dir"));
		createClient(parentName);
		for (Entry<String, IObjectDocumentation> entry : objects.entrySet())
		{
			IObjectDocumentation value = entry.getValue();
			if (value.isDeprecated() || value.getPublicName().equals("PrinterJob") || value.getFunctions().size() == 0) continue;
			ConfluenceGenerator cg = new ConfluenceGenerator(parentName, qualifiedToName, returnTypesToParentName);
			Class< ? > cls = Class.forName(value.getQualifiedName());
			IReturnedTypesProvider returnTypes = ScriptObjectRegistry.getScriptObjectForClass(cls);
			if (returnTypes != null && returnTypes.getAllReturnedTypes() != null)
			{
				Class< ? >[] allReturnedTypes = returnTypes.getAllReturnedTypes();
				ArrayList<Class< ? >> filterReturnTypes = new ArrayList<>();
				for (Class< ? > clsReturn : allReturnedTypes)
				{
					IObjectDocumentation retDoc = objects.get(clsReturn.getCanonicalName());
					if (retDoc == null || !retDoc.isDeprecated())
					{
						filterReturnTypes.add(clsReturn);
					}
				}
				cg.classList("Return Types", filterReturnTypes.toArray(new Class< ? >[filterReturnTypes.size()]));
			}

			cg.generateClientSupport(value);

			if (value.getExtendsClass() != null)
			{
				cg.classList("Extends", new Class[] { Class.forName(value.getExtendsClass()) });
			}


			SortedSet<IFunctionDocumentation> functions = value.getFunctions();
			cg.sumTable("Constants Summary", getConstants(functions), cls);
			cg.sumTable("Property Summary", getProperties(functions), cls);
			cg.sumTable("Command Summary", getCommands(functions), cls);
			cg.sumTable("Event Summary", getEvents(functions), cls);
			cg.sumTable("Methods Summary", getMethods(functions), cls);
			cg.detailsTable("Constants Details", "constant", getConstants(functions), cls);
			cg.detailsTable("Property Details", "property", getProperties(functions), cls);
			cg.detailsTable("Command Details", "command", getCommands(functions), cls);
			cg.detailsTable("Event Details", "event", getEvents(functions), cls);
			cg.detailsTable("Methods Details", "function", getMethods(functions), cls);

			cg.close();
			String output = cg.asString();


			File file = new File(userDir, "generated/" + value.getPublicName() + ".html");
			file.getParentFile().mkdirs();
			try (FileWriter writer = new FileWriter(file))
			{
				writer.write(output);
			}

			file = new File(userDir, "generated_old/" + value.getPublicName() + ".html");
			if (file.exists())
			{
				if (file.length() == output.length())
				{
					// check if it is still the same if the number of bytes are equal.
					try (FileReader reader = new FileReader(file))
					{
						char[] buf = new char[(int)file.length()];
						reader.read(buf);
						String old = new String(buf);
						if (old.equals(output))
						{
							System.out.println("not updating remote content because the file is the same as the old value " + file);
							continue;
						}
					}
				}
			}
			cg.updateContent(value.getPublicName(), output);

		}
	}

	/**
	 * @param returnTypesToParentName2
	 */
	static void fillStaticParents(HashMap<String, String> returnTypesToParentName)
	{
		// TODO Auto-generated method stub

		returnTypesToParentName.put("DataException", "ServoyException");
//		JSColumnObject");
//		JSServer");
//		JSTableObject");
		returnTypesToParentName.put("RuntimeForm", "Forms");
		returnTypesToParentName.put("RuntimeContainer", "RuntimeForm");
		returnTypesToParentName.put("containers", "RuntimeForm");
		returnTypesToParentName.put("elements", "RuntimeForm");
		returnTypesToParentName.put("controller", "RuntimeForm");

		returnTypesToParentName.put("RuntimeAccordionPanel", "elements");
		returnTypesToParentName.put("RuntimeDataLabel", "elements");
		returnTypesToParentName.put("RuntimeInsetList", "elements");
		returnTypesToParentName.put("RuntimeBean", "elements");
		returnTypesToParentName.put("RuntimePortal", "elements");
		returnTypesToParentName.put("RuntimeLabel", "elements");
		returnTypesToParentName.put("RuntimeSplitPane", "elements");
		returnTypesToParentName.put("RuntimeTabPanel", "elements");
		returnTypesToParentName.put("RuntimeButton", "elements");
		returnTypesToParentName.put("RuntimeCalendar", "elements");
		returnTypesToParentName.put("RuntimeCheck", "elements");
		returnTypesToParentName.put("RuntimeChecks", "elements");
		returnTypesToParentName.put("RuntimeCombobox", "elements");
		returnTypesToParentName.put("RuntimeComponent", "elements");
		returnTypesToParentName.put("RuntimeDataButton", "elements");
		returnTypesToParentName.put("RuntimeGroup", "elements");
		returnTypesToParentName.put("RuntimeHtmlArea", "elements");
		returnTypesToParentName.put("RuntimeImageMedia", "elements");
		returnTypesToParentName.put("RuntimeListBox", "elements");
		returnTypesToParentName.put("RuntimePassword", "elements");
		returnTypesToParentName.put("RuntimeRadio", "elements");
		returnTypesToParentName.put("RuntimeRadios", "elements");
		returnTypesToParentName.put("RuntimeRectangle", "elements");
		returnTypesToParentName.put("RuntimeRtfArea", "elements");
		returnTypesToParentName.put("RuntimeSpinner", "elements");
		returnTypesToParentName.put("RuntimeTextArea", "elements");
		returnTypesToParentName.put("RuntimeTextField", "elements");
		returnTypesToParentName.put("RuntimeWebComponent", "elements");


		returnTypesToParentName.put("Calendar", "Form");
		returnTypesToParentName.put("Footer", "Form");
		returnTypesToParentName.put("Header", "Form");
		returnTypesToParentName.put("HeaderTitle", "Form");
		returnTypesToParentName.put("InsetList", "Form");
		returnTypesToParentName.put("Layout Container", "Form");
		returnTypesToParentName.put("ListForm", "Form");
		returnTypesToParentName.put("Bean", "Form");
		returnTypesToParentName.put("Button", "Form");
		returnTypesToParentName.put("CheckBoxes", "Form");
		returnTypesToParentName.put("ComboBox", "Form");
		returnTypesToParentName.put("Image", "Form");
		returnTypesToParentName.put("Label", "Form");
		returnTypesToParentName.put("Part", "Form");
		returnTypesToParentName.put("Password", "Form");
		returnTypesToParentName.put("Portal", "Form");
		returnTypesToParentName.put("RadioButtons", "Form");
		returnTypesToParentName.put("Rectangle", "Form");
		returnTypesToParentName.put("TabPanel", "Form");
		returnTypesToParentName.put("Tab", "Form");
		returnTypesToParentName.put("Table", "Form");
		returnTypesToParentName.put("TextArea", "Form");
		returnTypesToParentName.put("TextField", "Form");

		returnTypesToParentName.put("RelationItem", "Relation");


	}

	private void close()
	{
		root.up().up();
	}

	/**
	 * @param value
	 */
	private void generateClientSupport(IObjectDocumentation value)
	{

		ClientSupport clientSupport = value.getClientSupport();
		List<String> support = getSupportedClientsList(clientSupport);
		if (support.size() > 0)
		{
			XMLBuilder2 start = root.e("p").t(" ").up().e("p").t(" ").up().e(MCR).a(NM, "table").e(PRM).a(NM, "id").up().e(PRM).a(NM, "class").t(
				"servoy sReturnTypes").up();
			start = start.e(RTB).e(MCR).a(NM, "colgroup").e(RTB).e(MCR).a(NM, "col").e(PRM).a(NM, "colspan").t("2").up().e(PRM).a(NM, "width").t("100%").up(
				2).e(MCR).a(NM, "col").up(3);
			start = start.e(MCR).a(NM, "thead").e(RTB).e(MCR).a(NM, "tr").e(PRM).a(NM, "style").t("height: 30px;").up().e(RTB).e(MCR).a(NM, "th").e(PRM).a(NM,
				"colspan").t("2").up().e(RTB).t("Supported Clients").up(6);
			start = start.e(MCR).a(NM, "tr").e(RTB).e(MCR).a(NM, "td").e(RTB);

			for (String publicName : support)
			{
				start = start.e(MCR).a(NM, "span").e(PRM).a(NM, "class").t("sWordList").up().e(RTB).t(publicName).up(2);
			}
		}

	}

	/**
	 * @param clientSupport
	 * @return
	 */
	public static List<String> getSupportedClientsList(ClientSupport clientSupport)
	{
		List<String> support = new ArrayList<>();
		if (clientSupport.hasSupport(ClientSupport.sc))
		{
			support.add("SmartClient");
		}
		if (clientSupport.hasSupport(ClientSupport.wc))
		{
			support.add("WebClient");
		}
		if (clientSupport.hasSupport(ClientSupport.ng))
		{
			support.add("NGClient");
		}
		if (clientSupport.hasSupport(ClientSupport.mc))
		{
			support.add("MobileClient");
		}
		return support;
	}

	/**
	 * @param alltypes
	 * @param qualifiedToName
	 */
	public ConfluenceGenerator classList(String name, Class< ? >[] alltypes)
	{
		if (alltypes != null && alltypes.length > 0)
		{
			List<String> publicNames = new ArrayList<>(alltypes.length);
			for (Class< ? > alltype : alltypes)
			{
				String publicName = getPublicName(alltype);
				if (publicName != null && !"Object".equals(publicName))
				{
					publicNames.add(publicName);
				}
			}
			if (publicNames.size() > 0)
			{
				XMLBuilder2 start = root.e("p").t(" ").up().e("p").t(" ").up().e(MCR).a(NM, "table").e(PRM).a(NM, "id").up().e(PRM).a(NM, "class").t(
					"servoy sReturnTypes").up();
				start = start.e(RTB).e(MCR).a(NM, "colgroup").e(RTB).e(MCR).a(NM, "col").e(PRM).a(NM, "colspan").t("2").up().e(PRM).a(NM, "width").t("100%").up(
					2).e(MCR).a(NM, "col").up(3);
				start = start.e(MCR).a(NM, "thead").e(RTB).e(MCR).a(NM, "tr").e(PRM).a(NM, "style").t("height: 30px;").up().e(RTB).e(MCR).a(NM, "th").e(PRM).a(
					NM, "colspan").t("2").up().e(RTB).t(name).up(6);
				start = start.e(MCR).a(NM, "tr").e(RTB).e(MCR).a(NM, "td").e(RTB);

				for (String publicName : publicNames)
				{
					start = start.e(MCR).a(NM, "span").e(PRM).a(NM, "class").t("sWordList").up().e(PRM).a(NM, "atlassian-macro-output-type").t("BLOCK").up().e(
						RTB).e(LNK).e(PG).a(CT, publicName).up(4);
				}
			}
		}
		return this;

	}

	public ConfluenceGenerator sumTable(String name, List<IFunctionDocumentation> functions, Class cls)
	{
		if (functions != null && functions.size() > 0)
		{
			XMLBuilder2 start = root.e("p").t(" ").up().e("p").t(" ").up().e(MCR).a(NM, "table").e(PRM).a(NM, "id").up().e(PRM).a(NM, "class").t(
				"servoy sSummary").up().e(RTB).e(MCR).a(NM, "colgroup").e(RTB).e(MCR).a(NM, "col").e(PRM).a(NM, "width").t("12%").up(2).e(MCR).a(NM, "col").e(
					PRM)
				.a(NM, "width").t("30%").up(2).e(MCR).a(NM, "col").e(PRM).a(NM, "width").t("58%").up(4).e(MCR).a(NM, "thead").e(RTB).e(MCR).a(NM,
					"tr")
				.e(PRM).a(NM, "style").t("height: 30px;").up().e(RTB).e(MCR).a(NM, "th").e(PRM).a(NM, "colspan").t("3").up().e(RTB).t(name).up(6);

			for (IFunctionDocumentation fd : functions)
			{
				if (fd.isDeprecated()) continue;
				FunctionTemplateModel ftm = new FunctionTemplateModel(fd, this::getPublicName, cls, false);
				start = start.e(MCR).a(NM, "tr").e(RTB).e(MCR).a(NM, "td").e(RTB);
				if ("void".equals(ftm.getReturnType()) || ftm.getReturnType() == null)
				{
					start = start.t("void").up(2);
				}
				else
				{
					start = start.e(LNK).e(PG).a(CT, ftm.getReturnType()).up(4);
				}

				String functionName = ftm.getFullFunctionName();
				start = start.e(MCR).a(NM, "td").e(RTB).e(LNK).a(ANC, functionName).e(PTLB).cdata(functionName).up(4).e(MCR).a(NM, "td").e(RTB).t(
					ftm.getSummary()).up(4);
			}
			start.up();
		}
		return this;
	}


	public ConfluenceGenerator detailsTable(String name, String id, List<IFunctionDocumentation> functions, Class cls)
	{
		if (functions != null && functions.size() > 0)
		{
			XMLBuilder2 start = root.e("p").t(" ").up().e("p").t(" ").up().e(MCR).a(NM, "table").e(PRM).a(NM, "id").t(id).up().e(PRM).a(NM, "class").t(
				"servoy sDetail").up().e(RTB).e(MCR).a(NM, "colgroup").e(RTB).e(MCR).a(NM, "col").e(PRM).a(NM, "colspan").t("2").up().e(PRM).a(NM, "width").t(
					"100%")
				.up(2).e(MCR).a(NM, "col").up(3).e(MCR).a(NM, "thead").e(RTB).e(MCR).a(NM, "tr").e(PRM).a(NM, "style").t("height:30px").up().e(
					RTB)
				.e(MCR).a(NM, "th").e(PRM).a(NM, "colspan").t("2").up().e(RTB).t(name).up(6);

			for (IFunctionDocumentation fd : functions)
			{
				if (fd.isDeprecated()) continue;
				FunctionTemplateModel ftm = new FunctionTemplateModel(fd, this::getPublicName, cls, false);
				start = start.e(MCR).a(NM, "tbody").e(PRM).a(NM, "id").t(fd.getMainName()).up().e(RTB).e(MCR).a(NM, "tr").e(PRM).a(NM, "id").t("name").up().e(
					RTB).e(MCR).a(NM, "td").e(RTB).e("h4").t(ftm.getFullFunctionName()).up(5).e(MCR).a(NM, "tr").e(PRM).a(NM, "id").t("des").up().e(RTB).e(
						MCR)
					.a(NM, "td").e(RTB).e(MCR).a(NM, "div").e(PRM).a(NM, "class").t("sIndent").up().e(RTB).e("pre").t(ftm.getDescription()).up(7);

				if (fd.getType() == IFunctionDocumentation.TYPE_FUNCTION || fd.getType() == IFunctionDocumentation.TYPE_EVENT)
				{
					// params
					LinkedHashMap<String, IParameterDocumentation> arguments = fd.getArguments();
					Class< ? >[] argumentsTypes = fd.getArgumentsTypes();
					if (arguments.size() > 0 || (argumentsTypes != null && argumentsTypes.length > 0))
					{
						start = start.e(MCR).a(NM, "tr").e(PRM).a(NM, "id").t("prs").up().e(RTB).e(MCR).a(NM, "td").e(RTB).e("p").e("strong").t(
							"Parameters").up(2).e(MCR).a(NM, "table").e(PRM).a(NM, "class").t("sIndent").up().e(MCR).a(NM, "tbody").e(RTB);
						if (arguments.size() == 0 && argumentsTypes.length > 0)
						{
							for (Class< ? > argumentType : argumentsTypes)
							{
								String argType = getPublicName(argumentType);
								if ("void".equals(argType)) argType = "Object";
								start = start.e(MCR).a(NM, "tr").e(RTB).e(MCR).a(NM, "td").e(MCR).a(NM, "div").e(RTB).e(LNK).e(PG).a(CT, argType).up(7);
							}
						}
						else
						{
							Iterator<IParameterDocumentation> iterator = fd.getArguments().values().iterator();
							while (iterator.hasNext())
							{
								IParameterDocumentation paramDoc = iterator.next();
								String description = paramDoc.getDescription();
								description = description == null ? " ;" : description;
								String paramType = getPublicName(paramDoc.getType());
								if ("void".equals(paramType))
								{
									if (paramDoc.getJSType() != null)
									{
										paramType = paramDoc.getJSType();
										start = start.e(MCR).a(NM, "tr").e(RTB).e(MCR).a(NM, "td").e(MCR).a(NM, "div").e(RTB).t(paramType).up(3).e(
											MCR).a(NM, "td").e(RTB).t(paramDoc.getName()).up(2).e(MCR).a(NM, "td").e(RTB).t(description).up(4);
										continue;
									}
									else
									{
										paramType = "Object";
									}
								}
								if (!iterator.hasNext() && fd.isVarargs() && paramType != null)
								{
									paramType = paramType.replace("[]", "...");
								}
								start = start.e(MCR).a(NM, "tr").e(RTB).e(MCR).a(NM, "td").e(MCR).a(NM, "div").e(RTB).e(LNK).e(PG).a(CT, paramType).up(5).e(
									MCR).a(NM, "td").e(RTB).t(paramDoc.getName()).up(2).e(MCR).a(NM, "td").e(RTB).t(description).up(4);
							}
						}
						start = start.up(7);
					}
				}


				if (ftm.getReturnType() != null && !ftm.getReturnType().equals("void"))
				{
					// returns
					start = start.e(MCR).a(NM, "tr").e(PRM).a(NM, "id").t("ret").up().e(RTB).e(MCR).a(NM, "td").e(RTB).e("p").e("strong").t("Returns").up(2).e(
						MCR).a(NM, "div").e(PRM).a(NM, "class").t("sIndent").up().e(RTB).e(LNK).e(PG).a(CT, ftm.getReturnType());
					String returnTypeDescription = ftm.getReturnTypeDescription();
					if (returnTypeDescription != null && !returnTypeDescription.trim().isEmpty())
					{
						start = start.up(2).e(RTB).t(" " + returnTypeDescription).up(7);
					}
					else
					{
						start = start.up(8);
					}
				}


				List<String> supportedClientsList = getSupportedClientsList(fd.getClientSupport());
				StringBuilder sb = new StringBuilder();
				for (String clientName : supportedClientsList)
				{
					sb.append(clientName);
					sb.append(",");
				}
				if (sb.length() > 0) sb.setLength(sb.length() - 1);
				// supported clients
				start = start.e(MCR).a(NM, "tr").e(PRM).a(NM, "id").t("clients").up().e(RTB).e(MCR).a(NM, "td").e(RTB).e("p").e("strong").t(
					"Supported Clients").up(2).e(MCR).a(NM, "div").e(PRM).a(NM, "class").t("sIndent").up().e(RTB).t(sb.toString()).up(6);

				// sample
				start = start.e(MCR).a(NM, "tr").e(PRM).a(NM, "id").t("sam").up().e(RTB).e(MCR).a(NM, "td").e(RTB).e("p").e("strong").t("Sample").up(2).e(
					MCR).a(NM, "div").e(PRM).a(NM, "class").t("sIdent").up().e(RTB).e(MCR).a(NM, "code").e(PRM).a(NM, "language").t("javascript").up().e(
						PTB)
					.cdata(ftm.getSampleCode()).up(8).e(MCR).a(NM, "tr").e(PRM).a(NM, "class").t("lastDetailRow").up().e(RTB).e(MCR).a(NM, "td").e(
						RTB)
					.t(" ").up(6);
			}
			start.up();
		}
		return this;
	}

	public String asString()
	{
		String str = root.asString();
		if (!str.startsWith("<root xmlns:ac=\"http://test.com\" xmlns:ri=\"http://test.com\">"))
		{
			return "";
		}
		str = str.substring("<root xmlns:ac=\"http://test.com\" xmlns:ri=\"http://test.com\">".length(), str.length() - "</root>".length());
		return str;
	}

	public String getPublicName(Class< ? > type)
	{
		if (type != null)
		{
			if (type == boolean.class || type == Boolean.class)
			{
				return "Boolean";
			}
			else if (type == void.class)
			{
				return "void";
			}
			else if (type.isPrimitive() || Number.class.isAssignableFrom(type) || com.servoy.j2db.documentation.scripting.docs.Number.class == type)
			{
				return "Number";
			}
			else if (type == String.class || com.servoy.j2db.documentation.scripting.docs.String.class == type)
			{
				return "String";
			}
			else if (Date.class.isAssignableFrom(type))
			{
				return "Date";
			}
			else if (type == Object.class || type == Scriptable.class || type == IdScriptableObject.class)
			{
				return "Object";
			}
			else if (type.isArray() || type == NativeArray.class || List.class.isAssignableFrom(type))
			{
				return "Array";
			}
			else if (IJSController.class.isAssignableFrom(type))
			{
				return "controller";
			}
			else if (IJSRecord.class.isAssignableFrom(type) || IRecord.class.isAssignableFrom(type))

			{
				return "JSRecord";
			}
			else if (type == FormScope.class)
			{
				return "RuntimeForm";
			}
			else if (IJSFoundSet.class.isAssignableFrom(type) || IFoundSet.class.isAssignableFrom(type))
			{
				return "JSFoundSet";
			}
			else if (IJSDataSet.class.isAssignableFrom(type) || IDataSet.class.isAssignableFrom(type))
			{
				return "JSDataSet";
			}
			else if (Exception.class.isAssignableFrom(type))
			{
				return "Exception";
			}
			else if (IBaseSMField.class.isAssignableFrom(type))
			{
				return "JSField";
			}
			else if (IBaseSMComponent.class.isAssignableFrom(type))
			{
				return "JSComponent";
			}
			else if (IBaseSMMethod.class.isAssignableFrom(type))
			{
				return "JSMethod";
			}
			else if (ISMPart.class.isAssignableFrom(type))
			{
				return "JSPart";
			}
			else if (IBaseSMForm.class.isAssignableFrom(type))
			{
				return "JSForm";
			}
			else if (Function.class.isAssignableFrom(type))
			{
				return "Function";
			}
			else if (IQueryBuilderLogicalCondition.class.isAssignableFrom(type))
			{
				return "QBLogicalCondition";
			}
			else if (IQueryBuilderCondition.class.isAssignableFrom(type))
			{
				return "QBCondition";
			}
			else if (IQueryBuilderCondition.class.isAssignableFrom(type))
			{
				return "QBCondition";
			}
			else if (IQueryBuilderColumn.class.isAssignableFrom(type))
			{
				return "QBColumn";
			}
			else if (ICSSPosition.class.isAssignableFrom(type) || CSSPosition.class == type)
			{
				return "CSSPosition";
			}
			else if (Color.class.isAssignableFrom(type) || Point.class.isAssignableFrom(type) || Dimension.class.isAssignableFrom(type) ||
				Insets.class.isAssignableFrom(type))
			{// special tabs that in scripting just map on a String
				return "String";
			}
			else if (type == Map.class || type == ServoyJSONObject.class || type == JSONObject.class || type == JSMap.class || type == NativeObject.class)
			{
				return "Object";
			}
			else if (type.isEnum())
			{
				return "enum";
			}
			String name = qualifiedToName.get(type.getCanonicalName());
			if (name == null)
			{
				System.err.println("public name not found for " + type);
				name = "Object";
			}
			return name;
		}
		return "void";

	}

	static List<IFunctionDocumentation> getEvents(SortedSet<IFunctionDocumentation> functions)
	{
		return getFilteredList(IFunctionDocumentation.TYPE_EVENT, functions);
	}


	static List<IFunctionDocumentation> getCommands(SortedSet<IFunctionDocumentation> functions)
	{
		return getFilteredList(IFunctionDocumentation.TYPE_COMMAND, functions);
	}


	static List<IFunctionDocumentation> getProperties(SortedSet<IFunctionDocumentation> functions)
	{
		return getFilteredList(IFunctionDocumentation.TYPE_PROPERTY, functions);
	}


	static List<IFunctionDocumentation> getConstants(SortedSet<IFunctionDocumentation> functions)
	{
		return getFilteredList(IFunctionDocumentation.TYPE_CONSTANT, functions);
	}

	static List<IFunctionDocumentation> getMethods(SortedSet<IFunctionDocumentation> functions)
	{
		return getFilteredList(IFunctionDocumentation.TYPE_FUNCTION, functions);
	}

	/**
	 * @param typeEvent
	 * @return
	 */
	private static List<IFunctionDocumentation> getFilteredList(Integer typeEvent, SortedSet<IFunctionDocumentation> functions)
	{
		List<IFunctionDocumentation> retValue = new ArrayList<IFunctionDocumentation>();
		for (IFunctionDocumentation fd : functions)
		{
			if (fd.getClientSupport().hasSupport(ClientSupport.Default))
			{
				if (fd.getType().intValue() == typeEvent.intValue())
				{
					retValue.add(fd);
				}
			}
		}
		return retValue.size() > 0 ? retValue : null;
	}

}
