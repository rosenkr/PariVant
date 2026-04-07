
feats/bugs/todos

General:
0. refine 1x2 pills in a match view:
    -base pick by model as darker orange, cover picks remain current light orange
    -pink border for the score (static pink when match not started at DRAW), 
        but as soon as livescore poller connects, add gradient to the live score, it will move between 1x2 as teams score

2. add more/other ingestion (relating to tipzer) so can have multiple upcoming rounds for a type (especially topptipset but also happens for europatipset)
   - for example web scraping other websites or even SS.
4. rework value as KL divergenece for value calculation (or renyi with R = risk aversion = alpha param in Renyi = 1 for neutral risk)
    -currently may be crude way of using subtraction
5. deploy on railway (PariVant) (separate frontend/backend servers?)

6. create name PariVant on social media, buy host name, enter forums for swedish bettors, or skugga. For example flashback. User research.
    - Write down pain points, wishes, etc with the betting experience or svenskaspel. 
    - Sites?: flashback, sweclockers, reddit, ???
7. Rework pink to bright cyan (for dark mode). Put it under a theme or colors folder to avoid hardcoding colors


Auth (MVP) work:
    -what is the modern way for handling secure auth and seamless UX?
    -How will I persist a user and store their decisions to build my own defensible data set which can refine my own model? (for example if allowing users 1 high confidence pick per round, then check their ROI over time, then mix their knowledge into the model run step accordingly)
    -how to do it safe/secure/law-abiding GDPR/modern web dev style?
1. add My Page dashboard page (authentication). User table? Security? Views? gmail?
2. sliders&tags
3. run model
4. modify selection
5. submit personal selection (1 per round)
6. upload/set own internal probs
7. view my past results, compare with base model
8. allow one confident pick that overrides model, can track stats for this

cleanup:
0. keep adding team names to match resolver - check 11elo for german matches, bzzoiro,
      -use the exporting tool once a day
2. bugfix: aliases.put("paris saint germain", "paris saint germain"); inferred from api-football "Paris Saint Germain vs Liverpool" matched to truth "Paris Saint-Germain|Liverpool"
   - team name normalizer strips "-" from db which is bad
1. see over JsonUtils/ApiFootballClient/ApiExceptionHandler
2. Lombokize everything to reduce boilerplate in persistence code
3. Clean up Instant/OffsetDatetime/LocalDateTime drift across whole project
4. add robots.txt
5. check that T-15 model runs are being generated
6. add an About page explaining the model, the purpose, restrictions, etc
7. Investigate how I handle an ended round: trigger, storage, presentation, correctness, match scores
   - How does it intermingle with model runs, compare result to model run. On ended page, store actual result
   - and comparison to model, display the models hitrate. Must have a solid way of knowing the scores of all matches at end of a round

ui refinements: 
0. color rework (pink/dark green)
1. add gradients/animations
2. use 11elo icons and navbar layout + about page + toggle dark/light mode + footer
3. Add highlighter to navbar on-hover like systemvetardagen.se


wishes:
*Information panel: more non-model "live" info to influence bettor
-explanatory ai generated message for each pick
-refined model
-investigate docker compose workflow, -docker Compose, define  containers for frontend/backend/db in yaml -> easier deployment/development
-calculate expected roi
-mobile-friendly rework
-FootballData match predictions? https://www.football-data.org/documentation/quickstart
    -costs 15$ month .. 
