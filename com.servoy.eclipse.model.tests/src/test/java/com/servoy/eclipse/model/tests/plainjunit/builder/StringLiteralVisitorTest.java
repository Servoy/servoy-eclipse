/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2026 Servoy BV

 This program is free software; you can redistribute it and/or modify it under
 the terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along
 with this program; if not, see http://www.gnu.org/licenses or write to the Free
 Software Foundation,Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
*/

package com.servoy.eclipse.model.tests.plainjunit.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.dltk.compiler.problem.IProblemReporter;
import org.eclipse.dltk.javascript.ast.Script;
import org.eclipse.dltk.javascript.ast.StringLiteral;
import org.eclipse.dltk.javascript.parser.JavaScriptParserUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.servoy.eclipse.model.builder.StringLiteralVisitor;
import com.servoy.eclipse.model.tests.plainjunit.utils.SeverityPreferences;

/**
 * {@link StringLiteralVisitor}: which string literals of a script are offered for externalizing, with their line bounds and their index
 * within the line.
 */
class StringLiteralVisitorTest
{
	/** The JavaScript parser reads its options from the Eclipse preferences. */
	private SeverityPreferences preferences;

	@BeforeEach
	void installPreferences()
	{
		preferences = new SeverityPreferences();
	}

	@AfterEach
	void closePreferences()
	{
		preferences.close();
	}

	private static final IProblemReporter NO_REPORTER = Mockito.mock(IProblemReporter.class);

	/** Visits the script and returns "value@lineText#index" for every literal offered. */
	private static List<String> visit(String source) throws Exception
	{
		List<String> found = new ArrayList<>();
		Script script = JavaScriptParserUtil.parse(source, NO_REPORTER);
		script.traverse(new StringLiteralVisitor(source.toCharArray())
		{
			@Override
			public boolean visitStringLiteral(StringLiteral node, int lineStartIdx, int lineEndIdx, int stringLiteralIdx)
			{
				found.add(node.getValue() + "@" + source.substring(lineStartIdx, lineEndIdx + 1) + "#" + stringLiteralIdx);
				return true;
			}
		});
		return found;
	}

	@Test
	@DisplayName("literals are numbered per line, starting at 1 on every new line")
	void numberedPerLine() throws Exception
	{
		assertEquals(List.of("a@var x = 'a' + 'b';#1", "b@var x = 'a' + 'b';#2", "c@var y = 'c';#1"), visit("var x = 'a' + 'b';\nvar y = 'c';"));
	}

	@Test
	@DisplayName("i18n: keys and the arguments of i18n.getI18NMessage are not offered")
	void i18nSkipped() throws Exception
	{
		assertEquals(List.of("plain@var z = 'plain';#1"),
			visit("var x = i18n.getI18NMessage('some.key', ['arg']);\nvar y = 'i18n:other.key';\nvar z = 'plain';"));
	}
}
