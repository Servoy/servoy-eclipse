package com.servoy.eclipse.aibridge.dto;

import org.json.JSONObject;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.servoy.eclipse.aibridge.AiBridgeTokenizer;

public class Response
{
	private String responseMessage;
	private String responseFunction;
	private String responseType;
	private boolean showPasteInCode;
	private boolean showCopyToClipboard;
	private boolean showContinueChat;
	private String chatID;
	private int tokensCount;

	public Response()
	{
		super();
	}

	public Response(JSONObject jsonResponse)
	{
		super();

		this.responseMessage = jsonResponse.optString("responseMessage");
		this.responseFunction = jsonResponse.optString("responseFunction");
		this.responseType = jsonResponse.optString("responseType");
		this.showPasteInCode = jsonResponse.optBoolean("showPasteInCode");
		this.showCopyToClipboard = jsonResponse.optBoolean("showCopyToClipboard");
		this.showContinueChat = jsonResponse.optBoolean("showContinueChat");
		this.chatID = jsonResponse.optString("chatID");
		this.tokensCount = AiBridgeTokenizer.getInstance().countTokens(this.responseMessage);
	}

	@JsonIgnore
	public boolean isEmptyResponse()
	{
		return (responseMessage == null || responseMessage.isEmpty()) &&
			(responseFunction == null || responseFunction.isEmpty()) &&
			(responseType == null || responseType.isEmpty()) &&
			(chatID == null || chatID.isEmpty()) &&
			!showPasteInCode &&
			!showCopyToClipboard &&
			!showContinueChat;
	}

	public String getResponseMessage()
	{
		return responseMessage == null ? "" : responseMessage;
	}

	public String getResponseFunction()
	{
		return responseFunction;
	}

	public String getResponseType()
	{
		return responseType;
	}

	public boolean isShowPasteInCode()
	{
		return showPasteInCode;
	}

	public boolean isShowCopyToClipboard()
	{
		return showCopyToClipboard;
	}

	public boolean isShowContinueChat()
	{
		return showContinueChat;
	}

	public String getChatID()
	{
		return chatID;
	}

	public int getTokensCount()
	{
		return this.tokensCount;
	}

	public void setResponseMessage(String responseMessage)
	{
		this.responseMessage = responseMessage;
		this.tokensCount = AiBridgeTokenizer.getInstance().countTokens(responseMessage);
	}

	public void setResponseFunction(String responseFunction)
	{
		this.responseFunction = responseFunction;
	}

	public void setResponseType(String responseType)
	{
		this.responseType = responseType;
	}

	public void setShowPasteInCode(boolean showPasteInCode)
	{
		this.showPasteInCode = showPasteInCode;
	}

	public void setShowCopyToClipboard(boolean showCopyToClipboard)
	{
		this.showCopyToClipboard = showCopyToClipboard;
	}

	public void setShowContinueChat(boolean showContinueChat)
	{
		this.showContinueChat = showContinueChat;
	}

	public void setChatID(String chatID)
	{
		this.chatID = chatID;
	}
}