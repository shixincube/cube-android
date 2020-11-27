package cube.core.manager;

import android.app.Activity;
import android.app.Application;
import android.app.Service;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import cube.core.EngineAgent;
import cube.utils.log.LogUtil;

/**
 * 生命周期管理
 *
 * @author LiuFeng
 * @data 2019/1/23 14:58
 */
public class LifecycleManager {
    private static final String TAG = "LifecycleManager";

    private static LifecycleManager instance = new LifecycleManager();

    private boolean isRegister = false;

    private LifecycleManager() {
    }

    public static LifecycleManager getInstance() {
        return instance;
    }

    /**
     * 反注册生命周期管理
     */
    public synchronized void unregisterActivityLifecycle(@NonNull Context context) {
        if (isRegister) {
            if (context instanceof Activity) {
                ((Activity) context).getApplication().unregisterActivityLifecycleCallbacks(lifecycle);
            } else if (context instanceof Service) {
                ((Service) context).getApplication().unregisterActivityLifecycleCallbacks(lifecycle);
            } else {
                ((Application) context).unregisterActivityLifecycleCallbacks(lifecycle);
            }

            isRegister = false;
        }
    }

    /**
     * 注册生命周期管理
     */
    public synchronized void registerActivityLifecycle(@NonNull Context context) {
        // 保证只注册一次
        if (!isRegister) {
            if (context instanceof Activity) {
                ((Activity) context).getApplication().registerActivityLifecycleCallbacks(lifecycle);
            } else if (context instanceof Service) {
                ((Service) context).getApplication().registerActivityLifecycleCallbacks(lifecycle);
            } else {
                ((Application) context).registerActivityLifecycleCallbacks(lifecycle);
            }

            isRegister = true;
        }
    }

    private int life = 0;

    private Application.ActivityLifecycleCallbacks lifecycle = new Application.ActivityLifecycleCallbacks() {
        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        }

        @Override
        public void onActivityStarted(Activity activity) {
            life++;
            if (0 == life - 1) {
                LogUtil.i(TAG, "Wakeup");
                wakeup();
            }
        }

        @Override
        public void onActivityResumed(Activity activity) {
        }

        @Override
        public void onActivityPaused(Activity activity) {
        }

        @Override
        public void onActivityStopped(Activity activity) {
            // 当引擎在非Application中启动时，
            // 易导致此处life为0，引擎没执行wakeup
            if (life == 0) {
                LogUtil.w(TAG, "wakeup 引擎在非Application中启动！");
                wakeup();
                return;
            }

            life--;
            if (0 == life) {
                LogUtil.i(TAG, "sleep");
                sleep();
            }
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
        }
    };

    /**
     * 唤醒引擎
     */
    private void wakeup() {
        synchronized (this) {
            EngineAgent.getInstance().wakeup();
        }
    }

    /**
     * 睡眠引擎
     */
    private void sleep() {
        synchronized (this) {
            EngineAgent.getInstance().sleep();
        }
    }
}
