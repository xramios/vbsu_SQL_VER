package com.group5.paul_esys.modules.prerequisites.services;

import com.group5.paul_esys.modules.prerequisites.model.Prerequisite;
import com.group5.paul_esys.modules.prerequisites.utils.PrerequisiteUtils;
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

public class PrerequisiteService {

  private final Connection conn = ConnectionService.getConnection();
  private final Logger logger = LoggerFactory.getLogger(PrerequisiteService.class);

  public List<Prerequisite> getAllPrerequisites() {
    List<Prerequisite> prerequisites = new ArrayList<>();
    try {
      PreparedStatement ps = conn.prepareStatement("SELECT * FROM prerequisites ORDER BY subject_id, pre_subject_id");
      ResultSet rs = ps.executeQuery();
      
      while (rs.next()) {
        prerequisites.add(PrerequisiteUtils.mapResultSetToPrerequisite(rs));
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }
    return prerequisites;
  }

  public Optional<Prerequisite> getPrerequisiteById(Long id) {
    try {
      PreparedStatement ps = conn.prepareStatement("SELECT * FROM prerequisites WHERE id = ?");
      ps.setLong(1, id);
      
      ResultSet rs = ps.executeQuery();
      if (rs.next()) {
        return Optional.of(PrerequisiteUtils.mapResultSetToPrerequisite(rs));
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }
    return Optional.empty();
  }

  public List<Prerequisite> getPrerequisitesBySubject(Long subjectId) {
    List<Prerequisite> prerequisites = new ArrayList<>();
    try {
      PreparedStatement ps = conn.prepareStatement("SELECT * FROM prerequisites WHERE subject_id = ? ORDER BY pre_subject_id");
      ps.setLong(1, subjectId);
      
      ResultSet rs = ps.executeQuery();
      while (rs.next()) {
        prerequisites.add(PrerequisiteUtils.mapResultSetToPrerequisite(rs));
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }
    return prerequisites;
  }

  public List<Prerequisite> getSubjectsRequiringPrerequisite(Long prerequisiteSubjectId) {
    List<Prerequisite> prerequisites = new ArrayList<>();
    try {
      PreparedStatement ps = conn.prepareStatement("SELECT * FROM prerequisites WHERE pre_subject_id = ? ORDER BY subject_id");
      ps.setLong(1, prerequisiteSubjectId);
      
      ResultSet rs = ps.executeQuery();
      while (rs.next()) {
        prerequisites.add(PrerequisiteUtils.mapResultSetToPrerequisite(rs));
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }
    return prerequisites;
  }

  public boolean createPrerequisite(Prerequisite prerequisite) {
    try {
      PreparedStatement ps = conn.prepareStatement(
          "INSERT INTO prerequisites (pre_subject_id, subject_id) VALUES (?, ?)"
      );
      ps.setLong(1, prerequisite.getPreSubjectId());
      ps.setLong(2, prerequisite.getSubjectId());
      
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }

  public boolean updatePrerequisite(Prerequisite prerequisite) {
    try {
      PreparedStatement ps = conn.prepareStatement(
          "UPDATE prerequisites SET pre_subject_id = ?, subject_id = ? WHERE id = ?"
      );
      ps.setLong(1, prerequisite.getPreSubjectId());
      ps.setLong(2, prerequisite.getSubjectId());
      ps.setLong(3, prerequisite.getId());
      
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }

  public boolean deletePrerequisite(Long id) {
    try {
      PreparedStatement ps = conn.prepareStatement("DELETE FROM prerequisites WHERE id = ?");
      ps.setLong(1, id);
      
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }

  public boolean deletePrerequisiteBySubjects(Long preSubjectId, Long subjectId) {
    try {
      PreparedStatement ps = conn.prepareStatement("DELETE FROM prerequisites WHERE pre_subject_id = ? AND subject_id = ?");
      ps.setLong(1, preSubjectId);
      ps.setLong(2, subjectId);
      
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }
}
