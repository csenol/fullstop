package org.zalando.fullstop.violation.persist.jpa;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.metrics.CounterService;
import org.zalando.stups.fullstop.violation.Violation;
import org.zalando.stups.fullstop.violation.entity.ApplicationEntity;
import org.zalando.stups.fullstop.violation.entity.VersionEntity;
import org.zalando.stups.fullstop.violation.entity.ViolationEntity;
import org.zalando.stups.fullstop.violation.entity.ViolationTypeEntity;
import org.zalando.stups.fullstop.violation.reactor.EventBusViolationHandler;
import org.zalando.stups.fullstop.violation.repository.ApplicationRepository;
import org.zalando.stups.fullstop.violation.repository.VersionRepository;
import org.zalando.stups.fullstop.violation.repository.ViolationRepository;
import org.zalando.stups.fullstop.violation.repository.ViolationTypeRepository;
import org.zalando.stups.fullstop.whitelist.WhitelistRules;
import reactor.bus.EventBus;

import java.util.List;

/**
 * @author jbellmann
 */
public class ViolationJpaPersister extends EventBusViolationHandler {

    private static final String VIOLATIONS_EVENTBUS_QUEUED = "violations.eventbus.queued";

    private static final String VIOLATIONS_PERSISTED_JPA = "violations.persisted.jpa";

    private final Logger log = LoggerFactory.getLogger(ViolationJpaPersister.class);

    private final ViolationRepository violationRepository;

    private final ViolationTypeRepository violationTypeRepository;

    private final ApplicationRepository applicationRepository;

    private final VersionRepository versionRepository;

    private final CounterService counterService;

    private final WhitelistRules whitelistRules;

    public ViolationJpaPersister(final EventBus eventBus, final ViolationRepository violationRepository,
                                 final ViolationTypeRepository violationTypeRepository,
                                 final CounterService counterService, final WhitelistRules whitelistRules,
                                 final ApplicationRepository applicationRepository, final VersionRepository versionRepository) {
        super(eventBus);
        this.violationRepository = violationRepository;
        this.violationTypeRepository = violationTypeRepository;
        this.counterService = counterService;
        this.whitelistRules = whitelistRules;
        this.applicationRepository = applicationRepository;
        this.versionRepository = versionRepository;
    }

    protected ViolationEntity buildViolationEntity(final Violation violation) {

        if (violation == null || violation.getViolationType() == null) {
            log.warn("Violation/Violation-Type must not be null!");
            return null;
        }


        final ApplicationEntity application = createApplication(violation.getApplicationId());
        final VersionEntity version = createVersion(violation.getApplicationVersion());
        final List<VersionEntity> versionEntities = application.getVersionEntities();
        versionEntities.add(version);
        application.setVersionEntities(versionEntities);
        applicationRepository.save(application);



        final String violationTypeId = violation.getViolationType();

        final ViolationEntity entity = new ViolationEntity();
        entity.setApplication(application);
        entity.setApplicationVersion(version);
        entity.setAccountId(violation.getAccountId());
        entity.setEventId(violation.getEventId());
        entity.setInstanceId(violation.getInstanceId());

        entity.setPluginFullyQualifiedClassName(violation.getPluginFullyQualifiedClassName());

        entity.setUsername(violation.getUsername());

        final ViolationTypeEntity violationTypeEntity = violationTypeRepository.findOne(violationTypeId);

        if (violationTypeEntity != null) {
            entity.setViolationTypeEntity(violationTypeEntity);
        } else {
            final ViolationTypeEntity vte = new ViolationTypeEntity();
            vte.setId(violationTypeId);
            vte.setViolationSeverity(0);
            vte.setIsAuditRelevant(false);
            vte.setHelpText("This is only a default message");

            final ViolationTypeEntity savedViolationTypeEntity = violationTypeRepository.save(vte);

            entity.setViolationTypeEntity(savedViolationTypeEntity);
        }

        entity.setMetaInfo(violation.getMetaInfo());

        entity.setRegion(violation.getRegion());

        whitelistRules.execute(entity);

        return entity;
    }

    private VersionEntity createVersion(final String applicationVersion) {
        final VersionEntity version = versionRepository.findByName(applicationVersion);
        if (version == null) {
            return versionRepository.save(new VersionEntity(applicationVersion));
        }
        return version;
    }

    private ApplicationEntity createApplication(final String applicationId) {
        final ApplicationEntity application = applicationRepository.findByName(applicationId);
        if (application == null) {
            return applicationRepository.save(new ApplicationEntity(applicationId));
        }
        return application;
    }

    @Override
    public void handleViolation(final Violation violation) {
        this.counterService.decrement(VIOLATIONS_EVENTBUS_QUEUED);

        if (violationRepository.violationExists(violation.getAccountId(), violation.getRegion(), violation.getEventId(), violation.getInstanceId(), violation.getViolationType())) {
            log.debug("Violation {} does already exist", violation);
        } else {
            violationRepository.saveAndFlush(buildViolationEntity(violation));
        }

        this.counterService.increment(VIOLATIONS_PERSISTED_JPA);
    }

}
