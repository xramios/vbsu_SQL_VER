package com.group5.paul_esys.screens.registrar.forms;

import com.group5.paul_esys.modules.enrollment_period.model.EnrollmentPeriod;
import com.group5.paul_esys.modules.enrollment_period.services.EnrollmentPeriodService;
import com.group5.paul_esys.modules.enrollment_period.utils.EnrollmentPeriodUtils;
import com.group5.paul_esys.modules.offerings.model.Offering;
import com.group5.paul_esys.modules.offerings.services.OfferingService;
import com.group5.paul_esys.modules.sections.model.Section;
import com.group5.paul_esys.modules.sections.services.SectionService;
import com.group5.paul_esys.modules.subjects.model.Subject;
import com.group5.paul_esys.modules.subjects.services.SubjectService;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class OfferingForm extends JDialog {

  private final EnrollmentPeriodService enrollmentPeriodService = EnrollmentPeriodService.getInstance();
  private final SubjectService subjectService = SubjectService.getInstance();
  private final SectionService sectionService = SectionService.getInstance();
  private final OfferingService offeringService = OfferingService.getInstance();

  private final Map<String, Long> enrollmentPeriodIdByLabel = new LinkedHashMap<>();
  private final Map<String, Long> subjectIdByLabel = new LinkedHashMap<>();
  private final Map<String, Long> sectionIdByLabel = new LinkedHashMap<>();

  private final Long preselectedEnrollmentPeriodId;
  private final Runnable onSavedCallback;

  private final JComboBox<String> cbxEnrollmentPeriod = new JComboBox<>();
  private final JComboBox<String> cbxSubject = new JComboBox<>();
  private final JComboBox<String> cbxSection = new JComboBox<>();
  private final JTextField txtCapacity = new JTextField(20);

  public OfferingForm(Frame parent, boolean modal, Long preselectedEnrollmentPeriodId, Runnable onSavedCallback) {
    super(parent, modal);
    this.preselectedEnrollmentPeriodId = preselectedEnrollmentPeriodId;
    this.onSavedCallback = onSavedCallback;
    initializeForm(parent);
  }

  private void initializeForm(Frame parent) {
    setTitle("Create Offering");
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    setLayout(new BorderLayout(10, 10));

    JPanel formPanel = new JPanel(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(6, 6, 6, 6);
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.anchor = GridBagConstraints.WEST;

    gbc.gridx = 0;
    gbc.gridy = 0;
    formPanel.add(new JLabel("Enrollment Period"), gbc);

    gbc.gridx = 1;
    gbc.weightx = 1.0;
    formPanel.add(cbxEnrollmentPeriod, gbc);

    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.weightx = 0;
    formPanel.add(new JLabel("Subject"), gbc);

    gbc.gridx = 1;
    gbc.weightx = 1.0;
    formPanel.add(cbxSubject, gbc);

    gbc.gridx = 0;
    gbc.gridy = 2;
    gbc.weightx = 0;
    formPanel.add(new JLabel("Section"), gbc);

    gbc.gridx = 1;
    gbc.weightx = 1.0;
    formPanel.add(cbxSection, gbc);

    gbc.gridx = 0;
    gbc.gridy = 3;
    gbc.weightx = 0;
    formPanel.add(new JLabel("Capacity (Optional)"), gbc);

    gbc.gridx = 1;
    gbc.weightx = 1.0;
    formPanel.add(txtCapacity, gbc);

    add(formPanel, BorderLayout.CENTER);

    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    JButton btnCancel = new JButton("Cancel");
    JButton btnSave = new JButton("Save");

    btnCancel.addActionListener(evt -> dispose());
    btnSave.addActionListener(evt -> saveOffering());

    buttonPanel.add(btnCancel);
    buttonPanel.add(btnSave);
    add(buttonPanel, BorderLayout.SOUTH);

    loadEnrollmentPeriods();
    loadSubjects();
    loadSections();

    pack();
    setMinimumSize(getSize());
    setLocationRelativeTo(parent);
  }

  private void loadEnrollmentPeriods() {
    cbxEnrollmentPeriod.removeAllItems();
    enrollmentPeriodIdByLabel.clear();

    List<EnrollmentPeriod> periods = enrollmentPeriodService.getAllEnrollmentPeriods();
    for (EnrollmentPeriod period : periods) {
      String label = buildEnrollmentPeriodLabel(period);
      cbxEnrollmentPeriod.addItem(label);
      enrollmentPeriodIdByLabel.put(label, period.getId());
    }

    selectEnrollmentPeriodById(preselectedEnrollmentPeriodId);
  }

  private void selectEnrollmentPeriodById(Long enrollmentPeriodId) {
    if (enrollmentPeriodId == null) {
      return;
    }

    for (Map.Entry<String, Long> entry : enrollmentPeriodIdByLabel.entrySet()) {
      if (enrollmentPeriodId.equals(entry.getValue())) {
        cbxEnrollmentPeriod.setSelectedItem(entry.getKey());
        return;
      }
    }
  }

  private String buildEnrollmentPeriodLabel(EnrollmentPeriod period) {
    String schoolYear = EnrollmentPeriodUtils.safeText(period.getSchoolYear(), "N/A");
    String semester = EnrollmentPeriodUtils.safeText(period.getSemester(), "N/A");
    String status = EnrollmentPeriodUtils.resolveStatus(period);
    return schoolYear + " | " + semester + " | " + status + " | ID " + period.getId();
  }

  private void loadSubjects() {
    cbxSubject.removeAllItems();
    subjectIdByLabel.clear();

    for (Subject subject : subjectService.getAllSubjects()) {
      if (subject.getId() == null) {
        continue;
      }
      String label = buildSubjectLabel(subject);
      cbxSubject.addItem(label);
      subjectIdByLabel.put(label, subject.getId());
    }
  }

  private String buildSubjectLabel(Subject subject) {
    String code = safeText(subject.getSubjectCode(), "NO-CODE");
    String name = safeText(subject.getSubjectName(), "Unnamed Subject");
    return code + " - " + name + " | ID " + subject.getId();
  }

  private void loadSections() {
    cbxSection.removeAllItems();
    sectionIdByLabel.clear();

    for (Section section : sectionService.getAllSections()) {
      if (section.getId() == null || isDissolved(section)) {
        continue;
      }
      String label = buildSectionLabel(section);
      cbxSection.addItem(label);
      sectionIdByLabel.put(label, section.getId());
    }
  }

  private boolean isDissolved(Section section) {
    String status = safeText(section.getStatus(), "OPEN");
    return "DISSOLVED".equalsIgnoreCase(status);
  }

  private String buildSectionLabel(Section section) {
    String code = safeText(section.getSectionCode(), "NO-CODE");
    String name = safeText(section.getSectionName(), "Unnamed Section");
    String status = safeText(section.getStatus(), "OPEN").toUpperCase();
    String capacity = section.getCapacity() == null ? "N/A" : String.valueOf(section.getCapacity());
    return code + " - " + name + " | Status: " + status + " | Cap: " + capacity + " | ID " + section.getId();
  }

  private String safeText(String value, String fallback) {
    if (value == null) {
      return fallback;
    }

    String normalized = value.trim();
    return normalized.isEmpty() ? fallback : normalized;
  }

  private void saveOffering() {
    Long enrollmentPeriodId = getSelectedEnrollmentPeriodId();
    Long subjectId = getSelectedSubjectId();
    Long sectionId = getSelectedSectionId();

    if (!isValidForm(enrollmentPeriodId, subjectId, sectionId)) {
      return;
    }

    Integer capacity = parseCapacity();
    if (capacity == INVALID_CAPACITY) {
      return;
    }

    if (offeringService.existsOffering(subjectId, sectionId, enrollmentPeriodId)) {
      JOptionPane.showMessageDialog(
          this,
          "This offering already exists for the selected subject, section, and enrollment period.",
          "Duplicate Offering",
          JOptionPane.WARNING_MESSAGE
      );
      return;
    }

    Offering offering = new Offering()
        .setSubjectId(subjectId)
        .setSectionId(sectionId)
        .setEnrollmentPeriodId(enrollmentPeriodId)
        .setSemesterSubjectId(null)
        .setCapacity(capacity);

    if (!offeringService.createOffering(offering)) {
      JOptionPane.showMessageDialog(
          this,
          "Failed to create offering. Please try again.",
          "Create Offering",
          JOptionPane.ERROR_MESSAGE
      );
      return;
    }

    JOptionPane.showMessageDialog(
        this,
        "Offering created successfully.",
        "Create Offering",
        JOptionPane.INFORMATION_MESSAGE
    );

    if (onSavedCallback != null) {
      onSavedCallback.run();
    }

    dispose();
  }

  private static final Integer INVALID_CAPACITY = Integer.MIN_VALUE;

  private boolean isValidForm(Long enrollmentPeriodId, Long subjectId, Long sectionId) {
    if (enrollmentPeriodId == null) {
      JOptionPane.showMessageDialog(
          this,
          "Please select an enrollment period.",
          "Validation Error",
          JOptionPane.WARNING_MESSAGE
      );
      return false;
    }

    if (subjectId == null) {
      JOptionPane.showMessageDialog(
          this,
          "Please select a subject.",
          "Validation Error",
          JOptionPane.WARNING_MESSAGE
      );
      return false;
    }

    if (sectionId == null) {
      JOptionPane.showMessageDialog(
          this,
          "Please select a section.",
          "Validation Error",
          JOptionPane.WARNING_MESSAGE
      );
      return false;
    }

    return true;
  }

  private Integer parseCapacity() {
    String raw = txtCapacity.getText();
    if (raw == null || raw.trim().isEmpty()) {
      return null;
    }

    try {
      int parsed = Integer.parseInt(raw.trim());
      if (parsed <= 0) {
        throw new NumberFormatException("Capacity must be positive");
      }
      return parsed;
    } catch (NumberFormatException e) {
      JOptionPane.showMessageDialog(
          this,
          "Capacity must be a positive whole number.",
          "Validation Error",
          JOptionPane.WARNING_MESSAGE
      );
      return INVALID_CAPACITY;
    }
  }

  private Long getSelectedEnrollmentPeriodId() {
    Object selectedItem = cbxEnrollmentPeriod.getSelectedItem();
    if (selectedItem == null) {
      return null;
    }
    return enrollmentPeriodIdByLabel.get(selectedItem.toString());
  }

  private Long getSelectedSubjectId() {
    Object selectedItem = cbxSubject.getSelectedItem();
    if (selectedItem == null) {
      return null;
    }
    return subjectIdByLabel.get(selectedItem.toString());
  }

  private Long getSelectedSectionId() {
    Object selectedItem = cbxSection.getSelectedItem();
    if (selectedItem == null) {
      return null;
    }
    return sectionIdByLabel.get(selectedItem.toString());
  }
}
