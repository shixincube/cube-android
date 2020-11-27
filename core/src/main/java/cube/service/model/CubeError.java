package cube.service.model;

/**
 * 引擎错误
 *
 * @author workerinchina@163.com
 */
public class CubeError {
    public CubeError(int code, String desc) {
        super();
        this.code = code;
        this.desc = desc;
    }

    public int    code;
    public String desc;

    @Override
    public String toString() {
        return "CubeError{" + "code=" + code + ", desc='" + desc + '\'' + '}';
    }
}
