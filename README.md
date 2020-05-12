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
