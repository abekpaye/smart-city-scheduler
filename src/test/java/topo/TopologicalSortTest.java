package topo;

import scc.TarjanSCC;
import model.*;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TopologicalSortTest {

    @Test
    public void testCondensationAndTopo() {
        Graph g = new Graph(8, true);
        g.addEdge(0, 1, 3);
        g.addEdge(1, 2, 2);
        g.addEdge(2, 3, 4);
        g.addEdge(3, 1, 1);
        g.addEdge(4, 5, 2);
        g.addEdge(5, 6, 5);
        g.addEdge(6, 7, 1);

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
        List<Integer> order = TopologicalSort.kahn(dag, topoCounter);

        assertEquals(dag.getN(), order.size(), "Topological sort should include all nodes");
    }
}
