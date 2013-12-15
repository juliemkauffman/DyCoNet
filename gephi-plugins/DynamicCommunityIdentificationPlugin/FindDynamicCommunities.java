package kcl.julie.DynamicCommunityIdentificationPlugin;

import java.awt.Color;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.gephi.data.attributes.api.AttributeColumn;
import org.openide.util.Lookup;
import org.gephi.data.attributes.api.AttributeModel;
import org.gephi.data.attributes.api.AttributeOrigin;
import org.gephi.data.attributes.api.AttributeRow;
import org.gephi.data.attributes.api.AttributeTable;
import org.gephi.data.attributes.api.AttributeType;
import org.gephi.data.attributes.type.DynamicInteger;
import org.gephi.data.attributes.type.Interval;
import org.gephi.data.attributes.type.TimeInterval;
import org.gephi.dynamic.api.DynamicController;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.GraphModel;
import org.gephi.statistics.spi.Statistics;
import org.gephi.statistics.plugin.Modularity;
import org.gephi.dynamic.api.DynamicGraph;
import org.gephi.dynamic.api.DynamicModel;
import org.gephi.graph.api.GraphEvent;
import org.gephi.graph.api.GraphListener;
import org.gephi.graph.api.GraphView;
import org.gephi.graph.api.HierarchicalUndirectedGraph;
import org.gephi.graph.api.Node;


/*
 * @author Julie Kauffman
 * This is the primary class. When the "Find Dynamic Communities"  button is clicked,
 * the plug-in executes here and a .HTML file is produced with the results. The user is prompted to choose
 * between an algorithm for social networks and the values for three cost parameters and an algorithm for 
 * biological networks. After the algorithm executes the color of each node in Gephi can be dynamically 
 * set to represent the community membership of each node. There is also a Node Persistence value for 
 * each node which reflects how many communities the node participates in. This can be used to change the 
 * size of the nodes in Gephi, so that nodes that frequently change their membership can be displayed larger.
 */

public class FindDynamicCommunities implements Statistics
{  
    
    private boolean isCanceled;
 
    public Boolean executeTBWForSocialNetwork = Boolean.TRUE;
    public static double costSwitch = 1;
    public static double costVisit = 1;
    public static double costAbsent = 1;
    public static int timeParameter = 1;
    public static double cutoffParameter = 0.4;
    public static String chosenDirectoryString = "";
    public static final String DYNAMIC_COMMUNITY = "Dynamic Community";
    public static final String NODE_PERSISTENCE = "Node Persistence";
    int[][]  arrayOfNodeColorsAtEachTimeStep;
    int[][]  arrayOfGroupColorsForAllNodes;
    int[]    numberOfNodesInEachCommunity;
    double[] arrayOfIndividualNodeCosts;
    int      numberOfNodes;
    double   totalCostForNetwork;
    double   timeBegin;
    double   timeEnd;
    String   report;
    String[] arrayOfColorsInHex;
    int minNodeId;
    int nodeIdOffset;        
    
    @Override
    //Executes when Find Dynamic Communities button is clicked
    public void execute(GraphModel graphModel, AttributeModel attributeModel) 
    {       
        isCanceled = Boolean.FALSE;
        
        DynamicController dc = Lookup.getDefault().lookup(DynamicController.class);   
        DynamicModel dynamicGraphModel = dc.getModel();
        Interval interval = new Interval(dynamicGraphModel.getMin(), dynamicGraphModel.getMax());
        DynamicGraph dynamicGraph = dynamicGraphModel.createDynamicGraph(graphModel.getGraph(), interval); 
                
        timeBegin = dynamicGraph.getLow();
        timeEnd   = dynamicGraph.getHigh();
        
        Graph graph   = graphModel.getGraph();
        numberOfNodes = graph.getNodeCount();
        
        minNodeId = Integer.MAX_VALUE;
        for(Node n:graph.getNodes())
        {
            if(n.getId()<minNodeId)
                minNodeId = n.getId();
        }
        
        nodeIdOffset = minNodeId - 1;
        
        GroupStructure[] groupStructure     = this.findGroupsAtEachTimeStep(graphModel, attributeModel);
        MatchedGroupGraph matchedGroupGraph = new MatchedGroupGraph(groupStructure, graphModel, timeParameter, cutoffParameter); 
        
        if(executeTBWForSocialNetwork)
        {
            arrayOfGroupColorsForAllNodes       = this.createArrayOfGroupAssociationForAllNodes(graphModel, attributeModel, groupStructure);
            arrayOfNodeColorsAtEachTimeStep     = this.colorAllIndividuals(arrayOfGroupColorsForAllNodes);
        }
        else
        {
            arrayOfGroupColorsForAllNodes       = this.createArrayOfGroupAssociationForAllNodes(graphModel, attributeModel, groupStructure);
            arrayOfNodeColorsAtEachTimeStep     = this.colorAllIndividualsByGroupColor(arrayOfGroupColorsForAllNodes);
            int currentHighestColorID = matchedGroupGraph.currentHighestColorID;
            this.findSmallerCommunities(graph, currentHighestColorID);
        }
        
        if(isCanceled)
        {
            return;
        }
        
        this.modifyAttributeTable(graphModel, attributeModel, arrayOfNodeColorsAtEachTimeStep);
        this.writeReport(graphModel, attributeModel);
       
        GraphActionListener graphListener = new GraphActionListener(dynamicGraphModel, graph, arrayOfNodeColorsAtEachTimeStep, arrayOfColorsInHex, nodeIdOffset); 
        graphModel.addGraphListener(graphListener);
        
    }   
    
    public void setExecuteTBW(Boolean executeTBW)
    {   executeTBWForSocialNetwork = executeTBW;}
    
    public void setSwitchCost(double switchCost)
    {   costSwitch=switchCost;}
    
    public void setVisitCost(double visitCost)
    {   costVisit=visitCost;}
    
    public void setAbsentCost(double absentCost)
    {   costAbsent=absentCost;}
    
    public void setTimeParameter(int timeParam)
    {   timeParameter = timeParam;}
    
    public void setCutoffParameter(double cutoffParam)
    {   cutoffParameter = cutoffParam;}
    
    public void setChosenDirectoryString(String str)
    {   chosenDirectoryString = str;}
    
    public double getSwitchCost()
    {   return costSwitch;}
    
    public double getVisitCost()
    {   return costVisit;}
    
    public double getAbsentCost()
    {   return costAbsent;}
    
    public Boolean getExecuteTBW()
    {   return executeTBWForSocialNetwork;}
    
    public int getTimeParameter()
    {   return timeParameter;}
    
    public double getCutoffParameter()
    {   return cutoffParameter;}
    
    public String getChosenDirectoryString()
    {   return chosenDirectoryString;}
    
    //Uses Louvain modularity algorithm to detect communities at each time step
    public GroupStructure[] findGroupsAtEachTimeStep(GraphModel graphModel, AttributeModel attributeModel)
    {      
        DynamicController dc = Lookup.getDefault().lookup(DynamicController.class);   
        DynamicModel dynamicGraphModel = dc.getModel();
        Interval interval = new Interval(dynamicGraphModel.getMin(), dynamicGraphModel.getMax());
        DynamicGraph dynamicGraph = dynamicGraphModel.createDynamicGraph(graphModel.getGraph(), interval);
        Modularity modularity = new Modularity();
        GroupStructure[] structureArray = new GroupStructure[(int)timeEnd+1];
        
        for(int t = (int)timeBegin; t < (int)timeEnd; t++)
        {
            Graph snapshotAtT = dynamicGraph.getSnapshotGraph((double) t);
            GraphView view = snapshotAtT.getView();
            GraphModel gmodel = snapshotAtT.getGraphModel();
            HierarchicalUndirectedGraph hierarchicalSnapshotAtT = gmodel.getHierarchicalUndirectedGraph(view);          
            modularity.execute(hierarchicalSnapshotAtT, attributeModel);
            structureArray[t] = new GroupStructure(snapshotAtT, t);            
        }      
        return structureArray;
    }
    
    //Sets the group color of each node to the color of the group it participates in at each time step
    //For working with social networks
    public int[][] createArrayOfGroupAssociationForAllNodes(GraphModel graphModel, AttributeModel attributeModel, GroupStructure[] structureArray)
    {   
        int[][] arrayOfNodeAssociationColors = new int[numberOfNodes+1][(int)timeEnd];

        for(int i=0; i<arrayOfNodeAssociationColors.length; i++)
            for(int j=0; j<arrayOfNodeAssociationColors[i].length; j++)
                arrayOfNodeAssociationColors[i][j] = 0;
        
        for(int timeStep = (int)timeBegin; timeStep < (int)timeEnd; timeStep++)
            for(Group group:structureArray[timeStep].groups)
                for(Node n:group.nodes)
                    arrayOfNodeAssociationColors[n.getId()-nodeIdOffset][timeStep] = group.getColorId();
                
        
        return arrayOfNodeAssociationColors;
    }
    
    //Instead of coloring by the social cost model, each node is simply given the color of its group. 
    //For working with biological networks.
    public int[][] colorAllIndividualsByGroupColor(int[][] arrayOfAllNodeAssociationsToGroups)
    {
        int[][] arrayOfNodeColors = new int[numberOfNodes+1][(int)timeEnd];
        for(int nodeIndex = 0; nodeIndex < arrayOfNodeColors.length; nodeIndex++)
        {
            for(int t = 1; t < arrayOfNodeColors[1].length; t++)
                arrayOfNodeColors[nodeIndex][t] = arrayOfAllNodeAssociationsToGroups[nodeIndex][t];
        }
        return arrayOfNodeColors;
    }
    
    //Used with biological networks to find collections of proteins that stay together at every timestep
    public void findSmallerCommunities(Graph graph, int currentHighestColorID)
    {
        Set<Community> setOfCommunitiesFoundByMatchingNodes = new HashSet<Community>();
        Set<Community> setOfCommunitiesDetectedByAlgorithm  = new HashSet<Community>();
        
        //Look at each node and compare its behavior to every other node
        for(int i=1; i<arrayOfNodeColorsAtEachTimeStep.length; i++)
        {
            for(int j=1; j<arrayOfNodeColorsAtEachTimeStep.length; j++)
            {
                if(j!=i)    //don't want to compare each node to itself
                {
                    //If nodes i and j do the same thing at each time step
                    if(Arrays.equals(arrayOfGroupColorsForAllNodes[i],arrayOfGroupColorsForAllNodes[j])) 
                    {
                        Boolean isInOneGroupAlways = Boolean.TRUE;
                        Boolean originalCommunityDoesExist = Boolean.FALSE;
                        for(int index=2; index<arrayOfGroupColorsForAllNodes[i].length; index++)
                        {
                            if(arrayOfGroupColorsForAllNodes[i][index]!=arrayOfGroupColorsForAllNodes[i][1])
                                isInOneGroupAlways = Boolean.FALSE;        
                        }
                        //If the two matching nodes are in exactly one community total, add them to the setOfCommunitiesDetectedByAlgorithm
                        if(isInOneGroupAlways)
                        {
                            for(Community d:setOfCommunitiesDetectedByAlgorithm)
                            {
                                if(d.contains(graph.getNode(i+nodeIdOffset).getNodeData().getLabel()))
                                {
                                    d.addNode(graph.getNode(j+nodeIdOffset).getNodeData().getLabel());
                                    originalCommunityDoesExist = Boolean.TRUE;
                                }
                                else if(d.contains(graph.getNode(j+nodeIdOffset).getNodeData().getLabel()))
                                {
                                    d.addNode(graph.getNode(i+nodeIdOffset).getNodeData().getLabel());
                                    originalCommunityDoesExist = Boolean.TRUE;
                                }
                            }
                            if(!originalCommunityDoesExist)
                            {
                                Community newCom = new Community();
                                newCom.addNode(graph.getNode(i+nodeIdOffset).getNodeData().getLabel());
                                newCom.addNode(graph.getNode(j+nodeIdOffset).getNodeData().getLabel());
                                setOfCommunitiesDetectedByAlgorithm.add(newCom);
                                newCom.color = arrayOfGroupColorsForAllNodes[i][1];
                            }
                        }
                        //else the two matched nodes change communities. Add them to setOfCommunitiesFoundByMatchingNodes
                        else
                        {
                            Boolean communityDoesExist = Boolean.FALSE;
                            for(Community c:setOfCommunitiesFoundByMatchingNodes)
                            {
                                if(c.contains(graph.getNode(i+nodeIdOffset).getNodeData().getLabel()))
                                {
                                    c.addNode(graph.getNode(j+nodeIdOffset).getNodeData().getLabel());
                                    communityDoesExist = Boolean.TRUE;
                                }
                                else if(c.contains(graph.getNode(j+nodeIdOffset).getNodeData().getLabel()))
                                {
                                    c.addNode(graph.getNode(i+nodeIdOffset).getNodeData().getLabel());
                                    communityDoesExist = Boolean.TRUE;
                                }
                            }
                            if(!communityDoesExist)
                            {
                                Community newCommunity = new Community();
                                newCommunity.addNode(graph.getNode(i+nodeIdOffset).getNodeData().getLabel());
                                newCommunity.addNode(graph.getNode(j+nodeIdOffset).getNodeData().getLabel());
                                setOfCommunitiesFoundByMatchingNodes.add(newCommunity);
                            }
                        }
                    }
                }
            }
        }
        
        int countOfNodesOriginallyInSameCommunityAlways = 0;
        for(Community f:setOfCommunitiesDetectedByAlgorithm)
        {
            //System.out.println("Original community found containing " + f.nodes.size() + " nodes:");
            //System.out.println("    " + f.nodes.toString());
            countOfNodesOriginallyInSameCommunityAlways += f.nodes.size();
        }
       
        int count = 0;
        int filter = 1;
        int numberOfNewCommunitiesLargerThanFilter = 0;
        int numberOfNodesInNewCommunitiesLargerThanFilter=0;
        double averageSizeOfNewCommunitiesLargerThanFilter;
        for(Community com:setOfCommunitiesFoundByMatchingNodes)
        {
            int newColorForThisGroup = currentHighestColorID++;
            com.color = newColorForThisGroup;
            Set<Integer> setOfGroups = new HashSet<Integer>();
            String testNode = "";
            int z=0;
            
            for(String n:com.nodes)
            {
                if(z==0)
                {
                    testNode = n;
                    z=1;
                }
                for(int ts = (int)timeBegin; ts<(int)timeEnd; ts++)
                    arrayOfNodeColorsAtEachTimeStep[graph.getNode(n).getId()-nodeIdOffset][ts]=newColorForThisGroup;
            }
            for(int t = (int)timeBegin; t<(int)timeEnd; t++)
                setOfGroups.add(arrayOfGroupColorsForAllNodes[graph.getNode(testNode).getId()-nodeIdOffset][t]);
            count += com.nodes.size();
            if(com.nodes.size()>filter)
            {
                numberOfNewCommunitiesLargerThanFilter++;
                numberOfNodesInNewCommunitiesLargerThanFilter+=com.nodes.size();
                //System.out.println("New small community found containing " + com.nodes.size() + " nodes originally in " + setOfGroups.size() + ":");
                //System.out.println("    " + com.nodes.toString());
            }            
        }
        
        numberOfNodesInEachCommunity = new int[currentHighestColorID+1];
        //System.out.println("numberOfNodesInEachCommunity array created.");
        //System.out.println("Current highest color Id is " + currentHighestColorID);
        for(int d=0; d<currentHighestColorID; d++)
            numberOfNodesInEachCommunity[d]=0;        
        
        for(Community cm: setOfCommunitiesDetectedByAlgorithm)
            numberOfNodesInEachCommunity[cm.color] = cm.nodes.size();
        
        for(Community cmm: setOfCommunitiesFoundByMatchingNodes)
            numberOfNodesInEachCommunity[cmm.color] = cmm.nodes.size();
        
        int numberOfNodesThatAreNotStable = 0;
        int numberOfNodesThatAreNotStableAndMissingAtOneTimeStep=0;
        for(Node n:graph.getNodes())
        {
            Boolean nodeIsStable = Boolean.TRUE;
            Boolean nodeIsAlwaysPresent = Boolean.TRUE;
            for(int t=(int)timeBegin+1; t<(int)timeEnd && nodeIsStable; t++)
            {
                if(arrayOfNodeColorsAtEachTimeStep[n.getId()-nodeIdOffset][(int)timeBegin]!=arrayOfNodeColorsAtEachTimeStep[n.getId()-nodeIdOffset][t])
                    nodeIsStable = Boolean.FALSE;
            }
            if(!nodeIsStable)
            {
                numberOfNodesThatAreNotStable++;
                for(int t=(int)timeBegin; t<(int)timeEnd && nodeIsStable; t++)
                {
                    if(arrayOfNodeColorsAtEachTimeStep[n.getId()-nodeIdOffset][t]==0)
                        nodeIsAlwaysPresent = Boolean.FALSE;
                }
            }
            
            if(!nodeIsAlwaysPresent)
                numberOfNodesThatAreNotStableAndMissingAtOneTimeStep++;
        }
        
        if(numberOfNewCommunitiesLargerThanFilter!=0)
            averageSizeOfNewCommunitiesLargerThanFilter = numberOfNodesInNewCommunitiesLargerThanFilter/numberOfNewCommunitiesLargerThanFilter;
        else
            averageSizeOfNewCommunitiesLargerThanFilter = 0;
        
        //System.out.println("\nThere are " + numberOfNodes + " nodes total");
        //System.out.println("There are " + countOfNodesOriginallyInSameCommunityAlways + " nodes in the same community at each timestep.");
        //System.out.println("There are " + setOfCommunitiesDetectedByAlgorithm.size() + " communities with at least one stable node identified by the algorithm");
        //System.out.println("There are " + setOfCommunitiesFoundByMatchingNodes.size() + " new communities identified by matching nodes with the same behavior.");
        //System.out.println("There are " + count + " nodes in these smaller communities.");
        //System.out.println("There are " + numberOfNewCommunitiesLargerThanFilter + " new smaller communities with more than " + filter + " nodes");
        //System.out.println("The average size of these communities is " + averageSizeOfNewCommunitiesLargerThanFilter);
        //System.out.println("There are " + numberOfNodesThatAreNotStable + " nodes that are not stable");
        //System.out.println(numberOfNodesThatAreNotStableAndMissingAtOneTimeStep + " of these are missing at one or more timesteps");
    }
    
    //Color all individuals using the social cost model
    public int[][] colorAllIndividuals(int[][] arrayOfAllNodeAssociationsToGroups)
    {
        int[][] arrayOfNodeColors = new int[numberOfNodes+1][(int)timeEnd+1];
        arrayOfIndividualNodeCosts = new double[numberOfNodes+1];
        double cost=0;
        for(int nodeIndex = 1; nodeIndex < arrayOfAllNodeAssociationsToGroups.length; nodeIndex++)
        {
            RecursionElement temp = colorOneIndividual(arrayOfAllNodeAssociationsToGroups[nodeIndex], nodeIndex, timeEnd-1); 
            arrayOfNodeColors[nodeIndex] = temp.getNodeColorAtEachTimeStep().clone();
            arrayOfIndividualNodeCosts[nodeIndex] = temp.getCost();
            cost += temp.getCost();
        }
        totalCostForNetwork = cost;
        //System.out.println("Total calculated cost is " + cost);
        return arrayOfNodeColors;
    }
    
    //Color one individual using social cost model
    public RecursionElement colorOneIndividual(int[] arrayOfOneNodeAssociationToGroups, int nodeIndex, double timeStep)
    {
        //System.out.println("Trying to color node " + nodeIndex);
        RecursionElement recursionElement;
        RecursionElement minRecursionElement = new RecursionElement();
        minRecursionElement.setCost(Double.MAX_VALUE);
        int[] array;
        Set<Integer> groupColorsOfNode = new HashSet<Integer>();
        int[] colorsOfNodeAtEachTimeStep = new int[numberOfNodes+1];
        
        for(int i:arrayOfOneNodeAssociationToGroups)
            groupColorsOfNode.add(i);
        
        //System.out.println("Set of group colors for this node includes :" + groupColorsOfNode.toString());
        
        for(int currentGroupColorBeingTested:groupColorsOfNode)
        {
            recursionElement=costToColorOneIndividualSpecificColor(arrayOfOneNodeAssociationToGroups, colorsOfNodeAtEachTimeStep, groupColorsOfNode, timeStep, currentGroupColorBeingTested);
            if(recursionElement.getCost()<minRecursionElement.getCost())
            {
                array = recursionElement.getNodeColorAtEachTimeStep().clone();
                array[(int)timeStep] = currentGroupColorBeingTested;
                minRecursionElement.setNodeColorAtEachTimeStep(array);
                minRecursionElement.setCost(recursionElement.getCost());
            }
        }
        
        //System.out.println("Calculated cost for node " + nodeIndex + " is " + minRecursionElement.getCost()+"\n");
        return minRecursionElement;
    }
    
    public RecursionElement costToColorOneIndividualSpecificColor(int[] arrayOfOneNodeAssociationToGroups, int[] colorsOfNodeAtEachTimeStep, Set<Integer> groupColorsOfNode, double timeStep, int color)
    {
        double cost;
        RecursionElement minRecursionElement = new RecursionElement();
        minRecursionElement.setCost(Double.MAX_VALUE);
        RecursionElement recursionElement = new RecursionElement(); 
        int[] array = new int[(int)timeEnd+1];
        if(timeStep==timeBegin)
        {
            cost = this.getVisitAndAbsenceCost(arrayOfOneNodeAssociationToGroups, timeStep, color);
            array[(int)timeStep]=color;
            recursionElement.setCost(cost);
            recursionElement.setNodeColorAtEachTimeStep(array);
            return recursionElement;
        }
        else
        {
            for(int currentPossibleGroupColor:groupColorsOfNode)
            {
                recursionElement = costToColorOneIndividualSpecificColor(arrayOfOneNodeAssociationToGroups, colorsOfNodeAtEachTimeStep, groupColorsOfNode, timeStep-1, currentPossibleGroupColor); 
                double newCost   = recursionElement.getCost()
                                 + getSwitchCost(arrayOfOneNodeAssociationToGroups, timeStep, color, currentPossibleGroupColor)
                                 + getVisitAndAbsenceCost(arrayOfOneNodeAssociationToGroups, timeStep, color);
                if(newCost<minRecursionElement.getCost())
                {
                    array = recursionElement.getNodeColorAtEachTimeStep().clone();
                    array[(int)timeStep] = color;
                    minRecursionElement.setNodeColorAtEachTimeStep(array);
                    minRecursionElement.setCost(newCost);
                }
            }
        }
        return minRecursionElement;   
    }
    
    public double getVisitAndAbsenceCost(int[] arrayOfOneNodeAssociationToGroups, double timeStep, int colorOfNode)
    {
        double costOfVisitAndAbsence = 0;
        int colorOfGroupAtTimeStep   = arrayOfOneNodeAssociationToGroups[(int)timeStep];
        
        //if the current group is present & the node is a different color=>visit
        if((colorOfGroupAtTimeStep!=0)&&(colorOfNode!=colorOfGroupAtTimeStep))
        {
            costOfVisitAndAbsence+=costVisit;
        }
        
        //if the node is absent from a timestep, but its community is present=>absent
        if((colorOfGroupAtTimeStep==0)||(colorOfNode!=colorOfGroupAtTimeStep))
        {
            Boolean isGroupPresent = Boolean.FALSE;
            for(int index=1; ((!isGroupPresent)&&(index<arrayOfGroupColorsForAllNodes.length)); index++)
            {
                if(arrayOfGroupColorsForAllNodes[index][(int)timeStep]==colorOfNode)    //community is present
                {
                    isGroupPresent=Boolean.TRUE;
                    costOfVisitAndAbsence+=costAbsent;
                }
            }            
        }
        return costOfVisitAndAbsence; 
    }
    
    //returns the switching cost incurred when coloring a given node a specific color at a certain timestep
    public double getSwitchCost(int[] arrayOfOneNodeAssociationToGroups, double timeStep, int colorOfNodeInCurrentTimeStep, int colorOfNodeInPreviousTimeStep)
    {
        double costOfSwitch=0;
        if(colorOfNodeInCurrentTimeStep!=colorOfNodeInPreviousTimeStep)
            costOfSwitch+=costSwitch;
        return costOfSwitch;
    }
    
    //This recursionElement is used when calculating the optimal coloring of a node using the social cost model
    public class RecursionElement
    {
        double cost;
        int[] nodeColorAtEachTimeStep;
        
        public RecursionElement()
        {
            cost=0;
            nodeColorAtEachTimeStep=null;
        }
        public RecursionElement(double c, int[] array)
        {
            cost = c;
            nodeColorAtEachTimeStep = array;
        }
        public double getCost() {return cost;}
        public void setCost(double c) {cost = c;}
        public int[] getNodeColorAtEachTimeStep() {return nodeColorAtEachTimeStep;}
        public void setNodeColorAtEachTimeStep(int[] array) {nodeColorAtEachTimeStep = array.clone();}
    }
    
    //Modifies the attributes tables in Gephi with the dynamic communities and node persistence of each node
    public void modifyAttributeTable(GraphModel graphModel, AttributeModel attributeModel, int[][] arrayOfNodeColorsAtEachTimeStep)
    {
        AttributeTable  nodeTable              = attributeModel.getNodeTable();
        AttributeColumn dynamicCommunityColumn = nodeTable.getColumn(DYNAMIC_COMMUNITY);
        AttributeColumn nodePersistenceColumn  = nodeTable.getColumn(NODE_PERSISTENCE);
        Graph graph = graphModel.getGraph();
        
        if(dynamicCommunityColumn == null)
            dynamicCommunityColumn = nodeTable.addColumn(DYNAMIC_COMMUNITY, AttributeType.DYNAMIC_INT, AttributeOrigin.COMPUTED);
        
        for(Node n : graph.getNodes())
        {
            int nodeIndex    = n.getId()-nodeIdOffset;
            AttributeRow row = (AttributeRow) n.getNodeData().getAttributes();
            int lastTimeStep = arrayOfNodeColorsAtEachTimeStep[nodeIndex].length-1;
            List<Interval<Integer>> listOfIntervals = new LinkedList<Interval<Integer>>();
            for(int timeStep = 1; timeStep<lastTimeStep; timeStep++)
            {
                Interval interval = new Interval(timeStep, timeStep+1, Boolean.FALSE, Boolean.TRUE, arrayOfNodeColorsAtEachTimeStep[nodeIndex][timeStep]);
                listOfIntervals.add(interval);
            }
            
            DynamicInteger dynamicInt = new DynamicInteger(listOfIntervals);
            row.setValue(dynamicCommunityColumn, dynamicInt);
        }
        
        if(nodePersistenceColumn == null)
        {
            nodePersistenceColumn = nodeTable.addColumn(NODE_PERSISTENCE, AttributeType.INT, AttributeOrigin.COMPUTED);
        }
        for(Node n: graph.getNodes())
        {
            int nodeIndex = n.getId()-nodeIdOffset;
            AttributeRow row = (AttributeRow) n.getNodeData().getAttributes();
            Set<Integer> colorsOfNode = new HashSet<Integer>();
            for(int timeStep = (int)timeBegin; timeStep<(int)timeEnd; timeStep++)
            {
                colorsOfNode.add(arrayOfNodeColorsAtEachTimeStep[nodeIndex][timeStep]);
            }
            int numberOfChanges = colorsOfNode.size(); 
            row.setValue(nodePersistenceColumn, numberOfChanges);
        }
        
        for(Node m : graph.getNodes())
        {
            int nodeIndex = m.getId()-nodeIdOffset;
            AttributeRow attRow = (AttributeRow) m.getNodeData().getAttributes();
            DynamicInteger dynamicInteger = (DynamicInteger)attRow.getValue(dynamicCommunityColumn);
            int lastTimeStep = arrayOfNodeColorsAtEachTimeStep[nodeIndex].length-1;
            for(int timeStep = 1; timeStep<lastTimeStep; timeStep++)
            {
                Interval interval = new Interval(timeStep, timeStep+1, Boolean.FALSE, Boolean.TRUE, arrayOfNodeColorsAtEachTimeStep[nodeIndex][timeStep]);
            }
        }
    }
    
    //creates an array of colors to be used for the first 16 communities identified, using the Windows 8 colors.
    public String[] createColorArray()
    {
        String[] colorArray = new String[17];
        colorArray[0]  = Integer.toHexString((new Color(255,255,255).getRGB())).substring(2,8);
        colorArray[1]  = Integer.toHexString((new Color(0,171,169).getRGB())).substring(2,8);    //teal
        colorArray[2]  = Integer.toHexString((new Color(170,0,255).getRGB())).substring(2,8);    //violet
        colorArray[3]  = Integer.toHexString((new Color(216,0,115).getRGB())).substring(2,8);    //magenta
        colorArray[4]  = Integer.toHexString((new Color(100,118,135).getRGB())).substring(2,8);  //steel
        colorArray[5]  = Integer.toHexString((new Color(96,169,23).getRGB())).substring(2,8);    //green
        colorArray[6]  = Integer.toHexString((new Color(118,96,138).getRGB())).substring(2,8);   //mauve
        colorArray[7]  = Integer.toHexString((new Color(27,161,226).getRGB())).substring(2,8);   //cyan
        colorArray[8]  = Integer.toHexString((new Color(227,200,0).getRGB())).substring(2,8);    //yellow
        colorArray[9]  = Integer.toHexString((new Color(244,114,208).getRGB())).substring(2,8);  //pink
        colorArray[10] = Integer.toHexString((new Color(162,0,37).getRGB())).substring(2,8);     //crimson
        colorArray[11] = Integer.toHexString((new Color(109,135,100).getRGB())).substring(2,8);  //olive
        colorArray[12] = Integer.toHexString((new Color(250,104,0).getRGB())).substring(2,8);    //orange
        colorArray[13] = Integer.toHexString((new Color(164,196,0).getRGB())).substring(2,8);    //lime
        colorArray[14] = Integer.toHexString((new Color(240,163,10).getRGB())).substring(2,8);   //amber
        colorArray[15] = Integer.toHexString((new Color(0,138,0).getRGB())).substring(2,8);      //emerald
        colorArray[16] = Integer.toHexString((new Color(106,0,255).getRGB())).substring(2,8);    //indigo
        return colorArray;        
    }
    
    //generates a random color. Based on http://martin.ankerl.com/2009/12/09/how-to-create-random-colors-programmatically/
    public String generateRandomColorInHex(Random randomDouble)
    {
        double goldenRatioConjugate = 0.618033988749895;
        double h = randomDouble.nextDouble();
        h+= goldenRatioConjugate;
        h%=1;
        String colorInHex = hsvToRgbToHex(h, 0.5, 0.95); 
        return colorInHex;
    }
    
    //Takes as input a color in HSV and returns a color in Hex. Based on http://martin.ankerl.com/2009/12/09/how-to-create-random-colors-programmatically/
    public String hsvToRgbToHex(double h, double s, double v)
    {
        double rDouble=255;
        double gDouble=255;
        double bDouble=255;
        double hInt = Math.floor(h*6);
        double f = (h*6)-hInt;
        double p = v*(1-s);
        double q = v*(1-f*s);
        double t = v*(1-(1-f)*s);
        if(hInt==0)
        {
            rDouble = v;
            gDouble = t;
            bDouble = p; 
        }
        if(hInt==1)
        {
            rDouble = q;
            gDouble = v;
            bDouble = p; 
        }
        if(hInt==2)
        {
            rDouble = p;
            gDouble = v;
            bDouble = t; 
        }
        if(hInt==3)
        {
            rDouble = p;
            gDouble = q;
            bDouble = v; 
        }
        if(hInt==4)
        {
            rDouble = t;
            gDouble = p;
            bDouble = v; 
        }
        if(hInt==5)
        {
            rDouble = v;
            gDouble = p;
            bDouble = q; 
        }

        int r = (int)(rDouble*255);
        int g = (int)(gDouble*255);
        int b = (int)(bDouble*255);
        Color newColor = new Color(r, g, b);
        String colorInHex = Integer.toHexString(newColor.getRGB());
        colorInHex = colorInHex.substring(2, colorInHex.length());
        return colorInHex;
    }
    
    //creates the .HTML file that Gephi displays after the algorithm is executed
    public void writeReport(GraphModel graphModel, AttributeModel attributeModel)
    {
        String[] arrayOfNodeNames = new String[numberOfNodes+1];
        Graph graph = graphModel.getGraph();
        for(Node n: graph.getNodes())
        {
            int nodeIndex    = n.getId()-nodeIdOffset;
            String nodeName  = n.getNodeData().getLabel();
            arrayOfNodeNames[nodeIndex] = nodeName;
        }
        
        //testing colors
        Set<Integer> setOfCommunities = new HashSet<Integer>();
        for(int j=1; j<arrayOfNodeColorsAtEachTimeStep.length; j++)
            for(int k=1; k<timeEnd; k++)
            {
                setOfCommunities.add(arrayOfGroupColorsForAllNodes[j][k]);
                setOfCommunities.add(arrayOfNodeColorsAtEachTimeStep[j][k]);
            }
        //String[] arrayOfColorsInHex = new String[setOfCommunities.size()+2];
        arrayOfColorsInHex = new String[setOfCommunities.size()+2];
        Random randomNumberSequence = new Random();
        int numberOfCommunities = setOfCommunities.size();
        String[] precomputedColorArray = this.createColorArray();
        
        for(int i=0; i<=numberOfCommunities; i++)
        {
            if(i<precomputedColorArray.length)
                arrayOfColorsInHex[i]=precomputedColorArray[i];
            else
                arrayOfColorsInHex[i]=this.generateRandomColorInHex(randomNumberSequence);                
        }        
        report = "<HTML> <BODY> <h1>Dynamic Communities Report </h1>"
                 + "<hr>"
                 + "<br>";
        
        //When using cost model
        if(executeTBWForSocialNetwork)
        {
            report = report.concat("Visiting cost = " + costVisit
                    + "<br>Absence cost = " + costAbsent
                    + "<br>Switching cost = " + costSwitch  + "<br>  <br>  ");
        }
        report = report.concat("<table border = '1'"
                + "cellpadding='5'>"
                + "<tr>"
                + "<th>Time step</th>");
        for(int index = 1; index < numberOfNodes+1; index++)
        {
           report = report.concat("<th colspan='2'>" + arrayOfNodeNames[index]  + "</th>"); 
        }
        
        report = report.concat("</tr>");        
                
        for(int timeStep = (int)timeBegin; timeStep<(int)timeEnd; timeStep++)
        {
            report = report.concat("<tr><td>" + timeStep + "</td>");
            for(int i=1; i < numberOfNodes+1; i++)
            {
                int colorOfNode = arrayOfNodeColorsAtEachTimeStep[i][timeStep];
                int colorOfGroup = arrayOfGroupColorsForAllNodes[i][timeStep];
                String colorOfNodeInHex = arrayOfColorsInHex[colorOfNode];
                String colorOfGroupInHex = "";
                if(colorOfGroup!=0)
                    colorOfGroupInHex = arrayOfColorsInHex[colorOfGroup];
                if(colorOfNode == colorOfGroup)
                    report = report.concat("<td colspan='2' bgcolor='#" + colorOfNodeInHex +"'>" + colorOfNode + ("</td>"));
                else if(colorOfGroup==0)
                {
                    Boolean isCommunityPresent = Boolean.FALSE;
                    for(int nodeIndex = 1; nodeIndex < numberOfNodes+1; nodeIndex++)
                    {
                        if(arrayOfGroupColorsForAllNodes[nodeIndex][timeStep]==colorOfNode)
                            isCommunityPresent = Boolean.TRUE;
                    }
                    if(isCommunityPresent)
                        report = report.concat("<td bgcolor='#" + colorOfNodeInHex +"'>" + colorOfNode + "</td>" + "<td>-</td>");
                    else
                        report = report.concat("<td bgcolor='#" + colorOfNodeInHex +"'>" + colorOfNode + "</td>" + "<td bgcolor='#" + colorOfNodeInHex + "'>-</td>");                        
                }else                
                    report = report.concat("<td bgcolor='#" + colorOfNodeInHex +"'>" + colorOfNode + "</td>" + "<td bgcolor='#" + colorOfGroupInHex + "'>" + colorOfGroup + "</td>");
            }
            report = report.concat("</tr>");
        }
        
        //When using cost model
        if(executeTBWForSocialNetwork)
        {
            report = report.concat("<tr><th>Cost</th>");
            for(int index=1; index < arrayOfIndividualNodeCosts.length; index++)
            {
                report = report.concat("<td colspan='2'>" + arrayOfIndividualNodeCosts[index] + "</td>");
            }
        }

        //When not using cost model
        else
        {
            report = report.concat("<tr><th># of nodes</th>");
            for(int indx = 1; indx < numberOfNodes+1; indx++)
            {
                Boolean sameCommunityAlways = Boolean.TRUE;
                for(int ts = (int) timeBegin + 1; ts < (int)timeEnd; ts++)
                {
                    if(arrayOfNodeColorsAtEachTimeStep[indx][ts]!=arrayOfNodeColorsAtEachTimeStep[indx][(int)timeBegin])
                        sameCommunityAlways=Boolean.FALSE;
     
                }
                if(sameCommunityAlways)
                {
                    int groupNumber = arrayOfNodeColorsAtEachTimeStep[indx][1];
                    int numInCommunity = numberOfNodesInEachCommunity[groupNumber];
                    report = report.concat("<td colspan='2'>" + numInCommunity + " </td>");
                }
                else
                    report = report.concat("<td colspan='2'> - </td>");
            }
        }
        
        report = report.concat("</tr>");
        if(executeTBWForSocialNetwork)
        {
            report = report.concat("<tr><th>Total Cost</th>");
            report = report.concat("<td colspan = '2'>" + totalCostForNetwork + "</td></tr>");
        }
        report = report.concat("</table>");
        
        if(executeTBWForSocialNetwork)
        {    
            report = report.concat("<p align = 'justify'> The community membership of each node is given in a column. Each community"
                                + " is associated with a unique color and number. If there is only one color and number"
                                + " listed for a node at a specific timestep, that node is actively participating in the"
                                + " indicated community at that timestep. If there are two numbers and colors for one node"
                                + " at one timestep, it is considered a member of the community on the left, although"
                                + " it is actively participating in, that is, visiting, the community on the right. If there"
                                + " is a colored square with a number and a white square with a dash, that node is a member"
                                + " of the inidicated community, although it is absent at that timestep. If there are two squares"
                                + " with the same color, one with a number and the second one with a dash, the node is a member"
                                + " of that community at that timestep, but both the community and the node are absent at that"
                                + " timestep, so no costs are incurred.</p>");
        }
        else
        {
            report = report.concat("<p align = 'justify'> The community membership of each node is given in a column. When there are"
                                + " two colors and numbers for one node at a certain timestep, the node is a member of a"
                                + " sub-community indicated by the left number. That sub-community is part of the"
                                + " community indicated by the right number. The number in the last row indicates the number"
                                + " of nodes in that same community or sub-community.</p>");
        }
        
        //String buttonClickedString = "You clicked the button!";
        //report = report.concat("<form>");
        //report = report.concat("<br> <input type='button' value='Open as CSV file' onclick='JavaScript:alert(" + buttonClickedString +")'/>");
        //report = report.concat("</form>");

        report = report.concat("</BODY></HTML>");
        
        
        try {
            this.createCSVFile(arrayOfNodeNames);
        } catch (IOException ex) {
            Logger.getLogger(FindDynamicCommunities.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void createCSVFile(String[] arrayOfNodeNames) throws IOException
    {
        System.out.println(chosenDirectoryString);
        String nameOfNewFile = chosenDirectoryString.concat("\\Report.csv");
        File file = new File(nameOfNewFile);
        file.createNewFile();
        FileWriter writer = new FileWriter(file);
        
        writer.append("Timestep \t");
        
        for(int index = 1; index < numberOfNodes+1; index++)
        {
           writer.append(arrayOfNodeNames[index] + "\t"); 
        }
        
        writer.append("\n");
        
        for(int timeStep = (int)timeBegin; timeStep<(int)timeEnd; timeStep++)
        {
            writer.append(timeStep + "\t");
            for(int i=1; i < numberOfNodes+1; i++)
            {
                int colorOfNode = arrayOfNodeColorsAtEachTimeStep[i][timeStep];
                int colorOfGroup = arrayOfGroupColorsForAllNodes[i][timeStep];
                if(colorOfNode == colorOfGroup)
                    writer.append(colorOfNode + "\t");
                else if(colorOfGroup==0)
                {
                    writer.append("(" + colorOfNode + ", - )\t");
                }else                
                    writer.append("(" + colorOfNode + "," + colorOfGroup + ")\t");
            }
            writer.append("\n");
        }
        
        //When using cost model
        if(executeTBWForSocialNetwork)
        {
            writer.append("Cost\t");
            for(int index=1; index < arrayOfIndividualNodeCosts.length; index++)
            {
                writer.append(arrayOfIndividualNodeCosts[index] + "\t");
            }
            writer.append("\n");
        }

        //When not using cost model
        else
        {
            writer.append("# of nodes\t");
            for(int indx = 1; indx < numberOfNodes+1; indx++)
            {
                Boolean sameCommunityAlways = Boolean.TRUE;
                for(int ts = (int) timeBegin + 1; ts < (int)timeEnd; ts++)
                {
                    if(arrayOfNodeColorsAtEachTimeStep[indx][ts]!=arrayOfNodeColorsAtEachTimeStep[indx][(int)timeBegin])
                        sameCommunityAlways=Boolean.FALSE;
     
                }
                if(sameCommunityAlways)
                {
                    int groupNumber = arrayOfNodeColorsAtEachTimeStep[indx][1];
                    int numInCommunity = numberOfNodesInEachCommunity[groupNumber];
                    writer.append(numInCommunity + "\t");
                }
                else
                    writer.append("- \t");
            }
            writer.append("\n");
        }
        
        if(executeTBWForSocialNetwork)
        {
            writer.append("Total Cost \t");
            writer.append(totalCostForNetwork +"\n");
        }
    }
    
    @Override
    public String getReport() 
    {   return report;}    
   
    
    public boolean cancel()
    {
        this.isCanceled = true;
        return true;
    }
    
}



class Community
{
    Set<String> nodes;
    int color;
    
    public Community()
    {   nodes = new HashSet<String>();}
    
    public void addNode(String newNodeName)
    {   nodes.add(newNodeName);}
    
    public Boolean contains(String newNodeName)
    {
        String[] nodeArray = new String[nodes.size()];
        nodes.toArray(nodeArray);
        for(String s: nodeArray)
        {
            if(s.equalsIgnoreCase(newNodeName))
                return Boolean.TRUE;
        } 
        return Boolean.FALSE;
    }
}



class GraphActionListener implements GraphListener
{
    Graph graph;
    DynamicModel dynamicModel;
    int[][] arrayOfNodeColors;
    String[] arrayOfColorsInHex;
    int nodeIdOffset;
    
    public GraphActionListener(DynamicModel dynamicModel, Graph graph, int[][] arrayOfNodeColors, String[] arrayOfColorsInHex, int nodeOffset)
    {
        this.graph = graph;
        this.dynamicModel = dynamicModel;
        this.arrayOfNodeColors = arrayOfNodeColors;
        this.arrayOfColorsInHex = arrayOfColorsInHex;
        this.nodeIdOffset = nodeOffset;
    }

    @Override
    public void graphChanged(GraphEvent ge) 
    {
        TimeInterval interval = dynamicModel.getVisibleInterval();
        double intervalStart = dynamicModel.getMin();
        double intervalEnd = dynamicModel.getMax();
        if(interval.getLow() > intervalStart)
            intervalStart = interval.getLow();
        if(interval.getHigh() < intervalEnd)
            intervalEnd = interval.getHigh();
        double intervalMiddle = (intervalStart+intervalEnd)/2.0;
        this.colorAllNodesForMiddleOfInterval(intervalMiddle);         
    }
    
    public void colorAllNodesForMiddleOfInterval(double intervalMiddle)
    {
        for(Node node: graph.getNodes())
        {
            int nodeId = node.getId() - nodeIdOffset;
            int nodeColor = arrayOfNodeColors[nodeId][(int)intervalMiddle];
            String nodeColorInHex = arrayOfColorsInHex[nodeColor];
            Color color = new Color(Integer.parseInt(nodeColorInHex, 16));
            float redValue = (float)color.getRed()/255;
            float greenValue = (float)color.getGreen()/255;
            float blueValue = (float)color.getBlue()/255;
            node.getNodeData().setColor(redValue, greenValue, blueValue);
        }
    }

}