// filename: ProductLinkRecord.java
// author  : DBGEN
// created : Tue May 31 17:52:16 CDT 2011 using database hd_ob83oax
// description: This class is used to get data from and put data into a
//              ProductLink table record format
//

package ohd.hseb.ihfsdb.generated;

import ohd.hseb.db.*;

public class ProductLinkRecord extends DbRecord
{
    private String lid;

    private String product_id;

    private long producttime;

    private long postingtime;

    //---------------------------------------------------------------
    // Empty constructor
    //---------------------------------------------------------------
    public ProductLinkRecord()
    {
    }

    //-----------------------------------------------------------------
    // Copy constructor
    //-----------------------------------------------------------------
    public ProductLinkRecord(ProductLinkRecord origRecord)
    {
        setLid(origRecord.getLid());
        setProduct_id(origRecord.getProduct_id());
        setProducttime(origRecord.getProducttime());
        setPostingtime(origRecord.getPostingtime());
    }

    //-----------------------------------------------------------------
    //  get and set methods for all data items in a ProductLink record

    //-----------------------------------------------------------------
    public String getLid()
    {
        return lid;
    }

    public void setLid(String lid)
    {
        this.lid = lid ;
    }

    public String getProduct_id()
    {
        return product_id;
    }

    public void setProduct_id(String product_id)
    {
        this.product_id = product_id ;
    }

    public long getProducttime()
    {
        return producttime;
    }

    public void setProducttime(long producttime)
    {
        this.producttime = producttime ;
    }

    public long getPostingtime()
    {
        return postingtime;
    }

    public void setPostingtime(long postingtime)
    {
        this.postingtime = postingtime ;
    }

//-----------------------------------------------------------------
//  getWhereString() - this method is called with no arguments
//  and returns a String that contains a valid where clause containing all the
//  primary key fields.
//-----------------------------------------------------------------
    public String getWhereString()
    {
        String outString = 
                "WHERE lid = '" + lid + "'" 
                 + " AND product_id = '" + product_id + "'" 
                 + " AND producttime = '" +  getDateTimeStringFromLongTime(producttime) + "'" 
                 + " AND postingtime = '" +  getDateTimeStringFromLongTime(postingtime) + "'" 
                ;
        return outString;
    } // end toString()
//-----------------------------------------------------------------
//  toString() - this method is called with no arguments
//  and returns a String of the internal values
//-----------------------------------------------------------------
    public String toString()
    {
        String outString = 
                getLid() + " " +
                getProduct_id() + " " +
                getDateTimeStringFromLongTime(getProducttime()) + " " +
                getDateTimeStringFromLongTime(getPostingtime()) + " " +
                "" ;
        return outString;
    } // end toString()
} // end of ProductLinkRecord class

