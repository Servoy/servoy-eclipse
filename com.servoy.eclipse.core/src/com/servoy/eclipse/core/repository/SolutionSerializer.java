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
package com.servoy.eclipse.core.repository;


import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;

import com.servoy.eclipse.core.IFileAccess;
import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyUpdatingProject;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.AbstractRepository;
import com.servoy.j2db.persistence.AbstractScriptProvider;
import com.servoy.j2db.persistence.ArgumentType;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ContentSpec;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IDeveloperRepository;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistVisitor;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.IVariable;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.NameComparator;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RelationItem;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.persistence.TabPanel;
import com.servoy.j2db.persistence.TableNode;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.ServoyJSONArray;
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.Utils;

/**
 * Writes repository (solution) objects from file system directories.
 * 
 * @author jblok
 */
public class SolutionSerializer
{
	public static final String FORMS_DIR = "forms"; //$NON-NLS-1$
	public static final String VALUELISTS_DIR = "valuelists"; //$NON-NLS-1$
	public static final String RELATIONS_DIR = "relations"; //$NON-NLS-1$

	public static final String DATASOURCES_DIR_NAME = "datasources"; //$NON-NLS-1$
	public static final int JSON_FILE_EXTENSION_SIZE = 4;
	public static final String JSON_DEFAULT_FILE_EXTENSION = ".obj"; //$NON-NLS-1$
	public static final String FORM_FILE_EXTENSION = ".frm"; //$NON-NLS-1$
	public static final String RELATION_FILE_EXTENSION = ".rel"; //$NON-NLS-1$
	public static final String VALUELIST_FILE_EXTENSION = ".val"; //$NON-NLS-1$
	public static final String TABLENODE_FILE_EXTENSION = ".tbl"; //$NON-NLS-1$
	public static final String JS_FILE_EXTENSION_WITHOUT_DOT = "js"; //$NON-NLS-1$
	public static final String JS_FILE_EXTENSION = '.' + JS_FILE_EXTENSION_WITHOUT_DOT;
	public static final String STYLE_FILE_EXTENSION = ".css"; //$NON-NLS-1$
	public static final String TEMPLATE_FILE_EXTENSION = ".template"; //$NON-NLS-1$
	public static final String CALCULATIONS_POSTFIX = "_calculations"; //$NON-NLS-1$
	public static final String CALCULATIONS_POSTFIX_WITH_EXT = CALCULATIONS_POSTFIX + JS_FILE_EXTENSION;
	public static final String GLOBALS_FILE = "globals" + JS_FILE_EXTENSION; //$NON-NLS-1$

	public static boolean isJSONFile(String fileName)
	{
		return fileName.endsWith(JSON_DEFAULT_FILE_EXTENSION) || fileName.endsWith(FORM_FILE_EXTENSION) || fileName.endsWith(RELATION_FILE_EXTENSION) ||
			fileName.endsWith(VALUELIST_FILE_EXTENSION) || fileName.endsWith(TABLENODE_FILE_EXTENSION);
	}

	private static Comparator<Object> PERSIST_COMPARATOR = new Comparator<Object>()
	{
		public int compare(Object o1, Object o2)
		{
			if (o1 instanceof AbstractBase && o2 instanceof AbstractBase) return VARS_METHODS.compare((AbstractBase)o1, (AbstractBase)o2);
			else return NameComparator.INSTANCE.compare(o1, o2);
		}
	};


	private static Comparator<AbstractBase> VARS_METHODS = new Comparator<AbstractBase>()
	{
		public int compare(AbstractBase o1, AbstractBase o2)
		{
			Integer line1 = getLineNumber(o1);
			Integer line2 = getLineNumber(o2);
			if (line1 == null && line2 == null)
			{
				if (o1 instanceof IVariable && o2 instanceof IVariable)
				{
					return ((IVariable)o1).getName().compareToIgnoreCase(((IVariable)o2).getName());
				}
				if (o1 instanceof IScriptProvider && o2 instanceof IScriptProvider)
				{
					return ((IScriptProvider)o1).getDisplayName().compareToIgnoreCase(((IScriptProvider)o2).getDisplayName());
				}
				if (o1 instanceof IVariable) return -1;
				return 1;
			}
			if (line1 == null)
			{
				return -1;
			}
			if (line2 == null)
			{
				return 1;
			}
			return line1.compareTo(line2);
		}

		private Integer getLineNumber(AbstractBase ab)
		{
			if (ab instanceof IScriptProvider)
			{
				IScriptProvider sp = (IScriptProvider)ab;
				if (sp.getLineNumberOffset() == 0) return null;
				return Integer.valueOf(sp.getLineNumberOffset());
			}
			return ab.getSerializableRuntimeProperty(IScriptProvider.LINENUMBER);
		}
	};

	public static final String SOLUTION_SETTINGS = "solution_settings" + JSON_DEFAULT_FILE_EXTENSION; //$NON-NLS-1$
	public static final String ROOT_METADATA = "rootmetadata" + JSON_DEFAULT_FILE_EXTENSION; //$NON-NLS-1$
	public static final String MEDIAS_DIR = "medias"; //$NON-NLS-1$
	public static final String FUNCTION_KEYWORD = "function"; //$NON-NLS-1$
	public static final String VAR_KEYWORD = "var"; //$NON-NLS-1$
	public static final String SV_COMMENT = "/**"; //$NON-NLS-1$
	public static final String PROP_ITEMS = "items"; //$NON-NLS-1$
	public static final String PROP_NAME = "name"; //$NON-NLS-1$
	public static final String PROP_TYPEID = "typeid"; //$NON-NLS-1$
	public static final String PROP_MIME_TYPE = "mimeType"; //$NON-NLS-1$
	public static final String PROP_UUID = "uuid"; //$NON-NLS-1$
	public static final String PROP_FILE_VERSION = "fileVersion"; //$NON-NLS-1$

	public static final String PROPERTIESKEY = "@properties="; //$NON-NLS-1$
	public static final String TYPEKEY = "@type"; //$NON-NLS-1$

	public static String generateScriptFile(final ISupportChilds parent, final IDeveloperRepository repository)
	{
		final Map<Pair<String, String>, Map<IPersist, Object>> projectContents = new HashMap<Pair<String, String>, Map<IPersist, Object>>();//filepathname -> map(persist -> contents)
		parent.acceptVisitor(new IPersistVisitor()
		{
			public Object visit(IPersist p)
			{
				if (p == parent) return CONTINUE_TRAVERSAL;

				if (p instanceof IVariable || p instanceof IScriptProvider)
				{
					Pair<String, String> filepathname = getFilePath(p, false);
					Map<IPersist, Object> fileContents = projectContents.get(filepathname);
					if (fileContents == null)
					{
						fileContents = new HashMap<IPersist, Object>();
						projectContents.put(filepathname, fileContents);
					}
					Object val = serializePersist(p, true, repository);
					if (val != null) fileContents.put(p, val);
				}
				return CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
			}
		});

		//write all object files
		Iterator<Entry<Pair<String, String>, Map<IPersist, Object>>> it = projectContents.entrySet().iterator();
		while (it.hasNext())
		{
			Entry<Pair<String, String>, Map<IPersist, Object>> elem = it.next();
			Map<IPersist, Object> fileContents = elem.getValue();
			AbstractBase[] persistArray = fileContents.keySet().toArray(new AbstractBase[fileContents.size()]);
			Arrays.sort(persistArray, VARS_METHODS);

			if (persistArray.length > 0)
			{
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < persistArray.length; i++)
				{
					Object content = fileContents.get(persistArray[i]);
					if (content instanceof CharSequence)
					{
						String val = ((CharSequence)content).toString();
						sb.append(val);
						if (i < persistArray.length - 1) sb.append('\n');
					}
				}
				return sb.toString();
			}
		}

		return ""; //$NON-NLS-1$
	}

	public static void writePersist(IPersist node, IFileAccess fileAccess, final IDeveloperRepository repository, boolean overwriteExisting,
		final boolean writeChangedPersistOnly, final boolean recursive) throws RepositoryException
	{
		try
		{
			final List<Pair<String, String>> changedFiles = new ArrayList<Pair<String, String>>();
			// First find all the changed files if that is needed (or should just all be saved)
			if (writeChangedPersistOnly)
			{
				node.acceptVisitor(new IPersistVisitor()
				{
					public Object visit(IPersist p)
					{
						if (p.isChanged())
						{
							changedFiles.add(getFilePath(p, false));
						}
						return recursive ? CONTINUE_TRAVERSAL : CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
					}
				});
			}

			final Map<Pair<String, String>, Map<IPersist, Object>> projectContents = new HashMap<Pair<String, String>, Map<IPersist, Object>>();//filepathname -> map(persist -> contents)
			IPersist compositeWithItems = node;
			while (compositeWithItems != null && SolutionSerializer.isCompositeItem(compositeWithItems))
			{
				compositeWithItems = compositeWithItems.getParent();
			}
			if (compositeWithItems == null) compositeWithItems = node;
			compositeWithItems.acceptVisitor(new IPersistVisitor()
			{
				public Object visit(IPersist p)
				{
					if (SolutionSerializer.isCompositeItem(p)) return CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;

					Pair<String, String> filepathname = getFilePath(p, false);
					if (!writeChangedPersistOnly || changedFiles.contains(filepathname))
					{
						Map<IPersist, Object> fileContents = projectContents.get(filepathname);
						if (fileContents == null)
						{
							fileContents = new HashMap<IPersist, Object>();
							projectContents.put(filepathname, fileContents);
						}
						if (fileContents.size() == 0 || filepathname.getRight().endsWith(JS_FILE_EXTENSION))
						{
							// only js-files have multiple top-level persists in 1 file.
							Object val = serializePersist(p, !(p instanceof Solution), repository);
							if (val != null) fileContents.put(p, val);
						}
						else
						{
							ServoyLog.logWarning("multiple objects found for the same file: " + p + " and " + fileContents + " -- ignoring input", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						}
					}
					return recursive && (!isCompositeWithItems(p) || p.getTypeID() == IRepository.FORMS || p.getTypeID() == IRepository.TABLENODES)
						? CONTINUE_TRAVERSAL : CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
				}
			});

			//write all object files
			Iterator<Map.Entry<Pair<String, String>, Map<IPersist, Object>>> it = projectContents.entrySet().iterator();
			while (it.hasNext())
			{
				Map.Entry<Pair<String, String>, Map<IPersist, Object>> elem = it.next();
				Pair<String, String> filepathname = elem.getKey();

				String fname = filepathname.getRight();
				String fileRelativePath = filepathname.getLeft() + fname;
				if (fileAccess.exists(fileRelativePath) && !overwriteExisting) continue;

				Map<IPersist, Object> fileContents = elem.getValue();
				Object[] persistArray = fileContents.keySet().toArray();
				Arrays.sort(persistArray, PERSIST_COMPARATOR);

				if (persistArray.length > 0)
				{
					OutputStream fos = fileAccess.getOutputStream(fileRelativePath);
					for (int i = 0; i < persistArray.length; i++)
					{
						Object content = fileContents.get(persistArray[i]);
						if (content instanceof byte[])
						{
							fos.write((byte[])content);
						}
						else if (content instanceof CharSequence)
						{
							fos.write(content.toString().getBytes("UTF8")); //$NON-NLS-1$
							if (i < persistArray.length - 1) fos.write('\n');
						}
					}
					fileAccess.closeOutputStream(fos);
				}
				if (fileAccess.getFileLength(fileRelativePath) == 0 && overwriteExisting) fileAccess.delete(fileRelativePath);
			}

			if (node instanceof Solution)
			{
				Solution solution = (Solution)node;

				//write root meta data
				String frmd = solution.getName() + '/' + ROOT_METADATA;
				if (!fileAccess.exists(frmd) || (solution.getSolutionMetaData().isChanged() && overwriteExisting))
				{
					String meta = serializeRootMetaData(solution);
					if (meta != null)
					{
						fileAccess.setUTF8Contents(frmd, meta);
						solution.getSolutionMetaData().clearChanged();
					}
				}

				//test if the globals js is created, else create one this is needed because debug needs to have 1 javascript file to be able to start.
				String globals = getScriptPath(solution, false);
				if (!fileAccess.exists(globals))
				{
					fileAccess.setUTF8Contents(globals, ""); //$NON-NLS-1$
				}

				//create the "solution_settings.obj" file
				Pair<String, String> solutionPath = getFilePath(node, false);
				String solutionFile = solutionPath.getLeft() + solutionPath.getRight();
				if (!fileAccess.exists(solutionFile))
				{
					Object val = serializePersist(node, false, repository);
					if (val != null) fileAccess.setUTF8Contents(solutionFile, val.toString());
				}
			}

			if (node instanceof Media || recursive && node instanceof Solution)
			{
				//custom list in one file for all medias
				String fmedias = node.getRootObject().getName() + '/' + MEDIAS_DIR + JSON_DEFAULT_FILE_EXTENSION;
				boolean existsFMedias = fileAccess.exists(fmedias);
				if (!existsFMedias || overwriteExisting)
				{
					String mediaInfo = serializeMediaInfo((Solution)node.getRootObject());
					if (mediaInfo == null)
					{
						// no medias left, remove fmedias
						if (existsFMedias)
						{
							try
							{
								fileAccess.delete(fmedias);
							}
							catch (IOException e)
							{
								ServoyLog.logError("Could not delete file " + fmedias, e); //$NON-NLS-1$
							}
						}
					}
					else
					{
						fileAccess.setUTF8Contents(fmedias, mediaInfo);
					}
				}

			}
		}
		catch (IOException e)
		{
			throw new RepositoryException("Could not write object " + node + " to workspace directory " + fileAccess.toOSPath(), e); //$NON-NLS-1$ //$NON-NLS-2$
		}
		catch (JSONException e)
		{
			throw new RepositoryException("Could not write object " + node + " to workspace directory " + fileAccess.toOSPath(), e); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private static String serializeRootMetaData(Solution s) throws JSONException
	{
		SolutionMetaData smd = s.getSolutionMetaData();
		ServoyJSONObject obj = new ServoyJSONObject();
		obj.put(PROP_UUID, smd.getRootObjectUuid().toString());
		obj.put(PROP_TYPEID, new Integer(smd.getObjectTypeId()));//just to be sure
		obj.put(PROP_NAME, smd.getName());
		obj.put(PROP_FILE_VERSION, AbstractRepository.repository_version);
		obj.put("solutionType", new Integer(smd.getSolutionType())); //$NON-NLS-1$
		obj.put("protectionPassword", smd.getProtectionPassword()); //$NON-NLS-1$
		obj.put("mustAuthenticate", new Boolean(smd.getMustAuthenticate())); //$NON-NLS-1$
		return obj.toString(true);
	}

	private static String serializeMediaInfo(Solution s) throws JSONException
	{
		Iterator<Media> it2 = s.getMedias(true);
		if (it2.hasNext())
		{
			JSONArray items = new ServoyJSONArray();
			while (it2.hasNext())
			{
				Media media = it2.next();
				ServoyJSONObject obj = new ServoyJSONObject();
				obj.put(PROP_UUID, media.getUUID().toString());
				obj.put(PROP_TYPEID, new Integer(media.getTypeID()));//just to be sure
				obj.put(PROP_NAME, media.getName());
				obj.put(PROP_MIME_TYPE, media.getMimeType());
				items.put(obj);
			}
			return items.toString();
		}
		return null;
	}

	public static boolean isCompositeWithItems(IPersist p)
	{
		if (p != null)
		{
			Solution parentSolution = (Solution)p.getAncestor(IRepository.SOLUTIONS);
			if (parentSolution != null && parentSolution.getSolutionMetaData().getFileVersion() < ServoyUpdatingProject.UPDATE_38)
			{
				// backward compatibility mode
				return p instanceof Relation;
			}
		}

		return p instanceof Relation || p instanceof TableNode || p instanceof Form || p instanceof TabPanel || p instanceof Portal; // can only return true for objects containing SolutionSerializer.PROP_ITEMS
	}

	public static boolean isCompositeItem(IPersist p)
	{
		return !(p instanceof Media || p instanceof IRootObject || p instanceof Relation || p instanceof ValueList || p instanceof Form ||
			p instanceof TableNode || p instanceof IScriptProvider || p instanceof IVariable);
	}

	//returns CharSequence,byte[] or JSONObject(=internal for relation items, work on parent!)
	public static Object serializePersist(IPersist persist, boolean forceRecursive, final IDeveloperRepository repository)
	{
		if (persist instanceof Media)
		{
			return ((Media)persist).getMediaData();
		}
		else
		{
			if (isCompositeWithItems(persist.getParent()) && !(persist instanceof IScriptProvider || persist instanceof IVariable)) // safety, should never get here anyway
			{
				return null;
			}
			ServoyJSONObject obj;
			try
			{
				obj = generateJSONObject(persist, forceRecursive, repository);
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError("Error generation json for: " + persist, e); //$NON-NLS-1$
				return null;
			}

			StringBuilder sb = new StringBuilder();
			if (persist instanceof IScriptProvider || persist instanceof IVariable)
			{
				obj.setNewLines(false);
				obj.remove("methodCode"); //$NON-NLS-1$
				obj.remove("declaration"); //$NON-NLS-1$
				obj.remove(PROP_NAME);
				obj.remove("defaultValue"); //$NON-NLS-1$
				obj.remove(SolutionDeserializer.COMMENT_JSON_ATTRIBUTE);
				obj.remove(SolutionDeserializer.LINE_NUMBER_OFFSET_JSON_ATTRIBUTE);

				if (persist instanceof AbstractScriptProvider)
				{
					AbstractScriptProvider abstractScriptProvider = (AbstractScriptProvider)persist;
					String source = abstractScriptProvider.getDeclaration();
//					source = Utils.stringReplaceRecursive(source, "/**/", "");//make sure empty comment is not in the source, which will interfere with next line
//					source = Utils.stringReplaceRecursive(source, SV_COMMENT, "/*");//make sure "/**" is not found in the source ever
					if (source != null && source.trim().length() > 0)
					{
						sb.append(source);
						replacePropertiesTag(obj, sb);
					}
					else
					{
						generateDefaultJSDoc(obj, sb);
						sb.append("function "); //$NON-NLS-1$
						sb.append(abstractScriptProvider.getName());
						sb.append("()\n{\n}\n"); //$NON-NLS-1$
					}
				}
				else if (persist instanceof ScriptVariable)
				{
					ScriptVariable sv = (ScriptVariable)persist;
					if (sv.getComment() != null)
					{
						sb.append(sv.getComment());
						sb.append('\n');
						replacePropertiesTag(obj, sb);
					}
					else
					{
						generateDefaultJSDoc(obj, sb);
					}
					int type = Column.mapToDefaultType(sv.getVariableType());
					// Add the "@type" tag.
					String jsType = sv.getSerializableRuntimeProperty(IScriptProvider.TYPE);
					String typeStr = ArgumentType.convertFromColumnType(type, jsType).getName();
					int index = sb.lastIndexOf(TYPEKEY);
					if (index != -1)
					{
						int lineEnd = sb.indexOf("\n", index); //$NON-NLS-1$
						sb.replace(index + TYPEKEY.length() + 1, lineEnd, typeStr);
					}
					else
					{
						int startComment = sb.indexOf(SV_COMMENT);
						int lineEnd = sb.indexOf("\n", startComment); //$NON-NLS-1$
						sb.insert(lineEnd, "\n * " + TYPEKEY + " " + typeStr + "\n *"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					}
					sb.append(VAR_KEYWORD);
					sb.append(' ');
					sb.append(sv.getName());
					if (jsType != null)
					{
						sb.append(':');
						sb.append(jsType);
					}
					String val = sv.getDefaultValue();
					if (type == IColumnTypes.TEXT)
					{
						if (val == null)
						{
							val = "''"; // to keep same behavior //$NON-NLS-1$
						}
					}
					else if (type == IColumnTypes.NUMBER)
					{
						if (val != null)
						{
							val = val.replace(",", "."); // you cannot have comma as decimal separator inside JS code  //$NON-NLS-1$ //$NON-NLS-2$
						}
					}
					if (val != null)
					{
						sb.append(" = "); //$NON-NLS-1$
						if ("now".equals(val)) val = "new Date()";//to keep same behavior //$NON-NLS-1$ //$NON-NLS-2$
						sb.append(val);
					}
					sb.append(";\n"); //$NON-NLS-1$
				}
			}
			else
			{
				sb.append(obj.toString(true));
			}
			return sb;
		}
	}

	/**
	 * @param obj
	 * @param sb
	 */
	@SuppressWarnings("nls")
	private static void replacePropertiesTag(ServoyJSONObject obj, StringBuilder sb)
	{
		if (sb.toString().trim().startsWith(SV_COMMENT))
		{
			int endComment = sb.indexOf("*/");
			if (endComment == -1) return;
			// just always replace the properties to be sure those are accurate. 
			int index = sb.lastIndexOf(PROPERTIESKEY, endComment);
			if (index != -1)
			{
				int lineEnd = sb.indexOf("\n", index); //$NON-NLS-1$
				sb.replace(index + PROPERTIESKEY.length(), lineEnd, obj.toString(false));
			}
			else if (obj.length() > 0)
			{
				sb.insert(endComment, "*\n * " + PROPERTIESKEY + obj.toString(false) + "\n "); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		else
		{
			StringBuilder doc = new StringBuilder();
			generateDefaultJSDoc(obj, doc);
			sb.insert(0, doc);
		}
	}

	/**
	 * @param obj
	 * @param sb
	 */
	private static void generateDefaultJSDoc(ServoyJSONObject obj, StringBuilder sb)
	{
		sb.append(SV_COMMENT);
		sb.append("\n *"); //$NON-NLS-1$
		if (obj.length() > 0)
		{
			sb.append(' ');
			sb.append(PROPERTIESKEY);
			sb.append(obj.toString(false));
		}
		sb.append("\n */\n"); //$NON-NLS-1$
	}

	/**
	 * @param persist
	 * @param repository
	 * @return
	 * @throws RepositoryException
	 */
	public static ServoyJSONObject generateJSONObject(IPersist persist, boolean forceRecursive, IDeveloperRepository repository) throws RepositoryException
	{
		Map<String, Object> property_values = repository.getPersistAsValueMap(persist);

		ContentSpec cs = repository.getContentSpec();
		Iterator<ContentSpec.Element> iterator = cs.getPropertiesForObjectType(persist.getTypeID());
		while (iterator.hasNext())
		{
			ContentSpec.Element element = iterator.next();

			if (element.isMetaData() || element.isDeprecated()) continue;

			String propertyName = element.getName();
			Object propertyObjectValue = property_values.get(propertyName);
			boolean isBoolean = (propertyObjectValue instanceof Boolean);
			boolean isNumber = (propertyObjectValue instanceof Number && element.getTypeID() != IRepository.ELEMENTS);//element_id for elements type becomes uuid

			String propertyValue = repository.convertObjectToArgumentString(element.getTypeID(), propertyObjectValue);//, persist.getID(), persist.getRevisionNumber(), element.contentID, resolver);

			if (persist instanceof AbstractBase && element.getTypeID() == IRepository.ELEMENTS &&
				((Integer)propertyObjectValue).intValue() == IRepository.UNRESOLVED_ELEMENT)
			{
				HashMap<String, String> unresolvedMap = ((AbstractBase)persist).getSerializableRuntimeProperty(AbstractBase.UnresolvedPropertyToValueMapProperty);
				if (unresolvedMap != null && unresolvedMap.containsKey(propertyName))
				{
					propertyValue = unresolvedMap.get(propertyName);
				}
			}

			if (!cs.mustUpdateValue(element.getContentID(), propertyValue))
			{
				property_values.remove(propertyName);
			}
			else if (!isBoolean && !isNumber)
			{
				property_values.put(propertyName, propertyValue);//replace with textual version
			}
		}

		property_values.put(PROP_UUID, persist.getUUID().toString());
		property_values.put(PROP_TYPEID, new Integer(persist.getTypeID()));//just to be sure

		if (persist instanceof ISupportChilds && (forceRecursive || isCompositeWithItems(persist)))
		{
			ArrayList<ServoyJSONObject> itemsArrayList = new ArrayList<ServoyJSONObject>();
			Iterator<IPersist> it = ((ISupportChilds)persist).getAllObjects();
			IPersist child;
			while (it.hasNext())
			{
				child = it.next();
				if (!(child instanceof IScriptProvider || child instanceof IVariable)) itemsArrayList.add(generateJSONObject(child, forceRecursive, repository));
			}
			if (itemsArrayList.size() > 0)
			{
				ServoyJSONObject[] itemsArray = itemsArrayList.toArray(new ServoyJSONObject[itemsArrayList.size()]);
				Arrays.sort(itemsArray, new Comparator<ServoyJSONObject>()
				{
					public int compare(ServoyJSONObject o1, ServoyJSONObject o2)
					{
						try
						{
							return ((String)o1.get(PROP_UUID)).compareTo((String)o2.get(PROP_UUID));
						}
						catch (JSONException ex)
						{
							ServoyLog.logWarning("Cannot compare json objects based on uuid", ex); //$NON-NLS-1$
							return 0;
						}
					}

				});
				JSONArray items = new ServoyJSONArray();
				for (ServoyJSONObject o : itemsArray)
					items.put(o);
				property_values.put(PROP_ITEMS, items);
			}
		}
		else if (persist instanceof RelationItem) // Relation items have always been written with newlines=false, for other composites (like forms) use newlines
		{
			return new ServoyJSONObject(property_values, true, false);
		}

		return new ServoyJSONObject(property_values);
	}

	/**
	 * Get the name of the file for a persist without the filename extension.
	 * 
	 * @param persist
	 * @param useOldName
	 * @return
	 */
	public static String getPersistBaseFilename(IPersist persist, boolean useOldName)
	{
		String name = null;
		if (persist instanceof ISupportName)
		{
			name = getPersistName((ISupportName)persist, useOldName);
		}
		else if (persist instanceof TableNode)
		{
			name = ((TableNode)persist).getTableName();
		}

		if (!Utils.stringIsEmpty(name))
		{
			return name;
		}
		return "sv_" + persist.getUUID().toString().replace('-', '_'); //$NON-NLS-1$
	}

	public static String getFileName(IPersist persist, boolean useOldName)
	{
		if (persist == null) return null;

		//combined cases
		if (persist instanceof IVariable || persist instanceof IScriptProvider)
		{
			return getScriptName(persist, useOldName);
		}
		if (persist instanceof Solution)
		{
			return SOLUTION_SETTINGS;
		}

		// Some persists do not have a file of its own, but are saved as item in its parents file
		if (isCompositeWithItems(persist.getParent()))
		{
			return getFileName(persist.getParent(), useOldName);
		}

		String name = getPersistBaseFilename(persist, useOldName);
		if (persist instanceof Media)
		{
			return name;
		}

		if (persist.getTypeID() == IRepository.FORMS)
		{
			return name + FORM_FILE_EXTENSION;
		}

		if (persist.getTypeID() == IRepository.RELATIONS)
		{
			return name + RELATION_FILE_EXTENSION;
		}

		if (persist.getTypeID() == IRepository.VALUELISTS)
		{
			return name + VALUELIST_FILE_EXTENSION;
		}

		if (persist.getTypeID() == IRepository.TABLENODES)
		{
			return name + TABLENODE_FILE_EXTENSION;
		}

		return name + JSON_DEFAULT_FILE_EXTENSION;
	}

	public static String getRelativePath(IPersist persist, boolean useOldName)
	{
		if (persist == null) return ""; //$NON-NLS-1$

		// Some persists do not have a file of its own, but are saved as item in its parents file
		if (isCompositeWithItems(persist.getParent()))
		{
			return getRelativePath(persist.getParent(), useOldName);
		}

		String name = ""; //$NON-NLS-1$

		/*
		 * <solutionname> (as project) /datamodel.xml /<styles>.css (? needed here) /calculations.js (all .js files do contain javadoc tags as
		 * 
		 * @property=uuid:value, "@property=" followed by json prop notation) /aggregates/sum_orders.obj (=aggregate - ISupportName defines the filename)
		 * /valuelists/employees.obj (obj files are JSON objects without the outer accolades) /globals.js (methods + vars) /forms/orders/sv_<uuid>.obj (nameless
		 * form elements) /forms/orders/my_field.obj (named form elements, with ISupportName) /forms/orders/orders.js (script methods)
		 * /forms/orders/tabpanels/sv_<uuid>.obj (elements) /media/aap.gif /media/noot.gif /media/medias.obj (combined media info)
		 * /relations/orders_to_orderitems.obj (JSON relation obj + RI items)
		 */

		if (persist instanceof TableNode)
		{
			name = DATASOURCES_DIR_NAME + '/' + ((TableNode)persist).getServerName();
		}
		if (persist.getParent() instanceof TableNode)
		{
			name = ((TableNode)persist.getParent()).getTableName();
		}
		else if (persist instanceof Media)
		{
			name = MEDIAS_DIR;
		}
		else if (persist instanceof Relation)
		{
			name = RELATIONS_DIR;
		}
		else if (persist instanceof ValueList)
		{
			name = VALUELISTS_DIR;
		}
		else if (persist instanceof Form)
		{
			name = FORMS_DIR;
		}
		else if (persist instanceof Solution)
		{
			name = ((Solution)persist).getName();
		}
		return getRelativePath(persist.getParent(), useOldName) + name + '/';
	}

	public static Pair<String, String> getFilePath(IPersist persist, boolean useOldName)
	{
		return new Pair<String, String>(getRelativePath(persist, useOldName), getFileName(persist, useOldName));
	}

	public static String getScriptName(IPersist persist, boolean useOldName)
	{
		if (persist instanceof IVariable || persist instanceof IScriptProvider)
		{
			return getScriptName(persist.getParent(), useOldName);
		}

		if (persist instanceof Solution)
		{
			return GLOBALS_FILE;
		}
		if (persist instanceof Form)
		{
			return getPersistName((Form)persist, useOldName) + JS_FILE_EXTENSION;
		}
		if (persist instanceof TableNode)
		{
			return ((TableNode)persist).getTableName() + CALCULATIONS_POSTFIX_WITH_EXT;
		}

		return null;
	}

	public static String getScriptPath(IPersist persist, boolean useOldName)
	{
		if (persist instanceof IVariable || persist instanceof IScriptProvider)
		{
			return getScriptPath(persist.getParent(), useOldName);
		}

		if (persist instanceof Solution || persist instanceof Form || persist instanceof TableNode)
		{
			return getRelativePath(persist, useOldName) + getScriptName(persist, useOldName);
		}

		return null;
	}

	/**
	 * Get the file that contains the parent of the persist saved in file.
	 * 
	 * @param file
	 * @return
	 */
	public static File getParentFile(File workspace, File file)
	{
		if (file == null || file.equals(workspace))
		{
			// at the root
			return null;
		}

		File dir = file.getParentFile();
		String fileName = file.getName();
		if (dir.getParentFile().equals(workspace))
		{
			// in the solution directory

			if (SOLUTION_SETTINGS.equals(fileName) || ROOT_METADATA.equals(fileName))
			{
				// at the root
				return null;
			}

			File solutionFile = new File(dir, SOLUTION_SETTINGS);
			if (solutionFile.exists())
			{
				return solutionFile;
			}

			// if a solution doesn't yet have a "solution_settings.obj" file (for example a new Solution),
			// we return the root meta data file that can be used to get the UUID
			// TODO maybe this should be removed and the creation of the "solution_settings.obj" be added in writePersist(...) when node instance of Solution...
			// TODO thus making the existence of "solution_settings.obj" mandatory
			solutionFile = new File(dir, ROOT_METADATA);
			if (solutionFile.exists())
			{
				return solutionFile;
			}

			// /mysolution/medias/button.png has parent rootmetadata.obj
			if (MEDIAS_DIR.equals(dir.getName()))
			{
				solutionFile = new File(dir.getParent(), ROOT_METADATA);
				if (solutionFile.exists())
				{
					return solutionFile;
				}
				// else, we have a tab called medias
			}
		}


		if (fileName.endsWith(JS_FILE_EXTENSION))
		{
			// is it a form
			File dirFile = new File(file.getParent(), fileName.substring(0, fileName.length() - JS_FILE_EXTENSION.length()) + FORM_FILE_EXTENSION);
			if (dirFile.exists()) return dirFile;
			// is it a tablenode
			if (fileName.endsWith(CALCULATIONS_POSTFIX_WITH_EXT))
			{
				dirFile = new File(file.getParent(), fileName.substring(0, fileName.length() - CALCULATIONS_POSTFIX_WITH_EXT.length()) +
					TABLENODE_FILE_EXTENSION);
				if (dirFile.exists()) return dirFile;
			}
		}
		// try one level up
		return getParentFile(workspace, dir);
	}

	private static String getPersistName(ISupportName p, boolean useOldName)
	{
		String retval = p.getName();
		if (useOldName)
		{
			//see if old name is present, if so use
			String oldName = ((AbstractBase)p).getRuntimeProperty(AbstractBase.NameChangeProperty);
			if (oldName != null)
			{
				if (oldName.equals("")) //$NON-NLS-1$
				{
					retval = null;
				}
				else
				{
					retval = oldName;
				}
			}
		}
		return retval;
	}
}
