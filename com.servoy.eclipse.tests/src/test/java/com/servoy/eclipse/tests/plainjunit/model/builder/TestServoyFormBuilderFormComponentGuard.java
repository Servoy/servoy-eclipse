package com.servoy.eclipse.tests.plainjunit.model.builder;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.servoy.j2db.server.ngclient.property.types.FormComponentPropertyType;

/**
 * SVY-21667: {@code ServoyFormBuilder.addFormMarkers}
 * (com.servoy.eclipse.model, a separate git repository from where the fix
 * originated, servoy-client/servoy_ngclient) had its own, previously unguarded
 * <code>(JSONObject)propertyValue</code> cast on a form-component property
 * value, reached whenever the Servoy Project Builder walks a form containing a
 * (List) Form Component. A bare form-name/UUID string resolves via
 * {@code FormComponentPropertyType.INSTANCE.getForm(...)} (which is
 * deliberately tolerant of a String/UUID) but is not a {@link JSONObject}, so
 * the raw cast threw a context-free {@code ClassCastException} on every
 * save/build of a workspace project containing such a form - confirmed live via
 * the Eclipse `.log` when building a form with a malformed `containedForm`.
 * <p>
 * The fix reuses the same shared guard already pinned by servoy_ngclient's own
 * test suite (<code>FormComponentPropertyTypeAsFormComponentValueTest</code>):
 * {@code ServoyFormBuilder.addFormMarkers} now calls
 * {@link FormComponentPropertyType#asFormComponentValue(Object, String, String, String)}
 * before the cast and {@code continue}s instead of casting when it returns
 * {@code null}.
 * <p>
 * This test cannot exercise {@code ServoyFormBuilder.addFormMarkers} itself
 * from a plain (non-OSGi) JUnit test - building real marker output needs a live
 * {@code ServoyProject}/{@code FlattenedSolution}, which requires the Eclipse
 * workspace/OSGi container that this plain fragment does not have access to
 * (and {@code com.servoy.eclipse.tests} does not depend on the shared PDE
 * integration-test infrastructure that would provide it). What it pins instead
 * is the exact contract {@code ServoyFormBuilder}'s new guard depends on: that
 * {@code asFormComponentValue} passes a canonical
 * {@code { "svy_form": "<uuid>" }} value through unchanged (so
 * {@code ServoyFormBuilder}'s existing marker logic sees the same value as
 * before), and rejects every other shape with {@code null} (so
 * {@code ServoyFormBuilder} takes its new {@code continue} branch instead of
 * reaching the old unguarded cast). A regression in either of these two
 * behaviours would re-expose the exact {@code ClassCastException} this ticket
 * fixed, in the IDE builder specifically.
 */
class TestServoyFormBuilderFormComponentGuard {
	private static final String FORM_NAME = "myOwningForm";
	private static final String ELEMENT_NAME = "myFormComponentElement";
	private static final String PROPERTY_NAME = "containedForm";
	private static final String SOME_FORM_UUID = "B2C3D4E5-0000-1111-2222-334455667788";

	@Test
	@DisplayName("a canonical { svy_form: <uuid> } value passes through unchanged, so ServoyFormBuilder's existing marker logic is unaffected")
	void canonicalValuePassesThroughUnchanged() {
		JSONObject canonicalValue = new JSONObject().put(FormComponentPropertyType.SVY_FORM, SOME_FORM_UUID);

		JSONObject result = FormComponentPropertyType.asFormComponentValue(canonicalValue, FORM_NAME, ELEMENT_NAME,
				PROPERTY_NAME);

		assertSame(canonicalValue, result,
				"ServoyFormBuilder.addFormMarkers must receive the exact same JSONObject instance it already had for a well-formed value");
		assertNotNull(result);
	}

	@Test
	@DisplayName("a bare form-name string - the exact shape that previously threw ClassCastException in ServoyFormBuilder.addFormMarkers - is rejected with null, not cast")
	void bareFormNameStringIsRejectedWithNull() {
		// this is the value that, before the fix,
		// FormComponentPropertyType.INSTANCE.getForm(...) would
		// resolve to a real form (so ServoyFormBuilder's "if (frm == null) continue;"
		// guard would NOT
		// trigger), immediately followed by the unguarded "(JSONObject)propertyValue"
		// cast that threw.
		String bareFormNameValue = "fc_some_row_form";

		JSONObject result = FormComponentPropertyType.asFormComponentValue(bareFormNameValue, FORM_NAME, ELEMENT_NAME,
				PROPERTY_NAME);

		assertNull(result,
				"a bare form-name string must be rejected with null so ServoyFormBuilder takes its new continue branch instead of casting it");
	}

	@Test
	@DisplayName("a bare form-UUID string is rejected with null, not cast")
	void bareFormUuidStringIsRejectedWithNull() {
		JSONObject result = FormComponentPropertyType.asFormComponentValue(SOME_FORM_UUID, FORM_NAME, ELEMENT_NAME,
				PROPERTY_NAME);

		assertNull(result,
				"a bare form-UUID string must be rejected with null so ServoyFormBuilder takes its new continue branch instead of casting it");
	}
}
