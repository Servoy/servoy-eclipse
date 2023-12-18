package com.servoy.eclipse.aibridge.dto;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.servoy.eclipse.aibridge.AiBridgeManager;
import com.servoy.eclipse.aibridge.AiBridgeStatus;
import com.servoy.eclipse.aibridge.AiBridgeTokenizer;
import com.servoy.eclipse.model.util.ServoyLog;

public class Completion
{
	//max length of the selection and context fields;
	private static int MAX_LENGTH = 256;

	private UUID id;
	private String status;
	private String selection;
	private String context;
	private String cmdName;
	private Date startTime;
	private Date endTime;
	private Response response;
	private int httpCode;
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

	public Completion(
		UUID id, String cmdName, String selection,
		String context, String sourcePath,
		int selectionOffset, int selectionLength)
	{
		this.id = id;
		this.cmdName = cmdName;
		this.selection = selection;
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
		this.response = new Response("Processing ...");
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

	public String getSelection()
	{
		return selection;
	}

	//called on deserialization only
	public void setSelection(String selection)
	{
		this.selection = selection;
		if (tokenizer == null) tokenizer = AiBridgeTokenizer.getInstance();
		this.tokensCount = tokenizer.countTokens(selection + context);
	}


	public String getContext()
	{
		return context;
	}

	//called on deserialization only
	public void setContext(String context)
	{
		this.context = context;
		if (tokenizer == null) tokenizer = AiBridgeTokenizer.getInstance();
		this.tokensCount = tokenizer.countTokens(selection + context);
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
		this.tokensCount = this.selectionTokens + contextTokens;
	}

	public int getSelectionTokensCount()
	{
		return this.selectionTokens;
	}

	public void setContextTokensCount(int tokensCount)
	{
		this.contextTokens = tokensCount;
		this.tokensCount = selectionTokens + this.contextTokens;
	}

	public int getContextTokensCount()
	{
		return this.contextTokens;
	}

	@JsonIgnore
	public String getMessage()
	{
		return response != null ? response.getResponseMessage() : "";
	}

	@JsonIgnore
	public void setMessage(String message)
	{
		if (response == null)
		{
			response = new Response();
		}
		response.setResponseMessage(message);
	}

	@JsonIgnore
	public Completion fullReset()
	{
		this.startTime = Calendar.getInstance().getTime();
		response = null;
		endTime = null;
		httpCode = 0;
		tokensCount = selectionTokens + contextTokens;
		return this;
	}

	@JsonIgnore
	public Completion partialReset()
	{
		this.selection = this.selection != null && this.selection.length() > MAX_LENGTH ? this.selection.substring(0, MAX_LENGTH) : this.selection;
		this.context = this.context != null && this.context.length() > MAX_LENGTH ? this.context.substring(0, MAX_LENGTH) : this.selection;
		this.response = this.response != null ? this.response.partialReset() : null;
		return this;
	}

	@JsonIgnore
	public Completion getFullCompletion()
	{
		ObjectMapper mapper = new ObjectMapper();
		Path completionPath = AiBridgeManager.getInstance().getCompletionPath(id);
		if (Files.exists(completionPath))
		{
			try
			{
				String json = new String(Files.readAllBytes(completionPath));
				Completion myCompletion = mapper.readValue(json, Completion.class);
				return myCompletion;
			}
			catch (IOException | IllegalArgumentException e)
			{
				// Handle the IOException
				ServoyLog.logError(e);
			}
		}
		return this;
	}
}