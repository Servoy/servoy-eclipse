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

package com.servoy.eclipse.core;

import java.awt.Component;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import org.mozilla.javascript.Scriptable;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebObjectSpecification;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.dataprocessing.IFoundSet;
import com.servoy.j2db.dataprocessing.IRecord;
import com.servoy.j2db.debug.DebugUtils;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.Bean;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IDataProviderLookup;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.TabPanel;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.server.ngclient.template.FormTemplateGenerator;
import com.servoy.j2db.smart.dataui.ComponentAccordionPanel;
import com.servoy.j2db.smart.dataui.ComponentJTabbedPane;
import com.servoy.j2db.smart.dataui.InvisibleBean;
import com.servoy.j2db.smart.dataui.SpecialSplitPane;
import com.servoy.j2db.smart.dataui.VisibleBean;
import com.servoy.j2db.ui.IComponent;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.ui.IStandardLabel;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.OrientationApplier;

/**
 * Component factory for use in design more
 * @author jblok
 */
public class DesignComponentFactory extends ComponentFactory
{
	public static Object getBeanDesignInstance(IApplication application, FlattenedSolution flattenedSolution, Bean bean, Form form)
	{
		Object beanDesignComponent = null;
		beanDesignComponent = flattenedSolution.getBeanDesignInstance(bean);
		if (beanDesignComponent == null)
		{
			createDesignComponent(application, flattenedSolution, bean, form);
			beanDesignComponent = flattenedSolution.getBeanDesignInstance(bean);
		}

		return beanDesignComponent;
	}


	public static Component createDesignComponent(final IApplication application, final FlattenedSolution flattenedSolution, final IPersist meta,
		final Form form)
	{
		final Component[] comp = { null };

		try
		{
			DebugUtils.invokeAndWaitWhileDispatchingOnSWT(new Runnable()
			{
				public void run()
				{
					comp[0] = createDesignComponentEx(application, flattenedSolution, meta, form);
				}
			});
		}
		catch (Exception ex)
		{
			ServoyLog.logError("Error creating design component  " + meta, ex);
		}

		return comp[0];
	}

	private static Component createDesignComponentEx(IApplication application, FlattenedSolution flattenedSolution, IPersist meta, Form form)
	{
		Component c = null;
		if (meta.getTypeID() == IRepository.BEANS)
		{
			// can cast, design should always be a swing
			IComponent comp = createBean(application, form, (Bean)meta, flattenedSolution);
			if (comp instanceof InvisibleBean)
			{
				c = (InvisibleBean)comp;
			}
			else if (comp instanceof VisibleBean)
			{
				c = ((VisibleBean)comp).getDelegate();
			}
			else if (comp instanceof Component)
			{
				c = (Component)comp;
			}
		}
		else if (meta.getTypeID() == IRepository.WEBCOMPONENTS)
		{
			if (meta instanceof WebComponent)
			{
				WebObjectSpecification spec = WebComponentSpecProvider.getSpecProviderState().getWebObjectSpecification(
					FormTemplateGenerator.getComponentTypeName((WebComponent)meta));
				String label;
				if (spec == null)
				{
					label = "Web component not found " + ((WebComponent)meta).getName();
				}
				else
				{
					label = "Web component (" + spec.getDisplayName() + ')';
				}
				c = (Component)application.getItemFactory().createLabel(((WebComponent)meta).getName(), label);
			}
		}
		else
		{
			switch (meta.getTypeID())
			{
				case IRepository.TABPANELS :
					JComponent retval;
					int orient = ((TabPanel)meta).getTabOrientation();
					if (orient == TabPanel.HIDE)
					{
						// Designer all always real components!!
						retval = (JComponent)application.getItemFactory().createLabel(null, "Tabless panel, for JavaScript use");
						((IStandardLabel)retval).setHorizontalAlignment(SwingConstants.CENTER);
						applyBasicComponentProperties(application, (IComponent)retval, (BaseComponent)meta,
							getStyleForBasicComponent(application, (BaseComponent)meta, form));
					}
					else if (orient == TabPanel.SPLIT_HORIZONTAL || orient == TabPanel.SPLIT_VERTICAL)
					{
						SpecialSplitPane splitPane = new SpecialSplitPane(application, null /* no scriptable */, orient, true);
						applyBasicComponentProperties(application, splitPane, (BaseComponent)meta,
							getStyleForBasicComponent(application, (BaseComponent)meta, form));

						for (int i = 0; i < 2; i++)
						{
							String name;
							if (i == 0) name = orient == TabPanel.SPLIT_HORIZONTAL ? "Left panel" : "Top panel";
							else name = orient == TabPanel.SPLIT_HORIZONTAL ? "Right panel" : "Bottom panel";

							JLabel label = new JLabel(name, SwingConstants.CENTER);
							if (i == 0) splitPane.getSplitPane().setLeftComponent(label);
							else splitPane.getSplitPane().setRightComponent(label);
						}

						splitPane.setRuntimeDividerLocation(
							(orient == TabPanel.SPLIT_HORIZONTAL ? splitPane.getSize().width / 2 : splitPane.getSize().height / 2));
						retval = splitPane;
					}
					else if (orient == TabPanel.ACCORDION_PANEL)
					{
						ComponentAccordionPanel tabs = new ComponentAccordionPanel();
						applyBasicComponentProperties(application, tabs, (BaseComponent)meta,
							getStyleForBasicComponent(application, (BaseComponent)meta, form));
						tabs.setAllTabsAlignment(((TabPanel)meta).getHorizontalAlignment());
						retval = tabs;
					}
					else
					{
						ComponentJTabbedPane tabs = new ComponentJTabbedPane();
						applyBasicComponentProperties(application, tabs, (BaseComponent)meta,
							getStyleForBasicComponent(application, (BaseComponent)meta, form));
						tabs.addTab("position example", new JLabel("form will appear here", SwingConstants.CENTER));
						tabs.addTab("position 2", new JLabel("another form showup here", SwingConstants.CENTER));
						if (orient == SwingConstants.TOP || orient == SwingConstants.LEFT || orient == SwingConstants.BOTTOM || orient == SwingConstants.RIGHT)
						{
							tabs.setTabPlacement(orient);
						}
						retval = tabs;
					}
					OrientationApplier.setOrientationToAWTComponent(retval, application.getLocale(), application.getSolution().getTextOrientation());
					return retval;

				default :
					IDataProviderLookup dataProviderLookup = application.getFlattenedSolution().getDataproviderLookup(application.getFoundSetManager(), form);
					c = (Component)createComponentEx(application, form, meta, dataProviderLookup, null, false);
			}
		}
		if (c instanceof JComponent)
		{
			((JComponent)c).setDoubleBuffered(false);
		}
		if (c instanceof IDisplayData && paintSampleData)
		{
			if (form != null)
			{
				try
				{
					IFoundSet fs = application.getFoundSetManager().getSharedFoundSet(form.getDataSource());
					if (fs != null && fs.getSize() == 0) fs.loadAllRecords();
					if (fs != null && fs.getSize() > 0)
					{
						IRecord record = fs.getRecord(0);
						IDisplayData data = (IDisplayData)c;
						Object value = record.getValue(data.getDataProviderID());
						if (value == Scriptable.NOT_FOUND)
						{
							ScriptVariable variable = form.getScriptVariable(data.getDataProviderID());
							if (variable != null) value = variable.getDefaultValue();
						}
						data.setValueObject(value);
					}
				}
				catch (Exception e)
				{
					Debug.error(e);
				}
			}
		}
		else if (meta instanceof GraphicalComponent && ((GraphicalComponent)meta).getDisplaysTags() && c instanceof ILabel)
		{
			((ILabel)c).setText(application.getI18NMessageIfPrefixed(((GraphicalComponent)meta).getText()));
		}
		if (c != null) OrientationApplier.setOrientationToAWTComponent(c, application.getLocale(), application.getSolution().getTextOrientation());
		return c;//removeTransparencyAndScrolling(c);
	}
//	private static Component removeTransparencyAndScrolling(Component c)
//	{
//		//remove any transparency of scrollpanes
//		if (c instanceof Container)
//		{
//			Component[] all = ((Container)c).getComponents();
//			for(int i = 0 ; i < all.length ; i++)
//			{
//				Component cold = all[i];
//				Component cnew = removeTransparencyAndScrolling(cold);
//				if (!cold.equals(cnew))
//				{
//					((Container)c).remove(cold);
//					((Container)c).add(cnew);
//				}
//			}
//		}
//		if (c instanceof JScrollPane)
//		{
//			c = removeTransparencyAndScrolling(((JScrollPane)c).getViewport().getView());
//		}
//		if (c instanceof JViewport)
//		{
//			c = removeTransparencyAndScrolling(((JViewport)c).getView());
//		}
//		if (c instanceof JComponent)
//		{
//			((JComponent)c).setOpaque(true);
//		}
//		return c;
//	}
}
