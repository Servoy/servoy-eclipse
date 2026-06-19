# Atlassian MCP Server Setup

The `opencode.json` in this project is configured to use Atlassian's official remote MCP server. This gives AI tools access to Jira and Confluence data.

## Prerequisites

- An Atlassian Cloud account with access to https://servoy-cloud.atlassian.net
- A personal MCP API token (not a regular API token)
- Your organization admin must have enabled API token authentication for the Rovo MCP server

## Step 1: Create an MCP API Token

Go to https://id.atlassian.com/manage-profile/security/api-tokens?autofillToken&expiryDays=max&appId=mcp&selectedScopes=all and create a new token with MCP scopes.

## Step 2: Set the Environment Variable

The `ATLASSIAN_AUTH_BASIC` environment variable must contain the base64-encoded value of `your-email:your-mcp-token`.

### PowerShell (Windows)

```powershell
$env:ATLASSIAN_AUTH_BASIC = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("your.email@servoy.com:YOUR_MCP_TOKEN"))
```

### Bash (Linux/Mac)

```bash
export ATLASSIAN_AUTH_BASIC=$(echo -n "your.email@servoy.com:YOUR_MCP_TOKEN" | base64)
```

Replace `your.email@servoy.com` with your Atlassian email and `YOUR_MCP_TOKEN` with the token from step 1.

To make this permanent, add the export to your shell profile (e.g. `~/.bashrc`, `~/.zshrc`) or set it as a system environment variable on Windows.

## Step 3: Start opencode

Start opencode from the same terminal where you set the environment variable. The Atlassian MCP server should now be available.

## How it works

```bash
curl -H "Authorization: Basic $ATLASSIAN_AUTH_BASIC" \
  "https://api.atlassian.com/ex/jira/7c2b3b79-12a3-4f2c-81e2-0d61b19464b3/rest/api/3/issue/SVY-12345"
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
