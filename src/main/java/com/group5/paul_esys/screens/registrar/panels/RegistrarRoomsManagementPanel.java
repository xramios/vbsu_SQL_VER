/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package com.group5.paul_esys.screens.registrar.panels;

import com.group5.paul_esys.modules.rooms.model.Room;
import com.group5.paul_esys.modules.rooms.services.RoomService;
import com.group5.paul_esys.screens.registrar.forms.RoomForm;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author nytri
 */
public class RegistrarRoomsManagementPanel extends javax.swing.JPanel {

        private static final String FILTER_ALL = "ALL";
        private static final String DEFAULT_ROOM_TYPE = "OTHER";
        private static final String DEFAULT_ROOM_STATUS = "AVAILABLE";

        private final RoomService roomService = RoomService.getInstance();

        private List<Room> rooms = new ArrayList<>();
        private List<Room> filteredRooms = new ArrayList<>();

	/**
	 * Creates new form RegistrarRoomsManagementPanel
	 */
	public RegistrarRoomsManagementPanel() {
		initComponents();
		initializeRoomPanel();
	}

        private void initializeRoomPanel() {
                menuItemUpdate.setText("Update Room");
                menuItemDelete.setText("Delete Room");

                tableSubjects.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
                tableSubjects.setComponentPopupMenu(popMenu);
                tableSubjects.setRowHeight(28);

                registerTablePopupSelectionBehavior();
                resetFilterModels();
                initializeRooms();
        }

        private void registerTablePopupSelectionBehavior() {
                tableSubjects.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mousePressed(MouseEvent evt) {
                                selectRowFromPointer(evt);
                        }

                        @Override
                        public void mouseReleased(MouseEvent evt) {
                                selectRowFromPointer(evt);
                        }

                        @Override
                        public void mouseClicked(MouseEvent evt) {
                                if (javax.swing.SwingUtilities.isLeftMouseButton(evt) && evt.getClickCount() == 2) {
                                        openUpdateRoomForm();
                                }
                        }
                });
        }

        private void selectRowFromPointer(MouseEvent evt) {
                if (!evt.isPopupTrigger()) {
                        return;
                }

                int row = tableSubjects.rowAtPoint(evt.getPoint());
                if (row >= 0) {
                        tableSubjects.setRowSelectionInterval(row, row);
                }
        }

        private void resetFilterModels() {
                cbxType.removeAllItems();
                cbxType.addItem(FILTER_ALL);
                cbxType.addItem("LECTURE");
                cbxType.addItem("LAB");
                cbxType.addItem("SEMINAR");
                cbxType.addItem("AUDITORIUM");
                cbxType.addItem("OTHER");

                cbxStatus.removeAllItems();
                cbxStatus.addItem(FILTER_ALL);
                cbxStatus.addItem("AVAILABLE");
                cbxStatus.addItem("UNAVAILABLE");
                cbxStatus.addItem("MAINTENANCE");
        }

        private void initializeRooms() {
                new SwingWorker<List<Room>, Void>() {
                        @Override
                        protected List<Room> doInBackground() throws Exception {
                                return roomService.getAllRooms();
                        }

                        @Override
                        protected void done() {
                                try {
                                        rooms = get();
                                        reloadFilterOptions();
                                        applyFilters();
                                } catch (Exception ex) {
                                        JOptionPane.showMessageDialog(
                                                RegistrarRoomsManagementPanel.this,
                                                "Error loading rooms: " + ex.getMessage(),
                                                "Rooms Management",
                                                JOptionPane.ERROR_MESSAGE
                                        );
                                }
                        }
                }.execute();
        }

        private void reloadFilterOptions() {
                String selectedType = cbxType.getSelectedItem() == null
                        ? FILTER_ALL
                        : cbxType.getSelectedItem().toString();
                String selectedStatus = cbxStatus.getSelectedItem() == null
                        ? FILTER_ALL
                        : cbxStatus.getSelectedItem().toString();

                List<String> typeOptions = new ArrayList<>(List.of("LECTURE", "LAB", "SEMINAR", "AUDITORIUM", "OTHER"));
                List<String> statusOptions = new ArrayList<>(List.of("AVAILABLE", "UNAVAILABLE", "MAINTENANCE"));

                for (Room room : rooms) {
                        String type = normalizeRoomType(room.getRoomType());
                        String status = normalizeRoomStatus(room.getStatus());

                        if (!typeOptions.contains(type)) {
                                typeOptions.add(type);
                        }

                        if (!statusOptions.contains(status)) {
                                statusOptions.add(status);
                        }
                }

                cbxType.removeAllItems();
                cbxType.addItem(FILTER_ALL);
                typeOptions.forEach(cbxType::addItem);

                cbxStatus.removeAllItems();
                cbxStatus.addItem(FILTER_ALL);
                statusOptions.forEach(cbxStatus::addItem);

                cbxType.setSelectedItem(typeOptions.contains(selectedType) ? selectedType : FILTER_ALL);
                cbxStatus.setSelectedItem(statusOptions.contains(selectedStatus) ? selectedStatus : FILTER_ALL);
        }

        private void applyFilters() {
                String searchTerm = txtSearch.getText() == null
                        ? ""
                        : txtSearch.getText().trim().toLowerCase();

                String selectedType = cbxType.getSelectedItem() == null
                        ? FILTER_ALL
                        : cbxType.getSelectedItem().toString();

                String selectedStatus = cbxStatus.getSelectedItem() == null
                        ? FILTER_ALL
                        : cbxStatus.getSelectedItem().toString();

                filteredRooms = rooms
                        .stream()
                        .filter(room -> matchesSearch(room, searchTerm))
                        .filter(room -> matchesType(room, selectedType))
                        .filter(room -> matchesStatus(room, selectedStatus))
                        .collect(Collectors.toList());

                populateTable(filteredRooms);
        }

        private boolean matchesSearch(Room room, String searchTerm) {
                if (searchTerm.isEmpty()) {
                        return true;
                }

                String roomName = safeText(room.getRoom(), "").toLowerCase();
                String building = safeText(room.getBuilding(), "").toLowerCase();
                String type = normalizeRoomType(room.getRoomType()).toLowerCase();
                String status = normalizeRoomStatus(room.getStatus()).toLowerCase();
                String capacity = room.getCapacity() == null ? "" : String.valueOf(room.getCapacity());

                return roomName.contains(searchTerm)
                        || building.contains(searchTerm)
                        || type.contains(searchTerm)
                        || status.contains(searchTerm)
                        || capacity.contains(searchTerm);
        }

        private boolean matchesType(Room room, String selectedType) {
                if (FILTER_ALL.equals(selectedType)) {
                        return true;
                }

                return normalizeRoomType(room.getRoomType()).equals(selectedType);
        }

        private boolean matchesStatus(Room room, String selectedStatus) {
                if (FILTER_ALL.equals(selectedStatus)) {
                        return true;
                }

                return normalizeRoomStatus(room.getStatus()).equals(selectedStatus);
        }

        private void populateTable(List<Room> roomsToDisplay) {
                DefaultTableModel model = (DefaultTableModel) tableSubjects.getModel();
                model.setRowCount(0);

                for (Room room : roomsToDisplay) {
                        model.addRow(new Object[]{
                                safeText(room.getRoom(), "N/A"),
                                safeText(room.getBuilding(), "N/A"),
                                normalizeRoomType(room.getRoomType()),
                                room.getCapacity(),
                                normalizeRoomStatus(room.getStatus())
                        });
                }
        }

        private Room getSelectedRoom() {
                int selectedRow = tableSubjects.getSelectedRow();
                if (selectedRow < 0) {
                        return null;
                }

                int modelRow = tableSubjects.convertRowIndexToModel(selectedRow);
                if (modelRow < 0 || modelRow >= filteredRooms.size()) {
                        return null;
                }

                return filteredRooms.get(modelRow);
        }

        private void openUpdateRoomForm() {
                Room selectedRoom = getSelectedRoom();
                if (selectedRoom == null) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Please select a room to update.",
                                "Update Room",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return;
                }

                RoomForm form = new RoomForm(selectedRoom, this::initializeRooms);
                form.setVisible(true);
        }

        private void deleteSelectedRoom() {
                Room selectedRoom = getSelectedRoom();
                if (selectedRoom == null) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Please select a room to delete.",
                                "Delete Room",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return;
                }

                int option = JOptionPane.showConfirmDialog(
                        this,
                        "Delete room " + safeText(selectedRoom.getRoom(), "") + "?",
                        "Confirm Delete",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );

                if (option != JOptionPane.YES_OPTION) {
                        return;
                }

                new SwingWorker<Boolean, Void>() {
                        @Override
                        protected Boolean doInBackground() throws Exception {
                                return roomService.deleteRoom(selectedRoom.getId());
                        }

                        @Override
                        protected void done() {
                                try {
                                        boolean deleted = get();
                                        if (!deleted) {
                                                JOptionPane.showMessageDialog(
                                                        RegistrarRoomsManagementPanel.this,
                                                        "Failed to delete room. It may be referenced by schedule records.",
                                                        "Delete Room",
                                                        JOptionPane.ERROR_MESSAGE
                                                );
                                                return;
                                        }
                                        initializeRooms();
                                        JOptionPane.showMessageDialog(
                                                RegistrarRoomsManagementPanel.this,
                                                "Room deleted successfully.",
                                                "Delete Room",
                                                JOptionPane.INFORMATION_MESSAGE
                                        );
                                } catch (Exception ex) {
                                        JOptionPane.showMessageDialog(
                                                RegistrarRoomsManagementPanel.this,
                                                "Error deleting room: " + ex.getMessage(),
                                                "Delete Room",
                                                JOptionPane.ERROR_MESSAGE
                                        );
                                }
                        }
                }.execute();
        }

        private String normalizeRoomType(String roomType) {
                if (roomType == null || roomType.trim().isEmpty()) {
                        return DEFAULT_ROOM_TYPE;
                }

                return roomType.trim().toUpperCase();
        }

        private String normalizeRoomStatus(String status) {
                if (status == null || status.trim().isEmpty()) {
                        return DEFAULT_ROOM_STATUS;
                }

                return status.trim().toUpperCase();
        }

        private String safeText(String value, String fallback) {
                if (value == null || value.trim().isEmpty()) {
                        return fallback;
                }

                return value.trim();
        }

	/**
	 * This method is called from within the constructor to initialize the
	 * form. WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
        // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
        private void initComponents() {

                popMenu = new javax.swing.JPopupMenu();
                menuItemUpdate = new javax.swing.JMenuItem();
                menuItemDelete = new javax.swing.JMenuItem();
                jLabel2 = new javax.swing.JLabel();
                jLabel1 = new javax.swing.JLabel();
                jPanel1 = new javax.swing.JPanel();
                jScrollPane1 = new javax.swing.JScrollPane();
                tableSubjects = new javax.swing.JTable();
                jLabel3 = new javax.swing.JLabel();
                txtSearch = new javax.swing.JTextField();
                jLabel4 = new javax.swing.JLabel();
                cbxType = new javax.swing.JComboBox<>();
                cbxStatus = new javax.swing.JComboBox<>();
                jLabel5 = new javax.swing.JLabel();
                btnClearFilter = new javax.swing.JButton();
                btnAddRoom = new javax.swing.JButton();

                menuItemUpdate.setText("jMenuItem1");
                menuItemUpdate.addActionListener(this::menuItemUpdateActionPerformed);
                popMenu.add(menuItemUpdate);

                menuItemDelete.setText("jMenuItem2");
                menuItemDelete.addActionListener(this::menuItemDeleteActionPerformed);
                popMenu.add(menuItemDelete);

                setBackground(new java.awt.Color(255, 255, 255));
                setPreferredSize(new java.awt.Dimension(1181, 684));

                jLabel2.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                jLabel2.setForeground(new java.awt.Color(153, 153, 153));
                jLabel2.setText("Manage the rooms of the University");

                jLabel1.setFont(new java.awt.Font("Poppins", 0, 24)); // NOI18N
                jLabel1.setText("Rooms Management");

                jPanel1.setBackground(new java.awt.Color(255, 255, 255));
                jPanel1.setBorder(new com.group5.paul_esys.ui.PanelRoundBorder());

                tableSubjects.setModel(new javax.swing.table.DefaultTableModel(
                        new Object [][] {

                        },
                        new String [] {
                                "Room Name", "Building", "Type", "Capacity", "Status"
                        }
                ) {
                        Class[] types = new Class [] {
                                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Integer.class, java.lang.String.class
                        };
                        boolean[] canEdit = new boolean [] {
                                false, false, false, false, false
                        };

                        public Class getColumnClass(int columnIndex) {
                                return types [columnIndex];
                        }

                        public boolean isCellEditable(int rowIndex, int columnIndex) {
                                return canEdit [columnIndex];
                        }
                });
                jScrollPane1.setViewportView(tableSubjects);

                jLabel3.setText("Search");

                txtSearch.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());
                txtSearch.addKeyListener(new java.awt.event.KeyAdapter() {
                        public void keyReleased(java.awt.event.KeyEvent evt) {
                                txtSearchKeyReleased(evt);
                        }
                });

                jLabel4.setText("Type");

                cbxType.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "LECTURE", "LAB", "SEMINAR", "AUDITORIUM", "OTHER" }));
                cbxType.addItemListener(this::cbxTypeItemStateChanged);

                cbxStatus.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "AVAILABLE", "UNAVAILABLE", "MAINTENANCE" }));
                cbxStatus.addItemListener(this::cbxStatusItemStateChanged);

                jLabel5.setText("Status");

                btnClearFilter.setBackground(new java.awt.Color(119, 0, 0));
                btnClearFilter.setForeground(new java.awt.Color(255, 255, 255));
                btnClearFilter.setText("Clear Filter");
                btnClearFilter.addActionListener(this::btnClearFilterActionPerformed);

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
                                                .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 500, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jLabel4)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(cbxType, 0, 212, Short.MAX_VALUE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jLabel5)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(cbxStatus, javax.swing.GroupLayout.PREFERRED_SIZE, 175, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(btnClearFilter, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addContainerGap())
                );
                jPanel1Layout.setVerticalGroup(
                        jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                .addComponent(jLabel3)
                                                .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addComponent(jLabel4)
                                                .addComponent(cbxType, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addComponent(cbxStatus, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addComponent(jLabel5))
                                        .addComponent(btnClearFilter, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 537, Short.MAX_VALUE)
                                .addContainerGap())
                );

                btnAddRoom.setBackground(new java.awt.Color(119, 0, 0));
                btnAddRoom.setForeground(new java.awt.Color(255, 255, 255));
                btnAddRoom.setText("Add Room");
                btnAddRoom.addActionListener(this::btnAddRoomActionPerformed);

                javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
                this.setLayout(layout);
                layout.setHorizontalGroup(
                        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addContainerGap())
                                        .addGroup(layout.createSequentialGroup()
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(jLabel2)
                                                        .addComponent(jLabel1))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(btnAddRoom, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addGap(18, 18, 18))))
                );
                layout.setVerticalGroup(
                        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(layout.createSequentialGroup()
                                                .addContainerGap()
                                                .addComponent(jLabel1)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jLabel2))
                                        .addGroup(layout.createSequentialGroup()
                                                .addGap(19, 19, 19)
                                                .addComponent(btnAddRoom, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addContainerGap())
                );
        }// </editor-fold>//GEN-END:initComponents

        private void txtSearchKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtSearchKeyReleased
                applyFilters();
        }//GEN-LAST:event_txtSearchKeyReleased

        private void cbxTypeItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_cbxTypeItemStateChanged
                if (evt.getStateChange() == ItemEvent.SELECTED) {
                        applyFilters();
                }
        }//GEN-LAST:event_cbxTypeItemStateChanged

        private void cbxStatusItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_cbxStatusItemStateChanged
                if (evt.getStateChange() == ItemEvent.SELECTED) {
                        applyFilters();
                }
        }//GEN-LAST:event_cbxStatusItemStateChanged

        private void btnClearFilterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnClearFilterActionPerformed
                txtSearch.setText("");

                if (cbxType.getItemCount() > 0) {
                        cbxType.setSelectedItem(FILTER_ALL);
                }

                if (cbxStatus.getItemCount() > 0) {
                        cbxStatus.setSelectedItem(FILTER_ALL);
                }

                applyFilters();
        }//GEN-LAST:event_btnClearFilterActionPerformed

        private void menuItemUpdateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuItemUpdateActionPerformed
                openUpdateRoomForm();
        }//GEN-LAST:event_menuItemUpdateActionPerformed

        private void menuItemDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuItemDeleteActionPerformed
                deleteSelectedRoom();
        }//GEN-LAST:event_menuItemDeleteActionPerformed

        private void btnAddRoomActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddRoomActionPerformed
		RoomForm form = new RoomForm(null, this::initializeRooms);
		form.setVisible(true);
        }//GEN-LAST:event_btnAddRoomActionPerformed


        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JButton btnAddRoom;
        private javax.swing.JButton btnClearFilter;
        private javax.swing.JComboBox<String> cbxStatus;
        private javax.swing.JComboBox<String> cbxType;
        private javax.swing.JLabel jLabel1;
        private javax.swing.JLabel jLabel2;
        private javax.swing.JLabel jLabel3;
        private javax.swing.JLabel jLabel4;
        private javax.swing.JLabel jLabel5;
        private javax.swing.JPanel jPanel1;
        private javax.swing.JScrollPane jScrollPane1;
        private javax.swing.JMenuItem menuItemDelete;
        private javax.swing.JMenuItem menuItemUpdate;
        private javax.swing.JPopupMenu popMenu;
        private javax.swing.JTable tableSubjects;
        private javax.swing.JTextField txtSearch;
        // End of variables declaration//GEN-END:variables
}
