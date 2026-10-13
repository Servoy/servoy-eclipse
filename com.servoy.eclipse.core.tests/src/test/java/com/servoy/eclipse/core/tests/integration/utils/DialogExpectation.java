/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 2026 Servoy BV

 This program is free software; you can redistribute it and/or modify it under
 the terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along
 with this program; if not, see http://www.gnu.org/licenses or write to the Free
 Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package com.servoy.eclipse.core.tests.integration.utils;

/**
 * Describes a dialog that a test expects to appear during execution.
 * <p>
 * Use the fluent {@link Builder} to construct instances:
 * <pre>
 *   TestDialogInterceptor.expect(
 *       DialogExpectation.withTitleContaining("Confirm Delete")
 *           .messageContains("Are you sure")
 *           .clickButtonContains("OK")
 *   );
 * </pre>
 *
 * Matching is done by substring (case-insensitive) on both title and message.
 * All match fields are optional; omitting them means "match any".
 * 
 * NOTE: this class is copied/duplicated into other repos. with name DialogExpectationCopy; it is duplicated in order to avoid cross repo
 * dependencies on test projects which would require more configuration for Jenkins (one more feature, update site etc.).
 * Keep this original and all the copies in sync pls.
 */
public class DialogExpectation
{
	/** Substring that must appear in the shell title, or {@code null} to match any title. */
	final String titleContains;

	/** Substring that must appear in a label inside the shell, or {@code null} to match any message. */
	final String messageContains;

	/**
	 * Text to type into the first editable {@code Text} widget found in the shell.
	 * {@code null} means do not touch the text field.
	 */
	final String inputText;

	/**
	 * Item text to select in the first {@code Combo} widget found in the shell.
	 * {@code null} means do not touch the combo.
	 */
	final String comboSelectionContains;

	/**
	 * State to set on the first {@code SWT.CHECK} {@code Button} found in the shell.
	 * {@code null} means do not touch the checkbox.
	 */
	final Boolean checkboxState;

	/**
	 * Text of the push-button to click before the dialog is dismissed.
	 * {@code null} means just close the shell without clicking any button.
	 */
	final String buttonTextContains;

	private DialogExpectation(Builder b)
	{
		this.titleContains = b.titleContains;
		this.messageContains = b.messageContains;
		this.inputText = b.inputText;
		this.comboSelectionContains = b.comboSelectionContains;
		this.checkboxState = b.checkboxState;
		this.buttonTextContains = b.buttonTextContains;
	}

	/**
	 * Shorthand to start a builder pre-seeded with the given title substring.
	 */
	public static Builder withTitleContaining(String titleContains)
	{
		return new Builder().titleContains(titleContains);
	}

	/**
	 * Start a builder that matches any dialog (no title or message constraint).
	 */
	public static Builder any()
	{
		return new Builder();
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder("DialogExpectation{");
		if (titleContains != null) sb.append("title~'").append(titleContains).append("' ");
		if (messageContains != null) sb.append("msg~'").append(messageContains).append("' ");
		if (inputText != null) sb.append("input='").append(inputText).append("' ");
		if (comboSelectionContains != null) sb.append("combo='").append(comboSelectionContains).append("' ");
		if (checkboxState != null) sb.append("checkbox=").append(checkboxState).append(" ");
		if (buttonTextContains != null) sb.append("click='").append(buttonTextContains).append("'");
		else sb.append("close");
		sb.append("}");
		return sb.toString();
	}

	// -------------------------------------------------------------------------

	public static class Builder
	{
		private String titleContains;
		private String messageContains;
		private String inputText;
		private String comboSelectionContains;
		private Boolean checkboxState;
		private String buttonTextContains;

		private Builder()
		{
		}

		/** Match dialogs whose title contains this substring (case-insensitive). */
		public Builder titleContains(String title)
		{
			this.titleContains = title;
			return this;
		}

		/** Match dialogs that have a label containing this substring (case-insensitive). */
		public Builder messageContains(String message)
		{
			this.messageContains = message;
			return this;
		}

		/** Type this text into the first editable text field found in the dialog. */
		public Builder typeInField(String text)
		{
			this.inputText = text;
			return this;
		}

		/** Select this item in the first combo box found in the dialog. */
		public Builder selectInCombo(String item)
		{
			this.comboSelectionContains = item;
			return this;
		}

		/** Set the first checkbox found in the dialog to the given state. */
		public Builder setCheckbox(boolean checked)
		{
			this.checkboxState = checked;
			return this;
		}

		/**
		 * Click the push-button whose text matches the given string (case-insensitive).
		 * This will dismiss the dialog via that button's selection event.
		 */
		public Builder clickButtonContains(String text)
		{
			this.buttonTextContains = text;
			return this;
		}

		/**
		 * Just close the shell without clicking any specific button
		 * (equivalent to pressing Escape / the window close button).
		 */
		public Builder close()
		{
			this.buttonTextContains = null;
			return this;
		}

		public DialogExpectation build()
		{
			return new DialogExpectation(this);
		}
	}
}
