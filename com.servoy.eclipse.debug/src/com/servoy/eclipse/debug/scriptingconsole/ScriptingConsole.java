package com.servoy.eclipse.debug.scriptingconsole;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.part.IPage;
import org.eclipse.ui.part.IPageBookViewPage;
import org.eclipse.ui.part.MessagePage;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.PageBookView;

public class ScriptingConsole extends PageBookView implements IConsoleView
{
	private ScriptConsole console;

	@Override
	public void createPartControl(Composite parent)
	{
		super.createPartControl(parent);

		IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();
		tbm.add(new Separator(IConsoleConstants.OUTPUT_GROUP));

		console = new ScriptConsole();
		ConsoleWorkbenchPart part = new ConsoleWorkbenchPart(console, getSite());
		partActivated(part);
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