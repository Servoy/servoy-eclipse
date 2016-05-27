package com.servoy.eclipse.designer.webpackage.open;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.designer.webpackage.WebPackageManager;
import com.servoy.j2db.util.Debug;

public class OpenWebPackageManagerCommandHandler extends AbstractHandler implements IHandler
{

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException
	{
		try
		{
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(new IEditorInput()
			{

				@Override
				public <T> T getAdapter(Class<T> adapter)
				{
					return null;
				}

				@Override
				public String getToolTipText()
				{
					return "Web Package Manager";
				}

				@Override
				public IPersistableElement getPersistable()
				{
					return null;
				}

				@Override
				public String getName()
				{
					return "Web Package Manager";
				}

				@Override
				public ImageDescriptor getImageDescriptor()
				{
					return null;
				}

				@Override
				public boolean exists()
				{
					return true;
				}

				@Override
				public boolean equals(Object obj)
				{
					if (obj instanceof IEditorInput) return ((IEditorInput)obj).getName().equals(this.getName());
					return super.equals(obj);
				}
			}, WebPackageManager.EDITOR_ID);
		}
		catch (PartInitException e)
		{
			Debug.log(e);
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.core.commands.AbstractHandler#isEnabled()
	 */
	@Override
	public boolean isEnabled()
	{
		return true;
	}

}
