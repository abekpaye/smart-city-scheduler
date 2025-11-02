package paths;

import scc.TarjanSCC;
import topo.*;
import model.*;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class DAGPathsTest {

    @Test
    public void testShortestAndLongestPathsInCondensedDAG() {
        Graph g = new Graph(7, true);
        g.addEdge(0, 1, 3);
        g.addEdge(1, 2, 4);
        g.addEdge(2, 0, 2);
        g.addEdge(2, 3, 5);
        g.addEdge(3, 4, 1);
        g.addEdge(4, 5, 2);
        g.addEdge(5, 6, 3);

        TarjanSCC tarjan = new TarjanSCC();
        OperationCounter sccCounter = new OperationCounter();
        List<List<Integer>> sccs = tarjan.findSCC(g, sccCounter);

        int[] compIds = new int[g.getN()];
        int id = 0;
        for (List<Integer> comp : sccs) {
            for (int v : comp) compIds[v] = id;
            id++;
        }

        Graph dag = CondensationGraph.buildCondensation(g, compIds, sccs.size());
        OperationCounter topoCounter = new OperationCounter();
        List<Integer> topoOrder = TopologicalSort.kahn(dag, topoCounter);

        assertEquals(dag.getN(), topoOrder.size());

        int sourceVertex = 4;
        int sourceComp = compIds[sourceVertex];

        OperationCounter shortCounter = new OperationCounter();
        double[] shortest = DAGShortestPath.findShortestPaths(dag, sourceComp, topoOrder, shortCounter);

        OperationCounter longCounter = new OperationCounter();
        double[] longest = DAGLongestPath.findLongestPaths(dag, sourceComp, topoOrder, longCounter);

        assertEquals(dag.getN(), shortest.length);
        assertEquals(dag.getN(), longest.length);

        assertTrue(Double.compare(shortest[sourceComp], 0.0) == 0);
        assertTrue(Double.compare(longest[sourceComp], 0.0) == 0);
    }
}
