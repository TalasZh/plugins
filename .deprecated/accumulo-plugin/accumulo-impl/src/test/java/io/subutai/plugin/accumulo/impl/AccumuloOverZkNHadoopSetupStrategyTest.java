package io.subutai.plugin.accumulo.impl;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
import io.subutai.common.environment.ContainerHostNotFoundException;
import io.subutai.common.environment.Environment;
import io.subutai.common.environment.EnvironmentNotFoundException;
import io.subutai.common.peer.EnvironmentContainerHost;
import io.subutai.common.tracker.TrackerOperation;
import io.subutai.core.environment.api.EnvironmentManager;
import io.subutai.core.tracker.api.Tracker;
import io.subutai.plugin.accumulo.api.AccumuloClusterConfig;
import io.subutai.core.plugincommon.api.ClusterConfigurationException;
import io.subutai.core.plugincommon.api.ClusterSetupException;
import io.subutai.core.plugincommon.api.ClusterSetupStrategy;
import io.subutai.core.plugincommon.api.PluginDAO;
import io.subutai.plugin.hadoop.api.Hadoop;
import io.subutai.plugin.hadoop.api.HadoopClusterConfig;
import io.subutai.plugin.zookeeper.api.Zookeeper;
import io.subutai.plugin.zookeeper.api.ZookeeperClusterConfig;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;


@Ignore
@RunWith( MockitoJUnitRunner.class )
public class AccumuloOverZkNHadoopSetupStrategyTest
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
    HadoopClusterConfig hadoopClusterConfig;
    @Mock
    ZookeeperClusterConfig zookeeperClusterConfig;
    @Mock
    Hadoop hadoop;
    @Mock
    Zookeeper zookeeper;
    @Mock
    PluginDAO pluginDAO;
    private AccumuloOverZkNHadoopSetupStrategy accumuloOverZkNHadoopSetupStrategy;
    private String id;


    @Before
    public void setUp() throws Exception
    {
        when( accumuloClusterConfig.getMasterNode() ).thenReturn( UUID.randomUUID().toString() );
        when( accumuloClusterConfig.getGcNode() ).thenReturn( UUID.randomUUID().toString() );
        when( accumuloClusterConfig.getMonitor() ).thenReturn( UUID.randomUUID().toString() );
        when( accumuloClusterConfig.getClusterName() ).thenReturn( "test-cluster" );
        when( accumuloClusterConfig.getInstanceName() ).thenReturn( "test-instance" );
        when( accumuloClusterConfig.getPassword() ).thenReturn( "test-password" );

        when( accumuloImpl.getHadoopManager() ).thenReturn( hadoop );

        id = UUID.randomUUID().toString();
        accumuloOverZkNHadoopSetupStrategy =
                new AccumuloOverZkNHadoopSetupStrategy( environment, accumuloClusterConfig, hadoopClusterConfig,
                        trackerOperation, accumuloImpl );
    }


    @Test
    public void testSetup() throws Exception
    {
        List<String> myList = new ArrayList<>();
        myList.add( id );

        Set<String> myUUID = new HashSet<>();
        myUUID.add( id );
        when( accumuloClusterConfig.getTracers() ).thenReturn( myUUID );
        when( accumuloClusterConfig.getSlaves() ).thenReturn( myUUID );

        when( hadoop.getCluster( anyString() ) ).thenReturn( hadoopClusterConfig );
        when( accumuloImpl.getZkManager() ).thenReturn( zookeeper );
        when( zookeeper.getCluster( anyString() ) ).thenReturn( zookeeperClusterConfig );
        when( zookeeperClusterConfig.getNodes() ).thenReturn( myUUID );
        when( zookeeperClusterConfig.getClusterName() ).thenReturn( "testClusterName" );
        when( environment.getContainerHostById( any( String.class ) ) ).thenReturn( containerHost );
        when( containerHost.getHostname() ).thenReturn( "testHostName" );

        when( zookeeper.startNode( anyString(), anyString() ) ).thenReturn( UUID.randomUUID() );

        when( hadoopClusterConfig.getAllNodes() ).thenReturn( myList );
        when( accumuloClusterConfig.getAllNodes() ).thenReturn( myUUID );
        when( containerHost.execute( any( RequestBuilder.class ) ) ).thenReturn( commandResult, commandResult );
        when( commandResult.hasSucceeded() ).thenReturn( true );
        when( commandResult.getStdOut() ).thenReturn( "Hadoop install ok installed" );


        // mock clusterConfiguration
        when( accumuloImpl.getEnvironmentManager() ).thenReturn( environmentManager );
        when( zookeeperClusterConfig.getEnvironmentId() ).thenReturn( id );
        when( environmentManager.loadEnvironment( id ) ).thenReturn( environment );
        when( accumuloImpl.getPluginDAO() ).thenReturn( pluginDAO );

        //accumuloOverZkNHadoopSetupStrategy.setup();

        // assertions
        assertNotNull( accumuloImpl.getHadoopManager().getCluster( anyString() ) );
        assertNotNull( accumuloImpl.getZkManager().getCluster( anyString() ) );
        //verify( containerHost ).execute( Commands.getInstallCommand() );
        //verify( trackerOperation )
        //.addLog( AccumuloClusterConfig.PRODUCT_KEY + " is installed on node " + containerHost.getHostname() );
        //assertNotNull( accumuloOverZkNHadoopSetupStrategy.setup() );
        assertEquals( accumuloClusterConfig, accumuloOverZkNHadoopSetupStrategy.setup() );
    }


    @Test( expected = ClusterSetupException.class )
    public void testSetupWhenMalformedConfiguration() throws ClusterSetupException
    {
        accumuloOverZkNHadoopSetupStrategy.setup();
    }


    @Test( expected = ClusterSetupException.class )
    public void testSetupWhenClusterNameExists() throws ClusterSetupException
    {
        Set<String> myUUID = new HashSet<>();
        myUUID.add( id );
        when( accumuloClusterConfig.getTracers() ).thenReturn( myUUID );
        when( accumuloClusterConfig.getSlaves() ).thenReturn( myUUID );
        when( accumuloImpl.getCluster( anyString() ) ).thenReturn( accumuloClusterConfig );
        accumuloOverZkNHadoopSetupStrategy.setup();
    }


    @Test( expected = ClusterSetupException.class )
    public void testSetupWhenHadoopClusterConfigIsNull() throws ClusterSetupException
    {
        Set<String> myUUID = new HashSet<>();
        myUUID.add( id );
        when( accumuloClusterConfig.getTracers() ).thenReturn( myUUID );
        when( accumuloClusterConfig.getSlaves() ).thenReturn( myUUID );
        when( accumuloImpl.getHadoopManager() ).thenReturn( hadoop );
        when( hadoop.getCluster( anyString() ) ).thenReturn( null );

        accumuloOverZkNHadoopSetupStrategy.setup();
    }


    @Test( expected = ClusterSetupException.class )
    public void testSetupWhenZookeperClusterConfigIsNull() throws ClusterSetupException
    {
        Set<String> myUUID = new HashSet<>();
        myUUID.add( id );
        when( accumuloClusterConfig.getTracers() ).thenReturn( myUUID );
        when( accumuloClusterConfig.getSlaves() ).thenReturn( myUUID );
        when( hadoop.getCluster( anyString() ) ).thenReturn( hadoopClusterConfig );
        when( accumuloImpl.getZkManager() ).thenReturn( zookeeper );
        when( zookeeper.getCluster( anyString() ) ).thenReturn( null );


        accumuloOverZkNHadoopSetupStrategy.setup();
    }


    @Test( expected = ClusterSetupException.class )
    public void testSetupWhenNodesNotBelongToHadoopCluster() throws ClusterSetupException
    {
        Set<String> myUUID2 = new HashSet<>();
        myUUID2.add( UUID.randomUUID().toString() );
        List<String> mylist2 = new ArrayList<>();
        mylist2.add( UUID.randomUUID().toString() );
        when( accumuloClusterConfig.getTracers() ).thenReturn( myUUID2 );
        when( accumuloClusterConfig.getSlaves() ).thenReturn( myUUID2 );
        when( hadoop.getCluster( anyString() ) ).thenReturn( hadoopClusterConfig );
        when( accumuloImpl.getZkManager() ).thenReturn( zookeeper );
        when( zookeeper.getCluster( anyString() ) ).thenReturn( zookeeperClusterConfig );
        when( hadoopClusterConfig.getAllNodes() ).thenReturn( mylist2 );
        when( accumuloClusterConfig.getAllNodes() ).thenReturn( myUUID2 );

        accumuloOverZkNHadoopSetupStrategy.setup();
    }


    @Test
    public void testSetupWhenCommandResultNotSucceded()
            throws CommandException, ClusterSetupException, ContainerHostNotFoundException, EnvironmentNotFoundException
    {
        List<String> myList = new ArrayList<>();
        myList.add( id );

        Set<String> myUUID = new HashSet<>();
        myUUID.add( id );
        when( accumuloClusterConfig.getTracers() ).thenReturn( myUUID );
        when( accumuloClusterConfig.getSlaves() ).thenReturn( myUUID );

        when( hadoop.getCluster( anyString() ) ).thenReturn( hadoopClusterConfig );
        when( accumuloImpl.getZkManager() ).thenReturn( zookeeper );
        when( zookeeper.getCluster( anyString() ) ).thenReturn( zookeeperClusterConfig );
        when( zookeeperClusterConfig.getNodes() ).thenReturn( myUUID );
        when( zookeeperClusterConfig.getClusterName() ).thenReturn( "testClusterName" );
        when( environment.getContainerHostById( any( String.class ) ) ).thenReturn( containerHost );
        when( containerHost.getHostname() ).thenReturn( "testHostName" );
        when( zookeeper.startNode( anyString(), anyString() ) ).thenReturn( UUID.randomUUID() );

        when( hadoopClusterConfig.getAllNodes() ).thenReturn( myList );
        when( accumuloClusterConfig.getAllNodes() ).thenReturn( myUUID );
        when( containerHost.execute( any( RequestBuilder.class ) ) ).thenReturn( commandResult );
        when( commandResult.getStdOut() ).thenReturn( "Hadoop install ok installed" );
        when( commandResult.hasSucceeded() ).thenReturn( false );


        // mock clusterConfiguration
        when( accumuloImpl.getEnvironmentManager() ).thenReturn( environmentManager );
        when( zookeeperClusterConfig.getEnvironmentId() ).thenReturn( id );
        when( environmentManager.loadEnvironment( id ) ).thenReturn( environment );
        when( accumuloImpl.getPluginDAO() ).thenReturn( pluginDAO );

        //accumuloOverZkNHadoopSetupStrategy.setup();
    }


    @Test( expected = ClusterSetupException.class )
    public void testSetupWhenCommandResultNotSucceded2()
            throws CommandException, ClusterSetupException, ContainerHostNotFoundException
    {
        List<String> myList = new ArrayList<>();
        myList.add( id );

        Set<String> myUUID = new HashSet<>();
        myUUID.add( id );
        when( accumuloClusterConfig.getTracers() ).thenReturn( myUUID );
        when( accumuloClusterConfig.getSlaves() ).thenReturn( myUUID );

        when( hadoop.getCluster( anyString() ) ).thenReturn( hadoopClusterConfig );
        when( accumuloImpl.getZkManager() ).thenReturn( zookeeper );
        when( zookeeper.getCluster( anyString() ) ).thenReturn( zookeeperClusterConfig );
        when( zookeeperClusterConfig.getNodes() ).thenReturn( myUUID );
        when( zookeeperClusterConfig.getClusterName() ).thenReturn( "testClusterName" );
        when( environment.getContainerHostById( any( String.class ) ) ).thenReturn( containerHost );
        when( containerHost.getHostname() ).thenReturn( "testHostName" );
        when( zookeeper.startNode( anyString(), anyString() ) ).thenReturn( UUID.randomUUID() );

        when( hadoopClusterConfig.getAllNodes() ).thenReturn( myList );
        when( accumuloClusterConfig.getAllNodes() ).thenReturn( myUUID );
        when( containerHost.execute( any( RequestBuilder.class ) ) ).thenThrow( CommandException.class );

        //accumuloOverZkNHadoopSetupStrategy.setup();
    }


    @Test( expected = ClusterSetupException.class )
    public void testSetupShouldThrowsClusterSetupException() throws Exception
    {
        List<String> myList = new ArrayList<>();
        myList.add( id );

        Set<String> myUUID = new HashSet<>();
        myUUID.add( id );
        when( accumuloClusterConfig.getTracers() ).thenReturn( myUUID );
        when( accumuloClusterConfig.getSlaves() ).thenReturn( myUUID );

        when( hadoop.getCluster( anyString() ) ).thenReturn( hadoopClusterConfig );
        when( accumuloImpl.getZkManager() ).thenReturn( zookeeper );
        when( zookeeper.getCluster( anyString() ) ).thenReturn( zookeeperClusterConfig );
        when( zookeeperClusterConfig.getNodes() ).thenReturn( myUUID );
        when( zookeeperClusterConfig.getClusterName() ).thenReturn( "testClusterName" );
        when( environment.getContainerHostById( any( String.class ) ) ).thenReturn( containerHost );
        when( containerHost.getHostname() ).thenReturn( "testHostName" );
        when( zookeeper.startNode( anyString(), anyString() ) ).thenReturn( UUID.randomUUID() );

        when( hadoopClusterConfig.getAllNodes() ).thenReturn( myList );
        when( accumuloClusterConfig.getAllNodes() ).thenReturn( myUUID );
        when( containerHost.execute( any( RequestBuilder.class ) ) ).thenReturn( commandResult );
        when( commandResult.hasSucceeded() ).thenReturn( true );
        when( commandResult.getStdOut() ).thenReturn( "Hadoop" );


        // mock clusterConfiguration
        when( accumuloImpl.getEnvironmentManager() ).thenReturn( environmentManager );
        when( zookeeperClusterConfig.getEnvironmentId() ).thenReturn( id );
        when( environmentManager.loadEnvironment( id ) ).thenReturn( environment );
        when( accumuloImpl.getPluginDAO() ).thenThrow( ClusterConfigurationException.class );

        accumuloOverZkNHadoopSetupStrategy.setup();
    }
}