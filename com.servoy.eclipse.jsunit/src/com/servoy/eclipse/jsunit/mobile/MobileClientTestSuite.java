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

package com.servoy.eclipse.jsunit.mobile;

import org.eclipse.core.runtime.IProgressMonitor;

import com.servoy.eclipse.jsunit.mobile.SuiteBridge.ICancelMonitor;
import com.servoy.eclipse.jsunit.scriptunit.RemoteScriptUnitRunnerClient;
import com.servoy.eclipse.jsunit.scriptunit.ScriptUnitTestRunNotifier;
import com.servoy.eclipse.model.test.TestTarget;

import junit.framework.Test;
import junit.framework.TestResult;
import junit.framework.TestSuite;

/**
 * Test suite that is generated and ran remotely from inside a mobile client browser.
 * @author acostescu
 */
public class MobileClientTestSuite extends TestSuite
{

	private static SuiteBridge staticBridge;
	private static TestTarget staticTarget;
	private static RemoteScriptUnitRunnerClient staticRemoteScriptUnitRunnerClient;
	private static IProgressMonitor staticLaunchMonitor;
	private final SuiteBridge bridge;
	private final TestTarget target; // TODO
	private final RemoteScriptUnitRunnerClient remoteScriptUnitRunnerClient;

	public MobileClientTestSuite(SuiteBridge bridge, TestTarget target, RemoteScriptUnitRunnerClient remoteScriptUnitRunnerClient)
	{
		this.bridge = bridge;
		this.target = target;
		this.remoteScriptUnitRunnerClient = remoteScriptUnitRunnerClient;

		bridge.createTestTree(this, new ICancelMonitor()
		{
			@Override
			public boolean isCanceled()
			{
				return staticLaunchMonitor.isCanceled();
			}
		});
	}

	public static void prepare(SuiteBridge bridge, TestTarget target, RemoteScriptUnitRunnerClient remoteScriptUnitRunnerClient, IProgressMonitor launchMonitor)
	{
		staticBridge = bridge;
		staticTarget = target;
		staticRemoteScriptUnitRunnerClient = remoteScriptUnitRunnerClient;
		staticLaunchMonitor = launchMonitor;
	}

	public static Test suite()
	{
		return new MobileClientTestSuite(staticBridge, staticTarget, staticRemoteScriptUnitRunnerClient);
	}

	@Override
	public void run(TestResult result)
	{
		if (remoteScriptUnitRunnerClient != null) remoteScriptUnitRunnerClient.setTestResultReference(result); // this direct reference to "result" is needed so that
		// a potential STOP from the UI reaches the "result" directly instead of waiting for the next listener event;
		// because in this case that flag can be tested from time to time while waiting for the client to connect - so it could actually STOP while waiting indefinitely (or a long time) for the client.
		// otherwise it could just wait forever or until a timeout occurs...

		ScriptUnitTestRunNotifier scriptUnitNotifier = new ScriptUnitTestRunNotifier(bridge.getTestList(), result, target);
		result.addListener(scriptUnitNotifier);

		bridge.runClientTests(result);
	}
}
