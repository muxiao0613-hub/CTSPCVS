package com.backend.pcx.controller;

import com.backend.pcx.dto.Result;
import com.backend.pcx.dto.SpeedRecordDTO;
import com.backend.pcx.service.SpeedRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/speeds")
@Tag(name = "速度数据", description = "速度数据查询接口")
public class SpeedRecordController {
    
    @Autowired
    private SpeedRecordService speedRecordService;

    @GetMapping
    @Operation(summary = "查询速度记录")
    public Result<List<SpeedRecordDTO>> getSpeedRecords(
            @Parameter(description = "路段ID") @RequestParam Long segmentId,
            @Parameter(description = "开始时间戳") @RequestParam(required = false) Long from,
            @Parameter(description = "结束时间戳") @RequestParam(required = false) Long to) {
        List<SpeedRecordDTO> records = speedRecordService.getSpeedRecords(segmentId, from, to);
        return Result.success(records);
    }
}
