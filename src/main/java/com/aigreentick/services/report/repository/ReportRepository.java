package com.aigreentick.services.report.repository;

import com.aigreentick.services.report.dto.BillingReportDTO;
import com.aigreentick.services.report.dto.CampaignDeliveryReportDTO;
import com.aigreentick.services.report.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    @Query("""
        select new com.aigreentick.services.report.dto.CampaignDeliveryReportDTO(
            c.id,
            c.campname,

            count(r.id),

            coalesce(sum(case when r.messageStatus = 'sent' then 1 else 0 end), 0),
            coalesce(sum(case when r.messageStatus = 'delivered' then 1 else 0 end), 0),
            coalesce(sum(case when r.messageStatus = 'failed' then 1 else 0 end), 0),
            coalesce(sum(case 
                when r.messageStatus is null 
                  or r.messageStatus = 'pending' 
                then 1 else 0 end), 0)
        )
        from Report r
        join r.campaign c
        where c.userId = :userId
          and r.deletedAt is null
          and r.createdAt between :from and :to
        group by c.id, c.campname
    """)
    List<CampaignDeliveryReportDTO> findCampaignDeliveryReport(
            Long userId,
            LocalDateTime from,
            LocalDateTime to
    );

    @Query("""
        select new com.aigreentick.services.report.dto.BillingReportDTO(
            u.id,
            u.name,

            coalesce(sum(case 
                when r.messageStatus = 'delivered' then 1 else 0 end), 0),

            u.marketMsgCharge,

            coalesce(sum(case 
                when r.messageStatus = 'delivered' then 1 else 0 end), 0) 
            * u.marketMsgCharge
        )
        from Report r
        join r.user u
        where r.deletedAt is null
          and r.createdAt between :from and :to
        group by u.id, u.name, u.marketMsgCharge
    """)
    List<BillingReportDTO> findBillingReport(
            LocalDateTime from,
            LocalDateTime to
    );
}
