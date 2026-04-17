import re

with open('src/main/java/com/group5/paul_esys/screens/student/StudentDashboard.java', 'r', encoding='utf-8') as f:
    text = f.read()

replacement = """private List<Integer> getCheckedCatalogModelRows(DefaultTableModel catalogModel) {
		List<Integer> checkedRows = new ArrayList<>();
		Set<Long> subjectIds = new HashSet<>();
		List<Integer> duplicateRows = new ArrayList<>();

		for (int modelRow = 0; modelRow < catalogModel.getRowCount(); modelRow++) {
			if (isCatalogRowChecked(catalogModel, modelRow)) {
				Long subjectId = parseLongCell(catalogModel.getValueAt(modelRow, CATALOG_COL_SUBJECT_ID));
				if (subjectId != null && !subjectIds.add(subjectId)) {
					duplicateRows.add(modelRow);
				}
				checkedRows.add(modelRow);
			}
		}

		if (!duplicateRows.isEmpty()) {
			int confirm = JOptionPane.showConfirmDialog(
					this,
					"Multiple sections for the same subject were detected. "
							+ "These duplicates will be submitted for the Registrar to review. "
							+ "Continue?",
					"Duplicate Subject Added",
					JOptionPane.YES_NO_OPTION,
					JOptionPane.WARNING_MESSAGE);

			if (confirm != JOptionPane.YES_OPTION) {
				return new ArrayList<>();
			}
		}

		return checkedRows;
	}
"""

pattern = r'private List<Integer> getCheckedCatalogModelRows\([^)]+\)\s*\{.*?return checkedRows;\s*\}'
text = re.sub(pattern, replacement, text, count=1, flags=re.DOTALL)

with open('src/main/java/com/group5/paul_esys/screens/student/StudentDashboard.java', 'w', encoding='utf-8') as f:
    f.write(text)