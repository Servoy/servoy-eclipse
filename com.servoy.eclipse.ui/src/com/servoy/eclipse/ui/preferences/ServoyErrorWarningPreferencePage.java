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

package com.servoy.eclipse.ui.preferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.dltk.compiler.problem.ProblemSeverity;
import org.eclipse.dltk.ui.util.SWTFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.SharedScrolledComposite;
import org.osgi.service.prefs.BackingStoreException;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.builder.ServoyBuilder;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Messages;
import com.servoy.j2db.util.Pair;

/**
 * Preference page that lets the user configure the type of Servoy problem markers (IGNORE, INFO, ERROR, WARNING).
 * This is minimal, just made to resemble existing Error/Warnings page.
 * 
 * When we need to extend this to work as JS Error/Warnings page does with project/workspace settings and more marker types (SVY-75),
 * have a look at jdt implementation or at org.eclipse.dltk.javascript.internal.ui.preferences.JavaScriptErrorWarningPreferencePage (should be pretty reusable - it's a lot of copy paste from jdt as well)
 * and rewrite it based on that.
 * 
 * @author acostescu
 */
public class ServoyErrorWarningPreferencePage extends PreferencePage implements IWorkbenchPreferencePage
{

	private final IEclipsePreferences settingsNode;
	private final HashMap<String, String> changes = new HashMap<String, String>();
	private final List<Pair<Combo, Integer>> defaults = new ArrayList<Pair<Combo, Integer>>();

	public ServoyErrorWarningPreferencePage()
	{
		settingsNode = new InstanceScope().getNode(ServoyBuilder.ERROR_WARNING_PREFERENCES_NODE);
	}

	private class ScrolledPage extends SharedScrolledComposite
	{
		public ScrolledPage(Composite parent)
		{
			super(parent, SWT.V_SCROLL | SWT.H_SCROLL);
			setExpandHorizontal(true);
			setExpandVertical(true);
			Composite body = new Composite(this, SWT.NONE);
			body.setFont(parent.getFont());
			setContent(body);
		}
	}

	@Override
	public Control createContents(Composite parent)
	{
		changes.clear();

		Label l = new Label(parent, SWT.NONE);
		l.setFont(parent.getFont());
		l.setText(Messages.ErrorWarningPreferencePageDescription);
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		l.setLayoutData(gd);

		ScrolledPage sc1 = new ScrolledPage(parent);
		gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = new PixelConverter(parent).convertHeightInCharsToPixels(20);
		gd.verticalIndent = 10;
		sc1.setLayoutData(gd);

		Composite composite = (Composite)sc1.getContent();
		final GridLayout layout = new GridLayout(2, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		composite.setLayout(layout);
		composite.setFont(parent.getFont());

		String[] names = new String[] { "Warning", "Error", "Info", "Ignore" }; //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$
		List<String> ids = new ArrayList<String>(4);
		ids.add(ProblemSeverity.WARNING.name());
		ids.add(ProblemSeverity.ERROR.name());
		ids.add(ProblemSeverity.INFO.name());
		ids.add(ProblemSeverity.IGNORE.name());

		Composite inner = addPreferenceComposite(composite, sc1);
		addPreferenceItem(inner, ServoyBuilder.LEVEL_PERFORMANCE_COLUMNS_TABLEVIEW.getLeft(), Messages.ErrorWarningPreferencePage_tooManyColumns, names, ids,
			ServoyBuilder.LEVEL_PERFORMANCE_COLUMNS_TABLEVIEW.getRight().name());
		addPreferenceItem(inner, ServoyBuilder.LEVEL_PERFORMANCE_TABS_PORTALS.getLeft(), Messages.ErrorWarningPreferencePage_tooManyTabsPortals, names, ids,
			ServoyBuilder.LEVEL_PERFORMANCE_TABS_PORTALS.getRight().name());

		applyDialogFont(composite);

		return composite;
	}

	protected Composite addPreferenceComposite(Composite parent, ScrolledPage sc)
	{
		final ExpandableComposite excomposite = createStyleSection(parent, sc, Messages.ErrorWarningPreferencePage_potentialDrawBacks, 2);
		final Composite inner = new Composite(excomposite, SWT.NONE);
		inner.setFont(parent.getFont());
		inner.setLayout(new GridLayout(2, false));
		excomposite.setClient(inner);

		return inner;
	}

	private void addPreferenceItem(Composite parent, final String key, String description, String[] names, final List<String> ids, final String defaultValue)
	{
		SWTFactory.createLabel(parent, description, 1).setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
		final Combo combo = SWTFactory.createCombo(parent, SWT.READ_ONLY, 1, 0, names);
		defaults.add(new Pair<Combo, Integer>(combo, Integer.valueOf(ids.indexOf(defaultValue))));
		int idx = ids.indexOf(getPreference(key, defaultValue));
		combo.select(idx >= 0 ? idx : 0);
		combo.addModifyListener(new ModifyListener()
		{
			public void modifyText(ModifyEvent e)
			{
				int index = combo.getSelectionIndex();
				if (ids.get(index) != getPreference(key, defaultValue))
				{
					changes.put(key, ids.get(index));
				}
				else
				{
					changes.remove(key);
				}
			}
		});
	}

	private String getPreference(String key, String defaultValue)
	{
		return settingsNode.get(key, defaultValue);
	}

	protected ExpandableComposite createStyleSection(Composite parent, final ScrolledPage sc1, String label, int nColumns)
	{
		ExpandableComposite excomposite = new ExpandableComposite(parent, SWT.NONE, ExpandableComposite.TWISTIE | ExpandableComposite.CLIENT_INDENT);
		excomposite.setText(label);
		excomposite.setExpanded(false);
		excomposite.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT));
		excomposite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, nColumns, 1));
		excomposite.addExpansionListener(new ExpansionAdapter()
		{
			@Override
			public void expansionStateChanged(ExpansionEvent e)
			{
				sc1.reflow(true);
			}
		});
		return excomposite;
	}

	@Override
	protected void performDefaults()
	{
		changes.clear();
		for (Pair<Combo, Integer> p : defaults)
		{
			p.getLeft().select(p.getRight().intValue() >= 0 ? p.getRight().intValue() : 0);
		}
		super.performDefaults();
	}


	@Override
	public boolean performCancel()
	{
		changes.clear();
		return super.performCancel();
	}

	@Override
	public boolean performOk()
	{
		try
		{
			if (changes.size() > 0)
			{
				for (Entry<String, String> e : changes.entrySet())
				{
					settingsNode.put(e.getKey(), e.getValue());
				}
				settingsNode.flush();
				changes.clear();
				ServoyModelManager.getServoyModelManager().getServoyModel().buildActiveProjectsInJob();
			}
		}
		catch (BackingStoreException e)
		{
			ServoyLog.logError(e);
		}
		return super.performOk();
	}

	public void init(IWorkbench workbench)
	{
		// not used
	}

}