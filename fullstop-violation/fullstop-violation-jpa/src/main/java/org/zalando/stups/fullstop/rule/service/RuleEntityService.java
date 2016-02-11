package org.zalando.stups.fullstop.rule.service;

import org.zalando.stups.fullstop.rule.entity.RuleDTO;
import org.zalando.stups.fullstop.rule.entity.RuleEntity;

import java.util.List;

public interface RuleEntityService {

    RuleEntity save(RuleDTO ruleDTO);

    RuleEntity update(RuleDTO ruleDTO, Long id);

    RuleEntity findById(Long id);

    List<RuleEntity> findByNotExpired();

    List<RuleEntity> findAll();
}
