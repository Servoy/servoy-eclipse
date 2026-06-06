import type { Plugin } from "@opencode-ai/plugin"

const JIRA_KEY_PATTERN = /^(SVY|SVYX|SERVOY)-\d+/
const AI_SUFFIX = "[ai]"

function validateCommitMessage(message: string): string[] {
  const errors: string[] = []
  const subject = message.split("\n")[0]

  if (!JIRA_KEY_PATTERN.test(subject)) {
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

export default (async () => {
  return {
    "tool.execute.before": async (input, output) => {
      if (input.tool !== "eclipse-git_gitCommit") return

      const args = output.args as Record<string, string>
      const message = args.message
      if (!message) return

      const errors = validateCommitMessage(message)

      if (errors.length > 0) {
        throw new Error(
          `Commit message validation failed:\n${errors.map((e) => `  - ${e}`).join("\n")}\n\n` +
            `Expected format: <JIRA_KEY> <short description> [ai]\n` +
            `Example: SVY-21080 add embedded opencode terminal support [ai]`
        )
      }
    },
  }
}) satisfies Plugin
