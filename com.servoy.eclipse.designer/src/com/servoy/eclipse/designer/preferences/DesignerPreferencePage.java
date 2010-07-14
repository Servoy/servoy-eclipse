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
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.servoy.eclipse.core.ServoyModelManager;
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

	private Spinner gridSizeSpinner;
	private ComboViewer gridPointSizeCombo;
	private ColorSelectViewer gridColorViewer;
	private Spinner guideSizeSpinner;
	private ComboViewer copyPasetOffsetCombo;
	private Spinner stepSizeSpinner;
	private ComboViewer metricsCombo;
	private Button gridShowButton;
	private Button gridSnapToButton;
	private Button saveEditorStateButton;

	public void init(IWorkbench workbench)
	{
	}

	@Override
	protected Control createContents(Composite parent)
	{
		initializeDialogUnits(parent);

		Composite composite = new Composite(parent, SWT.NONE);

		metricsCombo = new ComboViewer(composite);
		metricsCombo.setContentProvider(new ArrayContentProvider());
		metricsCombo.setLabelProvider(new LabelProvider());
		metricsCombo.setInput(new ObjectWrapper[] { new ObjectWrapper("pixels", new Integer(DesignerPreferences.PX)), new ObjectWrapper("centimeters",
			new Integer(DesignerPreferences.CM)), new ObjectWrapper("inches", new Integer(DesignerPreferences.IN)) });
		Control metricsControl;
		metricsControl = metricsCombo.getControl();

		Label gridSizeLabel = new Label(composite, SWT.NONE);
		gridSizeLabel.setText("Grid size");

		Label gridPointSizeLabel = new Label(composite, SWT.NONE);
		gridPointSizeLabel.setText("Grid point size");

		Label guideSizeLabel = new Label(composite, SWT.NONE);
		guideSizeLabel.setText("Guide size");

		Label copypasteOffsetLabel = new Label(composite, SWT.NONE);
		copypasteOffsetLabel.setText("Copy/Paste offset");

		Label stepsizeLabel = new Label(composite, SWT.NONE);
		stepsizeLabel.setText("Stepsize");

		Label gridColorLabel = new Label(composite, SWT.NONE);
		gridColorLabel.setText("Grid color");

		Label metricsLabel = new Label(composite, SWT.NONE);
		metricsLabel.setText("Metrics");

		stepSizeSpinner = new Spinner(composite, SWT.BORDER);
		stepSizeSpinner.setValues(0, 1, 100, 0, 1, 5);

		copyPasetOffsetCombo = new ComboViewer(composite);
		copyPasetOffsetCombo.setContentProvider(new ArrayContentProvider());
		copyPasetOffsetCombo.setLabelProvider(new LabelProvider());
		copyPasetOffsetCombo.setInput(new Integer[] { new Integer(0), new Integer(5), new Integer(10), new Integer(15), new Integer(20) });
		Control copyPasetOffsetText = copyPasetOffsetCombo.getControl();
		guideSizeSpinner = new Spinner(composite, SWT.BORDER);
		guideSizeSpinner.setValues(0, 3, 100, 0, 5, 20);

		gridColorViewer = new ColorSelectViewer(composite, SWT.NONE);
		Control gridColorControl = gridColorViewer.getControl();
		gridPointSizeCombo = new ComboViewer(composite);
		gridPointSizeCombo.setContentProvider(new ArrayContentProvider());
		gridPointSizeCombo.setLabelProvider(new LabelProvider());
		gridPointSizeCombo.setInput(new Integer[] { new Integer(0), new Integer(1), new Integer(2), new Integer(4) });
		Control gridPointSizeControl;
		gridPointSizeControl = gridPointSizeCombo.getControl();

		gridSizeSpinner = new Spinner(composite, SWT.BORDER);
		gridSizeSpinner.setValues(0, 3, 100, 0, 5, 20);

		gridShowButton = new Button(composite, SWT.CHECK);
		gridShowButton.setText("show");

		Label gridDefaultLabel = new Label(composite, SWT.NONE);
		gridDefaultLabel.setText("Grid default");

		gridSnapToButton = new Button(composite, SWT.CHECK);
		gridSnapToButton.setText("snap to");

		saveEditorStateButton = new Button(composite, SWT.CHECK);
		saveEditorStateButton.setText("re-open at startup");

		Label editorsLabel = new Label(composite, SWT.NONE);
		editorsLabel.setText("Editors");

		final GroupLayout groupLayout = new GroupLayout(composite);
		groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			groupLayout.createSequentialGroup().add(8, 8, 8).add(
				groupLayout.createParallelGroup(GroupLayout.LEADING).add(
					groupLayout.createSequentialGroup().add(gridColorLabel, GroupLayout.PREFERRED_SIZE, 171, GroupLayout.PREFERRED_SIZE).add(25, 25, 25).add(
						gridColorControl, GroupLayout.PREFERRED_SIZE, 68, GroupLayout.PREFERRED_SIZE)).add(
					groupLayout.createSequentialGroup().add(gridDefaultLabel, GroupLayout.PREFERRED_SIZE, 171, GroupLayout.PREFERRED_SIZE).add(25, 25, 25).add(
						gridShowButton, GroupLayout.PREFERRED_SIZE, 80, GroupLayout.PREFERRED_SIZE).add(22, 22, 22).add(gridSnapToButton,
						GroupLayout.PREFERRED_SIZE, 100, GroupLayout.PREFERRED_SIZE)).add(
					groupLayout.createSequentialGroup().addPreferredGap(LayoutStyle.RELATED).add(guideSizeLabel, GroupLayout.PREFERRED_SIZE, 171,
						GroupLayout.PREFERRED_SIZE).add(21, 21, 21).add(guideSizeSpinner, GroupLayout.DEFAULT_SIZE, 263, Short.MAX_VALUE).addContainerGap()).add(
					groupLayout.createSequentialGroup().add(
						groupLayout.createParallelGroup(GroupLayout.LEADING).add(gridPointSizeLabel, GroupLayout.PREFERRED_SIZE, 171,
							GroupLayout.PREFERRED_SIZE).add(
							groupLayout.createSequentialGroup().addPreferredGap(LayoutStyle.RELATED).add(gridSizeLabel, GroupLayout.PREFERRED_SIZE, 171,
								GroupLayout.PREFERRED_SIZE))).add(25, 25, 25).add(
						groupLayout.createParallelGroup(GroupLayout.LEADING).add(gridPointSizeControl, GroupLayout.DEFAULT_SIZE, 259, Short.MAX_VALUE).add(
							gridSizeSpinner, GroupLayout.DEFAULT_SIZE, 259, Short.MAX_VALUE)).addContainerGap()).add(
					groupLayout.createSequentialGroup().add(
						groupLayout.createParallelGroup(GroupLayout.LEADING).add(copypasteOffsetLabel, GroupLayout.PREFERRED_SIZE, 171,
							GroupLayout.PREFERRED_SIZE).add(stepsizeLabel, GroupLayout.PREFERRED_SIZE, 171, GroupLayout.PREFERRED_SIZE)).add(25, 25, 25).add(
						groupLayout.createParallelGroup(GroupLayout.LEADING).add(copyPasetOffsetText, GroupLayout.DEFAULT_SIZE, 259, Short.MAX_VALUE).add(
							stepSizeSpinner, GroupLayout.DEFAULT_SIZE, 259, Short.MAX_VALUE)).addContainerGap()).add(
					groupLayout.createSequentialGroup().add(
						groupLayout.createParallelGroup(GroupLayout.LEADING).add(metricsLabel, GroupLayout.PREFERRED_SIZE, 171, GroupLayout.PREFERRED_SIZE).add(
							editorsLabel, GroupLayout.PREFERRED_SIZE, 139, GroupLayout.PREFERRED_SIZE)).add(25, 25, 25).add(
						groupLayout.createParallelGroup(GroupLayout.LEADING).add(metricsControl, GroupLayout.DEFAULT_SIZE, 259, Short.MAX_VALUE).add(
							saveEditorStateButton)).addContainerGap()))));
		groupLayout.setVerticalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			groupLayout.createSequentialGroup().add(10, 10, 10).add(
				groupLayout.createParallelGroup(GroupLayout.LEADING).add(gridSizeLabel, GroupLayout.PREFERRED_SIZE, 20, GroupLayout.PREFERRED_SIZE).add(
					gridSizeSpinner, GroupLayout.PREFERRED_SIZE, 30, GroupLayout.PREFERRED_SIZE)).add(
				groupLayout.createParallelGroup(GroupLayout.LEADING).add(
					groupLayout.createSequentialGroup().add(11, 11, 11).add(gridPointSizeLabel, GroupLayout.PREFERRED_SIZE, 20, GroupLayout.PREFERRED_SIZE)).add(
					groupLayout.createSequentialGroup().addPreferredGap(LayoutStyle.RELATED).add(gridPointSizeControl, GroupLayout.PREFERRED_SIZE,
						GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))).add(12, 12, 12).add(
				groupLayout.createParallelGroup(GroupLayout.LEADING).add(
					groupLayout.createSequentialGroup().add(4, 4, 4).add(gridColorLabel, GroupLayout.PREFERRED_SIZE, 20, GroupLayout.PREFERRED_SIZE)).add(
					gridColorControl, GroupLayout.PREFERRED_SIZE, 30, GroupLayout.PREFERRED_SIZE)).add(14, 14, 14).add(
				groupLayout.createParallelGroup(GroupLayout.LEADING).add(gridDefaultLabel).add(gridShowButton).add(gridSnapToButton)).add(6, 6, 6).add(
				groupLayout.createParallelGroup(GroupLayout.TRAILING).add(guideSizeSpinner, GroupLayout.PREFERRED_SIZE, 30, GroupLayout.PREFERRED_SIZE).add(
					guideSizeLabel, GroupLayout.PREFERRED_SIZE, 20, GroupLayout.PREFERRED_SIZE)).add(
				groupLayout.createParallelGroup(GroupLayout.LEADING).add(
					groupLayout.createSequentialGroup().add(18, 18, 18).add(copypasteOffsetLabel, GroupLayout.PREFERRED_SIZE, 20, GroupLayout.PREFERRED_SIZE)).add(
					groupLayout.createSequentialGroup().add(13, 13, 13).add(copyPasetOffsetText, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
						GroupLayout.PREFERRED_SIZE))).add(10, 10, 10).add(
				groupLayout.createParallelGroup(GroupLayout.TRAILING).add(stepSizeSpinner, GroupLayout.PREFERRED_SIZE, 30, GroupLayout.PREFERRED_SIZE).add(
					stepsizeLabel, GroupLayout.PREFERRED_SIZE, 20, GroupLayout.PREFERRED_SIZE)).add(
				groupLayout.createParallelGroup(GroupLayout.LEADING).add(
					groupLayout.createSequentialGroup().add(19, 19, 19).add(metricsLabel, GroupLayout.PREFERRED_SIZE, 20, GroupLayout.PREFERRED_SIZE)).add(
					groupLayout.createSequentialGroup().add(18, 18, 18).add(metricsControl, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
						GroupLayout.PREFERRED_SIZE))).add(
				groupLayout.createParallelGroup(GroupLayout.LEADING).add(groupLayout.createSequentialGroup().add(8, 8, 8).add(editorsLabel)).add(
					groupLayout.createSequentialGroup().addPreferredGap(LayoutStyle.RELATED).add(saveEditorStateButton))).addContainerGap(
				GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
		composite.setLayout(groupLayout);

		initializeFields();

		return composite;
	}

	protected void initializeFields()
	{
		DesignerPreferences prefs = new DesignerPreferences(ServoyModelManager.getServoyModelManager().getServoyModel().getSettings());

		gridPointSizeCombo.setSelection(new StructuredSelection(new Integer(prefs.getGridPointSize())));
		gridSizeSpinner.setSelection(prefs.getGridSize());
		gridColorViewer.setSelection(new StructuredSelection(prefs.getGridColor()));
		gridSnapToButton.setSelection(prefs.getGridSnapTo());
		gridShowButton.setSelection(prefs.getGridShow());
		saveEditorStateButton.setSelection(prefs.getSaveEditorState());
		guideSizeSpinner.setSelection(prefs.getGuideSize());
		copyPasetOffsetCombo.setSelection(new StructuredSelection(new Integer(prefs.getCopyPasteOffset())));
		stepSizeSpinner.setSelection(prefs.getStepSize());
		setMetricsComboValue(prefs.getMetrics());
	}

	@Override
	public boolean performOk()
	{
		DesignerPreferences prefs = new DesignerPreferences(ServoyModelManager.getServoyModelManager().getServoyModel().getSettings());

		prefs.setGridPointSize(((Integer)((IStructuredSelection)gridPointSizeCombo.getSelection()).getFirstElement()).intValue());
		prefs.setGridSize(gridSizeSpinner.getSelection());
		prefs.setGridColor((RGB)((IStructuredSelection)gridColorViewer.getSelection()).getFirstElement());
		prefs.setGridShow(gridShowButton.getSelection());
		prefs.setGridSnapTo(gridSnapToButton.getSelection());
		prefs.setSaveEditorState(saveEditorStateButton.getSelection());
		prefs.setGuideSize(guideSizeSpinner.getSelection());
		prefs.setCopyPasteOffset(((Integer)((IStructuredSelection)copyPasetOffsetCombo.getSelection()).getFirstElement()).intValue());
		prefs.setStepSize(stepSizeSpinner.getSelection());
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
		copyPasetOffsetCombo.setSelection(new StructuredSelection(new Integer(DesignerPreferences.COPY_PASTE_OFFSET_DEFAULT)));
		stepSizeSpinner.setSelection(DesignerPreferences.STEP_SIZE_DEFAULT);
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
