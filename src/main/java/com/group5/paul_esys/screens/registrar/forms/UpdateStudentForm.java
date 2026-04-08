/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JDialog.java to edit this template
 */
package com.group5.paul_esys.screens.registrar.forms;

import com.group5.paul_esys.modules.courses.model.Course;
import com.group5.paul_esys.modules.courses.services.CourseService;
import com.group5.paul_esys.modules.students.model.Student;
import com.group5.paul_esys.modules.students.model.StudentStatus;
import com.group5.paul_esys.modules.students.services.StudentService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JOptionPane;

/**
 *
 * @author nytri
 */
public class UpdateStudentForm extends javax.swing.JDialog {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(UpdateStudentForm.class.getName());
        private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        private final StudentService studentService = StudentService.getInstance();
        private final CourseService courseService = CourseService.getInstance();
        private final Map<String, Long> courseIdByName = new LinkedHashMap<>();
        private final Student editingStudent;
        private final Runnable onSavedCallback;

    /**
     * Creates new form UpdateStudentForm
     */
    public UpdateStudentForm(java.awt.Frame parent, boolean modal) {
                this(parent, modal, null, null);
        }

        public UpdateStudentForm(
                java.awt.Frame parent,
                boolean modal,
                Student editingStudent,
                Runnable onSavedCallback
        ) {
        super(parent, modal);
                this.editingStudent = editingStudent;
                this.onSavedCallback = onSavedCallback;
                this.setUndecorated(true);
        initComponents();
                initializeForm();
        }

        private void initializeForm() {
                setLocationRelativeTo(getParent());

                jSpinner1.setModel(new javax.swing.SpinnerNumberModel(1, 1, 31, 1));
                jSpinner2.setModel(new javax.swing.SpinnerNumberModel(LocalDate.now().getYear(), 1900, LocalDate.now().getYear(), 1));

                jComboBox1.removeAllItems();
                jComboBox1.addItem(StudentStatus.REGULAR.name());
                jComboBox1.addItem(StudentStatus.IRREGULAR.name());

                jComboBox4.removeAllItems();
                for (int yearLevel = 1; yearLevel <= 6; yearLevel++) {
                        jComboBox4.addItem(String.valueOf(yearLevel));
                }

                loadCourses();

                jButton1.addActionListener(evt -> saveStudent());
                jButton2.addActionListener(evt -> dispose());

                if (editingStudent == null) {
                        return;
                }

                String middleName = editingStudent.getMiddleName() == null ? "" : editingStudent.getMiddleName().trim();
                String middleInitial = middleName.isEmpty() ? "" : " " + middleName.charAt(0) + ".";

                windowBar1.setTitle("Update Student " + editingStudent.getStudentId());
                lblStudentName.setText(
                        editingStudent.getLastName() + ", " + editingStudent.getFirstName() + middleInitial
                );

                jTextField1.setText(editingStudent.getFirstName());
                jTextField2.setText(editingStudent.getMiddleName() == null ? "" : editingStudent.getMiddleName());
                jTextField3.setText(editingStudent.getLastName());

                if (editingStudent.getBirthdate() != null) {
                        LocalDate birthDate = toLocalDate(editingStudent.getBirthdate());

                        jTextField4.setText(DATE_FORMATTER.format(birthDate));
                        jSpinner1.setValue(birthDate.getDayOfMonth());
                        jComboBox2.setSelectedIndex(birthDate.getMonthValue() - 1);
                        jSpinner2.setValue(birthDate.getYear());
                }

                if (editingStudent.getStudentStatus() != null) {
                        jComboBox1.setSelectedItem(editingStudent.getStudentStatus().name());
                }

                if (editingStudent.getCourseId() != null) {
                        courseService
                                .getCourseById(editingStudent.getCourseId())
                                .ifPresent(course -> jComboBox3.setSelectedItem(course.getCourseName()));
                }

                if (editingStudent.getYearLevel() != null) {
                        jComboBox4.setSelectedItem(String.valueOf(editingStudent.getYearLevel()));
                }
        }

        private LocalDate toLocalDate(Date date) {
                if (date instanceof java.sql.Date sqlDate) {
                        return sqlDate.toLocalDate();
                }

                return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }

        private void loadCourses() {
                jComboBox3.removeAllItems();
                courseIdByName.clear();

                for (Course course : courseService.getAllCourses()) {
                        jComboBox3.addItem(course.getCourseName());
                        courseIdByName.put(course.getCourseName(), course.getId());
                }
        }

        private LocalDate resolveBirthDate() {
                String birthDateText = jTextField4.getText() == null ? "" : jTextField4.getText().trim();
                if (!birthDateText.isEmpty()) {
                        return LocalDate.parse(birthDateText, DATE_FORMATTER);
                }

                int day = (Integer) jSpinner1.getValue();
                int month = jComboBox2.getSelectedIndex() + 1;
                int year = (Integer) jSpinner2.getValue();
                return LocalDate.of(year, month, day);
        }

        private void saveStudent() {
                if (editingStudent == null) {
                        JOptionPane.showMessageDialog(
                                this,
                                "No student selected for update.",
                                "Update Failed",
                                JOptionPane.ERROR_MESSAGE
                        );
                        return;
                }

                String firstName = jTextField1.getText() == null ? "" : jTextField1.getText().trim();
                String middleName = jTextField2.getText() == null ? "" : jTextField2.getText().trim();
                String lastName = jTextField3.getText() == null ? "" : jTextField3.getText().trim();

                if (firstName.isEmpty() || lastName.isEmpty()) {
                        JOptionPane.showMessageDialog(
                                this,
                                "First name and last name are required.",
                                "Validation Error",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return;
                }

                Object selectedCourse = jComboBox3.getSelectedItem();
                if (selectedCourse == null || !courseIdByName.containsKey(selectedCourse.toString())) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Please select a course/program.",
                                "Validation Error",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return;
                }

                LocalDate birthDate;
                try {
                        birthDate = resolveBirthDate();
                } catch (DateTimeParseException ex) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Birth date must follow YYYY-MM-DD format.",
                                "Validation Error",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return;
                } catch (RuntimeException ex) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Birth date is invalid.",
                                "Validation Error",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return;
                }

                editingStudent
                        .setFirstName(firstName)
                        .setMiddleName(middleName.isEmpty() ? null : middleName)
                        .setLastName(lastName)
                        .setBirthdate(java.sql.Date.valueOf(birthDate))
                        .setStudentStatus(StudentStatus.valueOf(jComboBox1.getSelectedItem().toString()))
                        .setCourseId(courseIdByName.get(selectedCourse.toString()))
                        .setYearLevel(Long.parseLong(jComboBox4.getSelectedItem().toString()));

                boolean success = studentService.update(editingStudent).isPresent();
                if (!success) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Failed to update student. Please try again.",
                                "Update Failed",
                                JOptionPane.ERROR_MESSAGE
                        );
                        return;
                }

                JOptionPane.showMessageDialog(
                        this,
                        "Student updated successfully.",
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
                windowBar1 = new com.group5.paul_esys.components.WindowBar();
                jLabel1 = new javax.swing.JLabel();
                lblStudentName = new javax.swing.JLabel();
                jLabel3 = new javax.swing.JLabel();
                jTextField1 = new javax.swing.JTextField();
                jTextField2 = new javax.swing.JTextField();
                jLabel4 = new javax.swing.JLabel();
                jTextField3 = new javax.swing.JTextField();
                jLabel5 = new javax.swing.JLabel();
                jTextField4 = new javax.swing.JTextField();
                jLabel6 = new javax.swing.JLabel();
                jLabel7 = new javax.swing.JLabel();
                jComboBox2 = new javax.swing.JComboBox<>();
                jSpinner1 = new javax.swing.JSpinner();
                jSpinner2 = new javax.swing.JSpinner();
                jLabel8 = new javax.swing.JLabel();
                jComboBox1 = new javax.swing.JComboBox<>();
                jLabel9 = new javax.swing.JLabel();
                jComboBox3 = new javax.swing.JComboBox<>();
                jLabel10 = new javax.swing.JLabel();
                jComboBox4 = new javax.swing.JComboBox<>();
                jButton1 = new javax.swing.JButton();
                jButton2 = new javax.swing.JButton();

                setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

                jPanel1.setBackground(new java.awt.Color(255, 255, 255));
                jPanel1.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                jPanel1.setPreferredSize(new java.awt.Dimension(430, 700));

                windowBar1.setTitle("Update Student <Name>");

                jLabel1.setFont(new java.awt.Font("Poppins", 0, 18)); // NOI18N
                jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                jLabel1.setText("Update Student");

                lblStudentName.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
                lblStudentName.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                lblStudentName.setText("LN, FN M.O");

                jLabel3.setText("First Name");

                jTextField1.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                jTextField1.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());

                jTextField2.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                jTextField2.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());

                jLabel4.setText("Middle Name");

                jTextField3.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                jTextField3.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());

                jLabel5.setText("Last Name");

                jTextField4.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                jTextField4.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());

                jLabel6.setText("Birth Date");

                jLabel7.setText("Birth Date");

                jComboBox2.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December" }));

                jLabel8.setText("Student Status");

                jComboBox1.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Select Student Status", "REGULAR", "IRREGULAR" }));

                jLabel9.setText("Course / Program");

                jComboBox3.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Set Student Course / Program", "REGULAR", "IRREGULAR" }));

                jLabel10.setText("Year Level");

                jComboBox4.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Set Student Year Level", "1st Year", "2nd Year", "3rd Year", "4th Year", "5th Year", "6th Year" }));

                jButton1.setBackground(new java.awt.Color(255, 234, 234));
                jButton1.setText("Update");

                jButton2.setBackground(new java.awt.Color(119, 0, 0));
                jButton2.setForeground(new java.awt.Color(255, 255, 255));
                jButton2.setText("Cancel");

                javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
                jPanel1.setLayout(jPanel1Layout);
                jPanel1Layout.setHorizontalGroup(
                        jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(windowBar1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, Short.MAX_VALUE))
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 115, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 115, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                                .addGap(48, 48, 48)
                                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addComponent(lblStudentName, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addComponent(jTextField1, javax.swing.GroupLayout.Alignment.TRAILING)
                                                        .addComponent(jTextField2)
                                                        .addComponent(jTextField3)
                                                        .addComponent(jTextField4)
                                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                                                .addGap(6, 6, 6)
                                                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                                                                .addComponent(jSpinner1, javax.swing.GroupLayout.PREFERRED_SIZE, 111, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                                .addComponent(jComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, 116, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                                .addComponent(jSpinner2))
                                                                        .addComponent(jComboBox1, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                                        .addComponent(jComboBox3, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                                        .addComponent(jComboBox4, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                                                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                                                        .addComponent(jLabel8)
                                                                                        .addComponent(jLabel3)
                                                                                        .addComponent(jLabel4)
                                                                                        .addComponent(jLabel5)
                                                                                        .addComponent(jLabel6)
                                                                                        .addComponent(jLabel7)
                                                                                        .addComponent(jLabel9)
                                                                                        .addComponent(jLabel10))
                                                                                .addGap(0, 0, Short.MAX_VALUE)))))))
                                .addGap(48, 48, 48))
                );
                jPanel1Layout.setVerticalGroup(
                        jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(windowBar1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(lblStudentName)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel3)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel4)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel5)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextField3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel6)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextField4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel7)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jSpinner1, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jSpinner2, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel8)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel9)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jComboBox3, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel10)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jComboBox4, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(0, 30, Short.MAX_VALUE))
                );

                getContentPane().add(jPanel1, java.awt.BorderLayout.CENTER);

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
                UpdateStudentForm dialog = new UpdateStudentForm(new javax.swing.JFrame(), true);
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
        private javax.swing.JComboBox<String> jComboBox3;
        private javax.swing.JComboBox<String> jComboBox4;
        private javax.swing.JLabel jLabel1;
        private javax.swing.JLabel jLabel10;
        private javax.swing.JLabel jLabel3;
        private javax.swing.JLabel jLabel4;
        private javax.swing.JLabel jLabel5;
        private javax.swing.JLabel jLabel6;
        private javax.swing.JLabel jLabel7;
        private javax.swing.JLabel jLabel8;
        private javax.swing.JLabel jLabel9;
        private javax.swing.JPanel jPanel1;
        private javax.swing.JSpinner jSpinner1;
        private javax.swing.JSpinner jSpinner2;
        private javax.swing.JTextField jTextField1;
        private javax.swing.JTextField jTextField2;
        private javax.swing.JTextField jTextField3;
        private javax.swing.JTextField jTextField4;
        private javax.swing.JLabel lblStudentName;
        private com.group5.paul_esys.components.WindowBar windowBar1;
        // End of variables declaration//GEN-END:variables
}
