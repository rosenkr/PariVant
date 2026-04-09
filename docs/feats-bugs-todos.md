V1:
2. "Rework value as KL divergence for value calculation "
3. Impl Auth V1, first see https://www.youtube.com/watch?v=eYCOzPx3ht8

--------------------------------------------------------------------------------------------------------
--------------------------------------------------------------------------------------------------------
--------------------------------------------------------------------------------------------------------
AUTH V1:
    -what is the modern way for handling secure auth and seamless UX?
    -How will I persist a user and store their decisions to build my own defensible data set which can refine my own model? (for example if allowing users 1 high confidence pick per round, then check their ROI over time, then mix their knowledge into the model run step accordingly)
    -how to do it safe/secure/law-abiding GDPR/modern web dev style?
    - Will want to support different views/possibilities for authenticated users vs just website visitor
    - will want to handle payments for additional service in future.
1. add My Page dashboard page (authentication). User table? Security? Views? gmail?
2. sliders&tags
3. run model
4. modify selection (with full coverage?)
5. submit personal selection (1 per round)
6. upload/set own internal probs
7. view my past results, compare with base model
8. allow one confident pick that overrides model, can track stats for this

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
1. Investigate Docker Compose https://www.youtube.com/watch?v=kOryO5I_w14, https://www.youtube.com/watch?v=Q5evuP3OnPY
2. Add more ingestion for rounds (Tipzer only gives 1 per type, best would be to scrape off source SS or the other website if has)
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
