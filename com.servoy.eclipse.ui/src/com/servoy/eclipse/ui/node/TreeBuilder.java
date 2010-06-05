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
package com.servoy.eclipse.ui.node;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.swt.graphics.Image;

import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.core.XMLScriptObjectAdapterLoader;
import com.servoy.eclipse.core.doc.IDocumentationManager;
import com.servoy.eclipse.core.doc.IDocumentationManagerProvider;
import com.servoy.eclipse.core.doc.IFunctionDocumentation;
import com.servoy.eclipse.core.doc.IObjectDocumentation;
import com.servoy.j2db.documentation.ParameterDocumentation;
import com.servoy.j2db.util.HtmlUtils;

public class TreeBuilder
{
	private static IDocumentationManager docManager;
	private static boolean docManagerLoaded = false;

	private static IDocumentationManager getDocManager()
	{
		if (!docManagerLoaded)
		{
			URL url = XMLScriptObjectAdapterLoader.class.getResource("doc/servoydoc_jslib.xml"); //$NON-NLS-1$
			IDocumentationManagerProvider documentationManagerProvider = Activator.getDefault().getDocumentationManagerProvider();
			if (documentationManagerProvider != null)
			{
				docManager = documentationManagerProvider.fromXML(url);
			}
			docManagerLoaded = true;
		}
		return docManager;
	}

	/**
	 * @param prefix
	 * @return
	 */
	public static UserNode[] createLengthAndArray(IImageLookup imageLookup, String prefix)
	{
		Object propertiesIcon = imageLookup.loadImage("properties_icon.gif"); //$NON-NLS-1$
		List<UserNode> dlm = new ArrayList<UserNode>();
		dlm.add(new UserNode(
			"allnames", UserNodeType.ARRAY, prefix + ".allnames", prefix + ".allnames", "Get all names as an array", null, imageLookup.loadImage("special_properties_icon.gif"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		dlm.add(new UserNode("length", UserNodeType.ARRAY, prefix + ".length", prefix + ".length", "Get the length of the array", null, propertiesIcon)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		dlm.add(new UserNode("[index]", UserNodeType.ARRAY, prefix + "[0]", prefix + "[0]", "Get an element by index", null, propertiesIcon)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		dlm.add(new UserNode("['name']", UserNodeType.ARRAY, prefix + "['name']", prefix + "['name']", "Get an element by name", null, propertiesIcon)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		return dlm.toArray(new UserNode[dlm.size()]);
	}

	public static UserNode[] createJSArray(IImageLookup imageLookup)
	{
		Object propertiesIcon = imageLookup.loadImage("properties_icon.gif"); //$NON-NLS-1$
		Object functionIcon = imageLookup.loadImage("function.gif"); //$NON-NLS-1$

		List<UserNode> dlm = new ArrayList<UserNode>();
		dlm.add(new UserNode(
			"Array()", UserNodeType.ARRAY, "new Array()", "var array = new Array();", "<html><body><b>Array()</b><br><pre>Constructs a new default array</pre></body></html>", null, functionIcon)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		dlm.add(new UserNode(
			"Array(number)", UserNodeType.ARRAY, "new Array(number)", "var array = new Array(number);", "<html><body><b>Array(number)</b><br><pre>Constructs a new array with size [number]</pre></body></html>", null, functionIcon)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		dlm.add(new UserNode(
			"Array(value1,value2)", UserNodeType.ARRAY, "new Array(value1,value2)", "var array = new Array(value1,value2);", "<html><body><b>Array(value1,value2)</b><br><pre>Constructs a new array that contains the given values</pre></body></html>", null, functionIcon)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

		return createTypedArray(imageLookup, com.servoy.eclipse.core.scripting.docs.Array.class, UserNodeType.ARRAY, dlm, true);
	}

	public static UserNode[] createJSDate(IImageLookup imageLookup)
	{
		Object functionIcon = imageLookup.loadImage("function.gif");

		List<UserNode> dlm = new ArrayList<UserNode>();

		dlm.add(new UserNode("Date()", UserNodeType.DATE, "new Date()", "var date = new Date();",
			"<html><body><b>Date()</b><br><pre>Use the current date and time to create an instance of the object date</pre></body></html>", null, functionIcon)); //$NON-NLS-1$
		dlm.add(new UserNode(
			"Date(dateString)",
			UserNodeType.DATE,
			"new Date(dateString)",
			"var date = new Date(dateString);",
			"<html><body><b>Date(dateString)</b><br><pre>Use the date specified by the string to create the instance of the date object. String format is \"month day, year hours:minutes:seconds\". </pre></body></html>",
			null, functionIcon));
		dlm.add(new UserNode("Date(year, month, day)", UserNodeType.DATE, "new Date(year, month, day)", "var date = new Date(year, month, day);",
			"<html><body><b>Date(year, month, day)</b><br><pre>Create an instance of date with the specified values.</pre></body></html>", null, functionIcon));
		dlm.add(new UserNode(
			"Date(year, month, day, hours, minutes, seconds)",
			UserNodeType.DATE,
			"new Date(year, month, day, hours, minutes, seconds)",
			"var date = new Date(year, month, day, hours, minutes, seconds);",
			"<html><body><b>Date(year, month, day, hours, minutes, seconds)</b><br><pre>Create an instance of date with the specified values</pre></body></html>",
			null, functionIcon));
		dlm.add(new UserNode(
			"Date(year, month, day, hours, minutes, seconds, milliseconds)",
			UserNodeType.DATE,
			"new Date(year, month, day, hours, minutes, seconds, milliseconds);",
			"var date = new Date(year, month, day, hours, minutes, seconds, milliseconds)",
			"<html><body><b>Date(year, month, day, hours, minutes, seconds, milliseconds)</b><br><pre>Create an instance of date with the specified values</pre></body></html>",
			null, functionIcon));
		dlm.add(new UserNode(
			"Date(milliseconds)",
			UserNodeType.DATE,
			"new Date(milliseconds)",
			"var date = new Date(milliseconds);",
			"<html><body><b>Date(milliseconds)</b><br><pre>Create instance of date 1 January 1970 00:00:00 UTC. Constructor argument is the number of milliseconds since 1 January 1970 00:00:00 UTC</pre></body></html>",
			null, functionIcon));

		return createTypedArray(imageLookup, com.servoy.eclipse.core.scripting.docs.Date.class, UserNodeType.DATE, dlm, true);
	}

	public static UserNode[] createJSString(IImageLookup imageLookup)
	{
		return createTypedArray(imageLookup, com.servoy.eclipse.core.scripting.docs.String.class, UserNodeType.STRING, null, true);
	}

	public static UserNode[] createJSMathFunctions(IImageLookup imageLookup)
	{
		return createTypedArray(imageLookup, com.servoy.eclipse.core.scripting.docs.Math.class, UserNodeType.FUNCTIONS_ITEM, null, true);
	}

	public static UserNode[] createFlows(IImageLookup imageLookup)
	{
		return createTypedArray(imageLookup, com.servoy.eclipse.core.scripting.docs.Statements.class, UserNodeType.STATEMENTS_ITEM, null, false);
	}

	public static UserNode[] createXMLListMethods(IImageLookup imageLookup)
	{
		return createTypedArray(imageLookup, com.servoy.eclipse.core.scripting.docs.XMLList.class, UserNodeType.XML_LIST_METHODS, null, true);
	}

	public static UserNode[] createXMLMethods(IImageLookup imageLookup)
	{
		return createTypedArray(imageLookup, com.servoy.eclipse.core.scripting.docs.XML.class, UserNodeType.XML_METHODS, null, true);
	}

	public static UserNode[] createJSRegexp(IImageLookup imageLookup)
	{
		return createTypedArray(imageLookup, com.servoy.eclipse.core.scripting.docs.RegExp.class, UserNodeType.REGEXP, null, true);
	}

	public static UserNode[] createTypedArray(IImageLookup imageLookup, Class< ? > clazz, UserNodeType type, List<UserNode> existingDlm, boolean extendedTooltip)
	{
		Object functionIcon = imageLookup.loadImage("function.gif"); //$NON-NLS-1$
		Object propertiesIcon = imageLookup.loadImage("properties_icon.gif"); //$NON-NLS-1$

		List<UserNode> dlm = existingDlm != null ? new ArrayList<UserNode>(existingDlm) : new ArrayList<UserNode>();

		IDocumentationManager dm = getDocManager();
		if (dm != null)
		{
			IObjectDocumentation objDoc = dm.getObjectByQualifiedName(clazz.getCanonicalName());
			if (objDoc != null)
			{
				// We need properties listed before functions.
				for (IFunctionDocumentation fdoc : objDoc.getFunctions())
				{
					if (fdoc.getType() == IFunctionDocumentation.TYPE_PROPERTY)
					{
						dlm.add(fdoc2usernode(fdoc, type, functionIcon, extendedTooltip));
					}
				}
				for (IFunctionDocumentation fdoc : objDoc.getFunctions())
				{
					if (fdoc.getType() == IFunctionDocumentation.TYPE_FUNCTION)
					{
						dlm.add(fdoc2usernode(fdoc, type, functionIcon, extendedTooltip));
					}
				}
			}
		}

		return dlm.toArray(new UserNode[dlm.size()]);
	}

	private static UserNode fdoc2usernode(IFunctionDocumentation fdoc, UserNodeType type, Object functionIcon, boolean extendedTooltip)
	{
		String retType = fdoc.getReturnedType();
		if (retType != null)
		{
			int idx = retType.lastIndexOf(".");
			if (idx >= 0) retType = retType.substring(idx + 1);
		}
		String tooltip = fdoc.getDescription();
		if (extendedTooltip) tooltip = "<html><body><b>" + retType + " " + fdoc.getMainName() + "</b> " + tooltip + "</body></html>";
		UserNode un = new UserNode(fdoc.getMainName(), type, fdoc.getSignature("."), fdoc.getSample(), tooltip, null, //$NON-NLS-1$
			functionIcon);
		return un;
	}

	public static UserNode[] docToNodes(Class< ? > clz, IImageLookup imageLookup, UserNodeType type, String codePrefix, List<SimpleUserNode> existingDlm)
	{
		List<SimpleUserNode> dlm = docToNodesInternal(clz, imageLookup, type, codePrefix, existingDlm, null, null);
		return dlm.toArray(new UserNode[dlm.size()]);
	}

	public static List<SimpleUserNode> docToSomeNodes(Class< ? > clz, IImageLookup imageLookup, UserNodeType type, String codePrefix,
		List<SimpleUserNode> existingDlm, Map<String, Object> onlyThese)
	{
		return docToNodesInternal(clz, imageLookup, type, codePrefix, existingDlm, onlyThese, null);
	}

	public static List<SimpleUserNode> docToOneNode(Class< ? > clz, IImageLookup imageLookup, UserNodeType type, String codePrefix,
		List<SimpleUserNode> existingDlm, String name, Object realObject, Object icon)
	{
		Map<String, Object> onlyThese = new HashMap<String, Object>();
		onlyThese.put(name, realObject);
		return docToNodesInternal(clz, imageLookup, type, codePrefix, existingDlm, onlyThese, icon);
	}

	public static List<SimpleUserNode> docToNodesInternal(Class< ? > clz, IImageLookup imageLookup, UserNodeType type, String codePrefix,
		List<SimpleUserNode> existingDlm, Map<String, Object> onlyThese, Object icon)
	{
		Object functionIcon = imageLookup.loadImage("function.gif"); //$NON-NLS-1$
		Object propertiesIcon = imageLookup.loadImage("properties_icon.gif"); //$NON-NLS-1$
		Object specialPropertiesIcon = imageLookup.loadImage("special_properties_icon.gif"); //$NON-NLS-1$

		List<SimpleUserNode> dlm = existingDlm != null ? existingDlm : new ArrayList<SimpleUserNode>();

		IObjectDocumentation objDoc = XMLScriptObjectAdapterLoader.getObjectDocumentation(clz);
		if (objDoc != null)
		{
			// We need properties listed before functions.
			visitDocs(objDoc.getFunctions(), IFunctionDocumentation.TYPE_PROPERTY, dlm, onlyThese, type, codePrefix, icon != null ? icon : propertiesIcon,
				icon != null ? icon : specialPropertiesIcon);
			visitDocs(objDoc.getFunctions(), IFunctionDocumentation.TYPE_FUNCTION, dlm, onlyThese, type, codePrefix, icon != null ? icon : functionIcon,
				icon != null ? icon : functionIcon);
		}

		return dlm;
	}

	private static void visitDocs(SortedSet<IFunctionDocumentation> fdocs, Integer typeFilter, List<SimpleUserNode> dlm, Map<String, Object> onlyThese,
		UserNodeType type, String codePrefix, Object icon, Object specialIcon)
	{
		for (IFunctionDocumentation fdoc : fdocs)
		{
			String answeredName = answersTo(fdoc, onlyThese);
			if ((onlyThese == null || answeredName != null) && fdoc.getType() == typeFilter)
			{
				Object realObject = null;
				if (answeredName != null) realObject = onlyThese.get(answeredName);
				dlm.add(new UserNode(fdoc.getMainName(), type, fdoc.getSignature(codePrefix != null ? codePrefix : null), fdoc.getSample(),
					fdoc.getDescription(), realObject, fdoc.isSpecial() ? specialIcon : icon));
			}
		}
	}

	private static String answersTo(IFunctionDocumentation fdoc, Map<String, Object> onlyThese)
	{
		String result = null;
		if (onlyThese != null)
		{
			for (String name : onlyThese.keySet())
				if (fdoc.answersTo(name))
				{
					result = name;
					break;
				}
		}
		return result;
	}

	public static List<IObjectDocumentation> generateDocumentation()
	{
		Set<String> addedJSLib = new TreeSet<String>();
		List<IObjectDocumentation> docs = new ArrayList<IObjectDocumentation>();
		IDocumentationManager dm = getDocManager();
		if (dm != null)
		{
			docs.add(generateObjectDocumentation("Array", createJSArray(DummyImageLoader.INSTANCE), addedJSLib)); //$NON-NLS-1$
			docs.add(generateObjectDocumentation("Date", createJSDate(DummyImageLoader.INSTANCE), addedJSLib)); //$NON-NLS-1$
			docs.add(generateObjectDocumentation("String", createJSString(DummyImageLoader.INSTANCE), addedJSLib)); //$NON-NLS-1$
			docs.add(generateObjectDocumentation("Math", createJSMathFunctions(DummyImageLoader.INSTANCE), addedJSLib)); //$NON-NLS-1$
			docs.add(generateObjectDocumentation("RegExp", createJSRegexp(DummyImageLoader.INSTANCE), addedJSLib)); //$NON-NLS-1$
			docs.add(generateObjectDocumentation("Flow", createFlows(DummyImageLoader.INSTANCE), addedJSLib)); //$NON-NLS-1$
			docs.add(generateObjectDocumentation("XML", createXMLMethods(DummyImageLoader.INSTANCE), addedJSLib)); //$NON-NLS-1$
			docs.add(generateObjectDocumentation("XMLList", createXMLListMethods(DummyImageLoader.INSTANCE), addedJSLib)); //$NON-NLS-1$

			IObjectDocumentation allJSLib = dm.createObjectDocumentation(IDocumentationManager.TAG_JSLIB, "AllJSLib", null); //$NON-NLS-1$ 
			allJSLib.setPublicName("JSLib"); //$NON-NLS-1$
			for (String key : addedJSLib)
			{
				allJSLib.addReturnedType(key);
			}
			docs.add(allJSLib);
		}
		return docs;
	}

	private static IObjectDocumentation generateObjectDocumentation(String name, UserNode[] nodes, Set<String> addedJSLib)
	{
		Object propertiesIcon = DummyImageLoader.INSTANCE.loadImage("properties_icon.gif"); //$NON-NLS-1$

		IDocumentationManager dm = getDocManager();
		if (dm == null)
		{
			return null;
		}
		IObjectDocumentation objDoc = dm.createObjectDocumentation(IDocumentationManager.TAG_JSLIB, name, new String[] { });
		objDoc.setPublicName(name);
		addedJSLib.add(name);
		for (UserNode node : nodes)
		{
			boolean isProperty = false;
			if (propertiesIcon != null) isProperty = propertiesIcon.equals(node.getIcon());

			String nameAndArgs = node.getName();
			String fName = nameAndArgs;
			List<String> args = new ArrayList<String>();
			int idx = nameAndArgs.indexOf("(");
			if (idx >= 0)
			{
				fName = nameAndArgs.substring(0, idx);
				String strArgs = nameAndArgs.substring(idx + 1, nameAndArgs.indexOf(")"));
				if (strArgs.trim().length() > 0)
				{
					String[] parts = strArgs.split(",");
					for (String p : parts)
						if (p.trim().length() > 0) args.add(p);
				}
			}

			IFunctionDocumentation fdoc = dm.createFunctionDocumentation(fName, isProperty ? IFunctionDocumentation.TYPE_PROPERTY
				: IFunctionDocumentation.TYPE_FUNCTION, false, IFunctionDocumentation.STATE_DOCUMENTED);
			fdoc.setSample(node.getSampleCode());

			String description = HtmlUtils.stripHTML(node.getToolTipText());
			if (description.startsWith(nameAndArgs)) description = description.substring(nameAndArgs.length());
			description = description.replaceAll("^[\\(\\)\\s]*", "");
			description = description.trim();
			fdoc.setDescription(description);
			for (String arg : args)
			{
				boolean optional = false;
				if (arg.endsWith("]")) optional = true;
				arg = arg.replaceAll("\\[", "");
				arg = arg.replaceAll("\\]", "");
				arg = arg.trim();
				fdoc.addArgument(new ParameterDocumentation(arg, "", "", optional));
			}
			objDoc.addFunction(fdoc);
		}
		return objDoc;
	}

	private static class DummyImageLoader implements IImageLookup
	{
		public static final DummyImageLoader INSTANCE = new DummyImageLoader();

		private final com.servoy.eclipse.ui.Activator uiActivator = com.servoy.eclipse.ui.Activator.getDefault();

		public Object loadImage(String name)
		{
			Image img = uiActivator.loadImageFromBundle(name);
			if (img == null)
			{
				img = uiActivator.loadImageFromOldLocation(name);
			}
			return img;
		}
	}
}
