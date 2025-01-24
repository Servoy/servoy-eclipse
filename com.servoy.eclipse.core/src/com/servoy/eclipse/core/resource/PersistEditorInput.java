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
package com.servoy.eclipse.core.resource;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.model.repository.SolutionDeserializer;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.util.UUID;

/**
 * Adapter for making a solution resource a suitable input for an editor.
 * </p>
 */
public class PersistEditorInput implements IEditorInput
{
	public static final String FORM_RESOURCE_ID = "com.servoy.eclipse.core.resource.form";
	public static final String VALUELIST_RESOURCE_ID = "com.servoy.eclipse.core.resource.valuelist";
	public static final String RELATION_RESOURCE_ID = "com.servoy.eclipse.core.resource.relation";
	public static final String MEDIA_RESOURCE_ID = "com.servoy.eclipse.core.resource.media";
	public static final String MENU_RESOURCE_ID = "com.servoy.eclipse.core.resource.menu";

	private IFile file;
	private final String name;
	private final UUID uuid;
	private final String solutionName;
	private boolean isNew = false; // runtime setting, should not be persisted.

	/**
	 * Creates a persist input.
	 *
	 * @param solution
	 */
	public PersistEditorInput(String name, String solutionName, UUID uuid)
	{
		this.name = name;
		this.uuid = uuid;
		this.solutionName = solutionName;
	}

	public static PersistEditorInput createFormEditorInput(IFile file)
	{
		IPersist filePersist = SolutionDeserializer.findPersistFromFile(file);
		if (filePersist != null)
		{
			Form form = (Form)filePersist.getAncestor(IRepository.FORMS);
			if (form != null)
			{
				return createFormEditorInput(form, file);
			}
		}

		return null;
	}

	public static PersistEditorInput createFormEditorInput(Form form)
	{
		return createFormEditorInput(form, ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(SolutionSerializer.getRelativeFilePath(form, true))));
	}

	private static PersistEditorInput createFormEditorInput(Form form, IFile file)
	{
		PersistEditorInput input = new PersistEditorInput(form.getName(), form.getSolution().getName(), form.getUUID());
		input.setFile(file);
		return input;
	}

	/**
	 * @return the file
	 */
	public IFile getFile()
	{
		return file;
	}

	/**
	 * @param file the file to set
	 */
	public void setFile(IFile file)
	{
		this.file = file;
	}

	/**
	 * @param isNew the isNew to set
	 */
	public PersistEditorInput setNew(boolean isNew)
	{
		this.isNew = isNew;
		return this;
	}

	/**
	 * @return the isNew
	 */
	public boolean isNew()
	{
		return isNew;
	}

	/*
	 * (non-Javadoc) Method declared on IEditorInput.
	 */
	public boolean exists()
	{
		return true;
	}

	/*
	 * (non-Javadoc) Method declared on IAdaptable.
	 */
	@Override
	public <T> T getAdapter(Class<T> adapter)
	{
		return Platform.getAdapterManager().getAdapter(this, adapter);
	}

	/*
	 * (non-Javadoc) Method declared on IEditorInput.
	 */
	public ImageDescriptor getImageDescriptor()
	{
		return PlatformUI.getWorkbench().getEditorRegistry().getImageDescriptor(null);
	}

	/*
	 * (non-Javadoc) Method declared on IEditorInput.
	 */
	public String getName()
	{
		return name;
	}


	public UUID getUuid()
	{
		return uuid;
	}

	public String getSolutionName()
	{
		return solutionName;
	}

	/**
	 * @return the designPagetype
	 */
	public DesignPagetype getDesignPagetype()
	{
		if (file != null)
		{
			try
			{
				return DesignPagetype.safeValueOf(file.getPersistentProperty(Activator.DESIGN_PAGE_TYPE_KEY));
			}
			catch (CoreException e)
			{
				ServoyLog.logError(e);
			}
		}
		return null;
	}

	/**
	* @param designPagetype the designPagetype to set
	*/
	public void setDesignPagetype(DesignPagetype designPagetype)
	{
		if (file != null)
		{
			try
			{
				file.setPersistentProperty(Activator.DESIGN_PAGE_TYPE_KEY, designPagetype == null ? null : designPagetype.name());
			}
			catch (CoreException e)
			{
				ServoyLog.logError(e);
			}
		}
	}

	/*
	 * (non-Javadoc) Method declared on IEditorInput.
	 */
	public String getToolTipText()
	{
		return solutionName + '.' + name;
	}

	/*
	 * (non-Javadoc) Method declared on IEditorInput.
	 */
	public IPersistableElement getPersistable()
	{
		return getAdapter(IPersistableElement.class);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return getClass().getName() + "(" + getToolTipText() + ")";
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		final PersistEditorInput other = (PersistEditorInput)obj;
		if (uuid == null)
		{
			if (other.uuid != null) return false;
		}
		else if (!uuid.equals(other.uuid)) return false;
		return true;
	}


}
