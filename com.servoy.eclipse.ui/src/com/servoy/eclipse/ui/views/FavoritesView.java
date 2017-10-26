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

package com.servoy.eclipse.ui.views;

import java.net.URL;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.intro.impl.model.loader.ModelLoaderUtil;
import org.eclipse.ui.internal.intro.impl.model.url.IntroURL;
import org.eclipse.ui.internal.intro.impl.model.url.IntroURLParser;
import org.eclipse.ui.part.ViewPart;
import org.json.JSONArray;
import org.json.JSONObject;

import com.servoy.eclipse.core.IStartPageAction;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Activator;
import com.servoy.j2db.util.Utils;

/**
 * @author gboros
 *
 */
public class FavoritesView extends ViewPart
{
	public static final String PART_ID = "com.servoy.eclipse.ui.views.FavoritesView";

	private final com.servoy.eclipse.ui.Activator uiActivator = com.servoy.eclipse.ui.Activator.getDefault();
	private JSONArray dataModel;

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(Composite parent)
	{
		loadDataModel();
		createScrolledContainer(parent);
	}

	private void loadDataModel()
	{
		URL favURL = uiActivator.getBundle().getEntry("favorites/favorites.json");
		String favoritesJSON = Utils.getURLContent(favURL);
		dataModel = new JSONArray(favoritesJSON);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
	 */
	@Override
	public void setFocus()
	{
	}

	private void createScrolledContainer(Composite parent)
	{
		Composite rootComposite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.verticalSpacing = 0;
		rootComposite.setLayout(layout);
		rootComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		rootComposite.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		ScrolledComposite sc = new ScrolledComposite(rootComposite, SWT.H_SCROLL | SWT.V_SCROLL);
		sc.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		Composite mainContainer = new Composite(sc, SWT.NONE);
		sc.setContent(mainContainer);
		mainContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		mainContainer.setLayout(layout);
		mainContainer.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND));

		for (int i = 0; i < dataModel.length(); i++)
		{
			new RowComposite(mainContainer, dataModel.getJSONObject(i));
		}

		sc.setExpandHorizontal(true);
		sc.setExpandVertical(true);
		sc.setMinSize(mainContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT, true));
		mainContainer.layout();
		sc.layout();
	}


	private class RowComposite extends Composite implements MouseListener
	{
		private final JSONObject rowData;

		RowComposite(Composite parent, JSONObject rowData)
		{
			super(parent, SWT.FILL);
			this.rowData = rowData;
			GridLayout layout = new GridLayout();
			layout.numColumns = 1;
			layout.verticalSpacing = 1;
			layout.marginHeight = 1;
			setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			setLayout(layout);
			setBackground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
			addMouseListener(this);

			Composite row = new Composite(this, SWT.FILL);
			row.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			row.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND));

			GridLayout rowLayout = new GridLayout();
			rowLayout.numColumns = 3;
			rowLayout.horizontalSpacing = 10;
			rowLayout.marginHeight = 1;
			row.setLayout(rowLayout);

			Label img = new Label(row, SWT.NONE);
			img.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
			img.setImage(Activator.getDefault().loadImageFromBundle(rowData.optString("icon")));
			img.addMouseListener(this);

			Label item = new Label(row, SWT.NONE);
			item.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
			item.setText(rowData.optString("name"));
			item.addMouseListener(this);

			Label open = new Label(row, SWT.NONE);
			open.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
			open.setText("-->");
			open.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, true));
			open.addMouseListener(this);

			Label separator = new Label(this, SWT.HORIZONTAL | SWT.SEPARATOR);
			separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see org.eclipse.swt.events.MouseListener#mouseDoubleClick(org.eclipse.swt.events.MouseEvent)
		 */
		@Override
		public void mouseDoubleClick(MouseEvent e)
		{
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see org.eclipse.swt.events.MouseListener#mouseDown(org.eclipse.swt.events.MouseEvent)
		 */
		@Override
		public void mouseDown(MouseEvent e)
		{
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see org.eclipse.swt.events.MouseListener#mouseUp(org.eclipse.swt.events.MouseEvent)
		 */
		@Override
		public void mouseUp(MouseEvent e)
		{
			String action = rowData.optString("action");
			if (action.length() > 0)
			{
				IntroURLParser parser = new IntroURLParser(action);
				if (parser.hasIntroUrl())
				{
					// execute the action embedded in the IntroURL
					IntroURL introURL = parser.getIntroURL();
					if (IntroURL.RUN_ACTION.equals(introURL.getAction()))
					{
						Object actionObject = ModelLoaderUtil.createClassInstance(introURL.getParameter(IntroURL.KEY_PLUGIN_ID),
							introURL.getParameter(IntroURL.KEY_CLASS));
						if (actionObject instanceof IStartPageAction)
						{
							((IStartPageAction)actionObject).runAction(introURL);
						}
						else
						{
							ServoyLog.logWarning("IntroURL action class is not of type IStartPageAction", null);
						}
					}
					else
					{
						ServoyLog.logWarning("IntroURL in favorites is not of type ACTION", null);
					}
				}
				else
				{
					try
					{
						PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(action));
					}
					catch (Exception ex)
					{
						ServoyLog.logError(ex);
					}
				}
			}
		}
	}
}