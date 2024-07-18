/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2023 Servoy BV

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

package com.servoy.eclipse.aibridge.editors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.texteditor.AbstractDocumentProvider;

public class StringDocumentProvider extends AbstractDocumentProvider
{
	private final String content;

	public StringDocumentProvider(String content)
	{
		super();
		this.content = content;
	}

	@Override
	protected IDocument createDocument(Object element) throws CoreException
	{
		if (element instanceof DualEditorInput)
		{
			Document document = new Document(content);
			return document;
		}
		return new Document();
	}

	@Override
	protected void doSaveDocument(IProgressMonitor monitor, Object element, IDocument document, boolean overwrite) throws CoreException
	{
		// This is a read-only editor, so no save operation is needed.
	}

	@Override
	protected IAnnotationModel createAnnotationModel(Object element) throws CoreException
	{
		return new AnnotationModel();
	}

	@Override
	protected ElementInfo createElementInfo(Object element) throws CoreException
	{
		IDocument document = createDocument(element);
		IAnnotationModel annotationModel = createAnnotationModel(element);
		return new ElementInfo(document, annotationModel);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.ui.texteditor.AbstractDocumentProvider#getOperationRunner(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected IRunnableContext getOperationRunner(IProgressMonitor monitor)
	{
		// TODO Auto-generated method stub
		return null;
	}

}
