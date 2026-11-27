package com.servoy.eclipse.model.tests.plainjunit.mobile;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.servoy.eclipse.model.mobile.exporter.ScriptStringBuilder;

class ScriptStringBuilderTest
{
	@Test
	@DisplayName("append without tracking just concatenates")
	void noTracking()
	{
		ScriptStringBuilder sb = new ScriptStringBuilder(false);
		sb.append("a").append("b\n").append("c");
		assertEquals("ab\nc", sb.toString());
	}

	@Test
	@DisplayName("getCurrentLineNumber throws when tracking is disabled")
	void noTrackingThrows()
	{
		ScriptStringBuilder sb = new ScriptStringBuilder(false);
		org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, sb::getCurrentLineNumber);
	}

	@Test
	@DisplayName("tracking counts newlines and starts at line 1")
	void tracking()
	{
		ScriptStringBuilder sb = new ScriptStringBuilder(true);
		assertEquals(1L, sb.getCurrentLineNumber());
		sb.append("line1\n");
		assertEquals(2L, sb.getCurrentLineNumber());
		sb.append("line2\r\nline3\r");
		assertEquals(4L, sb.getCurrentLineNumber());
		assertEquals("line1\nline2\r\nline3\r", sb.toString());
	}

	@Test
	@DisplayName("newLineSeparatorCount handles null, empty and mixed separators")
	void count()
	{
		assertEquals(0L, ScriptStringBuilder.newLineSeparatorCount(null));
		assertEquals(0L, ScriptStringBuilder.newLineSeparatorCount(""));
		assertEquals(1L, ScriptStringBuilder.newLineSeparatorCount("a\n"));
		assertEquals(1L, ScriptStringBuilder.newLineSeparatorCount("a\r\n"));
		assertEquals(1L, ScriptStringBuilder.newLineSeparatorCount("a\r"));
		assertEquals(2L, ScriptStringBuilder.newLineSeparatorCount("a\n\r"));
		assertEquals(2L, ScriptStringBuilder.newLineSeparatorCount("\n\r\n"));
	}
}
