package ar.ss.betting.roundingest;

import ar.ss.betting.domain.RoundType;

import java.util.List;

public interface RoundIngestSource {

    String sourceName();

    boolean supports(RoundType roundType);

    IngestedRound fetchRound(RoundType roundType);

    List<IngestedRound> fetchAvailableRounds();
}