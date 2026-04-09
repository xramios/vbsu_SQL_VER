/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package com.group5.paul_esys.screens.registrar.cards;

import com.group5.paul_esys.modules.semester.model.Semester;
import com.group5.paul_esys.modules.semester_subjects.model.SemesterSubject;
import com.group5.paul_esys.modules.semester_subjects.services.SemesterSubjectService;
import com.group5.paul_esys.modules.subjects.model.Subject;
import com.group5.paul_esys.modules.subjects.services.SubjectService;
import com.group5.paul_esys.screens.registrar.forms.SemesterSubjectForm;
import java.awt.Frame;
import java.awt.Window;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author nytri
 */
public class SemesterCard extends javax.swing.JPanel {

        private final SemesterSubjectService semesterSubjectService = SemesterSubjectService.getInstance();
        private final SubjectService subjectService = SubjectService.getInstance();
        private final Semester semester;
        private Map<Long, Subject> subjectsById;
        private final Runnable onDataChangedCallback;
        private List<SemesterSubject> semesterSubjects = new ArrayList<>();

	/**
	 * Creates new form SemesterCard
	 */
	public SemesterCard() {
                this(null, new LinkedHashMap<>(), null);
        }

        public SemesterCard(
                Semester semester,
                Map<Long, Subject> subjectsById,
                Runnable onDataChangedCallback
        ) {
                this.semester = semester;
                this.subjectsById = subjectsById == null ? new LinkedHashMap<>() : new LinkedHashMap<>(subjectsById);
                this.onDataChangedCallback = onDataChangedCallback;
		initComponents();
                initializeCard();
	}

        private void initializeCard() {
                configureTableModel();
                btnAddSubject.addActionListener(this::btnAddSubjectActionPerformed);
                btnRemoveSubject.addActionListener(this::btnRemoveSubjectActionPerformed);

                if (semester == null) {
                        lblSemesterName.setText("Semester");
                        setButtonsEnabled(false);
                        return;
                }

                lblSemesterName.setText(safeText(semester.getSemester(), "Semester"));
                reloadSemesterSubjects();
        }

        private void configureTableModel() {
                DefaultTableModel model = new DefaultTableModel(
                        new Object[][]{},
                        new String[]{"Name", "Code", "Units", "Description"}
                ) {
                        @Override
                        public boolean isCellEditable(int rowIndex, int columnIndex) {
                                return false;
                        }
                };

                tableSubjects.setModel(model);
                tableSubjects.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
                tableSubjects.setRowHeight(28);
        }

        private void setButtonsEnabled(boolean enabled) {
                btnAddSubject.setEnabled(enabled);
                btnRemoveSubject.setEnabled(enabled);
        }

        public void updateSubjectLookup(Map<Long, Subject> latestSubjectMap) {
                subjectsById = latestSubjectMap == null ? new LinkedHashMap<>() : new LinkedHashMap<>(latestSubjectMap);
                reloadSemesterSubjects();
        }

        private void reloadSemesterSubjects() {
                DefaultTableModel model = (DefaultTableModel) tableSubjects.getModel();
                model.setRowCount(0);

                if (semester == null || semester.getId() == null) {
                        semesterSubjects = new ArrayList<>();
                        setButtonsEnabled(false);
                        return;
                }

                setButtonsEnabled(true);
                semesterSubjects = semesterSubjectService.getSemesterSubjectsBySemester(semester.getId());

                for (SemesterSubject semesterSubject : semesterSubjects) {
                        Subject subject = resolveSubjectById(semesterSubject.getSubjectId());
                        model.addRow(new Object[]{
                                safeText(
                                        subject == null ? null : subject.getSubjectName(),
                                        subjectFallbackLabel(semesterSubject.getSubjectId())
                                ),
                                safeText(subject == null ? null : subject.getSubjectCode(), "N/A"),
                                subject == null || subject.getUnits() == null ? "N/A" : subject.getUnits(),
                                buildDescriptionPreview(subject == null ? null : subject.getDescription())
                        });
                }
        }

        private Subject resolveSubjectById(Long subjectId) {
                if (subjectId == null) {
                        return null;
                }

                Subject subject = subjectsById.get(subjectId);
                if (subject != null) {
                        return subject;
                }

                subject = subjectService.getSubjectById(subjectId).orElse(null);
                if (subject != null) {
                        subjectsById.put(subjectId, subject);
                }

                return subject;
        }

        private String subjectFallbackLabel(Long subjectId) {
                if (subjectId == null) {
                        return "N/A";
                }

                return "Unknown Subject (ID " + subjectId + ")";
        }

        private String safeText(String value, String fallback) {
                if (value == null || value.trim().isEmpty()) {
                        return fallback;
                }
                return value.trim();
        }

        private String buildDescriptionPreview(String description) {
                String safeDescription = safeText(description, "");
                if (safeDescription.length() <= 120) {
                        return safeDescription;
                }

                return safeDescription.substring(0, 117) + "...";
        }

        private SemesterSubject getSelectedSemesterSubject() {
                int selectedRow = tableSubjects.getSelectedRow();
                if (selectedRow < 0) {
                        return null;
                }

                int modelRow = tableSubjects.convertRowIndexToModel(selectedRow);
                if (modelRow < 0 || modelRow >= semesterSubjects.size()) {
                        return null;
                }

                return semesterSubjects.get(modelRow);
        }

        private void openAddSubjectForm() {
                if (semester == null || semester.getId() == null) {
                        return;
                }

                Window window = SwingUtilities.getWindowAncestor(this);
                Frame parentFrame = window instanceof Frame ? (Frame) window : null;

                SemesterSubjectForm form = new SemesterSubjectForm(
                        parentFrame,
                        true,
                        semester.getId(),
                        this::handleSemesterSubjectsChanged
                );
                form.setVisible(true);
        }

        private void removeSelectedSubject() {
                SemesterSubject selectedSemesterSubject = getSelectedSemesterSubject();
                if (selectedSemesterSubject == null) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Please select a subject to remove.",
                                "Remove Subject",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return;
                }

                Subject subject = resolveSubjectById(selectedSemesterSubject.getSubjectId());
                String subjectLabel = subject == null
                        ? subjectFallbackLabel(selectedSemesterSubject.getSubjectId())
                        : safeText(subject.getSubjectCode(), "SUBJECT") + " - " + safeText(subject.getSubjectName(), "Subject");

                int decision = JOptionPane.showConfirmDialog(
                        this,
                        "Remove " + subjectLabel + " from " + safeText(semester.getSemester(), "this semester") + "?",
                        "Confirm Remove",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );

                if (decision != JOptionPane.YES_OPTION) {
                        return;
                }

                if (!semesterSubjectService.deleteSemesterSubject(selectedSemesterSubject.getId())) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Failed to remove subject from semester.",
                                "Remove Subject",
                                JOptionPane.ERROR_MESSAGE
                        );
                        return;
                }

                JOptionPane.showMessageDialog(
                        this,
                        "Subject removed successfully.",
                        "Remove Subject",
                        JOptionPane.INFORMATION_MESSAGE
                );

                handleSemesterSubjectsChanged();
        }

        private void handleSemesterSubjectsChanged() {
                reloadSemesterSubjects();
                if (onDataChangedCallback != null) {
                        onDataChangedCallback.run();
                }
        }

        private void btnAddSubjectActionPerformed(java.awt.event.ActionEvent evt) {
                openAddSubjectForm();
        }

        private void btnRemoveSubjectActionPerformed(java.awt.event.ActionEvent evt) {
                removeSelectedSubject();
        }

	/**
	 * This method is called from within the constructor to initialize the
	 * form. WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
        // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
        private void initComponents() {

                jScrollPane1 = new javax.swing.JScrollPane();
                tableSubjects = new javax.swing.JTable();
                lblSemesterName = new javax.swing.JLabel();
                btnAddSubject = new javax.swing.JButton();
                btnRemoveSubject = new javax.swing.JButton();

                setBackground(new java.awt.Color(255, 255, 255));
                setBorder(new com.group5.paul_esys.ui.PanelRoundBorder());

                tableSubjects.setModel(new javax.swing.table.DefaultTableModel(
                        new Object [][] {

                        },
                        new String [] {
                                "Name", "Code", "Units", "Description"
                        }
                ));
                jScrollPane1.setViewportView(tableSubjects);

                lblSemesterName.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                lblSemesterName.setText("Semester Name");

                btnAddSubject.setText("Add Subject");

                btnRemoveSubject.setText("Remove Subject");

                javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
                this.setLayout(layout);
                layout.setHorizontalGroup(
                        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 728, Short.MAX_VALUE)
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(lblSemesterName)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(btnRemoveSubject)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(btnAddSubject, javax.swing.GroupLayout.PREFERRED_SIZE, 115, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addContainerGap())
                );
                layout.setVerticalGroup(
                        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(lblSemesterName)
                                        .addComponent(btnAddSubject)
                                        .addComponent(btnRemoveSubject))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 245, Short.MAX_VALUE)
                                .addContainerGap())
                );
        }// </editor-fold>//GEN-END:initComponents


        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JButton btnAddSubject;
        private javax.swing.JButton btnRemoveSubject;
        private javax.swing.JScrollPane jScrollPane1;
        private javax.swing.JLabel lblSemesterName;
        private javax.swing.JTable tableSubjects;
        // End of variables declaration//GEN-END:variables
}
