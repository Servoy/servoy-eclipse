package com.servoy.eclipse.knowledgebase;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.servoy.eclipse.core.IActiveProjectListener;
import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.knowledgebase.ai.ServoyEmbeddingService;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.extensions.IServoyModel;
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
		
		ServoyLog.logInfo("[KnowledgeBase] Plugin starting...");
		
		// Initialize embedding service with ONNX models (without knowledge base initially)
		try
		{
			ServoyLog.logInfo("[KnowledgeBase] Initializing embedding service...");
			ServoyEmbeddingService.getInstance();
			ServoyLog.logInfo("[KnowledgeBase] Embedding service initialized successfully");
		}
		catch (Exception e)
		{
			ServoyLog.logError("[KnowledgeBase] Failed to initialize embedding service: " + e.getMessage(), e);
		}
		
		// Register solution activation listener
		registerSolutionActivationListener();
		
		// Load knowledge bases for currently active solution (if any)
		loadKnowledgeBasesForCurrentSolution();

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
			IServoyModel servoyModel = ServoyModelFinder.getServoyModel();
			
			// Cast to IDeveloperServoyModel which has addActiveProjectListener
			if (!(servoyModel instanceof IDeveloperServoyModel))
			{
				ServoyLog.logError("[KnowledgeBase] ServoyModel is not IDeveloperServoyModel - cannot register listener");
				return;
			}
			
			IDeveloperServoyModel developerModel = (IDeveloperServoyModel)servoyModel;
			
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
					// Allow all solution changes
					return true;
				}
				
				@Override
				public void activeProjectUpdated(ServoyProject activeProject, int updateInfo)
				{
					// Do NOT react to updates - user must manually reload via context menu
				}
			};
			
			developerModel.addActiveProjectListener(solutionActivationListener);
			ServoyLog.logInfo("[KnowledgeBase] Solution activation listener registered");
		}
		catch (Exception e)
		{
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
			ServoyLog.logInfo("[KnowledgeBase] Solution activated: " + solutionName);
			
			// Load knowledge bases for new solution
			// This will automatically clear existing knowledge base first
			try
			{
				KnowledgeBaseManager.loadKnowledgeBasesForSolution(activeProject);
			}
			catch (Exception e)
			{
				ServoyLog.logError("[KnowledgeBase] Error loading knowledge bases for solution: " + e.getMessage(), e);
			}
		}
		else
		{
			// No active solution - clear knowledge base by reloading with empty array
			ServoyLog.logInfo("[KnowledgeBase] No active solution - clearing knowledge base");
			try
			{
				org.sablo.specification.Package.IPackageReader[] emptyReaders = 
					new org.sablo.specification.Package.IPackageReader[0];
				ServoyEmbeddingService.getInstance().reloadAllKnowledgeBasesFromReaders(emptyReaders);
			}
			catch (Exception e)
			{
				ServoyLog.logError("[KnowledgeBase] Error clearing knowledge base: " + e.getMessage(), e);
			}
		}
	}
	
	/**
	 * Loads knowledge bases for currently active solution at plugin startup.
	 */
	private void loadKnowledgeBasesForCurrentSolution()
	{
		try
		{
			ServoyProject activeProject = ServoyModelFinder.getServoyModel().getActiveProject();
			if (activeProject != null)
			{
				String solutionName = activeProject.getProject().getName();
				ServoyLog.logInfo("[KnowledgeBase] Loading knowledge bases for current solution: " + solutionName);
				KnowledgeBaseManager.loadKnowledgeBasesForSolution(activeProject);
			}
			else
			{
				ServoyLog.logInfo("[KnowledgeBase] No active solution at startup");
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError("[KnowledgeBase] Error loading knowledge bases at startup: " + e.getMessage(), e);
		}
	}

	@Override
	public void stop(BundleContext bundleContext) throws Exception
	{
		ServoyLog.logInfo("[KnowledgeBase] Plugin stopping...");
		
		// Unregister listener
		if (solutionActivationListener != null)
		{
			try
			{
				IServoyModel servoyModel = ServoyModelFinder.getServoyModel();
				if (servoyModel instanceof IDeveloperServoyModel)
				{
					IDeveloperServoyModel developerModel = (IDeveloperServoyModel)servoyModel;
					developerModel.removeActiveProjectListener(solutionActivationListener);
					ServoyLog.logInfo("[KnowledgeBase] Solution activation listener unregistered");
				}
			}
			catch (Exception e)
			{
				ServoyLog.logError("[KnowledgeBase] Error unregistering listener: " + e.getMessage(), e);
			}
		}
		
		// Clear knowledge base (reload with empty package reader array)
		try
		{
			org.sablo.specification.Package.IPackageReader[] emptyReaders = 
				new org.sablo.specification.Package.IPackageReader[0];
			ServoyEmbeddingService.getInstance().reloadAllKnowledgeBasesFromReaders(emptyReaders);
			ServoyLog.logInfo("[KnowledgeBase] Knowledge base cleared");
		}
		catch (Exception e)
		{
			ServoyLog.logError("[KnowledgeBase] Error clearing knowledge base: " + e.getMessage(), e);
		}

		Activator.context = null;
		Activator.plugin = null;

		ServoyLog.logInfo("[KnowledgeBase] Plugin stopped");
	}
}
