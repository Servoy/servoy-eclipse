# Atlassian Jira API Setup

This project uses the Jira REST API (v3) with Basic authentication for all Jira access:
reading issues, downloading attachments, searching, and posting comments.

## Prerequisites

- An Atlassian Cloud account with access to https://servoy-cloud.atlassian.net
- A personal API token (standard, no special scopes needed)

## Step 1: Create an API Token

Go to https://id.atlassian.com/manage-profile/security/api-tokens and create a new token.
This is a regular API token — no special scopes or MCP-specific token needed.

## Step 2: Set the Environment Variable

The `ATLASSIAN_AUTH_BASIC` environment variable must contain the **base64-encoded** value
of `your-email:your-api-token`.

### PowerShell (Windows)

```powershell
$env:ATLASSIAN_AUTH_BASIC = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("your.email@servoy.com:YOUR_API_TOKEN"))
```

### Bash (Linux/Mac)

```bash
export ATLASSIAN_AUTH_BASIC=$(echo -n "your.email@servoy.com:YOUR_API_TOKEN" | base64)
```

To make this permanent, add it to your shell profile or set it as a system environment variable.

## Step 3: opencode.json Configuration

No Atlassian MCP server is needed. Remove any `"atlassian"` entry from the `"mcp"` section
in `opencode.json`. All Jira access goes through the REST API using curl with Basic auth.

## How It Works

The SDD pipeline (and any agent needing Jira access) uses curl against the direct
tenant URL with the Basic auth header:

```bash
curl -H "Authorization: Basic $ATLASSIAN_AUTH_BASIC" \
  "https://servoy-cloud.atlassian.net/rest/api/3/issue/SVY-12345"
```

### Common API Endpoints

| Action | Endpoint |
|--------|----------|
| Get issue | `GET /rest/api/3/issue/{issueKey}` |
| Get issue with specific fields | `GET /rest/api/3/issue/{issueKey}?fields=summary,description,comment,attachment` |
| Download attachment | `GET /rest/api/3/attachment/content/{attachmentId}` |
| Search (JQL) | `GET /rest/api/3/search?jql={jql}` |
| Get comments | `GET /rest/api/3/issue/{issueKey}/comment` |
| Add comment | `POST /rest/api/3/issue/{issueKey}/comment` |

All endpoints are relative to `https://servoy-cloud.atlassian.net`.

## Why Not the Atlassian MCP Server?

We tested the official Atlassian MCP server at `mcp.atlassian.com`. Issues found:

1. **OAuth-only** — requires `opencode mcp auth atlassian` browser flow, tokens expire
2. **No attachment downloads** — the MCP toolset has no tool to download attachment content
3. **Search broken** — returns 403 "The app is not installed on this instance"
4. **Less reliable** — the REST API with Basic auth works for everything, including
   private security-level issues, attachments, and search

The REST API with a standard API token is simpler, more complete, and more reliable.

## Troubleshooting

- **401 Unauthorized** — your API token is expired or invalid. Create a new one at the URL above.
- **403 Forbidden** — you don't have permission to access that resource (e.g., private issue you're not on).
- **404 Not Found** — the issue key or attachment ID doesn't exist.
