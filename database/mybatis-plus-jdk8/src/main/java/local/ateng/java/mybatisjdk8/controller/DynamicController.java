package local.ateng.java.mybatisjdk8.controller;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import local.ateng.java.mybatisjdk8.entity.Project;
import local.ateng.java.mybatisjdk8.service.DynamicService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/dynamic")
@RequiredArgsConstructor
public class DynamicController {
    private final DynamicService dynamicService;

    @GetMapping("/testSelect")
    public List<JSONObject> testSelect() {
        List<JSONObject> list = dynamicService.testSelect();
        return list;
    }

    @GetMapping("/list")
    public List<Project> list() {
        List<Project> list = dynamicService.list();
        return list;
    }

    @GetMapping("/listWrapper")
    public List<Project> listWrapper() {
        List<Project> list = dynamicService.listWrapper();
        return list;
    }

    @GetMapping("/listLambdaWrapper")
    public List<Project> listLambdaWrapper() {
        List<Project> list = dynamicService.listLambdaWrapper();
        return list;
    }

    @GetMapping("/listParam")
    public List<Project> listParam() {
        List<Project> list = dynamicService.listParam();
        return list;
    }

    @GetMapping("/listParamDynamic")
    public List<Project> listParamDynamic() {
        List<Project> list = dynamicService.listParamDynamic();
        return list;
    }

    @GetMapping("/listWithCte")
    public List<Project> listWithCte() {
        List<Project> list = dynamicService.listWithCte();
        return list;
    }

    @GetMapping("/listWithCteMany")
    public List<Project> listWithCteMany() {
        List<Project> list = dynamicService.listWithCteMany();
        return list;
    }

    @GetMapping("/listWithCteAndIf")
    public List<Project> listWithCteAndIf() {
        List<Project> list = dynamicService.listWithCteAndIf();
        return list;
    }

    @GetMapping("/page")
    public IPage<JSONObject> page() {
        IPage<JSONObject> page = dynamicService.page();
        return page;
    }

    @GetMapping("/page2")
    public IPage<JSONObject> page2() {
        IPage<JSONObject> page = dynamicService.page2();
        return page;
    }

    @GetMapping("/insertByMap")
    public void insertByMap() {
        dynamicService.insertByMap();
    }

    @GetMapping("/insertByEntity")
    public void insertByEntity() {
        dynamicService.insertByEntity();
    }

    @GetMapping("/batchInsertByMap")
    public void batchInsertByMap() {
        dynamicService.batchInsertByMap();
    }

    @GetMapping("/batchInsertByEntity")
    public void batchInsertByEntity() {
        dynamicService.batchInsertByEntity();
    }

    @GetMapping("/updateByMap")
    public void updateByMap() {
        dynamicService.updateByMap();
    }

    @GetMapping("/updateByEntity")
    public void updateByEntity() {
        dynamicService.updateByEntity();
    }

    @GetMapping("/batchUpdate")
    public void batchUpdate() {
        dynamicService.batchUpdate();
    }

    @GetMapping("/deleteByMap")
    public void deleteByMap() {
        dynamicService.deleteByMap();
    }

    @GetMapping("/deleteByEntity")
    public void deleteByEntity() {
        dynamicService.deleteByEntity();
    }

    @GetMapping("/batchDelete")
    public void batchDelete() {
        dynamicService.batchDelete();
    }


}
