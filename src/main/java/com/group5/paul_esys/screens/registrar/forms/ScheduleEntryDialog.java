/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JDialog.java to edit this template
 */
package com.group5.paul_esys.screens.registrar.forms;

import com.group5.paul_esys.modules.enums.DayOfWeek;
import com.group5.paul_esys.modules.registrar.model.ScheduleLookupOption;
import com.group5.paul_esys.modules.registrar.model.ScheduleManagementRow;
import com.group5.paul_esys.modules.registrar.model.ScheduleOfferingOption;
import com.group5.paul_esys.modules.registrar.model.ScheduleUpsertRequest;

import java.awt.Dialog;
import java.awt.Frame;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import raven.datetime.TimePicker;

/**
 *
 * @author nytri
 */
public class ScheduleEntryDialog extends javax.swing.JDialog {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final List<ScheduleOfferingOption> offeringOptions;
    private final List<ScheduleLookupOption> roomOptions;
    private final List<ScheduleLookupOption> facultyOptions;
    private final ScheduleManagementRow editingSchedule;

    private final TimePicker startTimePicker = new TimePicker();
    private final TimePicker endTimePicker = new TimePicker();

    private final DefaultTableModel offeringTableModel = new DefaultTableModel(
            new Object[][] {},
            new String[] { "Section", "Subject Code", "Subject Name", "Prerequisite" }) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final List<Long> enrollmentPeriodIds = new ArrayList<>();
    private final List<ScheduleOfferingOption> visibleOfferingOptions = new ArrayList<>();
    private final List<Long> roomIds = new ArrayList<>();
    private final List<Long> facultyIds = new ArrayList<>();
    private final Map<Long, String> enrollmentPeriodLabelById = new LinkedHashMap<>();

    private TableRowSorter<DefaultTableModel> offeringRowSorter;
    private ScheduleUpsertRequest submission;

    public ScheduleEntryDialog(
            Frame parent,
            List<ScheduleOfferingOption> offeringOptions,
            List<ScheduleLookupOption> roomOptions,
            List<ScheduleLookupOption> facultyOptions,
            ScheduleManagementRow editingSchedule) {
        super(parent, true);
        this.offeringOptions = offeringOptions == null ? List.of() : offeringOptions;
        this.roomOptions = roomOptions == null ? List.of() : roomOptions;
        this.facultyOptions = facultyOptions == null ? List.of() : facultyOptions;
        this.editingSchedule = editingSchedule;

        setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        initComponents();

        configurePickers();
        configureOfferingTable();
        registerUiListeners();
        initializeValues();
        applyEditingDefaults();

        pack();
        setLocationRelativeTo(parent);
    }

    public ScheduleUpsertRequest getSubmission() {
        return submission;
    }

    private void configurePickers() {
        startTimePicker.set24HourView(true);
        endTimePicker.set24HourView(true);
        startTimePicker.setEditor(ftxtStartTime);
        endTimePicker.setEditor(ftxtEndTime);
    }

    private void configureOfferingTable() {
        tblOfferings.setModel(offeringTableModel);
        tblOfferings.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tblOfferings.setRowHeight(24);
        tblOfferings.setFillsViewportHeight(true);

        offeringRowSorter = new TableRowSorter<>(offeringTableModel);
        tblOfferings.setRowSorter(offeringRowSorter);

        int[] preferredWidths = { 90, 120, 210, 220 };
        for (int column = 0; column < preferredWidths.length; column++) {
            tblOfferings.getColumnModel().getColumn(column).setPreferredWidth(preferredWidths[column]);
        }
    }

    private void registerUiListeners() {
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
    }

    private void initializeValues() {
        populateDayOptions();
        populateEnrollmentPeriodOptions();
        populateRoomOptions();
        populateFacultyOptions();
    }

    private void applyEditingDefaults() {
        txtOfferingSearch.setText("");
        lblError.setText(" ");

        if (editingSchedule == null) {
            setTitle("New Schedule");
            lblTitle.setText("Create Schedule");
            btnSave.setText("Save");

            startTimePicker.clearSelectedTime();
            endTimePicker.clearSelectedTime();
            cbxDay.setSelectedIndex(0);
            cbxEnrollmentPeriod.setSelectedIndex(0);
            reloadOfferingOptions(null);
            return;
        }

        setTitle("Edit Schedule");
        lblTitle.setText("Update Schedule");
        btnSave.setText("Update");

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
                offeringTableModel.addRow(new Object[] {
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
            showError("Fixed schedule times are required. Use HH:mm format.");
            return;
        }

        if (isTBATime(startTime)) {
            showError("Start time cannot be TBA (00:00). Please set a specific start time.");
            return;
        }

        if (isTBATime(endTime)) {
            showError("End time cannot be TBA (00:00). Please set a specific end time.");
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
                endTime);

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

    private boolean isTBATime(LocalTime time) {
        return time != null && time.getHour() == 0 && time.getMinute() == 0;
    }

    private void showError(String message) {
        lblError.setHorizontalAlignment(SwingConstants.LEFT);
        lblError.setText(message == null ? "Unknown error." : message);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        lblTitle = new javax.swing.JLabel();
        lblSubtitle = new javax.swing.JLabel();
        lblEnrollmentPeriod = new javax.swing.JLabel();
        lblDay = new javax.swing.JLabel();
        cbxEnrollmentPeriod = new javax.swing.JComboBox<>();
        cbxDay = new javax.swing.JComboBox<>();
        lblOffering = new javax.swing.JLabel();
        txtOfferingSearch = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblOfferings = new javax.swing.JTable();
        lblRoom = new javax.swing.JLabel();
        lblFaculty = new javax.swing.JLabel();
        cbxRoom = new javax.swing.JComboBox<>();
        cbxFaculty = new javax.swing.JComboBox<>();
        lblStartTime = new javax.swing.JLabel();
        lblEndTime = new javax.swing.JLabel();
        ftxtStartTime = new javax.swing.JFormattedTextField();
        ftxtEndTime = new javax.swing.JFormattedTextField();
        lblError = new javax.swing.JLabel();
        btnReset = new javax.swing.JButton();
        btnCancel = new javax.swing.JButton();
        btnSave = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Schedule Entry");

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));

        lblTitle.setFont(new java.awt.Font("Poppins", 0, 20)); // NOI18N
        lblTitle.setText("Create Schedule");

        lblSubtitle.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        lblSubtitle.setForeground(new java.awt.Color(120, 120, 120));
        lblSubtitle.setText("Assign offering, room, faculty, and time details.");

        lblEnrollmentPeriod.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        lblEnrollmentPeriod.setText("Enrollment Period");

        lblDay.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        lblDay.setText("Day");

        cbxEnrollmentPeriod.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N

        cbxDay.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N

        lblOffering.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        lblOffering.setText("Offering (Section + Subject)");

        txtOfferingSearch.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        txtOfferingSearch.setToolTipText("Search by section, subject code, title, or prerequisite");

        tblOfferings.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Section", "Subject Code", "Subject Name", "Prerequisite"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        });
        tblOfferings.setFillsViewportHeight(true);
        tblOfferings.setRowHeight(24);
        jScrollPane1.setViewportView(tblOfferings);

        lblRoom.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        lblRoom.setText("Room");

        lblFaculty.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        lblFaculty.setText("Faculty");

        cbxRoom.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N

        cbxFaculty.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N

        lblStartTime.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        lblStartTime.setText("Start Time (HH:mm)");

        lblEndTime.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        lblEndTime.setText("End Time (HH:mm)");

        ftxtStartTime.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        ftxtStartTime.setToolTipText("24-hour format, e.g. 08:30");

        ftxtEndTime.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        ftxtEndTime.setToolTipText("24-hour format, e.g. 10:00");

        lblError.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        lblError.setForeground(new java.awt.Color(180, 35, 24));
        lblError.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblError.setText(" ");

        btnReset.setText("Reset");
        btnReset.addActionListener(this::btnResetActionPerformed);

        btnCancel.setText("Cancel");
        btnCancel.addActionListener(this::btnCancelActionPerformed);

        btnSave.setText("Save");
        btnSave.addActionListener(this::btnSaveActionPerformed);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblTitle, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblSubtitle, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblEnrollmentPeriod)
                            .addComponent(cbxEnrollmentPeriod, javax.swing.GroupLayout.PREFERRED_SIZE, 360, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblRoom)
                            .addComponent(cbxRoom, javax.swing.GroupLayout.PREFERRED_SIZE, 360, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblStartTime)
                            .addComponent(ftxtStartTime, javax.swing.GroupLayout.PREFERRED_SIZE, 360, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(24, 24, 24)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblDay)
                            .addComponent(cbxDay, javax.swing.GroupLayout.PREFERRED_SIZE, 360, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblFaculty)
                            .addComponent(cbxFaculty, javax.swing.GroupLayout.PREFERRED_SIZE, 360, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblEndTime)
                            .addComponent(ftxtEndTime, javax.swing.GroupLayout.PREFERRED_SIZE, 360, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(lblOffering)
                    .addComponent(txtOfferingSearch)
                    .addComponent(jScrollPane1)
                    .addComponent(lblError, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(btnReset, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnCancel, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnSave, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(16, 16, 16))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addComponent(lblTitle)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblSubtitle)
                .addGap(12, 12, 12)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblEnrollmentPeriod)
                    .addComponent(lblDay))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cbxEnrollmentPeriod, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cbxDay, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(12, 12, 12)
                .addComponent(lblOffering)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtOfferingSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblRoom)
                    .addComponent(lblFaculty))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cbxRoom, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cbxFaculty, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(12, 12, 12)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblStartTime)
                    .addComponent(lblEndTime))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(ftxtStartTime, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(ftxtEndTime, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblError)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnReset)
                    .addComponent(btnCancel)
                    .addComponent(btnSave))
                .addContainerGap(16, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnResetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnResetActionPerformed
        resetFormValues();
    }//GEN-LAST:event_btnResetActionPerformed

    private void btnCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelActionPerformed
        dispose();
    }//GEN-LAST:event_btnCancelActionPerformed

    private void btnSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveActionPerformed
        saveSchedule();
    }//GEN-LAST:event_btnSaveActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCancel;
    private javax.swing.JButton btnReset;
    private javax.swing.JButton btnSave;
    private javax.swing.JComboBox<String> cbxDay;
    private javax.swing.JComboBox<String> cbxEnrollmentPeriod;
    private javax.swing.JComboBox<String> cbxFaculty;
    private javax.swing.JComboBox<String> cbxRoom;
    private javax.swing.JFormattedTextField ftxtEndTime;
    private javax.swing.JFormattedTextField ftxtStartTime;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lblDay;
    private javax.swing.JLabel lblEndTime;
    private javax.swing.JLabel lblEnrollmentPeriod;
    private javax.swing.JLabel lblError;
    private javax.swing.JLabel lblFaculty;
    private javax.swing.JLabel lblOffering;
    private javax.swing.JLabel lblRoom;
    private javax.swing.JLabel lblStartTime;
    private javax.swing.JLabel lblSubtitle;
    private javax.swing.JLabel lblTitle;
    private javax.swing.JTable tblOfferings;
    private javax.swing.JTextField txtOfferingSearch;
    // End of variables declaration//GEN-END:variables
}
