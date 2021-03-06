// filename: BenchmarkTable.java
// author  : DBGEN
// created : Tue May 31 17:52:19 CDT 2011 using database hd_ob83oax
// description: This class is used to get data from and put data into the
//              benchmark table of an IHFS database
//
package ohd.hseb.ihfsdb.generated;

import java.sql.*;

import java.util.*;

import ohd.hseb.db.*;

public class BenchmarkTable extends DbTable
{
//-----------------------------------------------------------------
//  Private data
//-----------------------------------------------------------------
    private int _recordsFound = -1;
//-----------------------------------------------------------------
//  BenchmarkTable() - constructor to set statement variable and initialize
//		number of records found to zero
//-----------------------------------------------------------------
    public BenchmarkTable(Database database) 
    {
        //Constructor calls DbTable's constructor
        super(database);
        setTableName("benchmark");
    }


    //-----------------------------------------------------------------
    //  select() - this method is called with a where clause and returns
    //		a List of BenchmarkRecord objects
    //-----------------------------------------------------------------
    public List select(String where) throws SQLException
    {
        BenchmarkRecord record = null;

        // create a List to hold Benchmark Records
        List recordList = new ArrayList();

        // set number of records found to zero
        _recordsFound = 0;

        // Create the SQL statement and issue it
        // construct the select statment
        String selectStatement = "SELECT * FROM benchmark " + where;

        // get the result set back from the query to the database
        ResultSet rs = getStatement().executeQuery(selectStatement);

        // loop through the result set
        while (rs.next())
        {
            // create an instance of a BenchmarkRecord
            // and store its address in oneRecord
            record = new BenchmarkRecord();

            // increment the number of records found
            _recordsFound++;

            // assign the data returned to the result set for one
            // record in the database to a BenchmarkRecord object

            record.setLid(getString(rs, 1));
            record.setBnum(getString(rs, 2));
            record.setElev(getDouble(rs, 3));
            record.setRemark(getString(rs, 4));
            
            // add this BenchmarkRecord object to the list
            recordList.add(record);
        }
        // Close the result set
        rs.close();

        // return a List which holds the BenchmarkRecord objects
        return recordList;

    } // end of select method

    //-----------------------------------------------------------------
    //  selectNRecords() - this method is called with a where clause and returns
    //		a List filled with a maximum of maxRecordCount of BenchmarkRecord objects 
    //-----------------------------------------------------------------
    public List selectNRecords(String where, int maxRecordCount) throws SQLException
    {
        BenchmarkRecord record = null;

        // create a List to hold Benchmark Records
        List recordList = new ArrayList();

        // set number of records found to zero
        _recordsFound = 0;

        // Create the SQL statement and issue it
        // construct the select statment
        String selectStatement = "SELECT * FROM benchmark " + where;

        // get the result set back from the query to the database
        ResultSet rs = getStatement().executeQuery(selectStatement);

        // loop through the result set
        while (rs.next())
        {
            // create an instance of a BenchmarkRecord
            // and store its address in oneRecord
            record = new BenchmarkRecord();

            // increment the number of records found
            _recordsFound++;

            // assign the data returned to the result set for one
            // record in the database to a BenchmarkRecord object

            record.setLid(getString(rs, 1));
            record.setBnum(getString(rs, 2));
            record.setElev(getDouble(rs, 3));
            record.setRemark(getString(rs, 4));
            
            // add this BenchmarkRecord object to the list
            recordList.add(record);
            if (_recordsFound >= maxRecordCount)
            {
                break;
            }
        }
        // Close the result set
        rs.close();

        // return a List which holds the BenchmarkRecord objects
        return recordList;

    } // end of selectNRecords method

//-----------------------------------------------------------------
//  insert() - this method is called with a BenchmarkRecord object and..
//-----------------------------------------------------------------
    public int insert(BenchmarkRecord record)  throws SQLException
    {
        int returnCode=-999;

        // Create a SQL insert statement and issue it
        // construct the insert statement
        PreparedStatement insertStatement = getConnection().prepareStatement(
" INSERT INTO benchmark VALUES (?, ?, ?, ?        )");

        setString(insertStatement, 1, record.getLid());
        setString(insertStatement, 2, record.getBnum());
        setDouble(insertStatement, 3, record.getElev());
        setString(insertStatement, 4, record.getRemark());
        
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
        String deleteStatement = "DELETE FROM benchmark " + where;

        // get the number of records processed by the delete
        returnCode = getStatement().executeUpdate(deleteStatement);

        return returnCode;
    } // end of delete method 

//-----------------------------------------------------------------
//  update() - this method is called with a BenchmarkRecord object and a where clause..
//-----------------------------------------------------------------
    public int update(BenchmarkRecord record, String where)  throws SQLException
    {
        int returnCode=-999;
        // Create a SQL update statement and issue it
        // construct the update statement
        PreparedStatement updateStatement = getConnection().prepareStatement(
" UPDATE benchmark SET lid = ?, bnum = ?, elev = ?, remark = ?        " + where );

        setString(updateStatement, 1, record.getLid());
        setString(updateStatement, 2, record.getBnum());
        setDouble(updateStatement, 3, record.getElev());
        setString(updateStatement, 4, record.getRemark());
        // get the number of records processed by the update
        returnCode = updateStatement.executeUpdate();

        return returnCode;

    } // end of updateRecord method

//-----------------------------------------------------------------
//  delete() - this method is called with a where clause and returns
//                   the number of records deleted
//-----------------------------------------------------------------
    public int delete(BenchmarkRecord record) throws SQLException
    {
        int returnCode=-999;

        // Create a SQL delete statement and issue it
        // construct the delete statement
        String deleteStatement = "DELETE FROM benchmark " + record.getWhereString();

        // get the number of records processed by the delete
        returnCode = getStatement().executeUpdate(deleteStatement);

        return returnCode;
    } // end of delete method 

//-----------------------------------------------------------------
//  update() - this method is called with a BenchmarkRecord object and a where clause..
//-----------------------------------------------------------------
    public int update(BenchmarkRecord oldRecord, BenchmarkRecord newRecord)  throws SQLException
    {
        int returnCode=-999;
        // Create a SQL update statement and issue it
        // construct the update statement
        PreparedStatement updateStatement = getConnection().prepareStatement(
" UPDATE benchmark SET lid = ?, bnum = ?, elev = ?, remark = ?        " + oldRecord.getWhereString() );

        setString(updateStatement, 1, newRecord.getLid());
        setString(updateStatement, 2, newRecord.getBnum());
        setDouble(updateStatement, 3, newRecord.getElev());
        setString(updateStatement, 4, newRecord.getRemark());
        // get the number of records processed by the update
        returnCode = updateStatement.executeUpdate();

        return returnCode;

    } // end of updateRecord method

//-----------------------------------------------------------------
//  insertOrUpdate() - this method is call with a BenchmarkRecord object.
//                   the number of records inserted or updated
//-----------------------------------------------------------------
    public int insertOrUpdate(BenchmarkRecord record) throws SQLException
    {
        int returnCode=-999;
        List recordList = select(record.getWhereString());

        if (recordList.size() < 1)
        {
            returnCode = insert(record);
        }
        else
        {
            BenchmarkRecord oldRecord = (BenchmarkRecord) recordList.get(0);
            returnCode = update(oldRecord, record);
        }
        return returnCode;
    } // end of insertOrUpdate() 
} // end of BenchmarkTable class
