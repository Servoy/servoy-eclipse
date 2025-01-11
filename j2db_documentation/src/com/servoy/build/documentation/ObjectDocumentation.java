/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

import java.io.PrintStream;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import com.servoy.base.util.ITagResolver;
import com.servoy.j2db.documentation.ClientSupport;
import com.servoy.j2db.documentation.IDocumentationManager;
import com.servoy.j2db.documentation.IFunctionDocumentation;
import com.servoy.j2db.documentation.IObjectDocumentation;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.util.Utils;

public class ObjectDocumentation implements Comparable<ObjectDocumentation>, IObjectDocumentation
{

	// top level tag
	private static final String TAG_OBJECT = "object";

	// top level attributes
	private static final String ATTR_PUBLICNAME = "publicName";
	private static final String ATTR_SCRIPTINGNAME = "scriptingName";
	private static final String ATTR_REALCLASS = "realClass";
	private static final String ATTR_QUALIFIEDNAME = "qualifiedName";
	private static final String ATTR_DEPRECATED = "deprecated";
	private static final String ATTR_NAME = "name";
	private static final String ATTR_EXTENDS_COMPONENT = "extendsComponent";
	private static final String ATTR_CLIENT_SUPPORT = "clientSupport";
	private static final String ATTR_SERVOY_MOBILE = "servoyMobile"; // legacy

	// child tags
	private static final String TAG_CONSTANTS = "constants";
	private static final String TAG_PROPERTIES = "properties";
	private static final String TAG_CONSTRUCTORS = "constructors";
	private static final String TAG_FUNCTIONS = "functions";
	private static final String TAG_EVENTS = "events";
	private static final String TAG_COMMANDS = "commands";
	private static final String TAG_UNKNOWN = "unknown";
	private static final String TAG_RETURNEDTYPES = "returnTypes";
	private static final String TAG_RETURNEDTYPE = "returnType";
	private static final String TAG_SERVERPROPERTIES = "serverProperties";
	private static final String TAG_SERVERPROPERTY = "serverProperty";
	private static final String TAG_DESCRIPTION = "description";

	private final String category;
	private final String qualifiedName;
	private final String publicName;
	private final String scriptingName;
	private final String realClass;
	private String extendsComponent;
	private String description;
	private boolean deprecated;
	private final String[] parentClasses;
	private final SortedSet<IFunctionDocumentation> functions = new TreeSet<>();
	private final SortedSet<String> returnedTypes = new TreeSet<>();
	private final boolean hide = false;
	private ClientSupport clientSupport = null;

	private final SortedMap<String, String> serverProperties = new TreeMap<String, String>();

	public ObjectDocumentation(String category, String qualifiedName, String publicName, String scriptingName, String realClass, String extendsComponent,
		String[] parentClasses,
		ClientSupport csp)
	{
		if (category == null) throw new IllegalArgumentException("The category cannot be null.");
		if (qualifiedName == null) throw new IllegalArgumentException("The qualified name cannot be null.");
		this.category = category;
		this.qualifiedName = qualifiedName;
		this.publicName = publicName;
		this.scriptingName = scriptingName;
		this.realClass = realClass;
		this.extendsComponent = extendsComponent;
		this.parentClasses = parentClasses;
		this.deprecated = false;

		this.clientSupport = csp;
	}

	public ObjectDocumentation(String category, String qualifiedName, String publicName, String scriptingName, String realClass, String extendsComponent,
		String[] parentClasses)
	{
		this(category, qualifiedName, publicName, scriptingName, realClass, extendsComponent, parentClasses, null);
	}

	public void runResolver(ITagResolver resolver)
	{
		for (IFunctionDocumentation fdoc : functions)
		{
			fdoc.runResolver(resolver);
		}
	}

	public void check(PrintStream out, boolean dontCheckSyntax, DocumentationManager docManager)
	{
		if (this.isHide()) return;

		// Skip checking JSLib for now. It's full of JS errors.
		if (this.category.equals("jslib")) return;

		boolean inExtensions = this.category.equals(ServoyDocumented.PLUGINS) || this.category.equals(ServoyDocumented.BEANS);

		if (this.isDeprecated()) return;

		out.println("Checking object '" + this.qualifiedName + "'.");
		for (IFunctionDocumentation fdoc : functions)
		{
			if (fdoc instanceof FunctionDocumentation)
			{
				((FunctionDocumentation)fdoc).check(out, !category.equals(ServoyDocumented.DESIGNTIME), dontCheckSyntax, inExtensions, inExtensions,
					docManager);
			}
		}
	}

	public String getCategory()
	{
		return category;
	}

	public String getQualifiedName()
	{
		return qualifiedName;
	}

	public String getPublicName()
	{
		return publicName;
	}

	public String getScriptingName()
	{
		return scriptingName;
	}

	public String getRealClass()
	{
		return realClass;
	}

	public String getDescription(ClientSupport csp)
	{
		ClientSupport searchCSp = csp;
		if (searchCSp == null) searchCSp = ClientSupport.Default;

		ClientSupport thisObjectsSupport = getClientSupport();
		if (thisObjectsSupport == null) thisObjectsSupport = ClientSupport.Default;

		if (thisObjectsSupport.hasSupport(csp)) return description;
		else return null;
	}

	@Override
	public String getExtendsClass()
	{
		return extendsComponent;
	}

	public String[] getParentClasses()
	{
		return parentClasses;
	}

	public SortedSet<String> getReturnedTypes()
	{
		return returnedTypes;
	}

	public void addReturnedType(String qname)
	{
		if (qname != null && !Utils.equalObjects(qname, this.qualifiedName)) returnedTypes.add(qname);
	}

	public void addFunction(IFunctionDocumentation function)
	{
		if (function != null) functions.add(function);
	}

	public boolean isHide()
	{
		return hide;
	}


	public boolean isDeprecated()
	{
		return deprecated;
	}

	public void setDeprecated(boolean deprecated)
	{
		this.deprecated = deprecated;
	}

	public void setClientSupport(ClientSupport clientSupport)
	{
		this.clientSupport = clientSupport;
	}

	public void addServerProperty(String name, String description)
	{
		this.serverProperties.put(name, description);
	}

	public SortedSet<IFunctionDocumentation> getFunctions()
	{
		return functions;
	}

	public IFunctionDocumentation getFunction(String functionName, int argCount)
	{
		for (IFunctionDocumentation fdoc : functions)
		{
			if (fdoc.answersTo(functionName, argCount))
			{
				return fdoc;
			}
		}
		return null;
	}

	public IFunctionDocumentation getFunction(String functionName, Class< ? >[] argumentsTypes)
	{
		if (argumentsTypes == null)
		{
			return getFunction(functionName);
		}
		for (IFunctionDocumentation fdoc : functions)
		{
			if (fdoc.answersTo(functionName, argumentsTypes))
			{
				return fdoc;
			}
		}
		return null;
	}

	public IFunctionDocumentation getFunction(String functionName, String[] argumentsTypes)
	{
		for (IFunctionDocumentation fdoc : functions)
		{
			if (fdoc.answersTo(functionName, argumentsTypes))
			{
				return fdoc;
			}
		}
		return null;
	}

	public IFunctionDocumentation getFunction(String functionName)
	{
		// Take the first documented function with the specified name. If no function with
		// the specified name is documented, take the first function with that name. If there
		// is no function with the given name, just take null.
		IFunctionDocumentation first = null;
		for (IFunctionDocumentation fdoc : functions)
		{
			if (fdoc.answersTo(functionName))
			{
				if (first == null) first = fdoc;
				if (fdoc.getState() == IFunctionDocumentation.STATE_DOCUMENTED) return fdoc;
			}
		}
		return first;
	}

	public void fixExtendsComponent(IDocumentationManager docManager)
	{
		if (extendsComponent != null)
		{
			DocumentationLogger.getInstance().getOut().println(
				"Fixing extendsComponent for object '" + getQualifiedName() + "': " + (extendsComponent != null ? extendsComponent : "N/A"));
			// Try to see if the extendsComponent refers to a qualified name
			IObjectDocumentation ref = docManager.getObjectByQualifiedName(extendsComponent);
			if (ref == null)
			{
				// If not, then it's a public name. Search for the referenced object. Report if there are
				// more than one objects with that public name.
				for (IObjectDocumentation iod : docManager.getObjects().values())
				{
					if (iod.getPublicName() != null && iod.getPublicName().equals(extendsComponent))
					{
						if (ref != null)
						{
							DocumentationLogger.getInstance().getOut().println(
								"ERROR: The public name '" + extendsComponent + "' used at " + ATTR_EXTENDS_COMPONENT +
									" attribute appears at more than one object: '" + ref.getQualifiedName() + "' and '" + iod.getQualifiedName() + "'.");
						}
						else
						{
							ref = iod;
						}
					}
				}
				if (ref == null)
				{
					DocumentationLogger.getInstance().getOut().println("ERROR: The public name '" + extendsComponent + "' used at " + ATTR_EXTENDS_COMPONENT +
						" attribute does not refer to any object and will be ignored.");
					extendsComponent = null;
				}
				else
				{
					extendsComponent = ref.getQualifiedName();
				}
			}
			else
			{
				DocumentationLogger.getInstance().getOut().println("extendsComponent '" + extendsComponent + "' is a valid qualified name.");
			}
		}
	}

	public void beautifyTypes(IDocumentationManager docManager)
	{
		DocumentationLogger.getInstance().getOut().println("Beautifying types in object '" + getQualifiedName() + "'.");
		SortedSet<String> tempRetTypes = new TreeSet<String>(returnedTypes);
		returnedTypes.clear();
		for (String s : tempRetTypes)
		{
			boolean annotated = docManager.getObjectByQualifiedName(DocumentationHelper.removeArraySuffix(s)) != null;
			String bs = DocumentationHelper.mapReturnedType(s, annotated, qualifiedName);
			DocumentationLogger.getInstance().getOut().println("\t'" + s + "' becomes '" + bs + "'.");
			returnedTypes.add(bs);
		}
		for (IFunctionDocumentation fdoc : functions)
		{
			if (fdoc instanceof FunctionDocumentation)
			{
				((FunctionDocumentation)fdoc).beautifyTypes(docManager, this);
			}
		}
	}

	public boolean goesToXML(boolean hideDeprecated)
	{
		if (this.isHide()) return false;
		if (this.isDeprecated() && hideDeprecated) return false;
		return true;
	}

	public Element toXML(IDocumentationManager docManager, boolean hideDeprecated, boolean pretty)
	{
		Element objElement = DocumentHelper.createElement(TAG_OBJECT);

		if (deprecated) objElement.addAttribute(ATTR_DEPRECATED, Boolean.TRUE.toString());
		if (clientSupport != null) objElement.addAttribute(ATTR_CLIENT_SUPPORT, clientSupport.toAttribute());
		if (extendsComponent != null && extendsComponent.trim().length() > 0) objElement.addAttribute(ATTR_EXTENDS_COMPONENT, extendsComponent);
		if (publicName != null && publicName.trim().length() > 0) objElement.addAttribute(ATTR_PUBLICNAME, publicName);
		if (qualifiedName != null && qualifiedName.trim().length() > 0) objElement.addAttribute(ATTR_QUALIFIEDNAME, qualifiedName);
		if (scriptingName != null && scriptingName.trim().length() > 0) objElement.addAttribute(ATTR_SCRIPTINGNAME, scriptingName);

		putFunctionsByType(IFunctionDocumentation.TYPE_CONSTANT, objElement, TAG_CONSTANTS, hideDeprecated, pretty);
		putFunctionsByType(IFunctionDocumentation.TYPE_CONSTRUCTOR, objElement, TAG_CONSTRUCTORS, hideDeprecated, pretty);
		putFunctionsByType(IFunctionDocumentation.TYPE_PROPERTY, objElement, TAG_PROPERTIES, hideDeprecated, pretty);
		putFunctionsByType(IFunctionDocumentation.TYPE_FUNCTION, objElement, TAG_FUNCTIONS, hideDeprecated, pretty);
		putFunctionsByType(IFunctionDocumentation.TYPE_EVENT, objElement, TAG_EVENTS, hideDeprecated, pretty);
		putFunctionsByType(IFunctionDocumentation.TYPE_COMMAND, objElement, TAG_COMMANDS, hideDeprecated, pretty);
		putFunctionsByType(IFunctionDocumentation.TYPE_UNKNOWN, objElement, TAG_UNKNOWN, hideDeprecated, pretty);

		int retCount = 0;
		for (String qname : returnedTypes)
		{
			IObjectDocumentation retObjDoc = docManager.getObjectByQualifiedName(qname);
			if (retObjDoc != null && retObjDoc.goesToXML(hideDeprecated)) retCount++;
		}

		if (retCount > 0)
		{
			Element returnedTypesElement = objElement.addElement(TAG_RETURNEDTYPES);
			for (String qname : returnedTypes)
			{
				IObjectDocumentation retObjDoc = docManager.getObjectByQualifiedName(qname);
				if (retObjDoc != null && retObjDoc.goesToXML(hideDeprecated))
				{
					Element returnedTypeElement = returnedTypesElement.addElement(TAG_RETURNEDTYPE);
					returnedTypeElement.addAttribute(ATTR_QUALIFIEDNAME, qname);

					if (retObjDoc.getClientSupport() != null) returnedTypeElement.addAttribute(ATTR_CLIENT_SUPPORT, retObjDoc.getClientSupport().toAttribute());
				}
			}
		}

		if (serverProperties.size() > 0)
		{
			Element serverPropertiesRoot = objElement.addElement(TAG_SERVERPROPERTIES);
			for (String name : serverProperties.keySet())
			{
				String desc = serverProperties.get(name);
				Element propElem = serverPropertiesRoot.addElement(TAG_SERVERPROPERTY);
				propElem.addAttribute(ATTR_NAME, name);
				propElem.addElement(TAG_DESCRIPTION).addCDATA(desc);
			}
		}

		return objElement;
	}

	private void putFunctionsByType(Integer type, Element objElement, String holderName, boolean hideDeprecated, boolean pretty)
	{
		int count = 0;
		for (IFunctionDocumentation fdoc : functions)
			if (fdoc.getType() == type && (!fdoc.isDeprecated() || !hideDeprecated)) count++;
		if (count > 0)
		{
			Element propertiesElement = objElement.addElement(holderName);
			for (IFunctionDocumentation fdoc : functions)
			{
				if (fdoc.getType() == type && (!fdoc.isDeprecated() || !hideDeprecated))
				{
					propertiesElement.add(fdoc.toXML(ServoyDocumented.DESIGNTIME.equals(category), pretty));
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	public static ObjectDocumentation fromXML(Element objectElement, String category, ClassLoader loader)
	{
		if (!TAG_OBJECT.equals(objectElement.getName())) return null;

		String publicName = objectElement.attributeValue(ATTR_PUBLICNAME);
		String scriptingName = objectElement.attributeValue(ATTR_SCRIPTINGNAME);
		String realClass = objectElement.attributeValue(ATTR_REALCLASS);
		String qualifiedName = objectElement.attributeValue(ATTR_QUALIFIEDNAME);
		String extendsFrom = objectElement.attributeValue(ATTR_EXTENDS_COMPONENT);
		String deprecated = objectElement.attributeValue(ATTR_DEPRECATED);

		ClientSupport clientSupport = ClientSupport.fromString(objectElement.attributeValue(ATTR_CLIENT_SUPPORT));
		if (clientSupport == null && Boolean.TRUE.toString().equals(objectElement.attributeValue(ATTR_SERVOY_MOBILE)))
		{
			// legacy
			clientSupport = ClientSupport.mc_wc_sc;
		}

		ObjectDocumentation objDoc = new ObjectDocumentation(category, qualifiedName, publicName, scriptingName, realClass, extendsFrom, null);

		Node descriptionNode = objectElement.element("description");
		if (descriptionNode != null)
		{
			objDoc.setDescription(descriptionNode.getText());
		}

		if (deprecated != null && deprecated.equals(Boolean.TRUE.toString())) objDoc.setDeprecated(true);
		objDoc.setClientSupport(clientSupport);

		loadFunctions(objectElement, objDoc, TAG_CONSTANTS, loader);
		loadFunctions(objectElement, objDoc, TAG_CONSTRUCTORS, loader);
		loadFunctions(objectElement, objDoc, TAG_PROPERTIES, loader);
		loadFunctions(objectElement, objDoc, TAG_FUNCTIONS, loader);
		loadFunctions(objectElement, objDoc, TAG_EVENTS, loader);
		loadFunctions(objectElement, objDoc, TAG_COMMANDS, loader);
		loadFunctions(objectElement, objDoc, TAG_UNKNOWN, loader);

		Element returnedTypesElement = objectElement.element(TAG_RETURNEDTYPES);
		if (returnedTypesElement != null)
		{
			Iterator<Element> returnedTypes = returnedTypesElement.elementIterator(TAG_RETURNEDTYPE);
			while (returnedTypes.hasNext())
			{
				Element returnedTypeElement = returnedTypes.next();
				String qname = returnedTypeElement.attributeValue(ATTR_QUALIFIEDNAME);
				objDoc.addReturnedType(qname);
			}
		}

		Element serverPropertiesElement = objectElement.element(TAG_SERVERPROPERTIES);
		if (serverPropertiesElement != null)
		{
			Iterator<Element> serverPropertiesIter = serverPropertiesElement.elementIterator(TAG_SERVERPROPERTY);
			while (serverPropertiesIter.hasNext())
			{
				Element serverPropertyElement = serverPropertiesIter.next();
				String name = serverPropertyElement.attributeValue(ATTR_NAME);
				Element descElement = serverPropertyElement.element(TAG_DESCRIPTION);
				if (descElement != null)
				{
					String desc = descElement.getTextTrim();
					objDoc.addServerProperty(name, desc);
				}
			}
		}

		return objDoc;
	}

	private void setDescription(String description)
	{
		this.description = description;
	}

	private static void loadFunctions(Element objectElement, ObjectDocumentation objDoc, String holderTag, ClassLoader loader)
	{
		Element functionsElement = objectElement.element(holderTag);
		if (functionsElement != null)
		{
			Iterator<Element> functions = functionsElement.elementIterator();
			while (functions.hasNext())
			{
				Element fel = functions.next();
				IFunctionDocumentation fdoc = FunctionDocumentation.fromXML(fel, loader);
				if (fdoc != null)
				{
					objDoc.addFunction(fdoc);
				}
			}
		}
	}

	public int compareTo(ObjectDocumentation o)
	{
		return this.publicName.compareTo(o.publicName);
	}

	public ClientSupport getClientSupport()
	{
		return this.clientSupport;
	}
}
