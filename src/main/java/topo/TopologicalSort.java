package topo;

import model.Edge;
import model.Graph;
import model.OperationCounter;

import java.util.*;

public class TopologicalSort {

    public static List<Integer> kahn(Graph dag, OperationCounter counter) {
        if (counter == null) counter = new OperationCounter();
        int n = dag.getN();
        int[] indeg = new int[n];
        for (int u = 0; u < n; u++) {
            for (Edge e : dag.outgoing(u)) indeg[e.to()]++;
        }

        Deque<Integer> q = new ArrayDeque<>();
        for (int i = 0; i < n; i++) if (indeg[i] == 0) q.add(i);

        List<Integer> topo = new ArrayList<>();
        while (!q.isEmpty()) {
            int u = q.poll();
            counter.inc("kahn_pops");
            topo.add(u);
            for (Edge e : dag.outgoing(u)) {
                counter.inc("kahn_edge_checks");
                indeg[e.to()]--;
                if (indeg[e.to()] == 0) q.add(e.to());
            }
        }
        return topo;
    }
}
