package cube.fileprocessor.biscuit;

import java.util.ArrayList;

/**
 * 压缩结果。
 */
public class CompressResult {

    public ArrayList<String> mSuccessPaths;
    public ArrayList<String> mExceptionPaths;

    public CompressResult() {
        mSuccessPaths = new ArrayList<>();
        mExceptionPaths = new ArrayList<>();
    }
}
