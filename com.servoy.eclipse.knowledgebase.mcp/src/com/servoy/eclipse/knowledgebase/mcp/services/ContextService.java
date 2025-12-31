package com.servoy.eclipse.knowledgebase.mcp.services;

import java.util.ArrayList;
import java.util.List;

import com.servoy.eclipse.model.nature.ServoyProject;

/**
 * Manages the current context for write operations.
 * Context determines which solution/module will receive new items.
 */
public class ContextService
{
	private static ContextService instance;
	private String currentContext = null; // null = active solution

	public static synchronized ContextService getInstance()
	{
		if (instance == null)
		{
			instance = new ContextService();
		}
		return instance;
	}

	/**
	 * Get current context. Returns "active" for active solution,
	 * or module name like "Module_A".
	 */
	public String getCurrentContext()
	{
		return currentContext != null ? currentContext : "active";
	}

	/**
	 * Set current context. Use "active" for active solution,
	 * or module name. Null is treated as "active".
	 */
	public void setCurrentContext(String context)
	{
		this.currentContext = ("active".equals(context) || context == null) ? null : context;
	}

	/**
	 * Reset to active solution context.
	 * Called on solution activation.
	 */
	public void resetToActiveSolution()
	{
		this.currentContext = null;
	}

	/**
	 * Get list of available contexts (active solution + modules).
	 */
	public List<String> getAvailableContexts(ServoyProject activeProject)
	{
		List<String> contexts = new ArrayList<>();
		contexts.add("active");

		if (activeProject != null)
		{
			ServoyProject[] modules = getModuleProjects(activeProject);
			for (ServoyProject module : modules)
			{
				contexts.add(module.getProject().getName());
			}
		}

		return contexts;
	}

	private ServoyProject[] getModuleProjects(ServoyProject activeProject)
	{
		try
		{
			com.servoy.eclipse.model.extensions.IServoyModel model = com.servoy.eclipse.model.ServoyModelFinder.getServoyModel();
			return model.getModulesOfActiveProject();
		}
		catch (Exception e)
		{
			return new ServoyProject[0];
		}
	}
}
