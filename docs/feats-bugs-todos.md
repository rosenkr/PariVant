
feats/bugs/todos:
0. consider moving tipzer code to a package under core-libs, with interface etc as prediction providers. Both can be placed under a common network package
1. Display, per match, the %'s from provider module 
2. add bzzoiro prediction provider, FootballData has predictions? https://www.api-football.com/documentation-v3#section/Authentication



3. add My Page page (authentication). User table? Security? Views? gmail?
    -sliders&tags
    -run model
    -modify selection
    -submit personal selection (1 per round)
    -upload own internal probs
       -view my past results, compare with base model
6. add gradients/animations
7. rework value as KL divergenece for value calculation (or renyi with R = risk aversion = alpha param in Renyi = 1 for neutral risk)
8. UI: use 11elo icons and navbar layout + about page + toggle dark/light mode + footer

9. keep adding team names to match resolver

10. Clean up Instant/OffsetDatetime/LocalDateTime drift across whole project
11. Lombokize everything to reduce boilerplate in persistence code



12. deploy on railway (separate frontend/backend servers?)

35. explanatory ai generated message for each pick
20. Add highlighter to navbar on-hover like systemvetardagen.se
15. add robots.txt
25. website background animated bright/dark depending on time of day in sweden
30. employ docker compose workflow, -docker Compose, define  containers for frontend/backend/db in yaml -> easier deployment/development
40. calculate expected roi