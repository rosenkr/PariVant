package ar.ss.betting.domain;

public enum RoundType {
    STRYKTIPSET(13),
    EUROPATIPSET(13),
    TOPPTIPSET(8);

    private final int numberOfMatches;

    RoundType(int numberOfMatches) {
        if (numberOfMatches <= 0) {
            throw new IllegalArgumentException("numberOfMatches must be positive");
        }
        this.numberOfMatches = numberOfMatches;
    }

    public int getNumberOfMatches() {
        return numberOfMatches;
    }
}