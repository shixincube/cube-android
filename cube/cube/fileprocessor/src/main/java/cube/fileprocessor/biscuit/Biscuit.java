package cube.fileprocessor.biscuit;


import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.util.Size;

import androidx.annotation.IntDef;

import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 图片压缩处理实用库。
 */
public class Biscuit {

    private final static String TAG = "Biscuit";

    public final static int SCALE = 0;
    public final static int SAMPLE = 1;

    Dispatcher mDispatcher;

    private Executor mExecutor;
    private String targetDir;
    private boolean ignoreAlpha;
    private int quality;
    private int compressType;
    private boolean useOriginalName;
    private long thresholdSize;
    private ArrayList<CompressListener> mCompressListeners;
    private ArrayList<String> mPaths;
    private CompressResult mCompressResult;
    private OnCompressCompletedListener mOnCompressCompletedListener;

    Biscuit(ArrayList<String> paths, String targetDir, boolean ignoreAlpha, int quality,
            int compressType, boolean useOriginalName, boolean loggingEnabled,
            long thresholdSize, CompressListener compressListener,
            OnCompressCompletedListener onCompressCompletedListener, Executor executor) {
        Utils.loggingEnabled = loggingEnabled;

        mExecutor = executor;

        mCompressListeners = new ArrayList<>();
        addListener(compressListener);
        mPaths = new ArrayList<>();
        if (paths != null) {
            mPaths.addAll(paths);
        }
        this.targetDir = targetDir;
        this.ignoreAlpha = ignoreAlpha;
        this.quality = quality;
        this.compressType = compressType;
        this.useOriginalName = useOriginalName;
        this.thresholdSize = thresholdSize;
        this.mOnCompressCompletedListener = onCompressCompletedListener;
    }

    public void asyncCompress() {
        checkExecutorAndDispatcher();
        mCompressResult = new CompressResult();
        Iterator<String> iterator = mPaths.iterator();
        while (iterator.hasNext()) {
            String path = iterator.next();
            if (Utils.isImage(path)) {
                Compressor compressor = new ImageCompressor(path, targetDir, quality, compressType, ignoreAlpha, useOriginalName, thresholdSize, this);
                mExecutor.execute(compressor);
            } else {
                iterator.remove();
                Log.d(TAG, "can not recognize the path : " + path);
            }
        }
    }

    private void checkExecutorAndDispatcher() {
        if (mExecutor == null) {
            mExecutor = new HandlerExecutor();
        }
        if (mDispatcher == null) {
            mDispatcher = new Dispatcher();
        }
    }

    public ArrayList<Result> syncCompress() {
        ArrayList<Result> results = new ArrayList<>();
        Iterator<String> iterator = mPaths.iterator();
        while (iterator.hasNext()) {
            String path = iterator.next();
            if (Utils.isImage(path)) {
                ImageCompressor compressor = new ImageCompressor(path, targetDir, quality,
                        compressType, ignoreAlpha, useOriginalName, thresholdSize, null);
                boolean success = compressor.compress();
                Result result = null;
                if (success) {
                    result = new Result(compressor.targetPath,
                            compressor.inputWidth, compressor.inputHeight,
                            compressor.outputWidth, compressor.outputHeight);

                }
                else {
                    result = new Result(path,
                            compressor.inputWidth, compressor.inputHeight,
                            compressor.outputWidth, compressor.outputHeight);
                }
                results.add(result);
            } else {
                Log.d(TAG, "Can not recognize the path : " + path);
            }
            iterator.remove();
        }
        return results;
    }


    public void addListener(CompressListener compressListener) {
        if (compressListener != null) {
            mCompressListeners.add(compressListener);
        }
    }

    public void removeListener(CompressListener compressListener) {
        mCompressListeners.remove(compressListener);
    }

    public void setOnCompressCompletedListener(OnCompressCompletedListener compressCompletedListener) {
        this.mOnCompressCompletedListener = compressCompletedListener;
    }

    public String getTargetDir() {
        return targetDir;
    }

    public void setTargetDir(String targetDir) {
        this.targetDir = targetDir;
    }

    public boolean isIgnoreAlpha() {
        return ignoreAlpha;
    }

    public void setIgnoreAlpha(boolean ignoreAlpha) {
        this.ignoreAlpha = ignoreAlpha;
    }

    public int getQuality() {
        return quality;
    }

    public void setQuality(int quality) {
        if (quality < 0 || quality > 100) {
            throw new IllegalArgumentException("quality must be 0..100");
        }
        this.quality = quality;
    }

    public int getCompressType() {
        return compressType;
    }

    public void setCompressType(@CompressType int compressType) {
        this.compressType = compressType;
    }

    public boolean isUseOriginalName() {
        return useOriginalName;
    }

    public void setUseOriginalName(boolean useOriginalName) {
        this.useOriginalName = useOriginalName;
    }

    public long getThresholdSize() {
        return thresholdSize;
    }

    public void setThresholdSize(long thresholdSize) {
        this.thresholdSize = thresholdSize;
    }

    public ArrayList<String> getPaths() {
        return mPaths;
    }

    public void addPaths(ArrayList<String> paths) {
        if (paths != null && paths.size() > 0) {
            mPaths.addAll(paths);
        }
    }

    public void addPaths(String path) {
        if (!TextUtils.isEmpty(path)) {
            mPaths.add(path);
        }
    }

    public static Builder with(Context context) {
        return new Builder(context);
    }

    //default
    public static void clearCache(Context context) {
        Utils.clearCache(context);
    }

    //if have been customize cache dir
    public static void clearCache(String dir) {
        Utils.clearCache(dir);
    }

    void dispatchSuccess(String targetPath) {
        for (CompressListener compressListener :
                mCompressListeners) {
            compressListener.onSuccess(targetPath);
        }
        mCompressResult.mSuccessPaths.add(targetPath);
        dispatchFullCompressResult();
    }

    private void dispatchFullCompressResult() {
        if (mCompressResult.mExceptionPaths.size() + mCompressResult.mSuccessPaths.size() == mPaths.size()) {
            mPaths.clear();
            if (mOnCompressCompletedListener != null) {
                mOnCompressCompletedListener.onCompressCompleted(mCompressResult);
            }
        }
    }

    void dispatchError(CompressException exception) {
        for (CompressListener compressListener :
                mCompressListeners) {
            compressListener.onError(exception);
        }
        mCompressResult.mExceptionPaths.add(exception.originalPath);
        dispatchFullCompressResult();
    }

    @IntDef({SAMPLE, SCALE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface CompressType {
    }

    public class Result {

        public final String path;
        public final int inputWidth;
        public final int inputHeight;
        public final int outputWidth;
        public final int outputHeight;

        protected Result(String path, int inputWidth, int inputHeight, int outputWidth, int outputHeight) {
            this.path = path;
            this.inputWidth = inputWidth;
            this.inputHeight = inputHeight;
            this.outputWidth = outputWidth;
            this.outputHeight = outputHeight;
        }

        public Result reset(String path) {
            return new Result(path, this.inputWidth, this.inputHeight, this.outputWidth, this.outputHeight);
        }

        public Result reset(String path, Size size) {
            return new Result(path, size.getWidth(), size.getHeight(), size.getWidth(), size.getHeight());
        }
    }

    public static class Builder {
        private ArrayList<String> mPaths;
        private String mTargetDir;
        private boolean mIgnoreAlpha;
        private int mQuality;
        private int mCompressType;
        private boolean mUseOriginalName;
        private CompressListener mCompressListener;
        private Context mContext;
        private Executor mExecutor;
        private boolean loggingEnabled;
        private long mThresholdSize;
        private OnCompressCompletedListener mOnCompressCompletedListener;

        public Builder(Context context) {
            this.mContext = context;
            mQuality = Utils.getDefaultQuality(context);
            mPaths = new ArrayList<>();
            mCompressType = SCALE;
            mIgnoreAlpha = false;
            mUseOriginalName = false;
            loggingEnabled = false;
            mThresholdSize = -1;
        }

        public Builder targetDir(String targetDir) {
            if (!TextUtils.isEmpty(targetDir)) {
                String last = targetDir.substring(targetDir.length() - 1, targetDir.length());
                if (!last.equals(File.separator)) {
                    throw new IllegalArgumentException("targetDir must be end with " + File.separator);
                }
            }
            mTargetDir = targetDir;
            return this;
        }

        public Builder ignoreAlpha(boolean ignoreAlpha) {
            mIgnoreAlpha = ignoreAlpha;
            return this;
        }

        /**
         * Note that the unit is KB
         */
        public Builder ignoreLessThan(long thresholdSize) {
            mThresholdSize = thresholdSize;
            return this;
        }

        public Builder originalName(boolean originalName) {
            mUseOriginalName = originalName;
            return this;
        }

        public Builder compressType(@CompressType int compressType) {
            mCompressType = compressType;
            return this;
        }

        public Builder loggingEnabled(boolean enabled) {
            loggingEnabled = enabled;
            return this;
        }

        public Builder executor(Executor executor) {
            mExecutor = executor;
            return this;
        }

        public Builder quality(int quality) {
            if (quality < 0 || quality > 100) {
                throw new IllegalArgumentException("quality must be 0..100");
            }
            mQuality = quality;
            return this;
        }

        public Builder listener(CompressListener compressListener) {
            mCompressListener = compressListener;
            return this;
        }

        public Builder path(String source) {
            mPaths.add(source);
            return this;
        }

        public Builder path(List<String> source) {
            mPaths.addAll(source);
            return this;
        }

        public Builder listener(OnCompressCompletedListener compressCompletedListener) {
            mOnCompressCompletedListener = compressCompletedListener;
            return this;
        }

        public Biscuit build() {
            if (TextUtils.isEmpty(mTargetDir)) {
                mTargetDir = Utils.getCacheDir(mContext) + File.separator;
            }
            return new Biscuit(mPaths, mTargetDir, mIgnoreAlpha, mQuality, mCompressType, mUseOriginalName, loggingEnabled, mThresholdSize, mCompressListener, mOnCompressCompletedListener, mExecutor);
        }
    }
}
