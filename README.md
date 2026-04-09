# Decision-Support System for Pool Betting on Svenska Spel Rounds

A decision-support system for pool betting on Svenska Spel rounds, including **Topptipset**, **Stryktipset**, and **Europatipset**.

## Project Status

This project is **currently under construction**. Both the model and the project as a whole are still being developed, evaluated, and refined. The current version should therefore be seen as an evolving prototype rather than a finished product.

### Goal

The goal of the project is to **improve the betting experience** by providing structured, data-driven decision support for coupon construction.

Rather than relying only on intuition or public sentiment, the system aims to identify useful patterns in how probabilities, market expectations, and public betting behavior differ.

### Probability Model

For each match, the system estimates an internal probability distribution over the three possible outcomes:

- Home
- Draw
- Away

The internal probabilities are computed using:

- an ensemble of predictive models sourced online (ML models typically based on features like xG), and
- market-implied probabilities from bookmaker odds

The ensemble aggregation currently follows a linear pooling approach:

```
p = \frac{1}{n+1}\left(m + \sum_{i=1}^{n} q_i\right)
```


### Core Edge

A central idea in the project is a **KL-divergence-based notion of value**.

The model looks for discrepancies between:

- **internal probabilities**, and
- **the public betting distribution**

These discrepancies are treated as a potential source of edge because they may indicate outcomes that are mispriced by the crowd. In other words, when the public betting distribution differs meaningfully from the probability assessment implied by the model and market information, the resulting coupon may achieve better expected value.

KL divergence is used as a way to quantify these differences. The underlying assumption is that larger and more relevant divergences can help identify selections that improve the overall expected value of the coupon, even though this relationship is still being explored as part of the ongoing development of the project.

### Weaknesses

- Domain knowledge and more up-to-date information, such as injuries or late team news, are not currently incorporated into the model.
- The current model selects picks too naively by focusing mainly on estimated value, which can lead to an unreasonably risky coupon.

### Future additions

- Source up-to-date information from online sources (data not accounted for by current models) to influence the internal probabilities and/or to provide valuable informational material to the user
