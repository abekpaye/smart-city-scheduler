package model;

/**
 * Immutable edge representation.
 * Stores source (from), destination (to) and weight.
 */
public final class Edge {
    private final int from;
    private final int to;
    private final int weight;

    public Edge(int from, int to, int weight) {
        this.from = from;
        this.to = to;
        this.weight = weight;
    }

    public int from() { return from; }
    public int to() { return to; }
    public int weight() { return weight; }

    @Override
    public String toString() {
        return String.format("Edge(%d -> %d, w=%d)", from, to, weight);
    }
}

