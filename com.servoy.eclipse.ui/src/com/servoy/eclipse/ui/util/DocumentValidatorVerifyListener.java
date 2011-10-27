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
package com.servoy.eclipse.ui.util;

import javax.swing.text.AbstractDocument.Content;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import javax.swing.text.StringContent;

import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.widgets.Text;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.util.docvalidator.IdentDocumentValidator;
import com.servoy.j2db.util.docvalidator.NumberDocumentValidator;
import com.servoy.j2db.util.docvalidator.ValidatingDocument;
import com.servoy.j2db.util.docvalidator.ValidatingDocument.IDocumentValidator;

/**
 * Verify listener that uses document validators for validating changes.
 * 
 * @author rgansevles
 * 
 */
public class DocumentValidatorVerifyListener implements VerifyListener
{
	public static final DocumentValidatorVerifyListener IDENT_SQL_VERIFIER = new DocumentValidatorVerifyListener(new IdentDocumentValidator(
		IdentDocumentValidator.TYPE_SQL));
	public static final DocumentValidatorVerifyListener IDENT_SERVOY_VERIFIER = new DocumentValidatorVerifyListener(new IdentDocumentValidator(
		IdentDocumentValidator.TYPE_SERVOY));
	public static final DocumentValidatorVerifyListener NUMBER_VERIFIER = new DocumentValidatorVerifyListener(new NumberDocumentValidator());

	private final IDocumentValidator[] validators;

	public DocumentValidatorVerifyListener(IDocumentValidator[] validators)
	{
		this.validators = validators;
	}

	public DocumentValidatorVerifyListener(IDocumentValidator validator)
	{
		this(new IDocumentValidator[] { validator });
	}

	public void verifyText(VerifyEvent e)
	{
		String value = null;
		if (e.getSource() instanceof Text)
		{
			value = ((Text)e.getSource()).getText();
		}
		else if (e.getSource() instanceof CCombo)
		{
			value = ((CCombo)e.getSource()).getText();
		}
		if (value != null)
		{
			try
			{
				Content content = new StringContent();
				content.insertString(0, value);
				ValidatingDocument checkedNewContent = new ValidatingDocument(content, validators);
				checkedNewContent.replace(e.start, e.end - e.start, e.text, null);

				content = new StringContent();
				content.insertString(0, value);
				PlainDocument uncheckedNewContent = new PlainDocument(content);
				uncheckedNewContent.replace(e.start, e.end - e.start, e.text, null);

				e.doit = uncheckedNewContent.getText(0, uncheckedNewContent.getLength()).equals(checkedNewContent.getText(0, checkedNewContent.getLength()));
			}
			catch (BadLocationException ex)
			{
				ServoyLog.logError(ex);
				e.doit = false;
			}
		}
	}
}
