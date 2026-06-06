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
package com.servoy.eclipse.core.quickfix.security;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

import com.servoy.eclipse.core.util.ReturnValueRunnable;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.repository.WorkspaceUserManager;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.server.shared.SecurityInfo;

/**
 * Quick fix for removing one of the access masks for a form element.
 *
 * @author acostescu
 */
public class RemoveAllButOneOfTheAccessMasksForElement extends AlterPermissionSecFileQuickFix
{

	private static RemoveAllButOneOfTheAccessMasksForElement instance;

	public static RemoveAllButOneOfTheAccessMasksForElement getInstance()
	{
		if (instance == null)
		{
			instance = new RemoveAllButOneOfTheAccessMasksForElement();
		}
		return instance;
	}

	@Override
	protected boolean canHandleType(int type)
	{
		return type == WorkspaceUserManager.SecurityReadException.DUPLICATE_ELEMENT_PERMISSION;
	}

	public String getLabel()
	{
		return "Keep only one access mask. Choose which one.";
	}

	@Override
	protected boolean alterPermissionInfo(Map<String, List<SecurityInfo>> access)
	{
		boolean altered = false;
		final String[] elementAndGroup = (String[])wrongValue;
		final List<SecurityInfo> elementAccesses = access.get(elementAndGroup[1]); // permissions for group
		if (elementAccesses != null)
		{
			final ArrayList<Integer> duplicates = new ArrayList<Integer>();
			for (SecurityInfo si : elementAccesses)
			{
				if (elementAndGroup[0].equals(si.element_uid))
				{
					duplicates.add(new Integer(si.access));
				}
			}

			if (duplicates.size() > 1)
			{
				ReturnValueRunnable runnable = new ReturnValueRunnable()
				{
					public void run()
					{
						returnValue = Boolean.FALSE;
						ElementListSelectionDialog dialog = new ElementListSelectionDialog(UIUtils.getActiveShell(), new LabelProvider()
						{
							@Override
							public String getText(Object element)
							{
								int a = ((Integer)element).intValue();
								return "Viewable: " + ((a & IRepository.VIEWABLE) != 0) + ", Accessible: " + ((a & IRepository.ACCESSIBLE) != 0);
							}
						});
						dialog.setMultipleSelection(false);
						dialog.setEmptySelectionMessage("no selection");
						dialog.setTitle("Choose access rights");
						dialog.setMessage("Please select which acess rights should remain.");
						Object[] dup = duplicates.toArray();
						dialog.setElements(dup);
						dialog.setInitialSelections(new Object[] { dup[0] });

						int choice = dialog.open();
						if (choice == Window.OK)
						{
							int result = ((Integer)dialog.getResult()[0]).intValue();
							// remove element accesses what are wrong
							Iterator<SecurityInfo> it = elementAccesses.iterator();
							while (it.hasNext())
							{
								SecurityInfo si = it.next();
								if (elementAndGroup[0].equals(si.element_uid) && si.access != result)
								{
									returnValue = Boolean.TRUE;
									it.remove();
								}
							}
						}
					}
				};
				UIUtils.runInUI(runnable, true);
				altered = ((Boolean)runnable.getReturnValue()).booleanValue();
			}
			else
			{
				reportProblem("Cannot find more than 1 element access specified for this element/group pair...");
			}
		}
		else
		{
			reportProblem("Cannot find permissions for this group/element pair.");
		}

		return altered;
	}
}
