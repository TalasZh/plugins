package org.safehaus.subutai.plugin.lucene.impl;


import org.safehaus.subutai.common.exception.ClusterSetupException;
import org.safehaus.subutai.common.protocol.ClusterSetupStrategy;
import org.safehaus.subutai.common.tracker.TrackerOperation;
import org.safehaus.subutai.plugin.lucene.api.LuceneConfig;
import org.safehaus.subutai.plugin.lucene.api.SetupType;


abstract class LuceneSetupStrategy implements ClusterSetupStrategy
{

    final LuceneImpl manager;
    final LuceneConfig config;
    final TrackerOperation trackerOperation;


    public LuceneSetupStrategy( LuceneImpl manager, LuceneConfig config, TrackerOperation po )
    {
        this.manager = manager;
        this.config = config;
        this.trackerOperation = po;
    }


    void checkConfig() throws ClusterSetupException
    {
        String m = "Invalid configuration: ";

        if ( config.getClusterName() == null || config.getClusterName().isEmpty() )
        {
            throw new ClusterSetupException( m + "Cluster name not specified" );
        }

        if ( manager.getCluster( config.getClusterName() ) != null )
        {
            throw new ClusterSetupException(
                    m + String.format( "Cluster '%s' already exists", config.getClusterName() ) );
        }

        if ( config.getSetupType() == SetupType.OVER_HADOOP )
        {
            if ( config.getNodes() == null || config.getNodes().isEmpty() )
            {
                throw new ClusterSetupException( m + "Target nodes not specified" );
            }
        }
    }
}
