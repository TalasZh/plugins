package io.subutai.plugin.lucene.cli;


import java.util.List;

import io.subutai.plugin.lucene.api.Lucene;
import io.subutai.plugin.lucene.api.LuceneConfig;

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;

/**
 * sample command : lucene:list-clusters
 */
@Command( scope = "lucene", name = "list-clusters", description = "Lists Lucene clusters" )
public class ListClustersCommand extends OsgiCommandSupport
{

    private Lucene luceneManager;


    public Lucene getLuceneManager()
    {
        return luceneManager;
    }


    public void setLuceneManager( Lucene luceneManager )
    {
        this.luceneManager = luceneManager;
    }


    protected Object doExecute()
    {
        List<LuceneConfig> configList = luceneManager.getClusters();
        if ( !configList.isEmpty() )
        {
            for ( LuceneConfig config : configList )
            {
                System.out.println( config.getClusterName() );
            }
        }
        else
        {
            System.out.println( "No Lucene cluster" );
        }

        return null;
    }
}
