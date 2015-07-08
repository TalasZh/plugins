package io.subutai.plugin.spark.ui.wizard;


import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import io.subutai.common.environment.ContainerHostNotFoundException;
import io.subutai.common.environment.Environment;
import io.subutai.common.environment.EnvironmentNotFoundException;
import io.subutai.common.peer.ContainerHost;
import io.subutai.core.env.api.EnvironmentManager;
import io.subutai.core.tracker.api.Tracker;
import io.subutai.plugin.common.ui.ConfigView;
import io.subutai.plugin.hadoop.api.Hadoop;
import io.subutai.plugin.hadoop.api.HadoopClusterConfig;
import io.subutai.plugin.spark.api.Spark;
import io.subutai.plugin.spark.api.SparkClusterConfig;
import io.subutai.server.ui.component.ProgressWindow;

import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Window;


public class VerificationStep extends Panel
{

    public VerificationStep( final Hadoop hadoop, final Tracker tracker, final Spark spark,
                             final ExecutorService executor, EnvironmentManager environmentManager,
                             final Wizard wizard )
    {

        setSizeFull();

        GridLayout grid = new GridLayout( 1, 5 );
        grid.setSpacing( true );
        grid.setMargin( true );
        grid.setSizeFull();

        Label confirmationLbl = new Label( "<strong>Please verify the installation settings "
                + "(you may change them by clicking on Back button)</strong><br/>" );
        confirmationLbl.setContentMode( ContentMode.HTML );

        final SparkClusterConfig config = wizard.getConfig();

        ConfigView cfgView = new ConfigView( "Installation configuration" );
        cfgView.addStringCfg( "Cluster Name", config.getClusterName() );

        HadoopClusterConfig hadoopCluster = hadoop.getCluster( config.getHadoopClusterName() );
        Environment hadoopEnvironment;
        try
        {
            hadoopEnvironment = environmentManager.findEnvironment( hadoopCluster.getEnvironmentId() );
        }
        catch ( EnvironmentNotFoundException e )
        {
            Notification.show( String.format( "Hadoop environment %s not found", hadoopCluster.getEnvironmentId() ) );
            return;
        }
        ContainerHost master ;
        try
        {
            master = hadoopEnvironment.getContainerHostById( config.getMasterNodeId() );
        }
        catch ( ContainerHostNotFoundException e )
        {
            Notification.show( String.format( "Spark master %s not found", config.getMasterNodeId() ) );
            return;
        }
        Set<ContainerHost> slaves;
        try
        {
            slaves = hadoopEnvironment.getContainerHostsByIds( config.getSlaveIds() );
        }
        catch ( ContainerHostNotFoundException e )
        {
            Notification.show( String.format( "Spark slaves %s not found", config.getSlaveIds() ) );
            return;
        }
        cfgView.addStringCfg( "Hadoop cluster Name", config.getHadoopClusterName() );
        cfgView.addStringCfg( "Master Node", master.getHostname() );
        for ( ContainerHost slave : slaves )
        {
            cfgView.addStringCfg( "Slave nodes", slave.getHostname() );
        }


        Button install = new Button( "Install" );
        install.setId( "sparkVerificationInstall" );
        install.addStyleName( "default" );
        install.addClickListener( new Button.ClickListener()
        {
            @Override
            public void buttonClick( Button.ClickEvent clickEvent )
            {
                UUID trackId = spark.installCluster( config );
                ProgressWindow window =
                        new ProgressWindow( executor, tracker, trackId, SparkClusterConfig.PRODUCT_KEY );
                window.getWindow().addCloseListener( new Window.CloseListener()
                {
                    @Override
                    public void windowClose( Window.CloseEvent closeEvent )
                    {
                        wizard.init();
                    }
                } );
                getUI().addWindow( window.getWindow() );
            }
        } );

        Button back = new Button( "Back" );
        back.setId( "sparkVerificationBack" );
        back.addStyleName( "default" );
        back.addClickListener( new Button.ClickListener()
        {
            @Override
            public void buttonClick( Button.ClickEvent clickEvent )
            {
                wizard.back();
            }
        } );

        HorizontalLayout buttons = new HorizontalLayout();
        buttons.addComponent( back );
        buttons.addComponent( install );

        grid.addComponent( confirmationLbl, 0, 0 );

        grid.addComponent( cfgView.getCfgTable(), 0, 1, 0, 3 );

        grid.addComponent( buttons, 0, 4 );

        setContent( grid );
    }
}
