package io.subutai.plugin.sqoop.ui.manager;


import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import javax.naming.NamingException;

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
import com.vaadin.ui.Window;

import io.subutai.common.environment.ContainerHostNotFoundException;
import io.subutai.common.environment.Environment;
import io.subutai.common.environment.EnvironmentNotFoundException;
import io.subutai.common.peer.EnvironmentContainerHost;
import io.subutai.core.environment.api.EnvironmentManager;
import io.subutai.core.tracker.api.Tracker;
import io.subutai.plugin.hadoop.api.Hadoop;
import io.subutai.plugin.sqoop.api.Sqoop;
import io.subutai.plugin.sqoop.api.SqoopConfig;
import io.subutai.plugin.sqoop.ui.SqoopComponent;
import io.subutai.server.ui.component.ConfirmationDialog;
import io.subutai.server.ui.component.ProgressWindow;
import io.subutai.server.ui.component.TerminalWindow;


public class Manager
{
    protected static final String AVAILABLE_OPERATIONS_COLUMN_CAPTION = "AVAILABLE_OPERATIONS";
    protected static final String REFRESH_CLUSTERS_CAPTION = "Refresh Clusters";
    protected static final String DESTROY_ALL_INSTALLATIONS_BUTTON_CAPTION = "Destroy All Installations";
    protected static final String ADD_NODE_BUTTON_CAPTION = "Add Node";
    protected static final String HOST_COLUMN_CAPTION = "Host";
    protected static final String IP_COLUMN_CAPTION = "IP List";
    protected static final String BUTTON_STYLE_NAME = "default";

    private final Button refreshClustersBtn, destroyClusterBtn;
    private final GridLayout contentRoot;
    private final ComboBox clusterCombo;
    private final Table nodesTable;
    private final ImportPanel importPanel;
    private final ExportPanel exportPanel;
    private final Sqoop sqoop;
    private final ExecutorService executorService;
    private final Tracker tracker;
    private final EnvironmentManager environmentManager;
    private final SqoopComponent sqoopComponent;
    private final Embedded PROGRESS_ICON = new Embedded( "", new ThemeResource( "img/spinner.gif" ) );

    private SqoopConfig config;
    private Environment environment;
    private Hadoop hadoop;


    public Manager( ExecutorService executorService, Sqoop sqoop, Hadoop hadoop, Tracker tracker,
                    EnvironmentManager environmentManager, SqoopComponent sqoopComponent ) throws NamingException
    {

        this.executorService = executorService;
        this.sqoopComponent = sqoopComponent;
        this.sqoop = sqoop;
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
        nodesTable = createTableTemplate( "Nodes" );
        nodesTable.setId( "sqoopMngNodesTable" );
        contentRoot.setId( "sqoopMngContentRoot" );


        HorizontalLayout controlsContent = new HorizontalLayout();
        controlsContent.setSpacing( true );

        Label clusterNameLabel = new Label( "Select Sqoop installation:" );
        controlsContent.addComponent( clusterNameLabel );


        /** Combo box */
        clusterCombo = new ComboBox();
        clusterCombo.setId( "sqoopMngClusterCombo" );
        clusterCombo.setImmediate( true );
        clusterCombo.setTextInputAllowed( false );
        clusterCombo.setWidth( 200, Sizeable.Unit.PIXELS );
        clusterCombo.addValueChangeListener( new Property.ValueChangeListener()
        {
            @Override
            public void valueChange( Property.ValueChangeEvent event )
            {
                config = ( SqoopConfig ) event.getProperty().getValue();
                refreshUI();
            }
        } );
        controlsContent.addComponent( clusterCombo );
        controlsContent.setComponentAlignment( clusterCombo, Alignment.MIDDLE_CENTER );


        refreshClustersBtn = new Button( REFRESH_CLUSTERS_CAPTION );
        refreshClustersBtn.setId( "sqoopMngRefresh" );
        refreshClustersBtn.addStyleName( "default" );
        refreshClustersBtn.addClickListener( new Button.ClickListener()
        {
            @Override
            public void buttonClick( Button.ClickEvent event )
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


        /** Destroy Cluster button */
        destroyClusterBtn = new Button( DESTROY_ALL_INSTALLATIONS_BUTTON_CAPTION );
        destroyClusterBtn.setId( "sqoopMngDestroy" );
        destroyClusterBtn.addStyleName( "default" );
        addClickListenerToDestorClusterButton();
        controlsContent.addComponent( destroyClusterBtn );

        PROGRESS_ICON.setVisible( false );
        PROGRESS_ICON.setId( "indicator" );
        controlsContent.addComponent( PROGRESS_ICON );

        contentRoot.addComponent( controlsContent, 0, 0 );
        contentRoot.addComponent( nodesTable, 0, 1, 0, 9 );

        importPanel = new ImportPanel( sqoop, executorService, tracker );
        exportPanel = new ExportPanel( sqoop, executorService, tracker );
    }


    private void addClickListenerToDestorClusterButton()
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
                            UUID trackID = sqoop.uninstallCluster( config.getClusterName() );
                            ProgressWindow window =
                                    new ProgressWindow( executorService, tracker, trackID, SqoopConfig.PRODUCT_KEY );
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
        table.setColumnCollapsingAllowed( true );

        addItemClickListenerToTable( table );
        return table;
    }


    private void addItemClickListenerToTable( final Table table )
    {
        table.addItemClickListener( new ItemClickEvent.ItemClickListener()
        {
            @Override
            public void itemClick( ItemClickEvent event )
            {
                if ( event.isDoubleClick() )
                {
                    String hostname =
                            ( String ) table.getItem( event.getItemId() ).getItemProperty( "Host" ).getValue();
                    EnvironmentContainerHost host = null;
                    try
                    {
                        host = environment.getContainerHostByHostname( hostname );
                    }
                    catch ( ContainerHostNotFoundException e )
                    {
                        e.printStackTrace();
                    }
                    if ( host != null && host.isConnected() )
                    {
                        TerminalWindow terminal = new TerminalWindow( host );
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
            try
            {
                environment = environmentManager.loadEnvironment( config.getEnvironmentId() );

                Set<EnvironmentContainerHost> nodes = null;
                try
                {
                    nodes = environment.getContainerHostsByIds( config.getNodes() );
                }
                catch ( ContainerHostNotFoundException e )
                {
                    e.printStackTrace();
                }
                populateTable( nodesTable, nodes );
            }
            catch ( EnvironmentNotFoundException e )
            {
                e.printStackTrace();
            }
        }
        else
        {
            nodesTable.removeAllItems();
        }
    }


    private void addStyleNameToButtons( Button... buttons )
    {
        for ( Button b : buttons )
        {
            b.addStyleName( BUTTON_STYLE_NAME );
        }
    }


    private void addGivenComponents( Layout layout, Button... buttons )
    {
        for ( Button b : buttons )
        {
            layout.addComponent( b );
        }
    }


    private void populateTable( final Table table, Collection<EnvironmentContainerHost> nodes )
    {

        table.removeAllItems();

        for ( final EnvironmentContainerHost node : nodes )
        {
            String ip = getIPofHost( node );

            final Button importBtn = new Button( "Import" );
            importBtn.setId( ip + "-sqoopImport" );
            final Button exportBtn = new Button( "Export" );
            exportBtn.setId( ip + "-sqoopExport" );
            final Button destroyBtn = new Button( "Destroy" );
            destroyBtn.setId( ip + "-sqoopDestroy" );

            addStyleNameToButtons( importBtn, exportBtn, destroyBtn );

            HorizontalLayout availableOperations = new HorizontalLayout();
            availableOperations.setSpacing( true );
            availableOperations.addStyleName( "default" );

            addGivenComponents( availableOperations, importBtn, exportBtn, destroyBtn );

            table.addItem( new Object[] {
                    node.getHostname(), ip, availableOperations
            }, null );


            importBtn.addClickListener( new Button.ClickListener()
            {
                @Override
                public void buttonClick( Button.ClickEvent event )
                {
                    importPanel.setHost( node );
                    importPanel.setType( null );
                    importPanel.setClusterName( config.getClusterName() );
                    sqoopComponent.addTab( importPanel );
                }
            } );

            exportBtn.addClickListener( new Button.ClickListener()
            {
                @Override
                public void buttonClick( Button.ClickEvent event )
                {
                    exportPanel.setHost( node );
                    exportPanel.setClusterName( config.getClusterName() );
                    sqoopComponent.addTab( exportPanel );
                }
            } );

            addClickListenerToDestroyButton( node, destroyBtn );
        }
    }


    private String getIPofHost( EnvironmentContainerHost host )
    {
        return host.getIpByInterfaceName( "eth0" );
    }


    private void addClickListenerToDestroyButton( final EnvironmentContainerHost node, Button destroyBtn )
    {
        destroyBtn.addClickListener( new Button.ClickListener()
        {

            @Override
            public void buttonClick( Button.ClickEvent event )
            {

                if ( config.getNodes().size() == 1 )
                {
                    show( "This is the last sqoop node in the cluster, please destroy whole cluster!" );
                    return;
                }
                ConfirmationDialog alert = new ConfirmationDialog(
                        String.format( "Do you want to destroy the %s node?", node.getHostname() ), "Yes", "No" );
                alert.getOk().addClickListener( new Button.ClickListener()
                {
                    @Override
                    public void buttonClick( Button.ClickEvent clickEvent )
                    {
                        UUID trackId = sqoop.destroyNode( config.getClusterName(), node.getHostname() );
                        ProgressWindow window =
                                new ProgressWindow( executorService, tracker, trackId, SqoopConfig.PRODUCT_KEY );
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


    public void refreshClustersInfo()
    {
        List<SqoopConfig> clusters = sqoop.getClusters();
        SqoopConfig clusterInfo = ( SqoopConfig ) clusterCombo.getValue();
        clusterCombo.removeAllItems();

        if ( clusters == null || clusters.isEmpty() )
        {
            PROGRESS_ICON.setVisible( false );
            return;
        }

        for ( SqoopConfig sqoopConfig : clusters )
        {
            clusterCombo.addItem( sqoopConfig );
            clusterCombo.setItemCaption( sqoopConfig,
                    sqoopConfig.getClusterName() + "(" + sqoopConfig.getHadoopClusterName() + ")" );
        }

        if ( clusterInfo != null )
        {
            for ( SqoopConfig esConfig : clusters )
            {
                if ( esConfig.getClusterName().equals( clusterInfo.getClusterName() ) )
                {
                    clusterCombo.setValue( esConfig );
                    PROGRESS_ICON.setVisible( false );
                    return;
                }
            }
        }
        else
        {
            clusterCombo.setValue( clusters.iterator().next() );
            PROGRESS_ICON.setVisible( false );
        }
    }


    public Component getContent()
    {
        return contentRoot;
    }
}

