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
package com.servoy.eclipse.debug.script;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.core.IMember;
import org.eclipse.dltk.core.IModelElement;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.core.ModelException;
import org.eclipse.dltk.internal.ui.editor.EditorUtility;
import org.eclipse.dltk.javascript.typeinfo.IRProperty;
import org.eclipse.dltk.javascript.typeinfo.model.Element;
import org.eclipse.dltk.javascript.typeinfo.model.Method;
import org.eclipse.dltk.ui.IOpenDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ITable;

/**
 * Extension point that takes care of opening editors for {@link Element} objects.
 * It tries to load the editor based on the {@link TypeCreator#RESOURCE} attribute that is set.
 *
 * @author jcompagner
 * @since 6.0
 */
public class OpenAdapter implements IOpenDelegate
{

	public boolean supports(Object object)
	{
		return object instanceof Element element &&
			(element.getAttribute(TypeCreator.RESOURCE) != null || element.getAttribute(TypeCreator.LAZY_VALUECOLLECTION) != null) ||
			object instanceof IRProperty prop && prop.getSource() != null &&
				(prop.getSource().getAttribute(TypeCreator.RESOURCE) != null || prop.getSource().getAttribute(TypeCreator.LAZY_VALUECOLLECTION) != null);
	}

	public String getName(Object object)
	{
		if (object instanceof Element element) return element.getName();
		if (object instanceof IRProperty property) return property.getName();
		return null;
	}

	public IEditorPart openInEditor(Object object, boolean activate) throws PartInitException, CoreException
	{
		Element element = object instanceof IRProperty prop ? prop.getSource() : (Element)object;
		Object resource = element.getAttribute(TypeCreator.RESOURCE);
		if (resource == null)
		{
			resource = element.getAttribute(TypeCreator.LAZY_VALUECOLLECTION);
		}
		if (resource instanceof IPersist)
		{
			EditorUtil.openPersistEditor((IPersist)resource);
		}
		else if (resource instanceof IDataProvider)
		{
			EditorUtil.openDataProviderEditor((IDataProvider)resource);
		}
		else if (resource instanceof IFile)
		{
			IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			if (window != null)
			{
				IWorkbenchPage p = window.getActivePage();
				if (p != null)
				{
					try
					{
						IDE.openEditor(p, (IFile)resource, true);
					}
					catch (PartInitException e)
					{
						ServoyLog.logError(e);
					}
				}
			}
		}
		else if (resource instanceof String)
		{
			IFile sourceFile = null;

			IPath filePath = Path.fromPortableString(((String)resource).replace('\\', '/'));
			if (filePath.isAbsolute())
			{
				sourceFile = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(filePath);
			}
			else
			{
				sourceFile = ResourcesPlugin.getWorkspace().getRoot().getFile(filePath);
			}

			ISourceModule sourceFileModule = DLTKCore.createSourceModuleFrom(sourceFile);
			try
			{
				IModelElement[] children = sourceFileModule.getChildren();
				String name = ((Element)object).getName();
				for (IModelElement child : children)
				{
					if (child instanceof IMember)
					{
						if (name.equals(child.getElementName()))
						{
							// if it is an exact match break
							// method == function reference
							// field != function reference
							// else try the next.
							if ((child.getElementType() == IModelElement.METHOD && object instanceof Method) ||
								(child.getElementType() == IModelElement.FIELD && !(object instanceof Method)))
							{
								IEditorPart editorPart = EditorUtility.openInEditor(child, activate);
								EditorUtility.revealInEditor(editorPart, child);
								return editorPart;
							}
						}
					}
				}
			}
			catch (ModelException e)
			{
				ServoyLog.logError(e);
			}
		}
		else if (resource instanceof ITable)
		{
			EditorUtil.openTableEditor((ITable)resource);
		}
		else if (resource instanceof TableConfig)
		{
			EditorUtil.openTableEditor(((TableConfig)resource).getTable());
		}
		return null;
	}
}
