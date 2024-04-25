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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

import com.servoy.j2db.BasicFormController.JSForm;
import com.servoy.j2db.FormManager.HistoryProvider;
import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.dataprocessing.JSDatabaseManager;
import com.servoy.j2db.dataprocessing.datasource.JSDataSources;
import com.servoy.j2db.dataprocessing.datasource.JSViewDataSource;
import com.servoy.j2db.scripting.JSApplication;
import com.servoy.j2db.scripting.JSClientUtils;
import com.servoy.j2db.scripting.JSI18N;
import com.servoy.j2db.scripting.JSSecurity;
import com.servoy.j2db.scripting.JSUnitAssertFunctions;
import com.servoy.j2db.scripting.JSUtils;
import com.servoy.j2db.scripting.solutionmodel.JSSolutionModel;
import com.servoy.j2db.util.ServoyException;

/**
 * Maps Class<?>es to icons, so that the same icon is uniformly used for a certain class in all places.
 *
 * @author gerzse
 */
public class IconProvider
{
	private static IconProvider theInstance;

	public static IconProvider instance()
	{
		if (theInstance == null) theInstance = new IconProvider();
		return theInstance;
	}

	private final Map<Class< ? >, String> c2i;
	private final com.servoy.eclipse.ui.Activator uiActivator = com.servoy.eclipse.ui.Activator.getDefault();

	private IconProvider()
	{
		c2i = new HashMap<Class< ? >, String>();
		c2i.put(com.servoy.j2db.documentation.scripting.docs.JSLib.class, "jslibfolder.png");
		c2i.put(com.servoy.j2db.documentation.scripting.docs.Array.class, "jslibarray.png");
		c2i.put(com.servoy.j2db.documentation.scripting.docs.Object.class, "object.png");
		c2i.put(com.servoy.j2db.documentation.scripting.docs.Date.class, "day_obj.png");
		c2i.put(com.servoy.j2db.documentation.scripting.docs.String.class, "jslibstring.png");
		c2i.put(com.servoy.j2db.documentation.scripting.docs.Math.class, "sum.png");
		c2i.put(com.servoy.j2db.documentation.scripting.docs.JSON.class, "json.png");
		c2i.put(com.servoy.j2db.documentation.scripting.docs.Statements.class, "statements.png");
		c2i.put(com.servoy.j2db.documentation.scripting.docs.SpecialOperators.class, "special_operators.png");
		c2i.put(com.servoy.j2db.documentation.scripting.docs.XML.class, "xml.png");
		c2i.put(com.servoy.j2db.documentation.scripting.docs.XMLList.class, "xml-list.png");
		c2i.put(com.servoy.j2db.documentation.scripting.docs.RegExp.class, "reg_exp.png");
		c2i.put(com.servoy.j2db.documentation.scripting.docs.Number.class, "number.png");

		c2i.put(JSApplication.class, "application.png");
		c2i.put(JSDatabaseManager.class, "database_manager.png");
		c2i.put(JSDataSources.class, "datasources.png");
		c2i.put(JSViewDataSource.class, "viewfs.png");
		c2i.put(JSUtils.class, "utils.png");
		c2i.put(JSClientUtils.class, "clientutils.png");
		c2i.put(HistoryProvider.class, "history.png");
		c2i.put(JSSecurity.class, "security.png");
		c2i.put(JSI18N.class, "i18n.png");
		c2i.put(JSSolutionModel.class, "blueprint.png");
		c2i.put(JSUnitAssertFunctions.class, "jsunit.png");
		c2i.put(ServoyException.class, "exception.png");
		c2i.put(JSForm.class, "controller.png");
		c2i.put(FoundSet.class, "foundset.png");
	}

	public ImageDescriptor descriptor(Class< ? > cls)
	{
		String icon = c2i.get(cls);
		if (icon != null)
		{
			return com.servoy.eclipse.ui.Activator.loadImageDescriptorFromBundle(icon);
		}
		else return null;
	}

	public Image image(Class< ? > cls)
	{
		String icon = c2i.get(cls);
		if (icon != null)
		{
			return uiActivator.loadImageFromBundle(icon);
		}
		else return null;
	}
}
