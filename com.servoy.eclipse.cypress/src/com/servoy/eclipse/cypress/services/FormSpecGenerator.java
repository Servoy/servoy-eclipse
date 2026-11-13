package com.servoy.eclipse.cypress.services;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.eclipse.e4.core.di.annotations.Creatable;

import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.ISupportFormElement;
import com.servoy.j2db.persistence.WebComponent;

@Creatable
public class FormSpecGenerator
{
	public static final String SPEC_CY_JS_EXTENSION = ".spec.cy.js";
	public static final String SPEC_CY_TS_EXTENSION = ".spec.cy.ts";
	/** Form spec extensions the tooling understands; a repo may keep its form tests in either language. */
	public static final List<String> SPEC_CY_EXTENSIONS = List.of(SPEC_CY_JS_EXTENSION, SPEC_CY_TS_EXTENSION);
	private static final String SPEC_JS_EXTENSION = ".spec.js";
	private static final String SPEC_TS_EXTENSION = ".spec.ts";
	/** Setup/tearDown file extensions recognised in cy-form-spec/. A NEW setup file is always written as .spec.js (a Servoy scope file). */
	public static final List<String> SPEC_SETUP_EXTENSIONS = List.of(SPEC_JS_EXTENSION, SPEC_TS_EXTENSION);
	private static final String FORM_SPEC_RELATIVE_DIR = "jenkins-custom/e2e-test-scripts/cypress/cy-form";
	private static final String FORM_SETUP_RELATIVE_DIR = "jenkins-custom/e2e-test-scripts/cypress/cy-form-spec";

	public String generateSpec(String formName)
	{
		return generateSpec(formName, null);
	}

	/**
	 * @param model optional neutral description of the form's handlers; when null the spec only contains the visibility checks built from the form's elements.
	 */
	public String generateSpec(String formName, FormSpecModel model)
	{
		if (model != null && !formName.equals(model.formName()))
		{
			return "Error: Model is for form '" + model.formName() + "' but spec was requested for form '" + formName + "'";
		}
		try
		{
			ServoyProject activeProject = ServoyModelFinder.getServoyModel().getActiveProject();
			if (activeProject == null)
			{
				return "Error: No active Servoy project.";
			}

			FlattenedSolution flattenedSolution = activeProject.getEditingFlattenedSolution();
			if (flattenedSolution == null)
			{
				return "Error: Could not resolve the active solution.";
			}

			Form form = flattenedSolution.getForm(formName);
			if (form == null)
			{
				return "Error: Form not found: " + formName;
			}

			Path testsDir = resolveFormSpecDir();
			Path setupDir = resolveFormSetupDir();
			Files.createDirectories(testsDir);
			Files.createDirectories(setupDir);

			String solutionName = form.getRootObject().getName();
			Path existingCySpec = firstExisting(testsDir, solutionName + "." + formName);
			Path cySpecPath = existingCySpec != null ? existingCySpec : testsDir.resolve(solutionName + "." + formName + resolveSpecExtension());
			Path existingSetupSpec = firstExistingSetup(setupDir, solutionName + "." + formName);
			Path setupSpecPath = existingSetupSpec != null ? existingSetupSpec : setupDir.resolve(solutionName + "." + formName + SPEC_JS_EXTENSION);

			if (Files.exists(cySpecPath) && Files.exists(setupSpecPath))
			{
				return "Spec files already exist: " + FORM_SPEC_RELATIVE_DIR + "/" + cySpecPath.getFileName() + " and " +
					FORM_SETUP_RELATIVE_DIR + "/" + setupSpecPath.getFileName();
			}

			FormMetadata metadata = buildMetadata(form);
			metadata.solutionName = solutionName;

			StringBuilder result = new StringBuilder();

			if (!Files.exists(cySpecPath))
			{
				String cyContent = model == null ? generateCypressSpecContent(metadata)
					: FormSpecModelEmitter.emit(getFormUrl(metadata.solutionName, metadata.formName), model);
				Files.writeString(cySpecPath, cyContent, StandardCharsets.UTF_8);
				result.append("Created: ").append(FORM_SPEC_RELATIVE_DIR).append("/").append(cySpecPath.getFileName()).append(" (")
					.append(model == null ? metadata.namedElements.size() + " element assertions"
						: FormSpecModelEmitter.countVisibilityAssertions(model) + " element visibility assertions, " +
							FormSpecModelEmitter.countHandlerCases(model) + " handler cases")
					.append(")\n");
			}

			if (!Files.exists(setupSpecPath))
			{
				String setupContent = generateSetupContent(metadata);
				Files.writeString(setupSpecPath, setupContent, StandardCharsets.UTF_8);
				result.append("Created: ").append(FORM_SETUP_RELATIVE_DIR).append("/").append(setupSpecPath.getFileName())
					.append(" (setUp/tearDown for data setup)");
			}

			return result.toString().trim();
		}
		catch (Exception e)
		{
			return "Error generating spec: " + e.getMessage();
		}
	}

	private Path resolveFormSpecDir()
	{
		Path workspaceRoot = org.eclipse.core.resources.ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile()
			.toPath();
		return workspaceRoot.resolve("jenkins-custom").resolve("e2e-test-scripts").resolve("cypress")
			.resolve("cy-form");
	}

	private Path resolveFormSetupDir()
	{
		Path workspaceRoot = org.eclipse.core.resources.ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile()
			.toPath();
		return workspaceRoot.resolve("jenkins-custom").resolve("e2e-test-scripts").resolve("cypress")
			.resolve("cy-form-spec");
	}

	/**
	 * @return true when {@code fileName} is a form spec file name (.spec.cy.js or .spec.cy.ts).
	 */
	public static boolean isFormSpecFileName(String fileName)
	{
		return SPEC_CY_EXTENSIONS.stream().anyMatch(fileName::endsWith);
	}

	/**
	 * @return {@code fileName} without its form spec extension, or unchanged when it has none.
	 */
	public static String stripFormSpecExtension(String fileName)
	{
		for (String extension : SPEC_CY_EXTENSIONS)
		{
			if (fileName.endsWith(extension))
			{
				return fileName.substring(0, fileName.length() - extension.length());
			}
		}
		return fileName;
	}

	/**
	 * @return the first existing file named {@code baseName} plus a supported form spec extension, or null.
	 */
	public static Path firstExisting(Path dir, String baseName)
	{
		for (String extension : SPEC_CY_EXTENSIONS)
		{
			Path candidate = dir.resolve(baseName + extension);
			if (Files.exists(candidate))
			{
				return candidate;
			}
		}
		return null;
	}

	/**
	 * @return the first existing setup file named {@code baseName} plus a supported setup extension (.spec.js or .spec.ts), or null.
	 */
	public static Path firstExistingSetup(Path dir, String baseName)
	{
		for (String extension : SPEC_SETUP_EXTENSIONS)
		{
			Path candidate = dir.resolve(baseName + extension);
			if (Files.exists(candidate))
			{
				return candidate;
			}
		}
		return null;
	}

	/**
	 * The extension for a NEW form spec: .spec.cy.ts when cy-form/ already holds more TypeScript form specs than JavaScript ones, else
	 * .spec.cy.js (also when the folder does not exist yet).
	 */
	private String resolveSpecExtension()
	{
		return detectSpecExtension(resolveFormSpecDir());
	}

	/**
	 * @return {@link #SPEC_CY_TS_EXTENSION} when {@code dir} holds more TypeScript form specs than JavaScript ones, else
	 *         {@link #SPEC_CY_JS_EXTENSION} (also when {@code dir} is null or does not exist).
	 */
	public static String detectSpecExtension(Path dir)
	{
		if (dir == null || !Files.isDirectory(dir))
		{
			return SPEC_CY_JS_EXTENSION;
		}
		try (java.util.stream.Stream<Path> files = Files.list(dir))
		{
			List<String> names = files.map(p -> p.getFileName().toString()).toList();
			long ts = names.stream().filter(n -> n.endsWith(SPEC_CY_TS_EXTENSION)).count();
			long js = names.stream().filter(n -> n.endsWith(SPEC_CY_JS_EXTENSION)).count();
			return ts > js ? SPEC_CY_TS_EXTENSION : SPEC_CY_JS_EXTENSION;
		}
		catch (Exception e)
		{
			return SPEC_CY_JS_EXTENSION;
		}
	}

	/**
	 * @deprecated use {@link #specExists(String, String)} instead - if you really don't mean to check the old file locations.
	 */
	@Deprecated
	public boolean oldSpecExists(String formName)
	{
		try
		{
			Path testsDir = resolveFormSpecDir();
			Path setupDir = resolveFormSetupDir();
			return firstExisting(testsDir, formName) != null && firstExistingSetup(setupDir, formName) != null;
		}
		catch (Exception e)
		{
			return false;
		}
	}

	public Path getFormSpecDir()
	{
		try
		{
			return resolveFormSpecDir();
		}
		catch (Exception e)
		{
			return null;
		}
	}

	public boolean specExists(String formName, String solutionName)
	{
		try
		{
			Path testsDir = resolveFormSpecDir();
			Path setupDir = resolveFormSetupDir();
			return (firstExisting(testsDir, solutionName + "." + formName) != null &&
				firstExistingSetup(setupDir, solutionName + "." + formName) != null) || oldSpecExists(formName);
		}
		catch (Exception e)
		{
			return false;
		}
	}

	/**
	 * @deprecated Use {@link #getSpecFilePath(String, String)} instead.
	 */
	@Deprecated
	public Path getOldSpecFilePath(String formName)
	{
		try
		{
			Path testsDir = resolveFormSpecDir();
			Path existing = firstExisting(testsDir, formName);
			return existing != null ? existing : testsDir.resolve(formName + resolveSpecExtension());
		}
		catch (Exception e)
		{
			return null;
		}
	}

	/**
	 * @deprecated Use {@link #getOldSetupFilePath(String, String)} instead. Unless you really want the old path.
	 */
	@Deprecated
	public Path getOldSetupFilePath(String formName)
	{
		try
		{
			Path setupDir = resolveFormSetupDir();
			Path existing = firstExistingSetup(setupDir, formName);
			return existing != null ? existing : setupDir.resolve(formName + SPEC_JS_EXTENSION);
		}
		catch (Exception e)
		{
			return null;
		}
	}


	public Path getSpecFilePath(String formName, String solutionName)
	{
		try
		{
			Path testsDir = resolveFormSpecDir();
			Path existing = firstExisting(testsDir, solutionName + "." + formName);
			return existing != null ? existing : testsDir.resolve(solutionName + "." + formName + resolveSpecExtension());
		}
		catch (Exception e)
		{
			return null;
		}
	}

	public Path getSetupFilePath(String formName, String solutionName)
	{
		try
		{
			Path setupDir = resolveFormSetupDir();
			Path existing = firstExistingSetup(setupDir, solutionName + "." + formName);
			return existing != null ? existing : setupDir.resolve(solutionName + "." + formName + SPEC_JS_EXTENSION);
		}
		catch (Exception e)
		{
			return null;
		}
	}

	public Path findExistingSpecFile(String formName, String solutionName)
	{
		try
		{
			Path testsDir = resolveFormSpecDir();
			Path prefixed = firstExisting(testsDir, solutionName + "." + formName);
			if (prefixed != null)
			{
				return prefixed;
			}
			return firstExisting(testsDir, formName);
		}
		catch (Exception e)
		{
			return null;
		}
	}

	public Path findExistingSetupFile(String formName, String solutionName)
	{
		try
		{
			Path setupDir = resolveFormSetupDir();
			Path prefixed = firstExistingSetup(setupDir, solutionName + "." + formName);
			if (prefixed != null)
			{
				return prefixed;
			}
			return firstExistingSetup(setupDir, formName);
		}
		catch (Exception e)
		{
			return null;
		}
	}

	private FormMetadata buildMetadata(Form form)
	{
		FormMetadata metadata = new FormMetadata();
		metadata.formName = form.getName();
		metadata.dataSource = form.getDataSource();

		Iterator<ISupportFormElement> elements = form.getFormElementsSortedByFormIndex();
		while (elements.hasNext())
		{
			ISupportFormElement element = elements.next();
			if (!(element instanceof IFormElement formElement))
			{
				continue;
			}
			String name = formElement.getName();
			if (name == null || name.equals(form.getName()))
			{
				continue;
			}
			if (element instanceof BaseComponent baseComponent && !baseComponent.getVisible())
			{
				continue;
			}

			ElementInfo elem = new ElementInfo();
			elem.name = name;
			elem.isWebComponent = element instanceof WebComponent;
			if (elem.isWebComponent)
			{
				elem.typeName = ((WebComponent)element).getTypeName();
			}
			if (element instanceof GraphicalComponent gc)
			{
//				elem.dataProviderID = gc.getDataProviderID();
				boolean hasAction = gc.getOnActionMethodID() != null && !gc.getOnActionMethodID().isEmpty();
				elem.isButton = hasAction;
				elem.isLabel = !hasAction;
			}

			metadata.namedElements.add(elem);
		}

		return metadata;
	}

	private String generateCypressSpecContent(FormMetadata metadata)
	{
		StringBuilder sb = new StringBuilder();

		String formUrl = getFormUrl(metadata.solutionName, metadata.formName);

		sb.append("describe('").append(metadata.formName).append("', () => {\n\n");

		sb.append("  beforeEach(() => {\n");
		sb.append("    cy.visit('").append(formUrl).append("');\n");
		if (metadata.namedElements.isEmpty())
		{
			sb.append("    cy.get('.svy-form', { timeout: 30000 }).should('exist');\n");
		}
		else
		{
			sb.append("    cy.get('[data-cy^=\"").append(metadata.formName)
				.append(".\"]', { timeout: 30000 }).should('exist');\n");
		}
		sb.append("  });\n\n");

		sb.append("  it('loads without errors and all elements are visible', () => {\n");
		sb.append("    cy.get('.svy-error, .error-overlay').should('not.exist');\n");

		List<ElementInfo> visibleElements = metadata.namedElements.stream()
			.filter(e -> e.isWebComponent || e.isButton || e.isLabel).limit(8).toList();

		for (ElementInfo elem : visibleElements)
		{
			sb.append("    cy.get('[data-cy=\"").append(metadata.formName).append(".").append(elem.name)
				.append("\"]').should('be.visible');\n");
		}
		sb.append("  });\n\n");

		List<ElementInfo> buttons = metadata.namedElements.stream()
			.filter(e -> e.isButton || (e.typeName != null && e.typeName.contains("button"))).limit(3).toList();

		if (!buttons.isEmpty())
		{
			sb.append("  it('buttons are clickable', () => {\n");
			for (ElementInfo button : buttons)
			{
				sb.append("    cy.get('[data-cy=\"").append(metadata.formName).append(".").append(button.name)
					.append("\"]').should('be.visible').and('be.enabled');\n");
			}
			sb.append("  });\n\n");
		}

		sb.append("});\n");

		return sb.toString();
	}

	private String getFormUrl(String solutionName, String formName)
	{
		return "solution/" + solutionName + "/index.html?formpreview=" + formName + "&svy_testmode=true";
	}

	private String generateSetupContent(FormMetadata metadata)
	{
		StringBuilder sb = new StringBuilder();

		sb.append("/**\n");
		sb.append(" * Form test setup/teardown for: ").append(metadata.formName).append("\n");
		if (metadata.dataSource != null)
		{
			sb.append(" * DataSource: ").append(metadata.dataSource).append("\n");
		}
		sb.append(" *\n");
		sb.append(" * This file runs inside the Servoy runtime BEFORE the Cypress assertions.\n");
		sb.append(" * Use spec_setUp() to prepare test data (load records, set variables, etc.)\n");
		sb.append(" * Use spec_tearDown() to clean up after tests.\n");
		sb.append(" */\n\n");

		sb.append("/**\n");
		sb.append(" * @properties={typeid:24,uuid:\"").append(UUID.randomUUID()).append("\"}\n");
		sb.append(" */\n");
		sb.append("function spec_setUp() {\n");
		if (metadata.dataSource != null)
		{
			sb.append("\t// DataSource: ").append(metadata.dataSource).append("\n");
			sb.append("\t// Load specific records for testing:\n");
			sb.append("\t// foundset.loadAllRecords();\n");
			sb.append("\t// Or filter to specific test data:\n");
			sb.append("\t// foundset.find();\n");
			sb.append("\t// foundset.search();\n");
		}
		else
		{
			sb.append("\t// No dataSource on this form - set up form variables or other state\n");
		}
		sb.append("}\n\n");

		sb.append("/**\n");
		sb.append(" * @properties={typeid:24,uuid:\"").append(UUID.randomUUID()).append("\"}\n");
		sb.append(" */\n");
		sb.append("function spec_tearDown() {\n");
		sb.append("\t// Clean up test data if needed\n");
		sb.append("\t// databaseManager.rollbackEditedRecords();\n");
		sb.append("}\n");

		return sb.toString();
	}

	private static class FormMetadata
	{
		String formName;
		String solutionName;
		String dataSource;
		List<ElementInfo> namedElements = new ArrayList<>();
	}

	private static class ElementInfo
	{
		String name;
		String typeName;
//		String dataProviderID;
		boolean isWebComponent;
		boolean isButton;
		boolean isLabel;

		@Override
		public String toString()
		{
			return name;
		}
	}
}
