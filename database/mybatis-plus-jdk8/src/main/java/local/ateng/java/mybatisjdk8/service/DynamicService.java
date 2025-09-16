package local.ateng.java.mybatisjdk8.service;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import local.ateng.java.mybatisjdk8.config.DynamicSqlExecutor;
import local.ateng.java.mybatisjdk8.entity.Project;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DynamicService {
    private final DynamicSqlExecutor dynamicSqlExecutor;

    public List<JSONObject> testSelect() {
        String sql = "SELECT 1 AS id";
        List<JSONObject> result = dynamicSqlExecutor.select(sql, JSONObject.class, null);
        return result;
    }

    public List<Project> list() {
        String sql = "select * from project_mini limit 10";
        List<Project> result = dynamicSqlExecutor.select(sql, Project.class, null);
        return result;
    }

    public List<Project> listWrapper() {
        String sql = "select * from project_mini";
        QueryWrapper<Project> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1);
        List<Project> result = dynamicSqlExecutor.select(sql, Project.class, wrapper);
        return result;
    }

    public List<Project> listLambdaWrapper() {
        String sql = "select * from project_mini";
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Project::getStatus, 1);
        List<Project> result = dynamicSqlExecutor.select(sql, Project.class, wrapper);
        return result;
    }

    public List<Project> listParam() {
        String sql = "select * from project_mini where status = #{status} limit 10";
        Map<String, Object> param = new HashMap<>();
        param.put("status", 1);
        List<Project> result = dynamicSqlExecutor.select(sql, Project.class, param);
        return result;
    }

    public List<Project> listParamDynamic() {
        // 支持 <if> 动态拼接
        String sql = ""
                + "SELECT * FROM project_mini "
                + "WHERE 1=1 "
                + "  <if test='status != null'>AND status = #{status}</if> "
                + "  <if test='name != null'>AND name LIKE CONCAT('%', #{name}, '%')</if> "
                + "LIMIT 10";

        Map<String, Object> param = new HashMap<>();
        param.put("status", 1);
        // param.put("name", "测试"); // 不传 name 时自动忽略

        return dynamicSqlExecutor.select(sql, Project.class, param);
    }

    public List<Project> listWithCte() {
        // 使用 CTE 定义临时表 cte_projects
        String sql = "-- CTE复杂SQL\n" +
                "-- aaaa\n" +
                "/*1111*/\n" +
                "/* 1111 */\n"
                + "WITH cte_projects AS (\n" +
                "    SELECT \n" +
                "        *\n" +
                "  \t\t  FROM project_mini\n" +
                "    WHERE status = #{status} -- 查询\n" +
                ")\n" +
                "SELECT *\n" +
                "FROM cte_projects\n" +
                "WHERE id < 100\n" +
                "  and 1 > 0 -- 查询 \n" +
                "LIMIT 10";

        // 命名参数绑定
        Map<String, Object> param = new HashMap<>();
        param.put("status", 1);

        // 调用 select 执行 CTE SQL
        List<Project> result = dynamicSqlExecutor.select(sql, Project.class, param);
        return result;
    }

    public List<Project> listWithCteMany() {
        // 使用 CTE 定义临时表 cte_projects
        String sql = "-- CTE复杂SQL\n" +
                "-- aaaa\n" +
                "/*1111*/\n" +
                "/* 1111 */\n"
                + "WITH cte_projects AS (\n" +
                "    SELECT\n" +
                "        *\n" +
                "    FROM project_mini\n" +
                "    WHERE status = #{status}\n" +
                "), result as (\n" +
                "    select *,1 as id2 from cte_projects\n" +
                ")\n" +
                "SELECT *\n" +
                "FROM result\n" +
                "WHERE id < 100\n" +
                "  and 1 > 0\n" +
                "LIMIT 10";

        // 命名参数绑定
        Map<String, Object> param = new HashMap<>();
        param.put("status", 1);

        // 调用 select 执行 CTE SQL
        List<Project> result = dynamicSqlExecutor.select(sql, Project.class, param);
        return result;
    }

    public List<Project> listWithCteAndIf() {
        String sql = ""
                + "WITH cte_projects AS ( "
                + "    SELECT * FROM project_mini "
                + "    WHERE status = #{status} "
                + ") "
                + "SELECT * FROM cte_projects "
                + "WHERE id<100 "
                + "<if test='status != null'>AND status = #{status}</if> "
                + "LIMIT 10";

        Map<String, Object> param = new HashMap<>();
        param.put("status", 1);

        return dynamicSqlExecutor.select(sql, Project.class, param);
    }

    public IPage<JSONObject> page() {
        String sql = "WITH cte_projects AS ( " +
                "SELECT * FROM project_mini " +
                ") SELECT * FROM cte_projects";

        QueryWrapper<JSONObject> wrapper = new QueryWrapper<>();
        wrapper.lt("id", 100);  // 自动拼接到 SQL 末尾

        IPage<JSONObject> page = new Page<>(1, 2);
        IPage<JSONObject> resultPage = dynamicSqlExecutor.selectPage(sql, JSONObject.class, page, wrapper);

        System.out.println("分页查询结果: " + resultPage.getRecords().size());
        return resultPage;
    }

    public IPage<JSONObject> page2() {
        String sql = "WITH cte_projects AS ( " +
                "SELECT * FROM project_mini WHERE status = #{query._status} " +
                ") SELECT * FROM cte_projects";

        Map<String, Object> query = new HashMap<>();
        query.put("_status", 1);

        QueryWrapper<JSONObject> wrapper = new QueryWrapper<>();
        wrapper.lt("id", 100);  // 自动拼接到 SQL 末尾
        wrapper.orderByDesc("name");

        IPage<JSONObject> page = new Page<>(7, 2);
        IPage<JSONObject> resultPage = dynamicSqlExecutor.selectPage(sql, JSONObject.class, page, wrapper, query);

        System.out.println("分页查询结果: " + resultPage.getRecords().size());
        return resultPage;
    }

    public void insertByMap() {
        String sql = "INSERT INTO project_mini (name) VALUES (#{name})";
        Map<String, Object> param = new HashMap<>();
        param.put("name", "测试项目-Map方式");

        int rows = dynamicSqlExecutor.insert(sql, param);
        System.out.println("受影响行数: " + rows);
    }

    public void insertByEntity() {
        String sql = "INSERT INTO project_mini (name) VALUES (#{name})";

        Project project = new Project();
        project.setName("测试项目-实体方式");

        int rows = dynamicSqlExecutor.insert(sql, project);
        System.out.println("受影响行数: " + rows);
        System.out.println(project);
    }

    public void batchInsertByMap() {
        String sql = "INSERT INTO project_mini (name) VALUES (#{name})";

        List<Map<String, Object>> params = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            Map<String, Object> param = new HashMap<>();
            param.put("name", "批量Map项目-" + i);
            params.add(param);
        }

        int rows = dynamicSqlExecutor.insertBatch(sql, params);
        System.out.println("批量插入行数: " + rows);
    }

    public void batchInsertByEntity() {
        String sql = "INSERT INTO project_mini (name) VALUES (#{name})";

        List<Project> projects = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            Project project = new Project();
            project.setName("批量实体项目-" + i);
            projects.add(project);
        }

        int rows = dynamicSqlExecutor.insertBatch(sql, projects);
        System.out.println("批量插入行数: " + rows);
        System.out.println(projects);
    }


    public void updateByMap() {
        String sql = "UPDATE project_mini SET name = #{name} WHERE id = #{id}";
        Map<String, Object> param = new HashMap<>();
        param.put("id", 146);
        param.put("name", "更新后的项目名称");

        int rows = dynamicSqlExecutor.update(sql, param);
        System.out.println("更新影响行数: " + rows);
    }

    public void updateByEntity() {
        String sql = "UPDATE project_mini SET name = #{name} WHERE id = #{id}";

        Project project = new Project();
        project.setId(145);
        project.setName("更新后的实体项目");

        int rows = dynamicSqlExecutor.update(sql, project);
        System.out.println("更新影响行数: " + rows);
    }

    public void batchUpdate() {
        String sql = "UPDATE project_mini SET name = #{name} WHERE id = #{id}";

        List<Project> projects = new ArrayList<>();
        for (int i = 3; i <= 5; i++) {
            Project project = new Project();
            project.setId(i);
            project.setName("批量更新项目-" + i);
            projects.add(project);
        }

        int rows = dynamicSqlExecutor.updateBatch(sql, projects);
        System.out.println("批量更新影响行数: " + rows);
    }

    public void deleteByMap() {
        String sql = "DELETE FROM project_mini WHERE id = #{id}";
        Map<String, Object> param = new HashMap<>();
        param.put("id", 145);

        int rows = dynamicSqlExecutor.delete(sql, param);
        System.out.println("删除影响行数: " + rows);
    }

    public void deleteByEntity() {
        String sql = "DELETE FROM project_mini WHERE id = #{id}";

        Project project = new Project();
        project.setId(145);

        int rows = dynamicSqlExecutor.delete(sql, project);
        System.out.println("删除影响行数: " + rows);
    }

    public void batchDelete() {
        String sql = "DELETE FROM project_mini WHERE id = #{id}";

        List<Project> projects = new ArrayList<>();
        for (int i = 12; i <= 15; i++) {
            Project project = new Project();
            project.setId(i);
            projects.add(project);
        }

        int rows = dynamicSqlExecutor.deleteBatch(sql, projects);
        System.out.println("批量删除影响行数: " + rows);
    }


}

