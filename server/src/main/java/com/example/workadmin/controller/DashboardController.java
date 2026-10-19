package com.example.workadmin.controller;

import com.example.workadmin.common.Result;
import com.example.workadmin.dto.DashboardStats;
import com.example.workadmin.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 仪表盘真实统计接口。
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/stats")
    public Result<DashboardStats> stats() {
        return Result.ok(dashboardService.stats());
    }
}
