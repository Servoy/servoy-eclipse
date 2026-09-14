package com.servoy.eclipse.ui.property;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.servoy.j2db.persistence.ContentSpec;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IContentSpecConstants;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.RepositoryHelper;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.ValueList;

class RepositoryHelperShouldShowNameTest
{
	private static ContentSpec.Element formElement(String name)
	{
		ContentSpec.Element element = StaticContentSpecLoader.getContentSpec().getPropertyForObjectTypeByName(IRepository.FORMS, name);
		assertNotNull(element, "expected a content spec element for Form property '" + name + "'");
		return element;
	}

	@Test
	@DisplayName("SVY-21432: Form 'name' is shown again in the Properties view")
	void formNameIsShown()
	{
		ContentSpec.Element nameElement = formElement(IContentSpecConstants.PROPERTY_NAME);
		assertTrue(RepositoryHelper.shouldShow(IContentSpecConstants.PROPERTY_NAME, nameElement, Form.class, -1),
			"Form 'name' must be visible after SVY-21432 removed the shouldShow suppression");
	}

	@Test
	@DisplayName("Other visible Form identity properties remain shown")
	void otherFormPropertiesRemainShown()
	{
		assertTrue(RepositoryHelper.shouldShow("titleText", formElement("titleText"), Form.class, -1));
		assertTrue(RepositoryHelper.shouldShow("styleClass", formElement("styleClass"), Form.class, -1));
	}

	@Test
	@DisplayName("Genuinely suppressed Form properties stay hidden (size/background/labels)")
	void suppressedFormPropertiesStayHidden()
	{
		assertFalse(RepositoryHelper.shouldShow("size", formElement("size"), Form.class, -1));
		assertFalse(RepositoryHelper.shouldShow("background", formElement("background"), Form.class, -1));
		assertFalse(RepositoryHelper.shouldShow("labels", formElement("labels"), Form.class, -1));
	}

	@Test
	@DisplayName("ValueList suppression is unaffected: name shown, sortOptions hidden")
	void valueListSuppressionUnaffected()
	{
		ContentSpec.Element vlNameElement = StaticContentSpecLoader.getContentSpec()
			.getPropertyForObjectTypeByName(IRepository.VALUELISTS, IContentSpecConstants.PROPERTY_NAME);
		assertNotNull(vlNameElement);
		assertTrue(RepositoryHelper.shouldShow(IContentSpecConstants.PROPERTY_NAME, vlNameElement, ValueList.class, -1));

		ContentSpec.Element vlSortElement = StaticContentSpecLoader.getContentSpec()
			.getPropertyForObjectTypeByName(IRepository.VALUELISTS, "sortOptions");
		assertNotNull(vlSortElement);
		assertFalse(RepositoryHelper.shouldShow("sortOptions", vlSortElement, ValueList.class, -1));
	}
}
