import re

with open("src/main/java/com/group5/paul_esys/screens/student/StudentDashboard.java", "r") as f:
    content = f.read()

old_block = r"tblSelectedSubjects = new javax.swing.JTable\(selectedSubjectsTableModel\);\s*tblSelectedSubjects.setRowHeight\(26\);\s*tblSelectedSubjects.setSelectionMode\(ListSelectionModel.SINGLE_SELECTION\);"
new_block = """tblSelectedSubjects = new javax.swing.JTable(selectedSubjectsTableModel);
                tblSelectedSubjects.setRowHeight(26);
                tblSelectedSubjects.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                tblSelectedSubjects.getTableHeader().setReorderingAllowed(false);
                tblSelectedSubjects.getTableHeader().setResizingAllowed(false);"""

content = re.sub(old_block, new_block, content, count=1)

with open("src/main/java/com/group5/paul_esys/screens/student/StudentDashboard.java", "w") as f:
    f.write(content)
