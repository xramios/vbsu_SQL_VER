/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package com.group5.paul_esys.screens.registrar.cards;

import com.group5.paul_esys.modules.curriculum.model.Curriculum;
import com.group5.paul_esys.modules.semester.model.Semester;
import com.group5.paul_esys.modules.semester.services.SemesterService;
import com.group5.paul_esys.modules.subjects.model.Subject;
import com.group5.paul_esys.modules.subjects.services.SubjectService;
import java.time.ZoneId;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author nytri
 */
public class CurriculumCard extends javax.swing.JPanel {

        private final SemesterService semesterService = SemesterService.getInstance();
        private final SubjectService subjectService = SubjectService.getInstance();

        private final Curriculum curriculum;
        private final String courseName;
        private boolean semestersLoaded;

	/**
	 * Creates new form CurriculumCard
	 */
	public CurriculumCard() {
                this(null, null);
        }

        public CurriculumCard(Curriculum curriculum, String courseName) {
                this.curriculum = curriculum;
                this.courseName = courseName;
		initComponents();
                initializeCard();
	}

        private void initializeCard() {
                if (curriculum == null) {
                        txtCurriculumName.setText("");
                        txtCurYear.setText("");
                        cbxCourse.removeAllItems();
                        cbxCourse.addItem("");
                        showSemestersPlaceholder("No curriculum selected.");
                        return;
                }

                txtCurriculumName.setText(safeText(curriculum.getName(), "N/A"));
                txtCurYear.setText(formatYear(curriculum.getCurYear()));

                cbxCourse.removeAllItems();
                cbxCourse.addItem(safeText(courseName, "N/A"));

                showSemestersPlaceholder("Semesters load when this curriculum tab is selected.");
        }

        public boolean isSemestersLoaded() {
                return semestersLoaded;
        }

        public void ensureSemestersLoaded() {
                if (semestersLoaded) {
                        return;
                }

                loadSemesters();
        }

        public void reloadSemesters() {
                semestersLoaded = false;
                ensureSemestersLoaded();
        }

        private void loadSemesters() {
                tabbedPaneSemesters.removeAll();

                if (curriculum == null || curriculum.getId() == null) {
                        showSemestersPlaceholder("No curriculum selected.");
                        semestersLoaded = true;
                        return;
                }

                List<Semester> semesters = semesterService.getSemestersByCurriculum(curriculum.getId());
                if (semesters.isEmpty()) {
                        showSemestersPlaceholder("No semesters found for this curriculum.");
                        semestersLoaded = true;
                        return;
                }

                Map<Long, Subject> subjectMap = loadSubjectMapForCurriculum(curriculum.getId());
                for (Semester semester : semesters) {
                        SemesterCard semesterCard = new SemesterCard(semester, subjectMap, null);
                        tabbedPaneSemesters.addTab(buildSemesterTabTitle(semester), semesterCard);
                }

                semestersLoaded = true;
        }

        private Map<Long, Subject> loadSubjectMapForCurriculum(Long curriculumId) {
                List<Subject> subjects = subjectService.getAllSubjects();

                return subjects
                        .stream()
                        .collect(Collectors.toMap(Subject::getId, subject -> subject, (left, right) -> left, LinkedHashMap::new));
        }

        private String buildSemesterTabTitle(Semester semester) {
                return safeText(String.format("Year %s - %s", semester.getYearLevel(), semester.getSemester()), "Semester " + semester.getId());
        }

        private void showSemestersPlaceholder(String message) {
                tabbedPaneSemesters.removeAll();
                JPanel panel = new JPanel(new java.awt.GridBagLayout());
                panel.setBackground(java.awt.Color.WHITE);
                JLabel label = new JLabel(message);
                label.setForeground(new java.awt.Color(120, 120, 120));
                panel.add(label);
                tabbedPaneSemesters.addTab("Semesters", panel);
        }

        private String safeText(String value, String fallback) {
                if (value == null || value.trim().isEmpty()) {
                        return fallback;
                }
                return value.trim();
        }

        private String formatYear(Date yearDate) {
                if (yearDate == null) {
                        return "N/A";
                }

                if (yearDate instanceof java.sql.Date sqlDate) {
                        return String.valueOf(sqlDate.toLocalDate().getYear());
                }

                return String.valueOf(yearDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().getYear());
        }

	/**
	 * This method is called from within the constructor to initialize the
	 * form. WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
        // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
        private void initComponents() {

                tabbedPaneSemesters = new javax.swing.JTabbedPane();
                jLabel1 = new javax.swing.JLabel();
                txtCurriculumName = new javax.swing.JTextField();
                txtCurYear = new javax.swing.JTextField();
                jLabel2 = new javax.swing.JLabel();
                jLabel3 = new javax.swing.JLabel();
                cbxCourse = new javax.swing.JComboBox<>();

                setBackground(new java.awt.Color(255, 255, 255));
                setAutoscrolls(true);
                setPreferredSize(new java.awt.Dimension(696, 504));

                tabbedPaneSemesters.setBackground(new java.awt.Color(255, 255, 255));
                tabbedPaneSemesters.setTabLayoutPolicy(javax.swing.JTabbedPane.SCROLL_TAB_LAYOUT);
                tabbedPaneSemesters.setTabPlacement(javax.swing.JTabbedPane.LEFT);

                jLabel1.setText("Curriculum Name:");

                txtCurriculumName.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());
                txtCurriculumName.setEnabled(false);

                txtCurYear.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());
                txtCurYear.setEnabled(false);

                jLabel2.setText("Curriculum Year:");

                jLabel3.setText("Course / Program");

                cbxCourse.setEnabled(false);
                cbxCourse.addActionListener(this::cbxCourseActionPerformed);

                javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
                this.setLayout(layout);
                layout.setHorizontalGroup(
                        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(layout.createSequentialGroup()
                                                .addGap(10, 10, 10)
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addGroup(layout.createSequentialGroup()
                                                                .addComponent(jLabel1)
                                                                .addGap(11, 11, 11)
                                                                .addComponent(txtCurriculumName, javax.swing.GroupLayout.PREFERRED_SIZE, 240, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addGap(10, 10, 10)
                                                                .addComponent(jLabel2)
                                                                .addGap(10, 10, 10)
                                                                .addComponent(txtCurYear, javax.swing.GroupLayout.DEFAULT_SIZE, 220, Short.MAX_VALUE))
                                                        .addGroup(layout.createSequentialGroup()
                                                                .addComponent(jLabel3)
                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                                .addComponent(cbxCourse, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                                        .addGroup(layout.createSequentialGroup()
                                                .addContainerGap()
                                                .addComponent(tabbedPaneSemesters)))
                                .addContainerGap())
                );
                layout.setVerticalGroup(
                        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addGap(10, 10, 10)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(txtCurriculumName, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(txtCurYear, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGroup(layout.createSequentialGroup()
                                                .addGap(10, 10, 10)
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(jLabel1)
                                                        .addComponent(jLabel2))))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel3)
                                        .addComponent(cbxCourse, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(tabbedPaneSemesters, javax.swing.GroupLayout.PREFERRED_SIZE, 450, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                );
        }// </editor-fold>//GEN-END:initComponents

        private void cbxCourseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbxCourseActionPerformed
                // TODO add your handling code here:
        }//GEN-LAST:event_cbxCourseActionPerformed


        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JComboBox<String> cbxCourse;
        private javax.swing.JLabel jLabel1;
        private javax.swing.JLabel jLabel2;
        private javax.swing.JLabel jLabel3;
        private javax.swing.JTabbedPane tabbedPaneSemesters;
        private javax.swing.JTextField txtCurYear;
        private javax.swing.JTextField txtCurriculumName;
        // End of variables declaration//GEN-END:variables
}
