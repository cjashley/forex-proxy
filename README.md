# forex-proxy
code challenge 

First apologies for not completing the challenge earlier. I hope that Skillhouse communicated that I left for a family holiday driving down to Kagoshima and back on the day after the interview. Hence did not see the challenge until I was back Thursday.
Second, apologies for doing a slightly different challenge to the one set. I missed that you gave me a framework other than the One-Frame service managed by docker. When I found no code with that, I thought it was up to me to create a forex-proxy service from scratch!

Hence my challenge was to meet your other requirements by building a brand new forex-proxy that :
 * fetches rates from OneFrame (uses both single gets and streaming)
 * provides a client interface to fetch rates 
 * overcomes the 1,000 rate request limit from OneFrame's get
 * overcomes the ambiguous "Invalid Currency Pair" with multiple requests
 * supplies rates no older than 5 minuets

I met the above and have a working solution to show if you would like me to demo and explain the workings. This was probably harder for me than using an existing framework as an example to follow.

Further, to build a forex-proxy server my challenges were:
 * select a framework to use
 * understand how to use that framework as:
     i) a client of OneFrame
    ii) a server for a client that wants to fetch rates

"Some of the traits/specifics we are looking for using this exercise:"
Q) How can you navigate through an existing codebase;
* my navigations were through new frameworks and how to use them

Q) How easily do you pick up concepts, techniques and/or libraries you might not have encountered/used before;
* new concepts with jax-rs and Grizzly: wiring, annotations, and web applications in general 
* starting to get going with lambdas, would love to use them more balancing trying new with getting on 

Q) How do you work with third-party APIs that might not be (as) complete (as we would wish them to be);
* further, parts of the Jax-rs frame that were 'complete' but obtuse and required workarounds mapping of exceptions into http rest responses 
* configure the logging levels for below info, hence non-production system outs used

Q)How do you work around restrictions;
* enjoyed this part with OneFrame, changing single requests into pooled streaming requests
* added in client currency-pair pre-validation before checking once with OneFrame

Q) What design choices do you make;
* working around the 1,000 request limit with my RateSupplierService is something I am happy with
* being a novice writing web services, I can see I would quickly do things differently
* most of my choices were about proving how things worked, rather than finessing over them - hence the code and solution are not to production standards.
* the code commits are sparse since I was creating new code and not evolving an existing code base.

Q) How do you think beyond the happy path.
* by choosing the very difficult path with no code framework to start with and no familiarity with web frameworks. 
* with the multi-threading and error protections in the RateSupplierService both try to make things happy and prepare for the worst.
* and by writing tests for the unhappy path

Framework choice:
After looking at some comparison reviews I installed both spring-boot and jax-rs Eclipse Jersey. Spring Boot because I used Spring once when it was new, and Jersey because I'm an Eclipse user. The Eclipse Jersey framework built with maven (another new tech) self-selected itself, since I was unable to build the Spring Boot example project.

To run and test I have
OneFrame running on port 8080 from within docker
my Forex-Proxy running on port 8081 started with mvn execjava 

Client API endpoint available are:
http://localhost:8081/api/rates?pair=JPYNZD
http://localhost:8081/dev/buckets
http://localhost:8081/dev/conf


How the 1,000 rate request limit from OneFrame is overcome:
The getting of a rate is split into three cases:
* first time
* second and onwards where rate is lively 
* second and onwards where rate is stale

For "first time" fetches the RateSupplier:
* pre validates currency pairs
* makes one OneFrame/rates?pair= call for the initial price and further currency pair validation
* queues and starts a OneFrame::/streaming/rates?pairs call

For "second and onwards where rate is lively":
* looks up and returns the latest rate from a hash map

For "second and onwards where rate is stale":
* looks up rate, finds it stale, restarts a streaming rates request
* waits for streaming rate result, rate marked as fresh
* returns fresh rate
* NOTE a rate does not go stale until 5 mins after readSteal stopped the flow of streaming rates.

The rate the RateSupplierServices uses buckets where each bucket has streaming/rate fetches with the same number of currency pairs being fetched.
e.g. with three buckets configured and 9 currency pairs
1 [GBPNZD]
2 [JPYGBP,USDCAD]
3 [EURAUD,GBPCHF, EURJPY],[GBPCHF,AUDUSD,EURCAD]

If the GBPCHF rate were not fetched by a client for more the  RateStaleDurataion. Then GBPCHF would be stale and the buckets structure for OneFrame::/streaming/rates?pairs calls would become;
1 [GBPNZD]
2 [JPYGBP,USDCAD],[EURAUD,EURJPY]
3 [GBPCHF,AUDUSD,EURCAD]

I look forward to explaining more to you at your team.

Yours sincerely,
Chris

PS (added this morning Tue 30th) I wish I had found the Scala challenge, as I wrote far too many old fassioned loops seeing what the Scala language can do this morning after installing it.

