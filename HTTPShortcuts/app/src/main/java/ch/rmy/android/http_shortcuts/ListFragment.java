package ch.rmy.android.http_shortcuts;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import ch.rmy.android.http_shortcuts.adapters.ShortcutAdapter;
import ch.rmy.android.http_shortcuts.http.Executor;
import ch.rmy.android.http_shortcuts.listeners.OnItemClickedListener;
import ch.rmy.android.http_shortcuts.realm.Controller;
import ch.rmy.android.http_shortcuts.realm.models.Category;
import ch.rmy.android.http_shortcuts.realm.models.Shortcut;
import ch.rmy.android.http_shortcuts.utils.MenuDialogBuilder;
import ch.rmy.android.http_shortcuts.utils.Settings;

public class ListFragment extends Fragment {

    private final static int REQUEST_EDIT_SHORTCUT = 2;

    @Bind(R.id.shortcut_list)
    RecyclerView shortcutList;

    private long categoryId;
    private boolean shortcutPlacementMode;
    @Nullable
    private Category category;

    private Controller controller;
    private ShortcutAdapter adapter;
    private Executor executor;
    private List<Category> categories;

    private final OnItemClickedListener<Shortcut> clickListener = new OnItemClickedListener<Shortcut>() {
        @Override
        public void onItemClicked(Shortcut shortcut) {
            if (shortcutPlacementMode) {
                getTabHost().returnForHomeScreen(shortcut);
            } else {
                String action = new Settings(getContext()).getClickBehavior();
                switch (action) {
                    case Settings.CLICK_BEHAVIOR_RUN:
                        executeShortcut(shortcut);
                        break;
                    case Settings.CLICK_BEHAVIOR_EDIT:
                        editShortcut(shortcut);
                        break;
                    case Settings.CLICK_BEHAVIOR_MENU:
                        showContextMenu(shortcut);
                        break;
                }
            }
        }

        @Override
        public void onItemLongClicked(Shortcut shortcut) {
            showContextMenu(shortcut);
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        controller = new Controller(getContext());
        executor = new Executor(getContext());
        adapter = new ShortcutAdapter(getContext());
        categories = controller.getCategories();

        onCategoryChanged();
    }

    public void setCategoryId(long categoryId) {
        this.categoryId = categoryId;
        onCategoryChanged();
    }

    private void onCategoryChanged() {
        if (controller == null) {
            return;
        }
        category = controller.getCategoryById(categoryId);
        adapter.setParent(category);
    }

    public long getCategoryId() {
        return categoryId;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        controller.destroy();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list, parent, false);
        ButterKnife.bind(this, view);

        adapter.setOnItemClickListener(clickListener);
        LinearLayoutManager manager = new LinearLayoutManager(getContext());
        shortcutList.setLayoutManager(manager);
        shortcutList.setHasFixedSize(true);
        shortcutList.addItemDecoration(new ShortcutListDecorator(getContext(), R.drawable.list_divider));
        shortcutList.setAdapter(adapter);

        return view;
    }

    public void setShortcutPlacementMode(boolean enabled) {
        this.shortcutPlacementMode = enabled;
    }

    private void showContextMenu(final Shortcut shortcut) {
        MenuDialogBuilder builder = new MenuDialogBuilder(getContext())
                .title(shortcut.getName());

        builder.item(R.string.action_place, new MenuDialogBuilder.Action() {
            @Override
            public void execute() {
                getTabHost().placeShortcutOnHomeScreen(shortcut);
            }
        });
        builder.item(R.string.action_run, new MenuDialogBuilder.Action() {
            @Override
            public void execute() {
                executeShortcut(shortcut);
            }
        });
        builder.item(R.string.action_edit, new MenuDialogBuilder.Action() {
            @Override
            public void execute() {
                editShortcut(shortcut);
            }
        });
        if (canMoveShortcut(shortcut, -1)) {
            builder.item(R.string.action_move_up, new MenuDialogBuilder.Action() {
                @Override
                public void execute() {
                    moveShortcut(shortcut, -1);
                }
            });
        }
        if (canMoveShortcut(shortcut, 1)) {
            builder.item(R.string.action_move_down, new MenuDialogBuilder.Action() {
                @Override
                public void execute() {
                    moveShortcut(shortcut, 1);
                }
            });
        }
        if (categories.size() > 1) {
            builder.item(R.string.action_move_to_category, new MenuDialogBuilder.Action() {
                @Override
                public void execute() {
                    showMoveToCategoryDialog(shortcut);
                }
            });
        }
        builder.item(R.string.action_duplicate, new MenuDialogBuilder.Action() {
            @Override
            public void execute() {
                duplicateShortcut(shortcut);
            }
        });
        builder.item(R.string.action_delete, new MenuDialogBuilder.Action() {
            @Override
            public void execute() {
                showDeleteDialog(shortcut);
            }
        });
        builder.show();
    }

    private void executeShortcut(Shortcut shortcut) {
        executor.execute(shortcut.getId());
    }

    private void editShortcut(Shortcut shortcut) {
        Intent intent = new Intent(getContext(), EditorActivity.class);
        intent.putExtra(EditorActivity.EXTRA_SHORTCUT_ID, shortcut.getId());
        startActivityForResult(intent, REQUEST_EDIT_SHORTCUT);
    }

    private boolean canMoveShortcut(Shortcut shortcut, int offset) {
        int position = category.getShortcuts().indexOf(shortcut) + offset;
        return position >= 0 && position < category.getShortcuts().size();
    }

    private void moveShortcut(Shortcut shortcut, int offset) {
        if (!canMoveShortcut(shortcut, offset)) {
            return;
        }
        int position = category.getShortcuts().indexOf(shortcut) + offset;
        if (position == category.getShortcuts().size()) {
            controller.moveShortcut(shortcut, category);
        } else {
            controller.moveShortcut(shortcut, position);
        }
    }

    private void showMoveToCategoryDialog(final Shortcut shortcut) {
        List<CharSequence> categoryNames = new ArrayList<>();
        final List<Category> categories = new ArrayList<>();
        for (Category category : this.categories) {
            if (category.getId() != this.category.getId()) {
                categoryNames.add(category.getName());
                categories.add(category);
            }
        }

        new MaterialDialog.Builder(getContext())
                .title(R.string.title_move_to_category)
                .items(categoryNames)
                .itemsCallback(new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
                        if (which < categories.size() && categories.get(which).isValid()) {
                            moveShortcut(shortcut, categories.get(which));
                        }
                    }
                })
                .show();
    }

    private void moveShortcut(Shortcut shortcut, Category category) {
        controller.moveShortcut(shortcut, category);
        getTabHost().showSnackbar(String.format(getText(R.string.shortcut_moved).toString(), shortcut.getName()));
    }

    private void duplicateShortcut(Shortcut shortcut) {
        String newName = String.format(getText(R.string.copy).toString(), shortcut.getName());
        Shortcut duplicate = controller.persist(shortcut.duplicate(newName));
        controller.moveShortcut(duplicate, category);
        int position = category.getShortcuts().indexOf(shortcut);
        controller.moveShortcut(duplicate, position + 1);

        getTabHost().showSnackbar(String.format(getText(R.string.shortcut_duplicated).toString(), shortcut.getName()));
    }

    private void showDeleteDialog(final Shortcut shortcut) {
        (new MaterialDialog.Builder(getContext()))
                .content(R.string.confirm_delete_shortcut_message)
                .positiveText(R.string.dialog_delete)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        deleteShortcut(shortcut);
                    }
                })
                .negativeText(R.string.dialog_cancel)
                .show();
    }

    private void deleteShortcut(Shortcut shortcut) {
        getTabHost().showSnackbar(String.format(getText(R.string.shortcut_deleted).toString(), shortcut.getName()));
        getTabHost().removeShortcutFromHomeScreen(shortcut);
        controller.deleteShortcut(shortcut);
    }

    private TabHost getTabHost() {
        return (TabHost) getActivity();
    }

    public interface TabHost {

        void returnForHomeScreen(Shortcut shortcut);

        void placeShortcutOnHomeScreen(Shortcut shortcut);

        void removeShortcutFromHomeScreen(Shortcut shortcut);

        void showSnackbar(CharSequence message);

    }

}
