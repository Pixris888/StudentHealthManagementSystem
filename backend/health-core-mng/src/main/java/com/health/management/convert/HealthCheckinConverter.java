package com.health.management.convert;

import com.health.management.common.util.ValidateUtils;
import com.health.management.entity.HealthCheckin;
import com.health.management.entity.HealthCheckinPhoto;
import com.health.management.entity.HealthCheckinSession;
import com.health.management.entity.SysUser;
import com.health.management.vo.HealthCheckinPhotoVO;
import com.health.management.vo.HealthCheckinSessionVO;
import com.health.management.vo.HealthCheckinVO;

import java.util.List;

/**
 * 打卡实体 → VO 转换：派生字段、名称映射和身份脱敏。
 */
public final class HealthCheckinConverter {

    private HealthCheckinConverter() {
    }

    public static HealthCheckinVO toCheckinVO(HealthCheckin checkin, SysUser user, boolean desensitize,
                                              boolean backfilled,
                                              List<HealthCheckinPhotoVO> photos, int photoCount) {
        HealthCheckinVO vo = new HealthCheckinVO();
        vo.setCheckedIn(checkin != null && checkin.getId() != null);
        vo.setBackfilled(backfilled);
        vo.setPhotoCount(photoCount);
        vo.setPhotos(photos);

        if (checkin != null) {
            vo.setId(checkin.getId());
            vo.setUserId(checkin.getUserId());
            vo.setStudentNo(checkin.getStudentNo());
            vo.setCheckinDate(checkin.getCheckinDate());
            vo.setSleepTime(checkin.getSleepTime());
            vo.setWakeTime(checkin.getWakeTime());
            vo.setSleepDuration(checkin.getSleepDuration());
            vo.setStayUpLate(checkin.getStayUpLate());
            vo.setSleepSource(checkin.getSleepSource());
            vo.setExerciseType(checkin.getExerciseType());
            vo.setExerciseDuration(checkin.getExerciseDuration());
            vo.setExerciseIntensity(checkin.getExerciseIntensity());
            vo.setExerciseSufficient(checkin.getExerciseSufficient());
            vo.setExerciseSource(checkin.getExerciseSource());
            vo.setDietBreakfast(checkin.getDietBreakfast());
            vo.setDietBalance(checkin.getDietBalance());
            vo.setWaterIntake(checkin.getWaterIntake());
            vo.setEmotionType(checkin.getEmotionType());
            vo.setMoodScore(checkin.getMoodScore());
            vo.setContent(checkin.getContent());
            vo.setDataQuality(checkin.getDataQuality() == null ? 0 : checkin.getDataQuality());
            vo.setAbnormalReason(checkin.getAbnormalReason());
            vo.setCreateTime(checkin.getCreateTime());
            vo.setUpdateTime(checkin.getUpdateTime());

            vo.setSleepCompleted(checkin.getSleepTime() != null && checkin.getWakeTime() != null);
            vo.setExerciseCompleted(checkin.getExerciseDuration() != null);
            vo.setDietCompleted(checkin.getDietBreakfast() != null
                    || checkin.getDietBalance() != null
                    || checkin.getWaterIntake() != null);
            vo.setEmotionCompleted(checkin.getEmotionType() != null || checkin.getMoodScore() != null);
        } else {
            vo.setDataQuality(0);
            vo.setSleepCompleted(false);
            vo.setExerciseCompleted(false);
            vo.setDietCompleted(false);
            vo.setEmotionCompleted(false);
        }

        if (user != null) {
            vo.setUserId(user.getId());
            if (ValidateUtils.hasText(user.getStudentNo())) {
                vo.setStudentNo(user.getStudentNo());
            }
            vo.setRealName(user.getRealName());
        }
        if (desensitize) {
            vo.setStudentNo(HealthConverter.maskStudentNo(vo.getStudentNo()));
            vo.setRealName(HealthConverter.maskName(vo.getRealName()));
        }

        vo.setStayUpLateName(stayUpLateName(vo.getStayUpLate()));
        vo.setSleepSourceName(sourceName(vo.getSleepSource()));
        vo.setExerciseIntensityName(exerciseIntensityName(vo.getExerciseIntensity()));
        vo.setExerciseSufficientName(yesNoNullName(vo.getExerciseSufficient()));
        vo.setExerciseSourceName(sourceName(vo.getExerciseSource()));
        vo.setDietBalanceName(dietBalanceName(vo.getDietBalance()));
        vo.setEmotionTypeName(emotionTypeName(vo.getEmotionType()));
        vo.setDataQualityName(dataQualityName(vo.getDataQuality()));
        return vo;
    }

    public static HealthCheckinPhotoVO toPhotoVO(HealthCheckinPhoto photo, String url) {
        if (photo == null) {
            return null;
        }
        HealthCheckinPhotoVO vo = new HealthCheckinPhotoVO();
        vo.setId(photo.getId());
        vo.setCheckinId(photo.getCheckinId());
        vo.setPhotoUrl(url);
        vo.setPhotoType(photo.getPhotoType());
        vo.setPhotoTypeName(photoTypeName(photo.getPhotoType()));
        vo.setCreateTime(photo.getCreateTime());
        return vo;
    }

    public static HealthCheckinSessionVO toSessionVO(HealthCheckinSession session) {
        if (session == null) {
            return null;
        }
        HealthCheckinSessionVO vo = new HealthCheckinSessionVO();
        vo.setId(session.getId());
        vo.setUserId(session.getUserId());
        vo.setStudentNo(session.getStudentNo());
        vo.setCheckinDate(session.getCheckinDate());
        vo.setSessionType(session.getSessionType());
        vo.setSessionTypeName(sessionTypeName(session.getSessionType()));
        vo.setStartTime(session.getStartTime());
        vo.setEndTime(session.getEndTime());
        vo.setDurationMinutes(session.getDurationMinutes());
        vo.setStatus(session.getStatus());
        vo.setStatusName(sessionStatusName(session.getStatus()));
        vo.setExerciseType(session.getExerciseType());
        vo.setExerciseIntensity(session.getExerciseIntensity());
        vo.setExerciseIntensityName(exerciseIntensityName(session.getExerciseIntensity()));
        vo.setDataQuality(session.getDataQuality() == null ? 0 : session.getDataQuality());
        vo.setDataQualityName(dataQualityName(vo.getDataQuality()));
        vo.setAbnormalReason(session.getAbnormalReason());
        vo.setCreateTime(session.getCreateTime());
        return vo;
    }

    public static String sourceName(Integer source) {
        if (source == null || source == 0) {
            return "未填写";
        }
        if (source == 1) {
            return "计时打卡";
        }
        if (source == 2) {
            return "手动填写";
        }
        return "未知";
    }

    public static String stayUpLateName(Integer value) {
        return yesNoNullName(value);
    }

    public static String yesNoNullName(Integer value) {
        if (value == null) {
            return "未填写";
        }
        return value == 1 ? "是" : "否";
    }

    public static String exerciseIntensityName(Integer intensity) {
        if (intensity == null) {
            return "未填写";
        }
        switch (intensity) {
            case 1:
                return "低";
            case 2:
                return "中";
            case 3:
                return "高";
            default:
                return "未知";
        }
    }

    public static String dietBalanceName(Integer balance) {
        if (balance == null) {
            return "未填写";
        }
        switch (balance) {
            case 1:
                return "不均衡";
            case 2:
                return "一般";
            case 3:
                return "均衡";
            default:
                return "未知";
        }
    }

    public static String emotionTypeName(Integer emotionType) {
        if (emotionType == null) {
            return "未填写";
        }
        switch (emotionType) {
            case 1:
                return "愉悦";
            case 2:
                return "平静";
            case 3:
                return "焦虑";
            case 4:
                return "低落";
            case 5:
                return "愤怒";
            default:
                return "未知";
        }
    }

    public static String dataQualityName(Integer dataQuality) {
        return Integer.valueOf(1).equals(dataQuality) ? "异常待复核" : "正常";
    }

    public static String photoTypeName(Integer photoType) {
        if (photoType == null) {
            return "未分类";
        }
        switch (photoType) {
            case 1:
                return "睡眠";
            case 2:
                return "运动";
            case 3:
                return "饮食";
            case 4:
                return "其他";
            default:
                return "未知";
        }
    }

    public static String sessionTypeName(Integer sessionType) {
        if (sessionType == null) {
            return "未知";
        }
        return sessionType == 1 ? "睡眠" : (sessionType == 2 ? "运动" : "未知");
    }

    public static String sessionStatusName(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case 0:
                return "进行中";
            case 1:
                return "已结束";
            case 2:
                return "已过期";
            default:
                return "未知";
        }
    }
}
