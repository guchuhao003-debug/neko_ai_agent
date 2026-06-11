<script setup>
import { onMounted, ref } from 'vue'
import { deleteQuotaCode, generateQuotaCodes, listQuotaCodes } from '../api/quota'

const codes = ref([])
const total = ref(0)
const current = ref(1)
const pageSize = ref(10)
const status = ref('')
const keyword = ref('')
const loading = ref(false)
const generating = ref(false)
const generatedCodes = ref([])
const message = ref('')
const messageType = ref('success')

const generateForm = ref({
  count: 10,
  quotaAmount: 100
})

const showMessage = (text, type = 'success') => {
  message.value = text
  messageType.value = type
  setTimeout(() => { message.value = '' }, 3000)
}

const loadCodes = async () => {
  loading.value = true
  try {
    const response = await listQuotaCodes({
      current: current.value,
      pageSize: pageSize.value,
      code: keyword.value.trim() || null,
      status: status.value || null
    })
    if (response.data.code === 0) {
      codes.value = response.data.data.records || []
      total.value = Number(response.data.data.total || 0)
    } else {
      showMessage(response.data.message || '加载兑换码失败', 'error')
    }
  } catch (error) {
    showMessage('加载兑换码失败，请检查网络连接', 'error')
  } finally {
    loading.value = false
  }
}

const handleGenerate = async () => {
  generating.value = true
  try {
    const response = await generateQuotaCodes(generateForm.value)
    if (response.data.code === 0) {
      generatedCodes.value = response.data.data || []
      showMessage('兑换码生成成功')
      await loadCodes()
    } else {
      showMessage(response.data.message || '生成失败', 'error')
    }
  } catch (error) {
    showMessage(error?.response?.data?.message || '生成失败，请稍后重试', 'error')
  } finally {
    generating.value = false
  }
}

const handleDelete = async (item) => {
  if (!confirm(`确认删除兑换码 ${item.code} 吗？`)) return
  try {
    const response = await deleteQuotaCode(item.id)
    if (response.data.code === 0) {
      showMessage('删除成功')
      await loadCodes()
    } else {
      showMessage(response.data.message || '删除失败', 'error')
    }
  } catch (error) {
    showMessage('删除失败，请稍后重试', 'error')
  }
}

const totalPages = () => Math.ceil(total.value / pageSize.value) || 1

const goPage = (page) => {
  if (page < 1 || page > totalPages()) return
  current.value = page
  loadCodes()
}

const formatTime = (value) => {
  if (!value) return '-'
  return new Date(value).toLocaleString('zh-CN')
}

const statusText = (value) => {
  const statusMap = {
    UNUSED: '未使用',
    USED: '已使用',
    EXPIRED: '已过期'
  }
  return statusMap[value] || value || '-'
}

onMounted(loadCodes)
</script>

<template>
  <main class="page qcm-page">
    <div class="sparkle-container">
      <div class="sparkle-star" v-for="n in 24" :key="'qcm-star-' + n" />
    </div>

    <section class="qcm-shell">
      <header class="qcm-header">
        <router-link to="/" class="qcm-back-btn">‹ 返回首页</router-link>
        <p class="qcm-eyebrow">Admin Credits</p>
        <h1>积分兑换码管理</h1>
        <span>批量生成、检索和清理积分兑换码。兑换码 24 小时有效，使用后立即失效。</span>
        <div class="qcm-header-line"></div>
      </header>

      <Transition name="fade">
        <div v-if="message" class="qcm-message" :class="messageType">
          {{ message }}
        </div>
      </Transition>

      <section class="qcm-card">
        <div class="qcm-card-heading">
          <div>
            <p>Generator</p>
            <h2>批量生成兑换码</h2>
          </div>
          <span>有效期 24h</span>
        </div>
        <div class="qcm-form">
          <label>
            <span>生成数量</span>
            <input v-model.number="generateForm.count" type="number" min="1" max="1000" />
          </label>
          <label>
            <span>每码积分</span>
            <input
              v-model.number="generateForm.quotaAmount"
              type="number"
              min="1"
              max="100000"
            />
          </label>
          <button :disabled="generating" @click="handleGenerate">
            {{ generating ? '生成中...' : '生成兑换码' }}
          </button>
        </div>
        <div v-if="generatedCodes.length" class="qcm-generated">
          <div class="qcm-generated-title">
            <strong>本次生成</strong>
            <span>{{ generatedCodes.length }} 个兑换码</span>
          </div>
          <div class="qcm-generated-list">
            <code v-for="item in generatedCodes" :key="item.id">{{ item.code }}</code>
          </div>
        </div>
      </section>

      <section class="qcm-card">
        <div class="qcm-card-heading qcm-list-heading">
          <div>
            <p>Code Console</p>
            <h2>兑换码列表</h2>
          </div>
          <span>共 {{ total }} 条</span>
        </div>
        <div class="qcm-toolbar">
          <input v-model="keyword" placeholder="搜索兑换码" @keydown.enter="loadCodes" />
          <select v-model="status">
            <option value="">全部状态</option>
            <option value="UNUSED">未使用</option>
            <option value="USED">已使用</option>
            <option value="EXPIRED">已过期</option>
          </select>
          <button @click="loadCodes">查询</button>
        </div>

        <div v-if="loading" class="qcm-loading">加载中...</div>
        <div v-else class="qcm-table-wrap">
          <table class="qcm-table">
            <thead>
              <tr>
                <th>兑换码</th>
                <th>积分</th>
                <th>状态</th>
                <th>过期时间</th>
                <th>使用用户</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="codes.length === 0">
                <td colspan="6" class="qcm-empty">暂无兑换码</td>
              </tr>
              <tr v-for="item in codes" :key="item.id">
                <td><code>{{ item.code }}</code></td>
                <td class="qcm-amount">{{ item.quotaAmount }}</td>
                <td>
                  <span class="qcm-status" :class="'qcm-status-' + item.status?.toLowerCase()">
                    {{ statusText(item.status) }}
                  </span>
                </td>
                <td>{{ formatTime(item.expireTime) }}</td>
                <td>{{ item.usedUserId || '-' }}</td>
                <td>
                  <button class="qcm-danger" @click="handleDelete(item)">删除</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="qcm-pagination">
          <button :disabled="current <= 1" @click="goPage(current - 1)">上一页</button>
          <span>{{ current }} / {{ totalPages() }}</span>
          <button :disabled="current >= totalPages()" @click="goPage(current + 1)">下一页</button>
        </div>
      </section>
    </section>
  </main>
</template>

<style scoped>
.qcm-page {
  min-height: 100vh;
  position: relative;
  isolation: isolate;
  padding: 104px 28px 72px;
  overflow: hidden;
  color: #f8fafc;
  background:
    radial-gradient(ellipse 70% 48% at 50% 12%, rgba(255, 255, 255, 0.08), transparent 66%),
    radial-gradient(circle at 82% 20%, rgba(96, 165, 250, 0.12), transparent 30%),
    linear-gradient(180deg, #030405 0%, #08090b 48%, #020303 100%);
}

.qcm-page::before {
  content: '';
  position: fixed;
  inset: -50%;
  z-index: -2;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.025) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.025) 1px, transparent 1px);
  background-size: 56px 56px;
  mask-image: radial-gradient(ellipse at 50% 20%, #000 0%, transparent 72%);
  pointer-events: none;
  animation: grid-scroll 20s linear infinite;
}

.qcm-shell {
  width: min(1180px, 100%);
  margin: 0 auto;
}

.qcm-header {
  margin-bottom: 28px;
  animation: qcm-rise 0.8s var(--ease-out-expo) both;
}

.qcm-back-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-bottom: 22px;
  padding: 6px 14px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 20px;
  color: rgba(255, 255, 255, 0.62);
  font-size: 13px;
  line-height: 1.2;
  text-decoration: none;
  white-space: nowrap;
  vertical-align: top;
  backdrop-filter: blur(8px);
  transition: all 0.25s ease;
}

.qcm-back-btn:hover {
  color: #fff;
  border-color: rgba(255, 255, 255, 0.25);
  background: rgba(255, 255, 255, 0.06);
}

.qcm-eyebrow,
.qcm-card-heading p {
  margin-bottom: 12px;
  color: rgba(255, 255, 255, 0.68);
  font-family: 'Orbitron', sans-serif;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  text-shadow: 0 0 18px rgba(255, 255, 255, 0.28);
}

.qcm-header h1 {
  margin: 0 0 6px;
  color: rgba(255, 255, 255, 0.95);
  font-family: 'Noto Sans SC', Inter, sans-serif;
  font-size: 28px;
  font-weight: 700;
  line-height: 1.25;
  text-shadow: none;
}

.qcm-header span {
  display: block;
  width: min(680px, 100%);
  margin: 0;
  color: rgba(255, 255, 255, 0.4);
  font-size: 14px;
}

.qcm-header-line {
  width: 64px;
  height: 2px;
  margin-top: 16px;
  border-radius: 1px;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.42), transparent);
  animation: line-glow 3s ease-in-out infinite alternate;
}

.qcm-card {
  position: relative;
  overflow: hidden;
  margin-top: 22px;
  padding: 24px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 20px;
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.095), rgba(255, 255, 255, 0.035)),
    rgba(8, 9, 11, 0.82);
  box-shadow:
    0 24px 80px rgba(0, 0, 0, 0.42),
    inset 0 1px 0 rgba(255, 255, 255, 0.06);
  backdrop-filter: blur(20px);
  animation: qcm-rise 0.8s var(--ease-out-expo) 0.12s both;
}

.qcm-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(120deg, transparent, rgba(255, 255, 255, 0.08), transparent);
  opacity: 0.22;
  transform: translateX(-64%);
  pointer-events: none;
}

.qcm-card-heading {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 20px;
}

.qcm-card-heading p {
  margin: 0 0 8px;
  color: #93c5fd;
  font-size: 11px;
  letter-spacing: 0.12em;
}

.qcm-card-heading h2 {
  margin: 0;
  color: #fff;
  font-size: 22px;
}

.qcm-card-heading > span {
  padding: 6px 10px;
  border: 1px solid rgba(143, 211, 255, 0.24);
  border-radius: 999px;
  color: #bfdbfe;
  background: rgba(37, 99, 235, 0.14);
  font-size: 12px;
}

.qcm-form,
.qcm-toolbar {
  position: relative;
  z-index: 1;
  display: flex;
  gap: 12px;
  align-items: end;
  flex-wrap: wrap;
}

.qcm-form label {
  display: grid;
  gap: 8px;
  color: rgba(255, 255, 255, 0.64);
  font-size: 13px;
  font-weight: 600;
}

.qcm-form input,
.qcm-toolbar input,
.qcm-toolbar select {
  min-width: 180px;
  min-height: 46px;
  padding: 12px 14px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 12px;
  color: #f8fafc;
  background: rgba(0, 0, 0, 0.48);
  outline: none;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, background 0.18s ease;
}

.qcm-form input:focus,
.qcm-toolbar input:focus,
.qcm-toolbar select:focus {
  border-color: rgba(143, 211, 255, 0.46);
  background: rgba(0, 0, 0, 0.58);
  box-shadow: 0 0 0 4px rgba(96, 165, 250, 0.1);
}

.qcm-form button,
.qcm-toolbar button,
.qcm-pagination button {
  min-height: 46px;
  padding: 12px 18px;
  border: 0;
  border-radius: 12px;
  color: white;
  font-weight: 700;
  background: linear-gradient(135deg, #2563eb, #38bdf8);
  box-shadow: 0 16px 36px rgba(37, 99, 235, 0.24);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, opacity 0.2s ease;
}

.qcm-form button:hover:not(:disabled),
.qcm-toolbar button:hover:not(:disabled),
.qcm-pagination button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 20px 44px rgba(37, 99, 235, 0.34);
}

.qcm-form button:disabled,
.qcm-pagination button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.qcm-table-wrap {
  position: relative;
  z-index: 1;
  overflow-x: auto;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 16px;
  background: rgba(0, 0, 0, 0.18);
}

.qcm-table {
  width: 100%;
  border-collapse: collapse;
}

.qcm-table th,
.qcm-table td {
  padding: 15px 14px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  text-align: left;
  white-space: nowrap;
}

.qcm-table th {
  color: rgba(255, 255, 255, 0.48);
  font-size: 12px;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.qcm-table tbody tr {
  transition: background 0.18s ease;
}

.qcm-table tbody tr:hover {
  background: rgba(255, 255, 255, 0.035);
}

.qcm-table tbody tr:last-child td {
  border-bottom: 0;
}

.qcm-danger {
  padding: 8px 12px;
  border: 1px solid rgba(248, 113, 113, 0.18);
  border-radius: 10px;
  color: #fecaca;
  background: rgba(220, 38, 38, 0.18);
  cursor: pointer;
  transition: background 0.18s ease, transform 0.18s ease;
}

.qcm-danger:hover {
  transform: translateY(-1px);
  background: rgba(220, 38, 38, 0.28);
}

.qcm-generated {
  position: relative;
  z-index: 1;
  margin-top: 18px;
  padding: 16px;
  border: 1px solid rgba(143, 211, 255, 0.16);
  border-radius: 16px;
  background: rgba(37, 99, 235, 0.08);
}

.qcm-generated-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
  color: #e0f2fe;
}

.qcm-generated-title span {
  color: rgba(255, 255, 255, 0.46);
  font-size: 12px;
}

.qcm-generated-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

code {
  padding: 4px 8px;
  border-radius: 8px;
  color: #bfdbfe;
  background: rgba(37, 99, 235, 0.16);
}

.qcm-message {
  margin-bottom: 18px;
  padding: 12px 16px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 14px;
  backdrop-filter: blur(14px);
}

.qcm-message.success {
  color: #bbf7d0;
  background: rgba(22, 163, 74, 0.18);
}

.qcm-message.error {
  color: #fecaca;
  background: rgba(220, 38, 38, 0.18);
}

.qcm-pagination {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
  margin-top: 18px;
}

.qcm-pagination span {
  display: inline-flex;
  align-items: center;
  min-height: 42px;
  color: rgba(255, 255, 255, 0.58);
  font-family: 'Orbitron', sans-serif;
  font-size: 12px;
}

.qcm-loading,
.qcm-empty {
  position: relative;
  z-index: 1;
  padding: 40px 0;
  color: rgba(255, 255, 255, 0.52);
  text-align: center;
}

.qcm-amount {
  color: #fff;
  font-family: 'Noto Sans SC', Inter, sans-serif;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.qcm-status {
  display: inline-flex;
  align-items: center;
  padding: 5px 10px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 999px;
  color: rgba(255, 255, 255, 0.68);
  background: rgba(255, 255, 255, 0.06);
  font-size: 12px;
}

.qcm-status-unused {
  border-color: rgba(74, 222, 128, 0.24);
  color: #bbf7d0;
  background: rgba(22, 163, 74, 0.14);
}

.qcm-status-used {
  border-color: rgba(147, 197, 253, 0.24);
  color: #bfdbfe;
  background: rgba(37, 99, 235, 0.14);
}

.qcm-status-expired {
  border-color: rgba(248, 113, 113, 0.24);
  color: #fecaca;
  background: rgba(220, 38, 38, 0.14);
}

@keyframes qcm-rise {
  0% {
    opacity: 0;
    transform: translateY(18px);
  }
  100% {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 760px) {
  .qcm-page {
    padding: 92px 18px 48px;
  }

  .qcm-card {
    padding: 20px;
  }

  .qcm-card-heading {
    flex-direction: column;
  }

  .qcm-form,
  .qcm-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .qcm-form input,
  .qcm-toolbar input,
  .qcm-toolbar select {
    min-width: 0;
    width: 100%;
  }

  .qcm-pagination {
    justify-content: center;
  }
}
</style>
