package ar.ss.betting.predictionproviders.providers.bzzoiro;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class BzzoiroResponse {
    private final Instant fetchedAt;
    private final List<BzzoiroPredictionRow> predictions;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BzzoiroPage {
        private Integer count;
        private String next;
        private String previous;
        private List<BzzoiroPredictionRow> results;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BzzoiroPredictionRow {
        private Integer id;

        @JsonProperty("created_at")
        private String createdAt;

        @JsonProperty("prob_home_win")
        private Double probHomeWin;

        @JsonProperty("prob_draw")
        private Double probDraw;

        @JsonProperty("prob_away_win")
        private Double probAwayWin;

        private BzzoiroEvent event;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BzzoiroEvent {
        private Integer id;

        @JsonProperty("home_team")
        private String homeTeam;

        @JsonProperty("away_team")
        private String awayTeam;

        @JsonProperty("event_date")
        private String eventDate;
    }
}