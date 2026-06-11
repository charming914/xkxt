<template>
  <section class="page-card">
    <div class="page-header">
      <div>
        <h1>消息中心</h1>
        <p>查看审批结果通知与学校公告，及时了解你的申请进展。</p>
      </div>
      <div class="header-actions">
        <el-radio-group v-model="onlyUnread" @change="loadNotifications">
          <el-radio-button :value="false">全部</el-radio-button>
          <el-radio-button :value="true">未读</el-radio-button>
        </el-radio-group>
        <el-button :disabled="unreadCount === 0" @click="handleMarkAll">全部已读</el-button>
        <el-button @click="loadNotifications">刷新</el-button>
      </div>
    </div>

    <el-empty v-if="!loading && notifications.length === 0" description="暂无消息" />

    <div v-else v-loading="loading" class="message-list">
      <div
        v-for="item in notifications"
        :key="item.notificationId"
        class="message-item"
        :class="{ unread: !item.isRead }"
        @click="handleRead(item)"
      >
        <div class="message-main">
          <div class="message-head">
            <el-tag size="small" :type="categoryType(item.category)" effect="plain">
              {{ categoryLabel(item.category) }}
            </el-tag>
            <span class="message-title">{{ item.title }}</span>
            <span v-if="!item.isRead" class="dot" />
          </div>
          <p class="message-content">{{ item.content }}</p>
          <span class="message-time">{{ item.createdAt }}</span>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getNotifications,
  markNotificationRead,
  markAllNotificationsRead
} from '../api/notification'

const loading = ref(false)
const notifications = ref([])
const onlyUnread = ref(false)

const unreadCount = computed(() => notifications.value.filter((item) => !item.isRead).length)

const categoryTextMap = {
  APPROVAL: '审批结果',
  ANNOUNCEMENT: '公告'
}

function categoryLabel(category) {
  return categoryTextMap[category] || '通知'
}

function categoryType(category) {
  return category === 'ANNOUNCEMENT' ? 'warning' : 'success'
}

async function loadNotifications() {
  loading.value = true
  try {
    const response = await getNotifications({
      unreadOnly: onlyUnread.value,
      limit: 100
    })
    notifications.value = response.data || []
  } finally {
    loading.value = false
  }
}

async function handleRead(item) {
  if (item.isRead) {
    return
  }
  await markNotificationRead(item.notificationId)
  item.isRead = 1
  if (onlyUnread.value) {
    loadNotifications()
  }
}

async function handleMarkAll() {
  await markAllNotificationsRead()
  ElMessage.success('已全部标记为已读')
  loadNotifications()
}

onMounted(loadNotifications)
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.message-item {
  padding: 16px 18px;
  border-radius: 12px;
  background: #f8fbff;
  border: 1px solid #e6f0fa;
  cursor: pointer;
  transition: box-shadow 0.2s ease, transform 0.2s ease;
}

.message-item:hover {
  box-shadow: 0 8px 20px rgba(3, 105, 161, 0.12);
  transform: translateY(-1px);
}

.message-item.unread {
  background: #eef7ff;
  border-color: #bcdcff;
}

.message-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.message-title {
  font-weight: 600;
  font-size: 15px;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #f56c6c;
}

.message-content {
  margin: 8px 0;
  color: #475569;
  line-height: 1.6;
  white-space: pre-wrap;
}

.message-time {
  font-size: 12px;
  color: #94a3b8;
}
</style>
