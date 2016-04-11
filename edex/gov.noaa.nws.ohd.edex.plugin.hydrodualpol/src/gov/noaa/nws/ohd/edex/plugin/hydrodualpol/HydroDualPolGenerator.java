/**
 * This software was developed by HSEB, OHD
 **/
package gov.noaa.nws.ohd.edex.plugin.hydrodualpol;

import gov.noaa.nws.ohd.edex.plugin.hydrodualpol.common.HydroDualPolConfig;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.edex.urifilter.URIFilter;
import com.raytheon.edex.urifilter.URIGenerateMessage;
import com.raytheon.uf.common.dataplugin.radar.RadarRecord;
import com.raytheon.uf.common.dataplugin.radar.util.RadarsInUseUtil;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.edex.cpgsrv.CompositeProductGenerator;
import com.raytheon.uf.edex.database.dao.CoreDao;
import com.raytheon.uf.edex.database.dao.DaoConfig;

/**
 * 
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * May 13, 2013            jtDeng     Initial creation
 * August 2015 DR 17558    JtDeng HPE/DHR stacktrace and housekeep
 * 
 * This HydroDualPolGenerator creates data URI filters for dual pol radar products
 * DSA/DPR/DAA, it retrieves these radar records from "metadata" database and HDF5 files
 * once get notified. 
 * It also generates the post-processing DSA/DPR/DAA products in specific format and 
 * stores in IHFS database as inputs to MPE/HPE/HPN programs.
 * April  2016 DCS 18497  Build 17 DSA adaptable parameters changes 
 *                        write the log message to separate edex-ingestDat-hydrodualpol-yyymmdd.log file
 * </pre>
 * 
 * @author deng2
 * @version 1.0
 */

public class HydroDualPolGenerator extends CompositeProductGenerator {
    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(HydroDualPolGenerator.class);

    private static final String GEN_NAME = "HydroDualPol";

    private static final String PRODUCT_TYPE = "hydrodualpol";

    private static final Logger logger = LoggerFactory
            .getLogger("HydroDualPolLog");

    /** Set of icaos to filter for **/
    private Set<String> icaos = null;

    /** public constructor for HydroDualPolGenerator **/
    public HydroDualPolGenerator(String name, String compositeProductType) {
        super(GEN_NAME, PRODUCT_TYPE);
    }

    /** default thrift constructor **/
    public HydroDualPolGenerator() {
        super(GEN_NAME, PRODUCT_TYPE);
    }

    @Override
    protected void configureFilters() {

        logger.info(getGeneratorName() + " process Filter Config...");
        icaos = new HashSet<String>(RadarsInUseUtil.getSite(null,
                RadarsInUseUtil.LOCAL_CONSTANT));
        icaos.addAll(RadarsInUseUtil.getSite(null,
                RadarsInUseUtil.DIAL_CONSTANT));
    }

    @Override
    protected void createFilters() {

        String header = "HydroDualPolGenerator.createFilters(): ";
        ArrayList<URIFilter> tmp = new ArrayList<URIFilter>(icaos.size());
        Iterator<String> iter = icaos.iterator();

        int radarCount = 0;
        while (iter.hasNext()) {
            String icao = iter.next();
            /* retrieve the radar_id with radar_use as T in radarLoc table */
            if (checkRadarIdinUse(icao)) {
                try {
                    tmp.add(new HydroDualPolURIFilter(icao,
                            HydroDualPolURIFilter.daa));
                    tmp.add(new HydroDualPolURIFilter(icao,
                            HydroDualPolURIFilter.dpr));
                    tmp.add(new HydroDualPolURIFilter(icao,
                            HydroDualPolURIFilter.dsa));

                    radarCount++;

                    logger.info(header + " radar also in RadarLoc table # "
                            + radarCount + " radar id = " + icao);

                } catch (Exception e) {

                    logger.error("Couldn't create HydroDualPol URIFilter.."
                            + icao + " is not a known RADAR site.");
                    iter.remove();
                }
            } else {

                logger.warn("Couldn't create HydroDualPol URIFilter for "
                        + icao + " -- invalid radarId in RadarLoc table.");
                iter.remove();
            }
        }
        filters = tmp.toArray(new HydroDualPolURIFilter[tmp.size()]);
    }

    /**
     * check if the radarId is in radarLoc table and use_radar field as T
     * 
     * @param icao
     *            - radarId
     * @return true or false
     */
    private boolean checkRadarIdinUse(String icao) {

        String query = String
                .format("select radid from radarloc where use_radar='T' and radid = '"
                        + icao.substring(1).toUpperCase() + "'");

        CoreDao dao = new CoreDao(DaoConfig.forDatabase("ihfs"));
        Object[] results = dao.executeSQLQuery(query);
        for (Object result : results) {

            if (result != null) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void generateProduct(URIGenerateMessage genMessage) {
        HydroDualPolConfig hydrodualpol_config = null;

        /** test to retrieve the following fields from radar record **/
        try {

            statusHandler.handle(Priority.INFO,
                    "Process DSA, DPR, DAA products via HydroDualPol.");

            hydrodualpol_config = new HydroDualPolConfig(
                    (HydroDualPolURIGenerateMessage) genMessage, this);

            String productType = hydrodualpol_config.getProductType();

            if (productType.equalsIgnoreCase("DSA")) {
                RadarRecord record = hydrodualpol_config.getDSA();

                if (record != null) {
                    processDSAProduct(record);
                } else {

                    logger.warn("DSA product not found...");
                }
            } else if (productType.equalsIgnoreCase("DPR")) {

                RadarRecord record = hydrodualpol_config.getDPR();

                if (record != null) {
                    processDPRProduct(record);
                } else {

                    logger.warn("DPR product not found...");
                }

            }

            else if (productType.equalsIgnoreCase("DAA")) {
                RadarRecord record = hydrodualpol_config.getDAA();

                if (record != null) {
                    processDAAProduct(record);
                } else {

                    logger.warn("DAA product not found...");
                }
            }
        } catch (Exception e) {
            statusHandler
                    .handle(Priority.ERROR, "Can not run HydroDualPol.", e);
            logger.error("Can not run HydroDualPol.", e);

        }
    } // end generateProduct()

    private void processDAAProduct(RadarRecord record) {

        DAAProductProcessor processor = new DAAProductProcessor(logger);

        processor.process(record);

    }

    private void processDPRProduct(RadarRecord record) {

        DPRProductProcessor processor = new DPRProductProcessor(logger);

        processor.process(record);

    }

    private void processDSAProduct(RadarRecord record) {

        DSAProductProcessor processor = new DSAProductProcessor(logger);

        processor.process(record);

    }

    @Override
    public boolean isRunning() {
        return getConfigManager().getHydroDualPolState();
    }

}
