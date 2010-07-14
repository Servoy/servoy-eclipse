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
package com.servoy.eclipse.designer.outline;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.designer.property.PersistContext;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptVariable;

/**
 * Content provider for Servoy form in outline view.
 * 
 * @author rgansevles
 */

public class FormOutlineContentProvider implements ITreeContentProvider
{
	public static final Object ELEMENTS = new Object();
	public static final Object PARTS = new Object();

	private final Form form;

	public FormOutlineContentProvider(Form form)
	{
		this.form = form;
	}

	public Object[] getChildren(Object parentElement)
	{
		if (parentElement == ELEMENTS || parentElement == PARTS)
		{
			try
			{
				Form flattenedForm = (Form)getFlattenedWhenForm(form);
				List<Object> nodes = new ArrayList<Object>();
				Set<FormElementGroup> groups = new HashSet<FormElementGroup>();
				if (flattenedForm != null)
				{
					for (IPersist persist : flattenedForm.getAllObjectsAsList())
					{
						if (persist instanceof ScriptVariable || persist instanceof IScriptProvider)
						{
							continue;
						}
						if (parentElement == ELEMENTS && persist instanceof IFormElement && ((IFormElement)persist).getGroupID() != null)
						{
							FormElementGroup group = new FormElementGroup(((IFormElement)persist).getGroupID(), flattenedForm);
							if (groups.add(group))
							{
								nodes.add(group);
							}
							continue;
						}
						if ((parentElement == PARTS) != (persist instanceof Part))
						{
							continue;
						}
						nodes.add(new PersistContext(persist, form));
					}
				}
				return nodes.toArray();
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError("Could not create flattened form for form " + form, e);
			}
		}
		else if (parentElement instanceof PersistContext && ((PersistContext)parentElement).getPersist() instanceof AbstractBase)
		{
			List<PersistContext> list = new ArrayList<PersistContext>();
			for (IPersist persist : ((AbstractBase)((PersistContext)parentElement).getPersist()).getAllObjectsAsList())
			{
				list.add(new PersistContext(persist, ((PersistContext)parentElement).getContext()));
			}
			return list.toArray();
		}
		else if (parentElement instanceof FormElementGroup)
		{
			List<PersistContext> list = new ArrayList<PersistContext>();
			Iterator<IFormElement> elements = ((FormElementGroup)parentElement).getElements();
			while (elements.hasNext())
			{
				IFormElement element = elements.next();
				if (element instanceof IPersist)
				{
					list.add(new PersistContext((IPersist)element, null));
				}
			}
			return list.toArray();
		}
		return null;
	}

	public Object getParent(Object element)
	{
		if (element instanceof PersistContext)
		{
			IPersist persist = ((PersistContext)element).getPersist();
			if (persist != null)
			{
				if (persist instanceof IFormElement && ((IFormElement)persist).getGroupID() != null)
				{
					return new FormElementGroup(((IFormElement)persist).getGroupID(), persist.getParent());
				}

				if (persist.getParent() == form)
				{
					if (persist instanceof Part)
					{
						return PARTS;
					}
					return ELEMENTS;
				}
				// form element of sub element (Tab panel)
				try
				{
					return new PersistContext(getFlattenedWhenForm(persist.getParent()), ((PersistContext)element).getContext());
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError(e);
				}
			}
		}
		return null;
	}

	private static IPersist getFlattenedWhenForm(IPersist persist) throws RepositoryException
	{
		if (persist instanceof Form)
		{
			FlattenedSolution editingFlattenedSolution = ServoyModelManager.getServoyModelManager().getServoyModel().getEditingFlattenedSolution(persist);
			if (editingFlattenedSolution == null)
			{
				ServoyLog.logError("Could not get project for form " + persist, null);
				return null;
			}
			return editingFlattenedSolution.getFlattenedForm(persist);
		}
		return persist;
	}

	public boolean hasChildren(Object element)
	{
		return element == ELEMENTS ||
			element == PARTS ||
			element instanceof FormElementGroup ||
			(element instanceof PersistContext && ((PersistContext)element).getPersist() instanceof AbstractBase && (((AbstractBase)((PersistContext)element).getPersist())).getAllObjects().hasNext());
	}

	public Object[] getElements(Object inputElement)
	{
		return new Object[] { ELEMENTS, PARTS };
	}

	public void dispose()
	{
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
	{
	}
}
