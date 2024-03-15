/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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
package com.servoy.eclipse.designer.editor.commands;

import java.util.function.Consumer;

import org.eclipse.gef.commands.Command;

/**
 * Execute a command and and add refresh()
 *
 * @author rgansevles
 *
 */
public class RefreshingCommand<T extends Command> extends Command implements ICommandWrapper<T>
{
	private final T command;
	private final Consumer<Boolean> refresher;

	public RefreshingCommand(T command)
	{
		this(command, (Consumer<Boolean>)null);
	}

	public RefreshingCommand(T command, Runnable refresher)
	{
		this(command, (haveExecuted) -> refresher.run());
	}

	public RefreshingCommand(T command, Consumer<Boolean> refresher)
	{
		this.command = command;
		this.refresher = refresher;
		if (command != null)
		{
			setLabel(command.getLabel());
		}
	}

	@Override
	public void execute()
	{
		if (command != null)
		{
			command.execute();
			setLabel(command.getLabel());
			refresh(true);
		}
	}

	@Override
	public boolean canExecute()
	{
		return command != null && command.canExecute();
	}

	@Override
	public boolean canUndo()
	{
		return command != null && command.canUndo();
	}

	@Override
	public void redo()
	{
		if (command != null)
		{
			command.redo();
			setLabel(command.getLabel());
			refresh(true);
		}
	}

	@Override
	public void undo()
	{
		if (command != null)
		{
			command.undo();
			setLabel(command.getLabel());
			refresh(false);
		}
	}

	public T getCommand()
	{
		return command;
	}

	/**
	 * Refresh your views
	 *
	 * @param haveExecuted true after execute/redo, false after undo
	 */
	public void refresh(boolean haveExecuted)
	{
		if (refresher != null)
		{
			refresher.accept(haveExecuted);
		}
	}
}
