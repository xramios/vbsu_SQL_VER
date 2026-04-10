package com.group5.paul_esys.screens.registrar.panels;

import com.group5.paul_esys.modules.enrollment_period.model.EnrollmentPeriod;
import com.group5.paul_esys.modules.enrollment_period.services.EnrollmentPeriodService;
import com.group5.paul_esys.modules.enrollment_period.utils.EnrollmentPeriodUtils;
import com.group5.paul_esys.modules.offerings.model.OfferingGenerationPlanRow;
import com.group5.paul_esys.modules.offerings.model.OfferingGenerationResult;
import com.group5.paul_esys.modules.offerings.services.OfferingGenerationService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author nytri
 */
public class RegistrarOfferingsManagement extends javax.swing.JPanel {

  private final EnrollmentPeriodService enrollmentPeriodService = EnrollmentPeriodService.getInstance();
  private final OfferingGenerationService offeringGenerationService = OfferingGenerationService.getInstance();

  private final Map<String, Long> enrollmentPeriodIdByLabel = new LinkedHashMap<>();
  private final List<OfferingGenerationPlanRow> currentPlanRows = new ArrayList<>();

  /**
   * Creates new form RegistrarOfferingsManagement
   */
  public RegistrarOfferingsManagement() {
    initComponents();
    initializePanel();
  }

  private void initializePanel() {
    tablePreview.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
    tablePreview.setRowHeight(28);
    cbxEnrollmentPeriod.setPrototypeDisplayValue("2025-2026 | First Semester | OPEN | ID 999999");
    cbxSemester.setPrototypeDisplayValue("Semester 10");
    btnGenerate.setEnabled(false);
    chkIncludeWaitlist.setEnabled(chkOnlyActiveSections.isSelected());
    reloadFilterOptions();
  }

  private void onActiveSectionsFilterChanged() {
    chkIncludeWaitlist.setEnabled(chkOnlyActiveSections.isSelected());
    if (!chkOnlyActiveSections.isSelected()) {
      chkIncludeWaitlist.setSelected(false);
    }
  }

  private void reloadFilterOptions() {
    clearPreview();
    reloadFilterOptionsAsync();
  }

  private void reloadFilterOptionsAsync() {
    cbxEnrollmentPeriod.removeAllItems();
    enrollmentPeriodIdByLabel.clear();
    cbxEnrollmentPeriod.setEnabled(false);
    cbxSemester.removeAllItems();
    cbxSemester.setEnabled(false);

    new SwingWorker<FilterOptionsResult, Void>() {
      @Override
      protected FilterOptionsResult doInBackground() throws Exception {
        var periodsFuture = java.util.concurrent.Executors.newSingleThreadExecutor()
            .submit(() -> enrollmentPeriodService.getAllEnrollmentPeriods());
        var semestersFuture = java.util.concurrent.Executors.newSingleThreadExecutor()
            .submit(() -> offeringGenerationService.getDistinctSemesterNames());

        return new FilterOptionsResult(periodsFuture.get(), semestersFuture.get());
      }

      @Override
      protected void done() {
        try {
          FilterOptionsResult result = get();

          for (EnrollmentPeriod period : result.periods) {
            String label = buildEnrollmentPeriodLabel(period);
            cbxEnrollmentPeriod.addItem(label);
            enrollmentPeriodIdByLabel.put(label, period.getId());
          }

          for (String semesterName : result.semesterNames) {
            cbxSemester.addItem(semesterName);
          }
        } catch (InterruptedException | ExecutionException e) {
          JOptionPane.showMessageDialog(
              RegistrarOfferingsManagement.this,
              "Failed to load filter options: " + e.getMessage(),
              "Error",
              JOptionPane.ERROR_MESSAGE
          );
        } finally {
          cbxEnrollmentPeriod.setEnabled(true);
          cbxSemester.setEnabled(true);
        }
      }
    }.execute();
  }

  private record FilterOptionsResult(
      List<EnrollmentPeriod> periods,
      List<String> semesterNames
  ) {
  }

  private String buildEnrollmentPeriodLabel(EnrollmentPeriod period) {
    String schoolYear = EnrollmentPeriodUtils.safeText(period.getSchoolYear(), "N/A");
    String semester = EnrollmentPeriodUtils.safeText(period.getSemester(), "N/A");
    String status = EnrollmentPeriodUtils.resolveStatus(period);

    return schoolYear + " | " + semester + " | " + status + " | ID " + period.getId();
  }

  private void previewGenerationPlan() {
    Long enrollmentPeriodId = getSelectedEnrollmentPeriodId();
    String semesterName = getSelectedSemesterName();

    if (enrollmentPeriodId == null) {
      JOptionPane.showMessageDialog(
          this,
          "Please select an enrollment period.",
          "Preview Offerings",
          JOptionPane.WARNING_MESSAGE
      );
      return;
    }

    if (semesterName == null || semesterName.isBlank()) {
      JOptionPane.showMessageDialog(
          this,
          "Please select a semester.",
          "Preview Offerings",
          JOptionPane.WARNING_MESSAGE
      );
      return;
    }

    setControlsEnabled(false);

    new SwingWorker<List<OfferingGenerationPlanRow>, Void>() {
      @Override
      protected List<OfferingGenerationPlanRow> doInBackground() {
        return offeringGenerationService.previewGenerationPlan(
            enrollmentPeriodId,
            semesterName,
            chkOnlyActiveSections.isSelected(),
            chkIncludeWaitlist.isSelected()
        );
      }

      @Override
      protected void done() {
        try {
          currentPlanRows.clear();
          currentPlanRows.addAll(get());
          populatePreviewTable();
        } catch (InterruptedException | ExecutionException e) {
          JOptionPane.showMessageDialog(
              RegistrarOfferingsManagement.this,
              "Failed to preview generation plan: " + e.getMessage(),
              "Error",
              JOptionPane.ERROR_MESSAGE
          );
        } finally {
          setControlsEnabled(true);
        }
      }
    }.execute();
  }

  private void setControlsEnabled(boolean enabled) {
    btnPreview.setEnabled(enabled);
    btnGenerate.setEnabled(enabled && getPotentialNewCount() > 0);
    btnRefresh.setEnabled(enabled);
    cbxEnrollmentPeriod.setEnabled(enabled);
    cbxSemester.setEnabled(enabled);
    chkOnlyActiveSections.setEnabled(enabled);
    chkIncludeWaitlist.setEnabled(enabled && chkOnlyActiveSections.isSelected());
  }

  private void generateOfferings() {
    Long enrollmentPeriodId = getSelectedEnrollmentPeriodId();
    String semesterName = getSelectedSemesterName();

    if (enrollmentPeriodId == null || semesterName == null || semesterName.isBlank()) {
      previewGenerationPlan();
      return;
    }

    if (currentPlanRows.isEmpty()) {
      previewGenerationPlan();
    }

    int potentialCount = getPotentialNewCount();
    if (potentialCount <= 0) {
      JOptionPane.showMessageDialog(
          this,
          "No new offerings are available to generate for the selected filters.",
          "Generate Offerings",
          JOptionPane.INFORMATION_MESSAGE
      );
      return;
    }

    int confirm = JOptionPane.showConfirmDialog(
        this,
        "Generate " + potentialCount + " new offerings?",
        "Confirm Generation",
        JOptionPane.YES_NO_OPTION,
        JOptionPane.QUESTION_MESSAGE
    );

    if (confirm != JOptionPane.YES_OPTION) {
      return;
    }

    setControlsEnabled(false);

    new SwingWorker<OfferingGenerationResult, Void>() {
      @Override
      protected OfferingGenerationResult doInBackground() {
        return offeringGenerationService.generateOfferings(
            enrollmentPeriodId,
            semesterName,
            chkOnlyActiveSections.isSelected(),
            chkIncludeWaitlist.isSelected()
        );
      }

      @Override
      protected void done() {
        try {
          OfferingGenerationResult result = get();

          if (!result.successful()) {
            JOptionPane.showMessageDialog(
                RegistrarOfferingsManagement.this,
                result.message(),
                "Generate Offerings",
                JOptionPane.ERROR_MESSAGE
            );
            return;
          }

          JOptionPane.showMessageDialog(
              RegistrarOfferingsManagement.this,
              "Created: " + result.createdCount()
                  + "\nAlready existing: " + result.existingCount()
                  + "\nSkipped: " + result.skippedCount(),
              "Generate Offerings",
              JOptionPane.INFORMATION_MESSAGE
          );

          previewGenerationPlan();
        } catch (InterruptedException | ExecutionException e) {
          JOptionPane.showMessageDialog(
              RegistrarOfferingsManagement.this,
              "Failed to generate offerings: " + e.getMessage(),
              "Error",
              JOptionPane.ERROR_MESSAGE
          );
          setControlsEnabled(true);
        }
      }
    }.execute();
  }

  private Long getSelectedEnrollmentPeriodId() {
    Object selected = cbxEnrollmentPeriod.getSelectedItem();
    if (selected == null) {
      return null;
    }

    return enrollmentPeriodIdByLabel.get(selected.toString());
  }

  private String getSelectedSemesterName() {
    Object selected = cbxSemester.getSelectedItem();
    return selected == null ? null : selected.toString();
  }

  private void populatePreviewTable() {
    DefaultTableModel model = (DefaultTableModel) tablePreview.getModel();
    model.setRowCount(0);

    for (OfferingGenerationPlanRow row : currentPlanRows) {
      model.addRow(
          new Object[]{
              row.subjectCode(),
              row.subjectName(),
              row.sectionCode(),
              row.sectionCapacity() == null ? "N/A" : row.sectionCapacity(),
              row.semesterSubjectId() == null ? "N/A" : row.semesterSubjectId(),
              row.alreadyExists() ? "YES" : "NO"
          }
      );
    }

    updateSummaryLabels();
  }

  private void clearPreview() {
    currentPlanRows.clear();

    DefaultTableModel model = (DefaultTableModel) tablePreview.getModel();
    model.setRowCount(0);

    updateSummaryLabels();
  }

  private void updateSummaryLabels() {
    int candidates = currentPlanRows.size();
    int existing = 0;

    for (OfferingGenerationPlanRow row : currentPlanRows) {
      if (row.alreadyExists()) {
        existing++;
      }
    }

    int potential = Math.max(0, candidates - existing);

    lblCandidates.setText("Candidates: " + candidates);
    lblExisting.setText("Already Existing: " + existing);
    lblPotential.setText("Potential New: " + potential);

    btnGenerate.setEnabled(potential > 0);
  }

  private int getPotentialNewCount() {
    int existing = 0;
    for (OfferingGenerationPlanRow row : currentPlanRows) {
      if (row.alreadyExists()) {
        existing++;
      }
    }

    return Math.max(0, currentPlanRows.size() - existing);
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
    jLabel3 = new javax.swing.JLabel();
    cbxEnrollmentPeriod = new javax.swing.JComboBox<>();
    jLabel4 = new javax.swing.JLabel();
    cbxSemester = new javax.swing.JComboBox<>();
    btnRefresh = new javax.swing.JButton();
    btnPreview = new javax.swing.JButton();
    btnGenerate = new javax.swing.JButton();
    chkOnlyActiveSections = new javax.swing.JCheckBox();
    chkIncludeWaitlist = new javax.swing.JCheckBox();
    jScrollPane1 = new javax.swing.JScrollPane();
    tablePreview = new javax.swing.JTable();
    lblCandidates = new javax.swing.JLabel();
    lblExisting = new javax.swing.JLabel();
    lblPotential = new javax.swing.JLabel();

    setBackground(new java.awt.Color(255, 255, 255));
    setMaximumSize(new java.awt.Dimension(1181, 684));
    setMinimumSize(new java.awt.Dimension(1181, 684));

    jLabel1.setFont(new java.awt.Font("Poppins", 0, 24)); // NOI18N
    jLabel1.setText("Offerings Management");

    jLabel2.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
    jLabel2.setForeground(new java.awt.Color(153, 153, 153));
    jLabel2.setText("Generate offerings in bulk for a selected enrollment period and semester.");

    jPanel1.setBackground(new java.awt.Color(255, 255, 255));
    jPanel1.setBorder(new com.group5.paul_esys.ui.PanelRoundBorder());

    jLabel3.setText("Enrollment Period");

    cbxEnrollmentPeriod.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "" }));

    jLabel4.setText("Semester");

    cbxSemester.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "" }));

    btnRefresh.setText("Refresh Filters");
    btnRefresh.addActionListener(this::btnRefreshActionPerformed);

    btnPreview.setText("Preview");
    btnPreview.addActionListener(this::btnPreviewActionPerformed);

    btnGenerate.setText("Generate Offerings");
    btnGenerate.addActionListener(this::btnGenerateActionPerformed);

    chkOnlyActiveSections.setSelected(true);
    chkOnlyActiveSections.setText("Use active sections only");
    chkOnlyActiveSections.addActionListener(this::chkOnlyActiveSectionsActionPerformed);

    chkIncludeWaitlist.setSelected(true);
    chkIncludeWaitlist.setText("Include WAITLIST sections");

    tablePreview.setModel(new javax.swing.table.DefaultTableModel(
        new Object [][] {
            {null, null, null, null, null, null},
            {null, null, null, null, null, null},
            {null, null, null, null, null, null},
            {null, null, null, null, null, null}
        },
        new String [] {
            "Subject Code", "Subject Name", "Section", "Capacity", "Semester Subject ID", "Already Exists"
        }
    ) {
      Class<?>[] types = new Class<?> [] {
        java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Object.class, java.lang.Object.class, java.lang.String.class
      };
      boolean[] canEdit = new boolean [] {
        false, false, false, false, false, false
      };

      public Class<?> getColumnClass(int columnIndex) {
        return types [columnIndex];
      }

      public boolean isCellEditable(int rowIndex, int columnIndex) {
        return canEdit [columnIndex];
      }
    });
    jScrollPane1.setViewportView(tablePreview);

    lblCandidates.setText("Candidates: 0");

    lblExisting.setText("Already Existing: 0");

    lblPotential.setText("Potential New: 0");

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
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(cbxEnrollmentPeriod, javax.swing.GroupLayout.PREFERRED_SIZE, 320, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(jLabel4)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(cbxSemester, javax.swing.GroupLayout.PREFERRED_SIZE, 190, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(btnRefresh)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(btnPreview)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(btnGenerate)
            .addGap(0, 0, Short.MAX_VALUE))
          .addGroup(jPanel1Layout.createSequentialGroup()
            .addComponent(chkOnlyActiveSections)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addComponent(chkIncludeWaitlist)
            .addGap(0, 0, Short.MAX_VALUE))
          .addGroup(jPanel1Layout.createSequentialGroup()
            .addComponent(lblCandidates)
            .addGap(18, 18, 18)
            .addComponent(lblExisting)
            .addGap(18, 18, 18)
            .addComponent(lblPotential)
            .addGap(0, 0, Short.MAX_VALUE)))
        .addContainerGap())
    );
    jPanel1Layout.setVerticalGroup(
      jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(jPanel1Layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(jLabel3)
          .addComponent(cbxEnrollmentPeriod, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
          .addComponent(jLabel4)
          .addComponent(cbxSemester, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
          .addComponent(btnRefresh)
          .addComponent(btnPreview)
          .addComponent(btnGenerate))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(chkOnlyActiveSections)
          .addComponent(chkIncludeWaitlist))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 494, Short.MAX_VALUE)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(lblCandidates)
          .addComponent(lblExisting)
          .addComponent(lblPotential))
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
        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        .addContainerGap())
    );
  }// </editor-fold>//GEN-END:initComponents

  private void chkOnlyActiveSectionsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkOnlyActiveSectionsActionPerformed
    onActiveSectionsFilterChanged();
  }//GEN-LAST:event_chkOnlyActiveSectionsActionPerformed

  private void btnRefreshActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRefreshActionPerformed
    reloadFilterOptions();
  }//GEN-LAST:event_btnRefreshActionPerformed

  private void btnPreviewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPreviewActionPerformed
    previewGenerationPlan();
  }//GEN-LAST:event_btnPreviewActionPerformed

  private void btnGenerateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGenerateActionPerformed
    generateOfferings();
  }//GEN-LAST:event_btnGenerateActionPerformed

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JButton btnGenerate;
  private javax.swing.JButton btnPreview;
  private javax.swing.JButton btnRefresh;
  private javax.swing.JComboBox<String> cbxEnrollmentPeriod;
  private javax.swing.JComboBox<String> cbxSemester;
  private javax.swing.JCheckBox chkIncludeWaitlist;
  private javax.swing.JCheckBox chkOnlyActiveSections;
  private javax.swing.JLabel jLabel1;
  private javax.swing.JLabel jLabel2;
  private javax.swing.JLabel jLabel3;
  private javax.swing.JLabel jLabel4;
  private javax.swing.JPanel jPanel1;
  private javax.swing.JScrollPane jScrollPane1;
  private javax.swing.JLabel lblCandidates;
  private javax.swing.JLabel lblExisting;
  private javax.swing.JLabel lblPotential;
  private javax.swing.JTable tablePreview;
  // End of variables declaration//GEN-END:variables
}
