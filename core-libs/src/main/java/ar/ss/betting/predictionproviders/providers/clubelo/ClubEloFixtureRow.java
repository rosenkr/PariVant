package ar.ss.betting.predictionproviders.providers.clubelo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ClubEloFixtureRow {
    String home;
    String away;
    String date;

    double gdMinusMoreThan5;
    double gdMinus5;
    double gdMinus4;
    double gdMinus3;
    double gdMinus2;
    double gdMinus1;
    double gd0;
    double gd1;
    double gd2;
    double gd3;
    double gd4;
    double gd5;
    double gdMoreThan5;
}
