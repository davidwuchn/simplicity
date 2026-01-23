# Clojure-MCP Integration Guide

This document explains how to use **clojure-mcp** (Model Context Protocol) with the Simplicity project for AI-assisted development.

## What is Clojure-MCP?

**Clojure-MCP** is an MCP server that connects AI coding assistants (like Claude Desktop, Claude Code, or other LLM clients) to your Clojure project, providing:

- **REPL Integration**: Evaluate Clojure code in a running nREPL session
- **Clojure-Aware Editing**: Structure-aware editing with automatic delimiter repair
- **Project Context**: AI assistants can read project files, understand architecture, and follow code style
- **Safe Operations**: Configurable security boundaries for file operations

## Prerequisites

1. **Clojure-MCP Installation**
   ```bash
   # Install clojure-mcp globally using Clojure tools
   clojure -Ttools install-latest :lib io.github.bhauman/clojure-mcp :as mcp
   ```

2. **Verify Installation**
   ```bash
   clojure -Tmcp start --help
   ```

## Configuration

This project includes pre-configured clojure-mcp settings:

### 1. MCP Server Configuration (`.clojure-mcp/config.edn`)

Located at `.clojure-mcp/config.edn`, this file defines:
- **Allowed directories**: Restricts file operations to project directories
- **Write guards**: Requires partial read before writing to prevent overwrites
- **Code formatting**: Auto-format with cljfmt
- **Tool settings**: Timeouts for REPL evaluation and bash commands

### 2. nREPL Alias (`deps.edn`)

The `:nrepl` alias is configured to start an nREPL server on port 7888:
```bash
clojure -M:nrepl
```

This starts:
- nREPL server on port 7888
- CIDER middleware for enhanced REPL features
- Ready for clojure-mcp connection

### 3. Context Files

Three files provide context to AI assistants:

- **`PROJECT_SUMMARY.md`**: Project overview, architecture, API reference
- **`LLM_CODE_STYLE.md`**: Coding standards and Polylith constraints
- **`AGENTS.md`**: Operational guidelines for AI agents

## Usage Workflows

### Option 1: Claude Desktop (Recommended for Chat)

Claude Desktop starts MCP servers outside your project directory, so we use the `:not-cwd` flag.

**1. Start nREPL in your project:**
```bash
cd /Users/davidwu/workspace/simplicity
clojure -M:nrepl
```

**2. Configure Claude Desktop:**

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

**3. Restart Claude Desktop**

**4. Start a conversation:**
- Click the `+` button to add resources
- Add "Project Summary" resource (loads `PROJECT_SUMMARY.md`)
- Add "Clojure Project Info" resource (introspects nREPL)
- Optionally add "LLM_CODE_STYLE.md" resource

Now Claude can:
- Read and edit files in your project
- Evaluate Clojure code in the REPL
- Run tests and verify changes
- Understand Polylith architecture constraints

### Option 2: Claude Code (Recommended for CLI)

Claude Code runs from your project directory, making setup simpler.

**1. Add clojure-mcp with automatic nREPL startup:**
```bash
claude mcp add clojure-mcp -- clojure -Tmcp start \
  :start-nrepl-cmd '["clojure" "-M:nrepl"]'
```

This automatically:
- Starts an nREPL server when you begin a session
- Discovers the port from the command output
- Connects clojure-mcp to the running REPL

**2. Start using Claude Code:**
```bash
cd /Users/davidwu/workspace/simplicity
claude
```

Ask Claude to:
- "Show me the game engine implementation"
- "Add a test for the user authentication component"
- "Evaluate this expression in the REPL: `(require '[cc.mindward.game.interface :as game])`"

### Option 3: Manual Connection (Any LLM Client)

**1. Start nREPL:**
```bash
clojure -M:nrepl
```

**2. Start clojure-mcp in another terminal:**
```bash
clojure -Tmcp start :port 7888
```

**3. Configure your LLM client to connect to the MCP server**

The server communicates via JSON-RPC over stdio.

## Available Tools

When connected via clojure-mcp, AI assistants have access to:

### Read-Only Tools
- **`LS`**: Recursive directory tree view
- **`read_file`**: Smart file reader with pattern matching for Clojure files
- **`grep`**: Fast content search using regex
- **`glob_files`**: Pattern-based file finding

### Code Evaluation
- **`clojure_eval`**: Evaluate Clojure code in the nREPL session
- **`list_nrepl_ports`**: Discover running nREPL servers
- **`bash`**: Execute shell commands

### File Editing
- **`clojure_edit`**: Structure-aware editing of Clojure forms
- **`clojure_edit_replace_sexp`**: Modify expressions within functions
- **`file_edit`**: Edit files by replacing text strings (with parinfer repair)
- **`file_write`**: Write complete files with safety checks

### AI-Powered Tools (Require API Keys)
- **`dispatch_agent`**: Launch read-only agents for complex searches
- **`architect`**: Technical planning and implementation guidance
- **`code_critique`**: Interactive code review

## Example Session (Claude Desktop)

**You**: "I need to add a new function to calculate the population of a Game of Life board"

**Claude**: 
1. Reads `components/game/src/cc/mindward/component/game/interface.clj`
2. Uses `clojure_eval` to test the implementation in the REPL
3. Uses `clojure_edit` to add the new function
4. Runs tests with `bash` to verify it works

**You**: "Great! Can you show me how to use it?"

**Claude**: Uses `clojure_eval` to demonstrate:
```clojure
(require '[cc.mindward.component.game.interface :as game] :reload)
(game/population test-board)
```

## Tips for Effective Use

### 1. Always Load Context First
When starting a new conversation:
- Add "Project Summary" resource
- Add "Clojure Project Info" resource  
- This gives the AI understanding of the architecture

### 2. Use the REPL Actively
Encourage the AI to:
- Test ideas in the REPL before editing files
- Validate changes after editing
- Use `:reload` when requiring namespaces

### 3. Commit Often
- Create a branch before starting
- Have the AI commit working changes frequently
- This prevents losing good work if the AI goes in a bad direction

### 4. Verify with Polylith Commands
After significant changes:
```bash
clojure -M:poly check    # Verify architecture
clojure -M:poly test :dev # Run all tests
```

### 5. Use Architectural Constraints
The AI knows about Polylith rules from `LLM_CODE_STYLE.md`:
- Components must only expose `interface` namespaces
- Bases delegate to components (no business logic)
- No circular dependencies

## Troubleshooting

### nREPL Won't Start
```bash
# Check if port 7888 is in use
lsof -i :7888

# Kill existing nREPL
pkill -f "clojure.*nrepl"

# Try starting again
clojure -M:nrepl
```

### Claude Desktop Can't Connect
1. Check that nREPL is running: `cat .nrepl-port`
2. Verify the port matches in your config (default: 7888)
3. Restart Claude Desktop after configuration changes
4. Check MCP connection: Click `+` and look for "clojure-mcp"

### File Operations Blocked
If the AI can't read/write files:
1. Check `.clojure-mcp/config.edn` allowed directories
2. Ensure the path is within the allowed directories
3. For project root access, `.` is included by default

### Evaluation Timeouts
For long-running evaluations:
- Increase timeout in `.clojure-mcp/config.edn`
- Default is 30 seconds for `clojure_eval`

## Advanced Configuration

### Custom Tool Configuration
Edit `.clojure-mcp/config.edn`:
```clojure
:tools {:clojure_eval {:timeout-ms 60000}  ;; 60 second timeout
        :bash {:timeout-ms 120000}}        ;; 2 minute timeout
```

### Multiple REPL Sessions
You can run multiple REPLs and connect to different ports:
```bash
# Development REPL
clojure -M:nrepl  # port 7888

# Shadow-cljs REPL (if using ClojureScript)
shadow-cljs watch app  # might use port 9630
```

Tell the AI which port to use in the conversation.

### Scratch Pad Persistence
The scratch pad stores session state and planning data:
```clojure
;; In .clojure-mcp/config.edn
:scratch-pad {:persist true
              :file ".clojure-mcp/scratch-pad.edn"}
```

This persists across server restarts.

## Security Considerations

1. **Allowed Directories**: Only specified directories are accessible
2. **Write Guards**: Partial read required before writing (prevents overwrites)
3. **API Keys**: Store in environment variables, never commit
4. **Bash Commands**: Can execute system commands - be aware
5. **REPL Access**: Full access to running application state

For production deployment, consider:
- Read-only mode (disable write tools)
- Restricted bash access
- Audit logging of AI actions

## Resources

- **Clojure-MCP Documentation**: https://github.com/bhauman/clojure-mcp
- **MCP Protocol Spec**: https://modelcontextprotocol.io/
- **Polylith Documentation**: https://polylith.gitbook.io/
- **This Project's Docs**: See `docs/` directory

## Quick Reference

| Task | Command |
|------|---------|
| Start nREPL | `clojure -M:nrepl` |
| Start MCP server | `clojure -Tmcp start :port 7888` |
| Check config | `cat .clojure-mcp/config.edn` |
| Stop nREPL | `pkill -f "clojure.*nrepl"` |
| Test REPL | `nc localhost 7888` |

---
*For more details, see the official clojure-mcp documentation at https://github.com/bhauman/clojure-mcp*
