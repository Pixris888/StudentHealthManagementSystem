package com.health.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.health.management.common.exception.ServiceException;
import com.health.management.common.util.ValidateUtils;
import com.health.management.config.CheckinProperties;
import com.health.management.convert.HealthCheckinConverter;
import com.health.management.entity.HealthCheckin;
import com.health.management.entity.HealthCheckinPhoto;
import com.health.management.entity.SysUser;
import com.health.management.mapper.HealthCheckinMapper;
import com.health.management.mapper.HealthCheckinPhotoMapper;
import com.health.management.service.HealthCheckinPhotoService;
import com.health.management.service.support.HealthUserSupport;
import com.health.management.storage.FileStorageService;
import com.health.management.storage.ImageWatermarkService;
import com.health.management.vo.HealthCheckinPhotoVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 打卡照片业务实现：校验 → 水印 → 存储 → 落库。
 */
@Service
public class HealthCheckinPhotoServiceImpl extends ServiceImpl<HealthCheckinPhotoMapper, HealthCheckinPhoto>
        implements HealthCheckinPhotoService {

    private static final long MAX_PHOTO_SIZE = 5L * 1024 * 1024;
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter WATERMARK_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private HealthUserSupport healthUserSupport;

    @Autowired
    private HealthCheckinMapper healthCheckinMapper;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private ImageWatermarkService imageWatermarkService;

    @Autowired
    private CheckinProperties checkinProperties;

    @Override
    public HealthCheckinPhotoVO upload(Long checkinId, MultipartFile file, Integer photoType) {
        SysUser user = healthUserSupport.requireStudent();
        ValidateUtils.requireNotNull(checkinId, "打卡记录ID");
        HealthCheckin checkin = healthCheckinMapper.selectById(checkinId);
        if (checkin == null || !user.getId().equals(checkin.getUserId())) {
            throw ServiceException.notFound("打卡记录不存在");
        }
        validateFile(file);
        int type = photoType == null ? 1 : photoType;
        if (type < 1 || type > 4) {
            throw ServiceException.badRequest("照片类型只能是1-睡眠、2-运动、3-饮食、4-其他");
        }
        if (countByCheckin(checkinId) >= checkinProperties.getMaxPhotos()) {
            throw ServiceException.badRequest("每条打卡最多上传" + checkinProperties.getMaxPhotos() + "张照片");
        }

        String format = resolveFormat(file.getOriginalFilename(), file.getContentType());
        byte[] original;
        try {
            original = file.getBytes();
        } catch (IOException e) {
            throw ServiceException.fail("读取照片失败");
        }
        String watermarkText = "UID:" + user.getId() + "  "
                + LocalDateTime.now(ZONE).format(WATERMARK_TIME) + "  HEALTH-CHECKIN";
        byte[] watermarked = imageWatermarkService.watermark(original, watermarkText, format);

        String contentDir = "health/checkin/" + user.getId();
        String contentType = "png".equals(format) ? "image/png" : "image/jpeg";
        String objectKey = fileStorageService.upload(
                watermarked, file.getOriginalFilename(), contentType, contentDir);

        HealthCheckinPhoto photo = new HealthCheckinPhoto();
        photo.setCheckinId(checkinId);
        photo.setUserId(user.getId());
        photo.setPhotoUrl(objectKey);
        photo.setPhotoType(type);
        try {
            save(photo);
        } catch (RuntimeException e) {
            fileStorageService.delete(objectKey);
            throw e;
        }
        return HealthCheckinConverter.toPhotoVO(photo, fileStorageService.getUrl(objectKey));
    }

    @Override
    public List<HealthCheckinPhotoVO> listByCheckin(Long checkinId) {
        List<HealthCheckinPhotoVO> result = new ArrayList<>();
        if (checkinId == null) {
            return result;
        }
        SysUser user = healthUserSupport.requireStudent();
        HealthCheckin checkin = healthCheckinMapper.selectById(checkinId);
        if (checkin == null || !user.getId().equals(checkin.getUserId())) {
            throw ServiceException.notFound("打卡记录不存在");
        }
        List<HealthCheckinPhoto> photos = list(new LambdaQueryWrapper<HealthCheckinPhoto>()
                .eq(HealthCheckinPhoto::getCheckinId, checkinId)
                .orderByAsc(HealthCheckinPhoto::getId));
        for (HealthCheckinPhoto photo : photos) {
            result.add(HealthCheckinConverter.toPhotoVO(photo, fileStorageService.getUrl(photo.getPhotoUrl())));
        }
        return result;
    }

    @Override
    public void delete(Long photoId) {
        SysUser user = healthUserSupport.requireStudent();
        ValidateUtils.requireNotNull(photoId, "照片ID");
        HealthCheckinPhoto photo = getById(photoId);
        if (photo == null || !user.getId().equals(photo.getUserId())) {
            throw ServiceException.notFound("照片不存在");
        }
        removeById(photoId);
        fileStorageService.delete(photo.getPhotoUrl());
    }

    @Override
    public int countByCheckin(Long checkinId) {
        if (checkinId == null) {
            return 0;
        }
        Long count = baseMapper.selectCount(new LambdaQueryWrapper<HealthCheckinPhoto>()
                .eq(HealthCheckinPhoto::getCheckinId, checkinId));
        return count == null ? 0 : count.intValue();
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ServiceException.badRequest("请选择要上传的照片");
        }
        if (file.getSize() > MAX_PHOTO_SIZE) {
            throw ServiceException.badRequest("单张照片不能超过5MB");
        }
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw ServiceException.badRequest("文件名不能为空");
        }
        String lower = filename.toLowerCase();
        if (!lower.endsWith(".jpg") && !lower.endsWith(".jpeg") && !lower.endsWith(".png")) {
            throw ServiceException.badRequest("仅支持 JPG/PNG 格式的照片");
        }
        String contentType = file.getContentType();
        if (ValidateUtils.hasText(contentType)) {
            String type = contentType.toLowerCase();
            if (!"image/jpeg".equals(type) && !"image/jpg".equals(type) && !"image/png".equals(type)) {
                throw ServiceException.badRequest("照片 Content-Type 不正确");
            }
        }
    }

    private String resolveFormat(String filename, String contentType) {
        String lowerName = filename == null ? "" : filename.toLowerCase();
        String lowerType = contentType == null ? "" : contentType.toLowerCase();
        return lowerName.endsWith(".png") || lowerType.contains("png") ? "png" : "jpg";
    }
}
