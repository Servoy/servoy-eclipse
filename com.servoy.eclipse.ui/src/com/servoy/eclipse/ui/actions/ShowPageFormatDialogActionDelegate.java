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
package com.servoy.eclipse.ui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.print.PageFormat;
import java.util.NoSuchElementException;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.views.properties.IPropertySource;

import com.servoy.eclipse.core.Activator;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.gui.PageSetupDialog;

public class ShowPageFormatDialogActionDelegate implements IWorkbenchWindowActionDelegate, ActionListener
{
	protected PageSetupDialog pageSetupDialog;
	private String pageFormatString;

	public void dispose()
	{
		if (pageSetupDialog != null)
		{
			pageSetupDialog.getOKButton().removeActionListener(this);
			pageSetupDialog.setVisible(false);
			pageSetupDialog.dispose();
			pageSetupDialog = null;
		}
	}

	public void init(IWorkbenchWindow window)
	{
	}

	public void run(IAction action)
	{
		IApplication application = Activator.getDefault().getDesignClient();
		if (pageSetupDialog == null)
		{
			pageSetupDialog = new PageSetupDialog(application.getMainApplicationFrame(), false, true);
			pageSetupDialog.getOKButton().addActionListener(this);
		}
		PageFormat pageFormat = null;
		try
		{
			pageFormat = PersistHelper.createPageFormat(pageFormatString);
		}
		catch (NoSuchElementException e)
		{
		}
		pageSetupDialog.showDialog(pageFormat);
	}

	public void selectionChanged(IAction action, ISelection selection)
	{
		if (selection.isEmpty() || !(selection instanceof IStructuredSelection))
		{
			return;
		}
		IPersist persist = (IPersist)ResourceUtil.getAdapter(((IStructuredSelection)selection).getFirstElement(), IPersist.class, true);
		if (persist == null)
		{
			IPropertySource ps = (IPropertySource)ResourceUtil.getAdapter(((IStructuredSelection)selection).getFirstElement(), IPropertySource.class, true);
			if (ps != null)
			{
				persist = (IPersist)ResourceUtil.getAdapter(ps, IPersist.class, true);
			}
		}
		if (persist == null)
		{
			return;
		}
		Form form = (Form)persist.getAncestor(IRepository.FORMS);
		if (form == null)
		{
			return;
		}
		pageFormatString = form.getDefaultPageFormat();
	}

	public void actionPerformed(ActionEvent e)
	{
		if (pageSetupDialog != null && e.getSource() == pageSetupDialog.getOKButton())
		{
			// When user presses OK, copy page format string to clipboard
			PageFormat pageFormat = pageSetupDialog.getPageFormat();
			if (pageFormat != null)
			{
				pageFormatString = PersistHelper.createPageFormatString(pageFormat);
				if (pageFormatString != null)
				{
					final String selection = pageFormatString;
					Display.getDefault().asyncExec(new Runnable()
					{
						public void run()
						{
							Clipboard cb = new Clipboard(Display.getCurrent());
							cb.setContents(new Object[] { selection }, new Transfer[] { TextTransfer.getInstance() });
							cb.dispose();
						}
					});
				}
			}
		}

	}
}
