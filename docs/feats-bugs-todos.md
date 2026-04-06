
feats/bugs/todos

General:
1. fix no-odds-available parsing bug (accept it as specail case, dfeault to svf)
2. tab above round to switch which round for a round type?
    - the idea is, there may be not only 1 but up to (no more than) 10 upcoming rounds for topptipset, and even fewer for stryk/europatipset
    - thus can have a "round1","round2",... adaptive ui navbar like field above the presentation of the current round which allows one to swap between these. 
    - the default one should be shown leftmost and is with the closest kickoff
    - Preferred over a dropdown, and should be okay spacewise since the domain dictates there can only be so many upcoming rounds of a type at once
    - this navbar field should be shown above the round display. if there is only 1 round, it should still show it as 1 selected round
3. add more/other ingestion (relating to tipzer) so can have multiple upcoming rounds for a type (especially topptipset but also happens for europatipset)
   - for example web scraping other websites or even SS.
4. rework value as KL divergenece for value calculation (or renyi with R = risk aversion = alpha param in Renyi = 1 for neutral risk)
    -currently may be crude way of using subtraction
5. deploy on railway (PariVant) (separate frontend/backend servers?)



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
