package com.aigreentick.services.report.repository;

import com.aigreentick.services.report.entity.Template;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemplateRepository extends JpaRepository<Template, Long> {
}
