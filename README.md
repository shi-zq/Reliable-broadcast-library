# Reliable-broadcast-library

## AckMap
Contiene la struttura dati per salvare gli ACK ricevuti. 

## AliveSender
ha compito di inviare ALIVE con un certo intervallo

## IndexGenerator
Un interfaccia per nascondere le diverse generatori di index 

## LogicalClock
ren tao

## MsgReceiver
Ha solo il compito di ricevere dei msg e usare handle per gestire i messaggi ricevuti
ha 4 stati new-joining-joined-addmember(removemember)
il limite di buff di ricezione \`e 4096 bytes

## MsgSender
Usato per invio dei dati, ha un campo view per buttare messaggi in ritardo e sending per bloccare invio dei messaggi
in caso di cambio di view
Ha diversi sendMsg gia incluso per velocizzare il invio di messaggi 
Ha anche delle funzioni per il controllo dei ultimi ALIVE

## ReliableBroadcast 
Tutto viene wrappato sotto da questo classe e permette utente di inviare, rivecere, terminare,  avviare 

## ReliableMsg
La struttura del messaggio ,in caso di neccessita possiamo aggiungere dei campi 

## SharedArraylist
salva i messaggi ACK e non ACK 

## Join process
ALIVE viene inviato comunque 
Il NEW client invia JOIN
i JOINED client invia END quando hanno finito di processare i messaggi non ACK, la struttura della risposta \`e
La struttura del END type;ip;ip;....;ip

## parte mancante
join e remove dovrebbe essere simile il processo
ResendACK e ResendMsg ancora da implementare
ResendEnd come facciamo?
Penso che tutto dovrebbe fatto AliveSender che continua invio di ALIVE che sveglia 
MsgReceiver a fare il controllo dello stato e reinvio in caso di perdita dei dati
ACKmap 全错，全部的msg应该在同一个list里直到下一个view刷新吗？还是需要需要重新一个hashmap来记录每个client的最后一个msg