package org.safehaus.subutai.plugin.pig.ui.wizard;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.safehaus.subutai.core.environment.api.EnvironmentManager;
import org.safehaus.subutai.core.environment.api.helper.Environment;
import org.safehaus.subutai.core.peer.api.ContainerHost;
import org.safehaus.subutai.plugin.hadoop.api.Hadoop;
import org.safehaus.subutai.plugin.hadoop.api.HadoopClusterConfig;
import org.safehaus.subutai.plugin.pig.api.PigConfig;
import org.safehaus.subutai.plugin.pig.api.SetupType;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.vaadin.data.Property;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextField;
import com.vaadin.ui.TwinColSelect;
import com.vaadin.ui.VerticalLayout;


public class ConfigurationStep extends Panel
{
    private final Hadoop hadoop;
    private final EnvironmentManager environmentManager;
    private Environment hadoopEnvironment;
    final Wizard wizard;


    public ConfigurationStep( final Hadoop hadoop, final Wizard wizard, final EnvironmentManager environmentManager )
    {
        this.hadoop = hadoop;
        this.environmentManager = environmentManager;
        this.wizard = wizard;

        setSizeFull();

        GridLayout content = new GridLayout( 1, 3 );
        content.setSizeFull();
        content.setSpacing( true );
        content.setMargin( true );


        SetupType st = wizard.getConfig().getSetupType();

        if ( st == SetupType.OVER_HADOOP )
        {
            addOverHadoopControls( content, wizard.getConfig() );
        }
        else if ( st == SetupType.WITH_HADOOP )
        {
            addWithHadoopControls( content, wizard.getConfig(), wizard.getHadoopConfig() );
        }

        // --------------------------------------------------
        // Buttons

        Button next = new Button( "Next" );
        next.setId( "PigConfNext" );
        next.addStyleName( "default" );
        next.addClickListener( new Button.ClickListener()
        {
            @Override
            public void buttonClick( Button.ClickEvent clickEvent )
            {
                if ( Strings.isNullOrEmpty( wizard.getConfig().getHadoopClusterName() ) )
                {
                    show( "Please, enter Hadoop cluster" );
                }
                else
                {
                    wizard.setHadoopConfig( hadoop.getCluster( wizard.getConfig().getHadoopClusterName() ) );
                    wizard.next();
                }
            }
        } );

        Button back = new Button( "Back" );
        back.setId( "PigConfBack" );
        back.addStyleName( "default" );
        back.addClickListener( new Button.ClickListener()
        {
            @Override
            public void buttonClick( Button.ClickEvent clickEvent )
            {
                wizard.back();
            }
        } );

        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing( true );
        layout.addComponent( new Label( "Please, specify installation settings" ) );
        layout.addComponent( content );

        HorizontalLayout buttons = new HorizontalLayout();
        buttons.addComponent( back );
        buttons.addComponent( next );

        content.addComponent( buttons );

        setContent( layout );
    }


    private void addOverHadoopControls( ComponentContainer parent, final PigConfig config )
    {

        final TwinColSelect select = new TwinColSelect( "Nodes", new ArrayList<ContainerHost>() );
        select.setId( "PigConfSlaveNodes" );

        ComboBox hadoopClusters = new ComboBox( "Hadoop cluster" );
        hadoopClusters.setId( "PigConfHadoopCluster" );
        hadoopClusters.setImmediate( true );
        hadoopClusters.setTextInputAllowed( false );
        hadoopClusters.setRequired( true );
        hadoopClusters.setNullSelectionAllowed( false );
        hadoopClusters.addValueChangeListener( new Property.ValueChangeListener()
        {
            @Override
            public void valueChange( final Property.ValueChangeEvent event )
            {
                if ( event.getProperty().getValue() != null )
                {
                    HadoopClusterConfig hadoopInfo = ( HadoopClusterConfig ) event.getProperty().getValue();
                    config.setHadoopClusterName( hadoopInfo.getClusterName() );
                    config.setHadoopNodes( Sets.newHashSet( hadoopInfo.getAllNodes() ) );
                    hadoopEnvironment = environmentManager.getEnvironmentByUUID( hadoopInfo.getEnvironmentId() );
                    Set<ContainerHost> hadoopNodes =
                            hadoopEnvironment.getHostsByIds( Sets.newHashSet( hadoopInfo.getAllNodes() ) );
                    select.setValue( null );
                    select.setContainerDataSource( new BeanItemContainer<>( ContainerHost.class, hadoopNodes ) );
                    config.setHadoopClusterName( hadoopInfo.getClusterName() );
                    config.getNodes().clear();
                }
            }
        } );
        /*hadoopClusters.addValueChangeListener( new Property.ValueChangeListener()
        {
            @Override
            public void valueChange( Property.ValueChangeEvent event )
            {
                if ( event.getProperty().getValue() != null )
                {
                    HadoopClusterConfig hadoopInfo = ( HadoopClusterConfig ) event.getProperty().getValue();
                    hadoopEnvironment = environmentManager.getEnvironmentByUUID( hadoopInfo.getEnvironmentId() );
                    Set<ContainerHost> hadoopNodes =
                            hadoopEnvironment.getHostsByIds( Sets.newHashSet( hadoopInfo.getAllNodes() ) );

                    select.setValue( null );
                    select
                            .setContainerDataSource( new BeanItemContainer<>( ContainerHost.class, hadoopNodes ) );
                    config.setHadoopClusterName( hadoopInfo.getClusterName() );
                    wizard.setHadoopConfig( hadoop.getCluster( hadoopInfo.getClusterName() ) );
                    config.setHadoopManager( hadoop );
                    //config.getNodes().clear();
                }
            }
        } );*/

        select.setItemCaptionPropertyId( "hostname" );
        select.setRows( 7 );
        select.setMultiSelect( true );
        select.setImmediate( true );
        select.setLeftColumnCaption( "Available Nodes" );
        select.setRightColumnCaption( "Selected Nodes" );
        select.setWidth( 100, Unit.PERCENTAGE );
        select.setRequired( true );

        Hadoop hadoopManager = hadoop;
        List<HadoopClusterConfig> clusters = hadoopManager.getClusters();
        if ( clusters != null )
        {
            for ( HadoopClusterConfig hadoopClusterInfo : clusters )
            {
                hadoopClusters.addItem( hadoopClusterInfo );
                hadoopClusters.setItemCaption( hadoopClusterInfo, hadoopClusterInfo.getClusterName() );
            }
        }

        String hcn = config.getHadoopClusterName();
        if ( hcn != null )
        {
            HadoopClusterConfig info = hadoopManager.getCluster( hcn );
            if ( info != null )
            {
                hadoopClusters.setValue( info );
            }
        }
        else if ( clusters != null && !clusters.isEmpty() )
        {
            hadoopClusters.setValue( clusters.iterator().next() );
        }


        if ( config.getNodes() != null && !config.getNodes().isEmpty() )
        {
            select.setValue( config.getNodes() );
        }
        select.addValueChangeListener( new Property.ValueChangeListener()
        {
            @Override
            public void valueChange( Property.ValueChangeEvent event )
            {
                if ( event.getProperty().getValue() != null )
                {
                    Set<UUID> nodes = new HashSet<UUID>();
                    Set<ContainerHost> nodeList = ( Set<ContainerHost> ) event.getProperty().getValue();
                    for ( ContainerHost host : nodeList )
                    {
                        nodes.add( host.getAgent().getUuid() );
                    }
                    config.getNodes().clear();
                    config.getNodes().addAll( nodes );
                }
            }
        } );


        parent.addComponent( hadoopClusters );
        parent.addComponent( select );
    }


    private void addWithHadoopControls( ComponentContainer content, final PigConfig config,
                                        final HadoopClusterConfig hadoopConfig )
    {

        Collection<Integer> col = Arrays.asList( 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 );

        final TextField txtHadoopClusterName = new TextField( "Hadoop cluster name" );
        txtHadoopClusterName.setId( "PigConfHadoopCluster" );
        txtHadoopClusterName.setRequired( true );
        txtHadoopClusterName.setMaxLength( 20 );
        if ( hadoopConfig.getClusterName() != null )
        {
            txtHadoopClusterName.setValue( hadoopConfig.getClusterName() );
        }
        txtHadoopClusterName.addValueChangeListener( new Property.ValueChangeListener()
        {
            @Override
            public void valueChange( Property.ValueChangeEvent event )
            {
                String name = event.getProperty().getValue().toString().trim();
                config.setHadoopClusterName( name );
                hadoopConfig.setClusterName( name );
            }
        } );

        ComboBox cmbSlaveNodes = new ComboBox( "Number of Hadoop slave nodes", col );
        cmbSlaveNodes.setId( "PigConfSlaveNodes" );
        cmbSlaveNodes.setImmediate( true );
        cmbSlaveNodes.setTextInputAllowed( false );
        cmbSlaveNodes.setNullSelectionAllowed( false );
        cmbSlaveNodes.setValue( hadoopConfig.getCountOfSlaveNodes() );
        cmbSlaveNodes.addValueChangeListener( new Property.ValueChangeListener()
        {
            @Override
            public void valueChange( Property.ValueChangeEvent event )
            {
                hadoopConfig.setCountOfSlaveNodes( ( Integer ) event.getProperty().getValue() );
            }
        } );

        ComboBox cmbReplFactor = new ComboBox( "Replication factor for Hadoop slave nodes", col );
        cmbReplFactor.setId( "PigConfReplFactor" );
        cmbReplFactor.setImmediate( true );
        cmbReplFactor.setTextInputAllowed( false );
        cmbReplFactor.setNullSelectionAllowed( false );
        cmbReplFactor.setValue( hadoopConfig.getReplicationFactor() );
        cmbReplFactor.addValueChangeListener( new Property.ValueChangeListener()
        {
            @Override
            public void valueChange( Property.ValueChangeEvent event )
            {
                hadoopConfig.setReplicationFactor( ( Integer ) event.getProperty().getValue() );
            }
        } );

        TextField txtHadoopDomain = new TextField( "Hadoop cluster domain name" );
        txtHadoopDomain.setId( "PigConfHadoopClusterDomain" );
        txtHadoopDomain.setInputPrompt( hadoopConfig.getDomainName() );
        txtHadoopDomain.setValue( hadoopConfig.getDomainName() );
        txtHadoopDomain.setMaxLength( 20 );
        txtHadoopDomain.addValueChangeListener( new Property.ValueChangeListener()
        {
            @Override
            public void valueChange( Property.ValueChangeEvent event )
            {
                String val = event.getProperty().getValue().toString().trim();
                if ( !val.isEmpty() )
                {
                    hadoopConfig.setDomainName( val );
                }
            }
        } );

        content.addComponent( new Label( "Hadoop settings" ) );
        content.addComponent( txtHadoopClusterName );
        content.addComponent( cmbSlaveNodes );
        content.addComponent( cmbReplFactor );
        content.addComponent( txtHadoopDomain );
    }


    private void show( String notification )
    {
        Notification.show( notification );
    }
}
