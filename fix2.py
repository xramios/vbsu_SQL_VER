
with open('src/main/java/com/group5/paul_esys/screens/student/StudentDashboard.java', 'r', encoding='utf-8') as f:
    c = f.read()

s = '''                        if (confirm != JOptionPane.YES_OPTION) {
                txtFirstName.setText(student.getFirstName());'''

rep = '''                        if (confirm != JOptionPane.YES_OPTION) {
                                return new ArrayList<>();
                        }
                }

                return checkedRows;
        }

        private void initStudentData(Student student) {
                txtStudentID.setText(student.getStudentId());
                txtFirstName.setText(student.getFirstName());'''
import re
# normalize whitespace/newlines
def normalize(text):
    return re.sub(r'\s+', '', text)

# Find the location of the block
idx = c.find('JOptionPane.YES_OPTION) {')
idx2 = c.find('txtFirstName.setText(student.getFirstName());')

if idx != -1 and idx2 != -1:
    c = c[:idx] + 'JOptionPane.YES_OPTION) {\n\t\t\t\treturn new java.util.ArrayList<>();\n\t\t\t}\n\t\t}\n\n\t\treturn checkedRows;\n\t}\n\n\tprivate void initStudentData(Student student) {\n\t\t' + c[idx2:]
    with open('src/main/java/com/group5/paul_esys/screens/student/StudentDashboard.java', 'w', encoding='utf-8') as f:
        f.write(c)
    print('done')

