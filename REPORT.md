# SimpleChat Phase 2 - brief report

## Idea and approach

SimpleChat is a console-based client/server chat application. The client
collects a login ID and user commands; the server accepts clients, identifies
each connection, relays chat messages, and offers its own command console.
OCSF provides the reusable socket, object-serialization, and connection-thread
services. The application adds only its chat protocol and user interaction.

## OCSF framework mapping

- **Services:** `AbstractServer` accepts client connections and broadcasts
  objects; `AbstractClient` connects, sends, and receives objects;
  `ConnectionToClient` holds per-client state.
- **Hooks:** `handleMessageFromClient`, `clientConnected`,
  `clientDisconnected`, `serverStarted`, `serverStopped`,
  `connectionClosed`, and `connectionException` specialize framework events.
- **Slots:** `EchoServer` and `ChatClient` supply the application behavior at
  those hooks without changing OCSF itself.
- **Application/UI boundary:** `ChatIF` separates console output from client
  and server logic. `ClientConsole` and `ServerConsole` parse user input and
  invoke the application layer.
- **Connection protocol:** the first client object must be `#login <loginID>`.
  The server stores the ID with `ConnectionToClient.setInfo`; subsequent
  messages are relayed as `loginID> message`. Login and disconnect
  notifications are broadcast to connected clients.

## Features

- Client login, message exchange, `#quit`, `#logoff`, `#sethost`, `#setport`,
  `#login`, `#gethost`, and `#getport`.
- Server-side message entry and `#quit`, `#stop`, `#close`, `#setport`,
  `#start`, and `#getport`.
- Server connection notifications, per-client login state, error handling,
  and disconnect notifications.
- OCSF 2.31 source is included unmodified under `src/com/lloseng/ocsf`.

## Command reference

| Client command | Effect |
|---|---|
| `#quit` | Disconnect and exit |
| `#logoff` | Disconnect but keep the console open |
| `#login [loginID]` | Connect or reconnect; uses the startup ID if omitted |
| `#sethost <host>` / `#setport <port>` | Configure the next connection while disconnected |
| `#gethost` / `#getport` | Display the current connection settings |

| Server command | Effect |
|---|---|
| `#quit` | Close the server and exit |
| `#stop` / `#start` | Stop or restart accepting new connections |
| `#close` | Close the listening socket and disconnect all clients |
| `#setport <port>` | Change the port while the server is closed |
| `#getport` | Display the configured port |

## Build and run

From the project directory in PowerShell:

```powershell
New-Item -ItemType Directory -Force out
javac -encoding UTF-8 -d out (Get-ChildItem -Recurse -Filter *.java src | ForEach-Object FullName)
java -cp out simplechat1.ServerConsole
```

In another terminal:

```powershell
java -cp out simplechat1.ClientConsole alice
```

Start additional clients with unique IDs. To use a non-default host and port:

```powershell
java -cp out simplechat1.ClientConsole bob localhost 5556
java -cp out simplechat1.ServerConsole 5556
```

Client commands can be entered after connecting or while disconnected. Server
commands are entered in the server terminal. Use `#close`, then `#setport
<port>`, then `#start` to change the server's listening port while it is
running. `#stop` stops accepting new clients but leaves existing connections
active; `#close` disconnects clients and closes the listening socket.

## E52 test and demo checklist

1. Start a default server and verify it listens on port 5555.
2. Start a client without a login ID; verify it prints the missing-login error
   and exits.
3. Start clients with different login IDs; verify the server records login
   and each client receives the login notification.
4. Exchange messages between clients and the server; verify sender IDs and
   the `SERVER MESSAGE> ` prefix on server-originated messages.
5. Try `#logoff`, reconnect with `#login <loginID>`, and verify disconnect and
   login notifications.
6. Exercise `#stop`/`#start` and `#close`/`#setport`/`#start`.
7. End with `#quit` on clients and server.

A short screen recording of these steps is the remaining manual submission
item; this workspace can prepare the demo flow but cannot record a video.

## OCSF attribution

This software contains the Object Client-Server Framework copyright © 2001
By Timothy C. Lethbridge and Robert Laganière, www.lloseng.com. See
`LICENSE_OCSF.txt` for the license.
