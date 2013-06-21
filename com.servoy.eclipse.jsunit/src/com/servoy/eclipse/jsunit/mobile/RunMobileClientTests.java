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

import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;

import com.servoy.base.test.IJSUnitSuiteHandler;
import com.servoy.eclipse.jsunit.runner.SolutionJSUnitSuiteCodeBuilder;
import com.servoy.eclipse.jsunit.runner.TestTarget;
import com.servoy.eclipse.jsunit.scriptunit.RunJSUnitTests;
import com.servoy.j2db.util.StaticSingletonMap;

/**
 * Runner for mobile solution unit tests.
 * @author acostescu
 */
public class RunMobileClientTests extends RunJSUnitTests
{

	private SuiteBridge bridge;
	private final int clientConnectTimeout;
	private final SolutionJSUnitSuiteCodeBuilder builder;

	/**
	 * @param builder 
	 * @param clientConnectTimeout (in seconds) -1 fallsback to default value.
	 * @param monitor the monitor that can be used to check for stop requests before the tests start running.
	 */
	public RunMobileClientTests(TestTarget testTarget, SolutionJSUnitSuiteCodeBuilder builder, ILaunch launch, int clientConnectTimeout,
		IProgressMonitor monitor)
	{
		super(testTarget, launch, monitor);
		this.clientConnectTimeout = clientConnectTimeout;
		this.builder = builder;
	}

	@Override
	protected void prepareForTesting()
	{
		bridge = new SuiteBridge(clientConnectTimeout, -1);

		bridge.setSolutionSuiteJSCode(builder.getRootTestClassName(), builder.getCode());

		// perform the automated mobile export and start app. using [serverUrl]/MobileTestClient/servoy_mobile_test.html?noinitsmc=true&bid=[bridgeObjId]
		// as start URL; deploy that .war just like it's done in the .war exporter to Servoy Developer Tomcat
		// the service solution URL should use &nodebug = true when ran from developer
		Map<String, Object> sharedMap = StaticSingletonMap.instance();
		synchronized (sharedMap)
		{
			sharedMap.put(IJSUnitSuiteHandler.SERVOY_BRIDGE_KEY, bridge);
		}
	}

	/**
	 * @throws NullPointerException if the bridge is not yet initialized.
	 */
	public int getBridgeId()
	{
		return bridge.getId();
	}

	@Override
	protected void initializeAndRun(int port)
	{
		// TODO start browser and wait a while so that we get calls from the mobile browser app. - calls needed to set-up the test-suite hierarchy
		MobileClientTestSuite.prepare(bridge, testTarget, getScriptUnitRunnerClient(), getLaunchMonitor());
		runJUnitClass(port, MobileClientTestSuite.class);
	}

	@Override
	protected void cleanUpAfterPrepare()
	{
		Map<String, Object> sharedMap = StaticSingletonMap.instance();
		synchronized (sharedMap)
		{
			// only remove it if some other run session didn't already start (you can't have 2 test sessions running simultaneously
			if (sharedMap.get(IJSUnitSuiteHandler.SERVOY_BRIDGE_KEY) == bridge) sharedMap.remove(IJSUnitSuiteHandler.SERVOY_BRIDGE_KEY);
		}

		// TODO notify the client to change browser displayed contents to something like "Please close me?" just in case
	}

}
