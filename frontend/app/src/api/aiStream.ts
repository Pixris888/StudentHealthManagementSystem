import { API_BASE_URL } from '@/config'
import { getAccessToken } from '@/utils/auth'
import type {
  AiAnswerVO,
  AiAskDTO,
  AiStreamError,
  AiStreamHandlers,
  AiStreamMeta,
  AiStreamResult,
} from '@/types/ai'

const STREAM_TIMEOUT = 180000

export interface SseParser {
  push: (chunk: string) => void
  flush: () => void
  reset: () => void
}

/**
 * 纯文本 SSE 解析器，按 \n\n 分帧，忽略 :ping 心跳。
 * 单独导出方便单元测试，不依赖 uni / fetch。
 */
export function createSseParser(
  onEvent: (event: string, data: string) => void,
): SseParser {
  let buffer = ''

  function dispatch(frame: string): void {
    let event = 'message'
    const dataLines: string[] = []
    for (const line of frame.split('\n')) {
      if (!line || line.startsWith(':')) continue
      if (line.startsWith('event:')) {
        event = line.slice(6).trim()
      } else if (line.startsWith('data:')) {
        dataLines.push(line.slice(5).replace(/^ /, ''))
      }
    }
    if (dataLines.length) {
      onEvent(event, dataLines.join('\n'))
    }
  }

  return {
    push(chunk: string) {
      buffer += chunk.replace(/\r\n/g, '\n').replace(/\r/g, '\n')
      let index = -1
      while ((index = buffer.indexOf('\n\n')) >= 0) {
        const frame = buffer.slice(0, index)
        buffer = buffer.slice(index + 2)
        dispatch(frame)
      }
    },
    flush() {
      if (buffer.trim()) {
        dispatch(buffer)
        buffer = ''
      }
    },
    reset() {
      buffer = ''
    },
  }
}

function safeJson(raw: string): Record<string, unknown> | null {
  try {
    const value = JSON.parse(raw)
    return value && typeof value === 'object' ? (value as Record<string, unknown>) : null
  } catch {
    return null
  }
}

function stringValue(value: unknown): string {
  return typeof value === 'string' ? value : ''
}

function numberValue(value: unknown): number | undefined {
  return typeof value === 'number' && Number.isFinite(value) ? value : undefined
}

function isFallbackCondition(status: number, code: number, message: string): boolean {
  if ([404, 405, 501].includes(status) || [404, 405, 501].includes(code)) return true
  return /流式输出未启用|不支持流式|流式.{0,6}(未开启|未启用)|stream.{0,12}disabled/i.test(
    message,
  )
}

function httpError(
  status: number,
  body: Record<string, unknown> | null,
): AiStreamError {
  const code = numberValue(body?.code) ?? status
  const message =
    stringValue(body?.msg) ||
    stringValue(body?.message) ||
    (status ? `请求失败（HTTP ${status}）` : '网络异常，请稍后重试')
  return { code, message, partial: false }
}

/**
 * H5 端的 SSE 实现：POST + fetch + ReadableStream。
 * App 端逻辑层没有 fetch/ReadableStream，使用 SseBridge 组件。
 *
 * 返回：
 * - done：完整回答
 * - fallback：流式不可用，应回退同步 /ask
 * - error：流式过程中或服务端明确返回错误（不要再自动回退）
 * - aborted：用户停止
 */
export async function askAiStreamWeb(
  data: AiAskDTO,
  handlers: AiStreamHandlers,
  signal?: AbortSignal,
  timeoutMs = STREAM_TIMEOUT,
): Promise<AiStreamResult> {
  const token = getAccessToken()
  if (!token) {
    return {
      status: 'error',
      error: { code: 401, message: '登录状态已失效，请重新登录', partial: false },
    }
  }

  const controller = new AbortController()
  let timedOut = false

  const abortFromExternal = (): void => controller.abort()
  if (signal) {
    if (signal.aborted) controller.abort()
    else signal.addEventListener('abort', abortFromExternal, { once: true })
  }
  const timer = setTimeout(() => {
    timedOut = true
    controller.abort()
  }, Math.max(10000, timeoutMs))

  let sawStreamData = false

  const cleanup = (): void => {
    clearTimeout(timer)
    signal?.removeEventListener('abort', abortFromExternal)
  }

  try {
    const response = await fetch(`${API_BASE_URL}/ai/consultation/ask/stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'text/event-stream, application/json',
        'Cache-Control': 'no-cache',
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify(data),
      signal: controller.signal,
    })

    const contentType = (response.headers.get('content-type') || '').toLowerCase()

    if (!response.ok || contentType.includes('application/json')) {
      let body: Record<string, unknown> | null = null
      try {
        if (contentType.includes('application/json')) {
          body = (await response.json()) as Record<string, unknown>
        } else {
          body = safeJson(await response.text())
        }
      } catch {
        body = null
      }
      const error = httpError(response.status, body)
      if (isFallbackCondition(response.status, error.code, error.message)) {
        return { status: 'fallback', code: error.code, message: error.message }
      }
      handlers.onError?.(error)
      return { status: 'error', error }
    }

    if (!response.body) {
      return {
        status: 'fallback',
        code: 500,
        message: '当前浏览器不支持流式响应，已切换为同步回答',
      }
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let doneAnswer: AiAnswerVO | null = null
    let streamError: AiStreamError | null = null
    let finished = false

    const parser = createSseParser((event, raw) => {
      const payload = safeJson(raw)
      if (!payload) return

      if (event === 'meta') {
        sawStreamData = true
        handlers.onMeta?.(payload as unknown as AiStreamMeta)
        return
      }
      if (event === 'delta') {
        sawStreamData = true
        const content = stringValue(payload.content)
        if (content) handlers.onDelta?.(content)
        return
      }
      if (event === 'done') {
        sawStreamData = true
        doneAnswer = payload as unknown as AiAnswerVO
        finished = true
        handlers.onDone?.(doneAnswer)
        return
      }
      if (event === 'error') {
        sawStreamData = true
        streamError = {
          code: numberValue(payload.code) ?? 500,
          message:
            stringValue(payload.message) ||
            stringValue(payload.msg) ||
            '回答生成中断，请重试',
          consultationId: numberValue(payload.consultationId),
          partial: Boolean(payload.partial),
        }
        finished = true
        handlers.onError?.(streamError)
      }
    })

    try {
      while (!finished) {
        const { value, done } = await reader.read()
        if (done) break
        if (value) {
          parser.push(decoder.decode(value, { stream: true }))
        }
      }
      parser.push(decoder.decode())
      parser.flush()
    } finally {
      await reader.cancel().catch(() => undefined)
    }

    if (streamError) {
      return { status: 'error', error: streamError }
    }
    if (doneAnswer) {
      return { status: 'done', answer: doneAnswer }
    }
    return {
      status: 'error',
      error: {
        code: 500,
        message: '连接意外结束，请重试',
        partial: sawStreamData,
      },
    }
  } catch (error) {
    const name = (error as { name?: string } | null)?.name
    if (name === 'AbortError' || controller.signal.aborted) {
      if (timedOut) {
        return {
          status: 'error',
          error: {
            code: 408,
            message: 'AI 响应超时，请稍后重试',
            partial: sawStreamData,
          },
        }
      }
      return { status: 'aborted' }
    }
    if (!sawStreamData && (name === 'TypeError' || name === 'NetworkError')) {
      return {
        status: 'fallback',
        code: 0,
        message: '无法建立流式连接，已切换为同步回答',
      }
    }
    return {
      status: 'error',
      error: {
        code: 500,
        message: sawStreamData
          ? '流式连接中断，已保留已生成的内容'
          : '无法建立流式连接，请稍后重试',
        partial: sawStreamData,
      },
    }
  } finally {
    cleanup()
  }
}
