package com.servoy.eclipse.cypress.plainjunit.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.servoy.eclipse.cypress.services.ViewportWidths;

/**
 * SVY-21619: covers {@link ViewportWidths#parse(String)} - the shared comma-separated widths parser used by both Cypress spec generators
 * (acceptance criterion: "invalid viewportWidths token -&gt; Error: naming the token", "widths in given order", "dedupe").
 */
@DisplayName("ViewportWidths.parse")
class ViewportWidthsTest
{
	@Nested
	@DisplayName("valid input")
	class Valid
	{
		@Test
		@DisplayName("'375,1024' parses to [375, 1024] in the given order")
		void parsesTwoWidthsInOrder()
		{
			assertEquals(List.of(375, 1024), ViewportWidths.parse("375,1024"));
		}

		@Test
		@DisplayName("order is preserved and is not sorted ('1024,375' stays [1024, 375])")
		void orderPreservedNotSorted()
		{
			assertEquals(List.of(1024, 375), ViewportWidths.parse("1024,375"));
		}

		@Test
		@DisplayName("duplicate widths are dropped keeping the first occurrence ('375,375,1024' -> [375, 1024])")
		void dedupeKeepsFirstOccurrence()
		{
			assertEquals(List.of(375, 1024), ViewportWidths.parse("375,375,1024"));
		}

		@Test
		@DisplayName("whitespace around tokens is trimmed (' 375 , 1024 ' -> [375, 1024])")
		void whitespaceTrimmed()
		{
			assertEquals(List.of(375, 1024), ViewportWidths.parse(" 375 , 1024 "));
		}

		@Test
		@DisplayName("blank tokens between commas are skipped ('375,,1024' -> [375, 1024])")
		void blankTokensSkipped()
		{
			assertEquals(List.of(375, 1024), ViewportWidths.parse("375,,1024"));
		}

		@Test
		@DisplayName("a single width parses to a one-element list")
		void singleWidth()
		{
			assertEquals(List.of(768), ViewportWidths.parse("768"));
		}

		@Test
		@DisplayName("the returned list is mutable and independent, so callers may wrap it freely")
		void returnsIndependentList()
		{
			List<Integer> first = ViewportWidths.parse("375,1024");
			first.add(999);
			assertEquals(List.of(375, 1024), ViewportWidths.parse("375,1024"), "a later parse must be unaffected by mutating an earlier result");
		}
	}

	@Nested
	@DisplayName("empty input")
	class Empty
	{
		@ParameterizedTest
		@NullAndEmptySource
		@ValueSource(strings = { "   ", "\t", " \n " })
		@DisplayName("null, empty or blank input yields an empty list")
		void nullOrBlankYieldsEmpty(String csv)
		{
			assertTrue(ViewportWidths.parse(csv).isEmpty(), "blank input must give an empty list, not throw");
		}

		@Test
		@DisplayName("a string of only commas and whitespace yields an empty list")
		void onlySeparatorsYieldsEmpty()
		{
			assertTrue(ViewportWidths.parse(" , , ").isEmpty());
		}
	}

	@Nested
	@DisplayName("invalid tokens")
	class Invalid
	{
		@ParameterizedTest
		@ValueSource(strings = { "0", "-5", "abc", "12px", "3.5", "1e3", "375,abc", "1024,0" })
		@DisplayName("a non-positive or non-numeric token throws IllegalArgumentException naming the offending token")
		void invalidTokenThrowsNamingToken(String csv)
		{
			IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ViewportWidths.parse(csv));
			String offending = csv.substring(csv.lastIndexOf(',') + 1).trim();
			assertTrue(ex.getMessage().contains("'" + offending + "'"),
				"message must name the offending token '" + offending + "': " + ex.getMessage());
		}

		@Test
		@DisplayName("zero is rejected as not a positive integer")
		void zeroRejected()
		{
			IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ViewportWidths.parse("0"));
			assertTrue(ex.getMessage().contains("'0'"), ex.getMessage());
			assertTrue(ex.getMessage().contains("positive"), "message should explain it must be a positive integer: " + ex.getMessage());
		}

		@Test
		@DisplayName("a negative width is rejected")
		void negativeRejected()
		{
			IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ViewportWidths.parse("-5"));
			assertTrue(ex.getMessage().contains("'-5'"), ex.getMessage());
		}

		@Test
		@DisplayName("a NumberFormatException does not leak - it is wrapped as IllegalArgumentException")
		void numberFormatDoesNotLeak()
		{
			IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ViewportWidths.parse("abc"));
			assertFalse(ex instanceof NumberFormatException, "the thrown type must be IllegalArgumentException, not a leaked NumberFormatException");
			assertTrue(ex.getMessage().contains("'abc'"), ex.getMessage());
		}
	}
}
