package net.videgro.ships.nmea2ship;

import android.util.Log;

import net.videgro.ships.nmea2ship.domain.Ship;

import dk.dma.ais.binary.SixbitException;
import dk.dma.ais.message.AisMessageException;
import dk.dma.ais.packet.AisPacket;
import dk.dma.ais.packet.AisPacketParser;
import dk.dma.ais.sentence.SentenceException;

public class Nmea2Ship {
	private static final String TAG="Nmea2Ship - ";

	public enum NmeaSource { UDP, SOCKET_IO }

	private AisPacketParser aisPacketParser=new AisPacketParser();
	private final AisParser aisParser=new AisParser();
	
	public Nmea2Ship(){
		Log.i(TAG,"constructor");
	}
	
	public Ship onMessage(final String line,final NmeaSource nmeaSource){
		Ship result=null;
		AisPacket aisPacket=null;
		try {
			aisPacket = aisPacketParser.readLine(line);
		} catch (SentenceException e) {
			Log.e(TAG,"onMessage",e);
		}
		
		if (aisPacket!=null){
			try {
				result=aisParser.parse(aisPacket.getAisMessage());

                switch (nmeaSource) {
                    case UDP:
                        result.setSource(Ship.Source.UDP);
                        break;
                    case SOCKET_IO:
                        result.setSource(Ship.Source.SOCKET_IO);
                        break;
                    default:
                        // Nothing to do
                        break;
                } // End switch

			} catch (AisMessageException | SixbitException e) {
				Log.e(TAG,"onMessage",e);
			}
		}
		
		return result;		
	}
}
