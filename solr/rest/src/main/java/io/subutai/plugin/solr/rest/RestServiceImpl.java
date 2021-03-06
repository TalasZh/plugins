package io.subutai.plugin.solr.rest;


import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.core.Response;

import com.google.common.base.Preconditions;
import com.google.gson.reflect.TypeToken;

import io.subutai.common.environment.Environment;
import io.subutai.common.host.HostInterface;
import io.subutai.common.peer.EnvironmentContainerHost;
import io.subutai.common.tracker.OperationState;
import io.subutai.common.tracker.TrackerOperationView;
import io.subutai.common.util.JsonUtil;
import io.subutai.core.environment.api.EnvironmentManager;
import io.subutai.core.tracker.api.Tracker;
import io.subutai.plugin.solr.api.Solr;
import io.subutai.plugin.solr.api.SolrClusterConfig;
import io.subutai.plugin.solr.rest.dto.ContainerDto;
import io.subutai.plugin.solr.rest.dto.ClusterDto;


/**
 * REST implementation of Solr API
 */

public class RestServiceImpl implements RestService
{

    private Solr solrManager;
    private EnvironmentManager environmentManager;
    private Tracker tracker;


    public void setSolrManager( Solr solrManager )
    {
        Preconditions.checkNotNull( solrManager );

        this.solrManager = solrManager;
    }


    public Tracker getTracker()
    {
        return tracker;
    }


    public void setTracker( final Tracker tracker )
    {
        this.tracker = tracker;
    }


    @Override
    public Response listClusters()
    {

        List<SolrClusterConfig> configs = solrManager.getClusters();
        List<String> clusterNames = new ArrayList<>();
        for ( SolrClusterConfig config : configs )
        {
            clusterNames.add( config.getClusterName() );
        }
        String clusters = JsonUtil.GSON.toJson( clusterNames );
        return Response.status( Response.Status.OK ).entity( clusters ).build();
    }


    @Override
    public Response getCluster( final String clusterName )
    {
        SolrClusterConfig config = solrManager.getCluster( clusterName );

        boolean thrownException = false;
        if ( config == null )
        {
            thrownException = true;
        }


        ClusterDto clusterDto = new ClusterDto( clusterName );

        for ( String node : config.getNodes() )
        {
            try
            {
                ContainerDto containerDtoJson = new ContainerDto();

                Environment environment = environmentManager.loadEnvironment( config.getEnvironmentId() );
                EnvironmentContainerHost containerHost = environment.getContainerHostById( node );
				HostInterface hostInterface = containerHost.getInterfaceByName ("eth0");
                String ip = hostInterface.getIp ();
                containerDtoJson.setIp( ip );
                containerDtoJson.setId( node );
                containerDtoJson.setHostname( containerHost.getHostname() );

                UUID uuid = solrManager.checkNode( clusterName, node );
                OperationState state = waitUntilOperationFinish( uuid );
                Response response = createResponse( uuid, state );
                if ( response.getStatus() == 200 && !response.getEntity().toString().toUpperCase().contains( "NOT" ) )
                {
                    containerDtoJson.setStatus( "RUNNING" );
                }
                else
                {
                    containerDtoJson.setStatus( "STOPPED" );
                }

                clusterDto.addContainerDto( containerDtoJson );
            }
            catch ( Exception e )
            {
                thrownException = true;
            }
        }


        if ( thrownException )
        {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR )
                           .entity( clusterName + " cluster not found." ).build();
        }

        return Response.status( Response.Status.OK ).entity( JsonUtil.toJson( clusterDto ) ).build();
    }


    @Override
    public Response createCluster( final String clusterName, final String environmentId, final String nodes )
    {
        Preconditions.checkNotNull( clusterName );
        Preconditions.checkNotNull( nodes );

        SolrClusterConfig config = new SolrClusterConfig();
        config.setClusterName( clusterName );
        config.setEnvironmentId( environmentId );

        String[] arr = nodes.replaceAll( "\\s+", "" ).split( "," );
        for ( String node : arr )
        {
            config.getNodes().add( node );
        }

        UUID uuid = solrManager.installCluster( config );
        waitUntilOperationFinish( uuid );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }


    @Override
    public Response destroyCluster( final String clusterName )
    {
        Preconditions.checkNotNull( clusterName );
        if ( solrManager.getCluster( clusterName ) == null )
        {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).
                    entity( clusterName + " cluster not found." ).build();
        }
        UUID uuid = solrManager.uninstallCluster( clusterName );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }


    @Override
    public Response startNode( final String clusterName, final String lxcHostname )
    {
        Preconditions.checkNotNull( clusterName );
        Preconditions.checkNotNull( lxcHostname );
        if ( solrManager.getCluster( clusterName ) == null )
        {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).
                    entity( clusterName + " cluster not found." ).build();
        }
        UUID uuid = solrManager.startNode( clusterName, lxcHostname );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }


    @Override
    public Response stopNode( final String clusterName, final String lxcHostname )
    {
        Preconditions.checkNotNull( clusterName );
        Preconditions.checkNotNull( lxcHostname );
        if ( solrManager.getCluster( clusterName ) == null )
        {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).
                    entity( clusterName + " cluster not found." ).build();
        }
        UUID uuid = solrManager.stopNode( clusterName, lxcHostname );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }


    @Override
    public Response startNodes( final String clusterName, final String lxcHosts )
    {
        return nodeOperation( clusterName, lxcHosts, true );
    }


    @Override
    public Response stopNodes( final String clusterName, final String lxcHosts )
    {
        return nodeOperation( clusterName, lxcHosts, false );
    }


    private Response nodeOperation( String clusterName, String lxcHosts, boolean startNode )
    {
        Preconditions.checkNotNull( clusterName );
        Preconditions.checkNotNull( lxcHosts );
        List<String> hosts;


        if ( solrManager.getCluster( clusterName ) == null )
        {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).
                    entity( clusterName + " cluster not found." ).build();
        }

        try
        {
            hosts = JsonUtil.fromJson( lxcHosts, new TypeToken<List<String>>() {}.getType() );
        }
        catch ( Exception e )
        {
            return Response.status( Response.Status.BAD_REQUEST ).entity( JsonUtil.toJson( "Bad input form" + e ) ).build();
        }

        int errors = 0;

        for( String host : hosts )
        {
            UUID uuid;
            if( startNode )
            {
                uuid = solrManager.startNode( clusterName, host );
            }
            else
            {
                uuid = solrManager.stopNode( clusterName, host );
            }

            OperationState state = waitUntilOperationFinish( uuid );

            Response response = createResponse( uuid, state );

            if ( response.getStatus() != 200 )
            {
                errors++;
            }
        }

        if ( errors > 0 )
        {
            return Response.status( Response.Status.EXPECTATION_FAILED )
                           .entity( errors + " nodes are failed to execute" ).build();
        }

        return Response.ok().build();
    }


    @Override
    public Response checkNode( final String clusterName, final String lxcHostname )
    {
        Preconditions.checkNotNull( clusterName );
        Preconditions.checkNotNull( lxcHostname );
        if ( solrManager.getCluster( clusterName ) == null )
        {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).
                    entity( clusterName + " cluster not found." ).build();
        }
        UUID uuid = solrManager.checkNode( clusterName, lxcHostname );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }


    private Response createResponse( UUID uuid, OperationState state )
    {
        TrackerOperationView po = tracker.getTrackerOperation( SolrClusterConfig.PRODUCT_KEY, uuid );
        if ( state == OperationState.FAILED )
        {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).entity( JsonUtil.toJson( po.getLog() ) ).build();
        }
        else if ( state == OperationState.SUCCEEDED )
        {
            return Response.status( Response.Status.OK ).entity( JsonUtil.toJson( po.getLog() ) ).build();
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
            TrackerOperationView po = tracker.getTrackerOperation( SolrClusterConfig.PRODUCT_KEY, uuid );
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
            if ( System.currentTimeMillis() - start > ( 90 * 1000 ) )
            {
                break;
            }
        }
        return state;
    }


    public void setEnvironmentManager( final EnvironmentManager environmentManager )
    {
        Preconditions.checkNotNull( environmentManager );

        this.environmentManager = environmentManager;
    }
}
