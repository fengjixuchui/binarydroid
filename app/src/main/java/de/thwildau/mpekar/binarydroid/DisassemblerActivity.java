package de.thwildau.mpekar.binarydroid;

import android.arch.lifecycle.ViewModelProviders;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import net.fornwall.jelf.ElfFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import de.thwildau.mpekar.binarydroid.assembly.ByteAccessor;
import de.thwildau.mpekar.binarydroid.assembly.Disassembler;
import de.thwildau.mpekar.binarydroid.assembly.DisassemblerCapstone;
import de.thwildau.mpekar.binarydroid.model.ContainerELF;
import de.thwildau.mpekar.binarydroid.model.SymbolItem;
import de.thwildau.mpekar.binarydroid.ui.disasm.AssemblerDialog;
import de.thwildau.mpekar.binarydroid.ui.disasm.DisassemblerFragment;
import de.thwildau.mpekar.binarydroid.ui.disasm.DisassemblerViewModel;
import de.thwildau.mpekar.binarydroid.ui.disasm.HexEditorFragment;
import de.thwildau.mpekar.binarydroid.ui.disasm.SymbolFragment;

import static android.content.DialogInterface.BUTTON_POSITIVE;

/**
 * The main activity that contains every fragment related to the disassmbler.
 * (Hexview, Disassembler, Symbollist)
 */
public class DisassemblerActivity extends AppCompatActivity
        implements SymbolFragment.OnSymbolSelectListener {
    public static final String EXTRA_BINPATH = "extra_binpath";

    private ViewPager pager;
    private DisassemblerPagerAdapter pagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.disasm_activity);

        // Initialize our disassembler view model
        if (savedInstanceState == null) {
            String binpath = getIntent().getStringExtra(EXTRA_BINPATH);
            File file = new File(binpath);

            if (file.exists()) {
                DisassemblerViewModel viewModel =
                        ViewModelProviders.of(this).get(DisassemblerViewModel.class);

                viewModel.setDisasm(new DisassemblerCapstone());

                try {
                    ByteAccessor accessor = new ByteAccessor(file);
                    viewModel.setAccessor(accessor);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    finish();
                }

                try {
                    ElfFile elf = ElfFile.fromFile(file);
                    viewModel.setBinary(new ContainerELF(elf));
                } catch (IOException e) {
                    e.printStackTrace();
                    finish();
                }
            } else {
                Toast.makeText(getApplicationContext(),
                        R.string.binarynotexists, Toast.LENGTH_SHORT).show();
                finish();
            }
        }

        // Register viewpager adapter and install fragment change notifications
        pager = findViewById(R.id.container);
        pagerAdapter = new DisassemblerPagerAdapter(getSupportFragmentManager());
        pager.setAdapter(pagerAdapter);

        // Display active fragment in the ActionBar title
        ViewPager.OnPageChangeListener onPageChangeListener = new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                int stringId = 0;
                switch (position) {
                    case DisassemblerPagerAdapter.VIEW_HEXEDIT:
                        stringId = R.string.hexeditor;
                        break;
                    case DisassemblerPagerAdapter.VIEW_DISASM:
                        stringId = R.string.disassembler;
                        break;
                    case DisassemblerPagerAdapter.VIEW_SYMBOLS:
                        stringId = R.string.symbollist;
                        break;
                }

                if (stringId != 0) {
                    setActionBarTitle(getString(stringId));
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }

            @Override
            public void onPageScrolled(int position, float arg1, int arg2) {
            }
        };
        pager.addOnPageChangeListener (onPageChangeListener);
        onPageChangeListener.onPageSelected(pager.getCurrentItem());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.disassembler_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menu_hexview:
                pager.setCurrentItem(DisassemblerPagerAdapter.VIEW_HEXEDIT);
                return true;
            case R.id.menu_disasm:
                pager.setCurrentItem(DisassemblerPagerAdapter.VIEW_DISASM);
                return true;
            case R.id.menu_symbols:
                pager.setCurrentItem(DisassemblerPagerAdapter.VIEW_SYMBOLS);
                return true;
            case R.id.menu_assembler:
                DisassemblerViewModel viewModel =
                        ViewModelProviders.of(this).get(DisassemblerViewModel.class);
                Disassembler disassembler = viewModel.getDisasm().getValue();
                AssemblerDialog dialog = new AssemblerDialog(this, disassembler);
                dialog.show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onSymbolSelected(SymbolItem symbol) {
        DisassemblerViewModel viewModel =
                ViewModelProviders.of(this).get(DisassemblerViewModel.class);

        // set address
        viewModel.setAddress(symbol.addr);

        // ask the user where he wants to view the address?
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.app_name);
        builder.setMessage(R.string.symbolgotoquestion);
        DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int item;
                if (which == BUTTON_POSITIVE) {
                    item = DisassemblerPagerAdapter.VIEW_DISASM;
                } else {
                    item = DisassemblerPagerAdapter.VIEW_HEXEDIT;
                }
                // change the active view
                pager.setCurrentItem(item);
            }
        };
        builder.setPositiveButton(R.string.disassembler, clickListener);
        builder.setNegativeButton(R.string.hexeditor, clickListener);
        AlertDialog alert = builder.create();
        alert.show();
    }

    public void setActionBarTitle(String subtitle) {
        String title = getString(R.string.app_name);
        if (subtitle != null) {
            title += " - " + subtitle;
        }
        getSupportActionBar().setTitle(title);
    }

    private class DisassemblerPagerAdapter extends FragmentStatePagerAdapter {
        private Fragment[] fragments;

        public static final int VIEW_TOTAL_COUNT = 3;
        public static final int VIEW_HEXEDIT = 0;
        public static final int VIEW_DISASM = 1;
        public static final int VIEW_SYMBOLS = 2;

        public DisassemblerPagerAdapter(FragmentManager fm) {
            super(fm);

            fragments = new Fragment[VIEW_TOTAL_COUNT];
            fragments[VIEW_HEXEDIT] = new HexEditorFragment();
            fragments[VIEW_DISASM] = new DisassemblerFragment();
            fragments[VIEW_SYMBOLS] = new SymbolFragment();
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return fragments[VIEW_HEXEDIT];
                case 1:
                    return fragments[VIEW_DISASM];
                case 2:
                    return fragments[VIEW_SYMBOLS];
                default:
                    throw new RuntimeException("unknown position " + position);
            }
        }

        @Override
        public int getCount() {
            return fragments.length;
        }


    }
}