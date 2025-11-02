package topo;

import model.Edge;
import model.Graph;

import java.util.*;

public class CondensationGraph {

    public static Graph buildCondensation(Graph g, int[] compIds, int compCount) {
        Graph dag = new Graph(compCount, true);

        Map<Long, Integer> minMap = new HashMap<>();
        int n = g.getN();
        for (int u = 0; u < n; u++) {
            for (Edge e : g.outgoing(u)) {
                int cu = compIds[u];
                int cv = compIds[e.to()];
                if (cu != cv) {
                    long key = (((long)cu) << 32) | (cv & 0xffffffffL);
                    minMap.merge(key, e.weight(), Math::min);
                }
            }
        }

        for (Map.Entry<Long, Integer> en : minMap.entrySet()) {
            long key = en.getKey();
            int cu = (int)(key >> 32);
            int cv = (int) key;
            dag.addEdge(cu, cv, en.getValue());
        }
        return dag;
    }
}
