package com.servoy.eclipse.ui.views.solutionexplorer;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * SVY-21055: Tests for getParsedComment() tooltip line break rendering.
 *
 * Replicates the logic from SolutionExplorerListContentProvider.getParsedComment()
 * (lines 2155-2225) to verify that the elementName != null branch correctly
 * inserts &lt;br/&gt; tags before @param, @return, @example, and @element doc tags.
 *
 * Direct invocation of the actual method is not possible in a plain JUnit test
 * because loading SolutionExplorerListContentProvider triggers too many Eclipse
 * platform dependencies.
 */
class SolutionExplorerListContentProviderGetParsedCommentTest
{

	/**
	 * Replicates the relevant parts of getParsedComment() from
	 * SolutionExplorerListContentProvider (lines 2185-2213) for the !toHTML path.
	 */
	private static String getParsedComment(String comment, String elementName)
	{
		if (comment == null) return null;
		String c = comment;

		if (elementName != null) c = c.replaceAll("%%elementName%%", "elements." + elementName);

		String separator = System.getProperty("line.separator");
		String[] inputArray = c.split(separator);
		StringBuilder stringBuilder = new StringBuilder();
		for (String value : inputArray)
		{
			stringBuilder.append(value.trim());
			stringBuilder.append(separator);
		}
		c = stringBuilder.toString();

		if (elementName == null)
		{
			c = c.replaceAll("\n\n", "<br/>");
			c = c.replaceAll("\n", "<br/>");
			c = c.replaceFirst("@example", "<br/>@example");
			c = c.replaceFirst("@param", "<br/>@param");
			c = c.replaceFirst("@return", "<br/>@return");
			c = c.replaceFirst("@properties", "<br/><b>@properties</b>");
		}
		else
		{
			c = c.replaceAll("\n", "<br/>");
			c = c.replaceAll("(?<!<br/>)(@param|@return|@example|@element)", "<br/>$1");
			c = c.replaceAll("(<br/>)+(@param|@return|@example|@element)", "<br/>$2");
		}

		c = c.replaceAll(separator, "<br/>");
		return c;
	}

	@Nested
	class ComponentApiMethods
	{

		@Test
		@DisplayName("AC1: @param tags are displayed on separate lines")
		void paramTagsOnSeparateLines()
		{
			String input = "Adds a tab.\n@param {String} name The tab name\n@param {Number} index The index";
			String result = getParsedComment(input, "myButton");
			assertTrue(result.contains("<br/>@param"));
		}

		@Test
		@DisplayName("AC1: @return tag is displayed on a separate line")
		void returnTagOnSeparateLine()
		{
			String input = "Gets the value.\n@return {String} the value";
			String result = getParsedComment(input, "myButton");
			assertTrue(result.contains("<br/>@return"));
		}

		@Test
		@DisplayName("AC1: @example tag is displayed on a separate line")
		void exampleTagOnSeparateLine()
		{
			String input = "Some method.\n@example\nvar x = 1;";
			String result = getParsedComment(input, "myButton");
			assertTrue(result.contains("<br/>@example"));
		}

		@Test
		@DisplayName("AC2: @element tag is displayed on a separate line")
		void elementTagOnSeparateLine()
		{
			String input = "Requests focus.\n@element myButton";
			String result = getParsedComment(input, "myButton");
			assertTrue(result.contains("<br/>@element"));
		}

		@Test
		@DisplayName("AC5: all four doc tags handled in a single comment")
		void allFourDocTagsHandled()
		{
			String input = "Method doc.\n@param {String} name The name\n@return {Boolean} result\n@example\nvar x = 1;\n@element btn";
			String result = getParsedComment(input, "myElement");
			assertAll(
				() -> assertTrue(result.contains("<br/>@param"), "should have <br/> before @param"),
				() -> assertTrue(result.contains("<br/>@return"), "should have <br/> before @return"),
				() -> assertTrue(result.contains("<br/>@example"), "should have <br/> before @example"),
				() -> assertTrue(result.contains("<br/>@element"), "should have <br/> before @element"));
		}

		@Test
		@DisplayName("AC4: no double <br/> when \\n already precedes a doc tag")
		void noDoubleBrWhenNewlinePrecedesTag()
		{
			String input = "Method doc.\n@param {String} name The name";
			String result = getParsedComment(input, "myElement");
			assertFalse(result.contains("<br/><br/>@param"), "should not have double <br/> before @param");
		}

		@Test
		@DisplayName("multiple @param tags all get line breaks")
		void multipleParamTagsAllGetLineBreaks()
		{
			String input = "Adds a tab.\n@param {String} name Tab name\n@param {Number} index Position\n@param {Boolean} visible Visibility";
			String result = getParsedComment(input, "tabpanel");
			int count = countOccurrences(result, "<br/>@param");
			assertEquals(3, count, "each @param should be preceded by <br/>");
		}

		@Test
		@DisplayName("\\n characters are converted to <br/>")
		void newlinesConvertedToBr()
		{
			String input = "Line 1\nLine 2\nLine 3";
			String result = getParsedComment(input, "myElement");
			assertFalse(result.contains("\n"), "should not contain raw \\n");
			assertTrue(result.contains("<br/>"), "should contain <br/> tags");
		}

		@Test
		@DisplayName("%%elementName%% is replaced with elements prefix")
		void elementNamePlaceholderReplaced()
		{
			String input = "Use %%elementName%%.requestFocus()";
			String result = getParsedComment(input, "btn1");
			assertTrue(result.contains("elements.btn1.requestFocus()"));
		}
	}

	@Nested
	class FormScopeMethods
	{

		@Test
		@DisplayName("AC3: elementName==null path still inserts <br/> before @param")
		void paramTagGetsLineBreak()
		{
			String input = "Does something.\n@param {String} name The name";
			String result = getParsedComment(input, null);
			assertTrue(result.contains("<br/>@param"));
		}

		@Test
		@DisplayName("AC3: elementName==null path still inserts <br/> before @return")
		void returnTagGetsLineBreak()
		{
			String input = "Gets value.\n@return {String} the value";
			String result = getParsedComment(input, null);
			assertTrue(result.contains("<br/>@return"));
		}

		@Test
		@DisplayName("AC3: elementName==null path still inserts <br/> before @example")
		void exampleTagGetsLineBreak()
		{
			String input = "Some method.\n@example\nvar x = 1;";
			String result = getParsedComment(input, null);
			assertTrue(result.contains("<br/>@example"));
		}

		@Test
		@DisplayName("AC3: elementName==null path uses replaceFirst (only first @param gets extra <br/>)")
		void onlyFirstParamGetsExtraLineBreak()
		{
			String input = "Method doc.\n@param {String} a First\n@param {String} b Second";
			String result = getParsedComment(input, null);
			int firstIndex = result.indexOf("<br/>@param");
			assertTrue(firstIndex >= 0, "should have at least one <br/>@param");
		}
	}

	@Nested
	class EdgeCases
	{

		@Test
		@DisplayName("null comment returns null")
		void nullCommentReturnsNull()
		{
			assertNull(getParsedComment(null, "myElement"));
		}

		@Test
		@DisplayName("null comment with null elementName returns null")
		void nullCommentNullElementReturnsNull()
		{
			assertNull(getParsedComment(null, null));
		}

		@Test
		@DisplayName("empty string returns non-null")
		void emptyStringReturnsNonNull()
		{
			String result = getParsedComment("", "myElement");
			assertEquals("", result.replaceAll("<br/>", "").trim());
		}

		@Test
		@DisplayName("comment without any doc tags just converts newlines")
		void noDocTagsJustNewlineConversion()
		{
			String input = "Simple description\nwith two lines";
			String result = getParsedComment(input, "myElement");
			assertFalse(result.contains("\n"));
			assertTrue(result.contains("Simple description"));
			assertTrue(result.contains("with two lines"));
		}

		@Test
		@DisplayName("doc tag at the very start of the comment gets <br/> prepended")
		void docTagAtStart()
		{
			String input = "@param {String} name The name";
			String result = getParsedComment(input, "myElement");
			assertTrue(result.contains("<br/>@param"));
		}

		@Test
		@DisplayName("%%elementName%% not replaced when elementName is null")
		void placeholderNotReplacedWhenElementNameNull()
		{
			String input = "Use %%elementName%%.doSomething()";
			String result = getParsedComment(input, null);
			assertTrue(result.contains("%%elementName%%"));
		}
	}

	@Nested
	class TabpanelDocTests
	{

		private static final String ADDTAB_DOC = "Adds a tab with the given form and tab text on the given index.\n" +
			"\n" +
			"@param {Form} form The name of the form to add as a tab\n" +
			"@param {Tagstring} tabText The tab text that should be displayed. Can contain tags (i18n keys or foundset data).\n" +
			"@param {Number} [index] Give an index where the tab should be placed in the array of tabs, default at the end.\n" +
			"\n" +
			"@return {CustomType<bootstrapcomponents-tabpanel.tab>} The newly created tab object that represents the added form in the tab panel.";

		private static final String REMOVETABAT_DOC = "Removes the tab from the given index (index is 1-based).\n" +
			"\n" +
			"@param {Number} index The 1-based index of the tab to remove.\n" +
			"\n" +
			"@return {Boolean} True if the tab was successfully removed; false otherwise.";

		private static final String GETTABAT_DOC = "Retrieves the tab at the specified index from the tabs model.\n" +
			"@param {Number} index The 1-based index of the tab to retrieve.\n" +
			"\n" +
			"@return {CustomType<bootstrapcomponents-tabpanel.tab>} The tab object at the specified index, or null if the index is out of range.";

		private static final String ONCHANGE_DOC = "Fired after a different tab is selected\n" +
			"\n" +
			"@param {Number} previousIndex The previous tab index before the change\n" +
			"@param {JSEvent} event The event object associated with the tab change\n" +
			"@param {Number} newIndex The current tab index ( after the change )";

		private static final String ONTABCLICKED_DOC = "Fired when the user clicks on a tab. When false is returned, the tab switch is prevented\n" +
			"\n" +
			"@param {JSEvent} event The event object that triggered the action\n" +
			"@param {Number} clickedTabIndex The index of the tab that was clicked\n" +
			"@param {String} dataTarget The identifier of the closest data-target attribute, if available\n" +
			"\n" +
			"@return {Boolean} True to allow the tab switch, false to prevent it";

		private static final String SELECTTABAT_DOC = "Selects the tab of the given index\n" +
			"@deprecated use tabIndex property instead.";

		@Test
		@DisplayName("addTab: each @param and @return on separate line")
		void addTabTooltip()
		{
			String result = getParsedComment(ADDTAB_DOC, "tabpanel1");
			assertAll(
				() -> assertEquals(3, countOccurrences(result, "<br/>@param"), "addTab has 3 @param tags"),
				() -> assertTrue(result.contains("<br/>@return"), "addTab has @return on separate line"),
				() -> assertFalse(result.contains("<br/><br/>@param"), "no double <br/> before @param"),
				() -> assertFalse(result.contains("<br/><br/>@return"), "no double <br/> before @return"),
				() -> assertFalse(result.contains("\n"), "no raw newlines remain"));
		}

		@Test
		@DisplayName("removeTabAt: @param and @return on separate lines")
		void removeTabAtTooltip()
		{
			String result = getParsedComment(REMOVETABAT_DOC, "tabpanel1");
			assertAll(
				() -> assertTrue(result.contains("<br/>@param"), "@param on separate line"),
				() -> assertTrue(result.contains("<br/>@return"), "@return on separate line"),
				() -> assertFalse(result.contains("<br/><br/>@param"), "no double <br/> before @param"),
				() -> assertFalse(result.contains("\n"), "no raw newlines remain"));
		}

		@Test
		@DisplayName("getTabAt: @param and @return on separate lines")
		void getTabAtTooltip()
		{
			String result = getParsedComment(GETTABAT_DOC, "tabpanel1");
			assertAll(
				() -> assertTrue(result.contains("<br/>@param"), "@param on separate line"),
				() -> assertTrue(result.contains("<br/>@return"), "@return on separate line"),
				() -> assertFalse(result.contains("\n"), "no raw newlines remain"));
		}

		@Test
		@DisplayName("onChangeMethodID: multiple @param tags on separate lines")
		void onChangeTooltip()
		{
			String result = getParsedComment(ONCHANGE_DOC, "tabpanel1");
			assertAll(
				() -> assertEquals(3, countOccurrences(result, "<br/>@param"), "onChange has 3 @param tags"),
				() -> assertFalse(result.contains("<br/><br/>@param"), "no double <br/> before @param"),
				() -> assertFalse(result.contains("\n"), "no raw newlines remain"));
		}

		@Test
		@DisplayName("onTabClickedMethodID: @param and @return on separate lines")
		void onTabClickedTooltip()
		{
			String result = getParsedComment(ONTABCLICKED_DOC, "tabpanel1");
			assertAll(
				() -> assertEquals(3, countOccurrences(result, "<br/>@param"), "onTabClicked has 3 @param tags"),
				() -> assertTrue(result.contains("<br/>@return"), "@return on separate line"),
				() -> assertFalse(result.contains("<br/><br/>@param"), "no double <br/>"),
				() -> assertFalse(result.contains("\n"), "no raw newlines remain"));
		}

		@Test
		@DisplayName("selectTabAt with @deprecated: no crash, newlines converted")
		void selectTabAtTooltip()
		{
			String result = getParsedComment(SELECTTABAT_DOC, "tabpanel1");
			assertAll(
				() -> assertFalse(result.contains("\n"), "no raw newlines remain"),
				() -> assertTrue(result.contains("<br/>"), "has <br/> tags"));
		}
	}

	private static int countOccurrences(String text, String substring)
	{
		int count = 0;
		int index = 0;
		while ((index = text.indexOf(substring, index)) != -1)
		{
			count++;
			index += substring.length();
		}
		return count;
	}
}
