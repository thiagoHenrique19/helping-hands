package com.helpinghands.core.port.out;

import com.helpinghands.core.domain.VolunteerOpportunity;
import com.helpinghands.core.pagination.PageQuery;
import com.helpinghands.core.pagination.PageResult;
import com.helpinghands.core.port.in.dto.OpportunitySearchCriteria;
import java.time.Instant;
import java.util.Optional;

public interface VolunteerOpportunityRepository {

    VolunteerOpportunity save(VolunteerOpportunity opportunity);

    Optional<VolunteerOpportunity> findById(Long id);

    /** Locks the row and re-reads it, so slot counters are never stale even if it was loaded earlier. */
    Optional<VolunteerOpportunity> findByIdForUpdate(Long id);

    PageResult<VolunteerOpportunity> search(OpportunitySearchCriteria criteria, Instant now, PageQuery pageQuery);
}
