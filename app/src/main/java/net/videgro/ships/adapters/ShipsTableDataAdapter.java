package net.videgro.ships.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import net.videgro.ships.R;
import net.videgro.ships.nmea2ship.domain.Ship;

import java.io.IOException;
import java.util.List;

import de.codecrafters.tableview.TableDataAdapter;

import static java.text.DateFormat.getDateTimeInstance;

public class ShipsTableDataAdapter extends TableDataAdapter<Ship> {
    private static final String TAG = "ShipsTableDataAdapter";

    public static final int COL_COUNTRY = 0;
    public static final int COL_TYPE = 1;
    public static final int COL_MMSI = 2;
    public static final int COL_NAME_CALLSIGN = 3;
    public static final int COL_DESTINATION = 4;
    public static final int COL_NAV_STATUS = 5;
    public static final int COL_UPDATED = 6;

    public ShipsTableDataAdapter(Context context, List<Ship> data) {
        super(context, data);
    }

    @Override
    public View getCellView(int rowIndex, int columnIndex, ViewGroup parentView) {
        final Ship ship = getRowData(rowIndex);
        View renderedView = null;

        switch (columnIndex) {
            case COL_COUNTRY:
                try {
                    final ImageView imageView = new ImageView(getContext());
                    imageView.setLayoutParams(new TableRow.LayoutParams(36, 27));
                    imageView.setContentDescription(ship.getCountryName());
                    imageView.setImageDrawable(Drawable.createFromStream(getContext().getAssets().open("images/flags/" + ship.getCountryFlag() + ".png"), null));
                    renderedView = imageView;
                } catch (IOException e) {
                    Log.e(TAG, "country", e);
                }
                break;
            case COL_TYPE:
                try {
                    final ImageView imageView = new ImageView(getContext());
                    imageView.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                    imageView.setContentDescription(ship.getCountryName());
                    imageView.setImageDrawable(Drawable.createFromStream(getContext().getAssets().open("images/" + ship.getShipTypeIcon()), null));
                    renderedView = imageView;
                } catch (IOException e) {
                    Log.e(TAG, "type", e);
                }
                break;
            case COL_MMSI:
                renderedView = createTableTextView(String.valueOf(ship.getMmsi()));
                break;
            case COL_NAME_CALLSIGN:
                renderedView = createTableTextView(ship.getName() + (Ship.UNKNOWN.equals(ship.getCallsign()) ? "" : "\n" + ship.getCallsign()));
                break;
            case COL_DESTINATION:
                renderedView = createTableTextView(Ship.UNKNOWN.equals(ship.getDest()) ? "" : ship.getDest());
                break;
            case COL_NAV_STATUS:
                renderedView = createTableTextView(Ship.UNKNOWN.equals(ship.getNavStatus()) ? "" : ship.getNavStatus());
                break;
            case COL_UPDATED:
                renderedView = createTableTextView(getDateTimeInstance().format(ship.getTimestamp()));
                break;
            default:
                // Nothing to do
                break;
        }

        return renderedView;
    }

    private TextView createTableTextView(final String text) {
        final TextView result = new TextView(getContext());
        result.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
        result.setTextColor(ContextCompat.getColor(getContext(), R.color.ships_table_text));
        result.setText(text);
        return result;
    }
}
