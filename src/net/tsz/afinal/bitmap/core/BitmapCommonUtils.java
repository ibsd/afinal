package net.tsz.afinal.bitmap.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.os.StatFs;

import java.io.File;

public class BitmapCommonUtils {
    public static File getDiskCacheDir(Context context, String uniqueName) {
        final String cachePath = Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) ? getExternalCacheDir(
                context).getPath()
                : context.getCacheDir().getPath();

        return new File(cachePath + File.separator + uniqueName);
    }

    public static int getBitmapSize(Bitmap bitmap) {
        return bitmap.getRowBytes() * bitmap.getHeight();
    }

    public static File getExternalCacheDir(Context context) {
        final String cacheDir = "/Android/data/" + context.getPackageName() + "/cache/";
        return new File(Environment.getExternalStorageDirectory().getPath() + cacheDir);
    }

    public static long getUsableSpace(File path) {
        try {
            final StatFs stats = new StatFs(path.getPath());
            return (long) stats.getBlockSize() * (long) stats.getAvailableBlocks();
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }

    }

}
