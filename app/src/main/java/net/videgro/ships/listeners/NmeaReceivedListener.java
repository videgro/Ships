package net.videgro.ships.listeners;

import net.videgro.ships.services.NmeaClientService;

public interface NmeaReceivedListener {
	void onNmeaReceived(String line,final NmeaClientService.Source source);
}
