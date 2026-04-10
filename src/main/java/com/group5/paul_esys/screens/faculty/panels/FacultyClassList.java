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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author nytri
 */
public class FacultyClassList extends javax.swing.JPanel {

        private static final String[] TABLE_COLUMNS = {
                "Code", "Subject", "Section", "Schedule", "Room", "Students"
        };

        private final FacultyClassListService facultyClassListService = FacultyClassListService.getInstance();
        private List<FacultyClassListRow> classListRows = new ArrayList<>();

	/**
	 * Creates new form FacultyClassList
	 */
	public FacultyClassList() {
		initComponents();
                initializeClassListPanel();
	}

        private void initializeClassListPanel() {
                tableClassList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
                tableClassList.setRowHeight(28);
                tableClassList.setModel(new DefaultTableModel(new Object[][]{}, TABLE_COLUMNS) {
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
                                if (javax.swing.SwingUtilities.isLeftMouseButton(evt) && evt.getClickCount() == 2) {
                                        openSelectedClassList();
                                }
                        }
                });

                loadClassList();
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
                form.setLocationRelativeTo(javax.swing.SwingUtilities.getWindowAncestor(this));
                form.setVisible(true);
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
                jScrollPane1 = new javax.swing.JScrollPane();
                tableClassList = new javax.swing.JTable();

                setBackground(new java.awt.Color(255, 255, 255));

                jLabel1.setFont(new java.awt.Font("Poppins", 0, 24)); // NOI18N
                jLabel1.setText("Class List");

                jLabel2.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                jLabel2.setForeground(new java.awt.Color(102, 102, 102));
                jLabel2.setText("View enrolled students per section");

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
                        Class<?>[] types = new Class<?>[] {
                                java.lang.String.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class
                        };

                        public Class<?> getColumnClass(int columnIndex) {
                                return types [columnIndex];
                        }
                });
                jScrollPane1.setViewportView(tableClassList);

                javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
                this.setLayout(layout);
                layout.setHorizontalGroup(
                        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 1161, Short.MAX_VALUE)
                                        .addGroup(layout.createSequentialGroup()
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                        .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 343, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                .addGap(0, 0, Short.MAX_VALUE)))
                                .addContainerGap())
                );
                layout.setVerticalGroup(
                        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 601, Short.MAX_VALUE)
                                .addContainerGap())
                );
        }// </editor-fold>//GEN-END:initComponents


        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JLabel jLabel1;
        private javax.swing.JLabel jLabel2;
        private javax.swing.JScrollPane jScrollPane1;
        private javax.swing.JTable tableClassList;
        // End of variables declaration//GEN-END:variables
}
