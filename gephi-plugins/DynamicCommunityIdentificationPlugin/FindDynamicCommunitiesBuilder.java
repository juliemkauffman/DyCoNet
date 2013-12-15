
package kcl.julie.DynamicCommunityIdentificationPlugin;

import org.gephi.statistics.spi.Statistics;
import org.gephi.statistics.spi.StatisticsBuilder;
import org.openide.util.lookup.ServiceProvider;

/*
 * @author Julie Kauffman
 * Adapted from Gephi module ModularityBuilder.java and Gephi 
 * Developer Bootcamp module CountSelfLoopBuilder.java, both of which are
 * available on Github.
 */

@ServiceProvider(service = StatisticsBuilder.class)
public class FindDynamicCommunitiesBuilder implements StatisticsBuilder
{
    @Override
    public String getName()
    {
        String message = "Find Dynamic Communities";
        return message;
    }
    
    @Override
    public Statistics getStatistics()
    {    return new FindDynamicCommunities();}
    
    @Override
    public Class<? extends Statistics> getStatisticsClass()
    {    return FindDynamicCommunities.class;}    
}
