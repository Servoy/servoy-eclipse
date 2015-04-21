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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sablo.specification.WebComponentPackageSpecification;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebLayoutSpecification;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.designer.editor.BaseRestorableCommand;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.designer.util.DesignerUtil;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IDeveloperRepository;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ISupportFormElements;
import com.servoy.j2db.persistence.LayoutContainer;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.util.Debug;


public class AddContainerCommand extends AbstractHandler implements IHandler
{
	public static final String COMMAND_ID = "com.servoy.eclipse.designer.rfb.add";

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException
	{
		try
		{
			BaseVisualFormEditor activeEditor = DesignerUtil.getActiveEditor();
			if (activeEditor != null)
			{
				activeEditor.getCommandStack().execute(new BaseRestorableCommand("createLayoutContainer")
				{
					private LayoutContainer newContainer;

					@Override
					public void execute()
					{
						try
						{
							newContainer = addLayoutComponent(DesignerUtil.getContentOutlineSelection(),
								event.getParameter("com.servoy.eclipse.designer.editor.rfb.menu.add.spec"),
								event.getParameter("com.servoy.eclipse.designer.editor.rfb.menu.add.package"),
								new JSONObject(event.getParameter("com.servoy.eclipse.designer.editor.rfb.menu.add.config")),
								computeNextIndex(DesignerUtil.getContentOutlineSelection()));
							if (newContainer != null)
							{
								ServoyModelManager.getServoyModelManager().getServoyModel().firePersistsChanged(false,
									Arrays.asList(new IPersist[] { newContainer }));
								Object[] selection = new Object[] { newContainer };
								IStructuredSelection structuredSelection = new StructuredSelection(selection);
								DesignerUtil.getContentOutline().setSelection(structuredSelection);
							}
						}
						catch (Exception ex)
						{
							Debug.error(ex);
						}
					}

					@Override
					public void undo()
					{
						try
						{
							if (newContainer != null)
							{
								((IDeveloperRepository)newContainer.getRootObject().getRepository()).deleteObject(newContainer);
								ServoyModelManager.getServoyModelManager().getServoyModel().firePersistsChanged(false,
									Arrays.asList(new IPersist[] { newContainer }));
							}
						}
						catch (RepositoryException e)
						{
							ServoyLog.logError("Could not undo create layout container", e);
						}
					}
				});
			}
		}
		catch (NullPointerException npe)
		{
			Debug.log(npe);
		}
		return null;
	}

	private LayoutContainer addLayoutComponent(Object parent, String specName, String packageName, JSONObject configJson, int index)
	{
		LayoutContainer container;
		try
		{
			if (parent instanceof AbstractBase && parent instanceof ISupportChilds)
			{
				WebComponentPackageSpecification<WebLayoutSpecification> specifications = WebComponentSpecProvider.getInstance().getLayoutSpecifications().get(
					packageName);
				container = (LayoutContainer)((AbstractBase)parent).getRootObject().getChangeHandler().createNewObject(((ISupportChilds)parent),
					IRepository.LAYOUTCONTAINERS);
				container.setSpecName(specName);
				container.setPackageName(packageName);
				((AbstractBase)parent).addChild(container);
				container.setLocation(new Point(index, index));
				if (configJson != null)
				{
					Iterator keys = configJson.keys();
					while (keys.hasNext())
					{
						String key = (String)keys.next();
						Object value = configJson.get(key);
						if ("children".equals(key))
						{
							// special key to create children instead of a attribute set.
							JSONArray array = (JSONArray)value;
							for (int i = 0; i < array.length(); i++)
							{
								JSONObject jsonObject = array.getJSONObject(i);
								WebLayoutSpecification spec = specifications.getSpecification(jsonObject.getString("layoutName"));
								addLayoutComponent(container, spec.getName(), packageName, jsonObject.optJSONObject("model"), i + 1);
							}
						} // children and layoutName are special
						else if (!"layoutName".equals(key)) container.putAttribute(key, value.toString());
					}
					return container;
				}
			}
		}
		catch (RepositoryException e)
		{
			Debug.log(e);
		}
		catch (JSONException e)
		{
			Debug.log(e);
		}
		return null;
	}

	private int computeNextIndex(Object parent)
	{
		int i = 1;
		if (parent instanceof ISupportFormElements)
		{
			Iterator<IPersist> allObjects = ((ISupportFormElements)parent).getAllObjects();

			while (allObjects.hasNext())
			{
				allObjects.next();
				i++;
			}
			return i;
		}
		else if (parent instanceof Form)
		{
			Form form = (Form)parent;
			i = form.getAllObjectsAsList().size() + 1;
		}
		return i;
	}
}
