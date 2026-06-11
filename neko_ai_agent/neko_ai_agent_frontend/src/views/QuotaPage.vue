<script setup>
import { computed, onMounted, ref } from 'vue'
import { getMyQuota, redeemQuotaCode } from '../api/quota'

const quota = ref(null)
const redeemCode = ref('')
const loading = ref(false)
const redeeming = ref(false)
const message = ref('')
const messageType = ref('success')

const DAILY_FREE_QUOTA = 100

const usedQuota = computed(() => {
  const totalQuota = quota.value?.totalQuota ?? 0
  const bonusQuota = quota.value?.bonusQuota ?? 0
  return Math.max(DAILY_FREE_QUOTA + bonusQuota - totalQuota, 0)
})

const formatQuota = (value) => {
  return Number(value ?? 0).toLocaleString('zh-CN')
}

const showMessage = (text, type = 'success') => {
  message.value = text
  messageType.value = type
  setTimeout(() => { message.value = '' }, 3000)
}

const loadQuota = async () => {
  loading.value = true
  try {
    const response = await getMyQuota()
    if (response.data.code === 0) {
      quota.value = response.data.data
    } else {
      showMessage(response.data.message || '获取积分失败', 'error')
    }
  } catch (error) {
    showMessage('获取积分失败，请检查网络连接', 'error')
  } finally {
    loading.value = false
  }
}

const handleRedeem = async () => {
  const code = redeemCode.value.trim()
  if (!code) {
    showMessage('请输入兑换码', 'error')
    return
  }
  redeeming.value = true
  try {
    const response = await redeemQuotaCode(code)
    if (response.data.code === 0) {
      quota.value = response.data.data
      redeemCode.value = ''
      showMessage('兑换成功，积分已到账')
    } else {
      showMessage(response.data.message || '兑换失败', 'error')
    }
  } catch (error) {
    showMessage(error?.response?.data?.message || '兑换失败，请稍后重试', 'error')
  } finally {
    redeeming.value = false
  }
}

onMounted(loadQuota)
</script>

<template>
  <main class="page quota-page">
    <div class="sparkle-container">
      <div class="sparkle-star" v-for="n in 24" :key="'quota-star-' + n" />
    </div>

    <section class="quota-shell">
      <header class="quota-hero">
        <router-link to="/" class="quota-back-link">
          <span class="quota-back-arrow">‹</span>
          <span>返回首页</span>
        </router-link>
        <p class="quota-eyebrow">Neko Credits</p>
        <h1>我的积分配额</h1>
        <p class="quota-subtitle">
          每次智能体对话扣减 10 积分，每天 00:00 自动刷新 100 免费积分。
        </p>
        <div class="quota-hero-line"></div>
      </header>

      <Transition name="fade">
        <div v-if="message" class="quota-message" :class="messageType">
          {{ message }}
        </div>
      </Transition>

      <section class="quota-card">
        <div class="quota-card-glow"></div>
        <div v-if="loading" class="quota-loading">积分星图加载中...</div>
        <div v-else class="quota-grid">
          <div class="quota-stat quota-stat-main">
            <span>可用总积分</span>
            <strong class="quota-number" :data-value="formatQuota(quota?.totalQuota)">
              {{ formatQuota(quota?.totalQuota) }}
            </strong>
            <small>Total balance</small>
          </div>
          <div class="quota-stat">
            <span>每日免费积分</span>
            <strong class="quota-number" :data-value="formatQuota(quota?.dailyQuota)">
              {{ formatQuota(quota?.dailyQuota) }}
            </strong>
            <small>Daily credits</small>
          </div>
          <div class="quota-stat">
            <span>额外积分</span>
            <strong class="quota-number" :data-value="formatQuota(quota?.bonusQuota)">
              {{ formatQuota(quota?.bonusQuota) }}
            </strong>
            <small>Bonus credits</small>
          </div>
          <div class="quota-stat">
            <span>已消耗积分</span>
            <strong class="quota-number" :data-value="formatQuota(usedQuota)">
              {{ formatQuota(usedQuota) }}
            </strong>
            <small>Used today</small>
          </div>
        </div>

        <div class="redeem-panel">
          <div class="redeem-copy">
            <p class="redeem-eyebrow">Redeem Code</p>
            <h2>兑换积分</h2>
            <span>输入管理员生成的 24 小时有效兑换码，积分会立即加入额外积分。</span>
          </div>
          <div class="redeem-form">
            <input
              v-model="redeemCode"
              type="text"
              placeholder="请输入兑换码，例如 NQ-XXXXXXXX"
              @keydown.enter="handleRedeem"
            />
            <button :disabled="redeeming" @click="handleRedeem">
              {{ redeeming ? '兑换中...' : '立即兑换' }}
            </button>
          </div>
        </div>
      </section>
    </section>
  </main>
</template>

<style scoped>
.quota-page {
  min-height: 100vh;
  position: relative;
  isolation: isolate;
  padding: 104px 28px 72px;
  overflow: hidden;
  color: #f8fafc;
  background:
    radial-gradient(ellipse 70% 48% at 50% 12%, rgba(255, 255, 255, 0.08), transparent 66%),
    radial-gradient(circle at 14% 28%, rgba(96, 165, 250, 0.14), transparent 30%),
    linear-gradient(180deg, #030405 0%, #08090b 48%, #020303 100%);
}

.quota-page::before {
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

.quota-shell {
  width: min(1120px, 100%);
  margin: 0 auto;
}

.quota-hero {
  margin-bottom: 28px;
  animation: quota-rise 0.8s var(--ease-out-expo) both;
}

.quota-card {
  position: relative;
  overflow: hidden;
  padding: 30px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 24px;
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.095), rgba(255, 255, 255, 0.035)),
    rgba(8, 9, 11, 0.82);
  box-shadow:
    0 24px 80px rgba(0, 0, 0, 0.42),
    inset 0 1px 0 rgba(255, 255, 255, 0.06);
  backdrop-filter: blur(20px);
  animation: quota-rise 0.8s var(--ease-out-expo) 0.12s both;
}

.quota-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(120deg, transparent, rgba(255, 255, 255, 0.08), transparent);
  opacity: 0.24;
  transform: translateX(-62%);
  pointer-events: none;
}

.quota-card-glow {
  position: absolute;
  top: -160px;
  right: -100px;
  width: 300px;
  height: 300px;
  border-radius: 999px;
  background: rgba(96, 165, 250, 0.16);
  filter: blur(46px);
  pointer-events: none;
}

.quota-back-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 22px;
  padding: 6px 14px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 20px;
  color: rgba(255, 255, 255, 0.62);
  font-size: 13px;
  text-decoration: none;
  backdrop-filter: blur(8px);
  transition: all 0.25s ease;
}

.quota-back-link:hover {
  color: #fff;
  border-color: rgba(255, 255, 255, 0.25);
  background: rgba(255, 255, 255, 0.06);
}

.quota-back-arrow {
  font-size: 16px;
  line-height: 1;
}

.quota-hero h1 {
  margin: 0 0 6px;
  color: rgba(255, 255, 255, 0.95);
  font-family: 'Noto Sans SC', Inter, sans-serif;
  font-size: 28px;
  font-weight: 700;
  line-height: 1.25;
  text-shadow: none;
}

.quota-subtitle {
  width: min(640px, 100%);
  margin: 0;
  color: rgba(255, 255, 255, 0.4);
  font-size: 14px;
}

.quota-eyebrow {
  margin-bottom: 10px;
  color: rgba(255, 255, 255, 0.68);
  font-family: 'Orbitron', sans-serif;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  text-shadow: 0 0 18px rgba(255, 255, 255, 0.28);
}

.quota-hero-line {
  width: 64px;
  height: 2px;
  margin-top: 16px;
  border-radius: 1px;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.42), transparent);
  animation: line-glow 3s ease-in-out infinite alternate;
}

.quota-grid {
  position: relative;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  z-index: 1;
}

.quota-stat {
  position: relative;
  overflow: hidden;
  min-height: 142px;
  padding: 22px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 18px;
  background:
    radial-gradient(circle at 20% 0%, rgba(255, 255, 255, 0.09), transparent 36%),
    rgba(0, 0, 0, 0.28);
  transition: transform 0.22s ease, border-color 0.22s ease, box-shadow 0.22s ease;
}

.quota-stat:hover {
  transform: translateY(-4px);
  border-color: rgba(143, 211, 255, 0.34);
  box-shadow: 0 18px 50px rgba(0, 0, 0, 0.28);
}

.quota-stat::after {
  content: '';
  position: absolute;
  right: 18px;
  bottom: 18px;
  width: 34px;
  height: 34px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 50%;
  box-shadow: 0 0 22px rgba(143, 211, 255, 0.08);
}

.quota-stat span {
  display: block;
  margin-bottom: 10px;
  color: rgba(255, 255, 255, 0.58);
  font-weight: 600;
}

.quota-number {
  display: block;
  width: fit-content;
  color: #fff;
  font-family: 'Noto Sans SC', Inter, sans-serif;
  font-size: clamp(30px, 3.6vw, 38px);
  font-weight: 700;
  line-height: 1.08;
  letter-spacing: -0.02em;
  font-variant-numeric: tabular-nums;
  text-shadow: 0 0 12px rgba(255, 255, 255, 0.14);
}

.quota-stat small {
  display: block;
  margin-top: 12px;
  color: rgba(255, 255, 255, 0.34);
  font-family: 'Orbitron', sans-serif;
  font-size: 11px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.quota-stat-main {
  border-color: rgba(255, 255, 255, 0.12);
  background:
    radial-gradient(circle at 20% 0%, rgba(255, 255, 255, 0.09), transparent 36%),
    rgba(0, 0, 0, 0.28);
}

.redeem-panel {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: minmax(220px, 0.36fr) minmax(0, 1fr);
  gap: 28px;
  align-items: end;
  margin-top: 30px;
  padding: 24px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 18px;
  background: rgba(0, 0, 0, 0.22);
}

.redeem-panel h2 {
  margin: 6px 0 8px;
  color: #fff;
  font-size: 24px;
}

.redeem-copy span {
  color: rgba(255, 255, 255, 0.46);
  font-size: 13px;
  line-height: 1.7;
}

.redeem-eyebrow {
  margin: 0;
  color: #93c5fd;
  font-family: 'Orbitron', sans-serif;
  font-size: 11px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.redeem-form {
  display: flex;
  gap: 14px;
}

.redeem-form input {
  flex: 1;
  min-height: 52px;
  padding: 14px 16px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 14px;
  color: #f8fafc;
  background: rgba(0, 0, 0, 0.48);
  outline: none;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, background 0.18s ease;
}

.redeem-form input:focus {
  border-color: rgba(143, 211, 255, 0.46);
  background: rgba(0, 0, 0, 0.58);
  box-shadow: 0 0 0 4px rgba(96, 165, 250, 0.1);
}

.redeem-form button {
  min-height: 52px;
  padding: 0 26px;
  border: 0;
  border-radius: 14px;
  color: white;
  font-weight: 700;
  background: linear-gradient(135deg, #2563eb, #38bdf8);
  box-shadow: 0 16px 36px rgba(37, 99, 235, 0.28);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, opacity 0.2s ease;
}

.redeem-form button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 20px 44px rgba(37, 99, 235, 0.36);
}

.redeem-form button:disabled {
  cursor: not-allowed;
  opacity: 0.65;
}

.quota-message {
  margin-bottom: 18px;
  padding: 12px 16px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 14px;
  backdrop-filter: blur(14px);
}

.quota-message.success {
  color: #bbf7d0;
  background: rgba(22, 163, 74, 0.18);
}

.quota-message.error {
  color: #fecaca;
  background: rgba(220, 38, 38, 0.18);
}

.quota-loading {
  position: relative;
  z-index: 1;
  padding: 48px 0;
  color: rgba(255, 255, 255, 0.52);
  text-align: center;
}

@keyframes quota-rise {
  0% {
    opacity: 0;
    transform: translateY(18px);
  }
  100% {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 820px) {
  .quota-page {
    padding: 92px 18px 48px;
  }

  .quota-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .redeem-panel {
    grid-template-columns: 1fr;
  }

  .redeem-form {
    flex-direction: column;
  }
}

@media (max-width: 520px) {
  .quota-card {
    padding: 20px;
  }

  .quota-grid {
    grid-template-columns: 1fr;
  }
}
</style>
