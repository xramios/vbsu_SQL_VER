import re

with open("src/main/java/com/group5/paul_esys/screens/student/StudentDashboard.java", "r") as f:
    content = f.read()

old_block = r"List<Schedule> schedules = ScheduleService.getInstance\(\).getSchedulesByOffering\(offeringIdStr\);\s*StringBuilder facultyString = new StringBuilder\(\);\s*for \(Schedule sched : schedules\) \{\s*com.group5.paul_esys.modules.faculty.services.FacultyService.getInstance\(\).getFacultyById\(sched.getFacultyId\(\)\)\s*.ifPresent\(f -> facultyString.append\(f.getLastName\(\)\).append\(\x22, \x22\).append\(f.getFirstName\(\)\).append\(\x22 \x22\)\);\s*\}\s*String facultyValue = facultyString.length\(\) == 0 \? \x22TBA\x22 : facultyString.toString\(\).trim\(\);\s*selectedSubjectsTableModel.addRow\(new Object\[\] \{\s*true,\s*code,\s*subjectName,\s*units,\s*schedule,\s*section,\s*facultyValue,\s*offeringIdStr\s*\}\);\s*totalUnits \+= units;"

new_block = """List<Schedule> schedules = ScheduleService.getInstance().getSchedulesByOffering(offeringIdStr);
                        StringBuilder facultyString = new StringBuilder();
                        StringBuilder roomString = new StringBuilder();
                        for (Schedule sched : schedules) {
                                com.group5.paul_esys.modules.faculty.services.FacultyService.getInstance().getFacultyById(sched.getFacultyId())
                                                .ifPresent(f -> facultyString.append(f.getLastName()).append(", ").append(f.getFirstName()).append(" "));
                                com.group5.paul_esys.modules.rooms.services.RoomService.getInstance().getRoomById(sched.getRoomId())
                                                .ifPresent(r -> roomString.append(r.getRoom()).append(" "));
                        }
                        String facultyValue = facultyString.length() == 0 ? "TBA" : facultyString.toString().trim();
                        String roomValue = roomString.length() == 0 ? "TBA" : roomString.toString().trim();

                        selectedSubjectsTableModel.addRow(new Object[] {
                                true,
                                code,
                                subjectName,
                                section,
                                facultyValue,
                                schedule,
                                roomValue,
                                formatUnits(units),
                                offeringIdStr
                        });
                        totalUnits += units;"""

content = re.sub(old_block, new_block, content, count=1)

with open("src/main/java/com/group5/paul_esys/screens/student/StudentDashboard.java", "w") as f:
    f.write(content)
