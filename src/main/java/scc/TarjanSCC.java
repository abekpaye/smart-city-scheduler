package scc;

import model.Graph;
import model.OperationCounter;
import model.Edge;
import java.util.*;

/**
 * Tarjan's SCC algorithm.
 */
public class TarjanSCC {
    private int time;
    private int[] disc;
    private int[] low;
    private boolean[] inStack;
    private Deque<Integer> stack;
    private List<List<Integer>> components;
    private OperationCounter counter;

    /**
     * Find SCCs of graph g. Counter will be updated with keys:
     * - "dfs_visits" : increments when a vertex is first visited
     * - "edges_traversed" : increments per examined outgoing edge
     */
    public List<List<Integer>> findSCC(Graph g, OperationCounter counter) {
        this.counter = counter == null ? new OperationCounter() : counter;
        int n = g.getN();
        time = 0;
        disc = new int[n];
        low = new int[n];
        inStack = new boolean[n];
        stack = new ArrayDeque<>();
        components = new ArrayList<>();

        Arrays.fill(disc, 0);

        for (int u = 0; u < n; u++) {
            if (disc[u] == 0) {
                dfs(g, u);
            }
        }
        return components;
    }

    private void dfs(Graph g, int u) {
        counter.inc("dfs_visits");
        disc[u] = low[u] = ++time;
        stack.push(u);
        inStack[u] = true;

        for (Edge e : g.outgoing(u)) {
            counter.inc("edges_traversed");
            int v = e.to();
            if (disc[v] == 0) {
                dfs(g, v);
                low[u] = Math.min(low[u], low[v]);
            } else if (inStack[v]) {
                low[u] = Math.min(low[u], disc[v]);
            }
        }

        if (low[u] == disc[u]) {
            List<Integer> compNodes = new ArrayList<>();
            int w;
            do {
                w = stack.pop();
                inStack[w] = false;
                compNodes.add(w);
            } while (w != u);
            components.add(compNodes);
        }
    }
}
