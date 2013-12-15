package kcl.julie.DynamicCommunityIdentificationPlugin;

import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;
import org.gephi.graph.api.Node;

/*
 * @author Julie Kauffman
 * Each group refers to a specific timestep in the original network. A group 
 * contains a set of all the nodes that are in that group at the specified timestep.
 * Each group also has a priority queue, which is used when matching groups across
 * timesteps to form communities. The priority queue maintains the similarity index
 * between this group and groups in earlier timesteps.
 */

public class Group 
{
    PriorityQueue<PriorityQueueElement> priorityQueue;
    Set<Node> nodes;
    Integer   colorId;
    int       timestep;
    
    public Group(Integer name, int timestep)
    {
        this.nodes         = new HashSet<Node>();
        this.priorityQueue = new PriorityQueue();
        this.timestep      = timestep;
        this.colorId       = -1;
    }
    
    public int size()
    {return nodes.size();}
    
    public boolean add(Node node)
    {
        nodes.add(node);
        return true;
    }
    
    public void setTimeStep(int timestep)
    {this.timestep = timestep;}
    
    public int getTimeStep()
    {return this.timestep;}
    
    public void setColorId(int colorID)
    {this.colorId = colorID;}
    
    public int getColorId()
    {return this.colorId;}
}

