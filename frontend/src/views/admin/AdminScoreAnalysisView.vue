<template>
  <section class="page-card">
    <div class="page-header">
      <div>
        <h1>成绩图表分析</h1>
        <p>选择一个已提交成绩的教学班，查看分数段分布与统计指标。</p>
      </div>
      <el-button @click="loadClasses">刷新</el-button>
    </div>

    <div class="filter-bar">
      <el-select
        v-model="selectedClassId"
        placeholder="选择教学班"
        filterable
        style="width: 380px"
        @change="loadDistribution"
      >
        <el-option
          v-for="item in classes"
          :key="item.teachingClassId"
          :label="`${item.courseName}（${item.teacherName}，${item.semesterName}）`"
          :value="item.teachingClassId"
        />
      </el-select>
    </div>

    <el-empty v-if="classes.length === 0" description="暂无已提交成绩的教学班" />

    <template v-else-if="distribution">
      <div class="summary-row">
        <div class="summary-card">
          <span class="summary-label">参与人数</span>
          <strong>{{ distribution.totalCount }}</strong>
        </div>
        <div class="summary-card">
          <span class="summary-label">平均分</span>
          <strong>{{ formatNumber(distribution.avgScore) }}</strong>
        </div>
        <div class="summary-card">
          <span class="summary-label">最高分</span>
          <strong>{{ formatNumber(distribution.maxScore) }}</strong>
        </div>
        <div class="summary-card">
          <span class="summary-label">最低分</span>
          <strong>{{ formatNumber(distribution.minScore) }}</strong>
        </div>
        <div class="summary-card">
          <span class="summary-label">及格率</span>
          <strong>{{ passRate }}</strong>
        </div>
      </div>

      <div class="chart-grid">
        <div ref="barRef" class="chart-box" v-loading="loading" />
        <div ref="pieRef" class="chart-box" v-loading="loading" />
      </div>
    </template>
  </section>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { getAdminScoredClasses, getAdminScoreDistribution } from '../../api/admin'

const classes = ref([])
const selectedClassId = ref(null)
const distribution = ref(null)
const loading = ref(false)

const barRef = ref(null)
const pieRef = ref(null)
let barChart = null
let pieChart = null

const passRate = computed(() => {
  if (!distribution.value || !distribution.value.totalCount) {
    return '0.00%'
  }
  const pass = Number(distribution.value.passCount || 0)
  const total = Number(distribution.value.totalCount || 0)
  return `${((pass / total) * 100).toFixed(2)}%`
})

function formatNumber(value) {
  if (value === null || value === undefined || value === '') {
    return '-'
  }
  return Number(value).toFixed(1)
}

function buildRanges() {
  const d = distribution.value || {}
  return [
    { label: '不及格(<60)', value: Number(d.rangeFail || 0), color: '#f56c6c' },
    { label: '60-69', value: Number(d.range60 || 0), color: '#e6a23c' },
    { label: '70-79', value: Number(d.range70 || 0), color: '#409eff' },
    { label: '80-89', value: Number(d.range80 || 0), color: '#67c23a' },
    { label: '90-100', value: Number(d.range90 || 0), color: '#13c2c2' }
  ]
}

function renderCharts() {
  const ranges = buildRanges()

  if (barChart) {
    barChart.setOption({
      title: { text: '分数段人数分布', left: 'center', textStyle: { fontSize: 15 } },
      tooltip: { trigger: 'axis' },
      grid: { left: 40, right: 20, bottom: 40, top: 50 },
      xAxis: { type: 'category', data: ranges.map((r) => r.label), axisLabel: { fontSize: 11 } },
      yAxis: { type: 'value', minInterval: 1 },
      series: [
        {
          type: 'bar',
          data: ranges.map((r) => ({ value: r.value, itemStyle: { color: r.color } })),
          barWidth: '48%',
          label: { show: true, position: 'top' }
        }
      ]
    })
  }

  if (pieChart) {
    pieChart.setOption({
      title: { text: '分数段占比', left: 'center', textStyle: { fontSize: 15 } },
      tooltip: { trigger: 'item', formatter: '{b}: {c}人 ({d}%)' },
      legend: { bottom: 0, type: 'scroll' },
      series: [
        {
          type: 'pie',
          radius: ['38%', '62%'],
          center: ['50%', '48%'],
          data: ranges
            .filter((r) => r.value > 0)
            .map((r) => ({ name: r.label, value: r.value, itemStyle: { color: r.color } })),
          label: { formatter: '{b}\n{d}%' }
        }
      ]
    })
  }
}

async function loadClasses() {
  const response = await getAdminScoredClasses()
  classes.value = response.data || []
  if (classes.value.length && !selectedClassId.value) {
    selectedClassId.value = classes.value[0].teachingClassId
  }
  if (selectedClassId.value) {
    await loadDistribution()
  }
}

async function loadDistribution() {
  if (!selectedClassId.value) {
    return
  }
  loading.value = true
  try {
    const response = await getAdminScoreDistribution(selectedClassId.value)
    distribution.value = response.data || null
    await nextTick()
    initCharts()
    renderCharts()
  } finally {
    loading.value = false
  }
}

function initCharts() {
  if (barRef.value && !barChart) {
    barChart = echarts.init(barRef.value)
  }
  if (pieRef.value && !pieChart) {
    pieChart = echarts.init(pieRef.value)
  }
}

function handleResize() {
  barChart?.resize()
  pieChart?.resize()
}

onMounted(() => {
  window.addEventListener('resize', handleResize)
  loadClasses()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  barChart?.dispose()
  pieChart?.dispose()
})
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.filter-bar {
  margin-bottom: 20px;
}

.summary-row {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.summary-card {
  padding: 18px 20px;
  border-radius: 16px;
  background: linear-gradient(135deg, #e7f8ff 0%, #f8fdff 100%);
  border: 1px solid #c8ecff;
}

.summary-card strong {
  display: block;
  margin-top: 8px;
  font-size: 24px;
  color: var(--sky-700);
}

.summary-label {
  color: var(--text-muted);
  font-size: 13px;
}

.chart-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
}

.chart-box {
  height: 360px;
  padding: 16px;
  border-radius: 16px;
  background: #fff;
  border: 1px solid rgba(125, 211, 252, 0.28);
}

@media (max-width: 960px) {
  .summary-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .chart-grid {
    grid-template-columns: 1fr;
  }
}
</style>
