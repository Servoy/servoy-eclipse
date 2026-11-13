package com.servoy.eclipse.ui.tests.plainjunit.views.solutionexplorer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.servoy.eclipse.ui.views.solutionexplorer.actions.NewWebObjectAction;

/**
 * SVY-21702: Tests for the shared {@code NG2-Components} manifest helper added
 * to {@code NewWebObjectAction}
 * ({@code addComponentToNG2ComponentsAttribute(Manifest, String)}).
 *
 * <p>
 * The helper parses the current {@code NG2-Components} attribute into an
 * ordered, de-duplicated list of component class names, appends a new class
 * name (idempotently), and writes the attribute back comma-separated with no
 * spaces. It is the pure in-memory part of the file-based
 * {@code addComponentToNG2ComponentsManifest(IResource, String)}, so the real
 * production method is exercised directly against a {@link java.util.jar.Manifest}
 * with no Eclipse workspace / OSGi runtime, no mocking and no reflection.
 *
 * <p>
 * The round-trip tests additionally serialize the mutated manifest through
 * {@link Manifest#write} and re-parse it to prove the 72-byte line wrap and the
 * per-component {@code Name:} sub-sections (written by {@code addToManifest})
 * survive intact.
 */
class NewWebObjectActionNG2ComponentsManifestTest
{
	private static final String NG2_COMPONENTS = "NG2-Components";

	/** A manifest with only a main section built from the given attribute lines. */
	private static Manifest mainAttributes(String mainSectionLines) throws IOException
	{
		return parse("Manifest-Version: 1.0\r\n" + mainSectionLines);
	}

	private static Manifest parse(String raw) throws IOException
	{
		try (InputStream in = new ByteArrayInputStream(raw.getBytes(StandardCharsets.UTF_8)))
		{
			return new Manifest(in);
		}
	}

	private static String ng2Components(Manifest manifest)
	{
		return manifest.getMainAttributes().getValue(NG2_COMPONENTS);
	}

	/** Serializes the manifest and re-parses it, as the production file round-trip does. */
	private static Manifest roundTrip(Manifest manifest) throws IOException
	{
		try (ByteArrayOutputStream out = new ByteArrayOutputStream())
		{
			manifest.write(out);
			return new Manifest(new ByteArrayInputStream(out.toByteArray()));
		}
	}

	@Nested
	class EmptyAndAbsent
	{
		@Test
		@DisplayName("AC: absent NG2-Components attribute -> first add produces exactly the one class name")
		void absentAttributeFirstAddProducesSingleName() throws IOException
		{
			Manifest manifest = mainAttributes("NPM-PackageName: mypackage\r\nEntry-Point: dist\r\n");

			boolean changed = NewWebObjectAction.addComponentToNG2ComponentsAttribute(manifest, "MyComponent");

			assertTrue(changed, "adding to a package with no NG2-Components yet must report a change");
			assertEquals("MyComponent", ng2Components(manifest),
				"a package with no NG2-Components yet should list exactly the added class");
		}

		@Test
		@DisplayName("empty NG2-Components value -> first add produces exactly the one class name")
		void emptyValueFirstAddProducesSingleName() throws IOException
		{
			Manifest manifest = mainAttributes("NPM-PackageName: mypackage\r\nNG2-Components: \r\nEntry-Point: dist\r\n");

			boolean changed = NewWebObjectAction.addComponentToNG2ComponentsAttribute(manifest, "MyComponent");

			assertTrue(changed, "replacing an empty value with the first class must report a change");
			assertEquals("MyComponent", ng2Components(manifest),
				"an empty NG2-Components value should be replaced by the single added class");
		}

		@Test
		@DisplayName("absent NG2-Components attribute is null before any add")
		void absentAttributeStartsNull() throws IOException
		{
			Manifest manifest = mainAttributes("NPM-PackageName: mypackage\r\nEntry-Point: dist\r\n");
			assertNull(ng2Components(manifest),
				"precondition: freshly-created empty package has no NG2-Components attribute");
		}
	}

	@Nested
	class FormattingAndOrdering
	{
		@Test
		@DisplayName("AC: two adds produce 'A,B' (comma, NO spaces), order preserved")
		void twoAddsProduceCommaSeparatedNoSpaces() throws IOException
		{
			Manifest manifest = mainAttributes("NPM-PackageName: mypackage\r\nNG2-Components: \r\nEntry-Point: dist\r\n");

			NewWebObjectAction.addComponentToNG2ComponentsAttribute(manifest, "ServoyDefaultTextField");
			NewWebObjectAction.addComponentToNG2ComponentsAttribute(manifest, "ServoyDefaultButton");

			String value = ng2Components(manifest);
			assertEquals("ServoyDefaultTextField,ServoyDefaultButton", value,
				"format must match existing manifests: comma-separated, no spaces, insertion order preserved");
			assertFalse(value.contains(", "), "there must be no space after the comma");
		}

		@Test
		@DisplayName("three adds keep insertion order")
		void threeAddsKeepInsertionOrder() throws IOException
		{
			Manifest manifest = mainAttributes("NPM-PackageName: mypackage\r\nEntry-Point: dist\r\n");

			NewWebObjectAction.addComponentToNG2ComponentsAttribute(manifest, "ServoyDefaultTextField");
			NewWebObjectAction.addComponentToNG2ComponentsAttribute(manifest, "ServoyDefaultButton");
			NewWebObjectAction.addComponentToNG2ComponentsAttribute(manifest, "ServoyDefaultLabel");

			assertEquals("ServoyDefaultTextField,ServoyDefaultButton,ServoyDefaultLabel", ng2Components(manifest),
				"insertion order must be preserved across multiple adds");
		}

		@Test
		@DisplayName("a pre-existing multi-value attribute is extended, not overwritten")
		void preExistingValueExtended() throws IOException
		{
			Manifest manifest = mainAttributes(
				"NPM-PackageName: mypackage\r\nNG2-Components: DataGrid,PowerGrid\r\nEntry-Point: dist\r\n");

			boolean changed = NewWebObjectAction.addComponentToNG2ComponentsAttribute(manifest, "TreeGrid");

			assertTrue(changed, "appending a new class to an existing list must report a change");
			assertEquals("DataGrid,PowerGrid,TreeGrid", ng2Components(manifest),
				"an existing list must be preserved and the new class appended at the end");
		}
	}

	@Nested
	class Idempotency
	{
		@Test
		@DisplayName("AC: re-adding an existing class is a no-op (no duplicate, reports no change)")
		void reAddingExistingClassIsNoOp() throws IOException
		{
			Manifest manifest = mainAttributes("NPM-PackageName: mypackage\r\nEntry-Point: dist\r\n");

			NewWebObjectAction.addComponentToNG2ComponentsAttribute(manifest, "MyComponent");
			boolean changedSecondTime = NewWebObjectAction.addComponentToNG2ComponentsAttribute(manifest, "MyComponent");

			assertFalse(changedSecondTime, "re-adding the same class must report no change");
			assertEquals("MyComponent", ng2Components(manifest), "re-adding the same class must not create a duplicate");
		}

		@Test
		@DisplayName("re-adding an existing class in the middle of a list does not reorder or duplicate")
		void reAddingExistingMiddleClassIsNoOp() throws IOException
		{
			Manifest manifest = mainAttributes("NPM-PackageName: mypackage\r\nNG2-Components: A,B,C\r\nEntry-Point: dist\r\n");

			boolean changed = NewWebObjectAction.addComponentToNG2ComponentsAttribute(manifest, "B");

			assertFalse(changed, "re-adding an existing middle entry must report no change");
			assertEquals("A,B,C", ng2Components(manifest),
				"re-adding an existing middle entry must leave the list unchanged");
		}

		@Test
		@DisplayName("a duplicate already present in the stored value is collapsed when the list is next edited")
		void existingDuplicateCollapsedOnEdit() throws IOException
		{
			Manifest manifest = mainAttributes("NPM-PackageName: mypackage\r\nNG2-Components: A,A,B\r\nEntry-Point: dist\r\n");

			NewWebObjectAction.addComponentToNG2ComponentsAttribute(manifest, "C");

			assertEquals("A,B,C", ng2Components(manifest),
				"parsing must de-duplicate existing entries while preserving order");
		}
	}

	@Nested
	class NullAndBlankClassNames
	{
		@ParameterizedTest
		@NullAndEmptySource
		@ValueSource(strings = { "   ", "\t" })
		@DisplayName("AC: null/blank class name is ignored (attribute left untouched, no change reported)")
		void blankClassNameIgnored(String blank) throws IOException
		{
			Manifest manifest = mainAttributes("NPM-PackageName: mypackage\r\nNG2-Components: A\r\nEntry-Point: dist\r\n");

			boolean changed = NewWebObjectAction.addComponentToNG2ComponentsAttribute(manifest, blank);

			assertFalse(changed, "a null or blank class name must report no change");
			assertEquals("A", ng2Components(manifest),
				"a null or blank class name must not change the NG2-Components value");
		}

		@Test
		@DisplayName("class name is trimmed before being added")
		void classNameTrimmed() throws IOException
		{
			Manifest manifest = mainAttributes("NPM-PackageName: mypackage\r\nEntry-Point: dist\r\n");

			NewWebObjectAction.addComponentToNG2ComponentsAttribute(manifest, "  Padded  ");

			assertEquals("Padded", ng2Components(manifest), "surrounding whitespace on the class name must be trimmed");
		}
	}

	@Nested
	class ManifestRoundTrip
	{
		@Test
		@DisplayName("AC: a long component list round-trips (72-byte line wrap re-parses identically)")
		void longListRoundTripsThroughLineWrap() throws IOException
		{
			Manifest manifest = mainAttributes("NPM-PackageName: mypackage\r\nEntry-Point: dist\r\n");

			// enough long names that the NG2-Components header line exceeds the 72-byte
			// wrap limit of java.util.jar.Manifest
			String[] names = { "ServoyDefaultTextFieldComponent", "ServoyDefaultButtonComponent",
				"ServoyDefaultLabelComponent", "ServoyDefaultCalendarComponent", "ServoyDefaultComboboxComponent",
				"ServoyDefaultCheckGroupComponent" };
			StringBuilder expected = new StringBuilder();
			for (String name : names)
			{
				NewWebObjectAction.addComponentToNG2ComponentsAttribute(manifest, name);
				if (expected.length() > 0) expected.append(',');
				expected.append(name);
			}

			// serialize exactly as the production file round-trip does
			try (ByteArrayOutputStream out = new ByteArrayOutputStream())
			{
				manifest.write(out);
				String raw = new String(out.toByteArray(), StandardCharsets.UTF_8);
				assertTrue(raw.contains("\r\n "),
					"a value this long must be written across a continuation line (leading space)");
			}

			assertEquals(expected.toString(), ng2Components(roundTrip(manifest)),
				"the wrapped header must re-parse to the identical comma list");
		}

		@Test
		@DisplayName("AC: pre-existing per-component Name: sub-sections are preserved across the round-trip")
		void perComponentNameSubSectionsPreserved() throws IOException
		{
			// mirrors what addToManifest(...) writes: a Name: sub-section per component
			// spec with WebComponent: True
			String main = "Manifest-Version: 1.0\r\nNPM-PackageName: mypackage\r\nEntry-Point: dist\r\n";
			String sub = "\r\nName: existing/existing.spec\r\nWebComponent: True\r\n";
			Manifest manifest = parse(main + sub);

			NewWebObjectAction.addComponentToNG2ComponentsAttribute(manifest, "NewComponent");

			Manifest reparsed = roundTrip(manifest);
			assertEquals("NewComponent", reparsed.getMainAttributes().getValue(NG2_COMPONENTS),
				"the new component should be registered");
			Attributes entry = reparsed.getAttributes("existing/existing.spec");
			assertNotNull(entry, "the pre-existing per-component Name: sub-section must survive the round-trip");
			assertEquals("True", entry.getValue("WebComponent"), "the sub-section's attribute value must be preserved");
		}

		@Test
		@DisplayName("other main attributes (NPM-PackageName, Entry-Point) are preserved")
		void otherMainAttributesPreserved() throws IOException
		{
			Manifest manifest = mainAttributes("NPM-PackageName: mypackage\r\nEntry-Point: dist\r\n");

			NewWebObjectAction.addComponentToNG2ComponentsAttribute(manifest, "MyComponent");

			Attributes mainAttrs = roundTrip(manifest).getMainAttributes();
			assertEquals("mypackage", mainAttrs.getValue("NPM-PackageName"), "NPM-PackageName must be preserved");
			assertEquals("dist", mainAttrs.getValue("Entry-Point"), "Entry-Point must be preserved");
		}
	}
}
