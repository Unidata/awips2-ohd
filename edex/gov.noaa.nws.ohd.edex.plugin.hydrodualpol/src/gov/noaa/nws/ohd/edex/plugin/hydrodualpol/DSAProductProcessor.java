package gov.noaa.nws.ohd.edex.plugin.hydrodualpol;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import com.raytheon.uf.common.dataplugin.radar.RadarRecord;
import com.raytheon.uf.common.dataplugin.radar.level3.Layer;
import com.raytheon.uf.common.dataplugin.radar.level3.SymbologyBlock;
import com.raytheon.uf.common.dataplugin.radar.level3.SymbologyPacket;
import com.raytheon.uf.common.dataplugin.radar.level3.TextSymbolPacket;
import com.raytheon.uf.common.dataplugin.shef.tables.DSAAdapt;
import com.raytheon.uf.common.dataplugin.shef.tables.DSAAdaptId;
import com.raytheon.uf.common.dataplugin.shef.tables.DSARadar;
import com.raytheon.uf.common.dataplugin.shef.tables.DSARadarId;
import com.raytheon.uf.common.ohd.AppsDefaults;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.edex.database.dao.CoreDao;
import com.raytheon.uf.edex.database.dao.DaoConfig;

/**
 * Class to handle DSA radar product header processing for HPE/HPN.
 * <p>
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * July 2013 DCS 167    P. Tilles   Initial Creation
 * August 2015 DR 17558  JtDeng HPE/DHR stacktrace and housekeep
 * </pre>
 * 
 * 
 * @author Paul Tilles
 * 
 */

public class DSAProductProcessor {

    private String outputGridDirectory = "/tmp";
    private static final int MAX_IHRAP = PolarToQuarterHRAPTransformer.MAX_IHRAP;
    private static final int MAX_JHRAP = PolarToQuarterHRAPTransformer.MAX_JHRAP;
    private static final short NO_DATA_FLAG = 0;

    IUFStatusHandler statusHandler = null;

    // ---------------------------------------------------------------------

    public DSAProductProcessor(IUFStatusHandler statusHandler) {
        this.statusHandler = statusHandler;

        readAppsDefaults();
    }

    // ---------------------------------------------------------------------

    private void readAppsDefaults() {
        AppsDefaults ad = AppsDefaults.getInstance();

        outputGridDirectory = ad.getToken("dsa_grid_dir", null);

    }

    // ---------------------------------------------------------------------

    public void process(RadarRecord record) {
        DSAHeaderData headerData = new DSAHeaderData();

        processHeader(record, headerData);

        if (headerData.isProductNull()) {
            processNullProduct(record, headerData);
        } else {
            processGriddedDataProduct(record, headerData);

        }

    }

    // ---------------------------------------------------------------------
    private void processNullProduct(RadarRecord record, DSAHeaderData headerData) {

        statusHandler.handle(
                Priority.DEBUG,
                "DSA product is a null product" + "\n" + " radar id  = "
                        + headerData.getRadarId() + " obsTime  = "
                        + headerData.getObsTime() + " volumeCoveragePattern = "
                        + headerData.getVolumeCoveragePattern()
                        + " operationalMode = "
                        + headerData.getOperationalMode() + " biasValue = "
                        + headerData.getBiasValue() + " nullProductFlag = "
                        + headerData.getNullProductFlag());

        processNullProductString(record);

    }

    // ---------------------------------------------------------------------
    private void processNullProductString(RadarRecord record) {
        SymbologyBlock symbologyBlock = record.getSymbologyBlock();

        if (symbologyBlock == null) {

            statusHandler.handle(Priority.INFO,
                    "Null Product text not found (symbology block not found.");

            return;
        }

        int nlayer = 0;
        for (Layer layer : symbologyBlock.getLayers()) {
            int npacket = 0;
            nlayer++;
            for (SymbologyPacket packet : layer.getPackets()) {
                npacket++;
                if (packet instanceof TextSymbolPacket) {
                    TextSymbolPacket textSymbolPacket = (TextSymbolPacket) packet;

                    if (nlayer == 1 && npacket == 1) {

                        // print null product string
                        // packet #1 in layer #1 contains the null product
                        // string

                        String nullProductText = textSymbolPacket.getTheText()
                                .trim();
                        statusHandler.handle(Priority.DEBUG, nullProductText);

                        break;
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------------

    private void processGriddedDataProduct(RadarRecord record,
            DSAHeaderData headerData) {

        statusHandler.handle(
                Priority.DEBUG,
                "\n" + "DSA product:  radar id  = " + headerData.getRadarId()
                        + " obsTime  = " + headerData.getObsTime()
                        + " volumeCoveragePattern = "
                        + headerData.getVolumeCoveragePattern()
                        + " operationalMode = "
                        + headerData.getOperationalMode() + "\n" + " maxVal = "
                        + headerData.getMaxVal() + " scale = "
                        + headerData.getScale() + " offset = "
                        + headerData.getOffSet() + "\n" + " begin_time = "
                        + headerData.getBeginTimeString() + " end_time = "
                        + headerData.getEndTimeString() + " j_beg_date = "
                        + headerData.getStormTotalStartDate()
                        + " j_beg_time = "
                        + headerData.getStormTotalStartTime()
                        + " j_end_date = " + headerData.getStormTotalEndDate()
                        + " j_end_time = " + headerData.getStormTotalEndTime()
                        + "\n" + " biasValue = " + headerData.getBiasValue()
                        + " nullProductFlag = "
                        + headerData.getNullProductFlag() + " fileName = "
                        + headerData.getFileName());

        byte[] dataArray = record.getRawData();

        if (dataArray == null) {
            statusHandler.handle(Priority.ERROR,
                    "DSA Product: ERROR --- dataArray is null");
        }

        else {
            int numRangeBins = record.getNumBins();
            int numRadials = record.getNumRadials();

            float lat = record.getLatitude();
            float lon = record.getLongitude();

            float scale = headerData.getScale();
            float offset = headerData.getOffSet();

            // transform polar grid to HRAP

            float[][] polarGrid = getPolarGridFromDataArray(dataArray, scale,
                    offset, numRadials, numRangeBins);

            PolarToQuarterHRAPTransformer transformer = new PolarToQuarterHRAPTransformer(
                    statusHandler);
            float[][] hrapGrid = transformer
                    .transform250MeterPolarToQuarterHRAP(polarGrid, lat, lon);

            writeToDSARadarTable(headerData);

            writeOutDataFile(record, headerData, hrapGrid,
                    headerData.getFileName());

            // process adaptable parameters and write to DSAAdapt table

            String radid = headerData.getRadarId();
            String obstime = headerData.getObsTime();
            processAdaptableParameters(record, radid, obstime);

        }

    }

    // ----------------------------------------------------------

    private void processAdaptableParameters(RadarRecord record, String radid,
            String obstime) {

        SymbologyBlock symbologyBlock = record.getSymbologyBlock();

        if (symbologyBlock == null) {

            statusHandler.handle(Priority.DEBUG, "symbology block not found.");

            return;
        }

        String[] productText = new String[4];

        // layer #1 has 0 packets
        // in layer #2, packets 1-4 contain the adaptable parameters text

        int nlayer = 0;
        int nstring = 0;
        for (Layer layer : symbologyBlock.getLayers()) {
            int npacket = 0;
            nlayer++;
            for (SymbologyPacket packet : layer.getPackets()) {
                npacket++;
                if (packet instanceof TextSymbolPacket) {
                    TextSymbolPacket textSymbolPacket = (TextSymbolPacket) packet;

                    // print text

                    productText[nstring] = textSymbolPacket.getTheText().trim();
                    statusHandler.handle(Priority.DEBUG, productText[nstring]);
                    nstring++;

                    if (nlayer == 2 && npacket == 4)
                        break;

                } // end if (packet instanceof TextSymbolPacket)
            } // end for (SymbologyPacket packet : layer.getPackets())
        } // end for (Layer layer : symbologyBlock.getLayers())

        // parse 4 strings to get the adaptable parameters

        // parse string 1

        String productText0 = productText[0];

        String[] valuesFromString1 = new String[10];

        // remove "ADAP(" and ")" from string leaving "36" as the first
        // substring
        valuesFromString1[0] = productText0.substring(0, 8)
                .replace("ADAP(", "").replace(")", "");

        for (int i = 1; i < 10; i++) {
            valuesFromString1[i] = productText0.substring((i * 8),
                    ((i + 1) * 8));
        }

        if (statusHandler.isPriorityEnabled(Priority.DEBUG)) {
            for (int i = 0; i < valuesFromString1.length; i++) {
                statusHandler.handle(Priority.DEBUG, valuesFromString1[i]);
            }
        }

        // parse string 2
        String[] valuesFromString2 = productText[1].split("[ ]+");

        // parse string 3
        String[] valuesFromString3 = productText[2].split("[ ]+");

        // parse string 4
        int index = productText[3].indexOf("SUPL");
        String productText3 = productText[3].substring(0, index);

        String[] valuesFromString4 = productText3.split("[ ]+");

        // process DSA adaptable parameters from strings into proper types
        // write adaptable parameter values to DSAAdapt table

        processDSAAdaptParameters(radid, obstime, valuesFromString1,
                valuesFromString2, valuesFromString3, valuesFromString4);

    }

    // -------------------------------------------------------------------------------

    private void processDSAAdaptParameters(String radid, String obstime,
            String[] stringArray1, String[] stringArray2,
            String[] stringArray3, String[] stringArray4) {

        try {
            // process string arrays into individual values
            Short numOfAdap = Short.valueOf(stringArray1[0]);
            Float defaultMLDepth = Float.valueOf(stringArray1[1]);
            String mlOverideFlag = stringArray1[2];
            Float kdpMult = Float.valueOf(stringArray1[3]);
            Float kdpPower = Float.valueOf(stringArray1[4]);
            Float zrMult = Float.valueOf(stringArray1[5]);
            Float zrPower = Float.valueOf(stringArray1[6]);
            Float zdrzMult = Float.valueOf(stringArray1[7]);
            Float zdrzPower = Float.valueOf(stringArray1[8]);
            Float zdrzdrPower = Float.valueOf(stringArray1[9]);

            Float minCorrPrecip = Float.valueOf(stringArray2[0]);
            Float minCorrKDP = Float.valueOf(stringArray2[1]);
            Float reflMax = Float.valueOf(stringArray2[2]);
            Float kdpMaxBeamBlk = Float.valueOf(stringArray2[3]);
            float maxUsabilityBlk = 0.f; // equals "N/A" in product
                                         // (stringArray2[4])
            Float kdpMinUsageRate = Float.valueOf(stringArray2[5]);
            Float wsMult = Float.valueOf(stringArray2[6]);
            Float grMult = Float.valueOf(stringArray2[7]);
            Float rhMult = Float.valueOf(stringArray2[8]);
            Float dsMult = Float.valueOf(stringArray2[9]);

            Float icMult = Float.valueOf(stringArray3[0]);
            Float gridIsFull = Float.valueOf(stringArray3[1]);
            Float paifRate = Float.valueOf(stringArray3[2]);
            Float paifArea = Float.valueOf(stringArray3[3]);
            Float rainTimeThresh = Float.valueOf(stringArray3[4]);
            Float numZones = Float.valueOf(stringArray3[5]);
            Float maxPrecipRate = Float.valueOf(stringArray3[6]);
            // skip stringArray3[7], stringArray3[8], stringArray3[9]

            Float restartTime = Float.valueOf(stringArray4[0]);
            Float maxInterpTime = Float.valueOf(stringArray4[1]);
            Float maxHourlyAcc = Float.valueOf(stringArray4[2]);
            Float timeBias = Float.valueOf(stringArray4[3]);
            Float numGRPairs = Float.valueOf(stringArray4[4]);
            Float resetBias = Float.valueOf(stringArray4[5]);
            Float longstLag = Float.valueOf(stringArray4[6]);

            // write record to DSAAdapt table

            writeToDSAAdapt(radid, obstime, numOfAdap, defaultMLDepth,
                    mlOverideFlag, kdpMult, kdpPower, zrMult, zrPower,
                    zdrzMult, zdrzPower, zdrzdrPower, minCorrPrecip,
                    minCorrKDP, reflMax, kdpMaxBeamBlk, maxUsabilityBlk,
                    kdpMinUsageRate, wsMult, grMult, rhMult, dsMult, icMult,
                    gridIsFull, paifRate, paifArea, rainTimeThresh, numZones,
                    maxPrecipRate, restartTime, maxInterpTime, maxHourlyAcc,
                    timeBias, numGRPairs, resetBias, longstLag);

            statusHandler
                    .handle(Priority.DEBUG,
                            "In routine processDSAAdaptParameter - finish to write to DSAAdapt table");
        } catch (Exception e) {
            statusHandler
                    .handle(Priority.ERROR,
                            "In routine processDSAAdaptParameter -can not parse value.",
                            e);

        }
    }

    // ---------------------------------------------------------------------------

    private void writeToDSAAdapt(String radid, String obstime,
            short num_of_adap, float default_ml_depth, String ml_overide_flag,
            float kdp_mult, float kdp_power, float z_r_mult, float z_r_power,
            float zdr_z_mult, float zdr_z_power, float zdr_zdr_power,
            float min_corr_precip, float min_corr_kdp, float refl_max,
            float kdp_max_beam_blk, float max_usability_blk,
            float kdp_min_usage_rate, float ws_mult, float gr_mult,
            float rh_mult, float ds_mult, float ic_mult, float grid_is_full,
            float paif_rate, float paif_area, float rain_time_thresh,
            float num_zones, float max_precip_rate, float restart_time,
            float max_interp_time, float max_hourly_acc, float time_bias,
            float num_grpairs, float reset_bias, float longst_lag) {

        DSAAdaptId id = new DSAAdaptId();
        id.setRadid(radid);

        Date obstimeDate = HydroTimeUtility.getDateFromSQLString(obstime);
        id.setObstime(obstimeDate);

        // DSAAdapt is a PersistableDataObject
        DSAAdapt radarObject = new DSAAdapt(id, num_of_adap, default_ml_depth,
                ml_overide_flag, kdp_mult, kdp_power, z_r_mult, z_r_power,
                zdr_z_mult, zdr_z_power, zdr_zdr_power, min_corr_precip,
                min_corr_kdp, refl_max, kdp_max_beam_blk, max_usability_blk,
                kdp_min_usage_rate, ws_mult, gr_mult, rh_mult, ds_mult,
                ic_mult, grid_is_full, paif_rate, paif_area, rain_time_thresh,
                num_zones, max_precip_rate, restart_time, max_interp_time,
                max_hourly_acc, time_bias, num_grpairs, reset_bias, longst_lag);

        try {
            CoreDao dao = new CoreDao(DaoConfig.forDatabase("ihfs"));
            dao.saveOrUpdate(radarObject);

        } catch (Exception e) {
            statusHandler.handle(Priority.ERROR,
                    "Can not save/update DAAAdapt table.", e);
            statusHandler.handle(Priority.ERROR, "radid = " + radid
                    + " obstime = " + obstime + " num_of_adap = " + num_of_adap
                    + " default_ml_depth = " + default_ml_depth
                    + " ml_oreride_flag = " + ml_overide_flag + "\nkdp_mult = "
                    + kdp_mult + " kdp_power = " + kdp_power + " z_r_mult = "
                    + z_r_mult + " z_r_power = " + z_r_power + " zdr_z_mult = "
                    + zdr_z_mult + " zdr_z_power = " + zdr_z_power
                    + " zdr_zdr_power = " + zdr_zdr_power
                    + "\nmin_corr_precip = " + min_corr_precip
                    + " min_corr_kdp = " + min_corr_kdp + " refl_max = "
                    + refl_max + " kdp_max_beam_blk = " + kdp_max_beam_blk
                    + " max_usability_blk = " + max_usability_blk
                    + "\nkdp_min_usage_rate = " + kdp_min_usage_rate
                    + " ws_mult = " + ws_mult + " gr_mult = " + gr_mult
                    + " rh_mult = " + rh_mult + " ds_mult = " + ds_mult
                    + " ic_mult = " + ic_mult + " grid_is_full = "
                    + grid_is_full + "\npaif_rate = " + paif_rate
                    + " paif_area = " + paif_area + " rain_time_thresh = "
                    + rain_time_thresh + " num_zones = " + num_zones
                    + " max_precip_rate = " + max_precip_rate
                    + " restart_time = " + restart_time
                    + "\nmax_interp_time = " + max_interp_time
                    + " max_hourly_acc = " + max_hourly_acc + " time_bias = "
                    + time_bias + " num_grpairs = " + num_grpairs
                    + " reset_bias = " + reset_bias + " longst_lag = "
                    + longst_lag);

        }
    }

    // ---------------------------------------------------------------------
    private float[][] getPolarGridFromDataArray(byte[] dataArray, float scale,
            float offset, int numRadials, int numRangeBins) {

        // convert 1D array to 2D polar array
        // transform to precip value using scale and offset
        // according to the ICD, each grid value is transformed to a precip
        // value as
        // F = (N - offset)/scale
        // where F = resulting float precip value
        // N = short precip value read from the gridded data

        float[][] polarGrid = new float[numRadials][numRangeBins];
        int index = 0;

        for (int r = 0; r < numRadials; r++) {
            for (int b = 0; b < numRangeBins; b++) {
                byte byteValue = dataArray[index];

                float floatValue = 0;

                if (byteValue == NO_DATA_FLAG) {
                    floatValue = NO_DATA_FLAG;
                } else {
                    floatValue = (byteValue - offset) / scale;
                    floatValue *= 0.254f;// change units from hundredths of
                                         // inches to mm
                }

                polarGrid[r][b] = floatValue;

                index++;
            } // end for b (bins)

        } // end for r (radials)

        return polarGrid;
    }

    // ---------------------------------------------------------------------

    private String getProductObsTimeString(RadarRecord record,
            DSAHeaderData headerData) {

        // prodObsDate and prodObsTime for DSA products are in a different
        // location in the
        // ProductDependentValue array than the DAA products

        // The obstime for DSA is same as StormTotalEndTime

        int prodObsDate = record.getProductDependentValue(4);

        int prodObsTime = record.getProductDependentValue(5);
        int prodObsHour = prodObsTime / 60;
        int prodObsMin = prodObsTime % 60;

        headerData.setProdDate(prodObsDate);
        headerData.setProdHour(prodObsHour);
        headerData.setProdMin(prodObsMin);
        headerData.setProductObsTimeMinutes(prodObsMin);

        String obsTime = String.format("%s %02d:%02d:00",
                HydroTimeUtility.JulianDateConvertToYMD(prodObsDate),
                prodObsHour, prodObsMin);

        return obsTime;
    }

    // ---------------------------------------------------------------------
    private String getStormTotalBeginTimeString(RadarRecord record,
            DSAHeaderData headerData) {

        int beginDate = record.getProductDependentValue(0);

        int beginTime = record.getProductDependentValue(1);
        int beginHour = beginTime / 60;
        int beginMin = beginTime % 60;

        String stormTotalBeginTime = String.format("%s %02d:%02d:00",
                HydroTimeUtility.JulianDateConvertToYMD(beginDate), beginHour,
                beginMin);

        return stormTotalBeginTime;
    }

    // ---------------------------------------------------------------------

    private String getStormTotalEndTimeString(RadarRecord record,
            DSAHeaderData headerData) {

        int endDate = record.getProductDependentValue(4);

        int endTime = record.getProductDependentValue(5);
        int endHour = endTime / 60;
        int endMin = endTime % 60;

        String stormTotalEndTime = String.format("%s %02d:%02d:00",
                HydroTimeUtility.JulianDateConvertToYMD(endDate), endHour,
                endMin);

        return stormTotalEndTime;
    }

    // ---------------------------------------------------------------------

    private void processHeader(RadarRecord record, DSAHeaderData headerData) {

        String mnemonic = record.getMnemonic();
        headerData.setMnemonic(mnemonic);

        String icao = record.getIcao().substring(1);
        String radid = icao.toUpperCase();
        headerData.setRadarId(radid);

        String uri = record.getDataURI();
        headerData.setUri(uri);

        String obsTime = getProductObsTimeString(record, headerData);
        headerData.setObsTime(obsTime);

        String beginTimeString = getStormTotalBeginTimeString(record,
                headerData);
        headerData.setBeginTimeString(beginTimeString);

        String endTimeString = getStormTotalEndTimeString(record, headerData);
        headerData.setEndTimeString(endTimeString);

        float maxVal = (float) (record.getProductDependentValue(3) / 1000.);
        headerData.setMaxVal(maxVal);

        short biasValue = (short) (record.getProductDependentValue(6));
        headerData.setBiasValue(biasValue);

        // operationalMode = 1 -- clear air mode
        // = 2 -- precip mode
        short operationalMode = record.getOperationalMode().shortValue();
        headerData.setOperationalMode(operationalMode);

        short volumeCoveragePattern = record.getVolumeCoveragePattern()
                .shortValue();
        headerData.setVolumeCoveragePattern(volumeCoveragePattern);

        short nullProductFlag = record.getProductDependentValue(2);
        headerData.setNullProductFlag(nullProductFlag);
        if (nullProductFlag > 0) {
            headerData.setIsProductNull(true);
        } else {
            headerData.setIsProductNull(false);
        }

        // scale = in A1: HW 31,32 . In A2: thresholds - 0,1
        // offset = in A1: HW 33,34 . In A2: thresholds - 2,3
        float scale = HydroNumericUtility.convertShortsToFloat(
                record.getThreshold(0), record.getThreshold(1));
        headerData.setScale(scale);

        float offset = HydroNumericUtility.convertShortsToFloat(
                record.getThreshold(2), record.getThreshold(3));
        headerData.setOffSet(offset);

        short stormTotalStartDate = record.getProductDependentValue(0);
        headerData.setStormTotalStartDate(stormTotalStartDate);

        short stormTotalStartTime = record.getProductDependentValue(1);
        headerData.setStormTotalStartTime(stormTotalStartTime);

        short stormTotalEndDate = record.getProductDependentValue(4);
        headerData.setStormTotalEndDate(stormTotalEndDate);

        short stormTotalEndTime = record.getProductDependentValue(5);
        headerData.setStormTotalEndTime(stormTotalEndTime);

        int prodDate = headerData.getProdDate();
        int prodHour = headerData.getProdHour();
        int prodMin = headerData.getProdMin();
        String fileName = String.format("DSA%s%s%02d%02dZ", radid,
                HydroTimeUtility.JulianDateConvertToMDY(prodDate), prodHour,
                prodMin);
        headerData.setFileName(fileName);

        statusHandler.handle(Priority.INFO,
                "DSA product: uri = " + headerData.getUri());

    }

    // ---------------------------------------------------------------------

    private void writeOutDataFile(RadarRecord record, DSAHeaderData headerData,
            float[][] precipGridArray, String fileName) {
        // fileName is of the form DSAXXXmmddyyyyhhmmZ
        String fullPathName = outputGridDirectory + File.separator + fileName;
        DataOutputStream outputStream = null;

        try {

            FileOutputStream fileOutputStream = new FileOutputStream(
                    fullPathName);
            outputStream = new DataOutputStream(fileOutputStream);

            int hrapXMax = MAX_IHRAP;
            int hrapYMax = MAX_JHRAP;

            int intValue = 0;

            // write out header portion of decoded DSA product
            short beginDate = headerData.getStormTotalStartDate();
            short beginTime = headerData.getStormTotalStartTime();
            short endDate = headerData.getStormTotalEndDate();
            short endTime = headerData.getStormTotalEndTime();
            short operationalMode = headerData.getOperationalMode();

            outputStream.writeShort(Short.reverseBytes(beginDate));
            outputStream.writeShort(Short.reverseBytes(beginTime));
            outputStream.writeShort(Short.reverseBytes(operationalMode));

            outputStream.writeShort(Short.reverseBytes(endDate));
            outputStream.writeShort(Short.reverseBytes(endTime));
            outputStream.writeShort(Short.reverseBytes(operationalMode));

            float max = -9999f;

            // write out data portion of decoded DSA product
            for (int x = 0; x < hrapXMax; x++) {
                for (int y = 0; y < hrapYMax; y++) {
                    float floatValue = precipGridArray[x][y];
                    intValue = HydroNumericUtility
                            .getSwappedIntBytesFromFloat(floatValue);
                    outputStream.writeInt(intValue);

                    if (precipGridArray[x][y] > max) {
                        max = precipGridArray[x][y];
                    }
                }
            }

        } catch (Exception e) {
            statusHandler.handle(Priority.ERROR,
                    "Could not write out decoded DSA product file.", e);
        }

        finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    statusHandler.handle(Priority.ERROR,
                            "Could not close out decoded DSA product file - "
                                    + fullPathName);
                }
            }
        }
        // writer.print(d)
    } // end writeOutDataFile()

    // ---------------------------------------------------------------------

    private void writeToDSARadarTable(DSAHeaderData d) {

        writeToDSARadarTable(d.getRadarId(), d.getObsTime(),
                d.volumeCoveragePattern, d.operationalMode, d.getMaxVal(),
                d.getScale(), d.getOffSet(), d.getBeginTimeString(),
                d.getEndTimeString(), d.getStormTotalStartDate(),
                d.getStormTotalStartTime(), d.getStormTotalEndDate(),
                d.getStormTotalEndTime(), d.getBiasValue(),
                d.getNullProductFlag(), d.getFileName());

        statusHandler
                .handle(Priority.DEBUG,
                        "In routine writeToDSARadarTable - finish to write to DSARadar table");

    }

    // ---------------------------------------------------------------------
    private void writeToDSARadarTable(String radid, String obstime,
            short volumeCoveragePattern, short operationalMode, float maxVal,
            float scale, float offset, String beginTime, String endTime,
            short jBeginDate, short jBeginTime, short jEndDate, short jEndTime,
            short biasValue, short nullProductFlag, String filename) {
        DSARadarId id = new DSARadarId();
        id.setRadid(radid);

        Date obstimeDate = HydroTimeUtility.getDateFromSQLString(obstime);
        id.setObstime(obstimeDate);

        Date beginTimeDate = HydroTimeUtility.getDateFromSQLString(beginTime);

        Date endTimeDate = HydroTimeUtility.getDateFromSQLString(endTime);

        // DSARadar is a PersistableDataObject
        DSARadar radarObject = new DSARadar(id, volumeCoveragePattern,
                operationalMode, maxVal, scale, offset, beginTimeDate,
                endTimeDate, jBeginDate, jBeginTime, jEndDate, jEndTime,
                biasValue, nullProductFlag, filename);

        try {
            CoreDao dao = new CoreDao(DaoConfig.forDatabase("ihfs"));
            dao.saveOrUpdate(radarObject);

        } catch (Exception e) {
            statusHandler.handle(Priority.ERROR,
                    "Can not save/update DSARadar table.", e);
            statusHandler.handle(Priority.ERROR, "radid = " + radid
                    + " obstime = " + obstime + " volumeCoveragePattern = "
                    + volumeCoveragePattern + " operationalMode = "
                    + operationalMode + "\nmaxVal = " + maxVal + " scale = "
                    + scale + " offset = " + offset + " beginTime = "
                    + beginTime + " endTime = " + endTime + " jBeginDate = "
                    + jBeginDate + " jBeginTime = " + jBeginTime
                    + "\njEndDate = " + jEndDate + " jEndTime = " + jEndTime
                    + " biasValue = " + biasValue + " nullProductFlag = "
                    + nullProductFlag + " filename = " + filename);

        }
    }

    // ---------------------------------------------------------------------

}
