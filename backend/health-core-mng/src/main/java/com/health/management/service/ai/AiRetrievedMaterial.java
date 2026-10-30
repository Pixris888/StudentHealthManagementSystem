package com.health.management.service.ai;

import com.health.management.entity.HealthKnowledgeMaterial;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 知识素材召回结果。
 */
@Data
@AllArgsConstructor
public class AiRetrievedMaterial {

    private HealthKnowledgeMaterial material;

    private int score;
}
