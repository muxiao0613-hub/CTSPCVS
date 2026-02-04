package com.backend.pcx.service;

import com.backend.pcx.dto.ImportJobDTO;
import com.backend.pcx.entity.ImportJob;
import com.backend.pcx.entity.RoadSegment;
import com.backend.pcx.repository.FileBasedSpeedRepository;
import com.backend.pcx.repository.ImportJobRepository;
import com.backend.pcx.repository.RoadSegmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ImportService {
    
    private static final Logger logger = LoggerFactory.getLogger(ImportService.class);
    
    private final ImportJobRepository importJobRepository;
    private final RoadSegmentRepository roadSegmentRepository;
    private final FileBasedSpeedRepository fileBasedSpeedRepository;
    
    @Value("${app.data-dir:./data}")
    private String dataDir;
    
    private final Map<Long, ImportJob> jobCache = new ConcurrentHashMap<>();

    public ImportService(ImportJobRepository importJobRepository,
                         RoadSegmentRepository roadSegmentRepository,
                         FileBasedSpeedRepository fileBasedSpeedRepository) {
        this.importJobRepository = importJobRepository;
        this.roadSegmentRepository = roadSegmentRepository;
        this.fileBasedSpeedRepository = fileBasedSpeedRepository;
    }

    @Transactional
    public List<Long> importSpeedCsv(MultipartFile[] files) {
        List<Long> jobIds = new ArrayList<>();
        
        for (MultipartFile file : files) {
            Long jobId = importSingleFile(file);
            if (jobId != null) {
                jobIds.add(jobId);
            }
        }
        
        return jobIds;
    }

    @Transactional
    public Long importSpeedCsv(MultipartFile file) {
        return importSingleFile(file);
    }

    private Long importSingleFile(MultipartFile file) {
        try {
            Path targetDir = Paths.get(dataDir);
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }
            
            Path targetPath = targetDir.resolve(file.getOriginalFilename());
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            
            ImportJob job = new ImportJob();
            job.setFilename(file.getOriginalFilename());
            job.setStatus(ImportJob.ImportStatus.PROCESSING);
            job.setCreatedAt(System.currentTimeMillis());
            job = importJobRepository.save(job);
            
            jobCache.put(job.getId(), job);
            
            processImportAsync(job, targetPath);
            
            return job.getId();
        } catch (IOException e) {
            logger.error("File save error", e);
            return null;
        }
    }

    private void processImportAsync(ImportJob job, Path filePath) {
        try {
            List<FileBasedSpeedRepository.DataSource> dataSources = fileBasedSpeedRepository.getDataSources();
            
            int totalRows = 0;
            int successRows = 0;
            int failRows = 0;
            StringBuilder errorMessage = new StringBuilder();
            
            for (FileBasedSpeedRepository.DataSource source : dataSources) {
                if (source.getFilename().equals(job.getFilename())) {
                    totalRows = source.getTotalRoads() * source.getTotalDays() * 144;
                    successRows = totalRows;
                    
                    for (int roadId = 1; roadId <= source.getTotalRoads(); roadId++) {
                        if (!roadSegmentRepository.findByRoadId(roadId).isPresent()) {
                            RoadSegment segment = new RoadSegment();
                            segment.setRoadId(roadId);
                            segment.setName(null);
                            segment.setRegion(null);
                            roadSegmentRepository.save(segment);
                        }
                    }
                    
                    break;
                }
            }
            
            job.setTotalRows(totalRows);
            job.setSuccessRows(successRows);
            job.setFailRows(failRows);
            job.setStatus(ImportJob.ImportStatus.COMPLETED);
            if (errorMessage.length() > 0) {
                job.setErrorMessage(errorMessage.toString());
            }
            job.setFinishedAt(System.currentTimeMillis());
            importJobRepository.save(job);
            
        } catch (Exception e) {
            logger.error("Import error", e);
            job.setStatus(ImportJob.ImportStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            job.setFinishedAt(System.currentTimeMillis());
            importJobRepository.save(job);
        }
    }

    public ImportJobDTO getImportJob(Long jobId) {
        ImportJob job = importJobRepository.findById(jobId).orElse(null);
        if (job == null) {
            return null;
        }
        return toDTO(job);
    }

    private ImportJobDTO toDTO(ImportJob job) {
        return new ImportJobDTO(
                job.getId(),
                job.getFilename(),
                job.getStatus(),
                job.getTotalRows(),
                job.getSuccessRows(),
                job.getFailRows(),
                job.getErrorMessage(),
                job.getCreatedAt(),
                job.getFinishedAt()
        );
    }
    
    public List<FileBasedSpeedRepository.DataSource> getDataSources() {
        return fileBasedSpeedRepository.getDataSources();
    }
}
