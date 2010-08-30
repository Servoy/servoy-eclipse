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
import org.eclipse.dltk.javascript.typeinfo.model.Element;
import org.eclipse.dltk.javascript.typeinfo.model.Method;
import org.eclipse.dltk.ui.IOpenDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IPersist;

public class OpenAdapter implements IOpenDelegate
{

	public boolean supports(Object object)
	{
		return object instanceof Element && ((Element)object).getAttribute(TypeCreator.RESOURCE) != null;
	}

	public String getName(Object object)
	{
		if (object instanceof Element) return ((Element)object).getName();
		return null;
	}

	public IEditorPart openInEditor(Object object, boolean activate) throws PartInitException, CoreException
	{
		Object resource = ((Element)object).getAttribute(TypeCreator.RESOURCE);
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
		return null;
	}

}
