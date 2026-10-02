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
public class FormSpecGenerator {
	private static final String SPEC_CY_EXTENSION = ".spec.cy.js";
	private static final String SPEC_JS_EXTENSION = ".spec.js";
	private static final String FORM_SPEC_RELATIVE_DIR = "jenkins-custom/e2e-test-scripts/cypress/cy-form";
	private static final String FORM_SETUP_RELATIVE_DIR = "jenkins-custom/e2e-test-scripts/cypress/cy-form-spec";

	public String generateSpec(String formName) {
		try {
			ServoyProject activeProject = ServoyModelFinder.getServoyModel().getActiveProject();
			if (activeProject == null) {
				return "Error: No active Servoy project.";
			}

			FlattenedSolution flattenedSolution = activeProject.getEditingFlattenedSolution();
			if (flattenedSolution == null) {
				return "Error: Could not resolve the active solution.";
			}

			Form form = flattenedSolution.getForm(formName);
			if (form == null) {
				return "Error: Form not found: " + formName;
			}

			Path testsDir = resolveFormSpecDir();
			Path setupDir = resolveFormSetupDir();
			Files.createDirectories(testsDir);
			Files.createDirectories(setupDir);

			String solutionName = form.getRootObject().getName();
			Path cySpecPath = testsDir.resolve(solutionName + "." + formName + SPEC_CY_EXTENSION);
			Path setupSpecPath = setupDir.resolve(solutionName + "." + formName + SPEC_JS_EXTENSION);

			if (Files.exists(cySpecPath) && Files.exists(setupSpecPath)) {
				return "Spec files already exist: " + FORM_SPEC_RELATIVE_DIR + "/" + solutionName + "." + formName
						+ SPEC_CY_EXTENSION + " and " + FORM_SETUP_RELATIVE_DIR + "/" + solutionName + "." + formName
						+ SPEC_JS_EXTENSION;
			}

			FormMetadata metadata = buildMetadata(form);
			metadata.solutionName = solutionName;

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

	private FormMetadata buildMetadata(Form form) {
		FormMetadata metadata = new FormMetadata();
		metadata.formName = form.getName();
		metadata.dataSource = form.getDataSource();

		Iterator<ISupportFormElement> elements = form.getFormElementsSortedByFormIndex();
		while (elements.hasNext()) {
			ISupportFormElement element = elements.next();
			if (!(element instanceof IFormElement formElement)) {
				continue;
			}
			String name = formElement.getName();
			if (name == null || name.equals(form.getName())) {
				continue;
			}
			if (element instanceof BaseComponent baseComponent && !baseComponent.getVisible()) {
				continue;
			}

			ElementInfo elem = new ElementInfo();
			elem.name = name;
			elem.isWebComponent = element instanceof WebComponent;
			if (elem.isWebComponent) {
				elem.typeName = ((WebComponent) element).getTypeName();
			}
			if (element instanceof GraphicalComponent gc) {
				elem.dataProviderID = gc.getDataProviderID();
				boolean hasAction = gc.getOnActionMethodID() != null && !gc.getOnActionMethodID().isEmpty();
				elem.isButton = hasAction;
				elem.isLabel = !hasAction;
			}

			metadata.namedElements.add(elem);
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

		@Override
		public String toString() {
			return name;
		}
	}
}
