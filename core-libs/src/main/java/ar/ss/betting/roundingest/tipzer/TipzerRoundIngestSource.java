package ar.ss.betting.roundingest.tipzer;

import ar.ss.betting.domain.RoundType;
import ar.ss.betting.roundingest.IngestedRound;
import ar.ss.betting.roundingest.RoundIngestSource;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
@Order(1)
public class TipzerRoundIngestSource implements RoundIngestSource {

    private final TipzerClient tipzerClient;
    private final TipzerParser tipzerParser;
    private final TipzerTopptipsetParser tipzerTopptipsetParser;

    public TipzerRoundIngestSource(TipzerClient tipzerClient,
                                   TipzerParser tipzerParser,
                                   TipzerTopptipsetParser tipzerTopptipsetParser) {
        this.tipzerClient = Objects.requireNonNull(tipzerClient);
        this.tipzerParser = Objects.requireNonNull(tipzerParser);
        this.tipzerTopptipsetParser = Objects.requireNonNull(tipzerTopptipsetParser);
    }

    @Override
    public String sourceName() {
        return "TIPZER";
    }

    @Override
    public boolean supports(RoundType roundType) {
        return roundType == RoundType.STRYKTIPSET
                || roundType == RoundType.EUROPATIPSET
                || roundType == RoundType.TOPPTIPSET;
    }

    @Override
    public IngestedRound fetchRound(RoundType roundType) {
        return switch (roundType) {
            case STRYKTIPSET -> tipzerParser.parse(
                    RoundType.STRYKTIPSET,
                    tipzerClient.getStryktipsetTeamsRaw(),
                    tipzerClient.getStryktipsetSvenskaFolketRaw(),
                    tipzerClient.getStryktipsetOddsRaw()
            );
            case EUROPATIPSET -> tipzerParser.parse(
                    RoundType.EUROPATIPSET,
                    tipzerClient.getEuropatipsetTeamsRaw(),
                    tipzerClient.getEuropatipsetSvenskaFolketRaw(),
                    tipzerClient.getEuropatipsetOddsRaw()
            );
            case TOPPTIPSET -> tipzerTopptipsetParser.parseFromPageHtml(
                    tipzerClient.getTopptipsetPageRaw()
            );
        };
    }

    @Override
    public List<IngestedRound> fetchAvailableRounds() {
        return List.of(
                fetchRound(RoundType.STRYKTIPSET),
                fetchRound(RoundType.EUROPATIPSET),
                fetchRound(RoundType.TOPPTIPSET)
        );
    }
}