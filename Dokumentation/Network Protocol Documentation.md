### Protocol
---
The protocol consists of text commands sent as strings between the server and the client. Commands consist of the command followed by parameters, if required. The command and the parameters are separated by a colon. Parameters are separated by commas *except* for relayed chat messages from the server (see below). TCP is used for most commands to ensure stable communication. UDP is used for time-sensitive information like position updates and registration.

Format: *command*:*parameter*s
or: *command*

Parameter Format: param1,param2,...
Relayed Chat Format (Server -> Client): prefix:sender:message
UDP Position Format (Client -> Server): position:nickname:lobbyCode:x:y
UDP Position Format (Server -> Client): player_position:nickname:x:y

### TCP Commands
---
The following list contains the TCP commands used in the protocol. 

| Sender | Command                      | Description                                                                                                          | Example                      | Protocol |
| ------ | ---------------------------- | -------------------------------------------------------------------------------------------------------------------- | ---------------------------- | -------- |
| Client | connect:[nickname]           | Login request with desired username.                                                                                 | connect:bob                  | TCP      |
| Server | confirm:[nickname]           | Login confirmation with assigned/confirmed username. Also sent after successful username change.                     | confirm:bob_1                | TCP      |
| Server | error:[errormessage]         | Error message indicating a problem.                                                                                  | error:Invalid Command Format | TCP      |
| Client | chat:[message]               | Global chat message sent to everyone. Server adds whisper format if needed.                                        | chat:hello                   | TCP      |
| Client | whisper:[target,message]     | Sends a private message to the target player (via global chat command). **Deprecated: Use /whisper in chat.**        | whisper:bob,hello bob        | TCP      |
| Server | chat:[sender]:[message]      | Chat message relayed to clients (global). Can also be server system messages without a sender. Handles whispers.   | chat:Alice:hello everyone    | TCP      |
| Server | ping                         | Ping message to check client connection.                                                                             | ping                         | TCP      |
| Client | pong                         | Client response to a server ping.                                                                                      | pong                         | TCP      |
| Client | lobby:[code]                 | Request to join a lobby using a lobby code. (Code 0 means leave current lobby.)                                    | lobby:1312                   | TCP      |
| Server | lobby:[code]                 | Confirms player joined/left lobby. (Code 0 means left or failed to join.)                                          | lobby:1312                   | TCP      |
| Client | newlobby                     | Request to create a new lobby and automatically join it.                                                             | newlobby                     | TCP      |
| Client | username:[name]              | Request to change username. Server responds with `confirm:[newname]`.                                              | username:notbob              | TCP      |
| Client | exit                         | Notifies server that the client is disconnecting gracefully.                                                         | exit                         | TCP      |
| Client | getlobbies                   | Request list of available lobbies.                                                                                   | getlobbies                   | TCP      |
| Server | getlobbies:[list]            | Response with list of available lobbies. Format: code=players/max [state],...                                        | getlobbies:1234=2/4 [open]   | TCP      |
| Client | lobbychat:[message]          | Send a chat message only to players in the same lobby. Server adds whisper format if needed.                       | lobbychat:hello lobby        | TCP      |
| Server | lobbychat:[sender]:[message] | Lobby chat message relayed to clients in the same lobby. Handles whispers.                                           | lobbychat:Bob:in the lobby!  | TCP      |
| Client | teamchat:[message]           | Send a chat message only to players on the same team (if team logic exists). Server adds whisper format if needed. | teamchat:go team!            | TCP      |
| Server | teamchat:[sender]:[message]  | Team chat message relayed to clients on the same team. Handles whispers.                                           | teamchat:Guard1:defend       | TCP      |
| Client | getplayers                   | Request list of all connected players.                                                                               | getplayers                   | TCP      |
| Server | getplayers:[list]            | Response with comma-separated list of all connected players.                                                         | getplayers:bob,alice         | TCP      |
| Client | getlobbyplayers              | Request list of players in the current lobby.                                                                        | getlobbyplayers              | TCP      |
| Server | getlobbyplayers:[list]       | Response with comma-separated list of players in the current lobby.                                                  | getlobbyplayers:bob,alice    | TCP      |
| Client | ready                        | Player notifies server they are ready for the game to start in the current lobby.                                      | ready                        | TCP      |
| Server | ready_status:[player],[bool] | Broadcasts the ready status change of a player in the lobby.                                                         | ready_status:bob,true        | TCP      |
| Server | role:[player]:[roleId]       | Assigns a role (0: Goat, 1: Robot, 2: Guard) to a specific player. Broadcast to lobby.                             | role:bob:1                   | TCP      |
| Client | getroles                     | Request the roles of all players in the current lobby.                                                               | getroles                     | TCP      |
| Server | roles:[data]                 | Response with roles of lobby players. Format: player1=roleId,player2=roleId,...                                      | roles:bob=1,alice=2          | TCP      |
| Client | catch:[target]               | Guard requests to catch the target player.                                                                           | catch:bob                    | TCP      |
| Server | catch:[target]               | Broadcasts that a player was caught.                                                                                 | catch:bob                    | TCP      |
| Client | revive:[target]              | Goat requests to revive the target player (Robot).                                                                   | revive:bob                   | TCP      |
| Server | revive:[target]              | Broadcasts that a player was revived.                                                                                | revive:bob                   | TCP      |
| Client | terminal:[id]                | Player reports activating a terminal with the specified ID.                                                          | terminal:0                   | TCP      |
| Server | terminal:[id]                | Broadcasts that a terminal was activated in the lobby.                                                               | terminal:0                   | TCP      |
| Client | startgame                    | Request by the lobby host to start the game.                                                                         | startgame                    | TCP      |
| Server | game_started                 | Broadcasts that the game has started successfully.                                                                   | game_started                 | TCP      |
| Server | player_left:[player]         | Broadcasts that a player has left the lobby/game.                                                                    | player_left:alice            | TCP      |
| Client | mapinfo:[terminals]          | Client reports the number of terminals found on the map it loaded. Sent before 'ready'.                            | mapinfo:8                    | TCP      |

### UDP Commands
---
The following list contains the UDP commands used in the protocol. These are generally less reliable but faster, used for frequent updates or initial registration.

| Sender | Command                                   | Description                                                                                              | Example                          |
| ------ | ----------------------------------------- | -------------------------------------------------------------------------------------------------------- | -------------------------------- |
| Client | register_udp:[nickname]:[listeningPort] | Client registers its UDP listening port with the server after nickname confirmation.                     | register_udp:bob_1:54321         |
| Server | udp_ack:                                  | Server acknowledges successful UDP registration. Sent to the client's registered listening port.         | udp_ack:                         |
| Client | position:[nickname]:[lobbyCode]:[x]:[y] | Client sends its current position within a specific lobby.                                               | position:bob_1:1234:150:200      |
| Server | player_position:[nickname]:[x]:[y]      | Server broadcasts a player's position update to other clients in the same lobby.                         | player_position:alice:300:450    |
| Client | udp_bcast:[message]                       | **Deprecated/Test?** Client requests server broadcast a UDP message to lobby members.                    | udp_bcast:test                   |
