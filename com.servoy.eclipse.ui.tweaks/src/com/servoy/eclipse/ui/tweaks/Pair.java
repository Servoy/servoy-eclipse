package com.servoy.eclipse.ui.tweaks;

import java.io.Serializable;

public class Pair<L, R> implements Comparable<Pair<L, R>>, Serializable
{
	private L left;
	private R right;

	public Pair(L left, R right)
	{
		this.left = left;
		this.right = right;
	}

	/**
	 * Returns the left.
	 *
	 * @return Object
	 */
	public L getLeft()
	{
		return left;
	}

	/**
	 * Returns the right.
	 *
	 * @return Object
	 */
	public R getRight()
	{
		return right;
	}

	/**
	 * Sets the left.
	 *
	 * @param left The left to set
	 */
	public void setLeft(L left)
	{
		this.left = left;
	}

	/**
	 * Sets the right.
	 *
	 * @param right The right to set
	 */
	public void setRight(R right)
	{
		this.right = right;
	}

	@Override
	public boolean equals(Object other)
	{
		if (other != null && other instanceof Pair)
		{
			Pair< ? , ? > otherPair = (Pair< ? , ? >)other;
			return (left == otherPair.left || (left != null && left.equals(otherPair.left))) &&
				(right == otherPair.right || (right != null && right.equals(otherPair.right)));

		}
		return false;
	}

	@Override
	public int hashCode()
	{
		return 0x96ab4911 ^ (left == null ? 0 : left.hashCode()) ^ (right == null ? 0 : right.hashCode());
	}

	public int compareTo(Pair<L, R> pair)
	{
		int i = ((Comparable<L>)left).compareTo(pair.left);
		if (i != 0) return i;
		return ((Comparable<R>)right).compareTo(pair.right);
	}

	@Override
	public String toString()
	{
		StringBuffer buffer = new StringBuffer();
		buffer.append('[');
		buffer.append(left);
		buffer.append(", "); //$NON-NLS-1$
		buffer.append(right);
		buffer.append(']');
		return buffer.toString();
	}

}