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

import java.net.URL;

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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

import com.servoy.eclipse.model.util.ServoyLog;
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
	private Spinner gridPointSizeSpinner;
	private ColorSelectViewer gridColorViewer;
	private ColorSelectViewer alignmentGuidecolorSelectViewer;
	private Spinner guideSizeSpinner;
	private Spinner copyPasteOffsetSpinner;
	private Spinner stepSizeSpinner;
	private Spinner largeStepSizeSpinner;
	private ComboViewer metricsCombo;
	private Button snapToGridRadio;
	private Button anchorCheck;
	private Button snapToAlignmentRadio;
	private Spinner alignmentThresholdSpinner;
	private Spinner alignmentIndentSpinner;
	private Spinner alignmentSmallOffsetSpinner;
	private Spinner alignmentMediumDistanceSpinner;
	private Spinner alignmentLargeDistanceSpinner;
	private Button snapToNoneRadio;
	private Button sameSizeFeedbackCheck;
	private Button anchorFeedbackCheck;
	private Button alignmentFeedbackCheck;
	private Button gridFeedbackCheck;
	private Button paintPagebreaksCheck;
	private Button showRulersCheck;
	private Button marqueeSelectOuterCheck;
	private Button classicMobileFormeditorCheck;
	private Button classicFormeditorCheck;
	private ColorSelectViewer sameHeightWidthIndicatorColor;

	public void init(IWorkbench workbench)
	{
	}

	@Override
	protected Control createContents(Composite parent)
	{
		initializeDialogUnits(parent);

		Composite rootPanel = new Composite(parent, SWT.NONE);
		rootPanel.setLayout(new GridLayout(1, true));

		Composite optionsPanel = new Composite(rootPanel, SWT.NONE);
		optionsPanel.setLayout(new GridLayout(1, false));

		classicMobileFormeditorCheck = new Button(optionsPanel, SWT.CHECK);
		classicMobileFormeditorCheck.setText("Use classic form editor for mobile forms.");

		Link xulRunnerInfo = new Link(optionsPanel, SWT.NONE);
		xulRunnerInfo.setText("Consider installing <A>XulRunner</A> when this option is disabled, for increased compatibility.");
		xulRunnerInfo.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				try
				{
					IWorkbenchBrowserSupport support = PlatformUI.getWorkbench().getBrowserSupport();
					IWebBrowser browser = support.getExternalBrowser();
					browser.openURL(new URL(
						"https://wiki.servoy.com/display/public/DOCS/Post-Installation+Modifications#Post-InstallationModifications-InstallMozillaXulRunnerasinternalbrowser"));
				}
				catch (Exception ex)
				{
					ServoyLog.logError(ex);
				}
			}
		});

		classicFormeditorCheck = new Button(optionsPanel, SWT.CHECK);
		classicFormeditorCheck.setText("Use classic form editor for regular forms.");

		Composite copyPastePanel = new Composite(optionsPanel, SWT.NONE);
		copyPastePanel.setLayout(new GridLayout(2, false));

		new Label(copyPastePanel, SWT.NONE).setText("Copy/Paste offset");

		copyPasteOffsetSpinner = new Spinner(copyPastePanel, SWT.BORDER);
		copyPasteOffsetSpinner.setValues(0, 0, 100, 0, 1, 5);

		marqueeSelectOuterCheck = new Button(optionsPanel, SWT.CHECK);
		marqueeSelectOuterCheck.setText("Marquee selects only elements fully in lasso");

		showRulersCheck = new Button(optionsPanel, SWT.CHECK);
		showRulersCheck.setText("Show rulers");
		GridData metricsPanelGridData = new GridData(SWT.LEFT, SWT.CENTER, true, true);

		Composite metricsComboPanel = new Composite(optionsPanel, SWT.NONE);
		metricsComboPanel.setLayout(new GridLayout(2, false));
		metricsComboPanel.setLayoutData(metricsPanelGridData);

		new Label(metricsComboPanel, SWT.NONE).setText("Ruler Metrics");

		metricsCombo = new ComboViewer(metricsComboPanel);
		metricsCombo.setContentProvider(new ArrayContentProvider());
		metricsCombo.setLabelProvider(new LabelProvider());
		metricsCombo.setInput(new ObjectWrapper[] { new ObjectWrapper("pixels", Integer.valueOf(DesignerPreferences.PX)), new ObjectWrapper("centimeters",
			Integer.valueOf(DesignerPreferences.CM)), new ObjectWrapper("inches", Integer.valueOf(DesignerPreferences.IN)) });

		Composite settingsPanel = new Composite(rootPanel, SWT.NONE);
		settingsPanel.setLayout(new GridLayout(2, false));

		Group grpFeedbackSettings = new Group(settingsPanel, SWT.NONE);
		grpFeedbackSettings.setText("Feedback Settings");
		grpFeedbackSettings.setLayout(new GridLayout(1, false));
		grpFeedbackSettings.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));

		gridFeedbackCheck = new Button(grpFeedbackSettings, SWT.CHECK);
		gridFeedbackCheck.setText("Grid Guides");

		Composite gridColorPanel = new Composite(grpFeedbackSettings, SWT.NONE);
		gridColorPanel.setLayout(new GridLayout(2, false));

		gridColorViewer = new ColorSelectViewer(gridColorPanel, SWT.NONE);

		new Label(gridColorPanel, SWT.NONE).setText("Grid color");

		Composite gridPointSizePanel = new Composite(grpFeedbackSettings, SWT.NONE);
		gridPointSizePanel.setLayout(new GridLayout(2, false));

		gridPointSizeSpinner = new Spinner(gridPointSizePanel, SWT.BORDER);

		new Label(gridPointSizePanel, SWT.NONE).setText("Grid point size");

		Composite gridPointDistancePanel = new Composite(grpFeedbackSettings, SWT.NONE);
		gridPointDistancePanel.setLayout(new GridLayout(2, false));

		gridSizeSpinner = new Spinner(gridPointDistancePanel, SWT.BORDER);
		gridSizeSpinner.setValues(0, 3, 100, 0, 5, 20);

		new Label(gridPointDistancePanel, SWT.NONE).setText("Point distance");

		alignmentFeedbackCheck = new Button(grpFeedbackSettings, SWT.CHECK);
		alignmentFeedbackCheck.setText("Alignment Guides");

		Composite alignementGuideColorPanel = new Composite(grpFeedbackSettings, SWT.NONE);
		alignementGuideColorPanel.setLayout(new GridLayout(2, false));

		alignmentGuidecolorSelectViewer = new ColorSelectViewer(alignementGuideColorPanel, 0);

		new Label(alignementGuideColorPanel, SWT.NONE).setText("Guide color");

		anchorFeedbackCheck = new Button(grpFeedbackSettings, SWT.CHECK);
		anchorFeedbackCheck.setText("Show anchoring feedback");

		sameSizeFeedbackCheck = new Button(grpFeedbackSettings, SWT.CHECK);
		sameSizeFeedbackCheck.setText("Show same-size feedback");
		sameSizeFeedbackCheck.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				sameHeightWidthIndicatorColor.setEnabled(sameSizeFeedbackCheck.getSelection());
			}
		});

		Composite sameHeightWidthIndicatorPanel = new Composite(grpFeedbackSettings, SWT.NONE);
		sameHeightWidthIndicatorPanel.setLayout(new GridLayout(2, false));

		sameHeightWidthIndicatorColor = new ColorSelectViewer(sameHeightWidthIndicatorPanel, SWT.NONE);

		new Label(sameHeightWidthIndicatorPanel, SWT.NONE).setText("Same height/width indicator");

		paintPagebreaksCheck = new Button(grpFeedbackSettings, SWT.CHECK);
		paintPagebreaksCheck.setText("Paint page breaks");

		Composite guideKeyPanel = new Composite(settingsPanel, SWT.NONE);
		guideKeyPanel.setLayout(new GridLayout(1, true));

		Group grpAlignmentSettings = new Group(guideKeyPanel, SWT.NONE);
		grpAlignmentSettings.setText("Guide Settings");
		grpAlignmentSettings.setLayout(new GridLayout(1, false));
		grpAlignmentSettings.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));

		snapToNoneRadio = new Button(grpAlignmentSettings, SWT.RADIO);
		snapToNoneRadio.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				setEnabledState();
			}
		});
		snapToNoneRadio.setText("None");

		snapToGridRadio = new Button(grpAlignmentSettings, SWT.RADIO);
		snapToGridRadio.setText("Grid Guides");
		snapToGridRadio.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				setEnabledState();
			}
		});

		Composite guideSizePanel = new Composite(grpAlignmentSettings, SWT.NONE);
		guideSizePanel.setLayout(new GridLayout(2, false));

		guideSizeSpinner = new Spinner(guideSizePanel, SWT.BORDER);
		guideSizeSpinner.setValues(0, 3, 100, 0, 5, 20);

		new Label(guideSizePanel, SWT.NONE).setText("Guide size");

		snapToAlignmentRadio = new Button(grpAlignmentSettings, SWT.RADIO);
		snapToAlignmentRadio.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				setEnabledState();
			}
		});
		snapToAlignmentRadio.setText("Alignment Guides");

		Composite alignementGuidesPanel = new Composite(grpAlignmentSettings, SWT.NONE);
		alignementGuidesPanel.setLayout(new GridLayout(2, false));

		Composite smallOffsetPanel = new Composite(alignementGuidesPanel, SWT.NONE);
		smallOffsetPanel.setLayout(new GridLayout(2, false));

		alignmentSmallOffsetSpinner = new Spinner(smallOffsetPanel, SWT.BORDER);

		new Label(smallOffsetPanel, SWT.NONE).setText("Small offset");

		Composite indentOffsetPanel = new Composite(alignementGuidesPanel, SWT.NONE);
		indentOffsetPanel.setLayout(new GridLayout(2, false));

		alignmentIndentSpinner = new Spinner(indentOffsetPanel, SWT.BORDER);
		alignmentIndentSpinner.setBounds(186, 151, 60, 26);

		Label indentLabel = new Label(indentOffsetPanel, SWT.NONE);
		indentLabel.setBounds(252, 158, 128, 14);
		indentLabel.setText("Indent offset");

		Composite mediumOffsetPanel = new Composite(alignementGuidesPanel, SWT.NONE);
		mediumOffsetPanel.setLayout(new GridLayout(2, false));

		alignmentMediumDistanceSpinner = new Spinner(mediumOffsetPanel, SWT.BORDER);

		Label lblMediumOffset = new Label(mediumOffsetPanel, SWT.NONE);
		lblMediumOffset.setText("Medium offset");

		Composite snapToGuidePanel = new Composite(alignementGuidesPanel, SWT.NONE);
		snapToGuidePanel.setLayout(new GridLayout(2, false));

		alignmentThresholdSpinner = new Spinner(snapToGuidePanel, SWT.BORDER);

		new Label(snapToGuidePanel, SWT.NONE).setText("Snap to Guide Threshold");

		Composite largeOffsetPanel = new Composite(alignementGuidesPanel, SWT.NONE);
		largeOffsetPanel.setLayout(new GridLayout(2, false));

		alignmentLargeDistanceSpinner = new Spinner(largeOffsetPanel, SWT.BORDER);
		alignmentLargeDistanceSpinner.setBounds(20, 204, 60, 26);

		Label lblLargeOffset = new Label(largeOffsetPanel, SWT.NONE);
		lblLargeOffset.setText("Large offset");
		lblLargeOffset.setBounds(86, 211, 108, 20);

		anchorCheck = new Button(grpAlignmentSettings, SWT.CHECK);
		anchorCheck.setBounds(19, 230, 175, 26);
		anchorCheck.setText("Enable Smart Anchoring");

		Group grpResizing = new Group(guideKeyPanel, SWT.NONE);
		grpResizing.setText("Keyboard resize/move step sizes");
		grpResizing.setLayout(new GridLayout(2, false));
		grpResizing.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));

		Composite smallStepPanel = new Composite(grpResizing, SWT.NONE);
		smallStepPanel.setLayout(new GridLayout(2, false));

		Label stepSizeLabel = new Label(smallStepPanel, SWT.NONE);
		stepSizeLabel.setText("Small step");
		stepSizeLabel.setToolTipText("Move: Ctrl-Arrows\r\nResize : Ctrl-Shift-Arrows");

		stepSizeSpinner = new Spinner(smallStepPanel, SWT.BORDER);
		stepSizeSpinner.setValues(0, 1, 100, 0, 1, 5);

		Composite largeStepPanel = new Composite(grpResizing, SWT.NONE);
		largeStepPanel.setLayout(new GridLayout(2, false));

		Label labelLargeStep = new Label(largeStepPanel, SWT.NONE);
		labelLargeStep.setToolTipText("Move: Ctrl-Alt-Arrows\r\nResize: Alt-Shift-Arrows");
		labelLargeStep.setText("Large step");

		largeStepSizeSpinner = new Spinner(largeStepPanel, SWT.BORDER);
		largeStepSizeSpinner.setValues(0, 1, 100, 0, 1, 5);

		initializeFields();
		setEnabledState();

		return rootPanel;
	}

	private void setEnabledState()
	{
		boolean state = snapToGridRadio.getSelection();
		guideSizeSpinner.setEnabled(state);

		state = snapToAlignmentRadio.getSelection();
		alignmentSmallOffsetSpinner.setEnabled(state);
		alignmentMediumDistanceSpinner.setEnabled(state);
		alignmentLargeDistanceSpinner.setEnabled(state);
		alignmentGuidecolorSelectViewer.setEnabled(state);
		alignmentThresholdSpinner.setEnabled(state);
		alignmentIndentSpinner.setEnabled(state);
		anchorCheck.setEnabled(state);
		gridColorViewer.setEnabled(state);
		gridPointSizeSpinner.setEnabled(state);
		gridSizeSpinner.setEnabled(state);
		sameHeightWidthIndicatorColor.setEnabled(sameSizeFeedbackCheck.getSelection());
	}

	protected void initializeFields()
	{
		DesignerPreferences prefs = new DesignerPreferences();

		gridPointSizeSpinner.setSelection(prefs.getGridPointSize());
		gridSizeSpinner.setSelection(prefs.getGridSize());
		gridColorViewer.setSelection(new StructuredSelection(prefs.getGridColor()));
		sameHeightWidthIndicatorColor.setSelection(new StructuredSelection(prefs.getSameHeightWidthIndicatorColor()));
		alignmentGuidecolorSelectViewer.setSelection(new StructuredSelection(prefs.getAlignmentGuideColor()));
		snapToGridRadio.setSelection(prefs.getGridSnapTo());
		snapToAlignmentRadio.setSelection(prefs.getAlignmentSnapTo());
		snapToNoneRadio.setSelection(prefs.getNoneSnapTo());
		alignmentFeedbackCheck.setSelection(prefs.getFeedbackAlignment());
		gridFeedbackCheck.setSelection(prefs.getFeedbackGrid());
		guideSizeSpinner.setSelection(prefs.getGuideSize());
		copyPasteOffsetSpinner.setSelection(prefs.getCopyPasteOffset());
		alignmentThresholdSpinner.setSelection(prefs.getAlignmentThreshold());
		alignmentIndentSpinner.setSelection(prefs.getAlignmentIndent());
		anchorCheck.setSelection(prefs.getAnchor());
		int[] distances = prefs.getAlignmentDistances();
		alignmentSmallOffsetSpinner.setSelection(distances[0]);
		alignmentMediumDistanceSpinner.setSelection(distances[1]);
		alignmentLargeDistanceSpinner.setSelection(distances[2]);
		stepSizeSpinner.setSelection(prefs.getStepSize());
		largeStepSizeSpinner.setSelection(prefs.getLargeStepSize());
		setMetricsComboValue(prefs.getMetrics());
		sameSizeFeedbackCheck.setSelection(prefs.getShowSameSizeFeedback());
		anchorFeedbackCheck.setSelection(prefs.getShowAnchorFeedback());
		paintPagebreaksCheck.setSelection(prefs.getPaintPageBreaks());
		showRulersCheck.setSelection(prefs.getShowRulers());
		marqueeSelectOuterCheck.setSelection(prefs.getMarqueeSelectOuter());
		classicMobileFormeditorCheck.setSelection(prefs.getClassicFormEditorInMobile());
		classicFormeditorCheck.setSelection(prefs.getClassicFormEditor());
	}

	@Override
	public boolean performOk()
	{
		DesignerPreferences prefs = new DesignerPreferences();

		prefs.setGridPointSize(gridPointSizeSpinner.getSelection());
		prefs.setGridSize(gridSizeSpinner.getSelection());
		prefs.setGridColor((RGB)((IStructuredSelection)gridColorViewer.getSelection()).getFirstElement());
		prefs.setSameHeightWidthIndicatorColor((RGB)((IStructuredSelection)sameHeightWidthIndicatorColor.getSelection()).getFirstElement());
		prefs.setAlignmentGuideColor((RGB)((IStructuredSelection)alignmentGuidecolorSelectViewer.getSelection()).getFirstElement());
		prefs.setFeedbackAlignment(alignmentFeedbackCheck.getSelection());
		prefs.setFeedbackGrid(gridFeedbackCheck.getSelection());
		prefs.setSnapTo(snapToGridRadio.getSelection(), snapToAlignmentRadio.getSelection());
		prefs.setAnchor(anchorCheck.getSelection());
		prefs.setGuideSize(guideSizeSpinner.getSelection());
		prefs.setCopyPasteOffset(copyPasteOffsetSpinner.getSelection());
		prefs.setAlignmentThreshold(alignmentThresholdSpinner.getSelection());
		prefs.setAlignmentIndent(alignmentIndentSpinner.getSelection());
		prefs.setAlignmentDistances(alignmentSmallOffsetSpinner.getSelection(), alignmentMediumDistanceSpinner.getSelection(),
			alignmentLargeDistanceSpinner.getSelection());
		prefs.setStepSize(stepSizeSpinner.getSelection(), largeStepSizeSpinner.getSelection());
		prefs.setMetrics(((Integer)((ObjectWrapper)((IStructuredSelection)metricsCombo.getSelection()).getFirstElement()).getType()).intValue());
		prefs.setShowSameSizeFeedback(sameSizeFeedbackCheck.getSelection());
		prefs.setShowAnchorFeedback(anchorFeedbackCheck.getSelection());
		prefs.setPaintPageBreaks(paintPagebreaksCheck.getSelection());
		prefs.setShowRulers(showRulersCheck.getSelection());
		prefs.setMarqueeSelectOuter(marqueeSelectOuterCheck.getSelection());
		prefs.setClassicFormEditorInMobile(classicMobileFormeditorCheck.getSelection());
		prefs.setClassicFormEditor(classicFormeditorCheck.getSelection());

		prefs.save();

		return true;
	}

	@Override
	protected void performDefaults()
	{
		gridPointSizeSpinner.setSelection(DesignerPreferences.GRID_POINTSIZE_DEFAULT);
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
		copyPasteOffsetSpinner.setSelection(DesignerPreferences.COPY_PASTE_OFFSET_DEFAULT);
		alignmentThresholdSpinner.setSelection(DesignerPreferences.ALIGNMENT_THRESHOLD_DEFAULT);
		alignmentIndentSpinner.setSelection(DesignerPreferences.ALIGNMENT_INDENT_DEFAULT);
		alignmentSmallOffsetSpinner.setSelection(DesignerPreferences.ALIGNMENT_DISTANCES_DEFAULT[0]);
		alignmentMediumDistanceSpinner.setSelection(DesignerPreferences.ALIGNMENT_DISTANCES_DEFAULT[1]);
		alignmentLargeDistanceSpinner.setSelection(DesignerPreferences.ALIGNMENT_DISTANCES_DEFAULT[2]);
		stepSizeSpinner.setSelection(DesignerPreferences.STEP_SIZE_DEFAULT);
		largeStepSizeSpinner.setSelection(DesignerPreferences.LARGE_STEP_SIZE_DEFAULT);
		setMetricsComboValue(DesignerPreferences.METRICS_DEFAULT);
		sameSizeFeedbackCheck.setSelection(DesignerPreferences.SHOW_SAME_SIZE_DEFAULT);
		anchorFeedbackCheck.setSelection(DesignerPreferences.SHOW_ANCHORING_DEFAULT);
		paintPagebreaksCheck.setSelection(DesignerPreferences.PAINT_PAGEBREAKS_DEFAULT);
		showRulersCheck.setSelection(DesignerPreferences.SHOW_RULERS_DEFAULT);
		marqueeSelectOuterCheck.setSelection(DesignerPreferences.MARQUEE_SELECT_OUTER_DEFAULT);
		classicMobileFormeditorCheck.setSelection(DesignerPreferences.CLASSIC_FORM_EDITOR_MOBILE_DEFAULT);
		classicFormeditorCheck.setSelection(DesignerPreferences.CLASSIC_FORM_EDITOR_SETTING_DEFAULT);

		setEnabledState();
		super.performDefaults();
	}

	private void setMetricsComboValue(int metrics)
	{
		Integer metricsValue = Integer.valueOf(metrics);
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
