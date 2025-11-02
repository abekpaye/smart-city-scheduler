package scc;

import model.*;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TarjanTest {

    @Test
    public void testSimpleGraph() {
        Graph g = new Graph(4, true);
        g.addEdge(0, 1, 1);
        g.addEdge(1, 2, 1);
        g.addEdge(2, 0, 1);
        g.addEdge(2, 3, 1);

        TarjanSCC tarjan = new TarjanSCC();
        OperationCounter counter = new OperationCounter();
        List<List<Integer>> comps = tarjan.findSCC(g, counter);

        assertEquals(2, comps.size());
        assertTrue(comps.stream().allMatch(c -> c.size() >= 1));
    }
}
