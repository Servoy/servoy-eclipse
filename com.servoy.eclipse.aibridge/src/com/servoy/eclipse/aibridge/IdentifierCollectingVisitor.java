package com.servoy.eclipse.aibridge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.dltk.ast.ASTNode;
import org.eclipse.dltk.internal.javascript.ti.ITypeInferenceContext;
import org.eclipse.dltk.internal.javascript.ti.TypeInferencerVisitor;
import org.eclipse.dltk.javascript.ast.Identifier;
import org.eclipse.dltk.javascript.ast.PropertyExpression;
import org.eclipse.dltk.javascript.typeinference.IValueReference;

public class IdentifierCollectingVisitor extends TypeInferencerVisitor
{
	public final Map<ASTNode, IValueReference> identifiers = new HashMap<>();
	public final Map<ASTNode, List<IValueReference>> propertiesOrCalls = new HashMap<>();
	private final int offset;
	private final int length;

	public IdentifierCollectingVisitor(ITypeInferenceContext context, int offset, int length)
	{
		super(context);
		this.offset = offset;
		this.length = length;
	}

	@Override
	public IValueReference visit(ASTNode node)
	{
		final IValueReference reference = super.visit(node);
		if (reference != null && node.sourceStart() >= offset &&
			node.sourceEnd() <= (offset + length))
		{
			if (node instanceof Identifier)
			{
				identifiers.put(node, reference);
			}
			else if (node instanceof PropertyExpression pe && identifiers.containsKey(pe.getObject()))
			{
				List<IValueReference> list = propertiesOrCalls.get(pe.getObject());
				if (list == null)
				{
					list = new ArrayList<>();
					propertiesOrCalls.put(pe.getObject(), list);
				}
				list.add(reference);
			}
		}
		return reference;
	}
}