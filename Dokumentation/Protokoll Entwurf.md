### Client Server Connection
**Client:** connect:(nickname) *connection request*
**Server:** confirm:(adjusted nickname) *confirm connection to client*

**Server:** error:(errormsg) *error (should be logged, but game can continue)*

**Client:** chat:(msg)
**Server:** chat:(player),(message)
#### Ping Pong
**Server:** ping *check client connection max. every 100ms*
**Client:** pong *confirm client connection*
### Lobby
**Client:** lobby:(code) *join lobby*
**Server:** confirm:(lobby join)
**Server:** error:(couldn't join lobby..)

**Client:** newlobby *create new lobby*
**Server:** confirm:(lobby code) *confirm with lobby code*
**Server:** error:(errormsg)

**Client:** leave *leave lobby*

**Server:** list:(name1,name2,name3,name4) *send lobby player list to all clients*
**Client:** confirm *confirm player list*

**Client:** whisper:(name),(msg) */whisper (name) client to client*
**Server:** chat:(player),(msg) *server can also send server logs using this*

**Client:** username:(new nickname) *change nickname*
**Server:** confirm:(adjusted nickname) *confirm name change*
**Server:** error:(errormsg) *error -> keep old nickname*

**Client:** ready
**Server:** start:(role) *start msg + role assignment*
**Client:** confirm:(role)

### Game
**Server:** init:(id1,id2,id3)
**Client** confirm:(i1,id2,id3)

**Client:** pos:(x,y,r) *position & rotational data $r\in \{ 0,1,2,3 \}$* UDP
**Server:** pos:(name1,x,y,r,name1,x,y,r,...) *player positions including the client's own* UDP

**Client:** revive:(name) *Server needs to check validity* 
**Server:** revive:(target) *also send server log via chat*

**Client:** catch:(name) *Server needs to check validity*
**Server:** catch:(target) *also send server log via chat*

**Client:** term:(id) *terminal (terminal-id)*
**Server:** term:(id) *terminal (id), also send server log via chat*

**Server:** open *open exit gates*
**Client:** confirm

**Server:** result:(boolean) *send game result*