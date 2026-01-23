# Clojure-MCP Quick Start

## Start nREPL Server

```bash
clojure -M:nrepl
```

This starts an nREPL server on port 7888 with CIDER middleware.

## Claude Desktop Configuration

Edit `~/Library/Application Support/Claude/claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "clojure-mcp": {
      "command": "/bin/zsh",
      "args": [
        "-c",
        "clojure -Tmcp start :not-cwd true :port 7888"
      ]
    }
  }
}
```

**Note**: Change `/bin/zsh` to your shell path (`which zsh` or `which bash`)

## Claude Code Configuration

```bash
claude mcp add clojure-mcp -- clojure -Tmcp start \
  :start-nrepl-cmd '["clojure" "-M:nrepl"]'
```

## Context Files

When starting a conversation with an AI assistant, load these resources:

1. **PROJECT_SUMMARY.md** - Project overview, architecture, API docs
2. **LLM_CODE_STYLE.md** - Coding standards and Polylith rules
3. **AGENTS.md** - Operational guidelines for AI agents

## Available Commands

| Command | Description |
|---------|-------------|
| `clojure -M:nrepl` | Start nREPL server on port 7888 |
| `clojure -Tmcp start :port 7888` | Start clojure-mcp server |
| `clojure -M:poly check` | Verify Polylith architecture |
| `clojure -M:poly test :dev` | Run all tests |

## Troubleshooting

**nREPL won't start**:
```bash
lsof -i :7888              # Check if port is in use
pkill -f "clojure.*nrepl"  # Kill existing nREPL
clojure -M:nrepl           # Try again
```

**Claude Desktop can't connect**:
1. Verify nREPL is running: `cat .nrepl-port`
2. Check port matches config (7888)
3. Restart Claude Desktop

## Full Documentation

See [docs/clojure-mcp-integration.md](../docs/clojure-mcp-integration.md) for complete guide.
