package ml.puredark.hviewer.helpers;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.text.TextUtils;

import java.io.File;
import java.util.List;

import ml.puredark.hviewer.HViewerApplication;
import ml.puredark.hviewer.activities.SettingActivity;
import ml.puredark.hviewer.beans.DownloadTask;
import ml.puredark.hviewer.beans.LocalCollection;
import ml.puredark.hviewer.holders.DownloadTaskHolder;
import ml.puredark.hviewer.services.DownloadService;
import ml.puredark.hviewer.utils.SharedPreferencesUtil;
import ml.puredark.hviewer.utils.SimpleFileUtil;

import static android.content.Context.BIND_AUTO_CREATE;

/**
 * Created by PureDark on 2016/8/15.
 */

public class DownloadManager {
    private final static String DEFAULT_PATH = "/sdcard/H-Viewer/download";
    private DownloadTaskHolder holder;
    private DownloadService.DownloadBinder binder;

    private ServiceConnection conn = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (service instanceof DownloadService.DownloadBinder)
                binder = (DownloadService.DownloadBinder) service;
        }

        public void onServiceDisconnected(ComponentName name) {
        }
    };

    public DownloadManager(Context context) {
        holder = new DownloadTaskHolder(context);
        context.bindService(new Intent(context, DownloadService.class), conn, BIND_AUTO_CREATE);
        checkNoMediaFile();
    }

    private void checkNoMediaFile() {
        SimpleFileUtil.createIfNotExist(getDownloadPath() + "/.nomedia");
    }

    public static String getDownloadPath() {
        String downloadPath = (String) SharedPreferencesUtil.getData(HViewerApplication.mContext, SettingActivity.SettingFragment.KEY_PREF_DOWNLOAD_PATH, DEFAULT_PATH);
        if (downloadPath != null)
            return downloadPath;
        else
            return DEFAULT_PATH;
    }

    public boolean isDownloading() {
        return (binder.getCurrTask() != null && binder.getCurrTask().status == DownloadTask.STATUS_DOWNLOADING);
    }

    public List<DownloadTask> getDownloadTasks() {
        return holder.getDownloadTasks();
    }

    public boolean createDownloadTask(LocalCollection collection) {
        String path = getDownloadPath() + "/" + collection.title + "/";
        DownloadTask task = new DownloadTask(holder.getDownloadTasks().size() + 1, collection, path);
        if (holder.isInList(task) || binder == null)
            return false;
        int did = holder.addDownloadTask(task);
        task.did = did;
        if (TextUtils.isEmpty(collection.title)) {
            path = getDownloadPath() + "/" + collection.site.title + "_" + did + "/";
        }else if(new File(path).exists()){
            path = getDownloadPath() + "/" + collection.title + " (2)/";
        }
        task.path = path;
        holder.updateDownloadTasks(task);
        if (!isDownloading())
            startDownload(task);
        return true;
    }

    public void startDownload(DownloadTask task) {
        binder.start(task);
    }

    public void pauseDownload() {
        binder.pause();
    }

    public void deleteDownloadTask(DownloadTask downloadTask) {
        holder.deleteDownloadTask(downloadTask);
        binder.stop();
    }

    public void unbindService(Context context) {
        try {
            context.unbindService(conn);
        } catch (Exception e) {
        }
    }

}
