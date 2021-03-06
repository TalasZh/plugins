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
import io.subutai.plugin.cassandra.cli.StartServiceCommand;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class StartServiceCommandTest
{
    private StartServiceCommand startServiceCommand;
    @Mock
    Cassandra cassandra;
    @Mock
    Tracker tracker;

    @Before
    public void setUp() 
    {
        startServiceCommand = new StartServiceCommand();
        startServiceCommand.setTracker(tracker);
        startServiceCommand.setCassandraManager(cassandra);

    }

    @Test
    public void testGetCassandraManager() 
    {
        startServiceCommand.getCassandraManager();

        // assertions
        assertNotNull(startServiceCommand.getCassandraManager());
        assertEquals(cassandra, startServiceCommand.getCassandraManager());

    }

    @Test
    public void testGetTracker() 
    {
        startServiceCommand.getTracker();

        // assertions
        assertNotNull(startServiceCommand.getTracker());
        assertEquals(tracker, startServiceCommand.getTracker());

    }

    @Test
    public void testDoExecute() throws IOException
    {
        when(cassandra.startService(null, null)).thenReturn(UUID.randomUUID());

        startServiceCommand.doExecute();
    }

}