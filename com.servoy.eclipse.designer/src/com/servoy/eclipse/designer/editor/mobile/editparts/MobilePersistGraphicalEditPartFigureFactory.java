/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.eclipse.designer.editor.mobile.editparts;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JRadioButton;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicComboBoxUI;

import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;

import com.servoy.eclipse.designer.Activator;
import com.servoy.eclipse.designer.editor.IFigureFactory;
import com.servoy.eclipse.designer.editor.PersistImageFigure;
import com.servoy.eclipse.designer.editor.SetBoundsToSupportBoundsFigureListener;
import com.servoy.eclipse.designer.internal.core.IImageNotifier;
import com.servoy.eclipse.designer.internal.core.ImageDataCollector;
import com.servoy.eclipse.designer.internal.core.ImageNotifierSupport;
import com.servoy.eclipse.designer.internal.core.PersistImageNotifier;
import com.servoy.eclipse.designer.mobile.property.MobilePersistPropertySource;
import com.servoy.eclipse.ui.resource.FontResource;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.dataprocessing.CustomValueList;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportBounds;
import com.servoy.j2db.smart.dataui.DataCheckBox;
import com.servoy.j2db.smart.dataui.DataChoice;
import com.servoy.j2db.smart.dataui.DataComboBox;
import com.servoy.j2db.smart.dataui.DataField;
import com.servoy.j2db.smart.dataui.ScriptButton;
import com.servoy.j2db.util.gui.RoundedBorder;

/**
 * Factory for creating figures for persists in mobile form editor.
 * 
 * @author rgansevles
 *
 */
public class MobilePersistGraphicalEditPartFigureFactory implements IFigureFactory<PersistImageFigure>
{
	private final static Color BUTTON_BG = new Color(64, 116, 165); // TODO: use theme
	private final static Color BUTTON_BORDER = new Color(20, 80, 114); // TODO: use theme
	private final static Color TEXT_BG = new Color(240, 240, 240); // TODO: use theme
	private final static Color LABEL_FG = new Color(68, 68, 68); // TODO: use theme
	private final static Color COMBO_BG = new Color(246, 246, 246); // TODO: use theme
	private final static Color RADIOS_BG = new Color(246, 246, 246); // TODO: use theme
	private final static Color CHECKS_BG = new Color(246, 246, 246); // TODO: use theme
	private final static Color HEADER_LABEL_FG = Color.white; // TODO: use theme

	private final IApplication application;
	private final Form form;

	public MobilePersistGraphicalEditPartFigureFactory(IApplication application, Form form)
	{
		this.application = application;
		this.form = form;
	}

	public PersistImageFigure createFigure(final GraphicalEditPart editPart)
	{
		final IPersist persist = (IPersist)editPart.getModel();
		PersistImageFigure figure = new PersistImageFigure(application, persist, form)
		{
			@Override
			protected IImageNotifier createImageNotifier()
			{
				return new PersistImageNotifier(application, persist, form, this)
				{
					@Override
					protected Component createComponent()
					{
						Component component = super.createComponent();
						component.setFont(component.getFont().deriveFont(12f)); // 12 pt font

						if (component instanceof JButton || component instanceof DataComboBox || component instanceof DataField ||
							component instanceof DataChoice || component instanceof DataCheckBox)
						{
							RoundedBorder rborder = new RoundedBorder(1, 1, 1, 1, BUTTON_BORDER, BUTTON_BORDER, BUTTON_BORDER, BUTTON_BORDER);
							rborder.setRoundingRadius(new float[] { 10, 10, 10, 10 });
							((JComponent)component).setBorder(rborder);
						}

						if (component instanceof JButton)
						{
							((JButton)component).setBackground(BUTTON_BG);
							((JButton)component).setForeground(Color.white);
							component.setFont(component.getFont().deriveFont(Font.BOLD));
							if (persist instanceof AbstractBase)
							{
								String dataIcon = (String)((AbstractBase)persist).getCustomMobileProperty(MobilePersistPropertySource.DATA_ICON_PROPERTY);
								if (dataIcon != null)
								{
									ImageIcon icon = Activator.getDefault().loadImageIconFromBundle("mobile/icons-18-white-" + dataIcon + ".png");
									if (icon != null)
									{
										if (component instanceof ScriptButton)
										{
											((ScriptButton)component).setMediaOption(1);
										}
//										((JButton)component).setHorizontalTextPosition(SwingConstants.CENTER); // TODO: how to center the text and keep icon left?
										((JButton)component).setHorizontalAlignment(SwingConstants.LEFT);
										((JButton)component).setIcon(new IconWithRoundBackground(icon));
									}
								}
							}
						}
						else if (component instanceof JLabel)
						{
							((JLabel)component).setOpaque(false);
							component.setFont(component.getFont().deriveFont(Font.BOLD));
							boolean headerText = persist instanceof AbstractBase && ((AbstractBase)persist).getCustomMobileProperty("headerText") != null;
							((JLabel)component).setForeground(headerText ? HEADER_LABEL_FG : LABEL_FG);
							if (headerText)
							{
								((JLabel)component).setHorizontalAlignment(SwingConstants.CENTER);
							}
							else
							{
								Object headerSizeProp = ((AbstractBase)persist).getCustomMobileProperty(MobilePersistPropertySource.HEADER_SIZE_PROPERTY);
								int headerSize = headerSizeProp instanceof Integer ? Math.max(1, Math.min(6, ((Integer)headerSizeProp).intValue())) : 4;

								float fontsize;
								switch (headerSize)
								{
									case 1 :
										fontsize = 24f;
										break;
									case 2 :
										fontsize = 18f;
										break;
									case 3 :
										fontsize = 14f;
										break;
									case 5 :
										fontsize = 10f;
										break;
									case 6 :
										fontsize = 8f;
										break;
									default : // 4
										fontsize = 12f;
										break;
								}
								component.setFont(component.getFont().deriveFont(fontsize));
							}
						}
						else if (component instanceof DataComboBox)
						{
							((DataComboBox)component).setEditable(false);
							component.setBackground(COMBO_BG);
							((DataComboBox)component).setUI(new BasicComboBoxUI()
							{
								@Override
								protected JButton createArrowButton()
								{
									JButton button = new JButton(new IconWithRoundBackground(Activator.getDefault().loadImageIconFromBundle(
										"mobile/icons-18-white-arrow-d.png")));
									button.setBorder(null);
									button.setOpaque(false);
									return button;
								}
							});
						}
						else if (component instanceof DataField)
						{
							component.setBackground(TEXT_BG);
						}
						else if (component instanceof DataChoice &&
							(((DataChoice)component).getChoiceType() == Field.RADIOS || ((DataChoice)component).getChoiceType() == Field.CHECKS))
						{
							DataChoice dataChoice = (DataChoice)component;
							boolean horizontal = MobilePersistPropertySource.RADIO_STYLE_HORIZONTAL.equals(((AbstractBase)persist).getCustomMobileProperty(MobilePersistPropertySource.RADIO_STYLE_PROPERTY));

							component.setFont(component.getFont().deriveFont(Font.BOLD));
							if (dataChoice.getChoiceType() == Field.RADIOS)
							{
								component.setBackground(RADIOS_BG);
								((JRadioButton)dataChoice.getRendererComponent()).setIcon(Activator.getDefault().loadImageIconFromBundle(
									horizontal ? "empty_18x18.png" : "mobile/radio_off.png"));
								((JRadioButton)dataChoice.getRendererComponent()).setSelectedIcon(Activator.getDefault().loadImageIconFromBundle(
									horizontal ? "empty_18x18.png" : "mobile/radio_on.png"));
							}
							else
							{
								component.setBackground(CHECKS_BG);
								((JCheckBox)dataChoice.getRendererComponent()).setIcon(Activator.getDefault().loadImageIconFromBundle("mobile/check_off.png"));
								((JCheckBox)dataChoice.getRendererComponent()).setSelectedIcon(Activator.getDefault().loadImageIconFromBundle(
									"mobile/check_on.png"));
							}

							IValueList valueList = dataChoice.getValueList();
							if (valueList.getSize() == 0)
							{
								// some sample values
								dataChoice.setValueList(valueList = new CustomValueList(application, null, "One\nTwo\nThree", false, IColumnTypes.TEXT, null));
							}
							// select first item
							dataChoice.setValueObject(valueList.getRealElementAt(0));
							dataChoice.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
							if (!horizontal)
							{
								dataChoice.getEnclosedComponent().setVisibleRowCount(valueList.getSize());
								dataChoice.getEnclosedComponent().setLayoutOrientation(JList.VERTICAL);
							}
							Dimension compPrefsize = component.getPreferredSize();
							setPreferredSize(compPrefsize.width, compPrefsize.height);
						}
						else if (component instanceof DataCheckBox)
						{
							component.setBackground(CHECKS_BG);
							((DataCheckBox)component).setIcon(Activator.getDefault().loadImageIconFromBundle("mobile/check_on.png"));
						}

						return component;
					}

					@Override
					protected ImageDataCollector createImageDataCollector(ImageNotifierSupport imageNotifierSupport)
					{
						return new ImageDataCollector(imageNotifierSupport)
						{
							@Override
							protected void setGraphicsClip(Graphics graphics, Component component, int width, int height)
							{
								if (graphics instanceof Graphics2D && component instanceof JLabel && ((JLabel)component).getComponentCount() == 1)
								{
									Component comp = ((JLabel)component).getComponent(0);
									if (comp instanceof JComponent)
									{
										Border compBorder = ((JComponent)comp).getBorder();
										if (compBorder instanceof RoundedBorder)
										{
											graphics.setClip(((RoundedBorder)compBorder).createRoundedShape(width, height));
											return;
										}
									}
								}

								super.setGraphicsClip(graphics, component, width, height);
							}
						};
					}
				};
			}

			@Override
			protected org.eclipse.swt.graphics.Font createDrawnameFont()
			{
				return FontResource.getDefaultFont(SWT.NORMAL, 1);
			}

			@Override
			public void setPreferredSize(org.eclipse.draw2d.geometry.Dimension size)
			{
				if (prefSize == null || !prefSize.equals(size))
				{
					prefSize = size;
					Display.getDefault().asyncExec(new Runnable()
					{
						public void run()
						{
							revalidate();
						}
					});
				}
			}

			@Override
			public org.eclipse.draw2d.geometry.Dimension getPreferredSize(int wHint, int hHint)
			{
				return prefSize != null ? prefSize : super.getPreferredSize(wHint, hHint);
			}
		};

		figure.addFigureListener(new SetBoundsToSupportBoundsFigureListener((ISupportBounds)editPart.getModel(), false));
		return figure;
	}
}
