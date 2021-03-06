package io.subutai.plugin.etl.ui;


import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import com.vaadin.data.Property;
import com.vaadin.ui.AbstractTextField;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import io.subutai.core.tracker.api.Tracker;
import io.subutai.plugin.etl.api.ETL;
import io.subutai.plugin.sqoop.api.DataSourceType;
import io.subutai.plugin.sqoop.api.Sqoop;
import io.subutai.plugin.sqoop.api.setting.ImportParameter;
import io.subutai.plugin.sqoop.api.setting.ImportSetting;


public class ImportPanel extends ImportExportBase
{

    private final ETL etl;
    private final Sqoop sqoop;
    private final ExecutorService executorService;
    DataSourceType type;
    CheckBox chkImportAllTables = new CheckBox( "Import all tables" );
    AbstractTextField hbaseTableNameField = UIUtil.getTextField( "Table name:" );
    AbstractTextField hbaseColumnFamilyField = UIUtil.getTextField( "Column family:" );
    AbstractTextField hiveDatabaseField = UIUtil.getTextField( "Database:" );
    AbstractTextField hiveTableNameField = UIUtil.getTextField( "Table name:" );
    private ComboBox databases = UIUtil.getComboBox( "Databases" );
    private ComboBox tables = UIUtil.getComboBox( "Tables" );
    private ProgressBar progressIconDB = UIUtil.getProgressIcon();
    private ProgressBar progressIconTable = UIUtil.getProgressIcon();
    public ProgressBar progressIcon = UIUtil.getProgressIcon();


    public ImportPanel( ETL etl, Sqoop sqoop, ExecutorService executorService, Tracker tracker )
    {
        super( tracker );
        this.etl = etl;
        this.sqoop = sqoop;
        this.executorService = executorService;

        init();
    }


    public DataSourceType getType()
    {
        return type;
    }


    public void setType( DataSourceType type )
    {
        this.type = type;
        init();
    }


    @Override
    public ImportSetting makeSettings()
    {
        ImportSetting s = new ImportSetting();
        s.setType( type );
        s.setClusterName( UIUtil.findSqoopClusterName( sqoop, host.getId() ) );
        s.setHostname( host.getHostname() );

        if ( databases.getValue() == null )
        {
            s.setConnectionString( connStringField.getValue() );
        }
        else
        {
            if ( connStringField.getValue().endsWith( "/" ) )
            {
                s.setConnectionString( connStringField.getValue() + databases.getValue().toString() );
            }
            else
            {
                s.setConnectionString( connStringField.getValue() + "/" + databases.getValue().toString() );
            }
        }

        if ( tables.getValue() == null )
        {
            s.setTableName( connStringField.getValue() );
        }
        else
        {
            s.setTableName( tables.getValue().toString() );
        }
        s.setUsername( usernameField.getValue() );
        s.setPassword( passwordField.getValue() );
        s.setOptionalParameters( optionalParams.getValue() );
        switch ( type )
        {
            case HDFS:
                s.addParameter( ImportParameter.IMPORT_ALL_TABLES, chkImportAllTables.getValue() );
                break;
            case HBASE:
                s.addParameter( ImportParameter.DATASOURCE_TABLE_NAME, hbaseTableNameField.getValue() );
                s.addParameter( ImportParameter.DATASOURCE_COLUMN_FAMILY, hbaseColumnFamilyField.getValue() );
                break;
            case HIVE:
                s.addParameter( ImportParameter.DATASOURCE_DATABASE, hiveDatabaseField.getValue() );
                s.addParameter( ImportParameter.DATASOURCE_TABLE_NAME, hiveTableNameField.getValue() );
                break;
            default:
                throw new AssertionError( type.name() );
        }
        return s;
    }


    final void init()
    {
        removeAllComponents();

        if ( type == null )
        {
            type = DataSourceType.HDFS;
        }

        VerticalLayout layout = new VerticalLayout();

        layout.addComponent( UIUtil.getLabel( "Select data source type<br/>", 200 ) );

        TabSheet tabsheet = new TabSheet();

        VerticalLayout tab1 = new VerticalLayout();
        tab1.setCaption( DataSourceType.HDFS.name() );

        tabsheet.addTab( tab1 );

        VerticalLayout tab2 = new VerticalLayout();
        tab2.setCaption( DataSourceType.HBASE.name() );
        tabsheet.addTab( tab2 );

        VerticalLayout tab3 = new VerticalLayout();
        tab3.setCaption( DataSourceType.HIVE.name() );
        tabsheet.addTab( tab3 );

        switch ( type )
        {
            case HDFS:
                tabsheet.setSelectedTab( tab1 );
                break;
            case HBASE:
                tabsheet.setSelectedTab( tab2 );
                break;
            case HIVE:
                tabsheet.setSelectedTab( tab3 );
                break;
        }

        tabsheet.addSelectedTabChangeListener( new TabSheet.SelectedTabChangeListener()
        {
            @Override
            public void selectedTabChange( TabSheet.SelectedTabChangeEvent event )
            {
                TabSheet tabsheet = event.getTabSheet();
                String caption = tabsheet.getTab( event.getTabSheet().getSelectedTab() ).getCaption();
                if ( caption.equals( DataSourceType.HDFS.name() ) )
                {
                    type = DataSourceType.HDFS;
                    init();
                }
                else if ( caption.equals( DataSourceType.HBASE.name() ) )
                {
                    type = DataSourceType.HBASE;
                    init();
                }
                else if ( caption.equals( DataSourceType.HIVE.name() ) )
                {
                    type = DataSourceType.HIVE;
                    init();
                }
            }
        } );
        layout.addComponent( tabsheet );

        addComponent( layout );

        super.init();
        chkImportAllTables.addValueChangeListener( new Property.ValueChangeListener()
        {
            @Override
            public void valueChange( Property.ValueChangeEvent e )
            {
                String v = e.getProperty().getValue().toString();
                tableField.setEnabled( !Boolean.parseBoolean( v ) );
                tables.setEnabled( !Boolean.parseBoolean( v ) );
            }
        } );

        HorizontalLayout buttons = new HorizontalLayout();
        buttons.setSpacing( true );
        buttons.addComponent( UIUtil.getButton( "Review Query", new Button.ClickListener()
        {

            @Override
            public void buttonClick( final Button.ClickEvent event )
            {
                if ( host.getId() == null )
                {
                    Notification.show( "Please select sqoop node!" );
                    return;
                }
                ImportSetting es = makeSettings();
                es.setPassword( "***" );
                String cmd = sqoop.reviewImportQuery( es );
                Notification.show( cmd );
            }
        } ) );

        buttons.addComponent( UIUtil.getButton( "Import", new Button.ClickListener()
        {
            @Override
            public void buttonClick( Button.ClickEvent event )
            {
                clearLogMessages();
                if ( !checkFields() )
                {
                    return;
                }
                progressIcon.setVisible( true );
                final ImportSetting sett = makeSettings();
                executorService.execute( new Runnable()
                {
                    @Override
                    public void run()
                    {
                        final UUID trackId = sqoop.importData( sett );
                        OperationWatcher watcher = new OperationWatcher( trackId );
                        watcher.setCallback( new OperationCallback()
                        {
                            @Override
                            public void onComplete()
                            {
                                progressIcon.setVisible( false );
                            }
                        } );
                        executorService.execute( watcher );
                    }
                } );
            }
        } ) );

        //        buttons.addComponent( UIUtil.getButton( "Back", new Button.ClickListener()
        //        {
        //            @Override
        //            public void buttonClick( Button.ClickEvent event )
        //            {
        //                reset();
        //                setType( null );
        //            }
        //        } ) );

        buttons.addComponent( progressIcon );

        HorizontalLayout dbLayout = new HorizontalLayout();
        dbLayout.setSpacing( true );
        dbLayout.addComponent( databases );
        final Button fetchDB = new Button( "Fetch" );
        fetchDB.addStyleName( "default" );
        dbLayout.addComponent( fetchDB );
        dbLayout.setComponentAlignment( fetchDB, Alignment.BOTTOM_CENTER );
        dbLayout.addComponent( progressIconDB );
        dbLayout.setComponentAlignment( progressIconDB, Alignment.BOTTOM_CENTER );

        fetchDB.addClickListener( new Button.ClickListener()
        {
            @Override
            public void buttonClick( final Button.ClickEvent clickEvent )
            {
                if ( host == null )
                {
                    Notification.show( "Please select sqoop node!" );
                    return;
                }
                if ( connStringField.getValue().isEmpty() )
                {
                    Notification.show( "Please enter connection string!" );
                    return;
                }
                if ( passwordField.getValue().isEmpty() )
                {
                    Notification.show( "Please enter your password!" );
                    return;
                }

                progressIconDB.setVisible( true );
                // fetchDB.setEnabled(false);
                executorService.execute( new Runnable()
                {
                    @Override
                    public void run()
                    {
                        databases.removeAllItems();
                        ImportSetting importSettings = makeSettings();
                        String databaseList = sqoop.fetchDatabases( importSettings );
                        ArrayList<String> dbItems = clearResult( databaseList );
                        if ( dbItems.isEmpty() )
                        {

                            Notification.show( "Cannot fetch any database. Check your connection details !!!" );
                            progressIconDB.setVisible( false );
                            fetchDB.setEnabled( true );
                            return;
                        }

                        Notification.show( "Fetched " + dbItems.size() + " databases." );

                        for ( String dbItem : dbItems )
                        {
                            databases.addItem( dbItem );
                        }
                        UI.getCurrent().access( new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                // fetchDB.setEnabled(true);
                                progressIconDB.setVisible( false );
                            }
                        } );
                    }
                } );
            }
        } );


        HorizontalLayout tableLayout = new HorizontalLayout();
        tableLayout.setSpacing( true );
        tableLayout.addComponent( tables );
        final Button fetchTables = new Button( "Fetch" );
        fetchTables.addStyleName( "default" );
        tableLayout.addComponent( fetchTables );
        tableLayout.setComponentAlignment( fetchTables, Alignment.BOTTOM_CENTER );
        tableLayout.addComponent( progressIconTable );
        tableLayout.setComponentAlignment( progressIconTable, Alignment.BOTTOM_CENTER );
        fetchTables.addClickListener( new Button.ClickListener()
        {
            @Override
            public void buttonClick( final Button.ClickEvent clickEvent )
            {
                if ( host == null )
                {
                    Notification.show( "Please select sqoop node!" );
                    return;
                }
                if ( connStringField.getValue().isEmpty() )
                {
                    Notification.show( "Please enter connection string!" );
                    return;
                }
                if ( passwordField.getValue().isEmpty() )
                {
                    Notification.show( "Please enter your password!" );
                    return;
                }
                if ( databases.getValue() == null )
                {
                    Notification.show( "Please select database first!" );
                    return;
                }
                progressIconTable.setVisible( true );
                // fetchTables.setEnabled(false);

                executorService.execute( new Runnable()
                {
                    @Override
                    public void run()
                    {
                        tables.removeAllItems();
                        ImportSetting importSettings = makeSettings();
                        String tableList = sqoop.fetchTables( importSettings );
                        ArrayList<String> tableItems = clearResult( tableList );
                        if ( tableItems.isEmpty() )
                        {
                            Notification.show( "Cannot fetch any table. Check your connection details !!!" );
                            progressIconTable.setVisible( false );
                            return;
                        }
                        Notification.show( "Fetched " + tableItems.size() + " tables." );

                        for ( String tableItem : tableItems )
                        {
                            tables.addItem( tableItem );
                        }

                        UI.getCurrent().access( new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                progressIconTable.setVisible( false );
                                // fetchTables.setEnabled(true);
                            }
                        } );
                    }
                } );
            }
        } );

        List<Component> ls = new ArrayList<>();
        ls.add( UIUtil.getLabel( "<h1>Sqoop Import</h1>", Unit.PERCENTAGE ) );
        ls.add( UIUtil.getLabel( "<h1>" + type.toString() + "</h1>", 200 ) );
        ls.add( connStringField );
        ls.add( usernameField );
        ls.add( passwordField );
        ls.add( dbLayout );
        ls.add( tableLayout );

        switch ( type )
        {
            case HDFS:
                ls.add( 6, chkImportAllTables );
                this.fields.add( chkImportAllTables );
                break;
            case HBASE:
                ls.add( UIUtil.getLabel( "<b>HBase parameters</b>", 200 ) );
                ls.add( hbaseTableNameField );
                ls.add( hbaseColumnFamilyField );
                this.fields.add( hbaseTableNameField );
                this.fields.add( hbaseColumnFamilyField );
                break;
            case HIVE:
                ls.add( 3, chkImportAllTables );
                ls.add( UIUtil.getLabel( "<b>Hive parameters</b>", 200 ) );
                ls.add( hiveDatabaseField );
                ls.add( hiveTableNameField );
                this.fields.add( chkImportAllTables );
                this.fields.add( hiveDatabaseField );
                this.fields.add( hiveTableNameField );
                break;
            default:
                throw new AssertionError( type.name() );
        }
        ls.add( optionalParams );
        ls.add( buttons );

        addComponentsVertical( ls );
    }


    /**
     * Sqoop query message contains some unrelated sentences along with tables list. That's why we need to revoke this
     * method to filter unrelated sentences from query output.
     *
     * @param s sqoop query output
     */
    private ArrayList<String> clearResult( String s )
    {
        ArrayList<String> list = new ArrayList<>();
        String result[] = s.split( "\n" );
        for ( String part : result )
        {
            if ( !part.endsWith( "." ) )
            {
                list.add( part );
            }
        }
        return list;
    }


    @Override
    boolean checkFields()
    {
        if ( super.checkFields() )
        {
            switch ( type )
            {
                case HDFS:
                    if ( !isChecked( chkImportAllTables ) )
                    {
                        if ( !hasValue( tables, "Table name not specified" ) )
                        {
                            return false;
                        }
                    }
                    break;
                case HBASE:
                    if ( !hasValue( hbaseTableNameField, "HBase table name not specified" ) )
                    {
                        return false;
                    }
                    if ( !hasValue( hbaseColumnFamilyField, "HBase column family not specified" ) )
                    {
                        return false;
                    }
                    break;
                case HIVE:
                    if ( !isChecked( chkImportAllTables ) )
                    {
                        if ( !hasValue( tables, "Table name not specified" ) )
                        {
                            return false;
                        }
                    }
                    if ( !hasValue( hiveDatabaseField, "Hive database not specified" ) )
                    {
                        return false;
                    }
                    if ( !hasValue( hiveTableNameField, "Hive table name not specified" ) )
                    {
                        return false;
                    }
                    break;
                default:
                    throw new AssertionError( type.name() );
            }
            return true;
        }
        return false;
    }


    private boolean isChecked( CheckBox chb )
    {
        Object v = chb.getValue();
        return v != null && Boolean.parseBoolean( v.toString() );
    }
}
