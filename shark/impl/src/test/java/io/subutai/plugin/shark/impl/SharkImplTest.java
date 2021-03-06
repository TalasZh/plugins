package io.subutai.plugin.shark.impl;


import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import io.subutai.common.command.CommandException;
import io.subutai.common.environment.Environment;
import io.subutai.common.tracker.TrackerOperation;
import io.subutai.core.environment.api.EnvironmentManager;
import io.subutai.core.metric.api.Monitor;
import io.subutai.core.tracker.api.Tracker;
import io.subutai.core.plugincommon.api.AbstractOperationHandler;
import io.subutai.core.plugincommon.api.ClusterException;
import io.subutai.core.plugincommon.api.ClusterSetupException;
import io.subutai.core.plugincommon.api.PluginDAO;
import io.subutai.plugin.shark.api.SharkClusterConfig;
import io.subutai.plugin.spark.api.Spark;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith( MockitoJUnitRunner.class )
public class SharkImplTest
{
    private SharkImpl sharkImpl;
    private UUID uuid;
    @Mock
    SharkClusterConfig sharkClusterConfig;
    @Mock
    Tracker tracker;
    @Mock
    TrackerOperation trackerOperation;
    @Mock
    EnvironmentManager environmentManager;
    @Mock
    Environment environment;
    @Mock
    ExecutorService executor;
    @Mock
    Spark spark;
    @Mock
    PluginDAO pluginDAO;
    @Mock
    Monitor monitor;


    @Before
    public void setUp() throws Exception
    {
        // mock init
        //when( dataSource.getConnection() ).thenReturn( connection );
        //when( connection.prepareStatement( any( String.class ) ) ).thenReturn( preparedStatement );
        //when( preparedStatement.executeQuery() ).thenReturn( resultSet );
        //when( resultSet.getMetaData() ).thenReturn( resultSetMetaData );
        //when( resultSetMetaData.getColumnCount() ).thenReturn( 1 );


        uuid = new UUID( 50, 50 );
        sharkImpl = new SharkImpl( tracker, environmentManager, spark, monitor, pluginDAO );
        //        sharkImpl.init();
        sharkImpl.setPluginDAO( pluginDAO );

        // mock InstallClusterHandler
        when( tracker.createTrackerOperation( anyString(), anyString() ) ).thenReturn( trackerOperation );
        when( trackerOperation.getId() ).thenReturn( uuid );
        sharkImpl.executor = executor;

        when( pluginDAO.getInfo( SharkClusterConfig.PRODUCT_KEY, "test", SharkClusterConfig.class ) )
                .thenReturn( sharkClusterConfig );

        // asserts
        //assertEquals( connection, dataSource.getConnection() );
        //assertEquals( preparedStatement, connection.prepareStatement( any( String.class ) ) );
        //assertEquals( resultSet, preparedStatement.executeQuery() );
        //assertEquals( resultSetMetaData, resultSet.getMetaData() );
        //assertNotNull( resultSetMetaData.getColumnCount() );
    }


    @Test
    public void testGetSparkManager()
    {
        sharkImpl.getSparkManager();

        assertNotNull( sharkImpl.getSparkManager() );
        assertEquals( spark, sharkImpl.getSparkManager() );
    }


    @Test
    public void testGetEnvironmentManager()
    {
        sharkImpl.getEnvironmentManager();

        assertNotNull( sharkImpl.getEnvironmentManager() );
        assertEquals( environmentManager, sharkImpl.getEnvironmentManager() );
    }


    @Test
    public void testGetPluginDao() throws SQLException
    {
        sharkImpl.getPluginDao();

        assertNotNull( sharkImpl.getPluginDao() );
    }


    @Test
    public void testInit() throws SQLException
    {
        //        sharkImpl.init();
    }


    @Test
    public void testGetTracker()
    {
        sharkImpl.getTracker();

        assertEquals( tracker, sharkImpl.getTracker() );
        assertNotNull( sharkImpl.getTracker() );
    }


    @Test
    public void testGetCommands() throws SQLException
    {
        sharkImpl.getCommands();

        //        assertNotNull(sharkImpl.getCommands());
    }


    @Test
    public void testDestroy() throws SQLException
    {
        sharkImpl.destroy();
    }


    @Test
    public void testInstallCluster() throws SQLException, CommandException, ClusterException, ClusterSetupException
    {
        UUID id = sharkImpl.installCluster( sharkClusterConfig );

        // asserts
        verify( executor ).execute( isA( AbstractOperationHandler.class ) );
        assertEquals( uuid, id );
    }


    @Test
    public void testUninstallCluster()
    {
        UUID id = sharkImpl.uninstallCluster( "test" );

        // asserts
        verify( executor ).execute( isA( AbstractOperationHandler.class ) );
        assertEquals( uuid, id );
    }


    @Test
    public void testGetClusters() throws SQLException
    {
        List<SharkClusterConfig> myList = new ArrayList<>();
        myList.add( sharkClusterConfig );
        when( pluginDAO.getInfo( SharkClusterConfig.PRODUCT_KEY, SharkClusterConfig.class ) ).thenReturn( myList );


        sharkImpl.getClusters();

        // assertions
        assertNotNull( sharkImpl.getClusters() );
        assertEquals( myList, sharkImpl.getClusters() );
    }


    @Test
    public void testGetCluster() throws SQLException
    {
        sharkImpl.getCluster( "test" );

        // assertions
        assertNotNull( sharkImpl.getCluster( "test" ) );
        assertEquals( sharkClusterConfig, sharkImpl.getCluster( "test" ) );
    }


    @Test
    public void testGetClusterSetupStrategy()
    {
        sharkImpl.getClusterSetupStrategy( trackerOperation, sharkClusterConfig, environment );

        assertNotNull( sharkImpl.getClusterSetupStrategy( trackerOperation, sharkClusterConfig, environment ) );
    }


    @Test
    public void testAddNode()
    {
        UUID id = sharkImpl.addNode( "test", "test" );

        // asserts
        verify( executor ).execute( isA( AbstractOperationHandler.class ) );
        assertEquals( uuid, id );
    }


    @Test
    public void testDestroyNode()
    {
        UUID id = sharkImpl.destroyNode( "test", "test" );

        // asserts
        verify( executor ).execute( isA( AbstractOperationHandler.class ) );
        assertEquals( uuid, id );
    }


    @Test
    public void testActualizeMasterIP()
    {
        UUID id = sharkImpl.actualizeMasterIP( "test" );

        // asserts
        verify( executor ).execute( isA( AbstractOperationHandler.class ) );
        assertEquals( uuid, id );
    }
}