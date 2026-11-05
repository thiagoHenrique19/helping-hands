package com.helpinghands.infrastructure.persistence;

import com.helpinghands.core.domain.CampaignStatus;
import com.helpinghands.core.domain.PaymentMethod;
import com.helpinghands.core.port.in.dto.analytics.CampaignProgressRank;
import com.helpinghands.core.port.in.dto.analytics.MonthlyDonations;
import com.helpinghands.core.port.in.dto.analytics.OrganizationDashboard;
import com.helpinghands.core.port.in.dto.analytics.PaymentMethodBreakdown;
import com.helpinghands.core.port.in.dto.analytics.PlatformSummary;
import com.helpinghands.core.port.in.dto.analytics.TopCampaign;
import com.helpinghands.core.port.out.ImpactAnalyticsRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcImpactAnalyticsRepository implements ImpactAnalyticsRepository {

    private static final String ORGANIZATION_DASHBOARD_SQL = """
            WITH donation_stats AS (
                SELECT COALESCE(SUM(d.amount), 0)           AS total_raised,
                       COUNT(d.id)                          AS donations_count,
                       COUNT(DISTINCT lower(d.donor_email)) AS unique_donors
                FROM donation d
                JOIN campaign c ON c.id = d.campaign_id
                WHERE c.organization_id = :organizationId
            ), campaign_stats AS (
                SELECT COUNT(*) FILTER (WHERE c.status = 'ACTIVE')    AS active_campaigns,
                       COUNT(*) FILTER (WHERE c.status = 'COMPLETED') AS completed_campaigns
                FROM campaign c
                WHERE c.organization_id = :organizationId
            ), opportunity_stats AS (
                SELECT COUNT(*) FILTER (WHERE o.status = 'OPEN') AS open_opportunities
                FROM volunteer_opportunity o
                WHERE o.organization_id = :organizationId
            ), volunteer_stats AS (
                SELECT COUNT(*) AS approved_volunteers
                FROM volunteer_application a
                JOIN volunteer_opportunity o ON o.id = a.opportunity_id
                WHERE o.organization_id = :organizationId
                  AND a.status = 'APPROVED'
            )
            SELECT * FROM donation_stats, campaign_stats, opportunity_stats, volunteer_stats
            """;

    private static final String PLATFORM_SUMMARY_SQL = """
            SELECT (SELECT COUNT(*) FROM organization)                                    AS organizations,
                   (SELECT COUNT(*) FROM organization WHERE verified)                     AS verified_organizations,
                   (SELECT COUNT(*) FROM campaign)                                        AS campaigns,
                   (SELECT COUNT(*) FROM campaign WHERE status = 'ACTIVE')                AS active_campaigns,
                   (SELECT COALESCE(SUM(amount), 0) FROM donation)                        AS total_raised,
                   (SELECT COUNT(*) FROM donation)                                        AS donations_count,
                   (SELECT COUNT(DISTINCT lower(donor_email)) FROM donation)              AS unique_donors,
                   (SELECT COUNT(*) FROM volunteer_opportunity WHERE status = 'OPEN')     AS open_opportunities,
                   (SELECT COUNT(*) FROM volunteer_application WHERE status = 'APPROVED') AS approved_volunteers
            """;

    private static final String TOP_CAMPAIGNS_SQL = """
            SELECT c.id, c.organization_id, c.title, c.raised_amount, c.goal_amount
            FROM campaign c
            WHERE c.raised_amount > 0
              AND (CAST(:organizationId AS BIGINT) IS NULL OR c.organization_id = :organizationId)
            ORDER BY c.raised_amount DESC, c.id
            LIMIT :limit
            """;

    private static final String DONATIONS_BY_MONTH_SQL = """
            WITH months AS (
                SELECT generate_series(date_trunc('month', CAST(:from AS timestamp)),
                                       date_trunc('month', CAST(:to AS timestamp)),
                                       interval '1 month') AS month_start
            ), donations_in_range AS (
                SELECT d.id,
                       d.amount,
                       date_trunc('month', d.created_at AT TIME ZONE 'UTC') AS month_start
                FROM donation d
                JOIN campaign c ON c.id = d.campaign_id
                WHERE c.organization_id = :organizationId
                  AND CAST(d.created_at AT TIME ZONE 'UTC' AS date) BETWEEN CAST(:from AS date) AND CAST(:to AS date)
            )
            SELECT CAST(m.month_start AS date)  AS month,
                   COUNT(r.id)                  AS donations_count,
                   COALESCE(SUM(r.amount), 0)   AS total_amount
            FROM months m
            LEFT JOIN donations_in_range r ON r.month_start = m.month_start
            GROUP BY m.month_start
            ORDER BY m.month_start
            """;

    private static final String CAMPAIGN_PROGRESS_SQL = """
            SELECT CAST(rank() OVER (ORDER BY LEAST(c.raised_amount / c.goal_amount, 1) DESC) AS integer) AS campaign_rank,
                   c.id,
                   c.title,
                   c.status,
                   c.raised_amount,
                   c.goal_amount,
                   ROUND(LEAST(c.raised_amount * 100 / c.goal_amount, 100), 2) AS progress_percentage
            FROM campaign c
            WHERE c.organization_id = :organizationId
            ORDER BY campaign_rank, c.id
            """;

    private static final String PAYMENT_METHOD_SQL = """
            SELECT d.payment_method,
                   COUNT(*)                      AS donations_count,
                   COALESCE(SUM(d.amount), 0)    AS total_amount
            FROM donation d
            JOIN campaign c ON c.id = d.campaign_id
            WHERE CAST(:organizationId AS BIGINT) IS NULL OR c.organization_id = :organizationId
            GROUP BY d.payment_method
            ORDER BY total_amount DESC, d.payment_method
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    JdbcImpactAnalyticsRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public OrganizationDashboard organizationDashboard(Long organizationId) {
        return jdbcTemplate.queryForObject(ORGANIZATION_DASHBOARD_SQL,
                new MapSqlParameterSource("organizationId", organizationId),
                (rs, rowNum) -> new OrganizationDashboard(organizationId,
                        rs.getBigDecimal("total_raised"),
                        rs.getLong("donations_count"),
                        rs.getLong("unique_donors"),
                        rs.getLong("active_campaigns"),
                        rs.getLong("completed_campaigns"),
                        rs.getLong("open_opportunities"),
                        rs.getLong("approved_volunteers")));
    }

    @Override
    public PlatformSummary platformSummary() {
        return jdbcTemplate.queryForObject(PLATFORM_SUMMARY_SQL, new MapSqlParameterSource(),
                (rs, rowNum) -> new PlatformSummary(
                        rs.getLong("organizations"),
                        rs.getLong("verified_organizations"),
                        rs.getLong("campaigns"),
                        rs.getLong("active_campaigns"),
                        rs.getBigDecimal("total_raised"),
                        rs.getLong("donations_count"),
                        rs.getLong("unique_donors"),
                        rs.getLong("open_opportunities"),
                        rs.getLong("approved_volunteers")));
    }

    @Override
    public List<TopCampaign> topCampaigns(Long organizationId, int limit) {
        var parameters = new MapSqlParameterSource()
                .addValue("organizationId", organizationId)
                .addValue("limit", limit);
        return jdbcTemplate.query(TOP_CAMPAIGNS_SQL, parameters,
                (rs, rowNum) -> new TopCampaign(rs.getLong("id"), rs.getLong("organization_id"),
                        rs.getString("title"), rs.getBigDecimal("raised_amount"), rs.getBigDecimal("goal_amount")));
    }

    @Override
    public List<MonthlyDonations> donationsByMonth(Long organizationId, LocalDate from, LocalDate to) {
        var parameters = new MapSqlParameterSource()
                .addValue("organizationId", organizationId)
                .addValue("from", from)
                .addValue("to", to);
        return jdbcTemplate.query(DONATIONS_BY_MONTH_SQL, parameters,
                (rs, rowNum) -> new MonthlyDonations(rs.getObject("month", LocalDate.class),
                        rs.getLong("donations_count"), rs.getBigDecimal("total_amount")));
    }

    @Override
    public List<CampaignProgressRank> campaignProgressRanking(Long organizationId) {
        return jdbcTemplate.query(CAMPAIGN_PROGRESS_SQL, new MapSqlParameterSource("organizationId", organizationId),
                (rs, rowNum) -> new CampaignProgressRank(rs.getInt("campaign_rank"), rs.getLong("id"),
                        rs.getString("title"), CampaignStatus.valueOf(rs.getString("status")),
                        rs.getBigDecimal("raised_amount"), rs.getBigDecimal("goal_amount"),
                        rs.getBigDecimal("progress_percentage")));
    }

    @Override
    public List<PaymentMethodBreakdown> donationsByPaymentMethod(Long organizationId) {
        return jdbcTemplate.query(PAYMENT_METHOD_SQL, new MapSqlParameterSource("organizationId", organizationId),
                (rs, rowNum) -> new PaymentMethodBreakdown(PaymentMethod.valueOf(rs.getString("payment_method")),
                        rs.getLong("donations_count"), rs.getBigDecimal("total_amount")));
    }
}
