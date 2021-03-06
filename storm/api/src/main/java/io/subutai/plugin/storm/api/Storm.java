package io.subutai.plugin.storm.api;


import java.util.UUID;

import io.subutai.common.tracker.TrackerOperation;
import io.subutai.core.plugincommon.api.ApiBase;
import io.subutai.core.plugincommon.api.ClusterException;
import io.subutai.core.plugincommon.api.ClusterSetupStrategy;


public interface Storm extends ApiBase<StormClusterConfiguration>
{

    public UUID startNode( String clusterName, String hostname );

    public UUID startAll( String clusterName );

    public UUID checkAll( String clusterName );

    public UUID stopNode( String clusterName, String hostname );

    public UUID stopAll( String clusterName );

    public UUID checkNode( String clusterName, String hostname );

    public UUID restartNode( String clusterName, String hostname );

    /**
     * Adds a node to specified cluster.
     *
     * @param clusterName the name of cluster to which a node is added
     *
     * @return operation id to track
     */
    public UUID addNode( String clusterName );

    public UUID destroyNode( String clusterName, String hostname );

    public UUID removeCluster( String clusterName );

    public ClusterSetupStrategy getClusterSetupStrategy( StormClusterConfiguration config, TrackerOperation po );

    /**
     * Saves/Updates cluster config in database
     *
     * @param config - config to update
     */
    public void saveConfig( StormClusterConfiguration config ) throws ClusterException;

    public void deleteConfig( StormClusterConfiguration config ) throws ClusterException;
}
