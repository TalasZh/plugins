package io.subutai.plugin.hadoop.cli;


import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import io.subutai.common.tracker.OperationState;
import io.subutai.common.tracker.TrackerOperationView;
import io.subutai.core.tracker.api.Tracker;
import io.subutai.plugin.hadoop.api.Hadoop;
import io.subutai.plugin.hadoop.cli.UninstallClusterCommand;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UninstallClusterCommandTest
{
    private UninstallClusterCommand uninstallClusterCommand;
    @Mock
    Hadoop hadoop;
    @Mock
    Tracker tracker;
    @Mock
    TrackerOperationView trackerOperationView;
    
    @Before
    public void setUp() throws Exception
    {
        uninstallClusterCommand = new UninstallClusterCommand();
        uninstallClusterCommand.setHadoopManager(hadoop);
        uninstallClusterCommand.setTracker( tracker );
    }


    @Test
    public void testGetTracker() throws Exception
    {
        uninstallClusterCommand.getTracker();

        // assertions
        assertNotNull(uninstallClusterCommand.getTracker());
        assertEquals(tracker, uninstallClusterCommand.getTracker());

    }

    @Test
    public void testSetTracker() throws Exception
    {
        uninstallClusterCommand.setTracker(tracker);

        // assertions
        assertNotNull(uninstallClusterCommand.getTracker());

    }



    @Test
    public void testGetHadoopManager() throws Exception
    {
        uninstallClusterCommand.getHadoopManager();

        // assertions
        assertNotNull(uninstallClusterCommand.getHadoopManager());
        assertEquals(hadoop, uninstallClusterCommand.getHadoopManager());

    }

    @Test
    public void testSetHadoopManager() throws Exception
    {
        uninstallClusterCommand.setHadoopManager(hadoop);

        // assertions
        assertNotNull(uninstallClusterCommand.getHadoopManager());

    }

    @Test
    public void testDoExecute() throws Exception
    {
        when(hadoop.uninstallCluster(anyString())).thenReturn(UUID.randomUUID());
        when(tracker.getTrackerOperation(anyString(),any(UUID.class))).thenReturn(trackerOperationView);
        when(trackerOperationView.getLog()).thenReturn("test");

        uninstallClusterCommand.setTracker( tracker );
        uninstallClusterCommand.doExecute();

        // assertions
        verify(hadoop).uninstallCluster(anyString());
        assertNotEquals(OperationState.RUNNING,trackerOperationView.getState());
    }

    @Test
    public void testDoExecuteWhenTrackerOperationIsNull() throws Exception
    {
        when(hadoop.uninstallCluster( anyString() )).thenReturn(UUID.randomUUID());
        when(tracker.getTrackerOperation( anyString(), any( UUID.class ) )).thenReturn( mock( TrackerOperationView.class ) );
        uninstallClusterCommand.setTracker( tracker );
        uninstallClusterCommand.doExecute();

        // assertions
        verify(hadoop).uninstallCluster(anyString());
    }

}