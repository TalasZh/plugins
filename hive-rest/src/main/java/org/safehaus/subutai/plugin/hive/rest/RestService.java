package org.safehaus.subutai.plugin.hive.rest;


import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.safehaus.subutai.common.util.JsonUtil;
import org.safehaus.subutai.plugin.hadoop.api.HadoopClusterConfig;
import org.safehaus.subutai.plugin.hive.api.Hive;
import org.safehaus.subutai.plugin.hive.api.HiveConfig;
import org.safehaus.subutai.plugin.hive.api.SetupType;


public class RestService
{

    private static final String OPERATION_ID = "OPERATION_ID";

    private Hive hiveManager;


    public void setHiveManager( Hive hiveManager )
    {
        this.hiveManager = hiveManager;
    }


    @GET
    @Path( "clusters" )
    @Produces( { MediaType.APPLICATION_JSON } )
    public Response getClusters()
    {

        List<HiveConfig> configs = hiveManager.getClusters();
        ArrayList<String> clusterNames = new ArrayList();

        for ( HiveConfig config : configs )
        {
            clusterNames.add( config.getClusterName() );
        }

        String clusters = JsonUtil.GSON.toJson( clusterNames );
        return Response.status( Response.Status.OK ).entity( clusters ).build();
    }


    @GET
    @Path( "clusters/{clusterName}" )
    @Produces( { MediaType.APPLICATION_JSON } )
    public Response getCluster( @PathParam( "clusterName" ) String clusterName )
    {
        HiveConfig config = hiveManager.getCluster( clusterName );
        String cluster = JsonUtil.GSON.toJson( config );
        return Response.status( Response.Status.OK ).entity( cluster ).build();
    }


    @POST
    @Path( "clusters" )
    @Produces( { MediaType.APPLICATION_JSON } )
    public Response installCluster( @PathParam( "clusterName" ) String clusterName,
                                    @QueryParam( "hadoopClusterName" ) String hadoopClusterName,
                                    @QueryParam( "server" ) String server, @QueryParam( "clients" ) String clients )
    {

        HiveConfig config = new HiveConfig();
        config.setSetupType( SetupType.OVER_HADOOP );
        config.setClusterName( clusterName );
        config.setHadoopClusterName( hadoopClusterName );

        // TODO fix here please do not use agentManager, try to pass number of nodes to create instead of specific
        // containers
        //        Agent serverAgent = agentManager.getAgentByHostname( server );
        //        config.setServer( serverAgent.getUuid() );
        //
        //        for ( String client : clients.split( "," ) )
        //        {
        //            Agent agent = agentManager.getAgentByHostname( client );
        //            config.getClients().add( agent.getUuid() );
        //        }

        UUID uuid = hiveManager.installCluster( config );

        String operationId = JsonUtil.toJson( OPERATION_ID, uuid );
        return Response.status( Response.Status.CREATED ).entity( operationId ).build();
    }


    @POST
    @Path( "clusters/{name}/{hadoopName}/{slaveNodesCount}/{replFactor}/{domainName}" )
    @Produces( { MediaType.APPLICATION_JSON } )
    public Response install( @PathParam( "name" ) String name, @PathParam( "hadoopName" ) String hadoopName,
                             @PathParam( "slaveNodesCount" ) String slaveNodesCount,
                             @PathParam( "replFactor" ) String replFactor,
                             @PathParam( "domainName" ) String domainName )
    {

        HiveConfig config = new HiveConfig();
        config.setSetupType( SetupType.WITH_HADOOP );
        config.setClusterName( name );
        config.setHadoopClusterName( hadoopName );

        HadoopClusterConfig hc = new HadoopClusterConfig();
        hc.setClusterName( hadoopName );
        if ( domainName != null )
        {
            hc.setDomainName( domainName );
        }
        try
        {
            int i = Integer.parseInt( slaveNodesCount );
            hc.setCountOfSlaveNodes( i );
        }
        catch ( NumberFormatException ex )
        {
        }
        try
        {
            int i = Integer.parseInt( replFactor );
            hc.setReplicationFactor( i );
        }
        catch ( NumberFormatException ex )
        {
        }

        UUID trackId = hiveManager.installCluster( config, hc.getClusterName() );

        String operationId = JsonUtil.toJson( OPERATION_ID, trackId );
        return Response.status( Response.Status.CREATED ).entity( operationId ).build();
    }


    @DELETE
    @Path( "clusters/{clusterName}" )
    @Produces( { MediaType.APPLICATION_JSON } )
    public Response uninstallCluster( @PathParam( "clusterName" ) String clusterName )
    {
        UUID uuid = hiveManager.uninstallCluster( clusterName );
        String operationId = JsonUtil.toJson( OPERATION_ID, uuid );
        return Response.status( Response.Status.OK ).entity( operationId ).build();
    }


    @POST
    @Path( "clusters/{clusterName}/nodes/{hostname}" )
    @Produces( { MediaType.APPLICATION_JSON } )
    public Response addNode( @PathParam( "clusterName" ) String clusterName, @PathParam( "hostname" ) String hostname )
    {
        UUID uuid = hiveManager.addNode( clusterName, hostname );
        String operationId = JsonUtil.toJson( OPERATION_ID, uuid );
        return Response.status( Response.Status.CREATED ).entity( operationId ).build();
    }


    @DELETE
    @Path( "clusters/{clusterName}/nodes/{hostname}" )
    @Produces( { MediaType.APPLICATION_JSON } )
    public Response destroyNode( @PathParam( "clusterName" ) String clusterName,
                                 @PathParam( "hostname" ) String hostname )
    {
        UUID uuid = hiveManager.uninstallNode( clusterName, hostname );

        String operationId = JsonUtil.toJson( OPERATION_ID, uuid );
        return Response.status( Response.Status.OK ).entity( operationId ).build();
    }
}
