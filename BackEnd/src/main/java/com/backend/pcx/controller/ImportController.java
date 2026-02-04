package com.backend.pcx.controller;

import com.backend.pcx.dto.ImportJobDTO;
import com.backend.pcx.dto.Result;
import com.backend.pcx.repository.FileBasedSpeedRepository;
import com.backend.pcx.service.ImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/import")
@Tag(name = "数据导入", description = "CSV数据导入相关接口")
public class ImportController {
    
    @Autowired
    private ImportService importService;

    @PostMapping("/speed-csv")
    @Operation(summary = "上传CSV文件导入速度数据")
    public Result<Long> importSpeedCsv(@RequestParam("file") MultipartFile file) {
        try {
            Long jobId = importService.importSpeedCsv(file);
            if (jobId == null) {
                return Result.error("导入失败");
            }
            return Result.success("导入任务已创建", jobId);
        } catch (Exception e) {
            return Result.error("导入失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/speed-csv-batch")
    @Operation(summary = "批量上传CSV文件导入速度数据")
    public Result<List<Long>> importSpeedCsvBatch(@RequestParam("files") MultipartFile[] files) {
        try {
            List<Long> jobIds = importService.importSpeedCsv(files);
            if (jobIds.isEmpty()) {
                return Result.error("导入失败");
            }
            return Result.success("导入任务已创建", jobIds);
        } catch (Exception e) {
            return Result.error("导入失败: " + e.getMessage());
        }
    }

    @GetMapping("/{jobId}")
    @Operation(summary = "查询导入任务状态")
    public Result<ImportJobDTO> getImportJob(@PathVariable Long jobId) {
        ImportJobDTO job = importService.getImportJob(jobId);
        if (job == null) {
            return Result.error("任务不存在");
        }
        return Result.success(job);
    }
    
    @GetMapping("/data-sources")
    @Operation(summary = "获取已注册的数据源列表")
    public Result<List<FileBasedSpeedRepository.DataSource>> getDataSources() {
        List<FileBasedSpeedRepository.DataSource> dataSources = importService.getDataSources();
        return Result.success(dataSources);
    }

    @GetMapping("/template")
    @Operation(summary = "下载CSV模板")
    public Result<String> downloadTemplate() {
        String template = "road_id,day_id,time_id,speed\n" +
                "1,1,1,45.5\n" +
                "1,1,2,42.3\n" +
                "2,1,1,38.2\n" +
                "2,1,2,35.1";
        return Result.success(template);
    }
}
