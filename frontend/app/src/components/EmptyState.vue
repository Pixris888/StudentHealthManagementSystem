<template>
  <view class="empty-state">
    <view class="empty-icon">
      <uni-icons :type="icon" size="28" color="#b3c5bf" />
    </view>
    <text class="empty-title">{{ title }}</text>
    <text v-if="desc" class="empty-desc">{{ desc }}</text>
    <button v-if="actionText" class="empty-action" @click="emit('action')">
      {{ actionText }}
    </button>
  </view>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    icon?: string
    title?: string
    desc?: string
    actionText?: string
  }>(),
  {
    icon: 'info',
    title: '暂无数据',
    desc: '',
    actionText: '',
  },
)

const emit = defineEmits<{ action: [] }>()
</script>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 72rpx 40rpx;
  text-align: center;
}

.empty-icon {
  width: 96rpx;
  height: 96rpx;
  border-radius: 50%;
  background: #f1f7f5;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24rpx;
}

.empty-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #475569;
}

.empty-desc {
  margin-top: 10rpx;
  font-size: 25rpx;
  line-height: 1.6;
  color: #94a3b8;
}

.empty-action {
  margin-top: 28rpx;
  padding: 0 38rpx;
  height: 72rpx;
  line-height: 72rpx;
  border-radius: 999rpx;
  background: #0f9d8a;
  color: #ffffff;
  font-size: 27rpx;
  border: none;
}

.empty-action::after {
  border: none;
}
</style>
