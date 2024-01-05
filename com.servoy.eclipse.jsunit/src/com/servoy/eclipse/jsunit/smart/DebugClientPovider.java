package com.servoy.eclipse.jsunit.smart;

import com.servoy.j2db.debug.DebugClientHandler;
import com.servoy.j2db.debug.extensions.IDebugClientPovider;
import com.servoy.j2db.util.Debug;

/**
 * Provider of debug smart test client.
 *
 * @author acostescu
 */
public class DebugClientPovider implements IDebugClientPovider<DebugTestClient>
{

	public DebugClientPovider()
	{
	}

	@Override
	public DebugTestClient createDebugClient(final DebugClientHandler debugClientHandler)
	{
		try
		{
			return new DebugTestClient(debugClientHandler);
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return null;
	}

	@Override
	public boolean isSwingClient()
	{
		return true;
	}

}
