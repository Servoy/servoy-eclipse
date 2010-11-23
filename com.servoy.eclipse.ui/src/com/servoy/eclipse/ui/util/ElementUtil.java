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
package com.servoy.eclipse.ui.util;

import java.util.Iterator;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.AbstractRepository;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.Bean;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.RectShape;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.ScriptCalculation;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.ValueList;

/**
 * Utilities for elements and label providers.
 * 
 * @author rgansevles
 */

public class ElementUtil
{
	public static String getPersistImageName(IPersist persist)
	{
		if (persist instanceof BaseComponent)
		{
			String lookupName = ComponentFactory.getLookupName((BaseComponent)persist);
			if ("button".equals(lookupName))
			{
				return "button.gif";
			}
			if ("check".equals(lookupName))
			{
				return "chk_on_icon.gif";
			}
			if ("combobox".equals(lookupName))
			{
				return "field.gif"; // todo: combobox.gif
			}
			if ("field".equals(lookupName))
			{
				return "field.gif";
			}
			if ("portal".equals(lookupName))
			{
				return "portal.gif";
			}
			if ("tabpanel".equals(lookupName))
			{
				return "tabs.gif";
			}
			if ("label".equals(lookupName))
			{
				return "text.gif";
			}
			if ("radio".equals(lookupName))
			{
				return "radio_on.gif";
			}
		}
		if (persist instanceof Form)
		{
			return "designer.gif";
		}
		if (persist instanceof Bean)
		{
			return "bean.gif";
		}
		if (persist instanceof RectShape)
		{
			return "rectangle.gif";
		}
		if (persist instanceof Relation)
		{
			return "relation.gif";
		}
		if (persist instanceof Media)
		{
			return "image.gif";
		}
		if (persist instanceof ValueList)
		{
			return "valuelists.gif";
		}
		if (persist instanceof ScriptCalculation)
		{
			return "columncalc.gif";
		}
		if (persist instanceof ScriptVariable)
		{
			if (persist.getParent() instanceof Form)
			{
				return "form_variable.gif";
			}
			return "global_variable.gif";
		}
		if (persist instanceof ScriptMethod)
		{
			if (persist.getParent() instanceof Form)
			{
				return "form_method.gif";
			}
			return "global_method.gif";
		}

		return null;
	}

	public static boolean isInheritedFormElement(IPersist context, Object element)
	{
		if (element instanceof Form)
		{
			return false;
		}
		if (context instanceof Form && element instanceof IPersist && (((IPersist)element).getAncestor(IRepository.FORMS) != context))
		{
			if (element instanceof IPersist && (((IPersist)element).getAncestor(IRepository.FORMS) != context))
			{
				// child of super-form, readonly
				return true;
			}
		}
		if (element instanceof FormElementGroup)
		{
			Iterator<IFormElement> elements = ((FormElementGroup)element).getElements();
			while (elements.hasNext())
			{
				if (isInheritedFormElement(context, elements.next()))
				{
					return true;
				}
			}
		}
		if (element instanceof AbstractBase)
		{
			return ((AbstractBase)element).isOverrideElement();
		}
		// child of this form, not of a inherited form
		return false;
	}

	public static IPersist getOverridePersist(IPersist context, IPersist persist)
	{
		if (persist instanceof AbstractBase && context instanceof Form && persist.getAncestor(IRepository.FORMS) != context)
		{
			IPersist newPersist = AbstractRepository.searchPersist((Form)context, persist.getUUID());
			if (newPersist == null)
			{
				ISupportChilds parent = (Form)context;
				if (!(persist.getParent() instanceof Form))
				{
					parent = null;
					parent = (ISupportChilds)AbstractRepository.searchPersist((Form)context, persist.getParent().getUUID());
					if (parent == null)
					{
						try
						{
							parent = (ISupportChilds)((AbstractBase)persist.getParent()).cloneObj((Form)context, false, null, false, false);
							((AbstractBase)parent).resetUUID(persist.getParent().getUUID());
							((AbstractBase)parent).copyPropertiesMap(null);
							((AbstractBase)parent).putOverrideProperty(((Form)persist.getAncestor(IRepository.FORMS)).getName());
						}
						catch (Exception ex)
						{
							ServoyLog.logError(ex);
						}
					}
				}
				try
				{
					newPersist = ((AbstractBase)persist).cloneObj(parent, false, null, false, false);
					((AbstractBase)newPersist).resetUUID(persist.getUUID());
					((AbstractBase)newPersist).copyPropertiesMap(null);
					((AbstractBase)newPersist).putOverrideProperty(((Form)persist.getAncestor(IRepository.FORMS)).getName());
				}
				catch (Exception ex)
				{
					ServoyLog.logError(ex);
				}
			}
			return newPersist;
		}
		return persist;
	}
}
