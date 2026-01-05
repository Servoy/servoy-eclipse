package com.servoy.eclipse.knowledgebase.mcp.handlers;

import java.util.ArrayList;
import java.util.List;

import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.knowledgebase.mcp.services.ContextService;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;

/**
 * Abstract base class for handlers dealing with persisted Servoy objects
 * (relations, forms, valuelists, etc.).
 * Provides common context resolution and cross-module search functionality.
 */
public abstract class AbstractPersistenceHandler extends AbstractToolHandler
{
	// =============================================
	// CONTEXT RESOLUTION
	// =============================================

	/**
	 * Resolve target project based on current context.
	 * Returns active project if context is "active", otherwise finds module by name.
	 * 
	 * @param servoyModel The Servoy model
	 * @return Target ServoyProject
	 * @throws RepositoryException if context not found
	 */
	protected ServoyProject resolveTargetProject(IDeveloperServoyModel servoyModel) throws RepositoryException
	{
		String context = ContextService.getInstance().getCurrentContext();

		if ("active".equals(context))
		{
			return servoyModel.getActiveProject();
		}

		// Find module by name
		ServoyProject[] modules = servoyModel.getModulesOfActiveProject();
		for (ServoyProject module : modules)
		{
			if (module != null && module.getProject().getName().equals(context))
			{
				return module;
			}
		}

		throw new RepositoryException("Context '" + context + "' not found or not a module of active solution");
	}

	// =============================================
	// CROSS-MODULE SEARCH
	// =============================================

	/**
	 * Search result containing the found item and its location.
	 */
	protected static class SearchResult<T>
	{
		public final T item;
		public final String location;
		public final ServoyProject project;

		public SearchResult(T item, String location, ServoyProject project)
		{
			this.item = item;
			this.location = location;
			this.project = project;
		}
	}

	/**
	 * Functional interface for retrieving an item from a solution.
	 */
	@FunctionalInterface
	protected interface ItemGetter<T>
	{
		T get(Solution solution, String name);
	}

	/**
	 * Search for an item across all contexts (target, active solution, modules).
	 * 
	 * @param servoyModel The Servoy model
	 * @param targetProject Current target project
	 * @param name Item name to search for
	 * @param getter Function to retrieve item from solution
	 * @return List of all matches with their locations
	 */
	protected <T> List<SearchResult<T>> searchAcrossAllContexts(
		IDeveloperServoyModel servoyModel,
		ServoyProject targetProject,
		String name,
		ItemGetter<T> getter)
	{
		List<SearchResult<T>> results = new ArrayList<>();
		ServoyProject activeProject = servoyModel.getActiveProject();
		String targetContext = ContextService.getInstance().getCurrentContext();

		// Search in target context first
		if (targetProject != null && targetProject.getEditingSolution() != null)
		{
			T item = getter.get(targetProject.getEditingSolution(), name);
			if (item != null)
			{
				String location = "active".equals(targetContext)
					? targetProject.getProject().getName() + " (active solution)"
					: targetContext;
				results.add(new SearchResult<>(item, location, targetProject));
			}
		}

		// Search in active solution (if different from target)
		if (activeProject != null && !activeProject.equals(targetProject) && activeProject.getEditingSolution() != null)
		{
			T item = getter.get(activeProject.getEditingSolution(), name);
			if (item != null && !containsItem(results, item))
			{
				String location = activeProject.getProject().getName() + " (active solution)";
				results.add(new SearchResult<>(item, location, activeProject));
			}
		}

		// Search in all modules
		ServoyProject[] modules = servoyModel.getModulesOfActiveProject();
		for (ServoyProject module : modules)
		{
			if (module != null && module.getEditingSolution() != null &&
				!module.equals(targetProject) && !module.equals(activeProject))
			{
				T item = getter.get(module.getEditingSolution(), name);
				if (item != null && !containsItem(results, item))
				{
					String location = module.getProject().getName();
					results.add(new SearchResult<>(item, location, module));
				}
			}
		}

		return results;
	}

	/**
	 * Check if results list already contains the item (reference equality).
	 */
	private <T> boolean containsItem(List<SearchResult<T>> results, T item)
	{
		for (SearchResult<T> result : results)
		{
			if (result.item == item)
			{
				return true;
			}
		}
		return false;
	}

	// =============================================
	// APPROVAL FLOW HELPERS
	// =============================================

	/**
	 * Build approval request message for UPDATE/DELETE operations.
	 * 
	 * @param itemType Type of item (e.g., "Relation", "Form", "ValueList")
	 * @param itemName Name of the item
	 * @param foundContext Context where item was found
	 * @param currentContext Current context
	 * @param operation Operation to perform (e.g., "update", "delete")
	 * @return Approval request message
	 */
	protected String buildApprovalMessage(
		String itemType,
		String itemName,
		String foundContext,
		String currentContext,
		String operation)
	{
		StringBuilder msg = new StringBuilder();
		msg.append("Current context: ").append(currentContext).append("\n\n");
		msg.append(itemType).append(" '").append(itemName).append("' found in ").append(foundContext).append(".\n");
		msg.append("Current context is ").append(currentContext).append(".\n\n");
		msg.append("To ").append(operation).append(" this ").append(itemType.toLowerCase()).append(", I need to switch to ")
			.append(foundContext).append(".\n");
		msg.append("Do you want to proceed?\n\n");
		msg.append("[If yes, I will: setContext({context: \"");

		// Extract context name from display string
		String contextName = foundContext;
		if (foundContext.endsWith(" (active solution)"))
		{
			contextName = "active";
		}

		msg.append(contextName).append("\"}) then ").append(operation).append("]");
		return msg.toString();
	}

	// =============================================
	// FORMATTING UTILITIES
	// =============================================

	/**
	 * Format origin information for display.
	 * 
	 * @param context Context string ("active" or module name)
	 * @param project The project
	 * @return Formatted origin string
	 */
	protected String formatOriginInfo(String context, ServoyProject project)
	{
		if ("active".equals(context))
		{
			return project.getProject().getName() + " (active solution)";
		}
		return context;
	}

	/**
	 * Get solution name from project.
	 * 
	 * @param project The project
	 * @return Solution name, or project name if solution is null
	 */
	protected String getSolutionName(ServoyProject project)
	{
		if (project == null)
		{
			return "unknown";
		}
		if (project.getSolution() != null)
		{
			return project.getSolution().getName();
		}
		return project.getProject().getName();
	}

	/**
	 * Format context display string.
	 * 
	 * @param context Context ("active" or module name)
	 * @param project Project for active context
	 * @return Display string
	 */
	protected String getContextDisplay(String context, ServoyProject project)
	{
		if ("active".equals(context))
		{
			return getSolutionName(project) + " (active solution)";
		}
		return context;
	}
}
