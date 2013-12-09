package com.servoy.eclipse.jsunit.smart;

import com.servoy.j2db.debug.DebugClientHandler;
import com.servoy.j2db.debug.extensions.IDebugClientPovider;

/**
 * Provider of debug smart test client.
 * 
 * @author acostescu
 */
public class DebugClientPovider implements IDebugClientPovider<DebugJ2DBTestClient>
{

	public DebugClientPovider()
	{
	}

	@Override
	public DebugJ2DBTestClient createDebugClient(final DebugClientHandler debugClientHandler)
	{
		return new DebugJ2DBTestClient(debugClientHandler);
	}

	@Override
	public boolean isSwingClient()
	{
		return true;
	}

}
