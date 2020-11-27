package cube.whiteboard.impl;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cube.common.callback.CubeCallback1;
import cube.common.callback.SimpleCallback1;
import cube.utils.GsonUtil;
import cube.utils.ThreadUtil;
import cube.utils.UIHandler;
import cube.utils.Utils;
import cube.utils.ZipUtils;
import cube.utils.log.LogUtil;
import cube.whiteboard.api.WBApiFactory;
import cube.whiteboard.api.data.FileConvert;
import cube.whiteboard.api.data.FileConvertData;
import cube.whiteboard.api.data.FilePage;
import cube.whiteboard.api.data.FilePageData;
import cube.whiteboard.service.BrushType;
import cube.whiteboard.service.WhiteboardView;

/**
 * 白板view实体类
 *
 * @author LiuFeng
 * @data 2020/8/17 10:36
 */
@SuppressLint("ViewConstructor")
public class WhiteboardViewEntity extends WhiteboardView {
    private static final String TAG = "WhiteboardViewEntity";

    private Context mContext;
    private boolean isFirstLoad;
    private String whiteboardId;
    private WBJsListener listener;

    private int currentPage;
    private WhiteboardFile currentFile;
    private Map<String, WhiteboardFile> fileMap;

    /**
     * 构造方法
     *
     * @param context
     */
    public WhiteboardViewEntity(Context context) {
        super(context);
        this.mContext = context;
        this.isFirstLoad = true;
        this.fileMap = new ConcurrentHashMap<>();

        init();
    }

    /**
     * 初始化
     */
    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    private void init() {
        WebSettings webSettings = this.getSettings();
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setAllowFileAccess(true);// 设置允许访问文件数据
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setPluginState(WebSettings.PluginState.ON);

        webSettings.setAppCacheEnabled(false);
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);

        setWebViewClient(new WBWebViewClient());
        setWebChromeClient(new WebChromeClient());
        setVerticalScrollBarEnabled(false);
        setHorizontalScrollBarEnabled(false);

        // 开启调试
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        // 白板js桥接
        addJavascriptInterface(new WhiteboardBridge(), "WhiteboardBridge");
    }

    /**
     * 设置白板js监听
     *
     * @param listener
     */
    public void setListener(WBJsListener listener) {
        this.listener = listener;
    }

    /**
     * 设置白板Id
     *
     * @param whiteboardId
     */
    public void setWhiteboardId(String whiteboardId) {
        this.whiteboardId = whiteboardId;
    }

    /**
     * 加载白板
     */
    public void loadWhiteboard() {
        isFirstLoad = false;
        String url = "file:///android_asset/whiteboard/index.html";
        loadUrl(url);
    }

    /**
     * 白板的WebViewClient
     */
    private static class WBWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            LogUtil.i(TAG, "onPageFinished --> url:" + url);
        }
    }

    /**
     * 初始化白板配置
     */
    private void initWBConfig() {
        /*bridge.addJavaMethod("configWhiteboard", data -> {
            LogUtil.i(TAG, "configWhiteboard --> data:" + data);
            Bundle params = new Bundle();
            params.putString("userName", SpUtil.getCubeId());
            params.putString("whiteboardId", whiteboardId);
            params.putString("backgroundColor", "#ffffff");
            params.putString("deviceName", "Android");
            bridge.require("configWhiteboardCallBack", params, null);
            return params;
        });*/
    }

    /**
     * 获取版本版本
     */
    private void requireWBVersion() {
        /*bridge.require("version", null, (response, cmd, params) -> {
            try {
                if (response.has("version")) {
                    String version = response.getString("version");
                    int v = Integer.parseInt(version.replace(".", ""));
                    SpUtil.setVersionWB(v);
                    LogUtil.i(TAG, "version:" + version + "=v:" + v);
                }
            } catch (JSONException e) {
                LogUtil.e(TAG, e);
            }
        });*/
    }

    /**
     * 初始化资源
     *
     * @param wbPath
     */
    private void initResource(String wbPath) {
        if (new File(wbPath).exists()) {
            loadResource(wbPath);
            return;
        }

        ThreadUtil.request(() -> {
            try {
                String fileName = "whiteboard.zip";
                String cacheDir = mContext.getCacheDir().getAbsolutePath();
                File file = new File(cacheDir + File.separator + fileName);

                if (file.exists()) {
                    file.delete();
                }
                file.createNewFile();

                copyAssetsZipFile(mContext, file);
                ZipUtils.unZipFile(file, cacheDir);
                file.delete();

                File whiteboardFile = new File(wbPath);
                if (whiteboardFile.exists()) {
                    UIHandler.run(() -> loadResource(wbPath));
                } else {
                    String msg = "cube-whiteboard-v*.ast not exist! Or init error.";
                    alert(msg);
                    LogUtil.d(TAG, msg);
                }
            } catch (Exception e) {
                LogUtil.e(TAG, e);
            }
        });
    }

    /**
     * 复制资源文件
     *
     * @param context
     * @param outFile
     * @throws IOException
     */
    private void copyAssetsZipFile(Context context, File outFile) throws IOException {
        String secretKey = "CUBE";
        String wbLibKey = "whiteboard";
        byte[] buffer = new byte[2048];
        String fileName = Utils.findAssetsFile(context, wbLibKey);
        InputStream is = context.getAssets().open(fileName);
        OutputStream os = new FileOutputStream(outFile);
        is.read(new byte[secretKey.getBytes().length], 0, secretKey.getBytes().length);
        int length;
        while ((length = is.read(buffer)) > 0) {
            os.write(buffer, 0, length);
        }
        os.flush();
        os.close();
        is.close();
    }

    /**
     * 加载白板
     *
     * @param wbPath
     */
    private void loadResource(String wbPath) {
        if (new File(wbPath).exists()) {
            loadUrl("file://" + wbPath);
        }
    }

    /**
     * 监听处理view被添加到父容器可见时，再加载白板
     *
     * @param changedView
     * @param visibility
     */
    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);

        boolean isVisible = View.VISIBLE == visibility;
        LogUtil.d(TAG, "onVisibilityChanged --> isVisible:" + isVisible);

        // 在view可见，且第一次加载时，加载白板
        if (isVisible && isFirstLoad) {
            loadWhiteboard();
        }
    }

    /**
     * 选择画笔类型
     *
     * @param type
     */
    @Override
    public void select(BrushType type) {
        callJs(String.format("selectWhiteboardTool(%s)", type.type));
    }

    /**
     * 取消选中画笔
     */
    @Override
    public void unSelect() {
        callJs("deselectWhiteboardTool()");
    }

    /**
     * 设置画笔粗细
     *
     * @param weight
     */
    @Override
    public void setWeight(int weight) {
        setToolAttribute(weight, null);
    }

    /**
     * 设置画笔颜色
     *
     * @param color
     */
    @Override
    public void setColor(String color) {
        setToolAttribute(null, color);
    }

    /**
     * 设置画笔工具属性
     *
     * @param weight
     * @param color
     */
    private void setToolAttribute(Integer weight, String color) {
        JSONObject attr = new JSONObject();
        try {
            if (weight != null) {
                attr.put("size", weight);
            }
            if (color != null) {
                attr.put("color", color);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        callJs(String.format("setWhiteboardToolAttribute(%s)", attr.toString()));
    }

    /**
     * 撤销
     */
    @Override
    public void undo() {
        executeCmd(ToolType.UNDO.type);
    }

    /**
     * 清空
     */
    @Override
    public void cleanup() {
        executeCmd(ToolType.CLEAR.type);
    }

    /**
     * 分享文件
     *
     * @param fileId 上传文件ID
     */
    @Override
    public void shareFile(String fileId) {
        // 查缓存数据
        if (fileMap.containsKey(fileId)) {
            currentPage = 1;
            currentFile = fileMap.get(fileId);
            gotoPage(currentPage);
            return;
        }

        // 查网络数据
        queryFilePageSize(fileId, new SimpleCallback1<FilePage>() {
            @Override
            public void onSuccess(FilePage filePage) {
                WhiteboardFile file = new WhiteboardFile();
                file.roomId = filePage.roomId;
                file.fid = filePage.fid;
                file.pageTotal = filePage.total;
                currentPage = 1;
                currentFile = file;
                fileMap.put(fileId, file);

                gotoPage(currentPage);
            }
        });
    }

    /**
     * 查询文件页数
     *
     * @param fileId
     * @param callback
     */
    private void queryFilePageSize(String fileId, CubeCallback1<FilePage> callback) {
        WBApiFactory.getInstance().queryFilePageSize(whiteboardId, fileId, "1", new CubeCallback1<FilePageData>() {
            @Override
            public void onSuccess(FilePageData result) {
                FilePage filePage = result.filePage;

                if (callback != null) {
                    callback.onSuccess(filePage);
                }
            }

            @Override
            public void onError(int code, String desc) {
                if (callback != null) {
                    callback.onError(code, desc);
                }
            }
        });
    }

    /**
     * 查询文件转换信息
     *
     * @param file
     * @param page
     * @param callback
     */
    private void queryConvertInfo(WhiteboardFile file, int page, CubeCallback1<FileConvert> callback) {
        WBApiFactory.getInstance().queryFileConvertImageInfo(whiteboardId, String.valueOf(file.fid), page, "1", new CubeCallback1<FileConvertData>() {
            @Override
            public void onSuccess(FileConvertData result) {
                FileConvert convert = result.convert;
                file.addFileConvert(page, convert);

                LogUtil.d(TAG, "queryConvertInfo --> url:" + convert.originalUrl);
                if (callback != null) {
                    callback.onSuccess(convert);
                }
            }

            @Override
            public void onError(int code, String desc) {
                if (callback != null) {
                    callback.onError(code, desc);
                }
            }
        });
    }

    /**
     * 上一页
     */
    @Override
    public void prevPage() {
        if (currentFile == null || currentPage <= 1) {
            return;
        }

        int prevPage = currentPage - 1;
        FileConvert convert = currentFile.getFileConvert(prevPage);
        if (convert != null) {
            currentPage = prevPage;
            String url = convert.originalUrl;
            setWhiteboardImage(url);
            return;
        }

        queryConvertInfo(currentFile, prevPage, new SimpleCallback1<FileConvert>() {
            @Override
            public void onSuccess(FileConvert convert) {
                currentPage = prevPage;
                String url = convert.originalUrl;
                setWhiteboardImage(url);
            }
        });
    }

    /**
     * 下一页
     */
    @Override
    public void nextPage() {
        // 边界条件判断
        if (currentFile == null || currentPage >= currentFile.pageTotal) {
            return;
        }

        // 查缓存数据
        int nextPage = currentPage + 1;
        FileConvert convert = currentFile.getFileConvert(nextPage);
        if (convert != null) {
            currentPage = nextPage;
            String url = convert.originalUrl;
            setWhiteboardImage(url);
            return;
        }

        // 查网络数据
        queryConvertInfo(currentFile, nextPage, new SimpleCallback1<FileConvert>() {
            @Override
            public void onSuccess(FileConvert convert) {
                currentPage = nextPage;
                String url = convert.originalUrl;
                setWhiteboardImage(url);
            }
        });
    }

    /**
     * 跳转页面
     *
     * @param page 某页
     */
    @Override
    public void gotoPage(int page) {
        // 边界条件判断
        if (currentFile == null || page < 1 || page > currentFile.pageTotal) {
            return;
        }

        // 查缓存数据
        FileConvert convert = currentFile.getFileConvert(page);
        if (convert != null) {
            currentPage = page;
            String url = convert.originalUrl;
            setWhiteboardImage(url);
            return;
        }

        // 查网络数据
        queryConvertInfo(currentFile, page, new SimpleCallback1<FileConvert>() {
            @Override
            public void onSuccess(FileConvert convert) {
                currentPage = page;
                String url = convert.originalUrl;
                setWhiteboardImage(url);
            }
        });
    }

    /**
     * 设置白板图片
     *
     * @param url
     */
    private void setWhiteboardImage(String url) {
        callJs(String.format("setWhiteboardImage('%s')", url));
    }

    /**
     * 擦除
     */
    public void erase() {
//        callJs("erase()");
    }

    /**
     * 消息提示
     *
     * @param msg
     */
    public void alert(String msg) {
        callJs(String.format("javascript:alert(%s)", msg));
    }

    /**
     * 调用js执行命令
     *
     * @param cmd
     */
    public void executeCmd(int cmd) {
        callJs(String.format("executeWhiteboardCmd(%s)", cmd));
    }

    /**
     * js加载命令数据
     *
     * @param command
     */
    public void loadCommand(JSONArray command) {
        callJs(String.format("loadWhiteboardData(%s)", command.toString()));
    }

    /**
     * 设置白板背景色
     *
     * @param color
     */
    public void setBackgroundColor(String color) {
        /*Bundle params = new Bundle();
        params.putString("backgroundColor", color);
        bridge.require("setBackgroundColor", params, null);*/
    }

    /**
     * 创建白板
     *
     * @param boardName
     */
    public void createWhiteboard(String boardName) {
        callJs(String.format("createWhiteboard(%s)", boardName));
    }

    /**
     * 切换白板
     *
     * @param boardName
     */
    public void switchWhiteboard(String boardName) {
        callJs(String.format("switchWhiteboard(%s)", boardName));
    }

    /**
     * 切换白板
     *
     * @param boardName
     */
    public void deleteWhiteboard(String boardName) {
        callJs(String.format("deleteWhiteboard(%s)", boardName));
    }

    /**
     * 调用js函数
     *
     * @param function
     */
    private void callJs(String function) {
        UIHandler.run(() -> {
            LogUtil.d(TAG, "callJs --> function:" + function);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                evaluateJavascript(function, null);
            } else {
                loadUrl(function);
            }
        });
    }

    /**
     * 重写父类destroy，清理资源，但不销毁view，方便复用
     */
    @Override
    public void destroy() {
        UIHandler.run(() -> {
            loadDataWithBaseURL(null, "", "text/html", "utf-8", null);
            clearHistory();

            // 移除view父容器
            ViewGroup parent = (ViewGroup) getParent();
            if (parent != null) {
                parent.removeView(WhiteboardViewEntity.this);
            }

            isFirstLoad = true;
        });
    }

    /**
     * 白板桥接类
     *
     * @author LiuFeng
     * @data 2020/8/20 21:26
     */
    private class WhiteboardBridge {

        /**
         * 白板准备就绪
         */
        @JavascriptInterface
        public void onReady() {
            LogUtil.i(TAG, "白板已就绪");
            if (listener != null) {
                UIHandler.run(() -> listener.onReady(WhiteboardViewEntity.this));
            }
        }

        /**
         * 白板数据变化
         *
         * @param data
         */
        @JavascriptInterface
        public void onChange(String name, String data) {
            LogUtil.d(TAG, "onChange --> name:" + name + " data:\n" + LogUtil.getFormatJson(data));
            if (listener != null) {
                UIHandler.run(() -> listener.onDataChange(whiteboardId, GsonUtil.toJSONObject(data)));
            }
        }
    }
}
