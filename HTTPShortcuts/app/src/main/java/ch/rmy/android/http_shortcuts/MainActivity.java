package ch.rmy.android.http_shortcuts;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore.Images.Media;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import java.util.List;

import butterknife.Bind;
import ch.rmy.android.http_shortcuts.adapters.CategoryPagerAdapter;
import ch.rmy.android.http_shortcuts.dialogs.ChangeLogDialog;
import ch.rmy.android.http_shortcuts.realm.Controller;
import ch.rmy.android.http_shortcuts.realm.models.Category;
import ch.rmy.android.http_shortcuts.realm.models.Shortcut;

/**
 * Main activity to list all shortcuts
 *
 * @author Roland Meyer
 */
public class MainActivity extends BaseActivity implements ListFragment.TabHost {

    private static final String ACTION_INSTALL_SHORTCUT = "com.android.launcher.action.INSTALL_SHORTCUT";
    private static final String ACTION_UNINSTALL_SHORTCUT = "com.android.launcher.action.UNINSTALL_SHORTCUT";

    private final static int REQUEST_CREATE_SHORTCUT = 1;

    @Bind(R.id.button_create_shortcut)
    FloatingActionButton createButton;
    @Bind(R.id.view_pager)
    ViewPager viewPager;
    @Bind(R.id.tabs)
    TabLayout tabLayout;

    private Controller controller;
    private CategoryPagerAdapter adapter;

    private boolean shortcutPlacementMode = false;

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        shortcutPlacementMode = Intent.ACTION_CREATE_SHORTCUT.equals(getIntent().getAction());

        controller = destroyer.own(new Controller(this));

        createButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openEditorForCreation();
            }
        });
        setupViewPager();

        if (!shortcutPlacementMode) {
            checkChangeLog();
        }

        tabLayout.setTabTextColors(Color.WHITE, Color.WHITE);
        tabLayout.setSelectedTabIndicatorColor(Color.WHITE);
    }

    private void openEditorForCreation() {
        Intent intent = new Intent(this, EditorActivity.class);
        startActivityForResult(intent, REQUEST_CREATE_SHORTCUT);
    }

    private void setupViewPager() {
        adapter = new CategoryPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(adapter);
        tabLayout.setupWithViewPager(viewPager);
    }

    @Override
    protected void onStart() {
        super.onStart();
        List<Category> categories = controller.getCategories();
        tabLayout.setVisibility(categories.size() > 1 ? View.VISIBLE : View.GONE);
        if (viewPager.getCurrentItem() >= categories.size()) {
            viewPager.setCurrentItem(0);
        }
        adapter.setCategories(categories, shortcutPlacementMode);
    }

    private void checkChangeLog() {
        ChangeLogDialog changeLog = new ChangeLogDialog(this, true);
        if (!changeLog.isPermanentlyHidden() && !changeLog.wasAlreadyShown()) {
            changeLog.show();
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        if (requestCode == REQUEST_CREATE_SHORTCUT) {
            long shortcutId = intent.getLongExtra(EditorActivity.EXTRA_SHORTCUT_ID, 0);
            Shortcut shortcut = controller.getShortcutById(shortcutId);
            if (shortcut == null) {
                return;
            }

            Category category;
            int currentCategory = viewPager.getCurrentItem();
            if (currentCategory < adapter.getCount()) {
                ListFragment currentListFragment = adapter.getItem(currentCategory);
                long categoryId = currentListFragment.getCategoryId();
                category = controller.getCategoryById(categoryId);
            } else {
                category = controller.getCategories().first();
            }
            controller.moveShortcut(shortcut, category);

            if (shortcutPlacementMode) {
                returnForHomeScreen(shortcut);
            }
        }
    }

    @Override
    public void returnForHomeScreen(Shortcut shortcut) {
        Intent shortcutIntent = getShortcutPlacementIntent(shortcut, true);
        setResult(RESULT_OK, shortcutIntent);
        finish();
    }

    @Override
    protected int getNavigateUpIcon() {
        return 0;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                openSettings();
                return true;
            case R.id.action_categories:
                openCategoriesEditor();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    private void openCategoriesEditor() {
        Intent intent = new Intent(this, CategoriesActivity.class);
        startActivity(intent);
    }

    private Intent getShortcutPlacementIntent(Shortcut shortcut, boolean install) {
        Intent shortcutIntent = new Intent(this, ExecuteActivity.class);
        shortcutIntent.setAction(ExecuteActivity.ACTION_EXECUTE_SHORTCUT);

        Uri uri = ContentUris.withAppendedId(Uri.fromParts("content", getPackageName(), null), shortcut.getId());
        shortcutIntent.setData(uri);

        Intent addIntent = new Intent();
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, shortcut.getName());
        if (shortcut.getIconName() != null) {

            Uri iconUri = shortcut.getIconURI(this);
            Bitmap icon;
            try {
                icon = Media.getBitmap(this.getContentResolver(), iconUri);
                addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON, icon);

            } catch (Exception e) {
                addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(getApplicationContext(), Shortcut.DEFAULT_ICON));
            }
        } else {
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(getApplicationContext(), Shortcut.DEFAULT_ICON));
        }

        addIntent.setAction(install ? ACTION_INSTALL_SHORTCUT : ACTION_UNINSTALL_SHORTCUT);

        return addIntent;
    }

    @Override
    public void placeShortcutOnHomeScreen(Shortcut shortcut) {
        sendBroadcast(getShortcutPlacementIntent(shortcut, true));
        showSnackbar(String.format(getString(R.string.shortcut_placed), shortcut.getName()));
    }

    @Override
    public void removeShortcutFromHomeScreen(Shortcut shortcut) {
        sendBroadcast(getShortcutPlacementIntent(shortcut, false));
    }
}
