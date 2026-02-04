import request from '@/utils/request'
import type {
  RoadSegment,
  SpeedRecord,
  ImportJob,
  PredictionJob,
  PredictRequest,
  DashboardSummary,
  DataSource,
  Page
} from '@/types'

export const api = {
  dashboard: {
    getSummary: () => request.get<DashboardSummary>('/dashboard/summary')
  },
  
  segments: {
    getList: (params: { keyword?: string; region?: string; page?: number; size?: number }) =>
      request.get<Page<RoadSegment>>('/segments', { params }),
    
    getFromSources: () => request.get<RoadSegment[]>('/segments/from-sources'),
    
    getById: (id: number) => request.get<RoadSegment>(`/segments/${id}`)
  },
  
  speeds: {
    getList: (params: { segmentId: number; from?: number; to?: number }) =>
      request.get<SpeedRecord[]>('/speeds', { params })
  },
  
  import: {
    upload: (file: File, onProgress?: (percent: number) => void) =>
      request.upload<number>('/import/speed-csv', file, onProgress),
    
    uploadBatch: (files: File[]) => {
      const formData = new FormData()
      files.forEach(file => formData.append('files', file))
      return request.post<number[]>('/import/speed-csv-batch', formData)
    },
    
    getJob: (jobId: number) => request.get<ImportJob>(`/import/${jobId}`),
    
    getDataSources: () => request.get<DataSource[]>('/import/data-sources'),
    
    getTemplate: () => request.get<string>('/import/template')
  },
  
  predict: {
    create: (data: PredictRequest) => request.post<PredictionJob>('/predict', data),
    
    getJobs: (params: { page?: number; size?: number }) =>
      request.get<Page<PredictionJob>>('/predict/jobs', { params }),
    
    getJob: (jobId: number) => request.get<PredictionJob>(`/predict/jobs/${jobId}`),
    
    getJobsBySegment: (segmentId: number, params: { page?: number; size?: number }) =>
      request.get<Page<PredictionJob>>(`/predict/jobs/segment/${segmentId}`, { params })
  }
}

export default api
