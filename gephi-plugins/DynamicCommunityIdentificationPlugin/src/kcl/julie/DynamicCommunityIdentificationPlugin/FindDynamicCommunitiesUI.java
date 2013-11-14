package kcl.julie.DynamicCommunityIdentificationPlugin;

import javax.swing.JPanel;
import org.gephi.statistics.spi.Statistics;
import org.gephi.statistics.spi.StatisticsUI;
import org.openide.util.lookup.ServiceProvider;

/*
 * @author Julie Kauffman
 * Adapted from Gephi Developer Bootcamp modules CoutsSelfLoopUI.java and 
 * Gephi module PageRankUI.java, both of which are available on Github.
 */

@ServiceProvider(service = StatisticsUI.class)
public class FindDynamicCommunitiesUI implements StatisticsUI
{
    private final StatSettings settings = new StatSettings();
    private FindDynamicCommunities dynamicCommunitiesStatistic;
    private FindDynamicCommunitiesPanel panel;

    @Override
    public JPanel getSettingsPanel() 
    {
        panel = new FindDynamicCommunitiesPanel();
        return panel;
    }

    @Override
    public void setup(Statistics ststcs) 
    {
        this.dynamicCommunitiesStatistic = (FindDynamicCommunities) ststcs;
        if(panel!=null)
        {
            settings.load(dynamicCommunitiesStatistic);
            panel.setVisitCost(dynamicCommunitiesStatistic.getVisitCost());  
            panel.setAbsentCost(dynamicCommunitiesStatistic.getAbsentCost());
            panel.setSwitchCost(dynamicCommunitiesStatistic.getSwitchCost());
            panel.setExecuteTBW(dynamicCommunitiesStatistic.getExecuteTBW());
            panel.setTimeParameter(dynamicCommunitiesStatistic.getTimeParameter());
        }
    }

    @Override
    public void unsetup() 
    {
        if(panel!=null)
        {
            dynamicCommunitiesStatistic.setSwitchCost(panel.getSwitchCost());
            dynamicCommunitiesStatistic.setVisitCost(panel.getVisitCost());
            dynamicCommunitiesStatistic.setAbsentCost(panel.getAbsentCost());
            dynamicCommunitiesStatistic.setExecuteTBW(panel.getExecuteTBW());
            dynamicCommunitiesStatistic.setTimeParameter(panel.getTimeParameter());
            settings.save(dynamicCommunitiesStatistic);
        }
        panel = null;
        dynamicCommunitiesStatistic = null;
    }

    @Override
    public Class<? extends Statistics> getStatisticsClass() 
    {   return FindDynamicCommunities.class;}

    @Override
    public String getValue() 
    {   return null;}

    @Override
    public String getDisplayName() 
    {    return "Find Dynamic Communities";}

    @Override
    public String getCategory() 
    {    return StatisticsUI.CATEGORY_NETWORK_OVERVIEW;}

    @Override
    public int getPosition() 
    {    return 10000;}

    @Override
    public String getShortDescription() 
    {    return null;}
    
    private static class StatSettings 
    {
        private double visitCost=1;
        private double switchCost=1;
        private double absentCost=1;
        private int timeParameter=1;
        private Boolean executeTBW=Boolean.TRUE;

        private void save(FindDynamicCommunities stat) 
        {
            this.visitCost = stat.getVisitCost();
            this.switchCost = stat.getSwitchCost();
            this.absentCost = stat.getAbsentCost();
            this.executeTBW = stat.getExecuteTBW();
            this.timeParameter = stat.getTimeParameter();
        }

        private void load(FindDynamicCommunities stat) 
        {
            stat.setVisitCost(visitCost);
            stat.setSwitchCost(switchCost);
            stat.setAbsentCost(absentCost);
            stat.setExecuteTBW(executeTBW);
            stat.setTimeParameter(timeParameter);
        }
    }
}
    

