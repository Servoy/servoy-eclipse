import { Plugin } from "@opencode/plugin"

const JIRA_KEY_PATTERN = /^(SVY|SVYX|SERVOY)-\d+/
const AI_SUFFIX = "[ai]"

interface CommitLintOptions {
  requireJiraKey?: boolean
}

function validateCommitMessage(message: string, options: CommitLintOptions = {}): string[] {
  const errors: string[] = []
  const subject = message.split("\n")[0]

  if (options.requireJiraKey && !JIRA_KEY_PATTERN.test(subject)) {
    errors.push(
      "Commit subject must start with a Jira case number (e.g. SVY-21080, SVYX-456, SERVOY-293)"
    )
  }

  if (!subject.trimEnd().endsWith(AI_SUFFIX)) {
    errors.push("Commit subject must end with [ai] when code is AI-generated")
  }

  if (subject.length > 100) {
    errors.push(`Commit subject is ${subject.length} chars — keep it under 100`)
  }

  return errors
}

export default Plugin.define({
  id: "servoy.commit-lint",
  async setup(ctx) {
    // When the sdd skill runs, commits must carry a Jira key. The skill is
    // invoked through the skill tool, so watch for it in execute.before.
    let requireJiraKey = false

    await ctx.tool.hook("execute.before", (event) => {
      const input = (event.input ?? {}) as Record<string, unknown>

      if (event.tool === "skill" && input.name === "sdd") {
        requireJiraKey = true
        return
      }

      if (event.tool !== "eclipse-git_gitCommit") return

      const message = typeof input.message === "string" ? input.message : undefined
      if (!message) return

      const errors = validateCommitMessage(message, { requireJiraKey })

      if (errors.length > 0) {
        throw new Error(
          `Commit message validation failed:\n${errors.map((e) => `  - ${e}`).join("\n")}\n\n` +
            (requireJiraKey
              ? `Expected format: <JIRA_KEY> <short description> [ai]\n` +
                `Example: SVY-21080 add embedded opencode terminal support [ai]`
              : `Expected format: <short description> [ai]\n` +
                `Example: update project-context docs [ai]`)
        )
      }
    })
  },
})
