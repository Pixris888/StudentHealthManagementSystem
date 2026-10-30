<template>
  <!-- @vue-ignore renderjs 的 :change 绑定不是普通实例属性 -->
  <view
    class="sse-bridge"
    :request="request"
    :change:request="sseRender.onRequestChange"
  />
</template>

<script lang="ts">
import { defineComponent, type PropType } from 'vue'
import type { SseBridgeEvent, SseBridgeRequest } from '@/types/ai'

/**
 * App 端 SSE 桥。
 *
 * uni-app App 的逻辑层没有 fetch / ReadableStream，这里把请求放到 renderjs
 * （视图层 WebView）里，用 XMLHttpRequest 的 readyState=3 / onprogress 读取
 * 增量 responseText，再通过 $ownerInstance.callMethod 回传逻辑层。
 *
 * 逻辑层必须是 Options API（callMethod 不支持 <script setup>），
 * 因此本组件单独拆出来，AI 页面通过 @event 接收事件。
 */
export default defineComponent({
  name: 'SseBridge',
  props: {
    request: {
      type: Object as PropType<SseBridgeRequest | null>,
      default: null,
    },
  },
  emits: ['event'],
  methods: {
    onRenderEvent(payload: SseBridgeEvent): void {
      this.$emit('event', payload)
    },
  },
})
</script>

<script module="sseRender" lang="renderjs">
const states = Object.create(null)

function parseJson(text) {
  try {
    return JSON.parse(text)
  } catch (e) {
    return null
  }
}

export default {
  methods: {
    onRequestChange(newValue) {
      if (typeof newValue === 'string') {
        try {
          newValue = JSON.parse(newValue)
        } catch (e) {
          return
        }
      }
      if (!newValue || !newValue.id) return
      if (newValue.action === 'abort') {
        this.abortRequest(newValue.id, true)
        return
      }
      if (newValue.action === 'start') {
        this.startRequest(newValue)
      }
    },

    emitToLogic(id, event, payload) {
      this.$ownerInstance.callMethod(
        'onRenderEvent',
        Object.assign({ id: id, event: event }, payload || {}),
      )
    },

    startRequest(req) {
      this.abortRequest(req.id, false)

      const xhr = new XMLHttpRequest()
      const state = {
        xhr: xhr,
        buffer: '',
        lastIndex: 0,
        finished: false,
        closed: false,
        userAborted: false,
        sawSse: false,
      }
      states[req.id] = state

      const finish = (event, payload) => {
        if (state.closed) return
        state.closed = true
        this.emitToLogic(req.id, event, payload)
      }

      const consume = () => {
        if (state.closed || state.finished) return
        const contentType = (xhr.getResponseHeader('content-type') || '').toLowerCase()
        const text = xhr.responseText || ''
        // JSON 错误体不按 SSE 解析，等 readyState=4 统一处理
        if (
          contentType.indexOf('application/json') >= 0 ||
          (contentType.indexOf('text/event-stream') < 0 &&
            text.trimStart().indexOf('{') === 0)
        ) {
          return
        }
        if (!text || text.length <= state.lastIndex) return
        const chunk = text.slice(state.lastIndex)
        state.lastIndex = text.length
        if (!chunk) return
        state.sawSse = true
        state.buffer += chunk.replace(/\r\n/g, '\n').replace(/\r/g, '\n')

        let index = -1
        while ((index = state.buffer.indexOf('\n\n')) >= 0) {
          const frame = state.buffer.slice(0, index)
          state.buffer = state.buffer.slice(index + 2)
          this.dispatchFrame(req.id, state, frame)
        }
      }

      xhr.open('POST', req.url, true)
      xhr.setRequestHeader('Content-Type', 'application/json')
      xhr.setRequestHeader('Accept', 'text/event-stream, application/json')
      xhr.setRequestHeader('Cache-Control', 'no-cache')
      if (req.token) {
        xhr.setRequestHeader('Authorization', 'Bearer ' + req.token)
      }
      xhr.timeout = req.timeout || 180000

      xhr.onreadystatechange = () => {
        if (state.closed) return
        if (xhr.readyState === 3) {
          consume()
          return
        }
        if (xhr.readyState !== 4) return

        const status = xhr.status
        const contentType = (xhr.getResponseHeader('content-type') || '').toLowerCase()

        if (status === 0) {
          finish('network-error', { message: '网络异常，无法连接 AI 服务' })
          this.cleanup(req.id)
          return
        }

        const responseText = xhr.responseText || ''
        const looksJson =
          !state.sawSse && responseText.trim().indexOf('{') === 0

        if (
          contentType.indexOf('application/json') >= 0 ||
          status >= 400 ||
          looksJson
        ) {
          const body = parseJson(responseText)
          const code =
            body && typeof body.code === 'number' ? body.code : status || 500
          const message =
            (body && (body.msg || body.message)) ||
            (status ? '请求失败（HTTP ' + status + '）' : '网络异常，请稍后重试')
          finish('http-error', {
            code: code,
            message: message,
            httpStatus: status,
            partial: false,
          })
          this.cleanup(req.id)
          return
        }

        consume()
        if (state.buffer.trim()) {
          this.dispatchFrame(req.id, state, state.buffer)
          state.buffer = ''
        }
        if (!state.finished) {
          finish('ended', { partial: state.sawSse })
        } else {
          state.closed = true
        }
        this.cleanup(req.id)
      }

      xhr.onprogress = () => {
        consume()
      }

      xhr.onerror = () => {
        finish('network-error', { message: '网络异常，无法连接 AI 服务' })
        this.cleanup(req.id)
      }

      xhr.ontimeout = () => {
        finish('timeout', { code: 408, message: 'AI 响应超时，请稍后重试' })
        this.cleanup(req.id)
      }

      xhr.onabort = () => {
        if (state.userAborted) {
          finish('aborted', {})
        } else {
          state.closed = true
        }
        this.cleanup(req.id)
      }

      this.emitToLogic(req.id, 'started', {})

      try {
        xhr.send(JSON.stringify(req.body || {}))
      } catch (e) {
        finish('network-error', { message: '无法发起流式请求' })
        this.cleanup(req.id)
      }
    },

    dispatchFrame(id, state, frame) {
      if (state.closed) return
      let event = 'message'
      const dataLines = []

      for (const line of frame.split('\n')) {
        if (!line || line.indexOf(':') === 0) continue
        if (line.indexOf('event:') === 0) {
          event = line.slice(6).trim()
        } else if (line.indexOf('data:') === 0) {
          dataLines.push(line.slice(5).replace(/^ /, ''))
        }
      }

      if (!dataLines.length) return
      const raw = dataLines.join('\n')

      if (event === 'meta' || event === 'delta') {
        this.emitToLogic(id, event, { data: raw })
      } else if (event === 'done' || event === 'error') {
        state.finished = true
        this.emitToLogic(id, event, { data: raw })
      }
    },

    abortRequest(id, userInitiated) {
      const state = states[id]
      if (!state) return
      if (state.closed) {
        this.cleanup(id)
        return
      }

      state.userAborted = Boolean(userInitiated)
      state.finished = true
      try {
        state.xhr.abort()
      } catch (e) {
        // ignore
      }

      if (userInitiated) {
        state.closed = true
        this.emitToLogic(id, 'aborted', {})
      } else {
        state.closed = true
      }
      this.cleanup(id)
    },

    cleanup(id) {
      delete states[id]
    },
  },
}
</script>

<style scoped>
.sse-bridge {
  width: 0;
  height: 0;
  overflow: hidden;
}
</style>
