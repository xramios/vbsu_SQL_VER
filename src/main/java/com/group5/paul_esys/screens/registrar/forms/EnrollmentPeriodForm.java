/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.group5.paul_esys.screens.registrar.forms;

import com.group5.paul_esys.modules.enrollment_period.model.EnrollmentPeriod;
import com.group5.paul_esys.modules.enrollment_period.services.EnrollmentPeriodService;
import com.group5.paul_esys.modules.enrollment_period.utils.EnrollmentPeriodUtils;
import com.group5.paul_esys.utils.FormValidationUtil;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;
import raven.datetime.DatePicker;

/**
 *
 * @author nytri
 */
public class EnrollmentPeriodForm extends javax.swing.JFrame {
	
	private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(EnrollmentPeriodForm.class.getName());
        private static final Pattern SCHOOL_YEAR_PATTERN = Pattern.compile("^(\\d{4})\\s*-\\s*(\\d{4})$");
        private static final int SCHOOL_YEAR_TEXT_LENGTH = 9;
        private static final int MAX_DESCRIPTION_LENGTH = 500;

        private static final String[] SEMESTER_OPTIONS = {
                "First Semester",
                "Second Semester",
                "Summer"
        };

        private final EnrollmentPeriodService enrollmentPeriodService = EnrollmentPeriodService.getInstance();
        private final EnrollmentPeriod editingEnrollmentPeriod;
        private final Runnable onSavedCallback;
	private DatePicker startDatePicker = new DatePicker();
	private DatePicker endDatePicker = new DatePicker();

	/**
	 * Creates new form EnrollmentPeriodF
	 */
	public EnrollmentPeriodForm() {
		this(null, null);
	}

        public EnrollmentPeriodForm(EnrollmentPeriod editingEnrollmentPeriod, Runnable onSavedCallback) {
                this.editingEnrollmentPeriod = editingEnrollmentPeriod;
                this.onSavedCallback = onSavedCallback;
                this.setUndecorated(true);
                initComponents();
                this.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
                this.setLocationRelativeTo(null);
                initializeForm();
        }

        private void configureDatePickers() {

		startDatePicker.setEditor(ftxtStartDate);
		endDatePicker.setEditor(ftxtEndDate);
	}

        private void initializeForm() {
		this.configureDatePickers();
                txtSemester.setVisible(false);
                javax.swing.JComboBox<String> cbxSemesterOpt = new javax.swing.JComboBox<>(SEMESTER_OPTIONS);
                cbxSemesterOpt.setEditable(false);
                jPanel1.add(cbxSemesterOpt, new org.netbeans.lib.awtextra.AbsoluteConstraints(54, 188, 243, -1));

                if (editingEnrollmentPeriod == null) {
                        windowBar1.setTitle("Enrollment Period Form");
                        jLabel1.setText("Enrollment Period Form");
                        jLabel2.setText("Start a new school period");
                        btnSave.setText("Save");
                        return;
                }

                windowBar1.setTitle("Update Enrollment Period");
                jLabel1.setText("Update Enrollment Period");
                jLabel2.setText("Update existing enrollment period");
                btnSave.setText("Update");

                txtSchoolYear.setText(EnrollmentPeriodUtils.safeText(editingEnrollmentPeriod.getSchoolYear(), ""));
                cbxSemesterOpt.setSelectedItem(EnrollmentPeriodUtils.safeText(editingEnrollmentPeriod.getSemester(), SEMESTER_OPTIONS[0]));
                textAreaDescription.setText(EnrollmentPeriodUtils.safeText(editingEnrollmentPeriod.getDescription(), ""));

                if (editingEnrollmentPeriod.getStartDate() != null) {
                        startDatePicker.setSelectedDate(toLocalDate(editingEnrollmentPeriod.getStartDate()));
                }

                if (editingEnrollmentPeriod.getEndDate() != null) {
                        endDatePicker.setSelectedDate(toLocalDate(editingEnrollmentPeriod.getEndDate()));
                }
        }

        private String normalizeSchoolYear(String schoolYear) {
                if (schoolYear == null) {
                        return "";
                }

                return schoolYear.trim().replaceAll("\\s+", "");
        }


        private LocalDate toLocalDate(Date date) {
                if (date == null) {
                        return null;
                }

                return date.toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();
        }

        private Date toDate(LocalDate date, boolean endOfDay) {
                if (date == null) {
                        return null;
                }

                return Date.from((endOfDay ? date.atTime(LocalTime.MAX) : date.atStartOfDay())
                        .atZone(ZoneId.systemDefault())
                        .toInstant());
        }

        private String buildEnrollmentPeriodLabel(EnrollmentPeriod period) {
                if (period == null) {
                        return "N/A";
                }

                return EnrollmentPeriodUtils.safeText(period.getSchoolYear(), "N/A")
                        + " - "
                        + EnrollmentPeriodUtils.safeText(period.getSemester(), "N/A");
        }

        private boolean hasConflict(EnrollmentPeriod period) {
                Optional<EnrollmentPeriod> conflictingPeriod = enrollmentPeriodService.findConflictingEnrollmentPeriod(period);
                if (conflictingPeriod.isEmpty()) {
                        return false;
                }

                EnrollmentPeriod existingPeriod = conflictingPeriod.get();
                JOptionPane.showMessageDialog(
                        this,
                        "The selected date range overlaps with "
                                + buildEnrollmentPeriodLabel(existingPeriod)
                                + " ("
                                + EnrollmentPeriodUtils.formatDateTime(existingPeriod.getStartDate())
                                + " to "
                                + EnrollmentPeriodUtils.formatDateTime(existingPeriod.getEndDate())
                                + ").",
                        "Conflict Detected",
                        JOptionPane.WARNING_MESSAGE
                );
                return true;
        }

        private boolean isValidForm() {
                String schoolYear = normalizeSchoolYear(txtSchoolYear.getText());
                if (showValidationError(
                        FormValidationUtil.validateRequiredText(
                                "School year",
                                schoolYear,
                                SCHOOL_YEAR_TEXT_LENGTH,
                                SCHOOL_YEAR_TEXT_LENGTH,
                                Pattern.compile("^[0-9\\-]+$"),
                                "numbers and hyphen in YYYY-YYYY format"
                        )
                )) {
                        return false;
                }

                Matcher schoolYearMatcher = SCHOOL_YEAR_PATTERN.matcher(schoolYear);
                if (!schoolYearMatcher.matches()) {
                        JOptionPane.showMessageDialog(
                                this,
                                "School year must follow YYYY-YYYY format.",
                                "Validation Error",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return false;
                }

                int firstYear = Integer.parseInt(schoolYearMatcher.group(1));
                int secondYear = Integer.parseInt(schoolYearMatcher.group(2));
                if (secondYear != firstYear + 1) {
                        JOptionPane.showMessageDialog(
                                this,
                                "School year must use consecutive years (for example 2025-2026).",
                                "Validation Error",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return false;
                }

                LocalDate startDate = startDatePicker.getSelectedDate();
                LocalDate endDate = endDatePicker.getSelectedDate();

                if (startDate == null || endDate == null) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Start date and end date are required.",
                                "Validation Error",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return false;
                }
                
                if (editingEnrollmentPeriod == null && startDate.isBefore(LocalDate.now())) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Start date cannot be in the past.",
                                "Validation Error",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return false;
                }

                if (endDate.isBefore(startDate)) {
                        JOptionPane.showMessageDialog(
                                this,
                                "End date must be on or after the start date.",
                                "Validation Error",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return false;
                }

                if (showValidationError(
                        FormValidationUtil.validateOptionalText(
                                "Description",
                                textAreaDescription.getText(),
                                1,
                                MAX_DESCRIPTION_LENGTH,
                                null,
                                ""
                        )
                )) {
                        return false;
                }

                return true;
        }

        private boolean showValidationError(Optional<String> validationError) {
                if (validationError.isEmpty()) {
                        return false;
                }

                JOptionPane.showMessageDialog(
                        this,
                        validationError.get(),
                        "Validation Error",
                        JOptionPane.WARNING_MESSAGE
                );
                return true;
        }

        private void saveEnrollmentPeriod() {
                if (!isValidForm()) {
                        return;
                }

                LocalDate startDate = startDatePicker.getSelectedDate();
                LocalDate endDate = endDatePicker.getSelectedDate();
                
                String selectedSemester = "First Semester";
                for (java.awt.Component comp : jPanel1.getComponents()) {
                        if (comp instanceof javax.swing.JComboBox<?> cbx) {
                                Object item = cbx.getSelectedItem();
                                if (item != null) {
                                        selectedSemester = item.toString();
                                        break;
                                }
                        }
                }

                EnrollmentPeriod period = editingEnrollmentPeriod == null ? new EnrollmentPeriod() : editingEnrollmentPeriod;
                period
                        .setSchoolYear(normalizeSchoolYear(txtSchoolYear.getText()))
                        .setSemester(selectedSemester)
                        .setStartDate(toDate(startDate, false))
                        .setEndDate(toDate(endDate, true))
                        .setDescription(EnrollmentPeriodUtils.normalizeDescription(textAreaDescription.getText()));

                if (hasConflict(period)) {
                        return;
                }

                boolean success = editingEnrollmentPeriod == null
                        ? enrollmentPeriodService.createEnrollmentPeriod(period)
                        : enrollmentPeriodService.updateEnrollmentPeriod(period);

                if (!success) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Failed to save enrollment period. Please try again.",
                                "Save Failed",
                                JOptionPane.ERROR_MESSAGE
                        );
                        return;
                }

                JOptionPane.showMessageDialog(
                        this,
                        editingEnrollmentPeriod == null
                                ? "Enrollment period created successfully."
                                : "Enrollment period updated successfully.",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE
                );

                if (onSavedCallback != null) {
                        onSavedCallback.run();
                }

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
                txtSchoolYear = new javax.swing.JTextField();
                txtSemester = new javax.swing.JTextField();
                jLabel4 = new javax.swing.JLabel();
                jLabel5 = new javax.swing.JLabel();
                jLabel6 = new javax.swing.JLabel();
                jLabel7 = new javax.swing.JLabel();
                jScrollPane1 = new javax.swing.JScrollPane();
                textAreaDescription = new javax.swing.JTextArea();
                btnSave = new javax.swing.JButton();
                btnCanel = new javax.swing.JButton();
                ftxtEndDate = new javax.swing.JFormattedTextField();
                ftxtStartDate = new javax.swing.JFormattedTextField();

                setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

                windowBar1.setTitle("Enrollment Period Form");

                jPanel1.setBackground(new java.awt.Color(255, 255, 255));
                jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

                jLabel1.setFont(new java.awt.Font("Poppins", 0, 18)); // NOI18N
                jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                jLabel1.setText("Enrollment Period Form");
                jPanel1.add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(6, 29, 336, -1));

                jLabel2.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
                jLabel2.setForeground(new java.awt.Color(153, 153, 153));
                jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                jLabel2.setText("Start a new school period");
                jPanel1.add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(6, 63, 336, -1));

                jLabel3.setText("School Year");
                jPanel1.add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(54, 113, 243, -1));
                jPanel1.add(txtSchoolYear, new org.netbeans.lib.awtextra.AbsoluteConstraints(54, 136, 243, -1));
                jPanel1.add(txtSemester, new org.netbeans.lib.awtextra.AbsoluteConstraints(54, 188, 243, -1));

                jLabel4.setText("Semester");
                jPanel1.add(jLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(54, 165, 243, -1));

                jLabel5.setText("Start Date");
                jPanel1.add(jLabel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(54, 217, 243, -1));

                jLabel6.setText("End Date");
                jPanel1.add(jLabel6, new org.netbeans.lib.awtextra.AbsoluteConstraints(54, 269, 243, -1));

                jLabel7.setText("Description");
                jPanel1.add(jLabel7, new org.netbeans.lib.awtextra.AbsoluteConstraints(54, 321, 243, -1));

                textAreaDescription.setColumns(20);
                textAreaDescription.setRows(5);
                textAreaDescription.addKeyListener(new java.awt.event.KeyAdapter() {
                        public void keyReleased(java.awt.event.KeyEvent evt) {
                                textAreaDescriptionKeyReleased(evt);
                        }
                });
                jScrollPane1.setViewportView(textAreaDescription);

                jPanel1.add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(54, 344, 243, -1));

                btnSave.setBackground(new java.awt.Color(119, 0, 0));
                btnSave.setForeground(new java.awt.Color(255, 255, 255));
                btnSave.setText("Save");
                btnSave.addActionListener(this::btnSaveActionPerformed);
                jPanel1.add(btnSave, new org.netbeans.lib.awtextra.AbsoluteConstraints(196, 441, -1, -1));

                btnCanel.setBackground(new java.awt.Color(119, 0, 0));
                btnCanel.setForeground(new java.awt.Color(255, 255, 255));
                btnCanel.setText("Cancel");
                btnCanel.addActionListener(this::btnCanelActionPerformed);
                jPanel1.add(btnCanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(91, 441, -1, -1));
                jPanel1.add(ftxtEndDate, new org.netbeans.lib.awtextra.AbsoluteConstraints(55, 290, 240, -1));
                jPanel1.add(ftxtStartDate, new org.netbeans.lib.awtextra.AbsoluteConstraints(55, 240, 240, -1));

                javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
                getContentPane().setLayout(layout);
                layout.setHorizontalGroup(
                        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 350, Short.MAX_VALUE)
                        .addComponent(windowBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                );
                layout.setVerticalGroup(
                        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(windowBar1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, 0)
                                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 507, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, 0))
                );

                pack();
        }// </editor-fold>//GEN-END:initComponents

        private void btnSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveActionPerformed
                saveEnrollmentPeriod();
        }//GEN-LAST:event_btnSaveActionPerformed

        private void btnCanelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCanelActionPerformed
		dispose();
        }//GEN-LAST:event_btnCanelActionPerformed

        private void textAreaDescriptionKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textAreaDescriptionKeyReleased
                if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER && evt.isControlDown()) {
                        saveEnrollmentPeriod();
                }
        }//GEN-LAST:event_textAreaDescriptionKeyReleased

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
		java.awt.EventQueue.invokeLater(() -> new EnrollmentPeriodForm().setVisible(true));
	}

        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JButton btnCanel;
        private javax.swing.JButton btnSave;
        private javax.swing.JFormattedTextField ftxtEndDate;
        private javax.swing.JFormattedTextField ftxtStartDate;
        private javax.swing.JLabel jLabel1;
        private javax.swing.JLabel jLabel2;
        private javax.swing.JLabel jLabel3;
        private javax.swing.JLabel jLabel4;
        private javax.swing.JLabel jLabel5;
        private javax.swing.JLabel jLabel6;
        private javax.swing.JLabel jLabel7;
        private javax.swing.JPanel jPanel1;
        private javax.swing.JScrollPane jScrollPane1;
        private javax.swing.JTextArea textAreaDescription;
        private javax.swing.JTextField txtSchoolYear;
        private javax.swing.JTextField txtSemester;
        private com.group5.paul_esys.components.WindowBar windowBar1;
        // End of variables declaration//GEN-END:variables
}
