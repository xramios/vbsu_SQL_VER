package com.group5.paul_esys.screens.registrar.panels;

import com.group5.paul_esys.modules.registrar.model.ScheduleLookupOption;
import com.group5.paul_esys.modules.registrar.model.ScheduleManagementRow;
import com.group5.paul_esys.modules.registrar.model.ScheduleOfferingOption;
import com.group5.paul_esys.modules.registrar.model.ScheduleSaveResult;
import com.group5.paul_esys.modules.registrar.model.ScheduleUpsertRequest;
import com.group5.paul_esys.modules.registrar.services.RegistrarScheduleManagementService;
import com.group5.paul_esys.screens.registrar.forms.ScheduleEntryDialog;
import com.group5.paul_esys.ui.PanelRoundBorder;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public class RegistrarSchedulesManagement extends javax.swing.JPanel {

  private static final String FILTER_ALL = "ALL";

  private final RegistrarScheduleManagementService scheduleManagementService =
      RegistrarScheduleManagementService.getInstance();

  private final JTextField txtSearch = new JTextField();
  private final JComboBox<String> cbxDay = new JComboBox<>();
  private final JComboBox<String> cbxEnrollmentPeriod = new JComboBox<>();
  private final JButton btnNewSchedule = new JButton("New Schedule");
  private final JButton btnEditSchedule = new JButton("Edit Schedule");
  private final JButton btnDeleteSchedule = new JButton("Delete Schedule");
  private final JButton btnRefresh = new JButton("Refresh");
  private final JButton btnClearFilter = new JButton("Clear Filter");

  private final JLabel lblTableSummary = new JLabel("Showing 0 of 0 schedules");
  private final JPanel panelConflictWarning = new JPanel(new BorderLayout());
  private final JLabel lblConflictWarning = new JLabel("No conflict detected.");

  private final JLabel lblValueSection = new JLabel("N/A");
  private final JLabel lblValueSubject = new JLabel("N/A");
  private final JLabel lblValuePeriod = new JLabel("N/A");
  private final JLabel lblValueDay = new JLabel("N/A");
  private final JLabel lblValueTime = new JLabel("N/A");
  private final JLabel lblValueRoom = new JLabel("N/A");
  private final JLabel lblValueFaculty = new JLabel("N/A");
  private final JLabel lblValueConflict = new JLabel("NONE");

  private final JPopupMenu popupMenu = new JPopupMenu();
  private final JMenuItem menuItemEdit = new JMenuItem("Edit Schedule");
  private final JMenuItem menuItemDelete = new JMenuItem("Delete Schedule");

  private final Map<String, Long> enrollmentPeriodIdByLabel = new LinkedHashMap<>();

  private final List<ScheduleManagementRow> scheduleRows = new ArrayList<>();
  private final List<ScheduleManagementRow> filteredScheduleRows = new ArrayList<>();

  private DefaultTableModel tableModel;
  private JTable tableSchedules;

  public RegistrarSchedulesManagement() {
    initComponents();
    buildRuntimeUi();
    initializeSchedulePanel();
  }

  private void buildRuntimeUi() {
    jLabel2.setText("Manage class schedules by offering, room, faculty, and meeting time.");

    jPanel1.removeAll();
    jPanel1.setLayout(new BorderLayout(0, 10));

    JPanel filterPanel = buildFilterPanel();
    JSplitPane splitPane = buildMainSplitPane();

    lblTableSummary.setFont(new java.awt.Font("Poppins", 0, 12));
    lblTableSummary.setForeground(new Color(95, 95, 95));

    jPanel1.add(filterPanel, BorderLayout.NORTH);
    jPanel1.add(splitPane, BorderLayout.CENTER);
    jPanel1.add(lblTableSummary, BorderLayout.SOUTH);

    jPanel1.revalidate();
    jPanel1.repaint();
  }

  private JPanel buildFilterPanel() {
    JPanel panel = new JPanel(new GridBagLayout());
    panel.setOpaque(false);

    JLabel lblSearch = new JLabel("Search");
    JLabel lblDay = new JLabel("Day");
    JLabel lblPeriod = new JLabel("Enrollment Period");

    txtSearch.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());
    txtSearch.setFont(new java.awt.Font("Poppins", 0, 12));

    cbxDay.setFont(new java.awt.Font("Poppins", 0, 12));
    cbxEnrollmentPeriod.setFont(new java.awt.Font("Poppins", 0, 12));

    btnNewSchedule.setBackground(new Color(119, 0, 0));
    btnNewSchedule.setForeground(Color.WHITE);
    btnEditSchedule.setBackground(new Color(119, 0, 0));
    btnEditSchedule.setForeground(Color.WHITE);
    btnDeleteSchedule.setBackground(new Color(119, 0, 0));
    btnDeleteSchedule.setForeground(Color.WHITE);
    btnRefresh.setBackground(new Color(119, 0, 0));
    btnRefresh.setForeground(Color.WHITE);
    btnClearFilter.setBackground(new Color(245, 245, 245));

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(0, 0, 0, 6);
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;

    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.weightx = 0;
    panel.add(lblSearch, gbc);

    gbc.gridx = 1;
    gbc.weightx = 0.35;
    panel.add(txtSearch, gbc);

    gbc.gridx = 2;
    gbc.weightx = 0;
    panel.add(lblDay, gbc);

    gbc.gridx = 3;
    gbc.weightx = 0.1;
    panel.add(cbxDay, gbc);

    gbc.gridx = 4;
    gbc.weightx = 0;
    panel.add(lblPeriod, gbc);

    gbc.gridx = 5;
    gbc.weightx = 0.35;
    panel.add(cbxEnrollmentPeriod, gbc);

    JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
    actionsPanel.setOpaque(false);
    actionsPanel.add(btnClearFilter);
    actionsPanel.add(btnRefresh);
    actionsPanel.add(btnNewSchedule);
    actionsPanel.add(btnEditSchedule);
    actionsPanel.add(btnDeleteSchedule);

    gbc.gridx = 6;
    gbc.weightx = 0.2;
    panel.add(actionsPanel, gbc);

    return panel;
  }

  private JSplitPane buildMainSplitPane() {
    configureScheduleTableComponent();

    JScrollPane scrollPane = new JScrollPane(tableSchedules);
    scrollPane.setBorder(new PanelRoundBorder());

    JPanel leftPanel = new JPanel(new BorderLayout(0, 6));
    leftPanel.setOpaque(false);
    JLabel lblListTitle = new JLabel("Schedule List");
    lblListTitle.setFont(new java.awt.Font("Poppins", 0, 16));
    leftPanel.add(lblListTitle, BorderLayout.NORTH);
    leftPanel.add(scrollPane, BorderLayout.CENTER);

    JPanel summaryPanel = buildSummaryPanel();

    JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, summaryPanel);
    splitPane.setResizeWeight(0.75);
    splitPane.setBorder(null);

    return splitPane;
  }

  private JPanel buildSummaryPanel() {
    JPanel panel = new JPanel(new GridBagLayout());
    panel.setBackground(Color.WHITE);
    panel.setBorder(new PanelRoundBorder());

    JLabel lblSummaryTitle = new JLabel("Selected Schedule Summary");
    lblSummaryTitle.setFont(new java.awt.Font("Poppins", 0, 16));

    panelConflictWarning.setBackground(new Color(255, 244, 228));
    panelConflictWarning.setBorder(javax.swing.BorderFactory.createLineBorder(new Color(255, 212, 168)));
    lblConflictWarning.setFont(new java.awt.Font("Poppins", 0, 12));
    lblConflictWarning.setForeground(new Color(140, 70, 0));
    panelConflictWarning.add(lblConflictWarning, BorderLayout.CENTER);
    panelConflictWarning.setVisible(false);

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(8, 10, 4, 10);
    gbc.anchor = GridBagConstraints.NORTHWEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;

    gbc.gridx = 0;
    gbc.gridy = 0;
    panel.add(lblSummaryTitle, gbc);

    int row = 1;
    addSummaryRow(panel, row++, "Section", lblValueSection);
    addSummaryRow(panel, row++, "Subject", lblValueSubject);
    addSummaryRow(panel, row++, "Enrollment Period", lblValuePeriod);
    addSummaryRow(panel, row++, "Day", lblValueDay);
    addSummaryRow(panel, row++, "Time", lblValueTime);
    addSummaryRow(panel, row++, "Room", lblValueRoom);
    addSummaryRow(panel, row++, "Faculty", lblValueFaculty);
    addSummaryRow(panel, row++, "Conflict", lblValueConflict);

    gbc.gridx = 0;
    gbc.gridy = row;
    gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.BOTH;
    panel.add(Box.createGlue(), gbc);

    gbc.gridy = row + 1;
    gbc.weighty = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    panel.add(panelConflictWarning, gbc);

    return panel;
  }

  private void addSummaryRow(JPanel panel, int row, String labelText, JLabel valueLabel) {
    JLabel label = new JLabel(labelText);
    label.setFont(new java.awt.Font("Poppins", 0, 12));
    valueLabel.setFont(new java.awt.Font("Poppins", 0, 12));

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridy = row;
    gbc.insets = new Insets(2, 10, 2, 10);
    gbc.anchor = GridBagConstraints.WEST;

    gbc.gridx = 0;
    gbc.weightx = 0;
    panel.add(label, gbc);

    gbc.gridx = 1;
    gbc.weightx = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    panel.add(valueLabel, gbc);
  }

  private void configureScheduleTableComponent() {
    tableModel = new DefaultTableModel(
        new Object[][]{},
        new String[]{
            "Subject Code",
            "Subject Name",
            "Section",
            "Day",
            "Time",
            "Room",
            "Faculty",
            "Enrollment Period",
            "Conflict"
        }
    ) {
      @Override
      public boolean isCellEditable(int row, int column) {
        return false;
      }
    };

    tableSchedules = new JTable(tableModel);
    tableSchedules.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    tableSchedules.setRowHeight(28);
    tableSchedules.setAutoCreateRowSorter(false);
  }

  private void initializeSchedulePanel() {
    configurePopupMenu();
    configureTableInteractionListeners();
    configureFilterListeners();
    configureButtonListeners();
    configureConflictRenderer();

    initializeDayFilterOptions();
    reloadSchedules();
  }

  private void configurePopupMenu() {
    menuItemEdit.addActionListener(evt -> openUpdateScheduleDialog());
    menuItemDelete.addActionListener(evt -> deleteSelectedSchedule());

    popupMenu.add(menuItemEdit);
    popupMenu.add(menuItemDelete);
    tableSchedules.setComponentPopupMenu(popupMenu);
  }

  private void configureTableInteractionListeners() {
    tableSchedules.getSelectionModel().addListSelectionListener(evt -> {
      if (!evt.getValueIsAdjusting()) {
        updateScheduleSummary();
      }
    });

    tableSchedules.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent evt) {
        selectRowFromPointer(evt);
      }

      @Override
      public void mouseReleased(MouseEvent evt) {
        selectRowFromPointer(evt);
      }

      @Override
      public void mouseClicked(MouseEvent evt) {
        if (SwingUtilities.isLeftMouseButton(evt) && evt.getClickCount() == 2) {
          openUpdateScheduleDialog();
        }
      }
    });
  }

  private void configureFilterListeners() {
    txtSearch.getDocument().addDocumentListener(new DocumentListener() {
      @Override
      public void insertUpdate(DocumentEvent e) {
        applyFilters();
      }

      @Override
      public void removeUpdate(DocumentEvent e) {
        applyFilters();
      }

      @Override
      public void changedUpdate(DocumentEvent e) {
        applyFilters();
      }
    });

    cbxDay.addItemListener(evt -> {
      if (evt.getStateChange() == ItemEvent.SELECTED) {
        applyFilters();
      }
    });

    cbxEnrollmentPeriod.addItemListener(evt -> {
      if (evt.getStateChange() == ItemEvent.SELECTED) {
        applyFilters();
      }
    });
  }

  private void configureButtonListeners() {
    btnClearFilter.addActionListener(evt -> clearFilters());
    btnRefresh.addActionListener(evt -> reloadSchedules());
    btnNewSchedule.addActionListener(evt -> openCreateScheduleDialog());
    btnEditSchedule.addActionListener(evt -> openUpdateScheduleDialog());
    btnDeleteSchedule.addActionListener(evt -> deleteSelectedSchedule());
  }

  private void configureConflictRenderer() {
    DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
      @Override
      public Component getTableCellRendererComponent(
          JTable table,
          Object value,
          boolean isSelected,
          boolean hasFocus,
          int row,
          int column
      ) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        if (!isSelected) {
          ScheduleManagementRow scheduleRow = getRowByViewIndex(row);
          if (scheduleRow != null && scheduleRow.hasConflict()) {
            setBackground(new Color(255, 245, 232));
          } else {
            setBackground(Color.WHITE);
          }
          setForeground(new Color(38, 38, 38));
        }

        if (column == 8) {
          setHorizontalAlignment(SwingConstants.CENTER);
        } else {
          setHorizontalAlignment(SwingConstants.LEFT);
        }

        return this;
      }
    };

    for (int column = 0; column < tableSchedules.getColumnModel().getColumnCount(); column++) {
      tableSchedules.getColumnModel().getColumn(column).setCellRenderer(renderer);
    }
  }

  private void initializeDayFilterOptions() {
    cbxDay.removeAllItems();
    cbxDay.addItem(FILTER_ALL);
    cbxDay.addItem("MON");
    cbxDay.addItem("TUE");
    cbxDay.addItem("WED");
    cbxDay.addItem("THU");
    cbxDay.addItem("FRI");
    cbxDay.addItem("SAT");
    cbxDay.addItem("SUN");
    cbxDay.setSelectedItem(FILTER_ALL);
  }

  private void reloadSchedules() {
    Long selectedScheduleId = getSelectedScheduleId();

    scheduleRows.clear();
    scheduleRows.addAll(scheduleManagementService.getScheduleRows());

    reloadEnrollmentPeriodFilterOptions();
    applyFilters();

    selectScheduleById(selectedScheduleId);
  }

  private void reloadEnrollmentPeriodFilterOptions() {
    String selectedLabel = cbxEnrollmentPeriod.getSelectedItem() == null
        ? FILTER_ALL
        : cbxEnrollmentPeriod.getSelectedItem().toString();

    enrollmentPeriodIdByLabel.clear();
    cbxEnrollmentPeriod.removeAllItems();
    cbxEnrollmentPeriod.addItem(FILTER_ALL);

    for (ScheduleLookupOption option : scheduleManagementService.getEnrollmentPeriodOptions()) {
      if (option.id() == null) {
        continue;
      }

      cbxEnrollmentPeriod.addItem(option.label());
      enrollmentPeriodIdByLabel.put(option.label(), option.id());
    }

    if (FILTER_ALL.equals(selectedLabel) || enrollmentPeriodIdByLabel.containsKey(selectedLabel)) {
      cbxEnrollmentPeriod.setSelectedItem(selectedLabel);
    } else {
      cbxEnrollmentPeriod.setSelectedItem(FILTER_ALL);
    }
  }

  private void applyFilters() {
    String keyword = txtSearch.getText() == null ? "" : txtSearch.getText().trim().toLowerCase();
    String selectedDay = cbxDay.getSelectedItem() == null ? FILTER_ALL : cbxDay.getSelectedItem().toString();
    String selectedPeriodLabel = cbxEnrollmentPeriod.getSelectedItem() == null
        ? FILTER_ALL
        : cbxEnrollmentPeriod.getSelectedItem().toString();
    Long selectedPeriodId = enrollmentPeriodIdByLabel.get(selectedPeriodLabel);

    filteredScheduleRows.clear();

    for (ScheduleManagementRow row : scheduleRows) {
      if (!matchesSearch(row, keyword)) {
        continue;
      }

      if (!matchesDay(row, selectedDay)) {
        continue;
      }

      if (!matchesEnrollmentPeriod(row, selectedPeriodLabel, selectedPeriodId)) {
        continue;
      }

      filteredScheduleRows.add(row);
    }

    populateTable();
    updateTableSummary();
    updateScheduleSummary();
  }

  private boolean matchesSearch(ScheduleManagementRow row, String keyword) {
    if (keyword.isEmpty()) {
      return true;
    }

    return row.searchableText().contains(keyword);
  }

  private boolean matchesDay(ScheduleManagementRow row, String selectedDay) {
    if (FILTER_ALL.equals(selectedDay)) {
      return true;
    }

    return selectedDay.equalsIgnoreCase(row.day());
  }

  private boolean matchesEnrollmentPeriod(
      ScheduleManagementRow row,
      String selectedPeriodLabel,
      Long selectedPeriodId
  ) {
    if (FILTER_ALL.equals(selectedPeriodLabel)) {
      return true;
    }

    if (selectedPeriodId == null) {
      return false;
    }

    return selectedPeriodId.equals(row.enrollmentPeriodId());
  }

  private void populateTable() {
    tableModel.setRowCount(0);

    for (ScheduleManagementRow row : filteredScheduleRows) {
      tableModel.addRow(new Object[]{
          row.subjectCode(),
          row.subjectName(),
          row.sectionCode(),
          row.day(),
          row.timeRangeLabel(),
          row.roomDisplay(),
          row.facultyDisplay(),
          row.enrollmentPeriodLabel(),
          row.conflictLabel()
      });
    }
  }

  private void updateTableSummary() {
    String selectedDay = cbxDay.getSelectedItem() == null ? FILTER_ALL : cbxDay.getSelectedItem().toString();
    String selectedPeriod = cbxEnrollmentPeriod.getSelectedItem() == null
        ? FILTER_ALL
        : cbxEnrollmentPeriod.getSelectedItem().toString();
    String keyword = txtSearch.getText() == null ? "" : txtSearch.getText().trim();

    StringBuilder filters = new StringBuilder();

    if (!keyword.isEmpty()) {
      filters.append(" search=").append(keyword);
    }

    if (!FILTER_ALL.equals(selectedDay)) {
      filters.append(" day=").append(selectedDay);
    }

    if (!FILTER_ALL.equals(selectedPeriod)) {
      filters.append(" period=").append(selectedPeriod);
    }

    lblTableSummary.setText(
        "Showing " + filteredScheduleRows.size() + " of " + scheduleRows.size() + " schedules"
            + (filters.isEmpty() ? "" : " | Active filters:" + filters)
    );
  }

  private void clearFilters() {
    txtSearch.setText("");
    cbxDay.setSelectedItem(FILTER_ALL);
    cbxEnrollmentPeriod.setSelectedItem(FILTER_ALL);
    applyFilters();
  }

  private void updateScheduleSummary() {
    ScheduleManagementRow selected = getSelectedSchedule();
    if (selected == null) {
      setSummaryValues("N/A", "N/A", "N/A", "N/A", "N/A", "N/A", "N/A", "NONE");
      panelConflictWarning.setVisible(false);
      return;
    }

    setSummaryValues(
        selected.sectionCode(),
        selected.subjectCode() + " - " + selected.subjectName(),
        selected.enrollmentPeriodLabel(),
        selected.day(),
        selected.timeRangeLabel(),
        selected.roomDisplay(),
        selected.facultyDisplay(),
        selected.conflictLabel()
    );

    if (selected.hasConflict()) {
      lblConflictWarning.setText(buildConflictMessage(selected));
      panelConflictWarning.setVisible(true);
    } else {
      panelConflictWarning.setVisible(false);
    }
  }

  private void setSummaryValues(
      String section,
      String subject,
      String period,
      String day,
      String time,
      String room,
      String faculty,
      String conflict
  ) {
    lblValueSection.setText(section);
    lblValueSubject.setText(subject);
    lblValuePeriod.setText(period);
    lblValueDay.setText(day);
    lblValueTime.setText(time);
    lblValueRoom.setText(room);
    lblValueFaculty.setText(faculty);
    lblValueConflict.setText(conflict);
  }

  private String buildConflictMessage(ScheduleManagementRow row) {
    if (row.roomConflict() && row.facultyConflict()) {
      return "Potential room and faculty overlap detected on " + row.day() + " at " + row.timeRangeLabel() + ".";
    }

    if (row.roomConflict()) {
      return "Potential room overlap detected on " + row.day() + " at " + row.timeRangeLabel() + ".";
    }

    return "Potential faculty overlap detected on " + row.day() + " at " + row.timeRangeLabel() + ".";
  }

  private void openCreateScheduleDialog() {
    openScheduleDialog(null);
  }

  private void openUpdateScheduleDialog() {
    ScheduleManagementRow selected = getSelectedSchedule();
    if (selected == null) {
      JOptionPane.showMessageDialog(
          this,
          "Please select a schedule to update.",
          "Update Schedule",
          JOptionPane.WARNING_MESSAGE
      );
      return;
    }

    openScheduleDialog(selected);
  }

  private void openScheduleDialog(ScheduleManagementRow editingSchedule) {
    List<ScheduleOfferingOption> offeringOptions = scheduleManagementService.getOfferingOptions();
    if (offeringOptions.isEmpty()) {
      JOptionPane.showMessageDialog(
          this,
          "No offerings are available yet. Create offerings first before adding schedules.",
          "Schedules Management",
          JOptionPane.WARNING_MESSAGE
      );
      return;
    }

    List<ScheduleLookupOption> roomOptions = scheduleManagementService.getRoomOptions();
    List<ScheduleLookupOption> facultyOptions = scheduleManagementService.getFacultyOptions();

    Frame parentFrame = resolveParentFrame();
    ScheduleEntryDialog dialog = new ScheduleEntryDialog(
        parentFrame,
        offeringOptions,
        roomOptions,
        facultyOptions,
        editingSchedule
    );

    dialog.setVisible(true);
    ScheduleUpsertRequest request = dialog.getSubmission();

    if (request == null) {
      return;
    }

    ScheduleSaveResult result = editingSchedule == null
        ? scheduleManagementService.createSchedule(request)
        : scheduleManagementService.updateSchedule(request);

    JOptionPane.showMessageDialog(
        this,
        result.message(),
        "Schedules Management",
        result.successful() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE
    );

    if (result.successful()) {
      reloadSchedules();
      if (editingSchedule != null) {
        selectScheduleById(editingSchedule.scheduleId());
      }
    }
  }

  private void deleteSelectedSchedule() {
    ScheduleManagementRow selected = getSelectedSchedule();
    if (selected == null) {
      JOptionPane.showMessageDialog(
          this,
          "Please select a schedule to delete.",
          "Delete Schedule",
          JOptionPane.WARNING_MESSAGE
      );
      return;
    }

    int confirm = JOptionPane.showConfirmDialog(
        this,
        "Delete schedule for " + selected.subjectCode() + " / " + selected.sectionCode() + "?",
        "Confirm Delete",
        JOptionPane.YES_NO_OPTION,
        JOptionPane.WARNING_MESSAGE
    );

    if (confirm != JOptionPane.YES_OPTION) {
      return;
    }

    ScheduleSaveResult result = scheduleManagementService.deleteSchedule(selected.scheduleId());

    JOptionPane.showMessageDialog(
        this,
        result.message(),
        "Delete Schedule",
        result.successful() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE
    );

    if (result.successful()) {
      reloadSchedules();
    }
  }

  private void selectRowFromPointer(MouseEvent evt) {
    if (!evt.isPopupTrigger()) {
      return;
    }

    int row = tableSchedules.rowAtPoint(evt.getPoint());
    if (row >= 0) {
      tableSchedules.setRowSelectionInterval(row, row);
    }
  }

  private ScheduleManagementRow getSelectedSchedule() {
    int selectedRow = tableSchedules.getSelectedRow();
    if (selectedRow < 0) {
      return null;
    }

    int modelRow = tableSchedules.convertRowIndexToModel(selectedRow);
    if (modelRow < 0 || modelRow >= filteredScheduleRows.size()) {
      return null;
    }

    return filteredScheduleRows.get(modelRow);
  }

  private ScheduleManagementRow getRowByViewIndex(int viewRowIndex) {
    if (viewRowIndex < 0) {
      return null;
    }

    int modelRow = tableSchedules.convertRowIndexToModel(viewRowIndex);
    if (modelRow < 0 || modelRow >= filteredScheduleRows.size()) {
      return null;
    }

    return filteredScheduleRows.get(modelRow);
  }

  private Long getSelectedScheduleId() {
    ScheduleManagementRow selected = getSelectedSchedule();
    return selected == null ? null : selected.scheduleId();
  }

  private void selectScheduleById(Long scheduleId) {
    if (scheduleId == null) {
      return;
    }

    for (int modelRow = 0; modelRow < filteredScheduleRows.size(); modelRow++) {
      ScheduleManagementRow row = filteredScheduleRows.get(modelRow);
      if (scheduleId.equals(row.scheduleId())) {
        int viewRow = tableSchedules.convertRowIndexToView(modelRow);
        if (viewRow >= 0) {
          tableSchedules.setRowSelectionInterval(viewRow, viewRow);
        }
        return;
      }
    }
  }

  private Frame resolveParentFrame() {
    Window ancestor = SwingUtilities.getWindowAncestor(this);
    if (ancestor instanceof Frame frame) {
      return frame;
    }

    return null;
  }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

    jLabel2 = new javax.swing.JLabel();
    jLabel1 = new javax.swing.JLabel();
    jPanel1 = new javax.swing.JPanel();
    jLabel3 = new javax.swing.JLabel();
    jScrollPane1 = new javax.swing.JScrollPane();
    jTable1 = new javax.swing.JTable();

    setBackground(new java.awt.Color(255, 255, 255));

    jLabel2.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
    jLabel2.setForeground(new java.awt.Color(153, 153, 153));
    jLabel2.setText("Manage Schedules");

    jLabel1.setFont(new java.awt.Font("Poppins", 0, 24)); // NOI18N
    jLabel1.setText("Schedules Management");

    jPanel1.setBackground(new java.awt.Color(255, 255, 255));
    jPanel1.setBorder(new com.group5.paul_esys.ui.PanelRoundBorder());

    jLabel3.setFont(new java.awt.Font("Poppins", 0, 18)); // NOI18N
    jLabel3.setText("Schedule List");

    jTable1.setModel(new javax.swing.table.DefaultTableModel(
      new Object[][] {
        {null, null, null, null},
        {null, null, null, null},
        {null, null, null, null},
        {null, null, null, null}
      },
      new String[] {
        "Title 1", "Title 2", "Title 3", "Title 4"
      }
    ));
    jScrollPane1.setViewportView(jTable1);

    javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
    jPanel1.setLayout(jPanel1Layout);
    jPanel1Layout.setHorizontalGroup(
      jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(jPanel1Layout.createSequentialGroup()
          .addContainerGap()
          .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1)
            .addGroup(jPanel1Layout.createSequentialGroup()
              .addComponent(jLabel3)
              .addGap(0, 0, Short.MAX_VALUE)))
          .addContainerGap())
    );
    jPanel1Layout.setVerticalGroup(
      jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(jPanel1Layout.createSequentialGroup()
          .addContainerGap()
          .addComponent(jLabel3)
          .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
          .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 549, Short.MAX_VALUE)
          .addContainerGap())
    );

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
    this.setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(layout.createSequentialGroup()
          .addContainerGap()
          .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
              .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jLabel1)
                .addComponent(jLabel2))
              .addGap(0, 868, Short.MAX_VALUE)))
          .addContainerGap())
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(layout.createSequentialGroup()
          .addContainerGap()
          .addComponent(jLabel1)
          .addGap(6, 6, 6)
          .addComponent(jLabel2)
          .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
          .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
          .addContainerGap())
    );
    }
    // </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    // End of variables declaration//GEN-END:variables
}
