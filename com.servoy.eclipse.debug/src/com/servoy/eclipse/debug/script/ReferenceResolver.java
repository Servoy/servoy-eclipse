package com.servoy.eclipse.debug.script;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.core.IForeignElement;
import org.eclipse.dltk.core.IMember;
import org.eclipse.dltk.core.IModelElement;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.core.ModelException;
import org.eclipse.dltk.core.model.ForeignElement;
import org.eclipse.dltk.javascript.typeinfo.IElementConverter;
import org.eclipse.dltk.javascript.typeinfo.model.Element;
import org.eclipse.dltk.javascript.typeinfo.model.Method;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IPersist;

public class ReferenceResolver implements IElementConverter
{
	public IModelElement convert(ISourceModule module, Element element)
	{
		Object resource = element.getAttribute(TypeCreator.RESOURCE);
		if (resource instanceof String)
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
				String name = element.getName();
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
							if ((child.getElementType() == IModelElement.METHOD && element instanceof Method) ||
								(child.getElementType() == IModelElement.FIELD && !(element instanceof Method)))
							{
								return child;
							}
						}
					}
				}
			}
			catch (ModelException e)
			{
				return new ServoyEditor(sourceFileModule, resource);
			}
		}
		else if (resource != null)
		{
			return new ServoyEditor(module, resource);
		}
		return null;
	}


	private static class ServoyEditor extends ForeignElement implements IForeignElement
	{
		private final Object ressource;

		protected ServoyEditor(IModelElement parent, Object ressource)
		{
			super(parent);
			this.ressource = ressource;
		}

		public void codeSelect()
		{
			if (ressource instanceof IPersist)
			{
				EditorUtil.openPersistEditor((IPersist)ressource);
			}
			else if (ressource instanceof IDataProvider)
			{
				EditorUtil.openDataProviderEditor((IDataProvider)ressource);
			}
			else if (ressource instanceof IFile)
			{
				IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
				if (window != null)
				{
					IWorkbenchPage p = window.getActivePage();
					if (p != null)
					{
						try
						{
							IDE.openEditor(p, (IFile)ressource, true);
						}
						catch (PartInitException e)
						{
							e.printStackTrace();
						}
					}
				}
			}
		}
	}
}
