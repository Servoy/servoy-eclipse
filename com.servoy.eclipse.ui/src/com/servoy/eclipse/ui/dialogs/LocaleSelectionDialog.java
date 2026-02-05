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

import java.util.Locale;
import java.util.TreeMap;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.servoy.j2db.util.Utils;

/**
 * Dialog for selecting a locale with language and country options.
 */
public class LocaleSelectionDialog extends Dialog
{
	private ComboViewer languageComboViewer;
	private ComboViewer countryComboViewer;
	private Locale selectedLocale;

	private TreeMap<String, String> availableLanguages = null;
	private TreeMap<String, String> availableCountries = null;

	public LocaleSelectionDialog(Shell parentShell)
	{
		super(parentShell);
	}

	@Override
	protected void configureShell(Shell newShell)
	{
		super.configureShell(newShell);
		newShell.setText("Select Locale");
	}

	@Override
	protected Control createDialogArea(Composite parent)
	{
		Composite container = (Composite)super.createDialogArea(parent);
		container.setLayout(new GridLayout(2, false));

		// Language label and combo
		Label languageLabel = new Label(container, SWT.NONE);
		languageLabel.setText("Language:");
		languageLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));

		languageComboViewer = new ComboViewer(container, SWT.READ_ONLY);
		languageComboViewer.setContentProvider(new ArrayContentProvider());
		GridData languageData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		languageData.widthHint = 200;
		languageComboViewer.getCombo().setLayoutData(languageData);

		// Country label and combo
		Label countryLabel = new Label(container, SWT.NONE);
		countryLabel.setText("Country:");
		countryLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));

		countryComboViewer = new ComboViewer(container, SWT.READ_ONLY);
		countryComboViewer.setContentProvider(new ArrayContentProvider());
		GridData countryData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		countryData.widthHint = 200;
		countryComboViewer.getCombo().setLayoutData(countryData);

		// Initialize data
		initializeComboViewers();

		// Add selection listeners to update the selected locale
		languageComboViewer.addSelectionChangedListener(new ISelectionChangedListener()
		{
			@Override
			public void selectionChanged(SelectionChangedEvent event)
			{
				updateSelectedLocale();
			}
		});

		countryComboViewer.addSelectionChangedListener(new ISelectionChangedListener()
		{
			@Override
			public void selectionChanged(SelectionChangedEvent event)
			{
				updateSelectedLocale();
			}
		});

		return container;
	}

	private void initializeComboViewers()
	{
		// Populate language combo
		TreeMap<String, String> languages = getAvailableLanguages();
		languageComboViewer.setInput(languages.keySet().toArray());

		// Populate country combo
		TreeMap<String, String> countries = getAvailableCountries();
		countryComboViewer.setInput(countries.keySet().toArray());

		// Set initial selections
		if (!languages.isEmpty())
		{
			String firstLanguage = languages.keySet().iterator().next();
			languageComboViewer.setSelection(new StructuredSelection(firstLanguage));
		}

		if (!countries.isEmpty())
		{
			// Try to find an empty country option first, otherwise select the first one
			if (countries.containsKey(""))
			{
				countryComboViewer.setSelection(new StructuredSelection(""));
			}
			else
			{
				String firstCountry = countries.keySet().iterator().next();
				countryComboViewer.setSelection(new StructuredSelection(firstCountry));
			}
		}

		updateSelectedLocale();
	}

	private TreeMap<String, String> getAvailableLanguages()
	{
		if (availableLanguages == null)
		{
			availableLanguages = new TreeMap<String, String>();
			Locale[] availableLocales = Locale.getAvailableLocales();
			for (Locale locale : availableLocales)
			{
				if (Utils.stringIsEmpty(locale.getDisplayLanguage())) continue;
				availableLanguages.put(locale.getDisplayLanguage(), locale.getLanguage());
			}
		}
		return availableLanguages;
	}

	private TreeMap<String, String> getAvailableCountries()
	{
		if (availableCountries == null)
		{
			availableCountries = new TreeMap<String, String>();
			// Add an empty country option
			availableCountries.put("", "");
			
			Locale[] availableLocales = Locale.getAvailableLocales();
			for (Locale locale : availableLocales)
			{
				if (!Utils.stringIsEmpty(locale.getDisplayCountry()))
				{
					availableCountries.put(locale.getDisplayCountry(), locale.getCountry());
				}
			}
		}
		return availableCountries;
	}

	private void updateSelectedLocale()
	{
		IStructuredSelection languageSelection = (IStructuredSelection)languageComboViewer.getSelection();
		IStructuredSelection countrySelection = (IStructuredSelection)countryComboViewer.getSelection();

		if (!languageSelection.isEmpty() && !countrySelection.isEmpty())
		{
			String selectedLanguageDisplay = (String)languageSelection.getFirstElement();
			String selectedCountryDisplay = (String)countrySelection.getFirstElement();

			String languageCode = getAvailableLanguages().get(selectedLanguageDisplay);
			String countryCode = getAvailableCountries().get(selectedCountryDisplay);

			if (languageCode != null)
			{
				if (countryCode != null && !countryCode.isEmpty())
				{
					selectedLocale = new Locale(languageCode, countryCode);
				}
				else
				{
					selectedLocale = new Locale(languageCode);
				}
			}
		}
	}

	/**
	 * Returns the selected locale, or null if no valid locale is selected.
	 */
	public Locale getSelectedLocale()
	{
		return selectedLocale;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent)
	{
		// Create OK and Cancel buttons
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	@Override
	protected void okPressed()
	{
		// Ensure we have a valid locale selected before closing
		if (selectedLocale != null)
		{
			super.okPressed();
		}
	}
}