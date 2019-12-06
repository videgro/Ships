package net.videgro.ships.nmea2ship;

import android.util.Log;

import net.videgro.ships.nmea2ship.domain.Ship;
import net.videgro.ships.services.NmeaClientService;

import dk.dma.ais.binary.SixbitException;
import dk.dma.ais.message.AisMessageException;
import dk.dma.ais.packet.AisPacket;
import dk.dma.ais.packet.AisPacketParser;
import dk.dma.ais.sentence.SentenceException;

public class Nmea2Ship {
	private static final String TAG="Nmea2Ship - ";

	private AisPacketParser aisPacketParser=new AisPacketParser();
	private final AisParser aisParser=new AisParser();
	
	public Nmea2Ship(){
		Log.i(TAG,"constructor");
	}
	
	public Ship onMessage(final String line, final NmeaClientService.Source source){
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

                switch (source) {
                    case INTERNAL:
                        result.setSource(Ship.Source.INTERNAL);
                        break;
                    case EXTERNAL:
                        result.setSource(Ship.Source.EXTERNAL);
                        break;
					case CLOUD:
						result.setSource(Ship.Source.CLOUD);
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
