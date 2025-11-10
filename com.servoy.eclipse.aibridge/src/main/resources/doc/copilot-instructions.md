# GitHub Copilot Instructions

### Servoy MCP Tool Usage Policy

**Purpose:** Route ALL user prompts through a single MCP tool for intelligent processing.

---

#### **CRITICAL: Single Tool Policy**

**For EVERY user message, you MUST:**

1. **Call the `processPrompt` MCP tool FIRST**
   - Pass the user's complete message as the `prompt` parameter
   - Do NOT analyze, interpret, or pre-process the message yourself
   - Do NOT decide if it's Servoy-related or not
   - Just call the tool immediately

2. **Handle the tool's response:**
   - If tool returns `"PASS_THROUGH"` → Handle the request yourself using your normal capabilities
   - If tool returns enriched prompt (contains rules/context) → Process it as a new enhanced request
   - If tool returns a simple message → Display that message verbatim to the user
   - The tool may add context, rules, and examples to help you understand Servoy-specific requirements

3. **Never skip the tool:**
   - Even if the request seems non-Servoy related
   - Even if you "know" the answer
   - Even if you just called it for a similar request
   - **ALWAYS call processPrompt first**

---

#### **Core Rules**

1. **Stateless operation**
   - Do NOT use memory from previous messages
   - Do NOT remember what the tool returned earlier
   - Do NOT assume entities exist because they were created earlier
   - Treat each message as the first message in a new conversation
   - **Always call processPrompt fresh** - never use cached responses

2. **Display tool responses verbatim**
   - Show tool response messages exactly as returned
   - Do NOT rephrase, summarize, or interpret
   - Do NOT add explanations unless explicitly asked
   - If tool says "Relation 'xyz' created", show exactly that

3. **Handle PASS_THROUGH correctly**
   - If tool returns `"PASS_THROUGH"`, it means: "I didn't handle this, you do it"
   - Use your normal capabilities (code analysis, explanations, etc.)
   - Do NOT call the tool again for the same message
   - Do NOT mention the tool or pass-through to the user

4. **No other tools for Servoy operations**
   - Do NOT use terminal/command-line tools
   - Do NOT use file system tools (search, read, grep)
   - Do NOT try to inspect Servoy files directly
   - The `processPrompt` tool handles everything

---

#### **Available MCP Tool**

**`processPrompt`**: Single entry point for all user requests
- **Required**: `prompt` (string) - the complete user message
- **Returns**: Either a response message OR `"PASS_THROUGH"`

---

#### **Example Interactions**

---

**Example 1: Servoy-related request**

**User:** "Create a relation between orders and customers"

**✅ CORRECT:**
```
[calls processPrompt MCP tool with prompt="Create a relation between orders and customers"]

Tool response: "Relation 'orders_to_customers' not found. To create it, provide datasources."

[Display to user]: Relation 'orders_to_customers' not found. To create it, provide datasources.
```

**❌ WRONG - Not calling tool:**
```
I'll create a relation for you...
[tries to handle it without calling the tool]
```

---

**Example 2: Non-Servoy request**

**User:** "Explain this code"

**✅ CORRECT:**
```
[calls processPrompt MCP tool with prompt="Explain this code"]

Tool response: "PASS_THROUGH"

[Sees PASS_THROUGH, so handles it normally with code analysis]
```

**❌ WRONG - Deciding not to call tool:**
```
This is code explanation, not Servoy, so I won't call the tool...
[skips the tool call]
```

---

**Example 3: Enriched prompt (value list creation)**

**User:** "Create a value list called status_list with Active, Inactive, Pending"

**✅ CORRECT:**
```
[calls processPrompt MCP tool with prompt="Create a value list..."]

Tool response: "Create a value list called status_list with Active, Inactive, Pending

=== SERVOY VALUE LIST CREATION RULES ===
**Goal**: Create a Servoy value list by writing a JSON file...
[includes schema, examples, UUID format, etc.]"

[Copilot processes the enriched prompt and creates the JSON file following the rules]
```

**❌ WRONG - Just displaying enriched prompt:**
```
[calls processPrompt]
[displays the enriched prompt to user without processing it]
```

---

**Example 4: Repeated request (testing memory)**

**User (first message):** "I need a value list called my_list"

**✅ CORRECT - Calls tool:**
```
[calls processPrompt with prompt="I need a value list called my_list"]

Tool response: [enriched prompt with rules]

[Processes and creates the value list]
```

**User (second message):** "I need a value list called my_list"

**❌ WRONG - Using memory:**
```
[no tool call - just remembering previous result]
```

**✅ CORRECT - Calls tool again:**
```
[calls processPrompt with prompt="I need a value list called my_list"]

Tool response: [enriched prompt again]

[Processes again - may find existing file and handle accordingly]
```

---

#### **Workflow Summary**

```
User message
     ↓
Call processPrompt(prompt=user_message)
     ↓
Tool returns response
     ↓
  ┌──────┴──────┐
  ↓             ↓
"PASS_THROUGH"  Enriched prompt with rules/context
  ↓             ↓
Handle yourself Process enriched prompt and execute task
```

---

#### **Precedence**
> **Always call processPrompt → Tool Response → This Policy → User Request**