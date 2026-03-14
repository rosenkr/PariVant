1. AI/LLM textfield next to each selection which briefly explains why the decision
was made ("<Team 1> has good recent form, is playing to avoid relegation, is playing at home) which is info
that some AI summarizes into words (within some constraints of course) based on actual parameters that
influenced the calculation. But, what type of AI? What is AI in this context, and why not just hardcode texts to 
signals that can be deemed to have influenced the model result a lot? What do we have to gain from using AI tech here?
The reason I want AI is because so many apps use it so surely there must be some case for my app too?

8. Separate app into different views:
- Public (Anyone who enters the site can view these pages or use simple features)
  -  The current model for preset budgets for the 3 round types (Eur, Topp, Stryk) falls in this category
  - Past results
- Authenticated users (Anyone who is browsing the site while registered and logged in)
  - Having auth users opens up for persisting various info for different users.
    - Examples of such data (will be expanded) is persons OWN selections for a round (limited to 1 to make it more meaningful as you only have " 1 shot ").
    - Then they can compare their result to the models, over time they can track their performance Compared! to the model

- Authenticated PAYING subcribers: Can adjust weights (feature) and more?

showing:
If current still running, show it with livescores
otherwise upcoming

If has no upcoming or current, show most resent finished with results

website background animated bright/dark depending on time of day in sweden

Modify rulebsedmodel: Break out value behavior as more general class to be used for any model
let users insert their own I: Internal probabilities (logged in) and let then run model for that round (thus skipping default rulebased but using the value behavior above)

when game is live, make pink border be animated/glowing
add "starts in ..." countdown timer

New todo:
Add eur + topp ingestion
Have my model not just give picks but also output its own % triple?
fix dates
add github readme, make only this file public + linking to website
add fallback strategy for ingestion (possibly crawler in worst case)
add the bzzoiro model as additional shown info (but not yet part of my model).
add past results page (public) that shows how the model performed so far
add my-page with auth + future features: modify, save, view past results, add "lock plays" i.e only 1 selection for a match, thus making model place leftover selection elsewhere if had 2+
Improve actual rule-based model (more signals) OR introduce new types of models (externally bzzoiro, or others, or do ML on my own but problem is lack of data available).
Add handling for LIVE updates (border glow + LIVE! text at top, live score thru websockets or something?)
Add expert picks from online either as pure info OR have it influence model
buymeacoffee link?
AI-chatbot explaining each pick.
rework md docs + clean code + review architecture
consider security of my site
host/launch server and frontend on Railway on separate domains and handle CORS

Ultimate hybrid system model (Quant-part big data + qual-part modificator(injuries,motivation,..)): Multiple-bookie-average-adjusted-vig-corrected-baseline + multiple data-driven ML models + value-diff vs Sv.F + human-influenced domain knowledge tweaks -> Output

regarding model ideas, Grok:
"Brief summary of tips (8 sentences):
Start with free sources like football-data.co.uk for historical results and odds.
Compute no-vig probabilities from bookie odds as your strongest baseline feature.
Build simple models (Elo, Poisson, or light XGBoost) even with 1–2 seasons of data.
Add cheap signals like form, position, and home advantage before fancy stats.
Transform bookie probs rather than copy them to avoid being a pure follower.
Use hybrid tweaks: blend model output with manual adjustments for match-specific factors.
Backtest rigorously against historical lines to find real edge, nt just accuracy.
Focus on value (your prob > no-vig implied prob after vig) over win rate."

Bug: if current round ends, timed ingester might not ingest for quite a while even if we should see
the next game. Solution; Ingest for that type, immeditely when a round ends