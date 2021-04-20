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
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.Util;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.Bullet;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GlyphMetrics;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.json.JSONArray;
import org.json.JSONObject;

import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.dialogs.BrowserDialog;
import com.servoy.eclipse.ui.dialogs.ServoyLoginDialog;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;

/**
 * @author lvostinar, afara
 *
 */
public class TutorialView extends ViewPart
{
	public static final String PART_ID = "com.servoy.eclipse.ui.views.TutorialView";
	private static boolean isTutorialViewOpen = false;
	private static final String URL_DEFAULT_TUTORIALS_LIST = "https://tutorials.servoy.com/servoy-service/rest_ws/contentAPI/listtutorials";
	private static final String SWT_CSS_ID_KEY = "org.eclipse.e4.ui.css.id";//did not import it to avoid adding dependencies for using one constant from CSSSWTConstants
	private static final String SVY_BACKGROUND = "svybackground";

	private JSONObject dataModel;
	private JSONArray dataTutorialsList;
	BrowserDialog dialog = null;

	private Composite rootComposite;
	private boolean showTutorialsList;

	@Override
	public void createPartControl(Composite parent)
	{
		loadDataModel(true, null);
		createTutorialView(parent, true);
		parent.addControlListener(new ControlAdapter()
		{
			Runnable run = null;
			long last = 0;
			int previousWidth = 0;

			@Override
			public void controlResized(ControlEvent e)
			{
				last = System.currentTimeMillis();
				if (run == null && previousWidth != parent.getSize().x)
				{
					previousWidth = parent.getSize().x;
					run = () -> {
						if ((System.currentTimeMillis() - last) < 300)
						{
							parent.getDisplay().timerExec(300, run);
						}
						else
						{
							createTutorialView(parent, showTutorialsList);
							run = null;
						}
					};
					parent.getDisplay().timerExec(300, run);
				}
			}
		});
	}

	private boolean isLightColor(Color c)
	{
		double hsp = Math.sqrt(0.299 * (c.getRed() * c.getRed()) +
			0.587 * (c.getGreen() * c.getGreen()) +
			0.114 * (c.getBlue() * c.getBlue()));
		return hsp > 127.5;
	}

	private void createTutorialView(Composite parent, boolean createDefaultTutorialsList)
	{
		parent.setData(SWT_CSS_ID_KEY, SVY_BACKGROUND);
		this.showTutorialsList = createDefaultTutorialsList;
		if (this.rootComposite != null) rootComposite.dispose();
		rootComposite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.verticalSpacing = 0;
		rootComposite.setLayout(layout);
		rootComposite.setData(SWT_CSS_ID_KEY, SVY_BACKGROUND);
		ScrolledComposite sc = new ScrolledComposite(rootComposite, SWT.H_SCROLL | SWT.V_SCROLL);
		sc.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		Composite mainContainer = new Composite(sc, SWT.NONE);
		sc.setContent(mainContainer);
		sc.setData(SWT_CSS_ID_KEY, SVY_BACKGROUND);
		layout = new GridLayout();
		layout.numColumns = 1;
		layout.verticalSpacing = 0;
		mainContainer.setLayout(layout);

		//this is a workaround, the background of the main container is not really set with setData until the view is focused out
		//TODO remove this when the background is set on the main container using setData
		if (isLightColor(parent.getBackground())) mainContainer.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		//end workaround

		mainContainer.setData(SWT_CSS_ID_KEY, SVY_BACKGROUND);

		if (createDefaultTutorialsList)
		{
			new TutorialsList(mainContainer, parent, parent.getSize().x);
		}
		else
		{
			StyledText openLink = createDefaultColorsStyledText(mainContainer, "Go back to tutorials");
			openLink.setCursor(new Cursor(parent.getDisplay(), SWT.CURSOR_HAND));
			FontDescriptor descriptor = FontDescriptor.createFrom(openLink.getFont());
			openLink.setFont(descriptor.createFont(parent.getDisplay()));
			openLink.addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseUp(MouseEvent event)
				{
					super.mouseUp(event);
					if (event.getSource() instanceof StyledText)
					{
						createTutorialView(parent, true);
					}
				}
			});
			Label nameLabel = new Label(mainContainer, SWT.NONE);
			nameLabel.setData(SWT_CSS_ID_KEY, SVY_BACKGROUND);
			nameLabel.setText(dataModel.optString("name"));
			descriptor = FontDescriptor.createFrom(nameLabel.getFont());
			descriptor = descriptor.setStyle(SWT.BOLD);
			descriptor = descriptor.increaseHeight(12);
			nameLabel.setFont(descriptor.createFont(this.getViewSite().getShell().getDisplay()));

			JSONArray steps = dataModel.optJSONArray("steps");
			if (steps != null)
			{
				for (int i = 0; i < steps.length(); i++)
				{
					new RowComposite(mainContainer, steps.getJSONObject(i), i + 1, parent.getSize().x);
				}
			}

			new TutorialButtons(mainContainer, parent);
		}
		sc.setExpandHorizontal(true);
		sc.setExpandVertical(true);
		sc.setMinSize(mainContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT, true));
		sc.requestLayout();
	}

	private StyledText createDefaultColorsStyledText(Composite parent, String text)
	{
		StyledText styledText = new StyledText(parent, SWT.NONE);
		styledText.setEditable(false);
		styledText.setText(text);
		styledText.setData(SWT_CSS_ID_KEY, SVY_BACKGROUND);
		return styledText;
	}

	private StyleRange getDefaultColorStyleRange(int start, int length, boolean defaultFont)
	{
		StyleRange style = new StyleRange();
		if (defaultFont) style.foreground = Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW);
		style.start = start;
		style.length = length;
		return style;
	}

	/**
	 * This class is representing the default tutorials list.
	 */
	private class TutorialsList extends Composite
	{
		public TutorialsList(Composite mainContainer, Composite firstParent, int widthHint)
		{
			super(mainContainer, SWT.FILL);
			GridLayout layout = new GridLayout();
			layout.numColumns = 1;
			layout.verticalSpacing = 1;
			layout.marginHeight = 1;
			setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			setLayout(layout);
			setData(SWT_CSS_ID_KEY, SVY_BACKGROUND);

			if (dataTutorialsList != null)
			{
				for (int i = 0; i < dataTutorialsList.length(); i++)
				{
					Composite row = new Composite(this, SWT.FILL);
					row.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
					row.setData(SWT_CSS_ID_KEY, SVY_BACKGROUND);

					GridLayout rowLayout = new GridLayout();
					rowLayout.numColumns = 1;
					rowLayout.horizontalSpacing = 10;
					rowLayout.marginHeight = 1;
					rowLayout.marginTop = 10;
					row.setLayout(rowLayout);

					String dividerText = dataTutorialsList.getJSONObject(i).optString("divider");
					boolean hasDivider = dividerText != null && !dividerText.isEmpty();
					Label name = new Label(row, SWT.WRAP);
					name.setData(SWT_CSS_ID_KEY, SVY_BACKGROUND);
					name.setText(dataTutorialsList.getJSONObject(i).optString(hasDivider ? "divider" : "title"));
					name.setCursor(new Cursor(mainContainer.getDisplay(), SWT.CURSOR_HAND));
					FontDescriptor descriptor = FontDescriptor.createFrom(name.getFont());
					descriptor = descriptor.setStyle(hasDivider ? SWT.BOLD : SWT.ITALIC);
					descriptor = descriptor.increaseHeight(hasDivider ? 7 : 5);
					name.setFont(descriptor.createFont(mainContainer.getDisplay()));

					Label description = new Label(row, SWT.WRAP);
					description.setData(SWT_CSS_ID_KEY, SVY_BACKGROUND);
					description.setText(removeHTMLTags(dataTutorialsList.getJSONObject(i).optString("description")));
					GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
					gd.widthHint = widthHint - 70;
					description.setLayoutData(gd);
					descriptor = FontDescriptor.createFrom(description.getFont());
					descriptor = descriptor.increaseHeight(2);
					description.setFont(descriptor.createFont(mainContainer.getDisplay()));

					if (!hasDivider)
					{
						final String tutorialID = dataTutorialsList.getJSONObject(i).optString("id");
						Composite buttonsComposite = new Composite(row, SWT.FILL);
						buttonsComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
						buttonsComposite.setData(SWT_CSS_ID_KEY, SVY_BACKGROUND);
						GridLayout buttonsLayout = new GridLayout();
						buttonsLayout.numColumns = 3;
						buttonsLayout.marginTop = -2;
						buttonsComposite.setLayout(buttonsLayout);

						StyledText openLink = createDefaultColorsStyledText(buttonsComposite, "open tutorial");
						openLink.setCursor(new Cursor(mainContainer.getDisplay(), SWT.CURSOR_HAND));
						descriptor = FontDescriptor.createFrom(openLink.getFont());
						openLink.setFont(descriptor.createFont(mainContainer.getDisplay()));
						openLink.addMouseListener(new MouseAdapter()
						{
							@Override
							public void mouseUp(MouseEvent event)
							{
								super.mouseUp(event);

								if (event.getSource() instanceof StyledText)
								{
									String url = "https://tutorials.servoy.com/servoy-service/rest_ws/contentAPI/tutorial/" + tutorialID;
									loadDataModel(false, url);
									createTutorialView(firstParent, false);
								}
							}
						});

						createDefaultColorsStyledText(buttonsComposite, " / ");

						StyledText watchVideo = createDefaultColorsStyledText(buttonsComposite, "watch video");
						watchVideo.setCursor(new Cursor(mainContainer.getDisplay(), SWT.CURSOR_HAND));
						descriptor = FontDescriptor.createFrom(watchVideo.getFont());
						watchVideo.setFont(descriptor.createFont(mainContainer.getDisplay()));
						watchVideo.addMouseListener(new MouseAdapter()
						{
							@Override
							public void mouseUp(MouseEvent event)
							{
								super.mouseUp(event);

								if (event.getSource() instanceof StyledText)
								{
									String loginToken = getLoginToken();
									if (loginToken != null)
									{
										BrowserDialog tutorialDialog = new BrowserDialog(PlatformUI.getWorkbench().getDisplay().getActiveShell(),
											Activator.TUTORIALS_URL + loginToken + "&viewTutorial=" + tutorialID, true, false);
										tutorialDialog.open(true);
									}
								}
							}
						});
					}
				}
			}
		}
	}

	/**
	 * Buttons used for opening the videos for the current and next tutorial.
	 */
	private class TutorialButtons extends Composite
	{
		public TutorialButtons(Composite mainContainer, Composite fistParent)
		{
			super(mainContainer, SWT.FILL);
			GridLayout layout = new GridLayout();
			layout.numColumns = 1;
			layout.verticalSpacing = 1;
			layout.marginHeight = 1;
			setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			setLayout(layout);
			setData(SWT_CSS_ID_KEY, SVY_BACKGROUND);

			Composite row = new Composite(this, SWT.FILL);
			row.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			row.setData(SWT_CSS_ID_KEY, SVY_BACKGROUND);

			GridLayout rowLayout = new GridLayout();
			rowLayout.numColumns = 2;
			rowLayout.horizontalSpacing = 10;
			rowLayout.marginHeight = 1;
			rowLayout.marginTop = 10;
			row.setLayout(rowLayout);

			Button currentTutorial = new Button(row, SWT.BUTTON1);
			currentTutorial.setData(SWT_CSS_ID_KEY, SVY_BACKGROUND);
			currentTutorial.setText("Watch video");
			currentTutorial.setToolTipText("Play the video for this tutorial.");
			currentTutorial.setCursor(new Cursor(mainContainer.getDisplay(), SWT.CURSOR_HAND));
			currentTutorial.addListener(SWT.Selection, new Listener()
			{
				@Override
				public void handleEvent(Event event)
				{
					String loginToken = getLoginToken();
					if (loginToken != null)
					{
						final String currentTutorialID = dataModel.optString("tutorialID");
						BrowserDialog tutorialDialog = new BrowserDialog(PlatformUI.getWorkbench().getDisplay().getActiveShell(),
							Activator.TUTORIALS_URL + loginToken + "&viewTutorial=" + currentTutorialID, true, false);
						tutorialDialog.open(true);
					}
				}
			});

			final Object nextTutorialID = dataModel.opt("nextTutorialID");
			if (nextTutorialID instanceof Integer)
			{
				Integer id = (Integer)nextTutorialID;
				Button nextTutorial = new Button(row, SWT.BUTTON1);
				nextTutorial.setData(SWT_CSS_ID_KEY, SVY_BACKGROUND);
				nextTutorial.setText("Next tutorial");
				nextTutorial.setToolTipText("Open the next tutorial.");
				GridData gridData = new GridData();
				gridData.horizontalAlignment = GridData.END;
				nextTutorial.setLayoutData(gridData);
				nextTutorial.setCursor(new Cursor(mainContainer.getDisplay(), SWT.CURSOR_HAND));
				nextTutorial.addListener(SWT.Selection, new Listener()
				{
					@Override
					public void handleEvent(Event event)
					{
						String url = "https://tutorials.servoy.com/servoy-service/rest_ws/contentAPI/tutorial/" + id;
						loadDataModel(false, url);
						createTutorialView(fistParent, false);
					}
				});
			}
		}

	}

	private class RowComposite extends Composite
	{
		RowComposite(Composite parent, JSONObject rowData, int index, int widthHint)
		{
			super(parent, SWT.FILL);
			GridLayout layout = new GridLayout();
			layout.numColumns = 1;
			layout.verticalSpacing = 1;
			layout.marginHeight = 1;
			setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			setLayout(layout);
			setData(SWT_CSS_ID_KEY, SVY_BACKGROUND);

			Composite row = new Composite(this, SWT.FILL);
			row.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			row.setData(SWT_CSS_ID_KEY, SVY_BACKGROUND);

			GridLayout rowLayout = new GridLayout();
			rowLayout.numColumns = 1;
			rowLayout.horizontalSpacing = 10;
			rowLayout.marginHeight = 1;
			rowLayout.marginTop = 10;
			row.setLayout(rowLayout);

			Label stepName = new Label(row, SWT.NONE);
			stepName.setData(SWT_CSS_ID_KEY, SVY_BACKGROUND);
			stepName.setText(index + ". " + rowData.optString("name"));
			FontDescriptor descriptor = FontDescriptor.createFrom(stepName.getFont());
			descriptor = descriptor.setStyle(SWT.BOLD);
			descriptor = descriptor.increaseHeight(6);
			stepName.setFont(descriptor.createFont(parent.getDisplay()));

			String description = rowData.optString("description");
			// separate the code snippet from the description
			while (description != null)
			{
				int startCodeIndex = description.indexOf("<code>");
				int endCodeIndex = description.indexOf("</code>");
				if (startCodeIndex == 0)
				{
					// description starts with code snippet
					String codeSnippet = description.substring(startCodeIndex + 6, endCodeIndex);
					description = endCodeIndex + 7 == description.length() ? null : description.substring(endCodeIndex + 7);

					buildCodeSnippet(widthHint, row, codeSnippet);
				}
				else if (startCodeIndex > 0)
				{
					String text = description.substring(0, startCodeIndex);
					String codeSnippet = description.substring(startCodeIndex + 6, endCodeIndex);
					description = endCodeIndex + 7 == description.length() ? null : description.substring(endCodeIndex + 7);

					buildDescriptionText(parent, widthHint, row, text);
					buildCodeSnippet(widthHint, row, codeSnippet);

				}
				else
				{
					buildDescriptionText(parent, widthHint, row, description);
					description = null;
				}

			}

			StyledText gifURL = new StyledText(row, SWT.NONE);
			gifURL.setData(SWT_CSS_ID_KEY, SVY_BACKGROUND);
			gifURL.setEditable(false);
			gifURL.setText("< show");
			StyleRange styleRange = getDefaultColorStyleRange(0, 6, true);
			styleRange.underline = true;
			//gifURL.setStyleRange(styleRange);
			gifURL.setCursor(new Cursor(parent.getDisplay(), SWT.CURSOR_HAND));
			descriptor = FontDescriptor.createFrom(gifURL.getFont());
			descriptor = descriptor.setHeight(10);
			gifURL.setFont(descriptor.createFont(parent.getDisplay()));
			gifURL.addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseUp(MouseEvent e)
				{
					Point cursorLocation = Display.getCurrent().getCursorLocation();
					Dimension size = null;
					try
					{
						ImageDescriptor desc = ImageDescriptor.createFromURL(new URL(rowData.optString("gifURL")));
						ImageData imgData = desc.getImageData(100);
						if (imgData != null)
						{
							if (Util.isMac())
							{
								size = new Dimension(imgData.width, imgData.height);
							}
							else
							{
								Point dpi = Display.getCurrent().getDPI();
								size = new Dimension((int)(imgData.width * (dpi.x / 90f)), (int)(imgData.height * (dpi.y / 90f)));
							}
						}
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

					Rectangle bounds = Display.getCurrent().getBounds();


					Point location = new Point(cursorLocation.x - size.width - 50, cursorLocation.y + 5);
					if ((location.y + size.height) > bounds.height)
					{
						location.y = bounds.height - size.height - 70;
					}
					if (dialog == null || dialog.isDisposed())
					{
						dialog = new BrowserDialog(parent.getShell(),
							rowData.optString("gifURL"), false, false);
						dialog.open(location, size, false);
					}
					else
					{
						dialog.setUrl(rowData.optString("gifURL"));
						dialog.setLocationAndSize(location, size);
					}
				}
			});
		}

		/**
		 * Builds the text description. It has support for bold, italic, links, bullet lists.
		 */
		private void buildDescriptionText(Composite parent, int widthHint, Composite row, String text)
		{
			ArrayList<Integer> boldStartIndex = new ArrayList<>();
			ArrayList<Integer> boldEndIndex = new ArrayList<>();
			ArrayList<Integer> italicStartIndex = new ArrayList<>();
			ArrayList<Integer> italicEndIndex = new ArrayList<>();
			ArrayList<Integer> listStartIndex = new ArrayList<>();
			ArrayList<Integer> listEndIndex = new ArrayList<>();
			Map<Integer, String> linkStartIndex = new LinkedHashMap<Integer, String>();
			ArrayList<Integer> linkEndIndex = new ArrayList<>();


			int numberOfHtmlChars = 0; // the number of html characters to be deleted from description
			for (int i = 0, n = text.length(); i < n; i++)
			{
				if (text.charAt(i) == '<')
				{
					boolean isOpeningTag = (text.charAt(i + 1) != '/');
					char nextChar = isOpeningTag ? text.charAt(i + 1) : text.charAt(i + 2);
					switch (nextChar)
					{
						// bold
						case 'b' :
						{
							if (isOpeningTag)
							{
								boldStartIndex.add(Integer.valueOf(i - numberOfHtmlChars));
								boldEndIndex.add(Integer.valueOf(text.substring(i + 3, text.indexOf("</b>", i + 3)).length()));
							}
							break;
						}
						// italic
						case 'i' :
						{
							if (isOpeningTag)
							{
								italicStartIndex.add(Integer.valueOf(i - numberOfHtmlChars));
								italicEndIndex.add(Integer.valueOf(text.substring(i + 3, text.indexOf("</i>", i + 3)).length()));
							}
							break;
						}
						// list
						case 'l' :
						{
							if (isOpeningTag)
							{
								listStartIndex.add(Integer.valueOf((text.substring(0, i)).split("\n").length));
								listEndIndex.add(Integer.valueOf((text.substring(i + 1, text.indexOf("</l>", i + 1)).split("\n").length)));
							}
							break;
						}
						// link
						case 'a' :
						{
							if (isOpeningTag)
							{
								// save the starting point of the link and also the URL
								String href = text.substring(i, text.indexOf(">", i + 1));
								int firstQuote = href.indexOf("'");
								href = href.substring(firstQuote + 1, href.indexOf("'", firstQuote + 1));
								linkStartIndex.put(Integer.valueOf(i - numberOfHtmlChars), href);
								linkEndIndex.add(Integer.valueOf(text.substring(text.indexOf(">", i + 1) + 1, text.indexOf("</a>", i + 1)).length()));
								numberOfHtmlChars = numberOfHtmlChars + (text.substring(i, text.indexOf(">", i + 1) + 1).length());
							}
							else
							{
								numberOfHtmlChars = numberOfHtmlChars + 4;
							}
							break;
						}
					}
					if (nextChar != 'a') numberOfHtmlChars = isOpeningTag ? numberOfHtmlChars + 3 : numberOfHtmlChars + 4;
				}
			}

			StyledText styledText = new StyledText(row, SWT.WRAP | SWT.READ_ONLY);
			styledText.setText(removeHTMLTags(text));
			styledText.setData(SWT_CSS_ID_KEY, SVY_BACKGROUND);
			FontDescriptor descriptor = FontDescriptor.createFrom(styledText.getFont());
			descriptor = descriptor.increaseHeight(2);
			styledText.setFont(descriptor.createFont(parent.getDisplay()));
			GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
			gd.widthHint = widthHint - 70;
			styledText.setLayoutData(gd);

			for (int i = 0; i < boldStartIndex.size(); i++)
			{
				StyleRange styleRange = getDefaultColorStyleRange(boldStartIndex.get(i).intValue(), boldEndIndex.get(i).intValue(), false);				styleRange.fontStyle = SWT.BOLD;
				styledText.setStyleRange(styleRange);
			}

			for (int i = 0; i < italicStartIndex.size(); i++)
			{
				StyleRange styleRange = getDefaultColorStyleRange(italicStartIndex.get(i).intValue(), italicEndIndex.get(i).intValue(), false);
				styleRange.fontStyle = SWT.ITALIC;
				styledText.setStyleRange(styleRange);
			}

			for (int i = 0; i < listStartIndex.size(); i++)
			{
				StyleRange bulletStyle = new StyleRange();
				bulletStyle.metrics = new GlyphMetrics(0, 0, 40);
				bulletStyle.foreground = getDisplay().getSystemColor(SWT.COLOR_BLACK); //TODO or white..
				Bullet bullet = new Bullet(bulletStyle);
				styledText.setLineBullet(listStartIndex.get(i).intValue(), listEndIndex.get(i).intValue(), bullet);
			}

			int indexForLinkLength = 0;
			boolean mouseDownListenerAdded = false;
			for (Map.Entry<Integer, String> entry : linkStartIndex.entrySet())
			{
				StyleRange styleRange = getDefaultColorStyleRange(entry.getKey().intValue(), linkEndIndex.get(indexForLinkLength++).intValue(), false);
				styleRange.underline = true;
				styleRange.underlineStyle = SWT.UNDERLINE_LINK;
				styleRange.data = entry.getValue();
				styledText.setStyleRange(styleRange);

				// add click event only once
				if (!mouseDownListenerAdded) styledText.addListener(SWT.MouseDown, event -> {
					int offset = styledText.getOffsetAtPoint(new Point(event.x, event.y));
					if (offset != -1)
					{
						StyleRange range = null;
						try
						{
							range = styledText.getStyleRangeAtOffset(offset);
						}
						catch (IllegalArgumentException e)
						{
							e.printStackTrace();
						}
						if (range != null && range.underline && range.underlineStyle == SWT.UNDERLINE_LINK)
						{
							try
							{
								PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL((String)range.data));
							}
							catch (PartInitException | MalformedURLException e)
							{
								e.printStackTrace();
							}
						}
					}
				});
				mouseDownListenerAdded = true;
			}
		}

		/**
		 * Builds a code snippet.
		 */
		private void buildCodeSnippet(int widthHint, Composite row, String codeText)
		{
			Color color = new Color(new RGB(246, 248, 250));
			Composite codeWrapper = new Composite(row, SWT.FILL);
			codeWrapper.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			codeWrapper.setBackground(color);

			GridLayout codeLayout = new GridLayout();
			codeLayout.numColumns = 1;
			codeWrapper.setLayout(codeLayout);

			StyledText codeSnippet = new StyledText(codeWrapper, SWT.WRAP);
			codeSnippet.setText(codeText);
			codeSnippet.setBackground(color);
			GridData gdCode = new GridData(SWT.FILL, SWT.FILL, true, false);
			gdCode.widthHint = widthHint - 70;
			codeSnippet.setLayoutData(gdCode);
			int margin = 5;
			codeSnippet.setMargins(margin, margin, margin, margin);
			FontDescriptor descriptor = FontDescriptor.createFrom(codeSnippet.getFont());
			descriptor = descriptor.increaseHeight(2);
			codeSnippet.setFont(descriptor.createFont(codeWrapper.getDisplay()));
			codeSnippet.addDisposeListener(new DisposeListener()
			{
				@Override
				public void widgetDisposed(DisposeEvent e)
				{
					color.dispose();
				}
			});
		}
	}

	private void loadDataModel(boolean loadDefaultTutorialList, String url)
	{
		try (InputStream is = new URL(loadDefaultTutorialList ? URL_DEFAULT_TUTORIALS_LIST : url).openStream())
		{
			String jsonText = Utils.getTXTFileContent(is, Charset.forName("UTF-8"));
			if (loadDefaultTutorialList)
			{
				dataTutorialsList = new JSONArray(jsonText);
			}
			else
			{
				dataModel = new JSONObject(jsonText);
			}

		}
		catch (Exception e)
		{
			Debug.error(e);
		}
	}

	public void openTutorial(String url)
	{
		loadDataModel(false, url);

		Composite parent = rootComposite.getParent();
		createTutorialView(parent, false);
		parent.layout(true, true);

		isTutorialViewOpen = true;
	}

	public static boolean isTutorialViewOpen()
	{
		return isTutorialViewOpen;
	}

	@Override
	public void dispose()
	{
		isTutorialViewOpen = false;
		super.dispose();
	}

	@Override
	public void setFocus()
	{

	}

	/**
	 * Methods that removes the HTML tags from a text.
	 * @param text the text to be checked.
	 * @return the text without HTML tags.
	 */
	private String removeHTMLTags(final String text)
	{
		String copyText = text;
		while (true)
		{
			if (copyText.indexOf('<') != -1 && copyText.indexOf('>') != -1)
			{
				copyText = copyText.replace(copyText.substring(copyText.indexOf('<'), copyText.indexOf('>') + 1), "");
			}
			else
			{
				break;
			}
		}
		return copyText.trim().replaceAll(" +", " ");
	}

	private String getLoginToken()
	{
		String loginToken = ServoyLoginDialog.getLoginToken();
		if (loginToken == null)
		{
			loginToken = new ServoyLoginDialog(PlatformUI.getWorkbench().getDisplay().getActiveShell()).doLogin();
		}

		return loginToken;
	}
}
