package com.fxz.console.controller;

import com.fxz.console.pojo.beanexplorer.BeanInspectRequest;
import com.fxz.console.pojo.beanexplorer.BeanInspectResponse;
import com.fxz.console.pojo.beanexplorer.BeanTypeGroup;
import com.fxz.console.pojo.beanexplorer.ExecuteCodeRequest;
import com.fxz.console.pojo.beanexplorer.ExecuteCodeResponse;
import com.fxz.console.service.BeanExplorerService;
import com.fxz.console.service.DynamicJavaExecutor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bean-explorer/api")
public class BeanExplorerController {

    private final BeanExplorerService beanExplorerService;

    private final DynamicJavaExecutor dynamicJavaExecutor;

    public BeanExplorerController(BeanExplorerService beanExplorerService, DynamicJavaExecutor dynamicJavaExecutor) {
        this.beanExplorerService = beanExplorerService;
        this.dynamicJavaExecutor = dynamicJavaExecutor;
    }

    @GetMapping("/beans")
    public List<BeanTypeGroup> beans() {
        return beanExplorerService.listBeans();
    }

    @PostMapping(value = "/inspect", consumes = MediaType.APPLICATION_JSON_VALUE)
    public BeanInspectResponse inspect(@RequestBody BeanInspectRequest request) {
        return beanExplorerService.inspect(request);
    }

    @PostMapping(value = "/execute", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ExecuteCodeResponse execute(@RequestBody ExecuteCodeRequest request) {
        return dynamicJavaExecutor.execute(request == null ? null : request.getJavaCode());
    }

    @PostMapping(value = "/inspect-result", consumes = MediaType.APPLICATION_JSON_VALUE)
    public BeanInspectResponse inspectResult(@RequestBody BeanInspectRequest request) {
        return dynamicJavaExecutor.inspectResult(request == null ? null : request.getPath());
    }
}
