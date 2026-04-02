package ar.ss.betting.predictionproviders.providers._11elo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
public class _11eloPrediction {
    private Integer homeWin;
    private Integer draw;
    private Integer awayWin;
}