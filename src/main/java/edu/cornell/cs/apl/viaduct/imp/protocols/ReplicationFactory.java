package edu.cornell.cs.apl.viaduct.imp.protocols;

import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.HostName;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.pdg.PdgNode;
import edu.cornell.cs.apl.viaduct.protocol.PowersetProtocolFactory;
import edu.cornell.cs.apl.viaduct.protocol.Protocol;
import java.util.Set;

public class ReplicationFactory extends PowersetProtocolFactory<ImpAstNode> {
  @Override
  protected Protocol<ImpAstNode> createInstanceFromHostInfo(
      PdgNode<ImpAstNode> node,
      HostTrustConfiguration hostConfig,
      Set<HostName> hostSet)
  {
    return hostSet.size() >= 2 ? new Replication(hostConfig, hostSet) : null;
  }
}
