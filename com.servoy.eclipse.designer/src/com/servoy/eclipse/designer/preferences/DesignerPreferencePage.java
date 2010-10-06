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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.ui.preferences.DesignerPreferences;
import com.servoy.eclipse.ui.property.ColorPropertyController;
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
	public DesignerPreferencePage()
	{
	}

	private static final String STEP_SIZE_TOOLTIPTEXT = "Shortcuts for moving elements in Form Editor:\n" + //
		"\tMove:\n" + //
		"\t\t* 1px: Just Arrow\n" + //
		"\t\t* Small step: Ctrl-Arrows\n" + //
		"\t\t* Large step: Ctrl-Alt-Arrows\n" + //
		"\tResize:\n" + //
		"\t\t* 1px: Shift-Arrows\n" + //
		"\t\t* Small step: Ctrl-Shift-Arrows\n" + //
		"\t\t* Large step: Alt-Shift-Arrows";

	private Spinner gridSizeSpinner;
	private Spinner gridPointsizeSpinner;
	private ColorSelectViewer gridColorViewer;
	private ColorSelectViewer alignmentGuidecolorSelectViewer;
	private Spinner guideSizeSpinner;
	private Spinner copyPasteOffsetSpinner;
	private Spinner stepSizeSpinner;
	private Spinner largeStepSizeSpinner;
	private ComboViewer metricsCombo;
	private Button gridShowButton;
	private Button snapToGridCheck;
	private Button anchorCheck;
	private Button saveEditorStateButton;
	private Button toolbarsInFormWindowButton;
	private Button snapToAlignmentCheck;
	private Spinner alignmentThresholdSpinner;
	private Spinner alignmentSmallDistanceSpinner;
	private Spinner alignmentMediumDistanceSpinner;
	private Spinner alignmentLargeDistanceSpinner;


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
		metricsCombo.setContentProvider(new ArrayContentProvider());
		metricsCombo.setLabelProvider(new LabelProvider());
		metricsCombo.setInput(new ObjectWrapper[] { new ObjectWrapper("pixels", new Integer(DesignerPreferences.PX)), new ObjectWrapper("centimeters",
			new Integer(DesignerPreferences.CM)), new ObjectWrapper("inches", new Integer(DesignerPreferences.IN)) });
		Control metricsControl;
		metricsControl = metricsCombo.getControl();
		metricsControl.setBounds(109, 243, 100, 21);

		Label gridSizeLabel = new Label(composite, SWT.NONE);
		gridSizeLabel.setText("Grid");
		gridSizeLabel.setBounds(0, 13, 80, 20);

		Label gridPointSizeLabel = new Label(composite, SWT.NONE);
		gridPointSizeLabel.setText("point size");
		gridPointSizeLabel.setBounds(303, 48, 59, 20);

		Label guideSizeLabel = new Label(composite, SWT.NONE);
		guideSizeLabel.setText("Guide size");
		guideSizeLabel.setBounds(0, 156, 88, 20);

		Label copypasteOffsetLabel = new Label(composite, SWT.NONE);
		copypasteOffsetLabel.setText("Copy/Paste offset");
		copypasteOffsetLabel.setBounds(0, 182, 107, 20);

		Label stepsizeLabel = new Label(composite, SWT.NONE);
		stepsizeLabel.setText("Stepsize");
		stepsizeLabel.setToolTipText(STEP_SIZE_TOOLTIPTEXT);
		stepsizeLabel.setBounds(0, 208, 88, 20);

		Label gridColorLabel = new Label(composite, SWT.NONE);
		gridColorLabel.setText("color");
		gridColorLabel.setBounds(336, 13, 59, 20);

		Label metricsLabel = new Label(composite, SWT.NONE);
		metricsLabel.setText("Metrics");
		metricsLabel.setBounds(0, 245, 80, 20);

		stepSizeSpinner = new Spinner(composite, SWT.BORDER);
		stepSizeSpinner.setValues(0, 1, 100, 0, 1, 5);
		stepSizeSpinner.setToolTipText(STEP_SIZE_TOOLTIPTEXT);
		stepSizeSpinner.setBounds(109, 208, 60, 20);

		largeStepSizeSpinner = new Spinner(composite, SWT.BORDER);
		largeStepSizeSpinner.setValues(0, 1, 100, 0, 1, 5);
		largeStepSizeSpinner.setToolTipText(STEP_SIZE_TOOLTIPTEXT);
		largeStepSizeSpinner.setBounds(204, 205, 60, 20);

		guideSizeSpinner = new Spinner(composite, SWT.BORDER);
		guideSizeSpinner.setValues(0, 3, 100, 0, 5, 20);
		guideSizeSpinner.setBounds(109, 153, 60, 20);

		gridColorViewer = new ColorSelectViewer(composite, SWT.NONE);
		Control gridColorControl = gridColorViewer.getControl();
		gridColorControl.setBounds(270, 13, 60, 20);

		gridSizeSpinner = new Spinner(composite, SWT.BORDER);
		gridSizeSpinner.setValues(0, 3, 100, 0, 5, 20);
		gridSizeSpinner.setBounds(109, 45, 60, 20);

		gridShowButton = new Button(composite, SWT.CHECK);
		gridShowButton.setText("show");
		gridShowButton.setBounds(194, 7, 80, 26);

		Label gridDefaultLabel = new Label(composite, SWT.NONE);
		gridDefaultLabel.setText("size");
		gridDefaultLabel.setBounds(175, 48, 67, 14);

		snapToGridCheck = new Button(composite, SWT.CHECK);
		snapToGridCheck.setText("snap-to");
		snapToGridCheck.setBounds(109, 7, 79, 26);
		snapToGridCheck.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				if (snapToGridCheck.getSelection())
				{
					snapToAlignmentCheck.setSelection(false);
				}
			}
		});

		saveEditorStateButton = new Button(composite, SWT.CHECK);
		saveEditorStateButton.setText("re-open at startup");
		saveEditorStateButton.setBounds(109, 265, 225, 26);

		Label editorsLabel = new Label(composite, SWT.NONE);
		editorsLabel.setText("Editors");
		editorsLabel.setBounds(0, 271, 95, 14);

		toolbarsInFormWindowButton = new Button(composite, SWT.CHECK);
		toolbarsInFormWindowButton.setBounds(109, 297, 111, 26);
		toolbarsInFormWindowButton.setText("in form window");

		Label label = new Label(composite, SWT.NONE);
		label.setBounds(0, 303, 59, 14);
		label.setText("ToolBars");

		Button resetToolbarsButton = new Button(composite, SWT.NONE);
		resetToolbarsButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				new DesignerPreferences(ServoyModel.getSettings()).saveCoolbarLayout(null);
			}
		});
		resetToolbarsButton.setBounds(226, 297, 75, 26);
		resetToolbarsButton.setText("reset");

		snapToAlignmentCheck = new Button(composite, SWT.CHECK);
		snapToAlignmentCheck.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				if (snapToAlignmentCheck.getSelection())
				{
					snapToGridCheck.setSelection(false);
				}
			}
		});
		snapToAlignmentCheck.setBounds(109, 79, 85, 26);
		snapToAlignmentCheck.setText("snap-to");

		Label lblAlignment = new Label(composite, SWT.NONE);
		lblAlignment.setBounds(0, 85, 88, 14);
		lblAlignment.setText("Alignment");

		alignmentGuidecolorSelectViewer = new ColorSelectViewer(composite, 0);
		Control control = alignmentGuidecolorSelectViewer.getControl();
		control.setBounds(434, 85, 59, 20);

		Label lblGuideColor = new Label(composite, SWT.NONE);
		lblGuideColor.setText("guide color");
		lblGuideColor.setBounds(507, 85, 67, 20);

		alignmentThresholdSpinner = new Spinner(composite, SWT.BORDER);
		alignmentThresholdSpinner.setBounds(303, 82, 60, 20);

		alignmentSmallDistanceSpinner = new Spinner(composite, SWT.BORDER);
		alignmentSmallDistanceSpinner.setBounds(109, 111, 60, 20);

		alignmentMediumDistanceSpinner = new Spinner(composite, SWT.BORDER);
		alignmentMediumDistanceSpinner.setBounds(203, 111, 60, 20);

		alignmentLargeDistanceSpinner = new Spinner(composite, SWT.BORDER);
		alignmentLargeDistanceSpinner.setBounds(303, 111, 60, 20);

		Label lblThreshold = new Label(composite, SWT.NONE);
		lblThreshold.setBounds(369, 85, 59, 14);
		lblThreshold.setText("threshold");

		Label lblOffsets = new Label(composite, SWT.NONE);
		lblOffsets.setBounds(10, 114, 95, 14);
		lblOffsets.setText("Offsets");

		gridPointsizeSpinner = new Spinner(composite, SWT.BORDER);
		gridPointsizeSpinner.setBounds(248, 45, 49, 20);

		copyPasteOffsetSpinner = new Spinner(composite, SWT.BORDER);
		copyPasteOffsetSpinner.setValues(0, 0, 100, 0, 1, 5);
		copyPasteOffsetSpinner.setBounds(109, 182, 60, 20);

		anchorCheck = new Button(composite, SWT.CHECK);
		anchorCheck.setBounds(200, 79, 80, 26);
		anchorCheck.setText("anchor");

		initializeFields();

		return composite;
	}

	protected void initializeFields()
	{
		DesignerPreferences prefs = new DesignerPreferences(ServoyModel.getSettings());

		gridPointsizeSpinner.setSelection(prefs.getGridPointSize());
		gridSizeSpinner.setSelection(prefs.getGridSize());
		gridColorViewer.setSelection(new StructuredSelection(prefs.getGridColor()));
		alignmentGuidecolorSelectViewer.setSelection(new StructuredSelection(prefs.getAlignmentGuideColor()));
		snapToGridCheck.setSelection(prefs.getGridSnapTo());
		snapToAlignmentCheck.setSelection(prefs.getAlignmentSnapTo());
		gridShowButton.setSelection(prefs.getGridShow());
		saveEditorStateButton.setSelection(prefs.getSaveEditorState());
		toolbarsInFormWindowButton.setSelection(prefs.getFormToolsOnMainToolbar());
		guideSizeSpinner.setSelection(prefs.getGuideSize());
		copyPasteOffsetSpinner.setSelection(prefs.getCopyPasteOffset());
		alignmentThresholdSpinner.setSelection(prefs.getAlignmentThreshold());
		anchorCheck.setSelection(prefs.getAnchor());
		int[] distances = prefs.getAlignmentDistances();
		alignmentSmallDistanceSpinner.setSelection(distances[0]);
		alignmentMediumDistanceSpinner.setSelection(distances[1]);
		alignmentLargeDistanceSpinner.setSelection(distances[2]);
		stepSizeSpinner.setSelection(prefs.getStepSize());
		largeStepSizeSpinner.setSelection(prefs.getLargeStepSize());
		setMetricsComboValue(prefs.getMetrics());
	}

	@Override
	public boolean performOk()
	{
		DesignerPreferences prefs = new DesignerPreferences(ServoyModel.getSettings());

		prefs.setGridPointSize(gridPointsizeSpinner.getSelection());
		prefs.setGridSize(gridSizeSpinner.getSelection());
		prefs.setGridColor((RGB)((IStructuredSelection)gridColorViewer.getSelection()).getFirstElement());
		prefs.setAlignmentGuideColor((RGB)((IStructuredSelection)alignmentGuidecolorSelectViewer.getSelection()).getFirstElement());
		prefs.setGridShow(gridShowButton.getSelection());
		prefs.setSnapTo(snapToGridCheck.getSelection(), snapToAlignmentCheck.getSelection());
		prefs.setAnchor(anchorCheck.getSelection());
		prefs.setSaveEditorState(saveEditorStateButton.getSelection());
		prefs.setFormToolsOnMainToolbar(toolbarsInFormWindowButton.getSelection());
		prefs.setGuideSize(guideSizeSpinner.getSelection());
		prefs.setCopyPasteOffset(copyPasteOffsetSpinner.getSelection());
		prefs.setAlignmentThreshold(alignmentThresholdSpinner.getSelection());
		prefs.setAlignmentDistances(alignmentSmallDistanceSpinner.getSelection(), alignmentMediumDistanceSpinner.getSelection(),
			alignmentLargeDistanceSpinner.getSelection());
		prefs.setStepSize(stepSizeSpinner.getSelection(), largeStepSizeSpinner.getSelection());
		prefs.setMetrics(((Integer)((ObjectWrapper)((IStructuredSelection)metricsCombo.getSelection()).getFirstElement()).getType()).intValue());

		return true;
	}

	@Override
	protected void performDefaults()
	{
		gridPointsizeSpinner.setSelection(DesignerPreferences.GRID_POINTSIZE_DEFAULT);
		gridSizeSpinner.setSelection(DesignerPreferences.GRID_SIZE_DEFAULT);
		gridColorViewer.setSelection(new StructuredSelection(ColorPropertyController.PROPERTY_COLOR_CONVERTER.convertProperty("gridColor",
			PersistHelper.createColor(DesignerPreferences.GRID_COLOR_DEFAULT))));
		alignmentGuidecolorSelectViewer.setSelection(new StructuredSelection(ColorPropertyController.PROPERTY_COLOR_CONVERTER.convertProperty(
			"alignmentGuideColor", PersistHelper.createColor(DesignerPreferences.ALIGNMENT_GUIDE_COLOR_DEFAULT))));
		guideSizeSpinner.setSelection(DesignerPreferences.GUIDE_SIZE_DEFAULT);
		gridShowButton.setSelection(DesignerPreferences.GRID_SHOW_DEFAULT);
		snapToGridCheck.setSelection(DesignerPreferences.SNAPTO_DEFAULT.equals(DesignerPreferences.SNAP_TO_GRID));
		anchorCheck.setSelection(DesignerPreferences.ANCHOR_DEFAULT);
		snapToAlignmentCheck.setSelection(DesignerPreferences.SNAPTO_DEFAULT.equals(DesignerPreferences.SNAP_TO_ALIGMNENT));
		saveEditorStateButton.setSelection(DesignerPreferences.SAVE_EDITOR_STATE_DEFAULT);
		toolbarsInFormWindowButton.setSelection(DesignerPreferences.FORM_TOOLS_ON_MAIN_TOOLBAR_DEFAULT);
		copyPasteOffsetSpinner.setSelection(DesignerPreferences.COPY_PASTE_OFFSET_DEFAULT);
		alignmentThresholdSpinner.setSelection(DesignerPreferences.ALIGNMENT_THRESHOLD_DEFAULT);
		alignmentSmallDistanceSpinner.setSelection(DesignerPreferences.ALIGNMENT_DISTANCES_DEFAULT[0]);
		alignmentMediumDistanceSpinner.setSelection(DesignerPreferences.ALIGNMENT_DISTANCES_DEFAULT[1]);
		alignmentLargeDistanceSpinner.setSelection(DesignerPreferences.ALIGNMENT_DISTANCES_DEFAULT[2]);
		stepSizeSpinner.setSelection(DesignerPreferences.STEP_SIZE_DEFAULT);
		largeStepSizeSpinner.setSelection(DesignerPreferences.LARGE_STEP_SIZE_DEFAULT);
		setMetricsComboValue(DesignerPreferences.METRICS_DEFAULT);

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
