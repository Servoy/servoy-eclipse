package com.servoy.eclipse.debug.script;

import org.eclipse.dltk.ast.ASTNode;
import org.eclipse.dltk.core.IModelElement;
import org.eclipse.dltk.javascript.ast.FunctionStatement;
import org.eclipse.dltk.javascript.ast.JSNode;
import org.eclipse.dltk.javascript.ast.Script;
import org.eclipse.dltk.javascript.parser.JSProblemReporter;
import org.eclipse.dltk.javascript.parser.NodeTransformer;

import com.servoy.eclipse.model.repository.SolutionSerializer;

public class CalculationsNodeTransformerFactory implements NodeTransformer.Factory
{
	public NodeTransformer create(IModelElement element, JSProblemReporter reporter)
	{
		if (element != null && SolutionSerializer.getDataSourceForCalculationJSFile(element.getResource()) != null)
		{
			return new NodeTransformer()
			{
				public ASTNode transform(ASTNode node, JSNode parent)
				{
					if (node instanceof FunctionStatement && parent instanceof Script)
					{
						((Script)parent).getDeclarations().remove(node);
					}
					return null;
				}
			};
		}
		return null;
	}
}
