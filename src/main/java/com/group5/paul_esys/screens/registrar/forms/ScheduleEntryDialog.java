package com.group5.paul_esys.screens.registrar.forms;

import com.group5.paul_esys.modules.enums.DayOfWeek;
import com.group5.paul_esys.modules.registrar.model.ScheduleLookupOption;
import com.group5.paul_esys.modules.registrar.model.ScheduleManagementRow;
import com.group5.paul_esys.modules.registrar.model.ScheduleOfferingOption;
import com.group5.paul_esys.modules.registrar.model.ScheduleUpsertRequest;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
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
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JFormattedTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import raven.datetime.TimePicker;

public class ScheduleEntryDialog extends JDialog {

  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

  private final List<ScheduleOfferingOption> offeringOptions;
  private final List<ScheduleLookupOption> roomOptions;
  private final List<ScheduleLookupOption> facultyOptions;
  private final ScheduleManagementRow editingSchedule;

  private final JComboBox<String> cbxEnrollmentPeriod = new JComboBox<>();
  private final JTextField txtOfferingSearch = new JTextField();
  private final JTable tblOfferings = new JTable();
  private final DefaultTableModel offeringTableModel = new DefaultTableModel(
      new Object[][]{},
      new String[]{"Section", "Subject Code", "Subject Name", "Prerequisite"}
  ) {
    @Override
    public boolean isCellEditable(int row, int column) {
      return false;
    }
  };
  private final JComboBox<String> cbxRoom = new JComboBox<>();
  private final JComboBox<String> cbxFaculty = new JComboBox<>();
  private final JComboBox<String> cbxDay = new JComboBox<>();
  private final TimePicker startTimePicker = new TimePicker();
  private final TimePicker endTimePicker = new TimePicker();
  private final JFormattedTextField ftxtStartTime = new JFormattedTextField();
  private final JFormattedTextField ftxtEndTime = new JFormattedTextField();
  private final JLabel lblError = new JLabel(" ");
  private TableRowSorter<DefaultTableModel> offeringRowSorter;

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
    cbxRoom.setFont(new java.awt.Font("Poppins", 0, 12));
    cbxFaculty.setFont(new java.awt.Font("Poppins", 0, 12));
    cbxDay.setFont(new java.awt.Font("Poppins", 0, 12));

    txtOfferingSearch.setFont(new java.awt.Font("Poppins", 0, 12));
    txtOfferingSearch.setToolTipText("Search by section, subject code, title, or prerequisite");

    configureOfferingTable();

    ftxtStartTime.setFont(new java.awt.Font("Poppins", 0, 12));
    ftxtEndTime.setFont(new java.awt.Font("Poppins", 0, 12));
    ftxtStartTime.setToolTipText("24-hour format, e.g. 08:30");
    ftxtEndTime.setToolTipText("24-hour format, e.g. 10:00");

    startTimePicker.set24HourView(true);
    endTimePicker.set24HourView(true);
    startTimePicker.setEditor(ftxtStartTime);
    endTimePicker.setEditor(ftxtEndTime);

    JPanel offeringPanel = new JPanel(new BorderLayout(0, 6));
    offeringPanel.setOpaque(false);
    offeringPanel.setPreferredSize(new Dimension(0, 200));

    JScrollPane offeringScrollPane = new JScrollPane(tblOfferings);
    offeringScrollPane.setPreferredSize(new Dimension(560, 170));

    offeringPanel.add(txtOfferingSearch, BorderLayout.NORTH);
    offeringPanel.add(offeringScrollPane, BorderLayout.CENTER);

    int row = 0;
    addField(formPanel, "Enrollment Period", cbxEnrollmentPeriod, row, 0, 1);
    addField(formPanel, "Day", cbxDay, row, 1, 1);

    row++;
    addField(formPanel, "Offering (Section + Subject)", offeringPanel, row, 0, 2);

    row++;
    addField(formPanel, "Room", cbxRoom, row, 0, 1);
    addField(formPanel, "Faculty", cbxFaculty, row, 1, 1);

    row++;
    addField(formPanel, "Start Time (HH:mm)", ftxtStartTime, row, 0, 1);
    addField(formPanel, "End Time (HH:mm)", ftxtEndTime, row, 1, 1);

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
    txtOfferingSearch.getDocument().addDocumentListener(new DocumentListener() {
      @Override
      public void insertUpdate(DocumentEvent e) {
        applyOfferingSearchFilter();
      }

      @Override
      public void removeUpdate(DocumentEvent e) {
        applyOfferingSearchFilter();
      }

      @Override
      public void changedUpdate(DocumentEvent e) {
        applyOfferingSearchFilter();
      }
    });

    rootPanel.add(headerPanel, BorderLayout.NORTH);
    rootPanel.add(formPanel, BorderLayout.CENTER);
    rootPanel.add(buttonPanel, BorderLayout.SOUTH);

    getContentPane().setLayout(new BorderLayout());
    getContentPane().add(rootPanel, BorderLayout.CENTER);
  }

  private void configureOfferingTable() {
    tblOfferings.setModel(offeringTableModel);
    tblOfferings.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    tblOfferings.setRowHeight(24);
    tblOfferings.setFillsViewportHeight(true);

    offeringRowSorter = new TableRowSorter<>(offeringTableModel);
    tblOfferings.setRowSorter(offeringRowSorter);

    int[] preferredWidths = {90, 120, 210, 220};
    for (int column = 0; column < preferredWidths.length; column++) {
      tblOfferings.getColumnModel().getColumn(column).setPreferredWidth(preferredWidths[column]);
    }
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
    txtOfferingSearch.setText("");

    if (editingSchedule == null) {
      startTimePicker.clearSelectedTime();
      endTimePicker.clearSelectedTime();
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

    if (editingSchedule.startTime() == null) {
      startTimePicker.clearSelectedTime();
    } else {
      startTimePicker.setSelectedTime(editingSchedule.startTime());
    }

    if (editingSchedule.endTime() == null) {
      endTimePicker.clearSelectedTime();
    } else {
      endTimePicker.setSelectedTime(editingSchedule.endTime());
    }
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

    visibleOfferingOptions.clear();
    offeringTableModel.setRowCount(0);

    for (ScheduleOfferingOption option : offeringOptions) {
      if (selectedEnrollmentPeriodId == null || selectedEnrollmentPeriodId.equals(option.enrollmentPeriodId())) {
        visibleOfferingOptions.add(option);
        offeringTableModel.addRow(new Object[]{
            option.sectionCode(),
            option.subjectCode(),
            option.subjectName(),
            option.prerequisiteLabel()
        });
      }
    }

    applyOfferingSearchFilter();

    if (selectedOfferingId == null) {
      tblOfferings.clearSelection();
      return;
    }

    selectOfferingById(selectedOfferingId);
  }

  private void selectOfferingById(Long offeringId) {
    if (offeringId == null) {
      tblOfferings.clearSelection();
      return;
    }

    for (int modelRow = 0; modelRow < visibleOfferingOptions.size(); modelRow++) {
      ScheduleOfferingOption option = visibleOfferingOptions.get(modelRow);
      if (!offeringId.equals(option.offeringId())) {
        continue;
      }

      int viewRow = tblOfferings.convertRowIndexToView(modelRow);
      if (viewRow < 0) {
        tblOfferings.clearSelection();
        return;
      }

      tblOfferings.setRowSelectionInterval(viewRow, viewRow);
      tblOfferings.scrollRectToVisible(tblOfferings.getCellRect(viewRow, 0, true));
      return;
    }

    tblOfferings.clearSelection();
  }

  private void applyOfferingSearchFilter() {
    if (offeringRowSorter == null) {
      return;
    }

    String keyword = txtOfferingSearch.getText() == null ? "" : txtOfferingSearch.getText().trim();
    if (keyword.isEmpty()) {
      offeringRowSorter.setRowFilter(null);
      return;
    }

    offeringRowSorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(keyword)));
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

    LocalTime startTime = getSelectedStartTime();
    LocalTime endTime = getSelectedEndTime();
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
    int selectedViewRow = tblOfferings.getSelectedRow();
    if (selectedViewRow < 0) {
      return null;
    }

    int optionIndex = tblOfferings.convertRowIndexToModel(selectedViewRow);
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

  private LocalTime getSelectedStartTime() {
    LocalTime selected = startTimePicker.getSelectedTime();
    if (selected != null) {
      return selected;
    }

    LocalTime parsed = parseTime(ftxtStartTime.getText());
    if (parsed != null) {
      startTimePicker.setSelectedTime(parsed);
    }
    return parsed;
  }

  private LocalTime getSelectedEndTime() {
    LocalTime selected = endTimePicker.getSelectedTime();
    if (selected != null) {
      return selected;
    }

    LocalTime parsed = parseTime(ftxtEndTime.getText());
    if (parsed != null) {
      endTimePicker.setSelectedTime(parsed);
    }
    return parsed;
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