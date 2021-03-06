package io.subutai.plugin.accumulo.impl;


import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import io.subutai.common.environment.Environment;
import io.subutai.common.mdc.SubutaiExecutors;
import io.subutai.common.peer.EnvironmentContainerHost;
import io.subutai.core.environment.api.EnvironmentEventListener;
import io.subutai.core.environment.api.EnvironmentManager;
import io.subutai.core.lxc.quota.api.QuotaManager;
import io.subutai.core.metric.api.Monitor;
import io.subutai.core.metric.api.MonitorException;
import io.subutai.core.metric.api.MonitoringSettings;
import io.subutai.core.tracker.api.Tracker;
import io.subutai.plugin.accumulo.api.Accumulo;
import io.subutai.plugin.accumulo.api.AccumuloClusterConfig;
import io.subutai.plugin.accumulo.impl.alert.AccumuloAlertListener;
import io.subutai.plugin.accumulo.impl.handler.AddPropertyOperationHandler;
import io.subutai.plugin.accumulo.impl.handler.ClusterOperationHandler;
import io.subutai.plugin.accumulo.impl.handler.NodeOperationHandler;
import io.subutai.plugin.accumulo.impl.handler.RemovePropertyOperationHandler;
import io.subutai.core.plugincommon.api.AbstractOperationHandler;
import io.subutai.core.plugincommon.api.ClusterException;
import io.subutai.core.plugincommon.api.ClusterOperationType;
import io.subutai.core.plugincommon.api.NodeOperationType;
import io.subutai.core.plugincommon.api.NodeType;
import io.subutai.core.plugincommon.api.PluginDAO;
import io.subutai.plugin.hadoop.api.Hadoop;
import io.subutai.plugin.hadoop.api.HadoopClusterConfig;
import io.subutai.plugin.zookeeper.api.Zookeeper;
import io.subutai.plugin.zookeeper.api.ZookeeperClusterConfig;


public class AccumuloImpl implements Accumulo, EnvironmentEventListener
{
    private static final Logger LOG = LoggerFactory.getLogger( AccumuloImpl.class.getName() );
    private final MonitoringSettings alertSettings = new MonitoringSettings().withIntervalBetweenAlertsInMin( 45 );
    protected Commands commands;
    private Tracker tracker;
    private Hadoop hadoopManager;
    private Zookeeper zkManager;
    private EnvironmentManager environmentManager;
    private ExecutorService executor;
    private PluginDAO pluginDAO;
    private Monitor monitor;
    private QuotaManager quotaManager;


    public AccumuloImpl( Monitor monitor, PluginDAO pluginDAO )
    {
        this.monitor = monitor;
        this.pluginDAO = pluginDAO;
    }


    public void init()
    {
        executor = SubutaiExecutors.newCachedThreadPool();
    }


    public void destroy()
    {
        executor.shutdown();
    }


    public UUID installCluster( final AccumuloClusterConfig accumuloClusterConfig )
    {
        Preconditions.checkNotNull( accumuloClusterConfig, "Accumulo cluster configuration is null" );
        HadoopClusterConfig hadoopClusterConfig =
                hadoopManager.getCluster( accumuloClusterConfig.getHadoopClusterName() );
        ZookeeperClusterConfig zookeeperClusterConfig =
                zkManager.getCluster( accumuloClusterConfig.getZookeeperClusterName() );
        AbstractOperationHandler h =
                new ClusterOperationHandler( this, accumuloClusterConfig, hadoopClusterConfig, zookeeperClusterConfig,
                        ClusterOperationType.INSTALL );
        executor.execute( h );
        return h.getTrackerId();
    }


    public UUID uninstallCluster( final String clusterName )
    {
        Preconditions.checkArgument( !Strings.isNullOrEmpty( clusterName ), "Cluster name is null or empty" );
        AccumuloClusterConfig accumuloClusterConfig = getCluster( clusterName );
        HadoopClusterConfig hadoopClusterConfig =
                hadoopManager.getCluster( accumuloClusterConfig.getHadoopClusterName() );
        ZookeeperClusterConfig zookeeperClusterConfig =
                zkManager.getCluster( accumuloClusterConfig.getZookeeperClusterName() );
        AbstractOperationHandler operationHandler =
                new ClusterOperationHandler( this, accumuloClusterConfig, hadoopClusterConfig, zookeeperClusterConfig,
                        ClusterOperationType.UNINSTALL );
        executor.execute( operationHandler );
        return operationHandler.getTrackerId();
    }


    @Override
    public List<AccumuloClusterConfig> getClusters()
    {
        return pluginDAO.getInfo( AccumuloClusterConfig.PRODUCT_KEY, AccumuloClusterConfig.class );
    }


    @Override
    public AccumuloClusterConfig getCluster( String clusterName )
    {
        Preconditions.checkArgument( !Strings.isNullOrEmpty( clusterName ), "Cluster name is null or empty" );
        return pluginDAO.getInfo( AccumuloClusterConfig.PRODUCT_KEY, clusterName, AccumuloClusterConfig.class );
    }


    @Override
    public UUID addNode( final String clusterName, final String agentHostName )
    {
        return null;
    }


    public UUID startCluster( final String clusterName )
    {
        Preconditions.checkArgument( !Strings.isNullOrEmpty( clusterName ), "Cluster name is null or empty" );
        AccumuloClusterConfig accumuloClusterConfig = getCluster( clusterName );
        HadoopClusterConfig hadoopClusterConfig =
                hadoopManager.getCluster( accumuloClusterConfig.getHadoopClusterName() );
        ZookeeperClusterConfig zookeeperClusterConfig =
                zkManager.getCluster( accumuloClusterConfig.getZookeeperClusterName() );
        AbstractOperationHandler operationHandler =
                new ClusterOperationHandler( this, accumuloClusterConfig, hadoopClusterConfig, zookeeperClusterConfig,
                        ClusterOperationType.START_ALL );
        executor.execute( operationHandler );
        return operationHandler.getTrackerId();
    }


    public UUID stopCluster( final String clusterName )
    {
        Preconditions.checkArgument( !Strings.isNullOrEmpty( clusterName ), "Cluster name is null or empty" );
        AccumuloClusterConfig accumuloClusterConfig = getCluster( clusterName );
        HadoopClusterConfig hadoopClusterConfig =
                hadoopManager.getCluster( accumuloClusterConfig.getHadoopClusterName() );
        ZookeeperClusterConfig zookeeperClusterConfig =
                zkManager.getCluster( accumuloClusterConfig.getZookeeperClusterName() );
        AbstractOperationHandler operationHandler =
                new ClusterOperationHandler( this, accumuloClusterConfig, hadoopClusterConfig, zookeeperClusterConfig,
                        ClusterOperationType.STOP_ALL );
        executor.execute( operationHandler );
        return operationHandler.getTrackerId();
    }


    @Override
    public UUID checkCluster( final String clusterName )
    {
        Preconditions.checkArgument( !Strings.isNullOrEmpty( clusterName ), "Cluster name is null or empty" );
        AccumuloClusterConfig accumuloClusterConfig = getCluster( clusterName );
        HadoopClusterConfig hadoopClusterConfig =
                hadoopManager.getCluster( accumuloClusterConfig.getHadoopClusterName() );
        ZookeeperClusterConfig zookeeperClusterConfig =
                zkManager.getCluster( accumuloClusterConfig.getZookeeperClusterName() );
        AbstractOperationHandler operationHandler =
                new ClusterOperationHandler( this, accumuloClusterConfig, hadoopClusterConfig, zookeeperClusterConfig,
                        ClusterOperationType.STATUS_ALL );
        executor.execute( operationHandler );
        return operationHandler.getTrackerId();
    }


    public UUID checkNode( final String clusterName, final String lxcHostName )
    {
        Preconditions.checkArgument( !Strings.isNullOrEmpty( clusterName ), "Cluster name is null or empty" );
        Preconditions.checkArgument( !Strings.isNullOrEmpty( lxcHostName ), "Lxc hostname is null or empty" );

        AbstractOperationHandler operationHandler =
                new NodeOperationHandler( this, hadoopManager, zkManager, clusterName, lxcHostName,
                        NodeOperationType.STATUS, null );

        executor.execute( operationHandler );

        return operationHandler.getTrackerId();
    }


    public UUID addNode( final String clusterName, final String lxcHostname, final NodeType nodeType )
    {
        Preconditions.checkArgument( !Strings.isNullOrEmpty( clusterName ), "Cluster name is null or empty" );
        Preconditions.checkArgument( !Strings.isNullOrEmpty( lxcHostname ), "Lxc hostname is null or empty" );
        Preconditions.checkNotNull( nodeType, "Node type is null" );

        AbstractOperationHandler operationHandler =
                new NodeOperationHandler( this, hadoopManager, zkManager, clusterName, lxcHostname,
                        NodeOperationType.ADD, nodeType );

        executor.execute( operationHandler );

        return operationHandler.getTrackerId();
    }


    public UUID addNode( final String clusterName, final NodeType nodeType )
    {
        Preconditions.checkArgument( !Strings.isNullOrEmpty( clusterName ), "Cluster name is null or empty" );
        Preconditions.checkNotNull( nodeType, "Node type is null" );

        AbstractOperationHandler operationHandler =
                new NodeOperationHandler( this, hadoopManager, zkManager, clusterName, NodeOperationType.ADD,
                        nodeType );

        executor.execute( operationHandler );

        return operationHandler.getTrackerId();
    }


    public UUID destroyNode( final String clusterName, final String lxcHostName, final NodeType nodeType )
    {
        Preconditions.checkArgument( !Strings.isNullOrEmpty( clusterName ), "Cluster name is null or empty" );
        Preconditions.checkArgument( !Strings.isNullOrEmpty( lxcHostName ), "Lxc hostname is null or empty" );
        Preconditions.checkNotNull( nodeType, "Node type is null" );

        AbstractOperationHandler operationHandler =
                new NodeOperationHandler( this, hadoopManager, zkManager, clusterName, lxcHostName,
                        NodeOperationType.DESTROY, nodeType );

        executor.execute( operationHandler );

        return operationHandler.getTrackerId();
    }


    @Override
    public UUID addProperty( final String clusterName, final String propertyName, final String propertyValue )
    {
        Preconditions.checkArgument( !Strings.isNullOrEmpty( clusterName ), "Cluster name is null or empty" );
        Preconditions.checkArgument( !Strings.isNullOrEmpty( propertyName ), "Property name is null or empty" );
        Preconditions.checkArgument( !Strings.isNullOrEmpty( propertyValue ), "Property value is null or empty" );
        AbstractOperationHandler operationHandler =
                new AddPropertyOperationHandler( this, clusterName, propertyName, propertyValue );
        executor.execute( operationHandler );
        return operationHandler.getTrackerId();
    }


    @Override
    public UUID removeProperty( final String clusterName, final String propertyName )
    {
        Preconditions.checkArgument( !Strings.isNullOrEmpty( clusterName ), "Cluster name is null or empty" );
        Preconditions.checkArgument( !Strings.isNullOrEmpty( propertyName ), "Property name is null or empty" );
        AbstractOperationHandler operationHandler =
                new RemovePropertyOperationHandler( this, clusterName, propertyName );
        executor.execute( operationHandler );
        return operationHandler.getTrackerId();
    }


    @Override
    public void saveConfig( final AccumuloClusterConfig config ) throws ClusterException
    {
        Preconditions.checkNotNull( config );

        if ( !getPluginDAO().saveInfo( AccumuloClusterConfig.PRODUCT_KEY, config.getClusterName(), config ) )
        {
            throw new ClusterException( "Could not save cluster info" );
        }
    }


    public PluginDAO getPluginDAO()
    {
        return pluginDAO;
    }


    public void setPluginDAO( final PluginDAO pluginDAO )
    {
        this.pluginDAO = pluginDAO;
    }


    @Override
    public void deleteConfig( final AccumuloClusterConfig config ) throws ClusterException
    {
        Preconditions.checkNotNull( config );

        if ( !getPluginDAO().deleteInfo( AccumuloClusterConfig.PRODUCT_KEY, config.getClusterName() ) )
        {
            throw new ClusterException( "Could not delete cluster info" );
        }
    }


//    public void subscribeToAlerts( Environment environment ) throws MonitorException
//    {
//        getMonitor().startMonitoring( AccumuloAlertListener.ACCUMOLO_ALERT_LISTENER, environment, alertSettings );
//    }


    public Monitor getMonitor()
    {
        return monitor;
    }


    public void setMonitor( final Monitor monitor )
    {
        this.monitor = monitor;
    }


//    public void subscribeToAlerts( EnvironmentContainerHost host ) throws MonitorException
//    {
//        getMonitor().activateMonitoring( host, alertSettings );
//    }
//
//
//    public void unsubscribeFromAlerts( final Environment environment ) throws MonitorException
//    {
//        getMonitor().stopMonitoring( AccumuloAlertListener.ACCUMOLO_ALERT_LISTENER, environment );
//    }


    public MonitoringSettings getAlertSettings()
    {
        return alertSettings;
    }


    public QuotaManager getQuotaManager()
    {
        return quotaManager;
    }


    public void setQuotaManager( final QuotaManager quotaManager )
    {
        this.quotaManager = quotaManager;
    }


    @Override
    public void onEnvironmentCreated( final Environment environment )
    {
        LOG.info( String.format( "Environment: %s successfully created", environment.getName() ) );
    }


    @Override
    public void onEnvironmentGrown( final Environment environment, final Set<EnvironmentContainerHost> set )
    {
        List<AccumuloClusterConfig> clusterConfigs = new ArrayList<>( getClusters() );
        List<String> hostNames = new ArrayList<>();
        for ( final EnvironmentContainerHost containerHost : set )
        {
            hostNames.add( containerHost.getHostname() );
        }
        for ( final AccumuloClusterConfig clusterConfig : clusterConfigs )
        {
            if ( clusterConfig.getEnvironmentId().equals( environment.getId() ) )
            {
                LOG.info( String.format( "Accumulo %s cluster has been grown with %s hosts",
                        clusterConfig.getClusterName(), hostNames.toString() ) );
            }
        }
    }


    @Override
    public void onContainerDestroyed( final Environment environment, final String nodeId )
    {
        List<AccumuloClusterConfig> clusterConfigs = new ArrayList<>( getClusters() );
        for ( final AccumuloClusterConfig clusterConfig : clusterConfigs )
        {
            if ( clusterConfig.getEnvironmentId().equals( environment.getId() ) )
            {
                if ( clusterConfig.getAllNodes().contains( nodeId ) )
                {
                    if ( !clusterConfig.removeNode( nodeId ) )
                    {
                        getPluginDAO().deleteInfo( AccumuloClusterConfig.PRODUCT_KEY, clusterConfig.getClusterName() );
                    }
                    else
                    {
                        getPluginDAO().saveInfo( AccumuloClusterConfig.PRODUCT_KEY, clusterConfig.getClusterName(),
                                clusterConfig );
                    }
                }
            }
        }
    }


    @Override
    public void onEnvironmentDestroyed( final String environmentId )
    {
        List<AccumuloClusterConfig> clusterConfigs = new ArrayList<>( getClusters() );
        for ( final AccumuloClusterConfig clusterConfig : clusterConfigs )
        {
            if ( clusterConfig.getEnvironmentId().equals( environmentId ) )
            {
                getPluginDAO().deleteInfo( AccumuloClusterConfig.PRODUCT_KEY, clusterConfig.getClusterName() );
            }
        }
    }


    public ExecutorService getExecutor()
    {
        return executor;
    }


    public void setExecutor( final ExecutorService executor )
    {
        this.executor = executor;
    }


    public EnvironmentManager getEnvironmentManager()
    {
        return environmentManager;
    }


    public void setEnvironmentManager( final EnvironmentManager environmentManager )
    {
        this.environmentManager = environmentManager;
    }


    public Commands getCommands()
    {
        return commands;
    }


    public Tracker getTracker()
    {
        return tracker;
    }


    public void setTracker( final Tracker tracker )
    {
        this.tracker = tracker;
    }


    public Hadoop getHadoopManager()
    {
        return hadoopManager;
    }


    public void setHadoopManager( final Hadoop hadoopManager )
    {
        this.hadoopManager = hadoopManager;
    }


    public Zookeeper getZkManager()
    {
        return zkManager;
    }


    public void setZkManager( final Zookeeper zkManager )
    {
        this.zkManager = zkManager;
    }
}