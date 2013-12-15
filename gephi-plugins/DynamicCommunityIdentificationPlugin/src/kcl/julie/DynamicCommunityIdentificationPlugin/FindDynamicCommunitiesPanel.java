
package kcl.julie.DynamicCommunityIdentificationPlugin;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import org.jdesktop.swingx.JXHeader;

/*
 * @author Julie Kauffman
 * Adapted from Gephi PageRankPanel class available
 * on Github.
 */


public class FindDynamicCommunitiesPanel extends javax.swing.JPanel
{
    private JFormattedTextField visitCostTextField;
    private JFormattedTextField absentCostTextField;
    private JFormattedTextField switchCostTextField;
    private JFormattedTextField timeParameterTextField;
    private JFormattedTextField cutoffParameterTextField;
    private JTextField chosenDirectoryTextField;
    private JXHeader header;
    private JRadioButton subCommunityModelButton;
    private JRadioButton costModelButton;
    //private String chosenDirectoryString;
    private FileChooserListener fcListener;
    
    public FindDynamicCommunitiesPanel()
    {   initComponents();}
    
    public double getVisitCost()
    {
        try
        {
            return Double.parseDouble(visitCostTextField.getText());
        } catch(Exception e)
        {
            return 1;
        }
    }
    
    public double getAbsentCost()
    {
        try
        {
            return Double.parseDouble(absentCostTextField.getText());
        } catch(Exception e)
        {
            return 1;
        }
    }
    
    public double getSwitchCost()
    {
        try
        {
            return Double.parseDouble(switchCostTextField.getText());
        } catch(Exception e)
        {
            return 1;
        }
    }
    
    public Boolean getExecuteTBW()
    {
        if(costModelButton.isSelected())
            return Boolean.TRUE;
        else
            return Boolean.FALSE;
    }
    
    public int getTimeParameter()
    {
        try
        {
            return Integer.parseInt(timeParameterTextField.getText());
        } catch(Exception e)
        {
            return 1;
        }
    }
    
    public double getCutoffParameter()
    {
        try
        {
            return Double.parseDouble(cutoffParameterTextField.getText());
        } catch(Exception e)
        {
            return 0.4;
        }
    }
    
    public String getChosenDirectoryString()
    {
        //return fcListener.getDirectoryName();
        return chosenDirectoryTextField.getText();
    }
    
    public void setVisitCost(double visitCost)
    {   visitCostTextField.setText(Double.toString(visitCost));}
    
    public void setAbsentCost(double absentCost)
    {   absentCostTextField.setText(Double.toString(absentCost));}
    
    public void setSwitchCost(double switchCost)
    {   switchCostTextField.setText(Double.toString(switchCost));}
    
    public void setExecuteTBW(Boolean bool)
    {
        if(bool)
            costModelButton.setSelected(Boolean.TRUE);
        else
            subCommunityModelButton.setSelected(Boolean.TRUE);
    }
    
    public void setTimeParameter(int timeParam)
    {   timeParameterTextField.setText(Integer.toString(timeParam));}
    
    public void setCutoffParameter(double cutoffParam)
    {   cutoffParameterTextField.setText(Double.toString(cutoffParam));}
    
    public void setChosenDirectoryString(String str)
    {   chosenDirectoryTextField.setText(str);}
    
    private void initComponents()
    { 
        visitCostTextField       = new JFormattedTextField(new Double(1.0));
        absentCostTextField      = new JFormattedTextField(new Double(1.0));
        switchCostTextField      = new JFormattedTextField(new Double(1.0));
        timeParameterTextField   = new JFormattedTextField(new Integer(1));
        cutoffParameterTextField = new JFormattedTextField(new Double(0.4));
        chosenDirectoryTextField = new JTextField(" ");
        header              = new JXHeader();
        JLabel visitLabel   = new JLabel("Visiting cost:");
        JLabel absentLabel  = new JLabel("Absence cost:");
        JLabel switchLabel  = new JLabel("Switching cost:");
        JLabel timeLabel    = new JLabel("Search parameter");   
        JLabel cutoffLabel = new JLabel("Jaccard cutoff parameter");
        
        JButton fileChooserButton = new JButton("Choose file directory");
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fcListener = new FileChooserListener(fileChooser, chosenDirectoryTextField);
        fileChooserButton.addActionListener(fcListener);
        
        
        
        String subCommunityNetworkString = "sub-community model";
        String costNetworkString         = "cost model";
        subCommunityModelButton    = new JRadioButton(subCommunityNetworkString);
        costModelButton            = new JRadioButton(costNetworkString);
        
        subCommunityModelButton.setActionCommand(subCommunityNetworkString);
        costModelButton.setActionCommand(costNetworkString);
        
        subCommunityModelButton.setToolTipText("<html>The sub-community model identifies smaller communties"
                                                        + "<br>within larger communities. These smaller communities can"
                                                        + "<br>move between larger communities at each timestep.</html>");
        costModelButton.setToolTipText("<html>When using the cost model, the overall community membership pattern"
                                                    + "<br>of each node is decided by the minimization of a cost function"
                                                    + "<br>that uses the cost parameters given for the behaviours of"
                                                    + "<br>switching, visiting, and absence.</html>");
        visitLabel.setToolTipText("<html>A visiting cost is incurred by an individual node"
                                 + "<br>at each timestep during which that node visits a"
                                 + "<br>community of which it is not a member</html>");
        absentLabel.setToolTipText("<html>An absence cost is incurred by an individual node"
                                 + "<br>when that node is not participating in the community of"
                                 + "<br>which it is a member at a single timestep.</html>");
        switchLabel.setToolTipText("<html>A switching cost is incurred by an individual node"
                                 + "<br>each time that node changes its community membership.</html>");
        timeLabel.setToolTipText("<html>This parameter sets the number of timesteps included"
                                 + "<br>in the search for a possible earlier instance of a"
                                 + "<br>given community.</html>");
        cutoffLabel.setToolTipText("<html>This parameter sets the minimum value for the Jaccard "
                                 + "<br>Index that will be accepted for two groups to be considered"
                                 + "<br>part of the same community. A higher number requires two"
                                 + "<br>groups to me more similar. This parameter must be between 0 and 1.</html>");
        
        //ActionListener for RadioButtons
        RadioListener listener = new RadioListener(visitCostTextField, switchCostTextField, absentCostTextField, visitLabel, switchLabel, absentLabel);
        subCommunityModelButton.addActionListener(listener);
        costModelButton.addActionListener(listener);
        
        ButtonGroup groupOfButtons = new ButtonGroup();
        groupOfButtons.add(costModelButton);
        groupOfButtons.add(subCommunityModelButton);
        
        JPanel panelOfButtons = new JPanel();
        panelOfButtons.setLayout(new GridLayout(3,1));
        panelOfButtons.add(costModelButton);
        panelOfButtons.add(subCommunityModelButton);
        
        header.setTitle("Dynamic community detection:");
        header.setDescription("Identifies dynamic communities using the cost and sub-community models.");       
       
        BorderLayout layout = new BorderLayout();
        this.setLayout(layout);
        
        JPanel costPanel   = new JPanel();
        
        visitLabel.setHorizontalAlignment(JLabel.CENTER);
        absentLabel.setHorizontalAlignment(JLabel.CENTER);
        switchLabel.setHorizontalAlignment(JLabel.CENTER);
        timeLabel.setHorizontalAlignment(JLabel.CENTER);
        
        costPanel.setLayout(new GridLayout(7,4));
        costPanel.add(new JLabel());
        costPanel.add(visitLabel);
        costPanel.add(visitCostTextField);
        costPanel.add(new JLabel());
        costPanel.add(new JLabel());
        costPanel.add(absentLabel);
        costPanel.add(absentCostTextField);
        costPanel.add(new JLabel());
        costPanel.add(new JLabel());
        costPanel.add(switchLabel);
        costPanel.add(switchCostTextField);
        costPanel.add(new JLabel());
        costPanel.add(new JLabel());
        costPanel.add(timeLabel);
        costPanel.add(timeParameterTextField);
        costPanel.add(new JLabel());
        costPanel.add(new JLabel());
        costPanel.add(cutoffLabel);
        costPanel.add(cutoffParameterTextField);
        costPanel.add(new JLabel());
        costPanel.add(new JLabel());
        costPanel.add(new JLabel());
        costPanel.add(new JLabel());
        costPanel.add(new JLabel());
        costPanel.add(new JLabel());
        costPanel.add(fileChooserButton);
        costPanel.add(chosenDirectoryTextField);
        costPanel.add(new JLabel());
        
        this.add(header, BorderLayout.NORTH);
        this.add(panelOfButtons, BorderLayout.WEST);
        this.add(costPanel, BorderLayout.EAST);
    } 
}


class FileChooserListener implements ActionListener
{
    private JFileChooser fc;
    public String directoryName;
    JTextField directoryNameTextField;
    
    public FileChooserListener(JFileChooser chooser, JTextField textField)
    {
        this.fc = chooser;
        //directoryName = "";
        this.directoryNameTextField = textField;
    }

    @Override
    public void actionPerformed(ActionEvent e) 
    {
        int returnVal = fc.showOpenDialog(null);
        if(returnVal == JFileChooser.APPROVE_OPTION)
        {
            File chosenDirectory = fc.getSelectedFile();
            directoryName = chosenDirectory.getAbsolutePath();
            directoryNameTextField.setText(directoryName);
        }
    }
    
    public String getDirectoryName()
    {
        return directoryName;
    }   
}

class RadioListener implements ActionListener
{
    private JTextField visitTextField;
    private JTextField switchTextField;
    private JTextField absenceTextField;
    private JLabel visitLabel;
    private JLabel switchLabel;
    private JLabel absenceLabel;
    
    public RadioListener(JTextField visitTextField, JTextField switchTextField, JTextField absenceTextField, JLabel visitLabel, JLabel switchLabel, JLabel absenceLabel)
    {
        this.visitTextField = visitTextField;
        this.switchTextField = switchTextField;
        this.absenceTextField = absenceTextField;
        this.visitLabel = visitLabel;
        this.switchLabel = switchLabel;
        this.absenceLabel = absenceLabel;
    }
    
    @Override
    public void actionPerformed(ActionEvent e)
    {
        JRadioButton button = (JRadioButton) e.getSource();
        
        if(button.getText().equals("cost model"))
        {
            visitTextField.setEnabled(true);
            switchTextField.setEnabled(true);
            absenceTextField.setEnabled(true);
            visitLabel.setEnabled(true);
            switchLabel.setEnabled(true);
            absenceLabel.setEnabled(true);
        }
        else
        {
            visitTextField.setEnabled(false);
            switchTextField.setEnabled(false);
            absenceTextField.setEnabled(false);
            visitLabel.setEnabled(false);
            switchLabel.setEnabled(false);
            absenceLabel.setEnabled(false);
        }
    }
}