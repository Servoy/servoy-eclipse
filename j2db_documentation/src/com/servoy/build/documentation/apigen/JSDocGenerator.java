/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2025 Servoy BV

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

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Updated JSDocGenerator.
 * Generates properties documentation from the "model" part of the spec,
 * and the handlers documentation from the "handlers" part of the spec.
 * The generated docs are merged into the original doc file.
 */
public class JSDocGenerator
{
	private final JSONObject spec;
	private final File doc;

	private final boolean ALLOW_EXECUTION = false; // Set to true to allow execution of the generator.

	//Note that processing steps are intended to run only once on a given spec and _doc.js file.
	//Enabling multiple processing steps may lead to broken doc files.
	//For any changes, enable tempTarget to true to avoid overriding the original, and check the output.
	//Only when you're ABSOLUTELY SURE that the output is correct, set tempTarget to false to overwrite the original.
	// Then run the generator again ONLY ONCE.

	private final boolean processProperties = false;
	private final boolean processHandlers = false;
	private final boolean processTypes = false;

	private final boolean changeTypesName = false;

	private final boolean processList = false;

	private final boolean tempTarget = false;

	private final List<String> list = Arrays.asList(
		"bootstrapcomponents-floatlabelcalendar",
		"bootstrapcomponents-floatlabelcombobox",
		"bootstrapcomponents-floatlabeltextarea",
		"bootstrapcomponents-floatlabeltextbox",
		"bootstrapcomponents-floatlabeltypeahead",
		"bootstrapcomponents-formcomponent",
		"bootstrapcomponents-progressbar",
		"bootstrapcomponents-table");

	public JSDocGenerator(JSONObject spec, File doc)
	{
		this.spec = spec;
		this.doc = doc;
	}

	public void runGenerator()
	{

		if (!ALLOW_EXECUTION)
		{
			return;
		}

		// Read original file content.
		String originalContent = readOriginalContent();
		String propertiesDocs = null;
		String handlersDocs = null;
		String typesDocs = null;

		// Generate documentation for properties and handlers.
		if (processProperties)
		{
			propertiesDocs = generatePropertiesDocs();
		}
		if (processHandlers)
		{
			handlersDocs = generateHandlersDocs();
		}
		if (processTypes)
		{
			typesDocs = generateTypesDocs();
		}

		// Combine the two sections.
		String combinedDocs = combineDocs(propertiesDocs, handlersDocs);


		// Merge combined docs with the original file content.
		String mergedContent = mergeDocs(originalContent, combinedDocs);

		// Append types docs at the very end.
		if (processTypes)
		{
			mergedContent = mergedContent + "\n" + typesDocs;
		}

		if (changeTypesName)
		{
			mergedContent = mergedContent.replaceFirst("var\\s+types\\s*=\\s*\\{", "var svy_types = {");
		}

		// Write the merged content to a new file.
		writeMergedContent(mergedContent);
	}

	private String readOriginalContent()
	{
		try
		{
			return FileUtils.readFileToString(doc, Charset.forName("UTF8"));
		}
		catch (IOException e)
		{
			throw new RuntimeException("Error reading doc file: " + doc.getAbsolutePath(), e);
		}
	}

	private String generateTypesDocs()
	{
		StringBuilder overall = new StringBuilder();
		JSONObject typesObj = spec.optJSONObject("types");
		if (typesObj != null && typesObj.keySet().size() > 0)
		{
			overall.append("var types = {\n\n");
			for (String typeName : typesObj.keySet())
			{
				JSONObject typeDef = typesObj.optJSONObject(typeName);
				if (typeDef == null) continue;

				// Check type-level tags; if scope is private, skip this type.
				JSONObject typeTags = typeDef.optJSONObject("tags");
				if (typeTags != null && "private".equals(typeTags.optString("scope", "")))
				{
					continue;
				}

				// Build the type block in a temporary builder.
				StringBuilder typeBlock = new StringBuilder();
				String header = "    " + typeName + ": {\n\n";
				typeBlock.append(header);

				// Process the properties: if there's a "model" property use that; otherwise iterate over keys.
				JSONObject model = typeDef.optJSONObject("model");
				if (model != null)
				{
					for (String propName : model.keySet())
					{
						Object propValue = model.opt(propName);
						processTypeProperty(typeBlock, propName, propValue);
					}
				}
				else
				{
					Iterator<String> keys = typeDef.keys();
					while (keys.hasNext())
					{
						String propName = keys.next();
						if ("tags".equals(propName)) continue;
						Object propValue = typeDef.opt(propName);
						processTypeProperty(typeBlock, propName, propValue);
					}
				}

				// If no properties were added (i.e. typeBlock equals header), skip this type.
				if (typeBlock.toString().equals(header))
				{
					continue;
				}
				else
				{
					typeBlock.append("    },\n\n");
					overall.append(typeBlock);
				}
			}
			// Remove the trailing comma and newline.
			int lastComma = overall.lastIndexOf(",\n\n");
			if (lastComma != -1)
			{
				overall.delete(lastComma, overall.length());
			}
			overall.append("\n}\n");
		}
		return overall.toString();
	}

	private void processTypeProperty(StringBuilder sb, String propName, Object propValue)
	{
		if (propValue instanceof JSONObject)
		{
			JSONObject propObj = (JSONObject)propValue;
			JSONObject tags = propObj.optJSONObject("tags");
			// Skip if the property is private.
			if (tags != null && "private".equals(tags.optString("scope", "")))
			{
				return;
			}
			String docContent = (tags != null) ? tags.optString("doc", "").trim() : "";
			if (!docContent.isEmpty())
			{
				sb.append("        /**\n");
				sb.append("         * ").append(docContent).append("\n");
				sb.append("         */\n");
			}
			sb.append("        ").append(propName).append(" : null,\n\n");
		}
		else if (propValue instanceof String)
		{
			sb.append("        ").append(propName).append(" : null,\n\n");
		}
		else
		{
			sb.append("        ").append(propName).append(" : null,\n\n");
		}
	}

	private String generatePropertiesDocs()
	{
		StringBuilder sb = new StringBuilder();
		JSONObject model = spec.optJSONObject("model");
		if (model != null)
		{
			Iterator<String> propertyNames = model.keys();
			while (propertyNames.hasNext())
			{
				String propName = propertyNames.next();
				Object propValue = model.opt(propName);
				if (propValue instanceof JSONObject)
				{
					JSONObject propObj = (JSONObject)propValue;
					JSONObject tags = propObj.optJSONObject("tags");
					// Skip property if "scope" equals "private"
					if (tags != null && "private".equals(tags.optString("scope", "")))
					{
						continue;
					}
					String docContent = (tags != null) ? tags.optString("doc", "") : "";
					if (!docContent.isEmpty())
					{
						sb.append("/**\n")
							.append(" * ").append(docContent).append("\n")
							.append(" */\n")
							.append("var ").append(propName).append(";\n\n");
					}
					else
					{
						sb.append("var ").append(propName).append(";\n\n");
					}
				}
				else
				{
					// For simple properties (not JSONObject), just output the variable declaration.
					sb.append("var ").append(propName).append(";\n\n");
				}
			}
		}
		return sb.toString();
	}

	private String generateHandlersDocs()
	{
		String indentation = "    ";
		StringBuilder sb = new StringBuilder();
		JSONObject handlersObj = spec.optJSONObject("handlers");
		if (handlersObj != null && handlersObj.keySet().size() > 0)
		{
			Iterator<String> handlerNames = handlersObj.keys();
			while (handlerNames.hasNext())
			{
				String handlerName = handlerNames.next();
				JSONObject handlerJSON = handlersObj.optJSONObject(handlerName);
				if (handlerJSON == null)
				{
					continue;
				}
				// Skip if handler is marked as private or has a "deprecated" tag.
				if (handlerJSON.optBoolean("private", false) || handlerJSON.has("deprecated"))
				{
					continue;
				}

				// Determine if there is any documentation content.
				String handlerDoc = handlerJSON.optString("doc");
				JSONArray params = handlerJSON.optJSONArray("parameters");
				boolean hasParams = (params != null && params.length() > 0);
				boolean hasReturns = handlerJSON.has("returns");
				boolean hasDocContent = (handlerDoc != null && !handlerDoc.isEmpty()) || hasParams || hasReturns;

				if (hasDocContent)
				{
					sb.append(indentation).append("/**\n");
					if (handlerDoc != null && !handlerDoc.isEmpty())
					{
						sb.append(indentation)
							.append(" * ").append(handlerDoc.replace("\n", "\n" + indentation + " * "))
							.append("\n");
						if (hasParams || hasReturns)
						{
							sb.append(indentation).append(" *\n");
						}
					}
					if (hasParams)
					{
						for (int i = 0; i < params.length(); i++)
						{
							JSONObject paramObj = params.optJSONObject(i);
							if (paramObj != null)
							{
								String paramName = paramObj.optString("name");
								String paramType = paramObj.optString("type");
								String normalizedType = normalizeType(spec.optJSONObject("types"), paramType, spec.optString("name"));
								boolean isOptional = paramObj.optBoolean("optional", false);
								String paramDoc = paramObj.optString("doc", "");

								sb.append(indentation).append(" * @param {").append(normalizedType).append("} ");
								if (isOptional)
								{
									sb.append("[").append(paramName).append("]");
								}
								else
								{
									sb.append(paramName);
								}
								if (paramDoc != null && !paramDoc.isEmpty())
								{
									sb.append(" ").append(paramDoc);
								}
								sb.append("\n");
							}
						}
					}
					if (hasParams && hasReturns)
					{
						sb.append(indentation).append(" *\n");
					}
					if (hasReturns)
					{
						String returnType = "";
						String returnDoc = "";
						Object returnsObj = handlerJSON.get("returns");
						if (returnsObj instanceof JSONObject)
						{
							JSONObject returnsJSON = (JSONObject)returnsObj;
							returnType = returnsJSON.optString("type", "");
							returnDoc = returnsJSON.optString("doc", "");
						}
						else if (returnsObj instanceof String)
						{
							returnType = (String)returnsObj;
						}
						if (!returnType.isEmpty())
						{
							String normalizedReturnType = normalizeType(spec.optJSONObject("types"), returnType, spec.optString("name"));
							sb.append(indentation).append(" * @return {").append(normalizedReturnType).append("}");
							if (returnDoc != null && !returnDoc.isEmpty())
							{
								sb.append(" ").append(returnDoc);
							}
							sb.append("\n");
						}
					}
					sb.append(indentation).append(" */\n");
				}
				// Append the handler function declaration (using colon syntax) once.
				sb.append(indentation).append(handlerName).append(": function() {}").append(",\n\n");
			}
			// Remove the trailing comma and newline.
			if (sb.length() >= 2)
			{
				int lastCommaIndex = sb.lastIndexOf(",\n");
				if (lastCommaIndex != -1)
				{
					sb.delete(lastCommaIndex, sb.length());
				}
			}
		}
		return sb.toString();
	}

	private String combineDocs(String propertiesDocs, String handlersDocs)
	{
		StringBuilder combined = new StringBuilder();
		String myHandlersDocs = handlersDocs;
		if (propertiesDocs != null && !propertiesDocs.isEmpty())
		{
			combined.append(propertiesDocs);
		}
		if (handlersDocs != null && !handlersDocs.isEmpty())
		{
			// Check if there is a handlers-level doc.
			JSONObject handlersObj = spec.optJSONObject("handlers");
			if (handlersObj != null)
			{
				String handlersLevelDoc = handlersObj.optString("doc");
				if (handlersLevelDoc != null && !handlersLevelDoc.isEmpty())
				{
					// Prepend the handlers-level documentation.
					myHandlersDocs = "/**\n * " + handlersLevelDoc.replace("\n", "\n * ") + "\n */\n" + handlersDocs;
				}
			}
			combined.append("\n")
				.append("var handlers = {\n")
				.append(myHandlersDocs)
				.append("\n};\n");
		}
		return combined.toString();
	}

	private void writeMergedContent(String mergedContent)
	{
		try
		{
			File targetFile;
			if (tempTarget)
			{
				String fileName = doc.getName();
				if (fileName.endsWith("_doc.js"))
				{
					fileName = fileName.substring(0, fileName.length() - "_doc.js".length()) + "_doc_temp.js";
				}
				else
				{
					fileName = fileName + ".tmp";
				}
				targetFile = new File(doc.getParent(), fileName);
			}
			else
			{
				targetFile = doc;
			}
			FileUtils.writeStringToFile(targetFile, mergedContent, Charset.forName("UTF8"));
			System.out.println("Merged doc saved to: " + targetFile.getAbsolutePath());
		}
		catch (IOException e)
		{
			throw new RuntimeException("Error writing merged doc file: " + doc.getAbsolutePath(), e);
		}
	}

	private String mergeDocs(String originalContent, String combinedDocs)
	{
		String trimmedContent = originalContent.trim();
		if (trimmedContent.startsWith("/*"))
		{
			int endCommentIndex = originalContent.indexOf("*/");
			if (endCommentIndex != -1)
			{
				String afterComment = originalContent.substring(endCommentIndex + 2);
				String[] lines = afterComment.split("\n");
				for (String line : lines)
				{
					String trimmedLine = line.trim();
					if (!trimmedLine.isEmpty())
					{
						if (trimmedLine.startsWith("function"))
						{
							return combinedDocs + "\n" + originalContent;
						}
						break;
					}
				}
				int insertionIndex = endCommentIndex + 2;
				String before = originalContent.substring(0, insertionIndex);
				String after = originalContent.substring(insertionIndex);
				return before + "\n" + combinedDocs + after;
			}
		}
		if (combinedDocs.isEmpty())
		{
			return originalContent;
		}
		return combinedDocs + "\n" + originalContent;
	}

	private String normalizeType(JSONObject myTypes, String type, String componentName)
	{
		boolean isArray = false;
		String normalizedSpecType = type;
		if (normalizedSpecType.endsWith("[]"))
		{
			isArray = true;
			normalizedSpecType = normalizedSpecType.substring(0, normalizedSpecType.length() - 2);
		}
		switch (normalizedSpecType.toLowerCase())
		{
			case "int" :
			case "integer" :
			case "float" :
			case "byte" :
			case "double" :
				normalizedSpecType = "Number";
				break;
			case "bool" :
				normalizedSpecType = "Boolean";
				break;
			case "record" :
			case "jsrecord" :
				normalizedSpecType = "JSRecord";
				break;
			case "foundset" :
			case "jsfoundset" :
				normalizedSpecType = "JSFoundset";
				break;
			case "dataset" :
			case "jsdataset" :
				normalizedSpecType = "JSDataset";
				break;
			case "event" :
			case "jsevent" :
				normalizedSpecType = "JSEvent";
				break;
			case "jsupload" :
				normalizedSpecType = "JSUpload";
				break;
			case "jsmenu" :
				normalizedSpecType = "JSMenu";
				break;
			case "object" :
			case "string..." :
			case "string" :
			case "boolean" :
			case "number" :
			case "json" :
			case "dimension" :
			case "point" :
			case "date" :
			case "form" :
			case "function" :
				normalizedSpecType = normalizedSpecType.toLowerCase().substring(0, 1).toUpperCase() + normalizedSpecType.toLowerCase().substring(1);
				break;
			default :
				if (componentName != null && myTypes != null)
				{
					normalizedSpecType = normalizeTypeForComponent(myTypes, normalizedSpecType, componentName);
				}
				else
					normalizedSpecType = type;
		}
		if (isArray)
		{
			normalizedSpecType = "Array<" + normalizedSpecType + ">";
		}
		return normalizedSpecType;
	}

	private String normalizeTypeForComponent(JSONObject myTypes, String type, String componentName)
	{
		if (myTypes == null || componentName == null) return null;
		JSONObject customType = myTypes.optJSONObject(type);
		if (customType != null)
		{
			return "CustomType<" + componentName + "." + type + ">";
		}
		return type;
	}
}