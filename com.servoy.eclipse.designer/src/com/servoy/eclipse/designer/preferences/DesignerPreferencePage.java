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
package com.servoy.eclipse.designer.preferences;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.servoy.eclipse.ui.preferences.DesignerPreferences;
import com.servoy.eclipse.ui.resource.ColorResource;
import com.servoy.eclipse.ui.views.ColorSelectViewer;
import com.servoy.j2db.util.ObjectWrapper;
import com.servoy.j2db.util.PersistHelper;

/**
 * Preferences page for designer settings.
 * 
 * @author rgansevles
 * 
 */
public class DesignerPreferencePage extends PreferencePage implements IWorkbenchPreferencePage
{

	public static final String DESIGNER_PREFERENCES_ID = "com.servoy.eclipse.ui.designer";

	public DesignerPreferencePage()
	{
	}

	private Spinner gridSizeSpinner;
	private Spinner gridPointsizeSpinner;
	private ColorSelectViewer gridColorViewer;
	private ColorSelectViewer alignmentGuidecolorSelectViewer;
	private Spinner guideSizeSpinner;
	private Spinner copyPasteOffsetSpinner;
	private Spinner stepSizeSpinner;
	private Spinner largeStepSizeSpinner;
	private ComboViewer metricsCombo;
	private Button snapToGridRadio;
	private Button anchorCheck;
	private Button toolbarsInFormWindowButton;
	private Button snapToAlignmentRadio;
	private Spinner alignmentThresholdSpinner;
	private Spinner alignmentIndentSpinner;
	private Spinner alignmentSmallDistanceSpinner;
	private Spinner alignmentMediumDistanceSpinner;
	private Spinner alignmentLargeDistanceSpinner;
	private Button snapToNoneRadio;
	private Button sameSizeFeedbackCheck;
	private Button anchorFeedbackCheck;
	private Button alignmentFeedbackCheck;
	private Button gridFeedbackCheck;
	private Button paintPagebreaksCheck;
	private Button showRulersCheck;
	private ColorSelectViewer sameHeightWidthIndicatorColor;

	public void init(IWorkbench workbench)
	{
	}

	@Override
	protected Control createContents(Composite parent)
	{
		initializeDialogUnits(parent);

		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(null);


		metricsCombo = new ComboViewer(composite);
		Combo combo = metricsCombo.getCombo();
		combo.setBounds(195, 56, 125, 23);
		metricsCombo.setContentProvider(new ArrayContentProvider());
		metricsCombo.setLabelProvider(new LabelProvider());
		metricsCombo.setInput(new ObjectWrapper[] { new ObjectWrapper("pixels", new Integer(DesignerPreferences.PX)), new ObjectWrapper("centimeters",
			new Integer(DesignerPreferences.CM)), new ObjectWrapper("inches", new Integer(DesignerPreferences.IN)) });

		Label copypasteOffsetLabel = new Label(composite, SWT.NONE);
		copypasteOffsetLabel.setText("Copy/Paste offset");
		copypasteOffsetLabel.setBounds(0, 32, 112, 20);

		Label metricsLabel = new Label(composite, SWT.NONE);
		metricsLabel.setText("Ruler Metrics");
		metricsLabel.setBounds(109, 59, 80, 20);

		toolbarsInFormWindowButton = new Button(composite, SWT.CHECK);
		toolbarsInFormWindowButton.setBounds(0, 0, 343, 26);
		toolbarsInFormWindowButton.setText("Show Form Editing Toolbars inside Form Editor");

		Button resetToolbarsButton = new Button(composite, SWT.NONE);
		resetToolbarsButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				new DesignerPreferences().saveCoolbarLayout(null);
			}
		});
		resetToolbarsButton.setBounds(334, 2, 83, 23);
		resetToolbarsButton.setText("Show all");

		copyPasteOffsetSpinner = new Spinner(composite, SWT.BORDER);
		copyPasteOffsetSpinner.setValues(0, 0, 100, 0, 1, 5);
		copyPasteOffsetSpinner.setBounds(114, 29, 125, 26);

		Group grpAlignmentSettings = new Group(composite, SWT.NONE);
		grpAlignmentSettings.setText("Guide Settings");
		grpAlignmentSettings.setBounds(0, 424, 431, 266);

		snapToGridRadio = new Button(grpAlignmentSettings, SWT.RADIO);
		snapToGridRadio.setBounds(10, 50, 108, 26);
		snapToGridRadio.setText("Grid Guides");

		snapToAlignmentRadio = new Button(grpAlignmentSettings, SWT.RADIO);
		snapToAlignmentRadio.setBounds(10, 114, 137, 26);
		snapToAlignmentRadio.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				setEnabledState();
			}
		});
		snapToAlignmentRadio.setText("Alignment Guides");

		anchorCheck = new Button(grpAlignmentSettings, SWT.CHECK);
		anchorCheck.setBounds(19, 230, 175, 26);
		anchorCheck.setText("Enable Smart Anchoring");

		Label lblOffsets = new Label(grpAlignmentSettings, SWT.NONE);
		lblOffsets.setBounds(86, 158, 88, 20);
		lblOffsets.setText("Small offset");

		alignmentSmallDistanceSpinner = new Spinner(grpAlignmentSettings, SWT.BORDER);
		alignmentSmallDistanceSpinner.setBounds(20, 151, 60, 26);

		alignmentMediumDistanceSpinner = new Spinner(grpAlignmentSettings, SWT.BORDER);
		alignmentMediumDistanceSpinner.setBounds(20, 177, 60, 26);

		alignmentLargeDistanceSpinner = new Spinner(grpAlignmentSettings, SWT.BORDER);
		alignmentLargeDistanceSpinner.setBounds(20, 204, 60, 26);

		Label lblMediumOffset = new Label(grpAlignmentSettings, SWT.NONE);
		lblMediumOffset.setText("Medium offset");
		lblMediumOffset.setBounds(86, 184, 94, 30);

		Label lblLargeOffset = new Label(grpAlignmentSettings, SWT.NONE);
		lblLargeOffset.setText("Large offset");
		lblLargeOffset.setBounds(86, 211, 108, 20);

		guideSizeSpinner = new Spinner(grpAlignmentSettings, SWT.BORDER);
		guideSizeSpinner.setBounds(20, 82, 60, 26);

		Label guideSizeLabel = new Label(grpAlignmentSettings, SWT.NONE);
		guideSizeLabel.setBounds(86, 88, 88, 26);
		guideSizeLabel.setText("Guide size");

		alignmentThresholdSpinner = new Spinner(grpAlignmentSettings, SWT.BORDER);
		alignmentThresholdSpinner.setBounds(186, 177, 60, 26);

		Label lblThreshold = new Label(grpAlignmentSettings, SWT.NONE);
		lblThreshold.setBounds(252, 183, 169, 20);
		lblThreshold.setText("Snap to Guide Threshold");

		snapToNoneRadio = new Button(grpAlignmentSettings, SWT.RADIO);
		snapToNoneRadio.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				setEnabledState();
			}
		});
		snapToNoneRadio.setBounds(10, 21, 90, 23);
		snapToNoneRadio.setText("None");

		alignmentIndentSpinner = new Spinner(grpAlignmentSettings, SWT.BORDER);
		alignmentIndentSpinner.setBounds(186, 151, 60, 26);

		Label indentLabel = new Label(grpAlignmentSettings, SWT.NONE);
		indentLabel.setBounds(252, 158, 128, 14);
		indentLabel.setText("Indent offset");
		guideSizeSpinner.setValues(0, 3, 100, 0, 5, 20);

		Group grpResizing = new Group(composite, SWT.NONE);
		grpResizing.setText("Keyboard resize/move step sizes");
		grpResizing.setBounds(0, 696, 431, 93);

		Label stepsizeLabel = new Label(grpResizing, SWT.NONE);
		stepsizeLabel.setBounds(10, 29, 65, 20);
		stepsizeLabel.setText("Small step");
		stepsizeLabel.setToolTipText("Move: Ctrl-Arrows\r\nResize : Ctrl-Shift-Arrows");

		stepSizeSpinner = new Spinner(grpResizing, SWT.BORDER);
		stepSizeSpinner.setBounds(80, 21, 60, 26);
		stepSizeSpinner.setValues(0, 1, 100, 0, 1, 5);

		Label lblLargeStep = new Label(grpResizing, SWT.NONE);
		lblLargeStep.setToolTipText("Move: Ctrl-Alt-Arrows\r\nResize: Alt-Shift-Arrows");
		lblLargeStep.setText("Large step");
		lblLargeStep.setBounds(10, 61, 65, 20);

		largeStepSizeSpinner = new Spinner(grpResizing, SWT.BORDER);
		largeStepSizeSpinner.setBounds(80, 53, 60, 26);
		largeStepSizeSpinner.setValues(0, 1, 100, 0, 1, 5);

		Group grpFeedbackSettings = new Group(composite, SWT.NONE);
		grpFeedbackSettings.setText("Feedback Settings");
		grpFeedbackSettings.setBounds(0, 82, 430, 342);

		alignmentFeedbackCheck = new Button(grpFeedbackSettings, SWT.CHECK);
		alignmentFeedbackCheck.setBounds(10, 145, 303, 26);
		alignmentFeedbackCheck.setText("Alignment Guides");

		Label lblGuideColor = new Label(grpFeedbackSettings, SWT.NONE);
		lblGuideColor.setBounds(74, 187, 88, 20);
		lblGuideColor.setText("Guide color");

		gridFeedbackCheck = new Button(grpFeedbackSettings, SWT.CHECK);
		gridFeedbackCheck.setBounds(10, 23, 284, 26);
		gridFeedbackCheck.setText("Grid Guides");

		gridColorViewer = new ColorSelectViewer(grpFeedbackSettings, SWT.NONE);
		Control gridColorControl = gridColorViewer.getControl();
		gridColorControl.setBounds(10, 55, 60, 25);

		Label gridColorLabel = new Label(grpFeedbackSettings, SWT.NONE);
		gridColorLabel.setBounds(74, 55, 85, 20);
		gridColorLabel.setText("Grid color");

		gridPointsizeSpinner = new Spinner(grpFeedbackSettings, SWT.BORDER);
		gridPointsizeSpinner.setBounds(10, 86, 60, 26);

		Label gridPointSizeLabel = new Label(grpFeedbackSettings, SWT.NONE);
		gridPointSizeLabel.setBounds(74, 90, 124, 20);
		gridPointSizeLabel.setText("Grid point size");

		gridSizeSpinner = new Spinner(grpFeedbackSettings, SWT.BORDER);
		gridSizeSpinner.setBounds(10, 114, 60, 26);
		gridSizeSpinner.setValues(0, 3, 100, 0, 5, 20);
		snapToGridRadio.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				setEnabledState();
			}
		});

		Label gridDefaultLabel = new Label(grpFeedbackSettings, SWT.NONE);
		gridDefaultLabel.setBounds(74, 117, 155, 14);
		gridDefaultLabel.setText("Point distance");

		alignmentGuidecolorSelectViewer = new ColorSelectViewer(grpFeedbackSettings, 0);
		Control control = alignmentGuidecolorSelectViewer.getControl();
		control.setBounds(10, 177, 60, 27);

		anchorFeedbackCheck = new Button(grpFeedbackSettings, SWT.CHECK);
		anchorFeedbackCheck.setBounds(10, 203, 324, 26);
		anchorFeedbackCheck.setText("Show anchoring feedback");

		sameSizeFeedbackCheck = new Button(grpFeedbackSettings, SWT.CHECK);
		sameSizeFeedbackCheck.setBounds(10, 235, 324, 26);
		sameSizeFeedbackCheck.setText("Show same-size feedback");
		sameSizeFeedbackCheck.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				sameHeightWidthIndicatorColor.setEnabled(sameSizeFeedbackCheck.getSelection());
			}
		});

		sameHeightWidthIndicatorColor = new ColorSelectViewer(grpFeedbackSettings, SWT.NONE);
		Control sameHeightWidthIndicatorColorControl = sameHeightWidthIndicatorColor.getControl();
		sameHeightWidthIndicatorColorControl.setBounds(10, 264, 60, 25);

		Label sameHeightWidthIndicatorColorLabel = new Label(grpFeedbackSettings, SWT.NONE);
		sameHeightWidthIndicatorColorLabel.setBounds(74, 266, 190, 20);
		sameHeightWidthIndicatorColorLabel.setText("Same height/width indicator");

		paintPagebreaksCheck = new Button(grpFeedbackSettings, SWT.CHECK);
		paintPagebreaksCheck.setBounds(10, 299, 303, 26);
		paintPagebreaksCheck.setText("Paint page breaks");

		showRulersCheck = new Button(composite, SWT.CHECK);
		showRulersCheck.setBounds(0, 55, 101, 26);
		showRulersCheck.setText("Show rulers");

		initializeFields();
		setEnabledState();

		return composite;
	}

	private void setEnabledState()
	{
		boolean state = snapToGridRadio.getSelection();
		guideSizeSpinner.setEnabled(state);

		state = snapToAlignmentRadio.getSelection();
		alignmentSmallDistanceSpinner.setEnabled(state);
		alignmentMediumDistanceSpinner.setEnabled(state);
		alignmentLargeDistanceSpinner.setEnabled(state);
		alignmentGuidecolorSelectViewer.setEnabled(state);
		alignmentThresholdSpinner.setEnabled(state);
		alignmentIndentSpinner.setEnabled(state);
		anchorCheck.setEnabled(state);
		gridColorViewer.setEnabled(state);
		gridPointsizeSpinner.setEnabled(state);
		gridSizeSpinner.setEnabled(state);
		sameHeightWidthIndicatorColor.setEnabled(sameSizeFeedbackCheck.getSelection());
	}

	protected void initializeFields()
	{
		DesignerPreferences prefs = new DesignerPreferences();

		gridPointsizeSpinner.setSelection(prefs.getGridPointSize());
		gridSizeSpinner.setSelection(prefs.getGridSize());
		gridColorViewer.setSelection(new StructuredSelection(prefs.getGridColor()));
		sameHeightWidthIndicatorColor.setSelection(new StructuredSelection(prefs.getSameHeightWidthIndicatorColor()));
		alignmentGuidecolorSelectViewer.setSelection(new StructuredSelection(prefs.getAlignmentGuideColor()));
		snapToGridRadio.setSelection(prefs.getGridSnapTo());
		snapToAlignmentRadio.setSelection(prefs.getAlignmentSnapTo());
		snapToNoneRadio.setSelection(prefs.getNoneSnapTo());
		alignmentFeedbackCheck.setSelection(prefs.getFeedbackAlignment());
		gridFeedbackCheck.setSelection(prefs.getFeedbackGrid());
		toolbarsInFormWindowButton.setSelection(prefs.getFormToolsOnMainToolbar());
		guideSizeSpinner.setSelection(prefs.getGuideSize());
		copyPasteOffsetSpinner.setSelection(prefs.getCopyPasteOffset());
		alignmentThresholdSpinner.setSelection(prefs.getAlignmentThreshold());
		alignmentIndentSpinner.setSelection(prefs.getAlignmentIndent());
		anchorCheck.setSelection(prefs.getAnchor());
		int[] distances = prefs.getAlignmentDistances();
		alignmentSmallDistanceSpinner.setSelection(distances[0]);
		alignmentMediumDistanceSpinner.setSelection(distances[1]);
		alignmentLargeDistanceSpinner.setSelection(distances[2]);
		stepSizeSpinner.setSelection(prefs.getStepSize());
		largeStepSizeSpinner.setSelection(prefs.getLargeStepSize());
		setMetricsComboValue(prefs.getMetrics());
		sameSizeFeedbackCheck.setSelection(prefs.getShowSameSizeFeedback());
		anchorFeedbackCheck.setSelection(prefs.getShowAnchorFeedback());
		paintPagebreaksCheck.setSelection(prefs.getPaintPageBreaks());
		showRulersCheck.setSelection(prefs.getShowRulers());
	}

	@Override
	public boolean performOk()
	{
		DesignerPreferences prefs = new DesignerPreferences();

		prefs.setGridPointSize(gridPointsizeSpinner.getSelection());
		prefs.setGridSize(gridSizeSpinner.getSelection());
		prefs.setGridColor((RGB)((IStructuredSelection)gridColorViewer.getSelection()).getFirstElement());
		prefs.setSameHeightWidthIndicatorColor((RGB)((IStructuredSelection)sameHeightWidthIndicatorColor.getSelection()).getFirstElement());
		prefs.setAlignmentGuideColor((RGB)((IStructuredSelection)alignmentGuidecolorSelectViewer.getSelection()).getFirstElement());
		prefs.setFeedbackAlignment(alignmentFeedbackCheck.getSelection());
		prefs.setFeedbackGrid(gridFeedbackCheck.getSelection());
		prefs.setSnapTo(snapToGridRadio.getSelection(), snapToAlignmentRadio.getSelection());
		prefs.setAnchor(anchorCheck.getSelection());
		prefs.setFormToolsOnMainToolbar(toolbarsInFormWindowButton.getSelection());
		prefs.setGuideSize(guideSizeSpinner.getSelection());
		prefs.setCopyPasteOffset(copyPasteOffsetSpinner.getSelection());
		prefs.setAlignmentThreshold(alignmentThresholdSpinner.getSelection());
		prefs.setAlignmentIndent(alignmentIndentSpinner.getSelection());
		prefs.setAlignmentDistances(alignmentSmallDistanceSpinner.getSelection(), alignmentMediumDistanceSpinner.getSelection(),
			alignmentLargeDistanceSpinner.getSelection());
		prefs.setStepSize(stepSizeSpinner.getSelection(), largeStepSizeSpinner.getSelection());
		prefs.setMetrics(((Integer)((ObjectWrapper)((IStructuredSelection)metricsCombo.getSelection()).getFirstElement()).getType()).intValue());
		prefs.setShowSameSizeFeedback(sameSizeFeedbackCheck.getSelection());
		prefs.setShowAnchorFeedback(anchorFeedbackCheck.getSelection());
		prefs.setPaintPageBreaks(paintPagebreaksCheck.getSelection());
		prefs.setShowRulers(showRulersCheck.getSelection());

		prefs.save();

		return true;
	}

	@Override
	protected void performDefaults()
	{
		gridPointsizeSpinner.setSelection(DesignerPreferences.GRID_POINTSIZE_DEFAULT);
		gridSizeSpinner.setSelection(DesignerPreferences.GRID_SIZE_DEFAULT);
		gridColorViewer.setSelection(new StructuredSelection(ColorResource.ColorAwt2Rgb(PersistHelper.createColor(DesignerPreferences.GRID_COLOR_DEFAULT))));
		sameHeightWidthIndicatorColor.setSelection(new StructuredSelection(
			ColorResource.ColorAwt2Rgb(PersistHelper.createColor(DesignerPreferences.SAME_HEIGHT_WIDTH_INDICATOR_COLOR_DEFAULT))));
		alignmentGuidecolorSelectViewer.setSelection(new StructuredSelection(
			ColorResource.ColorAwt2Rgb(PersistHelper.createColor(DesignerPreferences.ALIGNMENT_GUIDE_COLOR_DEFAULT))));
		guideSizeSpinner.setSelection(DesignerPreferences.GUIDE_SIZE_DEFAULT);
		alignmentFeedbackCheck.setSelection(DesignerPreferences.FEEDBACK_ALIGNMENT_DEFAULT);
		gridFeedbackCheck.setSelection(DesignerPreferences.FEEDBACK_GRID_DEFAULT);
		snapToGridRadio.setSelection(DesignerPreferences.SNAPTO_DEFAULT.equals(DesignerPreferences.SNAP_TO_GRID));
		snapToNoneRadio.setSelection(DesignerPreferences.SNAPTO_DEFAULT.equals(DesignerPreferences.SNAP_TO_NONE));
		anchorCheck.setSelection(DesignerPreferences.ANCHOR_DEFAULT);
		snapToAlignmentRadio.setSelection(DesignerPreferences.SNAPTO_DEFAULT.equals(DesignerPreferences.SNAP_TO_ALIGMNENT));
		snapToNoneRadio.setSelection(DesignerPreferences.SNAPTO_DEFAULT.equals(DesignerPreferences.SNAP_TO_NONE));
		toolbarsInFormWindowButton.setSelection(DesignerPreferences.FORM_TOOLS_ON_MAIN_TOOLBAR_DEFAULT);
		copyPasteOffsetSpinner.setSelection(DesignerPreferences.COPY_PASTE_OFFSET_DEFAULT);
		alignmentThresholdSpinner.setSelection(DesignerPreferences.ALIGNMENT_THRESHOLD_DEFAULT);
		alignmentIndentSpinner.setSelection(DesignerPreferences.ALIGNMENT_INDENT_DEFAULT);
		alignmentSmallDistanceSpinner.setSelection(DesignerPreferences.ALIGNMENT_DISTANCES_DEFAULT[0]);
		alignmentMediumDistanceSpinner.setSelection(DesignerPreferences.ALIGNMENT_DISTANCES_DEFAULT[1]);
		alignmentLargeDistanceSpinner.setSelection(DesignerPreferences.ALIGNMENT_DISTANCES_DEFAULT[2]);
		stepSizeSpinner.setSelection(DesignerPreferences.STEP_SIZE_DEFAULT);
		largeStepSizeSpinner.setSelection(DesignerPreferences.LARGE_STEP_SIZE_DEFAULT);
		setMetricsComboValue(DesignerPreferences.METRICS_DEFAULT);
		sameSizeFeedbackCheck.setSelection(DesignerPreferences.SHOW_SAME_SIZE_DEFAULT);
		anchorFeedbackCheck.setSelection(DesignerPreferences.SHOW_ANCHORING_DEFAULT);
		paintPagebreaksCheck.setSelection(DesignerPreferences.PAINT_PAGEBREAKS_DEFAULT);
		showRulersCheck.setSelection(DesignerPreferences.SHOW_RULERS_DEFAULT);

		setEnabledState();
		super.performDefaults();
	}

	private void setMetricsComboValue(int metrics)
	{
		Integer metricsValue = new Integer(metrics);
		for (ObjectWrapper ow : (ObjectWrapper[])metricsCombo.getInput())
		{
			if (ow.getType().equals(metricsValue))
			{
				metricsCombo.setSelection(new StructuredSelection(ow));
				return;
			}
		}
	}
}
