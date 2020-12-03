package cube.engine;

import android.content.Context;
import android.location.Location;

import androidx.annotation.NonNull;
import cube.core.EngineAgent;
import cube.core.KernelConfig;
import cube.service.CubeEngineListener;
import cube.service.CubeState;
import cube.service.model.CubeConfig;
import cube.service.model.CubeSession;
import cube.service.model.DeviceInfo;

/**
 * 引擎实现类
 *
 * @author LiuFeng
 * @data 2020/8/28 14:40
 */
public final class EngineRoot extends CubeEngine {

    private EngineAgent agent = EngineAgent.getInstance();

    @Override
    public void addListener(CubeEngineListener listener) {
        agent.addListener(listener);
    }

    @Override
    public void removeListener(CubeEngineListener listener) {
        agent.removeListener(listener);
    }

    @Override
    public boolean startup(@NonNull Context context, KernelConfig config) {
        return agent.startup(context, config);
    }

    @Override
    public void shutdown() {
        agent.shutdown();
    }

    @Override
    public boolean isStarted() {
        return agent.isStarted();
    }

    @Override
    public void setCubeConfig(@NonNull CubeConfig config) {
        agent.setCubeConfig(config);
    }

    @Override
    public CubeConfig getCubeConfig() {
        return agent.getCubeConfig();
    }

    @Override
    public CubeSession getSession() {
        return agent.getSession();
    }

    @Override
    public CubeState getCubeEngineState() {
        return agent.getCubeEngineState();
    }

    @Override
    public DeviceInfo getDeviceInfo() {
        return agent.getDeviceInfo();
    }

    @Override
    public Context getContext() {
        return agent.getContext();
    }

    @Override
    public Location getLocation() {
        return agent.getLocation();
    }

    @Override
    public <T> T getService(@NonNull Class<T> cls) {
        return agent.getService(cls);
    }
}
