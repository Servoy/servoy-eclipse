package com.servoy.eclipse.core.ai.shared;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class PineconeItem implements Serializable
{

	private final String text;
	private List<Float> embeddings;
	private final int id;
	private Map<String, String> metadata;

	public PineconeItem(int id, String text, Map<String, String> metadata)
	{
		this.id = id;
		this.text = text;
		this.metadata = metadata;
	}

	public int getId()
	{
		return id;
	}

	public String getText()
	{
		return text;
	}

	public List<Float> embeddings()
	{
		return embeddings;
	}

	public void setEmbeddings(List<Float> embeddings)
	{
		this.embeddings = embeddings;
	}

	public List<Float> getEmbeddings()
	{
		return embeddings;
	}

	public Map<String, String> getMetadata()
	{
		return metadata;
	}

	public void setMetadata(Map<String, String> metadata)
	{
		this.metadata = metadata;
	}

	@Override
	public String toString()
	{
		return "PineconeItem [ id=" + id + ", embeddings=" + (embeddings != null ? "SET" : "null") + ", metadata=" + metadata + ", text=" + text + " ]";
	}


}
