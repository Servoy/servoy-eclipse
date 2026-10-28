package com.servoy.eclipse.designer.tests.plainjunit.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.servoy.eclipse.designer.util.DesignerUtil;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.LayoutContainer;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * Tests for the inherited-element guards in {@link DesignerUtil} that the delete paths use (SVY-21476): a plain inherited element of a super form is
 * read-only and must not be deletable, while an override element - one that lives in the current form and carries its own extendsID - must be deletable.
 */
class DesignerUtilInheritedElementTest
{

	private static Form createForm() throws Exception
	{
		Constructor<Form> ctor = Form.class.getDeclaredConstructor(ISupportChilds.class, UUID.class);
		ctor.setAccessible(true);
		return ctor.newInstance(null, UUID.randomUUID());
	}

	private static LayoutContainer createContainer(ISupportChilds parent) throws Exception
	{
		Constructor<LayoutContainer> ctor = LayoutContainer.class.getDeclaredConstructor(ISupportChilds.class, UUID.class);
		ctor.setAccessible(true);
		return ctor.newInstance(parent, UUID.randomUUID());
	}

	private Form superForm;
	private Form currentForm;

	@BeforeEach
	void setUp() throws Exception
	{
		superForm = createForm();
		currentForm = createForm();
	}

	@Nested
	@DisplayName("plain inherited element of a super form")
	class PlainInheritedElement
	{
		private List<Object> selection;

		@BeforeEach
		void setUp() throws Exception
		{
			LayoutContainer inherited = createContainer(superForm);
			assertTrue(Utils.isInheritedFormElement(inherited, currentForm), "precondition: element must count as inherited");
			assertFalse(PersistHelper.isOverrideElement(inherited), "precondition: element must not be an override");
			selection = Collections.singletonList(PersistContext.create(inherited, currentForm));
		}

		@Test
		@DisplayName("is read-only inherited, so delete must be blocked")
		void isReadOnlyInherited()
		{
			assertTrue(DesignerUtil.containsReadOnlyInheritedElement(selection));
		}

		@Test
		@DisplayName("is still reported by containsInheritedElement")
		void isInherited()
		{
			assertTrue(DesignerUtil.containsInheritedElement(selection));
		}
	}

	@Nested
	@DisplayName("override element of the current form")
	class OverrideElement
	{
		private List<Object> selection;

		@BeforeEach
		void setUp() throws Exception
		{
			LayoutContainer inherited = createContainer(superForm);
			LayoutContainer override = createContainer(currentForm);
			override.setExtendsID(inherited.getUUID().toString());
			assertTrue(PersistHelper.isOverrideElement(override), "precondition: element must be an override");
			selection = Collections.singletonList(PersistContext.create(override, currentForm));
		}

		@Test
		@DisplayName("is not read-only inherited, so delete must stay enabled")
		void isNotReadOnlyInherited()
		{
			assertFalse(DesignerUtil.containsReadOnlyInheritedElement(selection));
		}

		@Test
		@DisplayName("is reported by containsInheritedElement, which keeps its old meaning")
		void isInherited()
		{
			assertTrue(DesignerUtil.containsInheritedElement(selection));
		}
	}

	@Nested
	@DisplayName("element of the current form without an extendsID")
	class OwnElement
	{
		private List<Object> selection;

		@BeforeEach
		void setUp() throws Exception
		{
			LayoutContainer own = createContainer(currentForm);
			selection = Collections.singletonList(PersistContext.create(own, currentForm));
		}

		@Test
		@DisplayName("is neither inherited nor read-only inherited")
		void isNotInherited()
		{
			assertFalse(DesignerUtil.containsReadOnlyInheritedElement(selection));
			assertFalse(DesignerUtil.containsInheritedElement(selection));
		}
	}

	@Nested
	@DisplayName("mixed selection")
	class MixedSelection
	{

		@Test
		@DisplayName("one read-only inherited element blocks the whole selection")
		void readOnlyInheritedBlocksSelection() throws Exception
		{
			LayoutContainer inherited = createContainer(superForm);
			LayoutContainer override = createContainer(currentForm);
			override.setExtendsID(createContainer(superForm).getUUID().toString());

			List<Object> selection = Arrays.asList(PersistContext.create(override, currentForm), PersistContext.create(inherited, currentForm));
			assertTrue(DesignerUtil.containsReadOnlyInheritedElement(selection));
		}

		@Test
		@DisplayName("only override and own elements do not block the selection")
		void overridesDoNotBlockSelection() throws Exception
		{
			LayoutContainer override = createContainer(currentForm);
			override.setExtendsID(createContainer(superForm).getUUID().toString());
			LayoutContainer own = createContainer(currentForm);

			List<Object> selection = Arrays.asList(PersistContext.create(override, currentForm), PersistContext.create(own, currentForm));
			assertFalse(DesignerUtil.containsReadOnlyInheritedElement(selection));
		}
	}

	@Test
	@DisplayName("an empty selection contains nothing")
	void emptySelection()
	{
		assertFalse(DesignerUtil.containsReadOnlyInheritedElement(Collections.emptyList()));
		assertFalse(DesignerUtil.containsReadOnlyInheritedElement(null));
	}
}
