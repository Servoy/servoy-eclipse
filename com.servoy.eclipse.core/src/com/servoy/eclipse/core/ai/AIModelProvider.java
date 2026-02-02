package com.servoy.eclipse.core.ai;

public interface AIModelProvider
{
	public static final String EXTENSION_ID = "com.servoy.eclipse.core.aiprovider";

	/**
	 * This creates a ChatModel with the given system prompt.
	 *
	 * @param systemPrompt
	 *
	 * @return the created ChatModel
	 */
	public ChatModel createChatModel(String systemPrompt);

}
