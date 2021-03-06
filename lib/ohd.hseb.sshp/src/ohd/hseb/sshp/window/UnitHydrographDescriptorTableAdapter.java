/*
 * Created on Aug 19, 2004
 *
 *
 */
package ohd.hseb.sshp.window;

import java.util.List;

import ohd.hseb.model.DurationCode;
import ohd.hseb.model.UnitHydrographDescriptor;
import ohd.hseb.sshp.gui.TableAdapter;
import ohd.hseb.util.gui.TableColumnComparator;


/**
 * @author GobsC
 *
 * 
 */
public class UnitHydrographDescriptorTableAdapter extends TableAdapter
{
    
    private UnitHydrographDescriptor _descriptor = null;
    private List _descriptorList = null;
    
    // ---------------------------------------------------------------------------------
     
    
    
    public UnitHydrographDescriptorTableAdapter(TableColumnComparator comparator,
                                            String[] columnNameArray,
                                            List dataList)
    {
        super(comparator, columnNameArray, dataList);
        
        setAllowMultipleSelection(true);
          
    }
    
    // ---------------------------------------------------------------------------------
    public String[][] getDataStringArrayFromDataList(List dataList)
    {
        int fieldCount = 3;
        String[][] dataArray = new String[dataList.size()][fieldCount];
        
        
        for (int i = 0; i < dataList.size(); i++)
        {
            UnitHydrographDescriptor desc = (UnitHydrographDescriptor) dataList.get(i);
            dataArray[i][0] = desc.getBasinId();
            dataArray[i][1] = desc.getModel();
            dataArray[i][2] = "" + DurationCode.getHoursFromCode(desc.getDuration());
        }
        
        return dataArray;
        
    }
    // ---------------------------------------------------------------------------------
    public void refreshTableModel(List dataList)
    {
        setDataList(dataList);// for super class
        
        _descriptorList = dataList;
      
        String[][] dataStringArray = getDataStringArrayFromDataList(dataList);
    	getTableModel().updateData( dataStringArray );
    	getTableModel().fireTableChanged( null );
  

        return;
        
    }
    // ---------------------------------------------------------------------------------

    public int getDescriptorCount()
    {
        int count =  getDataList().size();
        return count;
    }
    
    // ---------------------------------------------------------------------------------

    public UnitHydrographDescriptor getDescriptorByIndex(int index)
    {
        List dataList = getDataList();
        UnitHydrographDescriptor desc = null;
        
        if ( dataList.size() > 0 )
        {
            Object object = dataList.get(index);
            desc = (UnitHydrographDescriptor) object;
        }
        return desc;
    }
    
    // ---------------------------------------------------------------------------------

    private int findIndexOfDescriptor(UnitHydrographDescriptor desc)
    {
        int index = _descriptorList.indexOf(desc);
        
        return index;
    }
    // ------------------------------------------------------------------------------------- 
     public void setDataList(List dataList)
    {
     
        super.setDataList(dataList);
        _descriptorList = dataList;
        
    }
  
     // ---------------------------------------------------------------------------------
     
    
    private void selectAll()
    {
        
        getListSelectionModel().setSelectionInterval(0, getTable().getModel().getRowCount()-1);
    }
    
    
    // ---------------------------------------------------------------------------------
    public void setSelectedIndex(int index)
    {
         
        getListSelectionModel().setSelectionInterval(index, index);
      
    }
    // ---------------------------------------------------------------------------------
    
    public void selectItems(List selectedDescriptorList)
    {
        getListSelectionModel().clearSelection();
        
        for (int i = 0; i < selectedDescriptorList.size(); i++)
        {
            UnitHydrographDescriptor descriptor = (UnitHydrographDescriptor) selectedDescriptorList.get(i);
           
            int index = findIndexOfDescriptor(descriptor);
            
            if (index >= 0)
            {
                getListSelectionModel().setSelectionInterval(index, index);
                  
            }
        }
    }
    // ---------------------------------------------------------------------------------
    
}
