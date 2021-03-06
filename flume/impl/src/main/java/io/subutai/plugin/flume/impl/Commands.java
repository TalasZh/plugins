package io.subutai.plugin.flume.impl;


import io.subutai.common.command.OutputRedirection;
import io.subutai.common.command.RequestBuilder;
import io.subutai.common.settings.Common;
import io.subutai.plugin.flume.api.FlumeConfig;


public class Commands
{

    public static final String PACKAGE_NAME = Common.PACKAGE_PREFIX + FlumeConfig.PRODUCT_KEY.toLowerCase();


    public static String make( CommandType type )
    {
        switch ( type )
        {
            case STATUS:
                return "dpkg -l | grep '^ii' | grep " + Common.PACKAGE_PREFIX_WITHOUT_DASH;
            case INSTALL:
            case PURGE:
                return "apt-get --force-yes --assume-yes " + type.toString().toLowerCase() + " " + PACKAGE_NAME;
            case START:
            case STOP:
                String s = "service flume-ng " + type.toString().toLowerCase() + " agent";
                if ( type == CommandType.START )
                {
                    s += " &"; // TODO:
                }
                return s;
            case SERVICE_STATUS:
                return "ps axu | grep [f]lume";
            default:
                throw new AssertionError( type.name() );
        }
    }


    public static RequestBuilder getAptUpdate()
    {
        return new RequestBuilder( "apt-get --force-yes --assume-yes update" ).withTimeout( 2000 )
                                                                              .withStdOutRedirection(
                                                                                      OutputRedirection.NO );
    }
}
