/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2025 Servoy BV

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

package com.servoy.eclipse.developersolution;

import java.util.List;

import org.mozilla.javascript.NativeJavaObject;

import com.servoy.j2db.IBasicFormManager;
import com.servoy.j2db.IDesignerCallback;
import com.servoy.j2db.debug.DebugNGClient;
import com.servoy.j2db.debug.DebugNGFormMananger;
import com.servoy.j2db.scripting.IExecutingEnviroment;
import com.servoy.j2db.scripting.InstanceJavaMembers;
import com.servoy.j2db.scripting.SolutionScope;
import com.servoy.j2db.server.ngclient.INGClientWebsocketSession;

/**
 * @author jcompagner
 *
 * @since 2025.09
 *
 */
public class DeveloperNGClient extends DebugNGClient
{

	/**
	 * @param wsSession
	 * @param designerCallback
	 * @throws Exception
	 */
	public DeveloperNGClient(INGClientWebsocketSession wsSession, IDesignerCallback designerCallback) throws Exception
	{
		super(wsSession, designerCallback);
	}

	@Override
	protected void runWhileShowingLoadingIndicator(Runnable r)
	{
		// just call the run because no need to show or hide the loading indicator
		r.run();
	}

	@Override
	protected IExecutingEnviroment createScriptEngine()
	{
		IExecutingEnviroment engine = super.createScriptEngine();
		SolutionScope scope = engine.getSolutionScope();
		scope.put("developerBridge", scope,
			new NativeJavaObject(scope, new DeveloperBridge(this), new InstanceJavaMembers(scope, DeveloperBridge.class)));

		return engine;
	}


	@Override
	protected IBasicFormManager createFormManager()
	{
		return new DebugNGFormMananger(this)
		{
			@Override
			protected void wrapInShowLoadingIndicator(List<Runnable> invokeLaterRunnables)
			{
				// ignore
			}
		};
	}

}
