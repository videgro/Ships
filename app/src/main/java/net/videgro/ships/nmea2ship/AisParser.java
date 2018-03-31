package net.videgro.ships.nmea2ship;

import android.util.Log;

import net.videgro.ships.nmea2ship.domain.Ship;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import dk.dma.ais.message.AisMessage;
import dk.dma.ais.message.AisMessage21;
import dk.dma.ais.message.AisMessage24;
import dk.dma.ais.message.AisMessage5;
import dk.dma.ais.message.AisPositionMessage;
import dk.dma.ais.message.AisStaticCommon;
import dk.dma.ais.message.IVesselPositionMessage;
import dk.dma.ais.message.NavigationalStatus;
import dk.dma.ais.message.ShipTypeCargo;
import dk.dma.ais.proprietary.IProprietarySourceTag;
import dk.dma.enav.model.geometry.Position;

public class AisParser {
	private static final String TAG="AisParser";
	
	private static final int AIS_CONSTANT_HEADING_UNKNOWN=511;	
	
	private Map<String,Ship> ships=new HashMap<>();
	
	public Ship parse(final AisMessage aisMessage){
		Ship ship=ships.get(""+aisMessage.getUserId());
		if (ship==null){
			// Create new ship
			ship=new Ship(aisMessage.getUserId());
			ships.put(""+aisMessage.getUserId(), ship);	
		}
		
		ship.setTimestamp(Calendar.getInstance().getTimeInMillis());
		
		final Position position = aisMessage.getValidPosition();
		if (position!=null){
			ship.setLat(position.getLatitude());
			ship.setLon(position.getLongitude());
		}
		
		// Cast message
		
		// Handle AtoN message
	    if (aisMessage instanceof AisMessage21) {
	        AisMessage21 msg = (AisMessage21) aisMessage;
	    }
	    // Handle position messages 1,2 and 3 (class A) by using their shared parent
	    if (aisMessage instanceof AisPositionMessage) {
	        AisPositionMessage msg = (AisPositionMessage)aisMessage;
	        
	        if (msg.getTrueHeading()!=AIS_CONSTANT_HEADING_UNKNOWN){
	        	ship.setHeading(msg.getTrueHeading());
	        }
	        
			ship.setCog(msg.getCog());
			ship.setNavStatus(NavigationalStatus.get(msg.getNavStatus()).prettyStatus());
			ship.setRaim(msg.getRaim());
			ship.setRot(msg.getRot());
			ship.setSensorRot(msg.getSensorRot());
			ship.setSog(msg.getSog());
			ship.setSpecialManIndicator(msg.getSpecialManIndicator());
	    }
	    // Handle position messages 1,2,3 and 18 (class A and B)
	    if (aisMessage instanceof IVesselPositionMessage) {
	    	IVesselPositionMessage msg = (IVesselPositionMessage)aisMessage;
	    	ship.setSog(msg.getSog());
	    }
	    // Handle static reports for both class A and B vessels (msg 5 + 24)
	    if (aisMessage instanceof AisStaticCommon) {
	        AisStaticCommon msg = (AisStaticCommon)aisMessage;
	        
	        final String name=cleanString(msg.getName());			        
			if (name!=null && !name.isEmpty()){
				ship.setName(name);		
				//audioCreator.create(ship);
			}					
			final String callsign=cleanString(msg.getCallsign());			        
			if (callsign!=null && !callsign.isEmpty()){
				ship.setCallsign(callsign);
			}
			
			ship.setDimBow(msg.getDimBow());
			ship.setDimPort(msg.getDimPort());
			ship.setDimStarboard(msg.getDimStarboard());
			ship.setDimStern(msg.getDimStern());
			final ShipTypeCargo shipTypeCargo = new ShipTypeCargo(msg.getShipType());
			//ship.setShipType(shipTypeCargo.prettyCargo()+" "+shipTypeCargo.prettyType());
            if (shipTypeCargo.getShipType()!=null) {
                ship.setShipType(shipTypeCargo.getShipType().toString());
            }
	    }
	    			    
	    if (aisMessage instanceof AisMessage5){
	    	AisMessage5 msg=(AisMessage5)aisMessage;
	    	
	    	final String dest=cleanString(msg.getDest());			        
			if (dest!=null && !dest.isEmpty()){			    	
				ship.setDest(dest);
			}
			
	    	ship.setDraught(msg.getDraught());
	    	ship.setDte(msg.getDte());
			ship.setEta(msg.getEta());
			ship.setEtaDate(msg.getEtaDate());
			ship.setImo(msg.getImo());
			ship.setVersion(msg.getVersion());
	    }
	    
	    if (aisMessage instanceof AisMessage24){
	    	AisMessage24 msg=(AisMessage24)aisMessage;
	    	ship.setVendorId(msg.getVendorId());
	    }
	    			    
	    if (aisMessage.getSourceTag() != null) {
            IProprietarySourceTag sourceTag = aisMessage.getSourceTag();
        }

	    Log.d(TAG,ship.toString()); 
		return ship;
	}

	private static String cleanString(final String str){
		String result=null;
		if (str!=null){
			result=str.replace("@","").trim();
		}
		return result;
	}
}
