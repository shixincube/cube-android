package cube.core;

import android.content.Context;
import android.content.SharedPreferences;

import junit.framework.TestCase;

import org.mockito.Mockito;
import static org.mockito.Mockito.*;
import org.mockito.MockitoAnnotations;

import cube.service.CubeEngine;
import cube.service.model.CubeConfig;

/**
 * @author: LiuFeng
 * @data: 2020/9/10
 */

public class EngineKernelTest extends TestCase {
    EngineKernel kernel;
    Context context;

    @Override
    protected void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        context = Mockito.mock(Context.class);
        when(context.getApplicationContext()).thenReturn(context);

        SharedPreferences sharedPrefs = Mockito.mock(SharedPreferences.class);
        SharedPreferences.Editor editor = mock(SharedPreferences.Editor.class);
        when(context.getSharedPreferences(anyString(), anyInt())).thenReturn(sharedPrefs);
        when(sharedPrefs.edit()).thenReturn(editor);

        when(sharedPrefs.getInt(anyString(),anyInt())).thenReturn(1);
        when(sharedPrefs.getBoolean(anyString(), anyBoolean())).thenReturn(false);
        when(sharedPrefs.getString(anyString(), anyString())).thenReturn("anyString()");

        when(editor.putInt(anyString(),anyInt())).thenReturn(editor);
        when(editor.putBoolean(anyString(),anyBoolean())).thenReturn(editor);
        when(editor.putString(anyString(),anyString())).thenReturn(editor);

        kernel = (EngineKernel) EngineAgent.getInstance();
        CubeConfig config = new CubeConfig();
        config.setDebug(false);
        CubeEngine.getInstance().setCubeConfig(config);
//        kernel.startup(context);
    }

    @Override
    protected void tearDown() throws Exception {
        kernel.shutdown();
    }

    public void testStartup() {
        assertFalse(kernel.startup(null));
        assertTrue(kernel.startup(context));
    }

    public void testGetService() {
        assertNull(kernel.getService(null));
        assertNull(kernel.getService(String.class));
        assertNull(kernel.getService(Module.class));
    }
}
