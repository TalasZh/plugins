package io.subutai.plugin.cassandra.cli;


import java.io.IOException;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import io.subutai.core.tracker.api.Tracker;
import io.subutai.plugin.cassandra.api.Cassandra;
import io.subutai.plugin.cassandra.cli.StatusServiceCommand;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class StatusServiceCommandTest
{
    private StatusServiceCommand statusServiceCommand;
    @Mock
    Cassandra cassandra;
    @Mock
    Tracker tracker;

    @Before
    public void setUp() 
    {
        statusServiceCommand = new StatusServiceCommand();
        statusServiceCommand.setCassandraManager(cassandra);
        statusServiceCommand.setTracker(tracker);
    }

    @Test
    public void testGetCassandraManager() 
    {
        statusServiceCommand.getCassandraManager();

        // assertions
        assertNotNull(statusServiceCommand.getCassandraManager());
        assertEquals(cassandra, statusServiceCommand.getCassandraManager());

    }

    @Test
    public void testGetTracker() 
    {
        statusServiceCommand.getTracker();

        // assertions
        assertNotNull(statusServiceCommand.getTracker());
        assertEquals(tracker, statusServiceCommand.getTracker());

    }

    @Test
    public void testDoExecute() throws IOException
    {
        when(cassandra.statusService(null, null)).thenReturn(UUID.randomUUID());

        statusServiceCommand.doExecute();
    }
}