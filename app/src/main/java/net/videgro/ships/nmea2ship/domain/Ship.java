package net.videgro.ships.nmea2ship.domain;

import android.util.Log;

import java.util.Date;

public class Ship {

    /**
     * INTERNAL: Received from RTL-SDR dongle
     * EXTERNAL: Received from external party (show as 'peers')
     * Both via UDP
     * CLOUD:    Received via Firebase
     */
    public enum Source { INTERNAL, EXTERNAL, CLOUD }

    private static final String TAG="Ship";
    public static final String UNKNOWN="UNKNOWN";

    private Source source;
    private final int mmsi;
    private String countryName;
    private String countryFlag;
    private String name="";
    private double lat;
    private double lon;
    private int heading=0;
    private long timestamp;

    /**
     * Course Over Ground
     */
    private int cog;

    private String navStatus=UNKNOWN;
    private int raim;

    /**
     * Rate of Turn (ROT)
     */
    private int rot;

    private Float sensorRot;
    private int sog;
    private int specialManIndicator;
    private int subMessage;
    private String dest=UNKNOWN;
    private String callsign=UNKNOWN;
    private int dimBow;
    private int dimPort;
    private int dimStarboard;
    private int dimStern;
    private int draught;
    private int dte;
    private long eta;
    private Date etaDate;
    private long imo;
    private String shipType=UNKNOWN;
    private String shipTypeIcon=UNKNOWN;
    private int version;
    private int altitude;
    private int commStateSelectorFlag;
    private int regionalReserved;
    private int syncState;
    private String vendorId;

    private boolean audioAvailable;

    public Ship(int mmsi) {
        this.mmsi = mmsi;

        final Mid mid=retrieveMid();
        if (mid!=null){
            countryName=mid.getFriendlyName();
            countryFlag=mid.getFlagCode();
        } else {
            countryName=UNKNOWN;
            countryFlag=UNKNOWN;
        }
    }

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public int getMmsi() {
        return mmsi;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public int getHeading() {
        return heading;
    }

    public void setHeading(int heading) {
        this.heading = heading;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getCog() {
        return cog;
    }

    public void setCog(int cog) {
        this.cog = cog;
    }

    public String getNavStatus() {
        return navStatus;
    }

    public void setNavStatus(String navStatus) {
        this.navStatus = navStatus;
    }

    public int getRaim() {
        return raim;
    }

    public void setRaim(int raim) {
        this.raim = raim;
    }

    public int getRot() {
        return rot;
    }

    public void setRot(int rot) {
        this.rot = rot;
    }

    public Float getSensorRot() {
        return sensorRot;
    }

    public void setSensorRot(Float sensorRot) {
        this.sensorRot = sensorRot;
    }

    public int getSog() {
        return sog;
    }

    public void setSog(int sog) {
        this.sog = sog;
    }

    public int getSpecialManIndicator() {
        return specialManIndicator;
    }

    public void setSpecialManIndicator(int specialManIndicator) {
        this.specialManIndicator = specialManIndicator;
    }

    public int getSubMessage() {
        return subMessage;
    }

    public void setSubMessage(int subMessage) {
        this.subMessage = subMessage;
    }

    public String getDest() {
        return dest;
    }

    public void setDest(String dest) {
        this.dest = dest;
    }

    public String getCallsign() {
        return callsign;
    }

    public void setCallsign(String callsign) {
        this.callsign = callsign;
    }

    public int getDimBow() {
        return dimBow;
    }

    public void setDimBow(int dimBow) {
        this.dimBow = dimBow;
    }

    public int getDimPort() {
        return dimPort;
    }

    public void setDimPort(int dimPort) {
        this.dimPort = dimPort;
    }

    public int getDimStarboard() {
        return dimStarboard;
    }

    public void setDimStarboard(int dimStarboard) {
        this.dimStarboard = dimStarboard;
    }

    public int getDimStern() {
        return dimStern;
    }

    public void setDimStern(int dimStern) {
        this.dimStern = dimStern;
    }

    public int getDraught() {
        return draught;
    }

    public void setDraught(int draught) {
        this.draught = draught;
    }

    public int getDte() {
        return dte;
    }

    public void setDte(int dte) {
        this.dte = dte;
    }

    public long getEta() {
        return eta;
    }

    public void setEta(long eta) {
        this.eta = eta;
    }

    public Date getEtaDate() {
        return etaDate;
    }

    public void setEtaDate(Date etaDate) {
        this.etaDate = etaDate;
    }

    public long getImo() {
        return imo;
    }

    public void setImo(long imo) {
        this.imo = imo;
    }

    public String getShipType() {
        return shipType;
    }

    public void setShipType(String shipType) {
        this.shipType = shipType;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public int getAltitude() {
        return altitude;
    }

    public void setAltitude(int altitude) {
        this.altitude = altitude;
    }

    public int getCommStateSelectorFlag() {
        return commStateSelectorFlag;
    }

    public void setCommStateSelectorFlag(int commStateSelectorFlag) {
        this.commStateSelectorFlag = commStateSelectorFlag;
    }

    public int getRegionalReserved() {
        return regionalReserved;
    }

    public void setRegionalReserved(int regionalReserved) {
        this.regionalReserved = regionalReserved;
    }

    public int getSyncState() {
        return syncState;
    }

    public void setSyncState(int syncState) {
        this.syncState = syncState;
    }

    public String getVendorId() {
        return vendorId;
    }

    public void setVendorId(String vendorId) {
        this.vendorId = vendorId;
    }

    public boolean isAudioAvailable() {
        return audioAvailable;
    }

    public void setAudioAvailable(boolean audioAvailable) {
        this.audioAvailable = audioAvailable;
    }

    public String getCountryName() {
        return countryName;
    }

    public String getCountryFlag() {
        return countryFlag;
    }

    public String getShipTypeIcon() {
        return shipTypeIcon;
    }

    public void setShipTypeIcon(String shipTypeIcon) {
        this.shipTypeIcon = shipTypeIcon;
    }

    public boolean isValid(){
        return (lat<0.0d || lat>0.0d) && (lon<0.0d || lon>0.0d);
    }

    private Mid retrieveMid(){
        final int midSize=3;
        Mid result=null;
        final String mmsiAsString=mmsi+"";
        if (mmsiAsString.length()>midSize){
            final String midAsString = mmsiAsString.substring(0,midSize);
            try {
                result=Mid.valueOf(Mid.PREFIX+midAsString);
            } catch (IllegalArgumentException e){
                Log.w(TAG,"retrieveMid - invalid - "+midAsString);
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return "Ship [mmsi=" + mmsi + ", source=" + source +", countryName=" + countryName + ", countryFlag=" + countryFlag + ", name=" + name
                + ", lat=" + lat + ", lon=" + lon + ", heading=" + heading + ", timestamp=" + timestamp + ", cog=" + cog
                + ", navStatus=" + navStatus + ", raim=" + raim + ", rot=" + rot + ", sensorRot=" + sensorRot + ", sog="
                + sog + ", specialManIndicator=" + specialManIndicator + ", subMessage=" + subMessage + ", dest=" + dest
                + ", callsign=" + callsign + ", dimBow=" + dimBow + ", dimPort=" + dimPort + ", dimStarboard="
                + dimStarboard + ", dimStern=" + dimStern + ", draught=" + draught + ", dte=" + dte + ", eta=" + eta
                + ", etaDate=" + etaDate + ", imo=" + imo + ", shipType=" + shipType + ", version=" + version
                + ", altitude=" + altitude + ", commStateSelectorFlag=" + commStateSelectorFlag + ", regionalReserved="
                + regionalReserved + ", syncState=" + syncState + ", vendorId=" + vendorId + ", audioAvailable="
                + audioAvailable + "]";
    }


}
