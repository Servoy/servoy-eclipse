/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2016 Servoy BV

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

package com.servoy.eclipse.cheatsheets;

import java.util.Properties;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.cheatsheets.OpenCheatSheetAction;
import org.eclipse.ui.internal.intro.impl.model.url.IntroURL;
import org.eclipse.ui.intro.IIntroSite;
import org.eclipse.ui.intro.config.IIntroAction;

import com.servoy.eclipse.core.IMainConceptsPageAction;

/**
 * @author gboros
 *
 */
public class OpenCheatSheet implements IIntroAction, IMainConceptsPageAction
{
	/*
	 * @see org.eclipse.ui.intro.config.IIntroAction#run(org.eclipse.ui.intro.IIntroSite, java.util.Properties)
	 */
	@Override
	public void run(IIntroSite site, final Properties params)
	{
		Display.getDefault().syncExec(new Runnable()
		{
			public void run()
			{
				IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				if (page != null)
				{
					for (IViewReference vw : page.getViewReferences())
						page.setPartState(vw, IWorkbenchPage.STATE_RESTORED);
					for (IEditorReference ed : page.getEditorReferences())
						page.setPartState(ed, IWorkbenchPage.STATE_RESTORED);
				}
				PlatformUI.getWorkbench().getIntroManager().closeIntro(PlatformUI.getWorkbench().getIntroManager().getIntro());
				new OpenCheatSheetAction(params.getProperty("id")).run();
			}
		});
	}

	@Override
	public void runAction(IntroURL introUrl)
	{
		new OpenCheatSheetAction(introUrl.getParameter("id")).run();
	}
}