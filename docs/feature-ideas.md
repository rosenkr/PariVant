1. The base pick is highlighted to the user when viewing the model selections for a round

2. How to think about what kind of information is already baked in to market odds?
For example most bettors already look at obvious things like recent form, but the RB model makes no assumption about this.
Whereas other info may matter but is barely accounted for by bettors or with what odds bookies decide to open a match (rain forecast? fake grass?)
Possible solution: Have the model make assumptions about to what grade various signals are 
already baked in to the market odds.


3. How to weight signals internally? Research or backcalculations
4. There may be a correlation between 3 & 4 if we consider for example recent form as a signal; 
In 3, we may deem it baked in a lot, so we reduce its impact in the adjustment phase,
but in 4, we deem it an important metric, so we increase its impact in said phase, effectively cancelling the effects out. 
How to think about this?

5. (Refactor) Add a Signal class as it is a prevalent concept
6. Add more signals! Consult research, observe the world, own experiences and biases, what data is consistently available?


7. AI/LLM textfield next to each selection which briefly explains why the decision
was made ("<Team 1> has good recent form, is playing to avoid relegation, is playing at home) which is info
that some AI summarizes into words (within some constraints of course) based on actual parameters that
influenced the calculation. But, what type of AI? What is AI in this context, and why not just hardcode texts to 
signals that can be deemed to have influenced the model result a lot? What do we have to gain from using AI tech here?
The reason I want AI is because so many apps use it so surely there must be some case for my app too?

8. Separate app into different views:
- Public (Anyone who enters the site can view these pages or use simple features)
  -  The current model for preset budgets for the 3 round types (Eur, Topp, Stryk) falls in this category
- Authenticated users (Anyone who is browsing the site while registered and logged in)
  - Having auth users opens up for persisting various info for different users.
    - Examples of such data (will be expanded) is persons OWN selections for a round (limited to 1 to make it more meaningful as you only have " 1 shot ").
    - Then they can compare their result to the models, over time they can track their performance Compared! to the model

- Authenticated PAYING subcribers: Can adjust weights (feature) and more?

9. In relation to the above points, it is clear that the MVP should focus on first implementing the public view
and thus make sure the backend also focuses on those parts
   