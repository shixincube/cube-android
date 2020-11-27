package cube.message.service.model;

/**
 * 分页信息
 */
public class PageInfo {
    public long   since;          //开始时间
    public long   until;          //结束时间
    public long   offset;         //开始页码
    public long   count;          //每页条数
    public long   total;          //总共条数
}
