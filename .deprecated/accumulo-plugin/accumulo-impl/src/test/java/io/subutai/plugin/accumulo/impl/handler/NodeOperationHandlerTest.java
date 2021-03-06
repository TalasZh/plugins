package io.subutai.plugin.accumulo.impl.handler;


import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import io.subutai.common.command.CommandException;
import io.subutai.common.command.CommandResult;
import io.subutai.common.command.RequestBuilder;
import io.subutai.common.environment.Environment;
import io.subutai.common.peer.EnvironmentContainerHost;
import io.subutai.common.settings.Common;
import io.subutai.common.tracker.TrackerOperation;
import io.subutai.core.environment.api.EnvironmentManager;
import io.subutai.core.tracker.api.Tracker;
import io.subutai.plugin.accumulo.api.AccumuloClusterConfig;
import io.subutai.plugin.accumulo.impl.AccumuloImpl;
import io.subutai.plugin.accumulo.impl.Commands;
import io.subutai.core.plugincommon.api.ClusterConfigurationException;
import io.subutai.core.plugincommon.api.ClusterSetupStrategy;
import io.subutai.core.plugincommon.api.NodeOperationType;
import io.subutai.core.plugincommon.api.NodeType;
import io.subutai.core.plugincommon.api.PluginDAO;
import io.subutai.plugin.hadoop.api.Hadoop;
import io.subutai.plugin.hadoop.api.HadoopClusterConfig;
import io.subutai.plugin.zookeeper.api.Zookeeper;
import io.subutai.plugin.zookeeper.api.ZookeeperClusterConfig;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@Ignore
@RunWith( MockitoJUnitRunner.class )
public class NodeOperationHandlerTest
{
    @Mock
    AccumuloImpl accumuloImpl;
    @Mock
    AccumuloClusterConfig accumuloClusterConfig;
    @Mock
    Tracker tracker;
    @Mock
    EnvironmentManager environmentManager;
    @Mock
    TrackerOperation trackerOperation;
    @Mock
    Environment environment;
    @Mock
    EnvironmentContainerHost containerHost;
    @Mock
    CommandResult commandResult;
    @Mock
    ClusterSetupStrategy clusterSetupStrategy;
    @Mock
    PluginDAO pluginDAO;
    @Mock
    Hadoop hadoop;
    @Mock
    Zookeeper zookeeper;
    @Mock
    HadoopClusterConfig hadoopClusterConfig;
    @Mock
    ZookeeperClusterConfig zookeeperClusterConfig;
    private NodeOperationHandler nodeOperationHandler;
    private NodeOperationHandler nodeOperationHandler2;
    private NodeOperationHandler nodeOperationHandler3;
    private NodeOperationHandler nodeOperationHandler4;
    private NodeOperationHandler nodeOperationHandler5;
    private NodeOperationHandler nodeOperationHandler6;
    private NodeOperationHandler nodeOperationHandler7;
    private String id;


    @Before
    public void setUp() throws Exception
    {
        // mock constructor
        id = UUID.randomUUID().toString();
        when( accumuloImpl.getCluster( "testClusterName" ) ).thenReturn( accumuloClusterConfig );
        when( accumuloImpl.getTracker() ).thenReturn( tracker );
        when( tracker.createTrackerOperation( anyString(), anyString() ) ).thenReturn( trackerOperation );
        when( trackerOperation.getId() ).thenReturn( UUID.randomUUID() );

        nodeOperationHandler =
                new NodeOperationHandler( accumuloImpl, hadoop, zookeeper, "testClusterName", "testHostName",
                        NodeOperationType.INSTALL, NodeType.ACCUMULO_TRACER );
        nodeOperationHandler2 =
                new NodeOperationHandler( accumuloImpl, hadoop, zookeeper, "testClusterName", "testHostName",
                        NodeOperationType.START, NodeType.ACCUMULO_TRACER );
        nodeOperationHandler3 =
                new NodeOperationHandler( accumuloImpl, hadoop, zookeeper, "testClusterName", "testHostName",
                        NodeOperationType.STOP, NodeType.ACCUMULO_TRACER );
        nodeOperationHandler4 =
                new NodeOperationHandler( accumuloImpl, hadoop, zookeeper, "testClusterName", "testHostName",
                        NodeOperationType.STATUS, NodeType.ACCUMULO_TRACER );
        nodeOperationHandler5 =
                new NodeOperationHandler( accumuloImpl, hadoop, zookeeper, "testClusterName", "testHostName",
                        NodeOperationType.UNINSTALL, NodeType.ACCUMULO_TRACER );
        nodeOperationHandler6 =
                new NodeOperationHandler( accumuloImpl, hadoop, zookeeper, "testClusterName", "testHostName",
                        NodeOperationType.INSTALL, NodeType.ACCUMULO_TABLET_SERVER );
        nodeOperationHandler7 =
                new NodeOperationHandler( accumuloImpl, hadoop, zookeeper, "testClusterName", "testHostName",
                        NodeOperationType.UNINSTALL, NodeType.ACCUMULO_TABLET_SERVER );

        // mock run method
        Set<EnvironmentContainerHost> mySet = new HashSet<>();
        mySet.add( containerHost );
        when( containerHost.getHostname() ).thenReturn( "testHostName" );
        when( containerHost.getId() ).thenReturn( UUID.randomUUID().toString() );
        when( environmentManager.loadEnvironment( any( String.class ) ) ).thenReturn( environment );
        when( environment.getContainerHosts() ).thenReturn( mySet );

        // mock installProductOnNode
        when( commandResult.hasSucceeded() ).thenReturn( true );
        when( hadoop.getCluster( anyString() ) ).thenReturn( hadoopClusterConfig );

        when( accumuloImpl.getPluginDAO() ).thenReturn( pluginDAO );

        // mock clusterConfiguration and configureCluster method
        when( hadoopClusterConfig.getEnvironmentId() ).thenReturn( id );
        when( zookeeper.getCluster( anyString() ) ).thenReturn( zookeeperClusterConfig );

        Set<String> myUUID = new HashSet<>();
        myUUID.add( id );

        when( accumuloClusterConfig.getAllNodes() ).thenReturn( myUUID );
        when( accumuloClusterConfig.getMasterNode() ).thenReturn( id );
        when( accumuloClusterConfig.getGcNode() ).thenReturn( id );
        when( accumuloClusterConfig.getMonitor() ).thenReturn( id );
        when( environment.getContainerHostById( any( String.class ) ) ).thenReturn( containerHost );
        when( environment.getContainerHostByHostname( anyString() ) ).thenReturn( containerHost );
        when( containerHost.execute( any( RequestBuilder.class ) ) ).thenReturn( commandResult );
        when( zookeeperClusterConfig.getNodes() ).thenReturn( myUUID );
    }


    @Test
    public void testRunWithNodeOperationTypeInstallAndNodeTypeAccumuloTracer() throws Exception
    {
        Set<String> myUUID = new HashSet<>();
        when( containerHost.execute( Commands.getInstallCommand().withTimeout( 3600 ) ) ).thenReturn( commandResult );
        when( accumuloImpl.getEnvironmentManager() ).thenReturn( environmentManager );
        when( accumuloClusterConfig.getTracers() ).thenReturn( myUUID );
        when( accumuloImpl.getZkManager() ).thenReturn( zookeeper );
        when( zookeeper.getCluster( anyString() ) ).thenReturn( zookeeperClusterConfig );

        nodeOperationHandler.run();

        // assertions
        assertNotNull( accumuloImpl.getCluster( "testClusterName" ) );
        verify( containerHost ).execute( Commands.getInstallCommand().withTimeout( 3600 ) );
        assertTrue( commandResult.hasSucceeded() );
        assertEquals( pluginDAO, accumuloImpl.getPluginDAO() );
        assertEquals( myUUID, accumuloClusterConfig.getTracers() );
    }


    @Test
    public void testRunWithNodeOperationTypeInstallAndNodeTypeAccumuloTabletServers() throws Exception
    {
        Set<String> myUUID = new HashSet<>();
        when( containerHost.execute( Commands.getInstallCommand().withTimeout( 3600 ) ) ).thenReturn( commandResult );
        when( accumuloImpl.getEnvironmentManager() ).thenReturn( environmentManager );
        when( accumuloImpl.getZkManager() ).thenReturn( zookeeper );
        when( zookeeper.getCluster( anyString() ) ).thenReturn( zookeeperClusterConfig );
        when( accumuloClusterConfig.getSlaves() ).thenReturn( myUUID );

        nodeOperationHandler6.run();

        // assertions
        assertNotNull( accumuloImpl.getCluster( "testClusterName" ) );
        verify( containerHost ).execute( Commands.getInstallCommand().withTimeout( 3600 ) );
        assertTrue( commandResult.hasSucceeded() );
        assertEquals( pluginDAO, accumuloImpl.getPluginDAO() );
        assertEquals( myUUID, accumuloClusterConfig.getSlaves() );
    }


    @Test
    public void testRunWithNodeOperationTypeStart() throws CommandException
    {
        when( containerHost.execute( Commands.startCommand ) ).thenReturn( commandResult );
        when( accumuloImpl.getEnvironmentManager() ).thenReturn( environmentManager );

        nodeOperationHandler2.run();

        // assertions
        assertNotNull( accumuloImpl.getCluster( "testClusterName" ) );
        verify( containerHost ).execute( Commands.startCommand );
    }


    @Test
    public void testRunWithNodeOperationTypeStop() throws CommandException
    {
        when( containerHost.execute( Commands.stopCommand ) ).thenReturn( commandResult );
        when( accumuloImpl.getEnvironmentManager() ).thenReturn( environmentManager );

        nodeOperationHandler3.run();

        // assertions
        assertNotNull( accumuloImpl.getCluster( "testClusterName" ) );
        verify( containerHost ).execute( Commands.stopCommand );
    }


    @Test
    public void testRunWithNodeOperationTypeStatus() throws CommandException
    {
        when( containerHost.execute( Commands.statusCommand ) ).thenReturn( commandResult );
        when( accumuloImpl.getEnvironmentManager() ).thenReturn( environmentManager );

        nodeOperationHandler4.run();

        // assertions
        assertNotNull( accumuloImpl.getCluster( "testClusterName" ) );
        verify( containerHost ).execute( Commands.statusCommand );
    }


    @Test
    public void testRunWithNodeOperationTypeUninstallAndNodeTypeAccumuloTracer() throws Exception
    {
        Set<String> myUUID = new HashSet<>();
        myUUID.add( UUID.randomUUID().toString() );
        myUUID.add( UUID.randomUUID().toString() );
        when( accumuloImpl.getEnvironmentManager() ).thenReturn( environmentManager );
        when( accumuloClusterConfig.getTracers() ).thenReturn( myUUID );
        when( accumuloImpl.getZkManager() ).thenReturn( zookeeper );
        when( zookeeper.getCluster( anyString() ) ).thenReturn( zookeeperClusterConfig );

        nodeOperationHandler5.run();

        // assertions
        assertNotNull( accumuloImpl.getCluster( "testClusterName" ) );
        verify( containerHost ).execute( new RequestBuilder(
                Commands.uninstallCommand + Common.PACKAGE_PREFIX + AccumuloClusterConfig.PRODUCT_KEY.toLowerCase() ) );
        assertTrue( commandResult.hasSucceeded() );
        assertEquals( pluginDAO, accumuloImpl.getPluginDAO() );
        assertEquals( myUUID, accumuloClusterConfig.getTracers() );
    }


    @Test
    public void testRunWithNodeOperationTypeUninstallAndNodeTypeAccumuloTabletServers() throws Exception
    {
        Set<String> myUUID = new HashSet<>();
        myUUID.add( UUID.randomUUID().toString() );
        myUUID.add( UUID.randomUUID().toString() );
        when( accumuloImpl.getEnvironmentManager() ).thenReturn( environmentManager );
        when( accumuloClusterConfig.getSlaves() ).thenReturn( myUUID );
        when( accumuloImpl.getZkManager() ).thenReturn( zookeeper );
        when( zookeeper.getCluster( anyString() ) ).thenReturn( zookeeperClusterConfig );

        nodeOperationHandler7.run();

        // assertions
        assertNotNull( accumuloImpl.getCluster( "testClusterName" ) );
        verify( containerHost ).execute( new RequestBuilder(
                Commands.uninstallCommand + Common.PACKAGE_PREFIX + AccumuloClusterConfig.PRODUCT_KEY.toLowerCase() ) );
        assertTrue( commandResult.hasSucceeded() );
        assertEquals( pluginDAO, accumuloImpl.getPluginDAO() );
        assertEquals( myUUID, accumuloClusterConfig.getSlaves() );
    }


    // exceptions
    @Test
    public void testRunWithNodeOperationTypeStartCommandException() throws CommandException
    {
        when( containerHost.execute( Commands.startCommand ) ).thenThrow( CommandException.class );
        when( accumuloImpl.getEnvironmentManager() ).thenReturn( environmentManager );

        nodeOperationHandler2.run();
    }


    @Test( expected = ClusterConfigurationException.class )
    public void testRunWithNodeOperationTypeInstallClusterConfigurationException() throws Exception
    {
        when( accumuloImpl.getEnvironmentManager() ).thenThrow( ClusterConfigurationException.class );

        nodeOperationHandler.run();
    }
}