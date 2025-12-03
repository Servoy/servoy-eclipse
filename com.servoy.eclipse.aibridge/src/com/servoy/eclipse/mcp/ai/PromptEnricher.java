package com.servoy.eclipse.mcp.ai;

import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.Solution;

/**
 * Enriches user prompts with Servoy-specific context and rules based on detected intent.
 * This is the bridge between intent detection and the existing enrichment methods.
 */
public class PromptEnricher
{

	private final IntentDetector intentDetector;

	public PromptEnricher()
	{
		this.intentDetector = new IntentDetector();
	}

	/**
	 * Process prompt: detect intent and enrich if needed
	 * @param prompt The user's original prompt
	 * @return Enriched prompt or "PASS_THROUGH"
	 */
	public String processPrompt(String prompt)
	{
		try
		{
			String intent = intentDetector.detectIntent(prompt);

			if (!intentDetector.isServoyIntent(intent))
			{
				return "PASS_THROUGH";
			}

			String enriched = enrichPrompt(intent, prompt);
			return enriched;
		}
		catch (Exception e)
		{
			ServoyLog.logError("[PromptEnricher] Error: " + e.getMessage());
			return "PASS_THROUGH";
		}
	}

	/**
	 * Enrich prompts based on user intent, using rules from RuleStore
	 */
	private String enrichPrompt(String intent, String prompt)
	{
		try
		{
			String rules = RulesCache.getRules(intent);

			if (rules.isEmpty())
			{
				ServoyLog.logError("[PromptEnricher] No rules found for intent: " + intent + ", returning PASS_THROUGH");
				return "PASS_THROUGH";
			}

			String context = gatherServoyContext();
			String projectName = getProjectName();
			String processedRules = rules.replace("{{PROJECT_NAME}}", projectName);

			String enriched = context + "\n\n" +
				"USER REQUEST:\n" + prompt + "\n\n" +
				processedRules + "\n\n" +
				"IMPORTANT: If you have all required parameters, call the appropriate tool immediately. " +
				"If any required parameters are missing or unclear, ASK THE USER before calling the tool.\n";

			ServoyLog.logInfo("[PromptEnricher] Final enriched prompt length: " + enriched.length() + " chars");
			return enriched;
		}
		catch (Exception e)
		{
			ServoyLog.logError("[PromptEnricher] Error: " + e.getMessage());
			return "PASS_THROUGH";
		}
	}

	/**
	 * Get the current project name for template substitution
	 * @return Project name or "UNKNOWN" if not available
	 */
	private String getProjectName()
	{
		try
		{
			IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
			ServoyProject activeProject = servoyModel.getActiveProject();

			if (activeProject != null)
			{
				return activeProject.getProject().getName();
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError("[PromptEnricher] Error getting project name: " + e.getMessage());
		}

		return "UNKNOWN";
	}

	/**
	 * Gather runtime context from the Servoy development environment
	 * @return Formatted context string with project info, datasources, etc.
	 */
	private String gatherServoyContext()
	{
		StringBuilder context = new StringBuilder();
		context.append("=== SERVOY DEVELOPMENT CONTEXT ===\n\n");

		try
		{
			IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
			ServoyProject activeProject = servoyModel.getActiveProject();

			if (activeProject != null)
			{
				// Project information
				context.append("**Current Project:** ").append(activeProject.getProject().getName()).append("\n");

				Solution solution = activeProject.getEditingSolution();
				if (solution != null)
				{
					context.append("**Active Solution:** ").append(solution.getName()).append("\n\n");
				}

				context.append("\n");
			}
			else
			{
				context.append("**No active Servoy project**\n\n");
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError("[PromptEnricher] Error gathering context: " + e.getMessage());
			context.append("**Error gathering context:** ").append(e.getMessage()).append("\n\n");
		}

		return context.toString();
	}
}
