/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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

package com.servoy.eclipse.debug.scriptingconsole;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPageService;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PerspectiveAdapter;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.part.IPage;
import org.eclipse.ui.part.IPageBookViewPage;
import org.eclipse.ui.part.MessagePage;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.PageBookView;

import com.servoy.eclipse.debug.Activator;

public class ScriptingConsole extends PageBookView implements IConsoleView
{
	public static final String SCRIPTINGCONSOLE_SETTING = "SCRIPTINGCONSOLE"; //$NON-NLS-1$
	private ScriptConsole console;
	private IMemento memento;

	@Override
	public void createPartControl(Composite parent)
	{
		super.createPartControl(parent);

		IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();
		tbm.add(new Separator(IConsoleConstants.OUTPUT_GROUP));

		console = new ScriptConsole();
		if (memento != null) console.restoreState(memento);
		ConsoleWorkbenchPart part = new ConsoleWorkbenchPart(console, getSite());
		partActivated(part);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.ViewPart#init(org.eclipse.ui.IViewSite, org.eclipse.ui.IMemento)
	 */
	@Override
	public void init(IViewSite site, IMemento mem) throws PartInitException
	{
		super.init(site, mem);

		IPageService service = (IPageService)getSite().getService(IPageService.class);
		service.addPerspectiveListener(new PerspectiveAdapter()
		{
			@Override
			public void perspectiveChanged(IWorkbenchPage page, IPerspectiveDescriptor perspective, IWorkbenchPartReference partRef, String changeId)
			{
				if (IWorkbenchPage.CHANGE_VIEW_HIDE.equals(changeId) && partRef.getPart(false) == ScriptingConsole.this)
				{
					XMLMemento xmlMemento = XMLMemento.createWriteRoot(SCRIPTINGCONSOLE_SETTING);
					saveState(xmlMemento);
					StringWriter writer = new StringWriter();
					try
					{
						xmlMemento.save(writer);
						Activator.getDefault().getDialogSettings().put(SCRIPTINGCONSOLE_SETTING, writer.getBuffer().toString());
					}
					catch (IOException e)
					{
						// don't do anything. Simply don't store the settings
					}
				}
			}
		});

		if (mem == null)
		{
			String persistedMemento = Activator.getDefault().getDialogSettings().get(SCRIPTINGCONSOLE_SETTING);
			if (persistedMemento != null)
			{
				try
				{
					this.memento = XMLMemento.createReadRoot(new StringReader(persistedMemento));
				}
				catch (WorkbenchException e)
				{
					// don't do anything. Simply don't restore the settings
				}
			}
		}
		else
		{
			this.memento = mem;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.ViewPart#saveState(org.eclipse.ui.IMemento)
	 */
	@Override
	public void saveState(IMemento mem)
	{
		super.saveState(mem);
		console.saveState(mem);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.WorkbenchPart#dispose()
	 */
	@Override
	public void dispose()
	{
		super.dispose();
	}

	@Override
	public void setFocus()
	{
		console.getTextWidget().setFocus();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.PageBookView#createDefaultPage(org.eclipse.ui.part.PageBook)
	 */
	@Override
	protected IPage createDefaultPage(PageBook book)
	{
		MessagePage page = new MessagePage();
		page.createControl(getPageBook());
		initPage(page);
		return page;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.PageBookView#doCreatePage(org.eclipse.ui.IWorkbenchPart)
	 */
	@Override
	protected PageRec doCreatePage(IWorkbenchPart dummyPart)
	{
		ConsoleWorkbenchPart part = (ConsoleWorkbenchPart)dummyPart;
		final IConsole console = part.getConsole();
		final IPageBookViewPage page = console.createPage(this);
		initPage(page);
		page.createControl(getPageBook());

		PageRec rec = new PageRec(dummyPart, page);
		return rec;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.PageBookView#doDestroyPage(org.eclipse.ui.IWorkbenchPart, org.eclipse.ui.part.PageBookView.PageRec)
	 */
	@Override
	protected void doDestroyPage(IWorkbenchPart part, PageRec pageRecord)
	{
		IPage page = pageRecord.page;
		page.dispose();
		pageRecord.dispose();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.PageBookView#getBootstrapPart()
	 */
	@Override
	protected IWorkbenchPart getBootstrapPart()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.PageBookView#isImportant(org.eclipse.ui.IWorkbenchPart)
	 */
	@Override
	protected boolean isImportant(IWorkbenchPart part)
	{
		return part instanceof ConsoleWorkbenchPart;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.console.IConsoleView#display(org.eclipse.ui.console.IConsole)
	 */
	public void display(IConsole console)
	{
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.console.IConsoleView#setPinned(boolean)
	 */
	public void setPinned(boolean pin)
	{
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.console.IConsoleView#pin(org.eclipse.ui.console.IConsole)
	 */
	public void pin(IConsole console)
	{
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.console.IConsoleView#isPinned()
	 */
	public boolean isPinned()
	{
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.console.IConsoleView#getConsole()
	 */
	public IConsole getConsole()
	{
		return console;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.console.IConsoleView#warnOfContentChange(org.eclipse.ui.console.IConsole)
	 */
	public void warnOfContentChange(IConsole console)
	{
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.console.IConsoleView#setScrollLock(boolean)
	 */
	public void setScrollLock(boolean scrollLock)
	{
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.console.IConsoleView#getScrollLock()
	 */
	public boolean getScrollLock()
	{
		return false;
	}
}
