package de.thwildau.mpekar.binarydroid;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import de.thwildau.mpekar.binarydroid.ui.disasm.HexEditorFragment;

public class DisassemblerActivity extends AppCompatActivity {

    public static final String EXTRA_BINPATH = "extra_binpath";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        // Add main fragment if this is first creation
        if (savedInstanceState == null) {
            String binpath = getIntent().getStringExtra(EXTRA_BINPATH);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, HexEditorFragment.newInstance(binpath))
                    .commitNow();
        }
    }
}