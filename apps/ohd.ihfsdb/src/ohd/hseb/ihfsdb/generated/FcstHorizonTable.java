// filename: FcstHorizonTable.java
// author  : DBGEN
// created : Tue May 31 17:52:22 CDT 2011 using database hd_ob83oax
// description: This class is used to get data from and put data into the
//              fcsthorizon table of an IHFS database
//
package ohd.hseb.ihfsdb.generated;

import java.sql.*;

import java.util.*;

import ohd.hseb.db.*;

public class FcstHorizonTable extends DbTable
{
//-----------------------------------------------------------------
//  Private data
//-----------------------------------------------------------------
    private int _recordsFound = -1;
//-----------------------------------------------------------------
//  FcstHorizonTable() - constructor to set statement variable and initialize
//		number of records found to zero
//-----------------------------------------------------------------
    public FcstHorizonTable(Database database) 
    {
        //Constructor calls DbTable's constructor
        super(database);
        setTableName("fcsthorizon");
    }


    //-----------------------------------------------------------------
    //  select() - this method is called with a where clause and returns
    //		a List of FcstHorizonRecord objects
    //-----------------------------------------------------------------
    public List select(String where) throws SQLException
    {
        FcstHorizonRecord record = null;

        // create a List to hold FcstHorizon Records
        List recordList = new ArrayList();

        // set number of records found to zero
        _recordsFound = 0;

        // Create the SQL statement and issue it
        // construct the select statment
        String selectStatement = "SELECT * FROM fcsthorizon " + where;

        // get the result set back from the query to the database
        ResultSet rs = getStatement().executeQuery(selectStatement);

        // loop through the result set
        while (rs.next())
        {
            // create an instance of a FcstHorizonRecord
            // and store its address in oneRecord
            record = new FcstHorizonRecord();

            // increment the number of records found
            _recordsFound++;

            // assign the data returned to the result set for one
            // record in the database to a FcstHorizonRecord object

            record.setFcst_horizon(getString(rs, 1));
            
            // add this FcstHorizonRecord object to the list
            recordList.add(record);
        }
        // Close the result set
        rs.close();

        // return a List which holds the FcstHorizonRecord objects
        return recordList;

    } // end of select method

    //-----------------------------------------------------------------
    //  selectNRecords() - this method is called with a where clause and returns
    //		a List filled with a maximum of maxRecordCount of FcstHorizonRecord objects 
    //-----------------------------------------------------------------
    public List selectNRecords(String where, int maxRecordCount) throws SQLException
    {
        FcstHorizonRecord record = null;

        // create a List to hold FcstHorizon Records
        List recordList = new ArrayList();

        // set number of records found to zero
        _recordsFound = 0;

        // Create the SQL statement and issue it
        // construct the select statment
        String selectStatement = "SELECT * FROM fcsthorizon " + where;

        // get the result set back from the query to the database
        ResultSet rs = getStatement().executeQuery(selectStatement);

        // loop through the result set
        while (rs.next())
        {
            // create an instance of a FcstHorizonRecord
            // and store its address in oneRecord
            record = new FcstHorizonRecord();

            // increment the number of records found
            _recordsFound++;

            // assign the data returned to the result set for one
            // record in the database to a FcstHorizonRecord object

            record.setFcst_horizon(getString(rs, 1));
            
            // add this FcstHorizonRecord object to the list
            recordList.add(record);
            if (_recordsFound >= maxRecordCount)
            {
                break;
            }
        }
        // Close the result set
        rs.close();

        // return a List which holds the FcstHorizonRecord objects
        return recordList;

    } // end of selectNRecords method

//-----------------------------------------------------------------
//  insert() - this method is called with a FcstHorizonRecord object and..
//-----------------------------------------------------------------
    public int insert(FcstHorizonRecord record)  throws SQLException
    {
        int returnCode=-999;

        // Create a SQL insert statement and issue it
        // construct the insert statement
        PreparedStatement insertStatement = getConnection().prepareStatement(
" INSERT INTO fcsthorizon VALUES (?        )");

        setString(insertStatement, 1, record.getFcst_horizon());
        
        // get the number of records processed by the insert
        returnCode = insertStatement.executeUpdate();

        return returnCode;

    } // end of insert method

//-----------------------------------------------------------------
//  delete() - this method is called with a where clause and returns
//                   the number of records deleted
//-----------------------------------------------------------------
    public int delete(String where) throws SQLException
    {
        int returnCode=-999;

        // Create a SQL delete statement and issue it
        // construct the delete statement
        String deleteStatement = "DELETE FROM fcsthorizon " + where;

        // get the number of records processed by the delete
        returnCode = getStatement().executeUpdate(deleteStatement);

        return returnCode;
    } // end of delete method 

//-----------------------------------------------------------------
//  update() - this method is called with a FcstHorizonRecord object and a where clause..
//-----------------------------------------------------------------
    public int update(FcstHorizonRecord record, String where)  throws SQLException
    {
        int returnCode=-999;
        // Create a SQL update statement and issue it
        // construct the update statement
        PreparedStatement updateStatement = getConnection().prepareStatement(
" UPDATE fcsthorizon SET fcst_horizon = ?        " + where );

        setString(updateStatement, 1, record.getFcst_horizon());
        // get the number of records processed by the update
        returnCode = updateStatement.executeUpdate();

        return returnCode;

    } // end of updateRecord method

//-----------------------------------------------------------------
//  delete() - this method is called with a where clause and returns
//                   the number of records deleted
//-----------------------------------------------------------------
    public int delete(FcstHorizonRecord record) throws SQLException
    {
        int returnCode=-999;

        // Create a SQL delete statement and issue it
        // construct the delete statement
        String deleteStatement = "DELETE FROM fcsthorizon " + record.getWhereString();

        // get the number of records processed by the delete
        returnCode = getStatement().executeUpdate(deleteStatement);

        return returnCode;
    } // end of delete method 

//-----------------------------------------------------------------
//  update() - this method is called with a FcstHorizonRecord object and a where clause..
//-----------------------------------------------------------------
    public int update(FcstHorizonRecord oldRecord, FcstHorizonRecord newRecord)  throws SQLException
    {
        int returnCode=-999;
        // Create a SQL update statement and issue it
        // construct the update statement
        PreparedStatement updateStatement = getConnection().prepareStatement(
" UPDATE fcsthorizon SET fcst_horizon = ?        " + oldRecord.getWhereString() );

        setString(updateStatement, 1, newRecord.getFcst_horizon());
        // get the number of records processed by the update
        returnCode = updateStatement.executeUpdate();

        return returnCode;

    } // end of updateRecord method

//-----------------------------------------------------------------
//  insertOrUpdate() - this method is call with a FcstHorizonRecord object.
//                   the number of records inserted or updated
//-----------------------------------------------------------------
    public int insertOrUpdate(FcstHorizonRecord record) throws SQLException
    {
        int returnCode=-999;
        List recordList = select(record.getWhereString());

        if (recordList.size() < 1)
        {
            returnCode = insert(record);
        }
        else
        {
            FcstHorizonRecord oldRecord = (FcstHorizonRecord) recordList.get(0);
            returnCode = update(oldRecord, record);
        }
        return returnCode;
    } // end of insertOrUpdate() 
} // end of FcstHorizonTable class
