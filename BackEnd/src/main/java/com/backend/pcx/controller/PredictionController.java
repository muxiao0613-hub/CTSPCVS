package com.backend.pcx.controller;

import com.backend.pcx.dto.PredictRequest;
import com.backend.pcx.dto.PredictionJobDTO;
import com.backend.pcx.dto.Result;
import com.backend.pcx.service.PredictionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/predict")
@Tag(name = "预测服务", description = "交通流量预测接口")
public class PredictionController {
    
    @Autowired
    private PredictionService predictionService;

    @PostMapping
    @Operation(summary = "执行预测")
    public Result<PredictionJobDTO> predict(@RequestBody PredictRequest request) {
        try {
            PredictionJobDTO result = predictionService.predict(request);
            return Result.success("预测完成", result);
        } catch (Exception e) {
            return Result.error("预测失败: " + e.getMessage());
        }
    }

    @GetMapping("/jobs")
    @Operation(summary = "查询预测任务列表")
    public Result<Page<PredictionJobDTO>> getPredictionJobs(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<PredictionJobDTO> jobs = predictionService.getPredictionJobs(page, size);
        return Result.success(jobs);
    }

    @GetMapping("/jobs/{jobId}")
    @Operation(summary = "查询预测任务详情")
    public Result<PredictionJobDTO> getPredictionJob(@PathVariable Long jobId) {
        PredictionJobDTO job = predictionService.getPredictionJob(jobId);
        if (job == null) {
            return Result.error("任务不存在");
        }
        return Result.success(job);
    }

    @GetMapping("/jobs/segment/{segmentId}")
    @Operation(summary = "查询路段的预测任务")
    public Result<Page<PredictionJobDTO>> getPredictionJobsBySegment(
            @PathVariable Long segmentId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<PredictionJobDTO> jobs = predictionService.getPredictionJobsBySegment(segmentId, page, size);
        return Result.success(jobs);
    }
}
