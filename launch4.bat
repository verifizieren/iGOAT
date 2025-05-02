@echo off

START "Server" java -jar .\build\libs\iGOAT-0.15-ALPHA.jar server 61000

TIMEOUT /T 1 /NOBREAK > NUL
START "Client 1" java -jar .\build\libs\iGOAT-0.15-ALPHA.jar client localhost:61000 Client1
START "Client 2" java -jar .\build\libs\iGOAT-0.15-ALPHA.jar client localhost:61000 Client2
START "Client 3" java -jar .\build\libs\iGOAT-0.15-ALPHA.jar client localhost:61000 Client3
START "Client 4" java -jar .\build\libs\iGOAT-0.15-ALPHA.jar client localhost:61000 Client4