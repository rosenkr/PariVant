A decision-support system for pool betting on Svenskaspel rounds (Topptipset, Stryktipset, Europatipset)
Goal is to produce a high-quality betting coupon using a value-based model
which tries to find an edge by observing discrepancies in market-implied odds and 
the public betting distribution of a round.

The model calculates an internal win probability using an ensemble of predictive models 
and the market implied win probabilities. The ensemble aggregation follows a weighted linear pool:
$$
p = \frac{1}{n+1}\left(m + \sum_{i=1}^{n} q_i\right)
$$
Where p is a probability triple {home,draw,away} for one match, n is the amount of providers, q and m are probability triples

Given a round such as Stryktipset, the Model first selects one outcome per match based on value.
Then it ranks matches by uncertainty, and adds one additional outcome for as many matches as the given budget allows.

Model outputs internal probabilities per match and a selection of outcomes.


Potential modelling issues: Double counting, not enough providers or unreliable provider data, biased or not robust mathematical model




