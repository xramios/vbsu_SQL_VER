/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JDialog.java to edit this template
 */
package com.group5.paul_esys.screens.registrar.forms;

import com.group5.paul_esys.modules.semester.model.Semester;
import com.group5.paul_esys.modules.semester.services.SemesterService;
import com.group5.paul_esys.modules.semester_subjects.model.SemesterSubject;
import com.group5.paul_esys.modules.semester_subjects.services.SemesterSubjectService;
import com.group5.paul_esys.modules.subjects.model.Subject;
import com.group5.paul_esys.modules.subjects.services.SubjectService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JOptionPane;

/**
 *
 * @author nytri
 */
public class SemesterSubjectForm extends javax.swing.JDialog {
	
	private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(SemesterSubjectForm.class.getName());
        private final SemesterService semesterService = SemesterService.getInstance();
        private final SubjectService subjectService = SubjectService.getInstance();
        private final SemesterSubjectService semesterSubjectService = SemesterSubjectService.getInstance();

        private final Long fixedSemesterId;
        private final Runnable onSavedCallback;

        private final Map<String, Long> semesterIdByLabel = new LinkedHashMap<>();
        private final Map<String, Long> subjectIdByLabel = new LinkedHashMap<>();

	/**
	 * Creates new form SemesterSubjectForm
	 */
	public SemesterSubjectForm(java.awt.Frame parent, boolean modal) {
                this(parent, modal, null, null);
        }

        public SemesterSubjectForm(
                java.awt.Frame parent,
                boolean modal,
                Long fixedSemesterId,
                Runnable onSavedCallback
        ) {
		super(parent, modal);
                this.fixedSemesterId = fixedSemesterId;
                this.onSavedCallback = onSavedCallback;
                this.setUndecorated(true);
		initComponents();
                this.setLocationRelativeTo(parent);
                initializeForm();
	}

        private void initializeForm() {
                jButton1.addActionListener(this::jButton1ActionPerformed);
                jButton2.addActionListener(this::jButton2ActionPerformed);

                jComboBox1.addItemListener(evt -> {
                        if (evt.getStateChange() == java.awt.event.ItemEvent.SELECTED) {
                                onSemesterSelectionChanged();
                        }
                });

                loadSemesters();

                if (fixedSemesterId != null) {
                        jComboBox1.setEnabled(false);
                        selectSemesterById(fixedSemesterId);
                }

                onSemesterSelectionChanged();
        }

        private void loadSemesters() {
                jComboBox1.removeAllItems();
                semesterIdByLabel.clear();

                List<Semester> semesters = new ArrayList<>();
                if (fixedSemesterId != null) {
                        semesterService.getSemesterById(fixedSemesterId).ifPresent(semesters::add);
                } else {
                        semesters.addAll(semesterService.getAllSemesters());
                }

                for (Semester semester : semesters) {
                        String label = buildSemesterLabel(semester);
                        semesterIdByLabel.put(label, semester.getId());
                        jComboBox1.addItem(label);
                }
        }

        private void selectSemesterById(Long semesterId) {
                if (semesterId == null) {
                        return;
                }

                for (Map.Entry<String, Long> entry : semesterIdByLabel.entrySet()) {
                        if (semesterId.equals(entry.getValue())) {
                                jComboBox1.setSelectedItem(entry.getKey());
                                return;
                        }
                }
        }

        private String buildSemesterLabel(Semester semester) {
                String semesterName = semester.getSemester() == null || semester.getSemester().trim().isEmpty()
                        ? "Semester"
                        : semester.getSemester().trim();
                return semesterName + " - ID " + semester.getId();
        }

        private Long getSelectedSemesterId() {
                Object selectedItem = jComboBox1.getSelectedItem();
                if (selectedItem == null) {
                        return null;
                }

                return semesterIdByLabel.get(selectedItem.toString());
        }

        private Long getSelectedSubjectId() {
                Object selectedItem = jComboBox2.getSelectedItem();
                if (selectedItem == null) {
                        return null;
                }

                return subjectIdByLabel.get(selectedItem.toString());
        }

        private void onSemesterSelectionChanged() {
                Long semesterId = getSelectedSemesterId();
                updateTitleForSelection();
                loadAvailableSubjects(semesterId);
        }

        private void updateTitleForSelection() {
                Object selectedItem = jComboBox1.getSelectedItem();
                if (selectedItem == null) {
                        windowBar1.setTitle("Add Subject to Semester");
                        return;
                }

                String label = selectedItem.toString();
                int suffixIndex = label.indexOf(" - ID ");
                String semesterName = suffixIndex > 0 ? label.substring(0, suffixIndex) : label;
                windowBar1.setTitle("Add Subject to Semester " + semesterName);
        }

        private List<Subject> resolveSubjectsSource() {
                return subjectService.getAllSubjects();
        }

        private void loadAvailableSubjects(Long semesterId) {
                jComboBox2.removeAllItems();
                subjectIdByLabel.clear();

                if (semesterId == null) {
                        return;
                }

                Set<Long> existingSubjectIds = new LinkedHashSet<>();
                for (SemesterSubject semesterSubject : semesterSubjectService.getSemesterSubjectsBySemester(semesterId)) {
                        existingSubjectIds.add(semesterSubject.getSubjectId());
                }

                for (Subject subject : resolveSubjectsSource()) {
                        if (existingSubjectIds.contains(subject.getId())) {
                                continue;
                        }

                        String label = buildSubjectLabel(subject);
                        subjectIdByLabel.put(label, subject.getId());
                        jComboBox2.addItem(label);
                }
        }

        private String buildSubjectLabel(Subject subject) {
                String code = subject.getSubjectCode() == null || subject.getSubjectCode().trim().isEmpty()
                        ? "NO-CODE"
                        : subject.getSubjectCode().trim();
                String name = subject.getSubjectName() == null || subject.getSubjectName().trim().isEmpty()
                        ? "Unnamed Subject"
                        : subject.getSubjectName().trim();
                return code + " - " + name;
        }

        private boolean isValidForm(Long semesterId, Long subjectId) {
                if (semesterId == null) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Please select a semester.",
                                "Validation Error",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return false;
                }

                if (subjectId == null) {
                        JOptionPane.showMessageDialog(
                                this,
                                "No available subject to add for this semester.",
                                "Validation Error",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return false;
                }

                return true;
        }

        private void saveSemesterSubject() {
                Long semesterId = getSelectedSemesterId();
                Long subjectId = getSelectedSubjectId();

                if (!isValidForm(semesterId, subjectId)) {
                        return;
                }

                if (semesterSubjectService.getBySemesterAndSubject(semesterId, subjectId).isPresent()) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Selected subject is already linked to this semester.",
                                "Duplicate Entry",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return;
                }

                SemesterSubject semesterSubject = new SemesterSubject()
                        .setSemesterId(semesterId)
                        .setSubjectId(subjectId);

                if (!semesterSubjectService.createSemesterSubject(semesterSubject)) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Failed to add subject to semester. Please try again.",
                                "Save Failed",
                                JOptionPane.ERROR_MESSAGE
                        );
                        return;
                }

                JOptionPane.showMessageDialog(
                        this,
                        "Subject added to semester successfully.",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE
                );

                if (onSavedCallback != null) {
                        onSavedCallback.run();
                }

                dispose();
        }

        private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {
                saveSemesterSubject();
        }

        private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {
                dispose();
        }

	/**
	 * This method is called from within the constructor to initialize the
	 * form. WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
        // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
        private void initComponents() {

                windowBar1 = new com.group5.paul_esys.components.WindowBar();
                jPanel1 = new javax.swing.JPanel();
                jLabel1 = new javax.swing.JLabel();
                jLabel2 = new javax.swing.JLabel();
                jLabel3 = new javax.swing.JLabel();
                jComboBox1 = new javax.swing.JComboBox<>();
                jComboBox2 = new javax.swing.JComboBox<>();
                jLabel4 = new javax.swing.JLabel();
                jButton1 = new javax.swing.JButton();
                jButton2 = new javax.swing.JButton();

                setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
                getContentPane().setLayout(new javax.swing.BoxLayout(getContentPane(), javax.swing.BoxLayout.Y_AXIS));

                windowBar1.setMaximumSize(new java.awt.Dimension(65534, 36));
                windowBar1.setTitle("Add Subject to Semester <SEMESTER NAME>");
                getContentPane().add(windowBar1);

                jPanel1.setBackground(new java.awt.Color(255, 255, 255));
                jPanel1.setPreferredSize(new java.awt.Dimension(300, 400));

                jLabel1.setFont(new java.awt.Font("Poppins", 0, 18)); // NOI18N
                jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                jLabel1.setText("Semester Subject Form");

                jLabel2.setForeground(new java.awt.Color(153, 153, 153));
                jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                jLabel2.setText("Add subject to semester seamlessly");

                jLabel3.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                jLabel3.setText("Semester");

                jComboBox1.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

                jComboBox2.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

                jLabel4.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                jLabel4.setText("Subject");

                jButton1.setBackground(new java.awt.Color(119, 0, 0));
                jButton1.setForeground(new java.awt.Color(255, 255, 255));
                jButton1.setText("Save");

                jButton2.setBackground(new java.awt.Color(119, 0, 0));
                jButton2.setForeground(new java.awt.Color(255, 255, 255));
                jButton2.setText("Cancel");

                javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
                jPanel1.setLayout(jPanel1Layout);
                jPanel1Layout.setHorizontalGroup(
                        jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(78, 78, 78)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                                .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 34, Short.MAX_VALUE)
                                                .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addComponent(jLabel4, javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jLabel3, javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jComboBox1, javax.swing.GroupLayout.Alignment.LEADING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jComboBox2, javax.swing.GroupLayout.Alignment.LEADING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap(78, Short.MAX_VALUE))
                );
                jPanel1Layout.setVerticalGroup(
                        jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(34, 34, 34)
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel2)
                                .addGap(26, 26, 26)
                                .addComponent(jLabel3)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(jLabel4)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(53, 53, 53)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addContainerGap(54, Short.MAX_VALUE))
                );

                getContentPane().add(jPanel1);

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

		/* Create and display the dialog */
		java.awt.EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				SemesterSubjectForm dialog = new SemesterSubjectForm(new javax.swing.JFrame(), true);
				dialog.addWindowListener(new java.awt.event.WindowAdapter() {
					@Override
					public void windowClosing(java.awt.event.WindowEvent e) {
						System.exit(0);
					}
				});
				dialog.setVisible(true);
			}
		});
	}

        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JButton jButton1;
        private javax.swing.JButton jButton2;
        private javax.swing.JComboBox<String> jComboBox1;
        private javax.swing.JComboBox<String> jComboBox2;
        private javax.swing.JLabel jLabel1;
        private javax.swing.JLabel jLabel2;
        private javax.swing.JLabel jLabel3;
        private javax.swing.JLabel jLabel4;
        private javax.swing.JPanel jPanel1;
        private com.group5.paul_esys.components.WindowBar windowBar1;
        // End of variables declaration//GEN-END:variables
}
