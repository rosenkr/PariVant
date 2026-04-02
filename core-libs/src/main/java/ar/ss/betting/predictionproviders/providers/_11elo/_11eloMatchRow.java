package ar.ss.betting.predictionproviders.providers._11elo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
public class _11eloMatchRow {
    private String id;
    private String date;
    private String homeTeam;
    private String awayTeam;
    private Double homeElo;
    private Double awayElo;
    private Double eloDiff;
    private String competition;
    private Integer matchDay;
    private _11eloPrediction prediction;
}