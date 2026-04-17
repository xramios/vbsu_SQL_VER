    private javax.swing.JPanel panelDuplicates;
    private javax.swing.JLabel lblDuplicateCount;
    private javax.swing.JTable tableDuplicates;
    private javax.swing.JButton btnDeleteDuplicate;

    private void injectDuplicateSubjectsPanel() {
        panelDuplicates = new javax.swing.JPanel(new java.awt.BorderLayout(5, 5));
        panelDuplicates.setBorder(javax.swing.BorderFactory.createTitledBorder("Duplicate Subjects"));
        
        lblDuplicateCount = new javax.swing.JLabel("Duplicates Found: 0");
        panelDuplicates.add(lblDuplicateCount, java.awt.BorderLayout.NORTH);
        
        tableDuplicates = new javax.swing.JTable();
        tableDuplicates.setModel(new javax.swing.table.DefaultTableModel(
            new Object[][]{},
            new String[]{"Code", "Section", "Instructor", "Source", "Detail ID"}
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        panelDuplicates.add(new javax.swing.JScrollPane(tableDuplicates), java.awt.BorderLayout.CENTER);
        
        btnDeleteDuplicate = new javax.swing.JButton("Delete Selected Duplicate");
        btnDeleteDuplicate.addActionListener(e -> deleteSelectedDuplicate());
        panelDuplicates.add(btnDeleteDuplicate, java.awt.BorderLayout.SOUTH);
        
        java.awt.Container parent = jScrollPane2.getParent();
        if (parent != null && parent.getLayout() instanceof javax.swing.GroupLayout gl) {
            javax.swing.JSplitPane splitPane = new javax.swing.JSplitPane(javax.swing.JSplitPane.VERTICAL_SPLIT, jScrollPane2, panelDuplicates);
            splitPane.setResizeWeight(0.6);
            gl.replace(jScrollPane2, splitPane);
        }
    }

    private void deleteSelectedDuplicate() {
        int row = tableDuplicates.getSelectedRow();
        if (row < 0) {
            javax.swing.JOptionPane.showMessageLabel(this, "Select a duplicate to delete.");
            return;
        }
        Long detailId = (Long) tableDuplicates.getModel().getValueAt(row, 4);
        try {
            com.group5.paul_esys.modules.enrollments.services.EnrollmentDetailService.getInstance().deleteEnrollmentDetail(detailId);
            javax.swing.JOptionPane.showMessageDialog(this, "Duplicate deleted.");
            loadStudentSchedules();
            loadDuplicateSubjects();
        } catch (Exception e) {}
    }

    private void loadDuplicateSubjects() {
        if (student == null) return;
        java.util.List<com.group5.paul_esys.modules.registrar.model.DuplicateSubjectDTO> duplicates = 
            registrarStudentScheduleService.getDuplicateSubjects(student.getStudentId(), enrollmentId);
        
        lblDuplicateCount.setText("Duplicates Found: " + duplicates.size());
        
        javax.swing.table.DefaultTableModel model = (javax.swing.table.DefaultTableModel) tableDuplicates.getModel();
        model.setRowCount(0);
        for (var d : duplicates) {
            model.addRow(new Object[]{ d.subjectCode(), d.section(), d.instructor(), d.source(), d.enrollmentDetailId() });
        }
    }

