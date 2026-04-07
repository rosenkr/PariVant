
feats/bugs/todos

General:
0. refine 1x2 (base pick by model as darker orange, cover picks current light orange, pink border for either 1x2 on live score depending on fetched score)
1. default if no stryktipset to show eur, if no eur, show topptips, if no topptips, show stryk
3. add more/other ingestion (relating to tipzer) so can have multiple upcoming rounds for a type (especially topptipset but also happens for europatipset)
   - for example web scraping other websites or even SS.
4. rework value as KL divergenece for value calculation (or renyi with R = risk aversion = alpha param in Renyi = 1 for neutral risk)
    -currently may be crude way of using subtraction
5. deploy on railway (PariVant) (separate frontend/backend servers?)

6. create name PariVant on social media, buy host name, enter forums for swedish bettors, or skugga. For example flashback. User research.
    - Write down pain points, wishes, etc with the betting experience or svenskaspel. 
    - Sites?: flashback, sweclockers, reddit, ???



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
1. see over JsonUtils/ApiFootballClient/ApiExceptionHandler
2. Lombokize everything to reduce boilerplate in persistence code
3. Clean up Instant/OffsetDatetime/LocalDateTime drift across whole project
4. add robots.txt

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
