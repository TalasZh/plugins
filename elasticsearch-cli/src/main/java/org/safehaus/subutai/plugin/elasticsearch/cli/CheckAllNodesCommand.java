package org.safehaus.subutai.plugin.elasticsearch.cli;


import java.io.IOException;
import java.util.UUID;

import org.safehaus.subutai.common.tracker.OperationState;
import org.safehaus.subutai.common.tracker.TrackerOperationView;
import org.safehaus.subutai.core.tracker.api.Tracker;
import org.safehaus.subutai.plugin.elasticsearch.api.Elasticsearch;
import org.safehaus.subutai.plugin.elasticsearch.api.ElasticsearchClusterConfiguration;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;



@Command(scope = "elasticsearch", name = "check-cluster", description = "Command to check Elasticsearch cluster")
public class CheckAllNodesCommand extends OsgiCommandSupport
{

    @Argument(index = 0, name = "clusterName", description = "The name of the cluster.", required = true,
            multiValued = false)
    String clusterName = null;
    private Elasticsearch elasticsearchManager;
    private Tracker tracker;

    protected Object doExecute() throws IOException
    {

        // TODO check cluster
        UUID uuid = elasticsearchManager.checkNode( clusterName, "test" );
        int logSize = 0;
        while ( !Thread.interrupted() )
        {
            TrackerOperationView po = tracker.getTrackerOperation( ElasticsearchClusterConfiguration.PRODUCT_KEY, uuid );
            if ( po != null )
            {
                if ( logSize != po.getLog().length() )
                {
                    System.out.print( po.getLog().substring( logSize, po.getLog().length() ) );
                    System.out.flush();
                    logSize = po.getLog().length();
                }
                if ( po.getState() != OperationState.RUNNING )
                {
                    break;
                }
            }
            else
            {
                System.out.println( "Product operation not found. Check logs" );
                break;
            }
            try
            {
                Thread.sleep( 1000 );
            }
            catch ( InterruptedException ex )
            {
                break;
            }
        }
        return null;
    }


    public Elasticsearch getElasticsearchManager()
    {
        return elasticsearchManager;
    }


    public void setElasticsearchManager( final Elasticsearch elasticsearchManager )
    {
        this.elasticsearchManager = elasticsearchManager;
    }


    public Tracker getTracker()
    {
        return tracker;
    }


    public void setTracker( Tracker tracker )
    {
        this.tracker = tracker;
    }

}
