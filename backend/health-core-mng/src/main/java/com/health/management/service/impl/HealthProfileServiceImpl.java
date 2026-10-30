package com.health.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.health.management.common.constant.RoleConstants;
import com.health.management.common.exception.ServiceException;
import com.health.management.common.util.ValidateUtils;
import com.health.management.convert.HealthConverter;
import com.health.management.dto.HealthProfileQueryDTO;
import com.health.management.dto.HealthProfileSaveDTO;
import com.health.management.entity.HealthProfile;
import com.health.management.entity.SysUser;
import com.health.management.mapper.HealthProfileMapper;
import com.health.management.mapper.SysUserMapper;
import com.health.management.security.SecurityUtils;
import com.health.management.service.HealthProfileService;
import com.health.management.service.support.HealthUserSupport;
import com.health.management.util.HealthMetricUtils;
import com.health.management.vo.HealthProfileVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 健康档案业务实现。
 */
@Service
public class HealthProfileServiceImpl extends ServiceImpl<HealthProfileMapper, HealthProfile>
        implements HealthProfileService {

    @Autowired
    private HealthUserSupport healthUserSupport;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Override
    public HealthProfileVO getMyProfile() {
        SysUser user = healthUserSupport.requireStudent();
        HealthProfile profile = getByUserId(user.getId());
        return HealthConverter.toProfileVO(profile, user, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HealthProfileVO saveMyProfile(HealthProfileSaveDTO dto) {
        SysUser user = healthUserSupport.requireStudent();
        ValidateUtils.requireNotNull(dto, "请求参数");
        validateHeightWeight(dto.getHeight(), dto.getWeight());

        HealthProfile existing = getByUserId(user.getId());
        HealthProfile profile = existing == null ? new HealthProfile() : existing;
        profile.setUserId(user.getId());
        profile.setStudentNo(user.getStudentNo());
        profile.setRealName(user.getRealName());

        Integer gender = dto.getGender() != null
                ? dto.getGender()
                : (profile.getGender() != null ? profile.getGender() : user.getGender());
        profile.setGender(gender == null ? 0 : gender);
        profile.setBirthDate(dto.getBirthDate());
        profile.setAge(HealthMetricUtils.calcAge(dto.getBirthDate()));
        profile.setBloodType(trimToNull(dto.getBloodType()));
        profile.setHeight(dto.getHeight());
        profile.setWeight(dto.getWeight());
        profile.setBmi(HealthMetricUtils.calcBmi(dto.getHeight(), dto.getWeight()));
        profile.setCollege(trimToNull(dto.getCollege()));
        profile.setMajor(trimToNull(dto.getMajor()));
        profile.setGrade(trimToNull(dto.getGrade()));
        profile.setClassName(trimToNull(dto.getClassName()));
        profile.setChronicDisease(trimToNull(dto.getChronicDisease()));
        profile.setAllergyHistory(trimToNull(dto.getAllergyHistory()));
        profile.setEmergencyContact(trimToNull(dto.getEmergencyContact()));
        profile.setEmergencyPhone(trimToNull(dto.getEmergencyPhone()));
        profile.setRemark(trimToNull(dto.getRemark()));

        if (existing == null) {
            save(profile);
        } else {
            updateProfile(profile);
        }

        // 性别同时回写账号表，避免两处数据不一致
        if (dto.getGender() != null && !Objects.equals(user.getGender(), dto.getGender())) {
            SysUser userUpdate = new SysUser();
            userUpdate.setId(user.getId());
            userUpdate.setGender(dto.getGender());
            sysUserMapper.updateById(userUpdate);
            user.setGender(dto.getGender());
        }

        return HealthConverter.toProfileVO(profile, user, false);
    }

    @Override
    public IPage<HealthProfileVO> pageProfiles(HealthProfileQueryDTO query) {
        if (!SecurityUtils.isDoctorOrAdmin()) {
            throw ServiceException.forbidden("仅校医或超级管理员可查看学生健康档案");
        }
        if (query == null) {
            query = new HealthProfileQueryDTO();
        }
        ValidateUtils.requirePage(query.getPage(), query.getSize());
        int current = query.getPage() == null ? 1 : query.getPage();
        int size = query.getSize() == null ? 10 : query.getSize();

        LambdaQueryWrapper<HealthProfile> wrapper = new LambdaQueryWrapper<>();
        if (ValidateUtils.hasText(query.getStudentNo())) {
            wrapper.like(HealthProfile::getStudentNo, query.getStudentNo().trim());
        }
        if (ValidateUtils.hasText(query.getRealName())) {
            wrapper.like(HealthProfile::getRealName, query.getRealName().trim());
        }
        if (ValidateUtils.hasText(query.getCollege())) {
            wrapper.like(HealthProfile::getCollege, query.getCollege().trim());
        }
        if (ValidateUtils.hasText(query.getMajor())) {
            wrapper.like(HealthProfile::getMajor, query.getMajor().trim());
        }
        if (ValidateUtils.hasText(query.getGrade())) {
            wrapper.like(HealthProfile::getGrade, query.getGrade().trim());
        }
        if (ValidateUtils.hasText(query.getClassName())) {
            wrapper.like(HealthProfile::getClassName, query.getClassName().trim());
        }
        if (query.getGender() != null) {
            wrapper.eq(HealthProfile::getGender, query.getGender());
        }
        wrapper.orderByDesc(HealthProfile::getId);

        IPage<HealthProfile> page = page(new Page<>(current, size), wrapper);
        Map<Long, SysUser> userMap = loadUsers(page.getRecords());
        boolean desensitize = !SecurityUtils.hasRole(RoleConstants.SUPER_ADMIN);

        Page<HealthProfileVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<HealthProfileVO> records = new ArrayList<>();
        for (HealthProfile profile : page.getRecords()) {
            records.add(HealthConverter.toProfileVO(profile, userMap.get(profile.getUserId()), desensitize));
        }
        voPage.setRecords(records);
        return voPage;
    }

    @Override
    public HealthProfileVO getProfileDetail(Long userId) {
        if (!SecurityUtils.isDoctorOrAdmin()) {
            throw ServiceException.forbidden("仅校医或超级管理员可查看学生健康档案");
        }
        ValidateUtils.requireNotNull(userId, "用户ID");
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || !Integer.valueOf(RoleConstants.USER_TYPE_STUDENT).equals(user.getUserType())) {
            throw ServiceException.notFound("学生不存在");
        }
        HealthProfile profile = getByUserId(userId);
        if (profile == null) {
            throw ServiceException.notFound("该学生尚未建立健康档案");
        }
        boolean desensitize = !SecurityUtils.hasRole(RoleConstants.SUPER_ADMIN);
        return HealthConverter.toProfileVO(profile, user, desensitize);
    }

    private HealthProfile getByUserId(Long userId) {
        return getOne(new LambdaQueryWrapper<HealthProfile>()
                .eq(HealthProfile::getUserId, userId), false);
    }

    /**
     * 用显式 set 更新，保证用户可以把血型、病史等可选字段清空。
     */
    private void updateProfile(HealthProfile profile) {
        Long operatorId = SecurityUtils.getUserId();
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<HealthProfile> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(HealthProfile::getId, profile.getId())
                .set(HealthProfile::getStudentNo, profile.getStudentNo())
                .set(HealthProfile::getRealName, profile.getRealName())
                .set(HealthProfile::getGender, profile.getGender())
                .set(HealthProfile::getAge, profile.getAge())
                .set(HealthProfile::getBirthDate, profile.getBirthDate())
                .set(HealthProfile::getBloodType, profile.getBloodType())
                .set(HealthProfile::getHeight, profile.getHeight())
                .set(HealthProfile::getWeight, profile.getWeight())
                .set(HealthProfile::getBmi, profile.getBmi())
                .set(HealthProfile::getCollege, profile.getCollege())
                .set(HealthProfile::getMajor, profile.getMajor())
                .set(HealthProfile::getGrade, profile.getGrade())
                .set(HealthProfile::getClassName, profile.getClassName())
                .set(HealthProfile::getChronicDisease, profile.getChronicDisease())
                .set(HealthProfile::getAllergyHistory, profile.getAllergyHistory())
                .set(HealthProfile::getEmergencyContact, profile.getEmergencyContact())
                .set(HealthProfile::getEmergencyPhone, profile.getEmergencyPhone())
                .set(HealthProfile::getRemark, profile.getRemark())
                .set(HealthProfile::getUpdateBy, operatorId)
                .set(HealthProfile::getUpdateTime, now);
        update(wrapper);
        profile.setUpdateBy(operatorId);
        profile.setUpdateTime(now);
    }

    private Map<Long, SysUser> loadUsers(List<HealthProfile> profiles) {
        Map<Long, SysUser> userMap = new HashMap<>();
        if (profiles == null || profiles.isEmpty()) {
            return userMap;
        }
        List<Long> userIds = new ArrayList<>();
        for (HealthProfile profile : profiles) {
            if (profile.getUserId() != null) {
                userIds.add(profile.getUserId());
            }
        }
        if (userIds.isEmpty()) {
            return userMap;
        }
        List<SysUser> users = sysUserMapper.selectBatchIds(userIds);
        for (SysUser user : users) {
            userMap.put(user.getId(), user);
        }
        return userMap;
    }

    private void validateHeightWeight(BigDecimal height, BigDecimal weight) {
        if ((height == null) != (weight == null)) {
            throw ServiceException.badRequest("身高和体重需同时填写，才能计算BMI");
        }
        if (height != null && (height.compareTo(new BigDecimal("50")) < 0
                || height.compareTo(new BigDecimal("250")) > 0)) {
            throw ServiceException.badRequest("身高应在50-250cm之间");
        }
        if (weight != null && (weight.compareTo(new BigDecimal("10")) < 0
                || weight.compareTo(new BigDecimal("500")) > 0)) {
            throw ServiceException.badRequest("体重应在10-500kg之间");
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
