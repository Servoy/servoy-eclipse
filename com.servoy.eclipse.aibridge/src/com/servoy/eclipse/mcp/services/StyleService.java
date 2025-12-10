package com.servoy.eclipse.mcp.services;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.servoy.eclipse.model.util.ServoyLog;

/**
 * Service for managing CSS/LESS styles in Servoy solutions.
 * 
 * Handles CRUD operations for CSS classes in LESS files:
 * - Styles are stored in <solution-name>.less by default
 * - Can optionally organize styles in separate .less files (model chooses filename)
 * - Ensures imports are added to main solution .less file
 * 
 * CRITICAL: All styles go to solution .less file by default, NOT ai-generated.less
 */
public class StyleService
{
	/**
	 * Adds or updates a CSS class in a LESS file.
	 * 
	 * @param projectPath Path to Servoy project root
	 * @param solutionName Name of the solution (for main .less file)
	 * @param lessFileName Name of LESS file to add style to (null = main solution file)
	 * @param className CSS class name (without dot)
	 * @param cssContent CSS rules (semicolon-separated or multi-line)
	 * @return null if successful, error message string if failed
	 */
	public static String addOrUpdateStyle(String projectPath, String solutionName, String lessFileName, 
		String className, String cssContent)
	{
		try
		{
			System.out.println("[StyleService] ========================================");
			System.out.println("[StyleService] addOrUpdateStyle CALLED");
			System.out.println("[StyleService] className='" + className + "'");
			System.out.println("[StyleService] lessFile='" + (lessFileName != null ? lessFileName : solutionName + ".less") + "'");
			System.out.println("[StyleService] cssContent RECEIVED (length=" + cssContent.length() + "):");
			System.out.println("[StyleService] --- START RECEIVED CONTENT ---");
			System.out.println(cssContent.length() > 500 ? cssContent.substring(0, 500) + "\n... (truncated)" : cssContent);
			System.out.println("[StyleService] --- END RECEIVED CONTENT ---");
			
			// STEP 1: Validate the content AS-IS before any modifications
			System.out.println("[StyleService] Starting validation of received content...");
			String validationError = validateLessContent(cssContent);
			if (validationError != null)
			{
				System.out.println("[StyleService] VALIDATION FAILED: " + validationError);
				ServoyLog.logError("[StyleService] CSS validation failed: " + validationError);
				return validationError; // Return immediately - no file operations
			}
			System.out.println("[StyleService] Validation PASSED");
			
			// Determine target LESS file
			String targetFile = (lessFileName != null && !lessFileName.trim().isEmpty()) 
				? lessFileName 
				: solutionName + ".less";
			
			if (!targetFile.endsWith(".less"))
			{
				targetFile += ".less";
			}
			
			Path lessPath = Paths.get(projectPath, "medias", targetFile);
			
			// Create file if doesn't exist
			if (!Files.exists(lessPath))
			{
				Files.createDirectories(lessPath.getParent());
				Files.write(lessPath, "/* Styles */\n\n".getBytes());
			}
			
			// Read existing content
			String content = new String(Files.readAllBytes(lessPath));
			
			// STEP 2: Check if content already has the class wrapper
			String trimmed = cssContent.trim();
			boolean hasClassWrapper = trimmed.matches("^\\." + Pattern.quote(className) + "\\s*\\{.*");
			
			System.out.println("[StyleService] Has class wrapper (.className {...}): " + hasClassWrapper);
			
			String newClassDef;
			if (hasClassWrapper)
			{
				// Model already provided the complete class definition - use as-is
				System.out.println("[StyleService] Using content as-is (already has class wrapper)");
				newClassDef = trimmed;
				// Ensure it ends with proper formatting
				if (!newClassDef.endsWith("\n"))
				{
					newClassDef += "\n";
				}
			}
			else
			{
				// Model provided only inner content - add class wrapper
				System.out.println("[StyleService] Adding class wrapper to content");
				String formattedCss = formatLessContent(cssContent);
				newClassDef = "." + className + " {\n" + formattedCss + "\n}\n";
			}
			
			// Create backup ONLY after validation passed
			Path backupPath = Paths.get(lessPath.toString() + ".backup");
			Files.copy(lessPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
			
			System.out.println("[StyleService] FINAL CLASS DEFINITION to write:");
			System.out.println("[StyleService] --- START FINAL CSS ---");
			System.out.println(newClassDef.length() > 500 ? newClassDef.substring(0, 500) + "\n... (truncated)" : newClassDef);
			System.out.println("[StyleService] --- END FINAL CSS ---");
			
			// Check if class already exists
			Pattern pattern = Pattern.compile("\\." + Pattern.quote(className) + "\\s*\\{[^}]*\\}", Pattern.DOTALL);
			Matcher matcher = pattern.matcher(content);
			
			if (matcher.find())
			{
				// Replace existing
				content = matcher.replaceFirst(Matcher.quoteReplacement(newClassDef));
				System.out.println("[StyleService] Updated existing class: " + className);
			}
			else
			{
				// Append new
				content += "\n" + newClassDef;
				System.out.println("[StyleService] Added new class: " + className);
			}
			
			// Write updated content
			Files.write(lessPath, content.getBytes());
			
			// If this is a separate file (not main solution file), ensure it's imported
			if (lessFileName != null && !lessFileName.trim().isEmpty() && 
				!targetFile.equals(solutionName + ".less"))
			{
				String importError = ensureImportInMainLess(projectPath, solutionName, targetFile);
				if (importError != null)
				{
					return importError;
				}
			}
			
			ServoyLog.logInfo("[StyleService] Successfully added/updated style: " + className);
			return null;
		}
		catch (Exception e)
		{
			ServoyLog.logError("[StyleService] Error adding/updating style: " + e.getMessage(), e);
			return "Error adding/updating style: " + e.getMessage();
		}
	}
	
	/**
	 * Gets the CSS content of a class from a LESS file.
	 * 
	 * @param projectPath Path to Servoy project root
	 * @param solutionName Name of the solution
	 * @param lessFileName Name of LESS file (null = search in main file)
	 * @param className CSS class name (without dot)
	 * @return CSS content if found, or error message
	 */
	public static String getStyle(String projectPath, String solutionName, String lessFileName, String className)
	{
		try
		{
			String targetFile = (lessFileName != null && !lessFileName.trim().isEmpty()) 
				? lessFileName 
				: solutionName + ".less";
			
			if (!targetFile.endsWith(".less"))
			{
				targetFile += ".less";
			}
			
			Path lessPath = Paths.get(projectPath, "medias", targetFile);
			
			if (!Files.exists(lessPath))
			{
				return "LESS file not found: " + targetFile;
			}
			
			String content = new String(Files.readAllBytes(lessPath));
			
			// Find class definition
			Pattern pattern = Pattern.compile("\\." + Pattern.quote(className) + "\\s*\\{([^}]*)\\}", Pattern.DOTALL);
			Matcher matcher = pattern.matcher(content);
			
			if (matcher.find())
			{
				String cssContent = matcher.group(1).trim();
				return cssContent;
			}
			
			return "Class '" + className + "' not found in " + targetFile;
		}
		catch (Exception e)
		{
			ServoyLog.logError("[StyleService] Error getting style: " + e.getMessage(), e);
			return "Error getting style: " + e.getMessage();
		}
	}
	
	/**
	 * Lists all CSS class names in a LESS file.
	 * 
	 * @param projectPath Path to Servoy project root
	 * @param solutionName Name of the solution
	 * @param lessFileName Name of LESS file (null = main solution file)
	 * @return Comma-separated list of class names, or error message
	 */
	public static String listStyles(String projectPath, String solutionName, String lessFileName)
	{
		try
		{
			String targetFile = (lessFileName != null && !lessFileName.trim().isEmpty()) 
				? lessFileName 
				: solutionName + ".less";
			
			if (!targetFile.endsWith(".less"))
			{
				targetFile += ".less";
			}
			
			Path lessPath = Paths.get(projectPath, "medias", targetFile);
			
			if (!Files.exists(lessPath))
			{
				return "LESS file not found: " + targetFile;
			}
			
			String content = new String(Files.readAllBytes(lessPath));
			
			// Find all class definitions
			Pattern pattern = Pattern.compile("\\.([a-zA-Z0-9_-]+)\\s*\\{", Pattern.MULTILINE);
			Matcher matcher = pattern.matcher(content);
			
			List<String> classes = new ArrayList<>();
			while (matcher.find())
			{
				classes.add(matcher.group(1));
			}
			
			if (classes.isEmpty())
			{
				return "No CSS classes found in " + targetFile;
			}
			
			return String.join(", ", classes);
		}
		catch (Exception e)
		{
			ServoyLog.logError("[StyleService] Error listing styles: " + e.getMessage(), e);
			return "Error listing styles: " + e.getMessage();
		}
	}
	
	/**
	 * Deletes a CSS class from a LESS file.
	 * 
	 * @param projectPath Path to Servoy project root
	 * @param solutionName Name of the solution
	 * @param lessFileName Name of LESS file (null = main solution file)
	 * @param className CSS class name (without dot)
	 * @return null if successful, error message string if failed
	 */
	public static String deleteStyle(String projectPath, String solutionName, String lessFileName, String className)
	{
		try
		{
			String targetFile = (lessFileName != null && !lessFileName.trim().isEmpty()) 
				? lessFileName 
				: solutionName + ".less";
			
			if (!targetFile.endsWith(".less"))
			{
				targetFile += ".less";
			}
			
			Path lessPath = Paths.get(projectPath, "medias", targetFile);
			
			if (!Files.exists(lessPath))
			{
				return "LESS file not found: " + targetFile;
			}
			
			String content = new String(Files.readAllBytes(lessPath));
			
			// Create backup
			Path backupPath = Paths.get(lessPath.toString() + ".backup");
			Files.copy(lessPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
			
			// Find and remove class definition
			Pattern pattern = Pattern.compile("\\." + Pattern.quote(className) + "\\s*\\{[^}]*\\}\\s*", Pattern.DOTALL);
			Matcher matcher = pattern.matcher(content);
			
			if (!matcher.find())
			{
				return "Class '" + className + "' not found in " + targetFile;
			}
			
			content = matcher.replaceFirst("");
			
			// Write updated content
			Files.write(lessPath, content.getBytes());
			
			ServoyLog.logInfo("[StyleService] Successfully deleted style: " + className);
			return null;
		}
		catch (Exception e)
		{
			ServoyLog.logError("[StyleService] Error deleting style: " + e.getMessage(), e);
			return "Error deleting style: " + e.getMessage();
		}
	}
	
	/**
	 * Ensures a LESS file is imported in the main solution LESS file.
	 * 
	 * @param projectPath Path to Servoy project root
	 * @param solutionName Name of the solution
	 * @param lessFileName LESS file to import
	 * @return null if successful, error message string if failed
	 */
	private static String ensureImportInMainLess(String projectPath, String solutionName, String lessFileName)
	{
		try
		{
			Path mainLessPath = Paths.get(projectPath, "medias", solutionName + ".less");
			
			// Create main file if doesn't exist
			if (!Files.exists(mainLessPath))
			{
				Files.createDirectories(mainLessPath.getParent());
				Files.write(mainLessPath, ("/* " + solutionName + " styles */\n\n").getBytes());
			}
			
			String content = new String(Files.readAllBytes(mainLessPath));
			String importStatement = "@import '" + lessFileName + "';";
			
			// Check if already imported
			if (content.contains(importStatement) || content.contains("@import \"" + lessFileName + "\";"))
			{
				return null; // Already imported
			}
			
			// Create backup
			Path backupPath = Paths.get(mainLessPath.toString() + ".backup");
			Files.copy(mainLessPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
			
			// Add import at appropriate location (after comments, before styles)
			String[] lines = content.split("\n");
			StringBuilder newContent = new StringBuilder();
			boolean importAdded = false;
			
			for (int i = 0; i < lines.length; i++)
			{
				String line = lines[i].trim();
				
				// Add import after existing imports or after initial comments
				if (!importAdded && !line.startsWith("//") && !line.startsWith("/*") && 
					!line.isEmpty() && !line.startsWith("@import"))
				{
					newContent.append(importStatement).append("\n");
					importAdded = true;
				}
				
				newContent.append(lines[i]).append("\n");
			}
			
			// If we reached end without adding, add at end
			if (!importAdded)
			{
				newContent.append(importStatement).append("\n");
			}
			
			Files.write(mainLessPath, newContent.toString().getBytes());
			
			ServoyLog.logInfo("[StyleService] Added import for " + lessFileName + " to " + solutionName + ".less");
			return null;
		}
		catch (Exception e)
		{
			ServoyLog.logError("[StyleService] Error ensuring import: " + e.getMessage(), e);
			return "Error ensuring import: " + e.getMessage();
		}
	}
	
	/**
	 * Preprocesses CSS content to strip class selector if model incorrectly included it.
	 * Model should send only content, but sometimes sends full class definition.
	 * 
	 * IMPORTANT: Distinguishes between:
	 * 1. Single wrapper: .className { properties } - needs unwrapping
	 * 2. Complete LESS: .className { } .className:hover { } @keyframes { } - use as-is
	 * 
	 * @param cssContent The CSS content from the model
	 * @param className The class name to detect and strip
	 * @return Clean CSS content (wrapper stripped if present)
	 */
	private static String preprocessCssContent(String cssContent, String className)
	{
		System.out.println("[StyleService] ===== PREPROCESSING =====");
		System.out.println("[StyleService] className: '" + className + "'");
		System.out.println("[StyleService] cssContent length: " + cssContent.length());
		System.out.println("[StyleService] cssContent (first 300 chars): " + 
			(cssContent.length() > 300 ? cssContent.substring(0, 300) + "..." : cssContent));
		
		String trimmed = cssContent.trim();
		System.out.println("[StyleService] After trim, first line: " + 
			(trimmed.contains("\n") ? trimmed.substring(0, Math.min(100, trimmed.indexOf('\n'))) : trimmed.substring(0, Math.min(100, trimmed.length()))));
		
		// Check if content starts with ".className {" or "className {"
		String pattern1 = "^\\." + Pattern.quote(className) + "\\s*\\{";
		String pattern2 = "^" + Pattern.quote(className) + "\\s*\\{";
		
		System.out.println("[StyleService] Testing pattern1: " + pattern1);
		System.out.println("[StyleService] Testing pattern2: " + pattern2);
		
		boolean matchesPattern1 = trimmed.matches(pattern1 + ".*");
		boolean matchesPattern2 = trimmed.matches(pattern2 + ".*");
		
		System.out.println("[StyleService] Matches pattern1 (." + className + " {): " + matchesPattern1);
		System.out.println("[StyleService] Matches pattern2 (" + className + " {): " + matchesPattern2);
		
		if (matchesPattern1 || matchesPattern2)
		{
			System.out.println("[StyleService] Class name detected at start - checking if wrapper or complete LESS...");
			
			// Check if this is a COMPLETE LESS definition (multiple rules/at-rules) or a SINGLE wrapper
			// Count top-level closing braces after the first opening brace
			int firstBrace = trimmed.indexOf('{');
			if (firstBrace == -1)
			{
				System.out.println("[StyleService] ERROR - no opening brace found");
				return cssContent;
			}
			
			// Find the matching closing brace for the first opening brace
			int braceDepth = 1;
			int matchingCloseBrace = -1;
			
			for (int i = firstBrace + 1; i < trimmed.length(); i++)
			{
				char c = trimmed.charAt(i);
				if (c == '{') braceDepth++;
				else if (c == '}')
				{
					braceDepth--;
					if (braceDepth == 0)
					{
						matchingCloseBrace = i;
						break;
					}
				}
			}
			
			System.out.println("[StyleService] First brace at: " + firstBrace + ", matching close at: " + matchingCloseBrace);
			
			if (matchingCloseBrace == -1)
			{
				System.out.println("[StyleService] ERROR - no matching closing brace");
				return cssContent;
			}
			
			// Check if there's significant content after the matching closing brace
			String afterFirstRule = trimmed.substring(matchingCloseBrace + 1).trim();
			System.out.println("[StyleService] Content after first rule (length=" + afterFirstRule.length() + "): " + 
				(afterFirstRule.length() > 100 ? afterFirstRule.substring(0, 100) + "..." : afterFirstRule));
			
			// If there's significant content after (other rules, keyframes, etc.), this is a COMPLETE LESS definition
			// Check for: other class definitions (.className:hover, .className::before), @keyframes, @media, etc.
			boolean hasAdditionalRules = afterFirstRule.length() > 0 && 
				(afterFirstRule.contains("{") || 
				 afterFirstRule.matches(".*\\." + Pattern.quote(className) + ".*\\{.*") ||
				 afterFirstRule.matches(".*@(keyframes|media|supports).*\\{.*"));
			
			System.out.println("[StyleService] Has additional rules after first block: " + hasAdditionalRules);
			
			if (hasAdditionalRules)
			{
				System.out.println("[StyleService] COMPLETE LESS DEFINITION detected - using as-is (no unwrapping)");
				System.out.println("[StyleService] This appears to be multiple rules/keyframes, not a single wrapper");
				System.out.println("[StyleService] ===== END PREPROCESSING =====");
				return cssContent;
			}
			
			// Single wrapper case - extract inner content
			System.out.println("[StyleService] SINGLE WRAPPER detected - unwrapping it...");
			String innerContent = trimmed.substring(firstBrace + 1, matchingCloseBrace).trim();
			System.out.println("[StyleService] Extracted inner content length: " + innerContent.length());
			System.out.println("[StyleService] Extracted content (first 200 chars): " + 
				(innerContent.length() > 200 ? innerContent.substring(0, 200) + "..." : innerContent));
			System.out.println("[StyleService] SUCCESS - wrapper stripped");
			System.out.println("[StyleService] ===== END PREPROCESSING =====");
			return innerContent;
		}
		else
		{
			System.out.println("[StyleService] NO WRAPPER DETECTED - using content as-is");
		}
		
		System.out.println("[StyleService] ===== END PREPROCESSING =====");
		return cssContent;
	}
	
	/**
	 * Validates LESS/CSS content for common syntax errors.
	 * Returns null if valid, or detailed error message if invalid.
	 * 
	 * @param cssContent The LESS/CSS content to validate
	 * @return null if valid, error message string if invalid
	 */
	private static String validateLessContent(String cssContent)
	{
		// Check for duplicate opening braces (e.g., "{ {")
		if (cssContent.matches(".*\\{\\s*\\{.*"))
		{
			return "CSS syntax error: Duplicate opening brace '{ {' detected. " +
				"Each selector should have only one opening brace. " +
				"Example: '.btn { color: red; }' not '.btn { { color: red; }'";
		}
		
		// Check for duplicate closing braces (e.g., "} }")
		if (cssContent.matches(".*\\}\\s*\\}(?!\\s*$).*"))
		{
			// Allow }} at end (for nested LESS), but not in middle
			String trimmed = cssContent.trim();
			if (!trimmed.endsWith("}") || cssContent.replaceAll("\\s", "").contains("}}}"))
			{
				return "CSS syntax error: Duplicate closing brace '} }' detected. " +
					"Check your brace pairing.";
			}
		}
		
		// Check for invalid closing sequence "};}" or "};"
		if (cssContent.matches(".*\\}\\s*;\\s*\\}.*"))
		{
			return "CSS syntax error: Invalid closing sequence '};' detected before final brace. " +
				"Remove the semicolon between closing braces. " +
				"Example: '} }' not '}; }'";
		}
		
		// Check for balanced braces
		int openCount = 0;
		int closeCount = 0;
		int lineNum = 1;
		String[] lines = cssContent.split("\n");
		
		for (String line : lines)
		{
			for (char c : line.toCharArray())
			{
				if (c == '{') openCount++;
				if (c == '}') closeCount++;
			}
			
			// Check if closing more than opened so far
			if (closeCount > openCount)
			{
				return "CSS syntax error: Unbalanced braces at line " + lineNum + ". " +
					"Found closing brace '}' without matching opening brace '{'. " +
					"Total so far: " + openCount + " opening, " + closeCount + " closing.";
			}
			
			lineNum++;
		}
		
		// Final balance check
		if (openCount != closeCount)
		{
			return "CSS syntax error: Unbalanced braces. " +
				"Found " + openCount + " opening braces '{' but " + closeCount + " closing braces '}'. " +
				"Each opening brace must have a matching closing brace.";
		}
		
		// Check for invalid semicolon after closing brace at class level
		if (cssContent.matches(".*\\}\\s*;\\s*$"))
		{
			return "CSS syntax error: Invalid semicolon after final closing brace. " +
				"Remove the semicolon. CSS classes end with '}' not '};'. " +
				"Example: '.btn { color: red; }' not '.btn { color: red; };'";
		}
		
		return null; // Valid
	}
	
	/**
	 * Formats LESS/CSS content with proper indentation.
	 * Preserves LESS syntax including nested selectors, variables, etc.
	 * Does NOT split on semicolons - treats content as complete LESS block.
	 * 
	 * @param cssContent The LESS/CSS content to format
	 * @return Formatted content with proper indentation
	 */
	private static String formatLessContent(String cssContent)
	{
		// Trim leading/trailing whitespace
		String content = cssContent.trim();
		
		// If content is already multi-line and properly formatted, preserve it
		if (content.contains("\n") && content.contains("{"))
		{
			// Already formatted (likely LESS with nesting) - just ensure proper indentation
			StringBuilder formatted = new StringBuilder();
			String[] lines = content.split("\n");
			int indentLevel = 0;
			
			for (String line : lines)
			{
				String trimmed = line.trim();
				if (trimmed.isEmpty()) continue;
				
				// Decrease indent before closing brace
				if (trimmed.startsWith("}"))
				{
					indentLevel = Math.max(0, indentLevel - 1);
				}
				
				// Add indentation
				for (int i = 0; i < indentLevel; i++)
				{
					formatted.append("  ");
				}
				formatted.append(trimmed).append("\n");
				
				// Increase indent after opening brace
				if (trimmed.endsWith("{"))
				{
					indentLevel++;
				}
			}
			
			return formatted.toString().trim();
		}
		
		// Simple flat CSS (semicolon-separated) - format with basic indentation
		if (!content.contains("{") && content.contains(";"))
		{
			String[] rules = content.split(";");
			StringBuilder formatted = new StringBuilder();
			
			for (String rule : rules)
			{
				rule = rule.trim();
				if (!rule.isEmpty())
				{
					formatted.append("  ").append(rule).append(";\n");
				}
			}
			
			return formatted.toString().trim();
		}
		
		// Return as-is with basic indentation
		return "  " + content.replace("\n", "\n  ");
	}
}
