package com.servoy.eclipse.ngclient.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.sablo.specification.WebObjectSpecification;

/**
 * SVY-21571 - servoycore-errorbean carries "deprecated": "true" (to stay hidden from the
 * component palette/placement, per SVY-19023) but is also the designated
 * FormElement.ERROR_BEAN runtime fallback substituted for any missing component spec, so it
 * must not be treated as "just another deprecated spec" by consumers that key off
 * isDeprecated() to skip/flag something. The "alwaysGenerateTemplate" tag lets a spec opt out
 * of that skip/flag behaviour while keeping isDeprecated() == true everywhere else (palette,
 * drag-create, outline, etc. are all unaffected).
 *
 * This pins the spec-level contract that both ComponentTemplateGenerator (skip deprecated
 * specs from code generation) and ServoyBuilder.checkSpecs (raise a DeprecatedSpecification
 * marker for a deprecated spec placed on a form) rely on: {@code isDeprecated() &&
 * !hasTag("alwaysGenerateTemplate")}.
 */
class WebObjectSpecificationAlwaysGenerateTemplateTagTest
{
	private static WebObjectSpecification parse(String json) throws Exception
	{
		return WebObjectSpecification.parseSpec(json, "servoycore", null, null);
	}

	@Test
	void errorbeanIsDeprecatedButTaggedAlwaysGenerateTemplate() throws Exception
	{
		WebObjectSpecification errorbean = parse(
			"{\"name\":\"servoycore-errorbean\",\"deprecated\":\"true\",\"tags\":{\"alwaysGenerateTemplate\":true},\"model\":{}}");

		assertTrue(errorbean.isDeprecated(), "errorbean must stay deprecated (hidden from the palette, SVY-19023)");
		assertTrue(errorbean.hasTag("alwaysGenerateTemplate"), "errorbean must carry the alwaysGenerateTemplate tag");
		// this is the exact condition ComponentTemplateGenerator/ServoyBuilder.checkSpecs use to skip/flag:
		assertFalse(errorbean.isDeprecated() && !errorbean.hasTag("alwaysGenerateTemplate"),
			"errorbean must NOT be treated as a plain deprecated spec (no template-skip, no DeprecatedSpecification marker)");
	}

	@Test
	void ordinaryDeprecatedSpecWithoutTheTagIsStillTreatedAsDeprecated() throws Exception
	{
		WebObjectSpecification defaultLoadingIndicator = parse(
			"{\"name\":\"servoycore-defaultLoadingIndicator\",\"deprecated\":\"true\",\"model\":{}}");

		assertTrue(defaultLoadingIndicator.isDeprecated());
		assertFalse(defaultLoadingIndicator.hasTag("alwaysGenerateTemplate"));
		assertTrue(defaultLoadingIndicator.isDeprecated() && !defaultLoadingIndicator.hasTag("alwaysGenerateTemplate"),
			"a deprecated spec without the tag must still be skipped/flagged as deprecated");
	}

	@Test
	void nonDeprecatedSpecIsUnaffectedByTheTagCheck() throws Exception
	{
		WebObjectSpecification button = parse("{\"name\":\"testpkg-button\",\"model\":{}}");

		assertFalse(button.isDeprecated());
		assertFalse(button.isDeprecated() && !button.hasTag("alwaysGenerateTemplate"));
	}

	@Test
	void tagAloneWithoutDeprecatedHasNoEffectOnTheSkipCondition() throws Exception
	{
		// a spec could carry the tag without being deprecated at all (e.g. future-proofing) -
		// the skip/flag condition must stay false either way, since it is gated on isDeprecated() first.
		WebObjectSpecification spec = parse("{\"name\":\"testpkg-button\",\"tags\":{\"alwaysGenerateTemplate\":true},\"model\":{}}");

		assertFalse(spec.isDeprecated());
		assertTrue(spec.hasTag("alwaysGenerateTemplate"));
		assertFalse(spec.isDeprecated() && !spec.hasTag("alwaysGenerateTemplate"));
	}
}
