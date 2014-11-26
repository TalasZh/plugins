package org.safehaus.subutai.plugin.spark.ui.manager;


import java.util.UUID;

import org.safehaus.subutai.common.tracker.OperationState;
import org.safehaus.subutai.common.tracker.TrackerOperationView;
import org.safehaus.subutai.core.tracker.api.Tracker;
import org.safehaus.subutai.plugin.spark.api.Spark;
import org.safehaus.subutai.plugin.spark.api.SparkClusterConfig;


public class StopAllTask implements Runnable
{
    private final String clusterName, lxcHostname;
    private final CompleteEvent completeEvent;
    private final Spark spark;
    private final Tracker tracker;


    public StopAllTask( final Spark spark, final Tracker tracker, String clusterName, String lxcHostname,
                        CompleteEvent completeEvent )
    {
        this.clusterName = clusterName;
        this.lxcHostname = lxcHostname;
        this.completeEvent = completeEvent;
        this.spark = spark;
        this.tracker = tracker;
    }


    @Override
    public void run()
    {

        UUID trackID = spark.stopCluster( clusterName, lxcHostname );

        long start = System.currentTimeMillis();

        while ( !Thread.interrupted() )
        {
            TrackerOperationView po = tracker.getTrackerOperation( SparkClusterConfig.PRODUCT_KEY, trackID );
            if ( po != null )
            {
                if ( po.getState() != OperationState.RUNNING )
                {
                    completeEvent.onComplete( po.getLog() );
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

            if ( System.currentTimeMillis() - start > 60 * 1000 )
            {
                break;
            }
        }
    }
}
