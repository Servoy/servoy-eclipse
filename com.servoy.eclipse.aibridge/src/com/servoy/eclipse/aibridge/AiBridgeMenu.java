package com.servoy.eclipse.aibridge;

import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;

public class AiBridgeMenu extends CompoundContributionItem
{

	CommandContributionItem explainItem = null;
	CommandContributionItem addCommentsItem = null;
	CommandContributionItem debugItem = null;

	@Override
	protected IContributionItem[] getContributionItems()
	{
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

		if (!shouldBeVisible())
		{
			return new IContributionItem[0];
		}

		MenuManager subMenuManager = new MenuManager("Servoy AI", "com.servoy.eclipse.aibridge.submenu");

		CommandContributionItemParameter explainParam = new CommandContributionItemParameter(
			window,
			null,
			"com.servoy.eclipse.aibridge.explain_command",
			CommandContributionItem.STYLE_PUSH);

		CommandContributionItemParameter addCommentsParam = new CommandContributionItemParameter(
			window,
			null,
			"com.servoy.eclipse.aibridge.add_inline_comments",
			CommandContributionItem.STYLE_PUSH);

		CommandContributionItemParameter debugParam = new CommandContributionItemParameter(
			window,
			null,
			"com.servoy.eclipse.aibridge.debug",
			CommandContributionItem.STYLE_PUSH);

		subMenuManager.add(new CommandContributionItem(explainParam));
		subMenuManager.add(new CommandContributionItem(addCommentsParam));
		subMenuManager.add(new CommandContributionItem(debugParam));

		return new IContributionItem[] { subMenuManager };
	}

	private boolean shouldBeVisible()
	{
		ISelection selection = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection();
		if (selection instanceof ITextSelection textSelection)
		{
			String text = textSelection.getText();
			return text != null && text.trim().length() > 0;
		}
		return false;
	}
}
