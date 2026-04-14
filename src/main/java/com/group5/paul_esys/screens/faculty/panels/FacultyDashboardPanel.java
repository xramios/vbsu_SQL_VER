/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package com.group5.paul_esys.screens.faculty.panels;

import com.group5.paul_esys.modules.faculty.model.Faculty;
import com.group5.paul_esys.modules.faculty.model.FacultyClassListRow;
import com.group5.paul_esys.modules.faculty.services.FacultyClassListService;
import com.group5.paul_esys.modules.users.models.user.UserInformation;
import com.group5.paul_esys.modules.users.services.UserSession;
import com.group5.paul_esys.screens.faculty.forms.FacultyViewClassForm;
import com.group5.paul_esys.screens.sign_in.SignIn;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author nytri
 */
public class FacultyDashboardPanel extends javax.swing.JPanel {

        private static final String[] CLASS_LIST_TABLE_COLUMNS = {
                "Code", "Subject", "Section", "Schedule", "Room", "Students"
        };

        private final FacultyClassListService facultyClassListService = FacultyClassListService.getInstance();
        private List<FacultyClassListRow> classListRows = new ArrayList<>();

	/**
	 * Creates new form FacultyDashboardPanel
	 */
	public FacultyDashboardPanel() {
		initComponents();
		initializeDashboardPanel();
	}

        private void initializeDashboardPanel() {
                Faculty currentFaculty = getCurrentFacultyFromSession();
                if (currentFaculty != null) {
                        String firstName = currentFaculty.getFirstName() == null ? "" : currentFaculty.getFirstName().trim();
                        String lastName = currentFaculty.getLastName() == null ? "" : currentFaculty.getLastName().trim();
                        String fullName = (firstName + " " + lastName).trim();
                        if (!fullName.isBlank()) {
                                lblProfessorGreeting.setText("Welcome back! Professor " + fullName);
                        }
                }

                configureClassListTable();
                bindUiActions();
                loadClassList();
        }

        private void configureClassListTable() {
                tableClassList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
                tableClassList.setRowHeight(28);
                tableClassList.setModel(new DefaultTableModel(new Object[][]{}, CLASS_LIST_TABLE_COLUMNS) {
                        private final Class<?>[] columnTypes = new Class[]{
                                String.class,
                                String.class,
                                String.class,
                                String.class,
                                String.class,
                                Integer.class
                        };

                        @Override
                        public Class<?> getColumnClass(int columnIndex) {
                                return columnTypes[columnIndex];
                        }

                        @Override
                        public boolean isCellEditable(int rowIndex, int columnIndex) {
                                return false;
                        }
                });

                tableClassList.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent evt) {
                                if (SwingUtilities.isLeftMouseButton(evt) && evt.getClickCount() == 2) {
                                        openSelectedClassList();
                                }
                        }
                });
        }

        private void bindUiActions() {
                jButton1.addActionListener(evt -> logoutCurrentUser());
        }

        private void loadClassList() {
                Faculty currentFaculty = getCurrentFacultyFromSession();
                if (currentFaculty == null || currentFaculty.getId() == null) {
                        classListRows = new ArrayList<>();
                        populateClassListTable(classListRows);
                        return;
                }

                classListRows = new ArrayList<>(
                        facultyClassListService.getClassListRowsByFaculty(currentFaculty.getId())
                );
                populateClassListTable(classListRows);
                populateUpcomingClassesTable(classListRows);
        }

        private Faculty getCurrentFacultyFromSession() {
                UserInformation<?> userInformation = UserSession.getInstance().getUserInformation();
                if (userInformation == null) {
                        return null;
                }

                Object user = userInformation.getUser();
                if (user instanceof Faculty faculty) {
                        return faculty;
                }

                return null;
        }

        private void populateClassListTable(List<FacultyClassListRow> rows) {
                DefaultTableModel model = (DefaultTableModel) tableClassList.getModel();
                model.setRowCount(0);

                for (FacultyClassListRow row : rows) {
                        model.addRow(new Object[]{
                                row.code(),
                                row.subject(),
                                row.section(),
                                row.schedule(),
                                row.room(),
                                row.students()
                        });
                }
        }

        private void populateUpcomingClassesTable(List<FacultyClassListRow> rows) {
                DefaultTableModel model = (DefaultTableModel) tableUpcomingClasses.getModel();
                model.setRowCount(0);

                for (int i = 0; i < rows.size() && i < 10; i++) {
                        FacultyClassListRow row = rows.get(i);
                        model.addRow(new Object[]{
                                row.subject(),
                                row.section(),
                                row.room(),
                                "-",
                                row.schedule()
                        });
                }
        }

        private FacultyClassListRow getSelectedClassListRow() {
                int selectedRow = tableClassList.getSelectedRow();
                if (selectedRow < 0) {
                        return null;
                }

                int modelRow = tableClassList.convertRowIndexToModel(selectedRow);
                if (modelRow < 0 || modelRow >= classListRows.size()) {
                        return null;
                }

                return classListRows.get(modelRow);
        }

        private void openSelectedClassList() {
                FacultyClassListRow selectedClassListRow = getSelectedClassListRow();
                if (selectedClassListRow == null) {
                        return;
                }

                FacultyViewClassForm form = new FacultyViewClassForm(selectedClassListRow);
                form.setLocationRelativeTo(SwingUtilities.getWindowAncestor(this));
                form.setVisible(true);
        }

        private void logoutCurrentUser() {
                int confirm = JOptionPane.showConfirmDialog(
                        this,
                        "Are you sure you want to logout?",
                        "Confirm Logout",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                );

                if (confirm != JOptionPane.YES_OPTION) {
                        return;
                }

                UserSession.getInstance().logout();
                java.awt.Window window = SwingUtilities.getWindowAncestor(this);
                if (window != null) {
                        window.dispose();
                }

                SwingUtilities.invokeLater(() -> new SignIn().setVisible(true));
        }

	/**
	 * This method is called from within the constructor to initialize the
	 * form. WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
        // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
        private void initComponents() {

                jLabel1 = new javax.swing.JLabel();
                lblProfessorGreeting = new javax.swing.JLabel();
                jPanel1 = new javax.swing.JPanel();
                jLabel3 = new javax.swing.JLabel();
                jScrollPane2 = new javax.swing.JScrollPane();
                tableUpcomingClasses = new javax.swing.JTable();
                jPanel2 = new javax.swing.JPanel();
                jLabel5 = new javax.swing.JLabel();
                jLabel6 = new javax.swing.JLabel();
                jScrollPane1 = new javax.swing.JScrollPane();
                tableClassList = new javax.swing.JTable();
                jButton1 = new javax.swing.JButton();

                setBackground(new java.awt.Color(255, 255, 255));

                jLabel1.setFont(new java.awt.Font("Poppins", 0, 24)); // NOI18N
                jLabel1.setText("Faculty Dashboard");

                lblProfessorGreeting.setFont(new java.awt.Font("Poppins", 1, 14)); // NOI18N
                lblProfessorGreeting.setForeground(new java.awt.Color(102, 51, 255));
                lblProfessorGreeting.setText("Welcome back! Professor XYZ");

                jPanel1.setBackground(new java.awt.Color(255, 255, 255));
                jPanel1.setBorder(new com.group5.paul_esys.ui.PanelRoundBorder());

                jLabel3.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                jLabel3.setText("Upcoming Classes");

                tableUpcomingClasses.setModel(new javax.swing.table.DefaultTableModel(
                        new Object [][] {
                                {null, null, null, null, null},
                                {null, null, null, null, null},
                                {null, null, null, null, null},
                                {null, null, null, null, null}
                        },
                        new String [] {
                                "Subject", "Section", "Room", "Building", "Schedule"
                        }
                ) {
                        Class[] types = new Class [] {
                                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
                        };
                        boolean[] canEdit = new boolean [] {
                                false, false, false, false, false
                        };

                        public Class getColumnClass(int columnIndex) {
                                return types [columnIndex];
                        }

                        public boolean isCellEditable(int rowIndex, int columnIndex) {
                                return canEdit [columnIndex];
                        }
                });
                jScrollPane2.setViewportView(tableUpcomingClasses);

                javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
                jPanel1.setLayout(jPanel1Layout);
                jPanel1Layout.setHorizontalGroup(
                        jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 1143, Short.MAX_VALUE)
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
                                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 190, Short.MAX_VALUE)
                                .addContainerGap())
                );

                jPanel2.setBackground(new java.awt.Color(255, 255, 255));
                jPanel2.setBorder(new com.group5.paul_esys.ui.PanelRoundBorder());

                jLabel5.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                jLabel5.setText("Class List");

                jLabel6.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
                jLabel6.setForeground(new java.awt.Color(102, 102, 102));
                jLabel6.setText("View enrolled students per section");

                tableClassList.setModel(new javax.swing.table.DefaultTableModel(
                        new Object [][] {
                                {null, null, null, null, null, null},
                                {null, null, null, null, null, null},
                                {null, null, null, null, null, null},
                                {null, null, null, null, null, null}
                        },
                        new String [] {
                                "Code", "Subject", "Section", "Schedule", "Room", "Students"
                        }
                ) {
                        Class[] types = new Class [] {
                                java.lang.String.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class
                        };

                        public Class getColumnClass(int columnIndex) {
                                return types [columnIndex];
                        }
                });
                jScrollPane1.setViewportView(tableClassList);

                javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
                jPanel2.setLayout(jPanel2Layout);
                jPanel2Layout.setHorizontalGroup(
                        jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jLabel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap())
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addContainerGap()
                                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 1143, Short.MAX_VALUE)
                                        .addContainerGap()))
                );
                jPanel2Layout.setVerticalGroup(
                        jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jLabel5)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel6)
                                .addContainerGap(293, Short.MAX_VALUE))
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addGap(61, 61, 61)
                                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 278, Short.MAX_VALUE)
                                        .addContainerGap()))
                );

                jButton1.setBackground(new java.awt.Color(119, 0, 0));
                jButton1.setForeground(new java.awt.Color(255, 255, 255));
                jButton1.setText("Logout");

                javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
                this.setLayout(layout);
                layout.setHorizontalGroup(
                        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(layout.createSequentialGroup()
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                        .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, 343, Short.MAX_VALUE)
                                                        .addComponent(lblProfessorGreeting, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap())
                );
                layout.setVerticalGroup(
                        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGap(14, 14, 14)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jLabel1)
                                        .addComponent(jButton1))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(lblProfessorGreeting)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addContainerGap())
                );
        }// </editor-fold>//GEN-END:initComponents


        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JButton jButton1;
        private javax.swing.JLabel jLabel1;
        private javax.swing.JLabel jLabel3;
        private javax.swing.JLabel jLabel5;
        private javax.swing.JLabel jLabel6;
        private javax.swing.JPanel jPanel1;
        private javax.swing.JPanel jPanel2;
        private javax.swing.JScrollPane jScrollPane1;
        private javax.swing.JScrollPane jScrollPane2;
        private javax.swing.JLabel lblProfessorGreeting;
        private javax.swing.JTable tableClassList;
        private javax.swing.JTable tableUpcomingClasses;
        // End of variables declaration//GEN-END:variables
}
