package io.subutai.plugin.oozie.ui.manager;


import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.data.Item;
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
import com.vaadin.ui.Layout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

import io.subutai.common.environment.ContainerHostNotFoundException;
import io.subutai.common.environment.EnvironmentNotFoundException;
import io.subutai.common.peer.EnvironmentContainerHost;
import io.subutai.core.environment.api.EnvironmentManager;
import io.subutai.core.tracker.api.Tracker;
import io.subutai.plugin.common.api.CompleteEvent;
import io.subutai.plugin.common.api.NodeOperationType;
import io.subutai.plugin.common.api.NodeState;
import io.subutai.plugin.hadoop.api.Hadoop;
import io.subutai.plugin.hadoop.api.HadoopClusterConfig;
import io.subutai.plugin.oozie.api.Oozie;
import io.subutai.plugin.oozie.api.OozieClusterConfig;
import io.subutai.plugin.oozie.api.OozieNodeOperationTask;
import io.subutai.server.ui.component.ConfirmationDialog;
import io.subutai.server.ui.component.ProgressWindow;
import io.subutai.server.ui.component.TerminalWindow;


public class Manager
{
    protected static final String AVAILABLE_OPERATIONS_COLUMN_CAPTION = "AVAILABLE_OPERATIONS";
    protected static final String REFRESH_CLUSTERS_CAPTION = "Refresh Clusters";
    protected static final String CHECK_BUTTON_CAPTION = "Check";
    protected static final String START_BUTTON_CAPTION = "Start";
    protected static final String STOP_BUTTON_CAPTION = "Stop";
    protected static final String DESTROY_BUTTON_CAPTION = "Destroy";
    protected static final String DESTROY_CLUSTER_BUTTON_CAPTION = "Destroy Cluster";
    protected static final String ADD_NODE_BUTTON_CAPTION = "Add Node";
    protected static final String SERVER_TABLE_CAPTION = "Server Nodes";
    protected static final String CLIENT_TABLE_CAPTION = "Client Nodes";
    protected static final String HOST_COLUMN_CAPTION = "Host";
    protected static final String IP_COLUMN_CAPTION = "IP List";
    protected static final String NODE_ROLE_COLUMN_CAPTION = "Node Role";
    protected static final String BUTTON_STYLE_NAME = "default";
    final Button refreshClustersBtn, destroyClusterBtn, addNodeBtn;
    private final Embedded PROGRESS_ICON = new Embedded( "", new ThemeResource( "img/spinner.gif" ) );
    private final ComboBox clusterCombo;
    private final Table serverTable, clientsTable;
    private final Oozie oozie;
    private final ExecutorService executorService;
    private final EnvironmentManager environmentManager;
    private final Tracker tracker;
    private GridLayout contentRoot;
    private OozieClusterConfig config;
    private Hadoop hadoop;
    private final static Logger LOGGER = LoggerFactory.getLogger( Manager.class );


    public Manager( final ExecutorService executorService, Oozie oozie, Hadoop hadoop, Tracker tracker,
                    EnvironmentManager environmentManager ) throws NamingException
    {
        this.executorService = executorService;
        this.oozie = oozie;
        this.hadoop = hadoop;
        this.tracker = tracker;
        this.environmentManager = environmentManager;


        contentRoot = new GridLayout();
        contentRoot.setSpacing( true );
        contentRoot.setMargin( true );
        contentRoot.setSizeFull();
        contentRoot.setRows( 10 );
        contentRoot.setColumns( 1 );

        //tables go here
        serverTable = createTableTemplate( SERVER_TABLE_CAPTION );
        serverTable.setId( "HiveTable" );
        clientsTable = createTableTemplate( CLIENT_TABLE_CAPTION );
        clientsTable.setId( "HiveClientsTable" );

        HorizontalLayout controlsContent = new HorizontalLayout();
        controlsContent.setSpacing( true );

        Label clusterNameLabel = new Label( "Select the cluster" );
        controlsContent.addComponent( clusterNameLabel );

        clusterCombo = new ComboBox();
        clusterCombo.setId( "HiveClusterCb" );
        clusterCombo.setImmediate( true );
        clusterCombo.setTextInputAllowed( false );
        clusterCombo.setWidth( 200, Sizeable.Unit.PIXELS );
        clusterCombo.addValueChangeListener( new Property.ValueChangeListener()
        {
            @Override
            public void valueChange( Property.ValueChangeEvent event )
            {
                config = ( OozieClusterConfig ) event.getProperty().getValue();
                refreshUI();
                checkServer();
            }
        } );

        /** Refresh Cluster Button */
        refreshClustersBtn = new Button( REFRESH_CLUSTERS_CAPTION );
        refreshClustersBtn.setId( "hiveRefreshClusterBtn" );
        refreshClustersBtn.addClickListener( new Button.ClickListener()
        {
            @Override
            public void buttonClick( Button.ClickEvent clickEvent )
            {
                refreshClustersInfo();
            }
        } );


        /** Destroy Cluster Button */
        destroyClusterBtn = new Button( DESTROY_CLUSTER_BUTTON_CAPTION );
        destroyClusterBtn.setId( "HiveDestroyClusterBtn" );
        addClickListenerToDestroyClusterButton();


        /** Add Node Button */
        addNodeBtn = new Button( ADD_NODE_BUTTON_CAPTION );
        addNodeBtn.setId( "HiveAddNodeBtn" );
        addClickListenerToAddNodeButton();


        addStyleNameToButtons( refreshClustersBtn, destroyClusterBtn, addNodeBtn );
        addGivenComponents( controlsContent, clusterCombo, refreshClustersBtn, destroyClusterBtn, addNodeBtn );
        controlsContent.setComponentAlignment( refreshClustersBtn, Alignment.MIDDLE_CENTER );
        controlsContent.setComponentAlignment( destroyClusterBtn, Alignment.MIDDLE_CENTER );
        controlsContent.setComponentAlignment( addNodeBtn, Alignment.MIDDLE_CENTER );

        VerticalLayout tablesLayout = new VerticalLayout();
        tablesLayout.setSizeFull();
        tablesLayout.setSpacing( true );

        addGivenComponents( tablesLayout, serverTable );
        addGivenComponents( tablesLayout, clientsTable );


        PROGRESS_ICON.setVisible( false );
        PROGRESS_ICON.setId( "indicator" );
        controlsContent.addComponent( PROGRESS_ICON );
        contentRoot.addComponent( controlsContent, 0, 0 );
        contentRoot.addComponent( tablesLayout, 0, 1, 0, 9 );
    }


    private void addClickListenerToAddNodeButton()
    {
        addNodeBtn.addClickListener( new Button.ClickListener()
        {
            @Override
            public void buttonClick( Button.ClickEvent clickEvent )
            {
                if ( config == null )
                {
                    show( "Select cluster" );
                    return;
                }
                HadoopClusterConfig hc = hadoop.getCluster( config.getHadoopClusterName() );
                if ( hc == null )
                {
                    show( String.format( "Hadoop cluster %s not found", config.getHadoopClusterName() ) );
                    return;
                }
                Set<String> set = new HashSet<>( hc.getAllNodes() );
                set.remove( config.getServer() );
                set.removeAll( config.getClients() );
                if ( set.isEmpty() )
                {
                    show( "All nodes in Hadoop cluster have Oozie installed" );
                    return;
                }

                Set<EnvironmentContainerHost> myHostSet = new HashSet<>();
                for ( String id : set )
                {
                    try
                    {
                        myHostSet.add( environmentManager.loadEnvironment(
                                hadoop.getCluster( config.getHadoopClusterName() ).getEnvironmentId() )
                                                         .getContainerHostById( id ) );
                    }
                    catch ( ContainerHostNotFoundException e )
                    {
                        LOGGER.error( "Container host not found", e );
                    }
                    catch ( EnvironmentNotFoundException e )
                    {
                        LOGGER.error( "Error getting environment by id: " + config.getEnvironmentId(), e );
                        return;
                    }
                }

                AddNodeWindow w = new AddNodeWindow( oozie, executorService, tracker, config, myHostSet );
                contentRoot.getUI().addWindow( w );
                w.addCloseListener( new Window.CloseListener()
                {
                    @Override
                    public void windowClose( Window.CloseEvent closeEvent )
                    {
                        refreshClustersInfo();
                        refreshUI();
                        checkServer();
                    }
                } );
            }
        } );
    }


    private void addClickListenerToDestroyClusterButton()
    {
        destroyClusterBtn.addClickListener( new Button.ClickListener()
        {
            @Override
            public void buttonClick( Button.ClickEvent clickEvent )
            {
                if ( config == null )
                {
                    show( "Select cluster" );
                    return;
                }
                ConfirmationDialog alert = new ConfirmationDialog(
                        String.format( "Cluster '%s' will be destroyed. Continue?", config.getClusterName() ), "Yes",
                        "No" );
                alert.getOk().addClickListener( new Button.ClickListener()
                {
                    @Override
                    public void buttonClick( Button.ClickEvent clickEvent )
                    {
                        destroyClusterHandler();
                    }
                } );

                contentRoot.getUI().addWindow( alert.getAlert() );
            }
        } );
    }


    private void destroyClusterHandler()
    {
        hadoop.getCluster( config.getClusterName() );
        UUID trackID = oozie.uninstallCluster( config.getClusterName() );

        ProgressWindow window = new ProgressWindow( executorService, tracker, trackID, OozieClusterConfig.PRODUCT_KEY );
        window.getWindow().addCloseListener( new Window.CloseListener()
        {
            @Override
            public void windowClose( Window.CloseEvent closeEvent )
            {
                refreshUI();
                refreshClustersInfo();
            }
        } );
        contentRoot.getUI().addWindow( window.getWindow() );
    }


    public void checkServer()
    {
        if ( serverTable != null )
        {
            for ( Object o : serverTable.getItemIds() )
            {
                int rowId = ( Integer ) o;
                Item row = serverTable.getItem( rowId );
                HorizontalLayout availableOperationsLayout =
                        ( HorizontalLayout ) ( row.getItemProperty( AVAILABLE_OPERATIONS_COLUMN_CAPTION ).getValue() );
                if ( availableOperationsLayout != null )
                {
                    Button checkBtn = getButton( availableOperationsLayout, CHECK_BUTTON_CAPTION );
                    if ( checkBtn != null )
                    {
                        checkBtn.click();
                    }
                }
            }
        }
    }


    protected Button getButton( final HorizontalLayout availableOperationsLayout, String caption )
    {
        if ( availableOperationsLayout == null )
        {
            return null;
        }
        else
        {
            for ( Component component : availableOperationsLayout )
            {
                if ( component.getCaption().equals( caption ) )
                {
                    return ( Button ) component;
                }
            }
            return null;
        }
    }


    private Table createTableTemplate( String caption )
    {
        final Table table = new Table( caption );
        table.addContainerProperty( HOST_COLUMN_CAPTION, String.class, null );
        table.addContainerProperty( IP_COLUMN_CAPTION, String.class, null );
        table.addContainerProperty( NODE_ROLE_COLUMN_CAPTION, String.class, null );
        table.addContainerProperty( AVAILABLE_OPERATIONS_COLUMN_CAPTION, HorizontalLayout.class, null );
        table.setSizeFull();
        table.setPageLength( 10 );
        table.setSelectable( false );
        table.setImmediate( true );
        addClickListenerToTable( table );
        return table;
    }


    private void addClickListenerToTable( final Table table )
    {
        table.addItemClickListener( new ItemClickEvent.ItemClickListener()
        {
            @Override
            public void itemClick( ItemClickEvent event )
            {
                String containerId =
                        ( String ) table.getItem( event.getItemId() ).getItemProperty( HOST_COLUMN_CAPTION ).getValue();
                EnvironmentContainerHost containerHost = null;
                try
                {
                    containerHost = environmentManager
                            .loadEnvironment( hadoop.getCluster( config.getHadoopClusterName() ).getEnvironmentId() )
                            .getContainerHostByHostname( containerId );
                }
                catch ( ContainerHostNotFoundException e )
                {
                    LOGGER.error( "Container host not found", e );
                }
                catch ( EnvironmentNotFoundException e )
                {
                    LOGGER.error( "Error getting environment by id: " + config.getEnvironmentId(), e );
                    return;
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
        } );
    }


    private void show( String notification )
    {
        Notification.show( notification );
    }


    public void refreshUI()
    {
        if ( config != null )
        {
            try
            {
                populateTable( serverTable,
                        getServers( environmentManager.loadEnvironment( config.getEnvironmentId() ).getContainerHosts(),
                                config ) );
            }
            catch ( EnvironmentNotFoundException e )
            {
                LOGGER.error( "Error getting environment by id: " + config.getEnvironmentId(), e );
                return;
            }
            try
            {
                populateTable( clientsTable,
                        getClients( environmentManager.loadEnvironment( config.getEnvironmentId() ).getContainerHosts(),
                                config ) );
            }
            catch ( EnvironmentNotFoundException e )
            {
                LOGGER.error( "Container host not found", e );
            }
        }
        else
        {
            serverTable.removeAllItems();
            clientsTable.removeAllItems();
        }
    }


    public Set<EnvironmentContainerHost> getServers( Set<EnvironmentContainerHost> containerHosts,
                                                     OozieClusterConfig config )
    {
        Set<EnvironmentContainerHost> list = new HashSet<>();
        for ( EnvironmentContainerHost containerHost : containerHosts )
        {
            if ( config.getServer().equals( containerHost.getId() ) )
            {
                list.add( containerHost );
            }
        }
        return list;
    }


    public Set<EnvironmentContainerHost> getClients( Set<EnvironmentContainerHost> containerHosts,
                                                     OozieClusterConfig config )
    {
        Set<EnvironmentContainerHost> list = new HashSet<>();
        for ( EnvironmentContainerHost containerHost : containerHosts )
        {
            if ( config.getClients().contains( containerHost.getId() ) )
            {
                list.add( containerHost );
            }
        }
        return list;
    }


    private void populateTable( final Table table, Set<EnvironmentContainerHost> containerHosts )
    {
        table.removeAllItems();

        for ( final EnvironmentContainerHost containerHost : containerHosts )
        {
            final Button checkBtn = new Button( CHECK_BUTTON_CAPTION );
            checkBtn.setId( containerHost.getIpByInterfaceName( "eth0" ) );
            final Button startBtn = new Button( START_BUTTON_CAPTION );
            startBtn.setId( containerHost.getIpByInterfaceName( "eth0" ) );
            final Button stopBtn = new Button( STOP_BUTTON_CAPTION );
            stopBtn.setId( containerHost.getIpByInterfaceName( "eth0" ) );
            final Button destroyBtn = new Button( DESTROY_BUTTON_CAPTION );
            destroyBtn.setId( containerHost.getIpByInterfaceName( "eth0" ) );

            addStyleNameToButtons( checkBtn, startBtn, stopBtn, destroyBtn );
            disableButtons( startBtn, stopBtn );

            final HorizontalLayout availableOperations = new HorizontalLayout();
            availableOperations.addStyleName( "default" );
            availableOperations.setSpacing( true );

            if ( isServer( containerHost ) )
            {
                addGivenComponents( availableOperations, checkBtn, startBtn, stopBtn );
            }
            else
            {
                addGivenComponents( availableOperations, destroyBtn );
                destroyBtn.addClickListener( new Button.ClickListener()
                {
                    @Override
                    public void buttonClick( Button.ClickEvent clickEvent )
                    {
                        ConfirmationDialog alert = new ConfirmationDialog(
                                String.format( "Do you want to destroy node  %s?", containerHost.getHostname() ), "Yes",
                                "No" );
                        alert.getOk().addClickListener( new Button.ClickListener()
                        {
                            @Override
                            public void buttonClick( Button.ClickEvent clickEvent )
                            {
                                UUID trackID =
                                        oozie.destroyNode( config.getClusterName(), containerHost.getHostname() );
                                ProgressWindow window = new ProgressWindow( executorService, tracker, trackID,
                                        OozieClusterConfig.PRODUCT_KEY );
                                window.getWindow().addCloseListener( new Window.CloseListener()
                                {
                                    @Override
                                    public void windowClose( Window.CloseEvent closeEvent )
                                    {
                                        refreshClustersInfo();
                                        refreshUI();
                                        checkServer();
                                    }
                                } );
                                contentRoot.getUI().addWindow( window.getWindow() );
                            }
                        } );

                        contentRoot.getUI().addWindow( alert.getAlert() );
                    }
                } );
            }

            table.addItem( new Object[] {
                    containerHost.getHostname(), containerHost.getIpByInterfaceName( "eth0" ),
                    checkNodeRole( containerHost ), availableOperations
            }, null );

            addClickListenerToCheckButton( containerHost, startBtn, stopBtn, checkBtn, destroyBtn );
            addClickListenerToStartButton( containerHost, startBtn, stopBtn, checkBtn, destroyBtn );
            addClickListenerToStopButton( containerHost, startBtn, stopBtn, checkBtn, destroyBtn );
        }
    }


    public String checkNodeRole( EnvironmentContainerHost agent )
    {

        if ( config.getServer().equals( agent.getId() ) )
        {
            return "Server";
        }
        else
        {
            return "Client";
        }
    }


    private boolean isServer( EnvironmentContainerHost agent )
    {
        return config.getServer().equals( agent.getId() );
    }


    private void addGivenComponents( Layout layout, Component... components )
    {
        for ( Component c : components )
        {
            layout.addComponent( c );
        }
    }


    private void addStyleNameToButtons( Button... buttons )
    {
        for ( Button b : buttons )
        {
            b.addStyleName( BUTTON_STYLE_NAME );
        }
    }


    private void disableButtons( Button... buttons )
    {
        for ( Button b : buttons )
        {
            b.setEnabled( false );
        }
    }


    private void addClickListenerToStopButton( final EnvironmentContainerHost containerHost, final Button... buttons )
    {
        getButton( STOP_BUTTON_CAPTION, buttons ).addClickListener( new Button.ClickListener()
        {
            @Override
            public void buttonClick( Button.ClickEvent clickEvent )
            {
                executorService.execute(
                        new OozieNodeOperationTask( oozie, tracker, config.getClusterName(), containerHost,
                                NodeOperationType.STOP, new CompleteEvent()
                        {

                            @Override
                            public void onComplete( final NodeState state )
                            {
                                getButton( CHECK_BUTTON_CAPTION, buttons ).setEnabled( true );
                                checkServer();
                            }
                        }, null ) );
            }
        } );
    }


    private void addClickListenerToStartButton( final EnvironmentContainerHost containerHost, final Button... buttons )
    {
        getButton( START_BUTTON_CAPTION, buttons ).addClickListener( new Button.ClickListener()
        {
            @Override
            public void buttonClick( Button.ClickEvent clickEvent )
            {
                disableButtons( buttons );
                executorService.execute(
                        new OozieNodeOperationTask( oozie, tracker, config.getClusterName(), containerHost,
                                NodeOperationType.START, new CompleteEvent()
                        {

                            @Override
                            public void onComplete( final NodeState state )
                            {
                                getButton( CHECK_BUTTON_CAPTION, buttons ).setEnabled( true );
                                checkServer();
                            }
                        }, null ) );
            }
        } );
    }


    private void addClickListenerToCheckButton( final EnvironmentContainerHost containerHost, final Button... buttons )
    {
        getButton( CHECK_BUTTON_CAPTION, buttons ).addClickListener( new Button.ClickListener()
        {
            @Override
            public void buttonClick( Button.ClickEvent clickEvent )
            {
                disableButtons( buttons );
                PROGRESS_ICON.setVisible( true );
                executorService.execute(
                        new OozieNodeOperationTask( oozie, tracker, config.getClusterName(), containerHost,
                                NodeOperationType.STATUS, new CompleteEvent()
                        {

                            @Override
                            public void onComplete( final NodeState state )
                            {
                                synchronized ( PROGRESS_ICON )
                                {
                                    if ( state == NodeState.RUNNING )
                                    {
                                        getButton( START_BUTTON_CAPTION, buttons ).setEnabled( false );
                                        getButton( STOP_BUTTON_CAPTION, buttons ).setEnabled( true );
                                    }
                                    else if ( state == NodeState.STOPPED )
                                    {
                                        getButton( START_BUTTON_CAPTION, buttons ).setEnabled( true );
                                        getButton( STOP_BUTTON_CAPTION, buttons ).setEnabled( false );
                                    }
                                    PROGRESS_ICON.setVisible( false );
                                    getButton( CHECK_BUTTON_CAPTION, buttons ).setEnabled( true );
                                    if ( getButton( DESTROY_BUTTON_CAPTION, buttons ) != null )
                                    {
                                        getButton( DESTROY_BUTTON_CAPTION, buttons ).setEnabled( true );
                                    }
                                }
                            }
                        }, null ) );
            }
        } );
    }


    private Button getButton( String caption, Button... buttons )
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
        List<OozieClusterConfig> clusters = oozie.getClusters();
        OozieClusterConfig clusterInfo = ( OozieClusterConfig ) clusterCombo.getValue();
        clusterCombo.removeAllItems();

        if ( clusters == null || clusters.isEmpty() )
        {
            PROGRESS_ICON.setVisible( false );
            return;
        }

        for ( OozieClusterConfig hiveConfig : clusters )
        {
            clusterCombo.addItem( hiveConfig );
            clusterCombo.setItemCaption( hiveConfig,
                    hiveConfig.getClusterName() + "(" + hiveConfig.getHadoopClusterName() + ")" );
        }

        if ( clusterInfo != null )
        {
            for ( OozieClusterConfig config : clusters )
            {
                if ( config.getClusterName().equals( clusterInfo.getClusterName() ) )
                {
                    clusterCombo.setValue( config );
                    return;
                }
            }
        }
        else
        {
            clusterCombo.setValue( clusters.iterator().next() );
        }
    }


    public Component getContent()
    {
        return contentRoot;
    }
}
