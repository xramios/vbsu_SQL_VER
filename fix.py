
import re
with open('src/main/java/com/group5/paul_esys/screens/student/StudentDashboard.java', 'r', encoding='utf-8') as f:
    c = f.read()

s = r'''                        if \(confirm != JOptionPane.YES_OPTION\) \{
                txtFirstName.setText\(student.getFirstName\(\)\);'''

rep = r'''                        if (confirm != JOptionPane.YES_OPTION) {
                                return new ArrayList<>();
                        }
                }

                return checkedRows;
        }

        private void initStudentData(Student student) {
                txtStudentID.setText(student.getStudentId());
                txtFirstName.setText(student.getFirstName());'''

c = re.sub(s, rep, c)
with open('src/main/java/com/group5/paul_esys/screens/student/StudentDashboard.java', 'w', encoding='utf-8') as f:
    f.write(c)

