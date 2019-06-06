package edu.cornell.cs.apl.viaduct.imp.protocols;

import edu.cornell.cs.apl.viaduct.PdgInfoEdge;
import edu.cornell.cs.apl.viaduct.PdgNode;
import edu.cornell.cs.apl.viaduct.Protocol;
import edu.cornell.cs.apl.viaduct.ProtocolFactory;
import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.Host;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.security.Label;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** contains ZK information flow constraints. */
public class ZKFactory implements ProtocolFactory<ImpAstNode> {
  @Override
  public Set<Protocol<ImpAstNode>> createInstances(
      HostTrustConfiguration hostConfig,
      Map<PdgNode<ImpAstNode>, Protocol<ImpAstNode>> protocolMap,
      PdgNode<ImpAstNode> node) {

    if (hostConfig.size() < 2) {
      return new HashSet<>();
    }

    // assume for now that there are only two hosts
    // generalize this later
    Host[] hostPair = new Host[2];
    // TODO: WTF does this do?
    // hostConfig.toArray(hostPair);
    int i = 0;
    for (Host host : hostConfig.hosts()) {
      hostPair[i] = host;
      i++;
      if (i == hostPair.length) {
        break;
      }
    }

    Set<PdgNode<ImpAstNode>> inNodes = new HashSet<>();
    for (PdgInfoEdge<ImpAstNode> edge : node.getInInfoEdges()) {
      PdgNode<ImpAstNode> source = edge.getSource();
      if (!source.isControlNode()) {
        inNodes.add(source);
      }
    }
    Host hostA = hostPair[0];
    Host hostB = hostPair[1];
    Label nInLabel = node.getInLabel();
    Label nOutLabel = node.getOutLabel();
    Label aLabel = hostConfig.getTrust(hostA);
    Label bLabel = hostConfig.getTrust(hostB);

    Set<Protocol<ImpAstNode>> instances = new HashSet<>();

    // prover: A, verifier: B
    if (inNodes.size() == 1 && node.isDeclassifyNode()) {
      PdgNode<ImpAstNode> inNode = (PdgNode<ImpAstNode>) inNodes.toArray()[0];
      Protocol<ImpAstNode> inProto = protocolMap.get(inNode);
      if (inProto instanceof Replication) {
        Replication inReplProto = (Replication) inProto;

        if (nInLabel.confidentiality().flowsTo(aLabel.confidentiality())
            && !nInLabel.confidentiality().flowsTo(bLabel.confidentiality())
            && nOutLabel.confidentiality().flowsTo(bLabel.confidentiality())
            // && bLabel.integrity().flowsTo(nOutLabel.integrity())
            && inReplProto.getRealReplicas().contains(hostA)
            && inReplProto.getHashReplicas().contains(hostB)) {

          instances.add(new ZK(hostA, hostB));
        }

        if (nInLabel.confidentiality().flowsTo(bLabel.confidentiality())
            && !nInLabel.confidentiality().flowsTo(aLabel.confidentiality())
            && nOutLabel.confidentiality().flowsTo(aLabel.confidentiality())
            // && aLabel.integrity().flowsTo(nOutLabel.integrity())
            && inReplProto.getRealReplicas().contains(hostB)
            && inReplProto.getHashReplicas().contains(hostA)) {

          instances.add(new ZK(hostB, hostA));
        }
      }
    }

    return instances;
  }
}