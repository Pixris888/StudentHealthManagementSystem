package com.health.management.controller.health;

import com.health.management.service.HealthCheckinPhotoService;
import com.health.management.vo.HealthCheckinPhotoVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 打卡照片接口：上传（服务端加水印）、查询、删除。
 * 照片仅本人可见；校医/管理员查询打卡详情时不返回照片。
 */
@RestController
@RequestMapping("/api/health/checkin")
public class HealthCheckinPhotoController {

    @Autowired
    private HealthCheckinPhotoService healthCheckinPhotoService;

    /**
     * 上传打卡照片。multipart 字段名 file，photoType 默认 1-睡眠。
     */
    @PostMapping("/{checkinId}/photos")
    public HealthCheckinPhotoVO upload(@PathVariable Long checkinId,
                                       @RequestParam("file") MultipartFile file,
                                       @RequestParam(value = "photoType", required = false, defaultValue = "1")
                                       Integer photoType) {
        return healthCheckinPhotoService.upload(checkinId, file, photoType);
    }

    /**
     * 查询某条打卡的照片（仅本人）。
     */
    @GetMapping("/{checkinId}/photos")
    public List<HealthCheckinPhotoVO> list(@PathVariable Long checkinId) {
        return healthCheckinPhotoService.listByCheckin(checkinId);
    }

    /**
     * 删除本人照片。
     */
    @DeleteMapping("/photos/{photoId}")
    public boolean delete(@PathVariable Long photoId) {
        healthCheckinPhotoService.delete(photoId);
        return true;
    }
}
