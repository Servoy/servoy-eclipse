/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2021 Servoy BV

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

package com.servoy.eclipse.ngclient.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.sablo.specification.PackageSpecification;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebObjectFunctionDefinition;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.specification.WebObjectSpecification.PushToServerEnum;
import org.sablo.specification.property.CustomJSONPropertyType;
import org.sablo.specification.property.ICustomType;
import org.sablo.specification.property.IPropertyType;
import org.sablo.websocket.impl.ClientService;

import com.servoy.eclipse.model.war.exporter.IWarExportModel;
import com.servoy.j2db.persistence.IContentSpecConstants;
import com.servoy.j2db.server.ngclient.property.FoundsetLinkedPropertyType;
import com.servoy.j2db.server.ngclient.property.FoundsetPropertyType;
import com.servoy.j2db.server.ngclient.property.types.DataproviderPropertyType;
import com.servoy.j2db.server.ngclient.property.types.FormPropertyType;
import com.servoy.j2db.server.ngclient.property.types.ValueListPropertyType;
import com.servoy.j2db.util.Pair;

/**
 * @author jcompagner
 * @since 2021.06
 */
public class ComponentTemplateGenerator
{
	/**
	 *  return a pair where left is the full template string of all the components and
	 *  right is the viewchild reference value to those components
	 * @return Pair<String,String>
	 */
	public Pair<StringBuilder, StringBuilder> generateHTMLTemplate(IWarExportModel model)
	{
		StringBuilder template = new StringBuilder();
		StringBuilder viewChild = new StringBuilder();

		template.append("<!-- component template generate start -->\n");
		viewChild.append("// component viewchild template generate start\n");
		WebObjectSpecification[] specs = WebComponentSpecProvider.getSpecProviderState().getAllWebObjectSpecifications();
		Arrays.sort(specs, new Comparator<WebObjectSpecification>()
		{
			@Override
			public int compare(WebObjectSpecification o1, WebObjectSpecification o2)
			{
				return o1.getName().compareToIgnoreCase(o2.getName());
			}
		});
		Map<String, Boolean> ng2Compatible = new HashMap<String, Boolean>();
		for (WebObjectSpecification spec : specs)
		{
			if (model == null || model.getAllExportedComponents().contains(spec.getName()))
			{
				String packageName = spec.getPackageName();
				if (!ng2Compatible.containsKey(packageName))
				{
					PackageSpecification packageSpecification = WebComponentSpecProvider.getSpecProviderState().getWebObjectSpecifications().get(packageName);
					if (packageSpecification != null)
					{
						Boolean isNG2Compatible = Boolean.FALSE;
						if ("servoycore".equals(packageName) || packageSpecification.getNpmPackageName() != null ||
							packageSpecification.getNg2Module() != null ||
							packageSpecification.getEntryPoint() != null)
						{
							isNG2Compatible = Boolean.TRUE;
						}
						ng2Compatible.put(packageName, isNG2Compatible);
					}
				}
				if (ng2Compatible.containsKey(packageName) && !ng2Compatible.get(packageName))
					continue;
				genereateSpec(template, viewChild, spec, spec.getName());
				if (spec.getName().equals("servoydefault-tabpanel"))
				{
					// also generate the tabless
					genereateSpec(template, viewChild, spec, "servoydefault-tablesspanel");
					genereateSpec(template, viewChild, spec, "servoydefault-accordion");
				}
			}

		}
		template.append("<!-- component template generate end -->");
		viewChild.append("// component viewchild template generate end");
		return new Pair<>(template, viewChild);
	}

	private void genereateSpec(StringBuilder template, StringBuilder viewChild, WebObjectSpecification spec, String specName)
	{
		String templateName = ClientService.convertToJSName(specName);
		template.append("<ng-template #");
		template.append(templateName);
		template.append(" let-callback=\"callback\" let-state=\"state\">");
		template.append('<');
		template.append(specName);
		template.append(' ');

		viewChild.append("@ViewChild('");
		viewChild.append(templateName);
		viewChild.append("', { static: true }) readonly ");
		viewChild.append(templateName);
		viewChild.append(": TemplateRef<any>;\n");

		ArrayList<PropertyDescription> specProperties = new ArrayList<>(spec.getProperties().values());
		Collections.sort(specProperties, new Comparator<PropertyDescription>()
		{
			@Override
			public int compare(PropertyDescription o1, PropertyDescription o2)
			{
				return o1.getName().compareToIgnoreCase(o2.getName());
			}
		});
		for (PropertyDescription pd : specProperties)
		{
			String name = pd.getName();
			if (name.equals("anchors") || name.equals("formIndex")) continue;
			if (name.equals(IContentSpecConstants.PROPERTY_ATTRIBUTES))
			{
				name = "servoyAttributes";
			}
			if (name.equals(IContentSpecConstants.PROPERTY_VISIBLE))
			{
				template.append(" *ngIf=\"state.model.");
			}
			else
			{
				template.append(" [");
				template.append(name);
				template.append("]=\"state.model.");
			}
			template.append(name);
			template.append('"');

			// all properties that handle there own stuff, (that have converters on the server side)
			// should not have the need for an emitter/datachange call. this should be handled in the type itself.
			// TODO can we make this check more generic?
			if (pd.getPushToServer() != null && pd.getPushToServer() != PushToServerEnum.reject &&
				!(pd.getType() instanceof FoundsetPropertyType ||
					pd.getType() instanceof FoundsetLinkedPropertyType ||
					pd.getType() instanceof ValueListPropertyType))
			{
				template.append(" (");
				template.append(name);
				template.append("Change)=\"callback.datachange(state,'");
				template.append(name);
				if (pd.getType() instanceof DataproviderPropertyType)
				{
					template.append("',$event, true)\"");
				}
				else
				{
					template.append("',$event)\"");
				}
			}
		}

		ArrayList<WebObjectFunctionDefinition> handlers = new ArrayList<>(spec.getHandlers().values());
		Collections.sort(handlers, new Comparator<WebObjectFunctionDefinition>()
		{
			@Override
			public int compare(WebObjectFunctionDefinition o1, WebObjectFunctionDefinition o2)
			{
				return o1.getName().compareToIgnoreCase(o2.getName());
			}
		});
		for (WebObjectFunctionDefinition handler : handlers)
		{
			if (!handler.isPrivate())
			{
				template.append(" [");
				template.append(handler.getName());
				template.append("]=\"callback.getHandler(state,'");
				template.append(handler.getName());
				template.append("')\"");
			}
		}
		template.append(" [servoyApi]=\"callback.getServoyApi(state)\"");
		template.append(" [name]=\"state.name\" #cmp");
		template.append(">");
		Collection<PropertyDescription> properties = spec.getProperties(FormPropertyType.INSTANCE);
		if (properties.size() == 0)
		{
			Map<String, ICustomType< ? >> declaredCustomObjectTypes = spec.getDeclaredCustomObjectTypes();
			for (IPropertyType< ? > pt : declaredCustomObjectTypes.values())
			{
				if (pt instanceof CustomJSONPropertyType< ? >)
				{
					PropertyDescription customJSONSpec = ((CustomJSONPropertyType< ? >)pt).getCustomJSONTypeDefinition();
					properties = customJSONSpec.getProperties(FormPropertyType.INSTANCE);
					if (properties.size() > 0) break;
				}
			}
		}
		if (properties.size() > 0)
		{
			template.append("<ng-template let-name='name'><svy-form *ngIf=\"isFormAvailable(name)\" [name]=\"name\"></svy-form></ng-template>");
		}
		template.append("</");
		template.append(specName);
		template.append(">");

		template.append("</ng-template>\n");
	}
}
