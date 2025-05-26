#!/bin/bash

SERVER_PORT=8080
SERVER_JAR="chat-server/target/chat-server-1.0-SNAPSHOT-jar-with-dependencies.jar"
CLIENT_JAR="chat-client/target/chat-client-1.0-SNAPSHOT-jar-with-dependencies.jar"
SERVER_LOG="server.log"

echo "--- Chat Start Script ---"

echo "[1/4] Attempting to stop any existing server on port $SERVER_PORT..."
PID=$(lsof -ti :$SERVER_PORT -sTCP:LISTEN)

if [ -n "$PID" ]; then
    echo "      Found running server (PID: $PID). Killing it..."
    kill $PID
    sleep 1
    if kill -0 $PID > /dev/null 2>&1; then
        echo "      Failed to kill process $PID automatically. Please kill it manually."
    else
        echo "      Server process $PID stopped."
    fi
else
    echo "      No running server found on port $SERVER_PORT."
fi
echo "      Done."
echo ""

echo "[2/4] Starting server in background..."
if [ ! -f "$SERVER_JAR" ]; then
    echo "      ERROR: Server JAR not found: $SERVER_JAR"
    echo "      Please build the project first using 'mvn clean package' in the chat-parent directory."
    exit 1
fi
nohup java -jar $SERVER_JAR > "$SERVER_LOG" 2>&1 &
SERVER_PID=$!
echo "      Server started with PID: $SERVER_PID."
echo "      Logs are being written to '$SERVER_LOG'."
echo "      Waiting a few seconds for server to initialize..."
sleep 3
echo "      Done."
echo ""

echo "[3/4] Starting first client..."
if [ ! -f "$CLIENT_JAR" ]; then
    echo "      ERROR: Client JAR not found: $CLIENT_JAR"
    echo "      Please build the project first using 'mvn clean package'."
    exit 1
fi
nohup java -jar $CLIENT_JAR &
CLIENT1_PID=$!
echo "      Client 1 started with PID: $CLIENT1_PID."
sleep 1
echo "      Done."
echo ""

echo "[4/4] Starting second client..."
nohup java -jar $CLIENT_JAR &
CLIENT2_PID=$!
echo "      Client 2 started with PID: $CLIENT2_PID."
echo "      Done."
echo ""

echo "--- Setup Complete ---"
echo "Server PID: $SERVER_PID (Logs: $SERVER_LOG)"
echo "Client 1 PID: $CLIENT1_PID"
echo "Client 2 PID: $CLIENT2_PID"
echo "To stop the server manually, run: kill $SERVER_PID" 