### Protocol
---
The protocol consists of text commands sent as strings between the server and the client. Commands consist of the command followed by parameters, if required. The command and the parameters are separated by a colon. Parameters are separated by commas. TCP is used for most commands to ensure stable communication. UDP is used for time-sensitive information.

Format: *command*:*parameter*s
or: *command*

Parameter Format: param1,param2,...

### Commands
---
The following list contains the commands used in the protocol. 

| Sender | Command                  | Description                                                                                                          | Example                      | Protocol |
| ------ | ------------------------ | -------------------------------------------------------------------------------------------------------------------- | ---------------------------- | -------- |
| Client | connect:[nickname]       | login request with username                                                                                          | connect:bob                  | TCP      |
| Server | confirm:[nickname]       | login confirmation with adjusted username                                                                            | confirm:bob                  | TCP      |
| Server | error:[errormessage]     | error message                                                                                                        | error:Invalid Command Format | TCP      |
| Client | chat:[message]           | chat message sent to everyone                                                                                        | chat:hello                   | TCP      |
| Client | whisper:[target,message] | sends a private message to the target player                                                                         | wisper:bob,hello bob         | TCP      |
| Server | chat:[message]           | chat message to client, can also be server logs                                                                      | chat:[Bob] hello             | TCP      |
| Server | ping                     | ping message to check client connection                                                                              | ping                         | TCP      |
| Client | pong                     | client response to a ping                                                                                            | pong                         | TCP      |
| Client | lobby:[code]             | join a lobby using a lobby code. (Code 0 stands for no lobby and is used for disconnecting.)                         | lobby:1312                   | TCP      |
| Server | lobby:[code]             | confirm that player joined lobby. (Code 0 stands for no lobby and is used when the player can't join or was kicked.) | lobby:1312                   | TCP      |
| Client | newlobby                 | create new lobby and automatically join it                                                                           | newlobby                     | TCP      |
| Client | username:[name]          | changing the username, server will react with confirm                                                                | username:notbob              | TCP      |
| Client | exit          | terminates the connection                                                                | exit              | TCP      |
#### To be implemented
---

| Sender | Command               | Description                                                                   | Example                                                     | Protocol |
| ------ | --------------------- | ----------------------------------------------------------------------------- | ----------------------------------------------------------- | -------- |
| Client | ready                 | player is ready for the game                                                  | ready                                                       | TCP      |
| Server | role:[role]           | communicates ingame-role to the client (0: goat, 1: iGOAT,2: Guard )          | role:0                                                      | TCP      |
| Client | role:[role]           | confirms the role to the server so the game can start                         | role:1                                                      | TCP      |
| Server | init:[terminal ids]   | sends the ids of the exit terminals to the client at the start of the game    | init:0,3,6                                                  | TCP      |
| Client | init:[termnal ids]    | confirms the terminal ids                                                     | init:0,3,6                                                  | TCP      |
| Client | pos:[positional data] | sends the x,y coordinates and the rotation $r\in \{ 0,1,2,3 \}$ of the player | pos:23.54,18.02,2                                           | **UDP**  |
| Server | pos:[positional data] | sends the positional data of all 4 players to the client                      | pos:23.54,18.02,2,23.54,18.02,2,23.54,18.02,2,23.54,18.02,2 | **UDP**  |
| Client | revive:[target]       | message to revive the target                                                  | revive:bob                                                  | TCP      |
| Server | revive:[target]       | message that the target was revived                                           | revive:bob                                                  | TCP      |
| Client | catch:[target]        | message to catch the target                                                   | catch:bob                                                   | TCP      |
| Server | catch:[target]        | message that the target was caught                                            | catch:bob                                                   | TCP      |
| Client | term:[id]             | message to activate terminal                                                  | term:1                                                      | TCP      |
| Server | term:[id]             | message that terminal was activated                                           | term:1                                                      | TCP      |
| Server | open                  | opens the exit gates                                                          | open                                                        | TCP      |
| Client | open                  | confirms the "open" message                                                   | open                                                        | TCP      |
| Server | result:[result]       | sends the individual game result to the client                                | result:true                                                 | TCP      |
