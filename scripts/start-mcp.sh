#!/bin/bash
# Start script for Clojure-MCP integration
# Usage: ./scripts/start-mcp.sh

set -e

echo "🚀 Starting Clojure-MCP integration..."

# Kill any existing nREPL processes
echo "1. Cleaning up existing processes..."
pkill -f "clojure.*nrepl" 2>/dev/null || true
sleep 1

# Start nREPL server
echo "2. Starting nREPL server on port 7888..."
clojure -M:nrepl &
NREPL_PID=$!
sleep 3

# Verify nREPL started
if lsof -i :7888 > /dev/null 2>&1; then
    echo "   ✅ nREPL started (PID: $NREPL_PID)"
else
    echo "   ❌ Failed to start nREPL"
    exit 1
fi

# Start MCP server
echo "3. Starting MCP server..."
echo "   Command: clojure -Tmcp start :port 7888 :not-cwd true"
echo ""
echo "📝 The MCP server will start in this terminal."
echo "   Keep this terminal open while using MCP."
echo ""
echo "🔗 MCP server is ready for connections."
echo "   Configure your MCP client to connect to this server."
echo ""
echo "📋 Quick reference:"
echo "   - nREPL: http://localhost:7888"
echo "   - Web server: http://localhost:3000 (after running 'bb dev' and '(start)')"
echo "   - Test MCP: ./scripts/test-mcp.sh"
echo ""
echo "Press Ctrl+C to stop the MCP server."

# Start MCP server
exec clojure -Tmcp start :port 7888 :not-cwd true