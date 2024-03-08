package com.servoy.eclipse.designer.editor.rfb.actions.handlers;

import java.awt.Dimension;
import java.awt.Point;

import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.designer.editor.BaseRestorableCommand;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.ui.property.IRAGTEST;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportBounds;

public final class SetPropertyCommand extends BaseRestorableCommand
{
	private final Object value;
	private final IRAGTEST source;
	private final String propertyName;

	/**
	 * @param label
	 * @param newLocation
	 * @param persist
	 */
	SetPropertyCommand(String label, IRAGTEST source, String propertyName, Object value)
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

	@Override
	public void undo()
	{
		super.undo();

		if (source.getPersist() instanceof ISupportBounds && PlatformUI.getWorkbench().getActiveWorkbenchWindow() != null &&
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage() != null &&
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor() instanceof BaseVisualFormEditor)
		{
			IPersist p = source.getPersist();
			Form form = (Form)p.getAncestor(IRepository.FORMS);
			Point location = ((ISupportBounds)p).getLocation();
			Dimension size = ((ISupportBounds)p).getSize();
			if (location.x + size.getWidth() > form.getWidth() || location.y + size.getHeight() > form.getSize().getHeight())
			{
				BaseVisualFormEditor editor = (BaseVisualFormEditor)PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
				editor.setRenderGhosts(true);
			}
		}
	}


}