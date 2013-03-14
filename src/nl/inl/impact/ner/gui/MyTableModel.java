package nl.inl.impact.ner.gui;

import java.util.HashMap;

import javax.swing.table.AbstractTableModel;

public class MyTableModel extends AbstractTableModel{
		
	private static final long serialVersionUID = 1L;
		HashMap<String, Boolean> filelist = new HashMap<String, Boolean>();
		private String[] columnNames = {"Lemma", "File"};
		private Object[][] data = {{"", ""}};
		int filelistsize = filelist.size();
		
		public MyTableModel(HashMap<String, Boolean> files){
	    	filelist.clear();
	    	fillList(files);
	    	/*
	    	if(files.size() > 0){
		    	data = new Object[files.size()][2];
		    	int counter = 0;
		    	for(String s : files.keySet()){
		    		boolean b = files.get(s);
		    		data[counter][0] = b;
		    		data[counter][1] = s;
		    		counter++;
		    	}
	    	}
	    	*/
	    }
		/*
		public void tableChanged(TableModelEvent e) {
	        int row = e.getFirstRow();
	        int column = e.getColumn();
	        TableModel model = (TableModel)e.getSource();
	        String columnName = model.getColumnName(column);
	        Object data = model.getValueAt(row, column);
	        System.err.println("e="+e+" columname="+columnName+" data="+data);
		}
		*/
		public void clearRows(){
			
		}
		
		
		public void fillList(HashMap<String, Boolean> files){
			System.err.println("recreating the file list with "+files.size()+" file(s)" );
	    	filelist = files;
			if(filelist.size() > 0){
		    	data = new Object[files.size()][2];
		    	int counter = 0;
		    	for(String s : files.keySet()){
		    		boolean b = files.get(s);
		    		data[counter][0] = b;
		    		data[counter][1] = s;
		    		counter++;
		    	}
	    	}
			System.err.println("data.length()="+data.length);
			if( (filelist.size() > 0 ) && (filelist.size() > filelistsize ) ){
				//file inserted
				System.err.println("Inserted a file. filelist.size()="+filelist.size()+" filelistsize="+filelistsize);
				fireTableRowsInserted(0, filelist.size());
				filelistsize = filelist.size();
			}
			if( (filelist.size() >= 0 ) && (filelist.size() < filelistsize ) ){
				//file deleted
				System.err.println("Deleted a file. filelist.size()="+filelist.size()+" filelistsize="+filelistsize);
				//fireTableRowsDeleted(0, filelist.size());
				fireTableRowsDeleted(filelistsize, filelistsize);
				filelistsize = filelist.size();
			}
			System.err.println("Done. filelist.size()="+filelist.size()+" filelistsize="+filelistsize);
			/*
			if( ( filelist.size() == 0) && (filelist.size() != filelistsize ) ){
				System.err.println("filelist.size()="+filelist.size()+" filelistsize="+filelistsize);
				fireTableRowsDeleted(0, filelist.size());
				filelistsize = filelist.size();
			}
			*/
			System.err.println("filesize: "+filelist.size());
		}
	    
		 //private Object[][] data = {{}};
	   
	    
	    public int getColumnCount() {
	        return columnNames.length;
	    }

	    public int getRowCount() {
	        return data.length;
	    }

	    public String getColumnName(int col) {
	        return columnNames[col];
	    }

	    public Object getValueAt(int row, int col) {
	    	if( (row>=0) && (col >=0) ){
	    		return data[row][col];
	    	}
	    	else{
	    		return -1;
	    	}
	    }

	    /*
	     * JTable uses this method to determine the default renderer/
	     * editor for each cell.  If we didn't implement this method,
	     * then the last column would contain text ("true"/"false"),
	     * rather than a check box.
	     */
	    public Class getColumnClass(int c) {
	        return getValueAt(0, c).getClass();
	    }

	    /*
	     * Don't need to implement this method unless your table's
	     * editable.
	     */
	    public boolean isCellEditable(int row, int col) {
	        //Note that the data/cell address is constant,
	        //no matter where the cell appears onscreen.
	        //if (col < 2) {
	         //   return false;
	        //} else {
	            return true;
	        //}
	    }

	    /*
	     * Don't need to implement this method unless your table's
	     * data can change.
	     */
	    public void setValueAt(Object value, int row, int col) {
	        data[row][col] = value;
	        fireTableCellUpdated(row, col);
	    }
	    
	    public void insertRows(){
	    	fireTableRowsInserted(filelist.size(), filelist.size()+1);
	    }
	    /*
	    public void deleteRow(){
	    	fireTableRowsDeleted(filelist.size(), filelist.size());
	    }
	    */
	    
	    
	}

