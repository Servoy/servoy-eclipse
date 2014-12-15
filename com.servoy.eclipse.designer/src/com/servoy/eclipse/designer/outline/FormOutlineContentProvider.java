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
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebComponentSpecification;
import org.sablo.specification.property.IPropertyType;

import com.servoy.eclipse.designer.editor.rfb.GhostBean;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.util.ElementUtil;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Bean;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IScriptElement;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.PositionComparator;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.server.ngclient.template.FormTemplateGenerator;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.SortedList;

/**
 * Content provider for Servoy form in outline view.
 *
 * @author rgansevles
 */

public class FormOutlineContentProvider implements ITreeContentProvider
{
	public static final Object ELEMENTS = new Object();
	public static final Object PARTS = new Object();

	protected final Form form;
	private static boolean displayType;

	public FormOutlineContentProvider(Form form)
	{
		this.form = form;
		IEclipsePreferences preferences = new InstanceScope().getNode("com.servoy.eclipse.designer");
		displayType = preferences.getBoolean("OutlineViewMode", false);
	}

	public Object[] getChildren(Object parentElement)
	{
		if (displayType) return getChildrenGrouped(parentElement);
		else return getChildrenNonGrouped(parentElement);
	}

	public Object[] getChildrenNonGrouped(Object parentElement)
	{
		Comparator comparator = form.getLayoutContainers().hasNext() ? PersistContextLocationComparator.INSTANCE : PersistContextNameComparator.INSTANCE;

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
						if (persist instanceof IScriptElement)
						{
							continue;
						}
						if (parentElement == ELEMENTS && persist instanceof IFormElement && ((IFormElement)persist).getGroupID() != null)
						{
							FormElementGroup group = new FormElementGroup(((IFormElement)persist).getGroupID(), ModelUtils.getEditingFlattenedSolution(form),
								form);
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
						nodes.add(PersistContext.create(persist, form));
					}
				}
				return (parentElement == ELEMENTS) ? new SortedList(comparator, nodes).toArray() : nodes.toArray();
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
				list.add(PersistContext.create(persist, ((PersistContext)parentElement).getContext()));
			}
			if (((PersistContext)parentElement).getPersist() instanceof Bean)
			{
				List<IPersist> allGhostElements = getAllGhostElements((Bean)((PersistContext)parentElement).getPersist());
				for (IPersist ghost : allGhostElements)
				{
					list.add(PersistContext.create(ghost, ((PersistContext)parentElement).getContext()));
				}
			}
			return new SortedList(comparator, list).toArray();
		}
		else if (parentElement instanceof FormElementGroup)
		{
			List<PersistContext> list = new ArrayList<PersistContext>();
			Iterator<IFormElement> elements = ((FormElementGroup)parentElement).getElements();
			while (elements.hasNext())
			{
				list.add(PersistContext.create(elements.next(), form));
			}
			return new SortedList(comparator, list).toArray();
		}
		else if (parentElement instanceof Bean)
		{
			return getAllGhostElements((Bean)parentElement).toArray();
		}
		return null;
	}

	public Object[] getChildrenGrouped(Object parentElement)
	{
		Comparator comparator = form.getLayoutContainers().hasNext() ? PersistContextLocationComparator.INSTANCE : PersistContextNameComparator.INSTANCE;
		if (parentElement == ELEMENTS || parentElement == PARTS)
		{
			HashSet<Object> availableCategories = null;
			try
			{
				Form flattenedForm = (Form)getFlattenedWhenForm(form);
				if (flattenedForm != null)
				{
					availableCategories = new HashSet<Object>();
					for (IPersist persist : flattenedForm.getAllObjectsAsList())
					{
						if (persist instanceof IScriptElement)
						{
							continue;
						}
						if (parentElement == ELEMENTS && persist instanceof IFormElement)
						{
							availableCategories.add(ElementUtil.getPersistNameAndImage(persist));
						}
						if ((parentElement == PARTS) && (persist instanceof Part))
						{
							availableCategories.add(PersistContext.create(persist, form));
						}
					}
				}
				return availableCategories.toArray();
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError("Could not create flattened form for form " + form, e);
			}
		}
		else if (parentElement instanceof Pair)
		{
			try
			{
				Form flattenedForm = (Form)getFlattenedWhenForm(form);
				List<Object> nodes = new ArrayList<Object>();
				if (flattenedForm != null)
				{
					for (IPersist persist : flattenedForm.getAllObjectsAsList())
					{
						if (persist instanceof IScriptElement || persist instanceof Part)
						{
							continue;
						}
						if (persist instanceof IFormElement)
						{
							Pair<String, Image> nameAndImage = ElementUtil.getPersistNameAndImage(persist);
							if (nameAndImage.equals(parentElement)) nodes.add(PersistContext.create(persist, form));
						}
					}
				}
				return new SortedList(comparator, nodes).toArray();
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
				list.add(PersistContext.create(persist, ((PersistContext)parentElement).getContext()));
			}
			return list.toArray();
		}
		else if (parentElement instanceof Bean)
		{
			return getAllGhostElements((Bean)parentElement).toArray();
		}

		return null;
	}

	public Object getParent(Object element)
	{
		if (displayType) return getGroupedParent(element);
		else return getParentNonGrouped(element);
	}

	public Object getParentNonGrouped(Object element)
	{
		if (element instanceof PersistContext)
		{
			IPersist persist = ((PersistContext)element).getPersist();
			if (persist != null)
			{
				if (persist instanceof IFormElement && ((IFormElement)persist).getGroupID() != null)
				{
					return new FormElementGroup(((IFormElement)persist).getGroupID(), ModelUtils.getEditingFlattenedSolution(persist),
						(Form)persist.getParent());
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
					return PersistContext.create(getFlattenedWhenForm(persist.getParent()), ((PersistContext)element).getContext());
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError(e);
				}
			}
		}
		return null;
	}

	public Object getGroupedParent(Object element)
	{
		if (element instanceof PersistContext)
		{
			IPersist persist = ((PersistContext)element).getPersist();
			if (persist != null)
			{
				if (persist.getParent() == form)
				{
					if (persist instanceof Part)
					{
						return PARTS;
					}
					else if (persist instanceof IFormElement)
					{
						return ElementUtil.getPersistNameAndImage(persist);
					}
				}
				// form element of sub element (Tab panel)
				try
				{
					return PersistContext.create(getFlattenedWhenForm(persist.getParent()), ((PersistContext)element).getContext());
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError(e);
				}
			}
		}
		else if (element instanceof Pair) return ELEMENTS;
		return null;
	}

	protected static IPersist getFlattenedWhenForm(IPersist persist) throws RepositoryException
	{
		if (persist instanceof Form)
		{
			FlattenedSolution editingFlattenedSolution = ModelUtils.getEditingFlattenedSolution(persist);
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
			element instanceof Pair ||
			element instanceof FormElementGroup ||
			(element instanceof PersistContext && ((PersistContext)element).getPersist() instanceof AbstractBase &&
				(((AbstractBase)((PersistContext)element).getPersist())).getAllObjects().hasNext() || (((PersistContext)element).getPersist() instanceof Bean &&
				!(((PersistContext)element).getPersist() instanceof GhostBean) && ((Bean)((PersistContext)element).getPersist()).getBeanXML() != null));
	}

	public Object[] getElements(Object inputElement)
	{
		return form.getLayoutContainers().hasNext() ? new Object[] { ELEMENTS } : new Object[] { ELEMENTS, PARTS };
	}

	public void dispose()
	{
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
	{
	}

	private List<IPersist> getAllGhostElements(Bean parentBean)
	{
		List<IPersist> result = new ArrayList<IPersist>();
		if (FormTemplateGenerator.isWebcomponentBean(parentBean))
		{
			String beanXML = parentBean.getBeanXML();
			if (beanXML != null)
			{
				WebComponentSpecification webComponentSpecification = WebComponentSpecProvider.getInstance().getWebComponentSpecification(
					parentBean.getBeanClassName());
				if (webComponentSpecification != null)
				{
					Map<String, IPropertyType< ? >> foundTypes = webComponentSpecification.getFoundTypes();
					try
					{
						JSONObject beanJSON = new JSONObject(beanXML);
						for (String beanJSONKey : JSONObject.getNames(beanJSON))
						{
							Object object = beanJSON.get(beanJSONKey);
							if (object != null)
							{
								IPropertyType< ? > type = webComponentSpecification.getProperty(beanJSONKey).getType();
								String simpleTypeName = type.getName().replaceFirst(webComponentSpecification.getName() + ".", "");
								if (foundTypes.containsKey(simpleTypeName))
								{
									WebComponentSpecification spec = WebComponentSpecProvider.getInstance().getWebComponentSpecification(
										parentBean.getBeanClassName());
									boolean arrayReturnType = spec.isArrayReturnType(beanJSONKey);
									if (!arrayReturnType)
									{
										GhostBean ghostBean = new GhostBean(parentBean, beanJSONKey, simpleTypeName, -1, arrayReturnType, false);
										ghostBean.setBeanClassName(simpleTypeName);
										result.add(ghostBean);
									}
									else if (object instanceof JSONArray) for (int i = 0; i < ((JSONArray)object).length(); i++)
									{
										GhostBean ghostBean = new GhostBean(parentBean, beanJSONKey, simpleTypeName, i, arrayReturnType, false);
										ghostBean.setBeanClassName(simpleTypeName);
										result.add(ghostBean);
									}
								}
							}
						}
					}
					catch (JSONException e)
					{
						Debug.error(e);
					}
				}
				else
				{
					Debug.error("no webcomponent specification found for: " + parentBean.getBeanClassName());
				}
			}
		}
		return result;
	}

	public static class PersistContextNameComparator implements Comparator<Object>
	{
		public static final PersistContextNameComparator INSTANCE = new PersistContextNameComparator();

		private PersistContextNameComparator()
		{
		}

		/**
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		public int compare(Object o1, Object o2)
		{
			if (!(o1 instanceof PersistContext) || !(o2 instanceof PersistContext)) return 0;
			String name1 = null;
			if (((PersistContext)o1).getPersist() instanceof ISupportName) name1 = ((ISupportName)((PersistContext)o1).getPersist()).getName();
			String name2 = null;
			if (((PersistContext)o2).getPersist() instanceof ISupportName) name2 = ((ISupportName)((PersistContext)o2).getPersist()).getName();
			if (name1 == null && name2 == null) return 0;
			if (name1 == null) return 1;
			if (name2 == null) return -1;
			return name1.compareToIgnoreCase(name2);
		}
	}

	public static class PersistContextLocationComparator implements Comparator<Object>
	{
		public static final PersistContextLocationComparator INSTANCE = new PersistContextLocationComparator();

		private PersistContextLocationComparator()
		{
		}

		/**
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		public int compare(Object o1, Object o2)
		{
			if (!(o1 instanceof PersistContext) || !(o2 instanceof PersistContext)) return 0;

			return PositionComparator.XY_PERSIST_COMPARATOR.compare(((PersistContext)o1).getPersist(), ((PersistContext)o2).getPersist());
		}
	}

	public static void setDisplayType(boolean grouped)
	{
		displayType = grouped;
	}

	public static boolean getDisplayType()
	{
		return displayType;
	}


}
