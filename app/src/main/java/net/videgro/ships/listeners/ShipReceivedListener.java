package net.videgro.ships.listeners;

import net.videgro.ships.nmea2ship.domain.Ship;

public interface ShipReceivedListener {
	void onShipReceived(final Ship ship);
}
