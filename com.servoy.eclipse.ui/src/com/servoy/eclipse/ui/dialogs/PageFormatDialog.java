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
package com.servoy.eclipse.ui.dialogs;

import java.awt.print.PageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.print.attribute.EnumSyntax;
import javax.print.attribute.Size2DSyntax;
import javax.print.attribute.standard.Media;
import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.MediaSizeName;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.j2db.Messages;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.gui.PageSetupDialog.MySize2DSyntax;

public class PageFormatDialog extends Dialog
{
	public static final String UNIT_MM = "mm"; //$NON-NLS-1$
	public static final String UNIT_INCH = "inch"; //$NON-NLS-1$
	public static final String UNIT_PIXELS = "pixels"; //$NON-NLS-1$

	private final String title;
	private final PageFormat format;

	private int currentUnits = Size2DSyntax.MM;

	//page settings
	private MediaSizeName mediaSizeName = MediaSizeName.ISO_A4; //if this is null it's a custom size paper
	private Size2DSyntax printingPageSize = null;
	private MediaMargins mediaMargins = null; //this defines the margin
	private int orientation;

	private PaperComposite paperComposite;
	private OrientationComposite orientationComposite;
	private MarginsComposite marginsComposite;

	private Combo unitCombo;
	private Button defaultButton;
	private boolean defaultPressed = false;

	public PageFormatDialog(Shell shell, Object value, String title)
	{
		super(shell);
		if (value instanceof PageFormat)
		{
			this.format = (PageFormat)value;
		}
		else
		{
			this.format = new PageFormat();
		}

		this.title = title;
		setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);

		String defaultCountry = Locale.getDefault().getCountry();
		if (defaultCountry != null &&
			(defaultCountry.equals("") //$NON-NLS-1$
				||
				defaultCountry.equals(Locale.US.getCountry()) || defaultCountry.equals(Locale.CANADA.getCountry()) || defaultCountry.equals(Locale.UK.getCountry())))
		{
			currentUnits = Size2DSyntax.INCH;
		}
		else
		{
			currentUnits = Size2DSyntax.MM;
		}


	}

	@Override
	protected Control createDialogArea(Composite parent)
	{
		getShell().setText(title);

		Composite composite = (Composite)super.createDialogArea(parent);

		paperComposite = new PaperComposite(composite, SWT.NONE);
		orientationComposite = new OrientationComposite(composite, SWT.NONE);
		marginsComposite = new MarginsComposite(composite, SWT.NONE);

		final GroupLayout groupLayout = new GroupLayout(composite);

		composite.setLayout(groupLayout);
		groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(GroupLayout.TRAILING).add(
			groupLayout.createSequentialGroup().addContainerGap().add(
				groupLayout.createParallelGroup(GroupLayout.LEADING).add(paperComposite, GroupLayout.PREFERRED_SIZE, 400, GroupLayout.PREFERRED_SIZE).add(
					groupLayout.createSequentialGroup().add(orientationComposite, GroupLayout.PREFERRED_SIZE, 198, GroupLayout.PREFERRED_SIZE).addPreferredGap(
						LayoutStyle.RELATED).add(marginsComposite, GroupLayout.PREFERRED_SIZE, 198, GroupLayout.PREFERRED_SIZE))).addContainerGap()));
		groupLayout.setVerticalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			groupLayout.createSequentialGroup().addContainerGap().add(groupLayout.createParallelGroup(GroupLayout.BASELINE).add(paperComposite)).addPreferredGap(
				LayoutStyle.RELATED).add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(orientationComposite, GroupLayout.PREFERRED_SIZE, 165, GroupLayout.PREFERRED_SIZE).add(
					marginsComposite, GroupLayout.PREFERRED_SIZE, 165, GroupLayout.PREFERRED_SIZE)).addContainerGap()));
		composite.setLayout(groupLayout);

		setValues();
		return composite;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent)
	{
		super.createButtonsForButtonBar(parent);
		defaultButton = super.createButton(parent, 3434, Messages.getString("servoy.button.default"), false);
		defaultButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				defaultPressed = true;
				PageFormatDialog.this.okPressed();
			}
		});

		((GridLayout)parent.getLayout()).numColumns++;
		unitCombo = new Combo(parent, SWT.BORDER | SWT.READ_ONLY);
		unitCombo.setItems(new String[] { UNIT_MM, UNIT_INCH, UNIT_PIXELS });

		if (currentUnits == Size2DSyntax.MM)
		{
			unitCombo.select(0);
		}
		else if (currentUnits == Size2DSyntax.INCH)
		{
			unitCombo.select(1);
		}
		else
		{
			unitCombo.select(2);
		}

		unitCombo.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				String item = unitCombo.getText();
				int unit = Size2DSyntax.INCH;
				if (item.equals(UNIT_INCH))
				{
					unit = Size2DSyntax.INCH;
				}
				if (item.equals(UNIT_MM))
				{
					unit = Size2DSyntax.MM;
				}
				if (item.equals(UNIT_PIXELS))
				{
					unit = (int)(Size2DSyntax.INCH / Utils.PPI);
				}
				currentUnits = unit;
				paperComposite.updateDimensions();
				marginsComposite.updateUnits(unit);

			}
		});

		GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		//int widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
		Point minSize = unitCombo.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
		data.widthHint = minSize.x;
		unitCombo.setLayoutData(data);
	}

	public void setValues()
	{
		orientation = format.getOrientation();

		float w = (float)format.getPaper().getWidth() / Utils.PPI;
		float h = (float)format.getPaper().getHeight() / Utils.PPI;

		mediaSizeName = null;
		if (w > 0 && h > 0) mediaSizeName = MediaSize.findMedia(w, h, Size2DSyntax.INCH);
		if (mediaSizeName != null && softEqual(MediaSize.getMediaSizeForName(mediaSizeName), w, h))
		{
			setPaperSize(MediaSize.getMediaSizeForName(mediaSizeName));
		}
		else
		{
			setPaperSize(w, h, Size2DSyntax.INCH);
			mediaSizeName = null; // custom size
		}

		// Note: PageFormat get-methods take into account the orientation.
		double x = format.getImageableX();
		double y = format.getImageableY();
		double iw = format.getImageableWidth();
		double ih = format.getImageableHeight();

		float lm = (float)x / Utils.PPI;
		float rm = (float)((format.getWidth() - (x + iw)) / Utils.PPI);
		float tm = (float)y / Utils.PPI;
		float bm = (float)((format.getHeight() - (y + ih)) / Utils.PPI);
		mediaMargins = new MediaMargins(lm, rm, tm, bm, Size2DSyntax.INCH).convertToUnit(currentUnits);

		paperComposite.updateInfo();
		orientationComposite.updateInfo();
		marginsComposite.updateInfo();
	}

	public Object getValue()
	{
		if (defaultPressed) return new PageFormat();
		else
		{
			Size2DSyntax paperSize = getPaperSize();
			MediaMargins inchPageMargins = mediaMargins.convertToUnit(Size2DSyntax.INCH);
			return Utils.createPageFormat(paperSize.getX(Size2DSyntax.INCH), paperSize.getY(Size2DSyntax.INCH), inchPageMargins.getLeftMargin(),
				inchPageMargins.getRightMargin(), inchPageMargins.getTopMargin(), inchPageMargins.getBottomMargin(), orientation, Size2DSyntax.INCH);
		}
	}

	private void setOrientation(int newOrientation)
	{
		if (orientation != newOrientation)
		{
			if ((orientation == PageFormat.PORTRAIT) || (newOrientation == PageFormat.PORTRAIT)) printingPageSize = new MySize2DSyntax(printingPageSize, true); // needs flip
			orientation = newOrientation;
		}
	}

	/**
	 * @param w the paper width. (ignoring orientation)
	 * @param h the pager height. (ignoring orientation)
	 */
	private void setPaperSize(float w, float h, int currentUnits)
	{
		if (orientation == PageFormat.PORTRAIT)
		{
			printingPageSize = new MySize2DSyntax(w, h, currentUnits);
		}
		else
		{
			printingPageSize = new MySize2DSyntax(h, w, currentUnits);
		}
	}

	/**
	 * @param size the paper size. (ignoring orientation)
	 */
	private void setPaperSize(Size2DSyntax size)
	{
		printingPageSize = new MySize2DSyntax(size, orientation != PageFormat.PORTRAIT);
	}

	private Size2DSyntax getPaperSize()
	{
		Size2DSyntax d;
		if (orientation == PageFormat.PORTRAIT)
		{
			d = printingPageSize;
		}
		else
		{
			d = new MySize2DSyntax(printingPageSize, true); // flip
		}
		return d;
	}

	public static final float precision = 1e-4f;//check for up to 4 digits after the comma

	private boolean softEqual(Size2DSyntax size, float w, float h)
	{
		if (size == null) return false;

		float a = size.getX(Size2DSyntax.INCH);
		float b = w;
		float c = size.getY(Size2DSyntax.INCH);
		float d = h;
		return (a == b || (Math.abs(a - b) < precision && ((1 - precision) < a / b && a / b < (1 + precision)))) &&
			(c == d || (Math.abs(c - d) < precision && ((1 - precision) < c / d && c / d < (1 + precision))));
	}

	private static class MediaMargins
	{

		private final float leftMargin;
		private final float rightMargin;
		private final float topMargin;
		private final float bottomMargin;
		private final int unit;

		MediaMargins(float leftMargin, float rightMargin, float topMargin, float bottomMargin, int unit)
		{
			this.leftMargin = leftMargin;
			this.rightMargin = rightMargin;
			this.topMargin = topMargin;
			this.bottomMargin = bottomMargin;
			this.unit = unit;
		}

		float getBottomMargin()
		{
			return bottomMargin;
		}

		float getLeftMargin()
		{
			return leftMargin;
		}

		float getRightMargin()
		{
			return rightMargin;
		}

		float getTopMargin()
		{
			return topMargin;
		}

		int getUnit()
		{
			return unit;
		}

		static MediaMargins getMarginsMinimal(int unit)
		{
			return new MediaMargins(0f, 0f, 0f, 0f, unit);
		}

		MediaMargins convertToUnit(int newUnit)
		{
			if (newUnit == unit)
			{
				return this;
			}
			return new MediaMargins((float)Utils.convertPageFormatUnit(unit, newUnit, leftMargin), (float)Utils.convertPageFormatUnit(unit, newUnit,
				rightMargin), (float)Utils.convertPageFormatUnit(unit, newUnit, topMargin), (float)Utils.convertPageFormatUnit(unit, newUnit, bottomMargin),
				newUnit);
		}

	}
	private class PaperComposite extends Group
	{
		private final Text widthText;
		private final Text heightText;
		private final Label widthLabel;
		private final Label heightLabel;
		private final Combo sizeCombo;
		private final Map<String, MediaSizeName> sizes;

		public PaperComposite(Composite parent, int style)
		{
			super(parent, style);
			setText("Paper");

			Label sizeLabel = new Label(this, SWT.NONE);
			sizeLabel.setText("Size");

			widthLabel = new Label(this, SWT.NONE);
			widthLabel.setText("Page width");
			widthLabel.setEnabled(false);

			heightLabel = new Label(this, SWT.NONE);
			heightLabel.setText("Page height");
			heightLabel.setEnabled(false);

			sizeCombo = new Combo(this, SWT.READ_ONLY | SWT.BORDER);
			UIUtils.setDefaultVisibleItemCount(sizeCombo);
			sizes = new HashMap<String, MediaSizeName>();
			class MyMedia extends MediaSizeName
			{
				MyMedia()
				{
					super(1);
				}

				@Override
				public EnumSyntax[] getEnumValueTable()
				{
					return super.getEnumValueTable();
				}

				@Override
				public String[] getStringTable()
				{
					return super.getStringTable();
				}
			}
			MyMedia mym = new MyMedia();
			Media[] media = (Media[])mym.getEnumValueTable();
			String[] names = mym.getStringTable();
			for (int i = 0; i < media.length; i++)
			{
				Media medium = media[i];
				if (medium instanceof MediaSizeName)
				{
					sizes.put(names[i], (MediaSizeName)medium);
					sizeCombo.add(names[i]);
				}
			}
			sizeCombo.add(Messages.getString("servoy.pagesetup.list.size.custom")); //$NON-NLS-1$
			sizeCombo.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent e)
				{
					String name = sizeCombo.getText();
					if (sizes.containsKey(name))
					{
						mediaSizeName = sizes.get(name);
						setPaperSize(MediaSize.getMediaSizeForName(mediaSizeName));
						mediaMargins = MediaMargins.getMarginsMinimal(currentUnits);
					}
					else
					{
						mediaSizeName = null;
					}
					updateDimensions();
					marginsComposite.updateInfo();
				}
			});

			widthText = new Text(this, SWT.BORDER);
			widthText.setEnabled(false);
			widthText.addVerifyListener(new NumberVerifyListener());
			widthText.addModifyListener(new ModifyListener()
			{
				public void modifyText(ModifyEvent e)
				{
					printingPageSize = new MySize2DSyntax(Utils.getAsFloat(widthText.getText()), printingPageSize.getY(currentUnits), currentUnits);
				}
			});

			heightText = new Text(this, SWT.BORDER);
			heightText.setEnabled(false);
			heightText.addVerifyListener(new NumberVerifyListener());
			heightText.addModifyListener(new ModifyListener()
			{
				public void modifyText(ModifyEvent e)
				{
					printingPageSize = new MySize2DSyntax(printingPageSize.getX(currentUnits), Utils.getAsFloat(heightText.getText()), currentUnits);
				}
			});

			final GroupLayout groupLayout = new GroupLayout(this);
			groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
				groupLayout.createSequentialGroup().addContainerGap().add(
					groupLayout.createParallelGroup(GroupLayout.LEADING).add(sizeLabel).add(widthLabel).add(heightLabel)).add(15, 15, 15).add(
					groupLayout.createParallelGroup(GroupLayout.TRAILING).add(sizeCombo, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
						Short.MAX_VALUE).add(widthText, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE).add(heightText,
						GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)).addContainerGap()));
			groupLayout.setVerticalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
				groupLayout.createSequentialGroup().addContainerGap().add(
					groupLayout.createParallelGroup(GroupLayout.BASELINE).add(sizeCombo, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
						GroupLayout.PREFERRED_SIZE).add(sizeLabel)).addPreferredGap(LayoutStyle.RELATED).add(
					groupLayout.createParallelGroup(GroupLayout.BASELINE).add(widthText, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
						GroupLayout.PREFERRED_SIZE).add(widthLabel)).addPreferredGap(LayoutStyle.RELATED).add(
					groupLayout.createParallelGroup(GroupLayout.BASELINE).add(heightText, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
						GroupLayout.PREFERRED_SIZE).add(heightLabel)).addContainerGap()));
			setLayout(groupLayout);
		}

		public void updateDimensions()
		{
			boolean isCustomFormat = (mediaSizeName == null);
			heightText.setEnabled(isCustomFormat);
			widthText.setEnabled(isCustomFormat);
			widthLabel.setEnabled(isCustomFormat);
			heightLabel.setEnabled(isCustomFormat);

			heightText.setText(Float.toString(printingPageSize.getY(currentUnits)));
			widthText.setText(Float.toString(printingPageSize.getX(currentUnits)));
		}

		public void updateInfo()
		{
			if (mediaSizeName == null) sizeCombo.select(sizeCombo.getItemCount() - 1);
			else
			{
				String name = mediaSizeName.toString();
				sizeCombo.setText(name);
			}
			updateDimensions();

		}

		@Override
		protected void checkSubclass()
		{

		}
	}
	private class OrientationComposite extends Group
	{
		private final Button portraitButton;
		private final Button landscapeButton;
//		private final Button reversedPortraitButton;
		private final Button reversedLandscapeButton;

		public OrientationComposite(Composite parent, int style)
		{
			super(parent, style);
			setText("Orientation");

			portraitButton = new Button(this, SWT.RADIO);
			portraitButton.setText(Messages.getString("servoy.pagesetup.button.portrait"));
			portraitButton.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent e)
				{
					if (portraitButton.getSelection()) setOrientation(PageFormat.PORTRAIT);
					paperComposite.updateDimensions();
				}
			});

			landscapeButton = new Button(this, SWT.RADIO);
			landscapeButton.setText(Messages.getString("servoy.pagesetup.button.landscape"));
			landscapeButton.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent e)
				{
					if (landscapeButton.getSelection()) setOrientation(PageFormat.LANDSCAPE);
					paperComposite.updateDimensions();
				}
			});

//			reversedPortraitButton = new Button(this, SWT.RADIO);
//			reversedPortraitButton.setText(Messages.getString("servoy.pagesetup.button.reversedportrait"));
//			reversedPortraitButton.addSelectionListener(new SelectionAdapter()
//			{
//				@Override
//				public void widgetSelected(SelectionEvent e)
//				{
//					marginsComposite.updateInfo();
//				}
//			});

			reversedLandscapeButton = new Button(this, SWT.RADIO);
			reversedLandscapeButton.setText(Messages.getString("servoy.pagesetup.button.reversedlandscape"));
			reversedLandscapeButton.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent e)
				{
					if (reversedLandscapeButton.getSelection()) setOrientation(PageFormat.REVERSE_LANDSCAPE);
					paperComposite.updateDimensions();
				}

			});
			final GroupLayout groupLayout = new GroupLayout(this);
			groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
				groupLayout.createSequentialGroup().addContainerGap().add(
					groupLayout.createParallelGroup(GroupLayout.LEADING).add(portraitButton).add(landscapeButton)/* .add(reversedPortraitButton) */.add(
						reversedLandscapeButton)).addContainerGap()));
			groupLayout.setVerticalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
				groupLayout.createSequentialGroup().addContainerGap().add(portraitButton, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
					GroupLayout.PREFERRED_SIZE).addPreferredGap(LayoutStyle.RELATED).add(landscapeButton, GroupLayout.PREFERRED_SIZE,
					GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE).addPreferredGap(LayoutStyle.RELATED)/*
																												 * .add(reversedPortraitButton,
																												 * GroupLayout.PREFERRED_SIZE,
																												 * GroupLayout.PREFERRED_SIZE,
																												 * Short.MAX_VALUE).addPreferredGap
																												 * (LayoutStyle.RELATED)
																												 */.add(reversedLandscapeButton,
					GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE).addContainerGap()));
			setLayout(groupLayout);

			updateInfo();
		}

		public void updateInfo()
		{
			portraitButton.setEnabled(true);
			landscapeButton.setEnabled(true);
//			reversedPortraitButton.setEnabled(false);
			reversedLandscapeButton.setEnabled(true);

			portraitButton.setSelection(false);
			landscapeButton.setSelection(false);
			reversedLandscapeButton.setSelection(false);
//			reversedPortraitButton.setSelection(false);

			if (orientation == PageFormat.PORTRAIT)
			{
				portraitButton.setSelection(true);
			}
			else if (orientation == PageFormat.LANDSCAPE)
			{
				landscapeButton.setSelection(true);
			}
			else if (orientation == PageFormat.REVERSE_LANDSCAPE)
			{
				reversedLandscapeButton.setSelection(true);
			}
//			else
//			{
//				reversedPortraitButton.setSelection(true);
//			}
		}

		@Override
		protected void checkSubclass()
		{

		}
	}
	private class MarginsComposite extends Group
	{
		private final Text leftText;
		private final Text rightText;
		private final Text topText;
		private final Text bottomText;

		public MarginsComposite(Composite parent, int style)
		{
			super(parent, style);
			setText("Page Margins");

			Label leftLabel = new Label(this, SWT.NONE);
			leftLabel.setText(Messages.getString("servoy.pagesetup.label.leftmargin"));

			Label rightLabel = new Label(this, SWT.NONE);
			rightLabel.setText(Messages.getString("servoy.pagesetup.label.rigthmargin"));

			Label topLabel = new Label(this, SWT.NONE);
			topLabel.setText(Messages.getString("servoy.pagesetup.label.topmargin"));

			Label bottomLabel = new Label(this, SWT.NONE);
			bottomLabel.setText(Messages.getString("servoy.pagesetup.label.bottommargin"));

			leftText = new Text(this, SWT.BORDER);
			leftText.addVerifyListener(new NumberVerifyListener());
			leftText.addModifyListener(new ModifyListener()
			{
				public void modifyText(ModifyEvent e)
				{
					float lm = Utils.getAsFloat(leftText.getText());
					float rm = mediaMargins.getRightMargin();
					float tm = mediaMargins.getTopMargin();
					float bm = mediaMargins.getBottomMargin();

					mediaMargins = validateMargins(lm, rm, tm, bm, currentUnits);
				}
			});

			rightText = new Text(this, SWT.BORDER);
			rightText.addVerifyListener(new NumberVerifyListener());
			rightText.addModifyListener(new ModifyListener()
			{
				public void modifyText(ModifyEvent e)
				{
					float lm = mediaMargins.getLeftMargin();
					float rm = Utils.getAsFloat(rightText.getText());
					float tm = mediaMargins.getTopMargin();
					float bm = mediaMargins.getBottomMargin();

					mediaMargins = validateMargins(lm, rm, tm, bm, currentUnits);
				}
			});

			topText = new Text(this, SWT.BORDER);
			topText.addVerifyListener(new NumberVerifyListener());
			topText.addModifyListener(new ModifyListener()
			{
				public void modifyText(ModifyEvent e)
				{
					float lm = mediaMargins.getLeftMargin();
					float rm = mediaMargins.getRightMargin();
					float tm = Utils.getAsFloat(topText.getText());
					float bm = mediaMargins.getBottomMargin();

					mediaMargins = validateMargins(lm, rm, tm, bm, currentUnits);
				}
			});

			bottomText = new Text(this, SWT.BORDER);
			bottomText.addVerifyListener(new NumberVerifyListener());
			bottomText.addModifyListener(new ModifyListener()
			{
				public void modifyText(ModifyEvent e)
				{
					float lm = mediaMargins.getLeftMargin();
					float rm = mediaMargins.getRightMargin();
					float tm = mediaMargins.getTopMargin();
					float bm = Utils.getAsFloat(bottomText.getText());

					mediaMargins = validateMargins(lm, rm, tm, bm, currentUnits);
				}
			});

			final GroupLayout groupLayout = new GroupLayout(this);
			groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
				groupLayout.createSequentialGroup().addContainerGap().add(
					groupLayout.createParallelGroup(GroupLayout.LEADING).add(leftLabel).add(rightLabel).add(topLabel).add(bottomLabel)).addPreferredGap(
					LayoutStyle.RELATED).add(
					groupLayout.createParallelGroup(GroupLayout.TRAILING).add(leftText, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE).add(
						rightText, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE).add(topText, GroupLayout.PREFERRED_SIZE,
						GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE).add(bottomText, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)).addContainerGap()));
			groupLayout.setVerticalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
				groupLayout.createSequentialGroup().addContainerGap().add(
					groupLayout.createParallelGroup(GroupLayout.BASELINE).add(leftText, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
						GroupLayout.PREFERRED_SIZE).add(leftLabel)).addPreferredGap(LayoutStyle.RELATED).add(
					groupLayout.createParallelGroup(GroupLayout.BASELINE).add(rightText, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
						GroupLayout.PREFERRED_SIZE).add(rightLabel)).addPreferredGap(LayoutStyle.RELATED).add(
					groupLayout.createParallelGroup(GroupLayout.BASELINE).add(topText, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
						GroupLayout.PREFERRED_SIZE).add(topLabel)).addPreferredGap(LayoutStyle.RELATED).add(
					groupLayout.createParallelGroup(GroupLayout.BASELINE).add(bottomText, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
						GroupLayout.PREFERRED_SIZE).add(bottomLabel)).addContainerGap()));
			setLayout(groupLayout);
		}

		public void updateInfo()
		{
			leftText.setText(Float.toString(mediaMargins.getLeftMargin()));
			rightText.setText(Float.toString(mediaMargins.getRightMargin()));
			topText.setText(Float.toString(mediaMargins.getTopMargin()));
			bottomText.setText(Float.toString(mediaMargins.getBottomMargin()));
		}

		private MediaMargins validateMargins(float lm, float rm, float tm, float bm, int unit)
		{
			if (lm < 0f || rm < 0f || tm < 0f || bm < 0f || (lm + rm) >= printingPageSize.getX(unit) || (tm + bm) >= printingPageSize.getY(unit))
			{
				// no more area left to print...
				return MediaMargins.getMarginsMinimal(unit);
			}

			return new MediaMargins(lm, rm, tm, bm, unit);
		}

		public void updateUnits(int unit)
		{
			mediaMargins = mediaMargins.convertToUnit(unit);
			updateInfo();
		}

		@Override
		protected void checkSubclass()
		{

		}
	}
	private class NumberVerifyListener implements VerifyListener
	{

		public void verifyText(VerifyEvent e)
		{
			String string = e.text;
			char[] chars = new char[string.length()];
			string.getChars(0, chars.length, chars, 0);
			for (int i = 0; i < chars.length; i++)
			{
				if (chars[i] != '.' && !('0' <= chars[i] && chars[i] <= '9'))
				{
					e.doit = false;
					return;
				}
			}

		}

	}

}
