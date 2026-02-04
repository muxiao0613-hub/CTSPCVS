package com.backend.pcx.controller;

import com.backend.pcx.dto.DashboardSummary;
import com.backend.pcx.dto.Result;
import com.backend.pcx.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "仪表盘", description = "仪表盘数据统计接口")
public class DashboardController {
    
    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/summary")
    @Operation(summary = "获取仪表盘汇总数据")
    public Result<DashboardSummary> getSummary() {
        DashboardSummary summary = dashboardService.getSummary();
        return Result.success(summary);
    }
}
