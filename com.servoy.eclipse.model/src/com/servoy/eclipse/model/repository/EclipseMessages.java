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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.WeakHashMap;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.nature.ServoyResourcesProject;
import com.servoy.eclipse.model.util.AvoidMultipleExecutionsWorkspaceJob;
import com.servoy.eclipse.model.util.IFileAccess;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.WorkspaceFileAccess;
import com.servoy.j2db.ICustomMessageLoader;
import com.servoy.j2db.Messages;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.I18NUtil;
import com.servoy.j2db.persistence.I18NUtil.MessageEntry;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistVisitor;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportText;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.server.headlessclient.AbstractApplication;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.SortedProperties;
import com.servoy.j2db.util.Utils;

public class EclipseMessages implements ICustomMessageLoader
{
	public static final String MESSAGES_EXTENSION = ".properties";
	public static final String MESSAGES_DIR = "messages";

	private final IFileAccess workspaceDir;
	private final HashMap<String, TreeMap<String, I18NUtil.MessageEntry>> i18nDatasourceMessages = new HashMap<String, TreeMap<String, I18NUtil.MessageEntry>>();
	private final HashMap<String, Boolean> hasI18nDatasourceUnsavedMessages = new HashMap<String, Boolean>();

	private static AvoidMultipleExecutionsWorkspaceJob writingI18NJobForAllActiveSolutionAndModules;
	private static String writingI18NJobForAllActiveSolutionAndModules_lastProjectName;
	private static boolean writingI18NJobForAllActiveSolutionAndModules_lastOverwriteExisting;
	private static boolean writingI18NJobForAllActiveSolutionAndModules_lastDeleteNonExistingKeys;

	public EclipseMessages()
	{
		this.workspaceDir = new WorkspaceFileAccess(ResourcesPlugin.getWorkspace());
	}

	public void addMessage(String i18nDatasource, I18NUtil.MessageEntry messageEntry)
	{
		TreeMap<String, I18NUtil.MessageEntry> messages = getDatasourceMessages(i18nDatasource);
		if (messages != null)
		{
			messages.put(messageEntry.getLanguageKey(), messageEntry);
			hasI18nDatasourceUnsavedMessages.put(i18nDatasource, new Boolean(true));
		}
	}

	public void removeMessage(String i18nDatasource, String messageKey)
	{
		TreeMap<String, I18NUtil.MessageEntry> messages = getDatasourceMessages(i18nDatasource);
		if (messages != null)
		{
			ArrayList<String> langKeyToRemove = new ArrayList<String>();
			for (Entry<String, MessageEntry> entry : messages.entrySet())
			{
				String msgKey = entry.getValue().getKey();
				if (msgKey.equals(messageKey)) langKeyToRemove.add(entry.getKey());
			}
			for (String langKey : langKeyToRemove)
				messages.remove(langKey);
			hasI18nDatasourceUnsavedMessages.put(i18nDatasource, new Boolean(langKeyToRemove.size() > 0));
		}
	}

	public boolean hasUnsavedMessages(String i18nDatasource)
	{
		Boolean hasUnsavedMessages = hasI18nDatasourceUnsavedMessages.get(i18nDatasource);
		return hasUnsavedMessages != null ? hasUnsavedMessages.booleanValue() : false;
	}

	public void clearUnsavedMessages(String i18nDatasource)
	{
		i18nDatasourceMessages.remove(i18nDatasource);
		hasI18nDatasourceUnsavedMessages.remove(i18nDatasource);
	}

	public void save(String i18nDatasource)
	{
		TreeMap<String, I18NUtil.MessageEntry> messages = getDatasourceMessages(i18nDatasource);
		if (messages != null)
		{
			String[] dbServernameTablename = DataSourceUtils.getDBServernameTablename(i18nDatasource);
			try
			{
				save(dbServernameTablename[0], dbServernameTablename[1], messages);
				clearUnsavedMessages(i18nDatasource);
			}
			catch (RepositoryException ex)
			{
				ServoyLog.logError(ex);
			}
		}
	}

	public void save(String serverName, String tableName, TreeMap<String, I18NUtil.MessageEntry> messages) throws RepositoryException
	{
		EclipseMessages.writeMessages(serverName, tableName, messages, workspaceDir);
	}

	public TreeMap<String, I18NUtil.MessageEntry> readMessages(String serverName, String tableName) throws RepositoryException
	{
		return EclipseMessages.readMessages(serverName, tableName, workspaceDir);
	}

	public void removeCachedMessages()
	{
		ArrayList<String> i18nDatasourceToRemove = new ArrayList<String>();
		String i18nDatasourceKey;
		for (String element : i18nDatasourceMessages.keySet())
		{
			i18nDatasourceKey = element;
			if (!hasUnsavedMessages(i18nDatasourceKey)) i18nDatasourceToRemove.add(i18nDatasourceKey);
		}

		for (String i18nDSKey : i18nDatasourceToRemove)
			i18nDatasourceMessages.remove(i18nDSKey);
	}

	public TreeMap<String, I18NUtil.MessageEntry> getDatasourceMessages(String i18nDatasource)
	{
		TreeMap<String, I18NUtil.MessageEntry> messages = null;
		if (i18nDatasource != null)
		{
			messages = i18nDatasourceMessages.get(i18nDatasource);
			if (messages == null)
			{
				String[] dbServernameTablename = DataSourceUtils.getDBServernameTablename(i18nDatasource);
				String i18nServerName = dbServernameTablename != null ? dbServernameTablename[0] : null;
				String i18nTableName = dbServernameTablename != null ? dbServernameTablename[1] : null;
				if (i18nServerName != null && i18nTableName != null)
				{
					try
					{
						messages = EclipseMessages.readMessages(i18nServerName, i18nTableName, workspaceDir);
						i18nDatasourceMessages.put(i18nDatasource, messages);
					}
					catch (RepositoryException ex)
					{
						ServoyLog.logError(ex);
					}
				}
			}
		}

		return messages;
	}

	public void loadMessages(String i18nDatasource, Properties defaultProperties, Properties localeProperties, Locale language, String searchKey)
	{
		TreeMap<String, I18NUtil.MessageEntry> messages = getDatasourceMessages(i18nDatasource);

		if (messages != null)
		{
			String searchKeyLowerCase = searchKey != null ? searchKey.toLowerCase() : null;

			TreeMap<String, String> allDefaults = new TreeMap<String, String>();
			TreeMap<String, String> allLocales = new TreeMap<String, String>();

			for (Entry<String, MessageEntry> entry : messages.entrySet())
			{
				String lang = entry.getValue().getLanguage();
				String messageKey = entry.getValue().getKey();
				String value = entry.getValue().getValue();
				if (lang.equals(""))
				{
					allDefaults.put(messageKey, value);
				}
				else if (lang.equals(Messages.localeToString(language)))
				{
					allLocales.put(messageKey, value);
				}
			}

			for (String messageKey : allDefaults.keySet())
			{
				String defaultValue = allDefaults.get(messageKey);
				if (searchKeyLowerCase == null || messageKey.toLowerCase().indexOf(searchKeyLowerCase) != -1 ||
					defaultValue.toLowerCase().indexOf(searchKeyLowerCase) != -1)
				{
					defaultProperties.setProperty(messageKey, defaultValue);
					if (allLocales.containsKey(messageKey))
					{
						String localeValue = allLocales.get(messageKey);
						localeProperties.put(messageKey, localeValue);
					}
				}
			}
			for (String messageKey : allLocales.keySet())
			{
				if (!localeProperties.containsKey(messageKey))
				{
					String localeValue = allLocales.get(messageKey);
					if (searchKeyLowerCase == null || messageKey.toLowerCase().indexOf(searchKeyLowerCase) != -1 ||
						localeValue.toLowerCase().indexOf(searchKeyLowerCase) != -1)
					{
						localeProperties.setProperty(messageKey, localeValue);
						if (!defaultProperties.containsKey(messageKey) && allDefaults.containsKey(messageKey))
						{
							String defaultValue = allDefaults.get(messageKey);
							defaultProperties.setProperty(messageKey, defaultValue);
						}
					}
				}
			}
		}
	}

	public void loadMessages(String i18nDatasource, Properties properties, Locale language, int loadingType, String searchKey)
	{
		if (i18nDatasource != null)
		{
			if (loadingType == Messages.ALL_LOCALES)
			{
				loadMessages(i18nDatasource, properties, language, Messages.DEFAULT_LOCALE, searchKey);
				loadMessages(i18nDatasource, properties, language, Messages.SPECIFIED_LANGUAGE, searchKey);
				loadMessages(i18nDatasource, properties, language, Messages.SPECIFIED_LOCALE, searchKey);
			}
			else
			{
				TreeMap<String, I18NUtil.MessageEntry> messages = getDatasourceMessages(i18nDatasource);

				if (messages != null)
				{
					for (Entry<String, MessageEntry> entry : messages.entrySet())
					{
						String lang = entry.getValue().getLanguage();
						String messageKey = entry.getValue().getKey();

						if ((loadingType == Messages.DEFAULT_LOCALE && (lang.equals(""))) ||
							(loadingType == Messages.SPECIFIED_LANGUAGE && (lang.equals(language.getLanguage()))) ||
							(loadingType == Messages.SPECIFIED_LOCALE && (lang.equals(Messages.localeToString(language)))))
						{
							String value = entry.getValue().getValue();
							String searchKeyLowerCase = searchKey != null ? searchKey.toLowerCase() : null;
							if (searchKeyLowerCase == null || messageKey.toLowerCase().indexOf(searchKeyLowerCase) != -1 ||
								value.toLowerCase().indexOf(searchKeyLowerCase) != -1) properties.setProperty(messageKey, value);
						}
					}
				}
			}
		}
	}

	@Override
	public String getI18nMessage(String i18nDatasource, String key, Locale language)
	{
		if (i18nDatasource != null && key != null && key.startsWith("i18n:")) //$NON-NLS-1$
		{
			key = key.substring(5);
			TreeMap<String, I18NUtil.MessageEntry> messages = getDatasourceMessages(i18nDatasource);
			if (messages != null)
			{
				String languageKey = Messages.localeToString(language) + "." + key;
				if (messages.containsKey(languageKey)) return messages.get(languageKey).getValue();
				languageKey = "." + key;
				if (messages.containsKey(languageKey)) return messages.get(languageKey).getValue();
				String message = AbstractApplication.getDefaultMessage(key, language);
				if (message != null) return message;
			}
		}
		return key;
	}

	// write project solution & its modules i18n files to the resource project
	public synchronized static void writeProjectI18NFiles(final ServoyProject servoyProject, final boolean overwriteExisting,
		final boolean deleteNonExistingKeys)
	{
		String projectName = null;
		if (servoyProject != null) projectName = servoyProject.getProject().getName();

		if (writingI18NJobForAllActiveSolutionAndModules == null ||
			!Utils.stringSafeEquals(writingI18NJobForAllActiveSolutionAndModules_lastProjectName, projectName) ||
			overwriteExisting != writingI18NJobForAllActiveSolutionAndModules_lastOverwriteExisting ||
			deleteNonExistingKeys != writingI18NJobForAllActiveSolutionAndModules_lastDeleteNonExistingKeys)
		{
			writingI18NJobForAllActiveSolutionAndModules_lastProjectName = projectName;
			writingI18NJobForAllActiveSolutionAndModules_lastOverwriteExisting = overwriteExisting;
			writingI18NJobForAllActiveSolutionAndModules_lastDeleteNonExistingKeys = deleteNonExistingKeys;
			writingI18NJobForAllActiveSolutionAndModules = new AvoidMultipleExecutionsWorkspaceJob("Writing I18N files for active solution & modules...")
			{

				@Override
				protected IStatus runInWWorkspaceAvoidingMultipleExecutions(IProgressMonitor monitor)
				{
					Solution[] modules = servoyProject.getModules();
					Solution[] allSolutions = new Solution[modules.length + 1];
					allSolutions[0] = servoyProject.getSolution();
					System.arraycopy(modules, 0, allSolutions, 1, modules.length);

					try
					{
						for (Solution s : allSolutions)
						{
							String i18nDataSource = s.getI18nDataSource();
							if (i18nDataSource != null)
							{
								ServoyResourcesProject resourceProject = servoyProject.getResourcesProject();
								if (resourceProject != null)
								{
									String[] serverTableNames = DataSourceUtils.getDBServernameTablename(i18nDataSource);
									TreeMap<String, I18NUtil.MessageEntry> messages = I18NUtil.loadSortedMessagesFromRepository(
										ApplicationServerRegistry.get().getDeveloperRepository(), ApplicationServerRegistry.get().getDataServer(),
										ApplicationServerRegistry.get().getClientId(), serverTableNames[0], serverTableNames[1], null, null, null);
									writeMessages(serverTableNames[0], serverTableNames[1], messages, new WorkspaceFileAccess(ResourcesPlugin.getWorkspace()),
										resourceProject.getProject(), false, overwriteExisting, deleteNonExistingKeys);
								}
							}
						}
					}
					catch (final Exception ex)
					{
						ServoyLog.logError(ex);
						ModelUtils.getUnexpectedSituationHandler().cannotWriteI18NFiles(ex);
					}
					return Status.OK_STATUS;
				}
			};
			writingI18NJobForAllActiveSolutionAndModules.setUser(false);
			writingI18NJobForAllActiveSolutionAndModules.setRule(ResourcesPlugin.getWorkspace().getRoot()); // prevent from running at the same time as builder that may be reading the messages
		}
		writingI18NJobForAllActiveSolutionAndModules.schedule();
	}

	public static void writeMessages(String i18nServer, String i18nTable, TreeMap<String, I18NUtil.MessageEntry> messages, final IFileAccess workspaceDir)
		throws RepositoryException
	{
		ServoyResourcesProject resourceProject = ServoyModelFinder.getServoyModel().getActiveResourcesProject();
		if (resourceProject != null) writeMessages(i18nServer, i18nTable, messages, workspaceDir, resourceProject.getProject());
	}

	public static void writeMessages(String i18nServer, String i18nTable, TreeMap<String, I18NUtil.MessageEntry> messages, final IFileAccess workspaceDir,
		IProject resourceProject) throws RepositoryException
	{
		writeMessages(i18nServer, i18nTable, messages, workspaceDir, resourceProject, true, true, true);
	}

	private synchronized static void writeMessages(String i18nServer, String i18nTable, TreeMap<String, I18NUtil.MessageEntry> messages,
		final IFileAccess workspaceDir, IProject resourceProject, boolean bDeleteUnnecessaryI18NFiles, boolean overwriteExisting, boolean deleteNonExistingKeys)
		throws RepositoryException
	{
		final HashMap<String, Properties> languagesOutput = new HashMap<String, Properties>();

		Map.Entry<String, I18NUtil.MessageEntry> entry;
		String lang, key;
		for (Entry<String, MessageEntry> element : messages.entrySet())
		{
			entry = element;
			lang = entry.getValue().getLanguage();
			key = entry.getValue().getKey();
			Properties output = languagesOutput.get(lang);
			if (output == null)
			{
				output = new SortedProperties();
				languagesOutput.put(lang, output);
			}
			String v = entry.getValue().getValue();
			if ("".equals(lang) || v != null && v.length() > 0) output.put(key, v);
		}

		try
		{
			IPath messageFilePath;
			String langExt;
			Map.Entry<String, Properties> languageOutputEntry;
			for (Entry<String, Properties> element : languagesOutput.entrySet())
			{
				languageOutputEntry = element;
				langExt = languageOutputEntry.getKey();
				if (!langExt.equals("")) langExt = "." + langExt;
				messageFilePath = resourceProject.getFullPath().append(MESSAGES_DIR).append(i18nServer + "." + i18nTable + langExt + MESSAGES_EXTENSION);

				String relativeFilePath = messageFilePath.toOSString();
				if (!workspaceDir.exists(relativeFilePath))
				{
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					languageOutputEntry.getValue().store(bos, null);
					// cut first line conaining current date
					workspaceDir.setContents(relativeFilePath, cutFirstLine(bos.toByteArray()));
					bos.close();
				}
				else
				{
					Properties newMessages = languageOutputEntry.getValue();
					Properties oldMessages = new Properties();
					oldMessages.load(new ByteArrayInputStream(workspaceDir.getContents(relativeFilePath)));
					if (!oldMessages.equals(newMessages))
					{
						if (!overwriteExisting || !deleteNonExistingKeys)
						{
							// put non-existing keys to the new messages
							for (Entry<Object, Object> e : oldMessages.entrySet())
							{
								if (!newMessages.containsKey(e.getKey()))
								{
									if (!deleteNonExistingKeys)
									{
										newMessages.put(e.getKey(), e.getValue());
									}
								}
								else if (!overwriteExisting)
								{
									newMessages.put(e.getKey(), e.getValue());
								}
							}
						}

						ByteArrayOutputStream bos = new ByteArrayOutputStream();
						newMessages.store(bos, null);
						// cut first line conaining current date
						workspaceDir.setContents(relativeFilePath, cutFirstLine(bos.toByteArray()));
						bos.close();
					}
				}
			}

			// delete unused messages files
			if (bDeleteUnnecessaryI18NFiles)
			{
				IResource messagesResource = resourceProject.findMember(MESSAGES_DIR);
				final String messageFileName = i18nServer + "." + i18nTable;
				if (messagesResource != null)
				{
					messagesResource.accept(new IResourceVisitor()
					{

						public boolean visit(IResource resource) throws CoreException
						{
							String resourceName = resource.getName();
							if (resourceName.startsWith(messageFileName))
							{
								try
								{
									String language = null;
									resourceName = resourceName.substring(0, resourceName.lastIndexOf(MESSAGES_EXTENSION));
									language = resourceName.substring(messageFileName.length());
									if (language.length() > 0 && language.startsWith("."))
									{
										language = language.substring(1);
									}

									if (!languagesOutput.containsKey(language))
									{
										workspaceDir.delete(resource.getFullPath().toOSString());
									}
								}
								catch (Exception ex)
								{
									ServoyLog.logError(ex);
								}
							}
							return true;
						}

					}, IResource.DEPTH_ONE, false);
				}
			}
		}
		catch (

		Exception e)
		{
			throw new RepositoryException(e);
		}
	}

	public synchronized static TreeMap<String, I18NUtil.MessageEntry> readMessages(String i18nServer, String i18nTable, final IFileAccess workspaceDir)
		throws RepositoryException
	{
		try
		{
			ServoyResourcesProject resourcesProject = ServoyModelFinder.getServoyModel().getActiveResourcesProject();
			if (resourcesProject != null)
			{
				final TreeMap<String, I18NUtil.MessageEntry> messagesMap = new TreeMap<String, I18NUtil.MessageEntry>();
				final String messageFileName = i18nServer + "." + i18nTable;

				IResource messagesResource = resourcesProject.getProject().findMember(MESSAGES_DIR);
				if (messagesResource != null)
				{
					messagesResource.accept(new IResourceVisitor()
					{
						public boolean visit(IResource resource) throws CoreException
						{
							String resourceName = resource.getName();
							if (resourceName.startsWith(messageFileName) && resourceName.endsWith(MESSAGES_EXTENSION))
							{
								try
								{
									String language = null;
									resourceName = resourceName.substring(0, resourceName.lastIndexOf(MESSAGES_EXTENSION));
									language = resourceName.substring(messageFileName.length());
									if (language.length() > 0 && language.startsWith("."))
									{
										language = language.substring(1);
									}

									byte[] messages = workspaceDir.getContents(resource.getFullPath().toOSString());
									Properties messagesProp = new Properties();
									messagesProp.load(new ByteArrayInputStream(messages));

									Map.Entry<Object, Object> messagesPropEntry;
									for (Entry<Object, Object> element : messagesProp.entrySet())
									{
										messagesPropEntry = element;
										I18NUtil.MessageEntry messageEntry = new I18NUtil.MessageEntry(language, messagesPropEntry.getKey().toString(),
											messagesPropEntry.getValue().toString());
										messagesMap.put(messageEntry.getLanguageKey(), messageEntry);
									}
								}
								catch (Exception ex)
								{
									ServoyLog.logError(ex);
								}
							}
							return true;
						}

					}, IResource.DEPTH_ONE, false);
				}
				return messagesMap;
			}

			return null;
		}
		catch (Exception ex)
		{
			throw new RepositoryException(ex);
		}
	}

	public synchronized static void deleteMessageFileNames(String i18nServer, String i18nTable)
	{
		String[] messageFileNames = getMessageFileNames(i18nServer, i18nTable, false);
		if (messageFileNames.length > 0)
		{
			ServoyResourcesProject resourcesProject = ServoyModelFinder.getServoyModel().getActiveResourcesProject();
			if (resourcesProject != null)
			{
				IContainer messagesResource = (IContainer)resourcesProject.getProject().findMember(MESSAGES_DIR);
				if (messagesResource != null)
				{
					for (String messageFileName : messageFileNames)
					{
						IResource i18nResource = messagesResource.findMember(messageFileName);
						if (i18nResource != null)
						{
							try
							{
								i18nResource.delete(true, null);
							}
							catch (Exception ex)
							{
								ServoyLog.logError(ex);
							}
						}
					}
				}
			}
		}
	}

	public static String[] getDefaultMessageFileNames()
	{
		return getMessageFileNames(null, null, true); // return all default i18n files from the active resource project
	}

	public static String[] getMessageFileNames(String i18nServer, String i18nTable)
	{
		return getMessageFileNames(i18nServer, i18nTable, false);
	}

	private synchronized static String[] getMessageFileNames(String i18nServer, String i18nTable, final boolean onlyDefaults)
	{
		try
		{
			final Set<String> fileNames = new HashSet<String>();

			ServoyResourcesProject resourcesProject = ServoyModelFinder.getServoyModel().getActiveResourcesProject();
			if (resourcesProject != null)
			{
				final String messageFileName = (i18nServer != null && i18nTable != null) ? i18nServer + "." + i18nTable : null;
				IResource messagesResource = resourcesProject.getProject().findMember(MESSAGES_DIR);
				if (messagesResource != null)
				{
					messagesResource.accept(new IResourceVisitor()
					{
						public boolean visit(IResource resource) throws CoreException
						{
							if (resource.getType() == IResource.FILE)
							{
								String resourceName = resource.getName();
								if (messageFileName == null || resourceName.startsWith(messageFileName))
								{
									if (onlyDefaults)
									{
										int extIdx = resourceName.indexOf(EclipseMessages.MESSAGES_EXTENSION);
										if (extIdx != -1)
										{
											String i18nServerTableName = resourceName.substring(0, extIdx);
											int serverTableSeparator = i18nServerTableName.indexOf('.');
											if (serverTableSeparator != -1 && serverTableSeparator < i18nServerTableName.length() - 1 &&
												i18nServerTableName.indexOf('.', serverTableSeparator + 1) != -1) // it is not a default
											{
												return true;
											}
										}

									}
									if (resourceName.endsWith(EclipseMessages.MESSAGES_EXTENSION)) fileNames.add(resourceName);
								}
							}
							return true;
						}
					}, IResource.DEPTH_ONE, false);
				}
			}
			String[] result = new String[fileNames.size()];
			fileNames.toArray(result);
			return result;
		}
		catch (Exception ex)
		{
			return null;
		}
	}

	public synchronized static String getMessageFileContent(String messageFileName, final IFileAccess workspaceDir)
	{
		try
		{
			final Set<String> fileNames = new HashSet<String>();

			ServoyResourcesProject resourcesProject = ServoyModelFinder.getServoyModel().getActiveResourcesProject();
			if (resourcesProject != null)
			{
				IResource resource = resourcesProject.getProject().getFolder(MESSAGES_DIR).findMember(messageFileName);
				if (resource != null)
				{
					String content = workspaceDir.getUTF8Contents(resource.getFullPath().toOSString());
					return content;
				}
			}
		}
		catch (Exception ex)
		{
			Debug.error("Exception while getting message file content from file '" + messageFileName + "'.", ex);
		}
		return null;
	}

	private static byte[] cutFirstLine(byte[] output)
	{
		int newLineIdx = -1;
		while (output[++newLineIdx] != '\n')
			;
		byte[] outputBytesCut = new byte[output.length - newLineIdx - 1];
		System.arraycopy(output, newLineIdx + 1, outputBytesCut, 0, outputBytesCut.length);

		return outputBytesCut;
	}

	private static WeakHashMap<IFormElement, List<Pair<String, String>>> componentsI18NKeys = new WeakHashMap<IFormElement, List<Pair<String, String>>>();

	public static void addI18NKey(IFormElement component, String i18nKey, String defaultValue)
	{
		List<Pair<String, String>> componentI18NKeys = componentsI18NKeys.get(component);
		if (componentI18NKeys == null)
		{
			componentI18NKeys = new ArrayList<Pair<String, String>>();
			componentsI18NKeys.put(component, componentI18NKeys);
		}
		Pair<String, String> pv = new Pair<String, String>(i18nKey, defaultValue);
		if (!componentI18NKeys.contains(pv)) componentI18NKeys.add(pv);
	}

	public static void saveFormI18NTexts(Form form) throws RepositoryException
	{
		ServoyProject activeProject = ServoyModelFinder.getServoyModel().getActiveProject();
		String activeSolutionI18NServerName = activeProject.getSolution().getI18nServerName();
		String activeSolutionI18NTableName = activeProject.getSolution().getI18nTableName();

		if (activeSolutionI18NServerName != null && activeSolutionI18NTableName != null &&
			ServoyModelFinder.getServoyModel().getActiveResourcesProject() != null)
		{
			final ArrayList<Pair<String, String>> i18nKeysToSave = new ArrayList<Pair<String, String>>();
			form.acceptVisitor(new IPersistVisitor()
			{
				@Override
				public Object visit(IPersist o)
				{
					if (o instanceof ISupportText || o instanceof WebComponent)
					{
						List<Pair<String, String>> componentI18NKeys = componentsI18NKeys.remove(o);
						if (componentI18NKeys != null) i18nKeysToSave.addAll(componentI18NKeys);
					}
					return IPersistVisitor.CONTINUE_TRAVERSAL;
				}
			});

			ArrayList<IFormElement> remainedI18NComponents = new ArrayList<IFormElement>();
			for (IFormElement textComponent : componentsI18NKeys.keySet())
			{
				Form f = (Form)((IPersist)textComponent).getAncestor(IRepository.FORMS);
				if (form.equals(f)) remainedI18NComponents.add(textComponent);
			}
			for (IFormElement textComponent : remainedI18NComponents)
			{
				componentsI18NKeys.remove(textComponent);
			}

			TreeMap<String, MessageEntry> messages = new TreeMap<String, MessageEntry>();
			for (Pair<String, String> i18nKeyValue : i18nKeysToSave)
			{
				MessageEntry messageEntry = new MessageEntry(null, i18nKeyValue.getLeft(), i18nKeyValue.getRight());
				messages.put(messageEntry.getLanguageKey(), messageEntry);
			}

			writeMessages(activeSolutionI18NServerName, activeSolutionI18NTableName, messages, new WorkspaceFileAccess(ResourcesPlugin.getWorkspace()),
				ServoyModelFinder.getServoyModel().getActiveResourcesProject().getProject(), false, false, false);
		}
	}
}
