package io.subutai.plugin.pig.ui.manager;


import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.vaadin.data.Property;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.server.Sizeable;
import com.vaadin.server.ThemeResource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.Embedded;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Table;
import com.vaadin.ui.Window;

import io.subutai.common.environment.ContainerHostNotFoundException;
import io.subutai.common.environment.Environment;
import io.subutai.common.environment.EnvironmentNotFoundException;
import io.subutai.common.peer.EnvironmentContainerHost;
import io.subutai.core.environment.api.EnvironmentManager;
import io.subutai.core.tracker.api.Tracker;
import io.subutai.plugin.hadoop.api.Hadoop;
import io.subutai.plugin.hadoop.api.HadoopClusterConfig;
import io.subutai.plugin.pig.api.Pig;
import io.subutai.plugin.pig.api.PigConfig;
import io.subutai.server.ui.component.ConfirmationDialog;
import io.subutai.server.ui.component.ProgressWindow;
import io.subutai.server.ui.component.TerminalWindow;


public class Manager
{
    private final static Logger LOG = LoggerFactory.getLogger( Manager.class );

    protected static final String AVAILABLE_OPERATIONS_COLUMN_CAPTION = "AVAILABLE_OPERATIONS";
    protected static final String REFRESH_CLUSTERS_CAPTION = "Refresh Clusters";
    protected static final String DESTROY_BUTTON_CAPTION = "Destroy";
    protected static final String DESTROY_CLUSTER_BUTTON_CAPTION = "Destroy Cluster";
    protected static final String ADD_NODE_BUTTON_CAPTION = "Add Node";
    protected static final String HOST_COLUMN_CAPTION = "Host";
    protected static final String IP_COLUMN_CAPTION = "IP List";
    protected static final String BUTTON_STYLE_NAME = "default";

    final Button refreshClustersBtn, destroyClusterBtn, addNodeBtn;
    private final GridLayout contentRoot;
    private final ComboBox clusterCombo;
    private final Table nodesTable;
    private final Tracker tracker;
    private final ExecutorService executorService;
    private final Pig pig;
    private final Hadoop hadoop;
    private PigConfig config;
    private final EnvironmentManager environmentManager;
    private final Embedded PROGRESS_ICON = new Embedded( "", new ThemeResource( "img/spinner.gif" ) );


    public Manager( ExecutorService executorService, Pig pig, Hadoop hadoop, Tracker tracker,
                    EnvironmentManager environmentManager ) throws NamingException
    {
        Preconditions.checkNotNull( executorService, "Executor is null" );

        this.executorService = executorService;
        this.pig = pig;
        this.hadoop = hadoop;
        this.tracker = tracker;
        this.environmentManager = environmentManager;


        contentRoot = new GridLayout();
        contentRoot.setSpacing( true );
        contentRoot.setMargin( true );
        contentRoot.setSizeFull();
        contentRoot.setRows( 9 );
        contentRoot.setColumns( 1 );

        //tables go here
        nodesTable = createTableTemplate( "Nodes" );
        nodesTable.setId( "PigMngNodesTable" );

        HorizontalLayout controlsContent = new HorizontalLayout();
        controlsContent.setSpacing( true );

        Label clusterNameLabel = new Label( "Select the cluster" );
        controlsContent.addComponent( clusterNameLabel );
        controlsContent.setComponentAlignment( clusterNameLabel, Alignment.MIDDLE_CENTER );

        clusterCombo = new ComboBox();
        clusterCombo.setId( "PigMngClusterCombo" );
        clusterCombo.setImmediate( true );
        clusterCombo.setTextInputAllowed( false );
        clusterCombo.setWidth( 200, Sizeable.Unit.PIXELS );
        clusterCombo.addValueChangeListener( new Property.ValueChangeListener()
        {
            @Override
            public void valueChange( Property.ValueChangeEvent event )
            {
                config = ( PigConfig ) event.getProperty().getValue();
                refreshUI();
            }
        } );

        controlsContent.addComponent( clusterCombo );
        controlsContent.setComponentAlignment( clusterCombo, Alignment.MIDDLE_CENTER );


        /** Refresh Cluster button */
        refreshClustersBtn = new Button( REFRESH_CLUSTERS_CAPTION );
        refreshClustersBtn.setId( "PigMngRefresh" );
        refreshClustersBtn.addClickListener( new Button.ClickListener()
        {
            @Override
            public void buttonClick( Button.ClickEvent clickEvent )
            {
                PROGRESS_ICON.setVisible( true );
                new Thread( new Runnable()
                {
                    @Override
                    public void run()
                    {
                        refreshClustersInfo();
                    }
                } ).start();
            }
        } );
        controlsContent.addComponent( refreshClustersBtn );
        controlsContent.setComponentAlignment( refreshClustersBtn, Alignment.MIDDLE_CENTER );


        /** Destroy Cluster button */
        destroyClusterBtn = new Button( DESTROY_CLUSTER_BUTTON_CAPTION );
        destroyClusterBtn.setId( "PigMngDestroy" );
        addClickListenerToDestroyClusterButton();
        controlsContent.addComponent( destroyClusterBtn );
        controlsContent.setComponentAlignment( destroyClusterBtn, Alignment.MIDDLE_CENTER );

        /** Add Node button */
        addNodeBtn = new Button( ADD_NODE_BUTTON_CAPTION );
        addNodeBtn.setId( "PigMngAddNode" );
        addClickListenerToAddNodeButton();
        controlsContent.addComponent( addNodeBtn );
        controlsContent.setComponentAlignment( addNodeBtn, Alignment.MIDDLE_CENTER );

        addStyleNameToButtons( refreshClustersBtn, destroyClusterBtn, addNodeBtn );

        PROGRESS_ICON.setVisible( false );
        PROGRESS_ICON.setId( "indicator" );
        controlsContent.addComponent( PROGRESS_ICON );

        contentRoot.addComponent( controlsContent, 0, 0 );
        contentRoot.addComponent( nodesTable, 0, 1, 0, 8 );
    }


    public void addStyleNameToButtons( Button... buttons )
    {
        for ( Button b : buttons )
        {
            b.addStyleName( BUTTON_STYLE_NAME );
        }
    }


    public void addClickListenerToAddNodeButton()
    {
        addNodeBtn.addClickListener( new Button.ClickListener()
        {
            @Override
            public void buttonClick( Button.ClickEvent clickEvent )
            {
                if ( config == null )
                {
                    show( "Please, select cluster" );
                }
                else
                {

                    String hn = config.getHadoopClusterName();
                    if ( hn == null || hn.isEmpty() )
                    {
                        show( "Undefined Hadoop cluster name" );
                        return;
                    }
                    HadoopClusterConfig info = hadoop.getCluster( hn );
                    if ( info != null )
                    {
                        Set<String> nodes = new HashSet<>( info.getAllNodes() );
                        nodes.removeAll( config.getNodes() );
                        if ( !nodes.isEmpty() )
                        {
                            Set<EnvironmentContainerHost> hosts = null;
                            try
                            {
                                hosts = environmentManager.loadEnvironment( info.getEnvironmentId() )
                                                          .getContainerHostsByIds( nodes );
                            }
                            catch ( ContainerHostNotFoundException e )
                            {
                                LOG.error( "Container host not found", e );
                            }
                            catch ( EnvironmentNotFoundException e )
                            {
                                LOG.error( "Environment not found", e );
                            }
                            AddNodeWindow addNodeWindow =
                                    new AddNodeWindow( pig, tracker, executorService, config, hosts );
                            contentRoot.getUI().addWindow( addNodeWindow );
                            addNodeWindow.addCloseListener( new Window.CloseListener()
                            {
                                @Override
                                public void windowClose( Window.CloseEvent closeEvent )
                                {
                                    refreshClustersInfo();
                                }
                            } );
                        }
                        else
                        {
                            show( "All nodes in corresponding Hadoop cluster have Presto installed" );
                        }
                    }
                    else
                    {
                        show( "Hadoop cluster info not found" );
                    }
                }
            }
        } );
    }


    public void addClickListenerToDestroyClusterButton()
    {
        destroyClusterBtn.addClickListener( new Button.ClickListener()
        {
            @Override
            public void buttonClick( Button.ClickEvent clickEvent )
            {
                if ( config != null )
                {
                    ConfirmationDialog alert = new ConfirmationDialog(
                            String.format( "Do you want to destroy the %s cluster?", config.getClusterName() ), "Yes",
                            "No" );
                    alert.getOk().addClickListener( new Button.ClickListener()
                    {
                        @Override
                        public void buttonClick( Button.ClickEvent clickEvent )
                        {

                            UUID trackID = pig.uninstallCluster( config );

                            ProgressWindow window =
                                    new ProgressWindow( executorService, tracker, trackID, PigConfig.PRODUCT_KEY );
                            window.getWindow().addCloseListener( new Window.CloseListener()
                            {
                                @Override
                                public void windowClose( Window.CloseEvent closeEvent )
                                {
                                    refreshClustersInfo();
                                }
                            } );
                            contentRoot.getUI().addWindow( window.getWindow() );
                        }
                    } );

                    contentRoot.getUI().addWindow( alert.getAlert() );
                }
                else
                {
                    show( "Please, select cluster" );
                }
            }
        } );
    }


    private Table createTableTemplate( String caption )
    {
        final Table table = new Table( caption );
        table.addContainerProperty( HOST_COLUMN_CAPTION, String.class, null );
        table.addContainerProperty( IP_COLUMN_CAPTION, String.class, null );
        table.addContainerProperty( AVAILABLE_OPERATIONS_COLUMN_CAPTION, HorizontalLayout.class, null );

        table.setSizeFull();
        table.setPageLength( 10 );
        table.setSelectable( false );
        table.setImmediate( true );
        addClickListenerToNodesTable( table );
        return table;
    }


    public void addClickListenerToNodesTable( final Table table )
    {
        table.addItemClickListener( new ItemClickEvent.ItemClickListener()
        {
            @Override
            public void itemClick( ItemClickEvent event )
            {
                if ( event.isDoubleClick() )
                {
                    String containerHostname =
                            ( String ) table.getItem( event.getItemId() ).getItemProperty( "Host" ).getValue();
                    Set<EnvironmentContainerHost> containerHosts;
                    try
                    {
                        containerHosts =
                                environmentManager.loadEnvironment( config.getEnvironmentId() ).getContainerHosts();
                    }
                    catch ( EnvironmentNotFoundException e )
                    {
                        LOG.error( "Error getting environment by id: " + config.getEnvironmentId(), e );
                        return;
                    }
                    Iterator iterator = containerHosts.iterator();
                    EnvironmentContainerHost containerHost = null;
                    while ( iterator.hasNext() )
                    {
                        containerHost = ( EnvironmentContainerHost ) iterator.next();
                        if ( containerHost.getHostname().equals( containerHostname ) )
                        {
                            break;
                        }
                    }
                    if ( containerHost != null )
                    {
                        TerminalWindow terminal = new TerminalWindow( containerHost );
                        contentRoot.getUI().addWindow( terminal.getWindow() );
                    }
                    else
                    {
                        show( "Host not found" );
                    }
                }
            }
        } );
    }


    private void show( String notification )
    {
        Notification.show( notification );
    }


    private void refreshUI()
    {
        if ( config != null )
        {
            Environment environment;
            try
            {
                environment = environmentManager.loadEnvironment( config.getEnvironmentId() );
            }
            catch ( EnvironmentNotFoundException e )
            {
                LOG.error( "Error getting environment by id: " + config.getEnvironmentId(), e );
                return;
            }
            Set<EnvironmentContainerHost> hosts = null;
            try
            {
                hosts = environment.getContainerHostsByIds( config.getNodes() );
            }
            catch ( ContainerHostNotFoundException e )
            {
                LOG.error( "Container host not found", e );
            }
            populateTable( nodesTable, hosts );
            PROGRESS_ICON.setVisible( false );
        }
        else
        {
            nodesTable.removeAllItems();
            PROGRESS_ICON.setVisible( false );
        }
    }


    private void populateTable( final Table table, Set<EnvironmentContainerHost> containerHosts )
    {
        table.removeAllItems();
        for ( final EnvironmentContainerHost host : containerHosts )
        {
            final Button destroyBtn = new Button( DESTROY_BUTTON_CAPTION );
            destroyBtn.setId( host.getIpByInterfaceName( "eth0" ) + "-pigDestroy" );
            destroyBtn.addStyleName( "default" );

            final HorizontalLayout availableOperations = new HorizontalLayout();
            availableOperations.addStyleName( BUTTON_STYLE_NAME );
            availableOperations.setSpacing( true );

            addGivenComponents( availableOperations, destroyBtn );

            table.addItem( new Object[] {
                    host.getHostname(), host.getIpByInterfaceName( "eth0" ), availableOperations
            }, null );

            addClickListenerToDestroyButton( host, destroyBtn );
        }
    }


    private void addClickListenerToDestroyButton( final EnvironmentContainerHost host, Button... buttons )
    {
        getButton( DESTROY_BUTTON_CAPTION, buttons ).addClickListener( new Button.ClickListener()
        {
            @Override
            public void buttonClick( Button.ClickEvent clickEvent )
            {
                ConfirmationDialog alert = new ConfirmationDialog(
                        String.format( "Do you want to destroy the %s node?", host.getHostname() ), "Yes", "No" );
                alert.getOk().addClickListener( new Button.ClickListener()
                {
                    @Override
                    public void buttonClick( Button.ClickEvent clickEvent )
                    {
                        UUID trackID = pig.destroyNode( config.getClusterName(), host.getHostname() );
                        ProgressWindow window =
                                new ProgressWindow( executorService, tracker, trackID, PigConfig.PRODUCT_KEY );
                        window.getWindow().addCloseListener( new Window.CloseListener()
                        {
                            @Override
                            public void windowClose( Window.CloseEvent closeEvent )
                            {
                                refreshClustersInfo();
                            }
                        } );
                        contentRoot.getUI().addWindow( window.getWindow() );
                    }
                } );

                contentRoot.getUI().addWindow( alert.getAlert() );
            }
        } );
    }


    public Button getButton( String caption, Button... buttons )
    {
        for ( Button b : buttons )
        {
            if ( b.getCaption().equals( caption ) )
            {
                return b;
            }
        }
        return null;
    }


    public void refreshClustersInfo()
    {
        List<PigConfig> clustersInfo = pig.getClusters();
        PigConfig clusterInfo = ( PigConfig ) clusterCombo.getValue();
        clusterCombo.removeAllItems();
        if ( clustersInfo != null && !clustersInfo.isEmpty() )
        {
            for ( PigConfig pigClusterInfo : clustersInfo )
            {
                clusterCombo.addItem( pigClusterInfo );
                clusterCombo.setItemCaption( pigClusterInfo,
                        pigClusterInfo.getClusterName() + "(" + pigClusterInfo.getHadoopClusterName() + ")" );
            }
            if ( clusterInfo != null )
            {
                for ( PigConfig pigClusterInfo : clustersInfo )
                {
                    if ( pigClusterInfo.getClusterName().equals( clusterInfo.getClusterName() ) )
                    {
                        clusterCombo.setValue( pigClusterInfo );
                        PROGRESS_ICON.setVisible( false );
                        return;
                    }
                }
            }
            else
            {
                clusterCombo.setValue( clustersInfo.iterator().next() );
                PROGRESS_ICON.setVisible( false );
            }
        }
        PROGRESS_ICON.setVisible( false );
    }


    public void addGivenComponents( HorizontalLayout layout, Button... buttons )
    {
        for ( Button b : buttons )
        {
            layout.addComponent( b );
        }
    }


    public Component getContent()
    {
        return contentRoot;
    }
}
