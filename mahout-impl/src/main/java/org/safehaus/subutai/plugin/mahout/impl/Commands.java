/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.safehaus.subutai.plugin.mahout.impl;


import java.util.Set;

import org.safehaus.subutai.common.enums.OutputRedirection;
import org.safehaus.subutai.common.protocol.Agent;
import org.safehaus.subutai.common.settings.Common;
import org.safehaus.subutai.core.command.api.command.Command;
import org.safehaus.subutai.core.command.api.command.CommandRunnerBase;
import org.safehaus.subutai.common.protocol.RequestBuilder;
import org.safehaus.subutai.plugin.mahout.api.MahoutClusterConfig;

import com.google.common.base.Preconditions;


public class Commands
{
    public static final String PACKAGE_NAME = Common.PACKAGE_PREFIX + MahoutClusterConfig.PRODUCT_KEY.toLowerCase();

    private final CommandRunnerBase commandRunnerBase;


    public Commands( final CommandRunnerBase commandRunnerBase )
    {
        Preconditions.checkNotNull( commandRunnerBase, "Command Runner is null" );

        this.commandRunnerBase = commandRunnerBase;
    }


    public Command getInstallCommand( Set<Agent> agents )
    {
        return commandRunnerBase.createCommand( "Install Mahout",
                new RequestBuilder( "apt-get --force-yes --assume-yes install " + PACKAGE_NAME ).withTimeout( 360 )
                                                                                                .withStdOutRedirection(
                                                                                                        OutputRedirection.NO ),
                agents );
    }


    public Command getUninstallCommand( Set<Agent> agents )
    {
        return commandRunnerBase.createCommand( "Uninstall Mahout",
                new RequestBuilder( "apt-get --force-yes --assume-yes purge " + PACKAGE_NAME ).withTimeout( 60 ),
                agents );
    }


    public Command getCheckInstalledCommand( Set<Agent> agents )
    {
        return commandRunnerBase.createCommand( "Check installed subutai packages",
                new RequestBuilder( "dpkg -l | grep '^ii' | grep " + Common.PACKAGE_PREFIX_WITHOUT_DASH ), agents );
    }
}
