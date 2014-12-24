/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.safehaus.subutai.plugin.mahout.api;


import java.util.UUID;

import org.safehaus.subutai.common.protocol.ApiBase;
import org.safehaus.subutai.common.protocol.ClusterSetupStrategy;
import org.safehaus.subutai.common.protocol.EnvironmentBuildTask;
import org.safehaus.subutai.common.tracker.TrackerOperation;
import org.safehaus.subutai.core.environment.api.helper.Environment;


public interface Mahout extends ApiBase<MahoutClusterConfig>
{

    UUID addNode( String clusterName, String lxcHostname );

    UUID destroyNode( String clusterName, String lxcHostname );

    UUID checkNode( String clustername, String lxchostname );

    UUID stopCluster( String clusterName );

    UUID startCluster( String clusterName );

    ClusterSetupStrategy getClusterSetupStrategy( Environment environment, MahoutClusterConfig config,
                                                  TrackerOperation po );

    EnvironmentBuildTask getDefaultEnvironmentBlueprint( MahoutClusterConfig config );
}
