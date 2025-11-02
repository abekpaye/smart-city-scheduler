package model;

import java.util.*;

/**
 * Simple adjacency-list graph. adjacency lists are encapsulated.
 */
public class Graph {
    private final int n;
    private final boolean directed;
    private final List<List<Edge>> adj;

    public Graph(int n, boolean directed) {
        this.n = n;
        this.directed = directed;
        this.adj = new ArrayList<>();
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
    }

    public int getN() { return n; }
    public boolean isDirected() { return directed; }

    /**
     * Add directed edge u->v with weight w. If graph is undirected, also adds v->u.
     */
    public void addEdge(int u, int v, int w) {
        adj.get(u).add(new Edge(u, v, w));
        if (!directed) adj.get(v).add(new Edge(v, u, w));
    }

    /**
     * Return read-only view of adjacency list for vertex u.
     */
    public List<Edge> outgoing(int u) {
        return Collections.unmodifiableList(adj.get(u));
    }

    /**
     * Return full adjacency lists (unmodifiable deep; caller should not mutate lists).
     */
    public List<List<Edge>> getAdj() {
        List<List<Edge>> copy = new ArrayList<>(n);
        for (List<Edge> list : adj) copy.add(Collections.unmodifiableList(list));
        return Collections.unmodifiableList(copy);
    }
}
