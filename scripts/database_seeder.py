#!/usr/bin/env python3
"""
Comprehensive Database Seeder for University Enrollment System
Generates realistic fake data for all tables in the enrollment system
"""

import random
from datetime import datetime, timedelta
from faker import Faker
import mysql.connector
from mysql.connector import Error
import argparse
import sys

# Initialize Faker with Philippine locale for more realistic local data
fake = Faker('en_PH')

class UniversityDatabaseSeeder:
    def __init__(self, host='localhost', database='university_db', user='root', password=''):
        self.host = host
        self.database = database
        self.user = user
        self.password = password
        self.connection = None
        
        # Storage for generated IDs to maintain referential integrity
        self.departments = []
        self.courses = []
        self.rooms = []
        self.users = []
        self.faculty = []
        self.students = []
        self.subjects = []
        self.sections = []
        self.curriculums = []
        self.enrollment_periods = []
        
    def connect(self):
        """Establish database connection"""
        try:
            self.connection = mysql.connector.connect(
                host=self.host,
                database=self.database,
                user=self.user,
                password=self.password
            )
            if self.connection.is_connected():
                print(f"Connected to MySQL database '{self.database}'")
                return True
        except Error as e:
            print(f"Error connecting to MySQL: {e}")
            return False
    
    def disconnect(self):
        """Close database connection"""
        if self.connection and self.connection.is_connected():
            self.connection.close()
            print("MySQL connection closed")
    
    def clear_tables(self):
        """Clear all tables in correct order to respect foreign key constraints"""
        print("Clearing existing data...")
        cursor = self.connection.cursor()
        
        # Order matters due to foreign key constraints
        tables_to_clear = [
            'student_enrolled_subjects',
            'enrollments_details',
            'enrollments',
            'schedules',
            'prerequisites',
            'sections',
            'subjects',
            'curriculum',
            'faculty',
            'students',
            'users',
            'rooms',
            'courses',
            'departments',
            'enrollment_period'
        ]
        
        for table in tables_to_clear:
            try:
                cursor.execute(f"DELETE FROM {table}")
                print(f"Cleared table: {table}")
            except Error as e:
                print(f"Error clearing {table}: {e}")
        
        self.connection.commit()
        cursor.close()
    
    def seed_departments(self, count=8):
        """Seed departments table"""
        print(f"Seeding {count} departments...")
        cursor = self.connection.cursor()
        
        department_data = [
            ('College of Engineering', 'Offers various engineering programs including Civil, Electrical, Mechanical, and Computer Engineering'),
            ('College of Business Administration', 'Provides business education with majors in Management, Accounting, Marketing, and Finance'),
            ('College of Arts and Sciences', 'Liberal arts college offering programs in Humanities, Social Sciences, and Natural Sciences'),
            ('College of Education', 'Teacher education institution producing future educators and school administrators'),
            ('College of Nursing', 'Healthcare education provider offering Bachelor of Science in Nursing'),
            ('College of Information Technology', 'Technology-focused college offering Computer Science, Information Technology, and Data Science programs'),
            ('College of Architecture', 'Architecture and design education provider'),
            ('College of Law', 'Legal education institution offering Juris Doctor program')
        ]
        
        for i, (name, description) in enumerate(department_data[:count]):
            query = """
            INSERT INTO departments (department_name, description)
            VALUES (%s, %s)
            """
            cursor.execute(query, (name, description))
            self.departments.append({
                'id': cursor.lastrowid,
                'name': name,
                'description': description
            })
        
        self.connection.commit()
        cursor.close()
        print(f"Created {len(self.departments)} departments")
    
    def seed_courses(self, count=15):
        """Seed courses table"""
        print(f"Seeding {count} courses...")
        cursor = self.connection.cursor()
        
        course_data = [
            ('Bachelor of Science in Computer Science', '4-year degree program focusing on software development, algorithms, and computing theory', 6),
            ('Bachelor of Science in Information Technology', 'Program focusing on IT infrastructure, network management, and systems administration', 6),
            ('Bachelor of Science in Civil Engineering', '5-year program covering structural design, construction management, and transportation engineering', 0),
            ('Bachelor of Science in Electrical Engineering', '5-year program focusing on power systems, electronics, and telecommunications', 0),
            ('Bachelor of Science in Mechanical Engineering', '5-year program covering thermodynamics, machine design, and manufacturing processes', 0),
            ('Bachelor of Science in Accountancy', '4-year program preparing students for CPA licensure and accounting careers', 1),
            ('Bachelor of Science in Business Administration', '4-year program with majors in Management, Marketing, and Finance', 1),
            ('Bachelor of Arts in English', '4-year liberal arts program focusing on literature and language studies', 2),
            ('Bachelor of Science in Psychology', '4-year program studying human behavior and mental processes', 2),
            ('Bachelor of Secondary Education', '4-year teacher education program', 3),
            ('Bachelor of Science in Nursing', '4-year professional nursing program', 4),
            ('Bachelor of Science in Architecture', '5-year professional architecture program', 6),
            ('Bachelor of Arts in Communication', '4-year program in mass communication and media studies', 2),
            ('Bachelor of Science in Mathematics', '4-year program in pure and applied mathematics', 2),
            ('Juris Doctor', '4-year professional law degree program', 7)
        ]
        
        for i, (name, description, dept_id) in enumerate(course_data[:count]):
            if dept_id < len(self.departments):
                query = """
                INSERT INTO courses (course_name, description, department_id)
                VALUES (%s, %s, %s)
                """
                cursor.execute(query, (name, description, self.departments[dept_id]['id']))
                self.courses.append({
                    'id': cursor.lastrowid,
                    'name': name,
                    'department_id': self.departments[dept_id]['id']
                })
        
        self.connection.commit()
        cursor.close()
        print(f"Created {len(self.courses)} courses")
    
    def seed_rooms(self, count=30):
        """Seed rooms table"""
        print(f"Seeding {count} rooms...")
        cursor = self.connection.cursor()
        
        room_types = [
            ('Lecture Hall', 150),
            ('Laboratory', 40),
            ('Computer Lab', 35),
            ('Discussion Room', 25),
            ('Conference Room', 20),
            ('Auditorium', 300),
            ('Classroom', 50),
            ('Seminar Room', 30)
        ]
        
        for i in range(count):
            room_type, base_capacity = random.choice(room_types)
            building = random.choice(['Engineering', 'Business', 'Arts', 'Science', 'Main'])
            floor = random.randint(1, 5)
            room_number = f"{building[0]}{floor:02d}{i+1:03d}"
            
            # Add some variation to capacity
            capacity = base_capacity + random.randint(-10, 20)
            capacity = max(15, capacity)  # Minimum capacity
            
            query = """
            INSERT INTO rooms (room, capacity)
            VALUES (%s, %s)
            """
            cursor.execute(query, (room_number, capacity))
            self.rooms.append({
                'id': cursor.lastrowid,
                'room': room_number,
                'capacity': capacity
            })
        
        self.connection.commit()
        cursor.close()
        print(f"Created {len(self.rooms)} rooms")
    
    def seed_users(self, student_count=500, faculty_count=50, registrar_count=5):
        """Seed users table with students, faculty, and registrars"""
        print(f"Seeding {student_count} students, {faculty_count} faculty, and {registrar_count} registrars...")
        cursor = self.connection.cursor()
        
        # Create student users
        for i in range(student_count):
            email = fake.unique.email()
            # Simple password hash simulation (in real app, use proper hashing)
            password = 'hashed_password_' + fake.password(length=20)
            role = 'STUDENT'
            
            query = """
            INSERT INTO users (email, password, role)
            VALUES (%s, %s, %s)
            """
            cursor.execute(query, (email, password, role))
            self.users.append({
                'id': cursor.lastrowid,
                'email': email,
                'role': role,
                'type': 'student'
            })
        
        # Create faculty users
        for i in range(faculty_count):
            email = fake.unique.email()
            password = 'hashed_password_' + fake.password(length=20)
            role = 'FACULTY'
            
            query = """
            INSERT INTO users (email, password, role)
            VALUES (%s, %s, %s)
            """
            cursor.execute(query, (email, password, role))
            self.users.append({
                'id': cursor.lastrowid,
                'email': email,
                'role': role,
                'type': 'faculty'
            })
        
        # Create registrar users
        for i in range(registrar_count):
            email = fake.unique.email()
            password = 'hashed_password_' + fake.password(length=20)
            role = 'REGISTRAR'
            
            query = """
            INSERT INTO users (email, password, role)
            VALUES (%s, %s, %s)
            """
            cursor.execute(query, (email, password, role))
            self.users.append({
                'id': cursor.lastrowid,
                'email': email,
                'role': role,
                'type': 'registrar'
            })
        
        self.connection.commit()
        cursor.close()
        print(f"Created {len(self.users)} users")
    
    def seed_students(self):
        """Seed students table"""
        print("Seeding students...")
        cursor = self.connection.cursor()
        
        student_users = [u for u in self.users if u['type'] == 'student']
        
        for user in student_users:
            # Generate student ID (e.g., 2023-12345)
            year = random.randint(2020, 2024)
            student_number = f"{year}-{random.randint(10000, 99999)}"
            
            first_name = fake.first_name()
            last_name = fake.last_name()
            middle_name = fake.first_name() if random.random() > 0.3 else None
            
            # Generate realistic birthdate (18-25 years old)
            age = random.randint(18, 25)
            birthdate = datetime.now() - timedelta(days=age*365)
            
            student_status = random.choice(['REGULAR', 'IRREGULAR'])
            course = random.choice(self.courses)
            year_level = min(random.randint(1, 5), 4 if course['name'].startswith('Bachelor') else 5)
            
            query = """
            INSERT INTO students (student_id, user_id, first_name, last_name, middle_name, 
                                 birthdate, student_status, course_id, year_level)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
            """
            cursor.execute(query, (student_number, user['id'], first_name, last_name, 
                                 middle_name, birthdate, student_status, course['id'], year_level))
            
            self.students.append({
                'student_id': student_number,
                'user_id': user['id'],
                'first_name': first_name,
                'last_name': last_name,
                'course_id': course['id'],
                'year_level': year_level
            })
        
        self.connection.commit()
        cursor.close()
        print(f"Created {len(self.students)} students")
    
    def seed_faculty(self):
        """Seed faculty table"""
        print("Seeding faculty...")
        cursor = self.connection.cursor()
        
        faculty_users = [u for u in self.users if u['type'] == 'faculty']
        
        for user in faculty_users:
            first_name = fake.first_name()
            last_name = fake.last_name()
            department = random.choice(self.departments)
            
            query = """
            INSERT INTO faculty (user_id, first_name, last_name, department_id)
            VALUES (%s, %s, %s, %s)
            """
            cursor.execute(query, (user['id'], first_name, last_name, department['id']))
            
            self.faculty.append({
                'id': cursor.lastrowid,
                'user_id': user['id'],
                'first_name': first_name,
                'last_name': last_name,
                'department_id': department['id']
            })
        
        self.connection.commit()
        cursor.close()
        print(f"Created {len(self.faculty)} faculty members")
    
    def seed_curriculum(self):
        """Seed curriculum table"""
        print("Seeding curriculum...")
        cursor = self.connection.cursor()
        
        for course in self.courses:
            # Create curriculum for each semester (1-8)
            for year in range(1, 5):
                for semester in [1, 2]:
                    curriculum_year = datetime.now() - timedelta(days=random.randint(365, 1825))
                    
                    query = """
                    INSERT INTO curriculum (semester, cur_year, course)
                    VALUES (%s, %s, %s)
                    """
                    cursor.execute(query, (f"Semester {semester}", curriculum_year, course['id']))
                    
                    self.curriculums.append({
                        'id': cursor.lastrowid,
                        'semester': f"Semester {semester}",
                        'course_id': course['id']
                    })
        
        self.connection.commit()
        cursor.close()
        print(f"Created {len(self.curriculums)} curriculum entries")
    
    def seed_subjects(self, count=100):
        """Seed subjects table"""
        print(f"Seeding {count} subjects...")
        cursor = self.connection.cursor()
        
        subject_templates = [
            ('Calculus', 'CALC', 3, 'Mathematical analysis of functions, limits, derivatives, and integrals'),
            ('Physics', 'PHYS', 4, 'Study of matter, energy, and their interactions'),
            ('Chemistry', 'CHEM', 3, 'Study of matter, its properties, composition, and reactions'),
            ('Programming', 'PROG', 3, 'Introduction to computer programming concepts and practices'),
            ('Data Structures', 'DS', 3, 'Study of data organization and manipulation algorithms'),
            ('Database Systems', 'DB', 3, 'Design and implementation of database management systems'),
            ('Software Engineering', 'SE', 3, 'Principles and practices of software development'),
            ('Web Development', 'WEB', 3, 'Design and development of web applications'),
            ('Accounting Principles', 'ACC', 3, 'Fundamental concepts and principles of accounting'),
            ('Business Finance', 'FIN', 3, 'Financial management and analysis in business'),
            ('Marketing Management', 'MKT', 3, 'Principles and strategies in marketing'),
            ('Organizational Behavior', 'ORG', 3, 'Study of human behavior in organizations'),
            ('Educational Psychology', 'EDPSY', 3, 'Psychological principles in education'),
            ('Teaching Methods', 'TCH', 3, 'Methodologies and strategies in teaching'),
            ('Nursing Fundamentals', 'NURS', 4, 'Basic principles and practices in nursing'),
            ('Anatomy and Physiology', 'ANAT', 4, 'Study of human body structure and function'),
            ('Engineering Mathematics', 'ENG MATH', 3, 'Advanced mathematics for engineering applications'),
            ('Thermodynamics', 'THERMO', 3, 'Study of heat, work, and energy'),
            ('Strength of Materials', 'SOM', 3, 'Analysis of material properties under stress'),
            ('Circuit Analysis', 'CIRCUIT', 3, 'Analysis of electrical circuits and components'),
            ('Digital Logic', 'DIGITAL', 3, 'Study of digital systems and logic design'),
            ('Machine Design', 'MACH', 3, 'Principles of mechanical machine design'),
            ('Structural Analysis', 'STRUCT', 3, 'Analysis of structures and loads'),
            ('Business Law', 'BLAW', 3, 'Legal aspects of business operations'),
            ('Cost Accounting', 'COST', 3, 'Accounting for product and service costs'),
            ('Human Resource Management', 'HRM', 3, 'Management of human resources in organizations'),
            ('Operations Management', 'OPS', 3, 'Management of production and service operations'),
            ('Literature', 'LIT', 3, 'Study of literary works and criticism'),
            ('Philosophy', 'PHIL', 3, 'Study of fundamental questions about existence and knowledge'),
            ('Statistics', 'STAT', 3, 'Mathematical analysis of data and probability'),
            ('Research Methods', 'RES', 3, 'Methodologies for conducting research'),
        ]
        
        for i in range(count):
            template = random.choice(subject_templates)
            subject_name = f"{template[0]} {random.randint(1, 4)}"
            subject_code = f"{template[1]}{random.randint(100, 999)}"
            units = template[2]
            description = template[3]
            
            curriculum = random.choice(self.curriculums) if self.curriculums else None
            department = random.choice(self.departments)
            
            query = """
            INSERT INTO subjects (subject_name, subject_code, units, description, curriculum_id, department_id)
            VALUES (%s, %s, %s, %s, %s, %s)
            """
            cursor.execute(query, (subject_name, subject_code, units, description, 
                                 curriculum['id'] if curriculum else None, department['id']))
            
            self.subjects.append({
                'id': cursor.lastrowid,
                'name': subject_name,
                'code': subject_code,
                'units': units,
                'department_id': department['id']
            })
        
        self.connection.commit()
        cursor.close()
        print(f"Created {len(self.subjects)} subjects")
    
    def seed_sections(self):
        """Seed sections table"""
        print("Seeding sections...")
        cursor = self.connection.cursor()
        
        # Create 2-4 sections for each subject
        for subject in self.subjects:
            num_sections = random.randint(2, 4)
            
            for i in range(num_sections):
                section_name = f"{subject['code']}-{chr(65 + i)}"  # A, B, C, D
                section_code = f"SEC{subject['id']}-{i+1}"
                capacity = random.randint(25, 50)
                
                query = """
                INSERT INTO sections (section_name, section_code, subject_id, capacity)
                VALUES (%s, %s, %s, %s)
                """
                cursor.execute(query, (section_name, section_code, subject['id'], capacity))
                
                self.sections.append({
                    'id': cursor.lastrowid,
                    'name': section_name,
                    'code': section_code,
                    'subject_id': subject['id'],
                    'capacity': capacity
                })
        
        self.connection.commit()
        cursor.close()
        print(f"Created {len(self.sections)} sections")
    
    def seed_enrollment_periods(self, count=4):
        """Seed enrollment periods"""
        print(f"Seeding {count} enrollment periods...")
        cursor = self.connection.cursor()
        
        for i in range(count):
            year = 2024 - i
            for semester in ['First Semester', 'Second Semester']:
                start_date = datetime(year, 8 if semester == 'First Semester' else 1, 1)
                end_date = start_date + timedelta(days=120)
                
                query = """
                INSERT INTO enrollment_period (school_year, semester, start_date, end_date)
                VALUES (%s, %s, %s, %s)
                """
                cursor.execute(query, (f"{year}-{year+1}", semester, start_date, end_date))
                
                self.enrollment_periods.append({
                    'id': cursor.lastrowid,
                    'school_year': f"{year}-{year+1}",
                    'semester': semester
                })
        
        self.connection.commit()
        cursor.close()
        print(f"Created {len(self.enrollment_periods)} enrollment periods")
    
    def seed_schedules(self):
        """Seed schedules table"""
        print("Seeding schedules...")
        cursor = self.connection.cursor()
        
        days = ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT']
        
        for section in self.sections:
            # Create 1-3 schedules for each section
            num_schedules = random.randint(1, 3)
            
            for _ in range(num_schedules):
                room = random.choice(self.rooms)
                faculty = random.choice(self.faculty)
                day = random.choice(days)
                
                # Generate realistic time slots
                start_hour = random.randint(7, 18)
                start_minute = random.choice([0, 30])
                start_time = datetime.strptime(f"{start_hour:02d}:{start_minute:02d}", "%H:%M").time()
                
                duration = random.choice([1, 1.5, 2, 3]) * 60
                end_time = (datetime.combine(datetime.now(), start_time) + timedelta(minutes=duration)).time()
                
                school_year = random.choice([f"{y}-{y+1}" for y in range(2021, 2025)])
                semester = random.randint(1, 2)
                
                query = """
                INSERT INTO schedules (section_id, room_id, faculty_id, day, start_time, end_time, school_year, semester)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
                """
                cursor.execute(query, (section['id'], room['id'], faculty['id'], day, 
                                     start_time, end_time, school_year, semester))
        
        self.connection.commit()
        cursor.close()
        print("Created schedules")
    
    def seed_prerequisites(self):
        """Seed prerequisites table"""
        print("Seeding prerequisites...")
        cursor = self.connection.cursor()
        
        # Create some prerequisite relationships
        for i, subject in enumerate(self.subjects[:50]):  # Limit to first 50 subjects
            if random.random() > 0.7:  # 30% chance of having prerequisites
                num_prereqs = random.randint(1, 2)
                available_prereqs = [s for s in self.subjects if s['id'] != subject['id']]
                
                for _ in range(num_prereqs):
                    if available_prereqs:
                        prereq = random.choice(available_prereqs)
                        available_prereqs.remove(prereq)
                        
                        query = """
                        INSERT INTO prerequisites (pre_subject_id, subject_id)
                        VALUES (%s, %s)
                        """
                        cursor.execute(query, (prereq['id'], subject['id']))
        
        self.connection.commit()
        cursor.close()
        print("Created prerequisites")
    
    def seed_enrollments(self):
        """Seed enrollments and enrollment details"""
        print("Seeding enrollments...")
        cursor = self.connection.cursor()
        
        statuses = ['DRAFT', 'SUBMITTED', 'APPROVED', 'ENROLLED', 'CANCELLED']
        
        for student in self.students:
            # Create 1-4 enrollments per student
            num_enrollments = random.randint(1, 4)
            
            for _ in range(num_enrollments):
                school_year = random.choice([f"{y}-{y+1}" for y in range(2021, 2025)])
                semester = random.randint(1, 2)
                status = random.choice(statuses)
                
                max_units = random.uniform(15, 24)
                total_units = random.uniform(12, max_units)
                
                submitted_at = fake.date_time_between(start_date='-2y', end_date='now')
                
                query = """
                INSERT INTO enrollments (student_id, school_year, semester, status, max_units, total_units, submitted_at)
                VALUES (%s, %s, %s, %s, %s, %s, %s)
                """
                cursor.execute(query, (student['student_id'], school_year, semester, status, 
                                     max_units, total_units, submitted_at))
                
                enrollment_id = cursor.lastrowid
                
                # Add enrollment details
                if status in ['APPROVED', 'ENROLLED']:
                    num_subjects = random.randint(3, 7)
                    available_sections = random.sample(self.sections, min(num_subjects, len(self.sections)))
                    
                    for section in available_sections:
                        detail_status = random.choice(['SELECTED', 'DROPPED'])
                        units = next((s['units'] for s in self.subjects if s['id'] == section['subject_id']), 3)
                        
                        query = """
                        INSERT INTO enrollments_details (enrollment_id, section_id, subject_id, units, status)
                        VALUES (%s, %s, %s, %s, %s)
                        """
                        cursor.execute(query, (enrollment_id, section['id'], section['subject_id'], units, detail_status))
                        
                        # Add to student_enrolled_subjects if enrolled
                        if status == 'ENROLLED' and detail_status == 'SELECTED':
                            query = """
                            INSERT IGNORE INTO student_enrolled_subjects (student_id, subject_id)
                            VALUES (%s, %s)
                            """
                            cursor.execute(query, (student['student_id'], section['subject_id']))
        
        self.connection.commit()
        cursor.close()
        print("Created enrollments and enrollment details")
    
    def seed_all(self, clear_existing=True):
        """Seed all tables with comprehensive data"""
        if not self.connect():
            return False
        
        try:
            if clear_existing:
                self.clear_tables()
            
            # Seed in order respecting foreign key constraints
            self.seed_departments()
            self.seed_courses()
            self.seed_rooms()
            self.seed_users(student_count=500, faculty_count=50, registrar_count=5)
            self.seed_students()
            self.seed_faculty()
            self.seed_curriculum()
            self.seed_subjects(count=100)
            self.seed_sections()
            self.seed_enrollment_periods(count=4)
            self.seed_schedules()
            self.seed_prerequisites()
            self.seed_enrollments()
            
            print("\n=== Database Seeding Complete ===")
            print(f"Departments: {len(self.departments)}")
            print(f"Courses: {len(self.courses)}")
            print(f"Rooms: {len(self.rooms)}")
            print(f"Users: {len(self.users)}")
            print(f"Students: {len(self.students)}")
            print(f"Faculty: {len(self.faculty)}")
            print(f"Subjects: {len(self.subjects)}")
            print(f"Sections: {len(self.sections)}")
            print(f"Curriculum entries: {len(self.curriculums)}")
            print(f"Enrollment periods: {len(self.enrollment_periods)}")
            
            return True
            
        except Exception as e:
            print(f"Error during seeding: {e}")
            return False
        finally:
            self.disconnect()

def main():
    parser = argparse.ArgumentParser(description='University Database Seeder')
    parser.add_argument('--host', default='localhost', help='Database host')
    parser.add_argument('--database', default='university_db', help='Database name')
    parser.add_argument('--user', default='root', help='Database user')
    parser.add_argument('--password', default='', help='Database password')
    parser.add_argument('--no-clear', action='store_true', help='Do not clear existing data')
    
    args = parser.parse_args()
    
    seeder = UniversityDatabaseSeeder(
        host=args.host,
        database=args.database,
        user=args.user,
        password=args.password
    )
    
    success = seeder.seed_all(clear_existing=not args.no_clear)
    
    if success:
        print("Database seeding completed successfully!")
        sys.exit(0)
    else:
        print("Database seeding failed!")
        sys.exit(1)

if __name__ == "__main__":
    main()
