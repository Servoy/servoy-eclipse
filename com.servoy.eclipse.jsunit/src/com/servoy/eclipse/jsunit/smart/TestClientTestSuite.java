/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

package com.servoy.eclipse.jsunit.smart;

import com.servoy.eclipse.jsunit.runner.ApplicationJSTestSuite;
import com.servoy.eclipse.jsunit.scriptunit.ScriptUnitTestRunNotifier;
import com.servoy.eclipse.model.test.TestTarget;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.J2DBGlobals;

import junit.framework.Test;
import junit.framework.TestResult;

/**
 * this test suite is used only when running tests from the UI .
 * Attach the ScriptUnit notifier  to update the ScriptUnit View .
 * @author obuligan
 *
 */
public class TestClientTestSuite extends ApplicationJSTestSuite
{

	public TestClientTestSuite(IApplication application, TestTarget target)
	{
		super(application, target, false);
	}

	@Override
	public void run(TestResult result)
	{
		ScriptUnitTestRunNotifier scriptUnitNotifier = new ScriptUnitTestRunNotifier(testList, result);
		result.addListener(scriptUnitNotifier);
		super.run(result);
	}

	public static void setTestTarget(IApplication app, TestTarget target)
	{
		ApplicationJSTestSuite.staticSuiteApplication = app;
		ApplicationJSTestSuite.staticTarget = target;
	}

	public static Test suite()
	{
		IServiceProvider prevServiceProvider = J2DBGlobals.getServiceProvider();
		J2DBGlobals.setServiceProvider(staticSuiteApplication);
		try
		{
			return new TestClientTestSuite(staticSuiteApplication, staticTarget);
		}
		finally
		{
			J2DBGlobals.setSingletonServiceProvider(prevServiceProvider);
		}
	}

}
