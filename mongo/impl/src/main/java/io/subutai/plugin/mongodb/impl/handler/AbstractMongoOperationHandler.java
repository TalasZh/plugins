package io.subutai.plugin.mongodb.impl.handler;


import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import io.subutai.common.command.CommandException;
import io.subutai.common.command.CommandResult;
import io.subutai.common.command.CommandUtil;
import io.subutai.common.command.RequestBuilder;
import io.subutai.common.peer.EnvironmentContainerHost;
import io.subutai.common.tracker.OperationState;
import io.subutai.common.tracker.TrackerOperation;
import io.subutai.core.plugincommon.api.AbstractOperationHandler;
import io.subutai.core.plugincommon.api.ApiBase;
import io.subutai.core.plugincommon.api.ConfigBase;
import io.subutai.plugin.mongodb.impl.common.CommandDef;


public abstract class AbstractMongoOperationHandler<T extends ApiBase<V>, V extends ConfigBase>
        extends AbstractOperationHandler<T, V>
{

    private static final Logger LOGGER = LoggerFactory.getLogger( AbstractMongoOperationHandler.class );
    private CommandUtil commandUtil = new CommandUtil();


    public AbstractMongoOperationHandler( final T manager, final V config )
    {
        super( manager, config );
    }


    /**
     * @deprecated
     */
    public AbstractMongoOperationHandler( final T manager, final String clusterName )
    {
        super( manager, clusterName );
    }


    public void logResults( TrackerOperation po, List<CommandResult> commandResultList )
    {
        Preconditions.checkNotNull( commandResultList );
        for ( CommandResult commandResult : commandResultList )
        {
            po.addLog( commandResult.getStdOut() );
        }
        if ( po.getState() == OperationState.FAILED )
        {
            po.addLogFailed( "" );
        }
        else
        {
            po.addLogDone( "" );
        }
    }


    public CommandResult executeCommand( CommandDef commandBuilder, EnvironmentContainerHost containerHost )
    {
        CommandResult commandResult = null;
        try
        {
            commandResult = commandUtil.execute(
                    new RequestBuilder( commandBuilder.getCommand() ).withTimeout( commandBuilder.getTimeout() ),
                    containerHost );
        }
        catch ( CommandException e )
        {
            LOGGER.error(
                    String.format( "Error executing command: %s on container host: %s", commandBuilder.getCommand(),
                            containerHost.getHostname() ), e );
        }
        return commandResult;
    }
}
