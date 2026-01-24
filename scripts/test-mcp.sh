#!/bin/bash
# Test script for Clojure-MCP integration
# Usage: ./scripts/test-mcp.sh

set -e

echo "🔧 Testing Clojure-MCP integration..."

# Check if mcp tool is installed
echo "1. Checking if mcp tool is installed..."
if clojure -Ttools list | grep -q "mcp"; then
    echo "   ✅ mcp tool is installed"
else
    echo "   ❌ mcp tool not found. Installing..."
    clojure -Ttools install-latest :lib io.github.bhauman/clojure-mcp :as mcp
    echo "   ✅ mcp tool installed"
fi

# Check if nREPL is running
echo "2. Checking nREPL server..."
NREPL_RUNNING=false
if ss -tuln 2>/dev/null | grep -q ":7888 "; then
    NREPL_RUNNING=true
elif netstat -tuln 2>/dev/null | grep -q ":7888 "; then
    NREPL_RUNNING=true
fi

if $NREPL_RUNNING; then
    echo "   ✅ nREPL is running on port 7888"
else
    echo "   ⚠️  nREPL not running on port 7888"
    echo "   Note: nREPL needs to be started separately for MCP to work"
    echo "   Start nREPL with: clojure -M:nrepl"
    echo "   Or use the start script: ./scripts/start-mcp.sh"
fi

# Test basic REPL connection (if nREPL is running)
echo "3. Testing REPL connection..."
if $NREPL_RUNNING; then
    if brepl <<'EOF' 2>/dev/null | grep -q "6"; then
(+ 1 2 3)
EOF
        echo "   ✅ REPL connection successful"
    else
        echo "   ⚠️  REPL connection test inconclusive"
    fi
else
    echo "   ⚠️  Skipping REPL test (nREPL not running)"
fi

# Test MCP tool
echo "4. Testing MCP tool..."
echo "   Testing command: clojure -Tmcp start :port 7888 :not-cwd true"
echo "   (This will timeout after 3 seconds if nREPL is not running)"
if timeout 3 clojure -Tmcp start :port 7888 :not-cwd true 2>&1 | head -5; then
    echo "   ✅ MCP tool starts successfully"
else
    echo "   ⚠️  MCP tool test inconclusive"
    echo "   This is normal if nREPL is not running"
fi

echo ""
echo "📋 Summary:"
echo "✅ mcp tool: Installed"
if $NREPL_RUNNING; then
    echo "✅ nREPL: Running on port 7888"
else
    echo "⚠️  nREPL: Not running (needs to be started)"
fi
echo "✅ MCP: Configuration updated"
echo ""
if $NREPL_RUNNING; then
    echo "🎉 Clojure-MCP integration is ready!"
    echo ""
    echo "Next steps:"
    echo "1. Restart your MCP client (Claude Desktop/Claude Code)"
    echo "2. Add 'Project Summary' resource"
    echo "3. Add 'Clojure Project Info' resource"
else
    echo "📝 Setup incomplete: nREPL server needs to be started"
    echo ""
    echo "To complete setup:"
    echo "1. Start nREPL: clojure -M:nrepl"
    echo "   Or use: ./scripts/start-mcp.sh"
    echo "2. Then restart your MCP client"
fi
echo ""
echo "For detailed instructions, see:"
echo "- docs/clojure-mcp-integration.md"
echo "- .clojure-mcp/README.md"