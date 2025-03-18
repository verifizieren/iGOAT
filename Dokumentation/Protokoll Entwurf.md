### Client Server Connection
**Client:** connect (nickname) *connection request*
**Server:** confirm (adjusted nickname) *confirm connection to client*

**Server:** error (errormsg) *error (should be logged, but game can continue)*

**Client:** chat (msg)
**Server:** chat (msg) (player)
#### Ping Pong
**Server:** ping *check client connection*
**Client:** pong confirm *client connection*
### Lobby
**Client:** lobby (code) *join lobby*
**Server:** confirm
**Server:** error (couldn't join lobby..)

**Client:** leave *leave lobby*

**Server:** list (name1,name2,name3,name4) *send lobby player list to all clients*
**Client:** confirm *confirm player list*

**Client:** chat (msg) */whisper (name) client to client*
**Server:** chat (msg) (player)

**Client:** name (new nickname) *change nickname*
**Server:** confirm (adjusted nickname) *confirm name change*
**Server:** error (errormsg) *error -> keep old nickname*

**Client:** ready
**Server:** start (role) *start msg + role assignment*
**Client:** confirm (role)

### Game
