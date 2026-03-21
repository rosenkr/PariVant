
feats/bugs/todos:
-calculate expected roi
-explanatory ai generated message for each pick
-allow User to select their own picks for a given game and save
-allow User to view their saved picks
-separate pages for upcoming/running/completed games
-ability to display, per match, the %'s from provider module
-UI: use 11elo icons and navbar layout + about page + toggle dark/light mode + footer
-auth -> different views -> 
    public = main page of ended/upcoming/running and past prediction><results
    user = sliders,tags, run model, make changes, create/save up to 1 coupon per round, saved rounds page with comparison to default model results
-Add highlighter to navbar on-hover like systemvetardagen.se
-add robots.txt
-integrate live-odds module to live page
-perform model re-work, see model-rework.md
-refactor GameRound -> Round. GameType` → `RoundType`.
-remove manual ingest folder/classes
-website background animated bright/dark depending on time of day in sweden
-employ docker compose workflow
-user feature: upload own internal probs and run model on that (key factor: ease)
-add ingestion fallbacks for rounds
-add github readme(with very general text, see chat on phone), make only README file public
-deploy on railway (separate frontend/backend servers?)
-docker Compose, define  containers for frontend/backend/db in yaml -> easier deployment/development
-break out into own module: Round ingestion
-Rework main page backend logic to: UI load public/upcoming, backend shows upcoming for stryktipset if has, else for europatipset if has, else for topptippset if has, else stryktipset page
 So (api could be live/upcoming/ended) (sorted by time).
-incorporate libs from public github
-add bzzoiro, its back up

-add lombok to reduce boilerplate in persistenc code
-rework @Lob annotations in entities when persisting json, bad style
-if want to do ui work: just mock data? dont rely on server
- rework match&round end logic by using status instead of end-dte + 2 hr (see step2.md) 
-modular monolith rework: split backend into application/infra/web. combine with libs on gh
- add gradients/animations
- FootballData has predictions? https://www.api-football.com/documentation-v3#section/Authentication