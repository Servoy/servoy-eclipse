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
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.servoy.eclipse.ui.preferences.DesignerPreferences;

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

	private Button favoritesComponentsCheck;
	private Button commonlyUsedComponentsCheck;
	private Spinner copyPasteOffsetSpinner;
	private Spinner commonlyUsedSizeSpinner;
	private Spinner stepSizeSpinner;
	private Spinner largeStepSizeSpinner;
	private Button sameSizeFeedbackCheck;
	private Button anchorFeedbackCheck;
	private Button marqueeSelectOuterCheck;
	private Spinner titanium_alignmentThresholdSpinner;
	private Spinner titanium_equalDistanceThresholdSpinner;
	private Spinner titanium_equalSizeThresholdSpinner;

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

		favoritesComponentsCheck = new Button(optionsPanel, SWT.CHECK);
		favoritesComponentsCheck.setText("Show Favorites Components - are those components selected by the user");

		commonlyUsedComponentsCheck = new Button(optionsPanel, SWT.CHECK);
		commonlyUsedComponentsCheck.setText(
			"Show Commonly Used Components - are the top 5 components frequently used in the project, if the below property \"Commonly used package size\" is not changed");

		Composite copyPastePanel = new Composite(optionsPanel, SWT.NONE);
		copyPastePanel.setLayout(new GridLayout(2, false));

		new Label(copyPastePanel, SWT.NONE).setText("Copy/Paste offset");

		copyPasteOffsetSpinner = new Spinner(copyPastePanel, SWT.BORDER);
		copyPasteOffsetSpinner.setValues(0, 0, 100, 0, 1, 5);

		new Label(copyPastePanel, SWT.NONE).setText("Commonly used package size");

		commonlyUsedSizeSpinner = new Spinner(copyPastePanel, SWT.BORDER);
		commonlyUsedSizeSpinner.setValues(5, 5, 15, 0, 1, 5);

		marqueeSelectOuterCheck = new Button(optionsPanel, SWT.CHECK);
		marqueeSelectOuterCheck.setText("Marquee selects only elements fully in lasso");

		Group titaniumEditorDesignerPreferenceGroup = new Group(optionsPanel, SWT.NONE);
		titaniumEditorDesignerPreferenceGroup.setText("Dynamic Guides Settings");
		titaniumEditorDesignerPreferenceGroup.setLayout(new GridLayout(2, false));
		titaniumEditorDesignerPreferenceGroup.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		new Label(titaniumEditorDesignerPreferenceGroup, SWT.NONE).setText("Snap to Guide Threshold");
		titanium_alignmentThresholdSpinner = new Spinner(titaniumEditorDesignerPreferenceGroup, SWT.BORDER);
		new Label(titaniumEditorDesignerPreferenceGroup, SWT.NONE).setText("Snap to Equal Distance Guides Threshold");
		titanium_equalDistanceThresholdSpinner = new Spinner(titaniumEditorDesignerPreferenceGroup, SWT.BORDER);
		new Label(titaniumEditorDesignerPreferenceGroup, SWT.NONE).setText("Snap to Same Size Guides Threshold");
		titanium_equalSizeThresholdSpinner = new Spinner(titaniumEditorDesignerPreferenceGroup, SWT.BORDER);

		Composite settingsPanel = new Composite(rootPanel, SWT.NONE);
		settingsPanel.setLayout(new GridLayout(2, false));

		Group grpFeedbackSettings = new Group(settingsPanel, SWT.NONE);
		grpFeedbackSettings.setText("Feedback Settings");
		grpFeedbackSettings.setLayout(new GridLayout(1, false));
		grpFeedbackSettings.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));

		anchorFeedbackCheck = new Button(grpFeedbackSettings, SWT.CHECK);
		anchorFeedbackCheck.setText("Show anchoring feedback");

		sameSizeFeedbackCheck = new Button(grpFeedbackSettings, SWT.CHECK);
		sameSizeFeedbackCheck.setText("Show same-size feedback");

		Composite guideKeyPanel = new Composite(settingsPanel, SWT.NONE);
		guideKeyPanel.setLayout(new GridLayout(1, true));

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

		return rootPanel;
	}

	protected void initializeFields()
	{
		DesignerPreferences prefs = new DesignerPreferences();

		favoritesComponentsCheck.setSelection(prefs.getShowFavoritesComponents());
		commonlyUsedComponentsCheck.setSelection(prefs.getShowCommonlyUsedComponents());
		copyPasteOffsetSpinner.setSelection(prefs.getCopyPasteOffset());
		commonlyUsedSizeSpinner.setSelection(prefs.getCommonlyUsedSize());
		stepSizeSpinner.setSelection(prefs.getStepSize());
		largeStepSizeSpinner.setSelection(prefs.getLargeStepSize());
		sameSizeFeedbackCheck.setSelection(prefs.getShowSameSizeFeedback());
		anchorFeedbackCheck.setSelection(prefs.getShowAnchorFeedback());
		marqueeSelectOuterCheck.setSelection(prefs.getMarqueeSelectOuter());

		titanium_alignmentThresholdSpinner.setSelection(prefs.getTitaniumAlignmentThreshold());
		titanium_equalDistanceThresholdSpinner.setSelection(prefs.getTitaniumSnapEqualDistanceThreshold());
		titanium_equalSizeThresholdSpinner.setSelection(prefs.getTitaniumSnapEqualSizeThreshold());
	}

	@Override
	public boolean performOk()
	{
		DesignerPreferences prefs = new DesignerPreferences();

		prefs.setShowFavoritesComponents(favoritesComponentsCheck.getSelection());
		prefs.setShowCommonlyUsedComponents(commonlyUsedComponentsCheck.getSelection());
		prefs.setCopyPasteOffset(copyPasteOffsetSpinner.getSelection());
		prefs.setCommonlyUsedSize(commonlyUsedSizeSpinner.getSelection());
		prefs.setStepSize(stepSizeSpinner.getSelection(), largeStepSizeSpinner.getSelection());
		prefs.setShowSameSizeFeedback(sameSizeFeedbackCheck.getSelection());
		prefs.setShowAnchorFeedback(anchorFeedbackCheck.getSelection());
		prefs.setMarqueeSelectOuter(marqueeSelectOuterCheck.getSelection());

		prefs.setTitaniumAlignmentThreshold(titanium_alignmentThresholdSpinner.getSelection());
		prefs.setTitaniumSnapEqualDistanceThreshold(titanium_equalDistanceThresholdSpinner.getSelection());
		prefs.setTitaniumSnapEqualSizeThreshold(titanium_equalSizeThresholdSpinner.getSelection());
		prefs.save();

		return true;
	}

	@Override
	protected void performDefaults()
	{
		favoritesComponentsCheck.setSelection(DesignerPreferences.SHOW_FAVORITES_COMPONENTS_DEFAULT);
		commonlyUsedComponentsCheck.setSelection(DesignerPreferences.SHOW_COMMONLY_USED_COMPONENTS_DEFAULT);
		copyPasteOffsetSpinner.setSelection(DesignerPreferences.COPY_PASTE_OFFSET_DEFAULT);
		commonlyUsedSizeSpinner.setSelection(DesignerPreferences.COMMONLY_USED_SIZE_DEFAULT);
		stepSizeSpinner.setSelection(DesignerPreferences.STEP_SIZE_DEFAULT);
		largeStepSizeSpinner.setSelection(DesignerPreferences.LARGE_STEP_SIZE_DEFAULT);
		sameSizeFeedbackCheck.setSelection(DesignerPreferences.SHOW_SAME_SIZE_DEFAULT);
		anchorFeedbackCheck.setSelection(DesignerPreferences.SHOW_ANCHORING_DEFAULT);
		marqueeSelectOuterCheck.setSelection(DesignerPreferences.MARQUEE_SELECT_OUTER_DEFAULT);
		titanium_alignmentThresholdSpinner.setSelection(DesignerPreferences.TITANIUM_ALIGNMENT_THRESHOLD_DEFAULT);
		titanium_equalDistanceThresholdSpinner.setSelection(DesignerPreferences.TITANIUM_SNAP_EQUAL_DISTANCE_THRESHOLD_DEFAULT);
		titanium_equalSizeThresholdSpinner.setSelection(DesignerPreferences.TITANIUM_SNAP_EQUAL_SIZE_THRESHOLD_DEFAULT);

		super.performDefaults();
	}
}
