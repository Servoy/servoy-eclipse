package com.servoy.eclipse.aibridge;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.dltk.ast.ASTNode;
import org.eclipse.dltk.internal.javascript.ti.ITypeInferenceContext;
import org.eclipse.dltk.internal.javascript.ti.TypeInferencerVisitor;
import org.eclipse.dltk.javascript.ast.Identifier;
import org.eclipse.dltk.javascript.typeinference.IValueReference;

public class IdentifierCollectingVisitor extends TypeInferencerVisitor
{
	public final Map<ASTNode, IValueReference> bindings = new HashMap<ASTNode, IValueReference>();
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
		if (reference != null && node instanceof Identifier &&
			node.sourceStart() >= offset &&
			node.sourceEnd() <= (offset + length))
		{
			bindings.put(node, reference);
		}
		return reference;
	}
}