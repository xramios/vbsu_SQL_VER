/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.group5.paul_esys.screens.registrar.forms;

import com.group5.paul_esys.modules.departments.model.Department;
import com.group5.paul_esys.modules.departments.services.DepartmentService;
import com.group5.paul_esys.modules.faculty.model.Faculty;
import com.group5.paul_esys.modules.faculty.services.FacultyService;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JOptionPane;

/**
 *
 * @author Shan
 */
public class FacultyForm extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(FacultyForm.class.getName());
                private static final DateTimeFormatter PASSWORD_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
        private final FacultyService facultyService = FacultyService.getInstance();
        private final DepartmentService departmentService = DepartmentService.getInstance();

        private final Map<String, Long> departmentIdByLabel = new LinkedHashMap<>();
        private final Map<Long, String> departmentLabelById = new LinkedHashMap<>();

        private final Faculty editingFaculty;
        private final Runnable onSavedCallback;
        private final Long preferredDepartmentId;

    /**
     * Creates new form FacultyForm
     */
    public FacultyForm() {
                this(null, null, null);
        }

        public FacultyForm(Faculty editingFaculty, Long preferredDepartmentId, Runnable onSavedCallback) {
                this.editingFaculty = editingFaculty;
                this.onSavedCallback = onSavedCallback;
                this.preferredDepartmentId = preferredDepartmentId;
                this.setUndecorated(true);
                initComponents();
                initializeForm();
        }

        private void initializeForm() {
                setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
                setLocationRelativeTo(null);

                btnCancel.addActionListener(evt -> dispose());
                loadDepartments();

                if (editingFaculty == null) {
                        selectPreferredDepartment();
                        return;
                }

                jLabel1.setText("Update Faculty");
                jLabel2.setText("Update existing faculty member");
                btnSave.setText("Update");

                txtFirstName.setText(editingFaculty.getFirstName() == null ? "" : editingFaculty.getFirstName());
                txtLastName.setText(editingFaculty.getLastName() == null ? "" : editingFaculty.getLastName());
                txtLastName1.setText(editingFaculty.getMiddleName() == null ? "" : editingFaculty.getMiddleName());
                txtLastName2.setText(editingFaculty.getContactNumber() == null ? "" : editingFaculty.getContactNumber());
                txtEmail.setText(
                        editingFaculty.getUserId() == null
                                ? ""
                                : facultyService.getUserEmailByUserId(editingFaculty.getUserId()).orElse("")
                );

                if (editingFaculty.getBirthdate() != null) {
                        LocalDate birthDate = toLocalDate(editingFaculty.getBirthdate());
                        if (birthDate != null) {
                                datePicker1.setSelectedDate(birthDate);
                        }
                }

                if (editingFaculty.getDepartmentId() != null) {
                        String departmentLabel = departmentLabelById.get(editingFaculty.getDepartmentId());
                        if (departmentLabel != null) {
                                cbxDepartment.setSelectedItem(departmentLabel);
                        }
                }
        }

        private void loadDepartments() {
                cbxDepartment.removeAllItems();
                departmentIdByLabel.clear();
                departmentLabelById.clear();

                for (Department department : departmentService.getAllDepartments()) {
                        String label = buildDepartmentLabel(department);
                        cbxDepartment.addItem(label);
                        departmentIdByLabel.put(label, department.getId());
                        departmentLabelById.put(department.getId(), label);
                }
        }

        private String buildDepartmentLabel(Department department) {
                String name = department.getDepartmentName() == null || department.getDepartmentName().trim().isEmpty()
                        ? "Department"
                        : department.getDepartmentName().trim();

                String code = department.getDepartmentCode() == null ? "" : department.getDepartmentCode().trim();
                if (code.isEmpty()) {
                        return name + " - ID " + department.getId();
                }

                return name + " (" + code + ") - ID " + department.getId();
        }

        private void selectPreferredDepartment() {
                if (preferredDepartmentId == null) {
                        return;
                }

                String departmentLabel = departmentLabelById.get(preferredDepartmentId);
                if (departmentLabel != null) {
                        cbxDepartment.setSelectedItem(departmentLabel);
                }
        }

        private String normalizeOptionalText(String value) {
                if (value == null) {
                        return null;
                }

                String trimmed = value.trim();
                return trimmed.isEmpty() ? null : trimmed;
        }

        private LocalDate toLocalDate(java.util.Date date) {
                if (date == null) {
                        return null;
                }

                if (date instanceof java.sql.Date sqlDate) {
                        return sqlDate.toLocalDate();
                }

                return date.toInstant()
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate();
        }

        private boolean isValidForm() {
                if (txtFirstName.getText() == null || txtFirstName.getText().trim().isEmpty()) {
                        JOptionPane.showMessageDialog(
                                this,
                                "First name is required.",
                                "Validation Error",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return false;
                }

                if (txtLastName.getText() == null || txtLastName.getText().trim().isEmpty()) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Last name is required.",
                                "Validation Error",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return false;
                }

                if (txtEmail.getText() == null || txtEmail.getText().trim().isEmpty()) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Email is required.",
                                "Validation Error",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return false;
                }

                String email = txtEmail.getText().trim();
                if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Email format is invalid.",
                                "Validation Error",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return false;
                }

                String contactNumber = txtLastName2.getText() == null ? "" : txtLastName2.getText().trim();
                if (!contactNumber.isEmpty() && !contactNumber.matches("^[0-9+\\-\\s()]{7,20}$")) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Contact number format is invalid.",
                                "Validation Error",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return false;
                }

                if (datePicker1.getSelectedDate() == null) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Birth date is required.",
                                "Validation Error",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return false;
                }

                Object selectedDepartment = cbxDepartment.getSelectedItem();
                if (selectedDepartment == null || !departmentIdByLabel.containsKey(selectedDepartment.toString())) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Please select a department.",
                                "Validation Error",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return false;
                }

                return true;
        }

        private String buildInitialPassword(LocalDate birthDate) {
                // LastName + "_" + Birthdate in ISO format (e.g. 1990-01-31)
                return this.txtLastName.getText() + "_" + PASSWORD_DATE_FORMATTER.format(birthDate);
        }

        private void saveFaculty() {
                if (!isValidForm()) {
                        return;
                }

                String firstName = txtFirstName.getText().trim();
                String lastName = txtLastName.getText().trim();
                String middleName = normalizeOptionalText(txtLastName1.getText());
                String contactNumber = normalizeOptionalText(txtLastName2.getText());
                LocalDate birthDate = datePicker1.getSelectedDate();
                String email = txtEmail.getText().trim();
                Long departmentId = departmentIdByLabel.get(cbxDepartment.getSelectedItem().toString());

                Faculty faculty = editingFaculty == null ? new Faculty() : editingFaculty;
                faculty
                        .setFirstName(firstName)
                        .setLastName(lastName)
                        .setMiddleName(middleName)
                        .setContactNumber(contactNumber)
                        .setBirthdate(java.sql.Date.valueOf(birthDate))
                        .setDepartmentId(departmentId);

                if (editingFaculty == null) {
                        String initialPassword = buildInitialPassword(birthDate);
                        boolean created = facultyService.registerFaculty(email, initialPassword, faculty).isPresent();

                        if (!created) {
                                JOptionPane.showMessageDialog(
                                        this,
                                        "Failed to create faculty. Please try again.",
                                        "Save Failed",
                                        JOptionPane.ERROR_MESSAGE
                                );
                                return;
                        }

                        JOptionPane.showMessageDialog(
                                this,
                                "Faculty created successfully.\nInitial Password: " + initialPassword,
                                "Success",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                } else {
                        boolean updated = facultyService.updateFacultyWithEmail(faculty, email);

                        if (!updated) {
                                JOptionPane.showMessageDialog(
                                        this,
                                        "Failed to update faculty. Please try again.",
                                        "Save Failed",
                                        JOptionPane.ERROR_MESSAGE
                                );
                                return;
                        }

                        JOptionPane.showMessageDialog(
                                this,
                                "Faculty updated successfully.",
                                "Success",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                }

                if (onSavedCallback != null) {
                        onSavedCallback.run();
                }

                dispose();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
        // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
        private void initComponents() {

                datePicker1 = new raven.datetime.DatePicker();
                windowBar1 = new com.group5.paul_esys.components.WindowBar();
                jPanel1 = new javax.swing.JPanel();
                jLabel1 = new javax.swing.JLabel();
                jLabel2 = new javax.swing.JLabel();
                jLabel3 = new javax.swing.JLabel();
                txtFirstName = new javax.swing.JTextField();
                txtLastName = new javax.swing.JTextField();
                jLabel4 = new javax.swing.JLabel();
                jLabel5 = new javax.swing.JLabel();
                cbxDepartment = new javax.swing.JComboBox<>();
                jLabel6 = new javax.swing.JLabel();
                txtEmail = new javax.swing.JTextField();
                btnSave = new javax.swing.JButton();
                btnCancel = new javax.swing.JButton();
                txtLastName1 = new javax.swing.JTextField();
                jLabel7 = new javax.swing.JLabel();
                jLabel8 = new javax.swing.JLabel();
                ftxtDatePicker = new javax.swing.JFormattedTextField();
                jLabel9 = new javax.swing.JLabel();
                txtLastName2 = new javax.swing.JTextField();

                datePicker1.setEditor(ftxtDatePicker);

                setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

                windowBar1.setTitle("Faculty Form");

                jPanel1.setBackground(new java.awt.Color(255, 255, 255));

                jLabel1.setFont(new java.awt.Font("Poppins", 0, 18)); // NOI18N
                jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                jLabel1.setText("Faculty Form");

                jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                jLabel2.setText("Add/Update Faculty Member");

                jLabel3.setText("First Name");

                txtFirstName.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());

                txtLastName.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());

                jLabel4.setText("Last Name");

                jLabel5.setText("Department");

                cbxDepartment.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

                jLabel6.setText("Email Address (For Portal)");

                txtEmail.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());

                btnSave.setBackground(new java.awt.Color(119, 0, 0));
                btnSave.setForeground(new java.awt.Color(255, 255, 255));
                btnSave.setText("Save");
                btnSave.addActionListener(this::btnSaveActionPerformed);

                btnCancel.setBackground(new java.awt.Color(119, 0, 0));
                btnCancel.setForeground(new java.awt.Color(255, 255, 255));
                btnCancel.setText("Cancel");

                txtLastName1.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());

                jLabel7.setText("Middle Name");

                jLabel8.setText("Date of Birth");

                jLabel9.setText("Contact Number");

                txtLastName2.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());

                javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
                jPanel1.setLayout(jPanel1Layout);
                jPanel1Layout.setHorizontalGroup(
                        jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap())
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(44, 44, 44)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jLabel8)
                                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(jLabel5)
                                                        .addComponent(cbxDepartment, javax.swing.GroupLayout.PREFERRED_SIZE, 258, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addComponent(btnCancel, javax.swing.GroupLayout.PREFERRED_SIZE, 89, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                                .addComponent(btnSave, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addGap(34, 34, 34)))
                                                .addComponent(ftxtDatePicker))
                                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                .addComponent(jLabel9)
                                                .addComponent(jLabel7)
                                                .addComponent(txtLastName1, javax.swing.GroupLayout.DEFAULT_SIZE, 258, Short.MAX_VALUE)
                                                .addComponent(jLabel6)
                                                .addComponent(jLabel4)
                                                .addComponent(txtLastName)
                                                .addComponent(jLabel3)
                                                .addComponent(txtFirstName)
                                                .addComponent(txtEmail)
                                                .addComponent(txtLastName2)))
                                .addContainerGap(44, Short.MAX_VALUE))
                );
                jPanel1Layout.setVerticalGroup(
                        jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(28, 28, 28)
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel2)
                                .addGap(18, 18, 18)
                                .addComponent(jLabel6)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtEmail, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(5, 5, 5)
                                .addComponent(jLabel3)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtFirstName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel4)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtLastName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel7)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtLastName1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel9)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtLastName2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel8)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(ftxtDatePicker, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jLabel5)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(cbxDepartment, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(btnSave)
                                        .addComponent(btnCancel))
                                .addContainerGap(26, Short.MAX_VALUE))
                );

                javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
                getContentPane().setLayout(layout);
                layout.setHorizontalGroup(
                        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(windowBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 346, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                );
                layout.setVerticalGroup(
                        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(windowBar1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, 0)
                                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                );

                pack();
        }// </editor-fold>//GEN-END:initComponents

        private void btnSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveActionPerformed
                saveFaculty();
        }//GEN-LAST:event_btnSaveActionPerformed

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
        java.awt.EventQueue.invokeLater(() -> new FacultyForm().setVisible(true));
    }

        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JButton btnCancel;
        private javax.swing.JButton btnSave;
        private javax.swing.JComboBox<String> cbxDepartment;
        private raven.datetime.DatePicker datePicker1;
        private javax.swing.JFormattedTextField ftxtDatePicker;
        private javax.swing.JLabel jLabel1;
        private javax.swing.JLabel jLabel2;
        private javax.swing.JLabel jLabel3;
        private javax.swing.JLabel jLabel4;
        private javax.swing.JLabel jLabel5;
        private javax.swing.JLabel jLabel6;
        private javax.swing.JLabel jLabel7;
        private javax.swing.JLabel jLabel8;
        private javax.swing.JLabel jLabel9;
        private javax.swing.JPanel jPanel1;
        private javax.swing.JTextField txtEmail;
        private javax.swing.JTextField txtFirstName;
        private javax.swing.JTextField txtLastName;
        private javax.swing.JTextField txtLastName1;
        private javax.swing.JTextField txtLastName2;
        private com.group5.paul_esys.components.WindowBar windowBar1;
        // End of variables declaration//GEN-END:variables
}
