package app;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import scc.TarjanSCC;
import topo.*;
import paths.*;
import model.*;

import java.io.*;
import java.util.*;

public class SmartCityScheduler {
    public static void main(String[] args) throws Exception {
        String inputPath = args.length > 0 ? args[0] : "data/input.json";
        String outputPath = args.length > 1 ? args[1] : "data/output.json";

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(new File(inputPath));

        ArrayNode allOutputs = mapper.createArrayNode();

        // обработка массива графов
        if (root.isArray()) {
            int index = 0;
            for (JsonNode dataset : root) {
                System.out.println("Processing dataset " + index + "...");
                ObjectNode result = processSingleDataset(dataset, mapper, inputPath, index);
                allOutputs.add(result);
                index++;
            }
        } else {
            ObjectNode result = processSingleDataset(root, mapper, inputPath, 0);
            allOutputs.add(result);
        }

        mapper.writerWithDefaultPrettyPrinter().writeValue(new File(outputPath), allOutputs);
        System.out.println("Results saved to " + outputPath);
    }

    private static ObjectNode processSingleDataset(JsonNode root, ObjectMapper mapper, String inputPath, int datasetIndex) {
        boolean directed = root.get("directed").asBoolean();
        int n = root.get("n").asInt();
        Graph g = new Graph(n, directed);

        for (JsonNode e : root.get("edges")) {
            int u = e.get("u").asInt();
            int v = e.get("v").asInt();
            int w = e.has("w") ? e.get("w").asInt() : 1;
            g.addEdge(u, v, w);
        }

        int source = root.has("source") ? root.get("source").asInt() : 0;
        String weightModel = root.has("weight_model") ? root.get("weight_model").asText() : "edge";

        ObjectNode output = mapper.createObjectNode();
        output.put("dataset_index", datasetIndex);
        output.put("input_path", inputPath);
        output.put("weight_model", weightModel);
        output.put("source_vertex", source);

        long t0 = System.nanoTime();

        OperationCounter sccCounter = new OperationCounter();
        long sccStart = System.nanoTime();
        TarjanSCC tarjan = new TarjanSCC();
        List<List<Integer>> sccs = tarjan.findSCC(g, sccCounter);
        long sccEnd = System.nanoTime();

        int[] compIds = new int[g.getN()];
        int compIndex = 0;
        for (List<Integer> comp : sccs) {
            for (int v : comp) compIds[v] = compIndex;
            compIndex++;
        }
        output.put("num_components", sccs.size());

        ArrayNode sccArray = mapper.createArrayNode();
        ArrayNode sccSizes = mapper.createArrayNode();
        for (List<Integer> comp : sccs) {
            ArrayNode arr = mapper.createArrayNode();
            for (int v : comp) arr.add(v);
            sccArray.add(arr);
            sccSizes.add(comp.size());
        }
        output.set("SCCs", sccArray);
        output.set("SCC_sizes", sccSizes);

        long condStart = System.nanoTime();
        Graph dag = CondensationGraph.buildCondensation(g, compIds, sccs.size());
        long condEnd = System.nanoTime();

        ArrayNode condEdges = mapper.createArrayNode();
        for (int u = 0; u < dag.getN(); u++) {
            for (Edge e : dag.outgoing(u)) {
                ObjectNode eNode = mapper.createObjectNode();
                eNode.put("from", e.from());
                eNode.put("to", e.to());
                eNode.put("w", e.weight());
                condEdges.add(eNode);
            }
        }
        output.set("condensation_edges", condEdges);

        OperationCounter topoCounter = new OperationCounter();
        long topoStart = System.nanoTime();
        List<Integer> topoOrder = TopologicalSort.kahn(dag, topoCounter);
        long topoEnd = System.nanoTime();

        if (topoOrder.size() != dag.getN())
            output.put("warning", "Topological sort incomplete (possible cycle in condensation graph)");

        int sourceComp = compIds[source];
        output.put("source_component", sourceComp);

        List<Integer> derivedOrder = new ArrayList<>();
        for (int comp : topoOrder) derivedOrder.addAll(sccs.get(comp));
        ArrayNode derivedArr = mapper.createArrayNode();
        for (int v : derivedOrder) derivedArr.add(v);
        output.set("derived_task_order", derivedArr);

        OperationCounter shortCounter = new OperationCounter();
        long spStart = System.nanoTime();
        double[] shortest = DAGShortestPath.findShortestPaths(dag, sourceComp, topoOrder, shortCounter);
        long spEnd = System.nanoTime();

        OperationCounter longCounter = new OperationCounter();
        long lpStart = System.nanoTime();
        double[] longest = DAGLongestPath.findLongestPaths(dag, sourceComp, topoOrder, longCounter);
        long lpEnd = System.nanoTime();

        List<Integer> criticalPath = reconstructLongestPath(dag, topoOrder, sourceComp, longest);
        double critLen = Double.NEGATIVE_INFINITY;
        for (double d : longest) critLen = Math.max(critLen, d);

        ArrayNode topoArr = mapper.createArrayNode();
        for (int v : topoOrder) topoArr.add(v);
        output.set("topological_order", topoArr);

        ArrayNode shortestArr = mapper.createArrayNode();
        for (double d : shortest)
            shortestArr.addPOJO(Double.isInfinite(d) ? (d > 0 ? "Infinity" : "-Infinity") : d);
        output.set("shortest_distances", shortestArr);

        ArrayNode longestArr = mapper.createArrayNode();
        for (double d : longest)
            longestArr.addPOJO(Double.isInfinite(d) ? (d > 0 ? "Infinity" : "-Infinity") : d);
        output.set("longest_distances", longestArr);

        ArrayNode critArr = mapper.createArrayNode();
        for (int v : criticalPath) critArr.add(v);
        output.set("critical_path", critArr);
        output.put("critical_length", critLen);

        ObjectNode metrics = mapper.createObjectNode();
        metrics.set("scc_counters", mapper.valueToTree(sccCounter.getAll()));
        metrics.put("scc_time_ns", sccEnd - sccStart);
        metrics.put("condensation_time_ns", condEnd - condStart);
        metrics.set("topo_counters", mapper.valueToTree(topoCounter.getAll()));
        metrics.put("topo_time_ns", topoEnd - topoStart);
        metrics.set("shortest_counters", mapper.valueToTree(shortCounter.getAll()));
        metrics.put("shortest_time_ns", spEnd - spStart);
        metrics.set("longest_counters", mapper.valueToTree(longCounter.getAll()));
        metrics.put("longest_time_ns", lpEnd - lpStart);
        metrics.put("total_time_ns", System.nanoTime() - t0);
        output.set("metrics", metrics);

        return output;
    }

    private static List<Integer> reconstructLongestPath(Graph dag, List<Integer> topo, int source, double[] dist) {
        int n = dag.getN();
        double maxDist = Double.NEGATIVE_INFINITY;
        int end = -1;
        int[] prev = new int[n];
        Arrays.fill(prev, -1);

        for (int u : topo) {
            for (Edge e : dag.outgoing(u)) {
                int v = e.to();
                if (dist[u] != Double.NEGATIVE_INFINITY && dist[v] == dist[u] + e.weight()) {
                    prev[v] = u;
                }
            }
        }

        for (int i = 0; i < n; i++) {
            if (dist[i] > maxDist) {
                maxDist = dist[i];
                end = i;
            }
        }

        List<Integer> path = new ArrayList<>();
        if (end == -1) return path;
        for (int v = end; v != -1; v = prev[v]) path.add(v);
        Collections.reverse(path);
        return path;
    }
}
