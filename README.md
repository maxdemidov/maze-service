# maze-service

Some description of implementation:

First of all
To respect user's events order, I can't see any efficient decision,
 because we should be blocked on each chunk (actually even worse) before taking next,
 cause each 'S' operation blocked separate processes for any other user in order to check followers.
So we should resolve that Future first (by taking followers and map them into active users) 
 before proceeding with the next operations, as it may interfere with any other user's events queue.
We can't split all amount of users between several parts to calculate asynchronous, 
 rather optimize operation with collection that stores our followers and parallelize that work as much as possible.
I use separate actor (with set under it) for each user's followers.
But for processing chunk I use promise to block it at all, to complete entire its operation, before taking next.
It should be a good parallelize decision with Akka Streams to process the entire flow if the order wouldn't be matter.

I have two versions of 'processors' to work under event source:
The first is ChunkProcessor with Future.sequence and combining results for followers by ask from state aggregator (UserController).
This decision a little bit faster.
The second is StreamProcessor with flow that lives for processing chunk and with ask stage to states aggregator (CommandResolver).
I have been searching for more elegance and efficient decision by doing so.. somehow parallelize this flow..

To switch between them, please uncomment appropriate processorRef initialization in main object BootMaze.

---

Next some more technical description about implementation:

EventSourceServer and UserClientsServer are TCP socket servers (extends abstract SocketServer),
 they start on configurable ports (maze-service.conf).
Thay also take (and their connection handlers in turn) appropriate processor as ActorRef.
Requests from connection handlers transform to commands which that processor performed,
 and can reply with SendToUser to send message for any user's client (through his holder)
 
About processors:

* First implementation - ChunkProcessor.
  ChunkProcessor stores all active connection handlers (as ActorRef) in Map by their Id (as they registered).
  If some client lost connection it would be unregistered, by appropriate command from those client's handler.
  ProcessData command carries a chunk of events from the event source (as ByteString).
  ProcessClean will have sent to clean state and to stop all inner actors in case the event source closed his connection.
  
  How chunks of data processed:
  It split, ordered, transform to Event objects (DataParser trait and its parseData method) 
   and then for each operation, we should either modify followers and(or) prepare appropriate events for the client(s).
  For storing followers I use UserController actor which manipulate with changes and reading followers.
  Its contains Map of references of actors intern that actually saved followers for each user under the hood as Set.
  Maybe its enough to have only Map of Set directly - I'm thinking about now...
  I have been trying to parallelize each read operation to that Map as more as possible, 
   because it looks like almost for each event takes to look to that Map for followers,
   so to operate with that operation concurrently I chose this way.
   
  So we have 5 types of operation:
  For 'F' command  we should only add follower and the event for client is obvious.
  For 'U' we need remove follower and there is no event at all.
  For 'B' we have the event as is, and nothing to modify followers.
  For 'P' we should only take all active users (they stored in processor as Map) and build event for each, and nothing with followers .
  For 'S' we should take set with follower for an appropriate user and leave only that that currently active to form event for them.
  All this operations processed as a list of Future, then I sequense them, 
   flatten as each event from event source can emit a list of events for users.
  And then I group them by Id to exclude inactive by once and bind with ActorRef through which will send events.
  I must resolve here entire Future for this butch in order to prevent race condition with another butch, 
   so I use Promise, onComplete and Await.. (which unlike me but..)

* Second implementation - StreamProcessor.
  Generally the same. And I blocked with Promise the same way..
  The idea is to use Streams API - flow and ask stage with EventResolver, 
   actor that decides how to translate event source's event to message (user events), 
   and there are some inner temporary actor GetFollowersTmp.
  Storing followers in the same way.
  I send active users Ids with each Event to EventResolver to intersect with followers 
   and exclude inactive directly on each event at once, not at the and as in the first implementation.


