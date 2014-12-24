package org.safehaus.subutai.plugin.mahout.impl;


import java.util.Iterator;

import org.safehaus.subutai.common.exception.ClusterSetupException;
import org.safehaus.subutai.common.protocol.Agent;
import org.safehaus.subutai.common.protocol.ConfigBase;
import org.safehaus.subutai.common.tracker.TrackerOperation;
import org.safehaus.subutai.common.util.CollectionUtil;
import org.safehaus.subutai.core.command.api.command.AgentResult;
import org.safehaus.subutai.core.command.api.command.Command;
import org.safehaus.subutai.plugin.mahout.api.MahoutClusterConfig;

import com.google.common.base.Strings;


class OverHadoopSetupStrategy extends MahoutSetupStrategy
{

    public OverHadoopSetupStrategy( MahoutImpl manager, MahoutClusterConfig config, TrackerOperation po )
    {
        super( manager, config, po );
    }


    @Override
    public ConfigBase setup() throws ClusterSetupException
    {

        if ( Strings.isNullOrEmpty( config.getHadoopClusterName() ) || CollectionUtil
                .isCollectionEmpty( config.getNodes() ) )
        {
            throw new ClusterSetupException( "Malformed configuration\nInstallation aborted" );
        }

        if ( manager.getCluster( config.getClusterName() ) != null )
        {
            throw new ClusterSetupException(
                    String.format( "Cluster with name '%s' already exists\nInstallation aborted",
                            config.getClusterName() ) );
        }

        // Check if node agent is connected
        for ( Iterator<Agent> it = config.getNodes().iterator(); it.hasNext(); )
        {
            Agent node = it.next();
            if ( manager.getAgentManager().getAgentByHostname( node.getHostname() ) == null )
            {
                trackerOperation.addLog(
                        String.format( "Node %s is not connected. Omitting this node from installation",
                                node.getHostname() ) );
                it.remove();
            }
        }

        if ( config.getNodes().isEmpty() )
        {
            throw new ClusterSetupException( "No nodes eligible for installation. Operation aborted" );
        }

        trackerOperation.addLog( "Checking prerequisites..." );

        // Check installed packages

        Command checkInstalledCommand = manager.getCommands().getCheckInstalledCommand( config.getNodes() );
        manager.getCommandRunner().runCommand( checkInstalledCommand );

        if ( !checkInstalledCommand.hasCompleted() )
        {
            throw new ClusterSetupException( "Failed to check presence of installed packages\nInstallation aborted" );
        }

        for ( Iterator<Agent> it = config.getNodes().iterator(); it.hasNext(); )
        {
            Agent node = it.next();
            AgentResult result = checkInstalledCommand.getResults().get( node.getUuid() );

            if ( result.getStdOut().contains( MahoutClusterConfig.PRODUCT_PACKAGE ) )
            {
                trackerOperation.addLog(
                        String.format( "Node %s already has Mahout installed. Omitting this node from installation",
                                node.getHostname() ) );
                it.remove();
            }
        }

        if ( config.getNodes().isEmpty() )
        {
            throw new ClusterSetupException( "No nodes eligible for installation. Operation aborted" );
        }

        trackerOperation.addLog( "Updating db..." );

        // Save to db
        manager.getPluginDAO().saveInfo( MahoutClusterConfig.PRODUCT_KEY, config.getClusterName(), config );

        Command installCommand = manager.getCommands().getInstallCommand( config.getNodes() );
        manager.getCommandRunner().runCommand( installCommand );
        if ( installCommand.hasSucceeded() )
        {
            trackerOperation.addLog( "Installation succeeded" );
        }
        else
        {
            trackerOperation.addLogFailed( String.format( "Installation failed, %s", installCommand.getAllErrors() ) );
        }

        return config;
    }
}