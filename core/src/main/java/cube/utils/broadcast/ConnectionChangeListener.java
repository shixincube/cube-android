package cube.utils.broadcast;

/**
 * @author fldy
 */
public interface ConnectionChangeListener {

    void onConnectionChange(boolean isNetworkAvailable);

    void onTimeTick(int interval5min);
}
