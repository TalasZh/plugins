package org.safehaus.subutai.plugin.mahout.impl.handler;


import org.safehaus.subutai.common.protocol.AbstractOperationHandler;
import org.safehaus.subutai.common.protocol.Agent;
import org.safehaus.subutai.common.tracker.TrackerOperation;
import org.safehaus.subutai.core.command.api.command.Command;
import org.safehaus.subutai.core.container.api.lxcmanager.LxcDestroyException;
import org.safehaus.subutai.plugin.mahout.api.MahoutClusterConfig;
import org.safehaus.subutai.plugin.mahout.api.SetupType;
import org.safehaus.subutai.plugin.mahout.impl.MahoutImpl;

import com.google.common.collect.Sets;


public class DestroyNodeHandler extends AbstractOperationHandler<MahoutImpl>
{
    private final String lxcHostname;


    public DestroyNodeHandler( MahoutImpl manager, String clusterName, String lxcHostname )
    {
        super( manager, clusterName );
        this.lxcHostname = lxcHostname;
        trackerOperation = manager.getTracker().createTrackerOperation( MahoutClusterConfig.PRODUCT_KEY,
                String.format( "Destroying %s in %s", lxcHostname, clusterName ) );
    }


    @Override
    public void run()
    {
        TrackerOperation po = trackerOperation;
        MahoutClusterConfig config = manager.getCluster( clusterName );
        if ( config == null )
        {
            po.addLogFailed( String.format( "Cluster with name %s does not exist\nOperation aborted", clusterName ) );
            return;
        }

        Agent agent = manager.getAgentManager().getAgentByHostname( lxcHostname );
        if ( agent == null )
        {
            po.addLogFailed(
                    String.format( "Agent with hostname %s is not connected\nOperation aborted", lxcHostname ) );
            return;
        }

        if ( config.getNodes().size() == 1 )
        {
            po.addLogFailed(
                    "This is the last slave node in the cluster. Please, destroy cluster instead\nOperation aborted" );
            return;
        }

        //check if node is in the cluster
        if ( !config.getNodes().contains( agent ) )
        {
            po.addLogFailed( String.format( "Node %s does not belong to this cluster\nOperation aborted",
                    agent.getHostname() ) );
            return;
        }

        boolean ok = false;
        if ( config.getSetupType() == SetupType.OVER_HADOOP )
        {
            ok = uninstall( agent );
        }
        else if ( config.getSetupType() == SetupType.WITH_HADOOP )
        {
            ok = destroyNode( agent );
        }
        else
        {
            po.addLog( "Undefined setup type" );
        }

        if ( ok )
        {
            config.getNodes().remove( agent );
            po.addLog( "Updating db..." );

            manager.getPluginDAO().saveInfo( MahoutClusterConfig.PRODUCT_KEY, config.getClusterName(), config );
            po.addLogDone( "Cluster info updated in DB\nDone" );
        }
        else
        {
            po.addLogFailed( "Failed to destroy node" );
        }
    }


    private boolean uninstall( Agent agent )
    {
        TrackerOperation po = trackerOperation;
        po.addLog( "Uninstalling Mahout..." );

        Command cmd = manager.getCommands().getUninstallCommand( Sets.newHashSet( agent ) );
        manager.getCommandRunner().runCommand( cmd );

        if ( cmd.hasSucceeded() )
        {
            po.addLog( "Mahout removed from " + agent.getHostname() );
            return true;
        }
        else
        {
            po.addLog( "Uninstallation failed: " + cmd.getAllErrors() );
            return false;
        }
    }


    private boolean destroyNode( Agent agent )
    {
        try
        {
            manager.getContainerManager().cloneDestroy( agent.getParentHostName(), agent.getHostname() );
            return true;
        }
        catch ( LxcDestroyException ex )
        {
            trackerOperation.addLog( "Failed to destroy node: " + ex.getMessage() );
            return false;
        }
    }
}