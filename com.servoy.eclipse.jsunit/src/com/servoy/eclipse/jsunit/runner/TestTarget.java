/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.eclipse.jsunit.runner;

import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.Pair;

/**
 * Specifies which tests of an active solution's subtree should run.
 * @author acostescu
 */
public class TestTarget
{

	public final Pair<Solution, String> globalScopeToTest; // if a global scope should be tested 
	public final Solution moduleToTest; // if a module is to be tested
	public final Form formToTest; // if a form scope should be tested
	public final ScriptMethod testMethodToTest; // if only one test method should be tested

	public TestTarget(Pair<Solution, String> globalScopeToTest)
	{
		this.globalScopeToTest = globalScopeToTest;
		this.moduleToTest = null;
		this.formToTest = null;
		this.testMethodToTest = null;
	}

	public TestTarget(Solution moduleToTest)
	{
		this.globalScopeToTest = null;
		this.moduleToTest = moduleToTest;
		this.formToTest = null;
		this.testMethodToTest = null;
	}

	public TestTarget(Form formToTest)
	{
		this.globalScopeToTest = null;
		this.moduleToTest = null;
		this.formToTest = formToTest;
		this.testMethodToTest = null;
	}

	public TestTarget(ScriptMethod testMethodToTest)
	{
		this.globalScopeToTest = null;
		this.moduleToTest = null;
		this.formToTest = null;
		this.testMethodToTest = testMethodToTest;
	}

}