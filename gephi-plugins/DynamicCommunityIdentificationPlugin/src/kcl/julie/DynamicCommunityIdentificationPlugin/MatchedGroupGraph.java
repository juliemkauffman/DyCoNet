
package kcl.julie.DynamicCommunityIdentificationPlugin;

import java.util.Arrays;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;
import org.gephi.data.attributes.type.Interval;
import org.gephi.dynamic.api.DynamicController;
import org.gephi.dynamic.api.DynamicGraph;
import org.gephi.dynamic.api.DynamicModel;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.Node;
import org.openide.util.Lookup;

/*
 * @author Julie Kauffman
 * 
 * The MatchedGroupGraph is used to match groups across timesteps to identify persistent 
 * communities. Groups that are matched to form a community are assigned the same 
 * color ID. At one timestep, there can be no two groups with the same color ID.
 * 
 * Groups are matched based on their similarity, as determined by 
 * the Jaccard index. To begin, each group in the first timestep is given its own unique color ID.
 * For each group g in each remaining timestep t, the Jaccard index of g and every 
 * group g' in timestep t'=t-1 is calculated. This is kept in 
 * a priority queue for each group g, so that the first element in the queue indicates
 * which group g' is most similar to g. A criterion is used so that if there is no
 * satisfactory match at timestep t-1, the algorithm will then consider all groups
 * at timestep t-2. Once each group at timestep t has its own priority queue of earlier
 * groups in decreasing order of similarity, the group g at timestep t with the highest 
 * Jaccard index with an earlier group g' is chosen. g is given the same color ID 
 * as g'. The next group at timestep t with the next highest Jaccard index is 
 * chosen. If the color ID of the group it is most similar to in an earlier timestep 
 * has already been used for a different group at timestep t, the next element in
 * the current group's priority queue is considered again. If a given group has no 
 * similarity to an earlier group with a color ID that is still available, the group 
 * is not matched to an earlier group and is given its own color ID. If all existing 
 * color IDs have been used at the current timestep already, the remaining uncolored 
 * groups are given new, unique color IDs.
 * 
 */

public final class MatchedGroupGraph 
{    
    int currentHighestColorID;
    int timeParameter;
    String report;
    
    public MatchedGroupGraph(GroupStructure[] groupStructure, GraphModel graphModel, int timeParam)
    {   
        timeParameter = timeParam;
        DynamicController dc = Lookup.getDefault().lookup(DynamicController.class);   
        DynamicModel dynamicGraphModel = dc.getModel();
        Interval interval = new Interval(dynamicGraphModel.getMin(), dynamicGraphModel.getMax());
        DynamicGraph dynamicGraph = dynamicGraphModel.createDynamicGraph(graphModel.getGraph(), interval);        
        double timeBegin = dynamicGraph.getLow();
        this.calculateSimilarityBetweenGroups(groupStructure, timeBegin);
        this.matchGroupsAtAllTimeSteps(groupStructure, timeBegin);   
    }
    
    public void calculateSimilarityBetweenGroups(GroupStructure[] groupStructure, double timeBegin)
    {
        int totalNumberOfTimeSteps = groupStructure.length;
        for(int currentTimeStep=(int)timeBegin+1; currentTimeStep < totalNumberOfTimeSteps-1; currentTimeStep++)
        {            
            Group[] groupArrayInCurrentTimeStep = groupStructure[currentTimeStep].groups;
            this.calculateSimilarityForAllGroupsInGivenTimeStep(groupStructure, groupArrayInCurrentTimeStep, currentTimeStep, totalNumberOfTimeSteps, timeBegin);
        }
    }
    
    public void calculateSimilarityForAllGroupsInGivenTimeStep(GroupStructure[] groupStructure, Group[] groupArrayInCurrentTimeStep, int currentTimeStep, int totalNumberOfTimeSteps, double timeBegin)
    {
        int numberOfGroupsInCurrentTimeStep = groupArrayInCurrentTimeStep.length;
        for(int currentGroupIndex=0; currentGroupIndex < numberOfGroupsInCurrentTimeStep; currentGroupIndex++)                
        {
            Group currentGroup = groupArrayInCurrentTimeStep[currentGroupIndex];
            this.calculateSimilarityForSingleGroupInGivenTimeStep(groupStructure, currentGroup, currentGroupIndex, currentTimeStep, timeBegin);               
        }
    }
   
    public void calculateSimilarityForSingleGroupInGivenTimeStep(GroupStructure[] groupStructure, Group currentGroup, int currentGroupIndex, int currentTimeStep, double timeBegin)
    {
        int earlierTimeStep = currentTimeStep;
        do
        {   
            earlierTimeStep--;
            Group[] groupArrayInEarlierTimeStep = groupStructure[earlierTimeStep].groups;
            int numberOfGroupsInEarlierTimeStep = groupArrayInEarlierTimeStep.length;
            for(int earlierGroupIndex = 0; earlierGroupIndex < numberOfGroupsInEarlierTimeStep; earlierGroupIndex++)
            {
                Group  earlierGroup    = groupArrayInEarlierTimeStep[earlierGroupIndex];
                double similarityIndex = this.calculateJaccard(currentGroup, earlierGroup);
                if(similarityIndex >= .4)   //Different thresholds can be tested
                {
                    PriorityQueueElement newElement = new PriorityQueueElement(earlierGroup, currentGroupIndex, similarityIndex);
                    currentGroup.priorityQueue.add(newElement);            
                }
            }
  
            //current group has nothing in common with earlier groups
            if(currentGroup.priorityQueue.peek()==null) 
            {
                Group tempGroup = null;
                PriorityQueueElement tempHeadOfQueue = new PriorityQueueElement(tempGroup, currentGroupIndex, 0);
                currentGroup.priorityQueue.add(tempHeadOfQueue);
            } 
        }while((currentTimeStep-timeParameter<earlierTimeStep)&&(earlierTimeStep>timeBegin));
    }
    
    public double calculateJaccard(Group g, Group gPrime)
    {  
        Set<String> idsOfNodesInG      = new HashSet<String>();
        Set<String> idsOfNodesInGPrime = new HashSet<String>(); 

        for(Node n: g.nodes)
            idsOfNodesInG.add(n.getNodeData().getId());
        for(Node m: gPrime.nodes)
            idsOfNodesInGPrime.add(m.getNodeData().getId());
        
        Set<String> intersection       = new HashSet<String>(idsOfNodesInG);
        Set<String> union              = new HashSet<String>(idsOfNodesInG);
        
        intersection.retainAll(idsOfNodesInGPrime);
        union.addAll(idsOfNodesInGPrime);
        
        double sizeOfIntersection = intersection.size();
        double sizeOfUnion        = union.size();
        double jaccardIndex       = sizeOfIntersection/sizeOfUnion;

        return jaccardIndex;  
    }
    
    public void matchGroupsAtAllTimeSteps(GroupStructure[] groupStructure, double timeBegin)
    {
        int totalNumberOfTimeSteps        = groupStructure.length;
        int numberOfGroupsInFirstTimeStep = groupStructure[(int)timeBegin].groups.length;
        currentHighestColorID = 1;
        
        //color each group present in initial timestep its own color
        for(int currentGroupIndex = 0; currentGroupIndex < numberOfGroupsInFirstTimeStep; currentGroupIndex++)
        {
            groupStructure[(int)timeBegin].groups[currentGroupIndex].setColorId(currentHighestColorID);     
            currentHighestColorID++;
        }
        
        //go through remaining timesteps sequentially and color them through matching
        for(int currentTimeStep=(int)timeBegin+1; currentTimeStep < totalNumberOfTimeSteps-1; currentTimeStep++)
            this.matchGroupsAtGivenTimeStep(groupStructure, currentTimeStep);
    }
    
    public void matchGroupsAtGivenTimeStep(GroupStructure[] groupStructure, int currentTimeStep)
    {
        Set<Integer> listOfColorIdsUsedInThisTimeStep        = new HashSet<Integer>();
        Set<Group>   listOfGroupsInThisTimeStepWithoutAColor = new HashSet<Group>();
        int numberOfGroupsInCurrentTimeStep                  = groupStructure[currentTimeStep].groups.length;
        PriorityQueue<PriorityQueueElement> priorityQueueForCurrentTimeStep = new PriorityQueue<PriorityQueueElement>();

        listOfGroupsInThisTimeStepWithoutAColor.addAll(Arrays.asList(groupStructure[currentTimeStep].groups));
        
        for(int currentGroupIndex = 0; currentGroupIndex < numberOfGroupsInCurrentTimeStep; currentGroupIndex++)
        {
            Group currentGroup = groupStructure[currentTimeStep].groups[currentGroupIndex];
            PriorityQueueElement tempElement = currentGroup.priorityQueue.peek();
            priorityQueueForCurrentTimeStep.add(tempElement);
        }
        
        while(!priorityQueueForCurrentTimeStep.isEmpty())
        {
            PriorityQueueElement frontOfQueue = priorityQueueForCurrentTimeStep.poll();
            Group previousGroup = frontOfQueue.getPreviousGroup();
            
            if(previousGroup!=null)
            {
                int colorIdOfPreviousGroup = previousGroup.getColorId();
                int thisGroupIndex         = frontOfQueue.getCurrentGroupIndex();
                Group thisGroup            = groupStructure[currentTimeStep].groups[thisGroupIndex];
                
                if(listOfColorIdsUsedInThisTimeStep.contains(colorIdOfPreviousGroup))
                {
                    thisGroup.priorityQueue.poll();
                    if(thisGroup.priorityQueue.peek()!=null)
                        priorityQueueForCurrentTimeStep.add(thisGroup.priorityQueue.peek());
                    else    //all remaining edges have similarity equal to 0. Group is given a unique color
                    {
                        thisGroup.setColorId(currentHighestColorID++);
                        listOfGroupsInThisTimeStepWithoutAColor.remove(thisGroup);
                    }
                }
                else    //the color of most similar group has not been used already in current time step
                {
                    thisGroup.setColorId(colorIdOfPreviousGroup);
                    listOfColorIdsUsedInThisTimeStep.add(colorIdOfPreviousGroup);
                    listOfGroupsInThisTimeStepWithoutAColor.remove(thisGroup);
                }
            }
            else    //previous group DNE. current group is given its own color
            {
                int thisGroupIndex = frontOfQueue.getCurrentGroupIndex();
                Group thisGroup = groupStructure[currentTimeStep].groups[thisGroupIndex];
                thisGroup.setColorId(currentHighestColorID++);
                listOfGroupsInThisTimeStepWithoutAColor.remove(thisGroup);
            }
        }
        
        if(!listOfGroupsInThisTimeStepWithoutAColor.isEmpty())
        {
            for(Group g:listOfGroupsInThisTimeStepWithoutAColor)
                g.setColorId(currentHighestColorID++);
        } 
    }
    
    public String getReport()
    {   return report;}
}

