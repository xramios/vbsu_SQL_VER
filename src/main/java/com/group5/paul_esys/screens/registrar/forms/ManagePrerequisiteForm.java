/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.group5.paul_esys.screens.registrar.forms;

import com.group5.paul_esys.modules.prerequisites.model.Prerequisite;
import com.group5.paul_esys.modules.prerequisites.services.PrerequisiteService;
import com.group5.paul_esys.modules.subjects.model.Subject;
import com.group5.paul_esys.modules.subjects.services.SubjectService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JOptionPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author nytri
 */
public class ManagePrerequisiteForm extends javax.swing.JFrame {
	
	private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(ManagePrerequisiteForm.class.getName());

        private final SubjectService subjectService = SubjectService.getInstance();
        private final PrerequisiteService prerequisiteService = PrerequisiteService.getInstance();

        private final Subject targetSubject;
        private final List<Subject> allCandidateSubjects = new ArrayList<>();
        private final List<Subject> filteredCandidateSubjects = new ArrayList<>();
        private final Set<Long> prerequisiteSubjectIds = new HashSet<>();

	/**
	 * Creates new form ManagePrerequisite
	 */
	public ManagePrerequisiteForm() {
		this(null);
	}

        public ManagePrerequisiteForm(Subject targetSubject) {
                this.targetSubject = targetSubject;
		this.setUndecorated(true);
		initComponents();
		this.setLocationRelativeTo(null);
		initializeForm();
	}

        private void initializeForm() {
                configureTable();

                btnAddPrerequisite.addActionListener(evt -> addSelectedPrerequisite());
                btnDeletePrerequisite.addActionListener(evt -> deleteSelectedPrerequisite());

                btnSearch.getDocument().addDocumentListener(new DocumentListener() {
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

                if (targetSubject == null || targetSubject.getId() == null) {
                        windowBar1.setTitle("Manage Prerequisites");
                        btnAddPrerequisite.setEnabled(false);
                        btnDeletePrerequisite.setEnabled(false);
                        JOptionPane.showMessageDialog(
                                this,
                                "Please select a subject before managing prerequisites.",
                                "Manage Prerequisite",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return;
                }

                windowBar1.setTitle("Manage Prerequisite of " + buildSubjectLabel(targetSubject));
                loadPrerequisiteData();
        }

        private void configureTable() {
                DefaultTableModel model = new DefaultTableModel(
                        new Object[][]{},
                        new String[]{"Code", "Name", "Units", "Status"}
                ) {
                        @Override
                        public boolean isCellEditable(int row, int column) {
                                return false;
                        }
                };

                tableSubjects.setModel(model);
                tableSubjects.setRowHeight(28);
                tableSubjects.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        }

        private void loadPrerequisiteData() {
                allCandidateSubjects.clear();
                prerequisiteSubjectIds.clear();

                List<Subject> allSubjects = new ArrayList<>(subjectService.getAllSubjects());
                allSubjects.sort(Comparator.comparing(subject -> safeText(subject.getSubjectCode(), "").toLowerCase()));

                for (Subject subject : allSubjects) {
                        if (subject.getId() == null) {
                                continue;
                        }

                        if (subject.getId().equals(targetSubject.getId())) {
                                continue;
                        }

                        allCandidateSubjects.add(subject);
                }

                prerequisiteService
                        .getPrerequisitesBySubject(targetSubject.getId())
                        .forEach(prerequisite -> {
                                if (prerequisite.getPreSubjectId() != null) {
                                        prerequisiteSubjectIds.add(prerequisite.getPreSubjectId());
                                }
                        });

                applyFilters();
        }

        private void applyFilters() {
                String searchTerm = btnSearch.getText() == null
                        ? ""
                        : btnSearch.getText().trim().toLowerCase();

                filteredCandidateSubjects.clear();

                for (Subject subject : allCandidateSubjects) {
                        if (matchesSearch(subject, searchTerm)) {
                                filteredCandidateSubjects.add(subject);
                        }
                }

                populateTable();
        }

        private boolean matchesSearch(Subject subject, String searchTerm) {
                if (searchTerm.isEmpty()) {
                        return true;
                }

                String code = safeText(subject.getSubjectCode(), "").toLowerCase();
                String name = safeText(subject.getSubjectName(), "").toLowerCase();
                String description = safeText(subject.getDescription(), "").toLowerCase();

                return code.contains(searchTerm) || name.contains(searchTerm) || description.contains(searchTerm);
        }

        private void populateTable() {
                DefaultTableModel model = (DefaultTableModel) tableSubjects.getModel();
                model.setRowCount(0);

                for (Subject subject : filteredCandidateSubjects) {
                        String status = prerequisiteSubjectIds.contains(subject.getId())
                                ? "Added"
                                : "Not Added";

                        model.addRow(new Object[]{
                                safeText(subject.getSubjectCode(), "N/A"),
                                safeText(subject.getSubjectName(), "N/A"),
                                subject.getUnits() == null ? "N/A" : subject.getUnits(),
                                status
                        });
                }
        }

        private Subject getSelectedCandidateSubject() {
                int selectedRow = tableSubjects.getSelectedRow();
                if (selectedRow < 0) {
                        return null;
                }

                int modelRow = tableSubjects.convertRowIndexToModel(selectedRow);
                if (modelRow < 0 || modelRow >= filteredCandidateSubjects.size()) {
                        return null;
                }

                return filteredCandidateSubjects.get(modelRow);
        }

        private void addSelectedPrerequisite() {
                Subject selectedPrerequisite = getSelectedCandidateSubject();
                if (selectedPrerequisite == null) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Please select a subject to add as prerequisite.",
                                "Add Prerequisite",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return;
                }

                if (selectedPrerequisite.getId().equals(targetSubject.getId())) {
                        JOptionPane.showMessageDialog(
                                this,
                                "A subject cannot be its own prerequisite.",
                                "Add Prerequisite",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return;
                }

                if (prerequisiteSubjectIds.contains(selectedPrerequisite.getId())) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Selected subject is already a prerequisite.",
                                "Add Prerequisite",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                        return;
                }

                Prerequisite prerequisite = new Prerequisite()
                        .setSubjectId(targetSubject.getId())
                        .setPreSubjectId(selectedPrerequisite.getId());

                boolean created = prerequisiteService.createPrerequisite(prerequisite);
                if (!created) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Failed to add prerequisite. Please try again.",
                                "Add Prerequisite",
                                JOptionPane.ERROR_MESSAGE
                        );
                        return;
                }

                prerequisiteSubjectIds.add(selectedPrerequisite.getId());
                populateTable();

                JOptionPane.showMessageDialog(
                        this,
                        "Prerequisite added successfully.",
                        "Add Prerequisite",
                        JOptionPane.INFORMATION_MESSAGE
                );
        }

        private void deleteSelectedPrerequisite() {
                Subject selectedPrerequisite = getSelectedCandidateSubject();
                if (selectedPrerequisite == null) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Please select a prerequisite subject to delete.",
                                "Delete Prerequisite",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return;
                }

                if (!prerequisiteSubjectIds.contains(selectedPrerequisite.getId())) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Selected subject is not currently a prerequisite.",
                                "Delete Prerequisite",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                        return;
                }

                int option = JOptionPane.showConfirmDialog(
                        this,
                        "Remove " + buildSubjectLabel(selectedPrerequisite) + " as prerequisite?",
                        "Delete Prerequisite",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );

                if (option != JOptionPane.YES_OPTION) {
                        return;
                }

                boolean deleted = prerequisiteService.deletePrerequisiteBySubjects(
                        selectedPrerequisite.getId(),
                        targetSubject.getId()
                );

                if (!deleted) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Failed to delete prerequisite. Please try again.",
                                "Delete Prerequisite",
                                JOptionPane.ERROR_MESSAGE
                        );
                        return;
                }

                prerequisiteSubjectIds.remove(selectedPrerequisite.getId());
                populateTable();

                JOptionPane.showMessageDialog(
                        this,
                        "Prerequisite deleted successfully.",
                        "Delete Prerequisite",
                        JOptionPane.INFORMATION_MESSAGE
                );
        }

        private String buildSubjectLabel(Subject subject) {
                String code = safeText(subject.getSubjectCode(), "N/A");
                String name = safeText(subject.getSubjectName(), "N/A");
                return code + " - " + name;
        }

        private String safeText(String value, String fallback) {
                if (value == null || value.trim().isEmpty()) {
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

                windowBar1 = new com.group5.paul_esys.components.WindowBar();
                jPanel1 = new javax.swing.JPanel();
                jScrollPane1 = new javax.swing.JScrollPane();
                tableSubjects = new javax.swing.JTable();
                btnAddPrerequisite = new javax.swing.JButton();
                btnDeletePrerequisite = new javax.swing.JButton();
                jLabel1 = new javax.swing.JLabel();
                btnSearch = new javax.swing.JTextField();

                setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

                windowBar1.setTitle("Manage Prerequisite of <SUBJECT NAME>");

                tableSubjects.setModel(new javax.swing.table.DefaultTableModel(
                        new Object [][] {

                        },
                        new String [] {

                        }
                ));
                jScrollPane1.setViewportView(tableSubjects);

                btnAddPrerequisite.setText("Add Prerequisite");

                btnDeletePrerequisite.setText("Delete Prerequisite");

                jLabel1.setText("Search");

                btnSearch.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());

                javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
                jPanel1.setLayout(jPanel1Layout);
                jPanel1Layout.setHorizontalGroup(
                        jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 888, Short.MAX_VALUE)
                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                                .addComponent(jLabel1)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(btnSearch)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(btnDeletePrerequisite)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(btnAddPrerequisite)))
                                .addContainerGap())
                );
                jPanel1Layout.setVerticalGroup(
                        jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(btnAddPrerequisite)
                                        .addComponent(btnDeletePrerequisite)
                                        .addComponent(jLabel1)
                                        .addComponent(btnSearch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 346, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                );

                javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
                getContentPane().setLayout(layout);
                layout.setHorizontalGroup(
                        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(windowBar1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                );
                layout.setVerticalGroup(
                        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(windowBar1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, 0)
                                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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
		java.awt.EventQueue.invokeLater(() -> new ManagePrerequisiteForm().setVisible(true));
	}

        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JButton btnAddPrerequisite;
        private javax.swing.JButton btnDeletePrerequisite;
        private javax.swing.JTextField btnSearch;
        private javax.swing.JLabel jLabel1;
        private javax.swing.JPanel jPanel1;
        private javax.swing.JScrollPane jScrollPane1;
        private javax.swing.JTable tableSubjects;
        private com.group5.paul_esys.components.WindowBar windowBar1;
        // End of variables declaration//GEN-END:variables
}
