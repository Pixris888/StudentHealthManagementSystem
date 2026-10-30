package com.health.management.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.health.management.dto.ai.AiAskDTO;
import com.health.management.dto.ai.AiConsultationQueryDTO;
import com.health.management.dto.ai.AiFeedbackDTO;
import com.health.management.entity.AiConsultation;
import com.health.management.service.ai.AiStreamSink;
import com.health.management.vo.ai.AiAnswerVO;
import com.health.management.vo.ai.AiConsultationVO;
import com.health.management.vo.ai.AiStatsVO;

import java.util.List;

/**
 * AI 健康问答业务接口。
 */
public interface AiConsultationService extends IService<AiConsultation> {

    /** 学生提问：安全拦截、健康画像、知识召回、模型调用/降级、落库 */
    AiAnswerVO ask(AiAskDTO dto);

    /**
     * 学生流式提问：在异步线程中执行，事件通过 sink 推送。
     * 调用前必须由 Controller 在请求线程捕获 userId，避免异步线程丢失 SecurityContext。
     */
    void askStream(AiAskDTO dto, Long userId, AiStreamSink sink);

    /** 当前学生问答历史 */
    IPage<AiConsultationVO> pageMine(AiConsultationQueryDTO query);

    /** 当前学生某个会话的全部消息 */
    List<AiConsultationVO> listSession(String sessionId);

    /** 当前学生问答详情 */
    AiConsultationVO getMine(Long id);

    /** 当前学生删除问答记录 */
    boolean deleteMine(List<Long> ids);

    /** 当前学生对回答反馈 */
    AiConsultationVO feedback(Long id, AiFeedbackDTO dto);

    /** 校医/超管分页查询问答记录（校医脱敏） */
    IPage<AiConsultationVO> pageAll(AiConsultationQueryDTO query);

    /** 校医/超管问答详情 */
    AiConsultationVO getDetail(Long id);

    /** 校医/超管使用统计 */
    AiStatsVO stats(Integer days);

    /** 校医/超管查看未命中/差评问题，用于反哺知识库 */
    List<AiConsultationVO> listUnanswered(Integer limit);
}
