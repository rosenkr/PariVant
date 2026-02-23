package ar.ss.betting.domain;

public enum GameType {

    STRYKTIPSET(13),
    EUROPATIPSET(13),
    TOPPTIPSET(8);

    private final int numberOfMatches;

    GameType(int numberOfMatches) {
        this.numberOfMatches = numberOfMatches;
    }

    public int getNumberOfMatches() {
        return numberOfMatches;
    }
}
