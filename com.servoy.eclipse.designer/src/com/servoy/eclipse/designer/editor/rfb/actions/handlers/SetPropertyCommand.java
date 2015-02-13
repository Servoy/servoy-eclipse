package com.servoy.eclipse.designer.editor.rfb.actions.handlers;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.designer.editor.BaseRestorableCommand;
import com.servoy.eclipse.ui.property.PersistPropertySource;

public final class SetPropertyCommand extends BaseRestorableCommand
{
	private final Object value;
	private final PersistPropertySource source;
	private final String propertyName;

	/**
	 * @param label
	 * @param newLocation
	 * @param persist
	 */
	SetPropertyCommand(String label, PersistPropertySource source, String propertyName, Object value)
	{
		super(label);
		this.source = source;
		this.propertyName = propertyName;
		this.value = value;
	}

	@Override
	public void execute()
	{
		setPropertyValue(source, propertyName, value);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.gef.commands.Command#redo()
	 */
	@Override
	public void redo()
	{
		super.redo();
		ServoyModelManager.getServoyModelManager().getServoyModel().firePersistChanged(false, source.getPersist(), true);
	}
}