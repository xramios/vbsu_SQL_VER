/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package com.group5.paul_esys.screens.admin.panels;

import com.group5.paul_esys.modules.departments.model.Department;
import com.group5.paul_esys.modules.departments.services.DepartmentService;
import com.group5.paul_esys.modules.faculty.model.Faculty;
import com.group5.paul_esys.modules.faculty.services.FacultyService;
import com.group5.paul_esys.screens.registrar.cards.FacultyMemberCard;
import com.group5.paul_esys.screens.registrar.forms.DepartmentForm;
import com.group5.paul_esys.screens.registrar.forms.FacultyForm;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.util.concurrent.ExecutionException;

/**
 *
 * @author nytri
 */
public class AdminDepartmentManagement extends javax.swing.JPanel {

        private enum TableMode {
                DEPARTMENTS,
                FACULTY
        }

        private final DepartmentService departmentService = DepartmentService.getInstance();
        private final FacultyService facultyService = FacultyService.getInstance();

        private final JPanel facultyCardsContainer = new JPanel(new GridLayout(0, 1, 0, 8));

        private TableMode tableMode = TableMode.DEPARTMENTS;
        private Department selectedDepartment;

        private List<Department> departments = new ArrayList<>();
        private List<Department> filteredDepartments = new ArrayList<>();

        private List<Faculty> facultyMembers = new ArrayList<>();
        private List<Faculty> filteredFacultyMembers = new ArrayList<>();
        private final Map<Long, String> facultyEmailByFacultyId = new LinkedHashMap<>();

        private SwingWorker<?, ?> loadWorker;
        private SwingWorker<?, ?> tableWorker;
        private SwingWorker<?, ?> deleteWorker;

	/**
	 * Creates new form DepartmentManagement
	 */
	public AdminDepartmentManagement() {
		initComponents();
		initializeDepartmentManagementPanel();
	}

        private void initializeDepartmentManagementPanel() {
                tableDepartments.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
                tableDepartments.setRowHeight(28);

                facultyCardsContainer.setBackground(Color.WHITE);
                scrollPanelFacultyMembers.setViewportView(facultyCardsContainer);
                scrollPanelFacultyMembers.getVerticalScrollBar().setUnitIncrement(16);

                btnAddDepartment.addActionListener(this::btnAddDepartmentActionPerformed);
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

                registerTableInteractions();
                switchToDepartmentView();
        }

        private void registerTableInteractions() {
                tableDepartments.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent evt) {
                                if (!SwingUtilities.isLeftMouseButton(evt)) {
                                        return;
                                }

                                if (tableMode == TableMode.DEPARTMENTS) {
                                        openSelectedDepartmentFacultyView();
                                        return;
                                }

                                if (tableMode == TableMode.FACULTY && evt.getClickCount() == 2) {
                                        openSelectedFacultyForUpdate();
                                }
                        }
                });
        }

        private void switchToDepartmentView() {
                tableMode = TableMode.DEPARTMENTS;
                selectedDepartment = null;
                txtSearch.setText("");

                btnNavigateBack.setEnabled(false);
                btnAddDepartment.setText("Add Department");
                btnAddFaculty.setText("Add Faculty");

                lblDepartmentName.setText("Select a Department");
                lblFacultytHeadName.setText("Click a department row to view faculty members.");

                facultyCardsContainer.removeAll();
                facultyCardsContainer.revalidate();
                facultyCardsContainer.repaint();

                loadDepartments();
        }

        private void switchToFacultyView(Department department) {
                if (department == null) {
                        return;
                }

                tableMode = TableMode.FACULTY;
                selectedDepartment = department;
                txtSearch.setText("");

                btnNavigateBack.setEnabled(true);
                btnAddDepartment.setText("Update Department");
                btnAddFaculty.setText("Add Faculty");

                loadFacultyBySelectedDepartment();
                applyFilters();
        }

        private void loadDepartments() {
                if (loadWorker != null && !loadWorker.isDone()) {
                        loadWorker.cancel(true);
                }

                loadWorker = new SwingWorker<List<Department>, Void>() {
                        @Override
                        protected List<Department> doInBackground() {
                                return new ArrayList<>(departmentService.getAllDepartments());
                        }

                        @Override
                        protected void done() {
                                if (isCancelled()) {
                                        return;
                                }
                                try {
                                        departments = get();
                                        departments.sort(Comparator.comparing(department -> safeText(department.getDepartmentName(), "").toLowerCase()));
                                        applyFilters();
                                } catch (InterruptedException | ExecutionException e) {
                                        Thread.currentThread().interrupt();
                                        showError("Failed to load departments: " + e.getMessage());
                                }
                        }
                };
                loadWorker.execute();
        }

        private void loadFacultyBySelectedDepartment() {
                facultyMembers = new ArrayList<>();
                facultyEmailByFacultyId.clear();

                if (selectedDepartment == null || selectedDepartment.getId() == null) {
                        return;
                }

                if (loadWorker != null && !loadWorker.isDone()) {
                        loadWorker.cancel(true);
                }

                final Long departmentId = selectedDepartment.getId();

                loadWorker = new SwingWorker<FacultyLoadResult, Void>() {
                        @Override
                        protected FacultyLoadResult doInBackground() {
                                List<Faculty> members = new ArrayList<>(facultyService.getFacultyByDepartment(departmentId));
                                members.sort(Comparator
                                        .comparing((Faculty faculty) -> safeText(faculty.getLastName(), "").toLowerCase())
                                        .thenComparing(faculty -> safeText(faculty.getFirstName(), "").toLowerCase()));

                                Map<Long, String> emails = new LinkedHashMap<>();
                                for (Faculty faculty : members) {
                                        if (faculty.getId() == null) {
                                                continue;
                                        }
                                        String email = facultyService
                                                .getUserEmailByUserId(faculty.getUserId())
                                                .orElse("");
                                        emails.put(faculty.getId(), email);
                                }
                                return new FacultyLoadResult(members, emails);
                        }

                        @Override
                        protected void done() {
                                if (isCancelled()) {
                                        return;
                                }
                                try {
                                        FacultyLoadResult result = get();
                                        facultyMembers = result.members;
                                        facultyEmailByFacultyId.putAll(result.emails);
                                        applyFilters();
                                } catch (InterruptedException | ExecutionException e) {
                                        Thread.currentThread().interrupt();
                                        showError("Failed to load faculty: " + e.getMessage());
                                }
                        }
                };
                loadWorker.execute();
        }

        private static class FacultyLoadResult {
                final List<Faculty> members;
                final Map<Long, String> emails;

                FacultyLoadResult(List<Faculty> members, Map<Long, String> emails) {
                        this.members = members;
                        this.emails = emails;
                }
        }

        private void applyFilters() {
                String searchTerm = txtSearch.getText() == null
                        ? ""
                        : txtSearch.getText().trim().toLowerCase();

                if (tableMode == TableMode.DEPARTMENTS) {
                        filteredDepartments = departments
                                .stream()
                                .filter(department -> matchesDepartmentSearch(department, searchTerm))
                                .collect(Collectors.toList());

                        populateDepartmentsTable(filteredDepartments);
                        return;
                }

                filteredFacultyMembers = facultyMembers
                        .stream()
                        .filter(faculty -> matchesFacultySearch(faculty, searchTerm))
                        .collect(Collectors.toList());

                populateFacultyTable(filteredFacultyMembers);
                updateDepartmentSummary(filteredFacultyMembers);
                renderFacultyCards(filteredFacultyMembers);
        }

        private boolean matchesDepartmentSearch(Department department, String searchTerm) {
                if (searchTerm.isEmpty()) {
                        return true;
                }

                String name = safeText(department.getDepartmentName(), "").toLowerCase();
                String code = safeText(department.getDepartmentCode(), "").toLowerCase();
                String description = safeText(department.getDescription(), "").toLowerCase();

                return name.contains(searchTerm)
                        || code.contains(searchTerm)
                        || description.contains(searchTerm);
        }

        private boolean matchesFacultySearch(Faculty faculty, String searchTerm) {
                if (searchTerm.isEmpty()) {
                        return true;
                }

                String fullName = (safeText(faculty.getLastName(), "") + " " + safeText(faculty.getFirstName(), "")).toLowerCase();
                String email = safeText(facultyEmailByFacultyId.get(faculty.getId()), "").toLowerCase();

                return fullName.contains(searchTerm) || email.contains(searchTerm);
        }

        private void populateDepartmentsTable(List<Department> departmentsToDisplay) {
                if (tableWorker != null && !tableWorker.isDone()) {
                        tableWorker.cancel(true);
                }

                tableWorker = new SwingWorker<Map<Long, Long>, Void>() {
                        @Override
                        protected Map<Long, Long> doInBackground() {
                                return facultyService.getAllFaculty()
                                        .stream()
                                        .collect(Collectors.groupingBy(Faculty::getDepartmentId, Collectors.counting()));
                        }

                        @Override
                        protected void done() {
                                if (isCancelled()) {
                                        return;
                                }
                                try {
                                        Map<Long, Long> facultyCountByDepartment = get();
                                        updateDepartmentsTableModel(departmentsToDisplay, facultyCountByDepartment);
                                } catch (InterruptedException | ExecutionException e) {
                                        Thread.currentThread().interrupt();
                                        showError("Failed to load faculty counts: " + e.getMessage());
                                }
                        }
                };
                tableWorker.execute();
        }

        private void updateDepartmentsTableModel(List<Department> departmentsToDisplay, Map<Long, Long> facultyCountByDepartment) {
                DefaultTableModel model = new DefaultTableModel(
                        new Object[][]{},
                        new String[]{"Name", "Code", "Description", "Faculty Count"}
                ) {
                        @Override
                        public boolean isCellEditable(int rowIndex, int columnIndex) {
                                return false;
                        }
                };

                for (Department department : departmentsToDisplay) {
                        long facultyCount = facultyCountByDepartment.getOrDefault(department.getId(), 0L);
                        model.addRow(new Object[]{
                                safeText(department.getDepartmentName(), "N/A"),
                                safeText(department.getDepartmentCode(), "N/A"),
                                safeText(department.getDescription(), "-"),
                                (int) facultyCount
                        });
                }

                tableDepartments.setModel(model);
        }

        private void populateFacultyTable(List<Faculty> facultyToDisplay) {
                DefaultTableModel model = new DefaultTableModel(
                        new Object[][]{},
                        new String[]{"Faculty Name", "Email", "Position"}
                ) {
                        @Override
                        public boolean isCellEditable(int rowIndex, int columnIndex) {
                                return false;
                        }
                };

                for (Faculty faculty : facultyToDisplay) {
                        String fullName = buildFacultyDisplayName(faculty);
                        String email = safeText(facultyEmailByFacultyId.get(faculty.getId()), "No email");
                        model.addRow(new Object[]{fullName, email, "Faculty"});
                }

                tableDepartments.setModel(model);
        }

        private void updateDepartmentSummary(List<Faculty> visibleFaculty) {
                if (selectedDepartment == null) {
                        lblDepartmentName.setText("Select a Department");
                        lblFacultytHeadName.setText("Click a department row to view faculty members.");
                        return;
                }

                String departmentName = safeText(selectedDepartment.getDepartmentName(), "Department");
                String departmentCode = safeText(selectedDepartment.getDepartmentCode(), "");

                if (departmentCode.isEmpty()) {
                        lblDepartmentName.setText(departmentName);
                } else {
                        lblDepartmentName.setText(departmentName + " (" + departmentCode + ")");
                }

                lblFacultytHeadName.setText(
                        "Faculty Members: " + facultyMembers.size() + " | Showing: " + visibleFaculty.size()
                );
        }

        private void renderFacultyCards(List<Faculty> facultyToDisplay) {
                facultyCardsContainer.removeAll();

                if (facultyToDisplay.isEmpty()) {
                        JLabel emptyLabel = new JLabel("No faculty members to display.");
                        emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
                        facultyCardsContainer.add(emptyLabel);
                } else {
                        for (Faculty faculty : facultyToDisplay) {
                                String email = facultyEmailByFacultyId.getOrDefault(faculty.getId(), "");
                                FacultyMemberCard card = new FacultyMemberCard(faculty, email);
                                card.setOnUpdateAction(this::openFacultyFormForUpdate);
                                card.setOnDeleteAction(this::deleteFacultyMember);
                                facultyCardsContainer.add(card);
                        }
                }

                facultyCardsContainer.revalidate();
                facultyCardsContainer.repaint();
        }

        private void openSelectedDepartmentFacultyView() {
                Department department = getSelectedDepartmentFromTable();
                if (department == null) {
                        return;
                }

                switchToFacultyView(department);
        }

        private Department getSelectedDepartmentFromTable() {
                int selectedRow = tableDepartments.getSelectedRow();
                if (selectedRow < 0) {
                        return null;
                }

                int modelRow = tableDepartments.convertRowIndexToModel(selectedRow);
                if (modelRow < 0 || modelRow >= filteredDepartments.size()) {
                        return null;
                }

                return filteredDepartments.get(modelRow);
        }

        private Faculty getSelectedFacultyFromTable() {
                int selectedRow = tableDepartments.getSelectedRow();
                if (selectedRow < 0) {
                        return null;
                }

                int modelRow = tableDepartments.convertRowIndexToModel(selectedRow);
                if (modelRow < 0 || modelRow >= filteredFacultyMembers.size()) {
                        return null;
                }

                return filteredFacultyMembers.get(modelRow);
        }

        private void openSelectedFacultyForUpdate() {
                Faculty selectedFaculty = getSelectedFacultyFromTable();
                if (selectedFaculty == null) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Please select a faculty row to update.",
                                "Update Faculty",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return;
                }

                openFacultyFormForUpdate(selectedFaculty);
        }

        private void openFacultyFormForUpdate(Faculty faculty) {
                if (selectedDepartment == null) {
                        return;
                }

                FacultyForm form = new FacultyForm(
                        faculty,
                        selectedDepartment.getId(),
                        this::refreshCurrentView
                );
                form.setVisible(true);
        }

        private void openFacultyFormForCreate() {
                if (selectedDepartment == null) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Please select a department first.",
                                "Add Faculty",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return;
                }

                FacultyForm form = new FacultyForm(
                        null,
                        selectedDepartment.getId(),
                        this::refreshCurrentView
                );
                form.setVisible(true);
        }

        private void deleteFacultyMember(Faculty faculty) {
                if (faculty == null || faculty.getId() == null) {
                        return;
                }

                int option = JOptionPane.showConfirmDialog(
                        this,
                        "Delete faculty " + buildFacultyDisplayName(faculty) + "?",
                        "Confirm Delete",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );

                if (option != JOptionPane.YES_OPTION) {
                        return;
                }

                if (deleteWorker != null && !deleteWorker.isDone()) {
                        deleteWorker.cancel(true);
                }

                final Long facultyId = faculty.getId();

                deleteWorker = new SwingWorker<Boolean, Void>() {
                        @Override
                        protected Boolean doInBackground() {
                                return facultyService.deleteFaculty(facultyId);
                        }

                        @Override
                        protected void done() {
                                if (isCancelled()) {
                                        return;
                                }
                                try {
                                        boolean deleted = get();
                                        if (!deleted) {
                                                showError("Failed to delete faculty. Please try again.");
                                                return;
                                        }
                                        refreshCurrentView();
                                        JOptionPane.showMessageDialog(AdminDepartmentManagement.this,
                                                "Faculty deleted successfully.",
                                                "Delete Faculty",
                                                JOptionPane.INFORMATION_MESSAGE
                                        );
                                } catch (InterruptedException | ExecutionException e) {
                                        Thread.currentThread().interrupt();
                                        showError("Failed to delete faculty: " + e.getMessage());
                                }
                        }
                };
                deleteWorker.execute();
        }

        private void openDepartmentFormForCreate() {
                DepartmentForm form = new DepartmentForm(null, this::refreshCurrentView);
                form.setVisible(true);
        }

        private void openDepartmentFormForUpdate() {
                if (selectedDepartment == null) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Please select a department first.",
                                "Update Department",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return;
                }

                DepartmentForm form = new DepartmentForm(selectedDepartment, this::refreshCurrentView);
                form.setVisible(true);
        }

        private void refreshCurrentView() {
                if (tableMode == TableMode.FACULTY && selectedDepartment != null) {
                        final Long deptId = selectedDepartment.getId();

                        if (loadWorker != null && !loadWorker.isDone()) {
                                loadWorker.cancel(true);
                        }

                        loadWorker = new SwingWorker<Department, Void>() {
                                @Override
                                protected Department doInBackground() {
                                        return departmentService
                                                .getDepartmentById(deptId)
                                                .orElse(null);
                                }

                                @Override
                                protected void done() {
                                        if (isCancelled()) {
                                                return;
                                        }
                                        try {
                                                Department refreshed = get();
                                                if (refreshed != null) {
                                                        selectedDepartment = refreshed;
                                                }
                                                loadFacultyBySelectedDepartment();
                                        } catch (InterruptedException | ExecutionException e) {
                                                Thread.currentThread().interrupt();
                                                showError("Failed to refresh view: " + e.getMessage());
                                        }
                                }
                        };
                        loadWorker.execute();
                        return;
                }

                loadDepartments();
        }

        private String buildFacultyDisplayName(Faculty faculty) {
                String firstName = safeText(faculty.getFirstName(), "");
                String lastName = safeText(faculty.getLastName(), "");
                String fullName = (lastName + ", " + firstName).replaceAll("^[,\\s]+|[,\\s]+$", "");
                return fullName.isEmpty() ? "Unknown Faculty" : fullName;
        }

        private String safeText(String value, String fallback) {
                if (value == null || value.trim().isEmpty()) {
                        return fallback;
                }
                return value.trim();
        }

        private void showError(String message) {
                SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(AdminDepartmentManagement.this,
                                message,
                                "Error",
                                JOptionPane.ERROR_MESSAGE
                        );
                });
        }

        private void btnAddDepartmentActionPerformed(java.awt.event.ActionEvent evt) {
                if (tableMode == TableMode.FACULTY) {
                        openDepartmentFormForUpdate();
                        return;
                }

                openDepartmentFormForCreate();
        }

	/**
	 * This method is called from within the constructor to initialize the
	 * form. WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
        // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
        private void initComponents() {

                jLabel1 = new javax.swing.JLabel();
                jLabel2 = new javax.swing.JLabel();
                jPanel1 = new javax.swing.JPanel();
                jScrollPane2 = new javax.swing.JScrollPane();
                tableDepartments = new javax.swing.JTable();
                jLabel3 = new javax.swing.JLabel();
                txtSearch = new javax.swing.JTextField();
                btnAddDepartment = new javax.swing.JButton();
                btnAddFaculty = new javax.swing.JButton();
                btnNavigateBack = new javax.swing.JButton();
                jPanel2 = new javax.swing.JPanel();
                scrollPanelFacultyMembers = new javax.swing.JScrollPane();
                panelFaculties = new javax.swing.JPanel();
                lblDepartmentName = new javax.swing.JLabel();
                lblFacultytHeadName = new javax.swing.JLabel();

                setBackground(new java.awt.Color(255, 255, 255));
                setMaximumSize(new java.awt.Dimension(1181, 684));

                jLabel1.setFont(new java.awt.Font("Poppins", 0, 24)); // NOI18N
                jLabel1.setText("Department Management");

                jLabel2.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                jLabel2.setForeground(new java.awt.Color(153, 153, 153));
                jLabel2.setText("Manage academic department and their heads.");

                jPanel1.setBackground(new java.awt.Color(255, 255, 255));
                jPanel1.setBorder(new com.group5.paul_esys.ui.PanelRoundBorder());

                tableDepartments.setModel(new javax.swing.table.DefaultTableModel(
                        new Object [][] {

                        },
                        new String [] {
                                "Name", "Code", "Description", "Faculty Count"
                        }
                ) {
                        Class[] types = new Class [] {
                                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Integer.class
                        };
                        boolean[] canEdit = new boolean [] {
                                false, false, false, false
                        };

                        public Class getColumnClass(int columnIndex) {
                                return types [columnIndex];
                        }

                        public boolean isCellEditable(int rowIndex, int columnIndex) {
                                return canEdit [columnIndex];
                        }
                });
                jScrollPane2.setViewportView(tableDepartments);

                jLabel3.setText("Search");

                txtSearch.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());

                btnAddDepartment.setBackground(new java.awt.Color(119, 0, 0));
                btnAddDepartment.setForeground(new java.awt.Color(255, 255, 255));
                btnAddDepartment.setText("Add Department");

                btnAddFaculty.setBackground(new java.awt.Color(119, 0, 0));
                btnAddFaculty.setForeground(new java.awt.Color(255, 255, 255));
                btnAddFaculty.setText("Add Faculty");
                btnAddFaculty.addActionListener(this::btnAddFacultyActionPerformed);

                btnNavigateBack.setBackground(new java.awt.Color(119, 0, 0));
                btnNavigateBack.setForeground(new java.awt.Color(255, 255, 255));
                btnNavigateBack.setText("< Back");
                btnNavigateBack.addActionListener(this::btnNavigateBackActionPerformed);

                javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
                jPanel1.setLayout(jPanel1Layout);
                jPanel1Layout.setHorizontalGroup(
                        jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 717, Short.MAX_VALUE)
                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                                .addComponent(btnNavigateBack)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jLabel3)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(txtSearch)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(btnAddFaculty)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(btnAddDepartment)))
                                .addContainerGap())
                );
                jPanel1Layout.setVerticalGroup(
                        jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel3)
                                        .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(btnAddDepartment)
                                        .addComponent(btnAddFaculty)
                                        .addComponent(btnNavigateBack))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jScrollPane2)
                                .addContainerGap())
                );

                jPanel2.setBackground(new java.awt.Color(255, 255, 255));
                jPanel2.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));

                scrollPanelFacultyMembers.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));

                panelFaculties.setBackground(new java.awt.Color(255, 255, 255));
                panelFaculties.setBorder(new com.group5.paul_esys.ui.RoundShadowBorder());

                lblDepartmentName.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                lblDepartmentName.setText("Department Name");

                lblFacultytHeadName.setText("Faculty Head Name");

                javax.swing.GroupLayout panelFacultiesLayout = new javax.swing.GroupLayout(panelFaculties);
                panelFaculties.setLayout(panelFacultiesLayout);
                panelFacultiesLayout.setHorizontalGroup(
                        panelFacultiesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelFacultiesLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(panelFacultiesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(lblFacultytHeadName, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(lblDepartmentName, javax.swing.GroupLayout.DEFAULT_SIZE, 398, Short.MAX_VALUE))
                                .addContainerGap())
                );
                panelFacultiesLayout.setVerticalGroup(
                        panelFacultiesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelFacultiesLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(lblDepartmentName)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(lblFacultytHeadName)
                                .addContainerGap(24, Short.MAX_VALUE))
                );

                javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
                jPanel2.setLayout(jPanel2Layout);
                jPanel2Layout.setHorizontalGroup(
                        jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                        .addComponent(panelFaculties, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(scrollPanelFacultyMembers))
                                .addContainerGap())
                );
                jPanel2Layout.setVerticalGroup(
                        jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(panelFaculties, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(scrollPanelFacultyMembers)
                                .addContainerGap())
                );

                javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
                this.setLayout(layout);
                layout.setHorizontalGroup(
                        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGroup(layout.createSequentialGroup()
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(jLabel2)
                                                        .addComponent(jLabel1))
                                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                );
                layout.setVerticalGroup(
                        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap())
                );
        }// </editor-fold>//GEN-END:initComponents

        private void btnNavigateBackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNavigateBackActionPerformed
                if (tableMode == TableMode.FACULTY) {
                        switchToDepartmentView();
                }
        }//GEN-LAST:event_btnNavigateBackActionPerformed

        private void btnAddFacultyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddFacultyActionPerformed
                if (tableMode == TableMode.DEPARTMENTS) {
                        Department department = getSelectedDepartmentFromTable();
                        if (department == null) {
                                JOptionPane.showMessageDialog(
                                        this,
                                        "Select a department row first before adding faculty.",
                                        "Add Faculty",
                                        JOptionPane.WARNING_MESSAGE
                                );
                                return;
                        }

                        switchToFacultyView(department);
                }

                openFacultyFormForCreate();
        }//GEN-LAST:event_btnAddFacultyActionPerformed


        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JButton btnAddDepartment;
        private javax.swing.JButton btnAddFaculty;
        private javax.swing.JButton btnNavigateBack;
        private javax.swing.JLabel jLabel1;
        private javax.swing.JLabel jLabel2;
        private javax.swing.JLabel jLabel3;
        private javax.swing.JPanel jPanel1;
        private javax.swing.JPanel jPanel2;
        private javax.swing.JScrollPane jScrollPane2;
        private javax.swing.JLabel lblDepartmentName;
        private javax.swing.JLabel lblFacultytHeadName;
        private javax.swing.JPanel panelFaculties;
        private javax.swing.JScrollPane scrollPanelFacultyMembers;
        private javax.swing.JTable tableDepartments;
        private javax.swing.JTextField txtSearch;
        // End of variables declaration//GEN-END:variables
}
