package net.videgro.ships.nmea2ship;

import android.util.Log;
import dk.dma.ais.binary.SixbitException;
import dk.dma.ais.message.AisMessageException;
import dk.dma.ais.packet.AisPacket;
import dk.dma.ais.packet.AisPacketParser;
import dk.dma.ais.sentence.SentenceException;
import net.videgro.ships.nmea2ship.domain.Ship;

public class Nmea2Ship {
	final String tag="Nmea2Ship - ";
	
	private AisPacketParser aisPacketParser=new AisPacketParser();
	private final AisParser aisParser=new AisParser();
	
	public Nmea2Ship(){
		Log.i(tag,"constructor");
	}
	
	public Ship onMessage(String line){
		Ship result=null;
		AisPacket aisPacket=null;
		try {
			aisPacket = aisPacketParser.readLine(line);
		} catch (SentenceException e) {
			Log.e(tag,"onMessage",e);
		}
		
		if (aisPacket!=null){
			try {
				result=aisParser.parse(aisPacket.getAisMessage());
			} catch (AisMessageException e) {
				Log.e(tag,"onMessage",e);
			} catch (SixbitException e) {
				Log.e(tag,"onMessage",e);
			}			
		}
		
		return result;		
	}
	
}
