package com.group5.paul_esys.screens.registrar.forms;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */

import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTGitHubIJTheme;
import com.group5.paul_esys.modules.courses.model.Course;
import com.group5.paul_esys.modules.courses.services.CourseService;
import com.group5.paul_esys.modules.curriculum.model.Curriculum;
import com.group5.paul_esys.modules.curriculum.services.CurriculumService;
import com.group5.paul_esys.modules.departments.model.Department;
import com.group5.paul_esys.modules.departments.services.DepartmentService;
import com.group5.paul_esys.modules.students.model.Student;
import com.group5.paul_esys.modules.students.model.StudentStatus;
import com.group5.paul_esys.modules.students.services.StudentService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JOptionPane;

/**
 *
 * @author janea
 */
public class StudentEnrollmentForm extends javax.swing.JFrame {

  private static final java.util.logging.Logger logger =
    java.util.logging.Logger.getLogger(StudentEnrollmentForm.class.getName());
  private final StudentService studentService = StudentService.getInstance();
  private final DepartmentService departmentService = DepartmentService.getInstance();
  private final CourseService courseService = CourseService.getInstance();
  private final CurriculumService curriculumService = CurriculumService.getInstance();
  private final Map<String, Long> departmentIdByName = new LinkedHashMap<>();
  private final Map<String, Long> courseIdByName = new LinkedHashMap<>();
  private final Map<String, Long> curriculumIdByName = new LinkedHashMap<>();
  private final Runnable onSavedCallback;

  /**
   * Creates new form Enrollment
   */
  public StudentEnrollmentForm() {
    this(null);
  }

  public StudentEnrollmentForm(Runnable onSavedCallback) {
    this.onSavedCallback = onSavedCallback;
    FlatMTGitHubIJTheme.setup();
    this.setUndecorated(true);
    initComponents();
    setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
    this.setLocationRelativeTo(null);
    initializeForm();
  }

  private LocalDate toLocalDate(Date date) {
    if (date instanceof java.sql.Date sqlDate) {
      return sqlDate.toLocalDate();
    }

    return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
  }

  private void initializeForm() {
    txtEmail.setEditable(false);

    cbxDepartment.addActionListener(evt -> {
      loadCoursesBySelectedDepartment();
      refreshRegisterButtonState();
    });

    cbxCurriculum.addActionListener(evt -> {
      loadCurriculumsBySelectedCourse();
      refreshRegisterButtonState();
    });

    cbxCurriculum1.addActionListener(evt -> refreshRegisterButtonState());
    jComboBox1.addActionListener(evt -> refreshRegisterButtonState());

    loadDepartments();
    refreshRegisterButtonState();
  }

  private String buildCurriculumLabel(Curriculum curriculum) {
    String curriculumName = curriculum.getName() == null ? "Curriculum" : curriculum.getName();
    if (curriculum.getCurYear() == null) {
      return curriculumName;
    }

    int year = toLocalDate(curriculum.getCurYear()).getYear();

    return curriculumName + " (" + year + ")";
  }

  private void loadDepartments() {
    cbxDepartment.removeAllItems();
    departmentIdByName.clear();

    for (Department department : departmentService.getAllDepartments()) {
      cbxDepartment.addItem(department.getDepartmentName());
      departmentIdByName.put(department.getDepartmentName(), department.getId());
    }

    if (cbxDepartment.getItemCount() > 0) {
      cbxDepartment.setSelectedIndex(0);
      loadCoursesBySelectedDepartment();
    } else {
      cbxCurriculum.removeAllItems();
      cbxCurriculum1.removeAllItems();
      courseIdByName.clear();
      curriculumIdByName.clear();
    }
  }

  private void loadCoursesBySelectedDepartment() {
    cbxCurriculum.removeAllItems();
    cbxCurriculum1.removeAllItems();
    courseIdByName.clear();
    curriculumIdByName.clear();

    Object selectedDepartment = cbxDepartment.getSelectedItem();
    if (selectedDepartment == null) {
      return;
    }

    Long departmentId = departmentIdByName.get(selectedDepartment.toString());
    if (departmentId == null) {
      return;
    }

    for (Course course : courseService.getCoursesByDepartment(departmentId)) {
      cbxCurriculum.addItem(course.getCourseName());
      courseIdByName.put(course.getCourseName(), course.getId());
    }

    if (cbxCurriculum.getItemCount() > 0) {
      cbxCurriculum.setSelectedIndex(0);
      loadCurriculumsBySelectedCourse();
    }
  }

  private void loadCurriculumsBySelectedCourse() {
    cbxCurriculum1.removeAllItems();
    curriculumIdByName.clear();

    Object selectedCourse = cbxCurriculum.getSelectedItem();
    if (selectedCourse == null) {
      return;
    }

    Long courseId = courseIdByName.get(selectedCourse.toString());
    if (courseId == null) {
      return;
    }

    for (Curriculum curriculum : curriculumService.getCurriculumsByCourse(courseId)) {
      String label = buildCurriculumLabel(curriculum);
      cbxCurriculum1.addItem(label);
      curriculumIdByName.put(label, curriculum.getId());
    }
  }

  private void refreshRegisterButtonState() {
    boolean hasFirstName = txtFirstName.getText() != null && !txtFirstName.getText().trim().isEmpty();
    boolean hasLastName = txtLastName.getText() != null && !txtLastName.getText().trim().isEmpty();
    boolean hasEmail = txtEmail.getText() != null && !txtEmail.getText().trim().isEmpty();
    boolean hasCourse = cbxCurriculum.getSelectedItem() != null;

    btnRegister.setEnabled(hasFirstName && hasLastName && hasEmail && hasCourse);
  }

  private String buildInitialPassword(String lastName, LocalDate birthDate) {
    String safeLastName = lastName.trim().replaceAll("\\s+", "");
    String year = String.valueOf(birthDate.getYear());
    String month = String.format("%02d", birthDate.getMonthValue());
    String day = String.format("%02d", birthDate.getDayOfMonth());
    return safeLastName + year + month + day;
  }

  private void registerStudent() {
    String firstName = txtFirstName.getText() == null ? "" : txtFirstName.getText().trim();
    String middleName = txtMiddleName.getText() == null ? "" : txtMiddleName.getText().trim();
    String lastName = txtLastName.getText() == null ? "" : txtLastName.getText().trim();
    String email = txtEmail.getText() == null ? "" : txtEmail.getText().trim();

    if (firstName.isEmpty() || lastName.isEmpty()) {
      JOptionPane.showMessageDialog(
        this,
        "First name and last name are required.",
        "Validation Error",
        JOptionPane.WARNING_MESSAGE
      );
      return;
    }

    Object selectedCourse = cbxCurriculum.getSelectedItem();
    if (selectedCourse == null || !courseIdByName.containsKey(selectedCourse.toString())) {
      JOptionPane.showMessageDialog(
        this,
        "Please select a program/course.",
        "Validation Error",
        JOptionPane.WARNING_MESSAGE
      );
      return;
    }

    LocalDate defaultBirthDate = LocalDate.now().minusYears(18);
    String initialPassword = buildInitialPassword(lastName, defaultBirthDate);

    Student student = new Student()
      .setStudentId(studentService.generateStudentId())
      .setFirstName(firstName)
      .setMiddleName(middleName.isEmpty() ? null : middleName)
      .setLastName(lastName)
      .setBirthdate(java.sql.Date.valueOf(defaultBirthDate))
      .setStudentStatus(StudentStatus.valueOf(jComboBox1.getSelectedItem().toString()))
      .setCourseId(courseIdByName.get(selectedCourse.toString()))
      .setYearLevel(1L);

    boolean success = studentService
      .registerStudent(email, initialPassword, student)
      .isPresent();

    if (!success) {
      JOptionPane.showMessageDialog(
        this,
        "Failed to register student. Please try again.",
        "Registration Failed",
        JOptionPane.ERROR_MESSAGE
      );
      return;
    }

    JOptionPane.showMessageDialog(
      this,
      "Student registered successfully.\nStudent ID: "
        + student.getStudentId()
        + "\nInitial Password: "
        + initialPassword,
      "Success",
      JOptionPane.INFORMATION_MESSAGE
    );

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
  @SuppressWarnings("unchecked")
        // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
        private void initComponents() {

                jPanel1 = new javax.swing.JPanel();
                jLabel1 = new javax.swing.JLabel();
                jLabel2 = new javax.swing.JLabel();
                jLabel3 = new javax.swing.JLabel();
                jLabel5 = new javax.swing.JLabel();
                txtFirstName = new javax.swing.JTextField();
                jLabel6 = new javax.swing.JLabel();
                txtEmail = new javax.swing.JTextField();
                jLabel8 = new javax.swing.JLabel();
                jLabel9 = new javax.swing.JLabel();
                btnClose = new javax.swing.JButton();
                btnRegister = new javax.swing.JButton();
                jComboBox1 = new javax.swing.JComboBox<>();
                windowBar1 = new com.group5.paul_esys.components.WindowBar();
                jLabel11 = new javax.swing.JLabel();
                jLabel12 = new javax.swing.JLabel();
                cbxDepartment = new javax.swing.JComboBox<>();
                cbxCurriculum = new javax.swing.JComboBox<>();
                cbxCurriculum1 = new javax.swing.JComboBox<>();
                txtLastName = new javax.swing.JTextField();
                jLabel13 = new javax.swing.JLabel();
                txtMiddleName = new javax.swing.JTextField();
                jLabel14 = new javax.swing.JLabel();

                setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

                jPanel1.setBackground(new java.awt.Color(255, 255, 255));
                jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

                jLabel1.setFont(new java.awt.Font("Century Gothic", 1, 18)); // NOI18N
                jLabel1.setText("VINCE BATECAN STATE ");
                jPanel1.add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(100, 50, -1, -1));

                jLabel2.setFont(new java.awt.Font("Century Gothic", 1, 18)); // NOI18N
                jLabel2.setText("UNIVERSITY");
                jPanel1.add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 70, -1, -1));

                jLabel3.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
                jLabel3.setForeground(new java.awt.Color(153, 153, 153));
                jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                jLabel3.setText("Enroll a new Student");
                jPanel1.add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 100, 330, -1));

                jLabel5.setFont(new java.awt.Font("Arial", 0, 10)); // NOI18N
                jLabel5.setText("First Name");
                jPanel1.add(jLabel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 130, -1, -1));

                txtFirstName.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());
                txtFirstName.addKeyListener(new java.awt.event.KeyAdapter() {
                        public void keyReleased(java.awt.event.KeyEvent evt) {
                                txtFirstNameKeyReleased(evt);
                        }
                });
                jPanel1.add(txtFirstName, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 150, 160, -1));

                jLabel6.setFont(new java.awt.Font("Arial", 0, 10)); // NOI18N
                jLabel6.setText("Email Address (For Student Portal)");
                jLabel6.setToolTipText("An email address that will be created for this student.");
                jPanel1.add(jLabel6, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 230, 330, -1));

                txtEmail.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());
                jPanel1.add(txtEmail, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 250, 330, -1));

                jLabel8.setFont(new java.awt.Font("Arial", 0, 10)); // NOI18N
                jLabel8.setText("Student Type");
                jPanel1.add(jLabel8, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 280, -1, -1));

                jLabel9.setFont(new java.awt.Font("Arial", 0, 10)); // NOI18N
                jLabel9.setText("Program/Course");
                jPanel1.add(jLabel9, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 330, -1, -1));

                btnClose.setBackground(new java.awt.Color(255, 234, 234));
                btnClose.setText("Cancel");
                btnClose.addActionListener(this::btnCloseActionPerformed);
                jPanel1.add(btnClose, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 490, 150, -1));

                btnRegister.setBackground(new java.awt.Color(255, 234, 234));
                btnRegister.setText("Register");
                btnRegister.setEnabled(false);
                btnRegister.addActionListener(this::btnRegisterActionPerformed);
                jPanel1.add(btnRegister, new org.netbeans.lib.awtextra.AbsoluteConstraints(210, 490, 150, -1));

                jComboBox1.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "REGULAR", "IRREGULAR" }));
                jPanel1.add(jComboBox1, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 300, 330, -1));

                windowBar1.setTitle("Enroll Student");
                jPanel1.add(windowBar1, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 391, -1));

                jLabel11.setFont(new java.awt.Font("Arial", 0, 10)); // NOI18N
                jLabel11.setText("Curriculum");
                jPanel1.add(jLabel11, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 380, -1, -1));

                jLabel12.setFont(new java.awt.Font("Arial", 0, 10)); // NOI18N
                jLabel12.setText("Department");
                jPanel1.add(jLabel12, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 430, -1, -1));

                cbxDepartment.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "College of Engineering" }));
                jPanel1.add(cbxDepartment, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 450, 330, -1));

                cbxCurriculum.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Information Technology" }));
                jPanel1.add(cbxCurriculum, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 350, 330, -1));

                cbxCurriculum1.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "IT2023" }));
                jPanel1.add(cbxCurriculum1, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 400, 330, -1));

                txtLastName.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());
                txtLastName.addKeyListener(new java.awt.event.KeyAdapter() {
                        public void keyReleased(java.awt.event.KeyEvent evt) {
                                txtLastNameKeyReleased(evt);
                        }
                });
                jPanel1.add(txtLastName, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 200, 330, -1));

                jLabel13.setFont(new java.awt.Font("Arial", 0, 10)); // NOI18N
                jLabel13.setText("Last Name");
                jPanel1.add(jLabel13, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 180, -1, -1));

                txtMiddleName.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());
                jPanel1.add(txtMiddleName, new org.netbeans.lib.awtextra.AbsoluteConstraints(200, 150, 160, -1));

                jLabel14.setFont(new java.awt.Font("Arial", 0, 10)); // NOI18N
                jLabel14.setText("Middle Name (Optional)");
                jPanel1.add(jLabel14, new org.netbeans.lib.awtextra.AbsoluteConstraints(200, 130, -1, -1));

                javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
                getContentPane().setLayout(layout);
                layout.setHorizontalGroup(
                        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 391, javax.swing.GroupLayout.PREFERRED_SIZE)
                );
                layout.setVerticalGroup(
                        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 540, javax.swing.GroupLayout.PREFERRED_SIZE)
                );

                pack();
        }// </editor-fold>//GEN-END:initComponents

  private void buildStudentEmail() {
    // Sa format na lastname.firstname@paul.edu.ph. Sometimes, yung first name may spaces
    String firstName = txtFirstName
      .getText()
      .trim()
      .toLowerCase()
      .replace(" ", "");
    String lastName = txtLastName.getText().trim().toLowerCase();
    String email = lastName + "." + firstName + "@paul.edu.ph";
    txtEmail.setText(email);
  }

  private void btnCloseActionPerformed(java.awt.event.ActionEvent evt) {
//GEN-FIRST:event_btnCloseActionPerformed
    this.dispose();
  }//GEN-LAST:event_btnCloseActionPerformed

  private void btnRegisterActionPerformed(java.awt.event.ActionEvent evt) {
//GEN-FIRST:event_btnRegisterActionPerformed
	  registerStudent();
  }//GEN-LAST:event_btnRegisterActionPerformed

  private void txtFirstNameKeyReleased(java.awt.event.KeyEvent evt) {
//GEN-FIRST:event_txtFirstNameKeyReleased
    buildStudentEmail();
    refreshRegisterButtonState();
  }//GEN-LAST:event_txtFirstNameKeyReleased

  private void txtLastNameKeyReleased(java.awt.event.KeyEvent evt) {
//GEN-FIRST:event_txtLastNameKeyReleased
    buildStudentEmail();
    refreshRegisterButtonState();
  }//GEN-LAST:event_txtLastNameKeyReleased

  /**
   * @param args the command line arguments
   */
  public static void main(String args[]) {
    /* Create and display the form */
    java.awt.EventQueue.invokeLater(() ->
      new StudentEnrollmentForm().setVisible(true)
    );
  }

        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JButton btnClose;
        private javax.swing.JButton btnRegister;
        private javax.swing.JComboBox<String> cbxCurriculum;
        private javax.swing.JComboBox<String> cbxCurriculum1;
        private javax.swing.JComboBox<String> cbxDepartment;
        private javax.swing.JComboBox<String> jComboBox1;
        private javax.swing.JLabel jLabel1;
        private javax.swing.JLabel jLabel11;
        private javax.swing.JLabel jLabel12;
        private javax.swing.JLabel jLabel13;
        private javax.swing.JLabel jLabel14;
        private javax.swing.JLabel jLabel2;
        private javax.swing.JLabel jLabel3;
        private javax.swing.JLabel jLabel5;
        private javax.swing.JLabel jLabel6;
        private javax.swing.JLabel jLabel8;
        private javax.swing.JLabel jLabel9;
        private javax.swing.JPanel jPanel1;
        private javax.swing.JTextField txtEmail;
        private javax.swing.JTextField txtFirstName;
        private javax.swing.JTextField txtLastName;
        private javax.swing.JTextField txtMiddleName;
        private com.group5.paul_esys.components.WindowBar windowBar1;
        // End of variables declaration//GEN-END:variables
}
