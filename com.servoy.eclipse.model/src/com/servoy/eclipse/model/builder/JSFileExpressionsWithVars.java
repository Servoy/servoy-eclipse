/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2022 Servoy BV

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

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.dltk.ast.ASTNode;
import org.eclipse.dltk.compiler.problem.IProblemFactory;
import org.eclipse.dltk.compiler.problem.IProblemSeverityTranslator;
import org.eclipse.dltk.compiler.problem.ProblemSeverity;
import org.eclipse.dltk.core.DLTKLanguageManager;
import org.eclipse.dltk.core.builder.IBuildContext;
import org.eclipse.dltk.core.builder.IBuildParticipant;
import org.eclipse.dltk.javascript.ast.Expression;
import org.eclipse.dltk.javascript.ast.IVariableStatement;
import org.eclipse.dltk.javascript.ast.Script;
import org.eclipse.dltk.javascript.ast.Statement;
import org.eclipse.dltk.javascript.ast.VariableDeclaration;
import org.eclipse.dltk.javascript.ast.VoidExpression;
import org.eclipse.dltk.javascript.parser.Reporter;

/**
 * Class used for generating errors for multiple variable declarations in the same expression in the js file
 * @author gboros
 */
public class JSFileExpressionsWithVars extends JSFileBuildParticipantFactory
{
	/*
	 * @see com.servoy.eclipse.model.builder.JSFileBuildParticipantFactory#newBuildParticipant()
	 */
	@Override
	public IBuildParticipant newBuildParticipant()
	{
		return new ExpressionsWithVarsParser();
	}

	class ExpressionsWithVarsParser extends JSFileBuildParticipant
	{
		private static final String ERROR_MESSAGE = "More then one variable/constant declared in a line, this is not supported in Servoy, please define each variable/constant in a separate line";

		/*
		 * @see org.eclipse.dltk.core.builder.IBuildParticipant#build(org.eclipse.dltk.core.builder.IBuildContext)
		 */
		@Override
		public void build(IBuildContext context) throws CoreException
		{
			if (context.getContents() == null || context.getContents().length == 0) return;
			if (context.getSourceModule() != null && context.getSourceModule().getScriptProject() != null)
			{
				final IProblemFactory problemFactory = DLTKLanguageManager.getProblemFactory(context.getSourceModule());
				IProblemSeverityTranslator severityTranslator = problemFactory.createSeverityTranslator(context.getSourceModule().getScriptProject());
				ProblemSeverity severity = severityTranslator.getSeverity(JSFileExpressionWithVarsProblem.EXPRESSION_WITH_VARS, ProblemSeverity.WARNING);
				if (severity != null && severity.equals(ProblemSeverity.IGNORE)) return;
			}
			final Script script = getScript(context);
			if (script != null)
			{
				Reporter reporter = new Reporter(context.getLineTracker(), context.getProblemReporter());
				List<Statement> statements = script.getStatements();
				for (ASTNode node : statements)
				{
					if (node instanceof VoidExpression)
					{
						Expression exp = ((VoidExpression)node).getExpression();
						if (exp instanceof IVariableStatement)
						{
							List<VariableDeclaration> vars = ((IVariableStatement)exp).getVariables();
							if (vars.size() > 1)
							{
								reporter.reportProblem(JSFileExpressionWithVarsProblem.EXPRESSION_WITH_VARS, ERROR_MESSAGE, exp.sourceStart(), exp.sourceEnd());
							}
						}
					}
				}
			}
		}
	}
}
