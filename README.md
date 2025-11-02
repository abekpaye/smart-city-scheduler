# Smart City / Smart Campus Scheduler
Assignment 4 – Data Analysis and Algorithms

## 1. Overview
This project implements a complete workflow for analyzing and scheduling city-service tasks represented as directed graphs. Each dataset models dependencies between maintenance, cleaning, and sensor subtasks.

The project consolidates two core algorithmic topics:
1. Strongly Connected Components (SCC) and Topological Ordering
2. Shortest and Longest Paths in Directed Acyclic Graphs (DAGs)

The system processes multiple datasets, detects and compresses cyclic dependencies into components, builds the condensation DAG, computes valid execution orders, and analyzes both shortest and critical paths.

---

## 2. Implementation Summary

### 2.1 Graph Algorithms
| Task | Method | Description |
|------|---------|-------------|
| SCC Detection | Tarjan’s Algorithm | Identifies strongly connected components, counts DFS visits and edges. |
| Condensation Graph | Constructed from SCCs | Merges each SCC into a single node; edges preserve original dependencies. |
| Topological Sort | Kahn’s Algorithm | Produces a valid ordering of components and tasks after compression. |
| Shortest Paths | Dynamic Programming over topological order | Computes single-source shortest distances in the DAG. |
| Longest Path (Critical Path) | DP with sign inversion / max aggregation | Finds maximum distance and reconstructs the critical path. |

---

### 2.2 Algorithm Choice Rationale

#### 2.2.1 Strongly Connected Components (SCC)

**Chosen algorithm:** Tarjan’s Algorithm  
**Alternative:** Kosaraju’s Algorithm

**Reasoning:**  
Tarjan’s algorithm detects strongly connected components in a single DFS pass, while Kosaraju’s requires two passes (one on the original graph and one on the reversed graph).  
This makes Tarjan’s method faster in practice and easier to instrument for operation counting (e.g., DFS visits, edges traversed).  
It also integrates naturally into a modular design by reusing the DFS recursion stack for component extraction.

→ *Tarjan chosen for efficiency and compactness.*

---

#### 2.2.2 Topological Sorting

**Chosen algorithm:** Kahn’s Algorithm  
**Alternative:** DFS-based topological sort

**Reasoning:**  
Kahn’s algorithm tracks in-degrees explicitly and processes nodes using a queue-based linear workflow.  
This makes it straightforward to measure operations such as queue pops and pushes.  
Its BFS-like structure is more intuitive and produces deterministic orderings when multiple nodes have equal priority.

The DFS-based approach has similar asymptotic complexity but is less transparent for instrumentation or debugging in large DAGs.

→ *Kahn chosen for clarity, instrumentation, and determinism.*

---

#### 2.2.3 Shortest and Longest Paths in DAG

**Chosen approach:** Dynamic Programming over Topological Order  
**Alternatives:** Bellman–Ford or Dijkstra algorithms

**Reasoning:**  
Since condensation graphs are acyclic, there is no need for algorithms that handle cycles or negative weights.  
Dynamic programming over a known topological order ensures **O(V + E)** complexity — optimal for DAGs.  
Both Bellman–Ford and Dijkstra are slower and less specialized for this case.

→ *Dynamic programming over topological order chosen for optimality and simplicity.*

---

#### 2.2.4 Weight Model

**Chosen model:** Edge-based weights  
**Alternative:** Node duration weights

**Reasoning:**  
Edge weights more accurately represent real dependencies, modeling the delay or cost *between* tasks rather than *within* them.  
They allow for flexible, condition-specific cost modeling between task pairs.  
Using node durations would require splitting nodes to simulate equivalent relationships.

→ *Edge weights chosen for realism and modeling flexibility.*

---

### 2.3 Metrics and Instrumentation

A shared `OperationCounter` class tracks multiple metrics across algorithms:

- **SCC computation:** DFS visits and edge traversals
- **Topological sorting:** Queue operations (pops and pushes)
- **Path computations:** Relaxations during shortest and longest path calculations

Execution times are measured using `System.nanoTime()`,  
and all metrics are serialized into the output JSON file for analysis.

---

### 2.3 Metrics and Instrumentation
A shared `OperationCounter` class tracks:
- DFS visits and edge traversals in SCC computation
- Queue operations (pops/pushes) in Kahn’s algorithm
- Relaxations in shortest and longest path calculations

Execution times are recorded via `System.nanoTime()`.  
All metrics are serialized into the output JSON file.

---

### 2.4 Code Structure
```
src/
 ├── app/
 │    └── SmartCityScheduler.java        // Main entry point
 ├── model/
 │    └── Graph.java, Edge.java
 ├── scc/
 │    └── TarjanSCC.java
 ├── topo/
 │    ├── CondensationGraph.java
 │    └── TopologicalSort.java
 ├── paths/
 │    ├── DAGShortestPath.java
 │    └── DAGLongestPath.java
 ├── util/
 │    └── OperationCounter.java
 └── test/
      ├── scc/TarjanTest.java
      ├── topo/TopologicalSortTest.java
      └── paths/DAGPathsTest.java
data/
 ├── input.json
 └── output.json
```

---

## 3. Datasets

Nine datasets were generated for three complexity levels.

| Category | Nodes (n) | Edges | Density | Cyclic | Description |
|-----------|------------|--------|----------|---------|--------------|
| Small | 6 | 4 | sparse | false | Simple DAG for base correctness tests |
| Small | 8 | 16 | medium | true | Mixed case with small SCCs |
| Small | 10 | 30 | dense | true | Dense cyclic graph |
| Medium | 12 | 18 | sparse | true | Multi-component cyclic structure |
| Medium | 15 | 30 | medium | true | Typical mixed SCC pattern |
| Medium | 18 | 25 | dense | false | Large DAG for topological timing |
| Large | 25 | 37 | sparse | true | Performance test with multiple SCCs |
| Large | 35 | 70 | medium | true | Heavier condensation case |
| Large | 45 | 135 | dense | true | Stress test for runtime and metrics |

All datasets are stored in `/data/input.json` and processed sequentially by `SmartCityScheduler`.

---

## 4. Execution and Output

### 4.1 Run Instructions
```
javac -d out -cp ".:lib/*" src/**/*.java
java -cp out app.SmartCityScheduler data/input.json data/output.json
```

### 4.2 Output Structure
Each dataset in `output.json` includes:
- `input_path`: source file
- `num_components`: number of SCCs
- `SCCs` and `SCC_sizes`
- `condensation_edges`: DAG representation
- `topological_order`, `derived_task_order`
- `shortest_distances`, `longest_distances`
- `critical_path`, `critical_length`
- `metrics`: time and operation counters per stage

Example fragment:
```json
{
  "category": "Small",
  "nodes": 6,
  "num_components": 3,
  "SCCs": [[0], [1,2,3], [4,5]],
  "topological_order": [2, 1, 0],
  "shortest_distances": [0, 3, 5, 6],
  "longest_distances": [0, 4, 8, 9],
  "critical_length": 9
}
```

---

## 5. Results Summary

### 5.1 Performance Overview (aggregated)
| Category | Avg SCC Count | Avg Time (ms) | Notes |
|-----------|----------------|----------------|-------|
| Small | 2–3 | <1 | Ideal for validation |
| Medium | 3–6 | 1–3 | Balanced complexity |
| Large | 6–12 | 3–10 | Stable scaling, no overflows |

### 5.2 Observations
- SCC compression significantly reduces computational cost for dense graphs.
- Kahn’s algorithm provides consistent linear-time ordering after condensation.
- Longest path (critical) scales linearly with DAG size.
- Graph density affects DFS and relaxation counters more than vertex count.

---

## 6. Analysis and Discussion

| Aspect | Observation |
|---------|-------------|
| SCC / Condensation | Tarjan’s method scales well even on large graphs. SCC sizes reflect cyclic density. |
| Topological Sorting | Kahn’s approach is stable and easily instrumented. Edge checks scale with edge count. |
| DAG Shortest/Longest | DP over topo order achieves O(V + E) performance. Negative weights are unnecessary since graphs are acyclic. |
| Bottlenecks | Parsing and serialization of large JSON datasets dominate runtime, not algorithmic cost. |

---

## 7. Conclusions
- For cyclic graphs, SCC detection followed by condensation is essential before any scheduling or DP analysis.
- For large DAGs, DP-based shortest and longest path computations are efficient and reliable.
- Edge-based weight models provide intuitive control over dependency costs.
- Instrumentation confirms near-linear complexity across all modules.

---

## 8. References
- R. Tarjan, “Depth-First Search and Linear Graph Algorithms,” SIAM J. Comput., 1972.
- Topological Sort (Kahn, 1962).
- Dynamic Programming on DAGs (CLRS, 3rd Edition).

---

## 9. Author and Repository
Author: Aida Bekpayeva
Project: Smart City Scheduler  
Language: Java 17  
Structure: Modular packages with metrics and tests  
Location: /src/app/SmartCityScheduler.java  
