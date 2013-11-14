
package kcl.julie.DynamicCommunityIdentificationPlugin;

/*
 * @author Julie Kauffman
 * The PriorityQueueElements are used in the priority queues kept by each group.
 * They are used when matching groups across timesteps. They have fields to keep 
 * track of the index of the current group, the previous group it is being compared
 * to, and their similarity index. The elements are kept in the priority queue w.r.t
 * the similarityIndex. Elements with a greater similarityIndex are kept at the 
 * front of the priority queue.
 */

public class PriorityQueueElement implements Comparable<PriorityQueueElement>
{
    Group  previousGroup;
    int    currentGroupIndex;
    double similarityIndex;
    
    public PriorityQueueElement(Group previousGroup, int currentGroupIndex, double index)
    {
        this.previousGroup     = previousGroup;
        this.currentGroupIndex = currentGroupIndex;
        this.similarityIndex   = index;
    }
    
    public Group getPreviousGroup()
    {return this.previousGroup;}
    
    public int getCurrentGroupIndex()
    {return this.currentGroupIndex;}
    
    public double getSimilarityIndex()
    {return this.similarityIndex;}    

    @Override
    public int compareTo(PriorityQueueElement x) 
    {
        if(this.getSimilarityIndex() < x.getSimilarityIndex())
            return 1;
        if(this.getSimilarityIndex() == x.getSimilarityIndex())
            return 0;
        else
            return -1;
    }
}

    

