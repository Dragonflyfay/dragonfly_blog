package com.dragonfly.mapper;

import com.dragonfly.pojo.OperationLog;
import org.apache.ibatis.annotations.*;



import java.util.List;

/**
 * 描述：
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/8/20 15:31
 */
@Mapper
public interface OperationLogMapper {
    @Insert("INSERT INTO operation_log (operator, module, action, detail, ip, create_time) " +
            "VALUES (#{operator}, #{module}, #{action}, #{detail}, #{ip}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(OperationLog log);

    @Select("<script>" +
            "SELECT * FROM operation_log WHERE 1=1 " +
            "<if test='operator != null and operator != \"\"'> AND operator LIKE CONCAT('%', #{operator}, '%') </if>" +
            "<if test='module != null and module != \"\"'> AND module = #{module} </if>" +
            "<if test='startDate != null and startDate != \"\"'> AND DATE(create_time) >= #{startDate} </if>" +
            "<if test='endDate != null and endDate != \"\"'> AND DATE(create_time) <= #{endDate} </if>" +
            "ORDER BY create_time DESC " +
            "LIMIT #{offset}, #{pageSize}" +
            "</script>")
    List<OperationLog> list(@Param("operator") String operator,
                            @Param("module") String module,
                            @Param("startDate") String startDate,
                            @Param("endDate") String endDate,
                            @Param("offset") Integer offset,
                            @Param("pageSize") Integer pageSize);

    @Select("<script>" +
            "SELECT COUNT(*) FROM operation_log WHERE 1=1 " +
            "<if test='operator != null and operator != \"\"'> AND operator LIKE CONCAT('%', #{operator}, '%') </if>" +
            "<if test='module != null and module != \"\"'> AND module = #{module} </if>" +
            "<if test='startDate != null and startDate != \"\"'> AND DATE(create_time) >= #{startDate} </if>" +
            "<if test='endDate != null and endDate != \"\"'> AND DATE(create_time) <= #{endDate} </if>" +
            "</script>")
    int count(@Param("operator") String operator,
              @Param("module") String module,
              @Param("startDate") String startDate,
              @Param("endDate") String endDate);
}
