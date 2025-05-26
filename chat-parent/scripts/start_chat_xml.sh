#!/bin/bash

SERVER_PORT=8080
SERVER_JAR="chat-server/target/chat-server-1.0-SNAPSHOT-jar-with-dependencies.jar"
CLIENT_JAR="chat-client/target/chat-client-1.0-SNAPSHOT-jar-with-dependencies.jar"
SERVER_LOG="server_xml.log"

echo "--- Chat XML Protocol Start Script ---"

# --- Stop Existing Server ---
echo "[1/4] Attempting to stop any existing server on port $SERVER_PORT..."
# Get PID listening on the port (-sTCP:LISTEN ensures it's the listening socket)
PID=$(lsof -ti :$SERVER_PORT -sTCP:LISTEN)

if [ -n "$PID" ]; then
    echo "      Found running server (PID: $PID). Killing it..."
    kill $PID
    sleep 1 # Give it a moment to shut down
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

# --- Update config file to use XML protocol ---
echo "[2/4] Updating server configuration for XML protocol..."
# Create or update the server.properties file
cat > server.properties << EOF
#Server Configuration
server.maxclients=10
logging.enabled=true
server.port=8080
# Протокол: java (сериализация Java-объектов) или xml (XML-формат)
server.protocol=xml
EOF
echo "      Done."
echo ""

# --- Start Server ---
echo "[3/4] Starting server in background with XML protocol..."
# Check if server JAR exists
if [ ! -f "$SERVER_JAR" ]; then
    echo "      ERROR: Server JAR not found: $SERVER_JAR"
    echo "      Please build the project first using 'mvn clean package' in the chat-parent directory."
    exit 1
fi
# Start server using nohup and redirect output/error to log file
nohup java -jar $SERVER_JAR > "$SERVER_LOG" 2>&1 &
SERVER_PID=$!
echo "      Server started with PID: $SERVER_PID."
echo "      Logs are being written to '$SERVER_LOG'."
echo "      Waiting a few seconds for server to initialize..."
sleep 3 # Adjust sleep time if server needs longer to start
echo "      Done."
echo ""

# --- Start Clients ---
echo "[4/4] Starting clients with XML protocol..."
# Check if client JAR exists
if [ ! -f "$CLIENT_JAR" ]; then
    echo "      ERROR: Client JAR not found: $CLIENT_JAR"
    echo "      Please build the project first using 'mvn clean package'."
    # Optionally kill the server we just started
    kill $SERVER_PID
    exit 1
fi
# Start clients with XML protocol parameter
echo "      Starting first client..."
nohup java -jar $CLIENT_JAR xml &
CLIENT1_PID=$!
echo "      Client 1 started with PID: $CLIENT1_PID."
sleep 1 # Small delay between clients

echo "      Starting second client..."
nohup java -jar $CLIENT_JAR xml &
CLIENT2_PID=$!
echo "      Client 2 started with PID: $CLIENT2_PID."
echo "      Done."
echo ""

echo "--- Setup Complete ---"
echo "Server PID: $SERVER_PID (Logs: $SERVER_LOG) - Protocol: XML"
echo "Client 1 PID: $CLIENT1_PID - Protocol: XML"
echo "Client 2 PID: $CLIENT2_PID - Protocol: XML"
echo "To stop the server manually, run: kill $SERVER_PID" 