package net.tsz.afinal;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import net.tsz.afinal.bitmap.core.BitmapCache;
import net.tsz.afinal.bitmap.core.BitmapCommonUtils;
import net.tsz.afinal.bitmap.core.BitmapDisplayConfig;
import net.tsz.afinal.bitmap.core.BitmapProcess;
import net.tsz.afinal.bitmap.display.Displayer;
import net.tsz.afinal.bitmap.display.SimpleDisplayer;
import net.tsz.afinal.bitmap.download.Downloader;
import net.tsz.afinal.bitmap.download.SimpleHttpDownloader;
import net.tsz.afinal.core.AsyncTask;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.widget.ImageView;

public class FinalBitmap {

	private FinalBitmapConfig			mConfig;
	private static BitmapCache			mImageCache;

	private boolean						mExitTasksEarly	= false;
	private boolean						mPauseWork			= false;
	private final Object					mPauseWorkLock		= new Object();
	private Context						mContext;

	private static ExecutorService	bitmapLoadAndDisplayExecutor;

	private static FinalBitmap			mFinalBitmap;

	private FinalBitmap(Context context) {
		mContext = context;
		mConfig = new FinalBitmapConfig(context);

		configDiskCachePath(BitmapCommonUtils.getDiskCacheDir(context, "ZE").getAbsolutePath());
		configDisplayer(new SimpleDisplayer());
		configDownlader(new SimpleHttpDownloader());
	}

	public static FinalBitmap create(Context ctx) {
		if (mFinalBitmap == null) {
			mFinalBitmap = new FinalBitmap(ctx.getApplicationContext());
			mFinalBitmap.init();
		}
		return mFinalBitmap;
	}

	public static FinalBitmap create(Context ctx, String diskCachePath) {
		if (mFinalBitmap == null) {
			mFinalBitmap = new FinalBitmap(ctx.getApplicationContext());
			mFinalBitmap.configDiskCachePath(diskCachePath);
			mFinalBitmap.init();
		}
		return mFinalBitmap;

	}

	public static FinalBitmap create(Context ctx, String diskCachePath, float memoryCacheSizePercent) {
		if (mFinalBitmap == null) {
			mFinalBitmap = new FinalBitmap(ctx.getApplicationContext());
			mFinalBitmap.configDiskCachePath(diskCachePath);
			mFinalBitmap.configMemoryCachePercent(memoryCacheSizePercent);
			mFinalBitmap.init();
		}

		return mFinalBitmap;
	}

	public static FinalBitmap create(Context ctx, String diskCachePath, int memoryCacheSize) {
		if (mFinalBitmap == null) {
			mFinalBitmap = new FinalBitmap(ctx.getApplicationContext());
			mFinalBitmap.configDiskCachePath(diskCachePath);
			mFinalBitmap.configMemoryCacheSize(memoryCacheSize);
			mFinalBitmap.init();
		}

		return mFinalBitmap;
	}

	public static FinalBitmap create(Context ctx, String diskCachePath, float memoryCacheSizePercent, int threadSize) {
		if (mFinalBitmap == null) {
			mFinalBitmap = new FinalBitmap(ctx.getApplicationContext());
			mFinalBitmap.configDiskCachePath(diskCachePath);
			mFinalBitmap.configBitmapLoadThreadSize(threadSize);
			mFinalBitmap.configMemoryCachePercent(memoryCacheSizePercent);
			mFinalBitmap.init();
		}

		return mFinalBitmap;
	}

	public static FinalBitmap create(Context ctx, String diskCachePath, int memoryCacheSize, int threadSize) {
		if (mFinalBitmap == null) {
			mFinalBitmap = new FinalBitmap(ctx.getApplicationContext());
			mFinalBitmap.configDiskCachePath(diskCachePath);
			mFinalBitmap.configBitmapLoadThreadSize(threadSize);
			mFinalBitmap.configMemoryCacheSize(memoryCacheSize);
			mFinalBitmap.init();
		}

		return mFinalBitmap;
	}

	public static FinalBitmap create(Context ctx, String diskCachePath, float memoryCacheSizePercent, int diskCacheSize,
			int threadSize) {
		if (mFinalBitmap == null) {
			mFinalBitmap = new FinalBitmap(ctx.getApplicationContext());
			mFinalBitmap.configDiskCachePath(diskCachePath);
			mFinalBitmap.configBitmapLoadThreadSize(threadSize);
			mFinalBitmap.configMemoryCachePercent(memoryCacheSizePercent);
			mFinalBitmap.configDiskCacheSize(diskCacheSize);
			mFinalBitmap.init();
		}

		return mFinalBitmap;
	}

	public static FinalBitmap create(Context ctx, String diskCachePath, int memoryCacheSize, int diskCacheSize,
			int threadSize) {
		if (mFinalBitmap == null) {
			mFinalBitmap = new FinalBitmap(ctx.getApplicationContext());
			mFinalBitmap.configDiskCachePath(diskCachePath);
			mFinalBitmap.configBitmapLoadThreadSize(threadSize);
			mFinalBitmap.configMemoryCacheSize(memoryCacheSize);
			mFinalBitmap.configDiskCacheSize(diskCacheSize);
			mFinalBitmap.init();
		}

		return mFinalBitmap;
	}

	public FinalBitmap configLoadingImage(Bitmap bitmap) {
		mConfig.defaultDisplayConfig.setLoadingBitmap(bitmap);
		return this;
	}

	public FinalBitmap configLoadingImage(int resId) {
		mConfig.defaultDisplayConfig.setLoadingBitmap(BitmapFactory.decodeResource(mContext.getResources(), resId));
		return this;
	}

	public FinalBitmap configLoadfailImage(Bitmap bitmap) {
		mConfig.defaultDisplayConfig.setLoadfailBitmap(bitmap);
		return this;
	}

	public FinalBitmap configLoadfailImage(int resId) {
		mConfig.defaultDisplayConfig.setLoadfailBitmap(BitmapFactory.decodeResource(mContext.getResources(), resId));
		return this;
	}

	public FinalBitmap configBitmapMaxHeight(int bitmapHeight) {
		mConfig.defaultDisplayConfig.setBitmapHeight(bitmapHeight);
		return this;
	}

	public FinalBitmap configBitmapMaxWidth(int bitmapWidth) {
		mConfig.defaultDisplayConfig.setBitmapWidth(bitmapWidth);
		return this;
	}

	public FinalBitmap configDownlader(Downloader downlader) {
		mConfig.downloader = downlader;
		mConfig.init();
		return this;
	}

	public FinalBitmap configDisplayer(Displayer displayer) {
		mConfig.displayer = displayer;
		return this;
	}

	public void configCompressFormat(CompressFormat format) {
		mImageCache.setCompressFormat(format);
	}

	public FinalBitmap configCalculateBitmapSizeWhenDecode(boolean neverCalculate) {
		if (mConfig != null && mConfig.bitmapProcess != null)
			mConfig.bitmapProcess.configCalculateBitmap(neverCalculate);
		return this;
	}

	private FinalBitmap configDiskCachePath(String strPath) {
		if (!TextUtils.isEmpty(strPath)) {
			mConfig.cachePath = strPath;
		}
		return this;
	}

	private FinalBitmap configMemoryCacheSize(int size) {
		mConfig.memCacheSize = size;
		return this;
	}

	private FinalBitmap configMemoryCachePercent(float percent) {
		mConfig.memCacheSizePercent = percent;
		return this;
	}

	private FinalBitmap configDiskCacheSize(int size) {
		mConfig.diskCacheSize = size;
		return this;
	}

	private FinalBitmap configBitmapLoadThreadSize(int size) {
		if (size >= 1)
			mConfig.poolSize = size;
		return this;
	}

	private FinalBitmap init() {

		mConfig.init();

		BitmapCache.ImageCacheParams imageCacheParams = new BitmapCache.ImageCacheParams(mConfig.cachePath);
		if (mConfig.memCacheSizePercent > 0.05 && mConfig.memCacheSizePercent < 0.8) {
			imageCacheParams.setMemCacheSizePercent(mContext, mConfig.memCacheSizePercent);
		} else {
			if (mConfig.memCacheSize > 1024 * 1024 * 2) {
				imageCacheParams.setMemCacheSize(mConfig.memCacheSize);
			} else {
				imageCacheParams.setMemCacheSizePercent(mContext, 0.3f);
			}
		}
		if (mConfig.diskCacheSize > 1024 * 1024 * 5)
			imageCacheParams.setDiskCacheSize(mConfig.diskCacheSize);
		mImageCache = new BitmapCache(imageCacheParams);

		bitmapLoadAndDisplayExecutor = Executors.newFixedThreadPool(mConfig.poolSize, new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r);
				t.setPriority(Thread.NORM_PRIORITY - 1);
				return t;
			}
		});

		new CacheExecutecTask().execute(CacheExecutecTask.MESSAGE_INIT_DISK_CACHE);

		return this;
	}

	public void display(ImageView imageView, String uri) {
		doDisplay(imageView, uri, null);
	}

	public void display(ImageView imageView, String uri, int imageWidth, int imageHeight) {
		BitmapDisplayConfig displayConfig = configMap.get(imageWidth + "_" + imageHeight);
		if (displayConfig == null) {
			displayConfig = getDisplayConfig();
			displayConfig.setBitmapHeight(imageHeight);
			displayConfig.setBitmapWidth(imageWidth);
			configMap.put(imageWidth + "_" + imageHeight, displayConfig);
		}

		doDisplay(imageView, uri, displayConfig);
	}

	public void display(ImageView imageView, String uri, Bitmap loadingBitmap) {
		BitmapDisplayConfig displayConfig = configMap.get(String.valueOf(loadingBitmap));
		if (displayConfig == null) {
			displayConfig = getDisplayConfig();
			displayConfig.setLoadingBitmap(loadingBitmap);
			configMap.put(String.valueOf(loadingBitmap), displayConfig);
		}

		doDisplay(imageView, uri, displayConfig);
	}

	public void display(ImageView imageView, String uri, Bitmap loadingBitmap, Bitmap laodfailBitmap) {
		BitmapDisplayConfig displayConfig = configMap.get(String.valueOf(loadingBitmap) + "_"
				+ String.valueOf(laodfailBitmap));
		if (displayConfig == null) {
			displayConfig = getDisplayConfig();
			displayConfig.setLoadingBitmap(loadingBitmap);
			displayConfig.setLoadfailBitmap(laodfailBitmap);
			configMap.put(String.valueOf(loadingBitmap) + "_" + String.valueOf(laodfailBitmap), displayConfig);
		}

		doDisplay(imageView, uri, displayConfig);
	}

	public void display(ImageView imageView, String uri, int imageWidth, int imageHeight, Bitmap loadingBitmap,
			Bitmap laodfailBitmap) {
		BitmapDisplayConfig displayConfig = configMap.get(imageWidth + "_" + imageHeight + "_"
				+ String.valueOf(loadingBitmap) + "_" + String.valueOf(laodfailBitmap));
		if (displayConfig == null) {
			displayConfig = getDisplayConfig();
			displayConfig.setBitmapHeight(imageHeight);
			displayConfig.setBitmapWidth(imageWidth);
			displayConfig.setLoadingBitmap(loadingBitmap);
			displayConfig.setLoadfailBitmap(laodfailBitmap);
			configMap.put(
					imageWidth + "_" + imageHeight + "_" + String.valueOf(loadingBitmap) + "_"
							+ String.valueOf(laodfailBitmap), displayConfig);
		}

		doDisplay(imageView, uri, displayConfig);
	}

	public void display(ImageView imageView, String uri, BitmapDisplayConfig config) {
		doDisplay(imageView, uri, config);
	}

	private void doDisplay(ImageView imageView, String uri, BitmapDisplayConfig displayConfig) {
		if (TextUtils.isEmpty(uri) || imageView == null) {
			return;
		}

		if (displayConfig == null)
			displayConfig = mConfig.defaultDisplayConfig;

		Bitmap bitmap = null;

		if (mImageCache != null) {
			bitmap = getBitmapFromCache(uri);
		}

		if (bitmap != null) {
			imageView.setImageBitmap(bitmap);

		} else if (checkImageTask(uri, imageView)) {

			final BitmapLoadAndDisplayTask task = new BitmapLoadAndDisplayTask(imageView, displayConfig);
			final AsyncDrawable asyncDrawable = new AsyncDrawable(mContext.getResources(),
					displayConfig.getLoadingBitmap(), task);
			imageView.setImageDrawable(asyncDrawable);

			task.executeOnExecutor(bitmapLoadAndDisplayExecutor, uri);
		}
	}

	private HashMap<String, BitmapDisplayConfig>	configMap	= new HashMap<String, BitmapDisplayConfig>();

	private BitmapDisplayConfig getDisplayConfig() {
		BitmapDisplayConfig config = new BitmapDisplayConfig();
		config.setAnimation(mConfig.defaultDisplayConfig.getAnimation());
		config.setAnimationType(mConfig.defaultDisplayConfig.getAnimationType());
		config.setBitmapHeight(mConfig.defaultDisplayConfig.getBitmapHeight());
		config.setBitmapWidth(mConfig.defaultDisplayConfig.getBitmapWidth());
		config.setLoadfailBitmap(mConfig.defaultDisplayConfig.getLoadfailBitmap());
		config.setLoadingBitmap(mConfig.defaultDisplayConfig.getLoadingBitmap());
		return config;
	}

	private void initDiskCacheInternalInBackgroud() {
		if (mImageCache != null) {
			mImageCache.initDiskCache();
		}
		if (mConfig != null && mConfig.bitmapProcess != null) {
			mConfig.bitmapProcess.initHttpDiskCache();
		}
	}

	private void clearCacheInternalInBackgroud() {
		if (mImageCache != null) {
			mImageCache.clearCache();
		}
		if (mConfig != null && mConfig.bitmapProcess != null) {
			mConfig.bitmapProcess.clearCacheInternal();
		}
	}

	private void clearMemoryCacheInBackgroud() {
		if (mImageCache != null) {
			mImageCache.clearMemoryCache();
		}
	}

	private void clearDiskCacheInBackgroud() {
		if (mImageCache != null) {
			mImageCache.clearDiskCache();
		}
		if (mConfig != null && mConfig.bitmapProcess != null) {
			mConfig.bitmapProcess.clearCacheInternal();
		}
	}

	private void clearCacheInBackgroud(String key) {
		if (mImageCache != null) {
			mImageCache.clearCache(key);
		}
	}

	private void clearDiskCacheInBackgroud(String key) {
		if (mImageCache != null) {
			mImageCache.clearDiskCache(key);
		}
	}

	private void clearMemoryCacheInBackgroud(String key) {
		if (mImageCache != null) {
			mImageCache.clearMemoryCache(key);
		}
	}

	private void flushCacheInternalInBackgroud() {
		if (mImageCache != null) {
			mImageCache.flush();
		}
		if (mConfig != null && mConfig.bitmapProcess != null) {
			mConfig.bitmapProcess.flushCacheInternal();
		}
	}

	/**
	 * 
	 * @author fantouch
	 */
	private void closeCacheInternalInBackgroud() {
		if (mImageCache != null) {
			mImageCache.close();
			mImageCache = null;

			mFinalBitmap = null;
		}
		if (mConfig != null && mConfig.bitmapProcess != null) {
			mConfig.bitmapProcess.clearCacheInternal();
		}
	}

	private Bitmap processBitmap(String uri, BitmapDisplayConfig config) {
		if (mConfig != null && mConfig.bitmapProcess != null) {
			return mConfig.bitmapProcess.processBitmap(uri, config);
		}
		return null;
	}

	public Bitmap getBitmapFromCache(String key) {
		Bitmap bitmap = getBitmapFromMemoryCache(key);
		if (bitmap == null)
			bitmap = getBitmapFromDiskCache(key);

		return bitmap;
	}

	public Bitmap getBitmapFromMemoryCache(String key) {
		return mImageCache.getBitmapFromMemCache(key);
	}

	public Bitmap getBitmapFromDiskCache(String key) {
		return mImageCache.getBitmapFromDiskCache(key);
	}

	public void setExitTasksEarly(boolean exitTasksEarly) {
		mExitTasksEarly = exitTasksEarly;
	}

	public void onResume() {
		setExitTasksEarly(false);
	}

	public void onPause() {
		setExitTasksEarly(true);
		flushCache();
	}

	public void onDestroy() {
		closeCache();
	}

	public void clearCache() {
		new CacheExecutecTask().execute(CacheExecutecTask.MESSAGE_CLEAR);
	}

	public void clearCache(String key) {
		new CacheExecutecTask().execute(CacheExecutecTask.MESSAGE_CLEAR_KEY, key);
	}

	/**
	 * 娓呴櫎缂撳瓨
	 */
	public void clearMemoryCache() {
		new CacheExecutecTask().execute(CacheExecutecTask.MESSAGE_CLEAR_MEMORY);
	}

	public void clearMemoryCache(String key) {
		new CacheExecutecTask().execute(CacheExecutecTask.MESSAGE_CLEAR_KEY_IN_MEMORY, key);
	}

	public void clearDiskCache() {
		new CacheExecutecTask().execute(CacheExecutecTask.MESSAGE_CLEAR_DISK);
	}

	public void clearDiskCache(String key) {
		new CacheExecutecTask().execute(CacheExecutecTask.MESSAGE_CLEAR_KEY_IN_DISK, key);
	}

	/**
	 * 鍒锋柊缂撳瓨
	 */
	public void flushCache() {
		new CacheExecutecTask().execute(CacheExecutecTask.MESSAGE_FLUSH);
	}

	public void closeCache() {
		new CacheExecutecTask().execute(CacheExecutecTask.MESSAGE_CLOSE);
	}

	public void exitTasksEarly(boolean exitTasksEarly) {
		mExitTasksEarly = exitTasksEarly;
		if (exitTasksEarly)
			pauseWork(false);// 璁╂殏鍋滅殑绾跨▼缁撴潫
	}

	public void pauseWork(boolean pauseWork) {
		synchronized (mPauseWorkLock) {
			mPauseWork = pauseWork;
			if (!mPauseWork) {
				mPauseWorkLock.notifyAll();
			}
		}
	}

	private static BitmapLoadAndDisplayTask getBitmapTaskFromImageView(ImageView imageView) {
		if (imageView != null) {
			final Drawable drawable = imageView.getDrawable();
			if (drawable instanceof AsyncDrawable) {
				final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
				return asyncDrawable.getBitmapWorkerTask();
			}
		}
		return null;
	}

	public static boolean checkImageTask(Object data, ImageView imageView) {
		final BitmapLoadAndDisplayTask bitmapWorkerTask = getBitmapTaskFromImageView(imageView);

		if (bitmapWorkerTask != null) {
			final Object bitmapData = bitmapWorkerTask.data;
			if (bitmapData == null || !bitmapData.equals(data)) {
				bitmapWorkerTask.cancel(true);
			} else {
				// 鍚屼竴涓嚎绋嬪凡缁忓湪鎵ц
				return false;
			}
		}
		return true;
	}

	private static class AsyncDrawable extends BitmapDrawable {
		private final WeakReference<BitmapLoadAndDisplayTask>	bitmapWorkerTaskReference;

		public AsyncDrawable(Resources res, Bitmap bitmap, BitmapLoadAndDisplayTask bitmapWorkerTask) {
			super(res, bitmap);
			bitmapWorkerTaskReference = new WeakReference<BitmapLoadAndDisplayTask>(bitmapWorkerTask);
		}

		public BitmapLoadAndDisplayTask getBitmapWorkerTask() {
			return bitmapWorkerTaskReference.get();
		}
	}

	/**
	 * @title 缂撳瓨鎿嶄綔鐨勫紓姝ヤ换鍔�
	 * @description 鎿嶄綔缂撳瓨
	 * @company 鎺㈢储鑰呯綉缁滃伐浣滃(www.tsz.net)
	 * @author michael Young (www.YangFuhai.com)
	 * @version 1.0
	 * @created 2012-10-28
	 */
	private class CacheExecutecTask extends AsyncTask<Object, Void, Void> {
		public static final int	MESSAGE_CLEAR						= 0;
		public static final int	MESSAGE_INIT_DISK_CACHE			= 1;
		public static final int	MESSAGE_FLUSH						= 2;
		public static final int	MESSAGE_CLOSE						= 3;
		public static final int	MESSAGE_CLEAR_MEMORY				= 4;
		public static final int	MESSAGE_CLEAR_DISK				= 5;
		public static final int	MESSAGE_CLEAR_KEY					= 6;
		public static final int	MESSAGE_CLEAR_KEY_IN_MEMORY	= 7;
		public static final int	MESSAGE_CLEAR_KEY_IN_DISK		= 8;

		@Override
		protected Void doInBackground(Object... params) {
			switch ((Integer) params[0]) {
			case MESSAGE_CLEAR:
				clearCacheInternalInBackgroud();
				break;
			case MESSAGE_INIT_DISK_CACHE:
				initDiskCacheInternalInBackgroud();
				break;
			case MESSAGE_FLUSH:
				clearMemoryCacheInBackgroud();
				flushCacheInternalInBackgroud();
				break;
			case MESSAGE_CLOSE:
				clearMemoryCacheInBackgroud();
				closeCacheInternalInBackgroud();
			case MESSAGE_CLEAR_MEMORY:
				clearMemoryCacheInBackgroud();
				break;
			case MESSAGE_CLEAR_DISK:
				clearDiskCacheInBackgroud();
				break;
			case MESSAGE_CLEAR_KEY:
				clearCacheInBackgroud(String.valueOf(params[1]));
				break;
			case MESSAGE_CLEAR_KEY_IN_MEMORY:
				clearMemoryCacheInBackgroud(String.valueOf(params[1]));
				break;
			case MESSAGE_CLEAR_KEY_IN_DISK:
				clearDiskCacheInBackgroud(String.valueOf(params[1]));
				break;
			}
			return null;
		}
	}

	private class BitmapLoadAndDisplayTask extends AsyncTask<Object, Void, Bitmap> {
		private Object									data;
		private final WeakReference<ImageView>	imageViewReference;
		private final BitmapDisplayConfig		displayConfig;

		public BitmapLoadAndDisplayTask(ImageView imageView, BitmapDisplayConfig config) {
			imageViewReference = new WeakReference<ImageView>(imageView);
			displayConfig = config;
		}

		@Override
		protected Bitmap doInBackground(Object... params) {
			data = params[0];
			final String dataString = String.valueOf(data);
			Bitmap bitmap = null;

			synchronized (mPauseWorkLock) {
				while (mPauseWork && !isCancelled()) {
					try {
						mPauseWorkLock.wait();
					} catch (InterruptedException e) {
					}
				}
			}

			if (mImageCache != null && !isCancelled() && getAttachedImageView() != null && !mExitTasksEarly) {
				bitmap = mImageCache.getBitmapFromDiskCache(dataString);
			}

			if (bitmap == null && !isCancelled() && getAttachedImageView() != null && !mExitTasksEarly) {
				bitmap = processBitmap(dataString, displayConfig);
			}

			if (bitmap != null && mImageCache != null) {
				mImageCache.addBitmapToCache(dataString, bitmap);
			}

			return bitmap;
		}

		@Override
		protected void onPostExecute(Bitmap bitmap) {
			if (isCancelled() || mExitTasksEarly) {
				bitmap = null;
			}

			// 鍒ゆ柇绾跨▼鍜屽綋鍓嶇殑imageview鏄惁鏄尮閰�
			final ImageView imageView = getAttachedImageView();
			if (bitmap != null && imageView != null) {
				mConfig.displayer.loadCompletedisplay(imageView, bitmap, displayConfig);
			} else if (bitmap == null && imageView != null) {
				mConfig.displayer.loadFailDisplay(imageView, displayConfig.getLoadfailBitmap());
			}
		}

		@Override
		protected void onCancelled(Bitmap bitmap) {
			super.onCancelled(bitmap);
			synchronized (mPauseWorkLock) {
				mPauseWorkLock.notifyAll();
			}
		}

		/**
		 * 鑾峰彇绾跨▼鍖归厤鐨刬mageView,闃叉鍑虹幇闂姩鐨勭幇璞�
		 * 
		 * @return
		 */
		private ImageView getAttachedImageView() {
			final ImageView imageView = imageViewReference.get();
			final BitmapLoadAndDisplayTask bitmapWorkerTask = getBitmapTaskFromImageView(imageView);

			if (this == bitmapWorkerTask) {
				return imageView;
			}

			return null;
		}
	}

	/**
	 * @title 閰嶇疆淇℃伅
	 * @description FinalBitmap鐨勯厤缃俊鎭�
	 * @company 鎺㈢储鑰呯綉缁滃伐浣滃(www.tsz.net)
	 * @author michael Young (www.YangFuhai.com)
	 * @version 1.0
	 * @created 2012-10-28
	 */
	private class FinalBitmapConfig {

		public String					cachePath;

		public Displayer				displayer;
		public Downloader				downloader;
		public BitmapProcess			bitmapProcess;
		public BitmapDisplayConfig	defaultDisplayConfig;
		public float					memCacheSizePercent;								// 缂撳瓨鐧惧垎姣旓紝android绯荤粺鍒嗛厤缁欐瘡涓猘pk鍐呭瓨鐨勫ぇ灏�
		public int						memCacheSize;
		public int						diskCacheSize;										// 纾佺洏鐧惧垎姣�
		public int						poolSize						= 3;						// 榛樿鐨勭嚎绋嬫睜绾跨▼骞跺彂鏁伴噺
		public int						originalDiskCacheSize	= 30 * 1024 * 1024;	// 50MB

		public FinalBitmapConfig(Context context) {
			defaultDisplayConfig = new BitmapDisplayConfig();

			defaultDisplayConfig.setAnimation(null);
			defaultDisplayConfig.setAnimationType(BitmapDisplayConfig.AnimationType.fadeIn);

			DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
			// int defaultWidth = (int) Math.floor(displayMetrics.widthPixels / 4);
			int defaultWidth = displayMetrics.widthPixels;
			defaultDisplayConfig.setBitmapHeight(defaultWidth);
			defaultDisplayConfig.setBitmapWidth(defaultWidth);

		}

		public void init() {
			if (downloader == null)
				downloader = new SimpleHttpDownloader();

			if (displayer == null)
				displayer = new SimpleDisplayer();

			bitmapProcess = new BitmapProcess(downloader, cachePath, originalDiskCacheSize);
		}

	}

}
