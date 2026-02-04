package com.backend.pcx.repository;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class FileBasedSpeedRepository {
    
    @Value("${app.data-dir:./data}")
    private String dataDir;
    
    @Value("${app.cache.max-roads:20}")
    private int maxCachedRoads;
    
    private final Map<Integer, List<SpeedDataPoint>> cache = new ConcurrentHashMap<>();
    private final Map<Integer, Long> lastAccessTime = new ConcurrentHashMap<>();
    
    private static final long TEN_MINUTES_MS = 10 * 60 * 1000L;
    
    private static final Map<String, Long> BASE_DATES = new HashMap<>();
    static {
        BASE_DATES.put("Aug", 1470000000000L);
        BASE_DATES.put("Sep", 1472691200000L);
    }
    
    @Data
    public static class SpeedDataPoint {
        private Long ts;
        private Double speed;
    }
    
    @Data
    public static class DataSource {
        private String filename;
        private String month;
        private Integer totalRoads;
        private Integer totalDays;
        
        public DataSource(String filename, String month, Integer totalRoads, Integer totalDays) {
            this.filename = filename;
            this.month = month;
            this.totalRoads = totalRoads;
            this.totalDays = totalDays;
        }
    }
    
    public List<SpeedDataPoint> getSpeedData(Integer roadId, Long fromTs, Long toTs, boolean interpolate) {
        List<SpeedDataPoint> data = loadRoadData(roadId);
        
        if (data.isEmpty()) {
            return data;
        }
        
        List<SpeedDataPoint> filtered = data.stream()
                .filter(p -> p.getSpeed() != null && p.getSpeed() > 0)
                .collect(Collectors.toList());
        
        if (fromTs != null || toTs != null) {
            filtered = filtered.stream()
                    .filter(p -> {
                        if (fromTs != null && p.getTs() < fromTs) return false;
                        if (toTs != null && p.getTs() > toTs) return false;
                        return true;
                    })
                    .collect(Collectors.toList());
        }
        
        if (interpolate && !filtered.isEmpty()) {
            filtered = interpolateMissingValues(filtered);
        }
        
        return filtered;
    }
    
    public List<SpeedDataPoint> getRecentSpeedData(Integer roadId, int limit) {
        List<SpeedDataPoint> data = loadRoadData(roadId);
        
        if (data.isEmpty()) {
            return data;
        }
        
        List<SpeedDataPoint> validData = data.stream()
                .filter(p -> p.getSpeed() != null && p.getSpeed() > 0)
                .collect(Collectors.toList());
        
        int size = Math.min(limit, validData.size());
        return validData.subList(validData.size() - size, validData.size());
    }
    
    public List<DataSource> getDataSources() {
        List<DataSource> sources = new ArrayList<>();
        
        try {
            Path dirPath = Paths.get(dataDir);
            if (!Files.exists(dirPath)) {
                return sources;
            }
            
            Files.list(dirPath)
                    .filter(p -> p.toString().endsWith(".csv") && p.toString().contains("speeddata"))
                    .forEach(p -> {
                        String filename = p.getFileName().toString();
                        String month = extractMonth(filename);
                        Integer totalRoads = countRoadsInFile(p.toString());
                        Integer totalDays = countDaysInFile(p.toString());
                        
                        sources.add(new DataSource(filename, month, totalRoads, totalDays));
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return sources;
    }
    
    private List<SpeedDataPoint> loadRoadData(Integer roadId) {
        lastAccessTime.put(roadId, System.currentTimeMillis());
        
        if (cache.containsKey(roadId)) {
            return cache.get(roadId);
        }
        
        List<SpeedDataPoint> data = loadFromFiles(roadId);
        
        if (!data.isEmpty()) {
            return data;
        }
        
        evictIfNeeded();
        cache.put(roadId, data);
        
        return data;
    }
    
    private List<SpeedDataPoint> loadFromFiles(Integer roadId) {
        List<SpeedDataPoint> allData = new ArrayList<>();
        
        try {
            Path dirPath = Paths.get(dataDir);
            if (!Files.exists(dirPath)) {
                return allData;
            }
            
            Files.list(dirPath)
                    .filter(p -> p.toString().endsWith(".csv") && p.toString().contains("speeddata"))
                    .forEach(p -> {
                        List<SpeedDataPoint> fileData = loadRoadFromFile(p.toString(), roadId);
                        allData.addAll(fileData);
                    });
            
            allData.sort(Comparator.comparing(SpeedDataPoint::getTs));
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return allData;
    }
    
    private List<SpeedDataPoint> loadRoadFromFile(String filePath, Integer roadId) {
        List<SpeedDataPoint> data = new ArrayList<>();
        String month = extractMonth(filePath);
        Long baseDate = BASE_DATES.get(month);
        
        if (baseDate == null) {
            return data;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line = reader.readLine();
            
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 4) continue;
                
                try {
                    Integer currentRoadId = Integer.parseInt(parts[0].trim());
                    if (!currentRoadId.equals(roadId)) continue;
                    
                    Integer dayId = Integer.parseInt(parts[1].trim());
                    Integer timeId = Integer.parseInt(parts[2].trim());
                    String speedStr = parts[3].trim();
                    
                    if (speedStr.isEmpty() || !speedStr.matches("-?\\d+(\\.\\d+)?")) {
                        continue;
                    }
                    
                    Double speed = Double.parseDouble(speedStr);
                    if (speed <= 0) continue;
                    
                    Long ts = calculateTimestamp(baseDate, dayId, timeId);
                    
                    SpeedDataPoint point = new SpeedDataPoint();
                    point.setTs(ts);
                    point.setSpeed(speed);
                    data.add(point);
                } catch (NumberFormatException e) {
                    continue;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return data;
    }
    
    private Long calculateTimestamp(Long baseDate, Integer dayId, Integer timeId) {
        long dayOffset = (dayId - 1) * 24L * 60 * 60 * 1000;
        long timeOffset = (timeId - 1) * TEN_MINUTES_MS;
        return baseDate + dayOffset + timeOffset;
    }
    
    private String extractMonth(String filename) {
        if (filename.contains("Aug")) return "Aug";
        if (filename.contains("Sep")) return "Sep";
        return "";
    }
    
    private Integer countRoadsInFile(String filePath) {
        Set<Integer> roadIds = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line = reader.readLine();
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 1) {
                    try {
                        roadIds.add(Integer.parseInt(parts[0].trim()));
                    } catch (NumberFormatException e) {
                        continue;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return roadIds.size();
    }
    
    private Integer countDaysInFile(String filePath) {
        Set<Integer> dayIds = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line = reader.readLine();
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 2) {
                    try {
                        dayIds.add(Integer.parseInt(parts[1].trim()));
                    } catch (NumberFormatException e) {
                        continue;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return dayIds.size();
    }
    
    private List<SpeedDataPoint> interpolateMissingValues(List<SpeedDataPoint> data) {
        if (data.size() < 2) return data;
        
        List<SpeedDataPoint> result = new ArrayList<>(data);
        
        for (int i = 1; i < result.size() - 1; i++) {
            SpeedDataPoint prev = result.get(i - 1);
            SpeedDataPoint curr = result.get(i);
            SpeedDataPoint next = result.get(i + 1);
            
            if (curr.getSpeed() == null || curr.getSpeed() <= 0) {
                if (prev.getSpeed() != null && prev.getSpeed() > 0 && 
                    next.getSpeed() != null && next.getSpeed() > 0) {
                    double interpolated = (prev.getSpeed() + next.getSpeed()) / 2.0;
                    curr.setSpeed(interpolated);
                }
            }
        }
        
        return result;
    }
    
    private void evictIfNeeded() {
        if (cache.size() <= maxCachedRoads) return;
        
        List<Map.Entry<Integer, Long>> entries = lastAccessTime.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .collect(Collectors.toList());
        
        int toRemove = cache.size() - maxCachedRoads;
        for (int i = 0; i < toRemove; i++) {
            Integer roadId = entries.get(i).getKey();
            cache.remove(roadId);
            lastAccessTime.remove(roadId);
        }
    }
    
    public void clearCache() {
        cache.clear();
        lastAccessTime.clear();
    }
    
    public Set<Integer> getAllRoadIds() {
        Set<Integer> allRoadIds = new HashSet<>();
        
        try {
            Path dirPath = Paths.get(dataDir);
            if (!Files.exists(dirPath)) {
                return allRoadIds;
            }
            
            Files.list(dirPath)
                    .filter(p -> p.toString().endsWith(".csv") && p.toString().contains("speeddata"))
                    .forEach(p -> {
                        Set<Integer> roadIdsInFile = getRoadIdsFromFile(p.toString());
                        allRoadIds.addAll(roadIdsInFile);
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return allRoadIds;
    }
    
    private Set<Integer> getRoadIdsFromFile(String filePath) {
        Set<Integer> roadIds = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line = reader.readLine();
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 1) {
                    try {
                        roadIds.add(Integer.parseInt(parts[0].trim()));
                    } catch (NumberFormatException e) {
                        continue;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return roadIds;
    }
}
