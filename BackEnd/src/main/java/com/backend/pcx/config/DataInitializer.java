package com.backend.pcx.config;

import com.backend.pcx.entity.RoadSegment;
import com.backend.pcx.entity.SpeedRecord;
import com.backend.pcx.repository.RoadSegmentRepository;
import com.backend.pcx.repository.SpeedRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
public class DataInitializer implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);
    
    private final RoadSegmentRepository roadSegmentRepository;
    private final SpeedRecordRepository speedRecordRepository;
    
    private final Random random = new Random();

    public DataInitializer(RoadSegmentRepository roadSegmentRepository,
                           SpeedRecordRepository speedRecordRepository) {
        this.roadSegmentRepository = roadSegmentRepository;
        this.speedRecordRepository = speedRecordRepository;
    }

    @Override
    public void run(String... args) {
        if (roadSegmentRepository.count() > 0) {
            logger.info("Database already initialized, skipping demo data creation");
            return;
        }
        
        logger.info("Initializing demo data...");
        
        List<RoadSegment> segments = createDemoSegments();
        createDemoSpeedRecords(segments);
        
        logger.info("Demo data initialization completed");
    }

    private List<RoadSegment> createDemoSegments() {
        List<RoadSegment> segments = new ArrayList<>();
        
        segments.add(createSegment(1, "人民路", "朝阳区"));
        segments.add(createSegment(2, "建设大道", "海淀区"));
        segments.add(createSegment(3, "中山路", "西城区"));
        segments.add(createSegment(4, "解放路", "东城区"));
        segments.add(createSegment(5, "和平大道", "丰台区"));
        segments.add(createSegment(6, "友谊路", "石景山区"));
        segments.add(createSegment(7, "长安街", "西城区"));
        segments.add(createSegment(8, "复兴路", "海淀区"));
        segments.add(createSegment(9, "朝阳路", "朝阳区"));
        segments.add(createSegment(10, "通惠河路", "通州区"));
        
        return roadSegmentRepository.saveAll(segments);
    }

    private RoadSegment createSegment(Integer roadId, String name, String region) {
        RoadSegment segment = new RoadSegment();
        segment.setRoadId(roadId);
        segment.setName(name);
        segment.setRegion(region);
        return segment;
    }

    private void createDemoSpeedRecords(List<RoadSegment> segments) {
        List<SpeedRecord> allRecords = new ArrayList<>();
        
        LocalDate today = LocalDate.now();
        long startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000;
        
        for (RoadSegment segment : segments) {
            List<SpeedRecord> segmentRecords = new ArrayList<>();
            
            for (int day = 0; day < 7; day++) {
                long dayStart = startOfDay + (day * 24L * 60 * 60 * 1000);
                
                for (int hour = 0; hour < 24; hour++) {
                    for (int minute = 0; minute < 60; minute += 10) {
                        long ts = dayStart + (hour * 60L * 60 * 1000) + (minute * 60L * 1000);
                        
                        double baseSpeed = 40.0;
                        
                        if ((hour >= 7 && hour <= 9) || (hour >= 17 && hour <= 19)) {
                            baseSpeed = 20.0 + random.nextDouble() * 15;
                        } else if (hour >= 22 || hour <= 5) {
                            baseSpeed = 50.0 + random.nextDouble() * 20;
                        } else {
                            baseSpeed = 35.0 + random.nextDouble() * 20;
                        }
                        
                        SpeedRecord record = new SpeedRecord();
                        record.setSegment(segment);
                        record.setTs(ts);
                        record.setSpeed(Math.round(baseSpeed * 10.0) / 10.0);
                        segmentRecords.add(record);
                    }
                }
            }
            
            allRecords.addAll(segmentRecords);
            
            if (allRecords.size() >= 1000) {
                speedRecordRepository.saveAll(allRecords);
                allRecords.clear();
            }
        }
        
        if (!allRecords.isEmpty()) {
            speedRecordRepository.saveAll(allRecords);
        }
    }
}
