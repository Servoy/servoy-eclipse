package com.servoy.eclipse.aibridge.dto;

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import com.servoy.eclipse.aibridge.AiBridgeStatus;
import com.servoy.eclipse.aibridge.AiBridgeTokenizer;

public class Completion
{
	private UUID id;
	private String status;
	private String endpoint;
	private String selection;
	private String context;
	private String cmdName;
	private Date startTime;
	private Date endTime;
	private Response response;
	private int httpCode;
	private String message;
	private String sourcePath;
	private int selectionOffset;
	private int selectionLength;
	private int tokensCount;
	private int selectionTokens;
	private int contextTokens;
	private AiBridgeTokenizer tokenizer;

	public Completion()
	{
		super();
	}

	public Completion(UUID id, String cmdName, String endpoint, String selection, String context, String sourcePath, int selectionOffset, int selectionLength)
	{
		this.id = id;
		this.cmdName = cmdName;
		this.selection = selection;
		this.endpoint = endpoint;
		this.context = context;
		this.status = AiBridgeStatus.SUBMITTED;
		this.startTime = Calendar.getInstance().getTime();
		this.sourcePath = sourcePath;
		this.selectionOffset = selectionOffset;
		this.selectionLength = selectionLength;
		this.tokenizer = AiBridgeTokenizer.getInstance();
		this.selectionTokens = tokenizer.countTokens(selection);
		this.contextTokens = tokenizer.countTokens(context);
		this.tokensCount = tokenizer.countTokens(selection + context);
	}

	public void reset()
	{
		this.startTime = Calendar.getInstance().getTime();
		response = null;
		endTime = null;
		httpCode = 0;
		message = null;
		tokensCount = selectionTokens + contextTokens;
	}

	public UUID getId()
	{
		return id;
	}

	public void setId(UUID id)
	{
		this.id = id;
	}

	public String getStatus()
	{
		return status;
	}


	public void setStatus(String status)
	{
		this.status = status;
	}


	public String getEndpoint()
	{
		return endpoint;
	}

	public void setEndpoint(String endpoint)
	{
		this.endpoint = endpoint;
	}

	public String getSelection()
	{
		return selection;
	}

	public void setSelection(String selection)
	{
		this.selection = selection;
	}


	public String getContext()
	{
		return context;
	}

	public void setContext(String context)
	{
		this.context = context;
	}


	public String getCmdName()
	{
		return cmdName;
	}

	public void setCmdName(String cmdName)
	{
		this.cmdName = cmdName;
	}

	public Date getStartTime()
	{
		return startTime;
	}

	public void setStartTime(Date startTime)
	{
		this.startTime = startTime;
	}


	public Date getEndTime()
	{
		return endTime;
	}

	public void setEndTime(Date endTime)
	{
		this.endTime = endTime;
	}

	public Response getResponse()
	{
		return response;
	}


	public void setResponse(Response response)
	{
		this.response = response;
	}

	public int getHttpCode()
	{
		return httpCode;
	}


	public void setHttpCode(int httpCode)
	{
		this.httpCode = httpCode;
	}


	public String getMessage()
	{
		return message;
	}


	public void setMessage(String message)
	{
		this.message = message;
	}

	public String getSourcePath()
	{
		return sourcePath;
	}

	public void setSourcePath(String sourcePath)
	{
		this.sourcePath = sourcePath;
	}

	public int getSelectionOffset()
	{
		return selectionOffset;
	}

	public void setSelectionOffset(int selectionOffset)
	{
		this.selectionOffset = selectionOffset;
	}

	public int getSelectionLength()
	{
		return selectionLength;
	}

	public void setSelectionLength(int selectionLength)
	{
		this.selectionLength = selectionLength;
	}

	public int getTokensCount()
	{
		return this.tokensCount + (this.response != null ? response.getTokensCount() : 0);
	}

	public void setSelectionTokensCount(int tokensCount)
	{
		this.selectionTokens = tokensCount;
	}

	public int getSelectionTokensCount()
	{
		return this.selectionTokens;
	}

	public void setContextTokensCount(int tokensCount)
	{
		this.contextTokens = tokensCount;
	}

	public int getContextTokensCount()
	{
		return this.contextTokens;
	}
}