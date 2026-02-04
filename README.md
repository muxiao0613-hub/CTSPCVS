# 城市区域交通流量预测系统

基于 Spring Boot 3 + Vue 3 + TypeScript 的城市区域交通流量预测系统，提供交通数据导入、查询、预测和可视化功能。

## 系统架构

### 后端技术栈
- Spring Boot 3.2.0
- Spring Data JPA
- H2 数据库（文件模式）
- SpringDoc OpenAPI (Swagger)
- Apache Commons CSV

### 前端技术栈
- Vue 3.3.8
- TypeScript 5.3.2
- Vite 5.0.2
- Element Plus 2.4.4
- ECharts 5.4.3
- Vue Router 4.2.5
- Pinia 2.1.7
- Axios 1.6.2

## 功能特性

### 1. 数据导入与管理
- 支持多 CSV 文件上传导入速度数据（10分钟粒度）
- 文件保存到本地 data 目录，不直接入库
- 数据源自动扫描和注册
- 导入任务状态跟踪与统计
- CSV 模板下载

### 2. 按需加载与缓存
- **FileBasedSpeedRepository**：基于文件的按需加载机制
- 首次查询某个 road_id 时，从 CSV 文件中提取该 road 的所有记录
- 数据过滤：空值、异常值（speed<=0）自动过滤
- LRU 缓存：最多缓存 20 条 road 序列（可配置）
- 后续查询同一 road_id：直接从缓存切片返回
- 支持时间范围查询和插值参数

### 3. 历史速度查询
- 路段列表：分页、关键字搜索
- 路段详情：基础信息 + 速度趋势图
- 速度查询：按时间段返回速度序列

### 4. 预测功能
- **BaselinePredictor**：基于移动平均 + 高峰/非高峰修正
- 预测参数：路段ID、基准时间（可选，默认最新）、预测步数
- 预测结果：速度序列 + 拥堵等级
- 预测任务历史记录

### 5. 流量/拥堵等级评估
- 速度 >= 40 km/h：畅通
- 25 km/h <= 速度 < 40 km/h：缓行
- 速度 < 25 km/h：拥堵

### 6. 可视化前端
- 交通态势大屏：KPI卡片、拥堵等级分布图、速度趋势图、拥堵Top5
- 路段管理：列表、搜索、详情
- 路段详情：历史速度图表、预测面板、任务历史
- 数据导入：文件上传、进度显示、结果统计、数据源列表
- 预测任务：任务列表、详情查看、CSV导出

## 数据格式

### 真实数据格式
系统支持您提供的真实数据集格式：

```csv
road_id,day_id,time_id,speed
1,1,1,45.5
1,1,2,42.3
2,1,1,38.2
2,1,2,35.1
```

字段说明：
- `road_id`：路段ID（1-214）
- `day_id`：日期（Aug: 1-31；Sep: 1-30）
- `time_id`：时间窗口（1-144，每个代表10分钟，time_id=1为00:00-00:10）
- `speed`：平均速度（km/h；空/0/非数字视为缺失；<=0 视为缺失）

### 时间戳换算规则
- Aug: day_id=1 => 2016-08-01
- Sep: day_id=1 => 2016-09-01
- ts = baseDate + (day_id-1)天 + (time_id-1)*10分钟

## 快速开始

### 环境要求
- JDK 17+
- Node.js 18+
- Maven 3.8+

### 后端启动

1. 进入后端目录：
```bash
cd BackEnd
```

2. 使用 Maven 启动：
```bash
./mvnw spring-boot:run
```

或在 Windows 上：
```cmd
mvnw.cmd spring-boot:run
```

3. 后端服务将在 `http://localhost:8080` 启动

4. 访问 Swagger API 文档：
```
http://localhost:8080/swagger-ui.html
```

### 前端启动

1. 进入前端目录：
```bash
cd FrontEnd
```

2. 安装依赖：
```bash
npm install
```

3. 启动开发服务器：
```bash
npm run dev
```

4. 前端服务将在 `http://localhost:5173` 启动

## 数据导入

### 准备数据文件
1. 将您的 CSV 文件（如 speeddata_Aug.csv, speeddata_Sep.csv）准备好
2. 确保文件格式符合上述要求

### 导入步骤
1. 启动后端服务
2. 访问前端导入页面
3. 拖拽或点击上传 CSV 文件
4. 系统将自动：
   - 保存文件到 data 目录
   - 扫描并注册数据源
   - 显示导入结果

### 数据源管理
- 系统启动时自动扫描 data 目录
- 支持查看已注册的数据源列表
- 支持刷新数据源

## API 接口

### 数据导入
- `POST /api/import/speed-csv` - 上传单个CSV文件
- `POST /api/import/speed-csv-batch` - 批量上传CSV文件
- `GET /api/import/{jobId}` - 查询导入任务状态
- `GET /api/import/data-sources` - 获取已注册数据源列表
- `GET /api/import/template` - 下载CSV模板

### 路段管理
- `GET /api/segments` - 查询路段列表
- `GET /api/segments/from-sources` - 从数据源获取路段列表
- `GET /api/segments/{id}` - 查询路段详情

### 速度数据
- `GET /api/speeds?segmentId=...` - 查询速度记录

### 预测服务
- `POST /api/predict` - 执行预测
- `GET /api/predict/jobs` - 查询预测任务列表
- `GET /api/predict/jobs/{jobId}` - 查询预测任务详情
- `GET /api/predict/jobs/segment/{segmentId}` - 查询路段的预测任务

### 仪表盘
- `GET /api/dashboard/summary` - 获取仪表盘汇总数据

## 数据库

### H2 数据库
- 模式：文件模式
- 位置：`BackEnd/data/traffic.mv.db`
- 控制台：`http://localhost:8080/h2-console`
- 连接信息：
  - JDBC URL: `jdbc:h2:file:./data/traffic`
  - 用户名: `sa`
  - 密码: (空)

### 数据表
- `road_segment`：路段信息
- `road_alias`：路段别名
- `user_setting`：用户设置
- `import_job`：导入任务
- `prediction_job`：预测任务
- `prediction_point`：预测结果点

**注意**：`speed_record` 表不再使用，速度数据直接从 CSV 文件按需加载。

## 配置说明

### 后端配置 (application.properties)
```properties
# 服务端口
server.port=8080

# 数据库配置
spring.datasource.url=jdbc:h2:file:./data/traffic;DB_CLOSE_ON_EXIT=FALSE
spring.datasource.username=sa
spring.datasource.password=

# 数据目录
app.data-dir=${DATA_DIR:./data}

# 缓存配置
app.cache.max-roads=20

# 预测阈值
traffic.prediction.free-speed-threshold=40
traffic.prediction.flowing-speed-threshold=25
traffic.prediction.max-valid-speed=200
traffic.prediction.min-valid-speed=0
traffic.prediction.prediction-window-size=6
```

### 前端配置 (vite.config.ts)
```typescript
export default defineConfig({
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
```

## 开发指南

### 添加新的预测器
1. 实现 `TrafficPredictor` 接口
2. 添加 `@Component` 注解
3. 在 `PredictionService` 中注入使用

### 添加新的API接口
1. 在 Controller 中添加接口方法
2. 在 Service 中实现业务逻辑
3. 在前端 `api/index.ts` 中添加接口调用
4. 在页面中使用

## 常见问题

### 后端启动失败
- 检查 JDK 版本是否为 17+
- 检查端口 8080 是否被占用
- 清理并重新构建：`./mvnw clean install`

### 前端启动失败
- 检查 Node.js 版本是否为 18+
- 删除 `node_modules` 重新安装：`rm -rf node_modules && npm install`
- 检查后端服务是否已启动

### 数据导入失败
- 检查 CSV 文件格式是否正确
- 检查时间戳是否为毫秒
- 检查速度值是否在 0-200 范围内

### 数据未加载
- 确认 CSV 文件已上传到 data 目录
- 检查文件名是否包含 "speeddata"
- 刷新数据源列表

## 项目结构

```
├── BackEnd/
│   ├── src/main/java/com/traffic/
│   │   ├── config/          # 配置类
│   │   ├── controller/       # 控制器
│   │   ├── dto/             # 数据传输对象
│   │   ├── entity/          # 实体类
│   │   ├── predictor/        # 预测器
│   │   ├── repository/       # 数据访问层
│   │   │   ├── FileBasedSpeedRepository.java  # 基于文件的速度数据仓库
│   │   │   └── ...
│   │   └── service/         # 业务逻辑层
│   └── pom.xml
├── FrontEnd/
│   ├── src/
│   │   ├── api/            # API 接口
│   │   ├── assets/          # 静态资源
│   │   ├── components/      # 通用组件
│   │   ├── router/          # 路由配置
│   │   ├── types/           # TypeScript 类型定义
│   │   ├── utils/           # 工具函数
│   │   ├── views/           # 页面组件
│   │   ├── App.vue
│   │   └── main.ts
│   ├── package.json
│   └── vite.config.ts
├── data/                   # 数据目录（自动创建）
│   ├── speeddata_Aug.csv   # 8月数据
│   └── speeddata_Sep.csv   # 9月数据
└── README.md
```

## 架构设计

### 为什么不把全量速度数据入库？
1. **性能考虑**：185万行数据入库和查询会带来性能压力
2. **存储效率**：CSV 文件本身就是高效的存储格式
3. **按需加载**：实际使用时只加载需要的数据
4. **缓存优化**：LRU 缓存确保热点数据快速访问

### 按需加载机制
1. 首次查询某 road_id 时，从 CSV 文件中扫描提取
2. 转换为按 ts 排序的序列，过滤缺失与异常值
3. 缓存到内存（LRU，最多 20 条 road 序列）
4. 后续查询同一 road_id，直接从缓存切片返回

### 预测器扩展
系统预留了 `TrafficPredictor` 接口，可以轻松扩展其他预测算法：
- 实现接口
- 注册为 Spring Bean
- 在预测服务中调用

## 许可证

Apache License 2.0
