/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.subutai.plugin.accumulo.ui.wizard;


import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Window;

import io.subutai.common.environment.ContainerHostNotFoundException;
import io.subutai.common.environment.Environment;
import io.subutai.common.environment.EnvironmentNotFoundException;
import io.subutai.common.peer.EnvironmentContainerHost;
import io.subutai.core.environment.api.EnvironmentManager;
import io.subutai.core.tracker.api.Tracker;
import io.subutai.plugin.accumulo.api.Accumulo;
import io.subutai.plugin.accumulo.api.AccumuloClusterConfig;
import io.subutai.plugin.accumulo.api.SetupType;
import io.subutai.plugin.hadoop.api.Hadoop;
import io.subutai.server.ui.component.ProgressWindow;


public class VerificationStep extends Panel
{
    private static final Logger LOGGER = LoggerFactory.getLogger( VerificationStep.class );


    public VerificationStep( final Accumulo accumulo, final Hadoop hadoop, final ExecutorService executorService,
                             final Tracker tracker, EnvironmentManager environmentManager, final Wizard wizard )
    {

        setSizeFull();

        GridLayout grid = new GridLayout( 1, 5 );
        grid.setSpacing( true );
        grid.setMargin( true );
        grid.setSizeFull();

        Label confirmationLbl = new Label( "<strong>Please verify the installation settings "
                + "(you may change them by clicking on Back button)</strong><br/>" );
        confirmationLbl.setContentMode( ContentMode.HTML );

        ConfigView cfgView = new ConfigView( "Installation configuration" );
        cfgView.addStringCfg( "Cluster Name", wizard.getConfig().getClusterName() );
        cfgView.addStringCfg( "Instance name", wizard.getConfig().getInstanceName() );
        cfgView.addStringCfg( "Password", wizard.getConfig().getPassword() );
        cfgView.addStringCfg( "Hadoop cluster", wizard.getConfig().getHadoopClusterName() );
        cfgView.addStringCfg( "Zookeeper cluster", wizard.getConfig().getZookeeperClusterName() );

        if ( wizard.getConfig().getSetupType() == SetupType.OVER_HADOOP_N_ZK )
        {
            try
            {
                Environment hadoopEnvironment = environmentManager.loadEnvironment(
                        hadoop.getCluster( wizard.getConfig().getHadoopClusterName() ).getEnvironmentId() );
                EnvironmentContainerHost master =
                        hadoopEnvironment.getContainerHostById( wizard.getConfig().getMasterNode() );
                EnvironmentContainerHost gc = hadoopEnvironment.getContainerHostById( wizard.getConfig().getGcNode() );
                EnvironmentContainerHost monitor =
                        hadoopEnvironment.getContainerHostById( wizard.getConfig().getMonitor() );
                Set<EnvironmentContainerHost> tracers =
                        hadoopEnvironment.getContainerHostsByIds( wizard.getConfig().getTracers() );
                Set<EnvironmentContainerHost> slaves =
                        hadoopEnvironment.getContainerHostsByIds( wizard.getConfig().getSlaves() );

                cfgView.addStringCfg( "Master node", master.getHostname() );
                cfgView.addStringCfg( "GC node", gc.getHostname() );
                cfgView.addStringCfg( "Monitor node", monitor.getHostname() );
                for ( EnvironmentContainerHost containerHost : tracers )
                {
                    cfgView.addStringCfg( "Tracers", containerHost.getHostname() );
                }
                for ( EnvironmentContainerHost containerHost : slaves )
                {
                    cfgView.addStringCfg( "Slaves", containerHost.getHostname() );
                }
            }
            catch ( EnvironmentNotFoundException | ContainerHostNotFoundException e )
            {
                LOGGER.error( "Error applying operations on environment/container", e );
            }
        }
        else
        {
            cfgView.addStringCfg( "Number of Hadoop slaves",
                    wizard.getHadoopClusterConfig().getCountOfSlaveNodes() + "" );
            cfgView.addStringCfg( "Hadoop replication factor",
                    wizard.getHadoopClusterConfig().getReplicationFactor() + "" );
            cfgView.addStringCfg( "Hadoop domain name", wizard.getHadoopClusterConfig().getDomainName() + "" );
            cfgView.addStringCfg( "Number of tracers", wizard.getConfig().getNumberOfTracers() + "" );
            cfgView.addStringCfg( "Number of slaves", wizard.getConfig().getNumberOfSlaves() + "" );
        }

        Button install = new Button( "Install" );
        install.setId( "installBtn" );
        install.addStyleName( "default" );
        install.addClickListener( new Button.ClickListener()
        {
            @Override
            public void buttonClick( Button.ClickEvent event )
            {
                UUID trackID;
                if ( wizard.getConfig().getSetupType() == SetupType.OVER_HADOOP_N_ZK )
                {
                    trackID = accumulo.installCluster( wizard.getConfig() );
                }
                else
                {
                    trackID = accumulo.installCluster( wizard.getConfig() );
                }
                ProgressWindow window =
                        new ProgressWindow( executorService, tracker, trackID, AccumuloClusterConfig.PRODUCT_KEY );
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
        back.setId( "verBack" );
        back.addStyleName( "default" );
        back.addClickListener( new Button.ClickListener()
        {
            @Override
            public void buttonClick( Button.ClickEvent event )
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
