package com.konfuse.topology;

import java.util.*;

/**
 * Directed graph providing a basic routing topology.
 *
 * @param <E> {@link AbstractLink} type of the graph.
 *
 * @Author: Konfuse
 * @Date: 2019/12/31 16:10
 */
public class Graph <E extends AbstractLink<E>>{
    private final HashMap<Long, E> edges = new HashMap<>();
    private final HashMap<Long, E> nodesOut = new HashMap<>();
    private final HashMap<Long, E> nodesIn = new HashMap<>();
    private final HashMap<Long, Integer> nodesDegree = new HashMap<>();

    /**
     * Adds an {@link AbstractLink} to the graph. (Requires construction.)
     *
     * @param edge Edge to be added.
     * @return Returns a self reference to this graph.
     */
    public Graph<E> add(E edge) {
        edges.put(edge.id(), edge);
        return this;
    }

    /**
     * Removes an {@link AbstractLink} from the graph. (Requires construction.)
     *
     * @param edge Edge to be removed.
     */
    public void remove(E edge) {
        edges.remove(edge.id());
    }

    /**
     * Gets {@link AbstractLink} by its identifier.
     *
     * @param id {@link AbstractLink}'s identifier.
     * @return {@link AbstractLink} object if it is contained in the graph, otherwise returns null.
     */
    public E get(long id) {
        return edges.get(id);
    }

    /**
     * Gets the size of the graph, i.e. the number of edges.
     *
     * @return Size of the graph, i.e. the number of edges.
     */
    public int size() {
        return edges.size();
    }

    /**
     * Gets an iterator over all edges of the graph.
     *
     * @return Iterator over all edges of the graph.
     */
    public Iterator<E> edges() {
        return edges.values().iterator();
    }

    /**
     * Constructs the graph which means edges are connected for iteration between connections.
     *
     * @return Returns a self reference to this graph.
     */
    public Graph<E> construct() {
        Map<Long, ArrayList<E>> mapOut = new HashMap<>();
        Map<Long, ArrayList<E>> mapIn = new HashMap<>();

        for (E edge : edges.values()) {
            if (!mapOut.containsKey(edge.source())) {
                mapOut.put(edge.source(), new ArrayList<>(Arrays.asList(edge)));
            } else {
                mapOut.get(edge.source()).add(edge);
            }

            if (!mapIn.containsKey(edge.target())) {
                mapIn.put(edge.target(), new ArrayList<>(Arrays.asList(edge)));
            } else {
                mapIn.get(edge.target()).add(edge);
            }
        }

        for (ArrayList<E> edges : mapOut.values()) {
            for (int i = 1; i < edges.size(); ++i) {
                edges.get(i - 1).neighbor(edges.get(i));
                ArrayList<E> successors = mapOut.get(edges.get(i - 1).target());
                edges.get(i - 1).successor(successors != null ? successors.get(0) : null);
            }

            edges.get(edges.size() - 1).neighbor(edges.get(0));
            ArrayList<E> successors = mapOut.get(edges.get(edges.size() - 1).target());
            edges.get(edges.size() - 1).successor(successors != null ? successors.get(0) : null);
        }

        for (ArrayList<E> edges : mapIn.values()) {
            for (int i = 1; i < edges.size(); ++i) {
                edges.get(i - 1).preneighbor(edges.get(i));
                ArrayList<E> predecessors = mapIn.get(edges.get(i - 1).source());
                edges.get(i - 1).predecessor(predecessors != null ? predecessors.get(0) : null);
            }

            edges.get(edges.size() - 1).preneighbor(edges.get(0));
            ArrayList<E> predecessors = mapIn.get(edges.get(edges.size() - 1).source());
            edges.get(edges.size() - 1).predecessor(predecessors != null ? predecessors.get(0) : null);
        }

        for (E edge : edges.values()) {
            if (!nodesOut.containsKey(edge.source())) {
                Iterator<E> itr = edge.neighbors();
                nodesOut.put(edge.source(), itr.next());
            }

            if (!nodesOut.containsKey(edge.target())) {
                Iterator<E> itr = edge.successors();
                nodesOut.put(edge.target(), itr.next());
            }


            if (!nodesIn.containsKey(edge.target())) {
                Iterator<E> itr = edge.preneighbors();
                nodesIn.put(edge.target(), itr.next());
            }

            if (!nodesIn.containsKey(edge.source())) {
                Iterator<E> itr = edge.predecessors();
                nodesIn.put(edge.source(), itr.next());
            }
        }

        for (Long id : nodesOut.keySet()) {
            ArrayList<E> out = mapOut.get(id);
            int degree_out = out != null ? out.size() : 0;

            ArrayList<E> in = mapIn.get(id);
            int degree_in = in != null ? in.size() : 0;

            nodesDegree.put(id, degree_in + degree_out);
        }

        return this;
    }

    /**
     * Discards the network topology (used for reconstruction of the network topology).
     */
    public void deconstruct() {
        for (E edge : edges.values()) {
            edge.successor(null);
            edge.neighbor(null);
        }
    }

    public HashMap<Long, E> getEdges() {
        return edges;
    }

    public HashMap<Long, E> getNodesOut() {
        return nodesOut;
    }

    public HashMap<Long, E> getNodesIn() {
        return nodesIn;
    }

    public HashMap<Long, Integer> getNodesDegree() {
        return nodesDegree;
    }

    /**
     * Gets the set of (weakly) connected components of the graph. (A weakly connected component is
     * the set of edges that is connected where directed edges are assumed to be undirected.)
     *
     * @return Set of (weakly) connected components.
     */
    public Set<Set<E>> components() {
        Set<E> unvisited = new HashSet<>(edges.values());
        Map<E, Integer> visited = new HashMap<>();
        Map<Integer, Set<E>> components = new HashMap<>();
        Queue<E> queue = new LinkedList<>();

        int componentcounter = 0;

        while (!unvisited.isEmpty()) {
            Iterator<E> it = unvisited.iterator();
            E edge = it.next();
            it.remove();

            queue.add(edge);

            Set<E> buffer = new HashSet<>();
            int componentid = componentcounter++;

            while (!queue.isEmpty()) {
                edge = queue.poll();
                buffer.add(edge);

                if (visited.containsKey(edge.neighbor())) {
                    componentid = visited.get(edge.neighbor());
                    Set<E> component = components.get(componentid);
                    component.addAll(buffer);
                    buffer = component;
                } else if (unvisited.contains(edge.neighbor())) {
                    unvisited.remove(edge.neighbor());
                    queue.add(edge.neighbor());
                }

                Iterator<E> successors = edge.successors();
                while (successors.hasNext()) {
                    E successor = successors.next();
                    if (visited.containsKey(successor)) {
                        componentid = visited.get(successor);
                        Set<E> component = components.get(componentid);
                        component.addAll(buffer);
                        buffer = component;
                    } else if (unvisited.contains(successor)) {
                        unvisited.remove(successor);
                        queue.add(successor);
                    }
                }
            }

            for (E member : buffer) {
                visited.put(member, componentid);
            }

            if (!components.containsKey(componentid)) {
                components.put(componentid, buffer);
            }
        }
        return new HashSet<>(components.values());
    }
}
