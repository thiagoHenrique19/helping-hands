package com.helpinghands.infrastructure.persistence;

import com.helpinghands.core.domain.Campaign;
import com.helpinghands.core.port.in.dto.CampaignSearchCriteria;
import org.springframework.data.jpa.domain.Specification;

final class CampaignSpecifications {

    private CampaignSpecifications() {
    }

    static Specification<Campaign> from(CampaignSearchCriteria criteria) {
        return Specification.where(belongsToOrganization(criteria.organizationId()))
                .and(hasStatus(criteria))
                .and(hasCategory(criteria))
                .and(titleContains(criteria.title()));
    }

    private static Specification<Campaign> belongsToOrganization(Long organizationId) {
        return (root, query, builder) -> organizationId == null ? null
                : builder.equal(root.get("organization").get("id"), organizationId);
    }

    private static Specification<Campaign> hasStatus(CampaignSearchCriteria criteria) {
        return (root, query, builder) -> criteria.status() == null ? null
                : builder.equal(root.get("status"), criteria.status());
    }

    private static Specification<Campaign> hasCategory(CampaignSearchCriteria criteria) {
        return (root, query, builder) -> criteria.category() == null ? null
                : builder.equal(root.get("category"), criteria.category());
    }

    private static Specification<Campaign> titleContains(String title) {
        return (root, query, builder) -> {
            if (title == null || title.isBlank()) {
                return null;
            }
            String escaped = title.trim().toLowerCase()
                    .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
            return builder.like(builder.lower(root.get("title")), "%" + escaped + "%", '\\');
        };
    }
}
