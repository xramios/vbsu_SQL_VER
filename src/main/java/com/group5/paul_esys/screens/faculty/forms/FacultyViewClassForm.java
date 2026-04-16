/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.group5.paul_esys.screens.faculty.forms;

import com.group5.paul_esys.modules.enums.StudentEnrolledSubjectStatus;
import com.group5.paul_esys.modules.enrollments.services.StudentEnrolledSubjectService;
import com.group5.paul_esys.modules.faculty.model.Faculty;
import com.group5.paul_esys.modules.faculty.model.FacultyClassListRow;
import com.group5.paul_esys.modules.faculty.model.FacultyClassStudentRow;
import com.group5.paul_esys.modules.faculty.services.FacultyClassListService;
import com.group5.paul_esys.modules.registrar.services.RegistrarDropRequestService;
import com.group5.paul_esys.modules.users.services.UserSession;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author nytri
 */
public class FacultyViewClassForm extends javax.swing.JFrame {
	
	private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(FacultyViewClassForm.class.getName());
        private static final String DEFAULT_WINDOW_TITLE = "View Class";

        private final FacultyClassListService facultyClassListService = FacultyClassListService.getInstance();
        private final StudentEnrolledSubjectService studentEnrolledSubjectService = StudentEnrolledSubjectService.getInstance();
        private FacultyClassListRow classListRow;
        private List<FacultyClassStudentRow> classStudents = new ArrayList<>();

	/**
	 * Creates new form FacultyViewClassForm
	 */
	public FacultyViewClassForm() {
		this.setUndecorated(true);
		initComponents();
                initializeFormWindow();
        }

        public FacultyViewClassForm(FacultyClassListRow classListRow) {
                this();
                this.classListRow = classListRow;
                loadClassStudents();
        }

        private void initializeFormWindow() {
		this.setLocationRelativeTo(null);
                setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
                configureClassStudentsTable();
                configureCompletionAction();
                windowBar1.setTitle(DEFAULT_WINDOW_TITLE);
        }

        private void configureClassStudentsTable() {
                tableClassStudents.setRowHeight(28);
                tableClassStudents.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                tableClassStudents.setModel(new DefaultTableModel(
                        new Object[][]{},
                        new String[]{"Full Name", "Student Status", "Course", "Curriculum", "Year Level", "Subject Status"}
                ) {
                        private final Class<?>[] columnTypes = new Class<?>[]{
                                String.class,
                                String.class,
                                String.class,
                                String.class,
                                String.class,
                                String.class
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
        }

        private void configureCompletionAction() {
                JPopupMenu contextMenu = new JPopupMenu();
                
                JMenuItem markCompletedItem = new JMenuItem("Mark as COMPLETED");
                markCompletedItem.addActionListener(evt -> markSelectedStudentCompleted());
                contextMenu.add(markCompletedItem);
                
                contextMenu.addSeparator();
                
                JMenuItem requestDropItem = new JMenuItem("Request Student DROP");
                requestDropItem.addActionListener(evt -> requestSelectedStudentDrop());
                contextMenu.add(requestDropItem);

                tableClassStudents.setComponentPopupMenu(contextMenu);
                tableClassStudents.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent evt) {
                                if (SwingUtilities.isLeftMouseButton(evt) && evt.getClickCount() == 2) {
                                        markSelectedStudentCompleted();
                                }
                        }
                });
        }

        private void loadClassStudents() {
                if (classListRow == null || classListRow.offeringId() == null) {
                        populateClassStudentsTable(List.of());
                        windowBar1.setTitle(DEFAULT_WINDOW_TITLE);
                        return;
                }

                windowBar1.setTitle(buildWindowTitle(classListRow));
                populateClassStudentsTable(
                        facultyClassListService.getClassStudentsByOffering(classListRow.offeringId())
                );
        }

        private void populateClassStudentsTable(List<FacultyClassStudentRow> classStudents) {
                this.classStudents = new ArrayList<>(classStudents);
                DefaultTableModel model = (DefaultTableModel) tableClassStudents.getModel();
                model.setRowCount(0);

                for (FacultyClassStudentRow classStudent : classStudents) {
                        model.addRow(new Object[]{
                                classStudent.fullName(),
                                classStudent.studentStatus(),
                                classStudent.course(),
                                classStudent.curriculum(),
                                classStudent.yearLevel(),
                                formatEnrolledSubjectStatus(classStudent)
                        });
                }
        }

        private String formatEnrolledSubjectStatus(FacultyClassStudentRow classStudent) {
                if (classStudent.enrolledSubjectStatus() == null) {
                        return StudentEnrolledSubjectStatus.ENROLLED.name();
                }

                return classStudent.enrolledSubjectStatus().name();
        }

        private FacultyClassStudentRow getSelectedClassStudent() {
                int selectedRow = tableClassStudents.getSelectedRow();
                if (selectedRow < 0) {
                        return null;
                }

                int modelRow = tableClassStudents.convertRowIndexToModel(selectedRow);
                if (modelRow < 0 || modelRow >= classStudents.size()) {
                        return null;
                }

                return classStudents.get(modelRow);
        }

        private void markSelectedStudentCompleted() {
                FacultyClassStudentRow selectedStudent = getSelectedClassStudent();
                if (selectedStudent == null) {
                        JOptionPane.showMessageDialog(this, "Select a student first.", "No Selection", JOptionPane.WARNING_MESSAGE);
                        return;
                }

                if (selectedStudent.enrolledSubjectStatus() == StudentEnrolledSubjectStatus.COMPLETED) {
                        JOptionPane.showMessageDialog(this, "This student is already marked as COMPLETED.", "No Changes", JOptionPane.INFORMATION_MESSAGE);
                        return;
                }

                if (selectedStudent.studentId() == null
                  || selectedStudent.studentId().isBlank()
                  || selectedStudent.enrollmentId() == null
                  || selectedStudent.offeringId() == null
                  || selectedStudent.semesterSubjectId() == null) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Unable to mark completion due to missing enrollment metadata.",
                                "Invalid Data",
                                JOptionPane.ERROR_MESSAGE
                        );
                        return;
                }

                int confirmation = JOptionPane.showConfirmDialog(
                        this,
                        "Mark " + selectedStudent.fullName() + " as COMPLETED for this subject?",
                        "Confirm Completion",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                );

                if (confirmation != JOptionPane.YES_OPTION) {
                        return;
                }

                boolean updated = studentEnrolledSubjectService.upsertStatus(
                        selectedStudent.studentId(),
                        selectedStudent.enrollmentId(),
                        selectedStudent.offeringId(),
                        selectedStudent.semesterSubjectId(),
                        StudentEnrolledSubjectStatus.COMPLETED,
                        true
                );

                if (updated) {
                        JOptionPane.showMessageDialog(this, "Student marked as COMPLETED.", "Success", JOptionPane.INFORMATION_MESSAGE);
                        loadClassStudents();
                        return;
                }

                JOptionPane.showMessageDialog(this, "Failed to mark student as COMPLETED.", "Error", JOptionPane.ERROR_MESSAGE);
        }

        private void requestSelectedStudentDrop() {
                FacultyClassStudentRow selectedStudent = getSelectedClassStudent();
                if (selectedStudent == null) {
                        JOptionPane.showMessageDialog(this, "Select a student first.", "No Selection", JOptionPane.WARNING_MESSAGE);
                        return;
                }

                if (selectedStudent.enrolledSubjectStatus() == StudentEnrolledSubjectStatus.DROPPED) {
                        JOptionPane.showMessageDialog(this, "This student is already marked as DROPPED.", "No Changes", JOptionPane.INFORMATION_MESSAGE);
                        return;
                }

                Faculty currentFaculty = (Faculty) UserSession.getInstance().getUserInformation().getUser();
                if (currentFaculty == null || currentFaculty.getId() == null) {
                        JOptionPane.showMessageDialog(this, "Faculty profile not found.", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                }

                if (!RegistrarDropRequestService.getInstance().isDropRequestAllowed(classListRow.offeringId())) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Drop requests are no longer allowed for this enrollment period.\n" +
                                "The enrollment period has ended.",
                                "Drop Request Not Allowed",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return;
                }

                String reason = JOptionPane.showInputDialog(
                        this,
                        "Enter reason for drop request (mandatory):",
                        "Request Student DROP",
                        JOptionPane.QUESTION_MESSAGE
                );
                if (reason == null || reason.trim().isEmpty()) {
                        return;
                }

                boolean success = RegistrarDropRequestService.getInstance().createDropRequest(
                        currentFaculty.getId(),
                        selectedStudent.studentId(),
                        selectedStudent.offeringId(),
                        reason.trim()
                );

                if (success) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Drop request for " + selectedStudent.fullName() + " has been submitted to the Registrar for approval.\n" +
                                "The student will be notified of the decision.",
                                "Request Submitted",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                        loadClassStudents();
                } else {
                        JOptionPane.showMessageDialog(
                                this,
                                "Failed to submit drop request. Please try again or contact the Registrar.",
                                "Submission Failed",
                                JOptionPane.ERROR_MESSAGE
                        );
                }
        }

        private String buildWindowTitle(FacultyClassListRow row) {
                String code = safeText(row.code(), "N/A");
                String section = safeText(row.section(), "N/A");
                return DEFAULT_WINDOW_TITLE + " - " + code + " / " + section;
        }

        private String safeText(String value, String fallback) {
                if (value == null || value.isBlank()) {
                        return fallback;
                }

                return value.trim();
	}

	/**
	 * This method is called from within the constructor to initialize the
	 * form. WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
        // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
        private void initComponents() {

                jScrollPane1 = new javax.swing.JScrollPane();
                tableClassStudents = new javax.swing.JTable();
                windowBar1 = new com.group5.paul_esys.components.WindowBar();

                setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

                tableClassStudents.setModel(new javax.swing.table.DefaultTableModel(
                        new Object [][] {

                        },
                        new String [] {
                                "Full Name", "Student Status", "Course", "Curriculum", "Year Level"
                        }
                ) {
                        Class<?>[] types = new Class<?>[] {
                                java.lang.String.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class
                        };
                        boolean[] canEdit = new boolean [] {
                                false, false, false, false, false
                        };

                        public Class<?> getColumnClass(int columnIndex) {
                                return types [columnIndex];
                        }

                        public boolean isCellEditable(int rowIndex, int columnIndex) {
                                return canEdit [columnIndex];
                        }
                });
                jScrollPane1.setViewportView(tableClassStudents);

                windowBar1.setTitle("View Class");

                javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
                getContentPane().setLayout(layout);
                layout.setHorizontalGroup(
                        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 864, Short.MAX_VALUE)
                                .addContainerGap())
                        .addComponent(windowBar1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                );
                layout.setVerticalGroup(
                        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(windowBar1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 455, Short.MAX_VALUE)
                                .addContainerGap())
                );

                pack();
        }// </editor-fold>//GEN-END:initComponents

	/**
	 * @param args the command line arguments
	 */
	public static void main(String args[]) {
		/* Set the Nimbus look and feel */
		//<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
		/* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
		 */
		try {
			for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
				if ("Nimbus".equals(info.getName())) {
					javax.swing.UIManager.setLookAndFeel(info.getClassName());
					break;
				}
			}
		} catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
			logger.log(java.util.logging.Level.SEVERE, null, ex);
		}
		//</editor-fold>

		/* Create and display the form */
		java.awt.EventQueue.invokeLater(() -> new FacultyViewClassForm().setVisible(true));
	}

        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JScrollPane jScrollPane1;
        private javax.swing.JTable tableClassStudents;
        private com.group5.paul_esys.components.WindowBar windowBar1;
        // End of variables declaration//GEN-END:variables
}
