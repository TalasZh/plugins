package io.subutai.plugin.mongodb.rest;


import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.ws.rs.core.Response;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

import io.subutai.common.environment.ContainerHostNotFoundException;
import io.subutai.common.environment.Environment;
import io.subutai.common.environment.EnvironmentNotFoundException;
import io.subutai.common.peer.ContainerHost;
import io.subutai.common.tracker.OperationState;
import io.subutai.common.tracker.TrackerOperationView;
import io.subutai.common.util.CollectionUtil;
import io.subutai.common.util.JsonUtil;
import io.subutai.core.environment.api.EnvironmentManager;
import io.subutai.core.tracker.api.Tracker;
import io.subutai.plugin.mongodb.api.Mongo;
import io.subutai.plugin.mongodb.api.MongoClusterConfig;
import io.subutai.plugin.mongodb.api.NodeType;
import io.subutai.plugin.mongodb.rest.pojo.ContainerPojo;
import io.subutai.plugin.mongodb.rest.pojo.MongoPojo;


/**
 * REST implementation of MongoDB API
 */

public class RestServiceImpl implements RestService
{

    private Mongo mongo;
    private Tracker tracker;
    private EnvironmentManager environmentManager;


    public RestServiceImpl( final Mongo mongo )
    {
        this.mongo = mongo;
    }


    @Override
    public Response listClusters()
    {
        List<MongoClusterConfig> configs = mongo.getClusters();
        List<String> clusterNames = new ArrayList<>();
        for ( MongoClusterConfig config : configs )
        {
            clusterNames.add( config.getClusterName() );
        }
        String clusters = JsonUtil.toJson( clusterNames );
        return Response.status( Response.Status.OK ).entity( clusters ).build();
    }


    @Override
    public Response getCluster( final String clusterName )
    {
        MongoClusterConfig config = mongo.getCluster( clusterName );
        if ( config == null )
        {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).
                    entity( clusterName + " cluster not found " ).build();
        }
        String cluster = JsonUtil.toJson( updateConfig( config ) );
        return Response.status( Response.Status.OK ).entity( cluster ).build();
    }


    @Override
    public Response configureCluster( final String config )
    {
        TrimmedMongodbConfig trimmedConfig = JsonUtil.fromJson( config, TrimmedMongodbConfig.class );
        MongoClusterConfig mongoConfig = mongo.newMongoClusterConfigInstance();
        mongoConfig.setDomainName( trimmedConfig.getDomainName() );
        mongoConfig.setReplicaSetName( trimmedConfig.getReplicaSetName() );
        mongoConfig.setRouterPort( trimmedConfig.getRouterPort() );
        mongoConfig.setDataNodePort( trimmedConfig.getDataNodePort() );
        mongoConfig.setCfgSrvPort( trimmedConfig.getCfgSrvPort() );
        mongoConfig.setEnvironmentId( trimmedConfig.getEnvironmentId() );
        mongoConfig.setClusterName( trimmedConfig.getClusterName() );

        if ( !CollectionUtil.isCollectionEmpty( trimmedConfig.getConfigNodes() ) )
        {
            mongoConfig.setConfigHosts( trimmedConfig.getConfigNodes() );
        }

        if ( !CollectionUtil.isCollectionEmpty( trimmedConfig.getDataNodes() ) )
        {
            mongoConfig.setDataHosts( trimmedConfig.getDataNodes() );
        }

        if ( !CollectionUtil.isCollectionEmpty( trimmedConfig.getRouterNodes() ) )
        {
            mongoConfig.setRouterHosts( trimmedConfig.getRouterNodes() );
        }
        UUID uuid = mongo.installCluster( mongoConfig );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }


    @Override
    public Response destroyCluster( final String clusterName )
    {
        Preconditions.checkNotNull( clusterName );
        if ( mongo.getCluster( clusterName ) == null )
        {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).
                    entity( clusterName + " cluster not found." ).build();
        }
        UUID uuid = mongo.uninstallCluster( clusterName );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }


    @Override
    public Response startNode( final String clusterName, final String lxcHostname, String nodeType )
    {
        Preconditions.checkNotNull( clusterName );
        Preconditions.checkNotNull( lxcHostname );
        if ( mongo.getCluster( clusterName ) == null )
        {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).
                    entity( clusterName + " cluster not found." ).build();
        }
        NodeType type = null;
        if ( nodeType.contains( "config" ) )
        {
            type = NodeType.CONFIG_NODE;
        }
        else if ( nodeType.contains( "data" ) )
        {
            type = NodeType.DATA_NODE;
        }
        else if ( nodeType.contains( "router" ) )
        {
            type = NodeType.ROUTER_NODE;
        }
        UUID uuid = mongo.startNode( clusterName, lxcHostname, type );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }


    @Override
    public Response stopNode( final String clusterName, final String lxcHostname, String nodeType )
    {
        Preconditions.checkNotNull( clusterName );
        Preconditions.checkNotNull( lxcHostname );
        if ( mongo.getCluster( clusterName ) == null )
        {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).
                    entity( clusterName + " cluster not found." ).build();
        }
        NodeType type = null;
        if ( nodeType.contains( "config" ) )
        {
            type = NodeType.CONFIG_NODE;
        }
        else if ( nodeType.contains( "data" ) )
        {
            type = NodeType.DATA_NODE;
        }
        else if ( nodeType.contains( "router" ) )
        {
            type = NodeType.ROUTER_NODE;
        }
        UUID uuid = mongo.stopNode( clusterName, lxcHostname, type );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }


    @Override
    public Response startCluster( final String clusterName )
    {
        Preconditions.checkNotNull( clusterName );
        if ( mongo.getCluster( clusterName ) == null )
        {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).
                    entity( clusterName + " cluster not found." ).build();
        }
        UUID uuid = mongo.startAllNodes( clusterName );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }


    @Override
    public Response stopCluster( final String clusterName )
    {
        Preconditions.checkNotNull( clusterName );
        if ( mongo.getCluster( clusterName ) == null )
        {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).
                    entity( clusterName + " cluster not found." ).build();
        }
        UUID uuid = mongo.stopAllNodes( clusterName );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }


    @Override
    public Response destroyNode( final String clusterName, final String lxcHostname, final String nodeType )
    {
        Preconditions.checkNotNull( clusterName );
        Preconditions.checkNotNull( lxcHostname );
        if ( mongo.getCluster( clusterName ) == null )
        {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).
                    entity( clusterName + " cluster not found." ).build();
        }
        NodeType type = null;
        if ( nodeType.contains( "config" ) )
        {
            type = NodeType.CONFIG_NODE;
        }
        else if ( nodeType.contains( "data" ) )
        {
            type = NodeType.DATA_NODE;
        }
        else if ( nodeType.contains( "router" ) )
        {
            type = NodeType.ROUTER_NODE;
        }

        UUID uuid = mongo.destroyNode( clusterName, lxcHostname, type );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }


    @Override
    public Response checkNode( final String clusterName, final String lxcHostname, String nodeType )
    {
        Preconditions.checkNotNull( clusterName );
        Preconditions.checkNotNull( lxcHostname );
        if ( mongo.getCluster( clusterName ) == null )
        {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).
                    entity( clusterName + " cluster not found." ).build();
        }
        NodeType type = null;
        if ( nodeType.contains( "config" ) )
        {
            type = NodeType.CONFIG_NODE;
        }
        else if ( nodeType.contains( "data" ) )
        {
            type = NodeType.DATA_NODE;
        }
        else if ( nodeType.contains( "router" ) )
        {
            type = NodeType.ROUTER_NODE;
        }
        UUID uuid = mongo.checkNode( clusterName, lxcHostname, type );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }


    @Override
    public Response addNode( final String clusterName, final String nodeType )
    {
        Preconditions.checkNotNull( clusterName );
        Preconditions.checkNotNull( nodeType );
        if ( mongo.getCluster( clusterName ) == null )
        {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).
                    entity( clusterName + " cluster not found." ).build();
        }
        NodeType type = null;
        if ( nodeType.contains( "config" ) )
        {
            type = NodeType.CONFIG_NODE;
        }
        else if ( nodeType.contains( "data" ) )
        {
            type = NodeType.DATA_NODE;
        }
        else if ( nodeType.contains( "router" ) )
        {
            type = NodeType.ROUTER_NODE;
        }
        UUID uuid = mongo.addNode( clusterName, type );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }


    private MongoPojo updateConfig( MongoClusterConfig config )
    {
        MongoPojo pojo = new MongoPojo();
        Set<ContainerPojo> configHosts = Sets.newHashSet();
        Set<ContainerPojo> routerHosts = Sets.newHashSet();
        Set<ContainerPojo> dataHosts = Sets.newHashSet();

        try
        {
            pojo.setClusterName( config.getClusterName() );
            pojo.setEnvironmentId( config.getEnvironmentId() );

            Environment environment = environmentManager.loadEnvironment( config.getEnvironmentId() );

            for ( final String uuid : config.getConfigHosts() )
            {
                ContainerHost ch = environment.getContainerHostById( uuid );
                UUID uuidStatus = mongo.checkNode( config.getClusterName(), ch.getHostname(), NodeType.CONFIG_NODE );
                configHosts.add( new ContainerPojo( ch.getHostname(), uuid, ch.getIpByInterfaceName( "eth0" ),
                        checkStatus( tracker, uuidStatus ) ) );
            }
            pojo.setConfigHosts( configHosts );

            for ( final String uuid : config.getRouterHosts() )
            {
                ContainerHost ch = environment.getContainerHostById( uuid );
                UUID uuidStatus = mongo.checkNode( config.getClusterName(), ch.getHostname(), NodeType.ROUTER_NODE );
                routerHosts.add( new ContainerPojo( ch.getHostname(), uuid, ch.getIpByInterfaceName( "eth0" ),
                        checkStatus( tracker, uuidStatus ) ) );
            }
            pojo.setRouterHosts( routerHosts );

            for ( final String uuid : config.getDataHosts() )
            {
                ContainerHost ch = environment.getContainerHostById( uuid );
                UUID uuidStatus = mongo.checkNode( config.getClusterName(), ch.getHostname(), NodeType.DATA_NODE );
                dataHosts.add( new ContainerPojo( ch.getHostname(), uuid, ch.getIpByInterfaceName( "eth0" ),
                        checkStatus( tracker, uuidStatus ) ) );
            }
            pojo.setDataHosts( dataHosts );
        }
        catch ( EnvironmentNotFoundException | ContainerHostNotFoundException e )
        {
            e.printStackTrace();
        }

        return pojo;
    }


    private String checkStatus( Tracker tracker, UUID uuid )
    {
        String state = "UNKNOWN";
        long start = System.currentTimeMillis();
        while ( !Thread.interrupted() )
        {
            TrackerOperationView po = tracker.getTrackerOperation( MongoClusterConfig.PRODUCT_KEY, uuid );
            if ( po != null )
            {
                if ( po.getState() != OperationState.RUNNING )
                {
                    if ( po.getLog().contains( "service is NOT running on node" ) )
                    {
                        state = "STOPPED";
                    }
                    else if ( po.getLog().contains( "service is running on node" ) )
                    {
                        state = "RUNNING";
                    }
                    break;
                }
            }
            try
            {
                Thread.sleep( 1000 );
            }
            catch ( InterruptedException ex )
            {
                break;
            }
            if ( System.currentTimeMillis() - start > ( 30 + 3 ) * 1000 )
            {
                break;
            }
        }

        return state;
    }


    private Response createResponse( UUID uuid, OperationState state )
    {
        TrackerOperationView po = tracker.getTrackerOperation( MongoClusterConfig.PRODUCT_KEY, uuid );
        if ( state == OperationState.FAILED )
        {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).entity( po.getLog() ).build();
        }
        else if ( state == OperationState.SUCCEEDED )
        {
            return Response.status( Response.Status.OK ).entity( po.getLog() ).build();
        }
        else
        {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).entity( "Timeout" ).build();
        }
    }


    private OperationState waitUntilOperationFinish( UUID uuid )
    {
        OperationState state = null;
        long start = System.currentTimeMillis();
        while ( !Thread.interrupted() )
        {
            TrackerOperationView po = tracker.getTrackerOperation( MongoClusterConfig.PRODUCT_KEY, uuid );
            if ( po != null )
            {
                if ( po.getState() != OperationState.RUNNING )
                {
                    state = po.getState();
                    break;
                }
            }
            try
            {
                Thread.sleep( 1000 );
            }
            catch ( InterruptedException ex )
            {
                break;
            }
            if ( System.currentTimeMillis() - start > ( 200 * 1000 ) )
            {
                break;
            }
        }
        return state;
    }


    public void setTracker( final Tracker tracker )
    {
        this.tracker = tracker;
    }


    public void setEnvironmentManager( final EnvironmentManager environmentManager )
    {
        this.environmentManager = environmentManager;
    }
}
