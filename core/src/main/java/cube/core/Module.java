package cube.core;


import java.util.List;

import cube.pipeline.Pipeline;

/**
 * 公共服务接口
 *
 * @author LiuFeng
 * @data 2020/8/27 11:09
 */
public interface Module {

    /**
     * 获取模块名称
     *
     * @return
     */
    String getName();

    /**
     * 获取该模块依赖的其他模块。
     */
    List<String> getRequires();

    /**
     * 获取外部依赖库文件。
     */
    List<String> getRequireFiles();

    /**
     * 绑定管道
     *
     * @param pipeline
     */
    void bindPipeline(Pipeline pipeline);

    /**
     * 启动
     */
    void start();

    /**
     * 停止
     */
    void stop();
}
