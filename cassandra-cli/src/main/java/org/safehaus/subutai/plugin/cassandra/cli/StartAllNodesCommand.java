package org.safehaus.subutai.plugin.cassandra.cli;


import java.io.IOException;
import java.util.UUID;

import org.safehaus.subutai.common.tracker.OperationState;
import org.safehaus.subutai.common.tracker.TrackerOperationView;
import org.safehaus.subutai.core.tracker.api.Tracker;
import org.safehaus.subutai.plugin.cassandra.api.Cassandra;
import org.safehaus.subutai.plugin.cassandra.api.CassandraClusterConfig;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;


/**
 * Displays the last log entries
 */
@Command(scope = "cassandra", name = "stop-cluster", description = "Command to start Cassandra cluster")
public class StartAllNodesCommand extends OsgiCommandSupport
{

    @Argument(index = 0, name = "clusterName", description = "The name of the cluster.", required = true,
            multiValued = false)
    String clusterName = null;
    private Cassandra cassandraManager;
    private Tracker tracker;


    public Cassandra getCassandraManager()
    {
        return cassandraManager;
    }


    public void setCassandraManager( Cassandra cassandraManager )
    {
        this.cassandraManager = cassandraManager;
    }


    public Tracker getTracker()
    {
        return tracker;
    }


    public void setTracker( Tracker tracker )
    {
        this.tracker = tracker;
    }


    protected Object doExecute() throws IOException
    {

        UUID uuid = cassandraManager.startCluster( clusterName );
        int logSize = 0;
        while ( !Thread.interrupted() )
        {
            TrackerOperationView po = tracker.getTrackerOperation( CassandraClusterConfig.PRODUCT_KEY, uuid );
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
}
