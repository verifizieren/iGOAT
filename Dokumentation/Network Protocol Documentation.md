### Protocol
---
The protocol consists of text commands sent as strings between the server and the client. Commands consist of the command followed by parameters, if required. The command and the parameters are separated by a colon. Parameters are separated by commas *except* for relayed chat messages from the server (see below). TCP is used for most commands to ensure stable communication. UDP is used for time-sensitive information.

Format: *command*:*parameter*s
or: *command*

Parameter Format: param1,param2,...
Relayed Chat Format (Server -> Client): prefix:sender:message

### Commands
---
The following list contains the commands used in the protocol. 

| Sender | Command                      | Description                                                                                                          | Example                      | Protocol |
| ------ | ---------------------------- | -------------------------------------------------------------------------------------------------------------------- | ---------------------------- | -------- |
| Client | connect:[nickname]           | login request with username                                                                                          | connect:bob                  | TCP      |
| Server | confirm:[nickname]           | login confirmation with adjusted username                                                                            | confirm:bob                  | TCP      |
| Server | error:[errormessage]         | error message                                                                                                        | error:Invalid Command Format | TCP      |
| Client | chat:[message]               | chat message sent to everyone                                                                                        | chat:hello                   | TCP      |
| Client | whisper:[target,message]     | sends a private message to the target player                                                                         | whisper:bob,hello bob        | TCP      |
| Server | chat:[sender]:[message]      | chat message relayed to clients (global), can also be server logs without a sender                                   | chat:Alice:hello everyone    | TCP      |
| Server | ping                         | ping message to check client connection                                                                              | ping                         | TCP      |
| Client | pong                         | client response to a ping                                                                                            | pong                         | TCP      |
| Client | lobby:[code]                 | join a lobby using a lobby code. (Code 0 stands for no lobby and is used for disconnecting.)                         | lobby:1312                   | TCP      |
| Server | lobby:[code]                 | confirm that player joined lobby. (Code 0 stands for no lobby and is used when the player can't join or was kicked.) | lobby:1312                   | TCP      |
| Client | newlobby                     | create new lobby and automatically join it                                                                           | newlobby                     | TCP      |
| Client | username:[name]              | changing the username, server will react with confirm                                                                | username:notbob              | TCP      |
| Client | exit                         | terminates the connection                                                                                            | exit                         | TCP      |
| Client | getlobbies                   | request list of available lobbies                                                                                    | getlobbies                   | TCP      |
| Server | getlobbies:[list]            | response with list of available lobbies and their codes                                                              | getlobbies:1234,5678         | TCP      |
| Client | lobbychat:[message]          | send a chat message only to players in the same lobby                                                                | lobbychat:hello lobby        | TCP      |
| Server | lobbychat:[sender]:[message] | lobby chat message relayed to clients in the same lobby                                                              | lobbychat:Bob:in the lobby!  | TCP      |
| Client | teamchat:[message]           | send a chat message only to players on the same team                                                                 | teamchat:go team!            | TCP      |
| Server | teamchat:[sender]:[message]  | team chat message relayed to clients on the same team (Requires server-side team logic)                              | teamchat:Guard1:defend       | TCP      |
| Client | getplayers                   | request list of all connected players                                                                                | getplayers                   | TCP      |
| Server | getplayers:[list]            | response with list of all connected players                                                                          | getplayers:bob,alice         | TCP      |
| Client | getlobbyplayers              | request list of players in current lobby                                                                             | getlobbyplayers              | TCP      |
| Server | getlobbyplayers:[list]       | response with list of players in current lobby                                                                       | getlobbyplayers:bob,alice    | TCP      |
| Client | ready                        | player is ready for the game                                                                                         | ready                        | TCP      |
| Server | role:[role]                  | assigns a role to the player (0: goat, 1: iGOAT, 2: Guard)                                                           | role:1                       | TCP      |
| Client | role:[role]                  | confirms the assigned role to the server                                                                             | role:1                       | TCP      |
| Client | catch:[target]               | message to catch the target                                                                                          | catch:bob                    | TCP      |
| Server | catch:[target]               | broadcasts that a player was caught                                                                                  | catch:bob                    | TCP      |
| Client | revive:[target]              | message to revive the target                                                                                         | revive:bob                   | TCP      |
| Server | revive:[target]              | broadcasts that a player was revived                                                                                 | revive:bob                   | TCP      |
| Client | terminal:[id]                | message that terminal was activated                                                                                  | terminal:0                   | TCP      |
| Server | terminal:[id]                | broadcasts that a terminal was activated                                                                             | terminal:0                   | TCP      |
| Client | startgame                    | request to start the game (for lobby host)                                                                           | startgame                    | TCP      |
| Server | startgame:[message]          | broadcasts that the game has started                                                                                 | startgame:Game started!      | TCP      |
| Client | udp_bcast:[message]          | broadcast a UDP message to all players in the lobby                                                                  | udp_bcast:test               | UDP      |
