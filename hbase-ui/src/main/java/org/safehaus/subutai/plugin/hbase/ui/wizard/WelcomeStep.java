/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.safehaus.subutai.plugin.hbase.ui.wizard;


import org.safehaus.subutai.common.util.FileUtil;
import org.safehaus.subutai.plugin.hbase.api.SetupType;
import org.safehaus.subutai.plugin.hbase.ui.HBasePortalModule;

import com.vaadin.server.FileResource;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;


public class WelcomeStep extends Panel
{

    public WelcomeStep( final Wizard wizard )
    {

        setSizeFull();

        GridLayout grid = new GridLayout( 10, 6 );
        grid.setSpacing( true );
        grid.setMargin( true );
        grid.setSizeFull();

        Label welcomeMsg = new Label( "<center><h2>Welcome to Spark Installation Wizard!</h2>" );
        welcomeMsg.setContentMode( ContentMode.HTML );
        grid.addComponent( welcomeMsg, 3, 1, 6, 2 );

        Label logoImg = new Label();
        logoImg.setIcon( new FileResource( FileUtil.getFile( HBasePortalModule.MODULE_IMAGE, this ) ) );
        logoImg.setContentMode( ContentMode.HTML );
        logoImg.setHeight( 100, Unit.PIXELS );
        logoImg.setWidth( 192, Unit.PIXELS );
        grid.addComponent( logoImg, 1, 3, 2, 5 );

        Button next = new Button( "Start over-Hadoop installation" );
        next.setId( "HbaseStartOverHadoop" );
        next.addStyleName( "default" );
        next.addClickListener( new NextClickHandler( wizard, SetupType.OVER_HADOOP ) );
        grid.addComponent( next, 4, 4, 4, 4 );
        grid.setComponentAlignment( next, Alignment.BOTTOM_RIGHT );

        Button next2 = new Button( "Start with-Hadoop installation" );
        next2.setId( "HbaseStartWithHadoop" );
        next2.addStyleName( "default" );
        next2.addClickListener( new NextClickHandler( wizard, SetupType.WITH_HADOOP ) );
        grid.addComponent( next2, 5, 4, 5, 4 );
        grid.setComponentAlignment( next2, Alignment.BOTTOM_RIGHT );

        setContent( grid );
    }


    private class NextClickHandler implements Button.ClickListener
    {

        private final Wizard wizard;
        private final SetupType setupType;


        public NextClickHandler( Wizard wizard, SetupType setupType )
        {
            this.wizard = wizard;
            this.setupType = setupType;
        }


        @Override
        public void buttonClick( Button.ClickEvent clickEvent )
        {
            wizard.init();
            wizard.getConfig().setSetupType( setupType );
            wizard.next();
        }
    }
}