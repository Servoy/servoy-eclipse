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

	private static final String STEP_SIZE_TOOLTIPTEXT = "Shortcuts for moving elements in form designer:\n" + //
		"\tMove:\n" + //
		"\t\t* 1px: Just Arrow\n" + //
		"\t\t* Small step: Ctrl-Arrows\n" + //
		"\t\t* Large step: Ctrl-Alt-Arrows\n" + //
		"\tResize:\n" + //
		"\t\t* 1px: Shift-Arrows\n" + //
		"\t\t* Small step: Ctrl-Shift-Arrows\n" + //
		"\t\t* Large step: Alt-Shift-Arrows";

	private Spinner gridSizeSpinner;
	private ComboViewer gridPointSizeCombo;
	private ColorSelectViewer gridColorViewer;
	private Spinner guideSizeSpinner;
	private ComboViewer copyPasetOffsetCombo;
	private Spinner stepSizeSpinner;
	private Spinner largeStepSizeSpinner;
	private ComboViewer metricsCombo;
	private Button gridShowButton;
	private Button gridSnapToButton;
	private Button saveEditorStateButton;
	private Button toolbarsInFormWindowButton;

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
		metricsControl.setBounds(204, 220, 100, 21);

		Label gridSizeLabel = new Label(composite, SWT.NONE);
		gridSizeLabel.setText("Grid size");
		gridSizeLabel.setBounds(8, 13, 171, 20);

		Label gridPointSizeLabel = new Label(composite, SWT.NONE);
		gridPointSizeLabel.setText("Grid point size");
		gridPointSizeLabel.setBounds(8, 42, 171, 20);

		Label guideSizeLabel = new Label(composite, SWT.NONE);
		guideSizeLabel.setText("Guide size");
		guideSizeLabel.setBounds(8, 135, 171, 20);

		Label copypasteOffsetLabel = new Label(composite, SWT.NONE);
		copypasteOffsetLabel.setText("Copy/Paste offset");
		copypasteOffsetLabel.setBounds(8, 170, 171, 20);

		Label stepsizeLabel = new Label(composite, SWT.NONE);
		stepsizeLabel.setText("Stepsize");
		stepsizeLabel.setToolTipText(STEP_SIZE_TOOLTIPTEXT);
		stepsizeLabel.setBounds(8, 196, 171, 20);

		Label gridColorLabel = new Label(composite, SWT.NONE);
		gridColorLabel.setText("Grid color");
		gridColorLabel.setBounds(8, 70, 171, 20);

		Label metricsLabel = new Label(composite, SWT.NONE);
		metricsLabel.setText("Metrics");
		metricsLabel.setBounds(8, 223, 171, 20);

		stepSizeSpinner = new Spinner(composite, SWT.BORDER);
		stepSizeSpinner.setValues(0, 1, 100, 0, 1, 5);
		stepSizeSpinner.setToolTipText(STEP_SIZE_TOOLTIPTEXT);
		stepSizeSpinner.setBounds(204, 194, 100, 20);

		largeStepSizeSpinner = new Spinner(composite, SWT.BORDER);
		largeStepSizeSpinner.setValues(0, 1, 100, 0, 1, 5);
		largeStepSizeSpinner.setToolTipText(STEP_SIZE_TOOLTIPTEXT);
		largeStepSizeSpinner.setBounds(350, 194, 100, 20);

		copyPasetOffsetCombo = new ComboViewer(composite);
		copyPasetOffsetCombo.setContentProvider(new ArrayContentProvider());
		copyPasetOffsetCombo.setLabelProvider(new LabelProvider());
		copyPasetOffsetCombo.setInput(new Integer[] { new Integer(0), new Integer(5), new Integer(10), new Integer(15), new Integer(20) });
		Control copyPasetOffsetText = copyPasetOffsetCombo.getControl();
		copyPasetOffsetText.setBounds(204, 167, 100, 21);
		guideSizeSpinner = new Spinner(composite, SWT.BORDER);
		guideSizeSpinner.setValues(0, 3, 100, 0, 5, 20);
		guideSizeSpinner.setBounds(204, 132, 100, 20);

		gridColorViewer = new ColorSelectViewer(composite, SWT.NONE);
		Control gridColorControl = gridColorViewer.getControl();
		gridColorControl.setBounds(204, 68, 100, 20);
		gridPointSizeCombo = new ComboViewer(composite);
		gridPointSizeCombo.setContentProvider(new ArrayContentProvider());
		gridPointSizeCombo.setLabelProvider(new LabelProvider());
		gridPointSizeCombo.setInput(new Integer[] { new Integer(0), new Integer(1), new Integer(2), new Integer(4) });
		Control gridPointSizeControl;
		gridPointSizeControl = gridPointSizeCombo.getControl();
		gridPointSizeControl.setBounds(204, 39, 100, 21);

		gridSizeSpinner = new Spinner(composite, SWT.BORDER);
		gridSizeSpinner.setValues(0, 3, 100, 0, 5, 20);
		gridSizeSpinner.setBounds(204, 10, 100, 20);

		gridShowButton = new Button(composite, SWT.CHECK);
		gridShowButton.setText("show");
		gridShowButton.setBounds(204, 94, 80, 26);

		Label gridDefaultLabel = new Label(composite, SWT.NONE);
		gridDefaultLabel.setText("Grid default");
		gridDefaultLabel.setBounds(8, 102, 171, 14);

		gridSnapToButton = new Button(composite, SWT.CHECK);
		gridSnapToButton.setText("snap to");
		gridSnapToButton.setBounds(350, 94, 100, 26);

		saveEditorStateButton = new Button(composite, SWT.CHECK);
		saveEditorStateButton.setText("re-open at startup");
		saveEditorStateButton.setBounds(204, 248, 225, 26);

		Label editorsLabel = new Label(composite, SWT.NONE);
		editorsLabel.setText("Editors");
		editorsLabel.setBounds(8, 254, 139, 14);

		toolbarsInFormWindowButton = new Button(composite, SWT.CHECK);
		toolbarsInFormWindowButton.setBounds(204, 280, 111, 26);
		toolbarsInFormWindowButton.setText("in form window");

		Label label = new Label(composite, SWT.NONE);
		label.setBounds(10, 286, 59, 14);
		label.setText("ToolBars");

		initializeFields();

		return composite;
	}

	protected void initializeFields()
	{
		DesignerPreferences prefs = new DesignerPreferences(ServoyModel.getSettings());

		gridPointSizeCombo.setSelection(new StructuredSelection(new Integer(prefs.getGridPointSize())));
		gridSizeSpinner.setSelection(prefs.getGridSize());
		gridColorViewer.setSelection(new StructuredSelection(prefs.getGridColor()));
		gridSnapToButton.setSelection(prefs.getGridSnapTo());
		gridShowButton.setSelection(prefs.getGridShow());
		saveEditorStateButton.setSelection(prefs.getSaveEditorState());
		toolbarsInFormWindowButton.setSelection(prefs.getFormToolsOnMainToolbar());
		guideSizeSpinner.setSelection(prefs.getGuideSize());
		copyPasetOffsetCombo.setSelection(new StructuredSelection(new Integer(prefs.getCopyPasteOffset())));
		stepSizeSpinner.setSelection(prefs.getStepSize());
		largeStepSizeSpinner.setSelection(prefs.getLargeStepSize());
		setMetricsComboValue(prefs.getMetrics());
	}

	@Override
	public boolean performOk()
	{
		DesignerPreferences prefs = new DesignerPreferences(ServoyModel.getSettings());

		prefs.setGridPointSize(((Integer)((IStructuredSelection)gridPointSizeCombo.getSelection()).getFirstElement()).intValue());
		prefs.setGridSize(gridSizeSpinner.getSelection());
		prefs.setGridColor((RGB)((IStructuredSelection)gridColorViewer.getSelection()).getFirstElement());
		prefs.setGridShow(gridShowButton.getSelection());
		prefs.setGridSnapTo(gridSnapToButton.getSelection());
		prefs.setSaveEditorState(saveEditorStateButton.getSelection());
		prefs.setFormToolsOnMainToolbar(toolbarsInFormWindowButton.getSelection());
		prefs.setGuideSize(guideSizeSpinner.getSelection());
		prefs.setCopyPasteOffset(((Integer)((IStructuredSelection)copyPasetOffsetCombo.getSelection()).getFirstElement()).intValue());
		prefs.setStepSize(stepSizeSpinner.getSelection(), largeStepSizeSpinner.getSelection());
		prefs.setMetrics(((Integer)((ObjectWrapper)((IStructuredSelection)metricsCombo.getSelection()).getFirstElement()).getType()).intValue());

		return true;
	}

	@Override
	protected void performDefaults()
	{
		gridPointSizeCombo.setSelection(new StructuredSelection(new Integer(DesignerPreferences.GRID_POINTSIZE_DEFAULT)));
		gridSizeSpinner.setSelection(DesignerPreferences.GRID_SIZE_DEFAULT);
		gridColorViewer.setSelection(new StructuredSelection(ColorPropertyController.PROPERTY_COLOR_CONVERTER.convertProperty("gridColor",
			PersistHelper.createColor(DesignerPreferences.GRID_COLOR_DEFAULT))));
		guideSizeSpinner.setSelection(DesignerPreferences.GUIDE_SIZE_DEFAULT);
		gridShowButton.setSelection(DesignerPreferences.GRID_SHOW_DEFAULT);
		gridSnapToButton.setSelection(DesignerPreferences.GRID_SNAPTO_DEFAULT);
		saveEditorStateButton.setSelection(DesignerPreferences.SAVE_EDITOR_STATE_DEFAULT);
		toolbarsInFormWindowButton.setSelection(DesignerPreferences.FORM_TOOLS_ON_MAIN_TOOLBAR_DEFAULT);
		copyPasetOffsetCombo.setSelection(new StructuredSelection(new Integer(DesignerPreferences.COPY_PASTE_OFFSET_DEFAULT)));
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
