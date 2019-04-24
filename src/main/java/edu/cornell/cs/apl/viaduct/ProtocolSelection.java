package edu.cornell.cs.apl.viaduct;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

public class ProtocolSelection<T extends AstNode> {
  /** represents a node in the search space. */
  private static class ProtocolMapNode<U extends AstNode> {
    final HashMap<PdgNode<U>,Protocol<U>> protocolMap;
    final int cost;

    ProtocolMapNode(HashMap<PdgNode<U>,Protocol<U>> pmap, int cost) {
      this.protocolMap = pmap;
      this.cost = cost;
    }

    HashMap<PdgNode<U>,Protocol<U>> getProtocolMap() {
      return this.protocolMap;
    }

    int getCost() {
      return this.cost;
    }

    @Override
    public boolean equals(Object o) {
      if (o == null) { return false; }

      if (o instanceof ProtocolMapNode<?>) {
        ProtocolMapNode<U> onode = (ProtocolMapNode<U>)o;
        return this.protocolMap.equals(onode.protocolMap);

      } else {
        return false;
      }
    }

    @Override
    public int hashCode() {
      return this.protocolMap.hashCode();
    }

    @Override
    public String toString() {
      StringBuffer str = new StringBuffer();
      for (Map.Entry<PdgNode<U>,Protocol<U>> kv : protocolMap.entrySet()) {
        str.append(String.format("%s => %s%n", kv.getKey().toString(), kv.getValue().toString()));
      }
      return str.toString();
    }
  }

  private class ProtocolMapComparator implements Comparator<ProtocolMapNode<T>> {
    public int compare(ProtocolMapNode<T> protoMap1, ProtocolMapNode<T> protoMap2) {
      return protoMap1.getCost() - protoMap2.getCost();
    }
  }

  ProtocolCostEstimator<T> costEstimator;

  public ProtocolSelection(ProtocolCostEstimator<T> estimator) {
    this.costEstimator = estimator;
  }

  /** return a mapping from PDG nodes to protocols.
   *  this uses A* search to find the cheapest protocol selection
   */
  public Map<PdgNode<T>,Protocol<T>> selectProtocols(
        Set<Host> hostConfig, ProgramDependencyGraph<T> pdg)
  {
    Set<PdgNode<T>> nodes = pdg.getNodes();
    Set<Protocol<T>> protocols = costEstimator.getProtocols();

    // the sequence in which PDG nodes will have a protocol selected.
    // this allows massive pruning of the search space,
    // as we will only visit maps that follow this selection order
    // obeys toposort according to PDG
    List<PdgNode<T>> selectionOrder = new ArrayList<>();
    while (selectionOrder.size() < nodes.size()) {
      for (PdgNode<T> node : nodes) {
        if (!selectionOrder.contains(node)) {
          if (node.isStorageNode()) {
            selectionOrder.add(node);
            break;
          } else {
            Set<PdgNode<T>> inNodes = node.getInNodes();
            for (PdgNode<T> inNode : inNodes) {
              if (!selectionOrder.contains(inNode)) {
                break;
              }
            }
            selectionOrder.add(node);
          }
        }
      }
    }

    // create open and closed sets
    ProtocolMapComparator comparator = new ProtocolMapComparator();
    PriorityQueue<ProtocolMapNode<T>> openSet =
        new PriorityQueue<>(pdg.getNodes().size(), comparator);
    HashSet<ProtocolMapNode<T>> closedSet = new HashSet<>();

    // start node is empty map
    HashMap<PdgNode<T>,Protocol<T>> initMap = new HashMap<>();
    openSet.add(new ProtocolMapNode<T>(initMap, 0));

    // explore nodes in open set until we find a goal node
    while (!openSet.isEmpty()) {
      ProtocolMapNode<T> currMapNode = openSet.remove();
      HashMap<PdgNode<T>,Protocol<T>> currMap = currMapNode.getProtocolMap();
      Set<PdgNode<T>> mappedNodes = currMap.keySet();

      // check if the current map is a goal node
      // (i.e. it has a mapping for all nodes in the PDG)
      if (mappedNodes.size() == nodes.size()) {
        return currMap;
      }

      closedSet.add(currMapNode);

      // get the next node to select a protocol for,
      // according to the selection order
      PdgNode<T> nextNode = null;
      for (PdgNode<T> node : selectionOrder) {
        if (!mappedNodes.contains(node)) {
          nextNode = node;
        }
      }
      // after this point, nextNode cannot be null!
      // otherwise, that means that all PDG nodes have been mapped
      // -- but then that means we found a goal, so we should already
      // have returned

      // visit neighbors from edges, where edge set is set of protocol instantiations
      // for each protocol, generate a set of possible instantiated protocols
      // each instantiated protocol represents an edge from the current map
      // to a new map with one new mapping from the PDG node to the instantiated protocol
      for (Protocol<T> protocol : protocols) {
        Set<Protocol<T>> protoInstances =  protocol.createInstances(hostConfig, currMap, nextNode);
        for (Protocol<T> protoInstance : protoInstances) {
          // instantiate neighbor
          HashMap<PdgNode<T>,Protocol<T>> newMap =
              (HashMap<PdgNode<T>,Protocol<T>>)currMap.clone();
          newMap.put(nextNode, protoInstance);
          int newMapCost = this.costEstimator.estimatePdgCost(newMap, pdg);
          ProtocolMapNode<T> newMapNode = new ProtocolMapNode<>(newMap, newMapCost);

          if (!closedSet.contains(newMapNode) && !openSet.contains(newMapNode)) {
            // System.out.println(newMapNode);
            openSet.add(newMapNode);
          }
        }
      }
    }

    // no mapping found. should be impossible, unless available protocols + host config are bad!
    return null;
  }
}
