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

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.dltk.ast.ASTNode;
import org.eclipse.dltk.compiler.problem.IProblem;
import org.eclipse.dltk.compiler.problem.IProblemReporter;
import org.eclipse.dltk.javascript.ast.Argument;
import org.eclipse.dltk.javascript.ast.ArrayInitializer;
import org.eclipse.dltk.javascript.ast.BinaryOperation;
import org.eclipse.dltk.javascript.ast.BooleanLiteral;
import org.eclipse.dltk.javascript.ast.CallExpression;
import org.eclipse.dltk.javascript.ast.Comment;
import org.eclipse.dltk.javascript.ast.ConstStatement;
import org.eclipse.dltk.javascript.ast.DecimalLiteral;
import org.eclipse.dltk.javascript.ast.Expression;
import org.eclipse.dltk.javascript.ast.FunctionStatement;
import org.eclipse.dltk.javascript.ast.IVariableStatement;
import org.eclipse.dltk.javascript.ast.Identifier;
import org.eclipse.dltk.javascript.ast.NewExpression;
import org.eclipse.dltk.javascript.ast.NullExpression;
import org.eclipse.dltk.javascript.ast.ObjectInitializer;
import org.eclipse.dltk.javascript.ast.Script;
import org.eclipse.dltk.javascript.ast.Statement;
import org.eclipse.dltk.javascript.ast.StringLiteral;
import org.eclipse.dltk.javascript.ast.UnaryOperation;
import org.eclipse.dltk.javascript.ast.VariableDeclaration;
import org.eclipse.dltk.javascript.ast.VoidExpression;
import org.eclipse.dltk.javascript.ast.v4.Keywords;
import org.eclipse.dltk.javascript.ast.v4.LetStatement;
import org.eclipse.dltk.javascript.parser.JavaScriptParserUtil;
import org.eclipse.dltk.javascript.parser.jsdoc.JSDocTag;
import org.eclipse.dltk.javascript.parser.jsdoc.JSDocTags;
import org.eclipse.dltk.javascript.parser.jsdoc.SimpleJSDocParser;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.builder.ErrorKeeper;
import com.servoy.eclipse.model.extensions.ICalculationTypeInferencer;
import com.servoy.eclipse.model.extensions.ICalculationTypeInferencerProvider;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.IFileAccess;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.AbstractRepository;
import com.servoy.j2db.persistence.AbstractScriptProvider;
import com.servoy.j2db.persistence.ArgumentType;
import com.servoy.j2db.persistence.ChangeHandler;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ContentSpec;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IDeveloperRepository;
import com.servoy.j2db.persistence.IItemChangeListener;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistVisitor;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IScriptElement;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.MethodArgument;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.RuntimeProperty;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.TableNode;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.ServoyJSONArray;
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * Reads repository (solution) objects from file system directories. (since we don't want to store the parent uuid in each element, we assume a certain aspects
 * from serializer. (such as directory structure and file locations)
 *
 * @author jblok
 */
public class SolutionDeserializer
{
	private static final String VARIABLE_TYPE_JSON_ATTRIBUTE = "variableType";
	private static final String JS_TYPE_JSON_ATTRIBUTE = "jsType";
	private static final String ARGUMENTS_JSON_ATTRIBUTE = "arguments";
	static final String LINE_NUMBER_OFFSET_JSON_ATTRIBUTE = "lineNumberOffset";
	static final String EXTRA_DOC_COMMENTS = "EXTRA_DOC_COMMENTS";
	static final String COMMENT_JSON_ATTRIBUTE = "comment";
	static final String DECL_KEYWORD = "keyword";
	private static final String CHANGED_JSON_ATTRIBUTE = "changed";

	public static final RuntimeProperty<Boolean> POSSIBLE_DUPLICATE_UUID = new RuntimeProperty<Boolean>()
	{

	};
	private final IDeveloperRepository repository;
	private final ErrorKeeper<File, String> errorKeeper;
	private static final Map<UUID, HashSet<UUID>> alreadyUsedUUID = new HashMap<UUID, HashSet<UUID>>(16, 0.9f);
	private static final Map<UUID, UUID> childToContainerUUID = new HashMap<UUID, UUID>(16, 0.9f);
	private final File jsFile;
	private final String jsContent;
	private ICalculationTypeInferencerProvider calculationTypeInferencerProvider;
	private ICalculationTypeInferencer calculationTypeInferencer;

	public SolutionDeserializer(IDeveloperRepository repository, ErrorKeeper<File, String> errorKeeper)
	{
		this.repository = repository;
		this.errorKeeper = errorKeeper;
		this.jsFile = null;
		this.jsContent = null;
	}

	public SolutionDeserializer(IDeveloperRepository repository, ErrorKeeper<File, String> errorKeeper, File jsFile, String jsContent)
	{
		this.repository = repository;
		this.errorKeeper = errorKeeper;
		this.jsFile = jsFile;
		this.jsContent = jsContent;
	}

	public static JSONObject getJSONObject(String content)
	{
		try
		{
			return new ServoyJSONObject(content, true);
		}
		catch (JSONException e)
		{
			ServoyLog.logError("Error created json object of: " + content, e);
		}
		return null;
	}

	public static JSONArray getJSONArray(String content)
	{
		try
		{
			return new ServoyJSONArray(content);
		}
		catch (JSONException e)
		{
			ServoyLog.logError("Error created json object of: " + content, e);
		}
		return null;
	}

	private static HashSet<UUID> getAlreadyUsedUUIDsForSolution(UUID solutionUUID)
	{
		HashSet<UUID> solutionUUIDs = alreadyUsedUUID.get(solutionUUID);
		if (solutionUUIDs == null)
		{
			solutionUUIDs = new HashSet<UUID>(512, 0.9f);
			alreadyUsedUUID.put(solutionUUID, solutionUUIDs);
		}

		return solutionUUIDs;
	}

	public Solution readSolution(File projectDir, SolutionMetaData smd, List<File> changedFiles, boolean useFilesForDirtyMark) throws RepositoryException
	{
		if (smd == null) return null;

		final Solution solution = (Solution)repository.createRootObject(smd);
		ChangeHandler handler = new ChangeHandler(repository);
		solution.setChangeHandler(handler);

		handler.addIPersistListener(new IItemChangeListener<IPersist>()
		{

			@Override
			public void itemRemoved(IPersist item)
			{
				UUID uuid = item.getUUID();
				HashSet<UUID> solutionUUIDs = alreadyUsedUUID.get(solution.getUUID());
				if (solutionUUIDs != null)
				{
					solutionUUIDs.remove(uuid);
				}
				childToContainerUUID.remove(uuid);
			}

			@Override
			public void itemCreated(IPersist item)
			{

			}

			@Override
			public void itemChanged(Collection<IPersist> items)
			{

			}

			@Override
			public void itemChanged(IPersist item)
			{

			}
		});
		HashSet<UUID> solutionUUIDs = getAlreadyUsedUUIDsForSolution(solution.getUUID());
		solutionUUIDs.clear();

		updateSolution(projectDir, solution, changedFiles, null, true, useFilesForDirtyMark, false, false, false);

		if (!useFilesForDirtyMark)
		{
			// clear all changed flags
			solution.acceptVisitor(new IPersistVisitor()
			{
				public Object visit(IPersist o)
				{
					o.clearChanged();
					return IPersistVisitor.CONTINUE_TRAVERSAL;
				}
			});
		}

		return solution;
	}

	/**
	 * Update the solution
	 *
	 * @param solutionDir
	 * @param solution
	 * @param changedFiles flag objects listed in these files as changed
	 * @param readAll when false, only read object listed in changedFileNames
	 * @return list of files in changedFileNames that have not been visited
	 * @throws RepositoryException
	 */
	public List<File> updateSolution(File solutionDir, final Solution solution, List<File> changedFiles, List<IPersist> strayCats, boolean readAll,
		boolean useFilesForDirtyMark, boolean shouldReset, boolean testExisting, boolean doCleanup) throws RepositoryException
	{
		if (solution == null) return null;
		try
		{
			if (errorKeeper != null)
			{
				errorKeeper.removeError(solutionDir);
			}
			List<File> changedFilesCopy = null;
			if (changedFiles != null)
			{
				if (errorKeeper != null)
				{
					for (File cf : changedFiles)
					{
						errorKeeper.removeError(cf);
					}
				}
				changedFilesCopy = new ArrayList<File>(changedFiles.size());
				changedFilesCopy.addAll(changedFiles);
			}
			Map<IPersist, JSONObject> persist_json_map = new HashMap<IPersist, JSONObject>();
			readObjectFilesFromSolutionDir(solutionDir, solutionDir, solution, persist_json_map, changedFilesCopy, strayCats, readAll, useFilesForDirtyMark,
				shouldReset, testExisting, doCleanup);
			readMediasFromSolutionDir(solutionDir, solution, persist_json_map, changedFilesCopy, strayCats, readAll, useFilesForDirtyMark, shouldReset,
				testExisting);
			completePersist(persist_json_map, useFilesForDirtyMark);
			solution.acceptVisitor(new IPersistVisitor()
			{
				public Object visit(IPersist o)
				{
					if (o.isChanged())
					{
						solution.getChangeHandler().fireIPersistChanged(o);
					}
					return CONTINUE_TRAVERSAL;
				}

			});

			// TODO isn't this done too soon? I mean a deleted form from disk for example would not yet be removed from the solution at this point right? (see the other TODO that was added in ServoyModel in the same commit)
			ModelUtils.updateSolutionServerProxies(solution, repository);

			return changedFilesCopy; // what remains of the day
		}
		catch (Exception e)
		{
			if (errorKeeper != null)
			{
				// get the innermost exception - the most relevant one for the user
				errorKeeper.addError(solutionDir, "Please check the .log file for more info.");
			}
			if (e instanceof RepositoryException)
			{
				throw (RepositoryException)e;
			}
			else
			{
				throw new RepositoryException(e);
			}
		}
		finally
		{
			// clear the UUID string/filename cache after loading of the solution.
			persistFileNameCache.clear();
		}
	}

	private void readObjectFilesFromSolutionDir(File solutionDir, File dir, ISupportChilds parent, Map<IPersist, JSONObject> persist_json_map,
		List<File> changedFiles, List<IPersist> strayCats, boolean readAll, boolean useFilesForDirtyMark, boolean shouldReset, boolean testExisting,
		boolean doCleanup)
		throws RepositoryException, JSONException
	{
		if (dir != null && dir.exists())
		{
			List<JSONObject> jsonObjects = new ArrayList<>();
			Map<JSONObject, File> fileMap = new HashMap<>();
			Map<File, String> formCssMap = new HashMap<>();
			List<File> scriptFiles = new ArrayList<>();
			List<File> subdirs = new ArrayList<>();
			String[] files = dir.list();
			Arrays.sort(files, new ObjBeforeJSExtensionComparator());
			Map<File, List<JSONObject>> childrenJSObjectMap = new HashMap<>(); // js objects from Form & TableNode
			Map<File, ISupportChilds> jsParentFileMap = new HashMap<>(); // keep which js files belong to which parent

			for (final String file : files)
			{
				File f = null;
				try
				{
					// root metadata and medias are read elsewhere
					if (dir.equals(solutionDir) && (file.equals(SolutionSerializer.MEDIAS_DIR) || file.equals(SolutionSerializer.MEDIAS_FILE) ||
						file.equals(SolutionSerializer.ROOT_METADATA)))
					{
						continue;
					}

					f = new File(dir, file);
					if (f.isDirectory())
					{
						subdirs.add(f);
					}
					else
					{
						boolean changed = isChangedFile(solutionDir, f, changedFiles);
						if (readAll || changed || hasSubEntries(f, changedFiles) || hasRelatedEntries(f, changedFiles))
						{
							boolean recognized = false;
							if (SolutionSerializer.isJSONFile(file))
							{
								JSONObject json_obj = new ServoyJSONObject(Utils.getTXTFileContent(f, Charset.forName("UTF8")), true);
								if (json_obj.length() == 0)
								{
									// empty file just skip this one.
									continue;
								}
								json_obj.put(CHANGED_JSON_ATTRIBUTE, changed);
								jsonObjects.add(json_obj);
								fileMap.put(json_obj, f);
								recognized = true;
							}
							else if (file.endsWith(SolutionSerializer.JS_FILE_EXTENSION))
							{
								List<JSONObject> scriptObjects = parseJSFile(f, changed, doCleanup);
								if (dir.equals(solutionDir))
								{
									// the scope name for global methods/variables is based on the filename
									String scopeName = file.substring(0, file.length() - SolutionSerializer.JS_FILE_EXTENSION.length());
									for (JSONObject so : scriptObjects)
									{
										so.put(StaticContentSpecLoader.PROPERTY_SCOPENAME.getPropertyName(), scopeName);
									}
								}
								File parentFile = f.getParentFile();
								if (parentFile.getName().equals(SolutionSerializer.FORMS_DIR) || (parentFile.getParentFile() != null &&
									parentFile.getParentFile().getName().equals(SolutionSerializer.DATASOURCES_DIR_NAME)))
								{
									childrenJSObjectMap.put(f, scriptObjects);
								}
								else
								{
									// old structure parsing
									if (!readAll)
									{
										testDuplicates(f, parent, scriptObjects);
									}
									if (scriptObjects != null)
									{
										jsonObjects.addAll(scriptObjects);
										scriptFiles.add(f);
										jsParentFileMap.put(f, parent);
									}
									if (scriptObjects != null)
									{
										for (JSONObject object : scriptObjects)
										{
											fileMap.put(object, f);
										}
									}
								}
								recognized = true;
							}
							else if (file.endsWith(".less") && f.getParentFile().getName().equals(SolutionSerializer.FORMS_DIR))
							{
								formCssMap.put(new File(f.getParentFile(), f.getName().replace(".less", SolutionSerializer.FORM_FILE_EXTENSION)),
									Utils.getTXTFileContent(f, Charset.forName("UTF8")));
								recognized = true;
							}
							if (changedFiles != null && recognized)
							{
								changedFiles.remove(f);
							}
						}
					}
					// skip all other files
				}
				catch (JSONException e)
				{
					// skip this file
					if (f != null && errorKeeper != null) errorKeeper.addError(f, e.getMessage());
					ServoyLog.logError("Invalid JSON syntax in file " + f, e);
				}
				catch (Exception e)
				{
					ServoyLog.logError("Error reading file " + f, e);
				}
			}

			Set<UUID> saved = new HashSet<UUID>();
			Map<File, IPersist> persistFileMap = new HashMap<File, IPersist>();
			for (JSONObject object : jsonObjects)
			{
				File file = fileMap.get(object);
				setMissingTypeOnScriptObject(object, parent, file);
				IPersist persist = null;
				try
				{
					persist = deserializePersist(repository, parent, persist_json_map, object, strayCats, file, saved, useFilesForDirtyMark, shouldReset,
						testExisting);
				}
				catch (JSONException e)
				{
					ServoyLog.logError("Could not read json object from file " + file + " -- skipping", e);
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError("Could not read json object from file " + file + " -- skipping", e);
				}
				if (persist != null)
				{
					saved.add(persist.getUUID());
					if (file != null)
					{
						persistFileMap.put(file, persist);
					}
				}
			}

			for (Entry<File, String> entry : formCssMap.entrySet())
			{
				File file = entry.getKey();
				IPersist persist = persistFileMap.get(file);
				if (persist instanceof Form form)
				{
					String css = entry.getValue();
					form.setFormCss(css);
				}
			}

			Entry<File, List<JSONObject>> childrenJSObjectMapEntry;
			File jsonFile, jsFile;
			for (Entry<File, List<JSONObject>> element : childrenJSObjectMap.entrySet())
			{
				childrenJSObjectMapEntry = element;
				jsFile = childrenJSObjectMapEntry.getKey();
				String jsFileName = jsFile.getName();
				try
				{
					if (jsFileName.endsWith(SolutionSerializer.JS_FILE_EXTENSION))
					{
						Pair<File, ISupportChilds> tmp = getJSONFileFromJS(jsFile, jsFileName, parent);
						jsonFile = tmp.getLeft();
						if (jsonFile == null)
						{
							errorKeeper.addError(jsFile, "Unrecognized javascript file name '" + jsFile.getName() + "'.");
							continue;
						}
						ISupportChilds scriptParent = tmp.getRight();
						if (scriptParent == null) // scriptParent may have been created when jsonfile does not exist
						{
							scriptParent = (ISupportChilds)persistFileMap.get(jsonFile);
						}
						if (scriptParent != null)
						{
							List<JSONObject> childrenJSObjects = childrenJSObjectMapEntry.getValue();
							if (!readAll)
							{
								testDuplicates(jsFile, scriptParent, childrenJSObjects);
							}
							if (childrenJSObjects != null)
							{
								scriptFiles.add(jsFile);
								for (JSONObject object : childrenJSObjects)
								{
									setMissingTypeOnScriptObject(object, scriptParent, jsFile);
									IPersist persist = null;
									try
									{
										persist = deserializePersist(repository, scriptParent, persist_json_map, object, strayCats, jsFile, saved,
											useFilesForDirtyMark, shouldReset, testExisting);
									}
									catch (JSONException e)
									{
										ServoyLog.logError("Could not read json object from file " + jsFile + " -- skipping", e);
									}
									catch (RepositoryException e)
									{
										ServoyLog.logError("Could not read json object from file " + jsFile + " -- skipping", e);
									}
									if (persist != null)
									{
										saved.add(persist.getUUID());
									}
								}
								if (jsFile != null)
								{
									jsParentFileMap.put(jsFile, scriptParent);
								}
							}
						}
						else
						{
							errorKeeper.addError(jsFile, "Invalid javascript file name '" + jsFile.getName() + "', doesn't have a corresponding object.");
						}
					}
				}
				catch (Exception e)
				{
					errorKeeper.addError(jsFile, "Error reading file " + jsFile);
					ServoyLog.logError("Error reading file " + jsFile, e);
				}
			}

			// check for lost children (stray cats blues)
			for (File scriptFile : scriptFiles)
			{
				if (jsParentFileMap.containsKey(scriptFile))
				{
					ISupportChilds jsParent = jsParentFileMap.get(scriptFile);
					for (IPersist child : Utils.asArray(jsParent.getAllObjects(), IPersist.class))
					{
						if (scriptFile.getName().equals(getFileName(child)) && !saved.contains(child.getUUID()))
						{
							jsParent.removeChild(child);
							if (strayCats != null)
							{
								strayCats.add(child);
							}
						}
					}
				}
			}

			// a parent that has children in a subdirectory will always have the same name as the directory,
			// example: forms/orders.obj describes the form and forms/orders/*.obj describe the elements.
			for (File subdir : subdirs)
			{
				// check for new parent
				File subdirPersistFile = new File(dir, subdir.getName() + SolutionSerializer.JSON_DEFAULT_FILE_EXTENSION);
				IPersist subdirPersist = persistFileMap.get(subdirPersistFile);
				if (subdirPersist != null || isContainerDir(parent, solutionDir, subdir))
				{
					ISupportChilds newParent = (subdirPersist instanceof ISupportChilds) ? (ISupportChilds)subdirPersist : parent;
					readObjectFilesFromSolutionDir(solutionDir, subdir, newParent, persist_json_map, changedFiles, strayCats, readAll, useFilesForDirtyMark,
						shouldReset, testExisting, doCleanup);
				}
			}
		}
	}

	public static Pair<File, ISupportChilds> getJSONFileFromJS(File jsFile, String jsFileName, ISupportChilds parent) throws RepositoryException
	{
		File jsonFile = null;
		ISupportChilds scriptParent = null;
		if (jsFile.getParentFile().getName().equals(SolutionSerializer.FORMS_DIR))
		{
			jsonFile = new File(jsFile.getParent(),
				jsFileName.substring(0, jsFileName.length() - SolutionSerializer.JS_FILE_EXTENSION.length()) + SolutionSerializer.FORM_FILE_EXTENSION);
		}
		else if (jsFile.getParentFile().getParentFile() != null &&
			jsFile.getParentFile().getParentFile().getName().equals(SolutionSerializer.DATASOURCES_DIR_NAME) &&
			(jsFileName.endsWith(SolutionSerializer.CALCULATIONS_POSTFIX) || jsFileName.endsWith(SolutionSerializer.FOUNDSET_POSTFIX)))
		{
			// tablenode
			if (jsFileName.endsWith(SolutionSerializer.CALCULATIONS_POSTFIX))
			{
				// calculations
				jsonFile = new File(jsFile.getParent(), jsFileName.substring(0, jsFileName.length() - SolutionSerializer.CALCULATIONS_POSTFIX.length()) +
					SolutionSerializer.TABLENODE_FILE_EXTENSION);
			}
			else
			// if (jsFileName.endsWith(SolutionSerializer.FOUNDSET_POSTFIX))
			{
				// foundset methods
				jsonFile = new File(jsFile.getParent(),
					jsFileName.substring(0, jsFileName.length() - SolutionSerializer.FOUNDSET_POSTFIX.length()) + SolutionSerializer.TABLENODE_FILE_EXTENSION);
			}
			if (!jsonFile.exists() && parent != null)
			{
				// tbl file does not exist yet, create a table node
				scriptParent = ((Solution)parent.getAncestor(IRepository.SOLUTIONS)).getOrCreateTableNode(
					DataSourceUtils.createDBTableDataSource(jsFile.getParentFile().getName(),
						jsonFile.getName().substring(0, jsonFile.getName().length() - SolutionSerializer.TABLENODE_FILE_EXTENSION.length())));
			}
		}

		return new Pair<File, ISupportChilds>(jsonFile, scriptParent);
	}

	/**
	 * @param parent
	 * @param subdir
	 * @return
	 */
	private boolean isContainerDir(ISupportChilds parent, File solutionDir, File subdir)
	{
		if (parent instanceof Solution)
		{
			File parentDir = subdir.getParentFile();
			if (parentDir.equals(solutionDir))
			{
				// main solution directory
				return SolutionSerializer.FORMS_DIR.equals(subdir.getName()) || SolutionSerializer.RELATIONS_DIR.equals(subdir.getName()) ||
					SolutionSerializer.VALUELISTS_DIR.equals(subdir.getName()) || SolutionSerializer.DATASOURCES_DIR_NAME.equals(subdir.getName()) ||
					SolutionSerializer.MENUS_DIR.equals(subdir.getName());
			}
			if (parentDir.getParentFile().equals(solutionDir) && SolutionSerializer.DATASOURCES_DIR_NAME.equals(parentDir.getName()))
			{
				// a subdirectory of the datasources directory (mysol/datasources/myserver) where table node files are stored
				return true;
			}
		}
		return false;
	}

	/**
	 * @param parent
	 * @param scriptObjects
	 */
	private void testDuplicates(File file, final ISupportChilds parent, List<JSONObject> scriptObjects)
	{
		if (scriptObjects != null && scriptObjects.size() > 0)
		{
			HashMap<String, JSONObject> uuidToJson = new HashMap<String, JSONObject>(scriptObjects.size());
			final HashMap<UUID, JSONObject> noParentDuplicates = new HashMap<UUID, JSONObject>(scriptObjects.size());
			for (JSONObject object : scriptObjects)
			{
				if (object.has(SolutionSerializer.PROP_UUID) && object.has(SolutionSerializer.PROP_NAME))
				{
					String uuid = object.optString(SolutionSerializer.PROP_UUID);
					UUID uuidObject = UUID.fromString(uuid);
					JSONObject duplicate = uuidToJson.put(uuid, object);
					if (duplicate != null)
					{
						noParentDuplicates.remove(uuidObject);
						try
						{
							IPersist persist = parent.getChild(uuidObject);
							if (persist instanceof ISupportName)
							{
								String name = ((ISupportName)persist).getName();
								if (duplicate.optString(SolutionSerializer.PROP_NAME).equals(name))
								{
									object.put(SolutionSerializer.PROP_UUID, UUID.randomUUID().toString());
									uuidToJson.put(uuid, duplicate);
								}
								else
								{
									duplicate.put(SolutionSerializer.PROP_UUID, UUID.randomUUID().toString());
								}
							}
							else
							{
								// if the uuid is a copy from a complete other place.
								// then just put 2 new uuid in it.
								object.put(SolutionSerializer.PROP_UUID, UUID.randomUUID().toString());
								duplicate.put(SolutionSerializer.PROP_UUID, UUID.randomUUID().toString());
							}
						}
						catch (JSONException e)
						{
							ServoyLog.logError(e);
						}
					}
					else
					{
						// search for duplicates for same parent in other files (same uuid in other global scope)
						IPersist child = parent.getChild(uuidObject);
						boolean updateUuid = child != null && !SolutionSerializer.isPersistWorkspaceFile(child, false, file);
						if (updateUuid)
						{
							Pair<String, String> pathPair = SolutionSerializer.getFilePath(child, false);
							Path path = new Path(pathPair.getLeft() + pathPair.getRight());
							// when old file does not exist this is a file rename, not a script method copy
							updateUuid = ResourcesPlugin.getWorkspace().getRoot().getFile(path).exists();
						}
						if (updateUuid)
						{
							// Found another child from different file, generate a new uuid for this one.
							uuidToJson.remove(uuid);
							uuidObject = UUID.randomUUID();
							uuidToJson.put(uuid = uuidObject.toString(), object);
							try
							{
								object.put(SolutionSerializer.PROP_UUID, uuid);
							}
							catch (JSONException e)
							{
								ServoyLog.logError(e);
							}
						}
						else
						{
							noParentDuplicates.put(uuidObject, object);
						}
					}
				}
			}
			if (noParentDuplicates.size() > 0 && ServoyModelFinder.getServoyModel().getActiveProject() != null)
			{
				for (ServoyProject servoyProject : ServoyModelFinder.getServoyModel().getModulesOfActiveProject())
				{
					Solution solution = servoyProject.getSolution();
					if (solution == null) continue;
					solution.acceptVisitor(new IPersistVisitor()
					{
						public Object visit(IPersist o)
						{
							if (o instanceof IScriptElement && !o.getParent().equals(parent))
							{
								JSONObject jsonObject = noParentDuplicates.remove(o.getUUID());
								if (jsonObject != null)
								{
									Pair<String, String> pathPair = SolutionSerializer.getFilePath(o, false);
									Path path = new Path(pathPair.getLeft() + pathPair.getRight());
									// when old file does not exist this is a file rename, not a script method copy
									if (ResourcesPlugin.getWorkspace().getRoot().getFile(path).exists())
									{
										try
										{
											jsonObject.put(SolutionSerializer.PROP_UUID, UUID.randomUUID().toString());
										}
										catch (JSONException e)
										{
											ServoyLog.logError(e);
										}
									}
								}
							}
							else if (o instanceof Solution || o instanceof Form || o instanceof TableNode)
							{
								return noParentDuplicates.size() == 0 ? null : IPersistVisitor.CONTINUE_TRAVERSAL;
							}
							return noParentDuplicates.size() == 0 ? null : IPersistVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
						}
					});
					if (noParentDuplicates.size() == 0) break;
				}
			}
		}
	}

	/**
	 * Check if file is a subdir-file and child elements are in the files list.
	 * <p>
	 * For example, file mysolution/forms/orders/button.obj is a sub-entry of mysolution/forms/orders.obj
	 *
	 * @param file
	 * @param files
	 * @return
	 */
	private boolean hasSubEntries(File file, List<File> files)
	{
		if (!file.getName().endsWith(SolutionSerializer.JSON_DEFAULT_FILE_EXTENSION)) return false;

		if (file.getName().equals(SolutionSerializer.SOLUTION_SETTINGS)) return true;

		String dirPath = file.getPath().substring(0, file.getPath().length() - SolutionSerializer.JSON_DEFAULT_FILE_EXTENSION.length()) + File.separatorChar;
		for (File f : files)
		{
			if (f.getPath().startsWith(dirPath))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Check if file is a form or tablenode file and child elements are in the files list.
	 * <p>
	 * For example, file mysolution/forms/orders.js is a sub-entry of mysolution/forms/orders.obj
	 *
	 * @param file
	 * @param files
	 * @return
	 */
	private boolean hasRelatedEntries(File file, List<File> files)
	{
		if (file.getName().endsWith(SolutionSerializer.FORM_FILE_EXTENSION))
		{
			String basePath = file.getPath().substring(0, file.getPath().length() - SolutionSerializer.FORM_FILE_EXTENSION.length());
			return containsPath(basePath + SolutionSerializer.JS_FILE_EXTENSION, files) ||
				containsPath(basePath + ".less", files);
		}

		if (file.getName().endsWith(SolutionSerializer.TABLENODE_FILE_EXTENSION))
		{
			String basePath = file.getPath().substring(0, file.getPath().length() - SolutionSerializer.TABLENODE_FILE_EXTENSION.length());
			return containsPath(basePath + SolutionSerializer.CALCULATIONS_POSTFIX, files) ||
				containsPath(basePath + SolutionSerializer.FOUNDSET_POSTFIX, files);
		}

		return false;
	}

	private static boolean containsPath(String path, List<File> files)
	{
		for (File f : files)
		{
			if (f.getPath().equals(path))
			{
				return true;
			}
		}
		return false;
	}

	private static void setMissingTypeOnScriptObject(JSONObject object, ISupportChilds parent, File jsFile) throws JSONException
	{
		int typeId;
		if (parent instanceof Form || parent instanceof Solution)
		{
			if (object.has("declaration"))
			{
				typeId = IRepository.METHODS;
			}
			else
			{
				typeId = IRepository.SCRIPTVARIABLES;
			}
		}
		else if (parent instanceof TableNode)
		{
			if (jsFile.getName().endsWith(SolutionSerializer.CALCULATIONS_POSTFIX))
			{
				typeId = IRepository.SCRIPTCALCULATIONS;
			}
			else if (jsFile.getName().endsWith(SolutionSerializer.FOUNDSET_POSTFIX) && object.has("declaration"))
			{
				typeId = IRepository.METHODS;
			}
			// cannot determine type from context
			else return;
		}
		// cannot determine type from context
		else return;

		if (object.has(SolutionSerializer.PROP_TYPEID))
		{
			// compare declared type id with determined one
			int declaredTypeId = object.getInt(SolutionSerializer.PROP_TYPEID);
			if (declaredTypeId != typeId)
			{
				// strange, type id is set, but different from what we would expect
				if ((declaredTypeId == IRepository.METHODS || declaredTypeId == IRepository.SCRIPTCALCULATIONS) && !object.has("declaration"))
				{
					object.put("declaration", "/* declaration missing */"); // scripts are expected to have a declaration,
				}
			}
		}
		else
		{
			// set missing type
			object.put(SolutionSerializer.PROP_TYPEID, typeId);
		}
	}

	private void readMediasFromSolutionDir(File dir, Solution parent, Map<IPersist, JSONObject> persist_json_map, List<File> changedFiles,
		List<IPersist> strayCats, boolean readAll, boolean useFilesForDirtyMark, boolean shouldReset, boolean testExisting) throws RepositoryException
	{
		if (dir != null && dir.exists())
		{
			File fmediasobjects = new File(dir, SolutionSerializer.MEDIAS_FILE);
			if (!readAll && (changedFiles == null /* || !changedFiles.contains(fmediasobjects) */)) // it is possible that only the media content has been updated
			{
				// no changes in medias
				return;
			}
			if (changedFiles != null)
			{
				changedFiles.remove(fmediasobjects);
			}

			Set<UUID> mediaUUIDS = new HashSet<UUID>();

			File mediasDir = new File(dir, SolutionSerializer.MEDIAS_DIR);
			String mediasobjects = Utils.getTXTFileContent(fmediasobjects);

			if (mediasobjects != null)
			{
				try
				{
					JSONArray array = new JSONArray(mediasobjects);
					for (int i = 0; i < array.length(); i++)
					{
						if (!array.isNull(i))
						{
							JSONObject obj = array.getJSONObject(i);
							String name = obj.has(SolutionSerializer.PROP_NAME) ? obj.getString(SolutionSerializer.PROP_NAME) : null;
							if (name != null)
							{
								boolean newMedia = parent.getMedia(name) == null;
								IPersist persist = deserializePersist(repository, parent, persist_json_map, obj, strayCats, mediasDir, new HashSet<UUID>(0),
									useFilesForDirtyMark, shouldReset, testExisting);
								if (persist instanceof Media)
								{
									File mf = new File(mediasDir, name);
									if (mf.exists())
									{
										mediaUUIDS.add(persist.getUUID());
										boolean changed = newMedia || isChangedFile(dir, mf, changedFiles);
										if (readAll || changed)
										{
											((Media)persist).setPermMediaData(Utils.getFileContent(mf));
											if (obj.has(SolutionSerializer.PROP_MIME_TYPE))
												((Media)persist).setMimeType(obj.getString(SolutionSerializer.PROP_MIME_TYPE));
											obj.put(CHANGED_JSON_ATTRIBUTE, changed);
											if (changed)
											{
												persist.flagChanged();
												if (changedFiles != null)
												{
													changedFiles.remove(mf);
												}
											}
										}
										else
										{
											persist_json_map.remove(persist);
										}
									}
								}
							}
						}
					}
				}
				catch (JSONException jsonex)
				{
					if (fmediasobjects != null && errorKeeper != null) errorKeeper.addError(fmediasobjects, jsonex.getMessage());
					ServoyLog.logError("Could not read medias.obj file " + fmediasobjects, jsonex);
					return;
				}
			}

			// find all media persists that are no longer listed in the medias.obj file
			for (IPersist media : Utils.asArray(parent.getMedias(false), Media.class))
			{
				if (!mediaUUIDS.contains(media.getUUID()))
				{
					parent.removeChild(media);
					if (strayCats != null)
					{
						strayCats.add(media);
					}
				}
			}

		}
	}

	private List<JSONObject> parseJSFile(final File file, boolean markAsChanged, boolean doCleanup) throws JSONException
	{
		String fileContent = jsContent;
		if (jsFile != file)
		{
			fileContent = Utils.getTXTFileContent(file, Charset.forName("UTF8"));
		}
		if (fileContent == null) return Collections.<JSONObject> emptyList();

		StringBuilder sbfileContent = null;
		int lastIndex = 0;
		for (int i = 0; i < fileContent.length(); i++)
		{
			if (fileContent.charAt(i) == '\r')
			{
				if (sbfileContent == null)
				{
					sbfileContent = new StringBuilder(fileContent.length());
				}
				sbfileContent.append(fileContent.substring(lastIndex, i));
				lastIndex = i + 1;
			}
		}
		if (sbfileContent != null)
		{
			sbfileContent.append(fileContent.substring(lastIndex));
			fileContent = sbfileContent.toString();
		}
		try
		{
			List<JSONObject> jsonObjects = new ArrayList<JSONObject>();
			final List<IProblem> problems = new ArrayList<IProblem>();
			IProblemReporter reporter = new IProblemReporter()
			{
				public void reportProblem(IProblem problem)
				{
					if (problem.isError())
					{
						problems.add(problem);
						errorKeeper.addError(file, problem.getMessage());
					}
				}
			};

			Script script = null;
			try
			{
				script = JavaScriptParserUtil.parse(fileContent, reporter);
			}
			catch (Throwable t)
			{
				Debug.error(t);
				Debug.error("Parse error with file: " + file +
					", please check this file for deep recursion like large string concats! ( string + string + string, replace this with string \\ string)");
				ServoyLog.logError("Parse error with file: " + file +
					", please check this file for deep recursion like large string concats! ( string + string + string, replace this with string \\ string)",
					t);
			}
			if (problems.size() > 0)
			{
				if (Debug.tracing()) Debug.trace(
					"Didn't update the Persist model Script and Variables objects because of problems " + problems + " in file: " + file.getAbsolutePath());
				return Collections.<JSONObject> emptyList();
			}
			if (script == null)
			{
				Debug.error("No script returned when parsing " + file.getAbsolutePath());
				ServoyLog.logError("No script returned when parsing " + file.getAbsolutePath(), null);
				return null;
			}


			List<VariableDeclaration> variables = new ArrayList<VariableDeclaration>();
			List<FunctionStatement> functions = new ArrayList<FunctionStatement>();
			List<Statement> statements = script.getStatements();
			List<Comment> comments = script.getComments();
			Set<Comment> sortedComments = new TreeSet<Comment>(new Comparator<Comment>()
			{
				@Override
				public int compare(Comment o1, Comment o2)
				{
					return o1.start() - o2.start();
				}
			});
			sortedComments.addAll(comments);
			for (ASTNode node : statements)
			{
				if (node instanceof VoidExpression)
				{
					Expression exp = ((VoidExpression)node).getExpression();
					if (exp instanceof IVariableStatement)
					{
						Comment doc = exp.getDocumentation();
						if (doc != null) sortedComments.remove(doc);
						List<VariableDeclaration> vars = ((IVariableStatement)exp).getVariables();
						for (VariableDeclaration var : vars)
						{
							doc = var.getDocumentation();
							if (doc != null) sortedComments.remove(doc);
						}
						Iterator<Comment> it = sortedComments.iterator();
						while (it.hasNext())
						{
							doc = it.next();
							if (doc.sourceStart() > exp.sourceStart() && doc.sourceEnd() < exp.sourceEnd()) it.remove();
						}
						variables.addAll(vars);
					}
					else if (exp instanceof FunctionStatement)
					{
						Comment doc = exp.getDocumentation();
						if (doc != null) sortedComments.remove(doc);
						Iterator<Comment> it = sortedComments.iterator();
						while (it.hasNext())
						{
							doc = it.next();
							if (doc.sourceStart() > exp.sourceStart() && doc.sourceEnd() < exp.sourceEnd()) it.remove();
						}
						functions.add((FunctionStatement)exp);
					}
				}
			}

			final IFile resource = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(Path.fromOSString(file.getAbsolutePath()));

			List<Line> lines = new ArrayList<Line>();
			int counter = 0;
			Line currentLine = new Line(0, 0);
			lines.add(currentLine);
			while (counter < fileContent.length())
			{
				if (fileContent.charAt(counter) == '\n')
				{
					currentLine = new Line(currentLine.line + 1, counter + 1);
					lines.add(currentLine);
				}
				counter++;
			}
			//add an extra last line.
			lines.add(new Line(currentLine.line + 1, counter + 1));


			for (VariableDeclaration field : variables)
			{
				Comment comment = field.getDocumentation();
				if (comment == null && field.getParent() instanceof IVariableStatement)
				{
					comment = ((IVariableStatement)field.getParent()).getDocumentation();
				}

				boolean newField = true;
				JSONObject json = null;
				String commentString = "";
				if (comment != null)
				{
					commentString = comment.getText();
					int prop_idx = commentString.indexOf(SolutionSerializer.PROPERTIESKEY);
					if (prop_idx != -1)
					{
						int prop_newline_idx = commentString.indexOf('}', prop_idx);
						if (prop_newline_idx < commentString.length() && prop_newline_idx >= prop_idx + SolutionSerializer.PROPERTIESKEY.length())
						{
							String sobj = commentString.substring(prop_idx + SolutionSerializer.PROPERTIESKEY.length(), prop_newline_idx + 1);
							json = new ServoyJSONObject(sobj, false);
							newField = false;
						}
						else
						{
							ServoyLog.logError("Invalid properties comment, ignoring:\n" + comment.getText(), null);
						}
					}
				}
				if (json == null)
				{
					json = new ServoyJSONObject();
				}

				Identifier ident = field.getIdentifier();
				json.put(SolutionSerializer.PROP_NAME, ident.getName());
				Expression code = field.getInitializer();
				if (commentString.length() > 0)
				{
					int typeIndex = commentString.indexOf(SolutionSerializer.TYPEKEY);
					if (typeIndex != -1)
					{
						int newLine = commentString.indexOf('\n', typeIndex);
						if (newLine > 0)
						{
							String typeName = commentString.substring(typeIndex + SolutionSerializer.TYPEKEY.length(), newLine).trim();
							// don't touch the special object types that start with {{
							if (!typeName.startsWith("{{"))
							{
								if (typeName.startsWith("{") && typeName.endsWith("}"))
								{
									typeName = typeName.substring(1, typeName.length() - 1);
								}
							}
							json.putOpt(JS_TYPE_JSON_ATTRIBUTE, typeName);
							int servoyType = getServoyType(typeName);
							if (servoyType == IColumnTypes.NUMBER)
							{
								int currentType = json.optInt(VARIABLE_TYPE_JSON_ATTRIBUTE, servoyType);
								if (currentType != IColumnTypes.INTEGER) json.put(VARIABLE_TYPE_JSON_ATTRIBUTE, servoyType);
							}
							else
							{
								json.put(VARIABLE_TYPE_JSON_ATTRIBUTE, servoyType);
							}
						}
					}
				}
				if (field.getParent() instanceof ConstStatement || field.getParent() instanceof LetStatement)
				{
					json.put(DECL_KEYWORD, field.getParent() instanceof ConstStatement ? Keywords.CONST : Keywords.LET);
				}

				if (code != null)
				{
					String value_part = fileContent.substring(code.sourceStart(), code.sourceEnd());
					if (value_part.endsWith(";")) value_part = value_part.substring(0, value_part.length() - 1);
					if (code instanceof UnaryOperation)
					{
						code = ((UnaryOperation)code).getExpression();
					}
					if (code instanceof DecimalLiteral)
					{
						int variableType = Column.mapToDefaultType(json.optInt(VARIABLE_TYPE_JSON_ATTRIBUTE, IColumnTypes.TEXT));
						json.put(VARIABLE_TYPE_JSON_ATTRIBUTE, variableType);
						try
						{
							Integer.parseInt(value_part);
							if (variableType != IColumnTypes.NUMBER) json.put(VARIABLE_TYPE_JSON_ATTRIBUTE, IColumnTypes.INTEGER);
						}
						catch (NumberFormatException e)
						{
							try
							{
								Double.parseDouble(value_part);
								json.put(VARIABLE_TYPE_JSON_ATTRIBUTE, IColumnTypes.NUMBER);
							}
							catch (NumberFormatException e2)
							{
								// ignore shouldnt happen
								if (json.has(VARIABLE_TYPE_JSON_ATTRIBUTE))
								{
									if (variableType == IColumnTypes.INTEGER || variableType == IColumnTypes.NUMBER)
									{
										json.remove(VARIABLE_TYPE_JSON_ATTRIBUTE);
									}
								}
							}
						}
					}
					else if (code instanceof StringLiteral)
					{
						json.put(VARIABLE_TYPE_JSON_ATTRIBUTE, IColumnTypes.TEXT);
					}
					else if (code instanceof NullExpression)
					{
						if (newField)
						{
							String typeName = json.optString(JS_TYPE_JSON_ATTRIBUTE, null);
							if (typeName != null)
							{
								json.put(VARIABLE_TYPE_JSON_ATTRIBUTE, getServoyType(typeName));
							}
							else
							{
								json.put(VARIABLE_TYPE_JSON_ATTRIBUTE, IColumnTypes.MEDIA);
							}
						}
					}
					else if (code instanceof CallExpression || code instanceof NewExpression || code instanceof FunctionStatement)
					{
						ASTNode callExpression = code;
						if (callExpression instanceof CallExpression)
						{
							callExpression = ((CallExpression)callExpression).getExpression();
						}
						String objectclass = null;
						if (callExpression instanceof NewExpression)
						{
							Expression objectClassExpression = ((NewExpression)callExpression).getObjectClass();
							if (objectClassExpression instanceof Identifier)
							{
								objectclass = ((Identifier)objectClassExpression).getName();
							}
							else if (objectClassExpression instanceof CallExpression &&
								((CallExpression)objectClassExpression).getExpression() instanceof Identifier)
							{
								objectclass = ((Identifier)((CallExpression)objectClassExpression).getExpression()).getName();
							}
						}
						if ("String".equals(objectclass))
						{
							json.put(VARIABLE_TYPE_JSON_ATTRIBUTE, IColumnTypes.TEXT);
						}
						else if ("Date".equals(objectclass))
						{
							json.put(VARIABLE_TYPE_JSON_ATTRIBUTE, IColumnTypes.DATETIME);
						}
						else if ("Array".equals(objectclass))
						{
							json.put(VARIABLE_TYPE_JSON_ATTRIBUTE, IColumnTypes.MEDIA);
							String current = json.optString(JS_TYPE_JSON_ATTRIBUTE, null);
							if (doCleanup && (current == null || (!current.startsWith("Array") && !current.endsWith("[]"))))
								json.putOpt(JS_TYPE_JSON_ATTRIBUTE, "Array");
						}
						else
						{
							String typeName = json.optString(JS_TYPE_JSON_ATTRIBUTE, null);
							if (typeName != null)
							{
								json.put(VARIABLE_TYPE_JSON_ATTRIBUTE, getServoyType(typeName));
							}
							else
							{
								if (objectclass != null)
								{
									json.putOpt(JS_TYPE_JSON_ATTRIBUTE, objectclass);
								}
								json.put(VARIABLE_TYPE_JSON_ATTRIBUTE, IColumnTypes.MEDIA);
							}
						}
					}
					else if (code instanceof ObjectInitializer)
					{
						json.put(VARIABLE_TYPE_JSON_ATTRIBUTE, IColumnTypes.MEDIA);
					}
					else if (code instanceof ArrayInitializer)
					{
						json.put(VARIABLE_TYPE_JSON_ATTRIBUTE, IColumnTypes.MEDIA);
						String current = json.optString(JS_TYPE_JSON_ATTRIBUTE, null);
						if (doCleanup && (current == null || (!current.startsWith("{Array") && !current.startsWith("Array")) && !current.endsWith("[]")))
						{
							List<Expression> items = ((ArrayInitializer)code).getItems();
							if (items != null && items.size() > 0)
							{
								boolean isString = true;
								boolean isNumber = true;
								for (Expression item : items)
								{
									if (!(item instanceof StringLiteral))
									{
										isString = false;
									}
									if (!(item instanceof DecimalLiteral))
									{
										isNumber = false;
									}
								}
								if (isString)
								{
									json.putOpt(JS_TYPE_JSON_ATTRIBUTE, "Array<String>");
								}
								else if (isNumber)
								{
									json.putOpt(JS_TYPE_JSON_ATTRIBUTE, "Array<Number>");
								}
							}

						}
					}
					else if (code instanceof BooleanLiteral)
					{
						json.put(VARIABLE_TYPE_JSON_ATTRIBUTE, IColumnTypes.MEDIA);
						if (doCleanup) json.putOpt(JS_TYPE_JSON_ATTRIBUTE, "Boolean");
					}
					else if (code instanceof BinaryOperation && json.opt(JS_TYPE_JSON_ATTRIBUTE) == null)
					{
						Expression le = ((BinaryOperation)code).getLeftExpression();
						Expression re = ((BinaryOperation)code).getRightExpression();
						if (le instanceof StringLiteral || re instanceof StringLiteral)
						{
							json.put(VARIABLE_TYPE_JSON_ATTRIBUTE, IColumnTypes.TEXT);
							if (doCleanup) json.putOpt(JS_TYPE_JSON_ATTRIBUTE, "String");
						}
						else if (le instanceof DecimalLiteral || re instanceof DecimalLiteral)
						{
							json.put(VARIABLE_TYPE_JSON_ATTRIBUTE, IColumnTypes.NUMBER);
							if (doCleanup) json.putOpt(JS_TYPE_JSON_ATTRIBUTE, "Number");
						}
					}
					else
					{
						// only fall back to media if the jstype is not set, else keep the jstype that is specified in the doc
						if (json.opt(JS_TYPE_JSON_ATTRIBUTE) == null)
						{
							Debug.log("Unknow expression falling back to media: " + code.getClass());
							json.put(VARIABLE_TYPE_JSON_ATTRIBUTE, IColumnTypes.MEDIA);
						}
						else
						{
							int current = json.optInt(VARIABLE_TYPE_JSON_ATTRIBUTE, -1);
							int servoyType = getServoyType(json.optString(JS_TYPE_JSON_ATTRIBUTE));
							if (current == -1 || !((servoyType == IColumnTypes.NUMBER || servoyType == IColumnTypes.INTEGER) &&
								(current == IColumnTypes.NUMBER || current == IColumnTypes.INTEGER)))
							{
								json.put(VARIABLE_TYPE_JSON_ATTRIBUTE, servoyType);
							}
						}
					}
					json.put("defaultValue", value_part);
				}
				else
				{
					json.put("defaultValue", "");
				}

				int linenr = 1;
				int fieldLineIndex = field.sourceStart();
				for (Line line : lines)
				{
					if (line.start > fieldLineIndex)
					{
						linenr = line.line;
						break;
					}
				}

				json.put(LINE_NUMBER_OFFSET_JSON_ATTRIBUTE, linenr);
				json.put(COMMENT_JSON_ATTRIBUTE, commentString);
				json.put(CHANGED_JSON_ATTRIBUTE, markAsChanged);
				jsonObjects.add(json);
			}

			for (FunctionStatement function : functions)
			{
				String comment = null;
				boolean hasDocs = false;
				int servoyType = -1;
				if (function.getDocumentation() != null)
				{
					comment = function.getDocumentation().getText();
					hasDocs = true;
				}

				if (resource.getName().endsWith(SolutionSerializer.CALCULATIONS_POSTFIX) && resource.getParent() != null &&
					resource.getParent().getParent() != null &&
					resource.getParent().getParent().getName().equals(SolutionSerializer.DATASOURCES_DIR_NAME) && !hasDocs)
				{

					if (calculationTypeInferencer == null)
					{
						calculationTypeInferencer = getCalculationTypeInferencer(script, resource);
					}
					if (calculationTypeInferencer != null)
					{

						List<String> types = calculationTypeInferencer.getReturnedType(function.getName().getName());
						if (types != null)
						{
							servoyType = IColumnTypes.MEDIA;//default
							for (String type : types)
							{
								servoyType = getServoyType(type);
								if (servoyType != IColumnTypes.MEDIA) break;
							}
						}
					}
					else
					{
						ServoyLog.logWarning("Could not get calculation type inferencer to set the returned type for " + function.getName().getName(), null);
					}
				}
				JSONObject json = null;
				if (comment != null)
				{
					if (!comment.startsWith("/**") && comment.startsWith("/*"))
					{
						comment = "/*" + comment.substring(1);
					}
					int prop_idx = comment.indexOf(SolutionSerializer.PROPERTIESKEY);
					if (prop_idx != -1)
					{
						int prop_newline_idx = comment.indexOf('}', prop_idx);
						if (prop_newline_idx < comment.length() && prop_newline_idx >= prop_idx + SolutionSerializer.PROPERTIESKEY.length())
						{
							String sobj = comment.substring(prop_idx + SolutionSerializer.PROPERTIESKEY.length(), prop_newline_idx + 1);
							if (sobj.indexOf(VARIABLE_TYPE_JSON_ATTRIBUTE) == -1)
							{
								json = new ServoyJSONObject(sobj, false);
							}
							else json = new ServoyJSONObject();
						}
						else
						{
							ServoyLog.logError("Invalid properties comment, ignoring:\n" + comment, null);
						}
					}
				}
				else
				{
					comment = "";
				}
				if (json == null)
				{
					json = new ServoyJSONObject();
				}

				json.put(SolutionSerializer.PROP_NAME, function.getName().getName());
				if (servoyType != -1)
				{
					json.put("type", servoyType);
				}

				String source = fileContent.substring(function.sourceStart(), function.sourceEnd());
				if ("".equals(comment) && (source.indexOf(".search") != -1 || source.indexOf("controller.loadAllRecords") != -1))
				{
					comment = "/**\n * @AllowToRunInFind\n */\n";
				}
				if ("".equals(comment))
				{
					json.put("declaration", source + '\n');
				}
				else
				{
					if (comment.indexOf("@AllowToRunInFind") == -1 && (source.indexOf(".search") != -1 || source.indexOf("controller.loadAllRecords") != -1))
					{
						int endComment = comment.lastIndexOf("*/");
						int lastNewLine = comment.lastIndexOf("\n", endComment);
						comment = comment.substring(0, lastNewLine + 1) + comment.substring(lastNewLine + 1, endComment) + "* @AllowToRunInFind\n" +
							comment.substring(lastNewLine + 1);
					}
					json.put("declaration", comment.trim() + '\n' + source + '\n');
				}
//				json.put("filename", file.getAbsolutePath());


				int linenr = 1;
				int functionLineIndex = function.sourceStart();
				for (Line line : lines)
				{
					if (line.start > functionLineIndex)
					{
						linenr = line.line;
						break;
					}
				}

				json.put(ARGUMENTS_JSON_ATTRIBUTE, (Object)function.getArguments());
				json.put(LINE_NUMBER_OFFSET_JSON_ATTRIBUTE, linenr);
				json.put(COMMENT_JSON_ATTRIBUTE, comment);
				json.put(CHANGED_JSON_ATTRIBUTE, markAsChanged);
				jsonObjects.add(json);
			}
			calculationTypeInferencer = null;//clear
			if (jsonObjects.size() > 0)
			{
				JSONArray array = new JSONArray();
				for (Comment comment : sortedComments)
				{
					JSONObject object = new JSONObject();
					object.put("start", comment.sourceStart());
					object.put("end", comment.sourceEnd());
					object.put("text", comment.getText());
					int linenr = 1;
					int commentLineIndex = comment.sourceStart();
					for (Line line : lines)
					{
						if (line.start > commentLineIndex)
						{
							linenr = line.line;
							break;
						}
					}
					object.put("linenr", linenr);
					array.put(object);
				}
				jsonObjects.get(0).put(EXTRA_DOC_COMMENTS, array.toString());
			}
			return jsonObjects;
		}
		catch (RuntimeException e)
		{
			// if there is a runtime exception throw then something in the parsing did go wrong.
			// then this js file will be skipped.
			ServoyLog.logWarning("Javascript file '" + file + "' had a parsing error ", e);
		}
		return null;
	}

	/**
	 * @param json
	 * @param name
	 * @throws JSONException
	 */
	private int getServoyType(String name)
	{
		if ("String".equalsIgnoreCase(name))
		{
			return IColumnTypes.TEXT;
		}
		else if ("Date".equals(name))
		{
			return IColumnTypes.DATETIME;
		}
		else if ("Number".equalsIgnoreCase(name))
		{
			return IColumnTypes.NUMBER;
		}
		else if ("Integer".equals(name))
		{
			return IColumnTypes.INTEGER;
		}
		else
		{
			return IColumnTypes.MEDIA;
		}
	}

	// cache for expensive UUID->string creation.
	private static final Map<IPersist, String> persistFileNameCache = new HashMap<IPersist, String>(512, 0.9f);

	private static String getFileName(IPersist persist)
	{
		String filename = persistFileNameCache.get(persist);
		if (filename == null)
		{
			filename = SolutionSerializer.getFileName(persist, false);
			persistFileNameCache.put(persist, filename);
		}
		return filename;
	}


	/**
	 * Deserialize JSONObject obj into parent persist.
	 *
	 * @param repository
	 * @param parent
	 * @param persist_json_map
	 * @param obj
	 * @param strayCats
	 * @param file
	 * @param saved
	 * @param useFilesForDirtyMark
	 * @return
	 * @throws RepositoryException
	 * @throws JSONException
	 */
	public static IPersist deserializePersist(IDeveloperRepository repository, final ISupportChilds parent, Map<IPersist, JSONObject> persist_json_map,
		JSONObject obj, final List<IPersist> strayCats, File file, Set<UUID> saved, boolean useFilesForDirtyMark, boolean shouldReset, boolean testExisting)
		throws RepositoryException, JSONException
	{
		if (!obj.has(SolutionSerializer.PROP_TYPEID))
		{
			ServoyLog.logError("The json object couldnt be deserialized into a persist: " + obj + " on parent: " + parent, null);
			return null;
		}

		HashSet<UUID> solutionUUIDs = getAlreadyUsedUUIDsForSolution(parent.getRootObject().getUUID());

		IPersist existingNode = null;
		UUID uuid;
		boolean persistUUIDNotFound = false;
		if (obj.has(SolutionSerializer.PROP_UUID))
		{
			try
			{
				uuid = UUID.fromString(obj.getString(SolutionSerializer.PROP_UUID));
			}
			catch (Exception e)
			{
				// object has corrupt uuid, generate a new one so that the object can at least be saved
				ServoyLog.logError("Could not parse UUID -- generating new uuid", e);
				uuid = UUID.randomUUID();
			}
			existingNode = testExisting ? AbstractRepository.searchPersist(parent, uuid, parent)
				: parent != null && parent.getUUID().equals(uuid) ? parent : null;

			if (existingNode == null)
			{
				if (file != null && !file.getPath().endsWith(SolutionSerializer.JS_FILE_EXTENSION) && !SolutionSerializer.isCompositeWithItems(parent))
				{
					// check if another persists exists linked to the same file, this can happen when the uuid has been updated
					// Note that this is only applicable if the persist has its own file
					final String fileName = file.getName();
					final String parentDirName = file.getParentFile().getName();
					final String parentRelativePath = SolutionSerializer.getRelativePath(parent, false);
					IPersist persistInSameFile = (IPersist)parent.acceptVisitor(new IPersistVisitor()
					{
						public Object visit(IPersist o)
						{
							if (o == parent)
							{
								return IPersistVisitor.CONTINUE_TRAVERSAL;
							}

							// if this persist is going to be updated by these same changes already, do use the new name (it might change right now);
							// for example f1 renamed into f2 in the same commit where f2 was renamed into f1
							JSONObject oIsGoingToBeUpdatedWithThisJSON = persist_json_map.get(o);
							String fileNameOfO;
							if (oIsGoingToBeUpdatedWithThisJSON != null && oIsGoingToBeUpdatedWithThisJSON.has(SolutionSerializer.PROP_NAME))
								fileNameOfO = SolutionSerializer.appendExtensionToFileName(o.getTypeID(),
									oIsGoingToBeUpdatedWithThisJSON.getString(SolutionSerializer.PROP_NAME));
							else fileNameOfO = getFileName(o);

							if (fileName.equals(fileNameOfO))
							{
								String relativePath = SolutionSerializer.getRelativePath(o, false);
								if (relativePath.replace(parentRelativePath, "").startsWith(parentDirName))
								{
									// must make sure also the same parent dir
									// updated persist in same file
									return o;
								}
							}
							// just check the immediate children only
							return IPersistVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
						}
					});
					if (persistInSameFile != null)
					{
						// updated persist in same file (uuid changed), add to strayCats and remove
						persistInSameFile.getParent().removeChild(persistInSameFile);
						if (strayCats != null) strayCats.add(persistInSameFile);
					}
				}
				else
				{
					persistUUIDNotFound = true;
				}
			}
			else
			{
				// ok so there is a persist with the same UUID is already present for this file that was changed; new JSON will be parsed later (via what is set in 'persist_json_map') and updated in this persist but
				// check if the persist with the UUID was actually renamed and overwrites now another existing persist with a different UUID but with the same name as this persist's new name;
				// if that happened then the other persist that was overridden needs to be removed from the solution and we should put it in 'strayCats';
				// FOR EXAMPLE you have formA and formB in the same solution under git or whatever; then someone else deletes formA and renames formB to formA and pushes to git; then you pull
				// formA will be the changed file but it's UUID will match existing persist formB; we must make sure that formA and it's child persists do get removed in this situation
				String newNameOfExistingPersistInSameParent = obj.optString(SolutionSerializer.PROP_NAME, null);
				if (newNameOfExistingPersistInSameParent != null)
				{
					String oldNameOfExistingPersistInSameParent = existingNode instanceof ISupportName ? ((ISupportName)existingNode).getName() : null;
					if (!newNameOfExistingPersistInSameParent.equals(oldNameOfExistingPersistInSameParent))
					{
						// so the persist with this UUID was renamed; check if it did overwrite something else (check if we have a old different (uuid) persist with the same name as new name)
						// we are checking agains the same type of children (so that the files can overwrite each other...)
						int objectTypeId = obj.getInt(SolutionSerializer.PROP_TYPEID);
						Iterator<IPersist> allObjects = parent.getObjects(objectTypeId);
						while (allObjects.hasNext())
						{
							IPersist someOtherPersistOfSameTypeInSameParent = allObjects.next();
							if (someOtherPersistOfSameTypeInSameParent instanceof ISupportName &&
								newNameOfExistingPersistInSameParent.equals(((ISupportName)someOtherPersistOfSameTypeInSameParent).getName()) &&
								SolutionSerializer.isPersistWorkspaceFile(someOtherPersistOfSameTypeInSameParent, false, file))
							{
								// bingo, it did replace some other persist when it was renamed; make sure to delete that...
								someOtherPersistOfSameTypeInSameParent.getParent().removeChild(someOtherPersistOfSameTypeInSameParent);
								if (strayCats != null)
								{
									strayCats.add(someOtherPersistOfSameTypeInSameParent); // remember that it is removed so that it can be removed later from editing solution as well
								}
								break; // no need to iterate further; we found it - also we removed it so it would lead to a ConcurrentModifEx
							}
						}
					}
				}
			}
		}
		else
		{
			uuid = UUID.randomUUID();
		}

		IPersist retval = null;
		if (existingNode == null)
		{
			// check if there is already a child with that name and not with that uuid (then it is an incoming uuid change)
			// so that child should be used. Else we will have 2 childs with the same name but different uuids.
			if (!obj.isNull(SolutionSerializer.PROP_NAME) && obj.has(SolutionSerializer.PROP_UUID))
			{
				int objectTypeId = obj.getInt(SolutionSerializer.PROP_TYPEID);
				String name = obj.getString(SolutionSerializer.PROP_NAME);
				Iterator<IPersist> allObjects = parent.getAllObjects();
				while (allObjects.hasNext())
				{
					IPersist persist = allObjects.next();
					if (persist.getTypeID() == objectTypeId && persist instanceof ISupportName && name.equals(((ISupportName)persist).getName()) &&
						!persist_json_map.containsKey(persist) && SolutionSerializer.isPersistWorkspaceFile(persist, false, file))
					{
						// object with same name and other uuid found in same file
						// when found in other file (like different scope.js file), the uuid should not be reused because the found persist is defined in another scope.
						retval = persist;
						if (persistUUIDNotFound)
						{
							// scriptUUID wasnt found previously. let this persist that maps with its name use the uuid from the file
							// so that overwrite and update from a team provider really overwrites it
							if (shouldReset) ((AbstractBase)persist).resetUUID(uuid);
							((AbstractBase)persist).setRuntimeProperty(POSSIBLE_DUPLICATE_UUID, Boolean.TRUE);
						}
						break;
					}
				}
			}
			if (retval == null)
			{
				retval = createPersistInParent(parent, repository, obj, uuid);
				if (persistUUIDNotFound && childToContainerUUID.get(uuid) != null &&
					!Utils.equalObjects(getContainerUUID(retval), childToContainerUUID.get(uuid)))
				{
					if (shouldReset) ((AbstractBase)retval).resetUUID();
				}
				if (persistUUIDNotFound && solutionUUIDs.contains(uuid))
				{
					((AbstractBase)retval).setRuntimeProperty(POSSIBLE_DUPLICATE_UUID, Boolean.TRUE);
				}
			}
		}
		else
		{
			retval = existingNode;
			String fileName = getFileName(retval);
			if (file != null && !fileName.equals(file.getName()) && SolutionSerializer.isJSONFile(fileName))
			{
				((AbstractBase)retval).setRuntimeProperty(POSSIBLE_DUPLICATE_UUID, Boolean.TRUE);
			}
		}

		solutionUUIDs.add(retval.getUUID());
		childToContainerUUID.put(retval.getUUID(), getContainerUUID(retval));

		if (file != null)
		{
			((AbstractBase)retval).setSerializableRuntimeProperty(IScriptProvider.FILENAME, file.getAbsolutePath());
		}
		if (!persist_json_map.containsKey(retval) || obj.has(SolutionSerializer.PROP_UUID)) persist_json_map.put(retval, obj);

		if (retval instanceof ISupportChilds && SolutionSerializer.isCompositeWithItems(retval))
		{
			Set<UUID> newChildUUIDs = new HashSet<UUID>();
			if (obj.has(SolutionSerializer.PROP_ITEMS))
			{
				JSONArray items = obj.getJSONArray(SolutionSerializer.PROP_ITEMS);
				for (int i = 0; i < items.length(); i++)
				{
					JSONObject child_obj = items.getJSONObject(i);
					if (SolutionSerializer.isCompositeWithItems(retval) && obj.has(CHANGED_JSON_ATTRIBUTE) &&
						Utils.getAsBoolean(obj.get(CHANGED_JSON_ATTRIBUTE))) child_obj.put(CHANGED_JSON_ATTRIBUTE, true);

					IPersist newChild = deserializePersist(repository, (ISupportChilds)retval, persist_json_map, child_obj, strayCats, file, saved,
						useFilesForDirtyMark, shouldReset, testExisting);
					if (newChild != null) newChildUUIDs.add(newChild.getUUID());
				}
			}

			List<IPersist> itemsToRemove = new ArrayList<IPersist>();
			// check for lost children
			Iterator<IPersist> it = ((ISupportChilds)retval).getAllObjects();
			while (it.hasNext())
			{
				IPersist ch = it.next();
				if (!newChildUUIDs.contains(ch.getUUID()) && SolutionSerializer.isCompositeItem(ch))
				{
					if (strayCats != null)
					{
						strayCats.add(ch);
					}
					itemsToRemove.add(ch);
					//it.remove() cannot remove on unmodifiable list, should use removeChild later on
				}
			}
			for (IPersist persist : itemsToRemove)
			{
				persist.getParent().removeChild(persist);
			}
		}

		if (useFilesForDirtyMark) handleChanged(obj, retval);
		return retval;
	}

	private static UUID getContainerUUID(IPersist persist)
	{
		Form form = (Form)persist.getAncestor(IRepository.FORMS);
		if (form != null)
		{
			return form.getUUID();
		}
		if (persist.getRootObject() instanceof Solution)
		{
			return ((Solution)persist.getRootObject()).getUUID();
		}
		return persist.getParent().getUUID();
	}

	private static IPersist createPersistInParent(ISupportChilds parent, IDeveloperRepository repository, JSONObject obj, UUID uuid)
		throws RepositoryException, JSONException
	{
		int objectTypeId = obj.getInt(SolutionSerializer.PROP_TYPEID);
		IPersist retval = null;
		if (objectTypeId == IRepository.SOLUTIONS)
		{
			retval = parent.getRootObject();
		}
		else
		{
			int element_id = repository.getElementIdForUUID(uuid);
			retval = repository.createObject(parent, objectTypeId, element_id, uuid);
			parent.addChild(retval);
		}
		return retval;
	}

	private static void handleChanged(JSONObject obj, IPersist retval) throws JSONException
	{
		retval.setRevisionNumber(-1);
		if (obj.has(CHANGED_JSON_ATTRIBUTE) && Utils.getAsBoolean(obj.get(CHANGED_JSON_ATTRIBUTE)))
		{
			retval.flagChanged();
			if (SolutionSerializer.isCompositeWithItems(retval))
			{
				// also flag items that are stored in the same file; otherwise they will never be flagged
				for (IPersist p : Utils.iterate(((ISupportChilds)retval).getAllObjects()))
				{
					if (!(p instanceof IScriptElement)) p.flagChanged();
				}
			}
		}
	}

	//for reference tracking we need to have 2 stage deserialize, this is the last part
	private void completePersist(Map<IPersist, JSONObject> persist_json_map, boolean useFilesForDirtyMark) throws RepositoryException, JSONException
	{
		SimpleJSDocParser jsdocParser = new SimpleJSDocParser();
		for (Entry<IPersist, JSONObject> entry : persist_json_map.entrySet())
		{
			IPersist retval = entry.getKey();
			JSONObject obj = entry.getValue();
			if (retval instanceof ScriptVariable)
			{
				if (obj.has(COMMENT_JSON_ATTRIBUTE))
				{
					String comment = obj.getString(COMMENT_JSON_ATTRIBUTE);
					((ScriptVariable)retval).setComment(comment);
				}
				if (obj.has(JS_TYPE_JSON_ATTRIBUTE))
				{
					String type = obj.getString(JS_TYPE_JSON_ATTRIBUTE);
					((ScriptVariable)retval).setSerializableRuntimeProperty(IScriptProvider.TYPE, type);
				}
				else
				{
					((ScriptVariable)retval).setSerializableRuntimeProperty(IScriptProvider.TYPE, null);
				}

			}
			else if (retval instanceof AbstractScriptProvider)
			{
				HashMap<String, String> paramIdToTypeMap = new HashMap<String, String>();
				if (obj.has(COMMENT_JSON_ATTRIBUTE))
				{
					String comment = obj.getString(COMMENT_JSON_ATTRIBUTE);
					((AbstractScriptProvider)retval).setRuntimeProperty(IScriptProvider.COMMENT, comment);
					if (comment != null)
					{
						JSDocTags jsDocTags = jsdocParser.parse(comment, 0);
						String jsDocTagName;
						String jsDocTagValue;
						for (JSDocTag jsDocTag : jsDocTags)
						{
							jsDocTagName = jsDocTag.name();
							jsDocTagValue = jsDocTag.value();

							int endBracketIdx = -1;
							if ((JSDocTag.PARAM.equals(jsDocTagName) || JSDocTag.RETURNS.equals(jsDocTagName) || JSDocTag.RETURN.equals(jsDocTagName)))
							{
								String tagValueType = null;
								String description = null;
								if (jsDocTagValue.startsWith("{{") && (endBracketIdx = jsDocTagValue.indexOf("}}", 1)) != -1)
								{
									endBracketIdx++;
									tagValueType = jsDocTagValue.substring(1, endBracketIdx);
									if (jsDocTagValue.length() > jsDocTagValue.indexOf("}}") + 2)
										description = jsDocTagValue.substring(jsDocTagValue.indexOf("}}") + 2);
								}
								else if (jsDocTagValue.startsWith("{") && (endBracketIdx = jsDocTagValue.lastIndexOf("}")) != -1)
								{
									tagValueType = jsDocTagValue.substring(1, endBracketIdx);
									if (jsDocTagValue.length() > jsDocTagValue.lastIndexOf("}") + 2)
										description = jsDocTagValue.substring(jsDocTagValue.lastIndexOf("}") + 2);
								}

								if (JSDocTag.RETURNS.equals(jsDocTagName) || JSDocTag.RETURN.equals(jsDocTagName))
								{
									MethodArgument returnType = new MethodArgument("", ArgumentType.valueOf(tagValueType), description);
									((AbstractScriptProvider)retval).setRuntimeProperty(IScriptProvider.METHOD_RETURN_TYPE, returnType);
								}
								else
								{
									if (endBracketIdx < jsDocTagValue.length() - 1)
									{
										StringTokenizer tagValueTk = new StringTokenizer(jsDocTagValue.substring(endBracketIdx + 1));
										if (tagValueTk.hasMoreTokens())
										{
											paramIdToTypeMap.put(tagValueTk.nextToken(), tagValueType);
										}
									}
								}
							}
						}
					}
				}

				MethodArgument[] methodArguments = NULL;
				if (obj.has(ARGUMENTS_JSON_ATTRIBUTE))
				{
					@SuppressWarnings("unchecked")
					List<Argument> arguments = (List<Argument>)obj.remove(ARGUMENTS_JSON_ATTRIBUTE);
					if (arguments.size() > 0)
					{
						methodArguments = new MethodArgument[arguments.size()];
//						String comment = obj.optString(COMMENT_JSON_ATTRIBUTE);
//						MethodArgument[] jsDocArguments = parseJSDocArguments(comment);
						for (int i = 0; i < arguments.size(); i++)
						{
							Argument argument = arguments.get(i);
							String name = argument.getArgumentName();
//							for (int j = 0; j < jsDocArguments.length; j++)
//							{
//								if (jsDocArguments[j].getName().equals(name))
//								{
//									methodArguments[i] = jsDocArguments[j];
//									continue outer;
//								}
//							}

							String paramType = paramIdToTypeMap.get(name);
							boolean isOptional = false;
							if (paramType == null)
							{
								String opName = '[' + name + ']';
								isOptional = paramIdToTypeMap.containsKey(opName);
								if (isOptional)
								{
									paramType = paramIdToTypeMap.get(opName);
								}
							}
							if (paramType == null || "*".equals(paramType)) paramType = "Any"; // if still null then it is the Any type.
							ArgumentType argumentType = ArgumentType.valueOf(paramType);
							methodArguments[i] = new MethodArgument(name, argumentType, null, isOptional); // TODO: parse description
						}
					}
				}
				((AbstractScriptProvider)retval).setRuntimeProperty(IScriptProvider.METHOD_ARGUMENTS, methodArguments);

				if (obj.has(JS_TYPE_JSON_ATTRIBUTE))
				{
					String type = obj.getString(JS_TYPE_JSON_ATTRIBUTE);
					((AbstractScriptProvider)retval).setSerializableRuntimeProperty(IScriptProvider.TYPE, type);
				}

			}

			setPersistValues(repository, retval, obj);
			if (obj.has(EXTRA_DOC_COMMENTS))
			{
				((AbstractBase)retval).putCustomProperty(new String[] { EXTRA_DOC_COMMENTS }, obj.get(EXTRA_DOC_COMMENTS));
			}
			if (retval instanceof ScriptVariable && obj.has(DECL_KEYWORD))
			{
				((ScriptVariable)retval).setKeyword(obj.getString(DECL_KEYWORD));
			}
			if (useFilesForDirtyMark) handleChanged(obj, retval);
		}
	}

	private static MethodArgument[] NULL = new MethodArgument[0];

	public static void setPersistValues(IDeveloperRepository repository, IPersist persist, JSONObject obj) throws RepositoryException, JSONException
	{
		Map<String, Object> propertyValues = getPropertyValuesForJsonObject(repository, persist, obj);
		repository.updatePersistWithValueMap(persist, propertyValues, true);
	}

	public static Map<String, Object> getPropertyValuesForJsonObject(IDeveloperRepository repository, IPersist persist, JSONObject obj)
		throws RepositoryException, JSONException
	{
		LinkedHashMap<String, Object> propertyValues = new LinkedHashMap<String, Object>(); //  use linked hashmap to preserve ordening
		ContentSpec cs = repository.getContentSpec();

		Iterator<ContentSpec.Element> iterator = cs.getPropertiesForObjectType(persist.getTypeID());
		// Note that elements are sorted by contentid desc.
		// This is needed because otherwise deprecated properties (with lower content id) may get overwritten with the default value of their replacement.
		while (iterator.hasNext())
		{
			ContentSpec.Element element = iterator.next();

			if (element.isMetaData()) continue;

			String propertyName = element.getName();
			if (SolutionSerializer.PROP_UUID.equals(propertyName) || SolutionSerializer.PROP_TYPEID.equals(propertyName)) continue;

			if (obj.has(propertyName))
			{
				Object propertyObjectValue = obj.get(propertyName);
				if (JSONObject.NULL == propertyObjectValue)
				{
					propertyObjectValue = null;
				}
				else
				{
					propertyObjectValue = propertyObjectValue.toString();
				}

				if (element.getTypeID() == IRepository.ELEMENTS)
				{
					String id = propertyObjectValue.toString();
					UUID uuid = null;
					if (id.indexOf('-') > 0)
					{
						uuid = UUID.fromString(id);
						propertyObjectValue = new Integer(repository.getElementIdForUUID(uuid));
					}
					else
					{
						propertyObjectValue = new Integer(Utils.getAsInteger(id));
					}

					//filling this in case the obj is sent to team repository (with other ids)
					HashMap<UUID, Integer> map = ((AbstractBase)persist).getSerializableRuntimeProperty(AbstractBase.UUIDToIDMapProperty);
					if (map == null)
					{
						map = new HashMap<UUID, Integer>();
						((AbstractBase)persist).setSerializableRuntimeProperty(AbstractBase.UUIDToIDMapProperty, map);
					}
					if (uuid != null)
					{
						map.put(uuid, (Integer)propertyObjectValue);
					}
				}
				else
				{
					propertyObjectValue = repository.convertArgumentStringToObject(element.getTypeID(), (String)propertyObjectValue);
				}
				propertyValues.put(propertyName, propertyObjectValue);
				obj.remove(propertyName);
			}
		}
		return propertyValues;
	}

	public static UUID getUUID(File file)
	{
		if (!SolutionSerializer.isJSONFile(file.getName()))
		{
			return null;
		}
		UUID uuid = null;
		try
		{
			JSONObject obj = new ServoyJSONObject(Utils.getTXTFileContent(file), true);
			if (obj.has(SolutionSerializer.PROP_UUID))
			{
				uuid = UUID.fromString(obj.getString(SolutionSerializer.PROP_UUID));
			}
			else
			{
				uuid = UUID.randomUUID();
			}
		}
		catch (JSONException e)
		{
			ServoyLog.logWarning("Cannot get uuid from file " + file, e);
		}
		return uuid;
	}

	public static String getUUID(File file, int position)
	{
		if (!SolutionSerializer.isJSONFile(file.getName()) || position < 0)
		{
			return null;
		}
		String uuid = null;
		String text = Utils.getTXTFileContent(file);
		if (text != null && text.length() > position)
		{
			String[] lLines = text.substring(0, position).split("\n");
			String[] rLines = text.substring(position).split("\n");
			for (int i = lLines.length - 1; i >= 0; i--)
			{
				if (lLines[i].trim().startsWith("{") || lLines[i].trim().startsWith("}")) break;
				if (lLines[i].trim().startsWith(SolutionSerializer.PROP_UUID))
				{
					try
					{
						JSONObject obj = new JSONObject("{" + lLines[i] + ((i == lLines.length - 1) ? rLines[0] : "") + "}");
						return obj.getString(SolutionSerializer.PROP_UUID);
					}
					catch (JSONException e)
					{
						ServoyLog.logError(e);
					}
				}
			}
			for (int i = 0; i <= rLines.length; i++)
			{
				if (rLines[i].trim().startsWith("{") || rLines[i].trim().startsWith("}")) break;
				if (rLines[i].trim().startsWith(SolutionSerializer.PROP_UUID))
				{
					try
					{
						JSONObject obj = new JSONObject("{" + rLines[i] + ((i == 0) ? lLines[lLines.length - 1] : "") + "}");
						return obj.getString(SolutionSerializer.PROP_UUID);
					}
					catch (JSONException e)
					{
						ServoyLog.logError(e);
					}
				}
			}

		}
		return uuid;
	}

	public static IPersist findPersistFromFile(IFile f)
	{
		try
		{
			IProjectNature nature = f.getProject().getNature(ServoyProject.NATURE_ID);
			if (nature instanceof ServoyProject)
			{
				if (f.getFileExtension() != null && !f.getFileExtension().equals(SolutionSerializer.JS_FILE_EXTENSION_WITHOUT_DOT) &&
					!SolutionSerializer.isJSONFile(f.getName()))
				{
					String[] segments = f.getProjectRelativePath().segments();
					if (segments.length == 2 && SolutionSerializer.MEDIAS_DIR.equals(segments[0]))
					{
						return ((ServoyProject)nature).getEditingSolution().getMedia(segments[1]);
					}
				}
				File file = f.getLocation().toFile();
				if (f.getFileExtension() != null && f.getFileExtension().equals(SolutionSerializer.JS_FILE_EXTENSION_WITHOUT_DOT))
				{
					try
					{
						file = getJSONFileFromJS(file, f.getName(), null).getLeft();
					}
					catch (RepositoryException e)
					{
						ServoyLog.logWarning("Error paring js to json", e);
					}
				}

				if (file != null && file.exists())
				{
					UUID uuid = getUUID(file);
					return AbstractRepository.searchPersist(((ServoyProject)nature).getSolution(), uuid);
				}
			}
		}
		catch (CoreException e)
		{
			ServoyLog.logError(e);
		}
		return null;
	}

	public static Map<String, List<String>> deserializeWorkingSets(IFileAccess fileAccess, String resourcesProjectName)
	{
		Map<String, List<String>> workingSets = new HashMap<String, List<String>>();
		if (fileAccess != null)
		{
			String path = SolutionSerializer.getWorkingSetPath(resourcesProjectName);
			if (fileAccess.exists(path))
			{
				try
				{
					JSONArray items = getJSONArray(fileAccess.getUTF8Contents(path));
					for (int i = 0; i < items.length(); i++)
					{
						if (!items.isNull(i))
						{
							try
							{
								JSONObject obj = items.getJSONObject(i);
								String name = obj.has(SolutionSerializer.PROP_NAME) ? obj.getString(SolutionSerializer.PROP_NAME) : null;
								if (name != null)
								{
									List<String> paths = new ArrayList<String>();
									workingSets.put(name, paths);
									if (obj.has("paths"))
									{
										JSONArray jsonPaths = obj.getJSONArray("paths");
										for (int j = 0; j < jsonPaths.length(); j++)
										{
											paths.add(jsonPaths.getString(j));
										}
									}
								}
							}
							catch (Exception e)
							{
								ServoyLog.logError("Error while reading working sets file: " + path, e);
							}
						}
					}
				}
				catch (Exception ex)
				{
					ServoyLog.logError("Error while reading working sets file: " + path, ex);
				}
			}
		}
		return workingSets;
	}

	public static SolutionMetaData deserializeRootMetaData(IDeveloperRepository repository, File projectFile, String name) throws RepositoryException
	{
		try
		{
			File frmd = new File(projectFile, SolutionSerializer.ROOT_METADATA);
			String solutionmetadata = Utils.getTXTFileContent(frmd);
			if (solutionmetadata == null) return null;

			int fileVersion;
			JSONObject obj = new ServoyJSONObject(solutionmetadata, true);
			if (!obj.has(SolutionSerializer.PROP_FILE_VERSION))
			{
				throw new RepositoryException("Cannot handle files with unknown version");
			}
			else
			{
				fileVersion = obj.getInt(SolutionSerializer.PROP_FILE_VERSION);
				if (fileVersion <= 0)
				{
					throw new RepositoryException("Cannot handle files with invalid version (<= 0)");
				}
			}
			UUID rootObjectUuid = UUID.fromString(obj.getString(SolutionSerializer.PROP_UUID));
			int objectTypeId = obj.getInt(SolutionSerializer.PROP_TYPEID);
//			String name = obj.getString(SolutionSerializer.PROP_NAME);
			int solutionType = obj.getInt("solutionType");
			boolean mustAuthenticate = obj.getBoolean("mustAuthenticate");
			//int id = repository.getNewElementID(rootObjectUuid);
			int id = repository.getElementIdForUUID(rootObjectUuid);
			SolutionMetaData metadata = (SolutionMetaData)repository.createRootObjectMetaData(id, rootObjectUuid, name, objectTypeId, 1, 1);
			metadata.setMustAuthenticate(mustAuthenticate);
			metadata.setSolutionType(solutionType);
			metadata.setFileVersion(fileVersion);
			if (AbstractRepository.repository_version != fileVersion) metadata.flagChanged();
			else metadata.clearChanged();

			return metadata;
		}
		catch (JSONException e)
		{
			throw new RepositoryException("Cannot get root meta data from file " + projectFile, e);
		}
	}

	public static class ObjBeforeJSExtensionComparator implements Comparator<String>
	{
		public int compare(String fname1, String fname2)
		{
			if (fname1 != null && fname2 != null)
			{
				if ((SolutionSerializer.isJSONFile(fname1) && SolutionSerializer.isJSONFile(fname2)) ||
					(!SolutionSerializer.isJSONFile(fname1) && !SolutionSerializer.isJSONFile(fname2)))
				{
					// 2 json files or 2 other files
					return fname1.compareTo(fname2);
				}
				else if (SolutionSerializer.isJSONFile(fname1))
				{
					// json and other
					return -1;
				}
				else
				{
					// other and orders.obj
					return 1;
				}
			}
			return 0;
		}
	}

	// as solutionDir can be also be a temp folder, ignore it when searching in changedFiles
	private boolean isChangedFile(File solutionDir, File file, List<File> changedFiles)
	{
		if (changedFiles != null)
		{
			String filePath = file.getPath().substring(solutionDir.getPath().length());

			for (File changedFile : changedFiles)
			{
				if (changedFile.getPath().endsWith(filePath))
				{
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * @author jcompagner
	 *
	 */
	private static class Line
	{
		int line;
		int start;

		/**
		 * @param i
		 * @param j
		 */
		public Line(int line, int start)
		{
			this.line = line;
			this.start = start;
		}
	}

	private ICalculationTypeInferencer getCalculationTypeInferencer(Script script, IFile resource)
	{
		if (calculationTypeInferencerProvider == null)
		{
			IExtensionRegistry reg = Platform.getExtensionRegistry();
			IExtensionPoint ep = reg.getExtensionPoint(ICalculationTypeInferencerProvider.EXTENSION_ID);
			IExtension[] extensions = ep.getExtensions();

			if (extensions == null || extensions.length == 0)
			{
				ServoyLog.logWarning(
					"Could not find calculation type inferencer plugin (extension point " + ICalculationTypeInferencerProvider.EXTENSION_ID + ")", null);
				return null;
			}
			if (extensions.length > 1)
			{
				ServoyLog.logWarning(
					"Multiple calculation type inferencer plugins found (extension point " + ICalculationTypeInferencerProvider.EXTENSION_ID + ")",
					null);
			}
			IConfigurationElement[] ce = extensions[0].getConfigurationElements();
			if (ce == null || ce.length == 0)
			{
				ServoyLog.logWarning(
					"Could not read calculation type inferencer plugin (extension point " + ICalculationTypeInferencerProvider.EXTENSION_ID + ")",
					null);
				return null;
			}
			if (ce.length > 1)
			{
				ServoyLog.logWarning(
					"Multiple extensions for calculation type inferencer plugins found (extension point " + ICalculationTypeInferencerProvider.EXTENSION_ID +
						")",
					null);
			}
			try
			{
				calculationTypeInferencerProvider = (ICalculationTypeInferencerProvider)ce[0].createExecutableExtension("class");
			}
			catch (CoreException e)
			{
				ServoyLog.logWarning(
					"Could not create calculation type inferencer plugin (extension point " + ICalculationTypeInferencerProvider.EXTENSION_ID + ")",
					e);
				return null;
			}
			if (calculationTypeInferencerProvider == null)
			{
				ServoyLog.logWarning(
					"Could not load calculation type inferencer plugin (extension point " + ICalculationTypeInferencerProvider.EXTENSION_ID + ")",
					null);
			}
		}
		return calculationTypeInferencerProvider.parse(script, resource);
	}

}
