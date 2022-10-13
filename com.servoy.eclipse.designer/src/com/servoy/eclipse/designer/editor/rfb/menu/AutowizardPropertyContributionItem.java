/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2022 Servoy BV

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

package com.servoy.eclipse.designer.editor.rfb.menu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jface.action.IContributionItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebObjectSpecification;

import com.servoy.eclipse.designer.editor.commands.ConfigureCustomTypeCommand;
import com.servoy.eclipse.designer.util.DesignerUtil;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.WebComponent;

/**
 * @author emera
 */
public class AutowizardPropertyContributionItem extends CompoundContributionItem
{
	@Override
	protected IContributionItem[] getContributionItems()
	{
		List<IContributionItem> list = new ArrayList<IContributionItem>();
		PersistContext persistContext = DesignerUtil.getContentOutlineSelection();
		IPersist persist = persistContext != null ? persistContext.getPersist() : null;
		if (persist instanceof WebComponent)
		{
			WebObjectSpecification spec = WebComponentSpecProvider.getSpecProviderState().getWebComponentSpecification(((WebComponent)persist).getTypeName());
			list = spec.getAllPropertiesNames().stream()
				.filter(property -> spec.getProperty(property) != null && "autoshow".equals(spec.getProperty(property).getTag("wizard")))//
				.map(p -> {
					final CommandContributionItemParameter commandContributionItemParameter = new CommandContributionItemParameter(
						PlatformUI.getWorkbench().getActiveWorkbenchWindow(), null, ConfigureCustomTypeCommand.COMMAND_ID, CommandContributionItem.STYLE_PUSH);
					commandContributionItemParameter.parameters = new HashMap<String, String>();
					commandContributionItemParameter.parameters.put("com.servoy.eclipse.designer.editor.rfb.menu.config.type", p);
					commandContributionItemParameter.label = p;
					commandContributionItemParameter.visibleEnabled = true;
					return new CommandContributionItem(commandContributionItemParameter);
				})
				.collect(Collectors.toList());
			return list.toArray(new IContributionItem[list.size()]);
		}
		return null;
	}
}
