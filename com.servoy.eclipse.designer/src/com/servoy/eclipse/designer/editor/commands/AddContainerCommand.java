package com.servoy.eclipse.designer.editor.commands;

import java.awt.Point;
import java.util.Arrays;
import java.util.Iterator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.contentoutline.ContentOutline;
import org.json.JSONException;
import org.json.JSONObject;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportFormElements;
import com.servoy.j2db.persistence.LayoutContainer;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.util.Debug;


public class AddContainerCommand extends AbstractHandler implements IHandler
{
	public static final String COMMAND_ID = "com.servoy.eclipse.designer.rfb.add";

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException
	{
		addLayoutComponent(getSelection(), event.getParameter("com.servoy.eclipse.designer.editor.rfb.menu.add.spec"),
			event.getParameter("com.servoy.eclipse.designer.editor.rfb.menu.add.package"),
			event.getParameter("com.servoy.eclipse.designer.editor.rfb.menu.add.config"), computeNextIndex(getSelection()));
		return null;
	}

	private void addLayoutComponent(ISupportFormElements parent, String specName, String packageName, String config, int index)
	{
		LayoutContainer container;
		try
		{
			container = (LayoutContainer)parent.getRootObject().getChangeHandler().createNewObject(parent, IRepository.LAYOUTCONTAINERS);
			container.setSpecName(specName);
			container.setPackageName(packageName);
			parent.addChild(container);
			container.setLocation(new Point(index, index));
			JSONObject configJson = new JSONObject(config);
			Iterator keys = configJson.keys();
			while (keys.hasNext())
			{
				String key = (String)keys.next();
				Object value = configJson.get(key);
				if (!"layoutName".equals(key)) container.putAttribute(key, value.toString());
			}
			ServoyModelManager.getServoyModelManager().getServoyModel().firePersistsChanged(false, Arrays.asList(new IPersist[] { container }));
			Object[] selection = new Object[] { container };
			IStructuredSelection structuredSelection = new StructuredSelection(selection);
			getContentOutline().setSelection(structuredSelection);
		}
		catch (RepositoryException e)
		{
			Debug.log(e);
		}
		catch (JSONException e)
		{
			Debug.log(e);
		}

	}

	private int computeNextIndex(ISupportFormElements parent)
	{
		int i = 0;
		Iterator<IPersist> allObjects = parent.getAllObjects();

		while (allObjects.hasNext())
		{
			allObjects.next();
			i++;
		}
		return i;
	}

	/**
	 * @return
	 */
	private ContentOutline getContentOutline()
	{
		IWorkbenchWindow active = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (active == null) return null;
		IWorkbenchPage page = active.getActivePage();
		if (page == null) return null;
		IWorkbenchPart part = page.getActivePart();
		if (part instanceof ContentOutline)
		{
			((ContentOutline)part).getViewSite().getShell().redraw();
			return (ContentOutline)part;
		}
		return null;
	}

	private ISupportFormElements getSelection()
	{
		Object firstElement = ((IStructuredSelection)getContentOutline().getSelection()).getFirstElement();
		if (firstElement instanceof PersistContext)
		{
			PersistContext persistContext = (PersistContext)firstElement;
			if (persistContext.getPersist() instanceof ISupportFormElements) return (ISupportFormElements)persistContext.getPersist();
		}
		return null;
	}


}
