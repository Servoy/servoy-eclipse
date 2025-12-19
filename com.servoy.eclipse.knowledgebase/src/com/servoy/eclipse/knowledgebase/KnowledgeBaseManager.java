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
		System.out.println("========================================");
		System.out.println("[KnowledgeBaseManager] loadKnowledgeBasesForSolution() CALLED");
		
		if (!(solution instanceof ServoyProject))
		{
			System.err.println("[KnowledgeBaseManager] ERROR: Invalid solution type: " + 
				(solution != null ? solution.getClass().getName() : "null"));
			ServoyLog.logError("[KnowledgeBaseManager] Invalid solution type: " + 
				(solution != null ? solution.getClass().getName() : "null"));
			return;
		}
		
		ServoyProject servoyProject = (ServoyProject)solution;
		String solutionName = servoyProject.getProject().getName();
		System.out.println("[KnowledgeBaseManager] Solution: " + solutionName);
		ServoyLog.logInfo("[KnowledgeBaseManager] Loading knowledge bases for solution: " + solutionName);
		
		// Discover knowledge base packages
		System.out.println("[KnowledgeBaseManager] Discovering knowledge base packages...");
		IPackageReader[] packageReaders = discoverKnowledgeBasePackagesInSolution(servoyProject);
		
		System.out.println("[KnowledgeBaseManager] Discovered " + packageReaders.length + 
			" knowledge base package(s)");
		ServoyLog.logInfo("[KnowledgeBaseManager] Discovered " + packageReaders.length + 
			" knowledge base package(s)");
		
		// ALWAYS reload (clears existing knowledge base even if no packages found)
		try
		{
			if (packageReaders.length > 0)
			{
				System.out.println("[KnowledgeBaseManager] Loading knowledge bases from " + 
					packageReaders.length + " package(s)...");
			}
			else
			{
				System.out.println("[KnowledgeBaseManager] No knowledge base packages found - clearing existing knowledge base");
			}
			
			ServoyEmbeddingService embeddingService = ServoyEmbeddingService.getInstance();
			embeddingService.reloadAllKnowledgeBasesFromReaders(packageReaders);
			
			int embeddingCount = embeddingService.getEmbeddingCount();
			int ruleCount = RulesCache.getRuleCount();
			
			if (packageReaders.length > 0)
			{
				System.out.println("[KnowledgeBaseManager] Knowledge bases loaded successfully:");
				System.out.println("  - Embeddings: " + embeddingCount);
				System.out.println("  - Rules: " + ruleCount);
				ServoyLog.logInfo("[KnowledgeBaseManager] Knowledge bases loaded successfully - " + 
					embeddingCount + " embeddings, " + ruleCount + " rules");
			}
			else
			{
				System.out.println("[KnowledgeBaseManager] Knowledge base cleared (no packages in solution)");
				System.out.println("  - Embeddings: " + embeddingCount + " (should be 0)");
				System.out.println("  - Rules: " + ruleCount + " (should be 0)");
				ServoyLog.logInfo("[KnowledgeBaseManager] Knowledge base cleared - no packages in solution");
			}
		}
		catch (Exception e)
		{
			System.err.println("[KnowledgeBaseManager] ERROR loading/clearing knowledge bases: " + e.getMessage());
			e.printStackTrace();
			ServoyLog.logError("[KnowledgeBaseManager] Error loading/clearing knowledge bases: " + 
				e.getMessage(), e);
		}
		
		System.out.println("[KnowledgeBaseManager] loadKnowledgeBasesForSolution() COMPLETED");
		System.out.println("========================================");
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
		System.out.println("========================================");
		System.out.println("[KnowledgeBaseManager] loadKnowledgeBase() CALLED");
		System.out.println("[KnowledgeBaseManager] Package name: " + packageName);
		ServoyLog.logInfo("[KnowledgeBaseManager] loadKnowledgeBase called for: " + packageName);
		
		if (packageName == null || packageName.trim().isEmpty())
		{
			System.err.println("[KnowledgeBaseManager] ERROR: Invalid package name");
			ServoyLog.logError("[KnowledgeBaseManager] Invalid package name: " + packageName);
			return;
		}
		
		try
		{
			// Find the package in the active solution
			System.out.println("[KnowledgeBaseManager] Searching for package in active solution...");
			ServoyProject activeProject = ServoyModelFinder.getServoyModel().getActiveProject();
			if (activeProject == null)
			{
				System.err.println("[KnowledgeBaseManager] ERROR: No active solution");
				ServoyLog.logError("[KnowledgeBaseManager] No active solution - cannot load package");
				return;
			}
			
			System.out.println("[KnowledgeBaseManager] Active solution: " + activeProject.getProject().getName());
			
			// Search in solution packages
			ServoyNGPackageProject[] packages = activeProject.getNGPackageProjects();
			System.out.println("[KnowledgeBaseManager] Searching " + packages.length + " packages in solution...");
			
			ServoyNGPackageProject targetPackage = null;
			for (ServoyNGPackageProject pkg : packages)
			{
				if (pkg.getProject().getName().equals(packageName))
				{
					targetPackage = pkg;
					System.out.println("[KnowledgeBaseManager] Found package in solution!");
					break;
				}
			}
			
			// If not found, search in modules
			if (targetPackage == null)
			{
				System.out.println("[KnowledgeBaseManager] Not found in solution, searching modules...");
				ServoyProject[] modules = ServoyModelFinder.getServoyModel().getModulesOfActiveProject();
				for (ServoyProject module : modules)
				{
					ServoyNGPackageProject[] modulePackages = module.getNGPackageProjects();
					for (ServoyNGPackageProject pkg : modulePackages)
					{
						if (pkg.getProject().getName().equals(packageName))
						{
							targetPackage = pkg;
							System.out.println("[KnowledgeBaseManager] Found package in module: " + 
								module.getProject().getName());
							break;
						}
					}
					if (targetPackage != null) break;
				}
			}
			
			if (targetPackage == null)
			{
				System.err.println("[KnowledgeBaseManager] ERROR: Package not found: " + packageName);
				ServoyLog.logError("[KnowledgeBaseManager] Package not found in solution or modules: " + packageName);
				return;
			}
			
			// Verify it's a knowledge base package
			System.out.println("[KnowledgeBaseManager] Verifying package is a knowledge base package...");
			if (!isKnowledgeBasePackage(targetPackage))
			{
				System.err.println("[KnowledgeBaseManager] ERROR: Package is not a knowledge base package");
				ServoyLog.logError("[KnowledgeBaseManager] Package is not a knowledge base package: " + packageName);
				return;
			}
			
			System.out.println("[KnowledgeBaseManager] Package verified as knowledge base package");
			
			// Create package reader
			System.out.println("[KnowledgeBaseManager] Creating package reader...");
			IProject project = targetPackage.getProject();
			File projectLocation = project.getLocation().toFile();
			DirPackageReader reader = new DirPackageReader(projectLocation);
			
			System.out.println("[KnowledgeBaseManager] Loading knowledge base (ADDITIVE - does not clear existing)...");
			
			// Load embeddings
			ServoyEmbeddingService embeddingService = ServoyEmbeddingService.getInstance();
			int oldEmbeddingCount = embeddingService.getEmbeddingCount();
			int oldRuleCount = RulesCache.getRuleCount();
			
			System.out.println("[KnowledgeBaseManager] Current state before load:");
			System.out.println("  - Embeddings: " + oldEmbeddingCount);
			System.out.println("  - Rules: " + oldRuleCount);
			
			// Load from this package reader (additive)
			int newEmbeddings = embeddingService.loadKnowledgeBaseFromReader(reader);
			int newRules = RulesCache.loadFromPackageReader(reader);
			
			int totalEmbeddings = embeddingService.getEmbeddingCount();
			int totalRules = RulesCache.getRuleCount();
			
			System.out.println("[KnowledgeBaseManager] Knowledge base loaded successfully!");
			System.out.println("  - New embeddings from package: " + newEmbeddings);
			System.out.println("  - New rules from package: " + newRules);
			System.out.println("  - Total embeddings now: " + totalEmbeddings);
			System.out.println("  - Total rules now: " + totalRules);
			
			ServoyLog.logInfo("[KnowledgeBaseManager] Knowledge base loaded: " + packageName + 
				" (added " + newEmbeddings + " embeddings, " + newRules + " rules)");
		}
		catch (Exception e)
		{
			System.err.println("[KnowledgeBaseManager] ERROR loading knowledge base: " + e.getMessage());
			e.printStackTrace();
			ServoyLog.logError("[KnowledgeBaseManager] Error loading knowledge base '" + packageName + "': " + 
				e.getMessage(), e);
		}
		
		System.out.println("[KnowledgeBaseManager] loadKnowledgeBase() COMPLETED");
		System.out.println("========================================");
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
	 * 1. Get all NG packages from solution via ServoyProject.getNGPackageProjects()
	 * 2. Get all NG packages from modules (excluding the main solution to avoid duplicates)
	 * 3. Filter for knowledge base packages (Knowledge-Base: true in MANIFEST.MF)
	 * 4. Create IPackageReader for each package (workspace projects, not OSGi bundles)
	 * 
	 * @param solution The Servoy solution to scan
	 * @return Array of package readers for knowledge base packages (no duplicates)
	 */
	private static IPackageReader[] discoverKnowledgeBasePackagesInSolution(ServoyProject solution)
	{
		System.out.println("[KnowledgeBaseManager] discoverKnowledgeBaseBundlesInSolution() starting...");
		Set<String> processedProjects = new HashSet<>();
		List<ServoyNGPackageProject> knowledgeBasePackages = new ArrayList<>();
		String solutionName = solution.getProject().getName();
		
		try
		{
			// Get NG packages from solution
			System.out.println("[KnowledgeBaseManager] Getting NG packages from solution...");
			ServoyNGPackageProject[] ngPackages = solution.getNGPackageProjects();
			System.out.println("[KnowledgeBaseManager] Found " + ngPackages.length + 
				" NG package(s) in solution: " + solutionName);
			ServoyLog.logInfo("[KnowledgeBaseManager] Found " + ngPackages.length + 
				" NG package(s) in solution: " + solutionName);
			
			for (ServoyNGPackageProject ngPackage : ngPackages)
			{
				try
				{
					String packageName = ngPackage.getProject().getName();
					System.out.println("[KnowledgeBaseManager] Checking package: " + packageName);
					
					if (processedProjects.contains(packageName))
					{
						System.out.println("[KnowledgeBaseManager]   -> Already processed (skipped)");
						continue;
					}
					
					if (isKnowledgeBasePackage(ngPackage))
					{
						System.out.println("[KnowledgeBaseManager]   -> IS a knowledge base package");
						knowledgeBasePackages.add(ngPackage);
						processedProjects.add(packageName);
						System.out.println("[KnowledgeBaseManager]   -> Added to knowledge base list");
						ServoyLog.logInfo("[KnowledgeBaseManager] Found knowledge base package: " + packageName);
					}
					else
					{
						System.out.println("[KnowledgeBaseManager]   -> Not a knowledge base package (skipped)");
					}
				}
				catch (Exception e)
				{
					System.err.println("[KnowledgeBaseManager] ERROR checking package: " + 
						ngPackage.getProject().getName() + " - " + e.getMessage());
					ServoyLog.logError("[KnowledgeBaseManager] Error checking package: " + 
						ngPackage.getProject().getName() + " - " + e.getMessage(), e);
				}
			}
			
			// Get NG packages from modules (excluding main solution to avoid duplicates)
			System.out.println("[KnowledgeBaseManager] Checking modules...");
			ServoyProject[] modules = ServoyModelFinder.getServoyModel().getModulesOfActiveProject();
			if (modules != null && modules.length > 0)
			{
				System.out.println("[KnowledgeBaseManager] Found " + modules.length + " module(s)");
				ServoyLog.logInfo("[KnowledgeBaseManager] Checking " + modules.length + " module(s)");
				
				for (ServoyProject module : modules)
				{
					try
					{
						String moduleName = module.getProject().getName();
						
						// Skip if this is the main solution (to avoid duplicates)
						if (moduleName.equals(solutionName))
						{
							System.out.println("[KnowledgeBaseManager] Skipping module: " + moduleName + " (is main solution)");
							continue;
						}
						
						System.out.println("[KnowledgeBaseManager] Checking module: " + moduleName);
						ServoyNGPackageProject[] modulePackages = module.getNGPackageProjects();
						System.out.println("[KnowledgeBaseManager]   -> Found " + modulePackages.length + " NG package(s)");
						ServoyLog.logInfo("[KnowledgeBaseManager] Found " + modulePackages.length + 
							" NG package(s) in module: " + moduleName);
						
						for (ServoyNGPackageProject ngPackage : modulePackages)
						{
							try
							{
								String packageName = ngPackage.getProject().getName();
								System.out.println("[KnowledgeBaseManager] Checking package: " + packageName);
								
								if (processedProjects.contains(packageName))
								{
									System.out.println("[KnowledgeBaseManager]   -> Already processed (skipped)");
									continue;
								}
								
								if (isKnowledgeBasePackage(ngPackage))
								{
									System.out.println("[KnowledgeBaseManager]   -> IS a knowledge base package");
									knowledgeBasePackages.add(ngPackage);
									processedProjects.add(packageName);
									System.out.println("[KnowledgeBaseManager]   -> Added to knowledge base list");
									ServoyLog.logInfo("[KnowledgeBaseManager] Found knowledge base package in module: " + packageName);
								}
								else
								{
									System.out.println("[KnowledgeBaseManager]   -> Not a knowledge base package (skipped)");
								}
							}
							catch (Exception e)
							{
								System.err.println("[KnowledgeBaseManager] ERROR checking module package: " + 
									ngPackage.getProject().getName() + " - " + e.getMessage());
								ServoyLog.logError("[KnowledgeBaseManager] Error checking module package: " + 
									ngPackage.getProject().getName() + " - " + e.getMessage(), e);
							}
						}
					}
					catch (Exception e)
					{
						System.err.println("[KnowledgeBaseManager] ERROR processing module: " + 
							module.getProject().getName() + " - " + e.getMessage());
						ServoyLog.logError("[KnowledgeBaseManager] Error processing module: " + 
							module.getProject().getName() + " - " + e.getMessage(), e);
					}
				}
			}
		}
		catch (Exception e)
		{
			System.err.println("[KnowledgeBaseManager] ERROR discovering knowledge base bundles: " + e.getMessage());
			e.printStackTrace();
			ServoyLog.logError("[KnowledgeBaseManager] Error discovering knowledge base bundles: " + 
				e.getMessage(), e);
		}
		
		System.out.println("[KnowledgeBaseManager] discoverKnowledgeBasePackagesInSolution() completed:");
		System.out.println("  -> Total knowledge base packages found: " + knowledgeBasePackages.size());
		
		// Create IPackageReader for each knowledge base package
		List<IPackageReader> packageReaders = new ArrayList<>();
		for (ServoyNGPackageProject pkg : knowledgeBasePackages)
		{
			try
			{
				String packageName = pkg.getProject().getName();
				System.out.println("[KnowledgeBaseManager] Creating package reader for: " + packageName);
				
				IProject project = pkg.getProject();
				File projectLocation = project.getLocation().toFile();
				
				// Create DirPackageReader for workspace project
				DirPackageReader reader = new DirPackageReader(projectLocation);
				packageReaders.add(reader);
				
				System.out.println("     - " + packageName + " (reader created)");
			}
			catch (Exception e)
			{
				System.err.println("[KnowledgeBaseManager] ERROR creating reader for " + 
					pkg.getProject().getName() + ": " + e.getMessage());
				e.printStackTrace();
				ServoyLog.logError("[KnowledgeBaseManager] Error creating package reader: " + e.getMessage(), e);
			}
		}
		
		IPackageReader[] result = packageReaders.toArray(new IPackageReader[0]);
		System.out.println("[KnowledgeBaseManager] Created " + result.length + " package reader(s)");
		return result;
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
