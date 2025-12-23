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

package com.servoy.eclipse.knowledgebase;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.Manifest;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.sablo.specification.Package.IPackageReader;

import com.servoy.eclipse.knowledgebase.service.RulesCache;
import com.servoy.eclipse.knowledgebase.service.ServoyEmbeddingService;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyNGPackageProject;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.ngpackages.BaseNGPackageManager;
import com.servoy.eclipse.model.util.ServoyLog;

/**
 * Central manager for knowledge base operations.
 * Provides facade for:
 * - Loading/reloading knowledge bases from SPM packages
 * - Accessing embedding service and rules cache
 * - Discovering knowledge base bundles in active solution
 * 
 * This class is the main integration point for knowledge base core functionality.
 */
public class KnowledgeBaseManager
{
	/**
	 * Get the embedding service singleton.
	 * 
	 * @return ServoyEmbeddingService instance
	 */
	public static ServoyEmbeddingService getEmbeddingService()
	{
		return ServoyEmbeddingService.getInstance();
	}

	/**
	 * Get the rules cache.
	 * 
	 * @return RulesCache class (static methods)
	 */
	public static Class<RulesCache> getRulesCache()
	{
		return RulesCache.class;
	}

	/**
	 * Load knowledge bases for the active solution.
	 * Called automatically when solution activates.
	 * 
	 * Discovers all knowledge base packages in the solution and its modules:
	 * 1. Gets all NG packages from solution via ServoyProject.getNGPackageProjects()
	 * 2. Gets all NG packages from modules
	 * 3. Filters for knowledge base packages (Knowledge-Base: true in MANIFEST.MF)
	 * 4. Loads embeddings and rules from discovered packages
	 * 
	 * @param solution The active Servoy solution
	 */
	public static void loadKnowledgeBasesForSolution(Object solution)
	{
		if (solution instanceof ServoyProject servoyProject)
		{
			IPackageReader[] packageReaders = discoverKnowledgeBasePackagesInSolution(servoyProject);
			try
			{
				ServoyEmbeddingService embeddingService = ServoyEmbeddingService.getInstance();
				embeddingService.reloadAllKnowledgeBasesFromReaders(packageReaders);
				
				int embeddingCount = embeddingService.getEmbeddingCount();
				int ruleCount = RulesCache.getRuleCount();
				
				if (packageReaders.length > 0)
				{
					ServoyLog.logInfo("[KnowledgeBaseManager] Knowledge bases loaded successfully - " + 
						embeddingCount + " embeddings, " + ruleCount + " rules");
				}
			}
			catch (Exception e)
			{
				ServoyLog.logError("[KnowledgeBaseManager] Error loading/clearing knowledge bases: " + 
					e.getMessage(), e);
			}
		}
	}

	/**
	 * Load a specific knowledge base package.
	 * Called manually by user via UI action when selecting a package.
	 * This does an ADDITIVE load (does NOT clear existing knowledge base).
	 * 
	 * @param packageName The name of the knowledge base package to load
	 */
	public static void loadKnowledgeBase(String packageName)
	{
		ServoyLog.logInfo("[KnowledgeBaseManager] loadKnowledgeBase called for: " + packageName);
		
		try
		{
			BaseNGPackageManager ngPackageManager = ServoyModelFinder.getServoyModel().getNGPackageManager();
			
			if (ngPackageManager != null)
			{
				List<IPackageReader> allReaders = ngPackageManager.getAllPackageReaders();
			
				IPackageReader targetReader = null;
				for (IPackageReader reader : allReaders)
				{
					if (reader.getPackageName().equals(packageName))
					{
						targetReader = reader;
						break;
					}
				}
				
				if (targetReader != null)
				{
					if (isKnowledgeBasePackage(targetReader))
					{
						ServoyEmbeddingService embeddingService = ServoyEmbeddingService.getInstance();
						int newEmbeddings = embeddingService.loadKnowledgeBaseFromReader(targetReader);
						int newRules = RulesCache.loadFromPackageReader(targetReader);
						
						ServoyLog.logInfo("[KnowledgeBaseManager] Knowledge base loaded: " + packageName + 
							" (added " + newEmbeddings + " embeddings, " + newRules + " rules)");
					}
				}
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError("[KnowledgeBaseManager] Error loading knowledge base '" + packageName + "': " + 
				e.getMessage(), e);
		}
	}

	/**
	 * Reload all knowledge bases from all installed bundles.
	 * Called manually by user via UI action.
	 * Clears existing knowledge and reloads fresh from active solution.
	 */
	public static void reloadAllKnowledgeBases()
	{
		ServoyLog.logInfo("[KnowledgeBaseManager] reloadAllKnowledgeBases called (manual trigger)");
		ServoyEmbeddingService.getInstance().reloadAllKnowledgeBasesFromReaders(new IPackageReader[0]);
		
		ServoyProject activeProject = ServoyModelFinder.getServoyModel().getActiveProject();
		if (activeProject != null)
		{
			IPackageReader[] packageReaders = discoverKnowledgeBasePackagesInSolution(activeProject);
			try
			{
				ServoyEmbeddingService embeddingService = ServoyEmbeddingService.getInstance();
				embeddingService.reloadAllKnowledgeBasesFromReaders(packageReaders);
				
				int embeddingCount = embeddingService.getEmbeddingCount();
				int ruleCount = RulesCache.getRuleCount();
				
				ServoyLog.logInfo("[KnowledgeBaseManager] Reload complete - Loaded " + embeddingCount + 
					" embeddings and " + ruleCount + " rules from " + packageReaders.length + " package(s)");
			}
			
			catch (Exception e)
			{
				ServoyLog.logError("[KnowledgeBaseManager] Error reloading knowledge bases: " + e.getMessage(), e);
			}
		}
	}

	/**
	 * Discover knowledge base packages in a solution.
	 * 
	 * 1. Get ALL loaded package readers from NGPackageManager (includes workspace projects AND installed zips)
	 * 2. For each package reader, check MANIFEST.MF for Knowledge-Base: true
	 * 3. Check for embeddings/embeddings.list OR rules/rules.list
	 * 4. Return filtered list of knowledge base package readers
	 * 
	 * @param solution The Servoy solution to scan
	 * @return Array of package readers for knowledge base packages (no duplicates)
	 */
	private static IPackageReader[] discoverKnowledgeBasePackagesInSolution(ServoyProject solution)
	{
		String solutionName = solution.getProject().getName();
		ServoyLog.logError("[discoverKnowledgeBasePackagesInSolution: " + solutionName);
		List<IPackageReader> knowledgeBaseReaders = new ArrayList<>();
		Set<String> processedPackageNames = new HashSet<>();
		
		try
		{
			BaseNGPackageManager ngPackageManager = ServoyModelFinder.getServoyModel().getNGPackageManager();
			
			if (ngPackageManager != null)
			{
				List<IPackageReader> allReaders = ngPackageManager.getAllPackageReaders();
				ServoyLog.logInfo("[KnowledgeBaseManager] Checking " + allReaders.size() + " loaded package(s) for knowledge bases");
				
				// Check each package reader for knowledge base markers
				for (IPackageReader reader : allReaders)
				{
					String packageName = reader.getPackageName();
					if (processedPackageNames.contains(packageName))
					{
						continue;
					}
						
					if (isKnowledgeBasePackage(reader))
					{
						knowledgeBaseReaders.add(reader);
						processedPackageNames.add(packageName);
						ServoyLog.logInfo("[KnowledgeBaseManager] Found knowledge base package: " + packageName);
					}
				}
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError("[KnowledgeBaseManager] Error discovering knowledge base packages: " + 
				e.getMessage(), e);
		}
		
		return knowledgeBaseReaders.toArray(new IPackageReader[0]);
	}
	
	/**
	 * Check if a package reader is a knowledge base package.
	 * Works with ANY IPackageReader (workspace projects, zips, etc.)
	 * 
	 * Requirements:
	 * 1. MANIFEST.MF must contain Knowledge-Base: true
	 * 2. Must have embeddings/embeddings.list AND rules/rules.list
	 * 
	 * @param reader The package reader to check
	 * @return true if this is a knowledge base package
	 */
	private static boolean isKnowledgeBasePackage(IPackageReader reader)
	{
		try
		{
			Manifest manifest = reader.getManifest();
			if (manifest != null)
			{
				
				String knowledgeBase = manifest.getMainAttributes().getValue("Knowledge-Base");
				if ("true".equalsIgnoreCase(knowledgeBase))
				{
					boolean hasEmbeddings = reader.getUrlForPath("embeddings/embeddings.list") != null;
					boolean hasRules = reader.getUrlForPath("rules/rules.list") != null;
					
					return hasEmbeddings && hasRules;
				}
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError("[KnowledgeBaseManager] Error checking if package reader is knowledge base: " + 
				reader.getPackageName() + " - " + e.getMessage(), e);
		}
		return false;
	}
}
