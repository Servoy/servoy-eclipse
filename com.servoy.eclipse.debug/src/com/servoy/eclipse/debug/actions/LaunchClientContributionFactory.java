/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2017 Servoy BV

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

package com.servoy.eclipse.debug.actions;


import org.eclipse.core.expressions.EvaluationResult;
import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.menus.ExtensionContributionFactory;
import org.eclipse.ui.menus.IContributionRoot;
import org.eclipse.ui.services.IServiceLocator;

import com.servoy.eclipse.debug.Activator;
import com.servoy.eclipse.debug.handlers.CreateBrowserContributions;

/**
 * @author jcomp
 *
 */
public abstract class LaunchClientContributionFactory extends ExtensionContributionFactory
{

	private final String menuText;
	private final String commandId;
	private final String variableName;
	private final String iconPath;

	/**
	 *
	 */
	public LaunchClientContributionFactory(String menuText, String commandId, String variableName, String iconPath)
	{
		super();
		this.menuText = menuText;
		this.commandId = commandId;
		this.variableName = variableName;
		this.iconPath = iconPath;
	}

	@Override
	public void createContributionItems(IServiceLocator serviceLocator, IContributionRoot additions)
	{
		MenuManager manager = new MenuManager(menuText);
		manager.setImageDescriptor(ImageDescriptor.createFromURL(FileLocator.find(Activator.getDefault().getBundle(), new Path(iconPath), null)));
		manager.setActionDefinitionId(commandId);
		CreateBrowserContributions item = new CreateBrowserContributions(commandId);
		item.initialize(serviceLocator);
		manager.add(item);
		additions.addContributionItem(manager, new Expression()
		{
			@Override
			public EvaluationResult evaluate(IEvaluationContext context) throws CoreException
			{
				Object variable = context.getVariable(variableName);
				if ("ENABLED".equals(variable)) return EvaluationResult.TRUE;
				else return EvaluationResult.FALSE;
			}
		});
	}

}