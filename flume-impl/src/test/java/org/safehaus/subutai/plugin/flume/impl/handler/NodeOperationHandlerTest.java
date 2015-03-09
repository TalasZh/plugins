package org.safehaus.subutai.plugin.flume.impl.handler;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.safehaus.subutai.common.command.CommandException;
import org.safehaus.subutai.common.command.CommandResult;
import org.safehaus.subutai.common.command.RequestBuilder;
import org.safehaus.subutai.common.environment.Environment;
import org.safehaus.subutai.common.peer.ContainerHost;
import org.safehaus.subutai.common.tracker.TrackerOperation;
import org.safehaus.subutai.core.env.api.EnvironmentManager;
import org.safehaus.subutai.core.tracker.api.Tracker;
import org.safehaus.subutai.plugin.common.PluginDAO;
import org.safehaus.subutai.plugin.common.api.ClusterSetupStrategy;
import org.safehaus.subutai.plugin.common.api.NodeOperationType;
import org.safehaus.subutai.plugin.flume.api.FlumeConfig;
import org.safehaus.subutai.plugin.flume.impl.CommandType;
import org.safehaus.subutai.plugin.flume.impl.Commands;
import org.safehaus.subutai.plugin.flume.impl.FlumeImpl;
import org.safehaus.subutai.plugin.hadoop.api.Hadoop;
import org.safehaus.subutai.plugin.hadoop.api.HadoopClusterConfig;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class NodeOperationHandlerTest
{
    private NodeOperationHandler nodeOperationHandler;
    private NodeOperationHandler nodeOperationHandler2;
    private NodeOperationHandler nodeOperationHandler3;
    private NodeOperationHandler nodeOperationHandler4;
    private NodeOperationHandler nodeOperationHandler5;
    private NodeOperationHandler nodeOperationHandler6;
    private NodeOperationHandler nodeOperationHandler7;
    private UUID uuid;
    @Mock
    FlumeImpl flumeImpl;
    @Mock
    FlumeConfig flumeConfig;
    @Mock
    Tracker tracker;
    @Mock
    EnvironmentManager environmentManager;
    @Mock
    TrackerOperation trackerOperation;
    @Mock
    Environment environment;
    @Mock
    ContainerHost containerHost;
    @Mock
    CommandResult commandResult;
    @Mock
    ClusterSetupStrategy clusterSetupStrategy;
    @Mock
    PluginDAO pluginDAO;
    @Mock
    Hadoop hadoop;
    @Mock
    HadoopClusterConfig hadoopClusterConfig;

    @Before
    public void setUp() throws Exception
    {
        // mock constructor
        uuid = new UUID(50, 50);
        when(flumeImpl.getCluster("testClusterName")).thenReturn(flumeConfig);
        when(flumeImpl.getTracker()).thenReturn(tracker);
        when(tracker.createTrackerOperation(anyString(), anyString())).thenReturn(trackerOperation);
        when(trackerOperation.getId()).thenReturn(uuid);

        nodeOperationHandler = new NodeOperationHandler(flumeImpl, "testClusterName", "testHostName",
                NodeOperationType.START);
        nodeOperationHandler2 = new NodeOperationHandler(flumeImpl, "testClusterName", "testHostName",
                NodeOperationType.STOP);
        nodeOperationHandler3 = new NodeOperationHandler(flumeImpl, "testClusterName", "testHostName",
                NodeOperationType.STATUS);
        nodeOperationHandler4 = new NodeOperationHandler(flumeImpl, "testClusterName", "testHostName",
                NodeOperationType.INSTALL);
        nodeOperationHandler5 = new NodeOperationHandler(flumeImpl, "testClusterName", "testHostName",
                NodeOperationType.UNINSTALL);

        // mock run method
        Set<ContainerHost> mySet = new HashSet<>();
        mySet.add(containerHost);
        when(containerHost.getHostname()).thenReturn("testHostName");
        when(environmentManager.findEnvironment(any(UUID.class))).thenReturn(environment);
        when(environment.getContainerHosts()).thenReturn(mySet);

        // mock installProductOnNode
        when(commandResult.hasSucceeded()).thenReturn(true);
        when(hadoop.getCluster(anyString())).thenReturn(hadoopClusterConfig);

        when(flumeImpl.getPluginDao()).thenReturn(pluginDAO);
        when(flumeImpl.getEnvironmentManager()).thenReturn(environmentManager);
    }


    @Test
    public void testRunOperationTypeStart() throws Exception
    {
        when(containerHost.execute((new RequestBuilder(Commands.make(CommandType.START))))).thenReturn(commandResult);

        nodeOperationHandler.run();

        // assertions
        assertNotNull(flumeImpl.getCluster("testClusterName"));
    }

    @Test
    public void testRunOperationTypeStartFlumeConfigIsNull() throws Exception
    {
        when(flumeImpl.getCluster("testClusterName")).thenReturn(null);

        nodeOperationHandler.run();

        // assertions
        verify(trackerOperation).addLogFailed(String.format("Cluster with name %s does not exist", "testClusterName"));
    }


    @Test
    public void testRunOperationTypeStop() throws CommandException
    {
        when(containerHost.execute((new RequestBuilder(Commands.make(CommandType.STOP))))).thenReturn(commandResult);

        nodeOperationHandler2.run();

        // assertions
        assertNotNull(flumeImpl.getCluster("testClusterName"));
    }

    @Test
    public void testRunOperationTypeStatus() throws CommandException
    {
        when(containerHost.execute((new RequestBuilder(Commands.make(CommandType.SERVICE_STATUS))))).thenReturn
                (commandResult);

        nodeOperationHandler3.run();

        // assertions
        assertNotNull(flumeImpl.getCluster("testClusterName"));
    }

    @Test
    public void testRunOperationTypeInstall() throws CommandException
    {
        when(containerHost.execute((new RequestBuilder(Commands.make(CommandType.INSTALL)).withTimeout(600))))
                .thenReturn(commandResult);
        when(commandResult.hasSucceeded()).thenReturn(true);

        nodeOperationHandler4.run();

        // assertions
        assertNotNull(flumeImpl.getCluster("testClusterName"));
        assertTrue(commandResult.hasSucceeded());
    }

    @Test
    public void testRunOperationTypeInstallHasNotSucceeded() throws CommandException
    {
        when(containerHost.execute((new RequestBuilder(Commands.make(CommandType.INSTALL)).withTimeout(600))))
                .thenReturn(commandResult);
        when(commandResult.hasSucceeded()).thenReturn(false);

        nodeOperationHandler4.run();

        // assertions
        assertNotNull(flumeImpl.getCluster("testClusterName"));
        assertFalse(commandResult.hasSucceeded());
    }

    @Test
    public void testRunOperationTypeUninstall() throws CommandException
    {
        when(containerHost.execute((new RequestBuilder(Commands.make(CommandType.PURGE)).withTimeout(600))))
                .thenReturn(commandResult);
        when(commandResult.hasSucceeded()).thenReturn(true);

        nodeOperationHandler5.run();

        // assertions
        assertNotNull(flumeImpl.getCluster("testClusterName"));
        assertTrue(commandResult.hasSucceeded());
    }

    @Test
    public void testRunOperationTypeUninstallHasNotSucceeded() throws CommandException
    {
        when(containerHost.execute((new RequestBuilder(Commands.make(CommandType.PURGE)).withTimeout(600))))
                .thenReturn(commandResult);
        when(commandResult.hasSucceeded()).thenReturn(false);

        nodeOperationHandler5.run();

        // assertions
        assertNotNull(flumeImpl.getCluster("testClusterName"));
        assertFalse(commandResult.hasSucceeded());
    }


    @Test
    public void testLogStatusResultsFlumeIsNotRunning() throws Exception
    {
        when(commandResult.getExitCode()).thenReturn(256);

        nodeOperationHandler.logStatusResults(trackerOperation, commandResult, NodeOperationType.CHECK_INSTALLATION);
    }
}