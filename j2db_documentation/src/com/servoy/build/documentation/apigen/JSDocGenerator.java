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
import java.util.Iterator;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Updated JSDocGenerator.
 * Generates properties documentation from the "model" part of the spec,
 * then merges that snippet into the original doc file.
 * When a package comment is detected, the docs are inserted after it.
 * If the first comment block is immediately followed by a function declaration,
 * it is considered a function comment and the docs are prepended.
 */
public class JSDocGenerator
{
	private final JSONObject spec;
	private final File doc;

	public JSDocGenerator(JSONObject spec, File doc)
	{
		this.spec = spec;
		this.doc = doc;
	}

	public void runGenerator()
	{
		String originalContent;
		String mergedContent = "";
		StringBuilder propertiesDocs = new StringBuilder();
		JSONObject model = spec.optJSONObject("model");
		String indentation = "    ";
		if (model != null)
		{
			Iterator<String> propertyNames = model.keys();
			while (propertyNames.hasNext())
			{
				String propName = propertyNames.next();
				JSONObject propObj = model.optJSONObject(propName);
				if (propObj != null)
				{
					JSONObject tags = propObj.optJSONObject("tags");
					if (tags != null)
					{
						String docContent = tags.optString("doc");
						if (docContent != null && !docContent.isEmpty())
						{
							propertiesDocs.append("/**\n")
								.append(" * ").append(docContent).append("\n")
								.append(" */\n")
								.append("var ").append(propName).append(";\n\n");
						}
						else
						{
							// When there's no doc text, add only the variable declaration.
							propertiesDocs.append("var ").append(propName).append(";\n\n");
						}
					}
				}
			}
		}

		JSONObject handlersObj = spec.optJSONObject("handlers");
		StringBuilder handlersDocs = new StringBuilder();

		if (handlersObj != null && handlersObj.keySet().size() > 0)
		{
			Iterator<String> handlerNames = handlersObj.keys();
			while (handlerNames.hasNext())
			{
				String handlerName = handlerNames.next();
				JSONObject handlerJSON = handlersObj.optJSONObject(handlerName);
				if (handlerJSON != null)
				{
					// Start handler documentation comment block with indentation.
					handlersDocs.append(indentation).append("/**\n");
					String handlerDoc = handlerJSON.optString("doc");
					if (handlerDoc != null && !handlerDoc.isEmpty())
					{
						// Add handler-level doc with proper indentation.
						handlersDocs.append(indentation).append(" * ").append(handlerDoc.replace("\n", "\n" + indentation + " * ")).append("\n")
							.append(indentation).append(" * \n");
					}
					// Process parameters, if any.
					JSONArray params = handlerJSON.optJSONArray("parameters");
					if (params != null)
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

								handlersDocs.append(indentation).append(" * @param {").append(normalizedType).append("} ");
								if (isOptional)
								{
									handlersDocs.append("[").append(paramName).append("]");
								}
								else
								{
									handlersDocs.append(paramName);
								}
								if (paramDoc != null && !paramDoc.isEmpty())
								{
									handlersDocs.append(" ").append(paramDoc);
								}
								handlersDocs.append("\n");
							}
						}
					}
					// Process return value, if any.
					if (handlerJSON.has("returns"))
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
							handlersDocs.append(indentation).append(" * @returns {").append(normalizedReturnType).append("}");
							if (returnDoc != null && !returnDoc.isEmpty())
							{
								handlersDocs.append(" ").append(returnDoc);
							}
							handlersDocs.append("\n");
						}
					}
					handlersDocs.append(indentation).append(" */\n");
					// Append the handler function placeholder with proper colon and indentation.
					handlersDocs.append(indentation).append(handlerName).append(": function() {}").append(",\n\n");
				}
			}
			// Remove the trailing comma and newline, if present.
			if (handlersDocs.length() >= 2)
			{
				int lastCommaIndex = handlersDocs.lastIndexOf(",\n");
				if (lastCommaIndex != -1)
				{
					handlersDocs.delete(lastCommaIndex, handlersDocs.length());
				}
			}
		}

		StringBuilder combinedDocs = new StringBuilder();
		if (propertiesDocs.length() > 0)
		{
			combinedDocs.append(propertiesDocs.toString());
		}

		if (handlersObj != null)
		{
			String handlersDoc = handlersObj.optString("doc");
			if (handlersDoc != null && !handlersDoc.isEmpty())
			{
				handlersDocs.append("/**\n")
					.append(" * ").append(handlersDoc.replace("\n", "\n * ")).append("\n")
					.append(" */\n");
			}
		}

		if (handlersDocs.length() > 0)
		{
			// Wrap the handlers docs with the variable declaration, using the handlersDoc if available
			combinedDocs.append("\n\n")
				.append("var handlers = {\n")
				.append(handlersDocs.toString())
				.append("\n};\n");
		}
		try
		{
			originalContent = FileUtils.readFileToString(doc, Charset.forName("UTF8"));
			mergedContent = insertPropertiesDocs(originalContent, combinedDocs.toString());
		}
		catch (IOException e)
		{
			throw new RuntimeException("Error reading doc file: " + doc.getAbsolutePath(), e);
		}
		if (handlersDocs.length() == 0 && propertiesDocs.length() == 0)
		{
			System.out.println("No properties or handlers found in spec; no changes made to doc file.");
			return;
		}
		try
		{
			FileUtils.writeStringToFile(doc, mergedContent, Charset.forName("UTF8"));
			System.out.println("Merged doc saved to: " + doc.getAbsolutePath());
		}
		catch (IOException e)
		{
			throw new RuntimeException("Error writing merged doc file: " + doc.getAbsolutePath(), e);
		}
	}

	/**
	 * Inserts the generated properties documentation snippet into the original file content.
	 * If the file starts with a block comment, we check the first non-empty line after it.
	 * If that line starts with "function", the block comment is considered a function comment,
	 * and we insert the properties docs at the beginning.
	 * Otherwise, we insert them immediately after the block comment.
	 *
	 * @param originalContent the content of the original _doc.js file
	 * @param propertiesDocs  the generated properties documentation snippet
	 * @return the merged content
	 */
	private String insertPropertiesDocs(String originalContent, String propertiesDocs)
	{
		String trimmedContent = originalContent.trim();
		if (trimmedContent.startsWith("/*"))
		{
			int endCommentIndex = originalContent.indexOf("*/");
			if (endCommentIndex != -1)
			{
				String afterComment = originalContent.substring(endCommentIndex + 2);
				// Split the text after the comment into lines and check the first non-empty one.
				String[] lines = afterComment.split("\n");
				for (String line : lines)
				{
					String trimmedLine = line.trim();
					if (!trimmedLine.isEmpty())
					{
						// If the first non-empty line starts with "function", consider this a function comment.
						if (trimmedLine.startsWith("function"))
						{
							// Insert the properties docs at the beginning, preserving the function declaration's order.
							return propertiesDocs + "\n" + originalContent;
						}
						break;
					}
				}
				// Otherwise, assume it's a package comment and insert after it.
				int insertionIndex = endCommentIndex + 2;
				String before = originalContent.substring(0, insertionIndex);
				String after = originalContent.substring(insertionIndex);
				return before + "\n\n" + propertiesDocs + after;
			}
		}
		// No package comment detected; insert at the beginning.
		return propertiesDocs + "\n" + originalContent;
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
			case "double" :
				normalizedSpecType = "Number";
				break;
			case "boolean" :
			case "bool" :
				normalizedSpecType = "Boolean";
				break;
			case "record" :
				normalizedSpecType = "JSRecord";
				break;
			case "foundset" :
				normalizedSpecType = "JSFoundset";
				break;
			case "dataset" :
				normalizedSpecType = "JSDataset";
				break;
			case "event" :
			case "jsevent" :
				normalizedSpecType = "JSEvent";
				break;
			case "tagstring" :
				normalizedSpecType = "String";
				break;
			case "string..." :
				normalizedSpecType = "String...";
				break;
			default :
				if (componentName != null && myTypes != null)
				{
					normalizedSpecType = normalizeTypeForComponent(myTypes, normalizedSpecType, componentName);
				}
				else if (type.length() > 1)
					normalizedSpecType = normalizedSpecType.substring(0, 1).toUpperCase() + normalizedSpecType.substring(1);
				else
					normalizedSpecType = type;
		}


		if (!normalizedSpecType.startsWith("CustomType"))
		{
			normalizedSpecType = normalizedSpecType.substring(0, 1).toUpperCase() + normalizedSpecType.substring(1);
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