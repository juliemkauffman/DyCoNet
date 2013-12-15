package kcl.julie.DynamicCommunityIdentificationPlugin;

import org.gephi.graph.api.Graph;
import org.gephi.graph.api.Node;

/*
 * @author Julie Kauffman
 * The GroupStructure keeps track of all of the groups present at one specific
 * timestep. The constructor takes as input the graph at a given timestep, and 
 * it creates an array of groups for each group present at that time. The 
 * constructor also puts each node in the graph at that time into the correct
 * group. 
 */

public class GroupStructure 
{
    public static final String MODULARITY_CLASS = "modularity class"; 
    Group[] groups; 
    Graph   graph;
    int     numberOfNodes;        
        
    GroupStructure(Graph graph, int timeStep)
    {
        this.graph         = graph;
        this.numberOfNodes = graph.getNodeCount();
        
        //get the number of distinct groups in the original dynamic network
        int numberOfGroups = 0;
        for(Node n : graph.getNodes())
        {
            Integer currentHighestGroupId = (Integer) n.getAttributes().getValue(MODULARITY_CLASS);
            if(currentHighestGroupId > numberOfGroups)
                numberOfGroups = currentHighestGroupId;
        }
        
        //create an array to hold each of these distinct groups
        groups = new Group[numberOfGroups+1];
        
        //for each node in the graph, add it to the correct group in the groups array
        for(Node n : graph.getNodes())
        {
            Integer groupInteger = (Integer) n.getAttributes().getValue(MODULARITY_CLASS);
            Group currentGroup;
            if(groups[groupInteger]==null)
            {
                currentGroup = new Group(groupInteger, timeStep);
                groups[groupInteger] = currentGroup;
            }
            else
                currentGroup = groups[groupInteger];
           
            currentGroup.add(n);                                      
        }
    }        
}
