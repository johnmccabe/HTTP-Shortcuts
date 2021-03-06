package ch.rmy.android.http_shortcuts.http;

import android.app.IntentService;
import android.content.ContentUris;
import android.content.Intent;
import android.net.Uri;

import ch.rmy.android.http_shortcuts.ExecuteActivity;
import ch.rmy.android.http_shortcuts.realm.Controller;
import ch.rmy.android.http_shortcuts.realm.models.PendingExecution;
import ch.rmy.android.http_shortcuts.utils.Connectivity;
import io.realm.RealmResults;

public class ExecutionService extends IntentService {

    private static final int INITIAL_DELAY = 1500;

    public ExecutionService() {
        super(ExecutionService.class.getName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        final Controller controller = new Controller(this);

        while (Connectivity.isNetworkConnected(this)) {
            RealmResults<PendingExecution> pendingExecutions = controller.getShortcutsPendingExecution();
            if (pendingExecutions.isEmpty()) {
                break;
            }

            final PendingExecution pendingExecution = pendingExecutions.first();
            long id = pendingExecution.getShortcutId();
            controller.removePendingExecution(pendingExecution);

            try {
                Thread.sleep(INITIAL_DELAY);
                executeShortcut(id);
            } catch (InterruptedException e) {
                break;
            }
        }

        controller.destroy();
    }

    private void executeShortcut(long id) {
        Intent shortcutIntent = new Intent(this, ExecuteActivity.class);
        shortcutIntent.setAction(ExecuteActivity.ACTION_EXECUTE_SHORTCUT);
        Uri uri = ContentUris.withAppendedId(Uri.fromParts("content", getPackageName(), null), id);
        shortcutIntent.setData(uri);
        shortcutIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(shortcutIntent);
    }

}
