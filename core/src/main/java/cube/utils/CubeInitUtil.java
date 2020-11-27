package cube.utils;

import cube.common.CubeConstants;
import java.io.File;

/**
 * 引擎初始化相关工具
 *
 * @author LiuFeng
 * @date 2018-5-26
 */
public class CubeInitUtil {

    /**
     * 初始化创建文件目录
     *
     * @param fileDir
     */
    public static void initFileDir(File fileDir) {
        File image = new File(fileDir, CubeConstants.Sp.PATH_IMAGE_DEF);
        File voice = new File(fileDir, CubeConstants.Sp.PATH_VOICE_DEF);
        File video = new File(fileDir, CubeConstants.Sp.PATH_VIDEO_DEF);
        File file = new File(fileDir, CubeConstants.Sp.PATH_FILE_DEF);
        File yun = new File(fileDir, CubeConstants.Sp.PATH_FILE_YUN_DEF);
        File wb = new File(fileDir, CubeConstants.Sp.PATH_FILE_WB_DEF);
        File tmp = new File(fileDir, CubeConstants.Sp.PATH_TMP_DEF);
        File thumb = new File(fileDir, CubeConstants.Sp.PATH_THUMB_DEF);
        File log = new File(fileDir, CubeConstants.Sp.PATH_LOG_DEF);

        FileUtil.createOrExistsDir(image);
        FileUtil.createOrExistsDir(voice);
        FileUtil.createOrExistsDir(video);
        FileUtil.createOrExistsDir(file);
        FileUtil.createOrExistsDir(yun);
        FileUtil.createOrExistsDir(wb);
        FileUtil.createOrExistsDir(tmp);
        FileUtil.createOrExistsDir(thumb);
        FileUtil.createOrExistsDir(log);
    }
}
