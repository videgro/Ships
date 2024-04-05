package net.videgro.ships.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import net.videgro.ships.fragments.HelpFragment;

public class HelpActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getSupportFragmentManager().beginTransaction().replace(android.R.id.content,new HelpFragment()).commit();
    }
}