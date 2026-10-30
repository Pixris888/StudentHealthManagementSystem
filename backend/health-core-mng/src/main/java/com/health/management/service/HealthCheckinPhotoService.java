package com.health.management.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.health.management.entity.HealthCheckinPhoto;
import com.health.management.vo.HealthCheckinPhotoVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 打卡照片业务接口。
 */
public interface HealthCheckinPhotoService extends IService<HealthCheckinPhoto> {

    /** 上传照片：校验、加水印、存储并保存记录 */
    HealthCheckinPhotoVO upload(Long checkinId, MultipartFile file, Integer photoType);

    /** 查询某条打卡的照片（仅本人调用） */
    List<HealthCheckinPhotoVO> listByCheckin(Long checkinId);

    /** 删除本人照片 */
    void delete(Long photoId);

    /** 统计照片数量 */
    int countByCheckin(Long checkinId);
}
