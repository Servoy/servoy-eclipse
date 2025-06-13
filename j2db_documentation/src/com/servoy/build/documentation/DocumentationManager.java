package com.servoy.build.documentation;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import com.servoy.base.util.ITagResolver;
import com.servoy.j2db.documentation.IDocumentationManager;
import com.servoy.j2db.documentation.IFunctionDocumentation;
import com.servoy.j2db.documentation.IObjectDocumentation;
import com.servoy.j2db.documentation.QualifiedDocumentationName;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.util.Pair;

public class DocumentationManager implements IAggregatedDocumentation, IDocumentationManager
{
	// top level tag
	public static String TAG_SERVOYDOC = "servoydoc";

	// top level attributes
	public static String ATTR_BUILDNUMBER = "buildNumber";
	public static String ATTR_VERSION = "version";
	public static String ATTR_GENTIME = "generationTime";

	private final SortedMap<String, IObjectDocumentation> objects;
	private final String categoryFilter;

	public DocumentationManager(String categoryFilter)
	{
		this.objects = new TreeMap<String, IObjectDocumentation>();
		this.categoryFilter = categoryFilter;
	}

	public IFunctionDocumentation createFunctionDocumentation(String mainName, Class< ? >[] argsTypes, Integer type, boolean deprecated, boolean varargs,
		int state)
	{
		return new FunctionDocumentation(mainName, argsTypes, type, deprecated, varargs, state);
	}

	public IObjectDocumentation createObjectDocumentation(String category, String qualifiedName, String publicName, String scriptingName, String realClass,
		String extendsComponent, String[] parentClasses)
	{
		return new ObjectDocumentation(category, qualifiedName, publicName, scriptingName, realClass, extendsComponent, parentClasses);
	}

	public SortedMap<String, IObjectDocumentation> getObjects()
	{
		return objects;
	}

	public IObjectDocumentation getObjectByQualifiedName(String qualifiedName)
	{
		for (IObjectDocumentation objDoc : objects.values())
		{
			if (objDoc.getQualifiedName().equals(qualifiedName)) return objDoc;
		}
		return null;
	}

	public void addObject(IObjectDocumentation object)
	{
		if (object != null) objects.put(object.getQualifiedName(), object);
	}

	public void runResolver(ITagResolver resolver)
	{
		for (IObjectDocumentation objDoc : objects.values())
		{
			objDoc.runResolver(resolver);
		}
	}

	private void solveDependencies()
	{
		for (IObjectDocumentation objDoc : objects.values())
		{
			for (IFunctionDocumentation fdoc : objDoc.getFunctions())
			{
				solveFunction(objDoc, fdoc);
			}
		}
	}

	public void check(PrintStream out, boolean dontCheckSyntax)
	{
		for (IObjectDocumentation objDoc : objects.values())
		{
			if (objDoc instanceof ObjectDocumentation)
			{
				((ObjectDocumentation)objDoc).check(out, dontCheckSyntax, this);
			}
		}
	}

	private void solveFunction(IObjectDocumentation objDoc, IFunctionDocumentation fdoc)
	{
		if (fdoc.isBeingSolved())
		{
			System.out.println("Circular dependency detected at " + objDoc.getQualifiedName() + " -- " + fdoc.getMainName());
			return;
		}
		fdoc.setBeingSolved(true);

		IObjectDocumentation targetObjDoc = null;
		IFunctionDocumentation targetFncDoc = null;
		if (fdoc.getState() != IFunctionDocumentation.STATE_DOCUMENTED)
		{
			if (objDoc.getParentClasses() != null)
			{
				for (String parentName : objDoc.getParentClasses())
				{
					IObjectDocumentation parentObjDoc = objects.get(parentName);
					if (parentObjDoc != null)
					{
						Pair<IObjectDocumentation, IFunctionDocumentation> pair = searchFunctionUpward(parentObjDoc, fdoc);
						if (pair != null)
						{
							targetObjDoc = pair.getLeft();
							targetFncDoc = pair.getRight();
							break;
						}
					}
				}
			}
		}
		else if (fdoc.needsRedirect())
		{
			QualifiedDocumentationName targetName = fdoc.getRedirect();
			targetObjDoc = getObjectByQualifiedName(targetName.getClassName());
			if (targetObjDoc != null)
			{
				targetFncDoc = targetObjDoc.getFunction(targetName.getMemberName(), targetName.getArgumentsTypes());
			}
		}

		if (targetObjDoc != null && targetFncDoc != null)
		{
			// TODO: A bit paranoid, should be rewritten.
			solveFunction(targetObjDoc, targetFncDoc);
//			if (targetFncDoc.getState() != IFunctionDocumentation.STATE_DOCUMENTED || targetFncDoc.needsRedirect()) solveFunction(targetObjDoc, targetFncDoc);

			if (fdoc instanceof FunctionDocumentation && targetFncDoc instanceof FunctionDocumentation)
			{
				if (fdoc.getState() != IFunctionDocumentation.STATE_DOCUMENTED)
				{
					((FunctionDocumentation)fdoc).copyFrom((FunctionDocumentation)targetFncDoc, false,
						fdoc.getState() == IFunctionDocumentation.STATE_INEXISTENT);
				}
				else if (fdoc.needsRedirect())
				{
					((FunctionDocumentation)fdoc).copyFrom((FunctionDocumentation)targetFncDoc, fdoc.getRedirectType() == FunctionDocumentation.REDIRECT_SAMPLE,
						false);
				}
			}
		}

		QualifiedDocumentationName cloneDescTargetName = fdoc.getCloneDescRedirect();
		if (cloneDescTargetName != null)
		{
			IObjectDocumentation cloneDescTarget = getObjectByQualifiedName(cloneDescTargetName.getClassName());
			if (cloneDescTarget != null)
			{
				IFunctionDocumentation cloneDescFuncTarget = cloneDescTarget.getFunction(cloneDescTargetName.getMemberName(),
					cloneDescTargetName.getArgumentsTypes());
				if (cloneDescFuncTarget != null)
				{
					fdoc.setDescriptions(cloneDescFuncTarget.getDescriptions());
				}
			}
		}

		fdoc.setBeingSolved(false);
	}

	private Pair<IObjectDocumentation, IFunctionDocumentation> searchFunctionUpward(IObjectDocumentation objDoc, IFunctionDocumentation fdoc)
	{
		for (IFunctionDocumentation fd : objDoc.getFunctions())
		{
			if (fd.answersTo(fdoc.getMainName(), fdoc.getArgumentsTypes()))
			{
				if (fd.getState() == IFunctionDocumentation.STATE_DOCUMENTED) return new Pair<IObjectDocumentation, IFunctionDocumentation>(objDoc, fd);
			}
		}

		if (objDoc.getParentClasses() != null)
		{
			for (String parentName : objDoc.getParentClasses())
			{
				IObjectDocumentation parentObjDoc = objects.get(parentName);
				if (parentObjDoc != null)
				{
					Pair<IObjectDocumentation, IFunctionDocumentation> parentFDoc = searchFunctionUpward(parentObjDoc, fdoc);
					if (parentFDoc != null) return parentFDoc;
				}
			}
		}

		return null;
	}

	public void fixExtendsComponent()
	{
		DocumentationLogger.getInstance().getOut().println("Fixing extendsComponent attribute...");
		for (IObjectDocumentation objDoc : objects.values())
		{
			if (objDoc instanceof ObjectDocumentation)
			{
				((ObjectDocumentation)objDoc).fixExtendsComponent(this);
			}
		}
	}

	public void beautifyTypes()
	{
		DocumentationLogger.getInstance().getOut().println("Beautifying types...");
		for (IObjectDocumentation objDoc : objects.values())
		{
			if (objDoc instanceof ObjectDocumentation)
			{
				((ObjectDocumentation)objDoc).beautifyTypes(this);
			}
		}
	}

	public Element getAggregatedElement(boolean pretty)
	{
		boolean hideDeprecated = false; // When aggregating we dont hide deprecated stuff.
		Element rootElement = DocumentHelper.createElement(TAG_SERVOYDOC);

		String[] categories = new String[] { ServoyDocumented.RUNTIME, ServoyDocumented.DESIGNTIME, ServoyDocumented.PLUGINS, ServoyDocumented.BEANS, ServoyDocumented.JSLIB };

		for (String cat : categories)
		{
			if (cat.equals(categoryFilter) || categoryFilter == null)
			{
				toXMLByCategory(cat, rootElement, hideDeprecated, pretty);
			}
		}
		return rootElement;
	}

	public Element toXML(String filename, boolean hideDeprecated, boolean pretty)
	{
		solveDependencies();
		fixExtendsComponent();

		Document document = DocumentHelper.createDocument();
		Element rootElement = getAggregatedElement(pretty);
		document.add(rootElement);

		OutputFormat outformat = OutputFormat.createPrettyPrint();
		outformat.setEncoding("UTF-8");

		try
		{
			XMLWriter writer = new XMLWriter(new PrintWriter(new File(filename)), outformat);
			writer.write(document);
			writer.flush();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		return rootElement;
	}

	private void toXMLByCategory(String category, Element parent, boolean hideDeprecated, boolean pretty)
	{
		SortedSet<IObjectDocumentation> ordered = new TreeSet<IObjectDocumentation>();
		for (IObjectDocumentation objDoc : objects.values())
		{
			if (objDoc.goesToXML(hideDeprecated) && objDoc.getCategory().equals(category))
			{
				ordered.add(objDoc);
			}
		}
		if (ordered.size() > 0)
		{
			Element runtimeElement = parent.addElement(category);
			for (IObjectDocumentation objDoc : ordered)
			{
				runtimeElement.add(objDoc.toXML(this, hideDeprecated, pretty));
			}
		}
	}

	public void gatherFrom(DocumentationManager other)
	{
		if (other != null)
		{
			for (String qname : other.objects.keySet())
			{
				if (!this.objects.containsKey(qname)) this.objects.put(qname, other.objects.get(qname));
			}
		}
	}

	public static DocumentationManager fromXML(String path, ClassLoader loader)
	{
		try
		{
			SAXReader reader = new SAXReader();
			Document doc = reader.read(path);
			return fromDoc(doc, loader);
		}
		catch (DocumentException e)
		{
			e.printStackTrace();
			return null;
		}
		finally
		{
			internedText = new HashMap<String, String>(8);
		}
	}

	public static DocumentationManager fromXML(InputStream is, ClassLoader loader)
	{
		try
		{
			SAXReader reader = new SAXReader();
			Document doc = reader.read(is);
			return fromDoc(doc, loader);
		}
		catch (DocumentException e)
		{
			e.printStackTrace();
			return null;
		}
		finally
		{
			internedText = new HashMap<String, String>(8);
		}
	}

	public static DocumentationManager fromXML(URL url, ClassLoader loader)
	{
		try
		{
			SAXReader reader = new SAXReader();
			if (reader != null)
			{
				Document doc = reader.read(url);
				return fromDoc(doc, loader);
			}
		}
		catch (DocumentException e)
		{
			e.printStackTrace();
		}
		finally
		{
			internedText = new HashMap<String, String>(8);
		}
		return null;
	}

	private static Map<String, String> internedText = new HashMap<String, String>(128);

	public static String getInternedText(String txt)
	{
		String string = internedText.get(txt);
		if (string != null) return string;
		internedText.put(txt, txt);
		return txt;
	}

	private static DocumentationManager fromDoc(Document doc, ClassLoader loader)
	{
		// TODO: check based on XML Schema
		// pick the first "servoydoc" tag encountered in a breadth-first traversal of the nodes
		Queue<Element> queue = new LinkedList<Element>();
		queue.add(doc.getRootElement());
		boolean found = false;
		DocumentationManager docManager = null;
		while (!found && !queue.isEmpty())
		{
			Element current = queue.poll();
			if (TAG_SERVOYDOC.equals(current.getName()))
			{
				docManager = new DocumentationManager(null);

				String[] categories = new String[] { ServoyDocumented.RUNTIME, ServoyDocumented.DESIGNTIME, ServoyDocumented.PLUGINS, ServoyDocumented.BEANS, ServoyDocumented.JSLIB };

				for (String cat : categories)
				{
					fromXMLByCategory(cat, current, docManager, loader);
				}

				docManager.solveDependencies();
				found = true;
			}
			else
			{
				// add all children elements to the queue
				for (Object obj : current.elements())
				{
					if (obj instanceof Element)
					{
						queue.add((Element)obj);
					}
				}
			}
		}
		return docManager;
	}

	private static void fromXMLByCategory(String category, Element parent, DocumentationManager docManager, ClassLoader loader)
	{
		Element runtimeElement = parent.element(category);
		if (runtimeElement != null)
		{
			Iterator<Element> runtimeObjects = runtimeElement.elementIterator();
			while (runtimeObjects.hasNext())
			{
				ObjectDocumentation objDoc = ObjectDocumentation.fromXML(runtimeObjects.next(), category, loader);
				if (objDoc != null)
				{
					docManager.addObject(objDoc);
				}
			}
		}
	}
}
