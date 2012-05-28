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

package com.servoy.eclipse.ui.wizards.extension;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.eclipse.swt.widgets.Display;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.extension.FileBasedExtensionProvider;
import com.servoy.extension.IExtensionProvider;
import com.servoy.extension.IFileBasedExtensionProvider;
import com.servoy.extension.dependency.DependencyPath;
import com.servoy.extension.parser.EXPParser;
import com.servoy.extension.parser.EXPParserPool;
import com.servoy.extension.parser.IEXPParserPool;
import com.servoy.j2db.util.Utils;

/**
 * This is the the part of the install wizard state that is needed after restart (so it needs to be persisted/restored).
 * Only this state should be enough for the actual install operations to use
 * @author acostescu
 */
public class RestartState implements IEXPParserPool
{
	protected final static String PERSISTED_STATE_FILENAME = "pending.pgo"; //$NON-NLS-1$

	public File installDir = null; // this one does not need to be persisted; can be recreated
	public IExtensionProvider extensionProvider; // after restart (if needed) it's recreated based on ".pending/.#" folder where # is an int, not persisted
	public IFileBasedExtensionProvider installedExtensionsProvider; // recreated, not persisted

	public DependencyPath chosenPath; // MAIN PERSISTED THING

	public boolean disallowCancel = false; // not persisted but used at actual install
	public Display display;

	public EXPParserPool parserPool = new EXPParserPool(); // not persisted but used at actual install

	public EXPParser getOrCreateParser(File f)
	{
		return parserPool.getOrCreateParser(f);
	}

	/**
	 * Serialises this state to disk (pending install).
	 * @param pendingDir the directory when the state should be persisted.
	 * @return null if all is ok, or an error message if the operation failed.
	 */
	public String storeToPending(File pendingDir)
	{
		String err = null;
		File destinationFile = new File(pendingDir, PERSISTED_STATE_FILENAME);
		ObjectOutputStream out = null;
		try
		{
			out = new ObjectOutputStream(new FileOutputStream(destinationFile));
			out.writeObject(chosenPath);
		}
		catch (IOException e)
		{
			err = "Error persisting pending install: " + e.getMessage(); //$NON-NLS-1$
			ServoyLog.logError(e);
		}
		finally
		{
			Utils.closeOutputStream(out);
		}
		return err;
	}

	/**
	 * De-serialises this state from disk (pending install).
	 * @param pendingDir the directory when the state is persisted.
	 * @return null if all is ok, or an error message if the operation failed.
	 */
	public String recreateFromPending(File pendingDir)
	{
		chosenPath = null;
		extensionProvider = null;

		String err = null;
		File sourceFile = new File(pendingDir, PERSISTED_STATE_FILENAME);
		ObjectInputStream in = null;
		try
		{
			in = new ObjectInputStream(new FileInputStream(sourceFile));
			chosenPath = (DependencyPath)in.readObject();

			// recreate stuff needed to continue install but do not need persisting
			extensionProvider = new FileBasedExtensionProvider(pendingDir, true, this);
			installedExtensionsProvider.flushCache(); // after last pending dir was processed, the installed extensions dir probably changed
		}
		catch (IOException e)
		{
			err = "Error preparing pending install: " + e.getMessage(); //$NON-NLS-1$
			ServoyLog.logError(e);
		}
		catch (ClassNotFoundException e)
		{
			err = "Error preparing pending install: " + e.getMessage(); //$NON-NLS-1$
			ServoyLog.logError(e);
		}
		finally
		{
			Utils.closeInputStream(in);
		}
		return err;
	}

}