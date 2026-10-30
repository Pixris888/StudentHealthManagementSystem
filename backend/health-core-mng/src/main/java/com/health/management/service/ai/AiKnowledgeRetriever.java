package com.health.management.service.ai;

import com.health.management.common.util.ValidateUtils;
import com.health.management.config.AiProperties;
import com.health.management.entity.HealthKnowledgeMaterial;
import com.health.management.service.HealthKnowledgeMaterialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 轻量级知识召回。
 *
 * 当前知识素材规模很小（几十条以内），不引入向量库：
 * 1. 取全部“审核通过 + 启用”的素材；
 * 2. 用关键词命中 + 字符 bigram 重叠打分；
 * 3. 取 Top N，低于阈值的素材不返回，避免强行凑答案。
 */
@Component
public class AiKnowledgeRetriever {

    private static final int MAX_MATERIALS = 50;

    @Autowired
    private HealthKnowledgeMaterialService healthKnowledgeMaterialService;

    @Autowired
    private AiProperties aiProperties;

    public List<AiRetrievedMaterial> retrieve(String question, Integer limit) {
        if (!ValidateUtils.hasText(question)) {
            return Collections.emptyList();
        }
        int defaultLimit = aiProperties.getRetrieveLimit() == null
                ? 4 : aiProperties.getRetrieveLimit();
        int safeLimit = limit == null ? defaultLimit : limit;
        if (safeLimit < 1) {
            safeLimit = 1;
        }
        if (safeLimit > 10) {
            safeLimit = 10;
        }

        List<HealthKnowledgeMaterial> materials =
                healthKnowledgeMaterialService.findUsableMaterials(null, MAX_MATERIALS);
        if (materials == null || materials.isEmpty()) {
            return Collections.emptyList();
        }

        String normalizedQuestion = normalize(question);
        Set<String> questionGrams = bigrams(normalizedQuestion);
        List<AiRetrievedMaterial> scored = new ArrayList<>();
        for (HealthKnowledgeMaterial material : materials) {
            int score = score(material, normalizedQuestion, questionGrams);
            if (score > 0) {
                scored.add(new AiRetrievedMaterial(material, score));
            }
        }
        if (scored.isEmpty()) {
            return Collections.emptyList();
        }

        Collections.sort(scored, new Comparator<AiRetrievedMaterial>() {
            @Override
            public int compare(AiRetrievedMaterial left, AiRetrievedMaterial right) {
                return Integer.compare(right.getScore(), left.getScore());
            }
        });
        if (scored.size() > safeLimit) {
            return new ArrayList<>(scored.subList(0, safeLimit));
        }
        return scored;
    }

    private int score(HealthKnowledgeMaterial material, String normalizedQuestion, Set<String> questionGrams) {
        String title = normalize(material.getTitle());
        String keyword = normalize(material.getKeyword());
        String content = normalize(material.getContent());

        int score = 0;
        // 关键词字段是人工维护的强信号
        if (ValidateUtils.hasText(material.getKeyword())) {
            String[] keywords = material.getKeyword().split("[,，;；]");
            for (String item : keywords) {
                String normalizedKeyword = normalize(item);
                if (normalizedKeyword.length() >= 2 && normalizedQuestion.contains(normalizedKeyword)) {
                    score += 8;
                }
            }
        }

        for (String gram : questionGrams) {
            if (title.contains(gram)) {
                score += 3;
            }
            if (keyword.contains(gram)) {
                score += 3;
            }
            if (content.contains(gram)) {
                score += 1;
            }
        }
        return score;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase()
                .replaceAll("[\\p{Punct}\\s，。！？、；：“”‘’（）《》【】…—]+", "");
    }

    private Set<String> bigrams(String value) {
        Set<String> grams = new HashSet<>();
        if (value == null || value.length() == 0) {
            return grams;
        }
        if (value.length() == 1) {
            grams.add(value);
            return grams;
        }
        for (int i = 0; i < value.length() - 1; i++) {
            grams.add(value.substring(i, i + 2));
        }
        return grams;
    }
}
