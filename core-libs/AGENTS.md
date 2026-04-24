core-libs exists mostly to make backend folder less cluttered
it contains three packages
matchresolver exists because I fetch football match data from multiple
sites for various reasons, and I need a way to identify which match
that corresponds to according to the source of truth being my ingested
rounds. predictionproviders tries to fetch from various external api's the
predictions for matches, which the backend tries to understand as triples of
floats summing up to 1 +- error.