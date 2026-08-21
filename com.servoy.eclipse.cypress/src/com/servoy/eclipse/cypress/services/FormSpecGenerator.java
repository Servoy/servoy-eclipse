package com.servoy.eclipse.cypress.services;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.e4.core.di.annotations.Creatable;

import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyProject;

@Creatable
@SuppressWarnings("restriction")
public class FormSpecGenerator {
	private static final String SPEC_CY_EXTENSION = ".spec.cy.js";
	private static final String SPEC_JS_EXTENSION = ".spec.js";
	private static final String FORM_SPEC_RELATIVE_DIR = "jenkins-custom/e2e-test-scripts/cypress/cy-form";
	private static final String FORM_SETUP_RELATIVE_DIR = "jenkins-custom/e2e-test-scripts/cypress/cy-form-spec";

	private static final Pattern DATA_SOURCE_PATTERN = Pattern.compile("\"dataSource\"\\s*:\\s*\"([^\"]+)\"");
	private static final Pattern ELEMENT_NAME_PATTERN = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");
	private static final Pattern TYPE_NAME_PATTERN = Pattern.compile("\"typeName\"\\s*:\\s*\"([^\"]+)\"");
	private static final Pattern DATA_PROVIDER_PATTERN = Pattern.compile("\"dataProviderID\"\\s*:\\s*\"([^\"]+)\"");

	public String generateSpec(String formName) {
		try {
			ServoyProject activeProject = ServoyModelFinder.getServoyModel().getActiveProject();
			if (activeProject == null) {
				return "Error: No active Servoy project.";
			}

			IProject project = activeProject.getProject();
			IFile frmFile = project.getFile("forms/" + formName + ".frm");
			if (!frmFile.exists()) {
				return "Error: Form file not found: forms/" + formName + ".frm";
			}

			Path testsDir = resolveFormSpecDir();
			Path setupDir = resolveFormSetupDir();
			Files.createDirectories(testsDir);
			Files.createDirectories(setupDir);

			String solutionName = activeProject.getSolution().getName();
			Path cySpecPath = testsDir.resolve(solutionName + "." + formName + SPEC_CY_EXTENSION);
			Path setupSpecPath = setupDir.resolve(solutionName + "." + formName + SPEC_JS_EXTENSION);

			if (Files.exists(cySpecPath) && Files.exists(setupSpecPath)) {
				return "Spec files already exist: " + FORM_SPEC_RELATIVE_DIR + "/" + solutionName + "." + formName + SPEC_CY_EXTENSION
						+ " and " + FORM_SETUP_RELATIVE_DIR + "/" + solutionName + "." + formName + SPEC_JS_EXTENSION;
			}

			String frmContent = new String(Files.readAllBytes(frmFile.getLocation().toFile().toPath()),
					StandardCharsets.UTF_8);
			FormMetadata metadata = parseFrmFile(frmContent, formName);
			metadata.solutionName = activeProject.getSolution().getName();

			StringBuilder result = new StringBuilder();

			if (!Files.exists(cySpecPath)) {
				String cyContent = generateCypressSpecContent(metadata);
				Files.writeString(cySpecPath, cyContent, StandardCharsets.UTF_8);
				result.append("Created: ").append(FORM_SPEC_RELATIVE_DIR).append("/").append(solutionName).append(".")
						.append(formName).append(SPEC_CY_EXTENSION).append(" (").append(metadata.namedElements.size())
						.append(" element assertions)\n");
			}

			if (!Files.exists(setupSpecPath)) {
				String setupContent = generateSetupContent(metadata);
				Files.writeString(setupSpecPath, setupContent, StandardCharsets.UTF_8);
				result.append("Created: ").append(FORM_SETUP_RELATIVE_DIR).append("/").append(solutionName).append(".")
						.append(formName).append(SPEC_JS_EXTENSION).append(" (setUp/tearDown for data setup)");
			}

			return result.toString().trim();
		} catch (Exception e) {
			return "Error generating spec: " + e.getMessage();
		}
	}

	private Path resolveFormSpecDir() {
		Path workspaceRoot = org.eclipse.core.resources.ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile()
				.toPath();
		return workspaceRoot.resolve("jenkins-custom").resolve("e2e-test-scripts").resolve("cypress")
				.resolve("cy-form");
	}

	private Path resolveFormSetupDir() {
		Path workspaceRoot = org.eclipse.core.resources.ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile()
				.toPath();
		return workspaceRoot.resolve("jenkins-custom").resolve("e2e-test-scripts").resolve("cypress")
				.resolve("cy-form-spec");
	}

	public boolean specExists(String formName) {
		try {
			Path testsDir = resolveFormSpecDir();
			Path setupDir = resolveFormSetupDir();
			return Files.exists(testsDir.resolve(formName + SPEC_CY_EXTENSION))
					&& Files.exists(setupDir.resolve(formName + SPEC_JS_EXTENSION));
		} catch (Exception e) {
			return false;
		}
	}

	public Path getSpecFilePath(String formName) {
		try {
			Path testsDir = resolveFormSpecDir();
			return testsDir.resolve(formName + SPEC_CY_EXTENSION);
		} catch (Exception e) {
			return null;
		}
	}

	public Path getSetupFilePath(String formName) {
		try {
			Path setupDir = resolveFormSetupDir();
			return setupDir.resolve(formName + SPEC_JS_EXTENSION);
		} catch (Exception e) {
			return null;
		}
	}

	public Path getFormSpecDir() {
		try {
			return resolveFormSpecDir();
		} catch (Exception e) {
			return null;
		}
	}

	public boolean specExists(String formName, String solutionName) {
		try {
			Path testsDir = resolveFormSpecDir();
			Path setupDir = resolveFormSetupDir();
			return Files.exists(testsDir.resolve(solutionName + "." + formName + SPEC_CY_EXTENSION))
					&& Files.exists(setupDir.resolve(solutionName + "." + formName + SPEC_JS_EXTENSION));
		} catch (Exception e) {
			return false;
		}
	}

	public Path getSpecFilePath(String formName, String solutionName) {
		try {
			Path testsDir = resolveFormSpecDir();
			return testsDir.resolve(solutionName + "." + formName + SPEC_CY_EXTENSION);
		} catch (Exception e) {
			return null;
		}
	}

	public Path getSetupFilePath(String formName, String solutionName) {
		try {
			Path setupDir = resolveFormSetupDir();
			return setupDir.resolve(solutionName + "." + formName + SPEC_JS_EXTENSION);
		} catch (Exception e) {
			return null;
		}
	}

	public Path findExistingSpecFile(String formName, String solutionName) {
		try {
			Path testsDir = resolveFormSpecDir();
			Path prefixed = testsDir.resolve(solutionName + "." + formName + SPEC_CY_EXTENSION);
			if (Files.exists(prefixed)) {
				return prefixed;
			}
			Path legacy = testsDir.resolve(formName + SPEC_CY_EXTENSION);
			if (Files.exists(legacy)) {
				return legacy;
			}
			return null;
		} catch (Exception e) {
			return null;
		}
	}

	public Path findExistingSetupFile(String formName, String solutionName) {
		try {
			Path setupDir = resolveFormSetupDir();
			Path prefixed = setupDir.resolve(solutionName + "." + formName + SPEC_JS_EXTENSION);
			if (Files.exists(prefixed)) {
				return prefixed;
			}
			Path legacy = setupDir.resolve(formName + SPEC_JS_EXTENSION);
			if (Files.exists(legacy)) {
				return legacy;
			}
			return null;
		} catch (Exception e) {
			return null;
		}
	}

	private FormMetadata parseFrmFile(String content, String formName) {
		FormMetadata metadata = new FormMetadata();
		metadata.formName = formName;

		Matcher dsMatcher = DATA_SOURCE_PATTERN.matcher(content);
		if (dsMatcher.find()) {
			metadata.dataSource = dsMatcher.group(1);
		}

		String[] items = content.split("\\{");
		for (String item : items) {
			Matcher nameMatcher = ELEMENT_NAME_PATTERN.matcher(item);
			Matcher typeMatcher = TYPE_NAME_PATTERN.matcher(item);
			Matcher dpMatcher = DATA_PROVIDER_PATTERN.matcher(item);

			if (nameMatcher.find()) {
				String name = nameMatcher.group(1);
				if (name.equals(formName))
					continue;

				ElementInfo elem = new ElementInfo();
				elem.name = name;
				elem.typeName = typeMatcher.find() ? typeMatcher.group(1) : null;
				elem.dataProviderID = dpMatcher.find() ? dpMatcher.group(1) : null;
				elem.isWebComponent = item.contains("\"typeid\":47");
				elem.isButton = item.contains("\"typeid\":7") && item.contains("onActionMethodID");
				elem.isLabel = item.contains("\"typeid\":7") && !item.contains("onActionMethodID");

				metadata.namedElements.add(elem);
			}
		}

		return metadata;
	}

	private String generateCypressSpecContent(FormMetadata metadata) {
		StringBuilder sb = new StringBuilder();

		String formUrl = getFormUrl(metadata.solutionName, metadata.formName);

		sb.append("describe('").append(metadata.formName).append("', () => {\n\n");

		sb.append("  beforeEach(() => {\n");
		sb.append("    cy.visit('").append(formUrl).append("');\n");
		if (metadata.namedElements.isEmpty()) {
			sb.append("    cy.get('.svy-form', { timeout: 30000 }).should('exist');\n");
		} else {
			sb.append("    cy.get('[data-cy^=\"").append(metadata.formName)
					.append(".\"', { timeout: 30000 }).should('exist');\n");
		}
		sb.append("  });\n\n");

		sb.append("  it('loads without errors and all elements are visible', () => {\n");
		sb.append("    cy.get('.svy-error, .error-overlay').should('not.exist');\n");

		List<ElementInfo> visibleElements = metadata.namedElements.stream()
				.filter(e -> e.isWebComponent || e.isButton || e.isLabel).limit(8).toList();

		for (ElementInfo elem : visibleElements) {
			sb.append("    cy.get('[data-cy=\"").append(metadata.formName).append(".").append(elem.name)
					.append("\"]').should('be.visible');\n");
		}
		sb.append("  });\n\n");

		List<ElementInfo> buttons = metadata.namedElements.stream()
				.filter(e -> e.isButton || (e.typeName != null && e.typeName.contains("button"))).limit(3).toList();

		if (!buttons.isEmpty()) {
			sb.append("  it('buttons are clickable', () => {\n");
			for (ElementInfo button : buttons) {
				sb.append("    cy.get('[data-cy=\"").append(metadata.formName).append(".").append(button.name)
						.append("\"]').should('be.visible').and('be.enabled');\n");
			}
			sb.append("  });\n\n");
		}

		sb.append("});\n");

		return sb.toString();
	}

	private String getFormUrl(String solutionName, String formName) {
		return "solution/" + solutionName + "/index.html?formpreview=" + formName + "&svy_testmode=true";
	}

	private String generateSetupContent(FormMetadata metadata) {
		StringBuilder sb = new StringBuilder();

		sb.append("/**\n");
		sb.append(" * Form test setup/teardown for: ").append(metadata.formName).append("\n");
		if (metadata.dataSource != null) {
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
		if (metadata.dataSource != null) {
			sb.append("\t// DataSource: ").append(metadata.dataSource).append("\n");
			sb.append("\t// Load specific records for testing:\n");
			sb.append("\t// foundset.loadAllRecords();\n");
			sb.append("\t// Or filter to specific test data:\n");
			sb.append("\t// foundset.find();\n");
			sb.append("\t// foundset.search();\n");
		} else {
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

	private static class FormMetadata {
		String formName;
		String solutionName;
		String dataSource;
		List<ElementInfo> namedElements = new ArrayList<>();
	}

	private static class ElementInfo {
		String name;
		String typeName;
		String dataProviderID;
		boolean isWebComponent;
		boolean isButton;
		boolean isLabel;
	}
}
