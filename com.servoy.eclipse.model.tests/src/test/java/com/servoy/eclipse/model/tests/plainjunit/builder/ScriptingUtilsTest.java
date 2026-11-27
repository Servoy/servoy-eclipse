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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Types;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.servoy.eclipse.model.builder.ScriptingUtils;
import com.servoy.eclipse.model.tests.plainjunit.utils.SeverityPreferences;
import com.servoy.eclipse.model.tests.plainjunit.utils.TestPersists;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.util.UUID;

/**
 * {@link ScriptingUtils}: the small JavaScript analyses the builder runs on method declarations and variable default values.
 */
class ScriptingUtilsTest
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

	private static ScriptVariable variable(String defaultValue)
	{
		ScriptVariable variable = TestPersists.create(null, IRepository.SCRIPTVARIABLES, UUID.randomUUID());
		variable.setName("STATUS");
		variable.setDefaultValue(defaultValue);
		return variable;
	}

	private static ScriptMethod method(String declaration)
	{
		ScriptMethod method = TestPersists.create(null, IRepository.METHODS, UUID.randomUUID());
		method.setName("m");
		method.setDeclaration(declaration);
		return method;
	}

	private static List<String> ids(List<? extends IDataProvider> dataProviders)
	{
		return dataProviders.stream().map(IDataProvider::getDataProviderID).toList();
	}

	private static List<Integer> types(List<? extends IDataProvider> dataProviders)
	{
		return dataProviders.stream().map(dp -> Integer.valueOf(dp.getDataProviderType())).toList();
	}

	@Test
	@DisplayName("getArgumentsUsage returns the line (relative to the function) of the first arguments[] access")
	void argumentsUsageLine()
	{
		assertEquals(2, ScriptingUtils.getArgumentsUsage("function f() {\n var a = 1;\n return arguments[0];\n}"));
	}

	@Test
	@DisplayName("getArgumentsUsage on the first line is 0")
	void argumentsUsageFirstLine()
	{
		assertEquals(0, ScriptingUtils.getArgumentsUsage("function f() { return arguments[0]; }"));
	}

	@Test
	@DisplayName("getArgumentsUsage is -1 without arguments[] access or for something that is not a single function")
	void argumentsUsageNone()
	{
		assertAll(() -> assertEquals(-1, ScriptingUtils.getArgumentsUsage("function f(a) { return a; }")),
			() -> assertEquals(-1, ScriptingUtils.getArgumentsUsage("var x = arguments[0];")),
			() -> assertEquals(-1, ScriptingUtils.getArgumentsUsage("function f() { var arguments2 = []; return arguments2[0]; }")));
	}

	@Test
	@DisplayName("enum data providers get the property names and a type per literal (number, text, null, otherwise media)")
	void enumDataProviders()
	{
		ScriptVariable variable = variable("{ A: 1, B: -2, C: 'x', D: null, E: [1], F: 10n }");

		var result = ScriptingUtils.getEnumDataProviders(variable);

		assertAll(() -> assertEquals(List.of("STATUS.A", "STATUS.B", "STATUS.C", "STATUS.D", "STATUS.E", "STATUS.F"), ids(result)),
			() -> assertEquals(List.of(IColumnTypes.NUMBER, IColumnTypes.NUMBER, IColumnTypes.TEXT, Types.NULL, IColumnTypes.MEDIA, IColumnTypes.NUMBER),
				types(result)));
	}

	@Test
	@DisplayName("constant data providers are built the same way as enum ones")
	void constantDataProviders()
	{
		ScriptVariable variable = variable("{ X: 'a', Y: 3.5 }");

		var result = ScriptingUtils.getConstantDataProviders(variable);

		assertAll(() -> assertEquals(List.of("STATUS.X", "STATUS.Y"), ids(result)),
			() -> assertEquals(List.of(IColumnTypes.TEXT, IColumnTypes.NUMBER), types(result)));
	}

	@Test
	@DisplayName("a default value that is not an object literal gives no data providers")
	void noObjectLiteral()
	{
		assertAll(() -> assertEquals(List.of(), ScriptingUtils.getEnumDataProviders(variable("5"))),
			() -> assertEquals(List.of(), ScriptingUtils.getConstantDataProviders(variable("'abc'"))));
	}

	@Test
	@DisplayName("a function returning a value without @return is missing return docs")
	void missingReturnDocs()
	{
		assertTrue(ScriptingUtils.isMissingReturnDocs(method("/** doc */\nfunction m() { return 1; }")));
	}

	@Test
	@DisplayName("no return value, a documented @return, a @constructor or only a nested function's return are not missing return docs")
	void notMissingReturnDocs()
	{
		assertAll(() -> assertFalse(ScriptingUtils.isMissingReturnDocs(method("function m() { return; }"))),
			() -> assertFalse(ScriptingUtils.isMissingReturnDocs(method("/** @return {Number} */\nfunction m() { return 1; }"))),
			() -> assertFalse(ScriptingUtils.isMissingReturnDocs(method("/** @constructor */\nfunction m() { return 1; }"))),
			() -> assertFalse(ScriptingUtils.isMissingReturnDocs(method("function m() { var f = function() { return 1; }; var g = () => { return 2; }; }"))),
			() -> assertFalse(ScriptingUtils.isMissingReturnDocs(method(""))));
	}
}
