/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2020 Servoy BV

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

import java.awt.Dimension;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.part.ViewPart;
import org.json.JSONArray;
import org.json.JSONObject;

import com.servoy.eclipse.ui.dialogs.BrowserDialog;
import com.servoy.j2db.util.Utils;

/**
 * @author lvostinar
 *
 */
public class TutorialView extends ViewPart
{
	public static final String PART_ID = "com.servoy.eclipse.ui.views.TutorialView";

	private JSONObject dataModel;
	BrowserDialog dialog = null;

	@Override
	public void createPartControl(Composite parent)
	{
		loadDataModel();
		createTutorialView(parent);
	}

	private void createTutorialView(Composite parent)
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

		Label nameLabel = new Label(mainContainer, SWT.NONE);
		nameLabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		nameLabel.setText(dataModel.optString("name"));
		FontDescriptor descriptor = FontDescriptor.createFrom(nameLabel.getFont());
		descriptor = descriptor.setStyle(SWT.BOLD);
		descriptor = descriptor.increaseHeight(12);
		nameLabel.setFont(descriptor.createFont(this.getViewSite().getShell().getDisplay()));

		JSONArray steps = dataModel.optJSONArray("steps");
		if (steps != null)
		{
			for (int i = 0; i < steps.length(); i++)
			{
				new RowComposite(mainContainer, steps.getJSONObject(i), i + 1);
			}
		}
		sc.setExpandHorizontal(true);
		sc.setExpandVertical(true);
		sc.setMinSize(mainContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT, true));
		mainContainer.layout();
		sc.layout();
	}

	private class RowComposite extends Composite
	{
		RowComposite(Composite parent, JSONObject rowData, int index)
		{
			super(parent, SWT.FILL);
			GridLayout layout = new GridLayout();
			layout.numColumns = 1;
			layout.verticalSpacing = 1;
			layout.marginHeight = 1;
			setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			setLayout(layout);
			setBackground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND));

			Composite row = new Composite(this, SWT.FILL);
			row.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			row.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND));

			GridLayout rowLayout = new GridLayout();
			rowLayout.numColumns = 1;
			rowLayout.horizontalSpacing = 10;
			rowLayout.marginHeight = 1;
			rowLayout.marginTop = 10;
			row.setLayout(rowLayout);

			Label stepName = new Label(row, SWT.NONE);
			stepName.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
			stepName.setText(index + ". " + rowData.optString("name"));
			FontDescriptor descriptor = FontDescriptor.createFrom(stepName.getFont());
			descriptor = descriptor.setStyle(SWT.BOLD);
			descriptor = descriptor.increaseHeight(6);
			stepName.setFont(descriptor.createFont(parent.getDisplay()));

			Label stepDescription = new Label(row, SWT.NONE);
			stepDescription.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
			stepDescription.setText(rowData.optString("description"));
			descriptor = FontDescriptor.createFrom(stepDescription.getFont());
			descriptor = descriptor.increaseHeight(2);
			stepDescription.setFont(descriptor.createFont(parent.getDisplay()));

			StyledText gifURL = new StyledText(row, SWT.NONE);
			gifURL.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
			gifURL.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
			gifURL.setEditable(false);
			gifURL.setText("< show");
			StyleRange styleRange = new StyleRange();
			styleRange.underline = true;
			styleRange.start = 0;
			styleRange.length = 6;
			gifURL.setStyleRange(styleRange);
			gifURL.setCursor(new Cursor(parent.getDisplay(), SWT.CURSOR_HAND));
			descriptor = FontDescriptor.createFrom(gifURL.getFont());
			descriptor = descriptor.setHeight(10);
			gifURL.setFont(descriptor.createFont(parent.getDisplay()));
			gifURL.addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseUp(MouseEvent e)
				{
					Dimension size = null;
					try
					{
						ImageDescriptor desc = ImageDescriptor.createFromURL(new URL(rowData.optString("gifURL")));
						ImageData imgData = desc.getImageData(100);
						size = new Dimension(imgData.width, imgData.height);
					}
					catch (MalformedURLException e1)
					{
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					if (size == null)
					{
						size = new Dimension(300, 300);
					}
					Point cursorLocation = Display.getCurrent().getCursorLocation();

					Point location = new Point(cursorLocation.x - size.width - 50, cursorLocation.y + 5);
					if (dialog == null || dialog.isDisposed())
					{
						dialog = new BrowserDialog(parent.getShell(),
							rowData.optString("gifURL"), false, false);
						dialog.open(location, size);
					}
					else
					{
						dialog.setUrl(rowData.optString("gifURL"));
					}
				}
			});
		}
	}

	private void loadDataModel()
	{
		URL favURL = com.servoy.eclipse.ui.Activator.getDefault().getBundle().getEntry("favorites/tutorial.json");
		String favoritesJSON = Utils.getURLContent(favURL);
		dataModel = new JSONObject(favoritesJSON);
	}

	@Override
	public void setFocus()
	{

	}
}
