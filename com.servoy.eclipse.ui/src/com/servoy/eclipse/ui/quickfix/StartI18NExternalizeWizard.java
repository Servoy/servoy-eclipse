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

package com.servoy.eclipse.ui.quickfix;

import org.eclipse.core.resources.IMarker;
import org.eclipse.dltk.compiler.problem.DefaultProblemIdentifier;
import org.eclipse.dltk.compiler.problem.IProblemIdentifier;
import org.eclipse.dltk.ui.editor.IScriptAnnotation;
import org.eclipse.dltk.ui.text.IAnnotationResolution;
import org.eclipse.dltk.ui.text.IScriptCorrectionContext;
import org.eclipse.dltk.ui.text.IScriptCorrectionProcessor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator;

import com.servoy.eclipse.core.builder.jsexternalize.JSFileExternalizeProblem;
import com.servoy.eclipse.ui.actions.ShowI18NDialogActionDelegate;

/**
 * Quick fix that starts the I18N externalize wizard
 * @author gboros
 */
public class StartI18NExternalizeWizard implements IMarkerResolutionGenerator, IScriptCorrectionProcessor
{
	private static IMarkerResolution[] NONE = new IMarkerResolution[0];

	/*
	 * @see org.eclipse.ui.IMarkerResolutionGenerator#getResolutions(org.eclipse.core.resources.IMarker)
	 */
	public IMarkerResolution[] getResolutions(IMarker marker)
	{
		IProblemIdentifier problemId = DefaultProblemIdentifier.getProblemId(marker);

		if (problemId == JSFileExternalizeProblem.NON_EXTERNALIZED_STRING)
		{
			return new IMarkerResolution[] { new RunI18NExternalizeWizardResolution() };
		}

		return NONE;
	}

	/*
	 * @see org.eclipse.dltk.ui.text.IScriptCorrectionProcessor#canFix(org.eclipse.dltk.ui.editor.IScriptAnnotation)
	 */
	public boolean canFix(IScriptAnnotation annotation)
	{
		return annotation.getId() == JSFileExternalizeProblem.NON_EXTERNALIZED_STRING;
	}

	/*
	 * @see org.eclipse.dltk.ui.text.IScriptCorrectionProcessor#canFix(org.eclipse.core.resources.IMarker)
	 */
	public boolean canFix(IMarker marker)
	{
		return false;
	}

	/*
	 * @see org.eclipse.dltk.ui.text.IScriptCorrectionProcessor#computeQuickAssistProposals(org.eclipse.dltk.ui.editor.IScriptAnnotation,
	 * org.eclipse.dltk.ui.text.IScriptCorrectionContext)
	 */
	public void computeQuickAssistProposals(IScriptAnnotation annotation, IScriptCorrectionContext context)
	{
		if (annotation.getId() == JSFileExternalizeProblem.NON_EXTERNALIZED_STRING)
		{
			context.addResolution(new RunI18NExternalizeWizardResolution(), annotation);
		}
	}

	/*
	 * @see org.eclipse.dltk.ui.text.IScriptCorrectionProcessor#computeQuickAssistProposals(org.eclipse.core.resources.IMarker,
	 * org.eclipse.dltk.ui.text.IScriptCorrectionContext)
	 */
	public void computeQuickAssistProposals(IMarker marker, IScriptCorrectionContext context)
	{
	}

	class RunI18NExternalizeWizardResolution implements IMarkerResolution, IAnnotationResolution
	{

		/*
		 * @see org.eclipse.dltk.ui.text.IAnnotationResolution#run(org.eclipse.dltk.ui.editor.IScriptAnnotation, org.eclipse.jface.text.IDocument)
		 */
		public void run(IScriptAnnotation annotation, IDocument document)
		{
			showWizard();
		}

		/*
		 * @see org.eclipse.ui.IMarkerResolution#run(org.eclipse.core.resources.IMarker)
		 */
		public void run(IMarker marker)
		{
			showWizard();
		}

		private void showWizard()
		{
			ShowI18NDialogActionDelegate delegate = new ShowI18NDialogActionDelegate();
			delegate.run(ShowI18NDialogActionDelegate.ACTION_EXTERNALIZE);
		}

		/*
		 * @see org.eclipse.ui.IMarkerResolution#getLabel()
		 */
		public String getLabel()
		{
			return "Open the 'Externalize Strings' wizard";
		}
	}
}
