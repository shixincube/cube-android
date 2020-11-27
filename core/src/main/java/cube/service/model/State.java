package cube.service.model;

/**
 * 状态信息
 */
public class State {
    public int code;
    public String desc;

    public State() {
    }

    public State(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Override
    public String toString() {
        return "State{" +
                "code=" + code +
                ", desc='" + desc + '\'' +
                '}';
    }
}
