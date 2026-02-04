package com.backend.pcx.controller;

import com.backend.pcx.dto.RoadSegmentDTO;
import com.backend.pcx.dto.Result;
import com.backend.pcx.service.RoadSegmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/segments")
@Tag(name = "路段管理", description = "路段信息管理接口")
public class RoadSegmentController {
    
    @Autowired
    private RoadSegmentService roadSegmentService;

    @GetMapping
    @Operation(summary = "查询路段列表")
    public Result<Page<RoadSegmentDTO>> searchSegments(
            @Parameter(description = "关键字") @RequestParam(required = false) String keyword,
            @Parameter(description = "区域") @RequestParam(required = false) String region,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size) {
        Page<RoadSegmentDTO> segments = roadSegmentService.searchSegments(keyword, region, page, size);
        return Result.success(segments);
    }
    
    @GetMapping("/from-sources")
    @Operation(summary = "从数据源获取路段列表")
    public Result<List<RoadSegmentDTO>> getSegmentsFromSources() {
        List<RoadSegmentDTO> segments = roadSegmentService.getSegmentsFromDataSources();
        return Result.success(segments);
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询路段详情")
    public Result<RoadSegmentDTO> getSegmentById(@PathVariable Long id) {
        RoadSegmentDTO segment = roadSegmentService.getSegmentById(id);
        if (segment == null) {
            return Result.error("路段不存在");
        }
        return Result.success(segment);
    }
}
