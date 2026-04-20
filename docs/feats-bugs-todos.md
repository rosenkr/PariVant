

3. Impl Auth V1, first see https://www.youtube.com/watch?v=eYCOzPx3ht8
4. about page add content (clickable card, for example "V1 model" with an image)
--------------------------------------------------------------------------------------------------------
--------------------------------------------------------------------------------------------------------
--------------------------------------------------------------------------------------------------------



--------------------------------------------------------------------------------------------------------

V2:
1. Refine Ensemble model
2. mobile-friendly rework
3. Explanatory AI messages per pick
4. More information in info panel (weather, injuries, type of clash, not sure what else). Goal is to provide value to make bettor more informed
5. Impl the About page explaining the model, the purpose, constraints, stats/maths + fresh domain data + knowledge = Parivant logo, etc

--------------------------------------------------------------------------------------------------------

Control of already implemented code behavior:
1. Investigate how I handle an ended round: trigger, storage, presentation, correctness, match scores
    - How does it intermingle with model runs, compare result to model run. On ended page, store actual result
    - and comparison to model, display the models hitrate. Must have a solid way of knowing the scores of all matches at end of a round
2. Ensure match 1x2 border is gradienty and depends on live score
3. check that T-15 model runs are being generated

--------------------------------------------------------------------------------------------------------
Other:
0. Test the app with various testing tools (QA, mockito, static code analyses, linting)
0. Add standardized logging system over the whole code (for example every scheduled action)
2. CI/CD pipeline: Jenkins
3. Add more ingestion for rounds (Tipzer only gives 1 per type, best would be to scrape off source SS or the other website if has)
3. Buy domain name PariVant.se? deploy on railway (PariVant) (separate frontend/backend servers?)
4. Do user research: create name PariVant on social media, buy host name, enter forums for swedish bettors, or skugga. For example flashback. User research.
    - Write down pain points, wishes, etc with the betting experience or svenskaspel.
    - Sites?: flashback, sweclockers, reddit, ???
5. keep adding team names to match resolver - check 11elo for german matches, bzzoiro,
   -use the exporting tool once a day
   -bugfix: aliases.put("paris saint germain", "paris saint germain"); inferred from api-football "Paris Saint Germain vs Liverpool" matched to truth "Paris Saint-Germain|Liverpool"
    - team name normalizer strips "-" from db which is bad
8. "rework/" should be split into sensible structure over time
8. Clean up Instant/OffsetDatetime/LocalDateTime drift across whole project
9. add robots.txt
11. introduce differernt runtime environments dev/test/staging/prod? flavors?
12. add extensive logging in the code (dev env)?
13. do we want to present only swedish-time for matches? 
14. fix parivant icon (works bad depending on background)
15. inspire UX colors dark blue from https://felixastner.com/articles/enhancing-mui-theming-with-typescript
16. The score = KL term contribution, is in fact more dog-heavy than the previous algorithm. 
    -It is pure value based. This is not what I want, given that base pick selector uses this
    - Renyi with alpha 0.5 im unsure if it works
    - try to tune the score post-edge finding (KL term), but how?
    - an alternative is to have base pick choose internal faves, and then coverage picks go for pure value
    - How to know which alternative best? ideally would run a set of different models, but I dont have the data to backtest
    - if sticing with base = value, then a good middlesolution by 
    - gpt is: score_i = (KL term_i) * p_i^B where beta=1 is reasonable:
    - Might thus settle for riskAverseKLedgePowerWeighting(...)
    - Can at least test vs my intuition (which naturally tries to bake in risk with value)
    - by looking at real internal vs public distrs for matches and calculating scores and see if highest score matches intuition
-User:
7. view my past results, compare with base model
8. allow one confident pick that overrides model, can track stats for this

Notes for Railway:
1. deploy Railway
   backend = Spring boot
   frontend = Vite/React web service
   postgres = Railway Postgres
   Wait for parivant.se to register, then connect to it
   Configure build options to use my dockerfile (Service -> Settings -> Build -> Deploy)
   Set preferred region to Stockholm Settings -> Deploy -. Regions
   Set Usage Limits to have spending cap!
2. restart policy: set to Never
   Configure (frontend)service to pause on no traffic (serverless deployment) (App Sleep) - but backend/db make outbound calls every min, so cant sleep
   Service communication (backend/database/frontend?) using
   Settings -> Networking to set domain for service, and Reference variables
   to share that with other services (SERVICE_NAME.railway.internal). service called api? then http://api.railway.internal:PORT
   Disable TCP proxy (avoid public connections for NON HTTP services)
   Ensure using  private network for inbetween service comms, + private env vars
   railway future: prod/dev environments, check suites, config as code in toml/json
   Cloudflare for WAF&DDoS mitigation.
3. be wary of memory leaks - can increease RAM costs
