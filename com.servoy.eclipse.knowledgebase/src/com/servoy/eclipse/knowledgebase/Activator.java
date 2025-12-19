package com.servoy.eclipse.knowledgebase;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.servoy.eclipse.core.IActiveProjectListener;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.knowledgebase.ai.ServoyEmbeddingService;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;

/**
 * Activator for the Servoy Knowledge Base plugin.
 * Manages lifecycle of embedding service and knowledge base loading.
 * 
 * This is a non-UI plugin that provides knowledge base services to other plugins
 * via OSGi bundle lookup (Platform.getBundle()) and reflection - no compile-time dependencies.
 * 
 * Solution Activation Strategy:
 * - Registers IActiveProjectListener to track solution changes
 * - When solution activates: clears existing knowledge base, discovers and loads knowledge base packages
 * - Knowledge base packages are NOT auto-reloaded on updates - user must use context menu actions
 */
public class Activator implements BundleActivator
{
	public static final String PLUGIN_ID = "com.servoy.eclipse.knowledgebase";
	
	private static Activator plugin;
	private static BundleContext context;
	private IActiveProjectListener solutionActivationListener;

	public static Activator getDefault()
	{
		return plugin;
	}

	public static BundleContext getContext()
	{
		return context;
	}

	@Override
	public void start(BundleContext bundleContext) throws Exception
	{
		Activator.context = bundleContext;
		Activator.plugin = this;
		
		System.out.println("========================================");
		System.out.println("[KnowledgeBase] PLUGIN STARTING...");
		System.out.println("========================================");
		ServoyLog.logInfo("[KnowledgeBase] Plugin starting...");
		
		// Initialize embedding service with ONNX models (without knowledge base initially)
		try
		{
			System.out.println("[KnowledgeBase] Initializing embedding service...");
			ServoyLog.logInfo("[KnowledgeBase] Initializing embedding service...");
			ServoyEmbeddingService.getInstance();
			System.out.println("[KnowledgeBase] Embedding service initialized successfully");
			ServoyLog.logInfo("[KnowledgeBase] Embedding service initialized successfully");
		}
		catch (Exception e)
		{
			System.err.println("[KnowledgeBase] FAILED to initialize embedding service: " + e.getMessage());
			ServoyLog.logError("[KnowledgeBase] Failed to initialize embedding service: " + e.getMessage(), e);
		}
		
		// Register solution activation listener
		System.out.println("[KnowledgeBase] Registering solution activation listener...");
		registerSolutionActivationListener();
		
		// Load knowledge bases for currently active solution (if any)
		System.out.println("[KnowledgeBase] Loading knowledge bases for current solution (if any)...");
		loadKnowledgeBasesForCurrentSolution();

		System.out.println("[KnowledgeBase] PLUGIN STARTED");
		System.out.println("========================================");
		ServoyLog.logInfo("[KnowledgeBase] Plugin started - service accessible via Activator.getDefault()");
	}
	
	/**
	 * Registers listener for solution activation events.
	 * When solution changes, clears and reloads knowledge base.
	 */
	private void registerSolutionActivationListener()
	{
		try
		{
			System.out.println("[KnowledgeBase] Getting ServoyModel...");
			ServoyModel servoyModel = (ServoyModel)ServoyModelFinder.getServoyModel();
			System.out.println("[KnowledgeBase] ServoyModel obtained: " + servoyModel);
			
			solutionActivationListener = new IActiveProjectListener()
			{
				@Override
				public void activeProjectChanged(ServoyProject activeProject)
				{
					handleSolutionActivation(activeProject);
				}
				
				@Override
				public boolean activeProjectWillChange(ServoyProject from, ServoyProject to)
				{
					System.out.println("[KnowledgeBase] Solution will change from: " + 
						(from != null ? from.getProject().getName() : "null") + " to: " + 
						(to != null ? to.getProject().getName() : "null"));
					// Allow all solution changes
					return true;
				}
				
				@Override
				public void activeProjectUpdated(ServoyProject activeProject, int updateInfo)
				{
					// Do NOT react to updates - user must manually reload via context menu
				}
			};
			
			servoyModel.addActiveProjectListener(solutionActivationListener);
			System.out.println("[KnowledgeBase] Solution activation listener registered successfully");
			ServoyLog.logInfo("[KnowledgeBase] Solution activation listener registered");
		}
		catch (Exception e)
		{
			System.err.println("[KnowledgeBase] FAILED to register solution activation listener: " + e.getMessage());
			e.printStackTrace();
			ServoyLog.logError("[KnowledgeBase] Failed to register solution activation listener: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Handles solution activation event.
	 * Clears existing knowledge base and loads packages for new solution.
	 * 
	 * @param activeProject The newly activated solution (or null if no solution active)
	 */
	private void handleSolutionActivation(ServoyProject activeProject)
	{
		System.out.println("========================================");
		System.out.println("[KnowledgeBase] SOLUTION ACTIVATION EVENT");
		System.out.println("========================================");
		
		if (activeProject != null)
		{
			String solutionName = activeProject.getProject().getName();
			System.out.println("[KnowledgeBase] Solution activated: " + solutionName);
			ServoyLog.logInfo("[KnowledgeBase] Solution activated: " + solutionName);
			
			// Load knowledge bases for new solution
			// This will automatically clear existing knowledge base first
			try
			{
				System.out.println("[KnowledgeBase] Calling KnowledgeBaseManager.loadKnowledgeBasesForSolution()...");
				KnowledgeBaseManager.loadKnowledgeBasesForSolution(activeProject);
				System.out.println("[KnowledgeBase] Knowledge bases loaded for solution: " + solutionName);
			}
			catch (Exception e)
			{
				System.err.println("[KnowledgeBase] ERROR loading knowledge bases: " + e.getMessage());
				e.printStackTrace();
				ServoyLog.logError("[KnowledgeBase] Error loading knowledge bases for solution: " + e.getMessage(), e);
			}
		}
		else
		{
			// No active solution - clear knowledge base by reloading with empty array
			System.out.println("[KnowledgeBase] No active solution - clearing knowledge base");
			ServoyLog.logInfo("[KnowledgeBase] No active solution - clearing knowledge base");
			try
			{
				org.sablo.specification.Package.IPackageReader[] emptyReaders = 
					new org.sablo.specification.Package.IPackageReader[0];
				ServoyEmbeddingService.getInstance().reloadAllKnowledgeBasesFromReaders(emptyReaders);
				System.out.println("[KnowledgeBase] Knowledge base cleared");
			}
			catch (Exception e)
			{
				System.err.println("[KnowledgeBase] ERROR clearing knowledge base: " + e.getMessage());
				e.printStackTrace();
				ServoyLog.logError("[KnowledgeBase] Error clearing knowledge base: " + e.getMessage(), e);
			}
		}
		
		System.out.println("[KnowledgeBase] Solution activation event handled");
		System.out.println("========================================");
	}
	
	/**
	 * Loads knowledge bases for currently active solution at plugin startup.
	 */
	private void loadKnowledgeBasesForCurrentSolution()
	{
		try
		{
			System.out.println("[KnowledgeBase] Checking for active solution at startup...");
			ServoyProject activeProject = ServoyModelFinder.getServoyModel().getActiveProject();
			if (activeProject != null)
			{
				String solutionName = activeProject.getProject().getName();
				System.out.println("[KnowledgeBase] Active solution found: " + solutionName);
				ServoyLog.logInfo("[KnowledgeBase] Loading knowledge bases for current solution: " + solutionName);
				KnowledgeBaseManager.loadKnowledgeBasesForSolution(activeProject);
				System.out.println("[KnowledgeBase] Knowledge bases loaded for startup solution: " + solutionName);
			}
			else
			{
				System.out.println("[KnowledgeBase] No active solution at startup");
				ServoyLog.logInfo("[KnowledgeBase] No active solution at startup");
			}
		}
		catch (Exception e)
		{
			System.err.println("[KnowledgeBase] ERROR loading knowledge bases at startup: " + e.getMessage());
			e.printStackTrace();
			ServoyLog.logError("[KnowledgeBase] Error loading knowledge bases at startup: " + e.getMessage(), e);
		}
	}

	@Override
	public void stop(BundleContext bundleContext) throws Exception
	{
		System.out.println("========================================");
		System.out.println("[KnowledgeBase] PLUGIN STOPPING...");
		System.out.println("========================================");
		ServoyLog.logInfo("[KnowledgeBase] Plugin stopping...");
		
		// Unregister listener
		if (solutionActivationListener != null)
		{
			try
			{
				System.out.println("[KnowledgeBase] Unregistering solution activation listener...");
				ServoyModel servoyModel = (ServoyModel)ServoyModelFinder.getServoyModel();
				servoyModel.removeActiveProjectListener(solutionActivationListener);
				System.out.println("[KnowledgeBase] Solution activation listener unregistered");
				ServoyLog.logInfo("[KnowledgeBase] Solution activation listener unregistered");
			}
			catch (Exception e)
			{
				System.err.println("[KnowledgeBase] ERROR unregistering listener: " + e.getMessage());
				e.printStackTrace();
				ServoyLog.logError("[KnowledgeBase] Error unregistering listener: " + e.getMessage(), e);
			}
		}
		
		// Clear knowledge base (reload with empty package reader array)
		try
		{
			System.out.println("[KnowledgeBase] Clearing knowledge base...");
			org.sablo.specification.Package.IPackageReader[] emptyReaders = 
				new org.sablo.specification.Package.IPackageReader[0];
			ServoyEmbeddingService.getInstance().reloadAllKnowledgeBasesFromReaders(emptyReaders);
			System.out.println("[KnowledgeBase] Knowledge base cleared");
			ServoyLog.logInfo("[KnowledgeBase] Knowledge base cleared");
		}
		catch (Exception e)
		{
			System.err.println("[KnowledgeBase] ERROR clearing knowledge base: " + e.getMessage());
			e.printStackTrace();
			ServoyLog.logError("[KnowledgeBase] Error clearing knowledge base: " + e.getMessage(), e);
		}

		Activator.context = null;
		Activator.plugin = null;

		System.out.println("[KnowledgeBase] PLUGIN STOPPED");
		System.out.println("========================================");
		ServoyLog.logInfo("[KnowledgeBase] Plugin stopped");
	}
}
