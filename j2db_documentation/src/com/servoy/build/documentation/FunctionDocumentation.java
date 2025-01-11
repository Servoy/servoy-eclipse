package com.servoy.build.documentation;

import java.io.CharArrayReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Parser;

import com.servoy.base.util.ITagResolver;
import com.servoy.j2db.documentation.ClientSupport;
import com.servoy.j2db.documentation.DocumentationUtil;
import com.servoy.j2db.documentation.IDescriptionDocumentation;
import com.servoy.j2db.documentation.IDocumentationManager;
import com.servoy.j2db.documentation.IFunctionDocumentation;
import com.servoy.j2db.documentation.IParameterDocumentation;
import com.servoy.j2db.documentation.ISampleDocumentation;
import com.servoy.j2db.documentation.QualifiedDocumentationName;
import com.servoy.j2db.util.Text;

public class FunctionDocumentation implements Comparable<FunctionDocumentation>, IFunctionDocumentation
{
//	private static final Class< ? >[] NO_ARGUMENTS = new Class< ? >[] { };

	// top level tags
	private static final String TAG_UNKNOWN = "unknown";
	private static final String TAG_CONSTRUCTOR = "constructor";
	private static final String TAG_FUNCTION = "function";
	private static final String TAG_PROPERTY = "property";
	private static final String TAG_CONSTANT = "constant";
	private static final String TAG_EVENT = "event";
	private static final String TAG_COMMAND = "command";

	// top level attributes
	private static final String ATTR_NAME = "name";
	private static final String TAG_ARGUMENTSTYPES = "argumentsTypes";
	private static final String TAG_ARGUMENTTYPE = "argumentType";
	private static final String ATTR_DEPRECATED = "deprecated";
	private static final String ATTR_UNDOCUMENTED = "undocumented";
	private static final String ATTR_VARARGS = "varargs";
	private static final String ATTR_DEFAULTVALUE = "defaultvalue";
	private static final String ATTR_SINCE = "since";
	private static final String ATTR_UNTIL = "until";
	private static final String ATTR_TYPE = "type";
	private static final String ATTR_TYPECODE = "typecode";
	private static final String ATTR_SPECIAL = "special";
	private static final String ATTR_SIMPLIFIEDSIGNATURE = "simplifiedSignature";
	private static final String ATTR_STATICCALL = "staticCall";
	private static final String ATTR_CLIENT_SUPPORT = "clientSupport";

	// child tags
	private static final String TAG_SUMMARIES = "summaries";
	private static final String TAG_SUMMARY = "summary";
	private static final String TAG_DESCRIPTIONS = "descriptions";
	private static final String TAG_DESCRIPTION = "description";
	private static final String TAG_SAMPLES = "samples";
	private static final String TAG_SAMPLE = "sample";
	private static final String TAG_PARAMETERS = "parameters";
	private static final String TAG_RETURN = "return";
	private static final String TAG_LINKS = "links";
	private static final String TAG_LINK = "link";
	private static final String TAG_DEPRECATED = "deprecated";
	private static final String TAG_URL = "url";

	// mapping from type codes to tags
	private static SortedMap<Integer, String> typesToTags = new TreeMap<Integer, String>();
	static
	{
		typesToTags.put(TYPE_UNKNOWN, TAG_UNKNOWN);
		typesToTags.put(TYPE_FUNCTION, TAG_FUNCTION);
		typesToTags.put(TYPE_PROPERTY, TAG_PROPERTY);
		typesToTags.put(TYPE_CONSTANT, TAG_CONSTANT);
		typesToTags.put(TYPE_EVENT, TAG_EVENT);
		typesToTags.put(TYPE_COMMAND, TAG_COMMAND);
		typesToTags.put(TYPE_CONSTRUCTOR, TAG_CONSTRUCTOR);
	}

	// redirect types
	public static final int REDIRECT_NONE = 0;
	public static final int REDIRECT_FULL = 1;
	public static final int REDIRECT_SAMPLE = 2;

	// Dummy string to keep in the map when a link has no alias.
	public static final String NO_ALIAS = "!@#$%^&*()";

	private final String mainName;
	private final Class< ? >[] argumentsTypes;
	private final SortedSet<String> names;
	private final Integer type;
	private Class< ? > returnedType;
	private String returnDescription;
	private final List<IDescriptionDocumentation> summaries;
	private final List<IDescriptionDocumentation> descriptions;
	private final List<ISampleDocumentation> samples;
	private boolean deprecated;
	private boolean simplifiedSignature = false;
	private boolean staticCall = false;
	private String deprecatedText;
	private int state;
	private final LinkedHashMap<String, IParameterDocumentation> parameters;
	private String defaultValue;
	private String since;
	private String until;
	private final boolean varargs;

	private int redirectType;
	private QualifiedDocumentationName redirect;
	private QualifiedDocumentationName cloneDescRedirect;
	private boolean redirectSolved;
	private boolean cloneDescRedirectSolved;

	private boolean beingSolved = false;

	private boolean isSpecial = false;

	private ClientSupport clientSupport = null;

	private final List<String> errors = new ArrayList<String>();

	private final SortedMap<String, String> links = new TreeMap<String, String>();

	public FunctionDocumentation(String mainName, Class< ? >[] argsTypes, Integer type, boolean deprecated, boolean varargs, int state)
	{
		if (mainName == null) throw new IllegalArgumentException("Main name cannot be null.");
		names = new TreeSet<String>();
		names.add(mainName);
		this.mainName = mainName;
		this.argumentsTypes = argsTypes;
		this.type = type;
		this.deprecated = deprecated;
		this.varargs = varargs;
		this.state = state;
		this.descriptions = new ArrayList<IDescriptionDocumentation>();
		this.summaries = new ArrayList<IDescriptionDocumentation>();
		this.samples = new ArrayList<ISampleDocumentation>();
		redirectType = REDIRECT_NONE;
		parameters = new LinkedHashMap<String, IParameterDocumentation>();
		simplifiedSignature = false;
	}

	public void addLink(String link)
	{
		String theLink;
		String theAlias;
		int spaceIndex = link.indexOf(' ');
		if (spaceIndex > 0)
		{
			theLink = link.substring(0, spaceIndex);
			theAlias = link.substring(spaceIndex).trim();
		}
		else
		{
			theLink = link;
			theAlias = NO_ALIAS;
		}
		addLink(theLink, theAlias);
	}

	private void addLink(String link, String alias)
	{
		if (!links.containsKey(link)) links.put(link, alias);
	}

	public String getDeprecatedText()
	{
		return deprecatedText;
	}

	public void setDeprecatedText(String text)
	{
		this.deprecatedText = text;
		if (deprecatedText != null && deprecatedText.trim().length() == 0)
		{
			deprecatedText = null;
		}
	}

	public String getFullSignature()
	{
		return getFullSignature(true, false);
	}

	public String getFullJSTranslatedSignature(boolean withNames, boolean withTypes)
	{
		String signature = getJSTranslatedSignature(null, withNames, withTypes);
		if (signature == null) return null;

		StringBuilder sb = new StringBuilder();

		String returnTypeString = null;
		if (!simplifiedSignature && returnedType != null) returnTypeString = getClassStringType(returnedType);
		if (withNames && returnTypeString != null) sb.append(returnTypeString).append(" ");
		sb.append(signature);
		if (!withNames && returnTypeString != null) sb.append(" - ").append(returnTypeString);
		return sb.toString();
	}

	/**
	 * @param typeTranslationMap
	 * @param retType
	 * @return
	 */
	private String getClassStringType(Class< ? > t)
	{
		if (t == null) return null;
		int counter = 0;
		Class< ? > retType = t;
		while (retType.isArray())
		{
			retType = retType.getComponentType();
			counter++;
		}
		String returnTypeString = DocumentationUtil.getJavaToJSTypeTranslator().translateJavaClassToJSTypeName(retType);
		while (counter-- > 0)
		{
			returnTypeString = "Array<" + returnTypeString + '>';
		}
		return returnTypeString;
	}

	public String getFullSignature(boolean withNames, boolean withTypes)
	{
		return getFullJSTranslatedSignature(withNames, withTypes);
	}

	public String getSignature(String prefix)
	{
		return getSignature(prefix, true, false);
	}

	private String getJSTranslatedSignature(String prefix, boolean withNames, boolean withTypes)
	{
		StringBuilder sb = new StringBuilder();

		if (prefix != null && !simplifiedSignature && !staticCall)
		{
			if (prefix.endsWith(".") && getMainName().startsWith("[")) sb.append(prefix.substring(0, prefix.length() - 1));
			else sb.append(prefix);
		}

		sb.append(getMainName());
		if (getType() != IFunctionDocumentation.TYPE_PROPERTY && !simplifiedSignature)
		{
			sb.append("(");
			boolean first = true;
			Iterator<IParameterDocumentation> iterator = parameters.values().iterator();
			while (iterator.hasNext())
			{
				IParameterDocumentation parDoc = iterator.next();
				if (!first) sb.append(", ");
				if (parDoc.isOptional()) sb.append("[");
				if (withNames) sb.append(parDoc.getName());
				if (withNames && withTypes) sb.append(":");
				if (withTypes)
				{
					Class< ? > parDocType = parDoc.getType();
					if (parDocType != null)
					{
						if (isVarargs() && !iterator.hasNext())
						{
							Class< ? > componentType = parDocType.getComponentType();
							if (componentType != null) sb.append(getClassStringType(componentType)).append("...");
							else return null;
						}
						else
						{
							sb.append(getClassStringType(parDocType));
						}
					}
					else return null;
				}
				if (parDoc.isOptional()) sb.append("]");
				first = false;
			}
			sb.append(")");
		}
		return sb.toString();
	}

	private String getSignature(String prefix, boolean withNames, boolean withTypes)
	{
		return getJSTranslatedSignature(prefix, withNames, withTypes);
	}

	public void addError(String err)
	{
		errors.add(err);
	}

	public boolean isBeingSolved()
	{
		return beingSolved;
	}

	public void setBeingSolved(boolean beingSolved)
	{
		this.beingSolved = beingSolved;
	}

	public void runResolver(ITagResolver resolver)
	{
		if (samples != null)
		{
			for (ISampleDocumentation sampl : samples)
			{
				sampl.setSampleCode(Text.processTags(sampl.getSampleCode(), resolver));
			}
		}
		if (descriptions != null)
		{
			for (IDescriptionDocumentation descr : descriptions)
			{
				descr.setText(Text.processTags(descr.getText(), resolver));
			}
		}
	}

	public String getMainName()
	{
		return mainName;
	}

	public Class< ? >[] getArgumentsTypes()
	{
		return argumentsTypes;
	}

	public void addName(String name)
	{
		names.add(name);
	}

	public boolean answersTo(String name)
	{
		boolean nameFound = false;
		if (mainName.equals(name)) nameFound = true;
		if (!nameFound)
		{
			for (String s : names)
			{
				if (s.equals(name))
				{
					nameFound = true;
					break;
				}
			}
		}
		return nameFound;
	}

	public boolean answersTo(String name, int argCount)
	{
		boolean nameFound = answersTo(name);
		if (nameFound)
		{
			if (this.argumentsTypes == null)
			{
				if (argCount == 0) return true;
			}
			else
			{
				if (this.argumentsTypes.length == argCount) return true;
			}
		}
		return false;
	}

	public boolean answersTo(String name, Class< ? >[] argsTypes)
	{
		boolean nameFound = answersTo(name);
		if (nameFound)
		{
			if (this.argumentsTypes == null)
			{
				if (argsTypes == null) return true;
			}
			else if (argsTypes != null)
			{
				if (this.argumentsTypes.length == argsTypes.length)
				{
					boolean allOK = true;
					for (int i = 0; i < this.argumentsTypes.length; i++)
					{
						if (!this.argumentsTypes[i].equals(argsTypes[i]))
						{
							allOK = false;
							break;
						}
					}
					if (allOK) return true;
				}
			}
		}
		return false;
	}

	public boolean answersTo(String name, String[] argsTypes)
	{
		boolean nameFound = answersTo(name);
		if (nameFound)
		{
			if (this.argumentsTypes == null)
			{
				if ((argsTypes == null) || (argsTypes == QualifiedDocumentationName.NO_ARGUMENTS)) return true;
			}
			else
			{
				if (this.argumentsTypes.length == argsTypes.length)
				{
					boolean allOK = true;
					for (int i = 0; i < this.argumentsTypes.length; i++)
					{
						if (!this.argumentsTypes[i].getCanonicalName().equals(argsTypes[i]) && !this.argumentsTypes[i].getSimpleName().equals(argsTypes[i]))
						{
							allOK = false;
							break;
						}
					}
					if (allOK) return true;
				}
			}
		}
		return false;
	}

	public Integer getType()
	{
		return this.type;
	}

	public Class< ? > getReturnedType()
	{
		return returnedType;
	}

	public void setReturnedType(Class< ? > retType)
	{
		this.returnedType = retType;
	}

	public String getReturnDescription()
	{
		return returnDescription;
	}

	public void setReturnDescription(String returnDescription)
	{
		this.returnDescription = returnDescription;
	}

	public List<IDescriptionDocumentation> getDescriptions()
	{
		return descriptions;
	}

	public void setDescriptions(List<IDescriptionDocumentation> descriptions)
	{
		this.descriptions.clear();
		this.descriptions.addAll(descriptions);

		// also set summary here
		for (IDescriptionDocumentation descr : descriptions)
		{
			String summary = "";
			int idx = descr.getText().indexOf('.');
			if (idx >= 0) summary = descr.getText().substring(0, idx + 1);
			else summary = descr.getText();
			summaries.add(new DescriptionDocumentation(descr.getClientSupport(), summary));
		}
	}

	public List<IDescriptionDocumentation> getSummaries()
	{
		return summaries;
	}

	// Private for now. Summary is extracted from description.
	// This is only used when deserializing from XML.
	private void setSummaries(List<IDescriptionDocumentation> summaries)
	{
		this.summaries.clear();
		this.summaries.addAll(summaries);
	}

	public boolean isDeprecated()
	{
		return deprecated;
	}

	public boolean isVarargs()
	{
		return varargs;
	}

	public void setDeprecated(boolean deprecated)
	{
		this.deprecated = deprecated;
	}

	public void setState(int state)
	{
		this.state = state;
	}

	public int getState()
	{
		return state;
	}

	public String getDefaultValue()
	{
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue)
	{
		this.defaultValue = defaultValue;
	}

	public String getSince()
	{
		return since;
	}

	public void setSince(String since)
	{
		this.since = since;
	}

	public String getUntil()
	{
		return until;
	}

	public void setUntil(String until)
	{
		this.until = until;
	}

	public boolean isSpecial()
	{
		return isSpecial;
	}

	public void setSpecial(boolean isSpecial)
	{
		this.isSpecial = isSpecial;
	}

	public LinkedHashMap<String, IParameterDocumentation> getArguments()
	{
		return parameters;
	}

	public void addArgument(IParameterDocumentation argDoc)
	{
		if (argDoc != null)
		{
			parameters.put(argDoc.getName(), argDoc);
//			rebuildArgTypes();
		}
	}

//	private void rebuildArgTypes()
//	{
//		List<Class< ? >> argTypesList = new ArrayList<Class< ? >>();
//		for (ParameterDocumentation pdoc : parameters.values())
//		{
//			if (pdoc.getType() != null) argTypesList.add(pdoc.getType());
//			else argTypesList.add(Object.class);
//		}
//		this.argumentsTypes = new Class< ? >[argTypesList.size()];
//		argTypesList.toArray(this.argumentsTypes);
//	}

	public void setRedirect(QualifiedDocumentationName redirect, int redirectType)
	{
		this.redirect = redirect;
		this.redirectType = redirectType;
		redirectSolved = false;
	}

	public QualifiedDocumentationName getRedirect()
	{
		return redirect;
	}

	public int getRedirectType()
	{
		return redirectType;
	}

	public boolean needsRedirect()
	{
		return !redirectSolved && (redirectType != REDIRECT_NONE);
	}

	public void markRedirectSolved()
	{
		redirectSolved = true;
	}

	public void setCloneDescrRedirect(QualifiedDocumentationName cloneDescRedirect)
	{
		this.cloneDescRedirect = cloneDescRedirect;
		cloneDescRedirectSolved = false;
	}

	public QualifiedDocumentationName getCloneDescRedirect()
	{
		return cloneDescRedirect;
	}

	public boolean needCloneDescRedirect()
	{
		return !cloneDescRedirectSolved && (cloneDescRedirect != null);
	}

	public void markCloneDescRedirectSolved()
	{
		cloneDescRedirectSolved = true;
	}

	public void copyFrom(FunctionDocumentation other, boolean onlySample, boolean copyDeprecated)
	{
		setSamples(other.getSamples());
		if (!onlySample)
		{
			setDescriptions(other.descriptions);
			setSummaries(other.summaries);
			this.parameters.clear();
			this.parameters.putAll(other.parameters);
			this.returnDescription = other.returnDescription;
			this.links.clear();
			this.links.putAll(other.links);
		}
		if (copyDeprecated)
		{
			this.deprecated = other.deprecated;
			this.deprecatedText = other.deprecatedText;
		}
		this.state = other.state;
		this.redirectSolved = other.state == STATE_DOCUMENTED;
	}

	public int compareTo(FunctionDocumentation o)
	{
		int result = this.mainName.compareTo(o.mainName);
		if (result == 0)
		{
			String thisArgs = argumentsArray2String(this.argumentsTypes);
			String otherArgs = argumentsArray2String(o.argumentsTypes);
			if (thisArgs == null)
			{
				if (otherArgs != null) result = -1;
			}
			else
			{
				if (otherArgs == null) result = 1;
				else result = thisArgs.compareTo(otherArgs);
			}
		}
		return result;
	}

	public void check(final PrintStream out, boolean needsSample, boolean dontCheckSyntax, boolean dontCheckParameters, boolean dontCheckReturnStuff,
		DocumentationManager docManager)
	{
		if (this.isDeprecated())
		{
			if (deprecatedText == null) out.println("\t'" + this.mainName + "' is deprecated and has no @deprecated tag text.");
			return;
		}

		// List already recorded error.
		for (String err : errors)
			out.println("\t'" + this.mainName + "': " + err);

		if (getState() != STATE_DOCUMENTED)
		{
			out.println("\t'" + this.mainName + "' is not documented.");
			return;
		}

		// Check return type.
		if (!dontCheckReturnStuff)
		{
			if (getType() == TYPE_EVENT || getType() == TYPE_COMMAND)
			{
				if (returnedType != null) out.println("\t'" + this.mainName + "' should not have a return type because it is an event or a command.");
				if (returnDescription != null && returnDescription.trim().length() > 0)
					out.println("\t'" + this.mainName + "' should not have a return description because it is an event or a command.");
			}
			else if (getType() == TYPE_PROPERTY || getType() == TYPE_CONSTANT)
			{
				if (returnedType == null) out.println("\t'" + this.mainName + "' should have a return type because it is a property.");
				if (returnDescription != null && returnDescription.trim().length() > 0)
					out.println("\t'" + this.mainName + "' should not have a return description because it is a property.");
			}
			else
			{
				if (returnedType == null) out.println("\t'" + this.mainName + "' should have a return type.");
				if (!"void".equals(returnedType) && (returnDescription == null || returnDescription.trim().length() == 0))
					out.println("\t'" + this.mainName + "' should have a return description.");
			}
		}

		// Do some check about summary vs. description.
		for (int i = 0; i < descriptions.size(); i++)
		{
			String description = descriptions.get(i).getText();
			String summary = summaries.get(i).getText();
			if (description.equals(summary) && !summary.endsWith("."))
				out.println("\t'" + this.mainName + "' should have a summary (first sentence) which ends in a dot.");
		}

		// Check description.
		for (IDescriptionDocumentation descr : descriptions)
		{
			if (descr == null || (descr.getText() != null && descr.getText().trim().length() == 0))
			{
				out.println("\t'" + this.mainName + "' has empty description.");
			}
		}

		// Check parameters.
		if (!dontCheckParameters)
		{
			for (IParameterDocumentation pdoc : this.parameters.values())
			{
				if (pdoc.getDescription() == null || pdoc.getDescription().trim().length() == 0)
					out.println("\t'" + this.mainName + "'.'" + pdoc.getName() + "' parameter has an empty description.");
			}
		}

		// Check samples.
		for (ISampleDocumentation sample : samples)
		{
			if (sample != null && sample.getSampleCode() != null && sample.getSampleCode().trim().length() > 0)
			{
				if (!needsSample)
				{
					out.println("\t'" + this.mainName + "' should not have sample code.");
				}
				else if (!dontCheckSyntax)
				{
					CompilerEnvirons cenv = new CompilerEnvirons();
					Parser parser = new Parser(cenv, new ErrorReporter()
					{

						public void error(String message, String sourceName, int line, String lineSource, int lineOffset)
						{
							out.println("\tERROR: " + message + " ON LINE " + line + " AT OFFSET " + lineOffset + ": " + lineSource);
						}

						public EvaluatorException runtimeError(String message, String sourceName, int line, String lineSource, int lineOffset)
						{
							return new EvaluatorException(message);
						}

						public void warning(String message, String sourceName, int line, String lineSource, int lineOffset)
						{
							out.println("\tWARNING: " + message + " ON LINE " + line + " AT OFFSET " + lineOffset + ": " + lineSource);
						}

					});

					StringBuilder sb = new StringBuilder();
					sb.append("function dummy() {\n");
					sb.append(sample.getSampleCode());
					sb.append("\n}");
					try
					{
						parser.parse(new CharArrayReader(sb.toString().toCharArray()), "", 0);
					}
					catch (Exception ex)
					{
						out.println(this.mainName + " has syntax errors: " + ex.getMessage());
						out.println("The bad code is:");
						out.println("---[start of code]---");
						out.println(sb.toString());
						out.println("---[ end of code ]---");
					}
				}
			}
			else
			{
				if (needsSample)
				{
					out.println("\t'" + this.mainName + "' should have sample code.");
				}
			}
		}
	}

	public boolean isDocumented()
	{
		return getState() == STATE_DOCUMENTED;
	}

	public void beautifyTypes(IDocumentationManager docManager, ObjectDocumentation objDoc)
	{
		DocumentationLogger.getInstance().getOut().println("Beautifying types in function '" + getMainName() + "' // " + toString());
//		for (ParameterDocumentation pdoc : parameters.values())
//		{
//			String origType = pdoc.getType();
//			boolean annotated = docManager.getObjectByQualifiedName(DocumentationHelper.removeArraySuffix(origType)) != null;
//			String beautType = DocumentationHelper.mapReturnedType(origType, annotated, objDoc.getQualifiedName());
//			DocumentationLogger.getInstance().getOut().println("\t'" + origType + "' becomes '" + beautType + "'.");
//			pdoc.setType(beautType);
//		}
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("[fdoc:").append(getMainName());
		sb.append(",alt names:");
		for (String n : names)
			sb.append(n).append("//");
		sb.append(",args=");
		sb.append(argumentsArray2String(getArgumentsTypes()));
		sb.append(",type=");
		sb.append(getType());
		sb.append(",deprecated=");
		sb.append(isDeprecated());
		sb.append(",state=");
		sb.append(getState());
		sb.append(",redirectType=");
		sb.append(redirectType);
		sb.append(",redirect=");
		sb.append(redirect != null ? redirect.toString() : "N/A");
		sb.append("]");
		return sb.toString();
	}

	public Element toXML(boolean hideSample, boolean pretty)
	{
		Element functionElement = DocumentHelper.createElement(typesToTags.get(type));

		if (argumentsTypes != null)
		{
			Element argsTypes = DocumentHelper.createElement(TAG_ARGUMENTSTYPES);
			functionElement.add(argsTypes);
			for (Class< ? > cc : argumentsTypes)
			{
				Element argType = DocumentHelper.createElement(TAG_ARGUMENTTYPE);
				argsTypes.add(argType);
				argType.addAttribute(ATTR_TYPECODE, cc.getName());
			}
		}

		if (isDeprecated()) functionElement.addAttribute(ATTR_DEPRECATED, Boolean.TRUE.toString());
		if (getClientSupport() != null) functionElement.addAttribute(ATTR_CLIENT_SUPPORT, getClientSupport().toAttribute());
		functionElement.addAttribute(ATTR_NAME, getMainName());
		if (isVarargs()) functionElement.addAttribute(ATTR_VARARGS, Boolean.TRUE.toString());

		String retTypeMapped = DocumentationUtil.getJavaToJSTypeTranslator().translateJavaClassToJSDocumentedJavaClassName(returnedType);
		if ((retTypeMapped != null) || (returnDescription != null && returnDescription.trim().length() > 0))
		{
			Element retElement = functionElement.addElement(TAG_RETURN);
			if (retTypeMapped != null)
			{
				retElement.addAttribute(ATTR_TYPE, retTypeMapped);
				if (!pretty)
				{
					retElement.addAttribute(ATTR_TYPECODE, returnedType.getName());
				}
			}
			if (getState() == STATE_DOCUMENTED)
			{
				if (returnDescription != null && returnDescription.trim().length() > 0) retElement.addCDATA(returnDescription);
			}
		}

		if (getState() != STATE_DOCUMENTED)
		{
			functionElement.addAttribute(ATTR_UNDOCUMENTED, Boolean.TRUE.toString());
		}
		else
		{
			if (defaultValue != null && defaultValue.trim().length() > 0) functionElement.addAttribute(ATTR_DEFAULTVALUE, defaultValue);

			if (since != null && since.trim().length() > 0) functionElement.addAttribute(ATTR_SINCE, since);
			if (until != null && until.trim().length() > 0) functionElement.addAttribute(ATTR_UNTIL, until);

			if (isSpecial) functionElement.addAttribute(ATTR_SPECIAL, Boolean.TRUE.toString());

			if (simplifiedSignature) functionElement.addAttribute(ATTR_SIMPLIFIEDSIGNATURE, Boolean.TRUE.toString());

			if (staticCall) functionElement.addAttribute(ATTR_STATICCALL, Boolean.TRUE.toString());

			Element descriptionsEl = functionElement.addElement(TAG_DESCRIPTIONS);
			Element summariesEl = null;
			if (getSummaries().size() > 0) summariesEl = functionElement.addElement(TAG_SUMMARIES);
			for (int i = 0; i < getDescriptions().size(); i++)
			{
				IDescriptionDocumentation descr = getDescriptions().get(i);
				if (descr != null && descr.getText() != null && descr.getText().trim().length() > 0)
				{
					Element elDescr = descriptionsEl.addElement(TAG_DESCRIPTION);
					elDescr.addAttribute(ATTR_CLIENT_SUPPORT, descr.getClientSupport().toAttribute());
					elDescr.addCDATA(descr.getText());
				}

				IDescriptionDocumentation summary = getSummaries().get(i);
				if (summary != null && summary.getText() != null && summary.getText().trim().length() > 0)
				{
					Element summaryEl = summariesEl.addElement(TAG_SUMMARY);
					summaryEl.addAttribute(ATTR_CLIENT_SUPPORT, descr.getClientSupport().toAttribute());
					summaryEl.addCDATA(summary.getText());
				}
			}
			if (deprecatedText != null && deprecatedText.trim().length() > 0)
			{
				functionElement.addElement(TAG_DEPRECATED).addCDATA(deprecatedText);
			}
			if (!hideSample)
			{
				Element samplesElement = functionElement.addElement(TAG_SAMPLES);
				for (ISampleDocumentation smpl : getSamples())
				{
					Element sampleEl = samplesElement.addElement(TAG_SAMPLE);
					sampleEl.addAttribute(ATTR_CLIENT_SUPPORT, smpl.getClientSupport().toAttribute());
					sampleEl.addCDATA(smpl.getSampleCode());
				}
			}
		}

		if (parameters.size() > 0)
		{
			Element argumentsElement = functionElement.addElement(TAG_PARAMETERS);
			for (IParameterDocumentation argDoc : parameters.values())
			{
				if (argDoc instanceof ParameterDocumentation)
				{
					ParameterDocumentation pdArgDoc = (ParameterDocumentation)argDoc;
					argumentsElement.add(pdArgDoc.toXML(pretty));
				}
			}
		}

		if (links.size() > 0)
		{
			Element linksElement = functionElement.addElement(TAG_LINKS);
			for (String link : links.keySet())
			{
				String alias = links.get(link);
				Element linkElem = linksElement.addElement(TAG_LINK);
				linkElem.addElement(TAG_URL).addText(link);
				if (!NO_ALIAS.equals(alias)) linkElem.addElement(TAG_DESCRIPTION).addText(alias);
			}
		}

		return functionElement;
	}

	/**
	 * @return the specialSimplifiedSignature
	 */
	public boolean isSimplifiedSignature()
	{
		return simplifiedSignature;
	}

	/**
	 * @param specialSimplifiedSignature the specialSimplifiedSignature to set
	 */
	public void setSimplifiedSignature(boolean specialSimplifiedSignature)
	{
		this.simplifiedSignature = specialSimplifiedSignature;
	}

	public static FunctionDocumentation fromXML(Element functionElement, ClassLoader loader)
	{
		boolean validTag = false;
		Integer type = TYPE_UNKNOWN;
		for (Integer key : typesToTags.keySet())
		{
			String tag = typesToTags.get(key);
			if (tag.equals(functionElement.getName()))
			{
				validTag = true;
				type = key;
				break;
			}
		}
		if (!validTag) return null;

		String name = functionElement.attributeValue(ATTR_NAME);

		Class< ? >[] argTypesArr = null;
		Element argsTypes = functionElement.element(TAG_ARGUMENTSTYPES);
		if (argsTypes != null)
		{
			Iterator<Element> arguments = argsTypes.elementIterator();
			List<Class< ? >> ats = new ArrayList<Class< ? >>();
			while (arguments.hasNext())
			{
				Element argType = arguments.next();
				String ccs = argType.attributeValue(ATTR_TYPECODE);

				Class< ? > cc = null;
				try
				{
					cc = DocumentationUtil.loadClass(loader, ccs);
				}
				catch (Throwable e)
				{
					System.out.println("Cannot load type from '" + type + "' at argument type " + ccs + "  of function '" + name + "'.");
				}
				if (cc != null)
				{
					ats.add(cc);
				}
			}
			argTypesArr = new Class< ? >[ats.size()];
			ats.toArray(argTypesArr);
		}

		boolean deprecated = Boolean.TRUE.toString().equals(functionElement.attributeValue(ATTR_DEPRECATED));
		boolean varargs = Boolean.TRUE.toString().equals(functionElement.attributeValue(ATTR_VARARGS));

		boolean undocumented = Boolean.TRUE.toString().equals(functionElement.attributeValue(ATTR_UNDOCUMENTED));
		FunctionDocumentation fdoc = new FunctionDocumentation(DocumentationManager.getInternedText(name), argTypesArr, type, deprecated, varargs,
			undocumented ? IFunctionDocumentation.STATE_UNDOCUMENTED : IFunctionDocumentation.STATE_DOCUMENTED);

		Element retElement = functionElement.element(TAG_RETURN);
		if (retElement != null)
		{
			String returnedTypeCode = retElement.attributeValue(ATTR_TYPECODE);
			if (returnedTypeCode != null)
			{
				Class< ? > returnedType = null;
				try
				{
					returnedType = DocumentationUtil.loadClass(loader, returnedTypeCode);
				}
				catch (Throwable e)
				{
					System.out.println("Failed to decode class from '" + returnedTypeCode + "' at return type of function " + name + ".");
				}
				if (returnedType != null)
				{
					fdoc.setReturnedType(returnedType);
				}
			}
			if ((retElement.getText() != null) && retElement.getText().length() > 0)
				fdoc.setReturnDescription(DocumentationManager.getInternedText(retElement.getText()));
		}

		String since = functionElement.attributeValue(ATTR_SINCE);
		if (since != null) fdoc.setSince(DocumentationManager.getInternedText(since));

		String until = functionElement.attributeValue(ATTR_UNTIL);
		if (until != null) fdoc.setUntil(DocumentationManager.getInternedText(until));

		if (Boolean.TRUE.toString().equals(functionElement.attributeValue(ATTR_SPECIAL))) fdoc.setSpecial(true);

		if (Boolean.TRUE.toString().equals(functionElement.attributeValue(ATTR_SIMPLIFIEDSIGNATURE))) fdoc.setSimplifiedSignature(true);

		if (Boolean.TRUE.toString().equals(functionElement.attributeValue(ATTR_STATICCALL))) fdoc.setStaticCall(true);

		fdoc.setClientSupport(ClientSupport.fromString(functionElement.attributeValue(ATTR_CLIENT_SUPPORT)));

		Element descriptionsElement = functionElement.element(TAG_DESCRIPTIONS);
		if (descriptionsElement != null)
		{
			Iterator<Element> descriptionsIter = descriptionsElement.elementIterator();
			while (descriptionsIter.hasNext())
			{
				Element descrDoc = descriptionsIter.next();
				if (descrDoc != null)
				{
					fdoc.addDescription(new DescriptionDocumentation(ClientSupport.fromString(descrDoc.attributeValue(ATTR_CLIENT_SUPPORT)),
						DocumentationManager.getInternedText(descrDoc.getText())));
				}
			}
		}
		else
		{
			String description = functionElement.elementText(TAG_DESCRIPTION);
			if (description != null)
				fdoc.addDescription(new DescriptionDocumentation(ClientSupport.fromString(functionElement.attributeValue(ATTR_CLIENT_SUPPORT)),
					DocumentationManager.getInternedText(description)));
		}

		Element summariesElement = functionElement.element(TAG_SUMMARIES);
		if (summariesElement != null)
		{
			Iterator<Element> summariesIter = summariesElement.elementIterator();
			while (summariesIter.hasNext())
			{
				Element summaryDoc = summariesIter.next();
				if (summaryDoc != null)
				{
					fdoc.addSummary(new DescriptionDocumentation(ClientSupport.fromString(summaryDoc.attributeValue(ATTR_CLIENT_SUPPORT)),
						DocumentationManager.getInternedText(summaryDoc.getText())));
				}
			}
		}
		else
		{
			String summary = functionElement.elementText(TAG_SUMMARY);
			if (summary != null) fdoc.addSummary(new DescriptionDocumentation(ClientSupport.fromString(functionElement.attributeValue(ATTR_CLIENT_SUPPORT)),
				DocumentationManager.getInternedText(summary)));
		}

		Element samplesElement = functionElement.element(TAG_SAMPLES);
		if (samplesElement != null)
		{
			Iterator<Element> samplesIter = samplesElement.elementIterator();
			while (samplesIter.hasNext())
			{
				Element sampleDoc = samplesIter.next();
				if (sampleDoc != null)
				{
					fdoc.addSample(new SampleDocumentation(ClientSupport.fromString(sampleDoc.attributeValue(ATTR_CLIENT_SUPPORT)),
						DocumentationManager.getInternedText(sampleDoc.getText())));
				}
			}
		}
		else
		{
			String sample = functionElement.elementText(TAG_SAMPLE);
			if (sample != null) fdoc.addSample(new SampleDocumentation(ClientSupport.fromString(functionElement.attributeValue(ATTR_CLIENT_SUPPORT)),
				DocumentationManager.getInternedText(sample)));
		}

		String defaultValue = functionElement.attributeValue(ATTR_DEFAULTVALUE);
		if (defaultValue != null) fdoc.setDefaultValue(DocumentationManager.getInternedText(defaultValue));

		Element argumentsElement = functionElement.element(TAG_PARAMETERS);
		if (argumentsElement != null)
		{
			Iterator<Element> arguments = argumentsElement.elementIterator();
			while (arguments.hasNext())
			{
				ParameterDocumentation argDoc = ParameterDocumentation.fromXML(arguments.next(), loader);
				if (argDoc != null)
				{
					fdoc.addArgument(argDoc);
				}
			}
		}

		Element linksElement = functionElement.element(TAG_LINKS);
		if (linksElement != null)
		{
			Iterator<Element> linksIter = linksElement.elementIterator();
			while (linksIter.hasNext())
			{
				Element linkElement = linksIter.next();
				String theLink;
				String theAlias;
				Element linkElem = linkElement.element(TAG_URL);
				if (linkElem != null)
				{
					theLink = linkElem.getText();
					Element aliasElem = linkElement.element(TAG_DESCRIPTION);
					if (aliasElem != null) theAlias = aliasElem.getText();
					else theAlias = NO_ALIAS;
					fdoc.addLink(DocumentationManager.getInternedText(theLink), DocumentationManager.getInternedText(theAlias));
				}
			}
		}

		String deprecatedText = functionElement.elementText(TAG_DEPRECATED);
		if (deprecatedText != null) fdoc.setDeprecatedText(DocumentationManager.getInternedText(deprecatedText));

		return fdoc;
	}

	private static String argumentsArray2String(Class< ? >[] args)
	{
		if (args == null)
		{
			return null;
		}
		else
		{
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < args.length; i++)
			{
				if (i > 0) sb.append(",");
				sb.append(args[i].getCanonicalName());
			}
			return sb.toString();
		}
	}

	/**
	 * @return the staticCall
	 */
	public boolean isStaticCall()
	{
		return staticCall;
	}

	/**
	 * @param staticCall the staticCall to set
	 */
	public void setStaticCall(boolean staticCall)
	{
		this.staticCall = staticCall;
	}


	public ClientSupport getClientSupport()
	{
		return clientSupport;
	}

	public void setClientSupport(ClientSupport clientSupport)
	{
		this.clientSupport = clientSupport;
	}

	public void setSamples(List<ISampleDocumentation> samples)
	{
		this.samples.clear();
		this.samples.addAll(samples);
	}

	public List<ISampleDocumentation> getSamples()
	{
		// if we have a mc specific sample, then no other sample will be marked with mc support
//		if (getSample(ClientSupport.mc) != null)
//		{
//			for (ISampleDocumentation sample : samples)
//			{
//				if (sample.getClientSupport().supports(ClientSupport.mc) && !ClientSupport.mc.equals(sample.getClientSupport()))
//				{
//					sample.setClientSupport(ClientSupport.Default);
//				}
//			}
//		}
		return samples;
	}

	public void addSample(ISampleDocumentation smpl)
	{
		samples.add(smpl);
	}

	public String getSample(ClientSupport csp)
	{
		ClientSupport searchCSp = csp;
		if (searchCSp == null) searchCSp = ClientSupport.Default;
		for (ISampleDocumentation smpl : samples)
		{
			if (smpl == null || (smpl != null && smpl.getClientSupport() == null)) continue;
			if (smpl.getClientSupport().hasSupport(csp)) return smpl.getSampleCode();
		}
		return null;
	}

	public void addDescription(IDescriptionDocumentation description)
	{
		descriptions.add(description);
	}

	public String getDescription(ClientSupport csp)
	{
		ClientSupport searchCSp = csp;
		if (searchCSp == null) searchCSp = ClientSupport.Default;
		for (IDescriptionDocumentation descr : descriptions)
		{
			if (descr == null || (descr != null && descr.getClientSupport() == null)) continue;
			if (descr.getClientSupport().hasSupport(csp)) return descr.getText();
		}
		return null;
	}


	public String getSummary(ClientSupport csp)
	{
		ClientSupport searchCSp = csp;
		if (searchCSp == null) searchCSp = ClientSupport.Default;
		for (IDescriptionDocumentation descr : summaries)
		{
			if (descr == null || (descr != null && descr.getClientSupport() == null)) continue;
			if (descr.getClientSupport().hasSupport(csp)) return descr.getText();
		}
		return null;
	}

	public void addDescription(ClientSupport csp, String text)
	{
		descriptions.add(new DescriptionDocumentation(csp, text));
	}

	public void addSummary(IDescriptionDocumentation summary)
	{
		summaries.add(summary);
	}

	public SortedMap<String, String> getLinks()
	{
		return links;

	}
}
