package SITE.RECIPICK.RECIPICK_PROJECT.repository;

import SITE.RECIPICK.RECIPICK_PROJECT.entity.ReportEntity;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.ReportStatus;
import SITE.RECIPICK.RECIPICK_PROJECT.entity.ReportTargetType;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReportRepository extends JpaRepository<ReportEntity, Integer> {

  long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end); // 오늘 접수 건수

  long countByStatus(ReportStatus status); // 미처리 건수

  List<ReportEntity> findByStatusAndTargetTypeOrderByCreatedAtDesc(
      ReportStatus status, ReportTargetType type, Pageable pageable);

  @Query(
      """
            select r.targetId as targetId, count(r.reportid) as cnt
              from ReportEntity r
             where r.targetType = SITE.RECIPICK.RECIPICK_PROJECT.entity.ReportTargetType.POST
               and r.createdAt between :from and :to
             group by r.targetId
             order by count(r.reportid) desc
          """)
  List<TopAgg> topReportedPosts(LocalDateTime from, LocalDateTime to, Pageable pageable);

  Page<ReportEntity> findByStatus(ReportStatus status, Pageable pageable);

  Page<ReportEntity> findByStatusAndTargetType(
      ReportStatus status, ReportTargetType type, Pageable pageable);

  // 최근 신고 N개
  @Query("""
      select r
      from ReportEntity r
      order by r.createdAt desc
      """)
  List<ReportEntity> findTopNByOrderByCreatedAtDesc(Pageable pageable);

  default List<ReportEntity> findTopNByOrderByCreatedAtDesc(int top) {
    return findTopNByOrderByCreatedAtDesc(PageRequest.of(0, Math.max(1, top)));
  }

  interface TopAgg {

    Integer getTargetId();

    Integer getCnt();
  }
}
