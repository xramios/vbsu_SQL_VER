package com.group5.paul_esys.screens.registrar.forms;

import com.group5.paul_esys.modules.enums.DayOfWeek;
import com.group5.paul_esys.modules.registrar.model.ScheduleLookupOption;
import com.group5.paul_esys.modules.registrar.model.ScheduleManagementRow;
import com.group5.paul_esys.modules.registrar.model.ScheduleOfferingOption;
import com.group5.paul_esys.modules.registrar.model.ScheduleUpsertRequest;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

public class ScheduleEntryDialog extends JDialog {

  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

  private final List<ScheduleOfferingOption> offeringOptions;
  private final List<ScheduleLookupOption> roomOptions;
  private final List<ScheduleLookupOption> facultyOptions;
  private final ScheduleManagementRow editingSchedule;

  private final JComboBox<String> cbxEnrollmentPeriod = new JComboBox<>();
  private final JComboBox<String> cbxOffering = new JComboBox<>();
  private final JComboBox<String> cbxRoom = new JComboBox<>();
  private final JComboBox<String> cbxFaculty = new JComboBox<>();
  private final JComboBox<String> cbxDay = new JComboBox<>();
  private final JTextField txtStartTime = new JTextField();
  private final JTextField txtEndTime = new JTextField();
  private final JLabel lblError = new JLabel(" ");

  private final List<Long> enrollmentPeriodIds = new ArrayList<>();
  private final List<ScheduleOfferingOption> visibleOfferingOptions = new ArrayList<>();
  private final List<Long> roomIds = new ArrayList<>();
  private final List<Long> facultyIds = new ArrayList<>();
  private final Map<Long, String> enrollmentPeriodLabelById = new LinkedHashMap<>();

  private ScheduleUpsertRequest submission;

  public ScheduleEntryDialog(
      Frame parent,
      List<ScheduleOfferingOption> offeringOptions,
      List<ScheduleLookupOption> roomOptions,
      List<ScheduleLookupOption> facultyOptions,
      ScheduleManagementRow editingSchedule
  ) {
    super(parent, true);
    this.offeringOptions = offeringOptions == null ? List.of() : offeringOptions;
    this.roomOptions = roomOptions == null ? List.of() : roomOptions;
    this.facultyOptions = facultyOptions == null ? List.of() : facultyOptions;
    this.editingSchedule = editingSchedule;

    setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    setTitle(editingSchedule == null ? "New Schedule" : "Edit Schedule");

    initializeUi();
    initializeValues();
    pack();
    setLocationRelativeTo(parent);
  }

  public ScheduleUpsertRequest getSubmission() {
    return submission;
  }

  private void initializeUi() {
    JPanel rootPanel = new JPanel(new BorderLayout(0, 12));
    rootPanel.setBorder(BorderFactory.createEmptyBorder(16, 16, 12, 16));
    rootPanel.setBackground(Color.WHITE);

    JLabel lblTitle = new JLabel(editingSchedule == null ? "Create Schedule" : "Update Schedule");
    lblTitle.setFont(new java.awt.Font("Poppins", 0, 20));

    JLabel lblSubtitle = new JLabel("Assign offering, room, faculty, and time details.");
    lblSubtitle.setFont(new java.awt.Font("Poppins", 0, 12));
    lblSubtitle.setForeground(new Color(120, 120, 120));

    JPanel headerPanel = new JPanel(new BorderLayout(0, 3));
    headerPanel.setOpaque(false);
    headerPanel.add(lblTitle, BorderLayout.NORTH);
    headerPanel.add(lblSubtitle, BorderLayout.CENTER);

    JPanel formPanel = new JPanel(new GridBagLayout());
    formPanel.setOpaque(false);

    cbxEnrollmentPeriod.setFont(new java.awt.Font("Poppins", 0, 12));
    cbxOffering.setFont(new java.awt.Font("Poppins", 0, 12));
    cbxRoom.setFont(new java.awt.Font("Poppins", 0, 12));
    cbxFaculty.setFont(new java.awt.Font("Poppins", 0, 12));
    cbxDay.setFont(new java.awt.Font("Poppins", 0, 12));

    txtStartTime.setFont(new java.awt.Font("Poppins", 0, 12));
    txtEndTime.setFont(new java.awt.Font("Poppins", 0, 12));
    txtStartTime.setToolTipText("24-hour format, e.g. 08:30");
    txtEndTime.setToolTipText("24-hour format, e.g. 10:00");

    int row = 0;
    addField(formPanel, "Enrollment Period", cbxEnrollmentPeriod, row, 0, 1);
    addField(formPanel, "Day", cbxDay, row, 1, 1);

    row++;
    addField(formPanel, "Offering (Section + Subject)", cbxOffering, row, 0, 2);

    row++;
    addField(formPanel, "Room", cbxRoom, row, 0, 1);
    addField(formPanel, "Faculty", cbxFaculty, row, 1, 1);

    row++;
    addField(formPanel, "Start Time (HH:mm)", txtStartTime, row, 0, 1);
    addField(formPanel, "End Time (HH:mm)", txtEndTime, row, 1, 1);

    row++;
    lblError.setFont(new java.awt.Font("Poppins", 0, 12));
    lblError.setForeground(new Color(180, 35, 24));
    addRawComponent(formPanel, lblError, row, 0, 2);

    JPanel buttonPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 0));
    buttonPanel.setOpaque(false);

    JButton btnReset = new JButton("Reset");
    JButton btnCancel = new JButton("Cancel");
    JButton btnSave = new JButton(editingSchedule == null ? "Save" : "Update");

    btnReset.addActionListener(evt -> resetFormValues());
    btnCancel.addActionListener(evt -> dispose());
    btnSave.addActionListener(evt -> saveSchedule());

    buttonPanel.add(btnReset);
    buttonPanel.add(btnCancel);
    buttonPanel.add(btnSave);

    cbxEnrollmentPeriod.addActionListener(evt -> reloadOfferingOptions(getCurrentSelectedOfferingId()));

    rootPanel.add(headerPanel, BorderLayout.NORTH);
    rootPanel.add(formPanel, BorderLayout.CENTER);
    rootPanel.add(buttonPanel, BorderLayout.SOUTH);

    getContentPane().setLayout(new BorderLayout());
    getContentPane().add(rootPanel, BorderLayout.CENTER);
  }

  private void initializeValues() {
    populateDayOptions();
    populateEnrollmentPeriodOptions();
    populateRoomOptions();
    populateFacultyOptions();
    applyEditingDefaults();
  }

  private void populateDayOptions() {
    cbxDay.removeAllItems();
    cbxDay.addItem("Select day");
    for (DayOfWeek day : DayOfWeek.values()) {
      cbxDay.addItem(day.name());
    }
  }

  private void populateEnrollmentPeriodOptions() {
    enrollmentPeriodLabelById.clear();
    for (ScheduleOfferingOption option : offeringOptions) {
      if (option.enrollmentPeriodId() == null) {
        continue;
      }

      enrollmentPeriodLabelById.putIfAbsent(option.enrollmentPeriodId(), option.enrollmentPeriodLabel());
    }

    cbxEnrollmentPeriod.removeAllItems();
    enrollmentPeriodIds.clear();

    cbxEnrollmentPeriod.addItem("Select enrollment period");
    enrollmentPeriodIds.add(null);

    for (Map.Entry<Long, String> entry : enrollmentPeriodLabelById.entrySet()) {
      cbxEnrollmentPeriod.addItem(entry.getValue());
      enrollmentPeriodIds.add(entry.getKey());
    }
  }

  private void populateRoomOptions() {
    cbxRoom.removeAllItems();
    roomIds.clear();

    cbxRoom.addItem("Unassigned");
    roomIds.add(null);

    for (ScheduleLookupOption option : roomOptions) {
      cbxRoom.addItem(option.label());
      roomIds.add(option.id());
    }
  }

  private void populateFacultyOptions() {
    cbxFaculty.removeAllItems();
    facultyIds.clear();

    cbxFaculty.addItem("Unassigned");
    facultyIds.add(null);

    for (ScheduleLookupOption option : facultyOptions) {
      cbxFaculty.addItem(option.label());
      facultyIds.add(option.id());
    }
  }

  private void applyEditingDefaults() {
    if (editingSchedule == null) {
      txtStartTime.setText("");
      txtEndTime.setText("");
      cbxDay.setSelectedIndex(0);
      cbxEnrollmentPeriod.setSelectedIndex(0);
      reloadOfferingOptions(null);
      return;
    }

    selectEnrollmentPeriod(editingSchedule.enrollmentPeriodId());
    reloadOfferingOptions(editingSchedule.offeringId());
    selectRoom(editingSchedule.roomId());
    selectFaculty(editingSchedule.facultyId());
    selectDay(editingSchedule.day());

    txtStartTime.setText(editingSchedule.startTime() == null ? "" : editingSchedule.startTime().format(TIME_FORMATTER));
    txtEndTime.setText(editingSchedule.endTime() == null ? "" : editingSchedule.endTime().format(TIME_FORMATTER));
  }

  private void resetFormValues() {
    lblError.setText(" ");
    applyEditingDefaults();
  }

  private void selectEnrollmentPeriod(Long enrollmentPeriodId) {
    for (int index = 0; index < enrollmentPeriodIds.size(); index++) {
      Long currentId = enrollmentPeriodIds.get(index);
      if (currentId != null && currentId.equals(enrollmentPeriodId)) {
        cbxEnrollmentPeriod.setSelectedIndex(index);
        return;
      }
    }

    cbxEnrollmentPeriod.setSelectedIndex(0);
  }

  private void reloadOfferingOptions(Long selectedOfferingId) {
    Long selectedEnrollmentPeriodId = getSelectedEnrollmentPeriodId();

    cbxOffering.removeAllItems();
    visibleOfferingOptions.clear();
    cbxOffering.addItem("Select offering");

    for (ScheduleOfferingOption option : offeringOptions) {
      if (selectedEnrollmentPeriodId == null || selectedEnrollmentPeriodId.equals(option.enrollmentPeriodId())) {
        visibleOfferingOptions.add(option);
        cbxOffering.addItem(option.label());
      }
    }

    if (selectedOfferingId == null) {
      cbxOffering.setSelectedIndex(0);
      return;
    }

    for (int index = 0; index < visibleOfferingOptions.size(); index++) {
      ScheduleOfferingOption option = visibleOfferingOptions.get(index);
      if (selectedOfferingId.equals(option.offeringId())) {
        cbxOffering.setSelectedIndex(index + 1);
        return;
      }
    }

    cbxOffering.setSelectedIndex(0);
  }

  private void selectRoom(Long roomId) {
    for (int index = 0; index < roomIds.size(); index++) {
      Long currentId = roomIds.get(index);
      if (roomId == null && currentId == null) {
        cbxRoom.setSelectedIndex(index);
        return;
      }

      if (roomId != null && roomId.equals(currentId)) {
        cbxRoom.setSelectedIndex(index);
        return;
      }
    }

    cbxRoom.setSelectedIndex(0);
  }

  private void selectFaculty(Long facultyId) {
    for (int index = 0; index < facultyIds.size(); index++) {
      Long currentId = facultyIds.get(index);
      if (facultyId == null && currentId == null) {
        cbxFaculty.setSelectedIndex(index);
        return;
      }

      if (facultyId != null && facultyId.equals(currentId)) {
        cbxFaculty.setSelectedIndex(index);
        return;
      }
    }

    cbxFaculty.setSelectedIndex(0);
  }

  private void selectDay(String day) {
    if (day == null || day.isBlank()) {
      cbxDay.setSelectedIndex(0);
      return;
    }

    for (int index = 1; index < cbxDay.getItemCount(); index++) {
      if (day.equalsIgnoreCase(cbxDay.getItemAt(index))) {
        cbxDay.setSelectedIndex(index);
        return;
      }
    }

    cbxDay.setSelectedIndex(0);
  }

  private Long getSelectedEnrollmentPeriodId() {
    int selectedIndex = cbxEnrollmentPeriod.getSelectedIndex();
    if (selectedIndex < 0 || selectedIndex >= enrollmentPeriodIds.size()) {
      return null;
    }

    return enrollmentPeriodIds.get(selectedIndex);
  }

  private Long getEditingOfferingId() {
    return editingSchedule == null ? null : editingSchedule.offeringId();
  }

  private Long getCurrentSelectedOfferingId() {
    ScheduleOfferingOption selected = getSelectedOfferingOption();
    return selected == null ? getEditingOfferingId() : selected.offeringId();
  }

  private void saveSchedule() {
    lblError.setText(" ");

    ScheduleOfferingOption selectedOffering = getSelectedOfferingOption();
    if (selectedOffering == null) {
      showError("Offering selection is required.");
      return;
    }

    DayOfWeek selectedDay = getSelectedDay();
    if (selectedDay == null) {
      showError("Day selection is required.");
      return;
    }

    LocalTime startTime = parseTime(txtStartTime.getText());
    LocalTime endTime = parseTime(txtEndTime.getText());
    if (startTime == null || endTime == null) {
      showError("Please use HH:mm format for start and end time.");
      return;
    }

    if (!startTime.isBefore(endTime)) {
      showError("Start time must be earlier than end time.");
      return;
    }

    submission = new ScheduleUpsertRequest(
        editingSchedule == null ? null : editingSchedule.scheduleId(),
        selectedOffering.offeringId(),
        getSelectedRoomId(),
        getSelectedFacultyId(),
        selectedDay,
        startTime,
        endTime
    );

    dispose();
  }

  private ScheduleOfferingOption getSelectedOfferingOption() {
    int selectedIndex = cbxOffering.getSelectedIndex();
    if (selectedIndex <= 0) {
      return null;
    }

    int optionIndex = selectedIndex - 1;
    if (optionIndex < 0 || optionIndex >= visibleOfferingOptions.size()) {
      return null;
    }

    return visibleOfferingOptions.get(optionIndex);
  }

  private Long getSelectedRoomId() {
    int selectedIndex = cbxRoom.getSelectedIndex();
    if (selectedIndex < 0 || selectedIndex >= roomIds.size()) {
      return null;
    }

    return roomIds.get(selectedIndex);
  }

  private Long getSelectedFacultyId() {
    int selectedIndex = cbxFaculty.getSelectedIndex();
    if (selectedIndex < 0 || selectedIndex >= facultyIds.size()) {
      return null;
    }

    return facultyIds.get(selectedIndex);
  }

  private DayOfWeek getSelectedDay() {
    int selectedIndex = cbxDay.getSelectedIndex();
    if (selectedIndex <= 0) {
      return null;
    }

    String day = cbxDay.getSelectedItem() == null ? null : cbxDay.getSelectedItem().toString();
    if (day == null || day.isBlank()) {
      return null;
    }

    return DayOfWeek.valueOf(day);
  }

  private LocalTime parseTime(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }

    try {
      return LocalTime.parse(value.trim(), TIME_FORMATTER);
    } catch (DateTimeParseException ex) {
      return null;
    }
  }

  private void showError(String message) {
    lblError.setHorizontalAlignment(SwingConstants.LEFT);
    lblError.setText(message == null ? "Unknown error." : message);
  }

  private void addField(
      JPanel panel,
      String labelText,
      java.awt.Component component,
      int row,
      int column,
      int columnSpan
  ) {
    JPanel fieldPanel = new JPanel(new BorderLayout(0, 4));
    fieldPanel.setOpaque(false);

    JLabel label = new JLabel(labelText);
    label.setFont(new java.awt.Font("Poppins", 0, 12));

    fieldPanel.add(label, BorderLayout.NORTH);
    fieldPanel.add(component, BorderLayout.CENTER);

    addRawComponent(panel, fieldPanel, row, column, columnSpan);
  }

  private void addRawComponent(JPanel panel, java.awt.Component component, int row, int column, int columnSpan) {
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = column;
    gbc.gridy = row;
    gbc.gridwidth = columnSpan;
    gbc.weightx = columnSpan == 2 ? 1.0 : 0.5;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.anchor = GridBagConstraints.NORTHWEST;
    gbc.insets = new Insets(2, 2, 8, 8);
    panel.add(component, gbc);
  }
}