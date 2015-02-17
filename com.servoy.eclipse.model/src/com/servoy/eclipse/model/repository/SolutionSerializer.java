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
package com.servoy.eclipse.model.repository;


import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.zip.GZIPOutputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.dltk.javascript.ast.Comment;
import org.eclipse.dltk.javascript.ast.MultiLineComment;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.servoy.base.util.DataSourceUtilsBase;
import com.servoy.eclipse.model.util.IFileAccess;
import com.servoy.eclipse.model.util.IValueFilter;
import com.servoy.eclipse.model.util.ServoyLog;
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
import com.servoy.j2db.persistence.IScriptElement;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ISupportExtendsID;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.ISupportScope;
import com.servoy.j2db.persistence.IVariable;
import com.servoy.j2db.persistence.LayoutContainer;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.MethodArgument;
import com.servoy.j2db.persistence.NameComparator;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RelationItem;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptCalculation;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.Style;
import com.servoy.j2db.persistence.TabPanel;
import com.servoy.j2db.persistence.TableNode;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.ServoyJSONArray;
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;

/**
 * Writes repository (solution) objects from file system directories.
 *
 * @author jblok
 */
public class SolutionSerializer
{
	public static final int VERSION_38 = 38;

	public static final String FORMS_DIR = "forms";
	public static final String WORKINGSETS_DIR = "workingsets";
	public static final String VALUELISTS_DIR = "valuelists";
	public static final String RELATIONS_DIR = "relations";
	public static final String COMPONENTS_DIR_NAME = "components";
	public static final String SERVICES_DIR_NAME = "services";

	public static final String DATASOURCES_DIR_NAME = "datasources";
	public static final int JSON_FILE_EXTENSION_SIZE = 4;
	public static final String JSON_DEFAULT_FILE_EXTENSION = ".obj";
	public static final String FORM_FILE_EXTENSION = ".frm";
	public static final String RELATION_FILE_EXTENSION = ".rel";
	public static final String VALUELIST_FILE_EXTENSION = ".val";
	public static final String TABLENODE_FILE_EXTENSION = ".tbl";
	public static final String JS_FILE_EXTENSION_WITHOUT_DOT = "js";
	public static final String JS_FILE_EXTENSION = '.' + JS_FILE_EXTENSION_WITHOUT_DOT;
	public static final String STYLE_FILE_EXTENSION = ".css";
	public static final String TEMPLATE_FILE_EXTENSION = ".template";
	public static final String CALCULATIONS_POSTFIX_WITHOUT_EXT = "_calculations";
	public static final String CALCULATIONS_POSTFIX = CALCULATIONS_POSTFIX_WITHOUT_EXT + JS_FILE_EXTENSION;
	public static final String FOUNDSET_POSTFIX_WITHOUT_EXT = "_entity";
	public static final String FOUNDSET_POSTFIX = FOUNDSET_POSTFIX_WITHOUT_EXT + JS_FILE_EXTENSION;
	public static final String GLOBALS_FILE = ScriptVariable.GLOBAL_SCOPE + JS_FILE_EXTENSION;
	public static final String WORKINGSETS_FILE = "workingsets.json";

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
			if (ab instanceof IScriptElement)
			{
				IScriptElement sp = (IScriptElement)ab;
				if (sp.getLineNumberOffset() != 0)
				{
					return Integer.valueOf(sp.getLineNumberOffset());
				}
			}
			return null;
		}
	};

	public static final String SOLUTION_SETTINGS = "solution_settings" + JSON_DEFAULT_FILE_EXTENSION;
	public static final String ROOT_METADATA = "rootmetadata" + JSON_DEFAULT_FILE_EXTENSION;
	public static final String MEDIAS_DIR = "medias";
	public static final String MEDIAS_FILE = MEDIAS_DIR + JSON_DEFAULT_FILE_EXTENSION;
	public static final String FUNCTION_KEYWORD = "function";
	public static final String VAR_KEYWORD = "var";
	public static final String SV_COMMENT_START = "/**";
	public static final String SV_COMMENT_END = "*/";
	public static final String PROP_ITEMS = "items";
	public static final String PROP_NAME = "name";
	public static final String PROP_TYPEID = "typeid";
	public static final String PROP_MIME_TYPE = "mimeType";
	public static final String PROP_UUID = "uuid";
	public static final String PROP_FILE_VERSION = "fileVersion";
	public static final String PROP_ENCAPSULATION = "encapsulation";
	public static final String PROP_DEPRECATED = "deprecated";

	public static final String PROPERTIESKEY = "@properties=";
	public static final String TYPEKEY = "@type";

	/**
	 *
	 * @param parent
	 * @param scriptPath file path to generate
	 * @param repository
	 * @param userTemplate
	 * @return
	 */
	public static String generateScriptFile(final ISupportChilds parent, final String scriptPath, final IDeveloperRepository repository,
		final String userTemplate)
	{
		final Map<IPersist, Object> fileContents = new HashMap<IPersist, Object>(); // map(persist -> contents)
		final TreeSet<Comment> comments = new TreeSet<Comment>(new Comparator<Comment>()
		{
			@Override
			public int compare(Comment o1, Comment o2)
			{
				return o1.sourceStart() - o2.sourceStart();
			}
		});
		parent.acceptVisitor(new IPersistVisitor()
		{
			public Object visit(IPersist p)
			{
				if (p == parent) return CONTINUE_TRAVERSAL;

				if (p instanceof IScriptElement && scriptPath.equals(getScriptPath(p, false)))
				{
					Object val = serializePersist(p, true, repository, userTemplate);
					if (val != null)
					{
						fileContents.put(p, val);
						Object prop = ((AbstractBase)p).getCustomProperty(new String[] { SolutionDeserializer.EXTRA_DOC_COMMENTS });
						if (prop != null)
						{
							try
							{
								JSONArray array = new JSONArray((String)prop);
								for (int i = 0; i < array.length(); i++)
								{
									JSONObject jsonObject = array.getJSONObject(i);
									MultiLineComment multiLineComment = new MultiLineComment();
									multiLineComment.setStart(jsonObject.getInt("start"));
									multiLineComment.setEnd(jsonObject.getInt("end"));
									multiLineComment.setText(jsonObject.getString("text"));
									comments.add(multiLineComment);
								}
							}
							catch (JSONException e)
							{
								ServoyLog.logError(e);
							}
						}
					}
				}
				return CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
			}
		});

		// write all objects for the target file to the string
		if (fileContents.size() > 0)
		{
			AbstractBase[] persistArray = fileContents.keySet().toArray(new AbstractBase[fileContents.size()]);
			Arrays.sort(persistArray, VARS_METHODS);

			StringBuilder sb = new StringBuilder();
			for (AbstractBase persist : persistArray)
			{
				if (sb.length() > 0) sb.append('\n');
				Object content = fileContents.get(persist);
				if (content instanceof CharSequence)
				{
					if (!comments.isEmpty())
					{
						Comment nextComment = comments.first();
						while (nextComment.sourceStart() <= (sb.length() + ((CharSequence)content).length()))
						{
							sb.append(nextComment.getText());
							sb.append('\n');
							sb.append('\n');
							comments.remove(nextComment);
							nextComment = comments.first();
						}
					}
					sb.append(((CharSequence)content).toString());
				}
			}

			for (Comment comment : comments)
			{
				sb.append(comment.getText());
				sb.append('\n');
				sb.append('\n');
			}
			return sb.toString();
		}

		return "";
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
			final Map<Pair<String, String>, TreeSet<Comment>> projectComments = new HashMap<Pair<String, String>, TreeSet<Comment>>();//filepathname -> map(persist -> contents)
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
							Object val = serializePersist(p, !(p instanceof Solution), repository, null);
							if (val != null)
							{
								if (p instanceof IScriptElement)
								{
									Object prop = ((AbstractBase)p).getCustomProperty(new String[] { SolutionDeserializer.EXTRA_DOC_COMMENTS });
									if (prop != null)
									{
										TreeSet<Comment> comments = projectComments.get(filepathname);
										if (comments == null)
										{
											comments = new TreeSet<Comment>(new Comparator<Comment>()
											{
												@Override
												public int compare(Comment o1, Comment o2)
												{
													return o1.sourceStart() - o2.sourceStart();
												}
											});
											projectComments.put(filepathname, comments);
										}
										try
										{
											JSONArray array = new JSONArray((String)prop);
											for (int i = 0; i < array.length(); i++)
											{
												JSONObject jsonObject = array.getJSONObject(i);
												MultiLineComment multiLineComment = new MultiLineComment();
												multiLineComment.setStart(jsonObject.getInt("start"));
												multiLineComment.setEnd(jsonObject.getInt("end"));
												multiLineComment.setText(jsonObject.getString("text"));
												comments.add(multiLineComment);
											}
										}
										catch (JSONException e)
										{
											ServoyLog.logError(e);
										}
									}
								}
								fileContents.put(p, val);
							}
						}
						else
						{
							ServoyLog.logWarning("multiple objects found for the same file: " + p + " and " + fileContents + " -- ignoring input", null);
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
					TreeSet<Comment> comments = projectComments.get(filepathname);
					OutputStream fos = fileAccess.getOutputStream(fileRelativePath);
					int written = 0;
					for (int i = 0; i < persistArray.length; i++)
					{
						Object content = fileContents.get(persistArray[i]);
						if (content instanceof byte[])
						{
							written += ((byte[])content).length;
							fos.write((byte[])content);
						}
						else if (content instanceof CharSequence)
						{
							if (comments != null && comments.size() > 0)
							{
								Comment first = comments.first();
								while (first != null && first.sourceStart() <= (written + ((CharSequence)content).length()))
								{
									written += first.getText().length();
									fos.write(first.getText().getBytes("UTF8"));
									fos.write('\n');
									fos.write('\n');
									comments.remove(first);
									first = (comments.size() > 0 ? comments.first() : null);
								}
							}
							written += ((CharSequence)content).length();
							fos.write(content.toString().getBytes("UTF8"));
							if (i < persistArray.length - 1) fos.write('\n');
						}
					}
					if (comments != null && comments.size() > 0)
					{
						for (Comment comment : comments)
						{
							fos.write(comment.getText().getBytes("UTF8"));
							fos.write('\n');
							fos.write('\n');
						}
					}
					fileAccess.closeOutputStream(fos);
				}
				if (fileAccess.getFileLength(fileRelativePath) == 0 && !(node instanceof Media) && overwriteExisting)
				{
					fileAccess.delete(fileRelativePath);
				}
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

				//create the "solution_settings.obj" file
				Pair<String, String> solutionPath = getFilePath(node, false);
				String solutionFile = solutionPath.getLeft() + solutionPath.getRight();
				if (!fileAccess.exists(solutionFile))
				{
					Object val = serializePersist(node, false, repository, null);
					if (val != null) fileAccess.setUTF8Contents(solutionFile, val.toString());
				}
			}

			if (node instanceof Media || recursive && node instanceof Solution)
			{
				//custom list in one file for all medias
				String fmedias = node.getRootObject().getName() + '/' + MEDIAS_FILE;
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
								ServoyLog.logError("Could not delete file " + fmedias, e);
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
			Solution s = (Solution)node.getAncestor(IRepository.SOLUTIONS);
			throw new RepositoryException("Could not write object " + node + " to workspace directory " +
				fileAccess.getWorkspaceOSPath(s != null ? s.getName() : null), e);
		}
		catch (JSONException e)
		{
			Solution s = (Solution)node.getAncestor(IRepository.SOLUTIONS);
			throw new RepositoryException("Could not write object " + node + " to workspace directory " +
				fileAccess.getWorkspaceOSPath(s != null ? s.getName() : null), e);
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
		obj.put("solutionType", new Integer(smd.getSolutionType()));
		obj.put("mustAuthenticate", Boolean.valueOf(smd.getMustAuthenticate()));
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
				obj.put(PROP_ENCAPSULATION, media.getEncapsulation());
				obj.put(PROP_DEPRECATED, media.getDeprecated());
				items.put(obj);
			}
			return items.toString();
		}
		return null;
	}

	public static String getWorkingSetPath(String resourcesProjectName)
	{
		return resourcesProjectName + IPath.SEPARATOR + WORKINGSETS_DIR + IPath.SEPARATOR + WORKINGSETS_FILE;
	}

	public static void serializeWorkingSetInfo(IFileAccess fileAccess, String resourcesProjectName, Map<String, List<String>> workingSets)
	{
		if (fileAccess != null)
		{
			String path = getWorkingSetPath(resourcesProjectName);
			if (workingSets != null)
			{
				JSONArray items = new ServoyJSONArray();
				for (String workingSetName : workingSets.keySet())
				{
					try
					{
						ServoyJSONObject obj = new ServoyJSONObject(false, true);
						obj.put(PROP_NAME, workingSetName);
						JSONArray jsonPaths = new ServoyJSONArray();
						obj.put("paths", jsonPaths);
						List<String> paths = workingSets.get(workingSetName);
						if (paths != null)
						{
							for (String filePath : paths)
							{
								jsonPaths.put(filePath);
							}
						}
						items.put(obj);
					}
					catch (Exception ex)
					{
						ServoyLog.logError(ex);
					}
				}
				String content = items.toString();
				try
				{
					fileAccess.setUTF8Contents(path, content);
				}
				catch (Exception ex)
				{
					ServoyLog.logError(ex);
				}
			}
		}
	}

	public static boolean isCompositeWithItems(IPersist p)
	{
		if (p != null)
		{
			Solution parentSolution = (Solution)p.getAncestor(IRepository.SOLUTIONS);
			if (parentSolution != null && parentSolution.getSolutionMetaData().getFileVersion() < VERSION_38)
			{
				// backward compatibility mode
				return p instanceof Relation;
			}
		}

		return p instanceof Relation || p instanceof TableNode || p instanceof Form || p instanceof TabPanel || p instanceof Portal ||
			p instanceof LayoutContainer; // can only return true for objects containing SolutionSerializer.PROP_ITEMS
	}

	public static boolean isCompositeItem(IPersist p)
	{
		return !(p instanceof Media || p instanceof IRootObject || p instanceof Relation || p instanceof ValueList || p instanceof Form ||
			p instanceof TableNode || p instanceof IScriptElement);
	}

	public static String getComment(IPersist persist, String userTemplate, final IDeveloperRepository repository)
	{
		if (persist instanceof ScriptVariable)
		{
			String comment = ((ScriptVariable)persist).getComment();
			if (comment != null)
			{
				if (comment.indexOf(SV_COMMENT_START) >= 0)
				{
					comment = comment.substring(comment.indexOf(SV_COMMENT_START));
				}
				else
				{
					// new variable
					comment = "";
				}
			}
			return getCommentImpl(persist, repository, comment, userTemplate);
		}
		if (persist instanceof AbstractScriptProvider)
		{
			String comment = ((AbstractScriptProvider)persist).getRuntimeProperty(IScriptProvider.COMMENT);
			if ("".equals(comment)) comment = null;
			if (comment == null)
			{
				String declaration = ((AbstractScriptProvider)persist).getDeclaration();
				int commentStart = declaration.indexOf(SV_COMMENT_START);
				if (commentStart != -1 && declaration.lastIndexOf(FUNCTION_KEYWORD, commentStart) == -1)
				{
					int commentEnd = declaration.indexOf(SV_COMMENT_END, commentStart);
					comment = declaration.substring(commentStart, commentEnd + 2);
				}
			}
			if (comment != null && comment.indexOf(SV_COMMENT_START) >= 0)
			{
				comment = comment.substring(comment.indexOf(SV_COMMENT_START));
			}
			else
			{
				// new function
				comment = "";
			}
			return getCommentImpl(persist, repository, comment, userTemplate);
		}
		throw new IllegalArgumentException("Persist must be an ScriptMethod/Calc or Variable " + persist);
	}

	/**
	 * @param variable
	 * @param repository
	 */
	private static String getCommentImpl(IPersist persist, final IDeveloperRepository repository, String currentComment, String userTemplate)
	{
		ServoyJSONObject obj;
		try
		{
			obj = generateJSONObject(persist, false, false, repository, false, null);
			obj.setNewLines(false);
			obj.remove(StaticContentSpecLoader.PROPERTY_METHODCODE.getPropertyName());
			obj.remove(StaticContentSpecLoader.PROPERTY_DECLARATION.getPropertyName());
			obj.remove(StaticContentSpecLoader.PROPERTY_NAME.getPropertyName());
			obj.remove(StaticContentSpecLoader.PROPERTY_DEFAULTVALUE.getPropertyName());
			obj.remove(StaticContentSpecLoader.PROPERTY_SCOPENAME.getPropertyName());
			obj.remove(SolutionDeserializer.COMMENT_JSON_ATTRIBUTE);
			obj.remove(SolutionDeserializer.LINE_NUMBER_OFFSET_JSON_ATTRIBUTE);
			// remove custom properties (extra comment part)
			obj.remove("customProperties");
			StringBuilder sb = new StringBuilder();
			if (currentComment == null)
			{
				generateDefaultJSDoc(obj, sb, userTemplate, persist instanceof AbstractScriptProvider ? (AbstractScriptProvider)persist : null);
			}
			else
			{
				sb.append(currentComment);
				replacePropertiesTag(obj, sb, userTemplate, persist instanceof AbstractScriptProvider ? (AbstractScriptProvider)persist : null);
			}
			if (persist instanceof ScriptVariable)
			{
				generateJSDocScriptVariableType((ScriptVariable)persist, sb);
			}

			return sb.toString();
		}
		catch (RepositoryException e)
		{
			ServoyLog.logError("Error generation json for: " + persist, e);
		}
		return null;
	}

	/**
	 * @param persist
	 * @param sb
	 */
	private static void generateJSDocScriptVariableType(ScriptVariable variable, StringBuilder sb)
	{
		int type = Column.mapToDefaultType(variable.getVariableType());
		// Add the "@type" tag.
		String jsType = variable.getSerializableRuntimeProperty(IScriptProvider.TYPE);
		ArgumentType argumentType = ArgumentType.convertFromColumnType(type, jsType);
		// don't replace object types see SolutionDeserializer.parseJSFile()
		if (jsType == null || !jsType.startsWith("{{"))
		{
			if (argumentType != ArgumentType.Object)
			{
				int index = sb.lastIndexOf(TYPEKEY);
				if (index != -1)
				{
					int lineEnd = sb.indexOf("\n", index);
					sb.replace(index + TYPEKEY.length() + 1, lineEnd, '{' + argumentType.getName() + '}');
				}
				else
				{
					int lineEnd = -1;
					int startProp = sb.indexOf(PROPERTIESKEY);
					if (startProp != 1)
					{
						// insert just before @properties
						lineEnd = sb.lastIndexOf("\n", startProp);
					}
					if (lineEnd == -1)
					{
						// else insert after comment start
						lineEnd = sb.indexOf("\n", sb.indexOf(SV_COMMENT_START));
					}
					sb.insert(lineEnd, "\n * " + TYPEKEY + " {" + argumentType.getName() + "}\n *");
				}
			}
			else if (jsType != null && ArgumentType.isGeneratedType(jsType))
			{
				// remove existing object type when generated from columnn type, do not touch others
				int index = sb.lastIndexOf(TYPEKEY);
				if (index != -1)
				{
					int lineEnd = sb.indexOf("\n", index);
					if (lineEnd != -1 && sb.substring(lineEnd + 1, lineEnd + 4).equals(" *\n"))
					{
						// delete next empty line as well
						lineEnd += 2;
					}

					int lineStart = sb.lastIndexOf("\n", index);
					sb.delete(lineStart == -1 ? index : lineStart, lineEnd == -1 ? sb.length() : lineEnd + 1);
				}
			}
		}
	}

	//returns CharSequence,byte[] or JSONObject(=internal for relation items, work on parent!)
	public static Object serializePersist(IPersist persist, boolean forceRecursive, final IDeveloperRepository repository, String userTemplate)
	{
		if (persist instanceof Media)
		{
			return ((Media)persist).getMediaData();
		}
		else
		{
			if (isCompositeWithItems(persist.getParent()) && !(persist instanceof IScriptElement)) // safety, should never get here anyway
			{
				return null;
			}
			ServoyJSONObject obj;
			try
			{
				obj = generateJSONObject(persist, forceRecursive, false, repository, false, null);
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError("Error generation json for: " + persist, e);
				return null;
			}

			StringBuilder sb = new StringBuilder();
			if (persist instanceof IScriptElement)
			{
				obj.setNewLines(false);
				obj.remove(StaticContentSpecLoader.PROPERTY_METHODCODE.getPropertyName());
				obj.remove(StaticContentSpecLoader.PROPERTY_DECLARATION.getPropertyName());
				obj.remove(PROP_NAME);
				obj.remove(StaticContentSpecLoader.PROPERTY_DEFAULTVALUE.getPropertyName());
				obj.remove(StaticContentSpecLoader.PROPERTY_SCOPENAME.getPropertyName());
				obj.remove(SolutionDeserializer.COMMENT_JSON_ATTRIBUTE);
				obj.remove(SolutionDeserializer.LINE_NUMBER_OFFSET_JSON_ATTRIBUTE);
				// remove custom properties (extra comment part)
				obj.remove("customProperties");

				if (persist instanceof AbstractScriptProvider)
				{
					AbstractScriptProvider abstractScriptProvider = (AbstractScriptProvider)persist;
					String source = abstractScriptProvider.getDeclaration();
//					source = Utils.stringReplaceRecursive(source, "/**/", "");//make sure empty comment is not in the source, which will interfere with next line
//					source = Utils.stringReplaceRecursive(source, SV_COMMENT, "/*");//make sure "/**" is not found in the source ever
					if (source != null && source.trim().length() > 0)
					{
						sb.append(source);
						replacePropertiesTag(obj, sb, userTemplate, abstractScriptProvider);
					}
					else
					{
						generateDefaultJSDoc(obj, sb, userTemplate, abstractScriptProvider);
						sb.append("function ");
						sb.append(abstractScriptProvider.getName());
						sb.append("()\n{\n}\n");
					}
				}
				else if (persist instanceof ScriptVariable)
				{
					ScriptVariable sv = (ScriptVariable)persist;
					if (sv.getComment() != null)
					{
						sb.append(sv.getComment());
						sb.append('\n');
						replacePropertiesTag(obj, sb, userTemplate, null);
					}
					else
					{
						generateDefaultJSDoc(obj, sb, userTemplate, null);
					}
					generateJSDocScriptVariableType(sv, sb);
					sb.append(VAR_KEYWORD);
					sb.append(' ');
					sb.append(sv.getName());
//					if (jsType != null)
//					{
//						sb.append(':');
//						sb.append(jsType);
//					}
					int type = Column.mapToDefaultType(sv.getVariableType());
					String val = sv.getDefaultValue();
					if (type == IColumnTypes.TEXT)
					{
						if (val == null)
						{
							val = "''"; // to keep same behavior
						}
					}
					else if (type == IColumnTypes.NUMBER)
					{
						if (val != null)
						{
							val = val.replace(",", "."); // you cannot have comma as decimal separator inside JS code
						}
					}
					if (val != null)
					{
						sb.append(" = ");
						if ("now".equals(val)) val = "new Date()";//to keep same behavior
						sb.append(val);
					}
					sb.append(";\n");
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
	private static void replacePropertiesTag(ServoyJSONObject obj, StringBuilder sb, String userTemplate, AbstractScriptProvider abstractScriptProvider)
	{
		if (sb.toString().trim().contains(SV_COMMENT_START))
		{
			int startIndex = sb.indexOf(SV_COMMENT_START);
			int endComment = sb.indexOf("*/", startIndex);
			if (endComment == -1) return;
			// just always replace the properties to be sure those are accurate.
			int index = sb.lastIndexOf(PROPERTIESKEY, endComment);
			if (index != -1)
			{
				int lineEnd = sb.indexOf("\n", index);
				lineEnd = lineEnd < endComment ? lineEnd : endComment;
				sb.replace(index + PROPERTIESKEY.length(), lineEnd, obj.toString(false));
			}
			else if (obj.length() > 0)
			{
				StringBuilder params = new StringBuilder();
				if (sb.indexOf("@param") == -1)
				{
					generateParams(params, abstractScriptProvider);
					if (params.length() > 0)
					{
						sb.insert(endComment, "* \n" + params + " ");
						endComment += params.length() + 4;
					}
				}
				sb.insert(endComment, "*\n * " + PROPERTIESKEY + obj.toString(false) + "\n ");

			}
		}
		else
		{
			StringBuilder doc = new StringBuilder();
			generateDefaultJSDoc(obj, doc, userTemplate, abstractScriptProvider);
			sb.insert(0, doc);
		}
	}

	/**
	 * @param obj
	 * @param sb
	 * @param abstractScriptProvider TODO
	 */
	private static void generateDefaultJSDoc(ServoyJSONObject obj, StringBuilder sb, String userTemplate, AbstractScriptProvider abstractScriptProvider)
	{
		sb.append(SV_COMMENT_START);
		sb.append("\n");

		if (userTemplate != null && userTemplate.length() > 0)
		{
			for (String line : userTemplate.split("\n"))
			{
				sb.append(" * ").append(line).append('\n');
			}
			sb.append(" *\n");
		}

		if (generateParams(sb, abstractScriptProvider))
		{
			sb.append(" *\n");
		}
		if (obj.length() > 0)
		{
			sb.append(" * ");
			sb.append(PROPERTIESKEY);
			sb.append(obj.toString(false));
		}
		else
		{
			sb.append(" *");
		}
		sb.append("\n " + SV_COMMENT_END + "\n");
	}

	/**
	 * @param sb
	 * @param abstractScriptProvider
	 */
	private static boolean generateParams(StringBuilder sb, AbstractScriptProvider abstractScriptProvider)
	{
		if (abstractScriptProvider == null) return false;
		MethodArgument[] arguments = abstractScriptProvider.getRuntimeProperty(IScriptProvider.METHOD_ARGUMENTS);
		if (arguments != null && arguments.length > 0)
		{
			sb.append(" * TODO generated, please specify type and doc for the params\n");
			for (MethodArgument methodArgument : arguments)
			{
				sb.append(" * @param ");
				if (methodArgument.getType() != null && !methodArgument.getType().toString().endsWith("Any"))
				{
					sb.append("{");
					sb.append(methodArgument.getType().toString());
					sb.append("} ");
				}
				sb.append(methodArgument.getName());
				if (methodArgument.getDescription() != null)
				{
					sb.append(methodArgument.getDescription());
				}
				sb.append("\n");
			}
			return true;
		}
		return false;
	}

	/**
	 * Get the persist values as map, when makeFlattened is true, the map will contain all super-propewrties as well.
	 *
	 * @param persist
	 * @param repository
	 * @param makeFlattened
	 * @return
	 * @throws RepositoryException
	 */
	public static Map<String, Object> getPersistAsValueMap(IPersist persist, IDeveloperRepository repository, boolean makeFlattened) throws RepositoryException
	{
		Map<String, Object> property_values = repository.getPersistAsValueMap(persist);
		if (makeFlattened && persist instanceof ISupportExtendsID)
		{
			IPersist superPersist = PersistHelper.getSuperPersist((ISupportExtendsID)persist);
			if (superPersist != null)
			{
				Map<String, Object> flattened_property_values = getPersistAsValueMap(superPersist, repository, makeFlattened);
				flattened_property_values.putAll(property_values);
				return flattened_property_values;
			}
		}
		return property_values;
	}

	/**
	 * @param persist
	 * @param repository
	 * @return
	 * @throws RepositoryException
	 */
	public static ServoyJSONObject generateJSONObject(IPersist persist, boolean forceRecursive, boolean makeFlattened, IDeveloperRepository repository,
		boolean useQuotesForKey, IValueFilter valueFilter) throws RepositoryException
	{
		Map<String, Object> property_values = getPersistAsValueMap(persist, repository, makeFlattened);

		ContentSpec cs = repository.getContentSpec();
		Iterator<ContentSpec.Element> iterator = cs.getPropertiesForObjectType(persist.getTypeID());
		while (iterator.hasNext())
		{
			ContentSpec.Element element = iterator.next();

			if (element.isMetaData() || element.isDeprecated() || !property_values.containsKey(element.getName())) continue;

			String propertyName = element.getName();
			if (makeFlattened && StaticContentSpecLoader.PROPERTY_EXTENDSID.getPropertyName().equals(propertyName) && element.getTypeID() != IRepository.FORMS)
			{
				property_values.remove(propertyName);
				continue; // we flattened the properties, ignore extends property
			}

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

			if (valueFilter != null)
			{
				String filteredValue = valueFilter.getFilteredValue(persist, property_values, propertyName, propertyValue);
				if (filteredValue != null)
				{
					if (!(filteredValue.equals(propertyValue) && (isBoolean || isNumber)))
					{
						property_values.put(propertyName, filteredValue);
					}
				}
				else
				{
					property_values.remove(propertyName);
				}
			}
			else
			{
				if (!isBoolean && !isNumber)
				{
					property_values.put(propertyName, propertyValue);//replace with textual version
				}
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
				if (!(child instanceof IScriptElement)) itemsArrayList.add(generateJSONObject(child, forceRecursive, makeFlattened, repository,
					useQuotesForKey, valueFilter));
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
							ServoyLog.logWarning("Cannot compare json objects based on uuid", ex);
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

		return new ServoyJSONObject(property_values, !useQuotesForKey, true);
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
			name = getServerNameTableName((TableNode)persist, useOldName)[1];
		}

		if (!Utils.stringIsEmpty(name))
		{
			return name;
		}
		return "sv_" + persist.getUUID().toString().replace('-', '_');
	}

	public static String getFileName(IPersist persist, boolean useOldName)
	{
		if (persist == null) return null;

		//combined cases
		if (persist instanceof IScriptElement)
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
		if (persist == null) return "";

		// Some persists do not have a file of its own, but are saved as item in its parents file
		if (isCompositeWithItems(persist.getParent()))
		{
			return getRelativePath(persist.getParent(), useOldName);
		}

		String name = "";

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
			name = DATASOURCES_DIR_NAME + '/' + getServerNameTableName((TableNode)persist, useOldName)[0];
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
		String relativePath = getRelativePath(persist.getParent(), useOldName) + name;
		return relativePath.endsWith("/") ? relativePath : (relativePath + '/');
	}

	public static Pair<String, String> getFilePath(IPersist persist, boolean useOldName)
	{
		return new Pair<String, String>(getRelativePath(persist, useOldName), getFileName(persist, useOldName));
	}

	public static String getRelativeFilePath(IPersist persist, boolean useOldName)
	{
		return getRelativePath(persist, useOldName) + getFileName(persist, useOldName);
	}

	public static String getScriptName(IPersist persist, boolean useOldName)
	{
		ISupportChilds parent;
		if (persist instanceof ISupportChilds)
		{
			parent = (ISupportChilds)persist;
		}
		else
		{
			parent = persist.getParent();
		}

		if (persist instanceof Solution)
		{
			return GLOBALS_FILE;
		}
		if (parent instanceof Solution && persist instanceof ISupportScope)
		{
			return ((ISupportScope)persist).getScopeName() + JS_FILE_EXTENSION;
		}
		if (parent instanceof Form)
		{
			return getPersistName((Form)parent, useOldName) + JS_FILE_EXTENSION;
		}
		if (parent instanceof TableNode)
		{
			String tablename = getServerNameTableName((TableNode)parent, useOldName)[1];
			if (persist instanceof ScriptCalculation)
			{
				return tablename + CALCULATIONS_POSTFIX;
			}
			return tablename + FOUNDSET_POSTFIX;
		}

		return null;
	}

	public static String getScriptPath(IPersist persist, boolean useOldName)
	{
		ISupportChilds parent;
		if (persist instanceof ISupportChilds)
		{
			parent = (ISupportChilds)persist;
		}
		else
		{
			parent = persist.getParent();
		}

		if (parent instanceof Solution || parent instanceof Form || parent instanceof TableNode)
		{
			return getRelativePath(parent, useOldName) + getScriptName(persist, useOldName);
		}

		return null;
	}

	public static String[] getScriptPaths(IPersist persist, boolean useOldName)
	{
		if (persist instanceof TableNode)
		{
			// has 2 script files
			String relativePath = getRelativePath(persist, useOldName);
			if (relativePath == null)
			{
				return null;
			}
			String tableName = getServerNameTableName((TableNode)persist, useOldName)[1];
			return new String[] { relativePath + tableName + CALCULATIONS_POSTFIX, relativePath + tableName + FOUNDSET_POSTFIX };
		}

		String path = getScriptPath(persist, useOldName);
		return path == null ? null : new String[] { path };
	}


	/**
	 * Get the file that contains the parent of the persist saved in file.
	 *
	 * @param file
	 * @return
	 */
	public static File getParentFile(File projectFile, File file)
	{
		if (file == null || file.equals(projectFile.getParentFile()))
		{
			// at the root
			return null;
		}

		File dir = file.getParentFile();
		String fileName = file.getName();
		if (dir.equals(projectFile))
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
			if (fileName.endsWith(CALCULATIONS_POSTFIX))
			{
				dirFile = new File(file.getParent(), fileName.substring(0, fileName.length() - CALCULATIONS_POSTFIX.length()) + TABLENODE_FILE_EXTENSION);
				if (dirFile.exists()) return dirFile;
			}
			if (fileName.endsWith(FOUNDSET_POSTFIX))
			{
				dirFile = new File(file.getParent(), fileName.substring(0, fileName.length() - FOUNDSET_POSTFIX.length()) + TABLENODE_FILE_EXTENSION);
				if (dirFile.exists()) return dirFile;
			}
		}
		// try one level up
		return getParentFile(projectFile, dir);
	}

	public static String getFormNameFromFile(IResource resource)
	{
		if (resource != null)
		{
			IPath path = resource.getProjectRelativePath();
			if (path.segmentCount() == 2 && path.segment(0).equals(SolutionSerializer.FORMS_DIR))
			{
				String formFile = path.segment(1);
				if (formFile.endsWith(SolutionSerializer.JS_FILE_EXTENSION))
				{
					return formFile.substring(0, formFile.length() - SolutionSerializer.JS_FILE_EXTENSION.length());
				}
				if (formFile.endsWith(SolutionSerializer.FORM_FILE_EXTENSION))
				{
					return formFile.substring(0, formFile.length() - SolutionSerializer.FORM_FILE_EXTENSION.length());
				}
			}
		}
		return null;
	}

	public static String[] getDataSourceForCalculationJSFile(IResource resource)
	{
		return getDataSourceForTablenodeJSFile(resource, SolutionSerializer.CALCULATIONS_POSTFIX);
	}

	public static String[] getDataSourceForFoundsetJSFile(IResource resource)
	{
		return getDataSourceForTablenodeJSFile(resource, SolutionSerializer.FOUNDSET_POSTFIX);
	}

	private static String[] getDataSourceForTablenodeJSFile(IResource resource, String postfix)
	{
		if (resource != null)
		{
			IPath path = resource.getProjectRelativePath();
			if (path.segmentCount() == 3 && path.segment(0).equals(SolutionSerializer.DATASOURCES_DIR_NAME))
			{
				String jsfile = path.segment(2);
				if (jsfile.endsWith(postfix))
				{
					return new String[] { path.segment(1), jsfile.substring(0, jsfile.length() - postfix.length()) };
				}
			}
		}
		return null;
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
				if (oldName.equals(""))
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

	/**
	 * check if the file is for the persist
	 *
	 */
	public static boolean isPersistWorkspaceFile(IPersist persist, boolean useOldName, File file)
	{
		if (persist == null) return false;

		IFile fileForLocation = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(new Path(file.getPath()));
		if (fileForLocation == null) return false; // not a workspace file

		Pair<String, String> filePath = getFilePath(persist, useOldName);
		return fileForLocation.getFullPath().toPortableString().equals(IPath.SEPARATOR + filePath.getLeft() + filePath.getRight());
	}

	private static String[] getServerNameTableName(TableNode tableNode, boolean useOldName)
	{
		if (tableNode == null) return null;
		String[] serverNameTableName = new String[2];
		serverNameTableName[0] = tableNode.getServerName();
		serverNameTableName[1] = tableNode.getTableName();
		if (useOldName && tableNode.getRuntimeProperty(AbstractBase.NameChangeProperty) != null)
		{
			String[] names = DataSourceUtilsBase.getDBServernameTablename(tableNode.getRuntimeProperty(AbstractBase.NameChangeProperty));
			if (names != null && names.length == 2)
			{
				serverNameTableName = names;
			}
		}
		return serverNameTableName;
	}

	public static void writeRuntimeSolution(Settings properties, File file, Solution solution, IDeveloperRepository repository, Solution[] mods)
		throws IOException, RepositoryException
	{
		FileOutputStream fis = new FileOutputStream(file);
		BufferedOutputStream bis = new BufferedOutputStream(fis);
		GZIPOutputStream zip = new GZIPOutputStream(bis);
		ObjectOutputStream ois = new ObjectOutputStream(zip);
		if (properties != null)
		{
			//write props into stream
			ois.writeObject(properties);
		}

		//load all blobs permanently in the solution
		Iterator itm = solution.getMedias(false);
		while (itm.hasNext())
		{
			Media media = (Media)itm.next();
			media.getMediaData();//make sure its loaded (lazy loaded normally)
			media.makeBlobPermanent();
		}

		//load all styles permanently in the solution
		Iterator solutionFormsIte = solution.getForms(null, false);
		HashMap<String, Style> all_styles = new HashMap<String, Style>();
		while (solutionFormsIte.hasNext())
		{
			Form solutionForm = (Form)solutionFormsIte.next();
			if (solutionForm.getStyleName() != null)
			{
				Style style = (Style)repository.getActiveRootObject(solutionForm.getStyleName(), IRepository.STYLES);
				if (style != null)
				{
					style.setServerProxies(null);//clear
					style.setRepository(null);//clear
					all_styles.put(style.getName(), style);
				}
			}
		}
		solution.setSerializableRuntimeProperty(Solution.PRE_LOADED_STYLES, all_styles);

		Map<String, IServer> serverProxies = solution.getServerProxies();
		IRepository oldRepository = solution.getRepository();
		solution.setServerProxies(null);//clear
		solution.setRepository(null);//clear

		//write solution into stream
		ois.writeObject(solution);

		// restore stuff that was removed only for serialize
		solution.setRepository(oldRepository);
		solution.setServerProxies(serverProxies);

		int modCount = (mods == null ? 0 : mods.length);
		ois.writeInt(modCount);
		if (mods != null)
		{
			Map<String, IServer>[] tmpModuleServerProxies = new Map[modCount];
			IRepository[] tmpModuleRepositories = new IRepository[modCount];
			int i;
			for (i = 0; i < modCount; i++)
			{
				Solution element = mods[i];
				//load all styles permanently in the solution
				solutionFormsIte = element.getForms(null, false);
				all_styles = new HashMap<String, Style>();
				while (solutionFormsIte.hasNext())
				{
					Form solutionForm = (Form)solutionFormsIte.next();
					if (solutionForm.getStyleName() != null)
					{
						Style style = (Style)repository.getActiveRootObject(solutionForm.getStyleName(), IRepository.STYLES);
						if (style != null)
						{
							style.setServerProxies(null);//clear
							style.setRepository(null);//clear
							all_styles.put(style.getName(), style);
						}
					}


				}
				element.setSerializableRuntimeProperty(Solution.PRE_LOADED_STYLES, all_styles);

				//load all blobs permanently in the module
				Iterator moduleMediaIte = element.getMedias(false);
				while (moduleMediaIte.hasNext())
				{
					Media media = (Media)moduleMediaIte.next();
					media.getMediaData();//make sure its loaded (lazy loaded normally)
					media.makeBlobPermanent();
				}

				tmpModuleServerProxies[i] = element.getServerProxies();
				tmpModuleRepositories[i] = element.getRepository();

				element.setServerProxies(null);//clear
				element.setRepository(null);//clear
			}
			//write modules into stream
			ois.writeObject(mods);

			// restore repositories and server proxies
			for (i = 0; i < modCount; i++)
			{
				mods[i].setServerProxies(tmpModuleServerProxies[i]);
				mods[i].setRepository(tmpModuleRepositories[i]);
			}
		}
		ois.close();
	}

}