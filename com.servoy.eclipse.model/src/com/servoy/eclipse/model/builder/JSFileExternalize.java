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

package com.servoy.eclipse.model.builder;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.dltk.ast.ASTNode;
import org.eclipse.dltk.compiler.problem.IProblemFactory;
import org.eclipse.dltk.compiler.problem.IProblemIdentifier;
import org.eclipse.dltk.compiler.problem.IProblemSeverityTranslator;
import org.eclipse.dltk.compiler.problem.ProblemSeverity;
import org.eclipse.dltk.core.DLTKLanguageManager;
import org.eclipse.dltk.core.builder.IBuildContext;
import org.eclipse.dltk.core.builder.IBuildParticipant;
import org.eclipse.dltk.javascript.ast.Comment;
import org.eclipse.dltk.javascript.ast.FunctionStatement;
import org.eclipse.dltk.javascript.ast.IVariableStatement;
import org.eclipse.dltk.javascript.ast.JSNode;
import org.eclipse.dltk.javascript.ast.Script;
import org.eclipse.dltk.javascript.ast.StringLiteral;
import org.eclipse.dltk.javascript.parser.Reporter;

import com.servoy.eclipse.model.util.ServoyLog;

/**
 * Class used for generating warnings for non-externalized strings in the js file
 * @author gboros
 */
public class JSFileExternalize extends JSFileBuildParticipantFactory
{
	/*
	 * @see com.servoy.eclipse.model.builder.JSFileBuildParticipantFactory#newBuildParticipant()
	 */
	@Override
	public IBuildParticipant newBuildParticipant()
	{
		return new StringExternalizeParser();
	}

	class StringExternalizeParser extends JSFileBuildParticipant
	{
		private int numberOfWarningsPerBlock = 0;

		private static final String WARNING_MESSAGE = "Non-externalized string literal; it should be followed by ";
		private static final String SUPPRESS_WARNING_NLS = "@SuppressWarnings(nls)";

		/*
		 * @see org.eclipse.dltk.core.builder.IBuildParticipant#build(org.eclipse.dltk.core.builder.IBuildContext)
		 */
		public void build(IBuildContext context) throws CoreException
		{
			if (context.getContents() == null || context.getContents().length == 0) return;
			if (context.getSourceModule() != null && context.getSourceModule().getScriptProject() != null)
			{
				final IProblemFactory problemFactory = DLTKLanguageManager.getProblemFactory(context.getSourceModule());
				IProblemSeverityTranslator severityTranslator = problemFactory.createSeverityTranslator(context.getSourceModule().getScriptProject());
				ProblemSeverity severity = severityTranslator.getSeverity(JSFileExternalizeProblem.NON_EXTERNALIZED_STRING, ProblemSeverity.WARNING);
				if (severity != null && severity.equals(ProblemSeverity.IGNORE)) return;
			}
			final Script script = getScript(context);
			if (script != null)
			{
				checkForNonExternalizedString(script, new Reporter(context.getLineTracker(), context.getProblemReporter()), context.getContents());
			}
		}

		private void reportWarning(Reporter reporter, int sourceStart, int sourceStop, int stringLiteralIdx)
		{
			if (reporter != null)
			{
				numberOfWarningsPerBlock++;
				reporter.reportProblem(JSFileExternalizeProblem.NON_EXTERNALIZED_STRING, WARNING_MESSAGE + "//$NON-NLS-" + stringLiteralIdx + "$", sourceStart,
					sourceStop);
			}
		}

		private void checkForNonExternalizedString(Script script, final Reporter reporter, final char[] content)
		{
			try
			{
				script.traverse(new StringLiteralVisitor(content)
				{
					private final List<IProblemIdentifier> suppressProblems = Arrays.asList(
						new IProblemIdentifier[] { JSFileExternalizeProblem.NON_EXTERNALIZED_STRING });

					/*
					 * @see com.servoy.eclipse.core.builder.StringLiteralVisitor#visitStringLiteral(org.eclipse.dltk.javascript.ast.StringLiteral, int, int,
					 * int)
					 */
					@Override
					public boolean visitStringLiteral(StringLiteral node, int lineStartIdx, int lineEndIdx, int stringLiteralIdx)
					{
						if (!isMarkedForSkip(new String(content, lineStartIdx, lineEndIdx - lineStartIdx + 1), stringLiteralIdx))
							reportWarning(reporter, node.sourceStart(), node.sourceEnd(), stringLiteralIdx);
						return true;
					}

					@Override
					public boolean visitGeneral(ASTNode node) throws Exception
					{
						if (node instanceof JSNode)
						{
							Comment comment = ((JSNode)node).getDocumentation();
							if (comment != null)
							{
								String commentStr = comment.getText();
								if (commentStr != null && commentStr.indexOf(SUPPRESS_WARNING_NLS) != -1)
								{
									return false;
								}
							}
						}
						if (numberOfWarningsPerBlock > 100)
						{
							return false;
						}
						return super.visitGeneral(node);
					}

					@Override
					public void endvisitGeneral(ASTNode node) throws Exception
					{
						if (node instanceof FunctionStatement || node instanceof IVariableStatement)
						{
							numberOfWarningsPerBlock = 0;
						}
						super.endvisitGeneral(node);
					}

					private boolean isMarkedForSkip(String nodeLine, int nodeLineIdx)
					{
						return nodeLine.indexOf(new StringBuilder("//$NON-NLS-").append(nodeLineIdx).append("$").toString()) != -1;
					}
				});
			}
			catch (Exception ex)
			{
				ServoyLog.logError(ex);
			}
		}
	}
}