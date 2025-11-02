package paths;

import model.Edge;
import model.Graph;
import model.OperationCounter;

import java.util.*;

public class DAGLongestPath {


    public static double[] findLongestPaths(Graph dag, int source, List<Integer> topoOrder, OperationCounter counter) {
        if (counter == null) counter = new OperationCounter();
        int n = dag.getN();
        double[] dist = new double[n];
        Arrays.fill(dist, Double.NEGATIVE_INFINITY);
        dist[source] = 0.0;

        for (int u : topoOrder) {
            if (dist[u] != Double.NEGATIVE_INFINITY) {
                for (Edge e : dag.outgoing(u)) {
                    counter.inc("edge_checks");
                    int v = e.to();
                    double nd = dist[u] + e.weight();
                    if (nd > dist[v]) {
                        dist[v] = nd;
                        counter.inc("relaxations");
                    }
                }
            }
        }
        return dist;
    }
}
