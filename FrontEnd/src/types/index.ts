export enum CongestionLevel {
  FREE = 'FREE',
  FLOWING = 'FLOWING',
  CONGESTED = 'CONGESTED'
}

export interface Result<T> {
  code: number
  message: string
  data: T
}

export interface RoadSegment {
  id: number
  roadId: number
  name: string
  region: string
}

export interface SpeedRecord {
  id: number
  segmentId: number
  segmentName: string
  ts: number
  speed: number
}

export interface ImportJob {
  id: number
  filename: string
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED'
  totalRows: number
  successRows: number
  failRows: number
  errorMessage: string
  createdAt: number
  finishedAt: number
}

export interface PredictionPoint {
  id: number
  ts: number
  predictedSpeed: number
  congestionLevel: CongestionLevel
}

export interface PredictionJob {
  id: number
  segmentId: number
  segmentName: string
  baseTime: number
  horizonSteps: number
  predictorType: string
  costMs: number
  createdAt: number
  points: PredictionPoint[]
}

export interface CongestedSegment {
  id: number
  roadId: number
  name: string
  region: string
  avgSpeed: number
  congestionLevel: CongestionLevel
}

export interface RegionCongestion {
  region: string
  freeCount: number
  flowingCount: number
  congestedCount: number
}

export interface CongestionLevelDistribution {
  freeCount: number
  flowingCount: number
  congestedCount: number
}

export interface DashboardSummary {
  todayAvgSpeed: number
  congestedSegments: number
  mostCongestedSegment: CongestedSegment
  regionCongestions: RegionCongestion[]
  topCongestedSegments: CongestedSegment[]
  congestionLevelDistribution: CongestionLevelDistribution
}

export interface DataSource {
  filename: string
  month: string
  totalRoads: number
  totalDays: number
}

export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

export interface PredictRequest {
  segmentId: number
  baseTime: number | null
  horizonSteps: number
}
