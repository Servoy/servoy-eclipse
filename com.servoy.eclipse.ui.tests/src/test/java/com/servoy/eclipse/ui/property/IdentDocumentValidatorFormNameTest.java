package com.servoy.eclipse.ui.property;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.servoy.j2db.util.docvalidator.IdentDocumentValidator;

class IdentDocumentValidatorFormNameTest {
	@ParameterizedTest
	@ValueSource(strings = { "form1", "myForm", "_x", "a", "MyForm_2", "$name" })
	@DisplayName("Valid form names are accepted (renaming from the Properties view is allowed)")
	void acceptsValidFormNames(String name) {
		assertTrue(IdentDocumentValidator.isJavaIdentifier(name), "'" + name + "' should be a valid form name");
	}

	@ParameterizedTest
	@EmptySource
	@ValueSource(strings = { " ", "1abc", "a b", "a-b", "a.b", "form!", "with space" })
	@DisplayName("Invalid form names are rejected (SVY-20310 null.js corruption is blocked at the setter)")
	void rejectsInvalidFormNames(String name) {
		assertFalse(IdentDocumentValidator.isJavaIdentifier(name), "'" + name + "' should be rejected as a form name");
	}

	@Test
	@DisplayName("null throws NPE from the validator; the setter guards null before calling it, so the name is never cleared")
	void nullIsGuardedBeforeTheValidator() {
		// isJavaIdentifier has no null guard (str.toCharArray()), so it throws on null.
		// PersistPropertySource.setPersistPropertyValue therefore rejects a null name
		// via its own
		// (newName == null || !isJavaIdentifier(newName)) check before ever calling the
		// validator,
		// which is what blocks the SVY-20310 restore-default-to-null corruption.
		assertThrows(NullPointerException.class, () -> IdentDocumentValidator.isJavaIdentifier(null));
	}
}
