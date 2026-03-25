package com.group5.paul_esys.modules.sections.services;

import com.group5.paul_esys.modules.sections.model.Section;
import com.group5.paul_esys.modules.sections.utils.SectionUtils;
import com.group5.paul_esys.modules.users.services.ConnectionService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SectionService {

  private final Connection conn = ConnectionService.getConnection();
  private final Logger logger = LoggerFactory.getLogger(SectionService.class);

  public List<Section> getAllSections() {
    List<Section> sections = new ArrayList<>();
    try {
      PreparedStatement ps = conn.prepareStatement("SELECT * FROM sections ORDER BY section_code");
      ResultSet rs = ps.executeQuery();
      
      while (rs.next()) {
        sections.add(SectionUtils.mapResultSetToSection(rs));
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }
    return sections;
  }

  public Optional<Section> getSectionById(Long id) {
    try {
      PreparedStatement ps = conn.prepareStatement("SELECT * FROM sections WHERE id = ?");
      ps.setLong(1, id);
      
      ResultSet rs = ps.executeQuery();
      if (rs.next()) {
        return Optional.of(SectionUtils.mapResultSetToSection(rs));
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }
    return Optional.empty();
  }

  public List<Section> getSectionsBySubject(Long subjectId) {
    List<Section> sections = new ArrayList<>();
    try {
      PreparedStatement ps = conn.prepareStatement("SELECT * FROM sections WHERE subject_id = ? ORDER BY section_code");
      ps.setLong(1, subjectId);
      
      ResultSet rs = ps.executeQuery();
      while (rs.next()) {
        sections.add(SectionUtils.mapResultSetToSection(rs));
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }
    return sections;
  }

  public boolean createSection(Section section) {
    try {
      PreparedStatement ps = conn.prepareStatement(
          "INSERT INTO sections (section_name, section_code, subject_id, capacity) VALUES (?, ?, ?, ?)"
      );
      ps.setString(1, section.getSectionName());
      ps.setString(2, section.getSectionCode());
      ps.setLong(3, section.getSubjectId());
      ps.setInt(4, section.getCapacity());
      
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }

  public boolean updateSection(Section section) {
    try {
      PreparedStatement ps = conn.prepareStatement(
          "UPDATE sections SET section_name = ?, section_code = ?, subject_id = ?, capacity = ? WHERE id = ?"
      );
      ps.setString(1, section.getSectionName());
      ps.setString(2, section.getSectionCode());
      ps.setLong(3, section.getSubjectId());
      ps.setInt(4, section.getCapacity());
      ps.setLong(5, section.getId());
      
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }

  public boolean deleteSection(Long id) {
    try {
      PreparedStatement ps = conn.prepareStatement("DELETE FROM sections WHERE id = ?");
      ps.setLong(1, id);
      
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }
}
