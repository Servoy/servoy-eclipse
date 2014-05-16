/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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

import org.eclipse.core.resources.IProject;
import org.eclipse.dltk.ui.preferences.AbstractConfigurationBlockPropertyAndPreferencePage;
import org.eclipse.dltk.ui.preferences.AbstractOptionsBlock;
import org.eclipse.dltk.ui.preferences.PreferenceKey;
import org.eclipse.dltk.ui.util.IStatusChangeListener;
import org.eclipse.dltk.ui.util.SWTFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;

import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.ui.quickfix.jsexternalize.PostSaveNLSRemover;

/**
 * Preference page for removing unused $NON-NLS$ tags on save
 * 
 * @author gboros
 */
public class JSFileExternalizeNLSRemoverConfigurationPage extends AbstractConfigurationBlockPropertyAndPreferencePage
{
	static final PreferenceKey[] KEYS = new PreferenceKey[] { new PreferenceKey(Activator.PLUGIN_ID, PostSaveNLSRemover.EDITOR_SAVE_PARTICIPANT_PREFIX +
		PostSaveNLSRemover.ID) };


	/*
	 * @see
	 * org.eclipse.dltk.ui.preferences.AbstractConfigurationBlockPropertyAndPreferencePage#createOptionsBlock(org.eclipse.dltk.ui.util.IStatusChangeListener,
	 * org.eclipse.core.resources.IProject, org.eclipse.ui.preferences.IWorkbenchPreferenceContainer)
	 */
	@Override
	protected AbstractOptionsBlock createOptionsBlock(IStatusChangeListener newStatusChangedListener, IProject project, IWorkbenchPreferenceContainer container)
	{
		return new JSFileExternalizeNLSRemoverConfigurationBlock(newStatusChangedListener, project, KEYS, container);
	}

	/*
	 * @see org.eclipse.dltk.ui.preferences.AbstractConfigurationBlockPropertyAndPreferencePage#getHelpId()
	 */
	@Override
	protected String getHelpId()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * @see org.eclipse.dltk.ui.preferences.AbstractConfigurationBlockPropertyAndPreferencePage#getProjectHelpId()
	 */
	@Override
	protected String getProjectHelpId()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * @see org.eclipse.dltk.ui.preferences.AbstractConfigurationBlockPropertyAndPreferencePage#setDescription()
	 */
	@Override
	protected void setDescription()
	{
		// TODO Auto-generated method stub
	}

	/*
	 * @see org.eclipse.dltk.ui.preferences.AbstractConfigurationBlockPropertyAndPreferencePage#setPreferenceStore()
	 */
	@Override
	protected void setPreferenceStore()
	{
		// TODO Auto-generated method stub

	}

	/*
	 * @see org.eclipse.dltk.internal.ui.preferences.PropertyAndPreferencePage#getPreferencePageId()
	 */
	@Override
	protected String getPreferencePageId()
	{
		return "org.eclipse.dltk.javascript.editor.preferences.removeNLS";
	}

	/*
	 * @see org.eclipse.dltk.internal.ui.preferences.PropertyAndPreferencePage#getPropertyPageId()
	 */
	@Override
	protected String getPropertyPageId()
	{
		return "org.eclipse.dltk.javascript.editor.propertyPage.removeNLS";
	}

	class JSFileExternalizeNLSRemoverConfigurationBlock extends AbstractOptionsBlock
	{

		public JSFileExternalizeNLSRemoverConfigurationBlock(IStatusChangeListener context, IProject project, PreferenceKey[] allKeys,
			IWorkbenchPreferenceContainer container)
		{
			super(context, project, allKeys, container);
		}

		@Override
		protected Control createOptionsBlock(Composite parent)
		{
			final Composite pageComponent = new Composite(parent, SWT.NULL);
			pageComponent.setLayout(GridLayoutFactory.swtDefaults().create());
			pageComponent.setFont(parent.getFont());
			pageComponent.setLayoutData(new GridData(GridData.FILL_BOTH));

			final Button formatOnSave = SWTFactory.createCheckButton(pageComponent, "Remove unused $NON-NLS$ tags on save");
			bindControl(formatOnSave, JSFileExternalizeNLSRemoverConfigurationPage.KEYS[0], null);

			return pageComponent;
		}
	}
}
