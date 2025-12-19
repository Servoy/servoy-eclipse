package com.servoy.eclipse.knowledgebase;

import java.io.File;
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
import org.sablo.specification.Package.DirPackageReader;
import org.sablo.specification.Package.IPackageReader;

import com.servoy.eclipse.knowledgebase.ai.RulesCache;
import com.servoy.eclipse.knowledgebase.ai.ServoyEmbeddingService;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyNGPackageProject;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;

/**
 * Central manager for knowledge base operations.
 * Provides facade for:
 * - Loading/reloading knowledge bases from SPM packages
 * - Accessing handlers for MCP tool registration
 * - Discovering knowledge base bundles in active solution
 * 
 * This class is the main integration point between aibridge and knowledgebase plugins.
 */
public class KnowledgeBaseManager
{
	/**
	 * Get all tool handlers for MCP registration.
	 * Called by aibridge's McpServletProvider to register tools.
 * 
 * @return Array of all tool handler instances
 */
public static IToolHandler[] getHandlers()
{
return ToolHandlerRegistry.getHandlers();
}

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
		if (!(solution instanceof ServoyProject))
		{
			ServoyLog.logError("[KnowledgeBaseManager] Invalid solution type: " + 
				(solution != null ? solution.getClass().getName() : "null"));
			return;
		}
		
		ServoyProject servoyProject = (ServoyProject)solution;
		String solutionName = servoyProject.getProject().getName();
		ServoyLog.logInfo("[KnowledgeBaseManager] Loading knowledge bases for solution: " + solutionName);
		
		// Discover knowledge base packages
		IPackageReader[] packageReaders = discoverKnowledgeBasePackagesInSolution(servoyProject);
		
		ServoyLog.logInfo("[KnowledgeBaseManager] Discovered " + packageReaders.length + 
			" knowledge base package(s)");
		
		// ALWAYS reload (clears existing knowledge base even if no packages found)
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
			else
			{
				ServoyLog.logInfo("[KnowledgeBaseManager] Knowledge base cleared - no packages in solution");
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError("[KnowledgeBaseManager] Error loading/clearing knowledge bases: " + 
				e.getMessage(), e);
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
		
		if (packageName == null || packageName.trim().isEmpty())
		{
			ServoyLog.logError("[KnowledgeBaseManager] Invalid package name: " + packageName);
			return;
		}
		
		try
		{
			// Get NGPackageManager
			com.servoy.eclipse.model.ngpackages.BaseNGPackageManager ngPackageManager = 
				ServoyModelFinder.getServoyModel().getNGPackageManager();
			
			if (ngPackageManager == null)
			{
				ServoyLog.logError("[KnowledgeBaseManager] NGPackageManager is null");
				return;
			}
			
			// Get ALL loaded package readers (includes workspace projects AND installed zips)
			List<IPackageReader> allReaders = ngPackageManager.getAllPackageReaders();
			
			// Find the target package by name
			IPackageReader targetReader = null;
			for (IPackageReader reader : allReaders)
			{
				if (reader.getPackageName().equals(packageName))
				{
					targetReader = reader;
					break;
				}
			}
			
			if (targetReader == null)
			{
				ServoyLog.logError("[KnowledgeBaseManager] Package not found in loaded packages: " + packageName);
				return;
			}
			
			// Verify it's a knowledge base package
			if (!isKnowledgeBasePackage(targetReader))
			{
				ServoyLog.logError("[KnowledgeBaseManager] Package is not a knowledge base package: " + packageName);
				return;
			}
			
			// Load from this package reader (additive)
			ServoyEmbeddingService embeddingService = ServoyEmbeddingService.getInstance();
			int newEmbeddings = embeddingService.loadKnowledgeBaseFromReader(targetReader);
			int newRules = RulesCache.loadFromPackageReader(targetReader);
			
			ServoyLog.logInfo("[KnowledgeBaseManager] Knowledge base loaded: " + packageName + 
				" (added " + newEmbeddings + " embeddings, " + newRules + " rules)");
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
		
		// Get active solution
		ServoyProject activeProject = ServoyModelFinder.getServoyModel().getActiveProject();
		if (activeProject == null)
		{
			ServoyLog.logInfo("[KnowledgeBaseManager] No active solution - clearing knowledge base");
			try
			{
				ServoyEmbeddingService.getInstance().reloadAllKnowledgeBasesFromReaders(new IPackageReader[0]);
			}
			catch (Exception e)
			{
				ServoyLog.logError("[KnowledgeBaseManager] Error clearing knowledge base: " + e.getMessage(), e);
			}
			return;
		}
		
		// Discover all knowledge base packages in active solution
		IPackageReader[] packageReaders = discoverKnowledgeBasePackagesInSolution(activeProject);
		
		ServoyLog.logInfo("[KnowledgeBaseManager] Discovered " + packageReaders.length + 
			" knowledge base package(s) for reload");
		
		try
		{
			// Reload knowledge bases (clears existing first)
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

	/**
	 * Discover knowledge base packages in a solution.
	 * 
	 * Strategy:
	 * 1. Get ALL loaded package readers from NGPackageManager (includes workspace projects AND installed zips)
	 * 2. For each package reader, check MANIFEST.MF for Knowledge-Base: true
	 * 3. Check for embeddings/embeddings.list OR rules/rules.list
	 * 4. Return filtered list of knowledge base package readers
	 * 
	 * This approach works for BOTH:
	 * - Workspace projects with NG package nature (source)
	 * - Installed zip packages from Servoy Package Manager
	 * 
	 * @param solution The Servoy solution to scan
	 * @return Array of package readers for knowledge base packages (no duplicates)
	 */
	private static IPackageReader[] discoverKnowledgeBasePackagesInSolution(ServoyProject solution)
	{
		String solutionName = solution.getProject().getName();
		List<IPackageReader> knowledgeBaseReaders = new ArrayList<>();
		Set<String> processedPackageNames = new HashSet<>();
		
		try
		{
			// Get NGPackageManager from ServoyModel
			com.servoy.eclipse.model.ngpackages.BaseNGPackageManager ngPackageManager = 
				ServoyModelFinder.getServoyModel().getNGPackageManager();
			
			if (ngPackageManager == null)
			{
				ServoyLog.logError("[KnowledgeBaseManager] NGPackageManager is null");
				return new IPackageReader[0];
			}
			
			// Get ALL loaded package readers (includes workspace projects AND installed zips)
			List<IPackageReader> allReaders = ngPackageManager.getAllPackageReaders();
			ServoyLog.logInfo("[KnowledgeBaseManager] Checking " + allReaders.size() + " loaded package(s) for knowledge bases");
			
			// Check each package reader for knowledge base markers
			for (IPackageReader reader : allReaders)
			{
				try
				{
					String packageName = reader.getPackageName();
					
					// Skip duplicates
					if (processedPackageNames.contains(packageName))
					{
						continue;
					}
					
					// Check if this is a knowledge base package
					if (isKnowledgeBasePackage(reader))
					{
						knowledgeBaseReaders.add(reader);
						processedPackageNames.add(packageName);
						ServoyLog.logInfo("[KnowledgeBaseManager] Found knowledge base package: " + packageName);
					}
				}
				catch (Exception e)
				{
					ServoyLog.logError("[KnowledgeBaseManager] Error checking package reader: " + e.getMessage(), e);
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
	 * 2. Must have embeddings/embeddings.list OR rules/rules.list
	 * 
	 * @param reader The package reader to check
	 * @return true if this is a knowledge base package
	 */
	private static boolean isKnowledgeBasePackage(IPackageReader reader)
	{
		try
		{
			// Check MANIFEST.MF for Knowledge-Base: true
			Manifest manifest = reader.getManifest();
			if (manifest == null)
			{
				return false;
			}
			
			String knowledgeBase = manifest.getMainAttributes().getValue("Knowledge-Base");
			if (!"true".equalsIgnoreCase(knowledgeBase))
			{
				return false;
			}
			
			// Check for embeddings/embeddings.list OR rules/rules.list
			boolean hasEmbeddings = reader.getUrlForPath("embeddings/embeddings.list") != null;
			boolean hasRules = reader.getUrlForPath("rules/rules.list") != null;
			
			return hasEmbeddings || hasRules;
		}
		catch (Exception e)
		{
			ServoyLog.logError("[KnowledgeBaseManager] Error checking if package reader is knowledge base: " + 
				reader.getPackageName() + " - " + e.getMessage(), e);
			return false;
		}
	}
	
	/**
	 * Check if an NG package is a knowledge base package.
	 * 
	 * Requirements:
	 * 1. MANIFEST.MF must contain Knowledge-Base: true
	 * 2. Must have embeddings/embeddings.list OR rules/rules.list
	 * 
	 * @param ngPackage The NG package project to check
	 * @return true if this is a knowledge base package
	 */
	private static boolean isKnowledgeBasePackage(ServoyNGPackageProject ngPackage)
	{
		try
		{
			IProject project = ngPackage.getProject();
			
			// Check MANIFEST.MF for Knowledge-Base: true
			IFile manifestFile = project.getFile("META-INF/MANIFEST.MF");
			if (!manifestFile.exists())
			{
				return false;
			}
			
			try (InputStream is = manifestFile.getContents())
			{
				Manifest manifest = new Manifest(is);
				String knowledgeBase = manifest.getMainAttributes().getValue("Knowledge-Base");
				
				if (!"true".equalsIgnoreCase(knowledgeBase))
				{
					return false;
				}
			}
			
			// Check for embeddings/embeddings.list OR rules/rules.list
			IFile embeddingsList = project.getFile("embeddings/embeddings.list");
			IFile rulesList = project.getFile("rules/rules.list");
			
			return embeddingsList.exists() || rulesList.exists();
		}
		catch (Exception e)
		{
			ServoyLog.logError("[KnowledgeBaseManager] Error checking if package is knowledge base: " + 
				ngPackage.getProject().getName() + " - " + e.getMessage(), e);
			return false;
		}
	}
	
	/**
	 * Get OSGi Bundle for an NG package project.
	 * 
	 * @param ngPackage The NG package project
	 * @return The corresponding OSGi Bundle, or null if not found
	 */
	private static Bundle getBundleForPackage(ServoyNGPackageProject ngPackage)
	{
		try
		{
			IProject project = ngPackage.getProject();
			String projectName = project.getName();
			
			// Try direct bundle lookup by project name
			Bundle bundle = Platform.getBundle(projectName);
			if (bundle != null)
			{
				return bundle;
			}
			
			// If not found, try reading symbolic name from MANIFEST.MF
			IFile manifestFile = project.getFile("META-INF/MANIFEST.MF");
			if (manifestFile.exists())
			{
				try (InputStream is = manifestFile.getContents())
				{
					Manifest manifest = new Manifest(is);
					String symbolicName = manifest.getMainAttributes().getValue("Bundle-SymbolicName");
					if (symbolicName != null)
					{
						// Remove any directives (e.g., ";singleton:=true")
						symbolicName = symbolicName.split(";")[0].trim();
						bundle = Platform.getBundle(symbolicName);
						if (bundle != null)
						{
							return bundle;
						}
					}
				}
			}
			
			ServoyLog.logInfo("[KnowledgeBaseManager] Could not find OSGi bundle for package: " + projectName);
			return null;
		}
		catch (Exception e)
		{
			ServoyLog.logError("[KnowledgeBaseManager] Error getting bundle for package: " + 
				ngPackage.getProject().getName() + " - " + e.getMessage(), e);
			return null;
		}
	}
	
	/**
	 * Check if a bundle is a knowledge base bundle.
	 * 
	 * @param bundle The OSGi bundle to check
	 * @return true if this bundle has Knowledge-Base: true in its manifest
	 */
	private static boolean isKnowledgeBaseBundle(Bundle bundle)
	{
		try
		{
			String knowledgeBase = bundle.getHeaders().get("Knowledge-Base");
			return "true".equalsIgnoreCase(knowledgeBase);
		}
		catch (Exception e)
		{
			ServoyLog.logError("[KnowledgeBaseManager] Error checking bundle: " + 
				bundle.getSymbolicName() + " - " + e.getMessage(), e);
			return false;
		}
	}
}
