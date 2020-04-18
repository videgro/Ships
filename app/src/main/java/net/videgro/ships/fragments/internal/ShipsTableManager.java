package net.videgro.ships.fragments.internal;

import android.util.Log;

import net.videgro.ships.adapters.ShipsTableDataAdapter;
import net.videgro.ships.nmea2ship.domain.Ship;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShipsTableManager {
    private static final String TAG = "ShipsTableManager";

    private final ShipsTableDataAdapter shipsTableDataAdapter;
    private final long maxAgeInMs;

    private Map<Ship.Source,Boolean> enabledSources=new HashMap<>();

    public ShipsTableManager(final ShipsTableDataAdapter shipsTableDataAdapter,final int maxAge){
        this.shipsTableDataAdapter=shipsTableDataAdapter;
        this.maxAgeInMs=1000*60*maxAge;
        enabledSources.put(Ship.Source.INTERNAL,true);
        enabledSources.put(Ship.Source.EXTERNAL,true);
        enabledSources.put(Ship.Source.CLOUD,true);
    }

    public void updateEnabledSource(Ship.Source source,boolean newValuie){
        enabledSources.put(source,newValuie);
        final List<Ship> ships = shipsTableDataAdapter.getData();
        cleanupShipsTableSource(ships);
        shipsTableDataAdapter.notifyDataSetChanged();
    }

    public void update(final Ship ship){
        final List<Ship> ships = shipsTableDataAdapter.getData();
        if (enabledSources.get(ship.getSource())!=null && enabledSources.get(ship.getSource())) {
            final int index = findShipInShipsTableData(ships, ship);

            if (index == -1) {
                // Not found, create entry
                ships.add(ship);
            } else {
                // Found, update entry
                ships.set(index, ship);
            }
        }

        cleanupShipsTableAge(ships);

        shipsTableDataAdapter.notifyDataSetChanged();
    }

    private static int findShipInShipsTableData(final List<Ship> data, final Ship ship){
        int result=-1;
        for (int i=0;i<data.size() && result==-1; i++){
            if (data.get(i).getMmsi()==ship.getMmsi()){
                result=i;
            }
        }
        return result;
    }

    private void cleanupShipsTableSource(final List<Ship> original){
        final String tag="cleanupShipsTableSource - ";

        final List<Ship> copy=new ArrayList<>(original);
        original.clear();
        for (final Ship ship:copy){
            if (enabledSources.get(ship.getSource())!=null && enabledSources.get(ship.getSource())){
                original.add(ship);
            } else {
                Log.d(TAG,tag+"Removed ship: "+ship);
            }
        }
    }

    private void cleanupShipsTableAge(final List<Ship> original){
        final String tag="cleanupShipsTable - ";
        final long currentTime= Calendar.getInstance().getTimeInMillis();

        final List<Ship> copy=new ArrayList<>(original);
        original.clear();
        for (final Ship ship:copy){
            if ((currentTime-ship.getTimestamp())<maxAgeInMs){
                original.add(ship);
            } else {
                Log.d(TAG,tag+"Removed ship: "+ship);
            }
        }
    }
}
